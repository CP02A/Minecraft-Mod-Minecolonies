package com.ldtteam.shim.mixin;

import com.ldtteam.shim.fabric.events.DamageContainerHolder;
import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.common.damagesource.DamageContainer;
import com.ldtteam.shim.neoforge.event.entity.item.ItemTossEvent;
import com.ldtteam.shim.neoforge.event.entity.living.LivingDamageEvent;
import com.ldtteam.shim.neoforge.event.entity.living.LivingDeathEvent;
import com.ldtteam.shim.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import com.ldtteam.shim.fabric.events.ShimDamageHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * NeoForge damage pipeline events (LivingIncomingDamageEvent, LivingDamageEvent.Pre/Post), LivingDeathEvent and ItemTossEvent.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements DamageContainerHolder
{
    @Unique
    private final Deque<DamageContainer> minecolonies$damageContainers = new ArrayDeque<>();
    @Override
    public Deque<DamageContainer> minecolonies$getDamageContainers()
    {
        return minecolonies$damageContainers;
    }

    /**
     * Pushes the damage container for this hurtServer call (popped at RETURN, also for vanilla's early returns).
     */
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void minecolonies$pushDamageContainer(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir)
    {
        minecolonies$damageContainers.push(new DamageContainer(source, damage));
    }

    /**
     * LivingIncomingDamageEvent, fired after vanilla's invulnerable / dead / fire resistance early returns like NeoForge;
     * cancelling aborts the damage.
     */
    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z", ordinal = 0), cancellable = true)
    private void minecolonies$incomingDamage(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir)
    {
        final DamageContainer container = minecolonies$damageContainers.peek();
        if (container != null && NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent((LivingEntity) (Object) this, container)).isCanceled())
        {
            minecolonies$damageContainers.pop();
            cir.setReturnValue(false);
        }
    }

    /**
     * Applies the amount set by LivingIncomingDamageEvent listeners (right after the vanilla invulnerability checks).
     */
    @ModifyVariable(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noActionTime:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD), argsOnly = true)
    private float minecolonies$applyIncomingDamage(final float damage)
    {
        final DamageContainer container = minecolonies$damageContainers.peek();
        return container == null ? damage : container.getNewDamage();
    }

    /**
     * Pops the damage container and fires LivingDamageEvent.Post when damage was dealt.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void minecolonies$damagePost(final ServerLevel level, final DamageSource source, final float damage, final CallbackInfoReturnable<Boolean> cir)
    {
        final DamageContainer container = minecolonies$damageContainers.poll();
        if (container != null && cir.getReturnValueZ())
        {
            NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post((LivingEntity) (Object) this, container));
        }
    }

    // Right before vanilla's `if (dmg != 0.0F)` (7th load of dmg, checked with javap on 26.3): NeoForge fires Pre also for 0 damage,
    // and a listener raising it must still apply.
    @ModifyVariable(method = "actuallyHurt", at = @At(value = "LOAD", ordinal = 6), argsOnly = true)
    private float minecolonies$livingDamagePre(final float dmg)
    {
        return ShimDamageHooks.fireDamagePre((LivingEntity) (Object) this, dmg);
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void minecolonies$onDeath(final DamageSource source, final CallbackInfo ci)
    {
        if ((Object) this instanceof ServerPlayer)
        {
            return; // handled by ServerPlayerMixin
        }
        if (NeoForge.EVENT_BUS.post(new LivingDeathEvent((LivingEntity) (Object) this, source)).isCanceled())
        {
            ci.cancel();
        }
    }

    @Inject(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), cancellable = true)
    private void minecolonies$onToss(final ItemStack itemStack, final boolean thrownFromHand, final Prediction prediction, final CallbackInfoReturnable<ItemEntity> cir,
        @Local final ItemEntity entity)
    {
        if ((Object) this instanceof ServerPlayer player && !player.isDeadOrDying())
        {
            if (NeoForge.EVENT_BUS.post(new ItemTossEvent(entity, player)).isCanceled())
            {
                cir.setReturnValue(null);
            }
        }
    }
}
