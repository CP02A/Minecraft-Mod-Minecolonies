package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.common.util.BlockSnapshot;
import com.ldtteam.shim.neoforge.event.level.BlockEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge fires BlockEvent.EntityPlaceEvent for blocks placed by any item use (flint and steel fire, fire charges, ...), not only
 * block items. Block items are handled in {@link BlockItemMixin}; for other items (except buckets, like NeoForge) the clicked block
 * and the block in front of it are snapshotted, and a cancelled place event restores them and the used item.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin
{
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult shim$firePlaceEventForItems(final Item item, final UseOnContext context, final Operation<InteractionResult> original)
    {
        final Level level = context.getLevel();
        if (level.isClientSide() || item instanceof BlockItem || item instanceof BucketItem)
        {
            return original.call(item, context);
        }

        final BlockPos clicked = context.getClickedPos();
        final BlockPos front = clicked.relative(context.getClickedFace());
        final BlockSnapshot clickedSnapshot = BlockSnapshot.create(level.dimension(), level, clicked);
        final BlockSnapshot frontSnapshot = BlockSnapshot.create(level.dimension(), level, front);
        final ItemStack before = context.getItemInHand().copy();

        final InteractionResult result = original.call(item, context);
        if (!result.consumesAction())
        {
            return result;
        }

        final Player player = context.getPlayer();
        boolean cancelled = false;
        if (frontSnapshot.getCurrentState() != frontSnapshot.getState())
        {
            cancelled = NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(frontSnapshot, level.getBlockState(clicked), player)).isCanceled();
        }
        if (!cancelled && clickedSnapshot.getCurrentState() != clickedSnapshot.getState())
        {
            cancelled = NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(clickedSnapshot, level.getBlockState(clicked.below()), player)).isCanceled();
        }
        if (!cancelled)
        {
            return result;
        }

        frontSnapshot.restore(Block.UPDATE_ALL);
        clickedSnapshot.restore(Block.UPDATE_ALL);
        if (player != null)
        {
            player.setItemInHand(context.getHand(), before);
            if (player instanceof ServerPlayer serverPlayer)
            {
                serverPlayer.containerMenu.sendAllDataToRemote();
                serverPlayer.inventoryMenu.sendAllDataToRemote();
            }
        }
        return InteractionResult.FAIL;
    }
}
