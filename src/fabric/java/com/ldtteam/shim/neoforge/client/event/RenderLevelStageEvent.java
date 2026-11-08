// Adapted from NeoForge 21.1/26.3 (net.neoforged.neoforge.client.event.RenderLevelStageEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the game bus at several points of level rendering, from Fabric's {@code LevelRenderEvents}. Supports both the NeoForge 21.1
 * style ({@link #getStage()}) and the NeoForge 26.x style (subclasses such as {@link AfterLevel}).
 * <ul>
 *     <li>{@link AfterOpaqueBlocks} ({@link Stage#AFTER_SOLID_BLOCKS}): Fabric AFTER_OPAQUE_TERRAIN (no pose stack / submit collector)</li>
 *     <li>{@link AfterEntities} ({@link Stage#AFTER_ENTITIES}): Fabric COLLECT_SUBMITS, the place to submit custom geometry</li>
 *     <li>{@link AfterOpaqueFeatures} ({@link Stage#AFTER_BLOCK_ENTITIES}): Fabric AFTER_SOLID_FEATURES</li>
 *     <li>{@link AfterTranslucentBlocks} ({@link Stage#AFTER_TRANSLUCENT_BLOCKS}, then again as {@link Stage#AFTER_TRIPWIRE_BLOCKS}): Fabric AFTER_TRANSLUCENT_TERRAIN</li>
 *     <li>{@link AfterTranslucentFeatures} ({@link Stage#AFTER_PARTICLES}): Fabric AFTER_TRANSLUCENT_FEATURES</li>
 *     <li>{@link AfterLevel} ({@link Stage#AFTER_LEVEL}): Fabric END_MAIN</li>
 * </ul>
 */
public class RenderLevelStageEvent extends Event
{
    private final Stage stage;
    private final LevelRenderer levelRenderer;
    private final LevelRenderState levelRenderState;
    @Nullable
    private final PoseStack poseStack;
    @Nullable
    private final SubmitNodeCollector submitNodeCollector;

    public RenderLevelStageEvent(
      final Stage stage,
      final LevelRenderer levelRenderer,
      final LevelRenderState levelRenderState,
      @Nullable final PoseStack poseStack,
      @Nullable final SubmitNodeCollector submitNodeCollector)
    {
        this.stage = stage;
        this.levelRenderer = levelRenderer;
        this.levelRenderState = levelRenderState;
        this.poseStack = poseStack;
        this.submitNodeCollector = submitNodeCollector;
    }

    public Stage getStage()
    {
        return stage;
    }

    public LevelRenderer getLevelRenderer()
    {
        return levelRenderer;
    }

    public LevelRenderState getLevelRenderState()
    {
        return levelRenderState;
    }

    /**
     * @return the pose stack (camera relative); a fresh identity pose stack if the stage has none.
     */
    public PoseStack getPoseStack()
    {
        return poseStack != null ? poseStack : new PoseStack();
    }

    @Nullable
    public SubmitNodeCollector getSubmitNodeCollector()
    {
        return submitNodeCollector;
    }

    public float getPartialTick()
    {
        return levelRenderState.worldPartialTicks;
    }

    public long getRenderTick()
    {
        return levelRenderState.gameTime;
    }

    public Minecraft getMinecraft()
    {
        return Minecraft.getInstance();
    }

    public static class AfterOpaqueBlocks extends RenderLevelStageEvent
    {
        public AfterOpaqueBlocks(final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(Stage.AFTER_SOLID_BLOCKS, levelRenderer, state, poseStack, collector);
        }
    }

    public static class AfterEntities extends RenderLevelStageEvent
    {
        public AfterEntities(final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(Stage.AFTER_ENTITIES, levelRenderer, state, poseStack, collector);
        }
    }

    public static class AfterOpaqueFeatures extends RenderLevelStageEvent
    {
        public AfterOpaqueFeatures(final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(Stage.AFTER_BLOCK_ENTITIES, levelRenderer, state, poseStack, collector);
        }
    }

    public static class AfterTranslucentBlocks extends RenderLevelStageEvent
    {
        public AfterTranslucentBlocks(final Stage stage, final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(stage, levelRenderer, state, poseStack, collector);
        }
    }

    public static class AfterTranslucentFeatures extends RenderLevelStageEvent
    {
        public AfterTranslucentFeatures(final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(Stage.AFTER_PARTICLES, levelRenderer, state, poseStack, collector);
        }
    }

    public static class AfterLevel extends RenderLevelStageEvent
    {
        public AfterLevel(final LevelRenderer levelRenderer, final LevelRenderState state, @Nullable final PoseStack poseStack, @Nullable final SubmitNodeCollector collector)
        {
            super(Stage.AFTER_LEVEL, levelRenderer, state, poseStack, collector);
        }
    }

    /**
     * Render stages (NeoForge 21.1 API). Only the stages listed in the class javadoc are fired on Fabric.
     */
    public enum Stage
    {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_ENTITIES,
        AFTER_BLOCK_ENTITIES,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_TRIPWIRE_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL
    }
}
