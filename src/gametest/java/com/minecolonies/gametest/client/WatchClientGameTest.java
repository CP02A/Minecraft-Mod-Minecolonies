package com.minecolonies.gametest.client;

import com.minecolonies.gametest.framework.E2EMode;
import com.minecolonies.gametest.framework.IntegratedTestRun;
import com.minecolonies.gametest.framework.WatchFocus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Watch mode ({@code ./gradlew runGameTest -Pe2eWatch}): runs the server game tests in a real game window so a person can watch citizens
 * work (docs/e2e-tests/framework.md, "Watch mode").
 * <p>
 * Opens a flat singleplayer world, starts the selected server game tests on its integrated server through {@link IntegratedTestRun} (same
 * test instances, environments, serial colony batches, pathfinding wait and test config as the headless run), and steers the camera (a
 * spectator player) itself: from above and at an angle, framing the part of the running test where something happens ({@link WatchFocus}:
 * town hall, citizens, their huts, other mobs; the whole area of a small test, as a close-up), and gliding after it while citizens move.
 * Ticks run at normal speed (20 per second; {@code -Pe2eWatchSpeed=2} for double). When all tests are done the run ends with the same
 * summary as the headless run and fails if a required test failed.
 * <p>
 * Real keyboard and mouse input is ignored (Fabric's client game test framework and {@code SDLEventHandlerMixin}), and the player is a
 * spectator, so the person watching can't change the world or move the camera. Without {@code -Pe2eWatch} this test does nothing.
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
     * Ticks between two updates of the camera target (the focus box is computed on the server).
     */
    private static final int FOCUS_INTERVAL = 10;

    /**
     * Camera direction: looking north-west (yaw 135) and down at 50 degrees.
     */
    private static final float YAW   = 135f;
    private static final float PITCH = 50f;

    /**
     * Part of the field of view the focus box may fill (the rest is margin).
     */
    private static final double FRAME_MARGIN = 0.95;

    /**
     * Part of the remaining way the camera moves each tick towards its target (smooth following). When a new test starts, or the target is
     * further than {@link #SNAP_DISTANCE}, it jumps.
     */
    private static final double FOLLOW = 0.06;
    private static final double SNAP_DISTANCE = 40;

    /**
     * Screenshots of each test, in ticks after it started: build/e2e-screenshots/watch-<test path>.png (early, so short tests get one
     * too) and -2.png.
     */
    private static final int[] SCREENSHOT_TICKS = {25, 1200};

    /**
     * During the first ticks of a test (while it sets up its colony) the camera jumps instead of gliding.
     */
    private static final int SETUP_TICKS = 20;

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
            // No FOV change from flying and speed (a flying spectator would see up to 1.5 times wider): the framing uses the FOV option.
            context.runOnClient(mc -> mc.options.fovEffectScale().set(0.0));
            server.runCommand(String.format(Locale.ROOT, "tick rate %.1f", 20 * E2EMode.watchSpeed()));

            final IntegratedTestRun[] run = new IntegratedTestRun[1];
            server.runOnServer(s -> run[0] = IntegratedTestRun.start(s, E2EMode.watchFilter(), false, FIRST_TEST_CORNER, E2EMode.watchReport()));
            server.runCommand("tellraw @a \"MineColonies e2e watch mode: keyboard and mouse are disabled, the camera follows the running test.\"");

            String lastTest = null;
            int testTicks = 0;
            int ticks = 0;
            Vec3 target = null;
            boolean snap = false;
            AABB lastFocus = null;
            while (!server.computeOnServer(s -> run[0].isDone()))
            {
                if (ticks % FOCUS_INTERVAL == 0)
                {
                    final String current = server.computeOnServer(s -> run[0].currentTestId());
                    final Optional<WatchFocus.Focus> focus = server.computeOnServer(s -> run[0].currentFocus());
                    if (!current.isEmpty() && focus.isPresent())
                    {
                        if (!current.equals(lastTest))
                        {
                            lastTest = current;
                            testTicks = 0;
                            snap = true;
                            LOGGER.info("MineColonies e2e watch: now showing {}", current);
                        }
                        lastFocus = focus.get().bounds();
                        target = context.computeOnClient(mc -> cameraPosition(mc, focus.get()));
                        if (snap)
                        {
                            LOGGER.info("MineColonies e2e watch: camera {} for focus {}", target, focus.get().bounds());
                        }
                    }
                }
                if (target != null)
                {
                    final Vec3 goal = target;
                    final boolean jump = snap || testTicks <= SETUP_TICKS;
                    context.runOnClient(mc -> moveCamera(mc.player, goal, jump));
                    snap = false;
                }

                context.waitTick();
                ticks++;
                testTicks++;
                for (int i = 0; i < SCREENSHOT_TICKS.length; i++)
                {
                    if (testTicks == SCREENSHOT_TICKS[i] && lastTest != null)
                    {
                        // Without the chat and hotbar, which would cover part of the colony.
                        context.runOnClient(mc -> setHudHidden(mc, true));
                        UiTestSupport.screenshot(context, "watch-" + lastTest.substring(lastTest.indexOf(':') + 1) + (i == 0 ? "" : "-" + (i + 1)));
                        context.runOnClient(mc -> setHudHidden(mc, false));
                        final Vec3 eyes = context.computeOnClient(mc -> mc.player.getEyePosition());
                        LOGGER.info("MineColonies e2e watch: screenshot of {} with focus {}, camera at {}, target {}", lastTest, lastFocus, eyes, target);
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
     * Where the camera (the player's eyes) must be to see every part of the focus, looking along {@link #YAW} and {@link #PITCH}: aimed
     * at the middle of the parts as seen from that direction, and just far enough back that every corner of every part is inside the
     * view (with a small margin).
     */
    private static Vec3 cameraPosition(final Minecraft mc, final WatchFocus.Focus focus)
    {
        final double halfFovY = Math.toRadians(mc.options.fov().get() / 2.0);
        final double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        final double tanY = Math.tan(halfFovY) * FRAME_MARGIN;
        final double tanX = Math.tan(halfFovY) * aspect * FRAME_MARGIN;

        final Vec3 forward = Vec3.directionFromRotation(PITCH, YAW);
        final Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        final Vec3 up = right.cross(forward);

        final List<Vec3> corners = new ArrayList<>();
        for (final AABB part : focus.parts())
        {
            for (final double x : new double[] {part.minX, part.maxX})
            {
                for (final double y : new double[] {part.minY, part.maxY})
                {
                    for (final double z : new double[] {part.minZ, part.maxZ})
                    {
                        corners.add(new Vec3(x, y, z));
                    }
                }
            }
        }

        // Aim at the middle of what the camera sees, sideways and up/down (ignoring perspective), so the margins are even.
        final Vec3 center = focus.bounds().getCenter();
        double minR = Double.MAX_VALUE, maxR = -Double.MAX_VALUE, minU = Double.MAX_VALUE, maxU = -Double.MAX_VALUE;
        for (final Vec3 corner : corners)
        {
            final Vec3 offset = corner.subtract(center);
            minR = Math.min(minR, offset.dot(right));
            maxR = Math.max(maxR, offset.dot(right));
            minU = Math.min(minU, offset.dot(up));
            maxU = Math.max(maxU, offset.dot(up));
        }
        final Vec3 aim = center.add(right.scale((minR + maxR) / 2)).add(up.scale((minU + maxU) / 2));

        // A corner at offset o from the aim point is at depth o.forward + d in front of a camera d blocks back; it is in view when
        // |o.right| <= depth * tanX and |o.up| <= depth * tanY.
        double distance = 4;
        for (final Vec3 corner : corners)
        {
            final Vec3 offset = corner.subtract(aim);
            final double depth = offset.dot(forward);
            distance = Math.max(distance, Math.abs(offset.dot(right)) / tanX - depth);
            distance = Math.max(distance, Math.abs(offset.dot(up)) / tanY - depth);
        }
        return aim.subtract(forward.scale(distance));
    }

    private static void setHudHidden(final Minecraft mc, final boolean hidden)
    {
        if (mc.gui.hud.isHidden() != hidden)
        {
            mc.gui.hud.toggle();
        }
    }

    /**
     * Moves the spectator's eyes part of the way to the target (all of it for a new test or a big jump) and turns it to the fixed view direction. On
     * the client: the player is moved like by its own movement, so the rendering glides between ticks and the server follows.
     */
    private static void moveCamera(final LocalPlayer player, final Vec3 target, final boolean jump)
    {
        if (player == null)
        {
            return;
        }
        final Vec3 eyes = player.getEyePosition();
        final Vec3 next = jump || eyes.distanceTo(target) > SNAP_DISTANCE ? target : eyes.add(target.subtract(eyes).scale(FOLLOW));
        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(next.x, next.y - player.getEyeHeight(), next.z);
        player.setYRot(YAW);
        player.setXRot(PITCH);
        player.setYHeadRot(YAW);
    }
}
