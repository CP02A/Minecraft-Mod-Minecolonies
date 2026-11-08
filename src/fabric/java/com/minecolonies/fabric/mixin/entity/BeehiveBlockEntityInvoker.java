package com.minecolonies.fabric.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Exposes BeehiveBlockEntity#releaseAllOccupants (was public through the NeoForge access transformer).
 */
@Mixin(BeehiveBlockEntity.class)
public interface BeehiveBlockEntityInvoker
{
    @Invoker("releaseAllOccupants")
    List<Entity> minecolonies$releaseAllOccupants(BlockState state, BeehiveBlockEntity.BeeReleaseStatus releaseStatus);
}
