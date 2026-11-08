package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.event.EventHooks;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingConversionEvent.Pre/Post around Mob#convertTo and MobDespawnEvent in Mob#checkDespawn.
 */
@Mixin(Mob.class)
public abstract class MobMixin
{
    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
      at = @At("HEAD"), cancellable = true)
    private <T extends Mob> void shim$preConvert(final EntityType<T> entityType, final ConversionParams params, final EntitySpawnReason spawnReason,
        final ConversionParams.AfterConversion<T> afterConversion, final CallbackInfoReturnable<T> cir)
    {
        final Mob self = (Mob) (Object) this;
        if (!self.isRemoved() && !self.level().isClientSide() && !EventHooks.canLivingConvert(self, entityType, null))
        {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",
      at = @At("RETURN"))
    private <T extends Mob> void shim$postConvert(final EntityType<T> entityType, final ConversionParams params, final EntitySpawnReason spawnReason,
        final ConversionParams.AfterConversion<T> afterConversion, final CallbackInfoReturnable<T> cir)
    {
        final T result = cir.getReturnValue();
        if (result != null)
        {
            EventHooks.onLivingConvert((Mob) (Object) this, result);
        }
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void shim$checkDespawn(final CallbackInfo ci)
    {
        final Mob self = (Mob) (Object) this;
        if (!self.level().isClientSide() && EventHooks.checkMobDespawn(self))
        {
            ci.cancel();
        }
    }
}
