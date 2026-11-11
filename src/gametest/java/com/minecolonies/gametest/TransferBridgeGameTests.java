package com.minecolonies.gametest;

import com.ldtteam.shim.neoforge.capabilities.Capabilities;
import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.minecolonies.api.blocks.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Game tests for the item transfer bridge between the shim's {@code IItemHandler} capability and the Fabric Transfer API
 * (docs/Isolation-rules.md, rule 6), in both directions. Run with {@code ./gradlew runGameTest}.
 */
public class TransferBridgeGameTests
{
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * MineColonies rack (shim item handler) seen by other mods through Fabric's {@code ItemStorage.SIDED}.
     */
    @GameTest
    public void fabricStorageSeesMinecoloniesRack(final GameTestHelper helper)
    {
        helper.setBlock(POS, ModBlocks.blockRack);
        final BlockPos abs = helper.absolutePos(POS);

        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), abs, Direction.UP);
        helper.assertTrue(storage != null, "Rack is not exposed as a Fabric item storage");

        final IItemHandler rack = Capabilities.ItemHandler.BLOCK.getCapability(helper.getLevel(), abs, null, null, null);
        helper.assertTrue(rack != null, "Rack has no shim item handler");

        final ItemVariant diamond = ItemVariant.of(Items.DIAMOND);

        // An aborted transaction must leave the rack untouched.
        try (Transaction tx = Transaction.openOuter())
        {
            helper.assertValueEqual(storage.insert(diamond, 10, tx), 10L, "simulated insert amount");
        }
        helper.assertValueEqual(count(rack, Items.DIAMOND), 0, "diamonds in rack after aborted insert");

        try (Transaction tx = Transaction.openOuter())
        {
            helper.assertValueEqual(storage.insert(diamond, 10, tx), 10L, "inserted amount");
            tx.commit();
        }
        helper.assertValueEqual(count(rack, Items.DIAMOND), 10, "diamonds in rack after insert");

        try (Transaction tx = Transaction.openOuter())
        {
            helper.assertValueEqual(storage.extract(diamond, 4, tx), 4L, "extracted amount");
            tx.commit();
        }
        helper.assertValueEqual(count(rack, Items.DIAMOND), 6, "diamonds in rack after extract");

        helper.succeed();
    }

    /**
     * Vanilla container (no Fabric provider) seen through the shim's item handler capability.
     */
    @GameTest
    public void shimItemHandlerSeesVanillaChest(final GameTestHelper helper)
    {
        helper.setBlock(POS, Blocks.CHEST);
        final BlockPos abs = helper.absolutePos(POS);

        final IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(helper.getLevel(), abs, null, null, Direction.UP);
        helper.assertTrue(handler != null, "Chest has no shim item handler");

        final ItemStack remainder = insert(handler, new ItemStack(Items.IRON_INGOT, 5));
        helper.assertTrue(remainder.isEmpty(), "Chest did not accept all iron ingots");

        final ChestBlockEntity chest = helper.getBlockEntity(POS, ChestBlockEntity.class);
        int inChest = 0;
        for (int i = 0; i < chest.getContainerSize(); i++)
        {
            if (chest.getItem(i).is(Items.IRON_INGOT))
            {
                inChest += chest.getItem(i).getCount();
            }
        }
        helper.assertValueEqual(inChest, 5, "iron ingots in chest");

        helper.succeed();
    }

    /**
     * Block that only has a Fabric storage provider (the composter) seen through the shim's item handler capability.
     */
    @GameTest
    public void shimItemHandlerSeesFabricStorage(final GameTestHelper helper)
    {
        helper.setBlock(POS, Blocks.COMPOSTER);
        final BlockPos abs = helper.absolutePos(POS);
        helper.assertTrue(ItemStorage.SIDED.getProvider(Blocks.COMPOSTER) != null, "Fabric API has no composter storage provider");

        final IItemHandler handler = Capabilities.ItemHandler.BLOCK.getCapability(helper.getLevel(), abs, null, null, Direction.UP);
        helper.assertTrue(handler != null, "Composter has no shim item handler");

        // Pumpkin pie always raises the compost level (chance 1.0).
        final ItemStack remainder = insert(handler, new ItemStack(Items.PUMPKIN_PIE, 1));
        helper.assertTrue(remainder.isEmpty(), "Composter did not accept the pumpkin pie");
        helper.assertValueEqual(helper.getBlockState(POS).getValue(ComposterBlock.LEVEL), 1, "composter level");

        helper.succeed();
    }

    private static ItemStack insert(final IItemHandler handler, ItemStack stack)
    {
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++)
        {
            stack = handler.insertItem(slot, stack, false);
        }
        return stack;
    }

    private static int count(final IItemHandler handler, final net.minecraft.world.item.Item item)
    {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (stack.is(item))
            {
                total += stack.getCount();
            }
        }
        return total;
    }
}
