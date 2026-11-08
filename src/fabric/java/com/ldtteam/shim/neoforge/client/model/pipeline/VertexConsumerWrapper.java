package com.ldtteam.shim.neoforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * NeoForge shim: a {@link VertexConsumer} that forwards everything to a parent consumer.
 */
public abstract class VertexConsumerWrapper implements VertexConsumer
{
    protected final VertexConsumer parent;

    public VertexConsumerWrapper(final VertexConsumer parent)
    {
        this.parent = parent;
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z)
    {
        parent.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int r, final int g, final int b, final int a)
    {
        parent.setColor(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int color)
    {
        parent.setColor(color);
        return this;
    }

    @Override
    public VertexConsumer setUv(final float u, final float v)
    {
        parent.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v)
    {
        parent.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v)
    {
        parent.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(final float u, final float v)
    {
        parent.setUv3(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(final float x, final float y, final float z)
    {
        parent.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(final float width)
    {
        parent.setLineWidth(width);
        return this;
    }
}
