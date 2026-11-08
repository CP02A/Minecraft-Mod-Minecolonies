package com.minecolonies.core.client.render;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.core.client.render.worldevent.WorldEventContext;
import com.minecolonies.core.tileentities.TileEntityColonySign;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;

import java.util.List;

import static com.minecolonies.api.util.constant.TranslationConstants.NEXT;
import static com.minecolonies.api.util.constant.TranslationConstants.PREVIOUS;
import static com.minecolonies.core.blocks.BlockColonySign.CONNECTED;

public class TileEntityColonySignRenderer implements BlockEntityRenderer<TileEntityColonySign, TileEntityColonySignRenderer.State>
{
    /**
     * Display context for the sign model.
     */
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    /**
     * One line of text on the sign.
     *
     * @param text the text.
     * @param y    the y position.
     */
    public record TextLine(FormattedCharSequence text, float x, float y) {}

    /**
     * Sign render state.
     */
    public static class State extends BlockEntityRenderState
    {
        public boolean                     render;
        public float                       rotation;
        public final BlockModelRenderState model = new BlockModelRenderState();
        public final List<TextLine>        lines = new ArrayList<>();
    }

    /**
     * Block model resolver.
     */
    private final BlockModelResolver blockModelResolver;

    /**
     * The font.
     */
    private final Font font;

