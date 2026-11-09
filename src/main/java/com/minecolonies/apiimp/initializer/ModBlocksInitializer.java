package com.minecolonies.apiimp.initializer;

import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.blocks.*;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagBanner;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagWallBanner;
import com.minecolonies.core.blocks.decorative.BlockConstructionTape;
import com.minecolonies.core.blocks.decorative.BlockGate;
import com.minecolonies.core.blocks.huts.*;
import com.minecolonies.core.blocks.schematic.BlockWaypoint;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.neoforge.registries.RegisterEvent;

import java.util.List;

import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.IRON_GATE;
import static com.minecolonies.api.blocks.decorative.AbstractBlockGate.WOODEN_GATE;
import static com.minecolonies.core.blocks.MinecoloniesCropBlock.*;
import static com.minecolonies.core.blocks.MinecoloniesFarmland.FARMLAND;
import static com.minecolonies.core.blocks.MinecoloniesFarmland.FLOODED_FARMLAND;

/**
 * This class deals with the initialization of blocks and their items.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModBlocksInitializer
{

    private ModBlocksInitializer()
    {
        throw new IllegalStateException("Tried to initialize: ModBlockInitializer but this is a Utility class.");
    }

    @SubscribeEvent
    public static void registerBlocks(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.BLOCK))
        {
            ModBlocksInitializer.init(event.getRegistry(Registries.BLOCK));
        }
    }

    /**
     * Initializes {@link ModBlocks} with the block instances.
     *
     * @param registry The registry to register the new blocks.
     */
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public static void init(final Registry<Block> registry)
    {
        ModBlocks.blockHutBaker = new BlockHutBaker().registerBlock(registry);
        ModBlocks.blockHutBlacksmith = new BlockHutBlacksmith().registerBlock(registry);
        ModBlocks.blockHutBuilder = new BlockHutBuilder().registerBlock(registry);
        ModBlocks.blockHutHome = new BlockHutCitizen().registerBlock(registry);
        ModBlocks.blockHutDeliveryman = new BlockHutDeliveryman().registerBlock(registry);
        ModBlocks.blockHutFarmer = new BlockHutFarmer().registerBlock(registry);
        ModBlocks.blockScarecrow = new BlockScarecrow().registerBlock(registry);
        ModBlocks.blockHutFisherman = new BlockHutFisherman().registerBlock(registry);
        ModBlocks.blockHutGuardTower = new BlockHutGuardTower().registerBlock(registry);
        ModBlocks.blockHutLumberjack = new BlockHutLumberjack().registerBlock(registry);
        ModBlocks.blockHutMiner = new BlockHutMiner().registerBlock(registry);
        ModBlocks.blockHutStonemason = new BlockHutStonemason().registerBlock(registry);
        ModBlocks.blockHutTownHall = new BlockHutTownHall().registerBlock(registry);
        ModBlocks.blockHutWareHouse = new BlockHutWareHouse().registerBlock(registry);
        ModBlocks.blockHutShepherd = new BlockHutShepherd().registerBlock(registry);
        ModBlocks.blockHutCowboy = new BlockHutCowboy().registerBlock(registry);
        ModBlocks.blockHutSwineHerder = new BlockHutSwineHerder().registerBlock(registry);
        ModBlocks.blockHutChickenHerder = new BlockHutChickenHerder().registerBlock(registry);
        ModBlocks.blockHutBarracks = new BlockHutBarracks().registerBlock(registry);
        ModBlocks.blockHutBarracksTower = new BlockHutBarracksTower().registerBlock(registry);
        ModBlocks.blockHutCook = new BlockHutCook().registerBlock(registry);
        ModBlocks.blockHutSmeltery = new BlockHutSmeltery().registerBlock(registry);
        ModBlocks.blockHutComposter = new BlockHutComposter().registerBlock(registry);
        ModBlocks.blockHutLibrary = new BlockHutLibrary().registerBlock(registry);
        ModBlocks.blockHutArchery = new BlockHutArchery().registerBlock(registry);
        ModBlocks.blockHutSawmill = new BlockHutSawmill().registerBlock(registry);
        ModBlocks.blockHutCombatAcademy = new BlockHutCombatAcademy().registerBlock(registry);
        ModBlocks.blockHutStoneSmeltery = new BlockHutStoneSmeltery().registerBlock(registry);
        ModBlocks.blockHutCrusher = new BlockHutCrusher().registerBlock(registry);
        ModBlocks.blockHutSifter = new BlockHutSifter().registerBlock(registry);
        ModBlocks.blockHutFlorist = new BlockHutFlorist().registerBlock(registry);
        ModBlocks.blockHutEnchanter = new BlockHutEnchanter().registerBlock(registry);
        ModBlocks.blockHutUniversity = new BlockHutUniversity().registerBlock(registry);
        ModBlocks.blockHutHospital = new BlockHutHospital().registerBlock(registry);
        ModBlocks.blockHutSchool = new BlockHutSchool().registerBlock(registry);
        ModBlocks.blockHutGlassblower = new BlockHutGlassblower().registerBlock(registry);
        ModBlocks.blockHutDyer = new BlockHutDyer().registerBlock(registry);
        ModBlocks.blockHutFletcher = new BlockHutFletcher().registerBlock(registry);
        ModBlocks.blockHutMechanic = new BlockHutMechanic().registerBlock(registry);
        ModBlocks.blockHutTavern = new BlockHutTavern().registerBlock(registry);
        ModBlocks.blockHutPlantation = new BlockHutPlantation().registerBlock(registry);
        ModBlocks.blockPlantationField = new BlockPlantationField().registerBlock(registry);
        ModBlocks.blockHutRabbitHutch = new BlockHutRabbitHutch().registerBlock(registry);
        ModBlocks.blockHutConcreteMixer = new BlockHutConcreteMixer().registerBlock(registry);
        ModBlocks.blockHutBeekeeper = new BlockHutBeekeeper().registerBlock(registry);
        ModBlocks.blockHutMysticalSite = new BlockHutMysticalSite().registerBlock(registry);
        ModBlocks.blockHutGraveyard = new BlockHutGraveyard().registerBlock(registry);
        ModBlocks.blockHutNetherWorker = new BlockHutNetherWorker().registerBlock(registry);
        ModBlocks.blockHutAlchemist = new BlockHutAlchemist().registerBlock(registry);
        ModBlocks.blockHutKitchen = new BlockHutKitchen().registerBlock(registry);
        ModBlocks.blockHutGateHouse = new BlockHutGateHouse().registerBlock(registry);
        ModBlocks.blockHutStable = new BlockHutStable().registerBlock(registry);

        ModBlocks.blockConstructionTape = new BlockConstructionTape().registerBlock(registry);
        ModBlocks.blockRack = new BlockMinecoloniesRack().registerBlock(registry);
        ModBlocks.blockGrave = new BlockMinecoloniesGrave().registerBlock(registry);
        ModBlocks.blockNamedGrave = new BlockMinecoloniesNamedGrave().registerBlock(registry);
        ModBlocks.blockWayPoint = new BlockWaypoint().registerBlock(registry);
        ModBlocks.blockPostBox = new BlockPostBox().registerBlock(registry);
        ModBlocks.blockStash = new BlockStash().registerBlock(registry);
        ModBlocks.blockDecorationPlaceholder = new BlockDecorationController().registerBlock(registry);
        ModBlocks.blockBarrel = new BlockBarrel().registerBlock(registry);
        ModBlocks.blockCompostedDirt = new BlockCompostedDirt().registerBlock(registry);
        ModBlocks.blockColonyBanner = new BlockColonyFlagBanner().registerBlock(registry);
        ModBlocks.blockColonyWallBanner = new BlockColonyFlagWallBanner().registerBlock(registry);
        ModBlocks.blockIronGate = new BlockGate(IRON_GATE, 10f, 6, 8).registerBlock(registry);
        ModBlocks.blockWoodenGate = new BlockGate(WOODEN_GATE, 7f, 6, 5).registerBlock(registry);
        ModBlocks.farmland = new MinecoloniesFarmland(FARMLAND, false, 15.0).registerBlock(registry);
        ModBlocks.floodedFarmland = new MinecoloniesFarmland(FLOODED_FARMLAND, true, 13.0).registerBlock(registry);
        ModBlocks.blockColonySign = new BlockColonySign().registerBlock(registry);

        // Could in the future add alternative versions of these crops that can be planted by the player and grow at a slower rate.
        ModBlocks.blockBellPepper = new MinecoloniesCropBlock(BELL_PEPPER, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes).registerBlock(registry);
        ModBlocks.blockCabbage = new MinecoloniesCropBlock(CABBAGE, ModBlocks.farmland, List.of(Blocks.FERN), ModTags.coldBiomes).registerBlock(registry);
        ModBlocks.blockChickpea = new MinecoloniesCropBlock(CHICKPEA, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.DEAD_BUSH), ModTags.dryBiomes).registerBlock(registry);
        ModBlocks.blockDurum = new MinecoloniesCropBlock(DURUM, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null).registerBlock(registry);
        ModBlocks.blockEggplant = new MinecoloniesCropBlock(EGGPLANT, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null).registerBlock(registry);
        ModBlocks.blockGarlic = new MinecoloniesCropBlock(GARLIC, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null).registerBlock(registry);
        ModBlocks.blockOnion = new MinecoloniesCropBlock(ONION, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null).registerBlock(registry);
        ModBlocks.blockSoyBean = new MinecoloniesCropBlock(SOYBEAN, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN), ModTags.humidBiomes).registerBlock(registry);
        ModBlocks.blockTomato = new MinecoloniesCropBlock(TOMATO, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes).registerBlock(registry);
        ModBlocks.blockRice = new MinecoloniesCropBlock(RICE, ModBlocks.floodedFarmland, List.of(Blocks.SEAGRASS, Blocks.SMALL_DRIPLEAF), ModTags.humidBiomes).registerBlock(registry);

        ModBlocks.blockButternutSquash = new MinecoloniesCropBlock(BUTTERNUT_SQUASH, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.coldBiomes).registerBlock(registry);
        ModBlocks.blockCorn = new MinecoloniesCropBlock(CORN, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.temperateBiomes).registerBlock(registry);
        ModBlocks.blockMint = new MinecoloniesCropBlock(MINT, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), null).registerBlock(registry);
        ModBlocks.blockNetherPepper = new MinecoloniesCropBlock(NETHER_PEPPER, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.dryBiomes).registerBlock(registry);
        ModBlocks.blockPeas = new MinecoloniesCropBlock(PEAS, ModBlocks.farmland, List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS), ModTags.humidBiomes).registerBlock(registry);

        ModBlocks.blockSimpleQuarry = new SimpleQuarry().registerBlock(registry);
        ModBlocks.blockMediumQuarry = new MediumQuarry().registerBlock(registry);
        //ModBlocks.blockLargeQuarry = new LargeQuarry().registerBlock(registry);
    }

    @SubscribeEvent
    public static void registerItems(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.ITEM))
        {
            ModBlocksInitializer.registerBlockItem(event.getRegistry(Registries.ITEM));
        }
    }

    /**
     * FABRIC-PORT: 26.x items need their id before construction. Block items take the block's registry id and the block's description id,
     * exactly like vanilla's Items#registerBlock.
     *
     * @param block the (already registered) block.
     * @return properties with the item id set.
     */
    private static Item.Properties blockItemProps(final net.minecraft.world.level.block.Block block)
    {
        final net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return new Item.Properties()
                 .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id))
                 .useBlockDescriptionPrefix();
    }

    /**
     * Initializes the registry with the relevant item produced by the relevant blocks.
     *
     * @param registry The item registry to add the items too.
     */
    public static void registerBlockItem(final Registry<Item> registry)
    {
        ModBlocks.blockHutBaker.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBaker));
        ModBlocks.blockHutBlacksmith.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBlacksmith));
        ModBlocks.blockHutBuilder.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBuilder));
        ModBlocks.blockHutHome.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutHome));
        ModBlocks.blockHutDeliveryman.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutDeliveryman));
        ModBlocks.blockHutFarmer.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutFarmer));
        ModBlocks.blockScarecrow.registerBlockItem(registry, blockItemProps(ModBlocks.blockScarecrow));
        ModBlocks.blockHutFisherman.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutFisherman));
        ModBlocks.blockHutGuardTower.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutGuardTower));
        ModBlocks.blockHutLumberjack.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutLumberjack));
        ModBlocks.blockHutMiner.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutMiner));
        ModBlocks.blockHutStonemason.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutStonemason));
        ModBlocks.blockHutTownHall.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutTownHall));
        ModBlocks.blockHutWareHouse.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutWareHouse));
        ModBlocks.blockHutShepherd.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutShepherd));
        ModBlocks.blockHutCowboy.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutCowboy));
        ModBlocks.blockHutSwineHerder.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutSwineHerder));
        ModBlocks.blockHutChickenHerder.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutChickenHerder));
        ModBlocks.blockHutBarracksTower.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBarracksTower));
        ModBlocks.blockHutBarracks.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBarracks));
        ModBlocks.blockHutCook.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutCook));
        ModBlocks.blockHutSmeltery.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutSmeltery));
        ModBlocks.blockHutComposter.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutComposter));
        ModBlocks.blockHutLibrary.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutLibrary));
        ModBlocks.blockHutArchery.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutArchery));
        ModBlocks.blockHutCombatAcademy.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutCombatAcademy));
        ModBlocks.blockHutSawmill.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutSawmill));
        ModBlocks.blockHutStoneSmeltery.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutStoneSmeltery));
        ModBlocks.blockHutCrusher.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutCrusher));
        ModBlocks.blockHutSifter.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutSifter));
        ModBlocks.blockHutFlorist.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutFlorist));
        ModBlocks.blockHutEnchanter.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutEnchanter));
        ModBlocks.blockHutUniversity.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutUniversity));
        ModBlocks.blockHutHospital.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutHospital));
        ModBlocks.blockHutSchool.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutSchool));
        ModBlocks.blockHutGlassblower.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutGlassblower));
        ModBlocks.blockHutDyer.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutDyer));
        ModBlocks.blockHutFletcher.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutFletcher));
        ModBlocks.blockHutMechanic.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutMechanic));
        ModBlocks.blockHutTavern.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutTavern));
        ModBlocks.blockHutPlantation.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutPlantation));
        ModBlocks.blockPlantationField.registerBlockItem(registry, blockItemProps(ModBlocks.blockPlantationField));
        ModBlocks.blockHutRabbitHutch.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutRabbitHutch));
        ModBlocks.blockHutConcreteMixer.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutConcreteMixer));
        ModBlocks.blockHutBeekeeper.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutBeekeeper));
        ModBlocks.blockHutMysticalSite.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutMysticalSite));
        ModBlocks.blockHutGraveyard.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutGraveyard));
        ModBlocks.blockHutNetherWorker.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutNetherWorker));
        ModBlocks.blockHutAlchemist.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutAlchemist));
        ModBlocks.blockHutKitchen.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutKitchen));
        ModBlocks.blockHutGateHouse.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutGateHouse));
        ModBlocks.blockHutStable.registerBlockItem(registry, blockItemProps(ModBlocks.blockHutStable));

        ModBlocks.blockConstructionTape.registerBlockItem(registry, blockItemProps(ModBlocks.blockConstructionTape));
        ModBlocks.blockRack.registerBlockItem(registry, blockItemProps(ModBlocks.blockRack));
        ModBlocks.blockGrave.registerBlockItem(registry, blockItemProps(ModBlocks.blockGrave));
        ModBlocks.blockNamedGrave.registerBlockItem(registry, blockItemProps(ModBlocks.blockNamedGrave));
        ModBlocks.blockWayPoint.registerBlockItem(registry, blockItemProps(ModBlocks.blockWayPoint));
        ModBlocks.blockBarrel.registerBlockItem(registry, blockItemProps(ModBlocks.blockBarrel));
        ModBlocks.blockPostBox.registerBlockItem(registry, blockItemProps(ModBlocks.blockPostBox));
        ModBlocks.blockStash.registerBlockItem(registry, blockItemProps(ModBlocks.blockStash));
        ModBlocks.blockDecorationPlaceholder.registerBlockItem(registry, blockItemProps(ModBlocks.blockDecorationPlaceholder));
        ModBlocks.blockCompostedDirt.registerBlockItem(registry, blockItemProps(ModBlocks.blockCompostedDirt));
        ModBlocks.farmland.registerBlockItem(registry, blockItemProps(ModBlocks.farmland));
        ModBlocks.floodedFarmland.registerBlockItem(registry, blockItemProps(ModBlocks.floodedFarmland));
        ModBlocks.blockColonySign.registerBlockItem(registry, blockItemProps(ModBlocks.blockColonySign));

        ModBlocks.blockBellPepper.registerBlockItem(registry, blockItemProps(ModBlocks.blockBellPepper));
        ModBlocks.blockCabbage.registerBlockItem(registry, blockItemProps(ModBlocks.blockCabbage));
        ModBlocks.blockChickpea.registerBlockItem(registry, blockItemProps(ModBlocks.blockChickpea));
        ModBlocks.blockDurum.registerBlockItem(registry, blockItemProps(ModBlocks.blockDurum));
        ModBlocks.blockEggplant.registerBlockItem(registry, blockItemProps(ModBlocks.blockEggplant));
        ModBlocks.blockGarlic.registerBlockItem(registry, blockItemProps(ModBlocks.blockGarlic));
        ModBlocks.blockOnion.registerBlockItem(registry, blockItemProps(ModBlocks.blockOnion));
        ModBlocks.blockSoyBean.registerBlockItem(registry, blockItemProps(ModBlocks.blockSoyBean));
        ModBlocks.blockTomato.registerBlockItem(registry, blockItemProps(ModBlocks.blockTomato));
        ModBlocks.blockRice.registerBlockItem(registry, blockItemProps(ModBlocks.blockRice));
        ModBlocks.blockButternutSquash.registerBlockItem(registry, blockItemProps(ModBlocks.blockButternutSquash));
        ModBlocks.blockCorn.registerBlockItem(registry, blockItemProps(ModBlocks.blockCorn));
        ModBlocks.blockMint.registerBlockItem(registry, blockItemProps(ModBlocks.blockMint));
        ModBlocks.blockNetherPepper.registerBlockItem(registry, blockItemProps(ModBlocks.blockNetherPepper));
        ModBlocks.blockPeas.registerBlockItem(registry, blockItemProps(ModBlocks.blockPeas));

        ModBlocks.blockSimpleQuarry.registerBlockItem(registry, blockItemProps(ModBlocks.blockSimpleQuarry));
        ModBlocks.blockMediumQuarry.registerBlockItem(registry, blockItemProps(ModBlocks.blockMediumQuarry));
        //ModBlocks.blockLargeQuarry.registerBlockItem(registry, blockItemProps(ModBlocks.blockLargeQuarry));
    }
}
