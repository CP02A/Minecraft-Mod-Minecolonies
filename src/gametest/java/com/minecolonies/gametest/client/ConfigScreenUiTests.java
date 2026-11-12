package com.minecolonies.gametest.client;

import com.ldtteam.shim.fabric.client.config.ShimConfigScreens;
import com.ldtteam.shim.fml.config.ModConfig;
import com.ldtteam.shim.neoforge.client.gui.ConfigurationListScreen;
import com.ldtteam.shim.neoforge.client.gui.ConfigurationScreen;
import com.ldtteam.shim.neoforge.client.gui.ConfigurationSectionScreen;
import com.minecolonies.core.MineColonies;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.gametest.client.UiTestSupport.SETTLE_TICKS;
import static com.minecolonies.gametest.client.UiTestSupport.assertThat;
import static com.minecolonies.gametest.client.UiTestSupport.closeScreen;
import static com.minecolonies.gametest.client.UiTestSupport.screenshot;

/**
 * UI-14 and UI-15 (docs/e2e-tests/ui.md): the in-game config screen of the Fabric shim (what Mod Menu opens for MineColonies), driven
 * with simulated mouse and keyboard input. Values are changed in the screen, saved with Done and then checked in memory and in the
 * config file the shim loads.
 */
final class ConfigScreenUiTests
{
    private static final String MOD_MENU_ENTRYPOINT = "com.ldtteam.shim.fabric.client.config.ShimModMenuIntegration";

    private ConfigScreenUiTests()
    {
    }

    /**
     * UI-14: from the main menu: the mod list screen lists the bundled mods with settings, MineColonies' screen offers its three config
     * files (server read-only outside a world), and client values changed with mouse and keyboard are saved to the client config file.
     */
    static void clientSettings(final ClientGameTestContext context, final List<Path> shots)
    {
        // The modmenu entrypoint is declared (Mod Menu itself is not in the test runtime; the entrypoint only hands out the same factory).
        final List<String> definitions = new ArrayList<>();
        for (final EntrypointContainer<Object> container : FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class))
        {
            definitions.add(container.getDefinition());
        }
        assertThat(definitions.contains(MOD_MENU_ENTRYPOINT), "fabric.mod.json declares no modmenu entrypoint " + MOD_MENU_ENTRYPOINT + ", found " + definitions);

        // 1. The screen Mod Menu opens for MineColonies: the bundled mods that have settings.
        context.runOnClient(mc -> mc.gui.setScreen(ShimConfigScreens.createModListScreen(mc.gui.screen())));
        context.waitFor(mc -> mc.gui.screen() instanceof ShimConfigScreens.ModChoiceScreen, 100);
        context.waitTicks(SETTLE_TICKS);
        shots.add(screenshot(context, "ui14-config-mods"));
        final List<String> mods = context.computeOnClient(mc -> ((ShimConfigScreens.ModChoiceScreen) mc.gui.screen()).modIds());
        assertThat(mods.contains("minecolonies") && mods.contains("structurize"), "Mod list should offer minecolonies and structurize, offers " + mods);

