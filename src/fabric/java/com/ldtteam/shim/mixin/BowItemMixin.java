package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.event.EventHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ArrowNockEvent / ArrowLooseEvent for the vanilla bow.
 * FABRIC-PORT TODO: a charge changed by ArrowLooseEvent listeners is not applied (only cancellation is honoured).
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin
{
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void minecolonies$arrowNock(final Level level, final Player player, final InteractionHand hand, final CallbackInfoReturnable<InteractionResult> cir)
    {
        final ItemStack stack = player.getItemInHand(hand);
        final InteractionResult result = EventHooks.onArrowNock(stack, level, player, hand, !player.getProjectile(stack).isEmpty());
        if (result != null)
        {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void minecolonies$arrowLoose(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime, final CallbackInfoReturnable<Boolean> cir)
    {
        if (entity instanceof Player player)
        {
            final int charge = ((BowItem) (Object) this).getUseDuration(itemStack, entity) - remainingTime;
            if (EventHooks.onArrowLoose(itemStack, level, player, charge, !player.getProjectile(itemStack).isEmpty()) < 0)
            {
                cir.setReturnValue(false);
            }
        }
    }
}
