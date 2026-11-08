package com.ldtteam.shim.neoforgespi.language;

/**
 * Mod metadata (NeoForge shim) backed by Fabric mod metadata.
 */
public interface IModInfo
{
    String getModId();

    String getDisplayName();

    String getDescription();

    ArtifactVersion getVersion();

    IModFileInfo getOwningFile();

    default String getNamespace()
    {
        return getModId();
    }
}
