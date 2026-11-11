package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires EntityTravelToDimensionEvent (cancellable) for non-player entities changing dimension.
 */
@Mixin(Entity.class)
public abstract class EntityMixin
{
    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void minecolonies$onTravelToDimension(final TeleportTransition transition, final CallbackInfoReturnable<Entity> cir)
    {
        final Entity self = (Entity) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel && !self.isRemoved() && transition.newLevel().dimension() != serverLevel.dimension())
        {
            if (NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(self, transition.newLevel().dimension())).isCanceled())
            {
                cir.setReturnValue(null);
            }
        }
    }
}
