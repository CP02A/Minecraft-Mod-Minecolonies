// Adapted from NeoForge 21.1.80 (event/level/BlockEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event.level;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.bus.api.ICancellableEvent;
import com.ldtteam.shim.neoforge.common.util.BlockSnapshot;
import org.jetbrains.annotations.Nullable;

public abstract class BlockEvent extends Event {
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("neoforge.debugBlockEvent", "false"));

    private final LevelAccessor level;
    private final BlockPos pos;
    private final BlockState state;

    public BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) {
        this.pos = pos;
        this.level = level;
        this.state = state;
    }

    public LevelAccessor getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    /**
     * This event is fired on the server when a player attempts to break a block, upon receipt of a block break packet.
     *
     * The following conditions may cause this event to fire in a cancelled state:
     * <ul>
     * <li>If {@link Player#blockActionRestricted} is true.</li>
     * <li>If the target block is a {@link GameMasterBlock} and {@link Player#canUseGameMasterBlocks()} is false.</li>
     * <li>If the the player is holding an item, and {@link Item#canAttackBlock} is false.</li>
     * </ul>
     *
     * In the first two cases, un-cancelling the event will not permit the block to be broken.
     * In the third case, un-cancelling will allow the break, bypassing the behavior of {@link Item#canAttackBlock}.
     */
    public static class BreakEvent extends BlockEvent implements ICancellableEvent {
        private final Player player;

        public BreakEvent(Level level, BlockPos pos, BlockState state, Player player) {
            super(level, pos, state);
            this.player = player;
        }

        /**
         * {@return the player who is attempting to break the block}
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * Cancelling this event will prevent the block from being broken, and notifies the client of the refusal.
         */
        @Override
        public void setCanceled(boolean canceled) {
            ICancellableEvent.super.setCanceled(canceled);
        }
    }

    /**
     * Called when a block is placed.
     *
     * If a Block Place event is cancelled, the block will not be placed.
     */
    public static class EntityPlaceEvent extends BlockEvent implements ICancellableEvent {
        private final Entity entity;
        private final BlockSnapshot blockSnapshot;
        private final BlockState placedBlock;
        private final BlockState placedAgainst;

        public EntityPlaceEvent(BlockSnapshot blockSnapshot, BlockState placedAgainst, @Nullable Entity entity) {
            super(blockSnapshot.getLevel(), blockSnapshot.getPos(), !(entity instanceof Player) ? blockSnapshot.getState() : blockSnapshot.getCurrentState());
            this.entity = entity;
            this.blockSnapshot = blockSnapshot;
            this.placedBlock = !(entity instanceof Player) ? blockSnapshot.getState() : blockSnapshot.getCurrentState();
            this.placedAgainst = placedAgainst;

            if (DEBUG) {
                System.out.printf("Created EntityPlaceEvent - [PlacedBlock: %s ][PlacedAgainst: %s ][Entity: %s ]\n", getPlacedBlock(), placedAgainst, entity);
            }
        }

        @Nullable
        public Entity getEntity() {
            return entity;
        }

        public BlockSnapshot getBlockSnapshot() {
            return blockSnapshot;
        }

        public BlockState getPlacedBlock() {
            return placedBlock;
        }

        public BlockState getPlacedAgainst() {
            return placedAgainst;
        }
    }

    /**
     * Fired when a single block placement triggers the
     * creation of multiple blocks(e.g. placing a bed block). The block returned
     * by {@link #state} and its related methods is the block where
     * the placed block would exist if the placement only affected a single
     * block.
     */
    public static class EntityMultiPlaceEvent extends EntityPlaceEvent implements ICancellableEvent {
        private final List<BlockSnapshot> blockSnapshots;

        public EntityMultiPlaceEvent(List<BlockSnapshot> blockSnapshots, BlockState placedAgainst, @Nullable Entity entity) {
            super(blockSnapshots.get(0), placedAgainst, entity);
            this.blockSnapshots = ImmutableList.copyOf(blockSnapshots);
            if (DEBUG) {
                System.out.printf("Created EntityMultiPlaceEvent - [PlacedAgainst: %s ][Entity: %s ]\n", placedAgainst, entity);
            }
        }

        /**
         * Gets a list of BlockSnapshots for all blocks which were replaced by the
         * placement of the new blocks. Most of these blocks will just be of type AIR.
         *
         * @return immutable list of replaced BlockSnapshots
         */
        public List<BlockSnapshot> getReplacedBlockSnapshots() {
            return blockSnapshots;
        }
    }

    /**
     * Fired when a physics update occurs on a block. This event acts as
     * a way for mods to detect physics updates, in the same way a BUD switch
     * does. This event is only called on the server.
     */
    public static class NeighborNotifyEvent extends BlockEvent implements ICancellableEvent {
        private final EnumSet<Direction> notifiedSides;
        private final boolean forceRedstoneUpdate;

        public NeighborNotifyEvent(Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides, boolean forceRedstoneUpdate) {
            super(level, pos, state);
            this.notifiedSides = notifiedSides;
            this.forceRedstoneUpdate = forceRedstoneUpdate;
        }

        /**
         * Gets a list of directions from the base block that updates will occur upon.
         *
         * @return list of notified directions
         */
        public EnumSet<Direction> getNotifiedSides() {
            return notifiedSides;
        }

        /**
         * Get if redstone update was forced during setBlock call (0x16 to flags)
         * 
         * @return if the flag was set
         */
        public boolean getForceRedstoneUpdate() {
            return forceRedstoneUpdate;
        }
    }

    /**
     * Fired when a liquid places a block. Use {@link #setNewState(BlockState)} to change the result of
     * a cobblestone generator or add variants of obsidian. Alternatively, you could execute
     * arbitrary code when lava sets blocks on fire, even preventing it.
     *
     * {@link #getState()} will return the block that was originally going to be placed.
     * {@link #getPos()} will return the position of the block to be changed.
     */
    public static class FluidPlaceBlockEvent extends BlockEvent implements ICancellableEvent {
        private final BlockPos liquidPos;
        private BlockState newState;
        private BlockState origState;

        public FluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
            super(level, pos, state);
            this.liquidPos = liquidPos;
            this.newState = state;
            this.origState = level.getBlockState(pos);
        }

        /**
         * @return The position of the liquid this event originated from. This may be the same as {@link #getPos()}.
         */
        public BlockPos getLiquidPos() {
            return liquidPos;
        }

        /**
         * @return The block state that will be placed after this event resolves.
         */
        public BlockState getNewState() {
            return newState;
        }

        public void setNewState(BlockState state) {
            this.newState = state;
        }

        /**
         * @return The state of the block to be changed before the event was fired.
         */
        public BlockState getOriginalState() {
            return origState;
        }
    }

    /**
     * Fired when when farmland gets trampled
     * This event is {@link ICancellableEvent}
     */
    public static class FarmlandTrampleEvent extends BlockEvent implements ICancellableEvent {
        private final Entity entity;
        private final float fallDistance;

        public FarmlandTrampleEvent(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
            super(level, pos, state);
            this.entity = entity;
            this.fallDistance = fallDistance;
        }

        public Entity getEntity() {
            return entity;
        }

        public float getFallDistance() {
            return fallDistance;
        }
    }

    /**
     * Fired when an attempt is made to spawn a nether portal from
     * {@link BaseFireBlock#onPlace(BlockState, Level, BlockPos, BlockState, boolean)}.
     *
     * If cancelled, the portal will not be spawned.
     */
    public static class PortalSpawnEvent extends BlockEvent implements ICancellableEvent {
        private final PortalShape size;

        public PortalSpawnEvent(LevelAccessor level, BlockPos pos, BlockState state, PortalShape size) {
            super(level, pos, state);
            this.size = size;
        }

        public PortalShape getPortalSize() {
            return size;
        }
    }

}
