package com.minecolonies.gametest.framework;

import com.ldtteam.shim.neoforge.common.ModConfigSpec;
import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blocks.interfaces.ISpecialCreativeHandlerAnchorBlock;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.operations.PlaceStructureOperation;
import com.ldtteam.structurize.placement.StructurePlacer;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.storage.StructurePackMeta;
import com.ldtteam.structurize.storage.StructurePacks;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.managers.interfaces.IRaiderManager;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.api.research.ILocalResearch;
import com.minecolonies.api.research.ILocalResearchTree;
import com.minecolonies.api.research.IResearchEffect;
import com.minecolonies.api.research.util.ResearchState;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import com.minecolonies.core.colony.workorders.WorkOrderBuilding;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.server.CreateColonyMessage;
import com.minecolonies.core.research.LocalResearch;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.gametest.mixin.GameTestHelperAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Builds the starting situation of a colony test directly ("skipping ahead", docs/e2e-tests/framework.md) and cleans it up when the test
 * ends, passed or failed. One fixture per test run: {@code ColonyFixture f = ColonyFixture.create(helper);} as the first line of the test.
 * <p>
 * Positions passed to the fixture are <b>relative to the test structure</b>, like everything in {@link GameTestHelper}. Use
 * {@link #abs(BlockPos)} to convert. In the {@link #ARENA} the floor's top layer is at y = {@link #FLOOR_Y}; the first free layer (where huts
 * stand) is y = {@link #GROUND_Y}.
 * <p>
 * Everything here uses the same MineColonies/Structurize entry points a player's actions end up in (build tool paste, colony creation
 * message, hire module, building work order request), so the tests exercise the real code.
 */
