package com.ldtteam.structurize.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;

/**
 * Helpers bridging the CompoundTag based structurize data model and 26.x ValueInput/ValueOutput serialization.
 */
public final class ValueIOUtils
{
    private ValueIOUtils()
    {
    }

    /**
     * @return a ValueInput reading the given tag
     */
    public static ValueInput input(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        return TagValueInput.create(ProblemReporter.DISCARDING, provider, tag);
    }

    /**
     * @return a ValueOutput which result can be obtained using {@link TagValueOutput#buildResult()}
     */
    public static TagValueOutput output(final HolderLookup.Provider provider)
    {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
    }

    /**
     * Saves entity (with id) to nbt, equivalent of old {@code Entity#save(CompoundTag)}.
     */
    public static CompoundTag saveEntity(final Entity entity)
    {
        final TagValueOutput output = output(entity.registryAccess());
        entity.save(output);
        return output.buildResult();
    }

    /**
     * Saves entity without id to nbt, equivalent of old {@code Entity#saveWithoutId(CompoundTag)}.
     */
    public static CompoundTag saveEntityWithoutId(final Entity entity)
    {
        final TagValueOutput output = output(entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    /**
     * Loads entity from nbt, equivalent of old {@code Entity#load(CompoundTag)}.
     */
    public static void loadEntity(final Entity entity, final CompoundTag tag)
    {
        entity.load(input(tag, entity.registryAccess()));
    }

    /**
     * Equivalent of old {@code EntityType#by(CompoundTag)}.
     */
    public static Optional<EntityType<?>> entityType(final CompoundTag tag, final HolderLookup.Provider provider)
    {
        return EntityType.by(input(tag, provider));
    }

    /**
     * Loads block entity data including components, equivalent of old {@code BlockEntity#loadWithComponents(CompoundTag, Provider)}.
     */
    public static void loadBlockEntityWithComponents(final BlockEntity blockEntity, final CompoundTag tag, final HolderLookup.Provider provider)
    {
        blockEntity.loadWithComponents(input(tag, provider));
    }
}
