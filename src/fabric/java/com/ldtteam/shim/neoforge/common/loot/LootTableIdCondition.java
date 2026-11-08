/*
 * Modelled after NeoForge 21.1.80 (net.neoforged.neoforge.common.loot.LootTableIdCondition), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim: vanilla LootContext does not know the queried table, so the id is
 * tracked by ShimLoot while loot modifiers run.
 */
package com.ldtteam.shim.neoforge.common.loot;

import com.ldtteam.shim.fabric.loot.ShimLoot;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code neoforge:loot_table_id} loot condition (NeoForge shim).
 */
public class LootTableIdCondition implements LootItemCondition
{
    public static final MapCodec<LootTableIdCondition> CODEC = RecordCodecBuilder.mapCodec(
        builder -> builder.group(Identifier.CODEC.fieldOf("loot_table_id").forGetter(idCondition -> idCondition.targetLootTableId))
            .apply(builder, LootTableIdCondition::new));

    public static final Identifier UNKNOWN_LOOT_TABLE = Identifier.fromNamespaceAndPath("neoforge", "unknown_loot_table");

    private final Identifier targetLootTableId;

    private LootTableIdCondition(final Identifier targetLootTableId)
    {
        this.targetLootTableId = targetLootTableId;
    }

    @Override
    public MapCodec<? extends LootItemCondition> codec()
    {
        return CODEC;
    }

    @Override
    public boolean test(final LootContext lootContext)
    {
        return ShimLoot.getQueriedLootTableId().equals(this.targetLootTableId);
    }

    public static Builder builder(final Identifier targetLootTableId)
    {
        return new Builder(targetLootTableId);
    }

    public static class Builder implements LootItemCondition.Builder
    {
        private final Identifier targetLootTableId;

        public Builder(final Identifier targetLootTableId)
        {
            if (targetLootTableId == null)
            {
                throw new IllegalArgumentException("Target loot table must not be null");
            }
            this.targetLootTableId = targetLootTableId;
        }

        @Override
        public LootItemCondition build()
        {
            return new LootTableIdCondition(this.targetLootTableId);
        }
    }
}
