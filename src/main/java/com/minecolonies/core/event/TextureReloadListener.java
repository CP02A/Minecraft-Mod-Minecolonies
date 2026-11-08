package com.minecolonies.core.event;

import com.ldtteam.blockui.mod.BlockUI;
import com.ldtteam.shim.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.model.sprite.AtlasManager.AtlasConfig;
import net.minecraft.resources.Identifier;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.fml.common.Mod;
import com.ldtteam.shim.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ldtteam.shim.fml.common.EventBusSubscriber.Bus.MOD;

/**
 * Specific texture reload listener.
 */
@EventBusSubscriber(value= Dist.CLIENT, modid= Constants.MOD_ID, bus=MOD)
public class TextureReloadListener extends SimplePreparableReloadListener<TextureReloadListener.TexturePacks>
{
    /**
     * List of all texture packs available.
     */
    public static final List<String> TEXTURE_PACKS = new ArrayList<>();

    @NotNull
    @Override
    protected TexturePacks prepare(@NotNull final ResourceManager manager, @NotNull final ProfilerFiller profiler)
    {
        final Set<String> set = new HashSet<>();
        final List<Identifier> resLocs = new ArrayList<>(manager.listResources("textures/entity/citizen", f -> true).keySet());
        for (final Identifier res : resLocs)
        {
            if (res.getPath().contains("png") && res.getPath().contains("textures/entity/citizen"))
            {
                final String folder = res.getPath().split("/")[3];
                if (!folder.isEmpty())
                {
                    set.add(folder);
                }
            }
        }

        final TexturePacks packs = new TexturePacks();
        packs.packs = new ArrayList<>(set);
        return packs;
    }

    @Override
    protected void apply(@NotNull final TexturePacks packs, @NotNull final ResourceManager manager, @NotNull final ProfilerFiller profiler)
    {
       TextureReloadListener.TEXTURE_PACKS.clear();
       TextureReloadListener.TEXTURE_PACKS.addAll(packs.packs);
    }

    /**
     * Storage class to hand the texture packs from off-thread to the main thread.
     */
    public static class TexturePacks
    {
        public List<String> packs = new ArrayList<>();
    }

    @SubscribeEvent
    public static void modInitClient(final RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new TextureReloadListener());
    }

    /**
     * Register the minecolonies gui atlas (assets/minecolonies/atlases/minecolonies_gui.json).
     * FABRIC-PORT: 26.x/BlockUI registers gui atlases through the texture atlas registration event instead of BlockUI's old AtlasManager.
     *
     * @param event the event.
     */
    @SubscribeEvent
    public static void onRegisterTextureAtlases(final RegisterTextureAtlasesEvent event)
    {
        final Identifier atlasKey = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "minecolonies_gui");
        BlockUI.NAMESPACE_TO_ATLAS_MAP.put(Constants.MOD_ID, atlasKey);
        event.register(new AtlasConfig(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/atlas/minecolonies_gui.png"), atlasKey, false, Set.of(GuiMetadataSection.TYPE)));
    }
}

