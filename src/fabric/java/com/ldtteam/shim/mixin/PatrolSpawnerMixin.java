package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.event.EventHooks;
import com.ldtteam.shim.neoforge.event.entity.living.MobSpawnEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MobSpawnEvent.PositionCheck for patrol members (NeoForge fires it for patrols too, not only natural spawns), so e.g.
 * MineColonies can keep pillager patrols out of colony buildings. Vanilla's own checks already ran, so only FAIL matters.
 */
@Mixin(PatrolSpawner.class)
public abstract class PatrolSpawnerMixin
{
    @Inject(method = "spawnPatrolMember", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/PatrollingMonster;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"), cancellable = true)
    private void shim$positionCheck(final ServerLevel level, final BlockPos pos, final RandomSource random, final boolean isLeader, final CallbackInfoReturnable<Boolean> cir,
        @Local final PatrollingMonster mob)
    {
        if (EventHooks.checkSpawnPositionResult(mob, level, EntitySpawnReason.PATROL, null) == MobSpawnEvent.PositionCheck.Result.FAIL)
        {
            cir.setReturnValue(false);
        }
    }
}
