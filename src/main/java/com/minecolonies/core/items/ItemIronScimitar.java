package com.minecolonies.core.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * Class handling the Scimitar item.
 */
public class ItemIronScimitar extends Item
{
    /**
     * Constructor method for the Scimitar Item
     *
     * @param properties the properties.
     */
    public ItemIronScimitar(final Item.Properties properties)
    {
        // Iron sword tier, 3 attack damage (was SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F)).
        super(properties.sword(ToolMaterial.IRON, 3 - ToolMaterial.IRON.attackDamageBonus(), -2.4F));
    }
}
