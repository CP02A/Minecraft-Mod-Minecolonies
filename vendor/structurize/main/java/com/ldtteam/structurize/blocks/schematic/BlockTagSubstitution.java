package com.ldtteam.structurize.blocks.schematic;

import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.blocks.interfaces.IAnchorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

/**
 * This block is a substitution block (it disappears on normal build) but stores blueprint data (mostly tags) during scan.
 */
public class BlockTagSubstitution extends BlockSubstitution implements IAnchorBlock, EntityBlock
{
    public BlockTagSubstitution(final Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final @NotNull BlockPos blockPos, final @NotNull BlockState blockState)
    {
        return new BlockEntityTagSubstitution(blockPos, blockState);
    }

    @NotNull
    @Override
    protected ItemStack getCloneItemStack(@NotNull final LevelReader level,
        @NotNull final BlockPos pos,
        @NotNull final BlockState blockState,
        final boolean includeData)
    {
        return cloneItemStack(super.getCloneItemStack(level, pos, blockState, includeData), level, pos);
    }

    private ItemStack cloneItemStack(final ItemStack stack, LevelReader level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof final BlockEntityTagSubstitution entity)
        {
            // equivalent of 1.21 BlockEntity#saveToItem
            final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            entity.saveCustomOnly(output);
            entity.removeComponentsFromTag(output);
            BlockItem.setBlockEntityData(stack, entity.getType(), output);
            stack.applyComponents(entity.collectComponents());
        }
        return stack;
    }
}
