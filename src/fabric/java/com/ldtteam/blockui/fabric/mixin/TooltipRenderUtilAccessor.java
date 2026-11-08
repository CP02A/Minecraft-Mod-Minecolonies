package com.ldtteam.blockui.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla tooltip background sprites (NeoForge access transformer replacement).
 */
@Mixin(TooltipRenderUtil.class)
public interface TooltipRenderUtilAccessor
{
    @Invoker("getBackgroundSprite")
    static Identifier blockui$getBackgroundSprite(@Nullable Identifier style)
    {
        throw new AssertionError();
    }

    @Invoker("getFrameSprite")
    static Identifier blockui$getFrameSprite(@Nullable Identifier style)
    {
        throw new AssertionError();
    }
}
