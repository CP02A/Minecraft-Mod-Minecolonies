package com.minecolonies.core.client.render;

import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Armor layer for citizens: renders the display armor (if set) and the custom texture player head.
 * FABRIC-PORT: display armor is resolved into the render state in {@link RenderBipedCitizen#extractRenderState}.
 */
public class CitizenArmorLayer<S extends CitizenRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends HumanoidArmorLayer<S, M, A>
{
    private final Function<SkullBlock.Type, SkullModelBase> skullModels;
    private final PlayerSkinRenderCache                    playerSkinRenderCache;
    private final Map<UUID, ResolvableProfile>             gameProfileMap = new HashMap<>();

    public CitizenArmorLayer(
      final RenderLayerParent<S, M> parentLayer,
      final ArmorModelSet<A> modelSet,
      final EquipmentLayerRenderer equipmentRenderer,
      final EntityModelSet entityModelSet,
      final PlayerSkinRenderCache playerSkinRenderCache)
    {
        super(parentLayer, modelSet, equipmentRenderer);
        this.skullModels = Util.memoize(type -> SkullBlockRenderer.createModel(entityModelSet, type));
        this.playerSkinRenderCache = playerSkinRenderCache;
    }

    @Override
    public void submit(
      @NotNull final PoseStack poseStack,
      @NotNull final SubmitNodeCollector submitNodeCollector,
      final int light,
      @NotNull final S citizen,
      final float headRotY,
      final float headRotX)
    {
        final ICitizenDataView citizenDataView = citizen.getCitizenDataView();
        if (citizenDataView == null || citizenDataView.getInventory() == null || citizen.isInvisible)
        {
            return;
        }

        final UUID textureUUID = citizenDataView.getCustomTextureUUID();
        if (textureUUID != null)
        {
            final ResolvableProfile gameProfile = gameProfileMap.computeIfAbsent(textureUUID, ResolvableProfile::createUnresolved);
            final SkullModelBase skullmodelbase = this.skullModels.apply(SkullBlock.Types.PLAYER);
            if (skullmodelbase != null)
            {
                poseStack.pushPose();
                poseStack.scale(1.0F, -1.0F, -1.0F);
                poseStack.rotateAround(Axis.YP.rotationDegrees(180), 0.0f, 0.0f, 0.0f);
                poseStack.scale(-1.0F, -1.0F, 1.0F);

                final RenderType rendertype = playerSkinRenderCache.getOrDefault(gameProfile).renderType();
                final SkullModelBase.State skullState = new SkullModelBase.State();
                skullState.animationPos = 0f;
                skullState.yRot = headRotY;
                skullState.xRot = headRotX;
                submitNodeCollector.submitModel(skullmodelbase, skullState, poseStack, rendertype, light, OverlayTexture.NO_OVERLAY, citizen.outlineColor);

                poseStack.popPose();
            }
        }

        try
        {
            super.submit(poseStack, submitNodeCollector, light, citizen, headRotY, headRotX);
        }
        catch (final Exception e)
        {
            Log.getLogger().warn("Error rendering armor on citizen, report to the armor's mod.", e);
            for (final ItemStack stack : new ItemStack[] {citizen.headEquipment, citizen.chestEquipment, citizen.legsEquipment, citizen.feetEquipment})
            {
                if (!stack.isEmpty())
                {
                    ClientEventHandler.extraItemTooltips.put(stack.getItem(),
                      Component.literal("This armor is causing errors when rendering on citizens, check your latest.log and report to the respective armor mod.").withStyle(
                        ChatFormatting.RED));
                }
            }
        }
    }
}
