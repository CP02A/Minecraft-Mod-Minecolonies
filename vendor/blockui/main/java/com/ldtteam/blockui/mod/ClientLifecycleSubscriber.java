package com.ldtteam.blockui.mod;

import com.ldtteam.blockui.Loader;
import com.ldtteam.blockui.mod.item.BlockStatePipRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.client.event.AddClientReloadListenersEvent;
import com.ldtteam.shim.neoforge.event.ModMismatchEvent;

import java.util.Set;

public class ClientLifecycleSubscriber
{
    @SubscribeEvent
    public static void onAddClientReloadListeners(final AddClientReloadListenersEvent event)
    {
        event.addListener(Loader.RELOADABLE_LISTEN_RES_LOC, Loader.INSTANCE);
    }

    /**
     * Fabric: registrations that NeoForge did through mod bus events (atlases, PiP renderers). Called from the
     * BlockUI Fabric client entrypoint. Custom GUI pipelines need no registration (compiled lazily by vanilla).
     */
    public static void registerFabric()
    {
        // register vanilla
        BlockUI.NAMESPACE_TO_ATLAS_MAP.put(Identifier.DEFAULT_NAMESPACE, AtlasIds.GUI);

        // register us
        final Identifier atlasKey = BlockUI.resLoc("blockui_gui");
        BlockUI.NAMESPACE_TO_ATLAS_MAP.put(BlockUI.MOD_ID, atlasKey);
        AtlasRegistry.register(new AtlasConfig(BlockUI.resLoc("textures/atlas/blockui_gui.png"), atlasKey, false, Set.of(GuiMetadataSection.TYPE)));

        PictureInPictureRendererRegistry.register(context -> new BlockStatePipRenderer());
    }

    @SubscribeEvent
    public static void onModMismatch(final ModMismatchEvent event)
    {
        // there are no world data and rest is mod compat anyway
        event.getVersionDifference(BlockUI.MOD_ID).ifPresent(id -> event.markResolved(BlockUI.MOD_ID));
    }

}
