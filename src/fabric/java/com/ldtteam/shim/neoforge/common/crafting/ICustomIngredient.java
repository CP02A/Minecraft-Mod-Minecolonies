// Adapted from NeoForge 26.3 (net.neoforged.neoforge.common.crafting.ICustomIngredient), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.crafting;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

/**
 * NeoForge custom ingredient, implemented on top of Fabric's {@link CustomIngredient}.
 * {@link #toVanilla()} (inherited from Fabric) wraps it into a vanilla {@link net.minecraft.world.item.crafting.Ingredient}.
 * The {@link IngredientType} must be registered (through the NeoForge ingredient type registry or {@link IngredientType#register}) so that
 * Fabric can (de)serialize it.
 */
public interface ICustomIngredient extends CustomIngredient
{
    @Override
    boolean test(ItemStack stack);

    @Override
    Stream<Holder<Item>> items();

    /**
     * @return true if the ingredient only depends on the item (not on components/count) and can be fully described by {@link #items()}.
     */
    boolean isSimple();

    IngredientType<?> getType();

    @Override
    default boolean requiresTesting()
    {
        return !isSimple();
    }

    @Override
    default CustomIngredientSerializer<?> getSerializer()
    {
        return getType().serializer();
    }
}
