package com.minecolonies.core.compatibility.jei;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * JEI GUI rendering utility helpers
 */
public class RenderHelper
{
    /**
     * Render a block model on a GUI.
     *
     * @param ctx   gui graphics
     * @param block the blockstate to render
     * @param x     horizontal center position
     * @param y     vertical bottom position
     * @param z     distance from camera
     * @param pitch rotation forwards
     * @param yaw   rotation sideways
     * @param scale scaling factor
     */
    public static void renderBlock(final GuiGraphicsExtractor ctx, final BlockState block, final float x, final float y, final float z, final float pitch, final float yaw, final float scale)
    {
        // FABRIC-PORT TODO: 26.x GUI has no immediate-mode 3D block rendering; the block is drawn via its item model
        // (isometric GUI transform) instead of a custom pitch/yaw rendered block model.
        final ItemStack stack = new ItemStack(block.getBlock());
        if (stack.isEmpty())
        {
            return;
        }

        final Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        final float itemScale = scale / 16.0F * 1.25F;
        pose.scale(itemScale, itemScale);
        ctx.item(stack, -8, -8);
        pose.popMatrix();
    }

    /**
     * Draw an entity on a GUI, similar to the inventory player display.
     *
     * @param ctx     gui graphics
     * @param x       horizontal center position
     * @param y       vertical bottom (feet) position
     * @param scale   pixels per block
     * @param headYaw head yaw
     * @param yaw     body yaw
     * @param pitch   pitch
     * @param entity  entity to draw
     */
    public static void drawEntity(final GuiGraphicsExtractor ctx,
        final int x,
        final int y,
        final float scale,
        final float headYaw,
        final float yaw,
        final float pitch,
        final Entity entity)
    {
        final EntityRenderer<? super Entity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        final EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        renderState.nameTag = null;

        if (renderState instanceof final LivingEntityRenderState living)
        {
            living.bodyRot = 180.0F + yaw;
            living.yRot = Mth.wrapDegrees(headYaw - yaw);
            living.xRot = -pitch;
            living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
            living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
            living.scale = 1.0F;
        }

        final Quaternionf pitchRotation = Axis.XP.rotationDegrees(pitch);
        final Quaternionf rotation = Axis.ZP.rotationDegrees(180.0F).mul(pitchRotation);

        final float height = renderState.boundingBoxHeight;
        final int halfW = (int) Math.ceil(scale * Math.max(1.0F, renderState.boundingBoxWidth));
        final int halfH = (int) Math.ceil(scale * (height / 2.0F + 0.5F));
        final int cy = (int) (y - scale * height / 2.0F);

        final Vector3f translation = new Vector3f(0.0F, height / 2.0F, 0.0F);
        ctx.entity(renderState, scale, translation, rotation, pitchRotation, x - halfW, cy - halfH, x + halfW, cy + halfH);
    }
}
