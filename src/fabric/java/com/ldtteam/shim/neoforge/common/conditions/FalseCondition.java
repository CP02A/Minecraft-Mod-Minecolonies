/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.MapCodec;

public final class FalseCondition implements ICondition {
    public static final FalseCondition INSTANCE = new FalseCondition();

    public static final MapCodec<FalseCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    private FalseCondition() {}

    @Override
    public boolean test(IContext condition) {
        return false;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    public String toString() {
        return "false";
    }
}
