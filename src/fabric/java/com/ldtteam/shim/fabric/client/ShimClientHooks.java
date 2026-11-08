package com.ldtteam.shim.fabric.client;

import com.ldtteam.shim.fabric.ComplexSpawnSync;
import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.fabric.capabilities.ShimCapabilityHooks;
import com.ldtteam.shim.neoforge.client.event.AddClientReloadListenersEvent;
import com.ldtteam.shim.neoforge.client.event.ClientPlayerNetworkEvent;
import com.ldtteam.shim.neoforge.client.event.ClientTickEvent;
import com.ldtteam.shim.neoforge.client.event.CustomizeGuiOverlayEvent;
import com.ldtteam.shim.neoforge.client.event.EntityRenderersEvent;
import com.ldtteam.shim.neoforge.client.event.RecipesUpdatedEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientReloadListenersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterItemDecorationsEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterKeyMappingsEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterMenuScreensEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterParticleProvidersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterRecipeBookCategoriesEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterRenderBuffersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterRenderPipelinesEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterTextureAtlasesEvent;
import com.ldtteam.shim.neoforge.client.event.RenderGuiLayerEvent;
import com.ldtteam.shim.neoforge.client.event.RenderLevelStageEvent;
import com.ldtteam.shim.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import com.ldtteam.shim.neoforge.client.gui.VanillaGuiLayers;
import com.ldtteam.shim.neoforge.common.NeoForge;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Client side hook-up of the NeoForge shim: fires the client mod-bus registration events and bridges Fabric client callbacks to
 * NeoForge game-bus client events. Called reflectively by the client entrypoint.
 */
public final class ShimClientHooks
{
    private static final Logger LOGGER = LoggerFactory.getLogger("ShimClientHooks");
    private static boolean initialized = false;

    private ShimClientHooks() {}

    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;

        // Mod bus registration events
        post(RegisterKeyMappingsEvent::new);
        post(EntityRenderersEvent.RegisterLayerDefinitions::new);
        post(EntityRenderersEvent.RegisterRenderers::new);
        post(RegisterMenuScreensEvent::new);
        post(RegisterParticleProvidersEvent::new);
        post(RegisterClientReloadListenersEvent::new);
        post(AddClientReloadListenersEvent::new);
        post(RegisterClientTooltipComponentFactoriesEvent::new);
        post(RegisterItemDecorationsEvent::new);
        post(RegisterRecipeBookCategoriesEvent::new);
        post(RegisterClientExtensionsEvent::new);
        post(RegisterPictureInPictureRenderersEvent::new);
        post(RegisterRenderPipelinesEvent::new);
        post(RegisterTextureAtlasesEvent::new);
        post(RegisterRenderBuffersEvent::new);

        // Game bus events
        ClientTickEvents.START_CLIENT_TICK.register(client -> NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> NeoForge.EVENT_BUS.post(new ClientTickEvent.Post()));

        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) ->
                                                   NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingIn(client.gameMode, client.player, listener.getConnection())));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) ->
                                                         NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingOut(client.gameMode, client.player, listener.getConnection())));

        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> NeoForge.EVENT_BUS.post(new RecipesUpdatedEvent(recipes)));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ShimCapabilityHooks.ensureInitialized());

        registerLevelRenderEvents();
        registerHudLayers();
        registerDebugText();

        ClientPlayNetworking.registerGlobalReceiver(ComplexSpawnSync.Payload.TYPE, (payload, context) -> {
            final Minecraft client = context.client();
            if (client.level != null)
            {
                final Entity entity = client.level.getEntity(payload.entityId());
                if (entity != null)
                {
                    ComplexSpawnSync.handle(entity, payload.data());
                }
            }
        });
    }

    /**
     * Called by the client entrypoint after FMLClientSetupEvent.
     */
    public static void postClientSetup()
    {
        // nothing to do yet
    }

    private static void post(final Supplier<? extends com.ldtteam.shim.bus.api.Event> factory)
    {
        try
        {
            ShimModBuses.post(factory.get());
        }
        catch (final RuntimeException e)
        {
            LOGGER.error("Error while posting client registration event", e);
        }
    }

    private static void registerLevelRenderEvents()
    {
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> NeoForge.EVENT_BUS.post(
          new RenderLevelStageEvent.AfterOpaqueBlocks(ctx.levelRenderer(), ctx.levelState(), null, null)));
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> NeoForge.EVENT_BUS.post(
          new RenderLevelStageEvent.AfterEntities(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector())));
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx -> NeoForge.EVENT_BUS.post(
          new RenderLevelStageEvent.AfterOpaqueFeatures(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector())));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(ctx -> {
            NeoForge.EVENT_BUS.post(new RenderLevelStageEvent.AfterTranslucentBlocks(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS,
              ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector()));
            NeoForge.EVENT_BUS.post(new RenderLevelStageEvent.AfterTranslucentBlocks(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS,
              ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector()));
        });
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> NeoForge.EVENT_BUS.post(
          new RenderLevelStageEvent.AfterTranslucentFeatures(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector())));
        LevelRenderEvents.END_MAIN.register(ctx -> NeoForge.EVENT_BUS.post(
          new RenderLevelStageEvent.AfterLevel(ctx.levelRenderer(), ctx.levelState(), ctx.poseStack(), ctx.submitNodeCollector())));
    }

    private static void registerHudLayers()
    {
        final Set<Identifier> ids = new LinkedHashSet<>();
        for (final Field field : VanillaGuiLayers.class.getFields())
        {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == Identifier.class)
            {
                try
                {
                    ids.add((Identifier) field.get(null));
                }
                catch (final IllegalAccessException ignored)
                {
                    // public field
                }
            }
        }
        ids.remove(VanillaGuiLayers.DEBUG_OVERLAY);
        ids.remove(VanillaGuiLayers.SAVING_INDICATOR);
        for (final Identifier id : ids)
        {
            HudElementRegistry.replaceElement(id, original -> (graphics, deltaTracker) -> {
                final RenderGuiLayerEvent.Pre pre = NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(graphics, deltaTracker, id));
                if (!pre.isCanceled())
                {
                    original.extractRenderState(graphics, deltaTracker);
                    NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(graphics, deltaTracker, id));
                }
            });
        }
    }

    private static void registerDebugText()
    {
        DebugScreenEntries.register(Identifier.fromNamespaceAndPath("minecolonies", "shim_debug_text"), (DebugScreenEntry) (displayer, level, clientChunk, serverChunk) -> {
            final CustomizeGuiOverlayEvent.DebugText event = NeoForge.EVENT_BUS.post(new CustomizeGuiOverlayEvent.DebugText(new ArrayList<>(), new ArrayList<>()));
            for (final String line : event.getLeft())
            {
                displayer.addLine(line);
            }
            for (final String line : event.getRight())
            {
                displayer.addLine(line);
            }
        });
    }
}
