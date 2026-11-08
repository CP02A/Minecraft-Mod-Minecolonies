package com.ldtteam.structurize.event;

import com.ldtteam.common.language.LanguageHandler;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.network.messages.*;
import com.ldtteam.structurize.storage.ServerStructurePackLoader;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.ModList;
import com.ldtteam.shim.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import com.ldtteam.shim.fml.event.lifecycle.FMLLoadCompleteEvent;
import com.ldtteam.shim.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.ldtteam.shim.neoforge.network.registration.PayloadRegistrar;

public class LifecycleSubscriber
{
    @SubscribeEvent
    public static void onNetworkRegistry(final RegisterPayloadHandlersEvent event)
    {
        final String modVersion = ModList.get().getModContainerById(Constants.MOD_ID).get().getModInfo().getVersion().toString();
        final PayloadRegistrar registry = event.registrar(Constants.MOD_ID).versioned(modVersion);

        AbsorbBlockMessage.TYPE.register(registry);
        AddRemoveTagMessage.TYPE.register(registry);
        BlueprintSyncMessage.TYPE.register(registry);
        BuildToolPlacementMessage.TYPE.register(registry);
        ClientBlueprintRequestMessage.TYPE.register(registry);
        FillTopPlaceholderMessage.TYPE.register(registry);
        ItemMiddleMouseMessage.TYPE.register(registry);
        NotifyClientAboutStructurePacksMessage.TYPE.register(registry);
        NotifyServerAboutStructurePacksMessage.TYPE.register(registry);
        OperationHistoryMessage.TYPE.register(registry);
        RemoveBlockMessage.TYPE.register(registry);
        RemoveEntityMessage.TYPE.register(registry);
        ReplaceBlockMessage.TYPE.register(registry);
        SaveScanMessage.TYPE.register(registry);
        ScanOnServerMessage.TYPE.register(registry);
        ScanToolTeleportMessage.TYPE.register(registry);
        SetTagInTool.TYPE.register(registry);
        ShowScanMessage.TYPE.register(registry);
        SyncPreviewCacheToClient.TYPE.register(registry);
        SyncPreviewCacheToServer.TYPE.register(registry);
        SyncSettingsToServer.TYPE.register(registry);
        TransferStructurePackToClient.TYPE.register(registry);
        UndoRedoMessage.TYPE.register(registry);
        UpdateClientRender.TYPE.register(registry);
        UpdateScanToolMessage.TYPE.register(registry);
    }

    /**
     * Called when MC loading is about to finish.
     *
     * @param event event
     */
    @SubscribeEvent
    public static void onLoadComplete(final FMLLoadCompleteEvent event)
    {
        LanguageHandler.setMClanguageLoaded();
    }

    @SubscribeEvent
    public static void onDedicatedServerInit(final FMLDedicatedServerSetupEvent event)
    {
        ServerStructurePackLoader.onServerStarting();
    }
}
