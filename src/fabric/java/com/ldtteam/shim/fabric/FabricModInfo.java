package com.ldtteam.shim.fabric;

import com.ldtteam.shim.neoforgespi.language.ArtifactVersion;
import com.ldtteam.shim.neoforgespi.language.IModFileInfo;
import com.ldtteam.shim.neoforgespi.language.IModInfo;
import com.ldtteam.shim.neoforgespi.locating.IModFile;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * IModInfo view of a Fabric mod container. The mod id may differ from the container id for provided ids
 * (structurize, blockui, domum_ornamentum are provided by the minecolonies jar).
 */
public final class FabricModInfo implements IModInfo, IModFileInfo, IModFile
{
    private final String       modId;
    private final ModContainer container;

    public FabricModInfo(final String modId, final ModContainer container)
    {
        this.modId = modId;
        this.container = container;
    }

    public ModContainer getFabricContainer()
    {
        return container;
    }

    @Override
    public String getModId()
    {
        return modId;
    }

    @Override
    public String getDisplayName()
    {
        return modId.equals(container.getMetadata().getId()) ? container.getMetadata().getName() : modId;
    }

    @Override
    public String getDescription()
    {
        return container.getMetadata().getDescription();
    }

    @Override
    public ArtifactVersion getVersion()
    {
        return new ArtifactVersion(container.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public IModFileInfo getOwningFile()
    {
        return this;
    }

    @Override
    public IModFile getFile()
    {
        return this;
    }

    @Override
    public List<IModInfo> getMods()
    {
        return List.of(this);
    }

    @Override
    public String versionString()
    {
        return getVersion().toString();
    }

    @Override
    public String moduleName()
    {
        return container.getMetadata().getId();
    }

    @Override
    public Path findResource(final String... pathName)
    {
        final String joined = String.join("/", pathName);
        for (final Path root : container.getRootPaths())
        {
            final Path p = root.resolve(joined);
            if (Files.exists(p))
            {
                return p;
            }
        }
        return container.getRootPaths().getFirst().resolve(joined);
    }

    @Override
    public Path getFilePath()
    {
        return container.getOrigin().getKind() == ModOrigin.Kind.PATH
            ? container.getOrigin().getPaths().getFirst()
            : container.getRootPaths().getFirst();
    }
}