public final class ColonyFixture
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyFixture.class);

    /**
     * Test environment of colony tests. Tests in it run one at a time ({@link SerialBatches}). Use it in every test that creates a colony.
     */
    public static final String COLONY_ENVIRONMENT = "minecolonies-gametest:colony";

    public static final ResourceKey<TestEnvironmentDefinition<?>> COLONY_ENVIRONMENT_ID =
      ResourceKey.create(Registries.TEST_ENVIRONMENT, Identifier.parse(COLONY_ENVIRONMENT));

    /**
     * Flat 64 x 32 x 64 arena (empty structure; {@link #create} lays the floor).
     */
    public static final String ARENA = "minecolonies-gametest:arena64";

    public static final int ARENA_SIZE   = 64;
    public static final int ARENA_HEIGHT = 32;

    /**
     * Relative y of the floor's top block (grass). Below it: dirt down to y = 0, then the test's barrier floor.
     */
    public static final int FLOOR_Y  = 4;
    /**
     * Relative y of the first free layer above the floor.
     */
    public static final int GROUND_Y = FLOOR_Y + 1;

    /**
     * Relative center of the arena, on the ground.
     */
    public static final BlockPos ARENA_CENTER = new BlockPos(ARENA_SIZE / 2, GROUND_Y, ARENA_SIZE / 2);

    /**
     * Absolute y of the lowest layer {@link #makeDeepGround()} fills with stone. Mining levels are absolute heights (y 48, 16, -16, -100,
     * {@code BuildingMiner.MINING_LEVELS}); a level 1 mine in the arena (ground at about y 9) digs its shaft down to y -16, so this leaves
     * 16 layers of stone below the first level for its nodes.
     */
    public static final int DEEP_GROUND_BOTTOM_Y = -32;

    /**
     * The fake owner: fixed UUID and name, so ownership and permissions work as for a real owner.
     */
    public static final UUID   OWNER_ID   = UUID.fromString("6d3b3a8e-9c1f-4f63-a3c5-0e2e2e000001");
    public static final String OWNER_NAME = "E2EOwner";

    /**
     * Structure pack the huts are pasted from, unless a test chooses another one.
     */
    public static final String DEFAULT_PACK = "Minecolonies Original";

    private final GameTestHelper helper;
    private final GameTestInfo   info;
    private final ServerLevel    level;
    private final FakePlayer     owner;
    private final Deque<Runnable> restore = new ArrayDeque<>();
    private final Deque<Runnable> endActions = new ArrayDeque<>();

    private String  pack = DEFAULT_PACK;
    @Nullable
    private IColony colony;
    /**
     * Colonies created besides the main one ({@link #createSecondColony}, {@link #createBareColony}); deleted when the test ends.
     */
    private final List<IColony> extraColonies = new ArrayList<>();
    /**
     * Fake players handed out by {@link #fakePlayer}, reset when the test ends.
     */
    private final List<ServerPlayer> players = new ArrayList<>();
    private boolean deepGround;
    private boolean cleanedUp;

    private ColonyFixture(final GameTestHelper helper)
    {
        this.helper = helper;
        this.info = ((GameTestHelperAccessor) helper).minecoloniesGametest$getTestInfo();
        this.level = helper.getLevel();
        this.owner = FakePlayer.get(level, new GameProfile(OWNER_ID, OWNER_NAME));
        resetPlayer(owner, GameType.CREATIVE);
        players.add(owner);
    }

    /**
     * {@code FakePlayer.get} hands out one cached player per profile, so a fake player keeps inventory, game mode, sneaking and health from
     * the test before. Every test starts with a clean one.
     */
    private static void resetPlayer(final ServerPlayer player, final GameType gameType)
    {
        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.setGameMode(gameType);
        player.setShiftKeyDown(false);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
    }

    /**
     * Creates the fixture for this test: lays the arena floor (grass on dirt), sets day and clear weather, deletes colonies left over by a
     * crashed earlier test, and registers the cleanup that runs when the test ends (passed or failed).
     */
    public static ColonyFixture create(final GameTestHelper helper)
    {
        final ColonyFixture fixture = new ColonyFixture(helper);
        fixture.info.addListener(new GameTestListener()
        {
            @Override
            public void testStructureLoaded(final GameTestInfo testInfo)
            {
            }

            @Override
            public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner)
            {
                fixture.cleanup();
            }

            @Override
            public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner)
            {
                fixture.cleanup();
            }

            @Override
            public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner)
            {
            }
        });

        fixture.deleteLeftoverColonies();
        fixture.fillFloor(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState());
        fixture.setTime(1000);
        fixture.setWeather(false, false);
        return fixture;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Basics
    // ------------------------------------------------------------------------------------------------------------------------------------

    public GameTestHelper helper()
    {
        return helper;
    }

    public ServerLevel level()
    {
        return level;
    }

    /**
     * @return the fake owner player: creative mode, empty inventory and full health at the start of every test; positioned at the town
     *     hall by {@link #createColony}. Not in the level's or the server's player list unless {@link #addOwnerToPlayerLists()} is called.
     */
    public ServerPlayer owner()
    {
        return owner;
    }

    /**
     * Another fake player (a friend, a second colony's owner, a stranger), reset like the owner: empty inventory, not sneaking, full health.
     * The same name gives the same player (and UUID) in every test.
     */
    public ServerPlayer fakePlayer(final String name, final GameType gameType)
    {
        final UUID id = UUID.nameUUIDFromBytes(("minecolonies-e2e:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final FakePlayer player = FakePlayer.get(level, new GameProfile(id, name));
        resetPlayer(player, gameType);
        if (!players.contains(player))
        {
            players.add(player);
        }
        return player;
    }

    /**
     * Puts the fake owner into the level's and the server's player lists for this test, so code that looks players up there finds him
     * (guard follow via {@code ServerUtils.getPlayerFromUUID}, rally banners, raid bars, messages to online owners). Removed again when the
     * test ends. Only use it when the test needs it: everything that iterates online players now sees him.
     */
    public void addOwnerToPlayerLists()
    {
        addToPlayerLists(owner);
    }

    /**
     * Puts a fake player into the level's and the server's player lists for this test; see {@link #addOwnerToPlayerLists()}.
     */
    @SuppressWarnings("unchecked")
    public void addToPlayerLists(final ServerPlayer player)
    {
        final List<ServerPlayer> inLevel = (List<ServerPlayer>) level.players();
        final List<ServerPlayer> online = level.getServer().getPlayerList().getPlayers();
        if (!inLevel.contains(player))
        {
            inLevel.add(player);
        }
        if (!online.contains(player))
        {
            online.add(player);
        }
        onTestEnd(() -> {
            inLevel.remove(player);
            online.remove(player);
        });
    }

    /**
     * Runs the action when the test ends, passed or failed, before the fixture's own cleanup (colonies still exist). Actions run in
     * reverse order of registration.
     */
    public void onTestEnd(final Runnable action)
    {
        endActions.push(action);
    }

    /**
     * @return the colony created by {@link #createColony}.
     */
    public IColony colony()
    {
        if (colony == null)
        {
            throw new IllegalStateException("No colony yet: call createColony() first");
        }
        return colony;
    }

    public BlockPos abs(final BlockPos relative)
    {
        return helper.absolutePos(relative);
    }

    /**
     * The inverse of {@link #abs(BlockPos)}. (Vanilla's {@code GameTestHelper.relativePos} is not: it turns the position by the test's
     * rotation plus 180 degrees, so with no rotation it mirrors x and z.)
     */
    public BlockPos rel(final BlockPos absolute)
    {
        final BlockPos origin = info.getTestOrigin();
        final Rotation inverse = switch (info.getRotation())
        {
            case NONE -> Rotation.NONE;
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
        };
        return StructureTemplate.transform(absolute, Mirror.NONE, inverse, origin).subtract(origin);
    }

    /**
     * Starts a step-by-step flow (actions, waits for outcomes, checks). See {@link TestFlow}.
     */
    public TestFlow flow()
    {
        return new TestFlow(helper);
    }

    /**
     * Uses another structure pack for the huts pasted after this call.
     */
    public ColonyFixture usePack(final String packName)
    {
        this.pack = packName;
        return this;
    }

    /**
     * Replaces the arena floor (top layer and the layers below it).
     */
    public void fillFloor(final BlockState top, final BlockState below)
    {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < ARENA_SIZE; x++)
        {
            for (int z = 0; z < ARENA_SIZE; z++)
            {
                for (int y = 0; y <= FLOOR_Y; y++)
                {
                    pos.set(x, y, z);
                    level.setBlock(abs(pos), y == FLOOR_Y ? top : below, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /**
     * "Deep arena" for mines: replaces the test's barrier floor under the whole arena with stone and fills stone below it down to
     * {@link #DEEP_GROUND_BOTTOM_Y}, so a level 1 mine's shaft and its first mining level (y -16) are in solid stone. Call it before placing
     * the mine. Below the arena's floor the world is otherwise empty (the test world's ground is at y -61), so keep the mine near the arena
     * center: the stone ends at the arena's sides. Removed again when the test ends.
     */
    public void makeDeepGround()
    {
        if (deepGround)
        {
            return;
        }
        deepGround = true;
        fillBelowArena(Blocks.STONE.defaultBlockState());
    }

    /**
     * Fills every block below the arena (relative y -1, the barrier floor, down to {@link #DEEP_GROUND_BOTTOM_Y}) with the state.
     */
    private void fillBelowArena(final BlockState state)
    {
        final int top = abs(new BlockPos(0, -1, 0)).getY();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < ARENA_SIZE; x++)
        {
            for (int z = 0; z < ARENA_SIZE; z++)
            {
                final BlockPos column = abs(new BlockPos(x, 0, z));
                for (int y = top; y >= DEEP_GROUND_BOTTOM_Y; y--)
                {
                    pos.set(column.getX(), y, column.getZ());
                    level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Colony and buildings
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Founds a colony with a level 1 town hall at the arena center. See {@link #createColony(BlockPos)}.
     */
    public IColony createColony()
    {
        return createColony(ARENA_CENTER);
    }

    /**
     * Founds a colony owned by the fake player, the way a creative player does it: pastes the level 1 town hall with the build tool
     * (Structurize's creative placement, done instantly), then runs the server side of the "create colony" message the town hall window
     * sends. The town hall ends up registered at level 1.
     *
     * @param townHallPos relative position of the town hall block.
     */
    public IColony createColony(final BlockPos townHallPos)
    {
        final BlockPos absPos = abs(townHallPos);
        owner.setPos(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5);

        final PastedBlueprint pasted = pasteHut(ModBuildings.TOWNHALL_ID, 1, absPos, RotationMirror.NONE);

        final CreateColonyMessage message = new CreateColonyMessage(absPos, false, "E2E colony " + info.id().getPath(), pack, pasted.subPath());
        try
        {
            final Method onExecute = CreateColonyMessage.class.getDeclaredMethod("onExecute",
              com.ldtteam.shim.neoforge.network.handling.IPayloadContext.class, ServerPlayer.class);
            onExecute.setAccessible(true);
            onExecute.invoke(message, null, owner);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("Could not run CreateColonyMessage on the server", e);
        }

        final IColony created = IColonyManager.getInstance().getColonyByPosFromWorld(level, absPos);
        if (created == null || !created.getCenter().equals(absPos))
        {
            throw new IllegalStateException("Colony was not created at " + absPos + " (found " + created + ")");
        }
        this.colony = created;
        return created;
    }

    /**
     * Creates another colony owned by {@code owner} (e.g. {@code fakePlayer("E2EOwnerB", GameType.CREATIVE)}): pastes a level 1 town hall
     * like the build tool and creates the colony the way the server side of the "create colony" message does, but without its distance
     * check (two colonies at the normal distance don't fit into the arena). Use {@code setConfig(...initialColonySize, 1)} so the claims
     * don't overlap. Automatic citizen arrival is off. The fixture's owner becomes an officer, so the fixture can paste huts into it
     * ({@link #placeBuilding(IColony, BuildingEntry, int, BlockPos)}). Deleted when the test ends. {@link #colony()} stays the main colony.
     *
     * @param townHallPos relative position of the town hall block.
     */
    public IColony createSecondColony(final BlockPos townHallPos, final ServerPlayer colonyOwner)
    {
        final BlockPos absPos = abs(townHallPos);
        colonyOwner.setPos(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5);
        final PastedBlueprint pasted = pasteHut(ModBuildings.TOWNHALL_ID, 1, absPos, RotationMirror.NONE);
        return registerExtraColony(absPos, colonyOwner, "E2E colony of " + colonyOwner.getName().getString(), pasted.subPath(), false);
    }

    /**
     * Creates a colony with only a town hall <i>block</i> at level 1 (no town hall structure; small enough to put two colonies and some huts
     * into the arena), owned by {@code owner} (may be the fixture's owner). Otherwise like {@link #createSecondColony}: no distance check,
     * citizen arrival off, the fixture's owner is an officer, deleted when the test ends, not the fixture's {@link #colony()}.
     */
    public IColony createBareColony(final BlockPos townHallPos, final ServerPlayer colonyOwner, final String name)
    {
        final BlockPos absPos = abs(townHallPos);
        colonyOwner.setPos(absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5);
        level.setBlockAndUpdate(absPos, ModBlocks.blockHutTownHall.defaultBlockState());
        return registerExtraColony(absPos, colonyOwner, name, "fundamentals/townhall1.blueprint", true);
    }

    private IColony registerExtraColony(final BlockPos absPos, final ServerPlayer colonyOwner, final String name, final String blueprintPath, final boolean setLevel)
    {
        if (!(level.getBlockEntity(absPos) instanceof final TileEntityColonyBuilding hut))
        {
            throw new IllegalStateException("No town hall block entity at " + absPos);
        }
        hut.setStructurePack(StructurePacks.getStructurePack(pack));
        hut.setBlueprintPath(blueprintPath);
        final IColony created = IColonyManager.getInstance().createColony(level, absPos, colonyOwner, name, pack);
        extraColonies.add(created);
        final IBuilding hall = created.getServerBuildingManager().addNewBuilding(hut, level);
        if (setLevel)
        {
            hall.setBuildingLevel(1);
        }
        final var moveIn = created.getSettings().getSetting(BuildingTownHall.MOVE_IN);
        if (moveIn.getValue())
        {
            moveIn.trigger();
        }
        if (!OWNER_ID.equals(colonyOwner.getUUID()))
        {
            created.getPermissions().addPlayer(OWNER_ID, OWNER_NAME, created.getPermissions().getRankOfficer());
        }
        return created;
    }

    /**
     * Pastes the hut's blueprint instantly (Structurize creative build tool, "pretty" placement) and registers the building at that level,
     * as if a builder had built it.
     *
     * @param type  the building type, e.g. {@code ModBuildings.builder.get()}.
     * @param level the building level (1..5; the blueprint {@code <registry path><level>}, e.g. {@code builder1}, must exist in the pack).
     * @param pos   relative position of the hut block.
     */
    public IBuilding placeBuilding(final BuildingEntry type, final int level, final BlockPos pos)
    {
        return placeBuilding(type, level, pos, RotationMirror.NONE);
    }

    public IBuilding placeBuilding(final BuildingEntry type, final int level, final BlockPos pos, final RotationMirror rotationMirror)
    {
        return placeBuilding(colony(), type, level, pos, rotationMirror);
    }

    /**
     * Like {@link #placeBuilding(BuildingEntry, int, BlockPos)}, for another colony than the fixture's (see {@link #createSecondColony}).
     * The position must be in that colony's claim, and the fixture's owner must be allowed to place there (he is an officer of colonies
     * the fixture creates).
     */
    public IBuilding placeBuilding(final IColony colony, final BuildingEntry type, final int level, final BlockPos pos)
    {
        return placeBuilding(colony, type, level, pos, RotationMirror.NONE);
    }

    public IBuilding placeBuilding(final IColony colony, final BuildingEntry type, final int level, final BlockPos pos, final RotationMirror rotationMirror)
    {
        final BlockPos absPos = abs(pos);
        pasteHut(type.getRegistryName().getPath(), level, absPos, rotationMirror);
        final IBuilding building = colony.getServerBuildingManager().getBuilding(absPos);
        if (building == null)
        {
            final IColony owning = IColonyManager.getInstance().getColonyByPosFromWorld(this.level, absPos);
            throw new IllegalStateException("Building " + type.getRegistryName() + " was pasted at " + absPos + " but not registered in colony " + colony.getID()
                                              + " (the position belongs to " + (owning == null ? "no colony" : "colony " + owning.getID()) + ")");
        }
        if (building.getBuildingLevel() != level)
        {
            throw new IllegalStateException("Building " + type.getRegistryName() + " at " + absPos + " has level " + building.getBuildingLevel() + ", expected " + level);
        }
        return building;
    }

    /**
     * Result of a paste: the blueprint and its path inside the pack (as the build tool sends it).
     */
    public record PastedBlueprint(Blueprint blueprint, String subPath)
    {
    }

    /**
     * Finds the blueprint {@code <blueprintName><level>} (e.g. {@code builder1}) anywhere in the current pack and pastes it with the hut's
     * creative placement handler. Hut blueprints are named after the building type's registry path ({@code ModBuildings.BUILDER_ID}).
     */
    public PastedBlueprint pasteHut(final String blueprintName, final int buildingLevel, final BlockPos absPos, final RotationMirror rotationMirror)
    {
        final String name = blueprintName + buildingLevel;
        final StructurePackMeta packMeta = StructurePacks.getStructurePack(pack);
        if (packMeta == null)
        {
            throw new IllegalStateException("Structure pack '" + pack + "' is not loaded. Loaded: "
                                              + StructurePacks.getPackMetas().stream().map(StructurePackMeta::getName).toList());
        }
        final Path file = StructurePacks.findBlueprint(pack, name);
        if (file == null)
        {
            throw new IllegalStateException("No blueprint '" + name + "' in pack '" + pack + "'");
        }
        final String subPath = packMeta.getSubPath(file).replace('\\', '/');
        return paste(subPath, absPos, rotationMirror);
    }

    /**
     * Pastes any blueprint of the current pack instantly, the way the creative build tool does ("pretty" placement): hut anchors run their
     * MineColonies setup (registers the building), other blueprints are placed as plain structures.
     *
     * @param subPath path inside the pack, e.g. {@code fundamentals/builder1.blueprint}.
     * @param absPos  absolute anchor position.
     */
    public PastedBlueprint paste(final String subPath, final BlockPos absPos, final RotationMirror rotationMirror)
    {
        final Blueprint blueprint = StructurePacks.getBlueprint(pack, subPath, level.registryAccess());
        if (blueprint == null)
        {
            throw new IllegalStateException("Could not load blueprint '" + subPath + "' from pack '" + pack + "'");
        }
        blueprint.setRotationMirror(rotationMirror, level);

        final BlockState anchor = blueprint.getBlockState(blueprint.getPrimaryBlockOffset());
        final IStructureHandler handler;
        if (anchor.getBlock() instanceof final ISpecialCreativeHandlerAnchorBlock special)
        {
            if (!special.setup(owner, level, absPos, blueprint, rotationMirror, true, pack, subPath))
            {
                throw new IllegalStateException("Build tool setup refused to paste '" + subPath + "' at " + absPos);
            }
            handler = special.getStructureHandler(level, absPos, blueprint, rotationMirror, true);
        }
        else
        {
            handler = new com.ldtteam.structurize.placement.structure.CreativeStructureHandler(level, absPos, blueprint, rotationMirror, true);
        }

        // The build tool queues this operation and runs it over several ticks; we run it to completion right now.
        final PlaceStructureOperation operation = new PlaceStructureOperation(new StructurePlacer(handler), owner);
        int steps = 0;
        while (!operation.apply(level))
        {
            if (++steps > 1_000_000)
            {
                throw new IllegalStateException("Pasting '" + subPath + "' did not finish");
            }
        }
        return new PastedBlueprint(blueprint, subPath);
    }

    /**
     * Requests a work order for the building the way the hut window buttons do ({@code BUILD}/{@code UPGRADE} = build/upgrade to the next
     * level, {@code REPAIR}, {@code REMOVE}). Not claimed by a specific builder.
     *
     * @return the created work order, or null if the colony refused it (the reason is sent to the owner as a chat message).
     */
    @Nullable
    public IServerWorkOrder addWorkOrder(final WorkOrderType type, final IBuilding building)
    {
        switch (type)
        {
            case BUILD, UPGRADE -> building.requestUpgrade(owner, BlockPos.ZERO);
            case REPAIR -> building.requestRepair(BlockPos.ZERO);
            case REMOVE -> building.requestRemoval(owner, BlockPos.ZERO);
        }
        for (final WorkOrderBuilding order : colony().getWorkManager().getWorkOrdersOfType(WorkOrderBuilding.class))
        {
            if (order.getLocation().equals(building.getID()))
            {
                return order;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Citizens
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Spawns a citizen with every skill at {@code skillLevel}, hired into {@code work} (may be null) and living in {@code home} (may be
     * null). Spawns near the work building, else near the home, else near the town hall.
     */
    public ICitizenData spawnCitizen(final int skillLevel, @Nullable final IBuilding work, @Nullable final IBuilding home)
    {
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, skillLevel);
        }
        return spawnCitizen(skills, work, home, null);
    }

    /**
     * Spawns a citizen with the given skill levels (missing skills keep their random level), hired into {@code work}, living in
     * {@code home}.
     *
     * @param spawnAt relative position to spawn near, or null for: the work building, else the home, else the town hall.
     */
    public ICitizenData spawnCitizen(final Map<Skill, Integer> skills, @Nullable final IBuilding work, @Nullable final IBuilding home, @Nullable final BlockPos spawnAt)
    {
        return spawnCitizen(colony(), skills, work, home, spawnAt);
    }

    /**
     * Like {@link #spawnCitizen(int, IBuilding, IBuilding)}, for another colony than the fixture's (see {@link #createSecondColony}).
     */
    public ICitizenData spawnCitizen(final IColony colony, final int skillLevel, @Nullable final IBuilding work, @Nullable final IBuilding home)
    {
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, skillLevel);
        }
        return spawnCitizen(colony, skills, work, home, null);
    }

    /**
     * Like {@link #spawnCitizen(Map, IBuilding, IBuilding, BlockPos)}, for another colony than the fixture's.
     */
    public ICitizenData spawnCitizen(final IColony colony, final Map<Skill, Integer> skills, @Nullable final IBuilding work, @Nullable final IBuilding home,
      @Nullable final BlockPos spawnAt)
    {
        final ICitizenData citizen = colony.getCitizenManager().createAndRegisterCivilianData();
        setSkills(citizen, skills);

        if (home != null)
        {
            final LivingBuildingModule living = home.getFirstModuleOccurance(LivingBuildingModule.class);
            if (living == null || !living.assignCitizen(citizen))
            {
                throw new IllegalStateException("Could not move citizen into " + home.getBuildingDisplayName() + " at " + home.getID()
                                                  + (living == null ? " (no living module)" : " (beds used " + living.getAssignedCitizen().size() + "/" + living.getModuleMax() + ")"));
            }
        }
        if (work != null)
        {
            hire(citizen, work);
        }

        final BlockPos spawnPos;
        if (spawnAt != null)
        {
            spawnPos = abs(spawnAt);
        }
        else if (work != null)
        {
            spawnPos = work.getPosition();
        }
        else if (home != null)
        {
            spawnPos = home.getPosition();
        }
        else
        {
            spawnPos = colony.getCenter();
        }
        colony.getCitizenManager().spawnOrCreateCivilian(citizen, level, List.of(spawnPos), true);
        return citizen;
    }

    /**
     * One-line description of a citizen for failure messages: id, name, position, job, worker AI state, citizen AI state (sleep, eat, work,
     * ...), visible status, and whether the entity is currently following a path.
     */
    public static String describe(@Nullable final ICitizenData citizen)
    {
        if (citizen == null)
        {
            return "<no citizen>";
        }
        final StringBuilder text = new StringBuilder("citizen #").append(citizen.getId()).append(' ').append(citizen.getName());
        citizen.getEntity().ifPresentOrElse(entity -> {
            text.append(" at ").append(entity.blockPosition().toShortString());
            if (entity instanceof final EntityCitizen living)
            {
                text.append(", citizen AI ").append(living.getCitizenAI().getState());
            }
            text.append(", navigating ").append(!entity.getNavigation().isDone());
        }, () -> text.append(" (no entity)"));
        if (citizen.getJob() != null)
        {
            text.append(", job ").append(citizen.getJob().getJobRegistryEntry().getKey().getPath());
            if (citizen.getJob().getWorkerAI() != null)
            {
                text.append(", worker AI ").append(citizen.getJob().getWorkerAI().getState());
            }
        }
        if (citizen.getStatus() != null)
        {
            text.append(", status ").append(citizen.getStatus().getTranslationKey());
        }
        return text.toString();
    }

    /**
     * Sets skill levels exactly.
     */
    public void setSkills(final ICitizenData citizen, final Map<Skill, Integer> skills)
    {
        skills.forEach((skill, target) -> {
            final int current = citizen.getCitizenSkillHandler().getLevel(skill);
            citizen.getCitizenSkillHandler().incrementLevel(skill, target - current);
        });
        citizen.markDirty(0);
    }

    /**
     * Hires the citizen into the building's first job module, as the hire button does.
     */
    public void hire(final ICitizenData citizen, final IBuilding work)
    {
        if (work instanceof IWareHouse)
        {
            throw new IllegalArgumentException("Couriers are not hired at the warehouse (its courier list gives no job): place a courier's hut "
                                                 + "(ModBuildings.deliveryman) and hire the courier there; the warehouse takes him on by itself");
        }
        final IAssignsJob module = work.getFirstModuleOccurance(IAssignsJob.class);
        if (module == null)
        {
            throw new IllegalStateException(work.getBuildingDisplayName() + " has no job module");
        }
        hire(citizen, work, module.getJobEntry());
    }

    /**
     * Hires the citizen into the building's job module for {@code job} (for buildings with several jobs).
     */
    public void hire(final ICitizenData citizen, final IBuilding work, final JobEntry job)
    {
        if (work instanceof IWareHouse)
        {
            hire(citizen, work); // throws with the explanation
        }
        final IAssignsJob module = work.getModuleMatching(IAssignsJob.class, m -> m.getJobEntry() == job);
        if (module == null)
        {
            throw new IllegalStateException(work.getBuildingDisplayName() + " has no job module for " + job.getKey());
        }
        citizen.setPaused(false);
        if (!module.assignCitizen(citizen))
        {
            throw new IllegalStateException("Could not hire citizen " + citizen.getName() + " into " + work.getBuildingDisplayName() + " (full?)");
        }
    }

    /**
     * Sets hunger and health. Health is applied to the living entity (no-op if the citizen has none yet).
     */
    public void setCitizenNeeds(final ICitizenData citizen, final double saturation, final float health)
    {
        citizen.setSaturation(saturation);
        citizen.getEntity().ifPresent(entity -> entity.setHealth(health));
        citizen.markDirty(0);
    }

    /**
     * Turns automatic citizen arrival on or off (town hall setting "move in"). With it on, the colony tops up to {@code initialCitizenAmount}
     * (4) citizens on its own, one about every 1500 ticks; tests that spawn their own citizens usually turn it off.
     */
    public void setMoveIn(final boolean enabled)
    {
        final var setting = colony().getSettings().getSetting(BuildingTownHall.MOVE_IN);
        if (setting.getValue() != enabled)
        {
            setting.trigger();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Inventories
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * @return the building's combined inventory (hut block and all its racks), the same handler MineColonies' workers and couriers use.
     */
    public IItemHandler inventory(final IBuilding building)
    {
        final IItemHandler handler = building instanceof final AbstractBuildingContainer container ? container.getItemHandlerCap((net.minecraft.core.Direction) null) : null;
        if (handler == null)
        {
            throw new IllegalStateException(building.getBuildingDisplayName() + " at " + building.getID() + " has no inventory");
        }
        return handler;
    }

    /**
     * Puts the items into the building's combined inventory (racks and hut block). Fails if they don't fit.
     */
    public void fillBuilding(final IBuilding building, final ItemStack... items)
    {
        final IItemHandler handler = inventory(building);
        for (final ItemStack original : items)
        {
            ItemStack rest = original.copy();
            for (int slot = 0; slot < handler.getSlots() && !rest.isEmpty(); slot++)
            {
                rest = handler.insertItem(slot, rest, false);
            }
            if (!rest.isEmpty())
            {
                throw new IllegalStateException("Not enough room in " + building.getBuildingDisplayName() + " for " + rest);
            }
        }
    }

    /**
     * @return how many items matching the predicate the building's combined inventory holds.
     */
    public int countItems(final IBuilding building, final Predicate<ItemStack> matcher)
    {
        final IItemHandler handler = inventory(building);
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && matcher.test(stack))
            {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Puts the items into the colony's first warehouse.
     */
    public void fillWarehouse(final ItemStack... items)
    {
        final List<IWareHouse> warehouses = colony().getServerBuildingManager().getWareHouses();
        if (warehouses.isEmpty())
        {
            throw new IllegalStateException("The colony has no warehouse");
        }
        fillBuilding(warehouses.get(0), items);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // World, config
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Sets the time of day (0 = sunrise, 1000 = morning, 6000 = noon, 13000 = night, 18000 = midnight). The day/night cycle is off; turn it
     * on with {@code setGameRule(GameRules.ADVANCE_TIME, true)}.
     */
    public void setTime(final long dayTime)
    {
        helper.setTime(dayTime);
    }

    /**
     * Sets the weather. The weather cycle is off, so it stays until changed.
     */
    public void setWeather(final boolean rain, final boolean thunder)
    {
        level.getServer().setWeatherParameters(rain ? 0 : 1_000_000, rain ? 1_000_000 : 0, rain, rain && thunder);
    }

    /**
     * Changes a game rule for this test; restored when the test ends.
     */
    public <T> void setGameRule(final GameRule<T> rule, final T value)
    {
        final T old = level.getGameRules().get(rule);
        restore.push(() -> level.getGameRules().set(rule, old, level.getServer()));
        level.getGameRules().set(rule, value, level.getServer());
    }

    /**
     * Changes a MineColonies config value for this test, e.g. {@code setConfig(MineColonies.getConfig().getServer().creativeResolve, true)};
     * restored when the test ends.
     */
    public <T> void setConfig(final ModConfigSpec.ConfigValue<T> value, final T newValue)
    {
        final T old = value.get();
        restore.push(() -> E2EBootstrap.set(value, old));
        E2EBootstrap.set(value, newValue);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Research, raids
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Starts a research (no cost, no requirement check) and sets its progress. Progress counts research ticks; see
     * {@code IGlobalResearchTree#getBranchData(branch).getBaseTime(depth)} for the total.
     */
    public ILocalResearch startResearch(final Identifier researchId, final int progress)
    {
        final IGlobalResearch research = IGlobalResearchTree.getInstance().getResearch(researchId);
        if (research == null)
        {
            throw new IllegalArgumentException("Unknown research " + researchId);
        }
        final ILocalResearchTree tree = colony().getResearchManager().getResearchTree();
        if (tree.getResearch(research.getBranch(), research.getId()) == null)
        {
            research.startResearch(tree);
        }
        final ILocalResearch local = tree.getResearch(research.getBranch(), research.getId());
        local.setProgress(progress);
        colony().getResearchManager().markDirty();
        return local;
    }

    /**
     * Finishes a research at once and applies its effects, the same way the colony does for instant research.
     */
    public void completeResearch(final Identifier researchId)
    {
        final IGlobalResearch research = IGlobalResearchTree.getInstance().getResearch(researchId);
        if (research == null)
        {
            throw new IllegalArgumentException("Unknown research " + researchId);
        }
        final IColony colony = colony();
        final ILocalResearchTree tree = colony.getResearchManager().getResearchTree();
        ILocalResearch local = tree.getResearch(research.getBranch(), research.getId());
        if (local == null)
        {
            tree.addResearch(research.getBranch(), new LocalResearch(research.getId(), research.getBranch(), research.getDepth()));
            local = tree.getResearch(research.getBranch(), research.getId());
        }
        if (local.getState() == ResearchState.FINISHED)
        {
            return;
        }
        local.setProgress(IGlobalResearchTree.getInstance().getBranchData(research.getBranch()).getBaseTime(research.getDepth()));
        local.setState(ResearchState.FINISHED);
        tree.finishResearch(research.getId());
        for (final IResearchEffect effect : research.getEffects())
        {
            colony.getResearchManager().getResearchEffects().applyEffect(effect);
        }
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            citizen.applyResearchEffects();
        }
        colony.getResearchManager().markDirty();
    }

    /**
     * Starts a raid now through the raid manager (like {@code /mc colony raid now}). Ships are not allowed.
     *
     * @param culture raid event type path: "barbarian_raid", "amazon_raid", "egyptian_raid", "norsemen_raid", "pirate_raid" (ground pirates
     *                without ships), "drowned_pirate_raid"; null = random by biome, like a natural raid. Any other value returns
     *                NO_SPAWN_POINT.
     * @param amount  number of raiders, or null for the colony's normal amount.
     * @param spawnAt relative spawn position, or null to let the raid manager choose.
     */
    public IRaiderManager.RaidSpawnResult startRaid(@Nullable final String culture, @Nullable final Integer amount, @Nullable final BlockPos spawnAt)
    {
        return colony().getRaiderManager()
          .raiderEvent(new IRaiderManager.RaidSettings(true, culture, false, amount, spawnAt == null ? null : abs(spawnAt)));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Deletes colonies that should not exist at the start of a test (a crashed or misbehaving earlier test). Logs a warning, since this
     * means a cleanup did not happen.
     */
    private void deleteLeftoverColonies()
    {
        for (final IColony leftover : new ArrayList<>(IColonyManager.getInstance().getColonies(level)))
        {
            LOGGER.warn("Test {}: deleting leftover colony {} at {}", info.id(), leftover.getID(), leftover.getCenter());
            IColonyManager.getInstance().deleteColonyByWorld(leftover.getID(), false, level);
        }
    }

    /**
     * Runs when the test ends: deletes the colony (citizens, buildings, claims), removes leftover entities in the arena, restores config and
     * game rules changed by the test, clear weather.
     */
    private void cleanup()
    {
        if (cleanedUp)
        {
            return;
        }
        cleanedUp = true;
        while (!endActions.isEmpty())
        {
            try
            {
                endActions.pop().run();
            }
            catch (final Exception e)
            {
                LOGGER.error("Test {}: an end-of-test action failed", info.id(), e);
            }
        }
        try
        {
            if (colony != null)
            {
                IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, level);
            }
            for (final IColony extra : extraColonies)
            {
                if (IColonyManager.getInstance().getColonyByWorld(extra.getID(), level) != null)
                {
                    IColonyManager.getInstance().deleteColonyByWorld(extra.getID(), false, level);
                }
            }
            if (deepGround)
            {
                fillBelowArena(Blocks.AIR.defaultBlockState());
            }
            for (final ServerPlayer player : players)
            {
                resetPlayer(player, GameType.CREATIVE);
            }
            final AABB bounds = info.getStructureBounds().inflate(16);
            for (final Entity entity : level.getEntitiesOfClass(Entity.class, bounds, e -> !(e instanceof Player)))
            {
                entity.discard();
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Test {}: colony cleanup failed", info.id(), e);
        }
        while (!restore.isEmpty())
        {
            try
            {
                restore.pop().run();
            }
            catch (final Exception e)
            {
                LOGGER.error("Test {}: restoring a setting failed", info.id(), e);
            }
        }
        setWeather(false, false);
    }
}
