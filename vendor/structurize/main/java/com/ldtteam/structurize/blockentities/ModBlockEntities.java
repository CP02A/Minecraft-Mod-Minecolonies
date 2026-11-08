package com.ldtteam.structurize.blockentities;

import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.blocks.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.ldtteam.shim.neoforge.registries.DeferredHolder;
import com.ldtteam.shim.neoforge.registries.DeferredRegister;

public final class ModBlockEntities
{
    private ModBlockEntities() { /* prevent construction */ }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockEntityTagSubstitution>> TAG_SUBSTITUTION = BLOCK_ENTITIES.register("tagsubstitution",
      () -> new BlockEntityType<>(BlockEntityTagSubstitution::new, java.util.Set.of(ModBlocks.blockTagSubstitution.get())));
}
