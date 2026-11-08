package com.minecolonies.core.client.render;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.blocks.huts.AbstractBlockMinecoloniesDefault;
import com.minecolonies.core.tileentities.TileEntityNamedGrave;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TileEntityNamedGraveRenderer implements BlockEntityRenderer<TileEntityNamedGrave, TileEntityNamedGraveRenderer.State>
{
    /**
     * Basic rotation to achieve a certain direction.
     */
    private static final int BASIC_ROTATION = 90;

    /**
     * Rotate by amount to go east.
     */
    private static final int ROTATE_EAST = 1;

    /**
     * Rotate by amount to go north.
     */
    private static final int ROTATE_NORTH = 2;

    /**
     * Rotate by amount to go west.
     */
    private static final int ROTATE_WEST = 3;

    /**
     * Grave render state.
     */
    public static class State extends BlockEntityRenderState
    {
        @Nullable
        public Direction    facing;
        public final List<String> lines = new ArrayList<>();
    }

    /**
     * The font.
     */
    private final Font font;

    public TileEntityNamedGraveRenderer(final BlockEntityRendererProvider.Context context)
    {
        super();
        this.font = context.font();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      @NotNull final TileEntityNamedGrave tileEntity,
      @NotNull final State state,
      final float partialTicks,
      @NotNull final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(tileEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = null;
        state.lines.clear();
        if (tileEntity.getLevel() == null)
        {
            return;
        }
        final BlockState blockState = tileEntity.getLevel().getBlockState(tileEntity.getBlockPos());
        if (blockState.getBlock() == ModBlocks.blockNamedGrave)
        {
            state.facing = blockState.getValue(AbstractBlockMinecoloniesDefault.FACING);
            if (tileEntity.getTextLines().isEmpty())
            {
                state.lines.add("Unknown Citizen");
            }
            else
            {
                state.lines.addAll(tileEntity.getTextLines());
            }
        }
    }

    @Override
    public void submit(@NotNull final State state, final PoseStack matrixStack, @NotNull final SubmitNodeCollector submitNodeCollector, @NotNull final CameraRenderState camera)
    {
        if (state.facing == null)
        {
            return;
        }

        matrixStack.pushPose();

        switch (state.facing)
        {
            case NORTH:
                matrixStack.translate(0.5f, 1.18F, 0.48F); //in front of the center point of the name plate
                matrixStack.scale(0.006F, -0.006F, 0.006F); //size of the text font
                matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_NORTH));
                break;
            case SOUTH:
                matrixStack.translate(0.5f, 1.18F, 0.54F); //in front of the center point of the name plate
                matrixStack.scale(0.006F, -0.006F, 0.006F); //size of the text font
                //don't rotate at all.
                break;
            case EAST:
                matrixStack.translate(0.54f, 1.18F, 0.5F); //in front of the center point of the name plate
                matrixStack.scale(0.006F, -0.006F, 0.006F); //size of the text font
                matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_EAST));
                break;
            case WEST:
                matrixStack.translate(0.48f, 1.18F, 0.5F); //in front of the center point of the name plate
                matrixStack.scale(0.006F, -0.006F, 0.006F); //size of the text font
                matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_WEST));
                break;
            default:
                break;
        }

        for (int i = 0; i < state.lines.size(); i++)
        {
            renderText(matrixStack, submitNodeCollector, state.lightCoords, state.lines.get(i), i);
        }

        // restore the original transformation matrix + normals matrix
        matrixStack.popPose();
    }

    private void renderText(final PoseStack matrixStack, final SubmitNodeCollector submitNodeCollector, final int combinedLight, String text, final int line)
    {
        final int maxSize = 20;
        if (text.length() > maxSize)
        {
            text = text.substring(0, maxSize);
        }

        final FormattedCharSequence iReorderingProcessor = FormattedCharSequence.forward(text, Style.EMPTY);
        float x = (float) (-font.width(iReorderingProcessor) / 2); //render width of text divided by 2
        submitNodeCollector.submitText(matrixStack, x, line * 10f, iReorderingProcessor, false, Font.DisplayMode.NORMAL, combinedLight, 0xdcdcdc00, 0, 0);
    }

    // this should be true for tileentities which render globally (no render bounding box), such as beacons.
    @Override
    public boolean shouldRenderOffScreen()
    {
        return false;
    }
}
