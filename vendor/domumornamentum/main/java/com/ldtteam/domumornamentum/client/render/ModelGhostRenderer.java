package com.ldtteam.domumornamentum.client.render;

import com.ldtteam.domumornamentum.util.ItemStackUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.ldtteam.shim.neoforge.model.data.ModelData;
import com.ldtteam.shim.neoforge.model.data.ModelDataHelper;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Renders a translucent preview ("ghost") of the materially textured block the player is about to place.
 *
 * <p>26.3/Fabric: geometry is submitted as custom geometry to the level's {@link SubmitNodeCollector}; the block model
 * is tessellated with Fabric's {@code AltModelBlockRenderer} so that dynamic (retextured) models receive the block
 * entity's model data through {@code getBlockEntityRenderData}.
 */
public class ModelGhostRenderer
{
    private static final ModelGhostRenderer INSTANCE = new ModelGhostRenderer();

    private static final float[] DIRECTIONAL_BRIGHTNESS = {0.5f, 1f, 0.7f, 0.7f, 0.6f, 0.6f};

    public static ModelGhostRenderer getInstance()
    {
        return INSTANCE;
    }

    private ModelGhostRenderer()
    {
    }

    public void renderGhost(
            final PoseStack poseStack,
            final SubmitNodeCollector collector,
            final ItemStack renderStack,
            final Vec3 targetedRenderPos,
            final BlockHitResult blockHitResult,
            final ClientLevel level,
            final boolean ignoreDepth)
    {
        if (!(renderStack.getItem() instanceof BlockItem blockItem))
        {
            // Non-BlockItem rendering is not supported
            return;
        }

        final BlockPlaceContext context = new BlockPlaceContext(
                Objects.requireNonNull(Minecraft.getInstance().player),
                Objects.requireNonNull(ItemStackUtils.getHandWithMateriallyTexturedItemStackFromPlayer(Minecraft.getInstance().player)),
                renderStack,
                blockHitResult
        );

        BlockState placementState = blockItem.getBlock().getStateForPlacement(context);
        if (placementState == null)
        {
            return;
        }

        placementState = renderStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(placementState);

        ModelData modelData = ModelData.EMPTY;
        if (blockItem.getBlock() instanceof EntityBlock entityBlock)
        {
            final BlockEntity blockEntity = entityBlock.newBlockEntity(context.getClickedPos(), placementState);
            if (blockEntity != null)
            {
                blockEntity.applyComponentsFromItemStack(renderStack);
                modelData = ModelDataHelper.getModelData(blockEntity);
            }
        }

        poseStack.pushPose();

        // Offset/scale by an unnoticeable amount to prevent z-fighting
        final Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        poseStack.translate(
                targetedRenderPos.x - camera.x - 0.000125,
                targetedRenderPos.y - camera.y + 0.000125,
                targetedRenderPos.z - camera.z - 0.000125
        );
        poseStack.scale(1.001F, 1.001F, 1.001F);

        final Vector4f color = new Vector4f(0, 0, 1, 0.5f);

        renderGhostForBlock(placementState, context.getClickedPos(), poseStack, collector, modelData, color, false, ignoreDepth, level);

        poseStack.popPose();
    }

    private void renderGhostForBlock(
            final BlockState state,
            final BlockPos pos,
            final PoseStack poseStack,
            final SubmitNodeCollector collector,
            final ModelData modelData,
            final Vector4f color,
            final boolean renderColoredGhost,
            final boolean ignoreDepth,
            final ClientLevel level)
    {
        if (renderColoredGhost)
        {
            final RenderType renderType = ignoreDepth
                    ? ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW_ALWAYS.get()
                    : ModRenderTypes.GHOST_BLOCK_COLORED_PREVIEW.get();
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    tesselate(state, pos, modelData, level, quad -> putColoredQuad(buffer, pose, quad, color)));
        }
        else
        {
            final RenderType renderType = ignoreDepth
                    ? ModRenderTypes.GHOST_BLOCK_PREVIEW_GREATER.get()
                    : ModRenderTypes.GHOST_BLOCK_PREVIEW.get();
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) ->
                    tesselate(state, pos, modelData, level, quad -> quad.buffer(OverlayTexture.NO_OVERLAY, pose, buffer)));
        }
    }

    /**
     * Tessellates the block model (lit and tinted like in the level) and hands every quad to the sink.
     */
    private static void tesselate(
            final BlockState state,
            final BlockPos pos,
            final ModelData modelData,
            final @Nullable ClientLevel level,
            final Consumer<MutableQuadView> sink)
    {
        final GhostBlockGetter getter = new GhostBlockGetter(modelData);
        getter.blockPos = pos;
        getter.randomSeedPos = pos;
        getter.blockState = state;
        if (level != null)
        {
            getter.biome = level.getBiome(pos);
            getter.cardinalLighting = level.cardinalLighting();
            getter.lightEngine = level.getLightEngine();
        }

        final Minecraft mc = Minecraft.getInstance();
        final BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        final Renderer renderer = Renderer.get();
        final QuadEmitter emitter = renderer.quadEmitter(sink);
        renderer.altModelBlockRenderer(mc.options.ambientOcclusion().get(), false, mc.getBlockColors())
                .tesselateBlock(emitter, 0, 0, 0, getter, pos, state, model, 42L);
    }

    private static void putColoredQuad(final VertexConsumer buffer, final PoseStack.Pose pose, final MutableQuadView quad, final Vector4f color)
    {
        final Direction face = quad.lightFace();
        final float brightness = DIRECTIONAL_BRIGHTNESS[face.get3DDataValue()];
        final Vector3f normal = pose.transformNormal(face.getStepX(), face.getStepY(), face.getStepZ(), new Vector3f());
        final Vector4f posVec = new Vector4f();

        for (int v = 0; v < 4; ++v)
        {
            posVec.set(quad.x(v), quad.y(v), quad.z(v), 1f);
            posVec.mul(pose.pose());

            buffer.addVertex(posVec.x(), posVec.y(), posVec.z())
                    .setColor(color.x() * brightness, color.y() * brightness, color.z() * brightness, 1f)
                    .setNormal(normal.x(), normal.y(), normal.z());
        }
    }

    /**
     * Single block getter that also exposes the ghost block entity's model data as Fabric render data.
     */
    private static final class GhostBlockGetter extends MovingBlockRenderState
    {
        private final ModelData modelData;

        private GhostBlockGetter(final ModelData modelData)
        {
            this.modelData = modelData;
        }

        @Override
        public @Nullable Object getBlockEntityRenderData(final BlockPos pos)
        {
            return pos.equals(blockPos) ? modelData : null;
        }
    }
}
