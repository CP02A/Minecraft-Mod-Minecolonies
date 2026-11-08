package com.ldtteam.domumornamentum.client.model.loader;

import com.ldtteam.domumornamentum.client.model.item.MateriallyTexturedItemModel;
import com.ldtteam.domumornamentum.util.Constants;
import com.ldtteam.shim.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.minecraft.client.renderer.item.ItemModels;

/**
 * Registers Domum's custom model types (Fabric: called from the client initializer, replaces NeoForge's
 * {@code RegisterBlockStateModels} / {@code RegisterItemModels} events).
 *
 * <ul>
 *     <li>Block states: {@code {"fabric:type": "domum_ornamentum:materially_textured", "parent": ...}}</li>
 *     <li>Item models: {@code {"type": "domum_ornamentum:materially_textured", "model": ...}}</li>
 * </ul>
 */
public class MateriallyTexturedModelLoader {

    private static boolean registered = false;

    private MateriallyTexturedModelLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CustomUnbakedBlockStateModel.register(Constants.MATERIALLY_TEXTURED_MODEL_LOADER, MateriallyTexturedUnbakedModel.CODEC);
        ItemModels.ID_MAPPER.put(Constants.MATERIALLY_TEXTURED_MODEL_LOADER, MateriallyTexturedItemModel.Unbaked.MAP_CODEC);
    }
}
