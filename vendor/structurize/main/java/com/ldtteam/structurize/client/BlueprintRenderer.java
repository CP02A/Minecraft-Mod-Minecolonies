package com.ldtteam.structurize.client;

import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtils;
import com.ldtteam.structurize.client.fakelevel.BlueprintBlockAccess;
import com.ldtteam.structurize.component.CapturedBlock;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import com.ldtteam.structurize.tag.ModTags;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.WorldRenderMacros;
import com.ldtteam.structurize.util.WorldRenderMacros.RecordingVertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import com.ldtteam.shim.neoforge.model.data.ModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The renderer for blueprint.
 * Holds all information required to render a blueprint.
 * <p>
 * FABRIC-PORT: 26.x has no immediate mode rendering nor VertexBuffer. Block geometry is tesselated once into in-memory vertex
 * recordings (per chunk section layer) and submitted every frame as custom geometry; entities and block entities are extracted
 * into render states and submitted through their dispatchers.
 */
public class BlueprintRenderer implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintRenderer.class);

    /**
     * Render type used for all block layers when preview transparency is applied (alpha baked into vertex colors).
     */
    private static RenderType TRANSPARENT_BLOCKS = null;

    private static boolean hasWarnedExceptions = false;

    private final BlueprintBlockAccess blockAccess;
    List<Entity> entities = List.of();
    private List<BlockEntity> tileEntities;
    private Map<ChunkSectionLayer, RecordingVertexConsumer> vertexBuffers;
    private long lastGameTime;
    private boolean bypassMainFrustum = false;
    private Set<Object> crashingObjects = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Static factory utility method to handle the extraction of the values from the blueprint.
     *
     * @param blueprint The blueprint to create an instance for.
     * @return The renderer.
     */
    public static BlueprintRenderer buildRendererForBlueprint(final Blueprint blueprint)
    {
        final BlueprintBlockAccess blockAccess = new BlueprintBlockAccess(blueprint);
        return new BlueprintRenderer(blockAccess);
    }

    private BlueprintRenderer(final BlueprintBlockAccess blockAccess)
    {
        this.blockAccess = blockAccess;
    }

    private static RenderType getTransparentBlocksRenderType()
    {
        if (TRANSPARENT_BLOCKS == null)
        {
            final RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                .withLocation(Constants.resLocStruct("pipeline/transparent_preview_block"))
                .withShaderDefine("ALPHA_CUTOUT", 0.004F)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .build();
            final RenderSetup setup = RenderSetup.builder(pipeline)
                .useLightmap()
                .withTexture("Sampler0",
                    TextureAtlas.LOCATION_BLOCKS,
                    () -> RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true))
                .sortOnUpload()
                .setOitPipelines(RenderPipelines.OIT_TRANSLUCENT_BLOCK)
                .createRenderSetup();
            TRANSPARENT_BLOCKS = RenderType.create(Constants.MOD_ID + "_transparent_preview_block", setup);
        }
        return TRANSPARENT_BLOCKS;
    }

    private static RenderType getRenderType(final ChunkSectionLayer layer)
    {
        return switch (layer)
        {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    /**
     * Updates blueprint reference if it has same hash.
     *
     * @param previewData blueprint and context from active structure
     */
    public void updateBlueprint(final BlueprintPreviewData previewData)
    {
        if (blockAccess.getLevelSource() != previewData.getBlueprint() && blockAccess.getLevelSource().hashCode() == previewData.getBlueprint().hashCode())
        {
            blockAccess.setLevelSource(previewData.getBlueprint());
        }
    }

    private void init(final BlueprintPreviewData previewData, final Map<Object, Exception> suppressedExceptions)
    {
        final Minecraft mc = Minecraft.getInstance();
        final Blueprint blueprint = previewData.getBlueprint();

        final Map<BlockPos, ModelData> teModelData = new HashMap<>();
        final Map<BlockPos, BlockEntity> tileEntitiesMap = BlueprintUtils.instantiateTileEntities(blueprint, blockAccess, teModelData);
        entities = BlueprintUtils.instantiateEntities(blueprint, blockAccess);

        blockAccess.setBlockEntities(tileEntitiesMap);
        blockAccess.setEntities(entities);
        blockAccess.setSolidSubstitutionOverride(previewData.getSolidSubstitutionOverride());
        blockAccess.setRenderBlocksNiceOverride(previewData.getRenderBlocksNice());

        final Map<ChunkSectionLayer, RecordingVertexConsumer> chunkBuffers = new EnumMap<>(ChunkSectionLayer.class);
        for (final ChunkSectionLayer layer : ChunkSectionLayer.values())
        {
            chunkBuffers.put(layer, new RecordingVertexConsumer());
        }

        final ModelBlockRenderer blockRenderer = new ModelBlockRenderer(mc.options.ambientOcclusion().get(), true, mc.getBlockColors());
        final FluidRenderer fluidRenderer = new FluidRenderer(mc.getModelManager().getFluidStateModelSet());
        final boolean cutoutLeaves = mc.options.cutoutLeaves().get();
        final ChunkOffsetBufferBuilderWrapper fluidBufferWrapper = new ChunkOffsetBufferBuilderWrapper();

        // offset by a tiny amount to prevent z-fighting with real world
        final float zFightOffset = 0.001f;
        final BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> chunkBuffers.get(quad.materialInfo().layer())
            .putBlockBakedQuad(x + zFightOffset, y + zFightOffset, z + zFightOffset, quad, instance);
        final BlockQuadOutput opaqueQuadOutput = (x, y, z, quad, instance) -> chunkBuffers.get(ChunkSectionLayer.SOLID)
            .putBlockBakedQuad(x + zFightOffset, y + zFightOffset, z + zFightOffset, quad, instance);

        BlockModelLighter.enableCaching();
        for (final BlockInfo blockInfo : blueprint.getBlockInfoAsList())
        {
            final BlockPos blockPos = blockInfo.getPos();
            BlockState state = blockInfo.getState();
            // specially handle blockTagSub here cuz of block entity changes
            if (previewData.getRenderBlocksNice() && state.getBlock() == ModBlocks.blockTagSubstitution.get())
            {
                if (tileEntitiesMap.remove(blockPos) instanceof final BlockEntityTagSubstitution tagTE)
                {
                    final CapturedBlock replacement = tagTE.getReplacement();
                    state = replacement.blockState();

                    replacement.serializedBE().map(tag -> BlockEntity.loadStatic(blockPos, replacement.blockState(), tag, blueprint.getRegistryAccess())).ifPresent(newBe -> {
                        newBe.setLevel(blockAccess);
                        tileEntitiesMap.put(blockPos, newBe);
                    });
                }
                else
                {
                    state = Blocks.AIR.defaultBlockState();
                }
            }
            else
            {
                state = blockAccess.prepareBlockStateForRendering(state, blockPos);
            }

            final FluidState fluidState = state.getFluidState();
            try
            {
                if (!fluidState.isEmpty())
                {
                    final int chunkOffsetX = blockPos.getX() - (blockPos.getX() & 15),
                        chunkOffsetY = blockPos.getY() - (blockPos.getY() & 15),
                        chunkOffsetZ = blockPos.getZ() - (blockPos.getZ() & 15);

                    final BlockState fluidBlockState = state;
                    fluidRenderer.tesselate(blockAccess, blockPos, layer -> {
                        fluidBufferWrapper.setOffset(chunkBuffers.get(layer), chunkOffsetX, chunkOffsetY, chunkOffsetZ);
                        return fluidBufferWrapper;
                    }, fluidBlockState, fluidState);
                }

                if (state.getRenderShape() == RenderShape.MODEL)
                {
                    blockRenderer.tesselateBlock(ModelBlockRenderer.forceOpaque(cutoutLeaves, state) ? opaqueQuadOutput : quadOutput,
                        blockPos.getX(),
                        blockPos.getY(),
                        blockPos.getZ(),
                        blockAccess,
                        blockPos,
                        state,
                        mc.getModelManager().getBlockStateModelSet().get(state),
                        state.getSeed(blockPos));
                }
            }
            catch (final ReportedException e)
            {
                suppressedExceptions.put(blockInfo, e);
            }
            catch (final RuntimeException e)
            {
                suppressedExceptions.put(blockInfo, e);
            }
        }
        BlockModelLighter.clearCache();

        blockAccess.setSolidSubstitutionOverride(null);
        blockAccess.setRenderBlocksNiceOverride(Structurize.getConfig().getClient().renderPlaceholdersNice.get());

        clearVertexBuffers();
        vertexBuffers = new EnumMap<>(ChunkSectionLayer.class);
        chunkBuffers.forEach((layer, buffer) -> {
            if (!buffer.isEmpty())
            {
                vertexBuffers.put(layer, buffer);
            }
        });

        tileEntities = new ArrayList<>(tileEntitiesMap.values());
    }

    /**
     * Draws structure into world.
     */
    public void draw(final BlueprintPreviewData previewData, final BlockPos pos, final WorldRenderMacros ctx)
    {
        // we've crashed hard before, full skip
        if (crashingObjects == null)
        {
            return;
        }

        try
        {
            final Map<Object, Exception> suppressedExceptions = drawUnsafe(previewData, pos, ctx);
            if (!suppressedExceptions.isEmpty())
            {
                if (!hasWarnedExceptions)
                {
                    hasWarnedExceptions = true;
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("structurize.preview_renderer.exception"));
                }

                boolean crashReported = false;
                boolean isEmpty = true;
                for (final Map.Entry<Object, Exception> e : suppressedExceptions.entrySet())
                {
                    if (!crashingObjects.add(e.getKey()))
                    {
                        continue;
                    }
                    isEmpty = false;

                    if (e.getValue() instanceof final ReportedException reportedException)
                    {
                        printCrashReport(reportedException.getReport(), previewData);
                        crashReported = true;
                    }
                    else
                    {
                        LOGGER.error("", e.getValue());
                    }
                }

                if (!crashReported && !isEmpty)
                {
                    printCrashReport(CrashReport.forThrowable(new Exception(), "Small exception, rendering partially"), previewData);
                }
            }
        }
        catch (final Exception e)
        {
            printCrashReport(CrashReport.forThrowable(e, "Fatal exception, cannot render"), previewData);

            crashingObjects = null;
            Minecraft.getInstance().player.sendSystemMessage(
                Component.translatable("structurize.preview_renderer.cannot_render", previewData.getBlueprint().getName()));
        }
    }

    private static void printCrashReport(final CrashReport report, final BlueprintPreviewData previewData)
    {
        previewData.getBlueprint().describeSelfInCrashReport(report.addCategory("Blueprint"));
        LOGGER.error(report.getFriendlyReport(new ReportType("Problem during blueprint rendering", ReportType.TEST.nuggets())));
    }

    /**
     * Draws structure into world.
     *
     * @return suppressed exceptions
     */
    public Map<Object, Exception> drawUnsafe(final BlueprintPreviewData previewData, final BlockPos pos, final WorldRenderMacros ctx)
    {
        final BlockPos anchorPos = pos.subtract(previewData.getBlueprint().getPrimaryBlockOffset());

        // cull entire rendering
        if (!ctx.frustum.isVisible(previewData.getBlueprint().getAABB().move(anchorPos)) && !bypassMainFrustum)
        {
            return Map.of();
        }

        final Map<Object, Exception> suppressedExceptions = new IdentityHashMap<>();
        final Minecraft mc = Minecraft.getInstance();
        final long gameTime = mc.level.getGameTime();
        final PoseStack matrixStack = ctx.poseStack;
        final SubmitNodeCollector collector = ctx.submitNodeCollector;
        final DeltaTracker deltaTracker = ctx.deltaTracker;
        final ProfilerFiller profiler = Profiler.get();

        profiler.push("struct_render_init");

        // make sure instances are synced
        updateBlueprint(previewData);
        blockAccess.setWorldPos(anchorPos);

        // init
        if (vertexBuffers == null)
        {
            init(previewData, suppressedExceptions);
        }

        profiler.popPush("struct_render_prepare");
        final Vec3 viewPosition = ctx.cameraPosition;
        final Vec3 realRenderRootVecd = Vec3.atLowerCornerOf(anchorPos).subtract(viewPosition);
        final Vec3 localCameraPos = viewPosition.subtract(anchorPos.getX(), anchorPos.getY(), anchorPos.getZ());

        final float partialTicks;
        {
            final Entity entity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
            partialTicks = mc.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : deltaTracker.getGameTimeDeltaPartialTick(!mc.level.tickRateManager().isFrozen());
        }

        final Frustum blueprintLocalFrustum = new Frustum(ctx.frustum);
        blueprintLocalFrustum.prepare(localCameraPos.x(), localCameraPos.y(), localCameraPos.z());
        bypassMainFrustum = false;

        // Render blocks

        profiler.popPush("struct_render_blocks");
        final float alpha = TransparencyHack.getAlpha(previewData.getOverridePreviewTransparency());
        final boolean transparent = alpha >= 0;

        matrixStack.pushPose();
        matrixStack.translate(realRenderRootVecd.x(), realRenderRootVecd.y(), realRenderRootVecd.z());
        for (final Map.Entry<ChunkSectionLayer, RecordingVertexConsumer> entry : vertexBuffers.entrySet())
        {
            final RecordingVertexConsumer buffer = entry.getValue();
            final RenderType renderType = transparent ? getTransparentBlocksRenderType() : getRenderType(entry.getKey());
            final float alphaScale = transparent ? alpha : 1.0f;
            collector.submitCustomGeometry(matrixStack, renderType, (pose, consumer) -> buffer.replay(consumer, pose, alphaScale));
        }
        matrixStack.popPose();

        // Entities

        profiler.popPush("struct_render_entities");
        final EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
        for (final Entity entity : entities)
        {
            try
            {
                if (!entityRenderDispatcher.shouldRender(entity,
                    blueprintLocalFrustum,
                    localCameraPos.x(),
                    localCameraPos.y(),
                    localCameraPos.z(),
                    partialTicks))
                {
                    continue;
                }

                if (gameTime != lastGameTime && entity.is(ModTags.PREVIEW_TICKING_ENTITIES))
                {
                    try
                    {
                        entity.tick();
                    }
                    catch (final Exception e)
                    {
                        // well, noop
                        suppressedExceptions.put(entity, e);
                    }
                }

                final EntityRenderState entityState = entityRenderDispatcher.extractEntity(entity, partialTicks);
                entityRenderDispatcher.submit(entityState,
                    ctx.cameraRenderState,
                    entityState.x + realRenderRootVecd.x(),
                    entityState.y + realRenderRootVecd.y(),
                    entityState.z + realRenderRootVecd.z(),
                    matrixStack,
                    collector);
            }
            catch (final Exception e)
            {
                // Oops
                suppressedExceptions.put(entity, e);
            }
        }

        // Block entities

        profiler.popPush("struct_render_blockentities");
        final BlockEntityRenderDispatcher beDispatcher = mc.getBlockEntityRenderDispatcher();
        beDispatcher.prepare(localCameraPos);
        try
        {
            for (final BlockEntity tileEntity : tileEntities)
            {
                final BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = beDispatcher.getRenderer(tileEntity);
                if (renderer == null || !renderer.shouldRender(tileEntity, localCameraPos))
                {
                    continue;
                }

                final BlockPos tePos = tileEntity.getBlockPos();

                if (gameTime != lastGameTime)
                {
                    tickBlockEntity(tileEntity, tePos, anchorPos, mc, suppressedExceptions);
                }

                bypassMainFrustum |= renderer.shouldRenderOffScreen();

                matrixStack.pushPose();
                matrixStack.translate(realRenderRootVecd.x() + tePos.getX(), realRenderRootVecd.y() + tePos.getY(), realRenderRootVecd.z() + tePos.getZ());

                try
                {
                    final BlockEntityRenderState beState = beDispatcher.tryExtractRenderState(tileEntity, partialTicks, null, renderer.shouldRenderOffScreen());
                    if (beState != null)
                    {
                        beDispatcher.submit(beState, matrixStack, collector, ctx.cameraRenderState);
                    }
                }
                catch (final Exception e)
                {
                    suppressedExceptions.put(tileEntity, e);
                }

                matrixStack.popPose();
            }
        }
        finally
        {
            // restore vanilla setup
            beDispatcher.prepare(viewPosition);
        }

        lastGameTime = gameTime;
        profiler.pop();

        return suppressedExceptions;
    }

    /**
     * Hooks from EntityBlock#getTicker(Level, BlockState, BlockEntityType) for client side
     * either mc.level and anchorPos - particles, player distance etc.
     * or blockAccess and tePos - blueprint neighborhood
     */
    private void tickBlockEntity(final BlockEntity tileEntity,
        final BlockPos tePos,
        final BlockPos anchorPos,
        final Minecraft mc,
        final Map<Object, Exception> suppressedExceptions)
    {
        try
        {
            if (tileEntity instanceof final SpawnerBlockEntity spawner)
            {
                SpawnerBlockEntity.clientTick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), spawner);
            }
            else if (tileEntity instanceof final EnchantingTableBlockEntity enchTable)
            {
                EnchantingTableBlockEntity.bookAnimationTick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), enchTable);
            }
            else if (tileEntity instanceof final CampfireBlockEntity campfire)
            {
                final BlockState bs = blockAccess.getBlockState(tePos);
                if (bs.getBlock() instanceof CampfireBlock && bs.getValue(CampfireBlock.LIT))
                {
                    CampfireBlockEntity.particleTick(mc.level, anchorPos.offset(tePos), bs, campfire);
                }
            }
            else if (tileEntity instanceof final SkullBlockEntity skull)
            {
                final BlockState bs = blockAccess.getBlockState(tePos);
                if (bs.getBlock() instanceof SkullBlock && (bs.is(Blocks.DRAGON_HEAD) || bs.is(Blocks.DRAGON_WALL_HEAD) ||
                    bs.is(Blocks.PIGLIN_HEAD) ||
                    bs.is(Blocks.PIGLIN_WALL_HEAD)))
                {
                    SkullBlockEntity.animation(blockAccess, tePos, bs, skull);
                }
            }
            else if (tileEntity instanceof final BeaconBlockEntity beacon)
            {
                // uses sound and applies buffs, but we dont want any of this since we're preview
                BeaconBlockEntity.tick(blockAccess, tePos, blockAccess.getBlockState(tePos), beacon);
            }
            else if (tileEntity instanceof final VaultBlockEntity vault)
            {
                VaultBlockEntity.Client.tick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), vault.getClientData(), vault.getSharedData());
            }
            else if (tileEntity instanceof final TrialSpawnerBlockEntity trialSpawner)
            {
                trialSpawner.getTrialSpawner().tickClient(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos).getOptionalValue(TrialSpawnerBlock.OMINOUS).orElse(false));
            }
        }
        catch (final Exception e)
        {
            suppressedExceptions.put(tileEntity, e);
        }
    }

    /**
     * Clears vertex data.
     */
    private void clearVertexBuffers()
    {
        if (vertexBuffers != null)
        {
            vertexBuffers = null;
        }
    }

    @Override
    public void close()
    {
        clearVertexBuffers();
    }

    /**
     * Preview transparency.
     * FABRIC-PORT: formerly used GL constant blend color, now the alpha is multiplied into vertex colors and drawn with a translucent pipeline.
     */
    public static class TransparencyHack
    {
        public static final float THRESHOLD = 0.99f;

        /**
         * @param overrideValue override from preview data, -1 if none
         * @return alpha to apply, or -1 if no transparency should be applied
         */
        public static float getAlpha(final float overrideValue)
        {
            float alpha = Structurize.getConfig().getClient().rendererTransparency.get().floatValue();
            if (overrideValue != -1)
            {
                alpha = Mth.clamp(overrideValue, 0, 1);
            }

            if (alpha < 0 || alpha > THRESHOLD)
            {
                return -1;
            }
            return alpha;
        }
    }
}
