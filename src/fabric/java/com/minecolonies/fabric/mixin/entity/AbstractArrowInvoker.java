package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes AbstractArrow#setPierceLevel (was public through the NeoForge access transformer).
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowInvoker
{
    @Invoker("setPierceLevel")
    void minecolonies$setPierceLevel(byte pierceLevel);

    @org.spongepowered.asm.mixin.gen.Accessor("baseDamage")
    double minecolonies$getBaseDamage();
}
