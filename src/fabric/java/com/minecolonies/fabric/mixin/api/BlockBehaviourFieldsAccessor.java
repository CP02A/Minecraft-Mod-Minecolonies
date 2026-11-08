package com.minecolonies.fabric.mixin.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/**
 * Setters for the id-derived final fields of BlockBehaviour (see {@link BlockBehaviourIdMixin}).
 */
@Mixin(BlockBehaviour.class)
public interface BlockBehaviourFieldsAccessor
{
    @Mutable
    @Accessor("drops")
    void minecolonies$setDrops(Optional<ResourceKey<LootTable>> drops);

    @Mutable
    @Accessor("descriptionId")
    void minecolonies$setDescriptionId(String descriptionId);
}
