package com.minecolonies.gametest.suites;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.entity.ai.statemachine.AIOneTimeEventTarget;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.buildings.modules.ChildrenBuildingModule;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.datalistener.DiseasesListener;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.util.ExperienceUtils;
import com.minecolonies.gametest.framework.TestFlow;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import java.util.EnumMap;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.happiness.IHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.ITimeBasedHappinessModifier;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.constant.HappinessConstants;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.interactionhandling.RequestBasedInteraction;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.core.entity.ai.minimal.EntityAICitizenChild;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityGrave;
import com.minecolonies.gametest.framework.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * End-to-end tests from docs/e2e-tests/citizens.md and docs/e2e-tests/services.md.
 * <p>
 * Test ids: {@code minecolonies-gametest:citizens_services_tests_<method_name>}. Run only these with
 * {@code ./gradlew runGameTest -Pe2eFilter=citizens_services_tests}. See docs/e2e-tests/AGENT-GUIDE.md.
 */
public class CitizensServicesTests
{
    private static final BlockPos NW = new BlockPos(12, ColonyFixture.GROUND_Y, 12);
    private static final BlockPos NE = new BlockPos(52, ColonyFixture.GROUND_Y, 12);
    private static final BlockPos SW = new BlockPos(12, ColonyFixture.GROUND_Y, 52);
    private static final BlockPos SE = new BlockPos(52, ColonyFixture.GROUND_Y, 52);

    // =====================================================================================================================================
    // CIT-01 .. CIT-14
    // =====================================================================================================================================

