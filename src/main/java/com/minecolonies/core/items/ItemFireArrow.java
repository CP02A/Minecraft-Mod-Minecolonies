package com.minecolonies.core.items;

import com.minecolonies.api.entity.ModEntities;
import net.minecraft.world.entity.Entity;
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

    // FABRIC-PORT TODO: NeoForge IItemExtension#hasCustomEntity/createEntity hooks have no Fabric equivalent.
    public boolean hasCustomEntity(final ItemStack stack)
    {
        return true;
    }

    @Override
    public AbstractArrow createArrow(final Level worldIn, final ItemStack stack, final LivingEntity shooter, @Nullable final ItemStack bow)
    {
        AbstractArrow entity = ModEntities.FIREARROW.create(worldIn, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        entity.setOwner(shooter);
        return entity;
    }

    @Nullable
    public Entity createEntity(final Level world, final Entity location, final ItemStack itemstack)
    {
        return ModEntities.FIREARROW.create(world, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
    }
}
