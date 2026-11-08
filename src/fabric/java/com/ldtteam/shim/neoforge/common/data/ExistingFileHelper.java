package com.ldtteam.shim.neoforge.common.data;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

/**
 * Datagen helper (NeoForge shim). Data generation is not supported in the Fabric port; every resource is reported as existing.
 */
public class ExistingFileHelper
{
    public boolean exists(final Identifier loc, final PackType packType)
    {
        return true;
    }

    public boolean exists(final Identifier loc, final PackType packType, final String pathSuffix, final String pathPrefix)
    {
        return true;
    }

    public void trackGenerated(final Identifier loc, final PackType packType, final String pathSuffix, final String pathPrefix)
    {
    }

    public boolean isEnabled()
    {
        return false;
    }
}
