package com.ldtteam.shim.fabric.client.config;

import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.neoforge.client.gui.ConfigurationScreen;
import com.ldtteam.shim.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Config screens of the shimmed mods for mod list integrations (Mod Menu), without referencing any mod list classes. The shimmed mods
 * register their screens like on NeoForge ({@code ModContainer#registerExtensionPoint(IConfigScreenFactory.class, ...)}).
 * <p>
 * All shimmed mods live in one Fabric mod (MineColonies bundles Structurize, BlockUI and Domum Ornamentum), so the Fabric mod's entry opens
 * a list of the bundled mods that have settings, each opening that mod's own screen.
 */
public final class ShimConfigScreens
{
    private ShimConfigScreens()
    {
    }

    /**
     * @return the config screen factory of every shimmed mod that registered one, in mod order.
     */
    public static Map<String, Function<Screen, Screen>> factories()
    {
        final Map<String, Function<Screen, Screen>> factories = new LinkedHashMap<>();
        for (final ModContainer container : ShimModBuses.containers())
        {
            container.getCustomExtension(IConfigScreenFactory.class)
                .ifPresent(factory -> factories.put(container.getModId(), parent -> factory.createScreen(container, parent)));
        }
        return factories;
    }

    /**
     * @return the screen for the Fabric mod's entry in a mod list: the only shimmed mod's config screen, or a list of the shimmed mods
     * with settings; null if no shimmed mod has a config screen.
     */
    @Nullable
    public static Screen createModListScreen(@Nullable final Screen parent)
    {
        final Map<String, Function<Screen, Screen>> factories = factories();
        if (factories.isEmpty())
        {
            return null;
        }
        if (factories.size() == 1)
        {
            return factories.values().iterator().next().apply(parent);
        }
        return new ModChoiceScreen(parent, factories);
    }

    /**
     * Lists the bundled mods that have settings; each button opens that mod's config screen.
     */
    public static final class ModChoiceScreen extends Screen
    {
        @Nullable
        private final Screen                                parent;
        private final Map<String, Function<Screen, Screen>> factories;
        private final HeaderAndFooterLayout                 layout = new HeaderAndFooterLayout(this);
        private final List<Button>                          modButtons = new ArrayList<>();

        private ModChoiceScreen(@Nullable final Screen parent, final Map<String, Function<Screen, Screen>> factories)
        {
            super(Component.translatableWithFallback("minecolonies_shim.configuration.mods_title", "Bundled mods: settings"));
            this.parent = parent;
            this.factories = factories;
        }

        @Override
        protected void init()
        {
            modButtons.clear();
            layout.addTitleHeader(title, font);
            final LinearLayout buttons = layout.addToContents(LinearLayout.vertical().spacing(8));
            for (final Map.Entry<String, Function<Screen, Screen>> entry : factories.entrySet())
            {
                final ModContainer container = ShimModBuses.getContainer(entry.getKey());
                final String name = container == null ? entry.getKey() : ConfigurationScreen.displayName(container);
                modButtons.add(buttons.addChild(Button.builder(Component.literal(name), b -> openMod(entry.getKey())).width(250).build()));
            }
            layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(200).build());
            layout.visitWidgets(this::addRenderableWidget);
            repositionElements();
        }

        @Override
        protected void repositionElements()
        {
            layout.arrangeElements();
        }

        /**
         * @return the ids of the mods listed, in button order.
         */
        public List<String> modIds()
        {
            return List.copyOf(factories.keySet());
        }

        /**
         * Opens the config screen of one of the listed mods.
         */
        public void openMod(final String modId)
        {
            final Function<Screen, Screen> factory = factories.get(modId);
            if (factory != null)
            {
                Minecraft.getInstance().gui.setScreen(factory.apply(this));
            }
        }

        @Override
        public void onClose()
        {
            Minecraft.getInstance().gui.setScreen(parent);
        }
    }
}
