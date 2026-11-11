package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.event.EventHooks;
import com.ldtteam.shim.neoforge.event.entity.living.MobSpawnEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MobSpawnEvent.PositionCheck for natural spawns (NeoForge replaces the checkSpawnRules/obstruction checks with the event).
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin
{
    @Inject(method = "isValidPositionForMob", at = @At("RETURN"), cancellable = true)
    private static void minecolonies$positionCheck(final ServerLevel level, final Mob mob, final double nearestPlayerDistanceSqr, final CallbackInfoReturnable<Boolean> cir)
    {
        // The despawn-distance part of the vanilla check is kept: only fire when the mob would not immediately despawn.
        final double despawn = mob.getType().getCategory().getDespawnDistance();
        if (nearestPlayerDistanceSqr > despawn * despawn && mob.removeWhenFarAway(nearestPlayerDistanceSqr))
        {
            return;
        }
        final MobSpawnEvent.PositionCheck.Result result = EventHooks.checkSpawnPositionResult(mob, level, EntitySpawnReason.NATURAL, null);
        if (result != MobSpawnEvent.PositionCheck.Result.DEFAULT)
        {
            cir.setReturnValue(result == MobSpawnEvent.PositionCheck.Result.SUCCEED);
        }
    }
}
