/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public record TagEmptyCondition(TagKey<Item> tag) implements ICondition {
    public static final MapCodec<TagEmptyCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Identifier.CODEC.xmap(loc -> TagKey.create(Registries.ITEM, loc), TagKey::location).fieldOf("tag").forGetter(TagEmptyCondition::tag))
                    .apply(builder, TagEmptyCondition::new));

    public TagEmptyCondition(String location) {
        this(Identifier.parse(location));
    }

    public TagEmptyCondition(String namespace, String path) {
        this(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public TagEmptyCondition(Identifier tag) {
        this(TagKey.create(Registries.ITEM, tag));
    }

    @Override
    public boolean test(ICondition.IContext context) {
        return context.getTag(tag).isEmpty();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "tag_empty(\"" + tag.location() + "\")";
    }
}
