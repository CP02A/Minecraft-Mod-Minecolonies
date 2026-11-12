package com.minecolonies.gametest;

import com.ldtteam.shim.neoforge.capabilities.Capabilities;
import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ldtteam.shim.neoforge.common.conditions.ICondition;
import com.ldtteam.shim.neoforge.common.conditions.TagEmptyCondition;
import com.ldtteam.shim.neoforge.event.AddReloadListenerEvent;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.CraftingUtils;
import com.minecolonies.core.datalistener.BaseDataListener;
import com.minecolonies.core.datalistener.util.MappingResult;
import com.minecolonies.core.entity.other.FireArrowEntity;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Game tests for composting, for the parts of the NeoForge shim MineColonies relies on (fire arrows, creative tab contents, tag conditions)
 * and for the item transfer bridge between the shim's {@code IItemHandler} capability and the Fabric Transfer API
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

    /**
     * MineColonies food and crops carry the vanilla compostable component with the vanilla chance tiers.
     */
    @GameTest
    public void minecoloniesFoodIsCompostable(final GameTestHelper helper)
    {
        helper.assertValueEqual(tierOf("cabbage"), ContextIntProviders.COMPOSTABLE_LOW_MEDIUM, "cabbage compost tier");
        helper.assertValueEqual(tierOf("golden_bread"), ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH, "golden bread compost tier");
        helper.assertValueEqual(tierOf("apple_pie"), ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE, "apple pie compost tier");

        // always_add_one is deterministic: one apple pie raises an empty composter to level 1.
        helper.setBlock(POS, Blocks.COMPOSTER);
        final Storage<ItemVariant> composter = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(composter != null, "Composter has no Fabric item storage");
        try (Transaction tx = Transaction.openOuter())
        {
            helper.assertValueEqual(composter.insert(ItemVariant.of(item("apple_pie")), 1, tx), 1L, "apple pies accepted by the composter");
            tx.commit();
        }
        helper.assertValueEqual(helper.getBlockState(POS).getValue(ComposterBlock.LEVEL), 1, "composter level after an apple pie");

        helper.succeed();
    }

    /**
     * FAB-05: the pharao scepter shoots fire arrows (ItemFireArrow#createArrow, called by vanilla's projectile weapon code).
     */
    @GameTest
    public void pharaoScepterShootsFireArrows(final GameTestHelper helper)
    {
        final ServerPlayer player = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.fromString("6d3b3a8e-9c1f-4f63-a3c5-0e2e2e0f0a05"), "E2EArcher"));
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        final BlockPos stand = helper.absolutePos(POS);
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        player.setXRot(-30f);

        final ItemStack scepter = new ItemStack(ModItems.pharaoscepter);
        player.setItemInHand(InteractionHand.MAIN_HAND, scepter);
        player.getInventory().add(new ItemStack(Items.ARROW, 2));
        scepter.getItem().releaseUsing(scepter, helper.getLevel(), player, scepter.getUseDuration(player) - 20);

        final List<FireArrowEntity> arrows = helper.getLevel().getEntitiesOfClass(FireArrowEntity.class, player.getBoundingBox().inflate(4));
        try
        {
            helper.assertValueEqual(arrows.size(), 1, "fire arrows fired");
            helper.assertTrue(arrows.get(0).getOwner() == player, "the fire arrow's owner is not the shooter");
            helper.assertTrue(arrows.get(0).position().distanceTo(player.position()) < 3, "the fire arrow starts at " + arrows.get(0).position() + ", not at the shooter");
            helper.assertTrue(arrows.get(0).isOnFire(), "the fire arrow is not burning");
            helper.assertValueEqual(player.getInventory().countItem(Items.ARROW), 1, "arrows left in the inventory");
        }
        finally
        {
            arrows.forEach(Entity::discard);
            player.getInventory().clearContent();
        }
        helper.succeed();
    }

    /**
     * FAB-06: a dropped fire arrow stays an item on the ground (NeoForge's createEntity hook turned it into an arrow at 0, 0, 0; not ported).
     */
    @GameTest
    public void droppedFireArrowStaysAnItem(final GameTestHelper helper)
    {
        final BlockPos abs = helper.absolutePos(POS);
        final ItemEntity drop = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, new ItemStack(ModItems.firearrow, 4));
        helper.getLevel().addFreshEntity(drop);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(drop.isAlive(), "the dropped fire arrows are gone");
            helper.assertTrue(drop.getItem().is(ModItems.firearrow) && drop.getItem().getCount() == 4, "the item entity holds " + drop.getItem());
            drop.discard();
            helper.succeed();
        });
    }

    /**
     * FAB-07: items other Fabric mods add to a vanilla creative tab (Fabric API CreativeModeTabEvents) are in MineColonies' list of
     * creative tab items, also where the tabs were never built (dedicated server).
     */
    @GameTest
    public void creativeTabAdditionsReachMineColonies(final GameTestHelper helper)
    {
        final String marker = "e2e creative tab marker";
        if (!tabListenerRegistered)
        {
            tabListenerRegistered = true;
            CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
                final ItemStack stack = new ItemStack(Items.STICK);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(marker));
                output.accept(stack);
            });
        }
        final CreativeModeTab ingredients = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.INGREDIENTS);
        helper.assertTrue(ingredients.getDisplayItems().isEmpty(), "the ingredients tab was built on the server; the test needs an unbuilt tab");

        final List<ItemStack> found = new ArrayList<>();
        final CreativeModeTab.ItemDisplayParameters params =
          new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), false, helper.getLevel().registryAccess());
        CraftingUtils.forEachCreativeTabItems(params, (tab, stacks) -> {
            if (tab == ingredients)
            {
                found.addAll(stacks);
            }
        });
        helper.assertTrue(found.stream().anyMatch(s -> s.is(Items.IRON_INGOT)), "the tab's own items are missing: " + found.size() + " items");
        helper.assertTrue(found.stream().anyMatch(s -> s.is(Items.STICK) && marker.equals(s.getHoverName().getString())),
          "the item added through Fabric API is missing");
        helper.succeed();
    }

    /**
     * FAB-08: tag conditions ({@code neoforge:tag_empty}) in MineColonies data files see the loaded tags, as NeoForge's staged tags did.
     */
    @GameTest
    public void tagConditionsSeeLoadedTags(final GameTestHelper helper)
    {
        final ICondition.IContext context =
          new AddReloadListenerEvent(null, helper.getLevel().registryAccess(), helper.getLevel().registryAccess()).getConditionContext();
        helper.assertFalse(new TagEmptyCondition(ItemTags.LOGS).test(context), "minecraft:logs looks empty to tag conditions");
        helper.assertTrue(new TagEmptyCondition("minecolonies-gametest", "no_such_tag").test(context), "a missing tag does not look empty");

        final ConditionProbe probe = new ConditionProbe();
        probe.injectConditionContext(context);
        final Map<Identifier, JsonElement> files = new HashMap<>();
        files.put(Identifier.fromNamespaceAndPath("minecolonies-gametest", "logs_not_empty"),
          JsonParser.parseString("{\"neoforge:conditions\":[{\"type\":\"neoforge:not\",\"value\":{\"type\":\"neoforge:tag_empty\",\"tag\":\"minecraft:logs\"}}]}"));
        files.put(Identifier.fromNamespaceAndPath("minecolonies-gametest", "logs_empty"),
          JsonParser.parseString("{\"neoforge:conditions\":[{\"type\":\"neoforge:tag_empty\",\"tag\":\"minecraft:logs\"}]}"));
        probe.load(files);
        helper.assertValueEqual(probe.getEntries().keySet(), Set.of(Identifier.fromNamespaceAndPath("minecolonies-gametest", "logs_not_empty")),
          "entries loaded by a MineColonies data listener");
        helper.succeed();
    }

    private static boolean tabListenerRegistered;

    /**
     * A MineColonies json data listener (BaseDataListener) that keeps every entry whose conditions pass.
     */
    private static final class ConditionProbe extends BaseDataListener<String>
    {
        private ConditionProbe()
        {
            super("e2e_condition_probe");
        }

        private void load(final Map<Identifier, JsonElement> files)
        {
            apply(files, null, InactiveProfiler.INSTANCE);
        }

        @Override
        protected MappingResult<String> mapEntry(final Identifier key, final JsonObject object)
        {
            return MappingResult.ok(key.toString());
        }
    }

    private static Item item(final String name)
    {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecolonies", name));
    }

    @Nullable
    private static ResourceKey<ContextIntProvider> tierOf(final String name)
    {
        final Compostable compostable = new ItemStack(item(name)).get(DataComponents.COMPOSTABLE);
        if (compostable == null || !(compostable.layers() instanceof ResolvableInt.Reference reference))
        {
            return null;
        }
        return reference.key();
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
