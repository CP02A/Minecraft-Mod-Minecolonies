package com.ldtteam.domumornamentum.fabric;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces NeoForge's positional {@code BlockState#getSoundType(level, pos, entity)} for materially textured blocks: their sounds follow
 * the block of their main material (stored in the block entity), as on NeoForge. Called by the sound lookup mixins in
 * {@code com.ldtteam.domumornamentum.fabric.mixin} at the places where NeoForge passes a position (step, fall, place, hit, break).
 */
public final class MaterialSoundTypes
{
    private MaterialSoundTypes()
    {
    }

    /**
     * @param original the sound type vanilla looked up ({@code state.getSoundType()}).
     * @return the positional sound type of a materially textured block, otherwise {@code original}.
     */
    public static SoundType get(final SoundType original, final BlockState state, @Nullable final LevelReader level, final BlockPos pos,
        @Nullable final Entity entity)
    {
        if (level != null && state.getBlock() instanceof final IMateriallyTexturedBlock block)
        {
            return block.getSoundType(state, level, pos, entity);
        }
        return original;
    }
}
