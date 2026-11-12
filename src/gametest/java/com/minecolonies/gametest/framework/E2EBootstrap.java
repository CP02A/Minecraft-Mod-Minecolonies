package com.minecolonies.gametest.framework;

import com.ldtteam.shim.neoforge.common.ModConfigSpec;
import com.minecolonies.api.configuration.ServerConfiguration;
import com.minecolonies.core.MineColonies;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares the game test server for the end-to-end tests (docs/e2e-tests/framework.md, "Test configuration" and "Speed"): game rules that
 * remove work and chance the tests aren't about, and the MineColonies test config. Runs once when the test server has started, before the
 * first test (tests start on the first server tick).
 * <p>
 * Only the test server, the world and the config are changed, never MineColonies code.
 */
public class E2EBootstrap implements ModInitializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(E2EBootstrap.class);

    @Override
    public void onInitialize()
    {
        ServerLifecycleEvents.SERVER_STARTED.register(E2EBootstrap::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (isTestServer(server))
            {
                LOGGER.info("MineColonies e2e: pathfinding wait: {}", PathfindingSync.statistics());
            }
        });
    }

    private static void onServerStarted(final MinecraftServer server)
    {
        if (!isTestServer(server))
        {
            return;
        }

        applyGameRules(server);
        applyTestConfig();
        LOGGER.info("MineColonies e2e: test game rules and test config applied");
    }

    /**
     * @return true for the servers the e2e tests run on: the headless game test server, and the integrated server of a client game test
     *     run (watch mode and UI tests, see {@link E2EMode}). A normal game ({@code runClient}) is never touched.
     */
    public static boolean isTestServer(final MinecraftServer server)
    {
        return server instanceof GameTestServer || (E2EMode.isClientTestRun() && !server.isDedicatedServer());
    }

    /**
     * Game rules from framework.md "Less work per tick". Tests that need one of these (day/night cycle, random ticks) change it through
     * {@link ColonyFixture#setGameRule}, which restores it when the test ends.
     */
    static void applyGameRules(final MinecraftServer server)
    {
        final GameRules rules = server.getGameRules();
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.SPAWN_MONSTERS, false, server);
        rules.set(GameRules.SPAWN_PATROLS, false, server);
        rules.set(GameRules.SPAWN_PHANTOMS, false, server);
        rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
        rules.set(GameRules.SPAWN_WARDENS, false, server);
        rules.set(GameRules.RAIDS, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.ADVANCE_TIME, false, server);
        // 0 = fire only spreads within 0 blocks of a player, i.e. never (-1 would mean everywhere).
        rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
        rules.set(GameRules.RANDOM_TICK_SPEED, 0, server);
        rules.set(GameRules.SHOW_DEATH_MESSAGES, false, server);
        rules.set(GameRules.SEND_COMMAND_FEEDBACK, false, server);
        rules.set(GameRules.LOG_ADMIN_COMMANDS, false, server);
    }

    /**
     * MineColonies test config from framework.md "Test configuration". Values are set in memory on the loaded server config (the file in
     * the throw-away test world is not rewritten).
     */
    static void applyTestConfig()
    {
        final ServerConfiguration config = MineColonies.getConfig().getServer();
        set(config.minColonyDistance, 1);
        set(config.initialCitizenAmount, 4);
        set(config.enableColonyRaids, false);
        set(config.forceLoadColony, true);
        set(config.creativeResolve, false);
        set(config.researchCreativeCompletion, false);
        // The design asks for 0, but the config's minimum is 1 and a higher value means rarer diseases ("1 in modifier x 10000" per
        // citizen check). 100 is the maximum: about one disease per 1,000,000 checks. Disease tests infect citizens on purpose.
        set(config.diseaseModifier, 100);
        set(config.minDistanceFromWorldSpawn, 0);
    }

    /**
     * Sets a config value in memory, also when the value is marked as needing a world restart (its cache is cleared so the next read sees
     * the new value).
     */
    public static <T> void set(final ModConfigSpec.ConfigValue<T> value, final T newValue)
    {
        value.set(newValue);
        value.clearCache();
    }
}
