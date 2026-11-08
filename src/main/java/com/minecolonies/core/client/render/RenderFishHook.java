package com.minecolonies.core.client.render;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.other.NewBobberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Determines how the fish hook is rendered.
 */
public class RenderFishHook extends EntityRenderer<NewBobberEntity, RenderFishHook.State>
{
    /**
     * The resource location containing the particle textures (Spawned by the fishHook).
     */
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");

    /**
     * The render type of the hook.
     */
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE);

    /**
     * Render state of the bobber.
     */
    public static class State extends FishingHookRenderState
    {
        /**
         * If the bobber belongs to a citizen (only then it is rendered).
         */
        public boolean citizenOwned;
    }

    /**
     * Required constructor, sets the RenderManager.
     *
     * @param context context that we use.
     */
    public RenderFishHook(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(final NewBobberEntity entity, final State state, final float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.getOwner() instanceof AbstractEntityCitizen citizen)
        {
            state.citizenOwned = true;
            int i = citizen.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            final ItemStack itemstack = citizen.getMainHandItem();
            if (!itemstack.is(Items.FISHING_ROD))
            {
                i = -i;
            }

            final float f2 = Mth.lerp(partialTicks, citizen.yBodyRotO, citizen.yBodyRot) * ((float) Math.PI / 180F);
            final double d0 = Mth.sin(f2);
            final double d1 = Mth.cos(f2);
            final double d2 = (double) i * 0.35D;

            final double d4 = Mth.lerp(partialTicks, citizen.xo, citizen.getX()) - d1 * d2 - d0 * 0.8D;
            final double d5 = citizen.yo + (double) citizen.getEyeHeight() + (citizen.getY() - citizen.yo) * (double) partialTicks - 0.45D;
            final double d6 = Mth.lerp(partialTicks, citizen.zo, citizen.getZ()) - d0 * d2 + d1 * 0.8D;
            final float f3 = citizen.isCrouching() ? -0.1875F : 0.0F;

            final double d9 = Mth.lerp(partialTicks, entity.xo, entity.getX());
            final double d10 = Mth.lerp(partialTicks, entity.yo, entity.getY()) + 0.25D;
            final double d8 = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            state.lineOriginOffset = new Vec3(d4 - d9, d5 - d10 + f3, d6 - d8);
        }
        else
        {
            state.citizenOwned = false;
            state.lineOriginOffset = Vec3.ZERO;
        }
    }

    @Override
    public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera)
    {
        if (!state.citizenOwned)
        {
            return;
        }

        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.rotate(camera.orientation);
        poseStack.rotate(Axis.YP.rotationDegrees(180.0F));
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
            vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();

        final float f4 = (float) state.lineOriginOffset.x;
        final float f5 = (float) state.lineOriginOffset.y;
        final float f6 = (float) state.lineOriginOffset.z;
        final float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (int k = 0; k < 16; ++k)
            {
                final float a0 = fraction(k, 16);
                final float a1 = fraction(k + 1, 16);
                stringVertex(f4, f5, f6, buffer, pose, a0, a1, width);
                stringVertex(f4, f5, f6, buffer, pose, a1, a0, width);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected boolean affectedByCulling(final NewBobberEntity entity)
    {
        return false;
    }

    private static float fraction(int first, int second)
    {
        return (float) first / (float) second;
    }

    private static void vertex(final VertexConsumer consumer, final PoseStack.Pose pose, final int light, final float x, final int y, final int u, final int v)
    {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
          .setColor(-1)
          .setUv((float) u, (float) v)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(
      final float xa,
      final float ya,
      final float za,
      final VertexConsumer buffer,
      final PoseStack.Pose pose,
      final float aa,
      final float nexta,
      final float width)
    {
        final float f = xa * aa;
        final float f1 = ya * (aa * aa + aa) * 0.5F + 0.25F;
        final float f2 = za * aa;
        float f3 = xa * nexta - f;
        float f4 = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - f1;
        float f5 = za * nexta - f2;
        final float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        buffer.addVertex(pose, f, f1, f2).setColor(-16777216).setNormal(pose, f3, f4, f5).setLineWidth(width);
    }

    /**
     * Returns the location of the texture.
     *
     * @return the address of the resource
     */
    @NotNull
    public static Identifier getTexture()
    {
        return TEXTURE;
    }
}
