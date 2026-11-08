package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Allows setting the owner reference of a horse (replacement for the removed AbstractHorse#setOwnerUUID).
 */
@Mixin(AbstractHorse.class)
public interface AbstractHorseAccessor
{
    @Accessor("owner")
    void minecolonies$setOwnerReference(@Nullable EntityReference<LivingEntity> owner);
}
