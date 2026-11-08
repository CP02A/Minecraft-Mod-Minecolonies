package com.minecolonies.core.client.render;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;

public class CavalryOverlayLayer extends RenderLayer<HorseRenderState, HorseModel>
{
    /**
     * Render state key with the number of readiness segments (null if not a cavalry horse).
     * FABRIC-PORT: filled by com.minecolonies.fabric.mixin.client.render.AbstractHorseRendererMixin.
     */
    public static final RenderStateDataKey<Integer> SEGMENTS = RenderStateDataKey.create(() -> "minecolonies:cavalry_segments");

    public CavalryOverlayLayer(RenderLayerParent<HorseRenderState, HorseModel> parent)
    {
        super(parent);
    }

    /**
     * Calculate the readiness segments of a cavalry horse.
     *
     * @param cavhorse the horse.
     * @return the number of segments (0-5).
     */
    public static int getSegments(final CavalryHorseEntity cavhorse)
    {
        float threshold = cavhorse.getMaxHealth() * CavalryHorseEntity.COMBAT_READINESS_THRESHOLD;
        float cooldown = Math.max(0f, cavhorse.getAnimalDataView() == null ? 0 : cavhorse.getAnimalDataView().getCombatCooldown());
        float readiness = Mth.clamp(1.0f - (cooldown / Math.max(0.001f, threshold)), 0f, 1f);

        return Mth.clamp((int) Math.floor(readiness * 5f + 0.0001f), 0, 5);
    }

    /**
     * Renders the cavalry horse overlay layer, which decorates the horse
     * and indicates the horse's readiness for combat.
     */
    @Override
    public void submit(
      @Nonnull final PoseStack pose,
      @Nonnull final SubmitNodeCollector submitNodeCollector,
      final int packedLight,
      @Nonnull final HorseRenderState state,
      final float yRot,
      final float xRot)
    {
        final Integer segments = state.getData(SEGMENTS);
        if (segments == null)
        {
            return;
        }

        Identifier OVERLAY_TEX = Identifier.fromNamespaceAndPath(
            Constants.MOD_ID,
            "textures/entity/horse/cavalry_overlay_layer" + segments + ".png"
        );

        // 0.85f alpha -> 217 (out of 255)
        int alpha = (int)(0.85f * 255.0f);
        int color = ARGB.color(alpha, 255, 255, 255);

        submitNodeCollector.submitModel(this.getParentModel(), state, pose, RenderTypes.entityTranslucent(OVERLAY_TEX), packedLight, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor);
    }
}
