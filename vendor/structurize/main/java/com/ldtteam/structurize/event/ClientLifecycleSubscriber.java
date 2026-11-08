package com.ldtteam.structurize.event;

import com.ldtteam.structurize.api.Log;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.blockentities.ModBlockEntities;
import com.ldtteam.structurize.client.BlueprintHandler;
import com.ldtteam.structurize.client.ClientItemStackTooltip;
import com.ldtteam.structurize.client.ModKeyMappings;
import com.ldtteam.structurize.client.TagSubstitutionRenderer;
import com.ldtteam.structurize.items.ItemStackTooltip;
import com.ldtteam.structurize.storage.ClientStructurePackLoader;
import com.ldtteam.structurize.util.WorldRenderMacros;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientReloadListenersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterKeyMappingsEvent;

public class ClientLifecycleSubscriber
{
    private static boolean fabricInitDone = false;

    /**
     * FABRIC-PORT: client registrations which are done directly against Fabric API / vanilla registries.
     * Must be called once during client mod init.
     */
    public static void initFabricClient()
    {
        if (fabricInitDone)
        {
            return;
        }
        fabricInitDone = true;

        // world rendering (was RenderLevelStageEvent), see WorldRenderMacros
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            WorldRenderContext.INSTANCE.renderWorldLastEvent(context);
            WorldRenderMacros.renderAllRegistered(context);
        });

        // was EntityRenderersEvent.RegisterRenderers
        BlockEntityRendererRegistry.register(ModBlockEntities.TAG_SUBSTITUTION.get(), TagSubstitutionRenderer::new);

        // was IClientItemExtensions#getCustomRenderer (BEWLR), now a special item model renderer used by assets/structurize/items/blocktagsubstitution.json
        SpecialModelRenderers.ID_MAPPER.put(Constants.resLocStruct("tag_substitution"), TagSubstitutionRenderer.ItemRenderer.Unbaked.MAP_CODEC);

        // FABRIC-PORT: ItemBlockRenderTypes#setRenderLayer(blockSubstitution, translucent) is gone in 26.x, chunk layers are derived from texture transparency
    }

    @SubscribeEvent
    public static void onClientInit(final RegisterClientReloadListenersEvent event)
    {
        ClientStructurePackLoader.onClientLoading();
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new SimplePreparableReloadListener<>()
        {
            @Override
            protected Object prepare(final ResourceManager manager, final ProfilerFiller profiler)
            {
                return new Object();
            }

            @Override
            protected void apply(final Object source, final ResourceManager manager, final ProfilerFiller profiler)
            {
                Log.getLogger().debug("Clearing blueprint renderer cache.");
                BlueprintHandler.getInstance().clearCache();
            }
        });
    }

    @SubscribeEvent
    public static void registerTooltips(final RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(ItemStackTooltip.class, ClientItemStackTooltip::new);
    }

    @SubscribeEvent
    public static void registerKeys(final RegisterKeyMappingsEvent event)
    {
        ModKeyMappings.register(event);
    }
}
