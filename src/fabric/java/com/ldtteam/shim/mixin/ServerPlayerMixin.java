package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.entity.EntityTravelToDimensionEvent;
import com.ldtteam.shim.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player variants of EntityTravelToDimensionEvent and LivingDeathEvent (ServerPlayer overrides without calling super).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin
{
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
    private void shim$onTravelToDimension(final TeleportTransition transition, final CallbackInfoReturnable<ServerPlayer> cir)
    {
        final ServerPlayer self = (ServerPlayer) (Object) this;
        if (!self.isRemoved() && transition.newLevel().dimension() != self.level().dimension())
        {
            if (NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(self, transition.newLevel().dimension())).isCanceled())
            {
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void shim$onDeath(final DamageSource source, final CallbackInfo ci)
    {
        if (NeoForge.EVENT_BUS.post(new LivingDeathEvent((ServerPlayer) (Object) this, source)).isCanceled())
        {
            ci.cancel();
        }
    }
}
