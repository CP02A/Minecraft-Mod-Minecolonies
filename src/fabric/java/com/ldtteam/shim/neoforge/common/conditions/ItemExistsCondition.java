/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ItemExistsCondition implements ICondition {
    public static MapCodec<ItemExistsCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Identifier.CODEC.fieldOf("item").forGetter(ItemExistsCondition::getItem))
                    .apply(builder, ItemExistsCondition::new));

    private final Identifier item;

    public ItemExistsCondition(String location) {
        this(Identifier.parse(location));
    }

    public ItemExistsCondition(String namespace, String path) {
        this(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public ItemExistsCondition(Identifier item) {
        this.item = item;
    }

    @Override
    public boolean test(IContext context) {
        return BuiltInRegistries.ITEM.containsKey(item);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    public Identifier getItem() {
        return item;
    }

    @Override
    public String toString() {
        return "item_exists(\"" + item + "\")";
    }
}
