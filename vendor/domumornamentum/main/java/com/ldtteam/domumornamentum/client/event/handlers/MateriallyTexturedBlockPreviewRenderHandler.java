package com.ldtteam.domumornamentum.client.event.handlers;

import com.ldtteam.domumornamentum.client.render.ModelGhostRenderer;
import com.ldtteam.domumornamentum.util.ItemStackUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the placement preview of held materially textured blocks.
 *
 * <p>Fabric: registered on {@code LevelRenderEvents.COLLECT_SUBMITS} by the Domum client initializer (replaces
 * NeoForge's {@code RenderLevelStageEvent.AfterLevel}); the ghost is submitted as custom geometry.
 */
public class MateriallyTexturedBlockPreviewRenderHandler
{
    private MateriallyTexturedBlockPreviewRenderHandler()
    {
    }

    public static void renderMateriallyTexturedBlockPreview(final PoseStack poseStack, final SubmitNodeCollector collector)
    {
        final HitResult rayTraceResult = Minecraft.getInstance().hitResult;
        if (!(rayTraceResult instanceof final BlockHitResult blockRayTraceResult) || blockRayTraceResult.getType() == HitResult.Type.MISS)
            return;

        final Player playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null || playerEntity.isSpectator() || Minecraft.getInstance().level == null)
            return;

        final ItemStack heldStack = ItemStackUtils.getMateriallyTexturedItemStackFromPlayer(playerEntity);
        if (heldStack.isEmpty())
            return;

        final Vec3 targetedRenderPos = Vec3.atLowerCornerOf(blockRayTraceResult.getBlockPos().offset(blockRayTraceResult.getDirection().getUnitVec3i()));

        ModelGhostRenderer.getInstance().renderGhost(
                poseStack,
                collector,
                heldStack,
                targetedRenderPos,
                blockRayTraceResult,
                Minecraft.getInstance().level,
                false
        );
    }
}