    public TileEntityColonySignRenderer(final BlockEntityRendererProvider.Context context)
    {
        super();
        this.blockModelResolver = context.blockModelResolver();
        this.font = context.font();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      @NotNull final TileEntityColonySign tileEntity,
      @NotNull final State renderState,
      final float partialTicks,
      @NotNull final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(tileEntity, renderState, partialTicks, cameraPosition, breakProgress);
        renderState.lines.clear();
        final BlockState state = tileEntity.getBlockState();
        renderState.render = state.getBlock() == ModBlocks.blockColonySign;
        if (!renderState.render)
        {
            return;
        }

        renderState.rotation = tileEntity.getRelativeRotation();
        final boolean connected = tileEntity.getTargetColonyId() != tileEntity.getCachedSignAboveColony();
        this.blockModelResolver.update(renderState.model, ModBlocks.blockColonySign.defaultBlockState().setValue(CONNECTED, connected), DISPLAY_CONTEXT);

        final String colonyName = tileEntity.getColonyName();
        final int distance = tileEntity.getColonyDistance();
        if (colonyName.isEmpty())
        {
            addText(renderState.lines, "Unknown Colony", 0, 0);
            addText(renderState.lines, Component.translatable("com.minecolonies.coremod.dist.blocks", distance).getString(), 3, 0);
        }
        else
        {
            final String targetColonyName = tileEntity.getTargetColonyName();
            if (!targetColonyName.isEmpty() && connected)
            {
                final int targetColonyDistance = tileEntity.getTargetColonyDistance();
                addColonyNameOnSign(renderState.lines, colonyName, distance, -10);
                addColonyNameOnSign(renderState.lines, targetColonyName, targetColonyDistance, -60);
            }
            else
            {
                addColonyNameOnSign(renderState.lines, colonyName, distance, -35);
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

        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);
        matrixStack.rotate(Axis.YP.rotationDegrees(state.rotation));
        matrixStack.translate(-0.5, -0.5, -0.5);
        state.model.submit(matrixStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        matrixStack.popPose();

        renderTextOnSide(matrixStack, submitNodeCollector, state, true);
        renderTextOnSide(matrixStack, submitNodeCollector, state, false);
    }

    /**
     * Render the text on the sign and handle rotation etc.
     *
     * @param matrixStack         the matrix stack.
     * @param submitNodeCollector the collector.
     * @param state               the render state.
     * @param mirrored            if mirrored or not.
     */
    private void renderTextOnSide(final PoseStack matrixStack, final SubmitNodeCollector submitNodeCollector, final State state, final boolean mirrored)
    {
        matrixStack.pushPose();
        matrixStack.translate(0.5f, 0.5F, 0.5f);
        matrixStack.rotate(Axis.YP.rotationDegrees(state.rotation));
        if (mirrored)
        {
            matrixStack.rotate(Axis.YP.rotationDegrees(180));
        }
        matrixStack.translate(-0.0f, -0.1F, 0.2f);

        matrixStack.scale(0.007F, -0.007F, 0.007F);

        for (final TextLine line : state.lines)
        {
            submitNodeCollector.submitText(matrixStack, line.x(), line.y(), line.text(), false, Font.DisplayMode.NORMAL, state.lightCoords, 0xdcdcdc00, 0, 0);
        }
        matrixStack.popPose();
    }

    /**
     * Add the name and distance on the sign at offset.
     *
     * @param lines      the lines to add to.
     * @param colonyName the name.
     * @param distance   the distance to the colony.
     * @param offset     the offset to render it at.
     */
    private void addColonyNameOnSign(final List<TextLine> lines, final String colonyName, final int distance, final int offset)
    {
        final int textWidth = font.width(colonyName);
        if (textWidth > 90)
        {
            final List<FormattedText> splitName = font.getSplitter().splitLines(colonyName, 90, Style.EMPTY);
            for (int i = 0; i < Math.min(2, splitName.size()); i++)
            {
                addText(lines, splitName.get(i).getString(), i, offset);
            }
            addText(lines, Component.translatable("com.minecolonies.coremod.dist.blocks", distance).getString(), 3, offset);
        }
        else
        {
            addText(lines, colonyName, 0, offset);
            addText(lines, Component.translatable("com.minecolonies.coremod.dist.blocks", distance).getString(), 3, offset);
        }
    }

    /**
     * Text utility.
     *
     * @param lines  the lines to add to.
     * @param text   the text to render.
     * @param line   the line of the text.
     * @param offset additional offset.
     */
    private void addText(final List<TextLine> lines, String text, final int line, final float offset)
    {
        final int maxSize = 20;
        if (text.length() > maxSize)
        {
            text = text.substring(0, maxSize);
        }

        final FormattedCharSequence iReorderingProcessor = FormattedCharSequence.forward(text, Style.EMPTY);
        float x = (float) (-font.width(iReorderingProcessor) / 2); //render width of text divided by 2
        lines.add(new TextLine(iReorderingProcessor, x, line * 8f + offset));
    }

    @Override
    public boolean shouldRenderOffScreen()
    {
        return false;
    }

    public static void renderSignHover(final WorldEventContext context)
    {
        final HitResult rayTraceResult = Minecraft.getInstance().hitResult;
        if (!(rayTraceResult instanceof final BlockHitResult blockRayTraceResult) || blockRayTraceResult.getType() == HitResult.Type.MISS)
            return;

        final BlockPos posAtCamera = blockRayTraceResult.getBlockPos();
        if (context.clientLevel.getBlockState(posAtCamera).getBlock() != ModBlocks.blockColonySign)
        {
            return;
        }

        if (context.clientLevel.getBlockEntity(posAtCamera) instanceof TileEntityColonySign tileEntityColonySign)
        {
            if (!BlockPos.ZERO.equals(tileEntityColonySign.getPreviousPos()))
            {
                renderTextBoxAtPos(context, tileEntityColonySign.getPreviousPos(), List.of(Component.translatable(PREVIOUS).getString()));
            }
            if (!BlockPos.ZERO.equals(tileEntityColonySign.getNextPosition()))
            {
                renderTextBoxAtPos(context, tileEntityColonySign.getNextPosition(), List.of(Component.translatable(NEXT).getString()));
            }
        }
    }

    private static void renderTextBoxAtPos(final WorldEventContext context, final BlockPos pos, final List<String> text)
    {
        context.pushPoseCameraToPos(pos);
        context.renderLineBoxWithShadow(BlockPos.ZERO, 0xffffffff, WorldEventContext.DEFAULT_LINE_WIDTH * 2);
        context.popPose();
        renderDebugText(pos, text, context, true, 3, 2.5f);
    }

    public static void renderDebugText(final BlockPos renderPos,
        final List<String> text,
        final WorldEventContext context,
        final boolean forceWhite,
        final int mergeEveryXListElements,
        final float scale)
    {
        if (mergeEveryXListElements < 1)
        {
            throw new IllegalArgumentException("mergeEveryXListElements is less than 1");
        }

        final PoseStack matrixStack = context.poseStack;
        final Vec3 cameraPosition = context.cameraPosition;
        final int cap = text.size();
        if (cap > 0)
        {
            final Font fontrenderer = Minecraft.getInstance().font;

            matrixStack.pushPose();
            matrixStack.translate(renderPos.getX() + 0.5d - cameraPosition.x(), renderPos.getY() + 0.6d - cameraPosition.y(), renderPos.getZ() + 0.5d - cameraPosition.z());
            matrixStack.rotate(context.cameraRenderState.orientation);
            matrixStack.scale(0.014f, -0.014f, 0.014f);
            matrixStack.scale(scale, scale, scale);

            final float backgroundTextOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            final int alphaMask = (int) (backgroundTextOpacity * 255.0F) << 24;

            for (int i = 0; i < cap; i += mergeEveryXListElements)
            {
                final MutableComponent renderText = Component.literal(
                    mergeEveryXListElements == 1 ? text.get(i) : text.subList(i, Math.min(i + mergeEveryXListElements, cap)).toString());
                final float textCenterShift = (float) (-fontrenderer.width(renderText) / 2);

                context.submitNodeCollector.submitText(matrixStack,
                    textCenterShift,
                    0,
                    renderText.getVisualOrderText(),
                    false,
                    Font.DisplayMode.SEE_THROUGH,
                    0x00f000f0,
                    forceWhite ? 0xffffffff : 0x20ffffff,
                    alphaMask,
                    0);
                if (!forceWhite)
                {
                    context.submitNodeCollector.submitText(matrixStack, textCenterShift, 0, renderText.getVisualOrderText(), false, Font.DisplayMode.NORMAL, 0x00f000f0, 0xffffffff, 0, 0);
                }
                matrixStack.translate(0.0d, fontrenderer.lineHeight + 1, 0.0d);
            }

            matrixStack.popPose();
        }
    }
}
