package com.ldtteam.domumornamentum.client.model.loader;

import com.ldtteam.domumornamentum.util.Constants;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.fml.common.EventBusSubscriber;
import com.ldtteam.shim.neoforge.client.event.RegisterBlockStateModels;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class MateriallyTexturedModelLoader {

    private MateriallyTexturedModelLoader() {
        throw new IllegalStateException("Utility class");
    }

    @SubscribeEvent
    public static void onRegisterBlockStateModels(final RegisterBlockStateModels event) {
        event.registerModel(Constants.MATERIALLY_TEXTURED_MODEL_LOADER, MateriallyTexturedUnbakedModel.CODEC);
    }
}
