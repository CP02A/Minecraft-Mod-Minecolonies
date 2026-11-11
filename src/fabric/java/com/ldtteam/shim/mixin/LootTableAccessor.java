package com.ldtteam.shim.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Access to loot table pools (LootTableLoadEvent#addPool).
 */
@Mixin(LootTable.class)
public interface LootTableAccessor
{
    @Accessor("pools")
    List<LootPool> minecolonies$getPools();

    @Mutable
    @Accessor("pools")
    void minecolonies$setPools(List<LootPool> pools);
}
