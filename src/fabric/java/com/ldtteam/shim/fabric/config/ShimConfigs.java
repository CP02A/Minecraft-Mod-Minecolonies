package com.ldtteam.shim.fabric.config;

import com.ldtteam.shim.fml.config.ConfigTracker;
import com.ldtteam.shim.fml.config.ModConfig;
import com.ldtteam.shim.fml.loading.FMLEnvironment;
import com.ldtteam.shim.fml.loading.FMLPaths;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Drives config loading for the shim (NeoForge-style lifecycle):
 * COMMON/CLIENT at init, SERVER per world on server start (with sync to remote clients), unloaded on server stop.
 */
public final class ShimConfigs
{
    private static final Logger     LOGGER          = LoggerFactory.getLogger("ShimConfigs");
    private static final Identifier EARLY_PHASE     = Identifier.fromNamespaceAndPath("minecolonies", "shim_config_early");
    private static final String     SERVER_CONFIG_DIR = "serverconfig";

    private static volatile MinecraftServer currentServer;
    private static boolean initialized;

    private ShimConfigs()
    {
    }

    /**
     * Call from the main entrypoint after all mods were constructed (configs registered).
     */
    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        final Path configDir = FMLPaths.CONFIGDIR.get();
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, configDir);
        if (FMLEnvironment.dist.isClient())
        {
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, configDir);
        }
        // Defaults so server values can be read before any world is loaded (client main menu etc.).
        ConfigTracker.INSTANCE.loadDefaultServerConfigs();

        PayloadTypeRegistry.clientboundPlay().register(ConfigFilePayload.TYPE, ConfigFilePayload.STREAM_CODEC);

        ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(EARLY_PHASE, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
        ServerLifecycleEvents.SERVER_STARTING.register(EARLY_PHASE, ShimConfigs::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(ShimConfigs::onServerStopped);
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> syncTo(listener.player, sender));

        ConfigTracker.INSTANCE.setServerConfigReloadListener(config -> {
            final MinecraftServer server = currentServer;
            if (server != null)
            {
                server.execute(() -> {
                    for (final ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers()))
                    {
                        sendConfig(player, config);
                    }
                });
            }
        });
    }

    private static void onServerStarting(final MinecraftServer server)
    {
        currentServer = server;
        final Path serverConfigDir = server.getWorldPath(LevelResource.ROOT).resolve(SERVER_CONFIG_DIR);
        LOGGER.debug("Loading server configs from {}", serverConfigDir);
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, serverConfigDir, FMLPaths.GAMEDIR.get().resolve("defaultconfigs"));
    }

    private static void onServerStopped(final MinecraftServer server)
    {
        currentServer = null;
        ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER);
        ConfigTracker.INSTANCE.loadDefaultServerConfigs();
    }

    private static void syncTo(final ServerPlayer player, final PacketSender sender)
    {
        if (!ServerPlayNetworking.canSend(player, ConfigFilePayload.TYPE))
        {
            return;
        }
        for (final ModConfig config : List.copyOf(ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER)))
        {
            final byte[] bytes = ConfigTracker.readLoadedBytes(config);
            if (bytes != null)
            {
                sender.sendPacket(new ConfigFilePayload(config.getFileName(), bytes));
            }
        }
    }

    private static void sendConfig(final ServerPlayer player, final ModConfig config)
    {
        if (!ServerPlayNetworking.canSend(player, ConfigFilePayload.TYPE))
        {
            return;
        }
        final byte[] bytes = ConfigTracker.readLoadedBytes(config);
        if (bytes != null)
        {
            ServerPlayNetworking.send(player, new ConfigFilePayload(config.getFileName(), bytes));
        }
    }
}
