package com.minecolonies.core.client.render;

import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for a normal tile entity (Nothing special with rendering).
 */
public class EmptyTileEntitySpecialRenderer implements BlockEntityRenderer<AbstractTileEntityColonyBuilding, BlockEntityRenderState>
{

    public EmptyTileEntitySpecialRenderer(BlockEntityRendererProvider.Context context)
    {
        super();
    }

    @Override
    public BlockEntityRenderState createRenderState()
    {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(
      @NotNull final BlockEntityRenderState state,
      @NotNull final PoseStack matrixStack,
      @NotNull final SubmitNodeCollector submitNodeCollector,
      @NotNull final CameraRenderState camera)
    {

    }
}
