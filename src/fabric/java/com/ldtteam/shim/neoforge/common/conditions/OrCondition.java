/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.google.common.base.Joiner;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record OrCondition(List<ICondition> values) implements ICondition {
    public static final MapCodec<OrCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            LIST_CODEC.fieldOf("values").forGetter(OrCondition::values))
                    .apply(builder, OrCondition::new));

    @Override
    public boolean test(IContext context) {
        for (ICondition child : values()) {
            if (child.test(context))
                return true;
        }

        return false;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return Joiner.on(" || ").join(values());
    }
}
