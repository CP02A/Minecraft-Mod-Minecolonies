// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.DeferredSpawnEggItem), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Supplier;

/**
 * Spawn egg whose entity type is resolved lazily. On 26.x the entity type is stored in the {@code ENTITY_DATA} default component,
 * which is filled in through Fabric's {@link DefaultItemComponentEvents#MODIFY} once all registries are populated.
 * The colours are kept for API compatibility only; 26.x spawn eggs are textured through their item model.
 */
public class DeferredSpawnEggItem extends SpawnEggItem
{
    private final Supplier<? extends EntityType<? extends Mob>> typeSupplier;
    private final int backgroundColor;
    private final int highlightColor;

    public DeferredSpawnEggItem(final Supplier<? extends EntityType<? extends Mob>> type, final int backgroundColor, final int highlightColor, final Item.Properties props)
    {
        super(props);
        this.typeSupplier = type;
        this.backgroundColor = backgroundColor;
        this.highlightColor = highlightColor;
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(this, builder -> {
            final EntityType<? extends Mob> entityType = typeSupplier.get();
            if (entityType != null)
            {
                builder.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, new CompoundTag()));
            }
        }));
    }

    public EntityType<? extends Mob> getDefaultType()
    {
        return typeSupplier.get();
    }

    public int getColor(final int tintIndex)
    {
        return tintIndex == 0 ? backgroundColor : highlightColor;
    }
}
