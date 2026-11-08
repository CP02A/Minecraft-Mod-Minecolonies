package com.ldtteam.shim.neoforge.client.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * NeoForge shim: a {@link BlockStateModel} whose parts depend on the level context.
 *
 * <p>Implemented on top of the Fabric renderer API: {@link #emitQuads} (called by Fabric/Indigo for chunk meshing,
 * block entity/falling block rendering, etc.) collects the level-aware parts and emits them. Implementations should
 * read per-block-entity data via {@code level.getBlockEntityRenderData(pos)} (see
 * {@link com.ldtteam.shim.neoforge.model.data.ModelDataHelper}) and also override {@code createGeometryKey} and
 * {@code particleMaterial(level, pos, state)} (both inherited from Fabric's {@code FabricBlockStateModel}, same
 * signatures as NeoForge).
 */
public interface DynamicBlockStateModel extends BlockStateModel
{
    /**
     * Context-free collection (item-like rendering without a level): uses an empty level.
     */
    @Override
    @Deprecated
    default void collectParts(final RandomSource random, final List<BlockStateModelPart> parts)
    {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
    }

    void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts);

    @Override
    default void emitQuads(
        final QuadEmitter emitter,
        final BlockAndTintGetter level,
        final BlockPos pos,
        final BlockState state,
        final RandomSource random,
        final Predicate<@Nullable Direction> cullTest)
    {
        final List<BlockStateModelPart> parts = new ArrayList<>();
        collectParts(level, pos, state, random, parts);
        for (final BlockStateModelPart part : parts)
        {
            part.emitQuads(emitter, cullTest);
        }
    }
}
