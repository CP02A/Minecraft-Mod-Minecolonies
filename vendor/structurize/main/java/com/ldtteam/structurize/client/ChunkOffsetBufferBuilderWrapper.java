package com.ldtteam.structurize.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Adds chunk offset to vertex positions (fluid renderer outputs chunk section relative positions).
 */
public class ChunkOffsetBufferBuilderWrapper implements VertexConsumer
{
    private VertexConsumer delegate;
    private int offsetX;
    private int offsetY;
    private int offsetZ;

    public void setOffset(final VertexConsumer delegate, final int offsetX, final int offsetY, final int offsetZ)
    {
        this.delegate = delegate;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z)
    {
        delegate.addVertex(offsetX + x, offsetY + y, offsetZ + z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int r, final int g, final int b, final int a)
    {
        delegate.setColor(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int color)
    {
        delegate.setColor(color);
        return this;
    }

    @Override
    public VertexConsumer setNormal(final float x, final float y, final float z)
    {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(final float width)
    {
        delegate.setLineWidth(width);
        return this;
    }

    @Override
    public VertexConsumer setUv(final float u, final float v)
    {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v)
    {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v)
    {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(final float u, final float v)
    {
        delegate.setUv3(u, v);
        return this;
    }
}
