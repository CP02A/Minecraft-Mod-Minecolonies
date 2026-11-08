package com.minecolonies.core.client.render.worldevent;

import com.minecolonies.api.util.constant.Constants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class RenderTypes
{
    /**
     * Textured, translucent, no depth test pipeline (POSITION_TEX_COLOR, quads).
     * FABRIC-PORT: 26.x replaced RenderStateShards by render pipelines; vertices must now also carry a color.
     */
    public static final RenderPipeline WORLD_ENTITY_ICON_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withLocation(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pipeline/world_entity_icon"))
        .withVertexShader("core/position_tex_color")
        .withFragmentShader("core/position_tex_color")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build());

    private static final Function<Identifier, RenderType> WORLD_ENTITY_ICON = Util.memoize(
      texture -> RenderType.create("minecolonies_entity_icon",
        RenderSetup.builder(WORLD_ENTITY_ICON_PIPELINE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));

    /**
     * Usable for rendering simple flat textures.
     * Vertices: position, uv, color.
     *
     * @param resLoc texture location
     * @return render type
     */
    public static RenderType worldEntityIcon(final Identifier resLoc)
    {
        return WORLD_ENTITY_ICON.apply(resLoc);
    }
}