        // 2. Structurize's screen opens too (client and server settings).
        context.runOnClient(mc -> ((ShimConfigScreens.ModChoiceScreen) mc.gui.screen()).openMod("structurize"));
        context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);
        context.waitTicks(SETTLE_TICKS);
        shots.add(screenshot(context, "ui14-config-structurize"));
        final List<String> structurizeButtons = buttonTexts(context);
        assertThat(structurizeButtons.contains("Client settings"), "Structurize screen should have 'Client settings', has " + structurizeButtons);
        context.runOnClient(mc -> mc.gui.screen().onClose());
        context.waitFor(mc -> mc.gui.screen() instanceof ShimConfigScreens.ModChoiceScreen, 100);

        // 3. MineColonies' screen: client and common editable, server read-only in the main menu.
        context.runOnClient(mc -> ((ShimConfigScreens.ModChoiceScreen) mc.gui.screen()).openMod("minecolonies"));
        context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);
        context.waitTicks(SETTLE_TICKS);
        shots.add(screenshot(context, "ui14-config-minecolonies"));
        final List<String> buttons = buttonTexts(context);
        assertThat(buttons.contains("Client settings") && buttons.contains("Common settings") && buttons.contains("Server settings (read-only)"),
          "MineColonies screen should offer 'Client settings', 'Common settings' and 'Server settings (read-only)', has " + buttons);

        // The server section is read-only: values shown, no Done.
        context.runOnClient(mc -> ((ConfigurationScreen) mc.gui.screen()).openSection(ModConfig.Type.SERVER));
        context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationSectionScreen, 100);
        context.waitTicks(SETTLE_TICKS);
        shots.add(screenshot(context, "ui14-config-server-readonly"));
        final boolean serverEditable = context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).isEditable());
        assertThat(!serverEditable, "Server settings must be read-only in the main menu");
        final boolean serverBoxEditable = context.computeOnClient(mc -> {
            final AbstractWidget widget = ((ConfigurationSectionScreen) mc.gui.screen()).widgetFor("gameplay.foodmodifier");
            return widget instanceof final EditBox box && box.canConsumeInput();
        });
        assertThat(!serverBoxEditable, "The read-only server screen lets you type into gameplay.foodmodifier");
        context.runOnClient(mc -> mc.gui.screen().onClose());
        context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);

        // 4. Client settings: toggle a boolean, type a number, try an invalid number.
        final var client = MineColonies.getConfig().getClient();
        final boolean holidayBefore = context.computeOnClient(mc -> client.holidayFeatures.get());
        final int rangeBefore = context.computeOnClient(mc -> client.neighborbuildingrange.get());
        final int newRange = rangeBefore == 9 ? 10 : 9;
        try
        {
            context.runOnClient(mc -> ((ConfigurationScreen) mc.gui.screen()).openSection(ModConfig.Type.CLIENT));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationSectionScreen, 100);
            context.waitTicks(SETTLE_TICKS);
            shots.add(screenshot(context, "ui14-config-client"));
            assertThat(context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).isEditable()), "Client settings should be editable");

            final AbstractWidget holiday = widget(context, "gameplay.holidayfeatures");
            assertThat(holiday instanceof CycleButton<?>, "gameplay.holidayfeatures should be an on/off button, is " + holiday);
            click(context, holiday);
            assertThat(context.computeOnClient(mc -> !Boolean.valueOf(holidayBefore).equals(((CycleButton<?>) holiday).getValue())),
              "Clicking the holidayfeatures button did not toggle it");

            final AbstractWidget range = widget(context, "gameplay.neighborbuildingrange");
            assertThat(range instanceof EditBox, "gameplay.neighborbuildingrange should be a text box, is " + range);
            replaceText(context, range, String.valueOf(newRange));
            assertThat(context.computeOnClient(mc -> ((EditBox) range).getValue()).equals(String.valueOf(newRange)),
              "Typing into neighborbuildingrange gave '" + context.computeOnClient(mc -> ((EditBox) range).getValue()) + "'");

            final AbstractWidget goggles = widget(context, "gameplay.buildgogglerange");
            final String gogglesBefore = context.computeOnClient(mc -> ((EditBox) goggles).getValue());
            replaceText(context, goggles, "999");
            context.waitTicks(2);
            shots.add(screenshot(context, "ui14-config-client-invalid"));
            assertThat(!context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).canSave()),
              "buildgogglerange 999 (max 250) should block Done");
            replaceText(context, goggles, gogglesBefore);
            assertThat(context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).canSave()), "Done stays blocked after the value was fixed");
            context.waitTicks(2);
            shots.add(screenshot(context, "ui14-config-client-edited"));

            // 5. Done: saved in memory and to the client config file.
            click(context, button(context, CommonComponents.GUI_DONE.getString()));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);
            assertThat(context.computeOnClient(mc -> client.holidayFeatures.get()) == !holidayBefore,
              "holidayfeatures in memory should be " + !holidayBefore + " after Done");
            assertThat(context.computeOnClient(mc -> client.neighborbuildingrange.get()) == newRange,
              "neighborbuildingrange in memory should be " + newRange + " after Done, is " + context.computeOnClient(mc -> client.neighborbuildingrange.get()));
            final String file = configFile(context, "minecolonies", ModConfig.Type.CLIENT);
            assertThat(file.contains("neighborbuildingrange = " + newRange) && file.contains("holidayfeatures = " + !holidayBefore),
              "Client config file should contain 'neighborbuildingrange = " + newRange + "' and 'holidayfeatures = " + !holidayBefore + "':\n" + file);
            assertThat(file.contains("buildgogglerange = " + gogglesBefore), "buildgogglerange in the file should be unchanged (" + gogglesBefore + "):\n" + file);
        }
        finally
        {
            context.runOnClient(mc -> {
                client.holidayFeatures.set(holidayBefore);
                client.neighborbuildingrange.set(rangeBefore);
                client.holidayFeatures.save();
            });
            closeScreen(context);
        }
    }

    /**
     * UI-15: in a singleplayer world the server settings are editable; a value changed in the screen reaches the integrated server and the
     * world's serverconfig file. The list editor opens for a list value.
     */
    static void serverSettings(final ClientGameTestContext context, final TestServerContext server, final List<Path> shots)
    {
        final var serverConfig = MineColonies.getConfig().getServer();
        final double before = server.computeOnServer(s -> serverConfig.foodModifier.get());
        final double changed = before == 2.5 ? 3.5 : 2.5;
        try
        {
            context.runOnClient(mc -> mc.gui.setScreen(ShimConfigScreens.factories().get("minecolonies").apply(null)));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);
            context.waitTicks(SETTLE_TICKS);
            final List<String> buttons = buttonTexts(context);
            assertThat(buttons.contains("Server settings"), "In a singleplayer world the server settings should be editable, buttons: " + buttons);

            context.runOnClient(mc -> ((ConfigurationScreen) mc.gui.screen()).openSection(ModConfig.Type.SERVER));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationSectionScreen, 100);
            context.waitTicks(SETTLE_TICKS);
            shots.add(screenshot(context, "ui15-config-server"));
            assertThat(context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).isEditable()), "Server settings should be editable in singleplayer");

            // The list editor (researchresetcost).
            final AbstractWidget listButton = widget(context, "research.researchresetcost");
            assertThat(listButton instanceof AbstractButton, "research.researchresetcost should be an edit-list button, is " + listButton);
            click(context, listButton);
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationListScreen, 100);
            context.waitTicks(SETTLE_TICKS);
            shots.add(screenshot(context, "ui15-config-server-list"));
            click(context, button(context, CommonComponents.GUI_CANCEL.getString()));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationSectionScreen, 100);

            final AbstractWidget food = widget(context, "gameplay.foodmodifier");
            assertThat(food instanceof EditBox, "gameplay.foodmodifier should be a text box, is " + food);
            replaceText(context, food, String.valueOf(changed));
            context.waitTicks(2);
            shots.add(screenshot(context, "ui15-config-server-edited"));
            click(context, button(context, CommonComponents.GUI_DONE.getString()));
            context.waitFor(mc -> mc.gui.screen() instanceof ConfigurationScreen, 100);

            final double onServer = server.computeOnServer(s -> serverConfig.foodModifier.get());
            assertThat(onServer == changed, "foodmodifier on the integrated server should be " + changed + " after Done, is " + onServer);
            final Path worldFile = server.computeOnServer(s -> s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
              .resolve("serverconfig").resolve(serverConfigFileName()).toAbsolutePath().normalize());
            final Path loadedFile = context.computeOnClient(mc -> configOf("minecolonies", ModConfig.Type.SERVER).getFullPath().toAbsolutePath().normalize());
            assertThat(worldFile.equals(loadedFile), "The server config should be saved in the world's serverconfig folder " + worldFile + ", is " + loadedFile);
            final String file = read(loadedFile);
            assertThat(file.contains("foodmodifier = " + changed), "Server config file should contain 'foodmodifier = " + changed + "':\n" + file);
        }
        finally
        {
            server.runOnServer(s -> {
                serverConfig.foodModifier.set(before);
                serverConfig.foodModifier.save();
            });
            closeScreen(context);
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------------------------------------------

    private static String serverConfigFileName()
    {
        return configOf("minecolonies", ModConfig.Type.SERVER).getFileName();
    }

    private static ModConfig configOf(final String modId, final ModConfig.Type type)
    {
        return com.ldtteam.shim.fml.config.ConfigTracker.INSTANCE.configsForMod(modId).stream()
          .filter(c -> c.getType() == type)
          .findFirst()
          .orElseThrow(() -> new AssertionError("No " + type + " config for " + modId));
    }

    private static String configFile(final ClientGameTestContext context, final String modId, final ModConfig.Type type)
    {
        final Path path = context.computeOnClient(mc -> configOf(modId, type).getFullPath());
        assertThat(path != null, modId + " " + type + " config has no file");
        return read(path);
    }

    private static String read(final Path path)
    {
        try
        {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (final IOException e)
        {
            throw new AssertionError("Could not read " + path, e);
        }
    }

    /**
     * @return the texts of the buttons on the current screen.
     */
    private static List<String> buttonTexts(final ClientGameTestContext context)
    {
        return context.computeOnClient(mc -> {
            final List<String> out = new ArrayList<>();
            for (final GuiEventListener child : mc.gui.screen().children())
            {
                if (child instanceof final AbstractButton b)
                {
                    out.add(b.getMessage().getString());
                }
            }
            return out;
        });
    }

    /**
     * @return the button of the current screen with this text.
     */
    private static AbstractWidget button(final ClientGameTestContext context, final String text)
    {
        final AbstractWidget found = context.computeOnClient(mc -> {
            for (final GuiEventListener child : mc.gui.screen().children())
            {
                if (child instanceof final AbstractButton b && b.getMessage().getString().equals(text))
                {
                    return b;
                }
            }
            return null;
        });
        assertThat(found != null, "No button '" + text + "' on the screen, buttons: " + buttonTexts(context));
        return found;
    }

    /**
     * @return the widget of a config value on the current section screen, scrolled into view (positions are set when the row is drawn).
     */
    @Nullable
    private static AbstractWidget widget(final ClientGameTestContext context, final String path)
    {
        context.runOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).scrollTo(path));
        context.waitTicks(3);
        return context.computeOnClient(mc -> ((ConfigurationSectionScreen) mc.gui.screen()).widgetFor(path));
    }

    /**
     * Clicks the middle of a widget with the simulated mouse.
     */
    private static void click(final ClientGameTestContext context, final LayoutElement widget)
    {
        final double[] pos = context.computeOnClient(mc -> {
            final int scale = mc.getWindow().getGuiScale();
            return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(2);
    }

    /**
     * Clicks a text box, deletes its text and types a new one.
     */
    private static void replaceText(final ClientGameTestContext context, final AbstractWidget box, final String text)
    {
        click(context, box);
        assertThat(context.computeOnClient(mc -> box.isFocused()), "Clicking the text box did not focus it");
        context.getInput().pressKey(InputConstants.KEY_END);
        final int length = context.computeOnClient(mc -> ((EditBox) box).getValue().length());
        for (int i = 0; i < length; i++)
        {
            context.getInput().pressKey(InputConstants.KEY_BACKSPACE);
        }
        context.getInput().typeChars(text);
        // Move the mouse away so no tooltip covers the screen in screenshots.
        context.getInput().setCursorPos(0, 0);
        context.waitTicks(1);
    }
}
