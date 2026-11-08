package com.ldtteam.shim.neoforge.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import com.ldtteam.shim.neoforgespi.language.ArtifactVersion;
import net.minecraft.world.level.storage.LevelResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fired on the mod bus when a world is loaded that was saved with different mod versions (NeoForge shim).
 * FABRIC-PORT TODO: Fabric does not track per-world mod versions, so this event is never fired; listeners stay harmless.
 */
public class ModMismatchEvent extends Event implements IModBusEvent
{
    public record MismatchedVersionInfo(ArtifactVersion oldVersion, ArtifactVersion newVersion)
    {
        public boolean isMissing()
        {
            return newVersion == null;
        }

        public boolean wasUpgrade()
        {
            return oldVersion != null && newVersion != null && newVersion.compareTo(oldVersion) > 0;
        }
    }

    private final LevelResource                       levelDirectory;
    private final Map<String, MismatchedVersionInfo> versionDifferences;
    private final Set<String>                         resolved = new HashSet<>();

    public ModMismatchEvent(final LevelResource levelDirectory, final Map<String, MismatchedVersionInfo> versionDifferences)
    {
        this.levelDirectory = levelDirectory;
        this.versionDifferences = new HashMap<>(versionDifferences);
    }

    public LevelResource getLevelDirectory()
    {
        return levelDirectory;
    }

    public Optional<MismatchedVersionInfo> getVersionDifference(final String modid)
    {
        return Optional.ofNullable(versionDifferences.get(modid));
    }

    public Optional<ArtifactVersion> getPreviousVersion(final String modId)
    {
        return getVersionDifference(modId).map(MismatchedVersionInfo::oldVersion);
    }

    public Optional<ArtifactVersion> getCurrentVersion(final String modId)
    {
        return getVersionDifference(modId).map(MismatchedVersionInfo::newVersion);
    }

    public void markResolved(final String modId)
    {
        resolved.add(modId);
    }

    public boolean wasResolved(final String modId)
    {
        return resolved.contains(modId);
    }

    public boolean anyUnresolved()
    {
        return !resolved.containsAll(versionDifferences.keySet());
    }
}
