package com.minecolonies.fabric.mixin.client.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces NeoForge's AbstractContainerScreen#getGuiLeft/getGuiTop (used by the JEI integration).
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor
{
    @Accessor("leftPos")
    int minecolonies$getLeftPos();

    @Accessor("topPos")
    int minecolonies$getTopPos();
}
