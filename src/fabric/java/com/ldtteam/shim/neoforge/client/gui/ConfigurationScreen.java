package com.ldtteam.shim.neoforge.client.gui;

import com.ldtteam.shim.fml.ModContainer;
import com.ldtteam.shim.fml.config.ConfigTracker;
import com.ldtteam.shim.fml.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Replacement of NeoForge's generated configuration screen, built from vanilla widgets: one button per config file of the mod (client,
 * common, server), each opening a {@link ConfigurationSectionScreen} that edits the values and saves them to the same file the shim
 * loads them from. Server configs can only be changed in a singleplayer world (as on NeoForge); otherwise they are shown read-only.
 */
public class ConfigurationScreen extends Screen
{
    private static final List<ModConfig.Type> ORDER = List.of(ModConfig.Type.CLIENT, ModConfig.Type.COMMON, ModConfig.Type.SERVER, ModConfig.Type.STARTUP);

    @Nullable
    private final Screen parent;
    private final ModContainer mod;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public ConfigurationScreen(final ModContainer mod, @Nullable final Screen parent)
    {
        super(Component.translatableWithFallback("minecolonies_shim.configuration.title", "%s configuration", displayName(mod)));
        this.mod = mod;
        this.parent = parent;
    }

    /**
     * @return a readable mod name: the Fabric mod name, or the capitalized id of a mod bundled in another mod's jar.
     */
    public static String displayName(final ModContainer mod)
    {
        final String name = mod.getModInfo().getDisplayName();
        if (name == null || name.isBlank() || name.equals(mod.getModId()))
        {
            final String id = mod.getModId().replace('_', ' ');
            return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
        }
        return name;
    }

    /**
     * @return the mod's config files that have values, in the order client, common, server, startup.
     */
    public List<ModConfig> configs()
    {
        final List<ModConfig> configs = new ArrayList<>();
        for (final ModConfig config : ConfigTracker.INSTANCE.configsForMod(mod.getModId()))
        {
            if (!config.getSpec().isEmpty())
            {
                configs.add(config);
            }
        }
        configs.sort(Comparator.comparingInt(c -> ORDER.indexOf(c.getType())));
        return configs;
    }

    @Override
    protected void init()
    {
        layout.addTitleHeader(title, font);
        final LinearLayout buttons = layout.addToContents(LinearLayout.vertical().spacing(8));
        final List<ModConfig> configs = configs();
        if (configs.isEmpty())
        {
            buttons.addChild(Button.builder(Component.translatableWithFallback("minecolonies_shim.configuration.none", "This mod has no settings"), b -> {})
                .width(250).build()).active = false;
        }
        for (final ModConfig config : configs)
        {
            final ConfigurationSectionScreen.Access access = ConfigurationSectionScreen.access(minecraft, config);
            Component label = typeName(config.getType());
            if (access != ConfigurationSectionScreen.Access.EDITABLE)
            {
                label = Component.translatableWithFallback("minecolonies_shim.configuration.readonly_suffix", "%s (read-only)", label);
            }
            final Component tooltip = Component.literal(config.getFileName()).withStyle(ChatFormatting.YELLOW)
                .append("\n")
                .append(access.description().copy().withStyle(ChatFormatting.GRAY));
            buttons.addChild(Button.builder(label, b -> openSection(config.getType())).width(250).tooltip(Tooltip.create(tooltip)).build());
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
     * Opens the section screen of the mod's config of this type (does nothing if the mod has none).
     */
    public void openSection(final ModConfig.Type type)
    {
        for (final ModConfig config : configs())
        {
            if (config.getType() == type)
            {
                minecraft.gui.setScreen(new ConfigurationSectionScreen(this, config, displayName(mod)));
                return;
            }
        }
    }

    /**
     * @return the translated name of a config type ("Client settings", ...).
     */
    public static Component typeName(final ModConfig.Type type)
    {
        return switch (type)
        {
            case CLIENT -> Component.translatableWithFallback("minecolonies_shim.configuration.type.client", "Client settings");
            case COMMON -> Component.translatableWithFallback("minecolonies_shim.configuration.type.common", "Common settings");
            case SERVER -> Component.translatableWithFallback("minecolonies_shim.configuration.type.server", "Server settings");
            case STARTUP -> Component.translatableWithFallback("minecolonies_shim.configuration.type.startup", "Startup settings");
        };
    }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
