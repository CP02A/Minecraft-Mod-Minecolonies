package com.minecolonies.gametest.client;

import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
import com.ldtteam.structurize.component.CapturedBlock;
import com.ldtteam.structurize.util.ScanToolData;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.research.IGlobalResearch;
import com.minecolonies.api.research.IGlobalResearchTree;
import com.minecolonies.core.client.gui.citizen.JobWindowCitizen;
import com.minecolonies.core.client.gui.citizen.MainWindowCitizen;
import com.minecolonies.core.client.gui.huts.WindowHutBuilderModule;
import com.minecolonies.core.client.gui.WindowPostBoxMain;
import com.minecolonies.core.client.gui.WindowResearchTree;
import com.minecolonies.core.client.gui.containers.WindowBrewingstandCrafting;
import com.minecolonies.core.client.gui.modules.building.UniversityModuleWindow;
import com.minecolonies.core.client.gui.townhall.WindowMainPage;
import com.minecolonies.core.client.gui.townhall.WindowStatsPage;
import com.minecolonies.core.colony.buildings.moduleviews.UniversityResearchModuleView;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingTownHall;
import com.minecolonies.core.colony.buildings.workerbuildings.PostBox;
import com.minecolonies.gametest.framework.E2EMode;
import com.minecolonies.gametest.framework.IntegratedTestRun;
import com.minecolonies.gametest.mixin.HudAccessor;
import com.minecolonies.gametest.ui.UiBridge;
import com.minecolonies.gametest.ui.UiColonySetup;
import com.mojang.blaze3d.platform.SDLEventHandler;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
import com.minecolonies.api.crafting.IGenericRecipe;
import com.minecolonies.api.crafting.registry.CraftingType;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.core.client.render.CitizenArmorModel;
import com.minecolonies.core.colony.crafting.RecipeAnalyzer;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingSawmill;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.fabric.CrafterRecipeSync;
import com.minecolonies.gametest.framework.TestShapedRecipe;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.ldtteam.structurize.api.constants.Constants.GROUNDSTYLE_RELATIVE;
import static com.minecolonies.gametest.client.UiTestSupport.assertThat;
import static com.minecolonies.gametest.client.UiTestSupport.assertTranslated;
import static com.minecolonies.gametest.client.UiTestSupport.closeScreen;
import static com.minecolonies.gametest.client.UiTestSupport.open;
import static com.minecolonies.gametest.client.UiTestSupport.screenshot;
import static com.minecolonies.gametest.client.UiTestSupport.textOf;
import static com.minecolonies.gametest.client.UiTestSupport.texts;

/**
 * UI tests (docs/e2e-tests/ui.md): open MineColonies windows in a real game window on a colony prepared by {@link UiColonySetup}, take
 * screenshots ({@code build/e2e-screenshots/<name>.png}) and check what the windows show against the colony data on the server.
 * <p>
 * Run with {@code ./gradlew runClientGameTest} (only some: {@code -Pe2eUiFilter=UI-01,UI-03}). Real keyboard and mouse input is ignored the
 * whole time. Each UI-n test runs on its own: a failure is recorded and the next test still runs; at the end the run fails if a required
 * test failed. The report is written to {@code build/e2e-ui-report.xml}.
 */