    /**
     * CIT-01 Eating when hungry: a hungry citizen with bread in its inventory eats it, and doesn't keep eating once full.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 1600, skyAccess = true)
    public void cit01_eatingWhenHungry(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizen = new ICitizenData[1];
        final int[] breadAfterMeal = new int[1];

        fixture.flow()
          .run("set up colony with a hungry citizen holding bread", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              citizen[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("citizen has an entity", 100, () -> citizen[0].getEntity().isPresent(), () -> ColonyFixture.describe(citizen[0]))
          .run("make hungry, give bread", () -> {
              give(citizen[0], new ItemStack(Items.BREAD, 16));
              fixture.setCitizenNeeds(citizen[0], 1, 20);
          })
          .awaitUntil("citizen ate", 800, () -> citizen[0].getSaturation() > 1 && count(citizen[0], Items.BREAD) < 16,
            () -> "saturation " + citizen[0].getSaturation() + ", bread " + count(citizen[0], Items.BREAD) + ", " + ColonyFixture.describe(citizen[0]))
          .awaitUntil("citizen finished the meal", 1200, () -> !isEating(citizen[0]) && citizen[0].getSaturation() >= 10,
            () -> "saturation " + citizen[0].getSaturation() + ", " + ColonyFixture.describe(citizen[0]))
          .run("remember bread count", () -> breadAfterMeal[0] = count(citizen[0], Items.BREAD))
          .idle(200)
          .check("no more bread eaten while full", () -> count(citizen[0], Items.BREAD) == breadAfterMeal[0],
            () -> "bread " + count(citizen[0], Items.BREAD) + " (was " + breadAfterMeal[0] + "), saturation " + citizen[0].getSaturation())
          .succeed();
    }

    /**
     * CIT-03 No food anywhere: a starving citizen without a restaurant or food raises the "no restaurant" interaction; once it gets
     * food it eats and the interaction goes away.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2400, skyAccess = true)
    public void cit03_noFoodAnywhere(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizen = new ICitizenData[1];
        final String noRestaurant = "com.minecolonies.coremod.ai.norestaurant";

        fixture.flow()
          .run("set up colony with one starving citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              citizen[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("citizen has an entity", 100, () -> citizen[0].getEntity().isPresent(), () -> ColonyFixture.describe(citizen[0]))
          .run("starve", () -> fixture.setCitizenNeeds(citizen[0], 0, 20))
          .awaitUntil("citizen complains that there is no restaurant", 1000, () -> hasInteraction(citizen[0], noRestaurant),
            () -> "interactions " + interactionKeys(citizen[0]) + ", " + ColonyFixture.describe(citizen[0]))
          .check("social happiness factor counts the hungry citizen", () -> factor(citizen[0], HappinessConstants.SOCIAL) < 1.0,
            () -> "social factor " + factor(citizen[0], HappinessConstants.SOCIAL))
          .run("give bread", () -> give(citizen[0], new ItemStack(Items.BREAD, 16)))
          .awaitUntil("citizen ate", 1000, () -> citizen[0].getSaturation() > 0 && count(citizen[0], Items.BREAD) < 16,
            () -> "saturation " + citizen[0].getSaturation() + ", " + ColonyFixture.describe(citizen[0]))
          .awaitUntil("interaction is gone", 400, () -> !hasInteraction(citizen[0], noRestaurant),
            () -> "interactions " + interactionKeys(citizen[0]))
          .succeed();
    }

    /**
     * CIT-04 Sleeping at night: four citizens in two residences sleep in distinct beds of their own home at night, wake up in the
     * morning, and get the "slept tonight" happiness reset.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void cit04_sleepingAtNight(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] homes = new IBuilding[2];
        final ICitizenData[] citizens = new ICitizenData[4];

        fixture.flow()
          .run("set up colony with two residences and four residents", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              homes[0] = fixture.placeBuilding(ModBuildings.home.get(), 2, NW);
              homes[1] = fixture.placeBuilding(ModBuildings.home.get(), 2, NE);
              for (int i = 0; i < 4; i++)
              {
                  citizens[i] = fixture.spawnCitizen(10, null, homes[i / 2]);
              }
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(citizens), () -> describeAll(citizens))
          .run("mark 'slept tonight' as not slept for 3 days, set nightfall", () -> {
              for (final ICitizenData c : citizens)
              {
                  c.setSaturation(20);
                  final ITimeBasedHappinessModifier slept = (ITimeBasedHappinessModifier) c.getCitizenHappinessHandler().getModifier(HappinessConstants.SLEPTTONIGHT);
                  for (int day = 0; day < 3; day++)
                  {
                      slept.dayEnd(c);
                  }
              }
              fixture.setTime(13000);
          })
          .awaitUntil("all citizens asleep in a bed of their own home", 2000, () -> {
              for (final ICitizenData c : citizens)
              {
                  if (!c.isAsleep() || !bedOf(c.getHomeBuilding()).contains(c.getBedPos()))
                  {
                      return false;
                  }
              }
              return true;
          }, () -> sleepInfo(citizens))
          .check("no two citizens share a bed", () -> {
              final Set<BlockPos> beds = new HashSet<>();
              for (final ICitizenData c : citizens)
              {
                  beds.add(c.getBedPos());
              }
              return beds.size() == citizens.length;
          }, () -> sleepInfo(citizens))
          .check("'slept tonight' was reset for every citizen", () -> {
              for (final ICitizenData c : citizens)
              {
                  if (((ITimeBasedHappinessModifier) c.getCitizenHappinessHandler().getModifier(HappinessConstants.SLEPTTONIGHT)).getDays() != 0)
                  {
                      return false;
                  }
              }
              return true;
          }, () -> "days: " + sleptDays(citizens))
          .run("morning", () -> fixture.setTime(1000))
          .awaitUntil("everyone woke up and left the bed", 600, () -> {
              for (final ICitizenData c : citizens)
              {
                  if (c.isAsleep() || c.getEntity().map(e -> e.isSleeping()).orElse(true))
                  {
                      return false;
                  }
              }
              return true;
          }, () -> sleepInfo(citizens))
          .succeed();
    }

    /**
     * CIT-09 Death, grave and mourning: one of two partners dies; a grave with its inventory appears, the grave manager knows it,
     * the citizen is removed, everybody gets the death modifier and the partner mourns from the next morning on.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 2000, skyAccess = true)
    public void cit09_deathGraveMourning(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizens = new ICitizenData[4];
        final BlockPos[] deathPos = new BlockPos[1];
        final String[] deadName = new String[1];
        final int[] deadId = new int[1];

        fixture.flow()
          .run("set up colony with four citizens, two of them partners", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              for (int i = 0; i < 4; i++)
              {
                  citizens[i] = fixture.spawnCitizen(10, null, null);
              }
              citizens[0].setPartner(citizens[1].getId());
              citizens[1].setPartner(citizens[0].getId());
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(citizens), () -> describeAll(citizens))
          .run("give the victim items", () -> {
              give(citizens[0], new ItemStack(Items.OAK_LOG, 7));
              deadName[0] = citizens[0].getName();
              deadId[0] = citizens[0].getId();
          })
          // Citizens cap the damage per hit, so hit until dead (as a player would).
          .awaitUntil("citizen is killed and removed from the colony", 600, () -> {
              citizens[0].getEntity().ifPresent(entity -> {
                  if (entity.isAlive())
                  {
                      deathPos[0] = entity.blockPosition();
                      entity.hurtServer(fixture.level(), fixture.level().damageSources().generic(), 1000f);
                  }
              });
              return fixture.colony().getCitizenManager().getCivilian(deadId[0]) == null;
          }, () -> "citizens " + fixture.colony().getCitizenManager().getCitizens().size() + ", victim " + ColonyFixture.describe(citizens[0]))
          .check("the grave manager knows a grave near the death position holding the items", () -> {
              for (final BlockPos grave : fixture.colony().getGraveManager().getGraves().keySet())
              {
                  if (grave.closerThan(deathPos[0], 11) && fixture.level().getBlockEntity(grave) instanceof final TileEntityGrave tile
                        && InventoryUtils.getItemCountInItemHandler(tile.getInventory(), s -> s.is(Items.OAK_LOG)) == 7)
                  {
                      return true;
                  }
              }
              return false;
          }, () -> "graves " + fixture.colony().getGraveManager().getGraves() + ", died at " + deathPos[0])
          .check("everyone got the death happiness modifier", () -> {
              for (int i = 1; i < 4; i++)
              {
                  final IHappinessModifier death = citizens[i].getCitizenHappinessHandler().getModifier(HappinessConstants.DEATH);
                  if (death == null || death.getFactor(citizens[i]) >= 1.0)
                  {
                      return false;
                  }
              }
              return true;
          }, () -> "death modifier of partner " + citizens[1].getCitizenHappinessHandler().getModifier(HappinessConstants.DEATH))
          .check("only the partner remembers the deceased", () -> citizens[1].getCitizenMournHandler().getDeceasedCitizens().contains(deadName[0])
                                                                  && citizens[2].getCitizenMournHandler().getDeceasedCitizens().isEmpty()
                                                                  && citizens[3].getCitizenMournHandler().getDeceasedCitizens().isEmpty(),
            () -> "partner " + citizens[1].getCitizenMournHandler().getDeceasedCitizens() + ", others " + citizens[2].getCitizenMournHandler().getDeceasedCitizens()
                    + citizens[3].getCitizenMournHandler().getDeceasedCitizens())
          .check("partner relation was cleared", () -> citizens[1].getPartner() == null, () -> "partner " + citizens[1].getPartner())
          .run("night falls", () -> fixture.setTime(13000))
          .awaitUntil("colony noticed the night", 400, () -> !fixture.colony().isDay(), () -> "isDay " + fixture.colony().isDay())
          .run("morning comes", () -> fixture.setTime(1000))
          .awaitUntil("partner is mourning", 600, () -> fixture.colony().isDay() && citizens[1].getCitizenMournHandler().isMourning()
                                                        && "MOURN".equals(aiState(citizens[1])),
            () -> "mourning " + citizens[1].getCitizenMournHandler().isMourning() + ", " + ColonyFixture.describe(citizens[1]))
          .check("the others are not mourning", () -> !citizens[2].getCitizenMournHandler().isMourning() && !citizens[3].getCitizenMournHandler().isMourning(),
            () -> describeAll(citizens))
          .succeed();
    }

    /**
     * CIT-10 Children are born and grow up: with move-in on and a spare bed, a man and a woman get a child that lives in the colony
     * and has no job. With its growth time set past the limit it grows up and can be hired.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 3000, skyAccess = true)
    public void cit10_childrenBornAndGrowUp(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] buildings = new IBuilding[4];
        final ICitizenData[] parents = new ICitizenData[2];
        final ICitizenData[] child = new ICitizenData[1];

        fixture.flow()
          .run("set up colony with a residence (3 beds), two employed adults and a free job", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              buildings[0] = fixture.placeBuilding(ModBuildings.home.get(), 3, NW);
              buildings[1] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, SW);
              buildings[2] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, NE);
              buildings[3] = fixture.placeBuilding(ModBuildings.builder.get(), 1, SE);
              parents[0] = fixture.spawnCitizen(10, buildings[2], buildings[0]);
              parents[1] = fixture.spawnCitizen(10, buildings[3], buildings[0]);
              parents[0].setGenderAndGenerateName(true);
              parents[1].setGenderAndGenerateName(false);
          })
          .awaitUntil("parents have entities", 100, () -> allEntities(parents), () -> describeAll(parents))
          .run("move in on, child timer due", () -> {
              fixture.setMoveIn(true);
              setField(fixture.colony().getReproductionManager(), "childCreationTimer", 0);
          })
          .awaitUntil("a child is born", 400, () -> {
              for (final ICitizenData c : fixture.colony().getCitizenManager().getCitizens())
              {
                  if (c.isChild())
                  {
                      child[0] = c;
                      return true;
                  }
              }
              return false;
          }, () -> "citizens " + fixture.colony().getCitizenManager().getCitizens().size())
          .run("move in off again", () -> fixture.setMoveIn(false))
          .check("child lives in the residence, has no job and knows its parents", () -> child[0].getHomeBuilding() == buildings[0] && child[0].getJob() == null
                                                                                      && (parents[0].getChildren().contains(child[0].getId()) || parents[1].getChildren().contains(child[0].getId())),
            () -> "home " + child[0].getHomeBuilding() + ", job " + child[0].getJob() + ", parents' children " + parents[0].getChildren() + parents[1].getChildren())
          .awaitUntil("child entity exists and is a baby", 200, () -> child[0].getEntity().map(e -> e.isBaby()).orElse(false), () -> ColonyFixture.describe(child[0]))
          .idle(100)
          .check("child still has no job", () -> child[0].getJob() == null, () -> ColonyFixture.describe(child[0]))
          .run("move the child's growth time past adulthood", () -> setField(childAI(child[0]), "aiActiveTime", 70_001))
          .awaitUntil("child grew up", 1200, () -> !child[0].isChild() && child[0].getEntity().map(e -> !e.isBaby()).orElse(false),
            () -> ColonyFixture.describe(child[0]))
          .run("hire the new adult into the free lumberjack hut (unless auto-hiring already did)", () -> {
              if (child[0].getWorkBuilding() == null)
              {
                  fixture.hire(child[0], buildings[1]);
              }
          })
          .check("adult has the job", () -> child[0].getJob() != null && child[0].getWorkBuilding() == buildings[1], () -> ColonyFixture.describe(child[0]))
          .succeed();
    }

    /**
     * CIT-14 Answering a citizen: a worker with an open blocking request interaction; the owner answers "cancel" through the same
     * server handler the GUI uses. The request is cancelled and the interaction closes.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 800, skyAccess = true)
    public void cit14_answeringACitizen(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final IToken<?>[] token = new IToken<?>[1];
        final IInteractionResponseHandler[] interaction = new IInteractionResponseHandler[1];

        fixture.flow()
          .run("set up colony with a worker", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, NW);
              worker[0] = fixture.spawnCitizen(10, hut[0], null);
          })
          .awaitUntil("worker has an entity", 100, () -> worker[0].getEntity().isPresent(), () -> ColonyFixture.describe(worker[0]))
          .run("worker requests a diamond", () -> token[0] = worker[0].createRequest(new Stack(new ItemStack(Items.DIAMOND), 1, 1)))
          .awaitUntil("worker shows the request interaction", 200, () -> {
              for (final IInteractionResponseHandler h : chatOptions(worker[0]).values())
              {
                  if (h instanceof RequestBasedInteraction)
                  {
                      interaction[0] = h;
                      return true;
                  }
              }
              return false;
          }, () -> "interactions " + interactionKeys(worker[0]))
          .run("owner answers 'cancel'", () -> {
              final List<Component> responses = interaction[0].getPossibleResponses();
              int cancel = -1;
              for (int i = 0; i < responses.size(); i++)
              {
                  if (responses.get(i).getContents() instanceof final TranslatableContents t && t.getKey().equals("com.minecolonies.coremod.gui.chat.cancel"))
                  {
                      cancel = i;
                  }
              }
              helper.assertTrue(cancel >= 0, "no cancel answer in " + responses);
              worker[0].onResponseTriggered(interaction[0].getId(), cancel, fixture.owner());
          })
          .awaitUntil("request is cancelled", 100, () -> {
              final IRequest<?> request = fixture.colony().getRequestManager().getRequestForToken(token[0]);
              return request == null || request.getState() == RequestState.CANCELLED || request.getState() == RequestState.COMPLETED;
          }, () -> "request " + fixture.colony().getRequestManager().getRequestForToken(token[0]))
          .awaitUntil("interaction closed", 400, () -> !chatOptions(worker[0]).containsKey(interaction[0].getId()),
            () -> "interactions " + interactionKeys(worker[0]))
          .succeed();
    }

    /**
     * CIT-02 Eating at the restaurant: three hungry citizens without food walk to the restaurant and are served bread by the cook.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void cit02_eatingAtTheRestaurant(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] restaurant = new IBuilding[1];
        final ICitizenData[] cook = new ICitizenData[1];
        final ICitizenData[] guests = new ICitizenData[3];
        final boolean[] visited = new boolean[3];

        fixture.flow()
          .run("set up colony with a staffed restaurant serving bread and three guests", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              restaurant[0] = fixture.placeBuilding(ModBuildings.cook.get(), 1, NW);
              restaurant[0].getModule(BuildingModules.RESTAURANT_MENU).addMenuItem(new ItemStack(Items.BREAD));
              fixture.fillBuilding(restaurant[0], new ItemStack(Items.BREAD, 64));
              cook[0] = fixture.spawnCitizen(20, restaurant[0], null);
              for (int i = 0; i < 3; i++)
              {
                  guests[i] = fixture.spawnCitizen(10, null, null);
              }
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(guests) && allEntities(cook), () -> describeAll(guests))
          .run("make the guests hungry", () -> {
              for (final ICitizenData g : guests)
              {
                  fixture.setCitizenNeeds(g, 1, 20);
              }
          })
          .awaitUntil("all guests visited the restaurant and ate", 5000, () -> {
              boolean all = true;
              for (int i = 0; i < 3; i++)
              {
                  final int index = i;
                  guests[i].getEntity().ifPresent(e -> visited[index] |= restaurant[0].isInBuilding(e.blockPosition()));
                  all &= visited[i] && guests[i].getSaturation() > 5;
              }
              return all;
          }, () -> "visited " + java.util.Arrays.toString(visited) + ", food served " + fixture.colony().getStatisticsManager().getStatTotal("food_served")
                     + ", bread in restaurant " + fixture.countItems(restaurant[0], s -> s.is(Items.BREAD)) + describeAll(guests) + "\n  cook " + ColonyFixture.describe(cook[0]))
          .check("bread left the restaurant", () -> fixture.countItems(restaurant[0], s -> s.is(Items.BREAD)) < 64,
            () -> "bread " + fixture.countItems(restaurant[0], s -> s.is(Items.BREAD)))
          .check("the cook served food", () -> fixture.colony().getStatisticsManager().getStatTotal("food_served") > 0,
            () -> "food served " + fixture.colony().getStatisticsManager().getStatTotal("food_served"))
          .check("the meal is in every guest's food history", () -> {
              for (final ICitizenData g : guests)
              {
                  if (!g.getCitizenFoodHandler().getLastEatenFoods().contains(Items.BREAD))
                  {
                      return false;
                  }
              }
              return true;
          }, () -> "histories " + guests[0].getCitizenFoodHandler().getLastEatenFoods() + guests[1].getCitizenFoodHandler().getLastEatenFoods()
                     + guests[2].getCitizenFoodHandler().getLastEatenFoods())
          .succeed();
    }

    /**
     * CIT-05 Rain: a lumberjack stops working in the rain and goes back to work after it; a teacher (whose hut allows rain work) keeps
     * working; with {@code workersAlwaysWorkInRain} on the lumberjack keeps working too.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void cit05_rain(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] huts = new IBuilding[2];
        final ICitizenData[] lumberjack = new ICitizenData[1];
        final ICitizenData[] teacher = new ICitizenData[1];
        final int[] streak = new int[1];

        fixture.flow()
          .run("set up colony with a lumberjack and a teacher", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              huts[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, NW);
              huts[1] = fixture.placeBuilding(ModBuildings.school.get(), 1, NE);
              lumberjack[0] = fixture.spawnCitizen(10, huts[0], null);
              teacher[0] = fixture.spawnCitizen(10, null, null);
              fixture.hire(teacher[0], huts[1], ModJobs.teacher.get());
          })
          .awaitUntil("both are working", 600, () -> isWorking(lumberjack[0]) && isWorking(teacher[0]), () -> describeAll(lumberjack[0], teacher[0]))
          .run("rain starts", () -> fixture.setWeather(true, false))
          .awaitUntil("lumberjack stops for bad weather", 400,
            () -> !isWorking(lumberjack[0]) && lumberjack[0].getStatus() == VisibleCitizenStatus.BAD_WEATHER, () -> describeAll(lumberjack[0]))
          .check("the teacher keeps working", () -> isWorking(teacher[0]), () -> describeAll(teacher[0]))
          .run("rain stops", () -> fixture.setWeather(false, false))
          .awaitUntil("lumberjack goes back to work", 600, () -> isWorking(lumberjack[0]), () -> describeAll(lumberjack[0]))
          .run("workersAlwaysWorkInRain on, rain again", () -> {
              fixture.setConfig(MineColonies.getConfig().getServer().workersAlwaysWorkInRain, true);
              fixture.setWeather(true, false);
              streak[0] = 0;
          })
          .awaitUntil("lumberjack keeps working for 400 ticks of rain", 1200, () -> {
              streak[0] = isWorking(lumberjack[0]) ? streak[0] + 1 : 0;
              return streak[0] >= 400;
          }, () -> "streak " + streak[0] + ", " + describeAll(lumberjack[0]))
          .run("weather back to clear", () -> fixture.setWeather(false, false))
          .succeed();
    }

    /**
     * CIT-06 Happiness reacts to living conditions: per-citizen factors (homelessness, unemployment, health, food) move in the right
     * direction against a baseline citizen, homelessness and good food move the total, and fixing homelessness restores the factor.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 1000, skyAccess = true)
    public void cit06_happinessReactsToConditions(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] b = new IBuilding[3];
        // 0 baseline, 1 homeless, 2 unemployed, 3 sick, 4 good food
        final ICitizenData[] c = new ICitizenData[5];

        happinessColony(fixture, b, c)
          .check("homeless citizen: homelessness factor below the baseline's",
            () -> factor(c[1], HappinessConstants.HOMELESSNESS) < 1.0 && factor(c[0], HappinessConstants.HOMELESSNESS) >= 1.0,
            () -> factors(c, HappinessConstants.HOMELESSNESS))
          .check("unemployed citizen: unemployment factor below the baseline's",
            () -> factor(c[2], HappinessConstants.UNEMPLOYMENT) < 1.0 && factor(c[0], HappinessConstants.UNEMPLOYMENT) >= 1.0,
            () -> factors(c, HappinessConstants.UNEMPLOYMENT))
          .check("sick citizen: health factor below the baseline's",
            () -> factor(c[3], HappinessConstants.HEALTH) < 1.0 && factor(c[0], HappinessConstants.HEALTH) >= 1.0,
            () -> factors(c, HappinessConstants.HEALTH))
          .check("well fed citizen: food factor above the baseline's",
            () -> factor(c[4], HappinessConstants.FOOD) > 1.0 && factor(c[0], HappinessConstants.FOOD) == 1.0,
            () -> factors(c, HappinessConstants.FOOD))
          .check("no guards: security factor below neutral (colony wide)", () -> factor(c[0], HappinessConstants.SECURITY) < 1.0,
            () -> factors(c, HappinessConstants.SECURITY))
          .check("total happiness: homeless below, well fed above the baseline", () -> {
              final double base = happiness(c[0]);
              return happiness(c[1]) < base && happiness(c[4]) > base;
          }, () -> "happiness " + happinessList(c))
          .run("give the homeless citizen a home", () -> b[1].getFirstModuleOccurance(LivingBuildingModule.class).assignCitizen(c[1]))
          .check("homelessness factor recovered", () -> factor(c[1], HappinessConstants.HOMELESSNESS) >= 1.0, () -> factors(c, HappinessConstants.HOMELESSNESS))
          .succeed();
    }

    /**
     * CIT-06b Negative factors lower the total: an unemployed or a sick citizen must not be happier than an otherwise identical
     * employed, healthy one.
     */
    // BUG E2E-CIT06-1: CitizenHappinessHandler.getHappiness averages only the factors that are not exactly 1.0. A below-neutral factor
    // (unemployment 0.5, health 0.5) that is still above the current average raises the total: the unemployed and the sick citizen
    // end up happier than the baseline.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 1000, skyAccess = true, required = false)
    public void cit06b_negativeFactorsLowerHappiness(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] b = new IBuilding[3];
        final ICitizenData[] c = new ICitizenData[5];

