package com.ldtteam.shim.neoforgespi.locating;

import java.nio.file.Path;

/**
 * A mod file (NeoForge shim).
 */
public interface IModFile
{
    /**
     * Resolve a path inside the mod file. The returned path may not exist.
     */
    Path findResource(String... pathName);

    Path getFilePath();
}
