package com.ldtteam.shim.fml.loading;

import com.ldtteam.shim.api.distmarker.Dist;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Environment information (NeoForge shim) backed by Fabric Loader.
 */
public final class FMLEnvironment
{
    public static final Dist    dist       = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    public static final boolean production = !FabricLoader.getInstance().isDevelopmentEnvironment();

    private FMLEnvironment()
    {
    }

    public static Dist getDist()
    {
        return dist;
    }

    public static boolean isProduction()
    {
        return production;
    }
}
