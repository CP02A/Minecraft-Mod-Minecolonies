package com.ldtteam.blockui.fabric.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * BlockUI block-state PiP renderer reuses the item GUI transform (NeoForge access transformer replacement).
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor
{
    @Accessor("itemTransform")
    ItemTransform blockui$itemTransform();

    @Accessor("localTransform")
    Matrix4f blockui$localTransform();
}
