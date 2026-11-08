package com.ldtteam.shim.neoforge.client.model.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/**
 * NeoForge shim: custom unbaked block state model type, backed by Fabric's
 * {@link net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel}.
 *
 * <p>Register the codec with {@link #register(Identifier, MapCodec)} during client init (replaces NeoForge's
 * {@code RegisterBlockStateModels} event). In blockstate JSON the type key is {@code "fabric:type"} (NeoForge used
 * {@code "type"}).
 */
public interface CustomUnbakedBlockStateModel extends net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel, BlockStateModel.Unbaked
{
    @Override
    MapCodec<? extends CustomUnbakedBlockStateModel> codec();

    static void register(final Identifier id, final MapCodec<? extends CustomUnbakedBlockStateModel> codec)
    {
        net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel.register(id, codec);
    }
}
