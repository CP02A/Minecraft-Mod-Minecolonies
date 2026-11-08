package com.ldtteam.shim.neoforgespi.language;

/**
 * Minimal stand-in for maven's ArtifactVersion (NeoForge shim): only the string form is needed.
 */
public record ArtifactVersion(String version) implements Comparable<ArtifactVersion>
{
    @Override
    public String toString()
    {
        return version;
    }

    @Override
    public int compareTo(final ArtifactVersion o)
    {
        return version.compareTo(o.version);
    }
}
