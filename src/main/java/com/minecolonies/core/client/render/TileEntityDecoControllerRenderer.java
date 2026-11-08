package com.minecolonies.core.client.render;

import com.minecolonies.core.blocks.BlockDecorationController;
import com.minecolonies.core.tileentities.TileEntityDecorationController;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityDecoControllerRenderer implements BlockEntityRenderer<BlockEntity, TileEntityDecoControllerRenderer.State>
{
    /**
     * Display context for the rendered block.
     */
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    /**
     * Render state.
     */
    public static class State extends BlockEntityRenderState
    {
        public boolean render;
        public Vec3    translate = Vec3.ZERO;
        public final BlockModelRenderState model = new BlockModelRenderState();
    }

    private final BlockModelResolver blockModelResolver;

    public TileEntityDecoControllerRenderer(BlockEntityRendererProvider.Context context)
    {
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      @NotNull final BlockEntity blockEntity,
      @NotNull final State renderState,
      final float partialTicks,
      @NotNull final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTicks, cameraPosition, breakProgress);
        renderState.render = false;
        if (blockEntity instanceof TileEntityDecorationController decorationController)
        {
            Level level = blockEntity.getLevel();
            if (level != null)
            {
                final BlockState decoController = decorationController.getBlockState();
                final Direction direction = decoController.getValue(BlockDecorationController.FACING);
                final BlockPos offsetPos = blockEntity.getBlockPos().relative(direction);
                final BlockState state = level.getBlockState(offsetPos);
                final VoxelShape shape = state.getShape(level, offsetPos);
                if (shape.isEmpty() || Block.isShapeFullBlock(shape))
                {
                    renderState.render = true;
                    renderState.translate = Vec3.ZERO;
                    this.blockModelResolver.update(renderState.model, decoController, DISPLAY_CONTEXT);
                    return;
                }

                renderState.translate = switch (direction)
                {
                    case UP -> new Vec3(0, shape.min(Direction.Axis.Y), 0);
                    case DOWN -> new Vec3(0, shape.max(Direction.Axis.Y) - 1, 0);
                    case NORTH -> new Vec3(0, 0, shape.max(Direction.Axis.Z) - 1);
                    case SOUTH -> new Vec3(0, 0, shape.min(Direction.Axis.Z));
                    case EAST -> new Vec3(shape.min(Direction.Axis.X), 0, 0);
                    case WEST -> new Vec3(shape.max(Direction.Axis.X) - 1, 0, 0);
                };

                if (!decoController.isAir())
                {
                    renderState.render = true;
                    this.blockModelResolver.update(renderState.model, decoController, DISPLAY_CONTEXT);
                }
            }
        }
    }

    @Override
    public void submit(@NotNull final State state, @NotNull final PoseStack matrixStack, @NotNull final SubmitNodeCollector submitNodeCollector, @NotNull final CameraRenderState camera)
    {
        if (!state.render)
        {
            return;
        }
        // FABRIC-PORT: rendered as a single block model (no smooth lighting/world tesselation as in 1.21.1).
        matrixStack.pushPose();
        matrixStack.translate(state.translate.x, state.translate.y, state.translate.z);
        state.model.submit(matrixStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrixStack.popPose();
    }
}
