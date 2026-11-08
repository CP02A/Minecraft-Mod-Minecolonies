/*
 * Modelled after NeoForge 21.1.80 (net.neoforged.neoforge.common.loot.IGlobalLootModifier), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim.
 */
package com.ldtteam.shim.neoforge.common.loot;

import com.ldtteam.shim.neoforge.common.conditions.ConditionalOps;
import com.ldtteam.shim.neoforge.common.conditions.WithConditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A global loot modifier (NeoForge shim). Applied by {@link com.ldtteam.shim.fabric.loot.ShimLoot} on Fabric's MODIFY_DROPS.
 */
public interface IGlobalLootModifier
{
    Codec<IGlobalLootModifier> DIRECT_CODEC = GlobalLootModifierCodecs.CODEC_TYPE.dispatch(IGlobalLootModifier::codec, Function.identity());

    Codec<Optional<WithConditions<IGlobalLootModifier>>> CONDITIONAL_CODEC = ConditionalOps.createConditionalCodecWithConditions(DIRECT_CODEC);

    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = LootItemCondition.DIRECT_CODEC.listOf().xmap(list -> list.toArray(LootItemCondition[]::new), List::of);

    /**
     * Applies the modifier to the generated loot (all loot conditions have already been checked and have returned true).
     */
    ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context);

    MapCodec<? extends IGlobalLootModifier> codec();
}
