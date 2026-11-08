package com.ldtteam.shim.neoforge.client;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * NeoForge shim: maps a block's chunk layer to the render type to use when drawing it outside of chunk meshing.
 *
 * <p>On 26.x chunk "render types" are {@link ChunkSectionLayer}s (see {@code BakedQuad.MaterialInfo#layer()}), so the
 * arguments are layers instead of NeoForge 1.21's chunk {@code RenderType}s.
 */
public final class RenderTypeHelper
{
    private RenderTypeHelper()
    {
    }

    /**
     * @return the entity-style (lit, overlay-aware) render type on the block atlas for quads of the given layer, like
     *         block items use.
     */
    public static RenderType getEntityRenderType(final ChunkSectionLayer layer)
    {
        return layer.translucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
    }

    /**
     * @param cull ignored on 26.x (block item sheets are always culled like vanilla block items)
     */
    public static RenderType getEntityRenderType(final ChunkSectionLayer layer, final boolean cull)
    {
        return getEntityRenderType(layer);
    }

    /**
     * @return the render type vanilla uses for moving blocks (pistons, falling blocks) for the given layer.
     */
    public static RenderType getMovingBlockRenderType(final ChunkSectionLayer layer)
    {
        return switch (layer)
        {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }

    /**
     * @return the item render type for quads of the given layer.
     */
    public static RenderType getFallbackItemRenderType(final ChunkSectionLayer layer)
    {
        return getEntityRenderType(layer);
    }
}
