package com.minecolonies.core.items;

import com.minecolonies.api.entity.ModEntities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Class handling the Scepter for the Pharao.
 */
public class ItemFireArrow extends ArrowItem
{
    /**
     * Constructor method for the Chief Sword Item
     *
     * @param properties the properties.
     */
    public ItemFireArrow(final Properties properties)
    {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(final Level worldIn, final ItemStack stack, final LivingEntity shooter, @Nullable final ItemStack bow)
    {
        AbstractArrow entity = ModEntities.FIREARROW.create(worldIn, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        entity.setOwner(shooter);
        return entity;
    }

    // FABRIC-PORT: the 1.21.1 hasCustomEntity/createEntity overrides (NeoForge IItemExtension) were dropped on purpose. NeoForge replaced
    // every dropped fire arrow item entity with a new fire arrow entity at (0, 0, 0) (createEntity ignored the dropped item's position
    // and stack), so dropped fire arrows (e.g. the pharao's loot) vanished. Shooting goes through createArrow above, which vanilla calls.
}
