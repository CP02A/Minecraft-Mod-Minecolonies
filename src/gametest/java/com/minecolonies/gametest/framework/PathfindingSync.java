package com.minecolonies.gametest.framework;

import com.minecolonies.core.entity.pathfinding.Pathfinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Makes MineColonies' background pathfinding deterministic in game ticks, without touching the pathfinding code.
 * <p>
 * Called at the end of every test-server tick. Every path job is submitted from the server thread to {@link Pathfinding#getExecutor()}, a
 * single-thread executor with a FIFO queue. We submit an empty marker job and wait for it: it can only finish after every job submitted
 * before it. So a path requested in tick N is always ready in tick N+1, on every machine.
 * <p>
 * Why a marker instead of comparing {@code getTaskCount()} and {@code getCompletedTaskCount()}: {@link ThreadPoolExecutor#getTaskCount()}
 * briefly misses a job that the worker has taken from the queue but not started yet, so the two counts can be equal while a job is still
 * pending. The marker has no such gap.
 */
public final class PathfindingSync
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PathfindingSync.class);

    /**
     * Safety net only: a path job is bounded by a node limit, so this never triggers in a healthy run. If it does, the run continues
     * (logged as an error) instead of hanging forever.
     */
    private static final long MAX_WAIT_SECONDS = 60;

    /**
     * Statistics for the end-of-run log line: ticks that ended with path jobs still pending, and the total time waited for them.
     */
    private static long ticks;
    private static long ticksWithPendingJobs;
    private static long nanosWaited;

    private PathfindingSync()
    {
    }

    /**
     * Blocks until every pathfinding job submitted so far has finished.
     */
    public static void awaitIdle()
    {
        final ThreadPoolExecutor executor = Pathfinding.getExecutor();
        if (executor.isShutdown())
        {
            return;
        }

        ticks++;
        final boolean pending = executor.getActiveCount() > 0 || !executor.getQueue().isEmpty();
        final long start = System.nanoTime();
        try
        {
            final Future<?> marker = executor.submit(() -> {});
            marker.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
            if (pending)
            {
                ticksWithPendingJobs++;
                nanosWaited += System.nanoTime() - start;
            }
        }
        catch (final TimeoutException e)
        {
            LOGGER.error("MineColonies pathfinding did not finish within {} s; continuing without waiting. Results of this tick may be timing dependent.",
              MAX_WAIT_SECONDS);
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (final Exception e)
        {
            LOGGER.error("Waiting for MineColonies pathfinding failed", e);
        }
    }

    /**
     * @return a one-line summary of how often the test server had to wait for pathfinding.
     */
    public static String statistics()
    {
        return String.format("%d ticks, %d ended with path jobs pending (waited %.1f ms in total)", ticks, ticksWithPendingJobs, nanosWaited / 1_000_000.0);
    }

    /**
     * @return the thread that runs MineColonies' path jobs (creates it if no path was requested yet).
     */
    public static Thread pathfindingThread() throws Exception
    {
        return Pathfinding.getExecutor().submit(Thread::currentThread).get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
    }
}
