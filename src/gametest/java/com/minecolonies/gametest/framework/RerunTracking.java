package com.minecolonies.gametest.framework;

import com.minecolonies.gametest.mixin.MultipleTestTrackerAccessor;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.MultipleTestTracker;

/**
 * Makes {@code maxAttempts}/{@code requiredSuccesses} work on the test server.
 * <p>
 * Vanilla schedules a re-run as a copy of the test that runs after all batches, but the server's tracker only knows the tests it started
 * with: when the last original test ends, the tracker counts everything as done, the server stops, and the re-run never runs (the log says
 * "Starting re-run of tests: ..." and the run ends with the first failure). This listener puts every re-run copy into the tracker in place
 * of the attempt it repeats, so the server waits for it, and only the last attempt counts as passed or failed.
 */
public final class RerunTracking implements GameTestListener
{
    private final MultipleTestTracker tracker;

    private RerunTracking(final MultipleTestTracker tracker)
    {
        this.tracker = tracker;
    }

    /**
     * Installs the re-run handling on the tracker that decides when the test run is over (headless server and watch mode).
     */
    public static void install(final MultipleTestTracker tracker)
    {
        tracker.addListener(new RerunTracking(tracker));
    }

    @Override
    public void testAddedForRerun(final GameTestInfo original, final GameTestInfo copy, final GameTestRunner runner)
    {
        ((MultipleTestTrackerAccessor) tracker).minecoloniesGametest$getTests().remove(original);
        // Also gives the copy this listener (the tracker adds all its listeners), so a re-run of the re-run is handled too.
        tracker.addTestToTrack(copy);
    }

    @Override
    public void testStructureLoaded(final GameTestInfo testInfo)
    {
    }

    @Override
    public void testPassed(final GameTestInfo testInfo, final GameTestRunner runner)
    {
    }

    @Override
    public void testFailed(final GameTestInfo testInfo, final GameTestRunner runner)
    {
    }
}
