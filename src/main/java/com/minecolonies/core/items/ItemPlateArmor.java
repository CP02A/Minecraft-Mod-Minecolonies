package com.minecolonies.core.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.NotNull;

/**
 * Class handling the Plate Armor.
 */
public class ItemPlateArmor extends Item
{
    /**
     * Constructor method for the Plate Armor
     *
     * @param name            the name.
     * @param materialIn      the material of the armour.
     * @param equipmentSlotIn the armor type (slot) of it.
     * @param properties      the item properties.
     */
    public ItemPlateArmor(
      @NotNull final String name,
      @NotNull final ArmorMaterial materialIn,
      @NotNull final ArmorType equipmentSlotIn,
      final Item.Properties properties)
    {
        super(properties.humanoidArmor(materialIn, equipmentSlotIn));
    }
}
