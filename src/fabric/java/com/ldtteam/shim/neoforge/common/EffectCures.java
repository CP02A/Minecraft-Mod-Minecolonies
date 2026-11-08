// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.EffectCures), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class EffectCures
{
    public static final EffectCure MILK = EffectCure.get("milk");
    public static final EffectCure HONEY = EffectCure.get("honey");
    public static final EffectCure PROTECTED_BY_TOTEM = EffectCure.get("protected_by_totem");

    public static final Set<EffectCure> DEFAULT_CURES = Set.of(MILK, PROTECTED_BY_TOTEM);

    /**
     * Replacement for NeoForge's {@code entity.removeEffectsCuredBy(cure)}. Vanilla has no per-effect cures; milk and totems clear all
     * effects (as vanilla does), honey clears poison.
     *
     * @return true if any effect was removed.
     */
    public static boolean removeEffectsCuredBy(final LivingEntity entity, final EffectCure cure)
    {
        if (cure == HONEY)
        {
            return entity.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
        }
        return entity.removeAllEffects();
    }
}
