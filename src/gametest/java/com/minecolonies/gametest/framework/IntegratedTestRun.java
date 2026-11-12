package com.minecolonies.gametest.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Runs server game tests on any running server, in particular the integrated server of a game window (watch mode, UI tests), the same way
 * the headless {@link GameTestServer} does: the same test instances from the registry, the same batching (so colony tests still run one at
 * a time, {@link SerialBatches}), the vanilla {@link GameTestRunner} and test ticker, the same pass/fail tracking and log summary.
 * <p>
 * All methods must be called on the server thread.
 */
public final class IntegratedTestRun
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegratedTestRun.class);

    private final MinecraftServer      server;
    private final GameTestRunner       runner;
    private final MultipleTestTracker  tracker;
    private final long                 startNanos = System.nanoTime();
    private boolean                    reported;

    private IntegratedTestRun(final MinecraftServer server, final GameTestRunner runner)
    {
        this.server = server;
        this.runner = runner;
        this.tracker = new MultipleTestTracker(runner.getTestInfos());
        RerunTracking.install(tracker);
    }

    /**
     * Selects tests and starts them.
     *
     * @param selection     vanilla test selector ({@code minecolonies-gametest:*_col01_*}), or null for every test.
     * @param includeManual also run tests marked {@code manualOnly} (the UI setup tests; headless runs never pick them).
     * @param firstTestCorner north-west corner of the first test area; later areas follow on a grid, as on the headless server.
     * @param reportFile    JUnit-style report file, or null for log output only.
     * @throws IllegalArgumentException if the selector matches no test.
     */
    public static IntegratedTestRun start(
      final MinecraftServer server,
      @Nullable final String selection,
      final boolean includeManual,
      final BlockPos firstTestCorner,
      @Nullable final Path reportFile)
    {
        final Collection<Holder.Reference<GameTestInstance>> tests = (selection == null
                                                                         ? server.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE).listElements()
                                                                         : GameTestServer.getTestsForSelection(server.registryAccess(), selection))
          .filter(test -> includeManual || !test.value().manualOnly())
          .toList();
        if (tests.isEmpty())
        {
            throw new IllegalArgumentException("Test selection matcher (" + selection + ") found no tests");
        }

        if (reportFile != null)
        {
            try
            {
                Files.createDirectories(reportFile.toAbsolutePath().getParent());
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(reportFile.toFile()));
            }
            catch (final Exception e)
            {
                LOGGER.error("Could not create the test report {}", reportFile, e);
            }
        }

        // Same batching as GameTestServer: GameTestBatchFactoryMixin splits colony batches into one test each.
        final List<GameTestBatch> batches = GameTestBatchFactory.divideIntoBatches(tests, GameTestBatchFactory.DIRECT, server);
        final GameTestRunner runner = GameTestRunner.Builder.fromBatches(batches, server)
          .newStructureSpawner(new StructureGridSpawner(dimension -> firstTestCorner, 8, false))
          .build();
        final IntegratedTestRun run = new IntegratedTestRun(server, runner);
        LOGGER.info("MineColonies e2e: {} tests are now running at position {} on the integrated server", run.tracker.getTotalCount(), firstTestCorner.toShortString());
        runner.start();
        return run;
    }

    public boolean isDone()
    {
        return tracker.isDone();
    }

    public String progressBar()
    {
        return tracker.getProgressBar();
    }

    public int totalCount()
    {
        return tracker.getTotalCount();
    }

    public int failedRequiredCount()
    {
        return tracker.getFailedRequiredCount();
    }

    /**
     * @return the tests that have their area placed and are not finished yet, oldest first.
     */
    public List<GameTestInfo> runningTests()
    {
        final List<GameTestInfo> running = new ArrayList<>();
        for (final GameTestInfo info : runner.getTestInfos())
        {
            if (info.getTestBlockPos() != null && !info.isDone())
            {
                running.add(info);
            }
        }
        return running;
    }

    /**
     * @return the area of the first running test, if any.
     */
    public Optional<AABB> currentTestArea()
    {
        for (final GameTestInfo info : runningTests())
        {
            try
            {
                return Optional.of(info.getStructureBounds());
            }
            catch (final RuntimeException e)
            {
                // Structure not placed yet.
            }
        }
        return Optional.empty();
    }

    /**
     * @return the part of the first running test's area where something happens ({@link WatchFocus}), if any.
     */
    public Optional<WatchFocus.Focus> currentFocus()
    {
        for (final GameTestInfo info : runningTests())
        {
            try
            {
                return Optional.of(WatchFocus.of(info.getLevel(), info.getStructureBounds()));
            }
            catch (final RuntimeException e)
            {
                // Structure not placed yet.
            }
        }
        return Optional.empty();
    }

    /**
     * @return the id of the first running test, or "" if none.
     */
    public String currentTestId()
    {
        final List<GameTestInfo> running = runningTests();
        return running.isEmpty() ? "" : running.getFirst().id().toString();
    }

    /**
     * Stops the runner (also the tests still running) and clears the vanilla test ticker.
     */
    public void stop()
    {
        runner.stop();
        GameTestTicker.SINGLETON.clear();
    }

    /**
     * Writes the end-of-run summary to the log (the same lines as the headless server) and saves the report. Once only.
     *
     * @return the summary lines.
     */
    public List<String> report()
    {
        final List<String> lines = new ArrayList<>();
        lines.add(tracker.getProgressBar());
        lines.add(String.format("========= %d GAME TESTS COMPLETE IN %.1f s ======================", tracker.getTotalCount(), (System.nanoTime() - startNanos) / 1e9));
        if (tracker.hasFailedRequired())
        {
            lines.add(tracker.getFailedRequiredCount() + " required tests failed :(");
            tracker.getFailedRequired().forEach(info -> lines.add("   - " + info.id() + ": " + describeError(info)));
        }
        else
        {
            lines.add("All " + tracker.getTotalCount() + " required tests passed :)");
        }
        if (tracker.hasFailedOptional())
        {
            lines.add(tracker.getFailedOptionalCount() + " optional tests failed");
            tracker.getFailedOptional().forEach(info -> lines.add("   - " + info.id() + ": " + describeError(info)));
        }
        lines.add("====================================================");

        if (!reported)
        {
            reported = true;
            lines.forEach(line -> LOGGER.info("{}", line));
            GlobalTestReporter.finish();
        }
        return lines;
    }

    private static String describeError(final GameTestInfo info)
    {
        return info.getError() == null ? "?" : info.getError().getDescription().getString();
    }

    public MinecraftServer server()
    {
        return server;
    }
}
