package com.minecolonies.gametest.suites;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IVisitorData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IGuardBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.modules.ISettingsModule;
import com.minecolonies.api.colony.buildings.modules.settings.ISetting;
import com.minecolonies.api.colony.buildings.modules.settings.ISettingKey;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.colonyEvents.EventStatus;
import com.minecolonies.api.colony.colonyEvents.IColonyEvent;
import com.minecolonies.api.colony.colonyEvents.IColonyRaidEvent;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.managers.interfaces.IRaiderManager;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.quests.IQuestInstance;
import com.minecolonies.api.quests.IQuestManager;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.constant.HappinessConstants;
import com.minecolonies.api.util.constant.StatisticsConstants;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.modules.TavernBuildingModule;
import com.minecolonies.core.colony.buildings.modules.settings.BoolSetting;
import com.minecolonies.core.colony.buildings.modules.settings.GuardTaskSetting;
import com.minecolonies.core.colony.events.raid.HordeRaidEvent;
import com.minecolonies.core.colony.interactionhandling.RecruitmentInteraction;
import com.minecolonies.core.entity.other.DruidPotionEntity;
import com.minecolonies.core.quests.QuestInstance;
import com.minecolonies.core.quests.triggers.CitizenTriggerReturnData;
import com.minecolonies.gametest.framework.ColonyFixture;
import com.minecolonies.gametest.framework.TestFlow;
import com.minecolonies.gametest.mixin.GameTestHelperAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * End-to-end tests from docs/e2e-tests/defense.md and docs/e2e-tests/visitors-quests.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:defense_visitors_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=defense_visitors_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 * <p>
 * Results: docs/e2e-tests/protocol/defense-visitors.md.
 */
public class DefenseVisitorsTests
{
    /**
     * Where the guard building stands in the guard tests (relative).
     */
    private static final BlockPos GUARD_HUT = new BlockPos(12, ColonyFixture.GROUND_Y, 12);

    /**
     * Where raids are forced to spawn (relative): the far corner of the arena, about 34 blocks from the town hall.
     */
    private static final BlockPos RAID_SPAWN = new BlockPos(56, ColonyFixture.GROUND_Y, 56);

    // ====================================================================================================================================
    // DEF-01 / DEF-02 Guards fight
    // ====================================================================================================================================

    /**
     * DEF-01 Guard kills a hostile mob: a knight notices a husk (a zombie that doesn't burn by day) near its tower and kills it, gains XP,
     * and hurts neither the bystander citizen nor the owner.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def01_guardKillsHostileMob(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.guardTower.get(), ModJobs.knight.get(), List.of(),
          List.of(new ItemStack(Items.STONE_SWORD), new ItemStack(Items.LEATHER_CHESTPLATE)),
          stack -> stack.is(Items.STONE_SWORD), null, true, true));
    }

    /**
     * DEF-02 Knight: melee with a sword.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def02_knightFightsWithSword(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.guardTower.get(), ModJobs.knight.get(), List.of(),
          List.of(new ItemStack(Items.STONE_SWORD)), stack -> stack.is(Items.STONE_SWORD), null, true, false));
    }

    /**
     * DEF-02 Huscarl: melee with an axe (research "sliced and decided" unlocks the huscarl).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def02_huscarlFightsWithAxe(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.guardTower.get(), ModJobs.huscarl.get(), List.of(Identifier.parse("minecolonies:combat/slicedanddecided")),
          List.of(new ItemStack(Items.STONE_AXE)), stack -> stack.is(Items.STONE_AXE), null, true, false));
    }

    /**
     * DEF-02 Ranger: keeps distance and shoots arrows with a bow.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def02_rangerShootsArrows(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.guardTower.get(), ModJobs.archer.get(), List.of(),
          List.of(new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64)), stack -> stack.is(Items.BOW), AbstractArrow.class, true, false));
    }

    /**
     * DEF-02 Marksman: shoots arrows with a crossbow (research "that hit the mark" unlocks the marksman).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def02_marksmanShootsCrossbow(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.guardTower.get(), ModJobs.marksman.get(), List.of(Identifier.parse("minecolonies:combat/thathitthemark")),
          List.of(new ItemStack(Items.CROSSBOW), new ItemStack(Items.ARROW, 64)), stack -> stack.is(Items.CROSSBOW), AbstractArrow.class, true, false));
    }

    /**
     * DEF-02 Druid: throws potions. The druid's potions don't kill: against a monster it throws slowness (or weakness with magic potions
     * and the research), so the check is "a druid potion was thrown and the husk got slowed".
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def02_druidThrowsPotions(final GameTestHelper helper)
    {
        guardFight(helper, new FightSpec(ModBuildings.barracksTower.get(), ModJobs.druid.get(), List.of(), List.of(), null, DruidPotionEntity.class, false, false));
    }

    /**
     * Shared setup for DEF-01/DEF-02: a guard building with one guard of the given job and its gear, a husk spawned within sight.
     */
    private record FightSpec(BuildingEntry building, JobEntry job, List<Identifier> research, List<ItemStack> gear,
                             @Nullable Predicate<ItemStack> weapon, @Nullable Class<? extends Entity> projectile, boolean mobMustDie,
                             boolean withBystander)
    {
    }

