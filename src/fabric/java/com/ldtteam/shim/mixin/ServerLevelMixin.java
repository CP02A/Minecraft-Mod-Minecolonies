package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.VanillaGameEvent;
import com.ldtteam.shim.neoforge.event.level.ExplosionEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VanillaGameEvent (cancellable) and ExplosionEvent.Start (cancellable).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin
{
    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V", at = @At("HEAD"), cancellable = true)
    private void minecolonies$vanillaGameEvent(final Holder<GameEvent> gameEvent, final Vec3 position, final GameEvent.Context context, final CallbackInfo ci)
    {
        if (NeoForge.EVENT_BUS.post(new VanillaGameEvent((ServerLevel) (Object) this, gameEvent, position, context)).isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;explode()I"), cancellable = true)
    private void minecolonies$explosionStart(final CallbackInfo ci, @Local final ServerExplosion explosion)
    {
        if (NeoForge.EVENT_BUS.post(new ExplosionEvent.Start((ServerLevel) (Object) this, explosion)).isCanceled())
        {
            ci.cancel();
        }
    }
}
