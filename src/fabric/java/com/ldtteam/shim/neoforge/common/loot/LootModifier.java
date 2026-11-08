/*
 * Modelled after NeoForge 21.1.80 (net.neoforged.neoforge.common.loot.LootModifier), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim.
 */
package com.ldtteam.shim.neoforge.common.loot;

import com.mojang.datafixers.Products;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Base for loot modifiers with loot conditions (NeoForge shim).
 */
public abstract class LootModifier implements IGlobalLootModifier
{
    protected final LootItemCondition[] conditions;

    protected static <T extends LootModifier> Products.P1<RecordCodecBuilder.Mu<T>, LootItemCondition[]> codecStart(final RecordCodecBuilder.Instance<T> instance)
    {
        return instance.group(LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions));
    }

    protected LootModifier(final LootItemCondition[] conditionsIn)
    {
        this.conditions = conditionsIn;
    }

    @Override
    public final ObjectArrayList<ItemStack> apply(final ObjectArrayList<ItemStack> generatedLoot, final LootContext context)
    {
        for (final LootItemCondition condition : conditions)
        {
            if (!condition.test(context))
            {
                return generatedLoot;
            }
        }
        return this.doApply(generatedLoot, context);
    }

    protected abstract ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context);
}
