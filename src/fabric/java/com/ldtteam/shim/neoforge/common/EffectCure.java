// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.EffectCure), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectCure
{
    private static final Map<String, EffectCure> CURES = new ConcurrentHashMap<>();

    public static Codec<EffectCure> CODEC = Codec.STRING.xmap(EffectCure::get, EffectCure::name);

    public static Collection<EffectCure> getAllCures()
    {
        return Collections.unmodifiableCollection(CURES.values());
    }

    public static EffectCure get(final String name)
    {
        return CURES.computeIfAbsent(name, EffectCure::new);
    }

    private final String name;

    private EffectCure(final String name)
    {
        this.name = name;
    }

    public String name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "EffectCure[" + name + "]";
    }
}
