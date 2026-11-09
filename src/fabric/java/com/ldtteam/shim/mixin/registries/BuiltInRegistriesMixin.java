package com.ldtteam.shim.mixin.registries;

import com.ldtteam.shim.fabric.ShimSetupPhase;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the FML setup lifecycle once Fabric froze the built-in registries (after all mod initializers).
 */
@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin
{
    @Inject(method = "bootStrap", at = @At("RETURN"))
    private static void shim$afterFreeze(final CallbackInfo ci)
    {
        ShimSetupPhase.onRegistriesFrozen();
    }
}