public class UiClientGameTest implements FabricClientGameTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UiClientGameTest.class);

    /**
     * North-west corner of the UI colony's arena.
     */
    private static final BlockPos ARENA_CORNER = new BlockPos(64, 4, 64);

    private record Result(String id, String title, boolean required, @Nullable Throwable error, List<Path> screenshots, long millis)
    {
    }

    @FunctionalInterface
    private interface Body
    {
        void run(List<Path> screenshots) throws Exception;
    }

    private final List<Result> results = new ArrayList<>();

    @Override
    public void runTest(final ClientGameTestContext context)
    {
        if (E2EMode.isWatch())
        {
            return;
        }

        runCase("UI-08", "Human input is ignored", true, shots -> checkInputBlocked());
        runCase("UI-14", "Config screen: client settings", true, shots -> ConfigScreenUiTests.clientSettings(context, shots));

        UiBridge.reset();
        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            final TestServerContext server = world.getServer();
            server.runCommand("gamemode creative @a");
            final IntegratedTestRun run = server.computeOnServer(s -> IntegratedTestRun.start(s, UiColonySetup.TEST_SELECTOR, true, ARENA_CORNER, null));

            final boolean ready = waitForColony(context, server, run);
            final UiBridge.Colony colony = UiBridge.colony;
            runCase("UI-09", "Brewing teaching computes the potion", true, shots -> brewingOutput(context));
            runCase("UI-12", "JEI draws blocks in 3D", true, shots -> jeiBlocks(context, shots));
            runCase("UI-15", "Config screen: server settings", true, shots -> ConfigScreenUiTests.serverSettings(context, server, shots));
            runCase("UI-16", "Structurize highlight tips", true, shots -> highlightTips(context, server, shots));
            if (ready && colony != null)
            {
                runCase("UI-01", "Town hall window", true, shots -> townHall(context, server, colony, shots));
                runCase("UI-02", "Builder's hut window", true, shots -> builderHut(context, server, colony, shots));
                runCase("UI-03", "Citizen window", true, shots -> citizen(context, server, colony, shots));
                runCase("UI-04", "Build tool window", true, shots -> buildTool(context, colony, shots));
                runCase("UI-05", "Research tree", true, shots -> researchTree(context, server, colony, shots));
                runCase("UI-06", "Post box request window", true, shots -> postBox(context, colony, shots));
                runCase("UI-07", "Warehouse window", true, shots -> warehouse(context, server, colony, shots));
                runCase("UI-11", "Child citizen proportions", true, shots -> childCitizen(context, server, colony, shots));
            }
            else
            {
                final String reason = "UI colony setup did not finish: " + (UiBridge.setupError != null ? UiBridge.setupError : server.computeOnServer(s -> run.report()));
                for (final String id : List.of("UI-01", "UI-02", "UI-03", "UI-04", "UI-05", "UI-06", "UI-07", "UI-11"))
                {
                    if (E2EMode.uiTestSelected(id))
                    {
                        results.add(new Result(id, "(setup)", true, new AssertionError(reason), List.of(), 0));
                    }
                }
            }

            runCase("UI-10", "Spear item renders in 3D", true, shots -> spear(context, server, shots));

            closeScreen(context);
            UiBridge.clientDone = true;
            server.waitFor(s -> run.isDone(), 400);
            server.runOnServer(s -> run.report());
        }

        // UI-13 needs a dedicated server: recipes really go over the network (encoded), and JEI has no integrated server to read them from.
        if (E2EMode.uiTestSelected("UI-13"))
        {
            try (TestDedicatedServerContext dedicated = context.worldBuilder().createServer();
                 TestDedicatedServerConnection connection = dedicated.connect())
            {
                runCase("UI-13", "JEI crafter recipes from synced recipes", true, shots -> jeiSyncedRecipes(context, dedicated, connection));
            }
        }

        reportAndFail();
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Waits until the setup test published the colony, puts the player in front of the town hall and waits until the client has the
     * colony's data (colony view with the buildings and citizens).
     */
    private static boolean waitForColony(final ClientGameTestContext context, final TestServerContext server, final IntegratedTestRun run)
    {
        server.waitFor(s -> UiBridge.colony != null || UiBridge.setupError != null || run.isDone(), 2400);
        final UiBridge.Colony colony = UiBridge.colony;
        if (colony == null)
        {
            return false;
        }

        // 14 blocks north of the town hall, between the huts, looking south at it.
        final BlockPos th = colony.townHall();
        server.runCommand(String.format(Locale.ROOT, "tp @a %d.5 %d %d.5 0 10", th.getX(), th.getY(), th.getZ() - 14));
        try
        {
            context.waitFor(mc -> {
                final IColonyView view = colonyView(mc, colony);
                return view != null
                         && IColonyManager.getInstance().getBuildingView(mc.level.dimension(), colony.townHall()) != null
                         && IColonyManager.getInstance().getBuildingView(mc.level.dimension(), colony.builder()) != null
                         && IColonyManager.getInstance().getBuildingView(mc.level.dimension(), colony.warehouse()) != null
                         && IColonyManager.getInstance().getBuildingView(mc.level.dimension(), colony.university()) != null
                         && IColonyManager.getInstance().getBuildingView(mc.level.dimension(), colony.postBox()) != null
                         && view.getCitizen(colony.workerId()) != null
                         && view.getCitizen(colony.secondId()) != null;
            }, 600);
        }
        catch (final AssertionError e)
        {
            UiBridge.setupError = "the client never received the colony's data (colony view, buildings, citizens): " + e.getMessage();
            return false;
        }
        // Let the world around render before the first screenshot.
        context.waitTicks(40);
        return true;
    }

    @Nullable
    private static IColonyView colonyView(final Minecraft mc, final UiBridge.Colony colony)
    {
        return mc.level == null ? null : IColonyManager.getInstance().getColonyView(colony.colonyId(), mc.level.dimension());
    }

    private static IBuildingView buildingView(final Minecraft mc, final BlockPos pos)
    {
        final IBuildingView view = IColonyManager.getInstance().getBuildingView(mc.level.dimension(), pos);
        if (view == null)
        {
            throw new AssertionError("No building view at " + pos);
        }
        return view;
    }

    private static IColony serverColony(final net.minecraft.server.MinecraftServer server, final UiBridge.Colony colony)
    {
        return IColonyManager.getInstance().getColonyByWorld(colony.colonyId(), server.getLevel(Level.OVERWORLD));
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // UI-01 .. UI-08
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * UI-01: town hall main page and statistics page. The statistics page shows the colony's real citizen count.
     */
    private static void townHall(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow main = open(context, mc -> new WindowMainPage((BuildingTownHall.View) buildingView(mc, colony.townHall())));
        shots.add(screenshot(context, "ui01-town-hall-main"));
        final List<String> mainTexts = texts(context, main);
        final String colonyName = server.computeOnServer(s -> serverColony(s, colony).getName());
        assertThat(String.join(" | ", mainTexts).contains(colonyName) || textContainsLevel(textOf(context, main, "name"), 1),
          "Town hall main page shows neither the colony name '" + colonyName + "' nor 'level 1'. Texts: " + mainTexts);
        assertTranslated("Town hall main page", mainTexts);

        final BOWindow stats = open(context, mc -> new WindowStatsPage((BuildingTownHall.View) buildingView(mc, colony.townHall())));
        shots.add(screenshot(context, "ui01-town-hall-stats"));
        final int citizens = server.computeOnServer(s -> serverColony(s, colony).getCitizenManager().getCitizens().size());
        final String total = textOf(context, stats, "totalCitizens");
        assertThat(total != null && total.matches("(?s).*\\b" + citizens + "\\s*/\\s*\\d+.*"),
          "Total citizens label should show " + citizens + "/<max>, shows: " + total);
        assertTranslated("Town hall statistics page", texts(context, stats));
        closeScreen(context);
    }

    /**
     * UI-02: builder's hut window: title with the building name and level, the hired builder in the worker list.
     */
    private static void builderHut(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow window = open(context, mc -> new WindowHutBuilderModule((BuildingBuilder.View) buildingView(mc, colony.builder()), false));
        shots.add(screenshot(context, "ui02-builder-hut"));
        final List<String> texts = texts(context, window);

        final int level = server.computeOnServer(s -> serverColony(s, colony).getServerBuildingManager().getBuilding(colony.builder()).getBuildingLevel());
        final String displayKey = server.computeOnServer(s -> serverColony(s, colony).getServerBuildingManager().getBuilding(colony.builder()).getBuildingDisplayName());
        final String workerName = server.computeOnServer(s -> serverColony(s, colony).getCitizenManager().getCivilian(colony.workerId()).getName());
        final String expectedName = context.computeOnClient(mc -> Component.translatable(displayKey).getString());

        final String title = textOf(context, window, "name");
        assertThat(title != null && title.contains(expectedName) && textContainsLevel(title, level),
          "Title should be '" + expectedName + " " + level + "', is '" + title + "'");
        assertThat(texts.stream().anyMatch(t -> t.contains(workerName)), "Worker list should show the builder '" + workerName + "'. Texts: " + texts);
        assertTranslated("Builder's hut window", texts);
        closeScreen(context);
    }

    /**
     * UI-03: citizen window (main page and job page): name, every skill level and the job match the citizen on the server.
     */
    private static void citizen(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow main = open(context, mc -> new MainWindowCitizen(colonyView(mc, colony).getCitizen(colony.workerId())));
        shots.add(screenshot(context, "ui03-citizen-main"));

        final String name = server.computeOnServer(s -> serverColony(s, colony).getCitizenManager().getCivilian(colony.workerId()).getName());
        final String shownName = textOf(context, main, "name");
        assertThat(name.equals(shownName), "Citizen name should be '" + name + "', window shows '" + shownName + "'");

        final List<String> wrong = new ArrayList<>();
        for (final Skill skill : Skill.values())
        {
            final int expected = server.computeOnServer(s -> {
                final ICitizenData data = serverColony(s, colony).getCitizenManager().getCivilian(colony.workerId());
                return data.getCitizenSkillHandler().getLevel(skill);
            });
            final String shown = textOf(context, main, skill.name().toLowerCase(Locale.US));
            if (!String.valueOf(expected).equals(shown))
            {
                wrong.add(skill + " expected " + expected + " shown " + shown);
            }
        }
        assertThat(wrong.isEmpty(), "Skill levels differ from the server: " + wrong);
        assertTranslated("Citizen main page", texts(context, main));

        final BOWindow job = open(context, mc -> new JobWindowCitizen(colonyView(mc, colony).getCitizen(colony.workerId())));
        shots.add(screenshot(context, "ui03-citizen-job"));
        final String jobKey = server.computeOnServer(s -> serverColony(s, colony).getCitizenManager().getCivilian(colony.workerId()).getJob().getJobRegistryEntry().getTranslationKey());
        final String jobName = context.computeOnClient(mc -> Component.translatable(jobKey).getString());
        final List<String> jobTexts = texts(context, job);
        assertThat(jobTexts.stream().anyMatch(t -> t.contains(jobName)), "Job page should name the job '" + jobName + "'. Texts: " + jobTexts);
        assertTranslated("Citizen job page", jobTexts);
        closeScreen(context);
    }

    /**
     * UI-04: build tool window: opens on the selected structure pack and lists its categories.
     */
    private static void buildTool(final ClientGameTestContext context, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BlockPos pos = colony.townHall().offset(0, 0, -20);
        final BOWindow window = open(context, mc -> new WindowExtendedBuildTool(pos, GROUNDSTYLE_RELATIVE, mc.level.registryAccess()));
        // Categories are loaded in the background.
        context.waitTicks(40);
        shots.add(screenshot(context, "ui04-build-tool"));
        final List<String> texts = texts(context, window);
        final String pack = context.computeOnClient(mc -> com.ldtteam.structurize.storage.StructurePacks.getSelectedPack() == null
                                                            ? null : com.ldtteam.structurize.storage.StructurePacks.getSelectedPack().getName());
        assertThat(pack != null, "No structure pack selected on the client");
        assertThat(texts.stream().anyMatch(t -> t.contains(pack)), "Build tool should show the selected pack '" + pack + "'. Texts: " + texts);
        // Categories are icon buttons (no text) in the view "categories" of structurize:gui/windowbuildtool.xml.
        final int categories = context.computeOnClient(mc -> {
            if (window.findPaneByID("categories") instanceof final com.ldtteam.blockui.views.View view)
            {
                return (int) view.getChildren().stream().filter(com.ldtteam.blockui.Pane::isVisible).count();
            }
            return 0;
        });
        assertThat(categories >= 3, "Build tool should show the pack's category buttons, shows " + categories + ". Texts: " + texts);
        assertTranslated("Build tool window", texts);
        closeScreen(context);
    }

    /**
     * UI-05: university window and the research tree of the civilian branch: every first-tier research of the branch is shown.
     */
    private static void researchTree(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow university = open(context, mc -> new UniversityModuleWindow(buildingView(mc, colony.university()).getModuleViewByType(UniversityResearchModuleView.class)));
        shots.add(screenshot(context, "ui05-university"));
        assertTranslated("University window", texts(context, university));

        final BOWindow tree = open(context, mc -> {
            final IBuildingView view = buildingView(mc, colony.university());
            final UniversityModuleWindow last = new UniversityModuleWindow(view.getModuleViewByType(UniversityResearchModuleView.class));
            return new WindowResearchTree(UiBridge.RESEARCH_BRANCH, view, last);
        });
        shots.add(screenshot(context, "ui05-research-tree"));

        final List<String> nameKeys = server.computeOnServer(s -> {
            final List<String> keys = new ArrayList<>();
            for (final Identifier id : IGlobalResearchTree.getInstance().getPrimaryResearch(UiBridge.RESEARCH_BRANCH))
            {
                final IGlobalResearch research = IGlobalResearchTree.getInstance().getResearch(UiBridge.RESEARCH_BRANCH, id);
                keys.add(research.getName().getKey());
            }
            return keys;
        });
        final List<String> names = context.computeOnClient(mc -> nameKeys.stream().map(key -> Component.translatable(key).getString()).toList());
        final List<String> texts = texts(context, tree);
        final List<String> missing = names.stream().filter(n -> texts.stream().noneMatch(t -> t.contains(n))).toList();
        assertThat(!names.isEmpty() && missing.isEmpty(), "Research tree should show the branch's first-tier research " + names + "; missing " + missing + ". Texts: " + texts);
        assertTranslated("Research tree", texts);
        closeScreen(context);
    }

    /**
     * UI-06: post box window (request items from the colony): lists requestable items with names.
     */
    private static void postBox(final ClientGameTestContext context, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow window = open(context, mc -> new WindowPostBoxMain((PostBox.View) buildingView(mc, colony.postBox())));
        context.waitTicks(20);
        shots.add(screenshot(context, "ui06-postbox"));
        final List<String> texts = texts(context, window);
        final int rows = context.computeOnClient(mc -> {
            final com.ldtteam.blockui.Pane list = window.findPaneByID("resources");
            if (list instanceof final com.ldtteam.blockui.views.View view && !view.getChildren().isEmpty()
                  && view.getChildren().getFirst() instanceof final com.ldtteam.blockui.views.View container)
            {
                return container.getChildren().size();
            }
            return 0;
        });
        assertThat(rows > 0, "Post box should list requestable items, shows none. Texts: " + texts);
        assertTranslated("Post box window", texts);
        closeScreen(context);
    }

    /**
     * UI-07: warehouse window: title with name and level.
     */
    private static void warehouse(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BOWindow window = open(context, mc -> buildingView(mc, colony.warehouse()).getWindow());
        shots.add(screenshot(context, "ui07-warehouse"));
        final int level = server.computeOnServer(s -> serverColony(s, colony).getServerBuildingManager().getBuilding(colony.warehouse()).getBuildingLevel());
        final String displayKey = server.computeOnServer(s -> serverColony(s, colony).getServerBuildingManager().getBuilding(colony.warehouse()).getBuildingDisplayName());
        final String expectedName = context.computeOnClient(mc -> Component.translatable(displayKey).getString());
        final String title = textOf(context, window, "name");
        assertThat(title != null && title.contains(expectedName) && textContainsLevel(title, level),
          "Title should be '" + expectedName + " " + level + "', is '" + title + "'");
        assertTranslated("Warehouse window", texts(context, window));
        closeScreen(context);
    }

    /**
     * UI-09: the brewing teaching window ({@link WindowBrewingstandCrafting}) computes the brewed potion on the client from the synchronized
     * brewing recipes (E2E-CRF01-2: the output was always empty, so no brewing recipe could be taught). Water bottle + nether wart must
     * give an awkward potion, a reagent that doesn't brew must give nothing.
     */
    private static void brewingOutput(final ClientGameTestContext context)
    {
        final ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        final ItemStack awkward = context.computeOnClient(mc -> WindowBrewingstandCrafting.getBrewingOutput(mc.level, water, new ItemStack(Items.NETHER_WART)));
        assertThat(awkward.is(Items.POTION) && awkward.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.AWKWARD),
          "Water bottle + nether wart should brew an awkward potion, got " + awkward + " " + awkward.getComponents());
        final ItemStack none = context.computeOnClient(mc -> WindowBrewingstandCrafting.getBrewingOutput(mc.level, water, new ItemStack(Items.DIRT)));
        assertThat(none.isEmpty(), "Water bottle + dirt should brew nothing, got " + none);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // UI-10 .. UI-13: client rendering and JEI support
    // --------------------------------------------------------------------------------------------------------------------------------------

    /**
     * UI-10: the spear item uses its special 3D model renderer in hand and in item frames, and the flat sprite in the GUI. Builds a small
     * scene away from the colony (a wall with the spear in an item frame, an armor stand holding it, the player holding it) and takes
     * screenshots in first and third person.
     */
    private static void spear(final ClientGameTestContext context, final TestServerContext server, final List<Path> shots)
    {
        // Scene in the air west of the colony arena: floor y 19, wall z 31, player at (26.5, 20, 24.5) looking south.
        server.runCommand("fill 18 19 22 34 19 31 minecraft:smooth_stone");
        server.runCommand("fill 18 20 31 34 25 31 minecraft:white_concrete");
        server.runCommand("summon item_frame 27 21 30 {Facing:2b,Fixed:1b,Item:{id:\"minecolonies:spear\",count:1}}");
        server.runCommand("summon armor_stand 24.5 20 28.5 {ShowArms:1b,NoGravity:1b,Rotation:[180f,0f],equipment:{mainhand:{id:\"minecolonies:spear\",count:1}}}");
        server.runCommand("item replace entity @a weapon.mainhand with minecolonies:spear");
        server.runCommand("tp @a 26.5 20 24.5 0 15");
        context.waitTicks(60);

        // Item model check: 3D (the spear model is about two blocks long) in hand and in item frames, the flat sprite (one block) in the GUI.
        final double[] sizes = context.computeOnClient(mc -> new double[] {
          itemModelSize(mc, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
          itemModelSize(mc, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
          itemModelSize(mc, ItemDisplayContext.FIXED),
          itemModelSize(mc, ItemDisplayContext.GUI)});
        LOGGER.info("UI-10 spear item model sizes (third person, first person, item frame, GUI): {}", java.util.Arrays.toString(sizes));

        shots.add(screenshot(context, "ui10-spear-first-person"));
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        context.waitTicks(5);
        shots.add(screenshot(context, "ui10-spear-third-person"));
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");

        assertThat(sizes[0] > 1.3 && sizes[1] > 1.3 && sizes[2] > 1.3,
          "The spear should use its 3D model in hand and in item frames (longest side > 1.3 blocks), sizes " + java.util.Arrays.toString(sizes));
        assertThat(sizes[3] < 1.1, "The spear should use its flat sprite in the GUI, size " + sizes[3]);
    }

    /**
     * @return the longest side of the spear's item model bounding box in this display context, in blocks.
     */
    private static double itemModelSize(final Minecraft mc, final ItemDisplayContext displayContext)
    {
        final ItemStackRenderState state = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(state, new ItemStack(ModItems.spear), displayContext, mc.level, mc.player, 0);
        final net.minecraft.world.phys.AABB box = state.getModelBoundingBox();
        return Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
    }

    /**
     * UI-11: a child citizen is drawn with child proportions (head scaled by 0.75, body by 0.5, like 1.21.1's HumanoidModel#young), an adult
     * of the same gender with full size. Spawns both in front of the player, frozen, with a sword in hand (held items follow the scaled
     * arm), takes a screenshot and checks the posed citizen models and the citizen armor model (armor must follow the child scaling too).
     */
    private static void childCitizen(final ClientGameTestContext context, final TestServerContext server, final UiBridge.Colony colony, final List<Path> shots)
    {
        final BlockPos th = colony.townHall();
        // The player stands 14 blocks north of the town hall looking south; both stand 4 blocks in front of him.
        final BlockPos childPos = th.offset(-1, 0, -10);
        final BlockPos adultPos = th.offset(1, 0, -10);
        final int[] citizenIds = server.computeOnServer(s -> {
            final IColony serverColony = serverColony(s, colony);
            final ICitizenData child = serverColony.getCitizenManager().createAndRegisterCivilianData();
            child.setIsChild(true);
            child.setGenderAndGenerateName(false);
            serverColony.getCitizenManager().spawnOrCreateCivilian(child, s.overworld(), List.of(childPos), true);
            final ICitizenData adult = serverColony.getCitizenManager().createAndRegisterCivilianData();
            adult.setGenderAndGenerateName(false);
            serverColony.getCitizenManager().spawnOrCreateCivilian(adult, s.overworld(), List.of(adultPos), true);
            return new int[] {child.getId(), adult.getId()};
        });
        try
        {
            server.waitFor(s -> citizenEntity(s, colony, citizenIds[0]) != null && citizenEntity(s, colony, citizenIds[1]) != null, 200);
            final int[] entityIds = server.computeOnServer(s -> new int[] {
              poseCitizen(citizenEntity(s, colony, citizenIds[0]), childPos), poseCitizen(citizenEntity(s, colony, citizenIds[1]), adultPos)});
            context.waitFor(mc -> mc.level.getEntity(entityIds[0]) instanceof final AbstractEntityCitizen child && child.isBaby()
                                    && child.getCitizenDataView() != null
                                    && mc.level.getEntity(entityIds[1]) instanceof final AbstractEntityCitizen adult && adult.getCitizenDataView() != null, 200);
            context.waitTicks(20);
            server.runOnServer(s -> {
                poseCitizen(citizenEntity(s, colony, citizenIds[0]), childPos);
                poseCitizen(citizenEntity(s, colony, citizenIds[1]), adultPos);
            });
            context.waitTicks(5);
            shots.add(screenshot(context, "ui11-child-citizen"));

            final float[] child = context.computeOnClient(mc -> posedScales(mc, entityIds[0]));
            final float[] adult = context.computeOnClient(mc -> posedScales(mc, entityIds[1]));
            LOGGER.info("UI-11 posed part scales (head, body, arm, leg, armor head, armor body): child {}, adult {}",
              java.util.Arrays.toString(child), java.util.Arrays.toString(adult));
            assertThat(near(child[0], 0.75F) && near(child[1], 0.5F) && near(child[2], 0.5F) && near(child[3], 0.5F),
              "Child citizen should be drawn with head 0.75 and body 0.5, model part scales " + java.util.Arrays.toString(child));
            assertThat(near(child[4], 0.75F) && near(child[5], 0.5F),
              "The child's armor model should follow the child proportions, scales " + java.util.Arrays.toString(child));
            assertThat(near(adult[0], 1F) && near(adult[1], 1F) && near(adult[4], 1F) && near(adult[5], 1F),
              "Adult citizen should be drawn full size, scales " + java.util.Arrays.toString(adult));
        }
        finally
        {
            server.runOnServer(s -> {
                final IColony serverColony = serverColony(s, colony);
                for (final int id : citizenIds)
                {
                    final ICitizenData data = serverColony.getCitizenManager().getCivilian(id);
                    if (data != null)
                    {
                        data.getEntity().ifPresent(e -> e.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED));
                        serverColony.getCitizenManager().removeCivilian(data);
                    }
                }
            });
        }
    }

    @Nullable
    private static AbstractEntityCitizen citizenEntity(final net.minecraft.server.MinecraftServer server, final UiBridge.Colony colony, final int citizenId)
    {
        final ICitizenData data = serverColony(server, colony).getCitizenManager().getCivilian(citizenId);
        return data == null ? null : data.getEntity().orElse(null);
    }

    /**
     * Freezes a citizen at a position, looking north (at the player), with an iron sword in hand.
     *
     * @return the entity id.
     */
    private static int poseCitizen(final AbstractEntityCitizen citizen, final BlockPos pos)
    {
        citizen.setNoAi(true);
        citizen.getNavigation().stop();
        citizen.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180F, 0F);
        citizen.setYHeadRot(180F);
        citizen.setYBodyRot(180F);
        citizen.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        return citizen.getId();
    }

    /**
     * Poses the citizen's model and an armor model the way rendering does and returns the part scales: head, body, right arm, right leg of
     * the citizen model, head and body of the armor model.
     */
    private static float[] posedScales(final Minecraft mc, final int entityId)
    {
        final net.minecraft.world.entity.Entity entity = mc.level.getEntity(entityId);
        final CitizenRenderState state = (CitizenRenderState) mc.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
        final CitizenModel<?> model = state.citizenModel;
        model.setupAnim(state);
        final float[] result = {model.head.xScale, model.body.xScale, model.rightArm.xScale, model.rightLeg.xScale, 0, 0};
        final CitizenArmorModel armor = new CitizenArmorModel(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_ARMOR.head()));
        armor.setupAnim(state);
        result[4] = armor.head.xScale;
        result[5] = armor.body.xScale;
        return result;
    }

    private static boolean near(final float value, final float expected)
    {
        return Math.abs(value - expected) < 0.001F;
    }

    /**
     * UI-12: blocks in JEI recipe categories are drawn as turned 3D block models of the exact block state (1.21.1 behavior), not as item
     * icons: three composting barrels with different fill levels, a furnace and a grown wheat crop (whose item would be seeds).
     */
    private static void jeiBlocks(final ClientGameTestContext context, final List<Path> shots) throws IOException
    {
        context.runOnClient(mc -> mc.gui.setScreen(new BlockRenderTestScreen()));
        context.waitFor(mc -> mc.gui.screen() instanceof BlockRenderTestScreen, 100);
        context.waitTicks(10);
        final Path shot = screenshot(context, "ui12-jei-blocks");
        shots.add(shot);
        final int guiWidth = context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
        closeScreen(context);

        // Check the pixels: each block covers its anchor, and the blocks of one draw are each drawn with their own state (the empty and
        // the finished barrel look different at the top; one shared picture would make them equal).
        final BufferedImage image = ImageIO.read(shot.toFile());
        final double scale = (double) image.getWidth() / guiWidth;
        int x = BlockRenderTestScreen.FIRST_X;
        final List<Integer> barrelX = new ArrayList<>();
        for (int i = 0; i < BlockRenderTestScreen.barrels().size(); i++)
        {
            barrelX.add(x);
            assertThat(coverage(image, scale, x - 8, BlockRenderTestScreen.BARREL_Y - 8, 16, 16) > 0.9,
              "Barrel " + i + " is not drawn at its anchor (" + x + ", " + BlockRenderTestScreen.BARREL_Y + ")");
            x += BlockRenderTestScreen.STEP_X;
        }
        x = BlockRenderTestScreen.FIRST_X;
        for (int i = 0; i < BlockRenderTestScreen.intermediates().size(); i++)
        {
            assertThat(coverage(image, scale, x - 10, BlockRenderTestScreen.INTERMEDIATE_Y - 10, 20, 20) > 0.1,
              "Intermediate block " + i + " is not drawn at its anchor (" + x + ", " + BlockRenderTestScreen.INTERMEDIATE_Y + ")");
            x += BlockRenderTestScreen.STEP_X;
        }
        final double topDifference = difference(image, scale, barrelX.getFirst() - 10, barrelX.getLast() - 10, BlockRenderTestScreen.BARREL_Y - 16, 20, 8);
        LOGGER.info("UI-12 mean color difference of the empty and the finished barrel's top: {}", topDifference);
        assertThat(topDifference > 10, "The empty and the finished composting barrel look the same at the top (difference " + topDifference + ")");
    }

    /**
     * @return the share of pixels in the GUI rectangle that are not the test screen's background.
     */
    private static double coverage(final BufferedImage image, final double scale, final int x, final int y, final int w, final int h)
    {
        int drawn = 0;
        int all = 0;
        for (int px = (int) (x * scale); px < (int) ((x + w) * scale); px++)
        {
            for (int py = (int) (y * scale); py < (int) ((y + h) * scale); py++)
            {
                all++;
                if ((image.getRGB(px, py) & 0xFFFFFF) != (BlockRenderTestScreen.BACKGROUND & 0xFFFFFF))
                {
                    drawn++;
                }
            }
        }
        return all == 0 ? 0 : (double) drawn / all;
    }

    /**
     * @return the mean color difference per channel between two GUI rectangles of the same size.
     */
    private static double difference(final BufferedImage image, final double scale, final int x1, final int x2, final int y, final int w, final int h)
    {
        long sum = 0;
        int count = 0;
        for (int dx = 0; dx < (int) (w * scale); dx++)
        {
            for (int dy = 0; dy < (int) (h * scale); dy++)
            {
                final int a = image.getRGB((int) (x1 * scale) + dx, (int) (y * scale) + dy);
                final int b = image.getRGB((int) (x2 * scale) + dx, (int) (y * scale) + dy);
                for (int shift = 0; shift <= 16; shift += 8)
                {
                    sum += Math.abs(((a >> shift) & 0xFF) - ((b >> shift) & 0xFF));
                }
                count += 3;
            }
        }
        return count == 0 ? 0 : (double) sum / count;
    }

    /**
     * UI-13: on a dedicated server JEI's crafter categories get their vanilla-derived recipes from the recipes the server sends to the client
     * (there is no integrated server): the Fabric recipe sync (vanilla and MineColonies serializers) plus MineColonies' own crafter recipe
     * payload for other mods' serializers ({@code CrafterRecipeSync}). Runs against a real dedicated server. Builds the recipe map from the
     * client's recipes like JEIPlugin does without an integrated server and compares it with the map built from the server's recipe manager
     * (the singleplayer path): the same number of recipes for every crafting type. The test mod's recipe with its own serializer
     * ({@link TestShapedRecipe}, not in the Fabric recipe sync) must be among them and be a sawmill recipe. Clients without MineColonies'
     * payload channel (a fake player here, like a vanilla client) are not sent the payload.
     */
    private static void jeiSyncedRecipes(final ClientGameTestContext context, final TestDedicatedServerContext server,
                                         final TestDedicatedServerConnection connection)
    {
        connection.waitForChunksDownload();
        final ResourceKey<Recipe<?>> testId = ResourceKey.create(Registries.RECIPE, TestShapedRecipe.RECIPE_ID);
        // The payload is sent on login, before MineColonies' custom recipe packets; wait until the test recipe arrived.
        context.waitFor(mc -> CrafterRecipeSync.withReceivedRecipes(List.of()).stream().anyMatch(h -> h.id().equals(testId)), 600);

        final Map<String, Integer> onServer = server.computeOnServer(s -> recipeCounts(RecipeAnalyzer.buildVanillaRecipesMap(s.getRecipeManager(), s.overworld())));
        final Map<String, Integer> onClient = context.computeOnClient(mc -> recipeCounts(RecipeAnalyzer.buildVanillaRecipesMap(clientRecipeManager(mc), mc.level)));
        final int fabricSynced = context.computeOnClient(mc -> mc.level.recipeAccess().getSynchronizedRecipes().recipes().size());
        final int received = context.computeOnClient(mc -> CrafterRecipeSync.receivedCount());
        LOGGER.info("UI-13 vanilla crafter recipes per crafting type: server {}, client {} (Fabric-synced recipes {}, MineColonies payload {})",
          onServer, onClient, fabricSynced, received);

        final boolean integrated = context.computeOnClient(mc -> mc.getSingleplayerServer() != null);
        assertThat(!integrated, "UI-13 must run against a dedicated server");
        for (final String type : List.of("minecolonies:smallcrafting", "minecolonies:largecrafting", "minecolonies:smelting"))
        {
            assertThat(onServer.getOrDefault(type, 0) > 0, "The server should have recipes for " + type + ": " + onServer);
        }
        assertThat(onServer.equals(onClient), "Recipes from the client's recipes differ from the server's: server " + onServer + ", client " + onClient);

        // The test mod's recipe: not in the Fabric recipe sync, only in MineColonies' payload; decoded as the test mod's recipe class.
        final boolean inFabricSync = context.computeOnClient(mc -> mc.level.recipeAccess().getSynchronizedRecipes().recipes().stream().anyMatch(h -> h.id().equals(testId)));
        assertThat(!inFabricSync, "The test recipe should not come through the Fabric recipe sync");
        final String receivedClass = context.computeOnClient(mc -> CrafterRecipeSync.withReceivedRecipes(List.of()).stream().filter(h -> h.id().equals(testId))
          .map(h -> h.value().getClass().getSimpleName()).findFirst().orElse("none"));
        assertThat(receivedClass.equals(TestShapedRecipe.class.getSimpleName()), "The test recipe should be decoded as TestShapedRecipe, is " + receivedClass);
        final boolean noResend = context.computeOnClient(mc -> CrafterRecipeSync.withReceivedRecipes(List.of()).stream().noneMatch(h -> h.value().getSerializer() == ShapedRecipe.SERIALIZER));
        assertThat(noResend, "Recipes of the Fabric-synced serializers should not be sent again in MineColonies' payload");

        // The sawmill (a MineColonies crafter) accepts it, on the client as on the server.
        final boolean serverSawmill = server.computeOnServer(s -> sawmillHasTestRecipe(s.getRecipeManager(), s.overworld()));
        final boolean clientSawmill = context.computeOnClient(mc -> sawmillHasTestRecipe(clientRecipeManager(mc), mc.level));
        assertThat(serverSawmill, "The server's sawmill recipes should contain the test recipe (7 spruce planks)");
        assertThat(clientSawmill, "The sawmill recipes built on the client should contain the test recipe (7 spruce planks)");

        // Isolation: only clients with MineColonies' payload channel get it. The real client has it; a fake player (no channels, like a
        // vanilla client) gets nothing.
        final boolean realPlayer = server.computeOnServer(s -> CrafterRecipeSync.shouldSend(s.getPlayerList().getPlayers().getFirst()));
        final boolean fakeCanSend = server.computeOnServer(s -> ServerPlayNetworking.canSend(FakePlayer.get(s.overworld()), CrafterRecipeSync.Payload.TYPE));
        final int toFake = server.computeOnServer(s -> CrafterRecipeSync.sendTo(FakePlayer.get(s.overworld())));
        assertThat(realPlayer, "The MineColonies client should get the crafter recipe payload");
        assertThat(!fakeCanSend && toFake == 0, "A client without MineColonies' payload channel must not be sent the payload (canSend " + fakeCanSend + ", sent " + toFake + ")");
    }

    private static net.minecraft.world.item.crafting.RecipeManager clientRecipeManager(final Minecraft mc)
    {
        return RecipeAnalyzer.createRecipeManager(CrafterRecipeSync.withReceivedRecipes(mc.level.recipeAccess().getSynchronizedRecipes().recipes()),
          mc.level.registryAccess());
    }

    private static boolean sawmillHasTestRecipe(final net.minecraft.world.item.crafting.RecipeManager manager, final Level level)
    {
        final Map<CraftingType, List<IGenericRecipe>> vanilla = RecipeAnalyzer.buildVanillaRecipesMap(manager, level);
        return RecipeAnalyzer.findRecipes(vanilla, new BuildingSawmill.CraftingModule(ModJobs.sawmill.get()), level).stream()
                 .anyMatch(r -> r.getPrimaryOutput().is(Items.SPRUCE_PLANKS) && r.getPrimaryOutput().getCount() == 7);
    }

    private static Map<String, Integer> recipeCounts(final Map<CraftingType, List<IGenericRecipe>> recipes)
    {
        final Map<String, Integer> counts = new java.util.TreeMap<>();
        recipes.forEach((type, list) -> counts.put(String.valueOf(MinecoloniesAPIProxy.getInstance().getCraftingTypeRegistry().getKey(type)), list.size()));
        return counts;
    }

    /**
     * UI-16: the selected item name above the hotbar shows Structurize's highlight tips (NeoForge {@code IItemExtension#getHighlightTip}):
     * the scan tool adds its current slot ("&lt;name&gt; - 1: tower"), the tag substitution item adds the absorbed block. When the scan tool's
     * slot changes while it stays selected, the name shows again with the new slot.
     */
    private static void highlightTips(final ClientGameTestContext context, final TestServerContext server, final List<Path> shots)
    {
        closeScreen(context);
        context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(8));
        server.runOnServer(s -> {
            final ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            final ItemStack scan = new ItemStack(com.ldtteam.structurize.items.ModItems.scanTool.get());
            new ScanToolData(List.of(ScanToolData.Slot.EMPTY, ScanToolData.Slot.EMPTY.withName("tower")), 1, null, null).writeToItemStack(scan);
            final ItemStack tag = new ItemStack(com.ldtteam.structurize.items.ModItems.blockTagSubstitution.get());
            new CapturedBlock(Blocks.OAK_PLANKS.defaultBlockState(), null, s.registryAccess(), new ItemStack(Items.OAK_PLANKS)).writeToItemStack(tag);
            player.getInventory().setItem(0, scan);
            player.getInventory().setItem(1, tag);
            player.getInventory().setItem(8, ItemStack.EMPTY);
        });
        context.waitFor(mc -> mc.player.getInventory().getItem(0).is(com.ldtteam.structurize.items.ModItems.scanTool.get())
                                && mc.player.getInventory().getItem(1).is(com.ldtteam.structurize.items.ModItems.blockTagSubstitution.get()), 200);
        context.waitTicks(2);

        // Scan tool: name + current slot.
        UiTestSupport.lastSelectedItemName = null;
        context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
        context.waitTicks(5);
        final String scanName = context.computeOnClient(mc -> mc.player.getInventory().getItem(0).getHoverName().getString());
        final String scanShown = UiTestSupport.lastSelectedItemName;
        assertThat((scanName + " - 1: tower").equals(scanShown), "Scan tool should show '" + scanName + " - 1: tower', shows '" + scanShown + "'");
        shots.add(screenshot(context, "ui16-scan-tool-highlight"));

        // Same item, other slot: the name must show again (NeoForge compares the highlight tips, not only the hover name).
        context.waitFor(mc -> ((HudAccessor) mc.gui.hud).minecoloniesGametest$getToolHighlightTimer() == 0, 600);
        server.runOnServer(s -> {
            final ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            final ItemStack scan = player.getInventory().getItem(0).copy();
            ScanToolData.updateItemStack(scan, data -> data.moveTo(2));
            player.getInventory().setItem(0, scan);
        });
        context.waitFor(mc -> ScanToolData.readFromItemStack(mc.player.getInventory().getItem(0)).currentSlotId() == 2, 200);
        UiTestSupport.lastSelectedItemName = null;
        context.waitTicks(3);
        final int timer = context.computeOnClient(mc -> ((HudAccessor) mc.gui.hud).minecoloniesGametest$getToolHighlightTimer());
        final String slotShown = UiTestSupport.lastSelectedItemName;
        assertThat(timer > 0, "Scan tool name should show again after its slot changed (timer " + timer + ")");
        assertThat((scanName + " - 2").equals(slotShown), "Scan tool should show '" + scanName + " - 2', shows '" + slotShown + "'");

        // Tag substitution: name + absorbed block.
        UiTestSupport.lastSelectedItemName = null;
        context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(1));
        context.waitTicks(5);
        final String tagName = context.computeOnClient(mc -> mc.player.getInventory().getItem(1).getHoverName().getString());
        final String planks = context.computeOnClient(mc -> new ItemStack(Items.OAK_PLANKS).getHoverName().getString());
        final String tagShown = UiTestSupport.lastSelectedItemName;
        assertThat((tagName + " - " + planks).equals(tagShown), "Tag substitution should show '" + tagName + " - " + planks + "', shows '" + tagShown + "'");
        shots.add(screenshot(context, "ui16-tag-substitution-highlight"));

        context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(8));
        server.runOnServer(s -> {
            final ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(1, ItemStack.EMPTY);
        });
    }

    /**
     * UI-08: real keyboard and mouse input can't reach the game: both the test mod's {@code SDLEventHandlerMixin} and Fabric's are applied
     * to every input handler of {@link SDLEventHandler}.
     */
    private static void checkInputBlocked()
    {
        final Set<String> mixins = new HashSet<>();
        for (final Method method : SDLEventHandler.class.getDeclaredMethods())
        {
            final MixinMerged merged = method.getAnnotation(MixinMerged.class);
            if (merged != null)
            {
                mixins.add(merged.mixin());
            }
        }
        assertThat(E2EMode.isClientTestRun(), "Not a client game test run (fabric.client.gametest not set)");
        assertThat(mixins.contains("com.minecolonies.gametest.mixin.SDLEventHandlerMixin"),
          "The test mod's input blocker (SDLEventHandlerMixin) is not applied to SDLEventHandler. Applied: " + mixins);
        assertThat(mixins.contains("net.fabricmc.fabric.mixin.client.gametest.input.SDLEventHandlerMixin"),
          "Fabric's client game test input blocker is not applied to SDLEventHandler. Applied: " + mixins);
    }

    // --------------------------------------------------------------------------------------------------------------------------------------
    // Running and reporting
    // --------------------------------------------------------------------------------------------------------------------------------------

    private static boolean textContainsLevel(@Nullable final String text, final int level)
    {
        return text != null && text.matches("(?s).*\\b" + level + "\\b.*");
    }

    private void runCase(final String id, final String title, final boolean required, final Body body)
    {
        if (!E2EMode.uiTestSelected(id))
        {
            return;
        }
        LOGGER.info("UI test {} {}: start", id, title);
        final long start = System.nanoTime();
        final List<Path> shots = new ArrayList<>();
        Throwable error = null;
        try
        {
            body.run(shots);
        }
        catch (final Throwable e)
        {
            error = e;
        }
        final long millis = (System.nanoTime() - start) / 1_000_000;
        results.add(new Result(id, title, required, error, shots, millis));
        if (error == null)
        {
            LOGGER.info("UI test {} {}: passed ({} ms), screenshots {}", id, title, millis, shots);
        }
        else
        {
            LOGGER.error("UI test {} {}: FAILED{} ({} ms), screenshots {}", id, title, required ? "" : " (not required)", millis, shots, error);
        }
    }

    private void reportAndFail()
    {
        final List<String> lines = new ArrayList<>();
        lines.add("========= " + results.size() + " UI TESTS COMPLETE ======================");
        int failedRequired = 0;
        for (final Result result : results)
        {
            final String status = result.error() == null ? "passed" : result.required() ? "FAILED" : "failed (not required)";
            if (result.error() != null && result.required())
            {
                failedRequired++;
            }
            lines.add(String.format("   %s %-28s %-22s %s", result.id(), result.title(), status, result.error() == null ? "" : result.error().getMessage()));
        }
        lines.add(failedRequired == 0 ? "All required UI tests passed :)" : failedRequired + " required UI tests failed :(");
        lines.add("Screenshots: " + E2EMode.screenshotDir());
        lines.add("====================================================");
        lines.forEach(line -> LOGGER.info("{}", line));
        writeJUnitReport();

        if (failedRequired > 0)
        {
            throw new AssertionError(String.join("\n", lines));
        }
    }

    private void writeJUnitReport()
    {
        final Path file = E2EMode.uiReport();
        if (file == null)
        {
            return;
        }
        final StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        final long failures = results.stream().filter(r -> r.error() != null).count();
        xml.append(String.format(Locale.ROOT, "<testsuite name=\"MineColonies UI tests\" tests=\"%d\" failures=\"%d\">%n", results.size(), failures));
        for (final Result result : results)
        {
            xml.append(String.format(Locale.ROOT, "  <testcase name=\"%s %s\" time=\"%.3f\">%n", escape(result.id()), escape(result.title()), result.millis() / 1000.0));
            if (result.error() != null)
            {
                xml.append(String.format("    <failure message=\"%s\">%s</failure>%n", escape(String.valueOf(result.error().getMessage())),
                  result.required() ? "required" : "not required"));
            }
            for (final Path shot : result.screenshots())
            {
                xml.append("    <system-out>screenshot: ").append(escape(shot.toString())).append("</system-out>\n");
            }
            xml.append("  </testcase>\n");
        }
        xml.append("</testsuite>\n");
        try
        {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, xml.toString(), StandardCharsets.UTF_8);
        }
        catch (final IOException e)
        {
            LOGGER.error("Could not write the UI test report {}", file, e);
        }
    }

    private static String escape(final String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
