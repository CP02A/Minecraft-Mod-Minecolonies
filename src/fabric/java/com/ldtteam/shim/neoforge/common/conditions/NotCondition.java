/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NotCondition(ICondition value) implements ICondition {
    public static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            ICondition.CODEC.fieldOf("value").forGetter(NotCondition::value))
                    .apply(builder, NotCondition::new));

    @Override
    public boolean test(IContext context) {
        return !value.test(context);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "!" + value;
    }
}
