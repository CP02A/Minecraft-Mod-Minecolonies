package com.ldtteam.domumornamentum.client.render;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;

/**
 * Custom pipeline snippets for custom depth test modes used by ModRenderTypes.
 *
 * <p>26.3 uses a reversed depth buffer (vanilla default test is GREATER_THAN_OR_EQUAL), so the compare ops are the
 * mirrored ones of the 26.1 originals; the snippet names keep their original (forward-Z) meaning.
 */
public enum ModRenderPipelineSnippets
{
    NO_DEPTH_TEST(() -> RenderPipeline.builder()
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .buildSnippet()),

    LESS_OR_EQUAL_DEPTH_TEST(() -> RenderPipeline.builder()
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .buildSnippet()),

    GREATER_OR_EQUAL_DEPTH_TEST(() -> RenderPipeline.builder()
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .buildSnippet()),

    GREATER_DEPTH_TEST(() -> RenderPipeline.builder()
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, false))
            .buildSnippet());

    private final Supplier<RenderPipeline.Snippet> getter;

    ModRenderPipelineSnippets(final Supplier<RenderPipeline.Snippet> getter) {
        this.getter = Suppliers.memoize(getter::get);
    }

    public RenderPipeline.Snippet snippet() {
        return getter.get();
    }
}
