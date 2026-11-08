package com.ldtteam.structurize.api;

/**
 * Replacement for the removed vanilla {@code net.minecraft.util.Tuple} (same API).
 */
public class Tuple<A, B>
{
    private A a;
    private B b;

    public Tuple(final A a, final B b)
    {
        this.a = a;
        this.b = b;
    }

    public A getA()
    {
        return a;
    }

    public void setA(final A a)
    {
        this.a = a;
    }

    public B getB()
    {
        return b;
    }

    public void setB(final B b)
    {
        this.b = b;
    }
}
