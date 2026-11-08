package com.ldtteam.domumornamentum.client.model.block;

import com.ldtteam.domumornamentum.client.model.RetexturingHandler;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.properties.ModProperties;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import com.ldtteam.shim.neoforge.client.model.DynamicBlockStateModel;
import com.ldtteam.shim.neoforge.model.data.ModelDataHelper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Retextures the parts of its parent model with the texture mapping of the block entity at the rendered position.
 *
 * <p>Fabric: the mapping is read through {@code BlockGetter#getBlockEntityRenderData} (the block entity returns its
 * {@link com.ldtteam.shim.neoforge.model.data.ModelData} as render data), which is safe during threaded chunk meshing;
 * geometry is emitted through {@link DynamicBlockStateModel#emitQuads}.
 */
public class MateriallyTexturedBlockStateModel implements DynamicBlockStateModel
{
    private final BlockStateModel parent;

    public MateriallyTexturedBlockStateModel(final BlockStateModel parent) {this.parent = parent;}

    /**
     * @return the texture mapping at the given position, or {@link MaterialTextureData#EMPTY}.
     */
    public static MaterialTextureData textureDataAt(final @Nullable BlockAndTintGetter level, final BlockPos pos)
    {
        final MaterialTextureData data = ModelDataHelper.getModelData(level, pos).get(ModProperties.MATERIAL_TEXTURE_PROPERTY);
        return data == null ? MaterialTextureData.EMPTY : data;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Material.@NonNull Baked particleMaterial()
    {
        return parent.particleMaterial();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @BakedQuad.MaterialFlags int materialFlags()
    {
        // retexturing may introduce translucent / animated target sprites
        return parent.materialFlags() | BakedQuad.FLAG_TRANSLUCENT | BakedQuad.FLAG_ANIMATED;
    }

    @Override
    public @Nullable Object createGeometryKey(final BlockAndTintGetter level, final @NonNull BlockPos pos, final @NonNull BlockState state, final @NonNull RandomSource random)
    {
        final Object parentKey = parent.createGeometryKey(level, pos, state, random);
        if (parentKey == null)
        {
            return null;
        }
        return new GeometryKey(this, parentKey, textureDataAt(level, pos));
    }

    @Override
    public Material.@NonNull Baked particleMaterial(final @NonNull BlockAndTintGetter level, final @NonNull BlockPos pos, final @NonNull BlockState state)
    {
        return parent.particleMaterial(level, pos, state);
    }

    @Override
    public void collectParts(final @NonNull BlockAndTintGetter level, final @NonNull BlockPos pos, final @NonNull BlockState state, final @NonNull RandomSource random, final @NonNull List<BlockStateModelPart> parts)
    {
        final var parentParts = new ArrayList<BlockStateModelPart>();
        parent.collectParts(random, parentParts);

        //retexturedParts passes the parent parts through unchanged when the mapping is empty
        parts.addAll(RetexturingHandler.retexturedParts(parentParts, textureDataAt(level, pos), pos, level));
    }

    private record GeometryKey(MateriallyTexturedBlockStateModel model, Object parentKey, MaterialTextureData data) {}
}
