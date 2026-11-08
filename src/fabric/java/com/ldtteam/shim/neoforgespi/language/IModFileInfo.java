package com.ldtteam.shim.neoforgespi.language;

import com.ldtteam.shim.neoforgespi.locating.IModFile;

import java.util.List;

/**
 * Mod file metadata (NeoForge shim).
 */
public interface IModFileInfo
{
    IModFile getFile();

    List<IModInfo> getMods();

    String versionString();

    String moduleName();
}
