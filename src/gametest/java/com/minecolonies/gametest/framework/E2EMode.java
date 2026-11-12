package com.minecolonies.gametest.framework;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * How the end-to-end tests were started, read from system properties that build.gradle sets (docs/e2e-tests/framework.md, "Watch mode" and
 * "UI tests"):
 * <ul>
 *     <li>headless server game tests ({@code ./gradlew runGameTest}): none of the properties below, the server is a {@code GameTestServer};</li>
 *     <li>watch mode ({@code ./gradlew runGameTest -Pe2eWatch}): a client game test run that runs the server game tests on the integrated
 *     server of a real game window;</li>
 *     <li>UI tests ({@code ./gradlew runClientGameTest}): a client game test run that opens MineColonies windows and takes screenshots.</li>
 * </ul>
 */
public final class E2EMode
{
    /**
     * Set by Fabric Loom on every client game test run ({@code runClientGameTest}); Fabric's client game test framework is active then.
     */
    public static final String CLIENT_GAMETEST_PROPERTY = "fabric.client.gametest";

    /**
     * {@code true} in watch mode.
     */
    public static final String WATCH_PROPERTY = "minecolonies.e2e.watch";

    /**
     * Watch mode: game speed as a multiple of normal speed (20 ticks per second), e.g. 2 or 0.5. Default 1.
     */
    public static final String WATCH_SPEED_PROPERTY = "minecolonies.e2e.watchSpeed";

    /**
     * Watch mode: vanilla test selector of the server game tests to run (same value as {@code fabric-api.gametest.filter} headless).
     */
    public static final String WATCH_FILTER_PROPERTY = "minecolonies.e2e.watchFilter";

    /**
     * Watch mode: JUnit-style report file (the headless run writes {@code build/gametest-report.xml} too).
     */
    public static final String WATCH_REPORT_PROPERTY = "minecolonies.e2e.watchReport";

    /**
     * UI tests: folder for screenshots ({@code build/e2e-screenshots}).
     */
    public static final String SCREENSHOT_DIR_PROPERTY = "minecolonies.e2e.screenshotDir";

    /**
     * UI tests: comma separated list of UI test ids to run (e.g. {@code UI-01,UI-03}); all when empty.
     */
    public static final String UI_FILTER_PROPERTY = "minecolonies.e2e.uiFilter";

    /**
     * UI tests: JUnit-style report file ({@code build/e2e-ui-report.xml}).
     */
    public static final String UI_REPORT_PROPERTY = "minecolonies.e2e.uiReport";

    private E2EMode()
    {
    }

    /**
     * @return true in a client game test run (watch mode or UI tests): a real game window with an integrated server, real keyboard and
     *     mouse input ignored.
     */
    public static boolean isClientTestRun()
    {
        return System.getProperty(CLIENT_GAMETEST_PROPERTY) != null;
    }

    /**
     * Set when a person closed the test window (close button, Alt+F4) during a client game test run; the run then ends with exit code
     * {@link #WINDOW_CLOSED_EXIT_CODE} so Gradle reports it as failed, not as passed (docs/e2e-tests/framework.md, "No human input").
     */
    private static volatile boolean windowClosed;

    /**
     * Exit code of a client game test run whose window was closed by hand.
     */
    public static final int WINDOW_CLOSED_EXIT_CODE = 3;

    public static void markWindowClosed()
    {
        windowClosed = true;
    }

    public static boolean wasWindowClosed()
    {
        return windowClosed;
    }

    /**
     * @return true in watch mode.
     */
    public static boolean isWatch()
    {
        return isClientTestRun() && Boolean.getBoolean(WATCH_PROPERTY);
    }

    /**
     * @return watch mode game speed factor (1 = normal speed, 20 ticks per second), clamped to 0.1 .. 10.
     */
    public static double watchSpeed()
    {
        try
        {
            final double speed = Double.parseDouble(System.getProperty(WATCH_SPEED_PROPERTY, "1"));
            return Math.max(0.1, Math.min(10, speed));
        }
        catch (final NumberFormatException e)
        {
            return 1;
        }
    }

    @Nullable
    public static String watchFilter()
    {
        final String filter = System.getProperty(WATCH_FILTER_PROPERTY);
        return filter == null || filter.isBlank() ? null : filter;
    }

    @Nullable
    public static Path watchReport()
    {
        final String file = System.getProperty(WATCH_REPORT_PROPERTY);
        return file == null || file.isBlank() ? null : Path.of(file);
    }

    public static Path screenshotDir()
    {
        return Path.of(System.getProperty(SCREENSHOT_DIR_PROPERTY, "e2e-screenshots")).toAbsolutePath();
    }

    @Nullable
    public static Path uiReport()
    {
        final String file = System.getProperty(UI_REPORT_PROPERTY);
        return file == null || file.isBlank() ? null : Path.of(file);
    }

    /**
     * @return true if the UI test with this id (e.g. {@code UI-03}) is selected by {@code -Pe2eUiFilter}.
     */
    public static boolean uiTestSelected(final String id)
    {
        final String filter = System.getProperty(UI_FILTER_PROPERTY, "");
        if (filter.isBlank())
        {
            return true;
        }
        for (final String part : filter.split(","))
        {
            if (part.trim().equalsIgnoreCase(id) || part.trim().replace("-", "").equalsIgnoreCase(id.replace("-", "")))
            {
                return true;
            }
        }
        return false;
    }
}
