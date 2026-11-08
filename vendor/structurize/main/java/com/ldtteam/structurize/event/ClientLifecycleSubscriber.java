package com.ldtteam.structurize.event;

import com.ldtteam.structurize.blockentities.ModBlockEntities;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.client.*;
import com.ldtteam.structurize.api.Log;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.client.model.OverlaidModelLoader;
import com.ldtteam.structurize.items.ItemStackTooltip;
import com.ldtteam.structurize.items.ModItems;
import com.ldtteam.structurize.storage.ClientStructurePackLoader;
import com.ldtteam.structurize.util.WorldRenderMacros;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.api.distmarker.OnlyIn;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.client.event.EntityRenderersEvent;
import com.ldtteam.shim.neoforge.client.event.ModelEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientReloadListenersEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterKeyMappingsEvent;
import com.ldtteam.shim.neoforge.client.event.RegisterRenderBuffersEvent;
import com.ldtteam.shim.neoforge.client.extensions.common.IClientItemExtensions;
import com.ldtteam.shim.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public class ClientLifecycleSubscriber
{
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

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void doClientStuff(final EntityRenderersEvent.RegisterRenderers event)
    {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.blockSubstitution.get(), RenderType.translucent());
    }

    @SubscribeEvent
    public static void registerGeometry(final ModelEvent.RegisterGeometryLoaders event)
    {
        event.register(Constants.resLocStruct("overlaid"), new OverlaidModelLoader());
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlockEntities.TAG_SUBSTITUTION.get(), TagSubstitutionRenderer::new);
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

    @SubscribeEvent
    public static void registerGlobablRenderBuffers(final RegisterRenderBuffersEvent event)
    {
        WorldRenderMacros.RenderTypes.registerBuffer(event);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event)
    {
        event.registerItem(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return TagSubstitutionRenderer.getInstance();
            }
        }, ModItems.blockTagSubstitution.get());
    }
}
