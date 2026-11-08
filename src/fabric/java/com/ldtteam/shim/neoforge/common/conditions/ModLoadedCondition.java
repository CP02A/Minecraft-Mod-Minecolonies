/*
 * Copied from NeoForge 21.1.80 (net.neoforged.neoforge.common.conditions), LGPL-2.1-only.
 * Adapted for the MineColonies Fabric shim (26.3 names, shim registries).
 */
package com.ldtteam.shim.neoforge.common.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ldtteam.shim.fml.ModList;

public record ModLoadedCondition(String modid) implements ICondition {
    public static MapCodec<ModLoadedCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Codec.STRING.fieldOf("modid").forGetter(ModLoadedCondition::modid))
                    .apply(builder, ModLoadedCondition::new));

    @Override
    public boolean test(IContext context) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "mod_loaded(\"" + modid + "\")";
    }
}
