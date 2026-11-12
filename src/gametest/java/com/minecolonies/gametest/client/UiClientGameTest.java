package com.minecolonies.gametest.client;

import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
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
import com.minecolonies.gametest.ui.UiBridge;
import com.minecolonies.gametest.ui.UiColonySetup;
import com.mojang.blaze3d.platform.SDLEventHandler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

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
        runCase("UI-10", "Config screen: client settings", true, shots -> ConfigScreenUiTests.clientSettings(context, shots));

        UiBridge.reset();
        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            final TestServerContext server = world.getServer();
            server.runCommand("gamemode creative @a");
            final IntegratedTestRun run = server.computeOnServer(s -> IntegratedTestRun.start(s, UiColonySetup.TEST_SELECTOR, true, ARENA_CORNER, null));

            final boolean ready = waitForColony(context, server, run);
            final UiBridge.Colony colony = UiBridge.colony;
            runCase("UI-09", "Brewing teaching computes the potion", true, shots -> brewingOutput(context));
            runCase("UI-11", "Config screen: server settings", true, shots -> ConfigScreenUiTests.serverSettings(context, server, shots));
            if (ready && colony != null)
            {
                runCase("UI-01", "Town hall window", true, shots -> townHall(context, server, colony, shots));
                runCase("UI-02", "Builder's hut window", true, shots -> builderHut(context, server, colony, shots));
                runCase("UI-03", "Citizen window", true, shots -> citizen(context, server, colony, shots));
                runCase("UI-04", "Build tool window", true, shots -> buildTool(context, colony, shots));
                runCase("UI-05", "Research tree", true, shots -> researchTree(context, server, colony, shots));
                runCase("UI-06", "Post box request window", true, shots -> postBox(context, colony, shots));
                runCase("UI-07", "Warehouse window", true, shots -> warehouse(context, server, colony, shots));
            }
            else
            {
                final String reason = "UI colony setup did not finish: " + (UiBridge.setupError != null ? UiBridge.setupError : server.computeOnServer(s -> run.report()));
                for (final String id : List.of("UI-01", "UI-02", "UI-03", "UI-04", "UI-05", "UI-06", "UI-07"))
                {
                    if (E2EMode.uiTestSelected(id))
                    {
                        results.add(new Result(id, "(setup)", true, new AssertionError(reason), List.of(), 0));
                    }
                }
            }

            closeScreen(context);
            UiBridge.clientDone = true;
            server.waitFor(s -> run.isDone(), 400);
            server.runOnServer(s -> run.report());
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
