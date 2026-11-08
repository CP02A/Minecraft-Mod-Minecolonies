package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Exposes CombatTracker#entries (was public through the NeoForge access transformer).
 */
@Mixin(CombatTracker.class)
public interface CombatTrackerAccessor
{
    @Accessor("entries")
    List<CombatEntry> minecolonies$getEntries();

    @org.spongepowered.asm.mixin.gen.Invoker("getMostSignificantFall")
    @org.jspecify.annotations.Nullable CombatEntry minecolonies$getMostSignificantFall();
}
