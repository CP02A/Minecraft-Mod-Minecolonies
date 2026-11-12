package com.minecolonies.gametest.client;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.AbstractTextElement;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.View;
import com.minecolonies.gametest.framework.E2EMode;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for UI tests: open a BlockUI window, read what it shows, take screenshots. Everything that touches a window runs on the client
 * thread ({@code context.runOnClient}/{@code computeOnClient}); the methods here are called from the client game test thread.
 */
public final class UiTestSupport
{
    /**
     * Ticks to wait after a window opened before reading or photographing it: lets {@code onOpened}/{@code onUpdate} fill lists and
     * labels, and textures load.
     */
    public static final int SETTLE_TICKS = 10;

    /**
     * Last text the HUD drew as the selected item name above the hotbar (set by the test mod's {@code HudSelectedItemNameCaptureMixin}).
     */
    @Nullable
    public static volatile String lastSelectedItemName = null;

    /**
     * Text that looks like an untranslated translation key, e.g. {@code com.minecolonies.coremod.gui.foo} or {@code structurize.gui.bar}.
     */
    private static final Pattern TRANSLATION_KEY = Pattern.compile(
      "\\b(?:com\\.minecolonies|com\\.ldtteam|minecolonies|structurize|blockui|domum_ornamentum|block\\.minecolonies|item\\.minecolonies)(?:\\.[a-z0-9_]+){1,}\\b");

    private UiTestSupport()
    {
    }

    /**
     * Opens a window (created on the client thread by {@code factory}), waits until it is the current screen and has settled.
     *
     * @return the window.
     */
    public static BOWindow open(final ClientGameTestContext context, final Function<Minecraft, BOWindow> factory)
    {
        context.runOnClient(mc -> factory.apply(mc).open());
        context.waitFor(mc -> mc.gui.screen() instanceof BOScreen, 200);
        context.waitTicks(SETTLE_TICKS);
        return context.computeOnClient(mc -> mc.gui.screen() instanceof final BOScreen screen ? screen.getWindow() : null);
    }

    /**
     * Closes whatever screen is open.
     */
    public static void closeScreen(final ClientGameTestContext context)
    {
        context.runOnClient(mc -> mc.gui.setScreen(null));
        context.waitTicks(2);
    }

    /**
     * Takes a screenshot of the whole window to {@code build/e2e-screenshots/<name>.png}.
     */
    public static Path screenshot(final ClientGameTestContext context, final String name)
    {
        return context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withDestinationDir(E2EMode.screenshotDir()));
    }

    /**
     * @return every visible text of the window (labels, buttons, list rows), in tree order.
     */
    public static List<String> texts(final ClientGameTestContext context, final BOWindow window)
    {
        return context.computeOnClient(mc -> {
            final List<String> out = new ArrayList<>();
            collect(window, out);
            return out;
        });
    }

    private static void collect(final Pane pane, final List<String> out)
    {
        if (!pane.isVisible())
        {
            return;
        }
        if (pane instanceof final AbstractTextElement text && text.getTextAsList() != null)
        {
            for (final Component line : text.getTextAsList())
            {
                final String value = line.getString();
                if (!value.isBlank())
                {
                    out.add(value);
                }
            }
        }
        if (pane instanceof final View view)
        {
            for (final Pane child : new ArrayList<>(view.getChildren()))
            {
                collect(child, out);
            }
        }
    }

    /**
     * @return the text of the pane with this id (all lines joined), or null if there is no such text pane.
     */
    @Nullable
    public static String textOf(final ClientGameTestContext context, final BOWindow window, final String id)
    {
        return context.computeOnClient(mc -> {
            final Pane pane = window.findPaneByID(id);
            if (!(pane instanceof final AbstractTextElement text) || text.getTextAsList() == null)
            {
                return null;
            }
            return String.join("\n", text.getTextAsList().stream().map(Component::getString).toList());
        });
    }

    /**
     * @return the texts that look like untranslated translation keys.
     */
    public static List<String> untranslated(final List<String> texts)
    {
        final List<String> keys = new ArrayList<>();
        for (final String text : texts)
        {
            final Matcher matcher = TRANSLATION_KEY.matcher(text);
            while (matcher.find())
            {
                keys.add(matcher.group());
            }
        }
        return keys;
    }

    /**
     * Fails if the texts contain untranslated keys.
     */
    public static void assertTranslated(final String what, final List<String> texts)
    {
        final List<String> keys = untranslated(texts);
        if (!keys.isEmpty())
        {
            throw new AssertionError(what + " shows untranslated keys: " + keys);
        }
    }

    public static void assertThat(final boolean condition, final String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }
}
