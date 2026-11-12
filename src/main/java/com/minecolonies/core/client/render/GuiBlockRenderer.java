package com.minecolonies.core.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a block state model, turned by a pitch and a yaw, into a GUI (used by the JEI recipe categories, see
 * {@code compatibility.jei.RenderHelper#renderBlock}).
 * FABRIC-PORT: 1.21.1 rendered the block directly into the GUI with BlockRenderDispatcher#renderSingleBlock. The 26.x GUI only collects
 * render states and draws 3D content through picture-in-picture renderers (like entities in GUIs); this renderer draws the block model of
 * the state with the same transforms as before. Fabric's PictureInPictureRendererRegistry gives each block drawn in a frame its own
 * renderer instance.
 */
public class GuiBlockRenderer extends PictureInPictureRenderer<GuiBlockRenderer.State>
{
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelRenderState blockModel = new BlockModelRenderState();

    @Nullable
    private BlockModelResolver resolver;

    @Override
    public Class<State> getRenderStateClass()
    {
        return State.class;
    }

    @Override
    protected void renderToTexture(final State state, final PoseStack poseStack, final SubmitNodeCollector collector)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (resolver == null)
        {
            resolver = new BlockModelResolver(mc.getModelManager());
        }
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

        // The picture-in-picture pose is scale(s, s, -s); the 1.21.1 GUI pose was scale(-s, -s, -s): flip x and y first.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.mulPose(blockTransform(state.pitch(), state.yaw()));

        resolver.update(blockModel, state.block(), DISPLAY_CONTEXT);
        blockModel.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale)
    {
        // The anchor (x, y of RenderHelper#renderBlock) is the center of the area.
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel()
    {
        return "minecolonies block";
    }

    /**
     * The transforms of 1.21.1's RenderHelper#renderBlock after its scale, in block units: the unit cube of the block model, turned by
     * the pitch around its front bottom edge and by the yaw around its vertical center axis.
     */
    private static Matrix4f blockTransform(final float pitch, final float yaw)
    {
        final PoseStack pose = new PoseStack();
        pose.translate(-0.5F, -0.5F, 0);
        pose.rotate(Axis.XP.rotationDegrees(pitch));
        pose.translate(0.5F, 0, -0.5F);
        pose.rotate(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5F, 0, 0.5F);
        pose.translate(0, 0, -1);
        return new Matrix4f(pose.last().pose());
    }

    /**
     * Render state of a block in a GUI.
     *
     * @param block       the block state.
     * @param pitch       rotation forwards in degrees.
     * @param yaw         rotation sideways in degrees.
     * @param x0          left edge of the drawn area.
     * @param y0          top edge.
     * @param x1          right edge.
     * @param y1          bottom edge.
     * @param scale       GUI pixels per block.
     * @param pose        the GUI pose.
     * @param scissorArea the GUI scissor area, if any.
     * @param bounds      the bounds of the drawn area on screen.
     */
    public record State(
      BlockState block,
      float pitch,
      float yaw,
      int x0,
      int y0,
      int x1,
      int y1,
      float scale,
      Matrix3x2fc pose,
      @Nullable ScreenRectangle scissorArea,
      @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState
    {
        /**
         * Create the render state of a block drawn around an anchor point; the area is just large enough for the turned block.
         *
         * @param block       the block state.
         * @param x           horizontal anchor position (GUI coordinates).
         * @param y           vertical anchor position.
         * @param pitch       rotation forwards in degrees.
         * @param yaw         rotation sideways in degrees.
         * @param scale       GUI pixels per block.
         * @param pose        the current GUI pose.
         * @param scissorArea the current scissor area.
         * @return the render state.
         */
        public static State of(
          final BlockState block,
          final float x,
          final float y,
          final float pitch,
          final float yaw,
          final float scale,
          final Matrix3x2fc pose,
          @Nullable final ScreenRectangle scissorArea)
        {
            // Screen extent of the turned unit cube around the anchor (the x/y flip of renderToTexture doesn't change it).
            final Matrix4f transform = blockTransform(pitch, yaw);
            float halfW = 0;
            float halfH = 0;
            final Vector3f corner = new Vector3f();
            for (int i = 0; i < 8; i++)
            {
                transform.transformPosition(i & 1, (i >> 1) & 1, (i >> 2) & 1, corner);
                halfW = Math.max(halfW, Math.abs(corner.x));
                halfH = Math.max(halfH, Math.abs(corner.y));
            }
            final int cx = Math.round(x);
            final int cy = Math.round(y);
            final int w = (int) Math.ceil(halfW * scale) + 1;
            final int h = (int) Math.ceil(halfH * scale) + 1;
            return new State(block, pitch, yaw, cx - w, cy - h, cx + w, cy + h, scale, pose, scissorArea,
              PictureInPictureRenderState.getBounds(cx - w, cy - h, cx + w, cy + h, pose, scissorArea));
        }
    }
}
