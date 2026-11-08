// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.event.EntityRenderersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Entity renderer registration events (mod bus), forwarded to Fabric's renderer/model layer registries.
 */
public abstract class EntityRenderersEvent extends Event implements IModBusEvent
{
    protected EntityRenderersEvent() {}

    public static class RegisterLayerDefinitions extends EntityRenderersEvent
    {
        public RegisterLayerDefinitions() {}

        public void registerLayerDefinition(final ModelLayerLocation layerLocation, final Supplier<LayerDefinition> supplier)
        {
            ModelLayerRegistry.registerModelLayer(layerLocation, supplier::get);
        }
    }

    public static class RegisterRenderers extends EntityRenderersEvent
    {
        public RegisterRenderers() {}

        public <T extends Entity> void registerEntityRenderer(final EntityType<? extends T> entityType, final EntityRendererProvider<T> entityRendererProvider)
        {
            EntityRendererRegistry.register(entityType, entityRendererProvider);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
          final BlockEntityType<? extends T> blockEntityType,
          final BlockEntityRendererProvider<T, S> blockEntityRendererProvider)
        {
            BlockEntityRendererRegistry.register((BlockEntityType) blockEntityType, (BlockEntityRendererProvider) blockEntityRendererProvider);
        }
    }
}
