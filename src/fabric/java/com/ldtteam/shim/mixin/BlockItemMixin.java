package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.common.util.BlockSnapshot;
import com.ldtteam.shim.neoforge.event.level.BlockEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fires BlockEvent.EntityPlaceEvent after a block item placed its block on the server; cancelling restores the previous
 * block and makes the placement fail (item is not consumed).
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin
{
    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean shim$firePlaceEvent(final BlockItem self, final BlockPlaceContext context, final BlockState state, final Operation<Boolean> original)
    {
        final Level level = context.getLevel();
        if (level.isClientSide())
        {
            return original.call(self, context, state);
        }
        final BlockPos pos = context.getClickedPos();
        final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!original.call(self, context, state))
        {
            return false;
        }
        final BlockState placedAgainst = level.getBlockState(pos.relative(context.getClickedFace().getOpposite()));
        final BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot, placedAgainst, context.getPlayer());
        if (NeoForge.EVENT_BUS.post(event).isCanceled())
        {
            snapshot.restore(Block.UPDATE_ALL);
            if (context.getPlayer() instanceof ServerPlayer player)
            {
                player.containerMenu.sendAllDataToRemote();
                player.inventoryMenu.sendAllDataToRemote();
            }
            return false;
        }
        return true;
    }
}
