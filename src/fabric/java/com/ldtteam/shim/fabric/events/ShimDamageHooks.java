package com.ldtteam.shim.fabric.events;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.common.damagesource.DamageContainer;
import com.ldtteam.shim.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * Helpers for the damage pipeline mixins.
 */
public final class ShimDamageHooks
{
    private ShimDamageHooks()
    {
    }

    /**
     * Fires LivingDamageEvent.Pre with the damage after armor/enchantments/absorption.
     *
     * @return the damage to apply to health.
     */
    public static float fireDamagePre(final LivingEntity entity, final float dmg)
    {
        final DamageContainer container = ((DamageContainerHolder) entity).minecolonies$getDamageContainers().peek();
        if (container == null)
        {
            return dmg;
        }
        container.setNewDamage(dmg);
        final LivingDamageEvent.Pre event = new LivingDamageEvent.Pre(entity, container);
        NeoForge.EVENT_BUS.post(event);
        return Math.max(0, event.getNewDamage());
    }
}
