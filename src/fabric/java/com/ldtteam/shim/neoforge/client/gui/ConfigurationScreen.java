package com.ldtteam.shim.neoforge.client.gui;

import com.ldtteam.shim.fml.ModContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal replacement of NeoForge's generated configuration screen: tells the user where the config files are.
 * FABRIC-PORT TODO: in-game config editing is not implemented; edit the files in the config folder instead.
 */
public class ConfigurationScreen extends Screen
{
    @Nullable
    private final Screen parent;
    private final ModContainer mod;

    public ConfigurationScreen(final ModContainer mod, @Nullable final Screen parent)
    {
        super(Component.literal(mod.getModId() + " configuration"));
        this.mod = mod;
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width / 2 - 100, height - 40, 200, 20).build());
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick)
    {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Edit the configuration files of '" + mod.getModId() + "' in the 'config' folder."), width / 2, height / 2 - 10, 0xFFFFFFFF);
        graphics.centeredText(font, Component.literal("Server configs are stored per world in 'serverconfig'."), width / 2, height / 2 + 4, 0xFFAAAAAA);
    }

    @Override
    public void onClose()
    {
        minecraft.gui.setScreen(parent);
    }
}
