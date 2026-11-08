/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;

public final class TrueCondition implements ICondition {
    public static final TrueCondition INSTANCE = new TrueCondition();

    public static MapCodec<TrueCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private TrueCondition() {}

    @Override
    public boolean test(IContext context) {
        return true;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "true";
    }
}
