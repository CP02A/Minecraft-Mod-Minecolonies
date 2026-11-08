package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.common.util.TriState;
import com.ldtteam.shim.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemEntityPickupEvent.Pre: a FALSE result prevents the pickup.
 * FABRIC-PORT TODO: TRUE (force pickup ignoring delay/owner) is treated like DEFAULT; ItemEntityPickupEvent.Post is not fired.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin
{
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void shim$pickupPre(final Player player, final CallbackInfo ci)
    {
        final ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide())
        {
            return;
        }
        final ItemEntityPickupEvent.Pre event = new ItemEntityPickupEvent.Pre(player, self);
        NeoForge.EVENT_BUS.post(event);
        if (event.canPickup() == TriState.FALSE)
        {
            ci.cancel();
        }
    }
}
