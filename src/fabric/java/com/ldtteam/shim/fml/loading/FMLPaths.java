package com.ldtteam.shim.fml.loading;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Standard paths (NeoForge shim).
 */
public enum FMLPaths
{
    GAMEDIR,
    CONFIGDIR,
    MODSDIR;

    public Path get()
    {
        return switch (this)
        {
            case GAMEDIR -> FabricLoader.getInstance().getGameDir();
            case CONFIGDIR -> FabricLoader.getInstance().getConfigDir();
            case MODSDIR -> FabricLoader.getInstance().getGameDir().resolve("mods");
        };
    }
}
