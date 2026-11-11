package com.minecolonies.fabric.mixin.api;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * FABRIC-PORT: 26.x requires BlockBehaviour.Properties#setId before a block is constructed. MineColonies blocks only know their
 * registry name after construction, so a block constructed without an id gets placeholder drops/description here, which
 * {@link com.minecolonies.api.blocks.BlockIdHelper#assignId} replaces when the block is registered.
 * <p>
 * Uses {@link WrapOperation} rather than {@code @Redirect} so other mods can wrap the same calls; blocks that have an id
 * go through the original call unchanged.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourIdMixin
{
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;effectiveDrops()Ljava/util/Optional;"))
    private Optional<ResourceKey<LootTable>> minecolonies$lenientDrops(final BlockBehaviour.Properties properties, final Operation<Optional<ResourceKey<LootTable>>> original)
    {
        if (((BlockPropertiesAccessor) properties).minecolonies$getId() == null)
        {
            return Optional.empty();
        }
        return original.call(properties);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;effectiveDescriptionId()Ljava/lang/String;"))
    private String minecolonies$lenientDescriptionId(final BlockBehaviour.Properties properties, final Operation<String> original)
    {
        if (((BlockPropertiesAccessor) properties).minecolonies$getId() == null)
        {
            return "block.minecolonies.unregistered";
        }
        return original.call(properties);
    }
}
