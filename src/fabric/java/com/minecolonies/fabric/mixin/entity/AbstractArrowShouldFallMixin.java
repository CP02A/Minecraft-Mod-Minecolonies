package com.minecolonies.fabric.mixin.entity;

import com.minecolonies.core.entity.other.CustomArrowEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets MineColonies arrows override AbstractArrow#shouldFall (private in 26.x; was overridden through the NeoForge access transformer).
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowShouldFallMixin
{
    @Inject(method = "shouldFall", at = @At("HEAD"), cancellable = true)
    private void minecolonies$shouldFall(final CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this instanceof CustomArrowEntity arrow)
        {
            cir.setReturnValue(arrow.shouldFall());
        }
    }
}
