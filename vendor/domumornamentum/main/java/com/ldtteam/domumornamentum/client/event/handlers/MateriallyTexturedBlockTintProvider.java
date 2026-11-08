package com.ldtteam.domumornamentum.client.event.handlers;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.client.model.RetexturingHandler;
import com.ldtteam.domumornamentum.client.model.block.MateriallyTexturedBlockStateModel;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.retexturing.TargetTextureSource;
import com.ldtteam.domumornamentum.util.Constants;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockTintsFactory;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

/**
 * Replacement for the old {@code BlockColor}/{@code ItemColor} handlers.
 *
 * <p>Retextured quads carry a {@code MaterialInfo.tintIndex} that indexes into this block instance's dynamic tint
 * list. Fabric fills that list through the {@link BlockTintsFactory} registered here (Fabric equivalent of NeoForge's
 * {@code IClientBlockExtensions#collectDynamicTintValues}). Each entry resolves the original tint source of whatever
 * block state the slot currently mimics, so biome tints and other dynamic target colors follow the mapping
 * automatically. Item tints are handled by {@link com.ldtteam.domumornamentum.client.model.item.MateriallyTexturedItemModel}.
 */
public final class MateriallyTexturedBlockTintProvider {

    private static final BlockTintsFactory TINTS = (final BlockState state, final BlockAndTintGetter level, final BlockPos pos, final IntList tintValues) -> {
        final MaterialTextureData textureData = MateriallyTexturedBlockStateModel.textureDataAt(level, pos);
        if (textureData.isEmpty()) {
            //No texture data: retexturing is inactive and no dynamic tints are needed for this instance.
            return;
        }

        for (final TargetTextureSource source : RetexturingHandler.targetsFor(textureData)) {
            if (source.originalTintIndex() == -1) {
                tintValues.add(-1);
                continue;
            }

            final BlockTintSource tintSource = Minecraft.getInstance().getBlockColors()
                    .getTintSource(source.targetBlockState(), source.originalTintIndex());

            //Resolve the target's own tint in our world position, so e.g. grass keeps its biome color.
            tintValues.add(tintSource != null ? tintSource.colorInWorld(source.targetBlockState(), level, pos) : -1);
        }
    };

    private MateriallyTexturedBlockTintProvider() {
    }

    /**
     * Registers the tint factory for all materially textured blocks and the cache-clearing reload listener.
     * Must run on the client after block registration.
     */
    public static void register() {
        final Block[] blocks = BuiltInRegistries.BLOCK.stream()
                .filter(IMateriallyTexturedBlock.class::isInstance)
                .toArray(Block[]::new);

        if (blocks.length > 0) {
            BlockColorRegistry.register(TINTS, blocks);
        }

        //Sprites and tint sources change with reloads; cached resolved targets must not outlive them.
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "retexturing_cache"),
                new SimplePreparableReloadListener<Unit>() {
                    @Override
                    protected @NonNull Unit prepare(final @NonNull ResourceManager manager, final @NonNull ProfilerFiller profiler) {
                        RetexturingHandler.clearCache();
                        return Unit.INSTANCE;
                    }

                    @Override
                    protected void apply(final @NonNull Unit preparations, final @NonNull ResourceManager manager, final @NonNull ProfilerFiller profiler) {
                        RetexturingHandler.clearCache();
                    }
                });
    }
}
