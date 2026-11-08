package com.ldtteam.domumornamentum.client.model.item;

import com.google.common.base.Suppliers;
import com.ldtteam.domumornamentum.client.model.RetexturingHandler;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.retexturing.TargetTextureSource;
import com.ldtteam.domumornamentum.component.ModDataComponents;
import com.ldtteam.domumornamentum.util.MaterialTextureDataUtil;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Item model for materially textured blocks: {@code {"type": "domum_ornamentum:materially_textured", "model": "..."}}.
 *
 * <p>Works like vanilla's {@code minecraft:model} item model, but retextures the model's quads with the
 * {@link MaterialTextureData} stored in the stack's {@code domum_ornamentum:texture_data} component (the same
 * retexturing as the block model, see {@link RetexturingHandler}). Stacks without texture data cycle through random
 * valid materials, like the 1.21 item renderer did.
 */
public final class MateriallyTexturedItemModel implements ItemModel
{
    private static final int MAX_CACHED = 256;

    private final List<BakedQuad> baseQuads;
    private final ItemQuads baseItemQuads;
    private final boolean animated;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Map<MaterialTextureData, ItemQuads> cache = new ConcurrentHashMap<>();

    private MateriallyTexturedItemModel(final QuadCollection quads, final ModelRenderProperties properties, final Matrix4fc transformation)
    {
        this.baseQuads = quads.getAll();
        this.baseItemQuads = ItemQuads.split(baseQuads);
        this.animated = quads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED);
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(baseQuads));
        this.properties = properties;
        this.transformation = transformation;
    }

    @Override
    public void update(
        final ItemStackRenderState output,
        final ItemStack item,
        final ItemModelResolver resolver,
        final ItemDisplayContext displayContext,
        final @Nullable ClientLevel level,
        final @Nullable ItemOwner owner,
        final int seed)
    {
        MaterialTextureData data = item.getOrDefault(ModDataComponents.TEXTURE_DATA.get(), MaterialTextureData.EMPTY);
        if (data.isEmpty())
        {
            // no material chosen yet: preview cycling random materials (changes once per second)
            data = MaterialTextureDataUtil.generateRandomTextureDataFrom(item);
            output.setAnimated();
        }

        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(data);

        final ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil())
        {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.setAnimated();
            output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }

        ItemQuads quads = baseItemQuads;
        if (!data.isEmpty())
        {
            // tint index i of retextured quads refers to target i of the mapping
            final IntList tintLayers = layer.tintLayers();
            for (final TargetTextureSource source : RetexturingHandler.targetsFor(data))
            {
                int tint = -1;
                if (source.originalTintIndex() != -1)
                {
                    final BlockTintSource tintSource = Minecraft.getInstance().getBlockColors()
                        .getTintSource(source.targetBlockState(), source.originalTintIndex());
                    if (tintSource != null)
                    {
                        tint = tintSource.color(source.targetBlockState());
                    }
                }
                tintLayers.add(tint);
                output.appendModelIdentityElement(tint);
            }

            if (cache.size() >= MAX_CACHED)
            {
                cache.clear();
            }
            quads = cache.computeIfAbsent(data, d -> ItemQuads.split(RetexturingHandler.retexturedQuads(baseQuads, d)));
            if (quads.all().stream().anyMatch(q -> q.materialInfo().sprite().contents().isAnimated()))
            {
                output.setAnimated(); // animated target sprite
            }
        }

        layer.setExtents(extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, displayContext);
        layer.setQuads(quads);
        if (animated)
        {
            output.setAnimated();
        }
    }

    /**
     * Drops cached retextured quads (resource reload).
     */
    public void clearCache()
    {
        cache.clear();
    }

    public record Unbaked(Identifier model, Optional<Transformation> transformation) implements ItemModel.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
                    Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation)
                )
                .apply(i, Unbaked::new)
        );

        @Override
        public void resolveDependencies(final ResolvableModel.Resolver resolver)
        {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(final ItemModel.BakingContext context, final Matrix4fc transformation)
        {
            final ModelBaker baker = context.blockModelBaker();
            final ResolvedModel resolvedModel = baker.getModel(model);
            final TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
            final QuadCollection quads = resolvedModel.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
            final ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
            final Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            return new MateriallyTexturedItemModel(quads, properties, modelTransform);
        }

        @Override
        public MapCodec<Unbaked> type()
        {
            return MAP_CODEC;
        }
    }
}
