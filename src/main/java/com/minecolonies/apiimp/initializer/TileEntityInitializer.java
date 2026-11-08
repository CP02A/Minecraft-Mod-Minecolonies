package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.tileentities.*;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.tileentities.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.ldtteam.shim.neoforge.registries.DeferredRegister;

public class TileEntityInitializer
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    /**
     * FABRIC-PORT: BlockEntityType.Builder was removed in 26.x.
     */
    private static java.util.Set<net.minecraft.world.level.block.Block> toSet(final net.minecraft.world.level.block.Block... blocks)
    {
        return java.util.Set.of(blocks);
    }

    static
    {
        MinecoloniesTileEntities.SCARECROW = BLOCK_ENTITIES.register("scarecrow", () -> new BlockEntityType<>(TileEntityScarecrow::new, toSet(ModBlocks.blockScarecrow)));

        MinecoloniesTileEntities.PLANTATION_FIELD = BLOCK_ENTITIES.register("plantationfield", () -> new BlockEntityType<>(TileEntityPlantationField::new, toSet(ModBlocks.blockPlantationField)));

        MinecoloniesTileEntities.BARREL = BLOCK_ENTITIES.register("barrel", () -> new BlockEntityType<>(TileEntityBarrel::new, toSet(ModBlocks.blockBarrel)));

        MinecoloniesTileEntities.BUILDING = BLOCK_ENTITIES.register("colonybuilding", () -> new BlockEntityType<>(TileEntityColonyBuilding::new, toSet(ModBlocks.getHuts())));

        MinecoloniesTileEntities.DECO_CONTROLLER = BLOCK_ENTITIES.register("decorationcontroller", () -> new BlockEntityType<>(TileEntityDecorationController::new, toSet(ModBlocks.blockDecorationPlaceholder)));

        MinecoloniesTileEntities.RACK = BLOCK_ENTITIES.register("rack", () -> new BlockEntityType<>(TileEntityRack::new, toSet(ModBlocks.blockRack)));

        MinecoloniesTileEntities.GRAVE = BLOCK_ENTITIES.register("grave", () -> new BlockEntityType<>(TileEntityGrave::new, toSet(ModBlocks.blockGrave)));

        MinecoloniesTileEntities.NAMED_GRAVE = BLOCK_ENTITIES.register("namedgrave", () -> new BlockEntityType<>(TileEntityNamedGrave::new, toSet(ModBlocks.blockNamedGrave)));

        MinecoloniesTileEntities.WAREHOUSE = BLOCK_ENTITIES.register("warehouse", () -> new BlockEntityType<>(TileEntityWareHouse::new, toSet(ModBlocks.blockHutWareHouse)));

        MinecoloniesTileEntities.COMPOSTED_DIRT = BLOCK_ENTITIES.register("composteddirt", () -> new BlockEntityType<>(TileEntityCompostedDirt::new, toSet(ModBlocks.blockCompostedDirt)));

        MinecoloniesTileEntities.ENCHANTER = BLOCK_ENTITIES.register("enchanter", () -> new BlockEntityType<>(TileEntityEnchanter::new, toSet(ModBlocks.blockHutEnchanter)));

        MinecoloniesTileEntities.STASH = BLOCK_ENTITIES.register("stash", () -> new BlockEntityType<>(TileEntityStash::new, toSet(ModBlocks.blockStash)));

        MinecoloniesTileEntities.COLONY_FLAG = BLOCK_ENTITIES.register("colony_flag", () -> new BlockEntityType<>(TileEntityColonyFlag::new, toSet(ModBlocks.blockColonyBanner, ModBlocks.blockColonyWallBanner)));

        MinecoloniesTileEntities.COLONY_SIGN = BLOCK_ENTITIES.register("colonysign", () -> new BlockEntityType<>(TileEntityColonySign::new, toSet(ModBlocks.blockColonySign)));
    }
}
