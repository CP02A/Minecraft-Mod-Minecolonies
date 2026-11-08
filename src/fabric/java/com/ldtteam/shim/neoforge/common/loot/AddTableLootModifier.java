/*
 * Modelled after NeoForge 21.1.80 (net.neoforged.neoforge.common.loot.AddTableLootModifier), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim.
 */
package com.ldtteam.shim.neoforge.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code neoforge:add_table}: rolls another loot table and adds its drops (NeoForge shim).
 */
public class AddTableLootModifier extends LootModifier
{
    public static final MapCodec<AddTableLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(glm -> glm.conditions),
        ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter(AddTableLootModifier::table)).apply(instance, AddTableLootModifier::new));

    private final ResourceKey<LootTable> table;

    public AddTableLootModifier(final LootItemCondition[] conditionsIn, final ResourceKey<LootTable> table)
    {
        super(conditionsIn);
        this.table = table;
    }

    public ResourceKey<LootTable> table()
    {
        return this.table;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot, final LootContext context)
    {
        context.getResolver().get(this.table).ifPresent(extraTable ->
            extraTable.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add)));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec()
    {
        return CODEC;
    }
}
