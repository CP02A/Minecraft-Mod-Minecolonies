package com.ldtteam.domumornamentum.fabric;

import com.ldtteam.domumornamentum.block.decorative.DynamicTimberFrameBlock;
import com.ldtteam.domumornamentum.block.decorative.FramedLightBlock;
import com.ldtteam.domumornamentum.block.decorative.TimberFrameBlock;
import com.ldtteam.domumornamentum.client.event.handlers.MateriallyTexturedBlockPreviewRenderHandler;
import com.ldtteam.domumornamentum.client.event.handlers.MateriallyTexturedBlockTintProvider;
import com.ldtteam.domumornamentum.client.model.item.BlockTypeItemProperty;
import com.ldtteam.domumornamentum.client.model.loader.MateriallyTexturedModelLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Fabric client entrypoint for Domum Ornamentum's model/rendering registrations (custom block state model type,
 * item model type and properties, dynamic block tints, placement preview, fluid overlays). Runs after all main
 * entrypoints, so blocks are registered.
 */
public class DomumOrnamentumFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        MateriallyTexturedModelLoader.register();
        BlockTypeItemProperty.register();
        MateriallyTexturedBlockTintProvider.register();

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> MateriallyTexturedBlockPreviewRenderHandler.renderMateriallyTexturedBlockPreview(
            context.poseStack(),
            context.submitNodeCollector()));

        // NeoForge shouldDisplayFluidOverlay(...) == true for frames
        BuiltInRegistries.BLOCK.stream()
            .filter(block -> block instanceof TimberFrameBlock || block instanceof DynamicTimberFrameBlock || block instanceof FramedLightBlock)
            .forEach(block -> FluidRenderingRegistry.setBlockTransparency(block, true));
    }
}
