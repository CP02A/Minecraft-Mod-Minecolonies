package com.minecolonies.gametest.suites;

import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.connections.ColonyConnection;
import com.minecolonies.api.colony.connections.ConnectionEvent;
import com.minecolonies.api.colony.connections.ConnectionEventType;
import com.minecolonies.api.colony.connections.DiplomacyStatus;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.permissions.PermissionEvent;
import com.minecolonies.api.colony.permissions.Rank;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.colony.savedata.IServerColonySaveData;
import com.minecolonies.api.colony.workorders.IServerWorkOrder;
import com.minecolonies.api.colony.workorders.WorkOrderType;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.items.component.ColonyId;
import com.minecolonies.api.research.ILocalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.util.ColonyUtils;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.Pathfinding;
import com.minecolonies.core.network.messages.server.colony.TeleportToColonyMessage;
import com.minecolonies.core.network.messages.server.colony.TriggerConnectionEventMessage;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.minecolonies.gametest.framework.PathfindingSync;
import com.minecolonies.gametest.framework.TestFlow;
import com.minecolonies.gametest.mixin.GameTestHelperAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * End-to-end tests from docs/e2e-tests/colony.md, docs/e2e-tests/connections.md and docs/e2e-tests/robustness.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:colony_connections_robustness_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=colony_connections_robustness_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 */
public class ColonyConnectionsRobustnessTests
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyConnectionsRobustnessTests.class);

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Colony lifecycle (colony.md)
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * COL-01 Found a colony: placing a town hall founds a working colony owned by the right player.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 100, skyAccess = true)
    public void col01_foundColony(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BlockPos townHall = fixture.abs(ColonyFixture.ARENA_CENTER);
        final int coloniesBefore = IColonyManager.getInstance().getColonies(helper.getLevel()).size();

        fixture.flow()
          .run("place town hall and found colony", fixture::createColony)
          .awaitUntil("colony registered at the town hall",
            () -> IColonyManager.getInstance().getColonyByPosFromWorld(helper.getLevel(), townHall) != null,
            () -> "colonies: " + IColonyManager.getInstance().getColonies(helper.getLevel()).size())
          .check("exactly one new colony, at the town hall, in the overworld",
            () -> IColonyManager.getInstance().getColonies(helper.getLevel()).size() == coloniesBefore + 1
                    && fixture.colony().getCenter().equals(townHall)
                    && fixture.colony().getDimension() == Level.OVERWORLD,
            () -> "colonies " + IColonyManager.getInstance().getColonies(helper.getLevel()).size() + " (before " + coloniesBefore + "), center "
                    + fixture.colony().getCenter() + ", dimension " + fixture.colony().getDimension().identifier())
          .check("owner is the fake player, with the owner rank",
            () -> ColonyFixture.OWNER_ID.equals(fixture.colony().getPermissions().getOwner())
                    && fixture.colony().getPermissions().getRank(ColonyFixture.OWNER_ID) == fixture.colony().getPermissions().getRankOwner(),
            () -> "owner " + fixture.colony().getPermissions().getOwner() + " (" + fixture.colony().getPermissions().getOwnerName() + "), rank "
                    + fixture.colony().getPermissions().getRank(ColonyFixture.OWNER_ID).getName())
          .check("town hall registered at level 1",
            () -> {
                final IBuilding hall = fixture.colony().getServerBuildingManager().getTownHall();
                return hall != null && hall.getPosition().equals(townHall) && hall.getBuildingLevel() == 1;
            },
            () -> {
                final IBuilding hall = fixture.colony().getServerBuildingManager().getTownHall();
                return hall == null ? "no town hall" : "town hall at " + hall.getPosition() + ", level " + hall.getBuildingLevel();
            })
          .check("chunks within initialColonySize of the town hall are claimed by the colony",
            () -> unclaimedChunks(fixture.colony(), helper).isEmpty(),
            () -> "not claimed by colony " + fixture.colony().getID() + ": " + unclaimedChunks(fixture.colony(), helper))
          .succeed();
    }

    /**
     * COL-02 First citizens arrive: a new colony gets its starting citizens (one at a time) and they become real entities.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void col02_firstCitizensArrive(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final int expected = MineColonies.getConfig().getServer().initialCitizenAmount.get();

        fixture.flow()
          .run("found colony (move in stays on)", () -> {
              fixture.createColony();
              if (!fixture.colony().getSettings().getSetting(BuildingTownHall.MOVE_IN).getValue())
              {
                  throw new IllegalStateException("A new colony should have 'move in' on");
              }
          })
          .awaitUntil("the colony has initialCitizenAmount (" + expected + ") citizens", 8000,
            () -> fixture.colony().getCitizenManager().getCitizens().size() >= expected,
            () -> fixture.colony().getCitizenManager().getCitizens().size() + " citizens")
          .awaitUntil("every citizen has a living entity", 400,
            () -> fixture.colony().getCitizenManager().getCitizens().stream().allMatch(c -> c.getEntity().map(Entity::isAlive).orElse(false)),
            () -> describeAll(fixture.colony()))
          .check("exactly initialCitizenAmount citizens, no more", () -> fixture.colony().getCitizenManager().getCitizens().size() == expected,
            () -> fixture.colony().getCitizenManager().getCitizens().size() + " citizens")
          .check("every citizen entity stands inside the colony border",
            () -> fixture.colony().getCitizenManager().getCitizens().stream()
                    .allMatch(c -> c.getEntity().map(e -> fixture.colony().isCoordInColony(helper.getLevel(), e.blockPosition())).orElse(false)),
            () -> describeAll(fixture.colony()))
          .check("every citizen has a name, skills in 1..99 and no job",
            () -> fixture.colony().getCitizenManager().getCitizens().stream().allMatch(c -> c.getName() != null && !c.getName().isBlank()
                    && c.getJob() == null && skillsInRange(c)),
            () -> fixture.colony().getCitizenManager().getCitizens().stream()
                    .map(c -> c.getName() + " female=" + c.isFemale() + " job=" + c.getJob() + " skills=" + skills(c))
                    .collect(Collectors.joining("; ")))
          .check("not all starting citizens have the same gender (the colony balances genders)",
            () -> fixture.colony().getCitizenManager().getCitizens().stream().map(ICitizenData::isFemale).distinct().count() == 2,
            () -> fixture.colony().getCitizenManager().getCitizens().stream().map(c -> c.getName() + " female=" + c.isFemale())
                    .collect(Collectors.joining("; ")))
          .succeed();
    }

    /**
     * COL-03 Housing limits the population: with move-in on, the colony grows (children) only while there are free beds.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void col03_housingLimitsPopulation(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] homes = new IBuilding[2];

        fixture.flow()
          .run("colony with one level 2 residence (2 beds) and a couple living in it", () -> {
              // The initial top-up stops at 2 citizens, so all further growth comes from the housing-limited reproduction.
              fixture.setConfig(MineColonies.getConfig().getServer().initialCitizenAmount, 2);
              fixture.createColony();
              fixture.setMoveIn(false);
              homes[0] = fixture.placeBuilding(ModBuildings.home.get(), 2, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              final ICitizenData a = fixture.spawnCitizen(10, null, homes[0]);
              final ICitizenData b = fixture.spawnCitizen(10, null, homes[0]);
              a.setGenderAndGenerateName(false);
              b.setGenderAndGenerateName(true);
              fixture.setMoveIn(true);
              // Skip ahead: a new colony waits 5-15 minutes before its first child attempt (ReproductionManager constructor); after that
              // it tries every colony tick (500 ticks) while there are fewer citizens than max(4, beds).
              setField(fixture.colony().getReproductionManager(), "childCreationTimer", 0);
          })
          .awaitUntil("colony counts 2 beds", 1000, () -> fixture.colony().getCitizenManager().getMaxCitizens() == 2,
            () -> "max citizens " + fixture.colony().getCitizenManager().getMaxCitizens())
          .idle(2000)
          .check("no new citizen while housing is full", () -> fixture.colony().getCitizenManager().getCitizens().size() == 2,
            () -> describeAll(fixture.colony()))
          .run("add a second level 2 residence", () ->
            homes[1] = fixture.placeBuilding(ModBuildings.home.get(), 2, new BlockPos(52, ColonyFixture.GROUND_Y, 12)))
          .awaitUntil("colony counts 4 beds", 1000, () -> fixture.colony().getCitizenManager().getMaxCitizens() == 4,
            () -> "max citizens " + fixture.colony().getCitizenManager().getMaxCitizens())
          .awaitUntil("citizens move in (are born) until the new beds are full", 5000,
            () -> fixture.colony().getCitizenManager().getCitizens().size() >= 4,
            () -> describeAll(fixture.colony()))
          .idle(2000)
          .check("growth stopped at the housing limit", () -> fixture.colony().getCitizenManager().getCitizens().size() == 4,
            () -> describeAll(fixture.colony()))
          .check("every citizen has a bed (home)", () -> fixture.colony().getCitizenManager().getCitizens().stream().allMatch(c -> c.getHomeBuilding() != null),
            () -> fixture.colony().getCitizenManager().getCitizens().stream().map(c -> c.getName() + " home=" + c.getHomeBuilding()).collect(Collectors.joining("; ")))
          .check("population never above maxCitizenPerColony",
            () -> fixture.colony().getCitizenManager().getCitizens().size() <= MineColonies.getConfig().getServer().maxCitizenPerColony.get(),
            () -> fixture.colony().getCitizenManager().getCitizens().size() + " citizens")
          .succeed();
    }

    /**
     * COL-04 Territory grows with buildings: a guard tower claims chunks around it, and they are released when it is removed.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true)
    public void col04_territoryGrowsWithBuildings(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Set<ChunkPos> before = new HashSet<>();
        final Set<ChunkPos> added = new HashSet<>();
        final IBuilding[] tower = new IBuilding[1];

        fixture.flow()
          .run("found a colony with initialColonySize 1 and record its claim", () -> {
              // Small static claim, so a guard tower inside the arena reaches chunks the colony did not own yet.
              fixture.setConfig(MineColonies.getConfig().getServer().initialColonySize, 1);
              fixture.createColony();
              fixture.setMoveIn(false);
              before.addAll(ownedChunks(fixture.colony(), helper.getLevel(), 8));
          })
          .run("place a level 1 guard tower inside the claim, near its border", () ->
            tower[0] = fixture.placeBuilding(ModBuildings.guardTower.get(), 1, new BlockPos(46, ColonyFixture.GROUND_Y, 46)))
          .awaitUntil("chunks around the tower that were not claimed before belong to the colony", 100,
            () -> {
                added.clear();
                added.addAll(ownedChunks(fixture.colony(), helper.getLevel(), 8));
                added.removeAll(before);
                return !added.isEmpty();
            },
            () -> "claimed before " + before.size() + ", now " + ownedChunks(fixture.colony(), helper.getLevel(), 8).size())
          .check("the new chunks lie around the tower (claim radius " + 2 + ")",
            () -> {
                final ChunkPos towerChunk = ChunkPos.containing(tower[0].getPosition());
                return added.stream().allMatch(c -> Math.abs(c.x() - towerChunk.x()) <= 2 && Math.abs(c.z() - towerChunk.z()) <= 2);
            },
            () -> "tower chunk " + ChunkPos.containing(tower[0].getPosition()) + ", added " + added)
          .run("the owner breaks the guard tower's hut block", () -> fixture.owner().gameMode.destroyBlock(tower[0].getPosition()))
          .awaitUntil("the guard tower is no longer a building of the colony", 100,
            () -> fixture.colony().getServerBuildingManager().getBuilding(tower[0].getPosition()) == null, () -> "still registered")
          .awaitUntil("the extra chunks are released", 100,
            () -> added.stream().noneMatch(c -> ownerOf(helper.getLevel(), c) == fixture.colony().getID()),
            () -> "still claimed: " + added.stream().filter(c -> ownerOf(helper.getLevel(), c) == fixture.colony().getID()).toList())
          .check("the chunks claimed by the town hall stay claimed",
            () -> before.stream().allMatch(c -> ownerOf(helper.getLevel(), c) == fixture.colony().getID()),
            () -> "lost: " + before.stream().filter(c -> ownerOf(helper.getLevel(), c) != fixture.colony().getID()).toList())
          .succeed();
    }

    /**
     * COL-05 Colony protection and permissions: outsiders can't grief, members can do what their rank allows; with protection off the
     * outsider is no longer blocked from blocks and containers.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 200, skyAccess = true)
    public void col05_protectionAndPermissions(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final Map<String, ServerPlayer> players = new LinkedHashMap<>();
        // Results per player: "break", "place", "chest", "hut" -> allowed?
        final Map<String, Map<String, Boolean>> results = new LinkedHashMap<>();
        final BlockPos chest = new BlockPos(44, ColonyFixture.GROUND_Y, 52);

        fixture.flow()
          .run("colony with a hut, a chest and four players", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              helper.setBlock(chest, Blocks.CHEST.defaultBlockState());
              final IColony colony = fixture.colony();
              players.put("owner", fixture.owner());
              players.put("officer", fakePlayer(helper.getLevel(), 2, "E2EOfficer", GameType.SURVIVAL));
              players.put("friend", fakePlayer(helper.getLevel(), 3, "E2EFriend", GameType.SURVIVAL));
              players.put("outsider", fakePlayer(helper.getLevel(), 4, "E2EOutsider", GameType.SURVIVAL));
              colony.getPermissions().addPlayer(players.get("officer").getUUID(), "E2EOfficer", colony.getPermissions().getRankOfficer());
              colony.getPermissions().addPlayer(players.get("friend").getUUID(), "E2EFriend", colony.getPermissions().getRankFriend());
              for (final ServerPlayer player : players.values())
              {
                  final BlockPos stand = fixture.abs(new BlockPos(40, ColonyFixture.GROUND_Y, 40));
                  if (player != fixture.owner())
                  {
                      player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
                  }
              }
          })
          .run("every player tries to break, place, open the chest and open the hut", () -> {
              int column = 0;
              for (final Map.Entry<String, ServerPlayer> entry : players.entrySet())
              {
                  results.put(entry.getKey(), tryEverything(fixture, entry.getValue(), column++, chest, hut[0]));
              }
          })
          .check("owner and officer can do everything",
            () -> results.get("owner").values().stream().allMatch(b -> b) && results.get("officer").values().stream().allMatch(b -> b),
            () -> results.toString())
          .check("the friend may not break, place or open the chest (opening huts: see COL-05b)",
            () -> !results.get("friend").get("break") && !results.get("friend").get("place") && !results.get("friend").get("chest"),
            () -> results + ", denied " + describeEvents(fixture.colony()))
          .check("the outsider is blocked from everything and the blocks are unchanged",
            () -> results.get("outsider").values().stream().noneMatch(b -> b),
            () -> results + ", denied " + describeEvents(fixture.colony()))
          .run("turn colony protection off; the outsider tries again", () -> {
              fixture.setConfig(MineColonies.getConfig().getServer().enableColonyProtection, false);
              results.put("outsider (protection off)", tryEverything(fixture, players.get("outsider"), 5, chest, hut[0]));
          })
          .check("without protection the outsider can break, place and open the chest",
            () -> {
                final Map<String, Boolean> r = results.get("outsider (protection off)");
                return r.get("break") && r.get("place") && r.get("chest");
            },
            () -> "outsider (protection off) " + results.get("outsider (protection off)"))
          .check("hut menus stay rank protected even with protection off (ACCESS_HUTS is checked regardless of the config)",
            () -> !results.get("outsider (protection off)").get("hut"),
            () -> "outsider (protection off) " + results.get("outsider (protection off)"))
          .succeed();
    }

    /**
     * COL-05b A friend opens a hut: the friend rank has ACCESS_HUTS by default, so right-clicking a hut must open its menu.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 100, skyAccess = true)
    public void col05b_friendOpensHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ServerPlayer[] friend = new ServerPlayer[1];
        final InteractionResult[] result = new InteractionResult[1];

        fixture.flow()
          .run("colony with a hut and a friend", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.builder.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              friend[0] = fakePlayer(helper.getLevel(), 3, "E2EFriend", GameType.SURVIVAL);
              fixture.colony().getPermissions().addPlayer(friend[0].getUUID(), "E2EFriend", fixture.colony().getPermissions().getRankFriend());
              fixture.colony().getServerBuildingManager().getTownHall().removePermissionEvents(friend[0].getUUID());
          })
          .check("the friend rank has ACCESS_HUTS", () -> fixture.colony().getPermissions().hasPermission(friend[0], Action.ACCESS_HUTS), () -> "no")
          .run("the friend right-clicks the hut", () -> result[0] = use(friend[0], ItemStack.EMPTY, hut[0].getPosition(), false))
          .check("the hut menu opens (the click is not refused)", () -> !(result[0] instanceof InteractionResult.Fail),
            () -> result[0] + ", denied " + describeEvents(fixture.colony()))
          .succeed();
    }

    /**
     * COL-06 Save and reload a colony: the colony is written to NBT the way the world save does, removed from the colony manager and
     * loaded back from that NBT the way a server start does; all data comes back and work continues.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void col06_saveAndReload(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[3]; // builder, lumberjack, residence
        final ICitizenData[] workers = new ICitizenData[2];
        final IColony[] reloaded = new IColony[1];
        final Map<String, String> before = new LinkedHashMap<>();
        final Identifier research = Identifier.parse("minecolonies:civilian/agile");

        fixture.flow()
          .run("busy colony: builder, lumberjack, residence, upgrade order, research in progress", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.builder.get(), 2, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              huts[1] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 12));
              huts[2] = fixture.placeBuilding(ModBuildings.home.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              workers[0] = fixture.spawnCitizen(20, huts[0], huts[2]);
              workers[1] = fixture.spawnCitizen(20, huts[1], null);
              fixture.setCitizenNeeds(workers[0], 15.0, 20f);
              if (fixture.addWorkOrder(WorkOrderType.UPGRADE, huts[2]) == null)
              {
                  throw new IllegalStateException("upgrade order for the residence was refused");
              }
              fixture.startResearch(research, 3);
          })
          .awaitUntil("the builder has open requests for the upgrade", 3000, () -> !openRequestTokens(fixture.colony()).isEmpty(),
            () -> ColonyFixture.describe(workers[0]))
          .run("snapshot, save, unload and load the colony", () -> {
              before.putAll(snapshot(fixture.colony(), research));
              reloaded[0] = saveAndReload(fixture.colony(), helper.getLevel());
          })
          .check("the reloaded colony replaced the old one in the colony manager",
            () -> IColonyManager.getInstance().getColonyByWorld(reloaded[0].getID(), helper.getLevel()) == reloaded[0],
            () -> "found " + IColonyManager.getInstance().getColonyByWorld(reloaded[0].getID(), helper.getLevel()))
          .check("buildings, citizens, work orders, requests and research are the same as before",
            () -> before.equals(snapshot(reloaded[0], research)),
            () -> diff(before, snapshot(reloaded[0], research)))
          .awaitUntil("all citizens have entities again", 2000,
            () -> reloaded[0].getCitizenManager().getCitizens().stream().allMatch(c -> c.getEntity().map(Entity::isAlive).orElse(false)),
            () -> describeAll(reloaded[0]))
          .awaitUntil("the lumberjack works again (worker AI past IDLE/INIT)", 2000,
            () -> {
                final ICitizenData lumberjack = reloaded[0].getCitizenManager().getCivilian(workers[1].getId());
                if (lumberjack == null || lumberjack.getJob() == null || lumberjack.getJob().getWorkerAI() == null)
                {
                    return false;
                }
                final Object state = lumberjack.getJob().getWorkerAI().getState();
                return state != AIWorkerState.IDLE && state != AIWorkerState.INIT;
            },
            () -> ColonyFixture.describe(reloaded[0].getCitizenManager().getCivilian(workers[1].getId())))
          .succeed();
    }

    /**
     * COL-07 Delete a colony: the colony, its citizens and its claims are gone, and a new colony can be founded at the same place.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 400, skyAccess = true)
    public void col07_deleteColony(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IColony[] old = new IColony[1];
        final List<ICitizenData> citizens = new ArrayList<>();
        final Set<ChunkPos> claimed = new HashSet<>();

        fixture.flow()
          .run("colony with huts and citizens", () -> {
              old[0] = fixture.createColony();
              fixture.setMoveIn(false);
              final IBuilding home = fixture.placeBuilding(ModBuildings.home.get(), 2, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              final IBuilding lumberjack = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              citizens.add(fixture.spawnCitizen(10, lumberjack, home));
              citizens.add(fixture.spawnCitizen(10, null, home));
          })
          .awaitUntil("citizens have entities", 200, () -> citizens.stream().allMatch(c -> c.getEntity().isPresent()), () -> describeAll(old[0]))
          .run("delete the colony like the delete command (buildings kept)", () -> {
              claimed.addAll(ownedChunks(old[0], helper.getLevel(), 8));
              IColonyManager.getInstance().deleteColonyByDimension(old[0].getID(), false, helper.getLevel().dimension());
          })
          .check("the colony manager no longer has it",
            () -> IColonyManager.getInstance().getColonyByWorld(old[0].getID(), helper.getLevel()) == null
                    && IColonyManager.getInstance().getColonies(helper.getLevel()).isEmpty(),
            () -> "colonies " + IColonyManager.getInstance().getColonies(helper.getLevel()).stream().map(IColony::getID).toList())
          .awaitUntil("all its citizen entities are gone", 100,
            () -> citizenEntities(helper, null).isEmpty(),
            () -> citizenEntities(helper, null).stream().map(e -> e.getName().getString() + " alive=" + e.isAlive()).toList().toString())
          .check("its chunks are no longer claimed",
            () -> claimed.stream().noneMatch(c -> ownerOf(helper.getLevel(), c) == old[0].getID()),
            () -> "still claimed: " + claimed.stream().filter(c -> ownerOf(helper.getLevel(), c) == old[0].getID()).toList())
          .run("found a new colony at the same place", () -> {
              final IColony created = fixture.createColony();
              if (created.getID() == old[0].getID() && created == old[0])
              {
                  throw new IllegalStateException("got the deleted colony object back");
              }
          })
          .check("the new colony is registered and owns the town hall chunk",
            () -> ownerOf(helper.getLevel(), ChunkPos.containing(fixture.colony().getCenter())) == fixture.colony().getID()
                    && fixture.colony().getServerBuildingManager().getTownHall() != null,
            () -> "new colony " + fixture.colony().getID() + ", chunk owner " + ownerOf(helper.getLevel(), ChunkPos.containing(fixture.colony().getCenter())))
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Colony connections (connections.md)
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Two colonies side by side in the arena (A west, B east), each with a gate house and one connection sign in front of it.
     */
    private static final class Twin
    {
        IColony     a;
        IColony     b;
        ServerPlayer ownerB;
        IBuilding   gateA;
        IBuilding   gateB;
        BlockPos    signA;
        BlockPos    signB;
        ItemStack   signItemA;
        ServerPlayer traveller;
    }

    private static final BlockPos TOWN_HALL_A = new BlockPos(16, ColonyFixture.GROUND_Y, 44);
    private static final BlockPos TOWN_HALL_B = new BlockPos(55, ColonyFixture.GROUND_Y, 44);
    private static final BlockPos GATE_A      = new BlockPos(24, ColonyFixture.GROUND_Y, 30);
    private static final BlockPos GATE_B      = new BlockPos(52, ColonyFixture.GROUND_Y, 30);
    private static final BlockPos SIGN_A      = new BlockPos(29, ColonyFixture.GROUND_Y, 31);
    private static final BlockPos SIGN_B      = new BlockPos(58, ColonyFixture.GROUND_Y, 31);

    /**
     * Adds the steps that build two connected colonies (CON-01 setup) to the flow.
     */
    private static TestFlow connectedColonies(final ColonyFixture fixture, final TestFlow flow, final Twin t)
    {
        final GameTestHelper helper = fixture.helper();
        return flow
          .run("two colonies with a gate house each", () -> {
              // Initial claim radius 1: the colonies' static claims don't overlap in the 64 block arena (B's center is >= 2 chunks from A's).
              fixture.setConfig(MineColonies.getConfig().getServer().initialColonySize, 1);
              t.a = fixture.createColony(TOWN_HALL_A);
              fixture.setMoveIn(false);
              t.ownerB = fakePlayer(helper.getLevel(), 11, "E2EOwnerB", GameType.CREATIVE);
              t.b = fixture.createSecondColony(TOWN_HALL_B, t.ownerB);
              t.gateA = fixture.placeBuilding(ModBuildings.gateHouse.get(), 1, GATE_A);
              fixture.pasteHut(ModBuildings.GATE_HOUSE_ID, 1, fixture.abs(GATE_B), RotationMirror.NONE);
              t.gateB = IColonyManager.getInstance().getBuilding(helper.getLevel(), fixture.abs(GATE_B));
              if (t.gateB == null || t.gateB.getColony() != t.b || t.gateB.getBuildingLevel() != 1)
              {
                  throw new IllegalStateException("gate house B not registered in colony B: " + t.gateB);
              }
              t.signA = fixture.abs(SIGN_A);
              t.signB = fixture.abs(SIGN_B);
          })
          .run("each owner links a sign item to his gate house and places a sign in front of it", () -> {
              t.signItemA = new ItemStack(ModBlocks.blockColonySign, 16);
              final ItemStack signItemB = new ItemStack(ModBlocks.blockColonySign, 16);
              use(fixture.owner(), t.signItemA, t.gateA.getPosition(), true);
              use(t.ownerB, signItemB, t.gateB.getPosition(), true);
              if (ColonyId.readFromItemStack(t.signItemA).id() != t.a.getID() || ColonyId.readFromItemStack(signItemB).id() != t.b.getID())
              {
                  throw new IllegalStateException("sign items not linked: A " + ColonyId.readFromItemStack(t.signItemA) + ", B " + ColonyId.readFromItemStack(signItemB));
              }
              use(fixture.owner(), t.signItemA, t.signA.below(), false);
              use(t.ownerB, signItemB, t.signB.below(), false);
              if (!helper.getLevel().getBlockState(t.signA).is(ModBlocks.blockColonySign) || !helper.getLevel().getBlockState(t.signB).is(ModBlocks.blockColonySign))
              {
                  throw new IllegalStateException("signs not placed: A " + helper.getLevel().getBlockState(t.signA) + ", B " + helper.getLevel().getBlockState(t.signB));
              }
          })
          .awaitUntil("both signs become connection nodes linked to their gate house", 600,
            () -> t.a.getConnectionManager().getNode(t.signA) != null && t.b.getConnectionManager().getNode(t.signB) != null,
            () -> "A node " + t.a.getConnectionManager().getNode(t.signA) + " (sign there: " + helper.getLevel().getBlockState(t.signA).is(ModBlocks.blockColonySign)
                    + "), B node " + t.b.getConnectionManager().getNode(t.signB) + " (sign there: " + helper.getLevel().getBlockState(t.signB).is(ModBlocks.blockColonySign) + ")")
          .run("owner A sneak-uses his sign item on colony B's sign to connect the colonies", () -> use(fixture.owner(), t.signItemA, t.signB, true))
          .awaitUntil("each colony lists the other as directly connected", 600,
            () -> t.a.getConnectionManager().getDirectlyConnectedColonies().containsKey(t.b.getID())
                    && t.b.getConnectionManager().getDirectlyConnectedColonies().containsKey(t.a.getID()),
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b));
    }

    /**
     * CON-01 Connecting two colonies: gate houses plus signs connect two colonies; removing a sign breaks the sign chain.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con01_connectTwoColonies(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();

        connectedColonies(fixture, fixture.flow(), t)
          .check("each connection points at the other colony's gate house",
            () -> t.a.getConnectionManager().getDirectlyConnectedColonies().get(t.b.getID()).pos.equals(t.gateB.getPosition())
                    && t.b.getConnectionManager().getDirectlyConnectedColonies().get(t.a.getID()).pos.equals(t.gateA.getPosition()),
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b) + ", gates A " + t.gateA.getPosition() + " B " + t.gateB.getPosition())
          .check("the sign nodes are resolved and chained gate A - sign A - sign B - gate B",
            () -> t.a.getConnectionManager().getNode(t.signA).getPreviousNode().equals(t.gateA.getPosition())
                    && t.a.getConnectionManager().getNode(t.signA).getNextNode().equals(t.signB)
                    && t.b.getConnectionManager().getNode(t.signB).getPreviousNode().equals(t.gateB.getPosition())
                    && t.b.getConnectionManager().getNode(t.signB).getNextNode().equals(t.signA),
            () -> "A sign " + node(t.a, t.signA) + ", B sign " + node(t.b, t.signB))
          .check("both colonies start neutral",
            () -> t.a.getConnectionManager().getColonyDiplomacyStatus(t.b.getID()) == DiplomacyStatus.NEUTRAL
                    && t.b.getConnectionManager().getColonyDiplomacyStatus(t.a.getID()) == DiplomacyStatus.NEUTRAL,
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b))
          .run("owner A breaks his sign in the middle of the path", () -> fixture.owner().gameMode.destroyBlock(t.signA))
          .check("the removed sign is no longer a node of colony A",
            () -> t.a.getConnectionManager().getNode(t.signA) == null && !helper.getLevel().getBlockState(t.signA).is(ModBlocks.blockColonySign),
            () -> "A sign " + node(t.a, t.signA))
          .check("the colonies stay listed as connected (the relation does not depend on the sign chain)",
            () -> t.a.getConnectionManager().getDirectlyConnectedColonies().containsKey(t.b.getID())
                    && t.b.getConnectionManager().getDirectlyConnectedColonies().containsKey(t.a.getID()),
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b))
          .succeed();
    }

    /**
     * CON-01b Re-placing a removed sign: after colony A's sign in the middle is broken and placed again, the colonies can be linked again.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con01b_reconnectAfterSignRemoved(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final BlockPos[] newSign = new BlockPos[1];

        connectedColonies(fixture, fixture.flow(), t)
          .run("owner A breaks his sign and places a new one next to it", () -> {
              fixture.owner().gameMode.destroyBlock(t.signA);
              newSign[0] = t.signA.offset(0, 0, 2);
              use(fixture.owner(), t.signItemA, newSign[0].below(), false);
          })
          .awaitUntil("the new sign becomes a node of A", 600, () -> t.a.getConnectionManager().getNode(newSign[0]) != null,
            () -> "sign there: " + helper.getLevel().getBlockState(newSign[0]).is(ModBlocks.blockColonySign))
          .run("owner A links the new sign to B's sign again", () -> use(fixture.owner(), t.signItemA, t.signB, true))
          .awaitUntil("B's sign is linked to A's new sign", 400,
            () -> t.b.getConnectionManager().getNode(t.signB).getNextNode().equals(newSign[0]),
            () -> "B sign " + node(t.b, t.signB) + ", A new sign " + node(t.a, newSign[0]) + " (removed sign was " + t.signA.toShortString() + ")")
          .succeed();
    }

    /**
     * CON-02 Becoming allies: an ally request and its confirmation make both colonies allies; allies may teleport to the ally's gate.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con02_becomingAllies(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final Vec3[] start = new Vec3[1];

        connectedColonies(fixture, fixture.flow(), t)
          .run("colony A requests an alliance", () -> sendEvent(t.a, fixture.owner(), ConnectionEventType.ALLY_REQUEST, t.b))
          .check("B has a pending ALLY_REQUEST from A, still neutral",
            () -> hasEvent(t.b, t.a, ConnectionEventType.ALLY_REQUEST) && t.b.getConnectionManager().getColonyDiplomacyStatus(t.a.getID()) == DiplomacyStatus.NEUTRAL,
            () -> "B events " + t.b.getConnectionManager().getConnectionEvents() + ", B: " + connections(t.b))
          .run("colony B confirms", () -> sendEvent(t.b, t.ownerB, ConnectionEventType.ALLY_CONFIRMED, t.a))
          .check("both colonies are allies",
            () -> t.a.getConnectionManager().getColonyDiplomacyStatus(t.b.getID()) == DiplomacyStatus.ALLIES
                    && t.b.getConnectionManager().getColonyDiplomacyStatus(t.a.getID()) == DiplomacyStatus.ALLIES
                    && hasEvent(t.a, t.b, ConnectionEventType.ALLY_CONFIRMED),
            () -> "A: " + connections(t.a) + " events " + t.a.getConnectionManager().getConnectionEvents() + ", B: " + connections(t.b))
          .run("a friend of A at A's gate house asks to teleport to the ally gate", () -> {
              t.traveller = traveller(fixture, t.a, t.gateA.getPosition().offset(0, 0, -6));
              start[0] = t.traveller.position();
              teleportToAlly(t, t.traveller);
          })
          .check("the friend was teleported (ally teleport works server side)",
            () -> !t.traveller.position().equals(start[0]) && t.traveller.blockPosition().closerThan(t.gateB.getPosition(), 8),
            () -> "from " + start[0] + " to " + t.traveller.position() + ", gate B " + t.gateB.getPosition())
          .succeed();
    }

    /**
     * CON-02b Ally teleport range: the message is refused away from the player's own gate houses and works from the own gate house (gates
     * 28 blocks apart), the way the gate house window sends it (was bug E2E-CON-02-1).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con02b_allyTeleportFromOwnGate(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final Vec3[] start = new Vec3[1];

        connectedColonies(fixture, fixture.flow(), t)
          .run("A and B become allies", () -> {
              sendEvent(t.a, fixture.owner(), ConnectionEventType.ALLY_REQUEST, t.b);
              sendEvent(t.b, t.ownerB, ConnectionEventType.ALLY_CONFIRMED, t.a);
          })
          .check("allies", () -> t.a.getConnectionManager().getColonyDiplomacyStatus(t.b.getID()) == DiplomacyStatus.ALLIES, () -> connections(t.a))
          .run("a friend of A 20 blocks away from A's gate house (not near any gate house) sends the teleport message", () -> {
              t.traveller = traveller(fixture, t.a, t.gateA.getPosition().offset(-20, 0, 0));
              start[0] = t.traveller.position();
              teleportToAlly(t, t.traveller);
          })
          .check("refused away from the own gate house (range restriction of #11888)", () -> t.traveller.position().equals(start[0]),
            () -> "moved from " + start[0] + " to " + t.traveller.position())
          .run("the friend walks to A's gate house and uses the teleport button for B", () -> {
              final BlockPos atGate = t.gateA.getPosition().offset(0, 0, -6);
              t.traveller.teleportTo(atGate.getX() + 0.25, atGate.getY(), atGate.getZ() + 0.25);
              start[0] = t.traveller.position();
              teleportToAlly(t, t.traveller);
          })
          .check("the friend arrived at B's gate house",
            () -> t.traveller.blockPosition().closerThan(t.gateB.getPosition(), 8),
            () -> "from " + start[0] + " to " + t.traveller.position() + " (gate A " + t.gateA.getPosition() + ", gate B " + t.gateB.getPosition()
                    + ", distance between gates " + String.format("%.1f", Math.sqrt(t.gateA.getPosition().distSqr(t.gateB.getPosition()))) + ")")
          .succeed();
    }

    /**
     * CON-03 Feud: starting a feud makes both colonies hostile, and ally features are refused.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con03_feud(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final Vec3[] start = new Vec3[1];

        connectedColonies(fixture, fixture.flow(), t)
          .run("A and B become allies, then A starts a feud", () -> {
              sendEvent(t.a, fixture.owner(), ConnectionEventType.ALLY_REQUEST, t.b);
              sendEvent(t.b, t.ownerB, ConnectionEventType.ALLY_CONFIRMED, t.a);
              sendEvent(t.a, fixture.owner(), ConnectionEventType.FEUD_STARTED, t.b);
          })
          .check("both colonies record the feud (HOSTILE), B has the FEUD_STARTED event",
            () -> t.a.getConnectionManager().getColonyDiplomacyStatus(t.b.getID()) == DiplomacyStatus.HOSTILE
                    && t.b.getConnectionManager().getColonyDiplomacyStatus(t.a.getID()) == DiplomacyStatus.HOSTILE
                    && hasEvent(t.b, t.a, ConnectionEventType.FEUD_STARTED),
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b) + " events " + t.b.getConnectionManager().getConnectionEvents())
          .run("a friend of A at A's gate house asks to teleport to B", () -> {
              t.traveller = traveller(fixture, t.a, t.gateA.getPosition().offset(0, 0, -6));
              start[0] = t.traveller.position();
              teleportToAlly(t, t.traveller);
          })
          .check("the teleport is refused while feuding", () -> t.traveller.position().equals(start[0]),
            () -> "moved from " + start[0] + " to " + t.traveller.position())
          .succeed();
    }

    /**
     * CON-04 Neutral and gate house removal: an alliance can be reset to neutral; removing a gate house keeps the relation but drops the
     * teleport target. (The design's "disconnect" step does not exist in the mod, see connections.md.)
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void con04_neutralAndGateRemoval(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final Vec3[] start = new Vec3[1];

        connectedColonies(fixture, fixture.flow(), t)
          .run("A and B become allies, then A sets the relation to neutral", () -> {
              sendEvent(t.a, fixture.owner(), ConnectionEventType.ALLY_REQUEST, t.b);
              sendEvent(t.b, t.ownerB, ConnectionEventType.ALLY_CONFIRMED, t.a);
              sendEvent(t.a, fixture.owner(), ConnectionEventType.NEUTRAL_SET, t.b);
          })
          .check("both colonies show NEUTRAL, B's event from A is NEUTRAL_SET (the ally request is replaced)",
            () -> t.a.getConnectionManager().getColonyDiplomacyStatus(t.b.getID()) == DiplomacyStatus.NEUTRAL
                    && t.b.getConnectionManager().getColonyDiplomacyStatus(t.a.getID()) == DiplomacyStatus.NEUTRAL
                    && hasEvent(t.b, t.a, ConnectionEventType.NEUTRAL_SET) && !hasEvent(t.b, t.a, ConnectionEventType.ALLY_REQUEST),
            () -> "A: " + connections(t.a) + ", B: " + connections(t.b) + " events " + t.b.getConnectionManager().getConnectionEvents())
          .run("a friend of A at A's gate house asks to teleport to B", () -> {
              t.traveller = traveller(fixture, t.a, t.gateA.getPosition().offset(0, 0, -6));
              start[0] = t.traveller.position();
              teleportToAlly(t, t.traveller);
          })
          .check("no ally teleport when neutral", () -> t.traveller.position().equals(start[0]),
            () -> "moved from " + start[0] + " to " + t.traveller.position())
          .run("owner A breaks his gate house", () -> fixture.owner().gameMode.destroyBlock(t.gateA.getPosition()))
          .check("B still lists A, but without a gate to teleport to (position zero)",
            () -> t.b.getConnectionManager().getDirectlyConnectedColonies().containsKey(t.a.getID())
                    && t.b.getConnectionManager().getDirectlyConnectedColonies().get(t.a.getID()).pos.equals(BlockPos.ZERO),
            () -> "B: " + connections(t.b))
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Robustness (robustness.md)
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * ROB-05 Colonies don't affect each other: two colonies side by side, both lumberjacks need an axe, only colony A's warehouse has axes.
     * A's request is delivered from A's warehouse by A's courier; B's request goes to B's player and never takes from A's warehouse.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void rob05_coloniesDontAffectEachOther(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final Twin t = new Twin();
        final IBuilding[] a = new IBuilding[3]; // warehouse, lumberjack, courier hut
        final IBuilding[] b = new IBuilding[1]; // lumberjack
        final ICitizenData[] citizens = new ICitizenData[3]; // courier A, lumberjack A, lumberjack B

        fixture.flow()
          .run("two small colonies: A with a warehouse, a courier hut with a courier and a lumberjack; B with a lumberjack", () -> {
              fixture.setConfig(MineColonies.getConfig().getServer().initialColonySize, 1);
              t.a = fixture.createBareColony(new BlockPos(20, ColonyFixture.GROUND_Y, 44), fixture.owner(), "E2E colony A");
              t.ownerB = fakePlayer(helper.getLevel(), 11, "E2EOwnerB", GameType.CREATIVE);
              t.b = fixture.createBareColony(new BlockPos(62, ColonyFixture.GROUND_Y, 40), t.ownerB, "E2E colony B");
              a[0] = fixture.placeBuilding(t.a, ModBuildings.wareHouse.get(), 1, new BlockPos(9, ColonyFixture.GROUND_Y, 34));
              a[1] = fixture.placeBuilding(t.a, ModBuildings.lumberjack.get(), 1, new BlockPos(30, ColonyFixture.GROUND_Y, 50));
              b[0] = fixture.placeBuilding(t.b, ModBuildings.lumberjack.get(), 1, new BlockPos(58, ColonyFixture.GROUND_Y, 44));
              fixture.fillBuilding(a[0], new ItemStack(Items.WOODEN_AXE), new ItemStack(Items.WOODEN_AXE));
              a[2] = fixture.placeBuilding(t.a, ModBuildings.deliveryman.get(), 1, new BlockPos(27, ColonyFixture.GROUND_Y, 38));
              citizens[0] = fixture.spawnCitizen(t.a, 20, a[2], null); // couriers are hired at the courier hut; the warehouse picks them up
              citizens[1] = fixture.spawnCitizen(t.a, 20, a[1], null);
              citizens[2] = fixture.spawnCitizen(t.b, 20, b[0], null);
          })
          .awaitUntil("A's lumberjack gets an axe from A's warehouse", 5000,
            () -> countAxes(citizens[1]) + countAxes(fixture, a[1]) >= 1,
            () -> "A warehouse axes " + countAxes(fixture, a[0]) + ", A lumberjack " + ColonyFixture.describe(citizens[1]) + ", courier "
                    + ColonyFixture.describe(citizens[0]) + ", A requests " + openRequestTokens(t.a))
          .awaitUntil("B's lumberjack has an open axe request, held by B's retrying or player resolver (no warehouse in B)", 200,
            () -> b[0].getOpenRequests(citizens[2].getId()).stream()
                    .anyMatch(r -> t.b.getRequestManager().getResolverForRequest(r.getId()) == t.b.getRequestManager().getPlayerResolver()
                                     || t.b.getRequestManager().getResolverForRequest(r.getId()) == t.b.getRequestManager().getRetryingRequestResolver()),
            () -> "B requests " + b[0].getOpenRequests(citizens[2].getId()).stream()
                    .map(r -> r.getRequest() + " -> " + t.b.getRequestManager().getResolverForRequest(r.getId())).toList())
          .idle(600)
          .check("A's warehouse gave exactly one axe (to A), B's lumberjack has none",
            () -> countAxes(fixture, a[0]) == 1 && countAxes(citizens[2]) + countAxes(fixture, b[0]) == 0,
            () -> "A warehouse axes " + countAxes(fixture, a[0]) + ", B lumberjack axes " + (countAxes(citizens[2]) + countAxes(fixture, b[0])))
          .check("every citizen works for (and its entity belongs to) its own colony",
            () -> citizens[0].getColony() == t.a && citizens[1].getColony() == t.a && citizens[2].getColony() == t.b
                    && citizens[0].getWorkBuilding().getColony() == t.a && citizens[1].getWorkBuilding().getColony() == t.a
                    && citizens[2].getWorkBuilding().getColony() == t.b
                    && ((EntityCitizen) citizens[2].getEntity().get()).getCitizenColonyHandler().getColonyId() == t.b.getID()
                    && ((EntityCitizen) citizens[1].getEntity().get()).getCitizenColonyHandler().getColonyId() == t.a.getID(),
            () -> "A " + t.a.getID() + ", B " + t.b.getID() + ": " + describeAll(t.a) + " / " + describeAll(t.b))
          .succeed();
    }

    /**
     * ROB-06 Lost citizen entities come back: a worker's entity removed without dying is spawned again with the same data.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void rob06_lostCitizenEntityComesBack(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] worker = new ICitizenData[1];
        final Entity[] removed = new Entity[1];

        fixture.flow()
          .run("colony with a lumberjack living in a residence", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              huts[1] = fixture.placeBuilding(ModBuildings.home.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              worker[0] = fixture.spawnCitizen(20, huts[0], huts[1]);
          })
          .awaitUntil("the worker has an entity", 200, () -> worker[0].getEntity().isPresent(), () -> ColonyFixture.describe(worker[0]))
          .idle(100)
          .run("remove the entity without killing it", () -> {
              removed[0] = worker[0].getEntity().get();
              removed[0].remove(Entity.RemovalReason.DISCARDED);
          })
          .awaitUntil("the colony spawns the citizen again (never two entities)", 8500,
            () -> {
                final List<EntityCitizen> entities = citizenEntities(helper, worker[0]);
                if (entities.size() > 1)
                {
                    throw new IllegalStateException("two entities for one citizen: " + entities);
                }
                return entities.size() == 1 && entities.get(0) != removed[0] && entities.get(0).isAlive()
                         && worker[0].getEntity().map(e -> e == entities.get(0)).orElse(false);
            },
            () -> ColonyFixture.describe(worker[0]) + ", entities " + citizenEntities(helper, worker[0]).size())
          .check("same citizen data, job and home",
            () -> fixture.colony().getCitizenManager().getCivilian(worker[0].getId()) == worker[0]
                    && worker[0].getWorkBuilding() == huts[0] && worker[0].getHomeBuilding() == huts[1]
                    && ((EntityCitizen) worker[0].getEntity().get()).getCitizenData() == worker[0],
            () -> ColonyFixture.describe(worker[0]) + ", work " + worker[0].getWorkBuilding() + ", home " + worker[0].getHomeBuilding())
          .succeed();
    }

    /**
     * ROB-07 Pathfinding runs on one known thread: all MineColonies path jobs go through Pathfinding.getExecutor(), and every job
     * submitted in a tick is finished before the next tick.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2000, skyAccess = true)
    public void rob07_pathfindingOnOneThread(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ThreadPoolExecutor executor = Pathfinding.getExecutor();
        final long[] startTasks = new long[1];
        final long[] startTick = new long[1];
        final long[] lastTaskCount = {-1};
        final Thread[] pathThread = new Thread[1];
        final ICitizenData[] citizens = new ICitizenData[2];
        final String[] raid = new String[1];

        fixture.flow()
          .run("colony with a lumberjack, a guard and a small raid", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              final IBuilding lumberjack = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              final IBuilding tower = fixture.placeBuilding(ModBuildings.guardTower.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              citizens[0] = fixture.spawnCitizen(allSkills(20), lumberjack, null, new BlockPos(56, ColonyFixture.GROUND_Y, 8));
              citizens[1] = fixture.spawnCitizen(20, tower, null);
              raid[0] = String.valueOf(fixture.startRaid("barbarian_raid", 2, new BlockPos(6, ColonyFixture.GROUND_Y, 58)));
              try
              {
                  pathThread[0] = PathfindingSync.pathfindingThread();
              }
              catch (final Exception e)
              {
                  throw new IllegalStateException(e);
              }
              startTasks[0] = executor.getTaskCount();
              startTick[0] = helper.getLevel().getServer().getTickCount();
              LOGGER.info("ROB-07: raid start result {}", raid[0]);
          })
          .awaitUntil("600 ticks of colony life with every path job finished by the next tick, on the one pathfinding thread", 1500,
            () -> {
                final long completed = executor.getCompletedTaskCount();
                if (lastTaskCount[0] >= 0 && completed < lastTaskCount[0])
                {
                    throw new IllegalStateException("path jobs of the previous tick were not finished: completed " + completed + " < submitted " + lastTaskCount[0]);
                }
                lastTaskCount[0] = executor.getTaskCount();
                final List<Thread> foreign = Thread.getAllStackTraces().keySet().stream()
                                               .filter(th -> th.getName().startsWith("Minecolonies Pathfinding") && th != pathThread[0] && th.isAlive())
                                               .toList();
                if (!foreign.isEmpty())
                {
                    throw new IllegalStateException("pathfinding threads outside Pathfinding.getExecutor(): " + foreign + " (executor thread " + pathThread[0] + ")");
                }
                return helper.getLevel().getServer().getTickCount() - startTick[0] >= 600;
            },
            () -> "ticks " + (helper.getLevel().getServer().getTickCount() - startTick[0]))
          .check("path jobs really went through the executor (more submitted jobs than the one per-tick marker of the test server)",
            () -> realPathJobs(executor, startTasks[0], startTick[0], helper) >= 2,
            () -> "path jobs " + realPathJobs(executor, startTasks[0], startTick[0], helper) + " (raid " + raid[0] + ")")
          .check("only one Minecolonies pathfinding thread exists",
            () -> Thread.getAllStackTraces().keySet().stream().filter(th -> th.getName().startsWith("Minecolonies Pathfinding")).count() == 1,
            () -> Thread.getAllStackTraces().keySet().stream().filter(th -> th.getName().startsWith("Minecolonies Pathfinding")).map(Thread::getName).toList().toString())
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Framework check
    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Framework check for real pathing: a hired worker spawned in the far corner of the arena walks to his hut
     * (a lumberjack: unlike a builder without work orders, he never goes idle, so he heads to his hut at once). Validates that citizens get
     * paths from the background pathfinding thread on the test server (the end-of-tick pathfinding wait).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void workerWalksToHut(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];

        fixture.flow()
          .run("found colony, place lumberjack's hut, hire a lumberjack", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              worker[0] = fixture.spawnCitizen(20, hut[0], null);
          })
          .awaitUntil("lumberjack entity exists", 200, () -> worker[0].getEntity().isPresent(), () -> "no entity yet")
          .run("move the lumberjack to the far corner", () -> {
              final BlockPos corner = fixture.abs(new BlockPos(56, ColonyFixture.GROUND_Y, 56));
              worker[0].getEntity().get().teleportTo(corner.getX() + 0.5, corner.getY(), corner.getZ() + 0.5);
          })
          .awaitUntil("lumberjack walks to within 4 blocks of his hut", 2500,
            () -> horizontalDistance(worker[0], hut[0].getPosition()) <= 4,
            () -> ColonyFixture.describe(worker[0]) + ", distance " + String.format("%.1f", horizontalDistance(worker[0], hut[0].getPosition()))
                    + ", hut at " + hut[0].getPosition().toShortString())
          .succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------------------------------------------

    private static double horizontalDistance(final ICitizenData citizen, final BlockPos target)
    {
        return citizen.getEntity()
          .map(entity -> Math.sqrt(Math.pow(entity.getX() - (target.getX() + 0.5), 2) + Math.pow(entity.getZ() - (target.getZ() + 0.5), 2)))
          .orElse(Double.MAX_VALUE);
    }

    private static List<ChunkPos> unclaimedChunks(final IColony colony, final GameTestHelper helper)
    {
        final int radius = MineColonies.getConfig().getServer().initialColonySize.get();
        final ChunkPos center = ChunkPos.containing(colony.getCenter());
        final List<ChunkPos> unclaimed = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                final int x = center.x() + dx;
                final int z = center.z() + dz;
                if (ColonyUtils.getOwningColony(helper.getLevel().getChunk(x, z)) != colony.getID())
                {
                    unclaimed.add(new ChunkPos(x, z));
                }
            }
        }
        return unclaimed;
    }

    private static int ownerOf(final ServerLevel level, final ChunkPos chunk)
    {
        return ColonyUtils.getOwningColony(level.getChunk(chunk.x(), chunk.z()));
    }

    /**
     * @return the chunks within {@code radius} chunks of the colony center that the colony owns.
     */
    private static Set<ChunkPos> ownedChunks(final IColony colony, final ServerLevel level, final int radius)
    {
        final ChunkPos center = ChunkPos.containing(colony.getCenter());
        final Set<ChunkPos> owned = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                final ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
                if (ownerOf(level, pos) == colony.getID())
                {
                    owned.add(pos);
                }
            }
        }
        return owned;
    }

    private static Map<Skill, Integer> allSkills(final int level)
    {
        final Map<Skill, Integer> skills = new java.util.EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, level);
        }
        return skills;
    }

    private static boolean skillsInRange(final ICitizenData citizen)
    {
        for (final Skill skill : Skill.values())
        {
            final int level = citizen.getCitizenSkillHandler().getLevel(skill);
            if (level < 1 || level > CitizenConstants.MAX_CITIZEN_LEVEL)
            {
                return false;
            }
        }
        return true;
    }

    private static String skills(final ICitizenData citizen)
    {
        final StringBuilder text = new StringBuilder();
        for (final Skill skill : Skill.values())
        {
            text.append(skill.name(), 0, 3).append('=').append(citizen.getCitizenSkillHandler().getLevel(skill)).append(' ');
        }
        return text.toString().trim();
    }

    private static String describeAll(final IColony colony)
    {
        return colony.getCitizenManager().getCitizens().size() + " citizens (max " + colony.getCitizenManager().getMaxCitizens() + "): "
                 + colony.getCitizenManager().getCitizens().stream().map(ColonyFixture::describe).collect(Collectors.joining("; "));
    }

    /**
     * @return living citizen entities in and around the arena; only those of {@code citizen} if not null.
     */
    private static List<EntityCitizen> citizenEntities(final GameTestHelper helper, @Nullable final ICitizenData citizen)
    {
        final AABB bounds = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(80);
        return helper.getLevel().getEntitiesOfClass(EntityCitizen.class, bounds,
          e -> !e.isRemoved() && (citizen == null || e.getCitizenData() == citizen
                                    || (e.getCitizenColonyHandler().getColonyId() == citizen.getColony().getID() && e.getCivilianID() == citizen.getId())));
    }

    /**
     * A fake player with a fixed UUID derived from {@code n}, in the given game mode. Not in the server's player list.
     */
    private static ServerPlayer fakePlayer(final ServerLevel level, final int n, final String name, final GameType gameType)
    {
        final UUID id = UUID.fromString(String.format("6d3b3a8e-9c1f-4f63-a3c5-0e2e2e%06d", n));
        final FakePlayer player = FakePlayer.get(level, new GameProfile(id, name));
        player.setGameMode(gameType);
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();
        return player;
    }

    /**
     * The player right-clicks the top face of {@code absPos} with {@code stack} in the main hand, like a real right click on the server.
     */
    private static InteractionResult use(final ServerPlayer player, final ItemStack stack, final BlockPos absPos, final boolean sneaking)
    {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(sneaking);
        try
        {
            final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absPos).add(0, 0.5, 0), Direction.UP, absPos, false);
            return player.gameMode.useItemOn(player, player.level(), stack, InteractionHand.MAIN_HAND, hit);
        }
        finally
        {
            player.setShiftKeyDown(false);
        }
    }

    /**
     * COL-05: the player tries to break a stone, place a cobblestone, open the chest and open the hut menu. Returns what was allowed.
     */
    private static Map<String, Boolean> tryEverything(final ColonyFixture fixture, final ServerPlayer player, final int column, final BlockPos chestRel,
      final IBuilding hut)
    {
        final ServerLevel level = fixture.level();
        final Map<String, Boolean> result = new LinkedHashMap<>();
        final BlockPos stone = fixture.abs(new BlockPos(30 + column * 2, ColonyFixture.GROUND_Y, 52));
        final BlockPos place = fixture.abs(new BlockPos(30 + column * 2, ColonyFixture.GROUND_Y, 56));
        level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        // Forget earlier denials of this player, so each try is judged on its own (the town hall de-duplicates its list).
        fixture.colony().getServerBuildingManager().getTownHall().removePermissionEvents(player.getUUID());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        player.gameMode.destroyBlock(stone);
        result.put("break", !level.getBlockState(stone).is(Blocks.STONE));

        use(player, new ItemStack(Items.COBBLESTONE, 4), place.below(), false);
        result.put("place", level.getBlockState(place).is(Blocks.COBBLESTONE));

        final InteractionResult chestResult = use(player, ItemStack.EMPTY, fixture.abs(chestRel), false);
        result.put("chest", !(chestResult instanceof InteractionResult.Fail));

        final InteractionResult hutResult = use(player, ItemStack.EMPTY, hut.getPosition(), false);
        result.put("hut", !(hutResult instanceof InteractionResult.Fail));
        LOGGER.info("COL-05: {} rank {}: chest {} hut {} -> {}", player.getName().getString(), fixture.colony().getPermissions().getRank(player).getName(),
          chestResult, hutResult, result);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return result;
    }

    /**
     * @return the denied actions the town hall recorded (server side list, read by reflection: there is no server getter).
     */
    @SuppressWarnings("unchecked")
    private static List<PermissionEvent> permissionEvents(final IColony colony)
    {
        try
        {
            final Field field = BuildingTownHall.class.getDeclaredField("permissionEvents");
            field.setAccessible(true);
            return new ArrayList<>((Collection<PermissionEvent>) field.get(colony.getServerBuildingManager().getTownHall()));
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static String describeEvents(final IColony colony)
    {
        return permissionEvents(colony).stream().map(e -> e.getName() + " " + e.getAction() + " at " + e.getPosition().toShortString()).toList().toString();
    }

    private static void setField(final Object target, final String name, final Object value)
    {
        try
        {
            final Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static boolean denied(final IColony colony, final ServerPlayer player, final BlockPos pos)
    {
        return permissionEvents(colony).stream().anyMatch(e -> player.getUUID().equals(e.getId()) && pos.equals(e.getPosition()));
    }

    private static int countAxes(final ICitizenData citizen)
    {
        int total = 0;
        for (int slot = 0; slot < citizen.getInventory().getSlots(); slot++)
        {
            if (citizen.getInventory().getStackInSlot(slot).is(Items.WOODEN_AXE))
            {
                total += citizen.getInventory().getStackInSlot(slot).getCount();
            }
        }
        return total;
    }

    private static int countAxes(final ColonyFixture fixture, final IBuilding building)
    {
        return fixture.countItems(building, stack -> stack.is(Items.WOODEN_AXE));
    }

    /**
     * Sends a diplomacy event the way the town hall's alliance page does (TriggerConnectionEventMessage, server side).
     */
    private static void sendEvent(final IColony from, final ServerPlayer player, final ConnectionEventType type, final IColony to)
    {
        new TriggerConnectionEventMessage(from, new ConnectionEvent(from.getID(), from.getName(), type), to.getID()).onExecute(null, player);
    }

    private static boolean hasEvent(final IColony in, final IColony from, final ConnectionEventType type)
    {
        return in.getConnectionManager().getConnectionEvents().stream().anyMatch(e -> e.id() == from.getID() && e.connectionEventType() == type);
    }

    /**
     * A friend of {@code colony} standing at {@code absPos} (survival, so no op bypass).
     */
    private static ServerPlayer traveller(final ColonyFixture fixture, final IColony colony, final BlockPos absPos)
    {
        final ServerPlayer player = fakePlayer(fixture.level(), 21, "E2ETraveller", GameType.SURVIVAL);
        colony.getPermissions().addPlayer(player.getUUID(), "E2ETraveller", colony.getPermissions().getRankFriend());
        player.teleportTo(absPos.getX() + 0.25, absPos.getY(), absPos.getZ() + 0.25);
        return player;
    }

    /**
     * The traveller presses "teleport" for colony B in A's gate house window (TeleportToColonyMessage with the connection's gate position).
     */
    private static void teleportToAlly(final Twin t, final ServerPlayer player)
    {
        final ColonyConnection connection = t.a.getConnectionManager().getDirectlyConnectedColonies().get(t.b.getID());
        new TeleportToColonyMessage(t.b.getDimension(), t.b.getID(), connection.pos, t.a.getID(), 0).onExecute(null, player);
    }

    private static String connections(final IColony colony)
    {
        return colony.getConnectionManager().getDirectlyConnectedColonies().values().stream()
                 .map(c -> "#" + c.id + " " + c.diplomacyStatus + " gate " + c.pos.toShortString())
                 .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String node(final IColony colony, final BlockPos pos)
    {
        final var node = colony.getConnectionManager().getNode(pos);
        return node == null ? "none" : "prev " + node.getPreviousNode().toShortString() + " next " + node.getNextNode().toShortString() + " target " + node.getTargetColonyId();
    }

    // --- COL-06 -------------------------------------------------------------------------------------------------------------------------

    private static Set<String> openRequestTokens(final IColony colony)
    {
        final Set<String> tokens = new HashSet<>();
        for (final IBuilding building : colony.getServerBuildingManager().getBuildings().values())
        {
            for (final Collection<IToken<?>> list : building.getOpenRequestsByRequestableType().values())
            {
                for (final IToken<?> token : list)
                {
                    tokens.add(building.getPosition().toShortString() + ":" + token.getIdentifier());
                }
            }
        }
        return tokens;
    }

    /**
     * Everything COL-06 compares, as text per key.
     */
    private static Map<String, String> snapshot(final IColony colony, final Identifier research)
    {
        final Map<String, String> s = new LinkedHashMap<>();
        for (final ICitizenData c : colony.getCitizenManager().getCitizens())
        {
            // Happiness is cached until something resets it; compare freshly computed values from the stored modifiers.
            setField(c.getCitizenHappinessHandler(), "cachedHappiness", -1.0);
        }
        s.put("name", colony.getName());
        s.put("owner", String.valueOf(colony.getPermissions().getOwner()));
        for (final IBuilding b : colony.getServerBuildingManager().getBuildings().values())
        {
            s.put("building " + b.getPosition().toShortString(), b.getBuildingType().getRegistryName() + " level " + b.getBuildingLevel()
                                                                   + " assigned " + b.getAllAssignedCitizen().stream().map(ICitizenData::getId).sorted().toList());
        }
        for (final ICitizenData c : colony.getCitizenManager().getCitizens())
        {
            s.put("citizen " + c.getId(), c.getName() + " female " + c.isFemale() + " job " + (c.getJob() == null ? "-" : c.getJob().getJobRegistryEntry().getKey())
                                          + " work " + (c.getWorkBuilding() == null ? "-" : c.getWorkBuilding().getPosition().toShortString())
                                          + " home " + (c.getHomeBuilding() == null ? "-" : c.getHomeBuilding().getPosition().toShortString())
                                          + " skills " + skills(c)
                                          + " saturation " + String.format("%.2f", c.getSaturation())
                                          + " happiness " + String.format("%.2f", c.getCitizenHappinessHandler().getHappiness(colony, c)));
        }
        for (final IServerWorkOrder order : colony.getWorkManager().getWorkOrders().values())
        {
            s.put("workorder " + order.getID(), order.getWorkOrderType() + " at " + order.getLocation().toShortString() + " " + order.getCurrentLevel() + "->"
                                                + order.getTargetLevel() + " claimed by " + (order.isClaimed() ? order.getClaimedBy().toShortString() : "-"));
        }
        s.put("open requests", new java.util.TreeSet<>(openRequestTokens(colony)).toString());
        final IGlobalResearch global = IGlobalResearchTree.getInstance().getResearch(research);
        final ILocalResearch local = colony.getResearchManager().getResearchTree().getResearch(global.getBranch(), global.getId());
        s.put("research " + research, local == null ? "none" : local.getState() + " progress " + local.getProgress());
        s.put("claimed chunks", String.valueOf(new java.util.TreeSet<>(ownedChunks(colony, (ServerLevel) colony.getWorld(), 6).stream().map(ChunkPos::toString).toList())));
        return s;
    }

    private static String diff(final Map<String, String> before, final Map<String, String> after)
    {
        final StringBuilder text = new StringBuilder();
        final Set<String> keys = new java.util.TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (final String key : keys)
        {
            if (!Objects.equals(before.get(key), after.get(key)))
            {
                text.append(key).append(": ").append(before.get(key)).append(" -> ").append(after.get(key)).append("; ");
            }
        }
        return text.toString();
    }

    /**
     * Saves the colony to NBT the way the world save does ({@code getColonyTag}), takes it out of the colony manager, saves and unloads its
     * citizen entities (as the chunk save does) and loads a new colony object from the NBT the way a server
     * start does ({@code Colony.loadColony} + {@code onWorldLoad}). Hut block entities are pointed at the new colony and buildings, as
     * freshly loaded block entities would look them up. Finally the citizen entities are loaded back from their saved data.
     */
    private static IColony saveAndReload(final IColony colony, final ServerLevel level)
    {
        final CompoundTag tag = colony.getColonyTag().copy();
        // Citizen entities are saved with their chunk and unloaded, like on a server stop; loaded again after the colony.
        final List<CompoundTag> entityTags = new ArrayList<>();
        for (final ICitizenData citizen : colony.getCitizenManager().getCitizens())
        {
            citizen.getEntity().ifPresent(e -> {
                final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                e.save(out);
                entityTags.add(out.buildResult());
                e.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            });
        }
        ((Colony) colony).onWorldUnload(level);
        final IServerColonySaveData saveData = IServerColonySaveData.getOrComputeSaveData(level);
        saveData.deleteColony(colony.getID());

        final Colony loaded = Colony.loadColony(tag, null, level.registryAccess());
        if (loaded == null)
        {
            throw new IllegalStateException("Colony.loadColony returned null");
        }
        saveData.addColony(loaded);
        loaded.onWorldLoad(level);
        for (final IBuilding building : loaded.getServerBuildingManager().getBuildings().values())
        {
            if (level.getBlockEntity(building.getPosition()) instanceof final AbstractTileEntityColonyBuilding hut)
            {
                hut.setColony(loaded);
                hut.setBuilding(building);
            }
        }
        for (final CompoundTag entityTag : entityTags)
        {
            final var type = net.minecraft.world.entity.EntityType.by(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entityTag));
            final Entity entity = type.orElseThrow().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), entityTag));
            level.addFreshEntity(entity);
        }
        // The fake owner stands at the town hall: subscribe him like the player join/move handling does for a real player.
        loaded.getPackageManager().addCloseSubscriber(FakePlayer.get(level, new GameProfile(ColonyFixture.OWNER_ID, ColonyFixture.OWNER_NAME)));
        loaded.getPackageManager().addImportantColonyPlayer(FakePlayer.get(level, new GameProfile(ColonyFixture.OWNER_ID, ColonyFixture.OWNER_NAME)));
        return loaded;
    }

    // --- ROB-07 -------------------------------------------------------------------------------------------------------------------------

    /**
     * @return jobs submitted to the pathfinding executor since the start, minus the one marker job the test server submits per tick.
     */
    private static long realPathJobs(final ThreadPoolExecutor executor, final long startTasks, final long startTick, final GameTestHelper helper)
    {
        return (executor.getTaskCount() - startTasks) - (helper.getLevel().getServer().getTickCount() - startTick);
    }
}