    private static void guardFight(final GameTestHelper helper, final FightSpec spec)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] guard = new ICitizenData[1];
        final ICitizenData[] bystander = new ICitizenData[1];
        final Mob[] mob = new Mob[1];
        final double[] xpBefore = new double[1];
        final boolean[] seen = new boolean[3]; // 0 = mob hurt by guard, 1 = projectile from guard, 2 = weapon in hand while attacking
        final Set<String> states = new HashSet<>();

        fixture.flow()
          .run("found colony, place " + spec.building().getRegistryName().getPath() + ", hire a " + spec.job().getKey().getPath(), () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              for (final Identifier research : spec.research())
              {
                  fixture.completeResearch(research);
              }
              hut[0] = fixture.placeBuilding(spec.building(), 1, GUARD_HUT);
              guard[0] = fixture.spawnCitizen(allSkills(30), null, null, GUARD_HUT.offset(6, 0, 6));
              fixture.hire(guard[0], hut[0], spec.job());
              for (final ItemStack stack : spec.gear())
              {
                  InventoryUtils.addItemStackToItemHandler(guard[0].getInventory(), stack.copy());
              }
              if (spec.withBystander())
              {
                  bystander[0] = fixture.spawnCitizen(10, null, null);
              }
          })
          .awaitUntil("guard entity exists", 200, () -> guard[0].getEntity().isPresent(), () -> ColonyFixture.describe(guard[0]))
          .awaitUntil("guard's AI runs its combat states (not preparing gear)", 1500,
            () -> {
                final String state = workerState(guard[0]);
                return state.equals(CombatAIStates.NO_TARGET.toString()) || state.equals(CombatAIStates.ATTACKING.toString());
            },
            () -> ColonyFixture.describe(guard[0]))
          .run("guard steps into the open field, a husk appears 8 blocks from him", () -> {
              xpBefore[0] = guard[0].getCitizenSkillHandler().getTotalXP();
              final BlockPos field = fixture.abs(standable(fixture, new BlockPos(36, ColonyFixture.GROUND_Y, 10)));
              guard[0].getEntity().get().teleportTo(field.getX() + 0.5, field.getY(), field.getZ() + 0.5);
              mob[0] = spawnMob(fixture, EntityTypes.HUSK, new BlockPos(44, ColonyFixture.GROUND_Y, 10));
          })
          .awaitUntil(spec.mobMustDie() ? "guard kills the husk" : "guard hits the husk with its weapon", 3000,
            () -> {
                final LivingEntity guardEntity = guard[0].getEntity().orElse(null);
                if (guardEntity == null)
                {
                    return false;
                }
                states.add(workerState(guard[0]));
                if (mob[0].getLastHurtByMob() == guardEntity)
                {
                    seen[0] = true;
                }
                if (spec.weapon() != null && workerState(guard[0]).equals(CombatAIStates.ATTACKING.toString())
                      && spec.weapon().test(guardEntity.getMainHandItem()))
                {
                    seen[2] = true;
                }
                if (spec.projectile() != null)
                {
                    for (final Entity projectile : fixture.level().getEntitiesOfClass(spec.projectile(), guardEntity.getBoundingBox().inflate(32)))
                    {
                        if (projectile instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() == guardEntity)
                        {
                            seen[1] = true;
                        }
                    }
                }
                final boolean weaponOk = spec.weapon() == null || seen[2];
                final boolean projectileOk = spec.projectile() == null || seen[1];
                if (spec.mobMustDie())
                {
                    return !mob[0].isAlive() && seen[0] && weaponOk && projectileOk;
                }
                return projectileOk && mob[0].hasEffect(MobEffects.SLOWNESS);
            },
            () -> ColonyFixture.describe(guard[0]) + ", husk " + (mob[0].isAlive() ? "alive at " + mob[0].blockPosition().toShortString() + " hp " + mob[0].getHealth()
                                                                : "dead") + ", hurt by guard " + seen[0] + ", projectile seen " + seen[1]
                    + ", weapon in hand while attacking " + seen[2] + ", main hand " + guard[0].getEntity().map(e -> e.getMainHandItem().toString()).orElse("-")
                    + ", guard states seen " + states)
          .awaitUntil("guard gained XP", 400, () -> guard[0].getCitizenSkillHandler().getTotalXP() > xpBefore[0],
            () -> "total XP " + guard[0].getCitizenSkillHandler().getTotalXP() + " (before " + xpBefore[0] + ")")
          .check("the guard hit no citizen and not the owner",
            () -> {
                final LivingEntity guardEntity = guard[0].getEntity().orElse(null);
                if (fixture.owner().getLastHurtByMob() == guardEntity && guardEntity != null)
                {
                    return false;
                }
                return bystander[0] == null || bystander[0].getEntity().map(e -> e.getLastHurtByMob() != guardEntity).orElse(true);
            },
            () -> "bystander " + ColonyFixture.describe(bystander[0]) + ", last hurt by "
                    + (bystander[0] == null ? "-" : bystander[0].getEntity().map(e -> String.valueOf(e.getLastHurtByMob())).orElse("-")))
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-03 Patrol, guard, follow
    // ====================================================================================================================================

    /**
     * DEF-03 Patrol, guard and follow modes: the guard patrols between points inside its range, then stays near a fixed guard spot, then
     * follows the owner around.
     */
    // Guards nap: every 200 ticks without a target a 1 in 35 chance (adaptability 30) to sleep for 2500-3000 ticks where they stand
    // (AbstractEntityAIGuard.shouldSleep, by design). A guarding guard also walks to his tower to dump (INVENTORY_FULL), idles there and
    // walks back lazily (1 in 10 every 100 ticks). The budgets are large: with 6000 the guard once slept 7.9 blocks from the guard spot
    // until the wait ran out; with 12000 one of 51 repeated runs failed (dump trip, 3,300 ticks idle at the tower, two naps).
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 100000, skyAccess = true)
    public void def03_patrolGuardFollow(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] guard = new ICitizenData[1];
        final Set<BlockPos> patrolPoints = new HashSet<>();
        final double[] maxDistFromHut = new double[1];
        final BlockPos guardSpot = new BlockPos(44, ColonyFixture.GROUND_Y, 44);

        fixture.flow()
          .run("found colony, guard tower with a knight", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.guardTower.get(), 1, GUARD_HUT);
              guard[0] = fixture.spawnCitizen(allSkills(30), null, null, GUARD_HUT.offset(6, 0, 6));
              fixture.hire(guard[0], hut[0], ModJobs.knight.get());
              InventoryUtils.addItemStackToItemHandler(guard[0].getInventory(), new ItemStack(Items.STONE_SWORD));
          })
          .awaitUntil("guard entity exists", 200, () -> guard[0].getEntity().isPresent(), () -> ColonyFixture.describe(guard[0]))
          .check("default task is patrol", () -> ((IGuardBuilding) hut[0]).getTask().equals(GuardTaskSetting.PATROL), () -> ((IGuardBuilding) hut[0]).getTask())
          .awaitUntil("patrol: guard visits at least 3 different patrol points", 20000,
            () -> {
                final BlockPos point = ((IGuardBuilding) hut[0]).getNextPatrolTarget(false);
                if (point != null)
                {
                    patrolPoints.add(point);
                }
                guard[0].getEntity().ifPresent(e -> maxDistFromHut[0] = Math.max(maxDistFromHut[0], Math.sqrt(e.blockPosition().distSqr(hut[0].getPosition()))));
                return patrolPoints.size() >= 3 && maxDistFromHut[0] > 8;
            },
            () -> ColonyFixture.describe(guard[0]) + ", patrol points " + patrolPoints + ", max distance from tower " + String.format("%.1f", maxDistFromHut[0]))
          .check("patrol stays inside the patrol range of the tower",
            () -> maxDistFromHut[0] <= ((IGuardBuilding) hut[0]).getPatrolDistance(),
            () -> "max distance " + maxDistFromHut[0] + ", patrol distance " + ((IGuardBuilding) hut[0]).getPatrolDistance())
          .run("set task to guard a fixed spot", () -> {
              ((IGuardBuilding) hut[0]).setGuardPos(fixture.abs(guardSpot));
              setGuardTask(fixture, hut[0], GuardTaskSetting.GUARD);
          })
          .awaitUntil("guard: guard walks to within 6 blocks of the guard spot and stops there", 30000,
            () -> distance(guard[0], fixture.abs(guardSpot)) <= 6 && guard[0].getEntity().map(e -> e.getNavigation().isDone()).orElse(false),
            () -> guardDetails(guard[0]) + ", distance " + String.format("%.1f", distance(guard[0], fixture.abs(guardSpot))))
          .idle(1500)
          .awaitUntil("guard: after 1500 more ticks the guard is (back) within 6 blocks of the spot", 20000,
            () -> distance(guard[0], fixture.abs(guardSpot)) <= 6,
            () -> guardDetails(guard[0]) + ", distance " + String.format("%.1f", distance(guard[0], fixture.abs(guardSpot))))
          .run("owner joins the world's player list (needed for follow), set task to follow the owner", () -> {
              fixture.addOwnerToPlayerLists();
              setGuardTask(fixture, hut[0], GuardTaskSetting.FOLLOW);
              moveOwner(fixture, new BlockPos(48, ColonyFixture.GROUND_Y, 20));
          })
          .awaitUntil("follow: guard comes within 6 blocks of the owner", 12000,
            () -> distance(guard[0], fixture.owner().blockPosition()) <= 6,
            () -> ColonyFixture.describe(guard[0]) + ", owner at " + fixture.owner().blockPosition().toShortString() + ", distance "
                    + String.format("%.1f", distance(guard[0], fixture.owner().blockPosition())) + ", task " + ((IGuardBuilding) hut[0]).getTask()
                    + ", position to follow " + ((IGuardBuilding) hut[0]).getPositionToFollow() + ", player to follow " + ((IGuardBuilding) hut[0]).getPlayerToFollowOrRally()
                    + ", owner in level players " + fixture.level().players().contains(fixture.owner()))
          .run("owner moves to the other side", () -> moveOwner(fixture, new BlockPos(20, ColonyFixture.GROUND_Y, 50)))
          .awaitUntil("follow: guard follows to within 6 blocks of the owner's new place", 12000,
            () -> distance(guard[0], fixture.owner().blockPosition()) <= 6,
            () -> ColonyFixture.describe(guard[0]) + ", owner at " + fixture.owner().blockPosition().toShortString() + ", distance "
                    + String.format("%.1f", distance(guard[0], fixture.owner().blockPosition())))
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-04 Gear by level
    // ====================================================================================================================================

    /**
     * DEF-04 Guards get gear by level: an unequipped knight in a level 1 tower requests a weapon and armor; the courier brings them from a
     * warehouse that holds leather, iron and diamond gear. The knight ends up wearing leather (level 1 allows leather/gold armor) and holding
     * a stone sword (level 1 allows wood/stone/gold tools), never iron or diamond.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 26000, skyAccess = true)
    public void def04_guardGetsGearByLevel(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[2];
        final ICitizenData[] guard = new ICitizenData[1];
        final ICitizenData[] courier = new ICitizenData[1];

        fixture.flow()
          .run("found colony, guard tower, warehouse with gear, courier, unequipped knight", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.guardTower.get(), 1, GUARD_HUT);
              hut[1] = fixture.placeBuilding(ModBuildings.wareHouse.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 12));
              fixture.fillWarehouse(new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.LEATHER_CHESTPLATE), new ItemStack(Items.LEATHER_LEGGINGS),
                new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.IRON_HELMET), new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS), new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.STONE_SWORD), new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.DIAMOND_SWORD));
              final IBuilding courierHut = fixture.placeBuilding(ModBuildings.deliveryman.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              courier[0] = fixture.spawnCitizen(20, courierHut, null);
              guard[0] = fixture.spawnCitizen(allSkills(30), null, null, GUARD_HUT.offset(6, 0, 6));
              fixture.hire(guard[0], hut[0], ModJobs.knight.get());
          })
          .awaitUntil("knight holds a sword and wears a chestplate", 10000,
            () -> guard[0].getEntity().map(e -> !e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()
                                                && InventoryUtils.hasItemInItemHandler(guard[0].getInventory(), s -> s.is(net.minecraft.tags.ItemTags.SWORDS))).orElse(false),
            () -> ColonyFixture.describe(guard[0]) + ", worn " + guard[0].getEntity().map(DefenseVisitorsTests::wornArmor).orElse("-")
                    + ", inventory " + inventoryText(guard[0]) + ", open requests " + hut[0].getOpenRequests(guard[0].getId()).stream()
                      .map(r -> r.getShortDisplayString().getString() + "/" + r.getState()).toList() + ", courier " + ColonyFixture.describe(courier[0])
                    + ", tower swords " + fixture.countItems(hut[0], s -> s.is(net.minecraft.tags.ItemTags.SWORDS)))
          .check("level 1: the knight wears only leather and holds a stone sword (no iron, no diamond)",
            () -> guard[0].getEntity().map(e -> {
                for (final ItemStack worn : List.of(e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                  e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)))
                {
                    if (!worn.isEmpty() && !worn.getItem().toString().contains("leather"))
                    {
                        return false;
                    }
                }
                return !InventoryUtils.hasItemInItemHandler(guard[0].getInventory(), s -> s.is(Items.IRON_SWORD) || s.is(Items.DIAMOND_SWORD)
                                                                                         || s.getItem().toString().contains("iron_") || s.getItem().toString().contains("diamond_"));
            }).orElse(false),
            () -> "worn " + guard[0].getEntity().map(DefenseVisitorsTests::wornArmor).orElse("-") + ", inventory " + inventoryText(guard[0]))
          .run("upgrade the tower to level 3: the level 3 blueprint is pasted over it (as the builder's finished upgrade places it)", () -> {
              fixture.pasteHut(ModBuildings.GUARD_TOWER_ID, 3, hut[0].getPosition(), com.ldtteam.structurize.api.RotationMirror.NONE);
          })
          .check("the tower is level 3 now", () -> hut[0].getBuildingLevel() == 3, () -> "level " + hut[0].getBuildingLevel())
          .run("the knight loses his helmet and leaves his sword in the tower; an iron and a diamond chestplate are put into the tower", () -> {
              // Guards only look at their gear while at their hut (PREPARING -> atBuildingActions), which they visit when a weapon is
              // missing or to dump loot (the dump cycle takes far longer than the budget). The sword in the tower sends him there now.
              final LivingEntity knight = guard[0].getEntity().get();
              guard[0].getInventory().moveArmorToInventory(net.minecraft.world.entity.EquipmentSlot.HEAD);
              knight.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
              knight.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
              for (int slot = 0; slot < guard[0].getInventory().getSlots(); slot++)
              {
                  final ItemStack stack = guard[0].getInventory().getStackInSlot(slot);
                  if (stack.is(Items.LEATHER_HELMET))
                  {
                      guard[0].getInventory().extractItem(slot, 64, false);
                  }
                  else if (stack.is(net.minecraft.tags.ItemTags.SWORDS))
                  {
                      fixture.fillBuilding(hut[0], guard[0].getInventory().extractItem(slot, 64, false));
                  }
              }
              fixture.fillBuilding(hut[0], new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.DIAMOND_CHESTPLATE));
          })
          .awaitUntil("level 3: the knight takes the iron chestplate from the tower, requests a helmet (iron arrives) and holds a sword again", 12000,
            () -> guard[0].getEntity().map(e -> e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.IRON_HELMET)
                                                && e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)
                                                && InventoryUtils.hasItemInItemHandler(guard[0].getInventory(), st -> st.is(net.minecraft.tags.ItemTags.SWORDS))).orElse(false),
            () -> ColonyFixture.describe(guard[0]) + ", worn " + guard[0].getEntity().map(DefenseVisitorsTests::wornArmor).orElse("-")
                    + ", inventory " + inventoryText(guard[0]) + ", open requests " + hut[0].getOpenRequests(guard[0].getId()).stream()
                      .map(r -> r.getShortDisplayString().getString() + "/" + r.getState()).toList() + ", courier " + ColonyFixture.describe(courier[0])
                    + ", tower chestplates " + fixture.countItems(hut[0], st -> st.is(Items.IRON_CHESTPLATE) || st.is(Items.DIAMOND_CHESTPLATE)))
          .check("level 3 allows iron but not diamond: no diamond armor worn, the diamond chestplate is still in the tower",
            () -> guard[0].getEntity().map(e -> !wornArmor(e).contains("diamond")).orElse(false)
                    && fixture.countItems(hut[0], st -> st.is(Items.DIAMOND_CHESTPLATE)) == 1,
            () -> "worn " + guard[0].getEntity().map(DefenseVisitorsTests::wornArmor).orElse("-") + ", diamond chestplates in tower "
                    + fixture.countItems(hut[0], st -> st.is(Items.DIAMOND_CHESTPLATE)))
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-05 Retreat
    // ====================================================================================================================================

    /**
     * DEF-05 Injured guards retreat and recover: with the retreat research done and the retreat setting on, a knight that falls below 20%
     * health while fighting away from its tower flees to the tower, regenerates to 75% and goes back to its task.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void def05_injuredGuardRetreats(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] guard = new ICitizenData[1];
        final Mob[] mob = new Mob[1];
        final float[] lowHealth = new float[1];

        fixture.flow()
          .run("found colony, retreat research, guard tower with a knight, retreat on", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.completeResearch(Identifier.parse("minecolonies:combat/regeneration"));
              hut[0] = fixture.placeBuilding(ModBuildings.guardTower.get(), 1, GUARD_HUT);
              guard[0] = fixture.spawnCitizen(allSkills(30), null, null, new BlockPos(44, ColonyFixture.GROUND_Y, 44));
              fixture.hire(guard[0], hut[0], ModJobs.knight.get());
              InventoryUtils.addItemStackToItemHandler(guard[0].getInventory(), new ItemStack(Items.STONE_SWORD));
              final BoolSetting retreat = hut[0].getSetting(AbstractBuildingGuards.RETREAT);
              if (!retreat.getValue())
              {
                  updateSetting(fixture, hut[0], AbstractBuildingGuards.RETREAT, new BoolSetting(true, retreat.getDefault()));
              }
          })
          .check("retreat setting on", () -> ((IGuardBuilding) hut[0]).shallRetrieveOnLowHealth(), () -> "off")
          .awaitUntil("guard entity exists and is ready", 1500,
            () -> guard[0].getEntity().isPresent() && workerState(guard[0]).equals(CombatAIStates.NO_TARGET.toString()),
            () -> ColonyFixture.describe(guard[0]))
          .run("move the guard far from the tower and spawn a slowed, weakened husk next to it", () -> {
              final BlockPos far = fixture.abs(standable(fixture, new BlockPos(46, ColonyFixture.GROUND_Y, 46)));
              guard[0].getEntity().get().teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
              mob[0] = spawnMob(fixture, EntityTypes.HUSK, new BlockPos(50, ColonyFixture.GROUND_Y, 46));
              mob[0].addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 600, 6));
              mob[0].addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 600, 9));
          })
          .awaitUntil("guard attacks the husk", 1500, () -> workerState(guard[0]).equals(CombatAIStates.ATTACKING.toString()),
            () -> ColonyFixture.describe(guard[0]))
          .run("guard drops to low health", () -> {
              fixture.setCitizenNeeds(guard[0], guard[0].getSaturation(), 2.0f);
              lowHealth[0] = guard[0].getEntity().get().getHealth();
          })
          .awaitUntil("guard flees", 400, () -> workerState(guard[0]).equals(AIWorkerState.GUARD_FLEE.toString()),
            () -> ColonyFixture.describe(guard[0]) + ", health " + guard[0].getEntity().map(LivingEntity::getHealth).orElse(-1f))
          .awaitUntil("guard reaches its tower and regenerates", 1500,
            () -> workerState(guard[0]).equals(AIWorkerState.GUARD_REGEN.toString()) && distance(guard[0], hut[0].getPosition()) <= 8,
            () -> ColonyFixture.describe(guard[0]) + ", distance to tower " + String.format("%.1f", distance(guard[0], hut[0].getPosition())))
          .awaitUntil("health back to at least 75% and guard back on duty", 3000,
            () -> guard[0].getEntity().map(e -> e.getHealth() >= (int) e.getMaxHealth() * 0.75).orElse(false)
                    && !workerState(guard[0]).equals(AIWorkerState.GUARD_REGEN.toString()) && !workerState(guard[0]).equals(AIWorkerState.GUARD_FLEE.toString()),
            () -> ColonyFixture.describe(guard[0]) + ", health " + guard[0].getEntity().map(e -> e.getHealth() + "/" + e.getMaxHealth()).orElse("-")
                    + " (was " + lowHealth[0] + ")")
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-06 Raids by culture
    // ====================================================================================================================================

    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void def06_barbarianRaid(final GameTestHelper helper)
    {
        raidLifecycle(helper, "barbarian_raid", "minecolonies:barbarian_raid", true);
    }

    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void def06_amazonRaid(final GameTestHelper helper)
    {
        raidLifecycle(helper, "amazon_raid", "minecolonies:amazon_raid", false);
    }

    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void def06_egyptianRaid(final GameTestHelper helper)
    {
        raidLifecycle(helper, "egyptian_raid", "minecolonies:egyptian_raid", false);
    }

    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void def06_norsemenRaid(final GameTestHelper helper)
    {
        raidLifecycle(helper, "norsemen_raid", "minecolonies:norsemen_raid", false);
    }

    /**
     * Pirates without ships (ships are off in the fixture): the land variant of the pirate raid.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void def06_pirateGroundRaid(final GameTestHelper helper)
    {
        raidLifecycle(helper, "pirate_raid", "minecolonies:pirate_ground_raid", false);
    }

    /**
     * DEF-06 one raid: forced raid of 4 raiders at the far corner. The raid event exists with the right culture, the raiders spawn at the
     * spawn point, camp and then march on the colony; when all are dead the event ends, is removed and the camp fires are gone.
     */
    private static void raidLifecycle(final GameTestHelper helper, final String culture, final String eventType, final boolean checkAmountRules)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IRaiderManager.RaidSpawnResult[] result = new IRaiderManager.RaidSpawnResult[2];
        final HordeRaidEvent[] event = new HordeRaidEvent[1];
        final double[] startDistance = new double[1];
        final int amount = 4;

        TestFlow flow = fixture.flow()
          .run("found colony", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
          });
        if (checkAmountRules)
        {
            flow = flow
              .run("a raid with the colony's own amount on a tiny colony", () -> result[1] = fixture.startRaid(culture, null, RAID_SPAWN))
              .check("a colony below raid level 75 is too small for a raid", () -> result[1] == IRaiderManager.RaidSpawnResult.TOO_SMALL,
                () -> "result " + result[1])
              .check("the raider amount never exceeds maxRaiders (+1 base)",
                () -> fixture.colony().getRaiderManager().calculateRaiderAmount(1_000_000) <= MineColonies.getConfig().getServer().maxRaiders.get() + 1,
                () -> "amount for a huge raid level " + fixture.colony().getRaiderManager().calculateRaiderAmount(1_000_000) + ", maxRaiders "
                        + MineColonies.getConfig().getServer().maxRaiders.get());
        }
        flow
          .run("start a raid of " + amount, () -> result[0] = fixture.startRaid(culture, amount, RAID_SPAWN))
          .check("raid started", () -> result[0] == IRaiderManager.RaidSpawnResult.SUCCESS, () -> "result " + result[0])
          .check("one raid event of type " + eventType,
            () -> {
                final List<IColonyEvent> events = new ArrayList<>(fixture.colony().getEventManager().getEvents().values());
                if (events.size() == 1 && events.get(0) instanceof HordeRaidEvent horde && horde.getEventTypeID().toString().equals(eventType))
                {
                    event[0] = horde;
                    return true;
                }
                return false;
            },
            () -> "events " + describeEvents(fixture.colony()))
          .awaitUntil(amount + " raiders spawned", 1500, () -> event[0].getEntities().size() == amount,
            () -> "raiders " + event[0].getEntities().size() + ", status " + event[0].getStatus())
          .check("raiders spawned near the spawn point",
            () -> event[0].getEntities().stream().allMatch(e -> horizontalDistance(e.blockPosition(), fixture.abs(RAID_SPAWN)) <= 16),
            () -> "raiders at " + event[0].getEntities().stream().map(e -> e.blockPosition().toShortString()).toList() + ", spawn " + fixture.abs(RAID_SPAWN))
          .run("remember distance", () -> startDistance[0] = closestRaiderDistance(event[0], fixture.colony().getCenter()))
          .awaitUntil("raiders march on the colony (one within 14 blocks of the town hall)", 6000,
            () -> closestRaiderDistance(event[0], fixture.colony().getCenter()) <= 14,
            () -> "closest raider " + String.format("%.1f", closestRaiderDistance(event[0], fixture.colony().getCenter())) + " (start "
                    + String.format("%.1f", startDistance[0]) + "), status " + event[0].getStatus() + ", raiders " + event[0].getEntities().size())
          .awaitUntil("all raiders killed, raid event ends and is removed", 1500, () -> {
                killRaiders(fixture, event[0]);
                return fixture.colony().getEventManager().getEvents().isEmpty();
            },
            () -> "events " + describeEvents(fixture.colony()) + ", raiders " + describeRaiders(event[0]))
          .check("no raid active and no camp fire left",
            () -> !fixture.colony().getRaiderManager().isRaided() && countBlocks(fixture, Blocks.CAMPFIRE) == 0,
            () -> "raided " + fixture.colony().getRaiderManager().isRaided() + ", camp fires " + countBlocks(fixture, Blocks.CAMPFIRE))
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-07 Raid without losses
    // ====================================================================================================================================

    /**
     * DEF-07 run 1: a raid that ends without a citizen death gives every citizen the "raid without death" happiness modifier.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def07_raidWithoutDeathMakesHappy(final GameTestHelper helper)
    {
        raidHappiness(helper, false);
    }

    /**
     * DEF-07 run 2: a citizen is killed during the raid: no "raid without death" modifier, the death is in the colony statistics.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void def07_raidWithDeathNoBonus(final GameTestHelper helper)
    {
        raidHappiness(helper, true);
    }

    private static void raidHappiness(final GameTestHelper helper, final boolean killACitizen)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizens = new ICitizenData[2];
        final HordeRaidEvent[] event = new HordeRaidEvent[1];
        final int[] deathsBefore = new int[1];

        TestFlow flow = fixture.flow()
          .run("found colony with two citizens, start a barbarian raid", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              citizens[0] = fixture.spawnCitizen(10, null, null);
              citizens[1] = fixture.spawnCitizen(10, null, null);
              deathsBefore[0] = fixture.colony().getStatisticsManager().getStatTotal(StatisticsConstants.DEATH);
              final IRaiderManager.RaidSpawnResult result = fixture.startRaid("barbarian_raid", 3, RAID_SPAWN);
              if (result != IRaiderManager.RaidSpawnResult.SUCCESS)
              {
                  throw new IllegalStateException("raid not started: " + result);
              }
              event[0] = (HordeRaidEvent) fixture.colony().getEventManager().getEvents().values().iterator().next();
          })
          .awaitUntil("raiders spawned and raid active", 1500, () -> event[0].getEntities().size() == 3 && event[0].isRaidActive(),
            () -> "raiders " + event[0].getEntities().size() + ", status " + event[0].getStatus());
        if (killACitizen)
        {
            flow = flow
              .awaitUntil("citizen entity present", 100, () -> citizens[0].getEntity().isPresent(), () -> ColonyFixture.describe(citizens[0]))
              .awaitUntil("a raider hits a citizen until he dies (citizens cap the damage per hit)", 1200, () -> {
                  citizens[0].getEntity().ifPresent(victim -> {
                      final LivingEntity raider = (LivingEntity) event[0].getEntities().get(0);
                      victim.setInvulnerableTime(0);
                      victim.hurtServer(fixture.level(), fixture.level().damageSources().mobAttack(raider), 10_000f);
                  });
                  return fixture.colony().getCitizenManager().getCivilian(citizens[0].getId()) == null;
              }, () -> ColonyFixture.describe(citizens[0]) + ", health " + citizens[0].getEntity().map(LivingEntity::getHealth).orElse(-1f));
        }
        flow
          .awaitUntil("all raiders killed, raid ends", 1500, () -> {
                killRaiders(fixture, event[0]);
                return fixture.colony().getEventManager().getEvents().isEmpty();
            },
            () -> "events " + describeEvents(fixture.colony()) + ", raiders " + describeRaiders(event[0]))
          .check(killACitizen ? "no 'raid without death' modifier, the death is counted" : "every citizen has the 'raid without death' modifier",
            () -> {
                if (killACitizen)
                {
                    return citizens[1].getCitizenHappinessHandler().getModifier(HappinessConstants.RAIDWITHOUTDEATH) == null
                             && fixture.colony().getStatisticsManager().getStatTotal(StatisticsConstants.DEATH) > deathsBefore[0]
                             && fixture.colony().getRaiderManager().getLostCitizen() > 0;
                }
                return citizens[0].getCitizenHappinessHandler().getModifier(HappinessConstants.RAIDWITHOUTDEATH) != null
                         && citizens[1].getCitizenHappinessHandler().getModifier(HappinessConstants.RAIDWITHOUTDEATH) != null;
            },
            () -> "modifiers " + citizens[1].getCitizenHappinessHandler().getModifiers() + ", deaths " + fixture.colony().getStatisticsManager().getStatTotal(StatisticsConstants.DEATH)
                    + " (before " + deathsBefore[0] + "), lost citizens " + fixture.colony().getRaiderManager().getLostCitizen())
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-08 Citizens flee
    // ====================================================================================================================================

    /**
     * DEF-08 Citizens flee from danger: a husk attacks a working lumberjack (two unemployed citizens stand by). The attacked citizen runs
     * away (citizen AI state FLEE) and goes back to work once the husk is gone. Citizens flee when hurt, not on sight.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 5000, skyAccess = true)
    public void def08_citizensFleeFromDanger(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] citizens = new ICitizenData[3];
        final Mob[] mob = new Mob[1];
        final double[] fleeDistance = new double[2];
        final Set<String> states = new HashSet<>();

        fixture.flow()
          .run("found colony, a lumberjack and two unemployed citizens", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 52));
              citizens[0] = fixture.spawnCitizen(20, hut[0], null);
              citizens[1] = fixture.spawnCitizen(10, null, null);
              citizens[2] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("lumberjack is working", 2000, () -> citizenState(citizens[0]) == CitizenAIState.WORK || citizenState(citizens[0]) == CitizenAIState.WORKING,
            () -> ColonyFixture.describe(citizens[0]))
          .run("spawn a husk next to the lumberjack, targeting him", () -> {
              final LivingEntity victim = citizens[0].getEntity().get();
              mob[0] = spawnMob(fixture, EntityTypes.HUSK, fixture.rel(victim.blockPosition()).offset(2, 0, 0));
              mob[0].setTarget(victim);
          })
          .awaitUntil("attacked citizen flees", 1200,
            () -> {
                states.add(String.valueOf(citizenState(citizens[0])));
                return citizenState(citizens[0]) == CitizenAIState.FLEE;
            },
            () -> ColonyFixture.describe(citizens[0]) + ", husk at " + mob[0].blockPosition().toShortString() + ", states " + states)
          .run("remember distance", () -> fleeDistance[0] = distance(citizens[0], mob[0].blockPosition()))
          .awaitUntil("citizen runs away from the husk (at least 8 blocks)", 600,
            () -> distance(citizens[0], mob[0].blockPosition()) >= 8 || !mob[0].isAlive(),
            () -> ColonyFixture.describe(citizens[0]) + ", distance " + String.format("%.1f", distance(citizens[0], mob[0].blockPosition()))
                    + " (at start of flight " + String.format("%.1f", fleeDistance[0]) + ")")
          .run("the husk disappears", () -> mob[0].discard())
          .awaitUntil("citizen goes back to work", 2500, () -> citizenState(citizens[0]) == CitizenAIState.WORK || citizenState(citizens[0]) == CitizenAIState.WORKING,
            () -> ColonyFixture.describe(citizens[0]))
          .check("all citizens survived", () -> fixture.colony().getCitizenManager().getCitizens().size() == 3,
            () -> "citizens " + fixture.colony().getCitizenManager().getCitizens().size())
          .succeed();
    }

    // ====================================================================================================================================
    // DEF-09 Training
    // ====================================================================================================================================

    /**
     * DEF-09 Training raises guard skills: a knight in training at the combat academy (sword + shield) and a ranger in training at the
     * archery (bow + arrows) train on their targets and gain experience.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void def09_trainingRaisesSkills(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] trainees = new ICitizenData[2];
        final double[] xpBefore = new double[2];
        final Set<String> knightStates = new HashSet<>();
        final Set<String> archerStates = new HashSet<>();

        fixture.flow()
          .run("found colony, combat academy with a knight trainee, archery with a ranger trainee", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.combatAcademy.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              huts[1] = fixture.placeBuilding(ModBuildings.archery.get(), 1, new BlockPos(52, ColonyFixture.GROUND_Y, 52));
              trainees[0] = fixture.spawnCitizen(10, huts[0], null);
              trainees[1] = fixture.spawnCitizen(10, huts[1], null);
              InventoryUtils.addItemStackToItemHandler(trainees[0].getInventory(), new ItemStack(Items.WOODEN_SWORD));
              InventoryUtils.addItemStackToItemHandler(trainees[0].getInventory(), new ItemStack(Items.SHIELD));
              InventoryUtils.addItemStackToItemHandler(trainees[1].getInventory(), new ItemStack(Items.BOW));
              InventoryUtils.addItemStackToItemHandler(trainees[1].getInventory(), new ItemStack(Items.ARROW, 64));
              xpBefore[0] = trainees[0].getCitizenSkillHandler().getTotalXP();
              xpBefore[1] = trainees[1].getCitizenSkillHandler().getTotalXP();
          })
          .awaitUntil("both trainees gained experience by training", 10000,
            () -> {
                knightStates.add(workerState(trainees[0]));
                archerStates.add(workerState(trainees[1]));
                return trainees[0].getCitizenSkillHandler().getTotalXP() > xpBefore[0] + 1 && trainees[1].getCitizenSkillHandler().getTotalXP() > xpBefore[1] + 1;
            },
            () -> "knight trainee XP " + trainees[0].getCitizenSkillHandler().getTotalXP() + " (" + ColonyFixture.describe(trainees[0]) + ", states " + knightStates
                    + "), archer trainee XP " + trainees[1].getCitizenSkillHandler().getTotalXP() + " (" + ColonyFixture.describe(trainees[1]) + ", states " + archerStates + ")")
          .check("the archer shot at targets", () -> archerStates.contains(AIWorkerState.ARCHER_SHOOT.toString()), () -> "states " + archerStates)
          .succeed();
    }

    // DEF-10 Stable and cavalry: not implemented (see docs/e2e-tests/protocol/defense-visitors.md).

    // ====================================================================================================================================
    // DEF-11 Raid timing
    // ====================================================================================================================================

    /**
     * DEF-11 Raid timing follows the config: raids on, minimum 2 nights between raids, average 3. Over ten nights raids start only at
     * nightfall and never with fewer than 2 raid-free nights between them; with raids off, five more nights bring no raid. Each raid is
     * ended by the test at once (status DONE) so the next night starts clean.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 30000, skyAccess = true)
    public void def11_raidTimingFollowsConfig(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final List<Integer> raidNights = new ArrayList<>();
        final List<String> notes = new ArrayList<>();
        final int[] night = new int[1];
        final Set<Integer> seenEvents = new HashSet<>();

        TestFlow flow = fixture.flow()
          .run("found a colony big enough to be raided, raids on (min 2, average 3 nights between raids)", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.setConfig(MineColonies.getConfig().getServer().enableColonyRaids, true);
              fixture.setConfig(MineColonies.getConfig().getServer().minimumNumberOfNightsBetweenRaids, 2);
              fixture.setConfig(MineColonies.getConfig().getServer().averageNumberOfNightsBetweenRaids, 3);
              // Natural raids spawn 40+ blocks from the buildings, outside the arena: give them ground there (removed when the test ends).
              layGroundAroundArena(fixture, 40);
              // The raid's spawn search walks outwards in 48-block steps while the chunks there are loaded, so how far out it ends depends
              // on which chunks other tests left loaded; past the ground ring it finds no floor and the raid fails with NO_SPAWN_POINT
              // (once in a full run: "next=true" on nine nights, no raid). Sky raiders may spawn in the air, so a spawn point is always found.
              // The test checks the timing of raids, not where they spawn.
              fixture.setConfig(MineColonies.getConfig().getServer().skyRaiders, true);
              for (int i = 0; i < 8; i++)
              {
                  fixture.spawnCitizen(80, null, null);
              }
              // The owner "logs in": the colony's important players are filled on login, canRaid() needs at least one.
              fixture.colony().getPackageManager().addImportantColonyPlayer(fixture.owner());
              // WorldUtil.isPeaceful() treats the game rule "spawn mobs = false" (test server default) as peaceful: no raids at all.
              fixture.setGameRule(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS, true);
          })
          .check("raid level is at least 75", () -> ((com.minecolonies.core.colony.events.raid.RaidManager) fixture.colony().getRaiderManager()).getColonyRaidLevel() >= 75,
            () -> "raid level " + ((com.minecolonies.core.colony.events.raid.RaidManager) fixture.colony().getRaiderManager()).getColonyRaidLevel())
          .check("colony can be raided", () -> fixture.colony().getRaiderManager().canRaid(), () -> "canRaid false");

        for (int n = 1; n <= 15; n++)
        {
            final int nightNo = n;
            final boolean raidsOn = n <= 10;
            if (n == 11)
            {
                flow = flow.run("turn raids off", () -> E2EBootstrapSet.set(MineColonies.getConfig().getServer().enableColonyRaids, false));
            }
            flow = flow
              .run("morning " + nightNo, () -> fixture.setTime(1000))
              .awaitUntil("colony sees the day " + nightNo, 400, () -> fixture.colony().isDay(), () -> "isDay false")
              .check("no raid starts by day " + nightNo, () -> newRaidEvents(fixture, seenEvents).isEmpty(), () -> "new raid at day: " + describeEvents(fixture.colony()))
              .run("nightfall " + nightNo, () -> {
                  night[0] = nightNo;
                  fixture.setTime(13500);
              })
              .awaitUntil("colony sees the night " + nightNo, 400, () -> !fixture.colony().isDay(), () -> "isDay true")
              .run("record raids of night " + nightNo, () -> {
                  final List<IColonyEvent> started = newRaidEvents(fixture, seenEvents);
                  final IRaiderManager raids = fixture.colony().getRaiderManager();
                  notes.add("n" + nightNo + ":raids=" + started.size() + ",since=" + raids.getNightsSinceLastRaid() + ",next=" + raids.willRaidTonight()
                              + ",can=" + raids.canRaid());
                  if (!started.isEmpty())
                  {
                      raidNights.add(nightNo);
                  }
                  for (final IColonyEvent event : started)
                  {
                      seenEvents.add(event.getID());
                      event.setStatus(EventStatus.DONE);
                  }
              })
              .awaitUntil("raid events of night " + nightNo + " are gone", 1200, () -> fixture.colony().getEventManager().getEvents().isEmpty(),
                () -> "events " + describeEvents(fixture.colony()));
            if (n == 10)
            {
                flow = flow
                  .check("with raids on: at least one raid in ten nights", () -> !raidNights.isEmpty(), () -> "no raid; " + notes)
                  .check("at least 2 raid-free nights between two raids", () -> {
                      for (int i = 1; i < raidNights.size(); i++)
                      {
                          if (raidNights.get(i) - raidNights.get(i - 1) - 1 < 2)
                          {
                              return false;
                          }
                      }
                      return true;
                  }, () -> "raid nights " + raidNights + " " + notes);
            }
        }
        flow
          .check("with raids off: no raid in nights 11-15", () -> raidNights.stream().noneMatch(nightNo -> nightNo > 10), () -> "raid nights " + raidNights + " " + notes)
          .succeed();
    }

    /**
     * Small adapter so the config can be turned off mid-test (the fixture's {@code setConfig} restores the old value at the end).
     */
    private static final class E2EBootstrapSet
    {
        static <T> void set(final com.ldtteam.shim.neoforge.common.ModConfigSpec.ConfigValue<T> value, final T newValue)
        {
            com.minecolonies.gametest.framework.E2EBootstrap.set(value, newValue);
        }
    }

    private static List<IColonyEvent> newRaidEvents(final ColonyFixture fixture, final Set<Integer> seen)
    {
        final List<IColonyEvent> result = new ArrayList<>();
        for (final IColonyEvent event : fixture.colony().getEventManager().getEvents().values())
        {
            if (event instanceof IColonyRaidEvent && !seen.contains(event.getID()))
            {
                result.add(event);
            }
        }
        return result;
    }

    // ====================================================================================================================================
    // VIS-01 .. VIS-04
    // ====================================================================================================================================

    /**
     * VIS-01 Visitors come to the tavern: the first visitor arrives on its own (the tavern waits 10000 ticks after it is built); with the
     * waiting time skipped, visitors keep coming up to the tavern's capacity (3 per tavern level) and never beyond. Visitors are not citizens
     * and have no job.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 20000, skyAccess = true)
    public void vis01_visitorsComeToTavern(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] tavern = new IBuilding[1];
        final int[] maxSeen = new int[1];
        final int[] watched = new int[1];

        fixture.flow()
          .run("found colony with a level 1 tavern", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              tavern[0] = fixture.placeBuilding(ModBuildings.tavern.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              fixture.spawnCitizen(10, null, tavern[0]);
          })
          .awaitUntil("the first visitor arrives", 13000, () -> !visitors(fixture.colony()).isEmpty(), () -> "visitors " + visitors(fixture.colony()).size())
          .check("the visitor is an entity at the tavern, not a citizen, without a job",
            () -> {
                final IVisitorData visitor = visitors(fixture.colony()).get(0);
                return visitor.getEntity().map(e -> horizontalDistance(e.blockPosition(), tavern[0].getPosition()) <= 24).orElse(false)
                         && visitor.getJob() == null
                         && fixture.colony().getCitizenManager().getCivilian(visitor.getId()) == null
                         && fixture.colony().getCitizenManager().getCitizens().size() == 1;
            },
            () -> {
                final IVisitorData visitor = visitors(fixture.colony()).get(0);
                return "visitor " + visitor.getName() + " at " + visitor.getEntity().map(e -> e.blockPosition().toShortString()).orElse("(no entity)") + ", job "
                         + visitor.getJob() + ", citizens " + fixture.colony().getCitizenManager().getCitizens().size();
            })
          .awaitUntil("with the wait skipped, the tavern fills up to 3 visitors and stays there for 3 more colony ticks", 6000,
            () -> {
                skipVisitorWait(tavern[0]);
                final int count = visitors(fixture.colony()).size();
                maxSeen[0] = Math.max(maxSeen[0], count);
                if (count >= 3)
                {
                    watched[0]++;
                }
                return watched[0] >= 1600;
            },
            () -> "visitors " + visitors(fixture.colony()).size() + ", max seen " + maxSeen[0] + ", ticks at capacity " + watched[0])
          .check("never more visitors than the capacity (3 x level 1)", () -> maxSeen[0] == 3, () -> "max visitors " + maxSeen[0])
          .succeed();
    }

    /**
     * VIS-02 Recruiting a visitor: without the recruitment items nothing happens; with them, the items are taken and the visitor becomes a
     * citizen with the same name and skills. (The mod lets 3% of recruits run off with the payment; the test is retried then.)
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 1200, skyAccess = true, maxAttempts = 4)
    public void vis02_recruitVisitor(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] tavern = new IBuilding[1];
        final IVisitorData[] visitor = new IVisitorData[1];
        final ItemStack[] cost = new ItemStack[1];
        final String[] name = new String[1];
        final int[] skill = new int[1];

        fixture.flow()
          .run("found colony with a tavern (free beds), the tavern spawns a visitor", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              tavern[0] = fixture.placeBuilding(ModBuildings.tavern.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              fixture.owner().setGameMode(GameType.SURVIVAL);
              fixture.owner().getInventory().clearContent();
              visitor[0] = tavern[0].getFirstModuleOccurance(TavernBuildingModule.class).spawnVisitor();
              visitor[0].triggerInteraction(new RecruitmentInteraction(Component.translatable("com.minecolonies.coremod.gui.chat.recruitstory1", visitor[0].getName().split(" ")[0]),
                ChatPriority.IMPORTANT));
              cost[0] = visitor[0].getRecruitCost().copy();
              name[0] = visitor[0].getName();
              skill[0] = visitor[0].getCitizenSkillHandler().getLevel(Skill.Strength);
          })
          .check("free housing for one more citizen",
            () -> fixture.colony().getCitizenManager().getCurrentCitizenCount() < fixture.colony().getCitizenManager().getPotentialMaxCitizens(),
            () -> "citizens " + fixture.colony().getCitizenManager().getCurrentCitizenCount() + "/" + fixture.colony().getCitizenManager().getPotentialMaxCitizens())
          .run("owner answers 'recruit' without the items", () -> respondRecruit(fixture, visitor[0]))
          .check("refused: visitor still a visitor, no new citizen",
            () -> fixture.colony().getVisitorManager().getVisitor(visitor[0].getId()) != null && fixture.colony().getCitizenManager().getCitizens().isEmpty(),
            () -> "visitor present " + (fixture.colony().getVisitorManager().getVisitor(visitor[0].getId()) != null) + ", citizens "
                    + fixture.colony().getCitizenManager().getCitizens().size())
          .run("owner gets the recruitment items and answers 'recruit'", () -> {
              fixture.owner().getInventory().add(cost[0].copy());
              respondRecruit(fixture, visitor[0]);
          })
          .check("the items were taken", () -> countInOwnerInventory(fixture, cost[0]) == 0, () -> "owner still has " + countInOwnerInventory(fixture, cost[0]) + " of " + cost[0])
          .check("the visitor is gone from the visitor list", () -> fixture.colony().getVisitorManager().getVisitor(visitor[0].getId()) == null, () -> "still a visitor")
          .check("the visitor is now a citizen with the same name and skills (fails if this one ran off: 3% chance, retried)",
            () -> fixture.colony().getCitizenManager().getCitizens().stream()
                    .anyMatch(c -> c.getName().equals(name[0]) && c.getCitizenSkillHandler().getLevel(Skill.Strength) == skill[0]),
            () -> "citizens " + fixture.colony().getCitizenManager().getCitizens().stream().map(c -> c.getName()).toList() + ", expected " + name[0])
          .succeed();
    }

    /**
     * VIS-03 A quest from start to reward. Part 1: the "welcome" tutorial quest (trigger: any citizen) is offered, accepted through the
     * dialogue, its place-block objective is completed by the owner placing a builder's hut block, and its rewards are given (quest log item,
     * the "builder" quest unlocked). Part 2: the "hungry courier" quest (its random trigger is skipped: injected with the citizen as quest
     * giver) is accepted, the baked potato delivered, and the reward gives gold and the quest happiness modifier to the quest giver.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void vis03_questFromStartToReward(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizen = new ICitizenData[1];
        final Identifier welcome = Identifier.parse("minecolonies:tutorial/welcome");
        final Identifier builderQuest = Identifier.parse("minecolonies:tutorial/builder");
        final Identifier hungry = Identifier.parse("minecolonies:general/hungrycourier");

        fixture.flow()
          .run("found colony with one citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.owner().setGameMode(GameType.SURVIVAL);
              fixture.owner().getInventory().clearContent();
              citizen[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("the welcome quest is available, given by the citizen", 1200,
            () -> available(fixture, welcome) != null,
            () -> "available " + fixture.colony().getQuestManager().getAvailableQuests().stream().map(IQuestInstance::getId).toList())
          .check("quest giver is the citizen", () -> available(fixture, welcome).getQuestGiverId() == citizen[0].getId(),
            () -> "giver " + available(fixture, welcome).getQuestGiverId())
          .run("owner: 'Yes please!' then 'Alright! Let me get to it!'", () -> answer(fixture, citizen[0], welcome, 0, 0))
          .check("quest accepted: in progress, objective 1", () -> inProgress(fixture, welcome) != null && inProgress(fixture, welcome).getObjectiveIndex() == 1,
            () -> questState(fixture, welcome))
          .run("objective 1: 'Yes, all done' then 'Okay, let me do that!'", () -> answer(fixture, citizen[0], welcome, 1, 0))
          .check("objective 2", () -> inProgress(fixture, welcome).getObjectiveIndex() == 2, () -> questState(fixture, welcome))
          .run("objective 2: 'Yes; mission accomplished!' then 'Alright!'", () -> answer(fixture, citizen[0], welcome, 0, 0))
          .check("objective 3", () -> inProgress(fixture, welcome).getObjectiveIndex() == 3, () -> questState(fixture, welcome))
          .run("objective 3: 'Yes, all done' then 'Alrighty then! Let me check this out!'", () -> answer(fixture, citizen[0], welcome, 1, 0))
          .check("objective 4: place a builder's hut", () -> inProgress(fixture, welcome).getObjectiveIndex() == 4, () -> questState(fixture, welcome))
          .run("owner places a builder's hut block", () -> placeBlockAsOwner(fixture, new ItemStack(ModBlocks.blockHutBuilder), new BlockPos(46, ColonyFixture.FLOOR_Y, 46)))
          .awaitUntil("objective 4 done: quest at objective 5", 40, () -> inProgress(fixture, welcome) != null && inProgress(fixture, welcome).getObjectiveIndex() == 5,
            () -> questState(fixture, welcome))
          .run("objective 5: 'Perfect! Thanks!'", () -> answer(fixture, citizen[0], welcome, 0))
          .check("quest finished, rewards given: quest log item, builder quest unlocked",
            () -> inProgress(fixture, welcome) == null && finishedCount(fixture, welcome) == 1
                    && countInOwnerInventory(fixture, new ItemStack(ModItems.questLog)) == 1
                    && fixture.colony().getQuestManager().isUnlocked(builderQuest),
            () -> questState(fixture, welcome) + ", quest logs " + countInOwnerInventory(fixture, new ItemStack(ModItems.questLog)) + ", builder unlocked "
                    + fixture.colony().getQuestManager().isUnlocked(builderQuest))
          .run("part 2: inject the hungry courier quest with the citizen as giver", () -> injectQuest(fixture, hungry, citizen[0]))
          .run("owner accepts: 'Sure' -> 'What kind of food?' -> 'Got it'", () -> answer(fixture, citizen[0], hungry, 0, 0, 0))
          .check("hungry courier in progress at the delivery", () -> inProgress(fixture, hungry) != null && inProgress(fixture, hungry).getObjectiveIndex() == 1,
            () -> questState(fixture, hungry))
          .run("owner gets a baked potato and hands it over", () -> {
              fixture.owner().getInventory().add(new ItemStack(Items.BAKED_POTATO));
              answer(fixture, citizen[0], hungry, 0);
          })
          .check("potato delivered: quest at the final dialogue", () -> inProgress(fixture, hungry) != null && inProgress(fixture, hungry).getObjectiveIndex() == 2
                                                                         && countInOwnerInventory(fixture, new ItemStack(Items.BAKED_POTATO)) == 0,
            () -> questState(fixture, hungry) + ", potatoes " + countInOwnerInventory(fixture, new ItemStack(Items.BAKED_POTATO)))
          .run("final dialogue: 'You're welcome!'", () -> answer(fixture, citizen[0], hungry, 0))
          .check("hungry courier finished: gold given, quest happiness modifier on the giver",
            () -> inProgress(fixture, hungry) == null && finishedCount(fixture, hungry) == 1
                    && countInOwnerInventory(fixture, new ItemStack(Items.GOLD_INGOT)) == 1
                    && citizen[0].getCitizenHappinessHandler().getModifier(HappinessConstants.QUEST) != null,
            () -> questState(fixture, hungry) + ", gold " + countInOwnerInventory(fixture, new ItemStack(Items.GOLD_INGOT)) + ", modifiers "
                    + citizen[0].getCitizenHappinessHandler().getModifiers())
          .succeed();
    }

    /**
     * VIS-04 Declining and cancelling quests (dialogue): the owner declines the welcome quest ("I'm a Colony Master!", go-to -1: the quest
     * file ends it at once), then cancels the hungry courier quest in its first dialogue ("Sorry, I'm in a rush right now!" = cancel).
     * Neither gives a reward; the declined quest counts as finished (its follow-up is not unlocked), the cancelled one is removed and not
     * finished, and nothing is stuck as active.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2000, skyAccess = true)
    public void vis04_declineAndCancelQuests(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizen = new ICitizenData[1];
        final Identifier welcome = Identifier.parse("minecolonies:tutorial/welcome");
        final Identifier hungry = Identifier.parse("minecolonies:general/hungrycourier");

        fixture.flow()
          .run("found colony with one citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.owner().setGameMode(GameType.SURVIVAL);
              fixture.owner().getInventory().clearContent();
              citizen[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("the welcome quest is available", 1200, () -> available(fixture, welcome) != null,
            () -> "available " + fixture.colony().getQuestManager().getAvailableQuests().stream().map(IQuestInstance::getId).toList())
          .run("owner declines: 'I'm a Colony Master! You underestimate my power!'", () -> answer(fixture, citizen[0], welcome, 2))
          .check("declined welcome quest: not available, not active, counted finished, no reward, follow-up not unlocked",
            () -> available(fixture, welcome) == null && inProgress(fixture, welcome) == null && finishedCount(fixture, welcome) == 1
                    && fixture.owner().getInventory().isEmpty()
                    && !fixture.colony().getQuestManager().isUnlocked(Identifier.parse("minecolonies:tutorial/builder")),
            () -> questState(fixture, welcome) + ", owner inventory empty " + fixture.owner().getInventory().isEmpty())
          .run("inject the hungry courier quest; owner answers 'Sorry, I'm in a rush right now!' (cancel)", () -> {
              injectQuest(fixture, hungry, citizen[0]);
              answer(fixture, citizen[0], hungry, 1);
          })
          .check("cancelled: not active, not available, not finished, no reward, no happiness modifier",
            () -> inProgress(fixture, hungry) == null && available(fixture, hungry) == null && finishedCount(fixture, hungry) == 0
                    && fixture.owner().getInventory().isEmpty()
                    && citizen[0].getCitizenHappinessHandler().getModifier(HappinessConstants.QUEST) == null,
            () -> questState(fixture, hungry) + ", owner inventory empty " + fixture.owner().getInventory().isEmpty() + ", modifiers "
                    + citizen[0].getCitizenHappinessHandler().getModifiers())
          .idle(600)
          .check("after more than one colony tick (500 ticks) nothing is active, and the cancelled quest was not finished",
            () -> fixture.colony().getQuestManager().getInProgressQuests().isEmpty() && finishedCount(fixture, hungry) == 0, () -> questState(fixture, hungry))
          .succeed();
    }

    /**
     * VIS-04 Cancelling an accepted quest: the owner accepts the hungry courier quest and, at the delivery step (no potato), picks the
     * dialogue's cancel answer. Expected: the quest is removed (not active, not finished), no reward.
     */
    // BUG E2E-VIS04-1: QuestDeliveryInteraction.onServerResponseTriggered (core/colony/interactionhandling/QuestDeliveryInteraction.java:65-72)
    // never applies a non-positive final answer: the "cancel" answer of a delivery objective only closes the dialogue, the quest stays in progress.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true, required = false)
    public void vis04_cancelAcceptedQuestAtDelivery(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizen = new ICitizenData[1];
        final Identifier hungry = Identifier.parse("minecolonies:general/hungrycourier");

        fixture.flow()
          .run("found colony with one citizen, inject the hungry courier quest, owner accepts it", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.owner().setGameMode(GameType.SURVIVAL);
              fixture.owner().getInventory().clearContent();
              citizen[0] = fixture.spawnCitizen(10, null, null);
              injectQuest(fixture, hungry, citizen[0]);
              answer(fixture, citizen[0], hungry, 0, 0, 0);
          })
          .check("hungry courier accepted, at the delivery", () -> inProgress(fixture, hungry) != null && inProgress(fixture, hungry).getObjectiveIndex() == 1,
            () -> questState(fixture, hungry))
          .run("owner picks the delivery dialogue's cancel answer (response 1 of the 'waiting' dialogue)", () -> answer(fixture, citizen[0], hungry, 1))
          .check("cancelled: not active, not available, not finished, no reward",
            () -> inProgress(fixture, hungry) == null && available(fixture, hungry) == null && finishedCount(fixture, hungry) == 0
                    && fixture.owner().getInventory().isEmpty(),
            () -> questState(fixture, hungry) + ", owner inventory empty " + fixture.owner().getInventory().isEmpty())
          .succeed();
    }

    /**
     * VIS-02 follow-up: a tavern that is removed right after one of its visitors was recruited removes its remaining visitors cleanly.
     */
    // BUG E2E-VIS02-1: TavernBuildingModule keeps the recruited visitor's id in externalCitizens until its next colony tick; onDestroyed()
    // (core/colony/buildings/modules/TavernBuildingModule.java:309-313) then passes null to VisitorManager.removeCivilian
    // (core/colony/managers/VisitorManager.java:301) -> NullPointerException, and the other visitors are not removed.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 600, skyAccess = true, required = false)
    public void vis02_tavernRemovedAfterRecruiting(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] tavern = new IBuilding[1];
        final IVisitorData[] visitors = new IVisitorData[2];
        final String[] error = new String[1];

        fixture.flow()
          .run("tavern with two visitors, the first is recruited (owner in creative: no cost)", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              tavern[0] = fixture.placeBuilding(ModBuildings.tavern.get(), 1, new BlockPos(12, ColonyFixture.GROUND_Y, 12));
              fixture.owner().setGameMode(GameType.CREATIVE); // the fake owner is shared between tests
              for (int i = 0; i < 2; i++)
              {
                  visitors[i] = tavern[0].getFirstModuleOccurance(TavernBuildingModule.class).spawnVisitor();
                  visitors[i].triggerInteraction(new RecruitmentInteraction(Component.translatable("com.minecolonies.coremod.gui.chat.recruitstory1", "x"),
                    ChatPriority.IMPORTANT));
              }
              respondRecruit(fixture, visitors[0]);
          })
          .run("the tavern is removed at once (before its next colony tick)", () -> {
              try
              {
                  tavern[0].getFirstModuleOccurance(TavernBuildingModule.class).onDestroyed();
              }
              catch (final RuntimeException e)
              {
                  error[0] = e.toString();
              }
          })
          .check("no error, and the remaining visitor is gone",
            () -> error[0] == null && fixture.colony().getVisitorManager().getVisitor(visitors[1].getId()) == null,
            () -> "error " + error[0] + ", second visitor still registered " + (fixture.colony().getVisitorManager().getVisitor(visitors[1].getId()) != null))
          .succeed();
    }

    // ====================================================================================================================================
    // Helpers
    // ====================================================================================================================================

    private static Map<Skill, Integer> allSkills(final int level)
    {
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, level);
        }
        return skills;
    }

    /**
     * @return the worker AI state of the citizen's job as text, or "none".
     */
    private static String workerState(final ICitizenData citizen)
    {
        if (citizen == null || citizen.getJob() == null || citizen.getJob().getWorkerAI() == null)
        {
            return "none";
        }
        return String.valueOf(citizen.getJob().getWorkerAI().getState());
    }

    @Nullable
    private static IState citizenState(final ICitizenData citizen)
    {
        return citizen.getEntity()
          .map(e -> e instanceof com.minecolonies.core.entity.citizen.EntityCitizen c ? (IState) c.getCitizenAI().getState() : null)
          .orElse(null);
    }

    private static double distance(final ICitizenData citizen, final BlockPos target)
    {
        return citizen.getEntity().map(e -> Math.sqrt(e.blockPosition().distSqr(target))).orElse(Double.MAX_VALUE);
    }

    /**
     * Guard details for DEF-03 failure messages: inventory, where it is walking to and whether it sleeps.
     */
    private static String guardDetails(final ICitizenData guard)
    {
        final StringBuilder sb = new StringBuilder(ColonyFixture.describe(guard)).append(", inventory [");
        for (int i = 0; i < guard.getInventory().getSlots(); i++)
        {
            final ItemStack stack = guard.getInventory().getStackInSlot(i);
            if (!stack.isEmpty())
            {
                sb.append(stack.getCount()).append(' ').append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())).append(", ");
            }
        }
        sb.append("]");
        guard.getEntity().ifPresent(e -> {
            final net.minecraft.world.level.pathfinder.Path path = e.getNavigation().getPath();
            sb.append(", path target ").append(path == null ? "-" : path.getTarget().toShortString()).append(", sleeping ")
              .append(((com.minecolonies.core.colony.jobs.AbstractJobGuard<?>) guard.getJob()).isAsleep());
        });
        return sb.toString();
    }

    private static double horizontalDistance(final BlockPos a, final BlockPos b)
    {
        final double dx = a.getX() - b.getX();
        final double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Finds a free standing spot (solid below, two air blocks) near the relative position, searching upward first, then around.
     */
    private static BlockPos standable(final ColonyFixture fixture, final BlockPos rel)
    {
        for (int radius = 0; radius <= 4; radius++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
                    {
                        continue;
                    }
                    final int x = Math.max(1, Math.min(ColonyFixture.ARENA_SIZE - 2, rel.getX() + dx));
                    final int z = Math.max(1, Math.min(ColonyFixture.ARENA_SIZE - 2, rel.getZ() + dz));
                    for (int y = ColonyFixture.GROUND_Y; y < ColonyFixture.GROUND_Y + 12; y++)
                    {
                        final BlockPos abs = fixture.abs(new BlockPos(x, y, z));
                        if (fixture.level().getBlockState(abs.below()).isSolid() && fixture.level().getBlockState(abs).isAir()
                              && fixture.level().getBlockState(abs.above()).isAir())
                        {
                            return new BlockPos(x, y, z);
                        }
                    }
                }
            }
        }
        return rel;
    }

    private static Mob spawnMob(final ColonyFixture fixture, final EntityType<? extends Mob> type, final BlockPos rel)
    {
        final Mob mob = fixture.helper().spawn(type, standable(fixture, rel));
        mob.setPersistenceRequired();
        return mob;
    }

    private static void setGuardTask(final ColonyFixture fixture, final IBuilding building, final String task)
    {
        final GuardTaskSetting current = building.getSetting(AbstractBuildingGuards.GUARD_TASK);
        final List<String> options = current.getSettings();
        updateSetting(fixture, building, AbstractBuildingGuards.GUARD_TASK, new GuardTaskSetting(options, options.indexOf(task)));
    }

    /**
     * Changes a building setting the way the building window does (server side of the setting message): replaces the value in the settings
     * module that owns the key and calls the setting's update hook with the owner as sender.
     */
    private static <T extends ISetting<?>> void updateSetting(final ColonyFixture fixture, final IBuilding building, final ISettingKey<T> key, final T value)
    {
        for (final ISettingsModule module : building.getModulesByType(ISettingsModule.class))
        {
            if (module.getOptionalSetting(key).isPresent())
            {
                module.updateSetting(key, value, fixture.owner());
                return;
            }
        }
        throw new IllegalStateException("No settings module with " + key.getUniqueId() + " in " + building.getBuildingDisplayName());
    }

    /**
     * Runs the action when the test ends, passed or failed.
     */
    private static void onTestEnd(final ColonyFixture fixture, final Runnable action)
    {
        final GameTestInfo info = ((GameTestHelperAccessor) fixture.helper()).minecoloniesGametest$getTestInfo();
        info.addListener(new GameTestListener()
        {
            @Override
            public void testStructureLoaded(final GameTestInfo testInfo)
            {
            }

            @Override
            public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner)
            {
                action.run();
            }

            @Override
            public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner)
            {
                action.run();
            }

            @Override
            public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner)
            {
            }
        });
    }

    /**
     * Lays a grass floor (at the arena floor's height, only where there is air) in a ring of the given width around the arena, and removes it
     * again when the test ends.
     */
    private static void layGroundAroundArena(final ColonyFixture fixture, final int margin)
    {
        final List<BlockPos> placed = new ArrayList<>();
        final net.minecraft.world.level.block.state.BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        for (int x = -margin; x < ColonyFixture.ARENA_SIZE + margin; x++)
        {
            for (int z = -margin; z < ColonyFixture.ARENA_SIZE + margin; z++)
            {
                if (x >= -1 && x <= ColonyFixture.ARENA_SIZE && z >= -1 && z <= ColonyFixture.ARENA_SIZE)
                {
                    continue;
                }
                final BlockPos abs = fixture.abs(new BlockPos(x, ColonyFixture.FLOOR_Y, z));
                if (fixture.level().getBlockState(abs).isAir())
                {
                    fixture.level().setBlock(abs, grass, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    placed.add(abs);
                }
            }
        }
        onTestEnd(fixture, () -> {
            for (final BlockPos pos : placed)
            {
                fixture.level().setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        });
    }

    private static void moveOwner(final ColonyFixture fixture, final BlockPos rel)
    {
        final BlockPos abs = fixture.abs(standable(fixture, rel));
        fixture.owner().teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
    }

    private static String wornArmor(final LivingEntity entity)
    {
        return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD) + "/" + entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST) + "/"
                 + entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS) + "/" + entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
    }

    private static String inventoryText(final ICitizenData citizen)
    {
        final List<String> items = new ArrayList<>();
        for (int slot = 0; slot < citizen.getInventory().getSlots(); slot++)
        {
            final ItemStack stack = citizen.getInventory().getStackInSlot(slot);
            if (!stack.isEmpty())
            {
                items.add(stack.toString());
            }
        }
        return items.toString();
    }

    private static double closestRaiderDistance(final HordeRaidEvent event, final BlockPos center)
    {
        double best = Double.MAX_VALUE;
        for (final Entity entity : event.getEntities())
        {
            best = Math.min(best, horizontalDistance(entity.blockPosition(), center));
        }
        return best;
    }

    /**
     * Kills all raiders of the event with a lethal hit (a raider-sourced mob attack: damage without a living source is capped by the raiders'
     * environmental damage protection).
     */
    private static void killRaiders(final ColonyFixture fixture, final HordeRaidEvent event)
    {
        final LivingEntity source = damageDealer(fixture);
        for (final Entity entity : new ArrayList<>(event.getEntities()))
        {
            if (entity instanceof final AbstractEntityMinecoloniesRaider raider && raider.isAlive())
            {
                raider.setInvulnerableTime(0);
                raider.hurtServer(fixture.level(), fixture.level().damageSources().mobAttack(source), 100_000f);
            }
        }
    }

    /**
     * A living entity (armor stand in an arena corner) to use as the attacker of test-dealt damage: raiders cap damage that has no living
     * source, and the fake owner counts as no living source.
     */
    private static LivingEntity damageDealer(final ColonyFixture fixture)
    {
        final List<net.minecraft.world.entity.decoration.ArmorStand> stands =
          fixture.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, arenaBox(fixture), e -> e.isAlive());
        if (!stands.isEmpty())
        {
            return stands.get(0);
        }
        return fixture.helper().spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, ColonyFixture.GROUND_Y, 1));
    }

    private static String describeRaiders(final HordeRaidEvent event)
    {
        final List<String> text = new ArrayList<>();
        for (final Entity entity : event.getEntities())
        {
            text.add(entity.getType().toShortString() + (entity.isAlive() ? " alive " : " dead ") + (entity instanceof LivingEntity l ? l.getHealth() : 0)
                       + (entity.isRemoved() ? " removed" : "") + " at " + entity.blockPosition().toShortString());
        }
        return text.toString();
    }

    private static String describeEvents(final IColony colony)
    {
        final List<String> text = new ArrayList<>();
        for (final IColonyEvent event : colony.getEventManager().getEvents().values())
        {
            text.add(event.getID() + ":" + event.getEventTypeID() + "/" + event.getStatus());
        }
        return text.toString();
    }

    private static AABB arenaBox(final ColonyFixture fixture)
    {
        final BlockPos min = fixture.abs(BlockPos.ZERO);
        final BlockPos max = fixture.abs(new BlockPos(ColonyFixture.ARENA_SIZE, ColonyFixture.ARENA_HEIGHT, ColonyFixture.ARENA_SIZE));
        return new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max));
    }

    private static int countBlocks(final ColonyFixture fixture, final net.minecraft.world.level.block.Block block)
    {
        int count = 0;
        for (int x = 0; x < ColonyFixture.ARENA_SIZE; x++)
        {
            for (int z = 0; z < ColonyFixture.ARENA_SIZE; z++)
            {
                for (int y = 0; y < 12; y++)
                {
                    if (fixture.level().getBlockState(fixture.abs(new BlockPos(x, y, z))).is(block))
                    {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static List<IVisitorData> visitors(final IColony colony)
    {
        final List<IVisitorData> result = new ArrayList<>();
        for (final ICivilianData data : colony.getVisitorManager().getCivilianDataMap().values())
        {
            if (data instanceof final IVisitorData visitor)
            {
                result.add(visitor);
            }
        }
        return result;
    }

    /**
     * Skips the tavern's waiting time before the next visitor (private counter {@code noVisitorTime}, counted down by 500 each colony tick).
     */
    private static void skipVisitorWait(final IBuilding tavern)
    {
        try
        {
            final TavernBuildingModule module = tavern.getFirstModuleOccurance(TavernBuildingModule.class);
            final Field field = TavernBuildingModule.class.getDeclaredField("noVisitorTime");
            field.setAccessible(true);
            field.setInt(module, 0);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("Cannot reset the tavern's visitor timer", e);
        }
    }

    /**
     * The chat options (interactions) of a citizen or visitor, keyed by their id, as the GUI response message finds them.
     */
    @SuppressWarnings("unchecked")
    private static Map<Component, IInteractionResponseHandler> chatOptions(final ICivilianData data)
    {
        try
        {
            final Field field = com.minecolonies.core.colony.CitizenData.class.getDeclaredField("citizenChatOptions");
            field.setAccessible(true);
            return (Map<Component, IInteractionResponseHandler>) field.get(data);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException("Cannot read the citizen's chat options", e);
        }
    }

    /**
     * Server side of the GUI's "recruit" button: the InteractionResponse message calls {@code onResponseTriggered(key, 1, player)} on the
     * visitor (response 1 of the recruitment interaction = "recruit").
     */
    private static void respondRecruit(final ColonyFixture fixture, final IVisitorData visitor)
    {
        for (final Map.Entry<Component, IInteractionResponseHandler> entry : new ArrayList<>(chatOptions(visitor).entrySet()))
        {
            if (entry.getValue() instanceof RecruitmentInteraction)
            {
                visitor.onResponseTriggered(entry.getKey(), 1, fixture.owner());
                return;
            }
        }
        throw new IllegalStateException("Visitor has no recruitment interaction: " + chatOptions(visitor).keySet());
    }

    private static int countInOwnerInventory(final ColonyFixture fixture, final ItemStack like)
    {
        int count = 0;
        for (int slot = 0; slot < fixture.owner().getInventory().getContainerSize(); slot++)
        {
            final ItemStack stack = fixture.owner().getInventory().getItem(slot);
            if (stack.is(like.getItem()))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Nullable
    private static IQuestInstance available(final ColonyFixture fixture, final Identifier id)
    {
        return fixture.colony().getQuestManager().getAvailableQuests().stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    @Nullable
    private static IQuestInstance inProgress(final ColonyFixture fixture, final Identifier id)
    {
        return fixture.colony().getQuestManager().getInProgressQuests().stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    private static int finishedCount(final ColonyFixture fixture, final Identifier id)
    {
        return fixture.colony().getQuestManager().getFinishedQuests().stream()
          .filter(f -> f.template() == IQuestManager.GLOBAL_SERVER_QUESTS.get(id)).mapToInt(f -> f.finishedCount()).sum();
    }

    private static String questState(final ColonyFixture fixture, final Identifier id)
    {
        final IQuestInstance av = available(fixture, id);
        final IQuestInstance ip = inProgress(fixture, id);
        return id + ": available " + (av != null) + ", in progress " + (ip == null ? "no" : "objective " + ip.getObjectiveIndex()) + ", finished "
                 + finishedCount(fixture, id);
    }

    /**
     * Server side of answering a quest dialogue in the GUI: each answer is the InteractionResponse message with the quest id as key.
     */
    private static void answer(final ColonyFixture fixture, final ICitizenData citizen, final Identifier quest, final int... responses)
    {
        final Component key = Component.literal(quest.toString());
        for (final int response : responses)
        {
            if (!chatOptions(citizen).containsKey(key))
            {
                throw new IllegalStateException("Citizen has no dialogue for " + quest + "; options " + chatOptions(citizen).keySet());
            }
            citizen.onResponseTriggered(key, response, fixture.owner());
        }
    }

    /**
     * Makes a quest available with this citizen as quest giver, the way the quest manager does once a quest's triggers match (skips only
     * the trigger evaluation, e.g. a random trigger with a rarity of millions).
     */
    private static void injectQuest(final ColonyFixture fixture, final Identifier quest, final ICitizenData giver)
    {
        if (!IQuestManager.GLOBAL_SERVER_QUESTS.containsKey(quest))
        {
            throw new IllegalStateException("Unknown quest " + quest);
        }
        final QuestInstance instance = new QuestInstance(quest, fixture.colony(), List.of(new CitizenTriggerReturnData(giver)));
        fixture.colony().getQuestManager().injectAvailableQuest(instance);
    }

    /**
     * The owner right-clicks the top of the block at {@code floorRel} with the item (BlockItem placement, which fires the place event).
     */
    private static void placeBlockAsOwner(final ColonyFixture fixture, final ItemStack item, final BlockPos floorRel)
    {
        final BlockPos floor = fixture.abs(floorRel);
        fixture.owner().setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        fixture.owner().getMainHandItem().useOn(new UseOnContext(fixture.owner(), InteractionHand.MAIN_HAND, hit));
        fixture.owner().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
}
