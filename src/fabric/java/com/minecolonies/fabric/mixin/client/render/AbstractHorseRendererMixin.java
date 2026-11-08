package com.minecolonies.fabric.mixin.client.render;

import com.minecolonies.core.client.render.CavalryOverlayLayer;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extracts the cavalry readiness into the horse render state, for {@link CavalryOverlayLayer}.
 */
@Mixin(AbstractHorseRenderer.class)
public class AbstractHorseRendererMixin
{
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/animal/equine/AbstractHorse;Lnet/minecraft/client/renderer/entity/state/EquineRenderState;F)V", at = @At("TAIL"))
    private void minecolonies$extractCavalry(final AbstractHorse entity, final EquineRenderState state, final float partialTicks, final CallbackInfo ci)
    {
        state.setData(CavalryOverlayLayer.SEGMENTS, entity instanceof CavalryHorseEntity cavalryHorse ? CavalryOverlayLayer.getSegments(cavalryHorse) : null);
    }
}
