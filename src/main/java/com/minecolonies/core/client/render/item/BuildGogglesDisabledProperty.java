package com.minecolonies.core.client.render.item;

import com.minecolonies.core.client.render.worldevent.ColonyBlueprintRenderer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Item model condition {@code minecolonies:build_goggles_disabled}: true while the build goggles are not rendering blueprints.
 * Replaces the 1.21.1 item property {@code disabled} of the build goggles.
 */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public record BuildGogglesDisabledProperty() implements ConditionalItemModelProperty
{
    public static final MapCodec<BuildGogglesDisabledProperty> MAP_CODEC = MapCodec.unit(new BuildGogglesDisabledProperty());

    @Override
    public boolean get(
      final ItemStack itemStack,
      final @Nullable ClientLevel level,
      final @Nullable LivingEntity owner,
      final int seed,
      final ItemDisplayContext displayContext)
    {
        return !ColonyBlueprintRenderer.willRenderBlueprints();
    }

    @Override
    public MapCodec<BuildGogglesDisabledProperty> type()
    {
        return MAP_CODEC;
    }
}
