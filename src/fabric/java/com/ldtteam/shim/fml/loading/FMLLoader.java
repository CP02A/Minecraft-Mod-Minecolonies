package com.ldtteam.shim.fml.loading;

import com.ldtteam.shim.api.distmarker.Dist;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Loader information (NeoForge shim) backed by Fabric Loader.
 */
public final class FMLLoader
{
    private FMLLoader()
    {
    }

    public static Dist getDist()
    {
        return FMLEnvironment.dist;
    }

    public static boolean isProduction()
    {
        return FMLEnvironment.production;
    }

    public static Path getGamePath()
    {
        return FabricLoader.getInstance().getGameDir();
    }
}
