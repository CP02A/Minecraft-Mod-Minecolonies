package com.minecolonies.core.tileentities;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import com.minecolonies.api.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.NbtTagConstants.*;

public class TileEntityColonyFlag extends BlockEntity
{
    /** A list of the default banner patterns, for colonies that have not chosen a flag */
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    /** The colony of the player that placed this banner */
    public int colonyId = -1;

    public TileEntityColonyFlag(final BlockPos pos, final BlockState state) { super(MinecoloniesTileEntities.COLONY_FLAG.get(), pos, state); }

    public BannerPatternLayers getPatterns()
    {
        if (level != null && level.getGameTime() % 20 == 0)
        {
            final IColonyView view = IColonyManager.getInstance().getColonyView(colonyId, level.dimension());
            if (view != null)
            {
                this.patterns = view.getColonyFlag();
            }
        }

        return this.patterns;
    }

    @Override
    public void saveAdditional(final net.minecraft.world.level.storage.ValueOutput valueOutput)
    {
        super.saveAdditional(valueOutput);
        final CompoundTag compound = new CompoundTag();
        final HolderLookup.Provider provider = com.minecolonies.core.entity.NbtValueIO.lookup(this);
        saveAdditionalTag(compound, provider);
        com.minecolonies.core.entity.NbtValueIO.write(valueOutput, compound);
    }

    private void saveAdditionalTag(final CompoundTag compound, final HolderLookup.Provider provider)
    {


        compound.put(TAG_BANNER_PATTERNS, Utils.serializeCodecMess(BannerPatternLayers.CODEC, provider, this.patterns));

        compound.putInt(TAG_COLONY_ID, colonyId);
        }

    @Override
    public void loadAdditional(final net.minecraft.world.level.storage.ValueInput valueInput)
    {
        super.loadAdditional(valueInput);
        final CompoundTag compound = com.minecolonies.core.entity.NbtValueIO.read(valueInput);
        final HolderLookup.Provider provider = valueInput.lookup();


        if (compound.contains(TAG_BANNER_PATTERNS))
        {
            this.patterns = Utils.deserializeCodecMess(BannerPatternLayers.CODEC, provider, compound.get(TAG_BANNER_PATTERNS));
        }

        this.colonyId = compound.getIntOr(TAG_COLONY_ID, 0);

        if(this.colonyId == -1 && this.hasLevel())
        {
            IColony colony = IColonyManager.getInstance().getIColony(this.getLevel(), worldPosition);
            if (colony != null)
            {
                this.colonyId = colony.getID();
                this.setChanged();
            }
        }
        }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(@NotNull final HolderLookup.Provider provider) { return this.saveWithoutMetadata(provider); }

}
