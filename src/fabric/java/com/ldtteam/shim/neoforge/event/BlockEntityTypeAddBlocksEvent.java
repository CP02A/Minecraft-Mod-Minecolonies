// Adapted from NeoForge 26.3.0.101 (event/BlockEntityTypeAddBlocksEvent.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.event;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.mixin.BlockEntityTypeAccessor;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import org.jspecify.annotations.Nullable;

/**
 * Allows injecting new blocks into a block entity's {@link BlockEntityType#validBlocks} field in a safe manner.
 * The class of the newly injected block should share the highest common class that all existing blocks in the targeted validBlocks has.
 * Please use this event instead of manipulating {@link BlockEntityType} directly.
 * <p>
 * Example: If the valid blocks list has {@linkplain StandingSignBlock} entry and {@linkplain WallSignBlock} entry, the common class is {@linkplain SignBlock},
 * the given block must be a {@linkplain SignBlock} or have {@link SignBlock} as a parent class in its hierarchy. Example:
 * {@snippet :
 * public static void onBlockEntityValidBlocks(BlockEntityTypeAddBlocksEvent event) {
 *     event.modify(BlockEntityType.SIGN, MODDED_SIGN_BLOCK.get());
 * }
 * }
 */
public class BlockEntityTypeAddBlocksEvent extends Event implements IModBusEvent {
    private final Function<BlockEntityType<?>, ? extends Class<?>> memoizedCommonSuperClass = Util.memoize((BlockEntityType<?> blockEntityType) -> getCommonSuperClassForExistingValidBlocks(((BlockEntityTypeAccessor) blockEntityType).minecolonies$getValidBlocks()));

    public BlockEntityTypeAddBlocksEvent() {}

    /**
     * Will add the given blocks to the provided {@link BlockEntityType}'s set of valid blocks.
     */
    public void modify(BlockEntityType<?> blockEntityType, Block... blocksToAdd) {
        if (blocksToAdd.length == 0) {
            return;
        }

        Set<Block> currentValidBlocks = new HashSet<>(((BlockEntityTypeAccessor) blockEntityType).minecolonies$getValidBlocks());

        for (Block block : blocksToAdd) {
            addValidBlock(block, memoizedCommonSuperClass.apply(blockEntityType), currentValidBlocks);
        }

        // Fabric API allows adding valid blocks (it makes the set mutable as needed).
        final Set<Block> existing = ((BlockEntityTypeAccessor) blockEntityType).minecolonies$getValidBlocks();
        for (Block block : currentValidBlocks) {
            if (!existing.contains(block)) {
                ((FabricBlockEntityType) blockEntityType).addValidBlock(block);
            }
        }
    }

    /**
     * Will add the given blocks to the {@link BlockEntityType}'s set of valid blocks.
     */
    public void modify(ResourceKey<BlockEntityType<?>> blockEntityTypeKey, Block... blocksToAdd) {
        BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(blockEntityTypeKey)
                .ifPresent(blockEntityType -> modify(blockEntityType, blocksToAdd));
    }

    /**
     * Will add the given blocks to the matching {@link BlockEntityType}'s set of valid blocks.
     */
    public void modify(BiPredicate<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>> blockEntityTypeToMatch, Block... blocksToAdd) {
        for (Map.Entry<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>> blockEntityTypeEntry : BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
            if (blockEntityTypeToMatch.test(blockEntityTypeEntry.getKey(), blockEntityTypeEntry.getValue())) {
                modify(blockEntityTypeEntry.getValue(), blocksToAdd);
            }
        }
    }

    private void addValidBlock(Block block, @Nullable Class<?> baseClass, Set<Block> currentValidBlocks) {
        if (baseClass == null || baseClass.isAssignableFrom(block.getClass())) {
            currentValidBlocks.add(block);
        } else {
            throw new IllegalArgumentException("Given block " + block + " does not derive from existing valid block's common superclass of " + baseClass);
        }
    }

    @Nullable
    private Class<?> getCommonSuperClassForExistingValidBlocks(Set<Block> validBlocks) {
        Class<?> calculatedBaseClass = null;

        for (Block existingBlock : validBlocks) {
            if (calculatedBaseClass != null) {
                calculatedBaseClass = findClosestCommonSuper(calculatedBaseClass, existingBlock.getClass());
            } else {
                calculatedBaseClass = existingBlock.getClass();
            }
        }

        return calculatedBaseClass;
    }

    private static Class<?> findClosestCommonSuper(Class<?> superClass, Class<?> childClass) {
        while (!superClass.isAssignableFrom(childClass)) {
            superClass = superClass.getSuperclass();
        }
        return superClass;
    }
}