        happinessColony(fixture, b, c)
          .check("unemployed and sick citizens are less happy than the baseline", () -> {
              final double base = happiness(c[0]);
              return happiness(c[2]) < base && happiness(c[3]) < base;
          }, () -> "happiness (baseline, homeless, unemployed, sick, well fed) " + happinessList(c))
          .succeed();
    }

    /**
     * Colony for CIT-06: two level 3 residences, a level 2 library with four student places, and five citizens: 0 baseline (home, job),
     * 1 homeless, 2 unemployed, 3 sick, 4 well fed (ten different MineColonies meals in its food history).
     */
    private static TestFlow happinessColony(final ColonyFixture fixture, final IBuilding[] b, final ICitizenData[] c)
    {
        return fixture.flow()
          .run("set up colony: two level 3 residences, a level 2 library with four student places", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              b[0] = fixture.placeBuilding(ModBuildings.home.get(), 3, NW);
              b[1] = fixture.placeBuilding(ModBuildings.home.get(), 3, NE);
              b[2] = fixture.placeBuilding(ModBuildings.library.get(), 2, SW);
              c[0] = fixture.spawnCitizen(5, b[2], b[0]);
              c[1] = fixture.spawnCitizen(5, b[2], null);
              c[2] = fixture.spawnCitizen(5, null, b[0]);
              c[3] = fixture.spawnCitizen(5, b[2], b[0]);
              c[4] = fixture.spawnCitizen(5, b[2], b[1]);
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(c), () -> describeAll(c))
          .run("make one sick and feed one ten different MineColonies meals", () -> {
              infect(c[3], "minecolonies:influenza");
              int added = 0;
              for (final Item item : BuiltInRegistries.ITEM)
              {
                  if (item instanceof IMinecoloniesFoodItem && added < 10)
                  {
                      c[4].getCitizenFoodHandler().addLastEaten(item);
                      added++;
                  }
              }
              fixture.helper().assertTrue(added == 10, "only " + added + " MineColonies foods found");
          });
        // No waiting after the infection: with more than four citizens the disease spreads on contact (CitizenDiseaseHandler
        // .onCollission) and the citizens stand close together at the library, so the checks follow in the same tick.
    }

    private static String happinessList(final ICitizenData[] citizens)
    {
        final List<String> values = new ArrayList<>();
        for (final ICitizenData c : citizens)
        {
            values.add(String.format("%.2f", happiness(c)));
        }
        return values.toString();
    }

    /**
     * CIT-07 Healing: an injured citizen heals on its own; with a hospital and a healer it heals faster.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void cit07_healing(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hospital = new IBuilding[1];
        final ICitizenData[] patient = new ICitizenData[1];
        final ICitizenData[] healer = new ICitizenData[1];
        final int[] ticks = new int[2];

        fixture.flow()
          .run("set up colony with one citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              patient[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("patient has an entity", 100, () -> patient[0].getEntity().isPresent(), () -> ColonyFixture.describe(patient[0]))
          .run("injure (no hospital)", () -> fixture.setCitizenNeeds(patient[0], 15, 4))
          .awaitUntil("patient healed on its own", 3000, () -> {
              ticks[0]++;
              return health(patient[0]) >= 12;
          }, () -> "health " + health(patient[0]) + ", " + ColonyFixture.describe(patient[0]))
          .run("build a hospital with a healer", () -> {
              hospital[0] = fixture.placeBuilding(ModBuildings.hospital.get(), 1, NW);
              healer[0] = fixture.spawnCitizen(30, hospital[0], null);
          })
          .awaitUntil("healer has an entity", 100, () -> healer[0].getEntity().isPresent(), () -> ColonyFixture.describe(healer[0]))
          .run("injure again", () -> fixture.setCitizenNeeds(patient[0], 15, 4))
          .awaitUntil("patient healed with the hospital", 3000, () -> {
              ticks[1]++;
              return health(patient[0]) >= 12;
          }, () -> "health " + health(patient[0]) + ", " + ColonyFixture.describe(patient[0]) + "\n  healer " + ColonyFixture.describe(healer[0]))
          .check("healing with the hospital was faster", () -> ticks[1] < ticks[0], () -> "without hospital " + ticks[0] + " ticks, with " + ticks[1])
          .succeed();
    }

    /**
     * CIT-08 Disease and cure: a citizen sick with influenza lies down in a hospital bed, the healer brings the cure items from the
     * hospital and the citizen is cured.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void cit08_diseaseAndCure(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingHospital[] hospital = new BuildingHospital[1];
        final ICitizenData[] patient = new ICitizenData[1];
        final ICitizenData[] healer = new ICitizenData[1];
        final boolean[] inBed = new boolean[1];

        fixture.flow()
          .run("set up colony with a hospital, a healer, cure items and a patient", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hospital[0] = (BuildingHospital) fixture.placeBuilding(ModBuildings.hospital.get(), 1, NW);
              fixture.fillBuilding(hospital[0], new ItemStack(Items.CARROT, 16), new ItemStack(Items.POTATO, 16));
              healer[0] = fixture.spawnCitizen(30, hospital[0], null);
              patient[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(patient[0], healer[0]), () -> describeAll(patient[0], healer[0]))
          .run("infect with influenza", () -> {
              infect(patient[0], "minecolonies:influenza");
              patient[0].setSaturation(20);
          })
          .awaitUntil("patient lies in a hospital bed", 2500, () -> inBed[0] |= patient[0].isAsleep() && hospital[0].getBedList().contains(patient[0].getBedPos()),
            () -> "beds " + hospital[0].getBedList() + ", " + sleepInfo(patient[0]) + "\n  healer " + ColonyFixture.describe(healer[0]))
          .awaitUntil("patient is cured", 3000, () -> !patient[0].getCitizenDiseaseHandler().isSick(),
            () -> "disease " + patient[0].getCitizenDiseaseHandler().getDisease() + ", " + ColonyFixture.describe(patient[0]) + ", inventory carrots "
                    + count(patient[0], Items.CARROT) + "\n  healer " + ColonyFixture.describe(healer[0]))
          .check("one carrot and one potato were used up", () -> totalCount(fixture, hospital[0], Items.CARROT, healer[0], patient[0]) == 15
                                                                && totalCount(fixture, hospital[0], Items.POTATO, healer[0], patient[0]) == 15,
            () -> "carrots " + totalCount(fixture, hospital[0], Items.CARROT, healer[0], patient[0]) + ", potatoes " + totalCount(fixture, hospital[0], Items.POTATO, healer[0], patient[0]))
          .awaitUntil("patient returns to normal life", 600, () -> !patient[0].isAsleep() && !"SICK".equals(aiState(patient[0])),
            () -> ColonyFixture.describe(patient[0]))
          .succeed();
    }

    /**
     * CIT-08b Disease without cure items: the healer requests exactly the cure items of the patient's disease.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 4000, skyAccess = true)
    public void cit08b_healerRequestsCure(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingHospital[] hospital = new BuildingHospital[1];
        final ICitizenData[] patient = new ICitizenData[1];
        final ICitizenData[] healer = new ICitizenData[1];

        fixture.flow()
          .run("set up colony with an empty hospital, a healer and a patient", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hospital[0] = (BuildingHospital) fixture.placeBuilding(ModBuildings.hospital.get(), 1, NW);
              healer[0] = fixture.spawnCitizen(30, hospital[0], null);
              patient[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(patient[0], healer[0]), () -> describeAll(patient[0], healer[0]))
          .run("infect with influenza", () -> {
              infect(patient[0], "minecolonies:influenza");
              patient[0].setSaturation(20);
          })
          .awaitUntil("healer requests carrots and potatoes", 3500,
            () -> requestedItems(hospital[0], healer[0]).containsAll(Set.of(Items.CARROT, Items.POTATO)),
            () -> "requested " + requestedItems(hospital[0], healer[0]) + ", " + describeAll(patient[0], healer[0]))
          .check("only cure items are requested", () -> Set.of(Items.CARROT, Items.POTATO).containsAll(requestedItems(hospital[0], healer[0])),
            () -> "requested " + requestedItems(hospital[0], healer[0]))
          .check("patient is still sick", () -> patient[0].getCitizenDiseaseHandler().isSick(), () -> ColonyFixture.describe(patient[0]))
          .succeed();
    }

    /**
     * CIT-11 Workers gain experience: a lumberjack fells trees; its primary skill (Strength) gains more than the secondary (Focus),
     * their complementary skills (Athletics, Mana) gain a share and their adverse skills (Agility, Adaptability) lose one, the other
     * skills stay unchanged, and a skill levels up exactly once per threshold.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 12000, skyAccess = true)
    public void cit11_workersGainExperience(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final Map<Skill, Double> before = new EnumMap<>(Skill.class);
        final BlockPos[] trees = {new BlockPos(6, ColonyFixture.GROUND_Y, 30), new BlockPos(12, ColonyFixture.GROUND_Y, 30), new BlockPos(18, ColonyFixture.GROUND_Y, 30)};

        fixture.flow()
          .run("set up colony with a lumberjack with an axe and three trees", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, NW);
              worker[0] = fixture.spawnCitizen(5, hut[0], null);
              for (final BlockPos tree : trees)
              {
                  growTree(helper, tree);
              }
          })
          .awaitUntil("worker has an entity", 100, () -> worker[0].getEntity().isPresent(), () -> ColonyFixture.describe(worker[0]))
          .run("give an axe and remember skills", () -> {
              give(worker[0], new ItemStack(Items.WOODEN_AXE));
              give(worker[0], new ItemStack(Items.WOODEN_HOE));
              worker[0].setSaturation(60);
              for (final Skill skill : Skill.values())
              {
                  before.put(skill, progress(worker[0], skill));
              }
          })
          .awaitUntil("a tree was felled and Strength gained experience", 11000,
            () -> progress(worker[0], Skill.Strength) > before.get(Skill.Strength) && treesLeft(helper, trees) < trees.length,
            () -> "strength " + before.get(Skill.Strength) + " -> " + progress(worker[0], Skill.Strength) + ", trees left " + treesLeft(helper, trees) + ", "
                    + ColonyFixture.describe(worker[0]))
          .check("primary skill gained more than secondary, secondary gained", () -> {
              final double primary = progress(worker[0], Skill.Strength) - before.get(Skill.Strength);
              final double secondary = progress(worker[0], Skill.Focus) - before.get(Skill.Focus);
              return primary > secondary && secondary > 0;
          }, () -> skillDelta(worker[0], before))
          .check("complementary skills gain a share, adverse skills lose one", () -> {
              final double athletics = progress(worker[0], Skill.Athletics) - before.get(Skill.Athletics);
              final double agility = progress(worker[0], Skill.Agility) - before.get(Skill.Agility);
              final double mana = progress(worker[0], Skill.Mana) - before.get(Skill.Mana);
              final double adaptability = progress(worker[0], Skill.Adaptability) - before.get(Skill.Adaptability);
              return athletics > 0 && agility < 0 && mana > 0 && adaptability < 0;
          }, () -> skillDelta(worker[0], before))
          .check("unrelated skills unchanged", () -> {
              for (final Skill skill : List.of(Skill.Dexterity, Skill.Stamina, Skill.Knowledge, Skill.Intelligence, Skill.Creativity))
              {
                  if (progress(worker[0], skill) != before.get(skill))
                  {
                      return false;
                  }
              }
              return true;
          }, () -> skillDelta(worker[0], before))
          .check("one threshold of experience gives exactly one level", () -> {
              final int level = worker[0].getCitizenSkillHandler().getLevel(Skill.Creativity);
              final double xp = worker[0].getCitizenSkillHandler().getSkills().get(Skill.Creativity).getExperience();
              worker[0].getCitizenSkillHandler().addXpToSkill(Skill.Creativity, ExperienceUtils.getXPNeededForNextLevel(level) - xp, worker[0]);
              return worker[0].getCitizenSkillHandler().getLevel(Skill.Creativity) == level + 1;
          }, () -> "creativity level " + worker[0].getCitizenSkillHandler().getLevel(Skill.Creativity))
          .succeed();
    }

    /**
     * CIT-12 Finding the way: a citizen is sent (as by the walk command) to a house behind a door, a platform up stairs, a tower top
     * up a ladder, across a bridge and back through a shallow ford, without taking damage.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 6000, skyAccess = true)
    public void cit12_findingTheWay(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] walker = new ICitizenData[1];
        final boolean[] arrived = new boolean[1];
        final boolean[] doorOpened = new boolean[1];
        final float[] minHealth = {Float.MAX_VALUE};
        final BlockPos door = new BlockPos(6, 5, 40);
        final BlockPos inHouse = new BlockPos(6, 5, 43);
        final BlockPos onPlatform = new BlockPos(11, 8, 28);
        final BlockPos onTower = new BlockPos(4, 10, 20);
        final BlockPos acrossBridge = new BlockPos(8, 5, 58);
        final BlockPos backThroughFord = new BlockPos(18, 5, 46);

        TestFlow flow = fixture.flow()
          .run("build the course and spawn the walker", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              buildCourse(helper);
              walker[0] = fixture.spawnCitizen(10, null, null);
          })
          .awaitUntil("walker has an entity", 100, () -> walker[0].getEntity().isPresent(), () -> ColonyFixture.describe(walker[0]))
          .run("full health and food", () -> fixture.setCitizenNeeds(walker[0], 60, 20));

        for (final BlockPos target : List.of(inHouse, onPlatform, onTower, acrossBridge, backThroughFord))
        {
            flow = flow
              .run("send walker to " + target.toShortString(), () -> walkTo(fixture, walker[0], fixture.abs(target), arrived))
              .awaitUntil("walker reached " + target.toShortString(), 1500, () -> {
                  walker[0].getEntity().ifPresent(e -> minHealth[0] = Math.min(minHealth[0], e.getHealth()));
                  doorOpened[0] |= helper.getBlockState(door).getValue(DoorBlock.OPEN);
                  return arrived[0] && walker[0].getEntity().map(e -> e.blockPosition().closerThan(fixture.abs(target), 2.5)).orElse(false);
              }, () -> "arrived " + arrived[0] + ", " + ColonyFixture.describe(walker[0]));
        }
        flow
          .check("the door was opened", () -> doorOpened[0], () -> "door never seen open")
          .awaitUntil("the door is closed again", 200, () -> !helper.getBlockState(door).getValue(DoorBlock.OPEN), () -> "door still open")
          .check("no damage from falling or drowning", () -> minHealth[0] >= 20, () -> "lowest health " + minHealth[0])
          .succeed();
    }

    /**
     * CIT-13 Getting unstuck: a lumberjack trapped in a 1x1 hole two blocks deep gets out by itself and walks to its hut.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 7500, skyAccess = true)
    public void cit13_gettingUnstuck(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] hut = new IBuilding[1];
        final ICitizenData[] worker = new ICitizenData[1];
        final BlockPos hole = new BlockPos(16, 3, 44);

        fixture.flow()
          .run("set up colony with a lumberjack and a hole", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hut[0] = fixture.placeBuilding(ModBuildings.lumberjack.get(), 1, NE);
              helper.setBlock(hole, Blocks.AIR);
              helper.setBlock(hole.above(), Blocks.AIR);
              worker[0] = fixture.spawnCitizen(10, hut[0], null);
          })
          .awaitUntil("worker has an entity", 100, () -> worker[0].getEntity().isPresent(), () -> ColonyFixture.describe(worker[0]))
          .run("drop the worker into the hole", () -> {
              final BlockPos abs = fixture.abs(hole);
              worker[0].getEntity().get().teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
              worker[0].getEntity().get().getNavigation().stop();
          })
          .check("worker is in the hole", () -> fixture.rel(worker[0].getEntity().get().blockPosition()).getY() < ColonyFixture.GROUND_Y,
            () -> ColonyFixture.describe(worker[0]) + ", rel " + worker[0].getEntity().map(e -> fixture.rel(e.blockPosition()).toShortString()).orElse("-") + ", hut rel " + fixture.rel(hut[0].getPosition()).toShortString())
          .awaitUntil("worker got out of the hole", 5000, () -> worker[0].getEntity().map(e -> {
              final BlockPos rel = fixture.rel(e.blockPosition());
              return rel.getY() >= ColonyFixture.GROUND_Y;
          }).orElse(false), () -> ColonyFixture.describe(worker[0]) + ", rel " + worker[0].getEntity().map(e -> fixture.rel(e.blockPosition()).toShortString()).orElse("-") + ", hut rel " + fixture.rel(hut[0].getPosition()).toShortString())
          .awaitUntil("worker reached its hut", 1500, () -> worker[0].getEntity().map(e -> e.blockPosition().closerThan(hut[0].getPosition(), 6)).orElse(false),
            () -> ColonyFixture.describe(worker[0]) + ", rel " + worker[0].getEntity().map(e -> fixture.rel(e.blockPosition()).toShortString()).orElse("-") + ", hut rel " + fixture.rel(hut[0].getPosition()).toShortString())
          .succeed();
    }

    // =====================================================================================================================================
    // SRV-01 .. SRV-05
    // =====================================================================================================================================

    /**
     * SRV-01 School: a teacher teaches two pupils; the pupils sit down, gain Intelligence and use up paper; once the paper is gone the
     * teacher requests more. An adult put in a pupil place is removed again.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void srv01_school(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] school = new IBuilding[1];
        final ICitizenData[] teacher = new ICitizenData[1];
        final ICitizenData[] pupils = new ICitizenData[2];
        final ICitizenData[] adult = new ICitizenData[1];
        final double[] before = new double[2];
        final boolean[] sat = new boolean[2];

        fixture.flow()
          .run("set up colony with a school, a teacher, two pupils and two sheets of paper", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              school[0] = fixture.placeBuilding(ModBuildings.school.get(), 1, NW);
              fixture.fillBuilding(school[0], new ItemStack(Items.PAPER, 2));
              teacher[0] = fixture.spawnCitizen(30, null, null);
              fixture.hire(teacher[0], school[0], ModJobs.teacher.get());
              for (int i = 0; i < 2; i++)
              {
                  pupils[i] = spawnChild(fixture, school[0]);
              }
              adult[0] = fixture.spawnCitizen(5, null, null);
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(pupils) && allEntities(teacher[0], adult[0]), () -> describeAll(pupils))
          .run("remember Intelligence, put an adult into a pupil place", () -> {
              for (int i = 0; i < 2; i++)
              {
                  before[i] = progress(pupils[i], Skill.Intelligence);
              }
              school[0].getModuleMatching(ChildrenBuildingModule.class, m -> true).assignCitizen(adult[0]);
          })
          .awaitUntil("the adult is no pupil", 200, () -> adult[0].getJob() == null || adult[0].getWorkBuilding() != school[0],
            () -> ColonyFixture.describe(adult[0]))
          .awaitUntil("both pupils sat at a desk and gained Intelligence", 5000, () -> {
              boolean all = true;
              for (int i = 0; i < 2; i++)
              {
                  final int index = i;
                  pupils[i].getEntity().ifPresent(e -> sat[index] |= e.getVehicle() != null);
                  all &= sat[i] && progress(pupils[i], Skill.Intelligence) > before[i];
              }
              return all;
          }, () -> "sat " + java.util.Arrays.toString(sat) + ", int " + progress(pupils[0], Skill.Intelligence) + "/" + progress(pupils[1], Skill.Intelligence)
                     + describeAll(pupils) + "\n  teacher " + ColonyFixture.describe(teacher[0]))
          .awaitUntil("paper is used up and the teacher requests more", 3500,
            () -> fixture.countItems(school[0], s -> s.is(Items.PAPER)) + count(teacher[0], Items.PAPER) == 0
                    && requestedItems(school[0], teacher[0]).contains(Items.PAPER),
            () -> "paper in school " + fixture.countItems(school[0], s -> s.is(Items.PAPER)) + ", teacher " + count(teacher[0], Items.PAPER) + ", pupils "
                    + count(pupils[0], Items.PAPER) + "/" + count(pupils[1], Items.PAPER) + ", requests " + requestedItems(school[0], teacher[0]))
          .succeed();
    }

    /**
     * SRV-02 Library: two students study with paper; their Intelligence rises and paper is used up (paper breaks on every use).
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 10000, skyAccess = true)
    public void srv02_library(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] library = new IBuilding[1];
        final ICitizenData[] students = new ICitizenData[2];
        final double[] before = new double[2];

        fixture.flow()
          .run("set up colony with a library, two students and paper", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              library[0] = fixture.placeBuilding(ModBuildings.library.get(), 1, NW);
              fixture.fillBuilding(library[0], new ItemStack(Items.PAPER, 16));
              for (int i = 0; i < 2; i++)
              {
                  students[i] = fixture.spawnCitizen(Map.of(Skill.Intelligence, 1), library[0], null, null);
              }
          })
          .awaitUntil("students have entities", 100, () -> allEntities(students), () -> describeAll(students))
          .run("remember Intelligence", () -> {
              for (int i = 0; i < 2; i++)
              {
                  before[i] = progress(students[i], Skill.Intelligence);
              }
          })
          .awaitUntil("both students gained Intelligence", 9500,
            () -> progress(students[0], Skill.Intelligence) > before[0] && progress(students[1], Skill.Intelligence) > before[1],
            () -> "int " + before[0] + "->" + progress(students[0], Skill.Intelligence) + ", " + before[1] + "->" + progress(students[1], Skill.Intelligence)
                    + ", paper " + totalCount(fixture, library[0], Items.PAPER, students[0], students[1]) + describeAll(students))
          .check("paper was used up", () -> totalCount(fixture, library[0], Items.PAPER, students[0], students[1]) < 16,
            () -> "paper " + totalCount(fixture, library[0], Items.PAPER, students[0], students[1]))
          .succeed();
    }

    /**
     * SRV-02b Library study items help: the chance of an Intelligence gain per study session must be higher with a study item than
     * without. Uses the same chances {@code EntityAIStudy.study} passes to {@code tryLevelUpIntelligence}.
     */
    // BUG E2E-SRV02-1: CitizenSkillHandler.tryLevelUpIntelligence fails when random * customChance < 1, so the success chance is
    // 1 - 1/customChance: 87.5% without items, 50% with paper, 25% with a book. Study items make studying worse.
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 200, skyAccess = true, required = false)
    public void srv02b_studyItemsHelp(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] student = new ICitizenData[1];
        final int[] successes = new int[3];
        final int trials = 4000;

        fixture.flow()
          .run("set up colony with one citizen", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              student[0] = fixture.spawnCitizen(Map.of(Skill.Intelligence, 1), null, null, null);
          })
          .run("roll the study chances", () -> {
              final java.util.Random random = new java.util.Random(42);
              // EntityAIStudy: no item -> ONE_IN_X_CHANCE (8); with item -> 8 * (10 / skill_increase_chance): paper 40, book 60.
              final double[] chances = {8, 8 * (10D / 40), 8 * (10D / 60)};
              for (int k = 0; k < 3; k++)
              {
                  for (int i = 0; i < trials; i++)
                  {
                      if (student[0].getCitizenSkillHandler().tryLevelUpIntelligence(random, chances[k], student[0]))
                      {
                          successes[k]++;
                      }
                  }
              }
          })
          .check("studying with paper or a book succeeds more often than without items",
            () -> successes[1] > successes[0] && successes[2] > successes[0],
            () -> "successes of " + trials + ": no item " + successes[0] + ", paper " + successes[1] + ", book " + successes[2])
          .succeed();
    }

    /**
     * SRV-03 Hospital: a healer with two sick patients (cure items for only one disease) and one injured citizen.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 14000, skyAccess = true)
    public void srv03_hospital(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final BuildingHospital[] hospital = new BuildingHospital[1];
        final ICitizenData[] healer = new ICitizenData[1];
        // 0 influenza (cure in stock), 1 measles (no cure), 2 injured
        final ICitizenData[] p = new ICitizenData[3];
        final BlockPos[] beds = new BlockPos[2];
        final boolean[] sharedBed = new boolean[1];

        fixture.flow()
          .run("set up colony with a hospital, a healer, influenza cure items and three patients", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              hospital[0] = (BuildingHospital) fixture.placeBuilding(ModBuildings.hospital.get(), 2, NW);
              fixture.fillBuilding(hospital[0], new ItemStack(Items.CARROT, 16), new ItemStack(Items.POTATO, 16));
              healer[0] = fixture.spawnCitizen(30, hospital[0], null);
              for (int i = 0; i < 3; i++)
              {
                  p[i] = fixture.spawnCitizen(10, null, null);
              }
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(p) && allEntities(healer[0]), () -> describeAll(p))
          .run("infect two, injure one", () -> {
              infect(p[0], "minecolonies:influenza");
              infect(p[1], "minecolonies:measles");
              p[0].setSaturation(20);
              p[1].setSaturation(20);
              fixture.setCitizenNeeds(p[2], 20, 4);
          })
          // 13000: room for a random 3-minute leisure break of the healer (CitizenData.update); once 8500 were not enough.
          .awaitUntil("influenza cured, measles cure requested, injured healed, both sick patients were in hospital beds", 13000, () -> {
              for (int i = 0; i < 2; i++)
              {
                  if (p[i].isAsleep() && hospital[0].getBedList().contains(p[i].getBedPos()))
                  {
                      beds[i] = p[i].getBedPos();
                  }
              }
              sharedBed[0] |= p[0].isAsleep() && p[1].isAsleep() && p[0].getBedPos().equals(p[1].getBedPos());
              final Set<Item> requested = requestedItems(hospital[0], healer[0]);
              return !p[0].getCitizenDiseaseHandler().isSick() && p[1].getCitizenDiseaseHandler().isSick()
                       && (requested.contains(Items.POPPY) || requested.contains(Items.DANDELION) || requested.contains(Items.KELP))
                       && health(p[2]) >= 10 && beds[0] != null && beds[1] != null;
          }, () -> "beds " + java.util.Arrays.toString(beds) + " of " + hospital[0].getBedList() + ", requested " + requestedItems(hospital[0], healer[0])
                     + ", injured health " + health(p[2]) + describeAll(p) + "\n  healer " + ColonyFixture.describe(healer[0]))
          .check("the two sick patients never shared a bed", () -> !sharedBed[0], () -> "shared")
          .check("no influenza items were requested", () -> !requestedItems(hospital[0], healer[0]).contains(Items.CARROT)
                                                           && !requestedItems(hospital[0], healer[0]).contains(Items.POTATO),
            () -> "requested " + requestedItems(hospital[0], healer[0]))
          .succeed();
    }

    /**
     * SRV-04 Graveyard: the undertaker digs up a dead citizen's grave, takes its items to the graveyard and buries the citizen there.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 9000, skyAccess = true)
    public void srv04_graveyard(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] graveyard = new IBuilding[1];
        final ICitizenData[] undertaker = new ICitizenData[1];
        final ICitizenData[] victim = new ICitizenData[1];
        final BlockPos[] grave = new BlockPos[1];
        final String[] name = new String[1];

        fixture.flow()
          .run("set up colony with a graveyard, an undertaker with a shovel and a victim", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              graveyard[0] = fixture.placeBuilding(ModBuildings.graveyard.get(), 1, NW);
              undertaker[0] = fixture.spawnCitizen(20, graveyard[0], null);
              victim[0] = fixture.spawnCitizen(Map.of(), null, null, new BlockPos(44, ColonyFixture.GROUND_Y, 50));
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(undertaker[0], victim[0]), () -> describeAll(undertaker[0], victim[0]))
          .run("give tools and items, force the resurrection roll to miss", () -> {
              give(undertaker[0], new ItemStack(Items.WOODEN_SHOVEL));
              give(victim[0], new ItemStack(Items.OAK_LOG, 7));
              name[0] = victim[0].getName();
              // Without research the undertaker still resurrects with about 3% (building level 1 x 0.005 + mana 20 x 0.00125); then the
              // citizen lives and is never buried (seen once in a full run). The roll is forced to miss; SRV-04b covers resurrection.
              setField(undertaker[0].getJob().getWorkerAI(), "random", new java.util.Random()
              {
                  @Override
                  public double nextDouble()
                  {
                      return 1.0;
                  }
              });
          })
          .awaitUntil("victim died and left a grave", 600, () -> {
              killStep(fixture, victim[0]);
              grave[0] = fixture.colony().getGraveManager().getGraves().keySet().stream().findFirst().orElse(null);
              return grave[0] != null;
          }, () -> "graves " + fixture.colony().getGraveManager().getGraves())
          .awaitUntil("the undertaker buried the citizen", 8000,
            () -> graveyard[0].getModule(BuildingModules.GRAVEYARD).hasRestingCitizen(Set.of(name[0])),
            () -> "grave block " + fixture.level().getBlockState(grave[0]) + ", " + ColonyFixture.describe(undertaker[0]))
          .check("the grave block is gone", () -> !(fixture.level().getBlockEntity(grave[0]) instanceof TileEntityGrave), () -> "still " + fixture.level().getBlockState(grave[0]))
          .awaitUntil("the dead citizen's items are in the graveyard", 1500, () -> fixture.countItems(graveyard[0], s -> s.is(Items.OAK_LOG)) == 7,
            () -> "logs in graveyard " + fixture.countItems(graveyard[0], s -> s.is(Items.OAK_LOG)) + ", undertaker " + count(undertaker[0], Items.OAK_LOG)
                    + ", " + ColonyFixture.describe(undertaker[0]))
          .succeed();
    }

    /**
     * SRV-04b Resurrection: with the resurrection research and a forced lucky roll, the undertaker brings the dead citizen back.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 15000, skyAccess = true)
    public void srv04b_resurrection(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final IBuilding[] graveyard = new IBuilding[1];
        final ICitizenData[] undertaker = new ICitizenData[1];
        final ICitizenData[] victim = new ICitizenData[1];
        final BlockPos[] grave = new BlockPos[1];
        final String[] name = new String[1];

        fixture.flow()
          .run("set up colony with a graveyard, an undertaker with a shovel and a victim", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              fixture.completeResearch(Identifier.parse("minecolonies:civilian/resurrectchance1"));
              graveyard[0] = fixture.placeBuilding(ModBuildings.graveyard.get(), 1, NW);
              undertaker[0] = fixture.spawnCitizen(20, graveyard[0], null);
              victim[0] = fixture.spawnCitizen(Map.of(), null, null, new BlockPos(44, ColonyFixture.GROUND_Y, 50));
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(undertaker[0], victim[0]), () -> describeAll(undertaker[0], victim[0]))
          .run("give the shovel, force the resurrection roll", () -> {
              give(undertaker[0], new ItemStack(Items.WOODEN_SHOVEL));
              name[0] = victim[0].getName();
              setField(undertaker[0].getJob().getWorkerAI(), "random", new java.util.Random()
              {
                  @Override
                  public double nextDouble()
                  {
                      return 0.0;
                  }
              });
          })
          .awaitUntil("victim died and left a grave", 600, () -> {
              killStep(fixture, victim[0]);
              grave[0] = fixture.colony().getGraveManager().getGraves().keySet().stream().findFirst().orElse(null);
              return grave[0] != null;
          }, () -> "graves " + fixture.colony().getGraveManager().getGraves())
          .awaitUntil("the dead citizen is alive again", 14000, () -> {
              for (final ICitizenData c : fixture.colony().getCitizenManager().getCitizens())
              {
                  if (c.getName().equals(name[0]) && c.getEntity().isPresent())
                  {
                      return true;
                  }
              }
              return false;
          }, () -> "grave block " + fixture.level().getBlockState(grave[0]) + ", " + ColonyFixture.describe(undertaker[0]))
          .check("not buried", () -> !graveyard[0].getModule(BuildingModules.GRAVEYARD).hasRestingCitizen(Set.of(name[0])), () -> "buried")
          .succeed();
    }

    /**
     * SRV-05 Mystical site: the mystical site happiness factor is neutral without a site, and grows with the highest site level.
     */
    @GameTest(environment = ColonyFixture.COLONY_ENVIRONMENT, structure = ColonyFixture.ARENA, maxTicks = 400, skyAccess = true)
    public void srv05_mysticalSite(final GameTestHelper helper)
    {
        final ColonyFixture fixture = ColonyFixture.create(helper);
        final ICitizenData[] citizens = new ICitizenData[2];
        final double[] happiness = new double[4];
        final double[] factor = new double[4];

        fixture.flow()
          .run("set up colony with two citizens", () -> {
              fixture.createColony();
              fixture.setMoveIn(false);
              citizens[0] = fixture.spawnCitizen(5, null, null);
              citizens[1] = fixture.spawnCitizen(5, null, null);
          })
          .awaitUntil("citizens have entities", 100, () -> allEntities(citizens), () -> describeAll(citizens))
          .run("no site", () -> {
              factor[0] = factor(citizens[0], HappinessConstants.MYSTICAL_SITE);
              happiness[0] = happiness(citizens[0]);
          })
          .run("level 1 site", () -> {
              fixture.placeBuilding(ModBuildings.mysticalSite.get(), 1, NW);
              factor[1] = factor(citizens[0], HappinessConstants.MYSTICAL_SITE);
              happiness[1] = happiness(citizens[0]);
          })
          .run("level 3 site", () -> {
              fixture.placeBuilding(ModBuildings.mysticalSite.get(), 3, NE);
              factor[2] = factor(citizens[0], HappinessConstants.MYSTICAL_SITE);
              happiness[2] = happiness(citizens[0]);
          })
          .run("level 5 site", () -> {
              fixture.placeBuilding(ModBuildings.mysticalSite.get(), 5, SW);
              factor[3] = factor(citizens[0], HappinessConstants.MYSTICAL_SITE);
              happiness[3] = happiness(citizens[0]);
          })
          .check("no bonus without a site or with a level 1 site (factor = max(1, level / 2))", () -> factor[0] == 1.0 && factor[1] == 1.0,
            () -> "factors " + java.util.Arrays.toString(factor))
          .check("level 3 and 5 sites give growing bonuses", () -> factor[2] == 1.5 && factor[3] == 2.5, () -> "factors " + java.util.Arrays.toString(factor))
          .check("total happiness grows with the site level", () -> happiness[2] > happiness[0] && happiness[3] > happiness[2],
            () -> "happiness " + java.util.Arrays.toString(happiness))
          .check("every citizen has the same colony wide bonus", () -> factor(citizens[1], HappinessConstants.MYSTICAL_SITE) == factor[3],
            () -> "second citizen " + factor(citizens[1], HappinessConstants.MYSTICAL_SITE))
          .succeed();
    }

    // =====================================================================================================================================
    // Helpers
    // =====================================================================================================================================

    private static boolean isWorking(final ICitizenData citizen)
    {
        final String state = aiState(citizen);
        return "WORK".equals(state) || "WORKING".equals(state);
    }

    private static float health(final ICitizenData citizen)
    {
        return citizen.getEntity().map(e -> e.getHealth()).orElse(-1f);
    }

    /**
     * Infects the citizen. {@code CitizenDiseaseHandler.setDisease} refuses while the colony has no more than {@code initialCitizenAmount}
     * (4) citizens, so the disease field is set directly.
     */
    private static void infect(final ICitizenData citizen, final String diseaseId)
    {
        final Disease disease = DiseasesListener.getDisease(Identifier.parse(diseaseId));
        if (disease == null)
        {
            throw new IllegalStateException("Unknown disease " + diseaseId);
        }
        if (!citizen.getCitizenDiseaseHandler().setDisease(disease))
        {
            setField(citizen.getCitizenDiseaseHandler(), "disease", disease);
        }
    }

    private static int totalCount(final ColonyFixture fixture, final IBuilding building, final Item item, final ICitizenData... citizens)
    {
        int total = fixture.countItems(building, s -> s.is(item));
        for (final ICitizenData c : citizens)
        {
            total += count(c, item);
        }
        return total;
    }

    /**
     * Items of the open Stack requests the citizen made through its work building.
     */
    private static Set<Item> requestedItems(final IBuilding building, final ICitizenData citizen)
    {
        final Set<Item> items = new HashSet<>();
        for (final IRequest<? extends Stack> request : building.getOpenRequestsOfType(citizen.getId(), TypeToken.of(Stack.class)))
        {
            items.add(request.getRequest().getStack().getItem());
        }
        return items;
    }

    private static double happiness(final ICitizenData citizen)
    {
        // The total is cached until a modifier changes; resetting the (fresh) homelessness day counter clears the cache.
        citizen.getCitizenHappinessHandler().resetModifier(HappinessConstants.HOMELESSNESS);
        return citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony(), citizen);
    }

    private static String factors(final ICitizenData[] citizens, final String modifier)
    {
        final StringBuilder text = new StringBuilder(modifier).append(':');
        for (final ICitizenData c : citizens)
        {
            text.append(' ').append(factor(c, modifier));
        }
        return text.toString();
    }

    /**
     * Total experience of a skill: experience of all levels below the current one plus the current experience.
     */
    private static double progress(final ICitizenData citizen, final Skill skill)
    {
        final int level = citizen.getCitizenSkillHandler().getLevel(skill);
        double total = citizen.getCitizenSkillHandler().getSkills().get(skill).getExperience();
        for (int l = 1; l < level; l++)
        {
            total += ExperienceUtils.getXPNeededForNextLevel(l);
        }
        return total;
    }

    private static String skillDelta(final ICitizenData citizen, final Map<Skill, Double> before)
    {
        final StringBuilder text = new StringBuilder();
        for (final Skill skill : Skill.values())
        {
            text.append(skill).append(' ').append(String.format("%+.2f", progress(citizen, skill) - before.get(skill))).append(", ");
        }
        return text.toString();
    }

    private static ICitizenData spawnChild(final ColonyFixture fixture, final IBuilding school)
    {
        final ICitizenData child = fixture.colony().getCitizenManager().createAndRegisterCivilianData();
        child.setIsChild(true);
        final Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        for (final Skill skill : Skill.values())
        {
            skills.put(skill, 1);
        }
        fixture.setSkills(child, skills);
        fixture.hire(child, school, ModJobs.pupil.get());
        fixture.colony().getCitizenManager().spawnOrCreateCivilian(child, fixture.level(), List.of(school.getPosition()), true);
        return child;
    }

    /**
     * One hit on the citizen (citizens cap the damage per hit, so this is repeated each tick until it dies).
     */
    private static void killStep(final ColonyFixture fixture, final ICitizenData citizen)
    {
        citizen.getEntity().ifPresent(entity -> {
            if (entity.isAlive())
            {
                entity.hurtServer(fixture.level(), fixture.level().damageSources().generic(), 1000f);
            }
        });
    }

    /**
     * A simple oak tree (trunk of 5 logs, leaves around the top) at a relative ground position.
     */
    private static void growTree(final GameTestHelper helper, final BlockPos base)
    {
        for (int dy = 0; dy < 5; dy++)
        {
            helper.setBlock(base.above(dy), Blocks.OAK_LOG);
        }
        for (int dx = -2; dx <= 2; dx++)
        {
            for (int dz = -2; dz <= 2; dz++)
            {
                for (int dy = 3; dy <= 5; dy++)
                {
                    final BlockPos pos = base.offset(dx, dy, dz);
                    if ((dx != 0 || dz != 0 || dy == 5) && Math.abs(dx) + Math.abs(dz) <= (dy == 5 ? 1 : 3))
                    {
                        helper.setBlock(pos, Blocks.OAK_LEAVES);
                    }
                }
            }
        }
    }

    private static int treesLeft(final GameTestHelper helper, final BlockPos[] trees)
    {
        int left = 0;
        for (final BlockPos tree : trees)
        {
            if (helper.getBlockState(tree.above()).is(Blocks.OAK_LOG))
            {
                left++;
            }
        }
        return left;
    }

    /**
     * The pathfinding course: a closed house with a door (west half, z 40-44), a platform up stairs (z 26-30), a ladder tower
     * (x 3-5, z 19-21, top at y 10), a 2-deep river along z 50-52 with a plank bridge at x 8 and a 1-deep ford at x 18.
     */
    private static void buildCourse(final GameTestHelper helper)
    {
        // House: oak plank walls x 4-8, z 40-44, y 5-7, roof y 8, door in the north wall at (6, 5, 40).
        for (int x = 4; x <= 8; x++)
        {
            for (int z = 40; z <= 44; z++)
            {
                for (int y = 5; y <= 8; y++)
                {
                    final boolean wall = x == 4 || x == 8 || z == 40 || z == 44 || y == 8;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
        helper.setBlock(new BlockPos(6, 5, 40), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(new BlockPos(6, 6, 40), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Platform: stone x 9-13, z 26-30, top at y 7 (stand at y 8); stairs up from the east: (16,5), (15,6), (14,7) ascending west.
        for (int x = 9; x <= 13; x++)
        {
            for (int z = 26; z <= 30; z++)
            {
                for (int y = 5; y <= 7; y++)
                {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        for (int step = 0; step < 3; step++)
        {
            final int x = 16 - step;
            final int y = 5 + step;
            for (int below = 5; below < y; below++)
            {
                helper.setBlock(new BlockPos(x, below, 28), Blocks.STONE);
            }
            helper.setBlock(new BlockPos(x, y, 28), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        }

        // Tower: stone pillar x 3-5, z 19-21, y 5-9 (stand on top at y 10), ladder on its east face at x 6, z 20.
        for (int x = 3; x <= 5; x++)
        {
            for (int z = 19; z <= 21; z++)
            {
                for (int y = 5; y <= 9; y++)
                {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        for (int y = 5; y <= 10; y++)
        {
            helper.setBlock(new BlockPos(6, y, 20), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
        // the top ladder block needs a wall: one more stone block above the pillar's east edge
        helper.setBlock(new BlockPos(5, 10, 20), Blocks.STONE);

        // River: water at y 3-4 for x 1-22, z 50-52; ford at x 18 (stone at y 3, water only at y 4); bridge of planks at y 5 over x 8.
        for (int x = 1; x <= 22; x++)
        {
            for (int z = 50; z <= 52; z++)
            {
                helper.setBlock(new BlockPos(x, 3, z), x == 18 ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 4, z), Blocks.WATER);
            }
        }
        for (int z = 49; z <= 53; z++)
        {
            helper.setBlock(new BlockPos(8, z == 49 || z == 53 ? 4 : 5, z), Blocks.OAK_PLANKS);
        }
    }

    /**
     * Sends the citizen to {@code absTarget} the way {@code /mc citizens walk} does: an AI event that keeps calling
     * {@code EntityNavigationUtils.walkToPos} until it returns true. Sets {@code arrived[0]} when it does.
     */
    private static void walkTo(final ColonyFixture fixture, final ICitizenData citizen, final BlockPos absTarget, final boolean[] arrived)
    {
        arrived[0] = false;
        final EntityCitizen entity = (EntityCitizen) citizen.getEntity().orElseThrow();
        entity.getCitizenAI().addTransition(new AIOneTimeEventTarget<IState>(() -> {
            if (!arrived[0] && EntityNavigationUtils.walkToPos(entity, absTarget, 1, true))
            {
                arrived[0] = true;
            }
            return entity.getCitizenAI().getState();
        })
        {
            @Override
            public boolean shouldRemove()
            {
                return arrived[0];
            }
        });
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static void give(final ICitizenData citizen, final ItemStack stack)
    {
        if (!InventoryUtils.addItemStackToItemHandler(citizen.getInventory(), stack))
        {
            throw new IllegalStateException("No room in the inventory of " + citizen.getName() + " for " + stack);
        }
    }

    private static int count(final ICitizenData citizen, final Item item)
    {
        return InventoryUtils.getItemCountInItemHandler(citizen.getInventory(), s -> s.is(item));
    }

    private static String aiState(final ICitizenData citizen)
    {
        return citizen.getEntity().map(e -> e instanceof final EntityCitizen ec ? String.valueOf(ec.getCitizenAI().getState()) : "?").orElse("<no entity>");
    }

    private static boolean isEating(final ICitizenData citizen)
    {
        return citizen.getEntity().map(e -> e instanceof final EntityCitizen ec
                                              && (ec.getCitizenAI().getState() == CitizenAIState.EATING
                                                    || ec.getCitizenAI().getState() instanceof EntityAIEatTask.EatingState)).orElse(false);
    }

    private static boolean allEntities(final ICitizenData... citizens)
    {
        for (final ICitizenData c : citizens)
        {
            if (c == null || c.getEntity().isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    private static String describeAll(final ICitizenData... citizens)
    {
        final StringBuilder text = new StringBuilder();
        for (final ICitizenData c : citizens)
        {
            text.append("\n  ").append(ColonyFixture.describe(c));
        }
        return text.toString();
    }

    private static List<BlockPos> bedOf(@Nullable final IBuilding home)
    {
        return home == null ? List.of() : home.getModule(BuildingModules.BED).getRegisteredBlocks();
    }

    private static String sleepInfo(final ICitizenData... citizens)
    {
        final StringBuilder text = new StringBuilder();
        for (final ICitizenData c : citizens)
        {
            text.append("\n  ").append(ColonyFixture.describe(c)).append(", asleep ").append(c.isAsleep()).append(", bed ").append(c.getBedPos().toShortString())
              .append(", home beds ").append(bedOf(c.getHomeBuilding()));
        }
        return text.toString();
    }

    private static String sleptDays(final ICitizenData... citizens)
    {
        final List<Integer> days = new ArrayList<>();
        for (final ICitizenData c : citizens)
        {
            days.add(((ITimeBasedHappinessModifier) c.getCitizenHappinessHandler().getModifier(HappinessConstants.SLEPTTONIGHT)).getDays());
        }
        return days.toString();
    }

    private static double factor(final ICitizenData citizen, final String modifier)
    {
        final IHappinessModifier m = citizen.getCitizenHappinessHandler().getModifier(modifier);
        return m == null ? Double.NaN : m.getFactor(citizen);
    }

    /**
     * The citizen's open interactions (server side), read from the protected {@code CitizenData.citizenChatOptions}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Component, IInteractionResponseHandler> chatOptions(final ICitizenData citizen)
    {
        try
        {
            final Field field = CitizenData.class.getDeclaredField("citizenChatOptions");
            field.setAccessible(true);
            return (Map<Component, IInteractionResponseHandler>) field.get(citizen);
        }
        catch (final ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static boolean hasInteraction(final ICitizenData citizen, final String translationKey)
    {
        for (final IInteractionResponseHandler handler : chatOptions(citizen).values())
        {
            if (handler.getInquiry().getContents() instanceof final TranslatableContents t && t.getKey().equals(translationKey))
            {
                return true;
            }
        }
        return false;
    }

    private static List<String> interactionKeys(final ICitizenData citizen)
    {
        final List<String> keys = new ArrayList<>();
        for (final IInteractionResponseHandler handler : chatOptions(citizen).values())
        {
            keys.add(handler.getInquiry().getContents() instanceof final TranslatableContents t ? t.getKey() : handler.getInquiry().getString());
        }
        return keys;
    }

    /**
     * The child AI of a child citizen's entity: found among the citizen AI's transitions (it registers them in its constructor).
     */
    private static EntityAICitizenChild childAI(final ICitizenData child)
    {
        final EntityCitizen entity = (EntityCitizen) child.getEntity().orElseThrow();
        final Object found = findReferenced(entity.getCitizenAI(), EntityAICitizenChild.class, 8, new HashSet<>());
        if (found == null)
        {
            throw new IllegalStateException("No EntityAICitizenChild found for " + child.getName());
        }
        return (EntityAICitizenChild) found;
    }

    /**
     * Searches the object graph (fields, collections, arrays, lambda captures) for an instance of {@code type}, up to {@code depth} levels.
     */
    @Nullable
    private static Object findReferenced(@Nullable final Object root, final Class<?> type, final int depth, final Set<Integer> seen)
    {
        if (root == null || !seen.add(System.identityHashCode(root)))
        {
            return null;
        }
        if (type.isInstance(root))
        {
            return root;
        }
        if (depth <= 0 || root instanceof String || root instanceof Number || root.getClass().isPrimitive())
        {
            return null;
        }
        if (root instanceof final Iterable<?> iterable)
        {
            for (final Object o : iterable)
            {
                final Object r = findReferenced(o, type, depth - 1, seen);
                if (r != null)
                {
                    return r;
                }
            }
            return null;
        }
        if (root instanceof final Map<?, ?> map)
        {
            return findReferenced(map.values(), type, depth, seen);
        }
        if (root.getClass().isArray())
        {
            if (root instanceof final Object[] array)
            {
                for (final Object o : array)
                {
                    final Object r = findReferenced(o, type, depth - 1, seen);
                    if (r != null)
                    {
                        return r;
                    }
                }
            }
            return null;
        }
        final String name = root.getClass().getName();
        if (!name.startsWith("com.minecolonies") && !name.contains("$$Lambda"))
        {
            return null;
        }
        for (Class<?> c = root.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for (final Field f : c.getDeclaredFields())
            {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive())
                {
                    continue;
                }
                try
                {
                    f.setAccessible(true);
                    final Object r = findReferenced(f.get(root), type, depth - 1, seen);
                    if (r != null)
                    {
                        return r;
                    }
                }
                catch (final RuntimeException | IllegalAccessException ignored)
                {
                    // inaccessible field (module system), skip
                }
            }
        }
        return null;
    }

    private static Field field(final Object target, final String name)
    {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass())
        {
            try
            {
                final Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            }
            catch (final NoSuchFieldException ignored)
            {
                // try the superclass
            }
        }
        throw new IllegalStateException("No field " + name + " in " + target.getClass());
    }

    private static void setField(final Object target, final String name, final Object value)
    {
        try
        {
            field(target, name).set(target, value);
        }
        catch (final IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static int getIntField(final Object target, final String name)
    {
        try
        {
            return field(target, name).getInt(target);
        }
        catch (final IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
