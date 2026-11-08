package com.minecolonies.fabric.mixin.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

/**
 * Access to the block id of BlockBehaviour.Properties (see {@link BlockBehaviourIdMixin}).
 */
@Mixin(BlockBehaviour.Properties.class)
public interface BlockPropertiesAccessor
{
    @Accessor("id")
    ResourceKey<Block> minecolonies$getId();

    @Invoker("effectiveDrops")
    Optional<ResourceKey<LootTable>> minecolonies$effectiveDrops();

    @Invoker("effectiveDescriptionId")
    String minecolonies$effectiveDescriptionId();
}
