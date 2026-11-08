package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.ldtteam.shim.fabric.events.ShimDamageHooks;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires PlayerTickEvent.Pre/Post on both logical sides, and LivingDamageEvent.Pre for players (Player overrides actuallyHurt).
 */
@Mixin(Player.class)
public abstract class PlayerMixin
{
    @Inject(method = "tick", at = @At("HEAD"))
    private void shim$preTick(final CallbackInfo ci)
    {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre((Player) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void shim$postTick(final CallbackInfo ci)
    {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post((Player) (Object) this));
    }

    @ModifyVariable(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getCombatTracker()Lnet/minecraft/world/damagesource/CombatTracker;"), argsOnly = true)
    private float shim$livingDamagePre(final float dmg)
    {
        return ShimDamageHooks.fireDamagePre((Player) (Object) this, dmg);
    }
}
