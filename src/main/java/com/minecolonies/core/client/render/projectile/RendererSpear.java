package com.minecolonies.core.client.render.projectile;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.model.SpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.jetbrains.annotations.NotNull;

/**
 * Custom renderer for spears
 */
public class RendererSpear extends EntityRenderer<ThrownTrident, ThrownTridentRenderState>
{
    private final Identifier texture = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entity/spear.png");
    private final SpearModel model;

    /**
     * Create a new spear renderer.
     * @param context the context.
     */
    public RendererSpear(final EntityRendererProvider.Context context)
    {
        super(context);
        this.model = new SpearModel(context.bakeLayer(ModelLayers.TRIDENT));
    }

    @Override
    public ThrownTridentRenderState createRenderState()
    {
        return new ThrownTridentRenderState();
    }

    @Override
    public void extractRenderState(final ThrownTrident entity, final ThrownTridentRenderState state, final float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.isFoil = entity.isFoil();
    }

    @Override
    public void submit(@NotNull final ThrownTridentRenderState state, @NotNull final PoseStack stack, @NotNull final SubmitNodeCollector submitNodeCollector, @NotNull final CameraRenderState camera)
    {
        super.submit(state, stack, submitNodeCollector, camera);

        stack.pushPose();
        stack.rotate(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        stack.rotate(Axis.ZP.rotationDegrees(state.xRot + 90.0F));
        if (state.isFoil)
        {
            submitNodeCollector.submitModel(model, Unit.INSTANCE, stack, RenderTypes.entitySolidGlint(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        else
        {
            submitNodeCollector.submitModel(model, Unit.INSTANCE, stack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
        stack.popPose();
    }
}
