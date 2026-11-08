package com.ldtteam.shim.neoforge.client;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * NeoForge shim: extra render types, mapped onto the closest 26.3 vanilla render types.
 *
 * <p>The enum constants mirror NeoForge 1.21's {@code NeoForgeRenderTypes} (block-atlas, item-style render types);
 * the static methods mirror NeoForge 26.x.
 */
public enum NeoForgeRenderTypes implements Supplier<RenderType>
{
    ITEM_LAYERED_SOLID(() -> RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS)),
    ITEM_LAYERED_CUTOUT(Sheets::cutoutBlockItemSheet),
    ITEM_LAYERED_CUTOUT_MIPPED(Sheets::cutoutBlockItemSheet),
    ITEM_LAYERED_TRANSLUCENT(Sheets::translucentBlockItemSheet),
    ITEM_UNSORTED_TRANSLUCENT(Sheets::translucentBlockItemSheet),
    ITEM_UNLIT_TRANSLUCENT(Sheets::translucentBlockItemSheet),
    ITEM_UNSORTED_UNLIT_TRANSLUCENT(Sheets::translucentBlockItemSheet),
    TRANSLUCENT_ON_PARTICLES_TARGET(Sheets::translucentBlockItemSheet);

    /**
     * Solid equivalent of {@link Sheets#cutoutBlockItemSheet()} / {@link Sheets#translucentBlockItemSheet()}.
     */
    public static final RenderType SOLID_BLOCK_SHEET = RenderTypes.entitySolid(TextureAtlas.LOCATION_BLOCKS);

    /**
     * NeoForge 1.21 toggle for linear filtering of text textures. Kept for source compatibility; vanilla 26.x text
     * rendering has no such switch, so it has no effect.
     */
    public static boolean enableTextTextureLinearFiltering = false;

    private final Supplier<RenderType> supplier;

    NeoForgeRenderTypes(final Supplier<RenderType> supplier)
    {
        this.supplier = supplier;
    }

    @Override
    public RenderType get()
    {
        return supplier.get();
    }

    public static RenderType getItemLayeredSolid(final Identifier texture)
    {
        return RenderTypes.entitySolid(texture);
    }

    public static RenderType getItemLayeredCutout(final Identifier texture)
    {
        return RenderTypes.itemCutout(texture);
    }

    public static RenderType getItemLayeredTranslucent(final Identifier texture)
    {
        return RenderTypes.itemTranslucent(texture);
    }

    public static RenderType getEntityUnlitTranslucent(final Identifier texture)
    {
        return RenderTypes.entityTranslucent(texture);
    }
}
