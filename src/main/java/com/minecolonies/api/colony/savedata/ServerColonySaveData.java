package com.minecolonies.api.colony.savedata;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.ColonyList;
import com.minecolonies.core.util.BackUpHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_COLONIES;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_COLONY_MANAGER;

/**
 * The implementation of the colonyTagCapability.
 */
public class ServerColonySaveData extends SavedData implements IServerColonySaveData
{
    /**
     * World save data name.
     */
    public static final String NAME = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "colony_manager").toDebugFileName();

    /**
     * Worldsavedata factory.
     */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "colony_manager");

    /**
     * FABRIC-PORT: 26.x saved data is codec based (SavedDataType). The codec closes over the level's registry access since the
     * colony NBT (de)serialization needs a HolderLookup.Provider. A null data fixer type is a no-op with Fabric API.
     *
     * @param level the level the data belongs to.
     * @return the saved data type.
     */
    public static net.minecraft.world.level.saveddata.SavedDataType<ServerColonySaveData> type(final net.minecraft.server.level.ServerLevel level)
    {
        final HolderLookup.Provider provider = level.registryAccess();
        return new net.minecraft.world.level.saveddata.SavedDataType<>(ID,
          ServerColonySaveData::new,
          CompoundTag.CODEC.xmap(tag -> {
              final ServerColonySaveData colonyManagerData = new ServerColonySaveData();
              colonyManagerData.readNBT(provider, tag);
              return colonyManagerData;
          }, data -> data.save(new CompoundTag(), provider)),
          null);
    }

    /**
     * Levels whose legacy colony file was already looked for (per level instance, so a new server/world checks again).
     */
    private static final java.util.Set<ServerLevel> LEGACY_CHECKED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /**
     * FABRIC-PORT: 1.21.1 stored the colonies in {@code <dimension data>/minecolonies_colony_manager.dat} (overworld: {@code data/},
     * nether: {@code DIM-1/data/}, end: {@code DIM1/data/}). 26.x resolves the same id to
     * {@code dimensions/<ns>/<dim>/data/minecolonies/colony_manager.dat}, and vanilla's world upgrade only moves its own files, so
     * colonies of migrated worlds were not found. Move the legacy file into place before the data is loaded for the first time.
     *
     * @param level the level about to load its colony data.
     */
    public static void migrateLegacyFile(final ServerLevel level)
    {
        synchronized (LEGACY_CHECKED)
        {
            if (!LEGACY_CHECKED.add(level))
            {
                return;
            }
        }

        final java.nio.file.Path root = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        final java.nio.file.Path dataFolder = net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(), root).resolve("data");
        final java.nio.file.Path target = ID.withSuffix(".dat").resolveAgainst(dataFolder);
        if (java.nio.file.Files.exists(target))
        {
            return;
        }

        final String legacyName = NAME + ".dat";
        final java.util.List<java.nio.file.Path> candidates = new java.util.ArrayList<>();
        candidates.add(dataFolder.resolve(legacyName));
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD)
        {
            candidates.add(root.resolve("data").resolve(legacyName));
        }
        else if (level.dimension() == net.minecraft.world.level.Level.NETHER)
        {
            candidates.add(root.resolve("DIM-1").resolve("data").resolve(legacyName));
        }
        else if (level.dimension() == net.minecraft.world.level.Level.END)
        {
            candidates.add(root.resolve("DIM1").resolve("data").resolve(legacyName));
        }

        for (final java.nio.file.Path legacy : candidates)
        {
            if (java.nio.file.Files.isRegularFile(legacy))
            {
                try
                {
                    java.nio.file.Files.createDirectories(target.getParent());
                    java.nio.file.Files.move(legacy, target);
                    Log.getLogger().info("Migrated 1.21.1 colony data {} to {}", legacy, target);
                }
                catch (final java.io.IOException e)
                {
                    Log.getLogger().error("Could not migrate 1.21.1 colony data {} to {}", legacy, target, e);
                }
                return;
            }
        }
    }

    /**
     * The list of all colonies.
     */
    @NotNull
    private final ColonyList<IColony> colonies = new ColonyList<>();

    /**
     * Is this the main overworld cap?
     */
    private boolean overworld;

    private ServerColonySaveData()
    {

    }

    @NotNull
    // FABRIC-PORT: no longer an override (SavedData#save was replaced by the SavedDataType codec)
    public CompoundTag save(final @NotNull CompoundTag tag, @NotNull final HolderLookup.Provider provider)
    {
        return writeNBT(provider, tag);
    }

    @Override
    public IColony createColony(@NotNull final ServerLevel w, @NotNull final String name, @NotNull final BlockPos pos)
    {
        return colonies.create(w, name, pos);
    }

    @Override
    public void deleteColony(final int id)
    {
        colonies.remove(id);
    }

    @Override
    public IColony getColony(final int id)
    {
        return colonies.get(id);
    }

    @Override
    public List<IColony> getColonies()
    {
        return colonies.getCopyAsList();
    }

    @Override
    public void addColony(final IColony colony)
    {
        colonies.add(colony);
    }

    @Override
    public int getTopID()
    {
        return colonies.getTopID();
    }

    @Override
    public boolean isDirty()
    {
        return true;
    }

    private CompoundTag writeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag inputTag)
    {
        final CompoundTag compound = new CompoundTag();
        final ListTag colonyTag = new ListTag();
        for (final IColony colony : colonies)
        {
            try
            {
                colonyTag.add(colony.getColonyTag());
            }
            catch (Exception e)
            {
                Log.getLogger()
                  .error("Colony: " + colony.getName() + " id:" + colony.getID() + " owner:" + colony.getPermissions().getOwnerName() + " could not be saved! Error:", e);
            }
        }

        compound.put(TAG_COLONIES, colonyTag);

        if (overworld)
        {
            final CompoundTag managerCompound = new CompoundTag();
            IColonyManager.getInstance().write(provider, managerCompound);
            compound.put(TAG_COLONY_MANAGER, managerCompound);
        }

        inputTag.put(Constants.MOD_ID, compound);
        return inputTag;
    }

    @Override
    public IServerColonySaveData setOverworld(final boolean overworld)
    {
        this.overworld = overworld;
        return this;
    }

    private void readNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag inputTag)
    {
        final CompoundTag compound = inputTag.getCompoundOrEmpty(Constants.MOD_ID);

        if (!compound.contains(TAG_COLONIES))
        {
            BackUpHelper.loadManagerBackup(provider);
            return;
        }

        // Load all colonies from Nbt
        Multimap<BlockPos, IColony> tempColonies = ArrayListMultimap.create();
        for (final Tag tag : compound.getListOrEmpty(TAG_COLONIES))
        {
            final IColony colony = Colony.loadColony((CompoundTag) tag, null, provider);
            if (colony != null)
            {
                tempColonies.put(colony.getCenter(), colony);
                colonies.add(colony);
            }
        }

        if (compound.contains(TAG_COLONY_MANAGER))
        {
            IColonyManager.getInstance().read(provider, compound.getCompoundOrEmpty(TAG_COLONY_MANAGER));
            this.overworld = true;
        }

        // Check colonies for duplicates causing issues.
        for (final BlockPos pos : tempColonies.keySet())
        {
            // Check if any position has more than one colony
            if (tempColonies.get(pos).size() > 1)
            {
                Log.getLogger().warn("Detected duplicate colonies which are at the same position:");
                for (final IColony colony : tempColonies.get(pos))
                {
                    Log.getLogger()
                        .warn(
                        "ID: " + colony.getID() + " name:" + colony.getName() + " citizens:" + colony.getCitizenManager().getCitizens().size() + " building count:" + colony
                                                                                                                                                                        .getCommonBuildingManager()
                                                                                                                                                                        .getBuildings()
                                                                                                                                                                        .size());
                }
                Log.getLogger().warn("Check and remove all except one of the duplicated colonies above!");
            }
        }
    }
}