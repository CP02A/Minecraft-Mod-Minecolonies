package com.ldtteam.domumornamentum.event.handlers;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.block.decorative.DynamicTimberFrameBlock;
import com.ldtteam.domumornamentum.entity.block.ModBlockEntityTypes;
import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.neoforge.event.BlockEntityTypeAddBlocksEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class ModBusEventHandler
{
    // FABRIC-PORT: data generator registration removed (datagen output is shipped pre-generated)

    @SuppressWarnings("SuspiciousToArrayCall")
    @SubscribeEvent
    public static void onBlockEntityTypeAddBlocksEvent(final BlockEntityTypeAddBlocksEvent event) {
        event.modify(
                ModBlockEntityTypes.MATERIALLY_TEXTURED.get(),
                BuiltInRegistries.BLOCK.stream()
                        .filter(IMateriallyTexturedBlock.class::isInstance)
                        .toArray(Block[]::new)
        );

        event.modify(
                ModBlockEntityTypes.DYNAMIC_TIMBERFRAME.get(),
                BuiltInRegistries.BLOCK.stream()
                        .filter(DynamicTimberFrameBlock.class::isInstance)
                        .toArray(Block[]::new)
        );
    }
}
