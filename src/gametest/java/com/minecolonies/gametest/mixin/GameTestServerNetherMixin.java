package com.minecolonies.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;
import java.util.Map;

/**
 * Test server only: the Nether gets vanilla's real noise generator (from the "normal" world preset) instead of the flat Nether of the
 * game test preset {@code flat_all_dimensions}. The overworld (where all other tests run) and the End stay flat. PRT-01 needs one
 * dimension with worldgen material (surface) rules; the Nether is only generated where a test loads chunks.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerNetherMixin
{
    @ModifyExpressionValue(
        method = {"lambda$create$0", "lambda$create$1"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/presets/WorldPreset;createWorldDimensions()Lnet/minecraft/world/level/levelgen/WorldDimensions;")
    )
    private static WorldDimensions minecoloniesGametest$noiseNether(final WorldDimensions flat, @Local(argsOnly = true) final WorldLoader.DataLoadContext context)
    {
        final WorldDimensions normal = context.datapackWorldRegistries()
                                         .lookupOrThrow(Registries.WORLD_PRESET)
                                         .getOrThrow(WorldPresets.NORMAL)
                                         .value()
                                         .createWorldDimensions();
        final Map<ResourceKey<LevelStem>, LevelStem> dimensions = new HashMap<>(flat.dimensions());
        dimensions.put(LevelStem.NETHER, normal.dimensions().get(LevelStem.NETHER));
        return new WorldDimensions(dimensions);
    }
}
