package com.minecolonies.core.entity;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.stream.Stream;

/**
 * Bridge between the 26.x {@link ValueInput}/{@link ValueOutput} save API and the legacy {@link CompoundTag} based
 * (de)serialization code. Reads/writes the entries of the value container 1:1, so the on-disk format is unchanged.
 */
public final class NbtValueIO
{
    /**
     * Map codec that passes all entries of the surrounding map through as a compound tag.
     */
    public static final MapCodec<CompoundTag> PASSTHROUGH = new MapCodec<>()
    {
        @Override
        public <T> Stream<T> keys(final DynamicOps<T> ops)
        {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<CompoundTag> decode(final DynamicOps<T> ops, final MapLike<T> input)
        {
            final CompoundTag tag = new CompoundTag();
            input.entries().forEach(pair -> ops.getStringValue(pair.getFirst()).ifSuccess(key -> {
                final Tag value = ops.convertTo(NbtOps.INSTANCE, pair.getSecond());
                if (value != null)
                {
                    tag.put(key, value);
                }
            }));
            return DataResult.success(tag);
        }

        @Override
        public <T> RecordBuilder<T> encode(final CompoundTag input, final DynamicOps<T> ops, final RecordBuilder<T> prefix)
        {
            for (final String key : input.keySet())
            {
                final Tag value = input.get(key);
                if (value != null)
                {
                    prefix.add(key, NbtOps.INSTANCE.convertTo(ops, value));
                }
            }
            return prefix;
        }
    };

    private NbtValueIO()
    {
    }

    /**
     * Read the whole value input as a compound tag.
     *
     * @param input the input.
     * @return the tag (empty if nothing could be read).
     */
    @SuppressWarnings("deprecation")
    public static CompoundTag read(final ValueInput input)
    {
        return input.read(PASSTHROUGH).orElseGet(CompoundTag::new);
    }

    /**
     * Registry lookup to use when serializing a block entity into a value output (which does not expose its lookup).
     *
     * @param blockEntity the block entity.
     * @return the registry access of its level, or the static registries if it has no level.
     */
    public static HolderLookup.Provider lookup(final BlockEntity blockEntity)
    {
        final Level level = blockEntity.getLevel();
        if (level != null)
        {
            return level.registryAccess();
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    /**
     * Write all entries of the compound into the value output.
     *
     * @param output the output.
     * @param tag    the tag to write.
     */
    public static void write(final ValueOutput output, final CompoundTag tag)
    {
        output.store(PASSTHROUGH, tag);
    }
}
