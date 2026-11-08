package com.ldtteam.structurize.client;

import com.ldtteam.common.fakelevel.SingleBlockFakeLevel;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.component.CapturedBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Tag anchor renderer; renders replacement block model inside of anchor "overlay" model.
 * <p>
 * FABRIC-PORT: 26.x render state based. The block entity renderer renders the replacement inside the placed anchor block,
 * {@link ItemRenderer} is the special item model renderer (formerly BEWLR) used by {@code assets/structurize/items/blocktagsubstitution.json}.
 */
public class TagSubstitutionRenderer implements BlockEntityRenderer<BlockEntityTagSubstitution, TagSubstitutionRenderer.TagSubstitutionRenderState>
{
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private static TagSubstitutionRenderer INSTANCE;

    public static TagSubstitutionRenderer getInstance()
    {
        return INSTANCE;
    }

    private final BlockEntityRendererProvider.Context context;

    public TagSubstitutionRenderer(@NotNull final BlockEntityRendererProvider.Context context)
    {
        INSTANCE = this;
        this.context = context;
    }

    @Override
    public TagSubstitutionRenderState createRenderState()
    {
        return new TagSubstitutionRenderState();
    }

    @Override
    public void extractRenderState(final BlockEntityTagSubstitution blockEntity,
        final TagSubstitutionRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        extractReplacement(blockEntity.getReplacement(), blockEntity.getLevel(), partialTicks, state.replacement, context.blockModelResolver(), context.blockEntityRenderDispatcher());
    }

    @Override
    public void submit(final TagSubstitutionRenderState state,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final CameraRenderState camera)
    {
        state.replacement.submit(poseStack, submitNodeCollector, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0, camera, context.blockEntityRenderDispatcher());
    }

    /**
     * Fills the replacement render state.
     */
    private static void extractReplacement(final CapturedBlock replacement,
        @Nullable final Level realLevel,
        final float partialTicks,
        final ReplacementRenderState out,
        final BlockModelResolver blockModelResolver,
        final BlockEntityRenderDispatcher beDispatcher)
    {
        out.clear();
        if (replacement.blockState().isAir())
        {
            return;
        }

        out.empty = false;
        blockModelResolver.update(out.blockModel, replacement.blockState(), BLOCK_DISPLAY_CONTEXT);

        if (replacement.hasBlockEntity() && realLevel != null && replacement.serializedBE().isPresent())
        {
            final BlockEntity blockEntity = BlockEntity.loadStatic(BlockPos.ZERO, replacement.blockState(), replacement.serializedBE().get(), realLevel.registryAccess());
            if (blockEntity == null)
            {
                return;
            }
            final BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = beDispatcher.getRenderer(blockEntity);
            if (renderer == null)
            {
                return;
            }

            final SingleBlockFakeLevel renderLevel = getRenderLevel(realLevel);
            renderLevel.withFakeLevelContext(replacement.blockState(), blockEntity, realLevel, fakeLevel -> {
                final BlockEntityRenderState beState = renderer.createRenderState();
                renderer.extractRenderState(blockEntity, beState, partialTicks, Vec3.ZERO, null);
                out.blockEntity = beState;
            });
        }
    }

    private static SingleBlockFakeLevel renderLevel;

    private static SingleBlockFakeLevel getRenderLevel(final Level realLevel)
    {
        if (renderLevel == null)
        {
            renderLevel = new SingleBlockFakeLevel(realLevel);
        }
        return renderLevel;
    }

    /**
     * Render state of captured replacement block
     */
    public static class ReplacementRenderState
    {
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
        @Nullable
        public BlockEntityRenderState blockEntity = null;
        public boolean empty = true;

        public void clear()
        {
            blockModel.clear();
            blockEntity = null;
            empty = true;
        }

        public void submit(final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int lightCoords,
            final int overlayCoords,
            final int outlineColor,
            final CameraRenderState camera,
            final BlockEntityRenderDispatcher beDispatcher)
        {
            if (empty)
            {
                return;
            }

            poseStack.pushPose();
            poseStack.scale(0.995f, 0.995f, 0.995f);
            poseStack.translate(0.0025f, 0.0025f, 0.0025f);

            blockModel.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
            if (blockEntity != null)
            {
                blockEntity.lightCoords = lightCoords;
                beDispatcher.submit(blockEntity, poseStack, submitNodeCollector, camera);
            }

            poseStack.popPose();
        }
    }

    public static class TagSubstitutionRenderState extends BlockEntityRenderState
    {
        public final ReplacementRenderState replacement = new ReplacementRenderState();
    }

    /**
     * Item renderer: renders the replacement block inside the anchor item model (see item model definition).
     */
    public static class ItemRenderer implements SpecialModelRenderer<CapturedBlock>
    {
        @Override
        public void submit(@Nullable final CapturedBlock argument,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int lightCoords,
            final int overlayCoords,
            final boolean hasFoil,
            final int outlineColor)
        {
            if (argument == null || argument.blockState().isAir())
            {
                return;
            }

            final Minecraft mc = Minecraft.getInstance();
            final BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
            final ReplacementRenderState state = new ReplacementRenderState();
            extractReplacement(argument, mc.level, 0, state, new BlockModelResolver(mc.getModelManager()), beDispatcher);
            state.submit(poseStack,
                submitNodeCollector,
                lightCoords,
                overlayCoords,
                outlineColor,
                mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState,
                beDispatcher);
        }

        @Override
        public void getExtents(final Consumer<Vector3fc> output)
        {
            for (int i = 0; i < 8; i++)
            {
                output.accept(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
            }
        }

        @Nullable
        @Override
        public CapturedBlock extractArgument(final ItemStack stack)
        {
            return CapturedBlock.readFromItemStack(stack);
        }

        public record Unbaked() implements SpecialModelRenderer.Unbaked<CapturedBlock>
        {
            public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

            @Override
            public SpecialModelRenderer<CapturedBlock> bake(final SpecialModelRenderer.BakingContext context)
            {
                return new ItemRenderer();
            }

            @Override
            public MapCodec<Unbaked> type()
            {
                return MAP_CODEC;
            }
        }
    }
}
