package com.minecolonies.gametest.client;

import com.minecolonies.gametest.framework.E2EMode;
import com.minecolonies.gametest.framework.IntegratedTestRun;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Watch mode ({@code ./gradlew runGameTest -Pe2eWatch}): runs the server game tests in a real game window so a person can watch citizens
 * work (docs/e2e-tests/framework.md, "Watch mode").
 * <p>
 * Opens a flat singleplayer world, starts the selected server game tests on its integrated server through {@link IntegratedTestRun} (same
 * test instances, environments, serial colony batches, pathfinding wait and test config as the headless run), and moves the camera
 * (a spectator player) above the test that is running. Ticks run at normal speed (20 per second; {@code -Pe2eWatchSpeed=2} for double).
 * When all tests are done the run ends with the same summary as the headless run and fails if a required test failed.
 * <p>
 * Real keyboard and mouse input is ignored (Fabric's client game test framework and {@code SDLEventHandlerMixin}), and the player is a
 * spectator, so the person watching can't change the world. Without {@code -Pe2eWatch} this test does nothing.
 */
public class WatchClientGameTest implements FabricClientGameTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchClientGameTest.class);

    /**
     * North-west corner of the first test area. Near the world origin (the headless server uses a random spot up to 15 million blocks out;
     * that far out the client renders with visible jitter). Tests don't depend on their position.
     */
    private static final BlockPos FIRST_TEST_CORNER = new BlockPos(64, 4, 64);

    /**
     * Ticks between camera updates.
     */
    private static final int CAMERA_INTERVAL = 2;

    @Override
    public void runTest(final ClientGameTestContext context)
    {
        if (!E2EMode.isWatch())
        {
            return;
        }

        final List<String> summary;
        final boolean failed;
        try (TestSingleplayerContext world = context.worldBuilder().create())
        {
            final TestServerContext server = world.getServer();
            server.runCommand("gamemode spectator @a");
            server.runCommand(String.format(Locale.ROOT, "tick rate %.1f", 20 * E2EMode.watchSpeed()));

            final IntegratedTestRun[] run = new IntegratedTestRun[1];
            server.runOnServer(s -> run[0] = IntegratedTestRun.start(s, E2EMode.watchFilter(), false, FIRST_TEST_CORNER, E2EMode.watchReport()));
            server.runCommand("tellraw @a \"MineColonies e2e watch mode: keyboard and mouse are disabled, the camera follows the running test.\"");

            String lastTest = null;
            int ticks = 0;
            int screenshotAt = -1;
            while (!server.computeOnServer(s -> run[0].isDone()))
            {
                context.waitTick();
                ticks++;
                if (ticks == screenshotAt && lastTest != null)
                {
                    // One picture per test, 2 seconds after the camera arrived: build/e2e-screenshots/watch-<test path>.png.
                    UiTestSupport.screenshot(context, "watch-" + lastTest.substring(lastTest.indexOf(':') + 1));
                }
                if (ticks % CAMERA_INTERVAL != 0)
                {
                    continue;
                }

                final String current = server.computeOnServer(s -> run[0].currentTestId());
                if (!current.isEmpty() && !current.equals(lastTest))
                {
                    final Optional<AABB> area = server.computeOnServer(s -> run[0].currentTestArea());
                    if (area.isPresent())
                    {
                        lastTest = current;
                        server.runCommand(cameraCommand(area.get()));
                        screenshotAt = ticks + 40;
                        LOGGER.info("MineColonies e2e watch: now showing {}", current);
                    }
                }
                if (ticks % 200 == 0)
                {
                    final String progress = server.computeOnServer(s -> run[0].progressBar());
                    LOGGER.info("MineColonies e2e watch: {}", progress);
                }
            }

            summary = server.computeOnServer(s -> run[0].report());
            failed = server.computeOnServer(s -> run[0].failedRequiredCount() > 0);
            server.runCommand("tick rate 20");
        }

        if (failed)
        {
            throw new AssertionError("Watch mode: required game tests failed\n" + String.join("\n", summary));
        }
    }

    /**
     * Teleports the spectator to a corner above the test area, looking at its center.
     */
    private static String cameraCommand(final AABB box)
    {
        final double width = Math.max(box.getXsize(), box.getZsize());
        final double targetX = box.getCenter().x;
        final double targetY = box.minY + Math.min(6, box.getYsize() / 2);
        final double targetZ = box.getCenter().z;
        final double eyeX = box.minX - width * 0.15;
        final double eyeZ = box.minZ - width * 0.15;
        final double eyeY = targetY + Math.max(8, width * 0.6);

        final double dx = targetX - eyeX;
        final double dy = targetY - eyeY;
        final double dz = targetZ - eyeZ;
        final double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        final double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        // The player's eyes are 1.62 above its feet.
        return String.format(Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", eyeX, eyeY - 1.62, eyeZ, yaw, pitch);
    }
}
