package com.ldtteam.blockui.fabric.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * BlockUI block-state PiP renderer reuses the item GUI transform (NeoForge access transformer replacement).
 */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor
{
    @Invoker("firstLayer")
    ItemStackRenderState.LayerRenderState blockui$firstLayer();
}
