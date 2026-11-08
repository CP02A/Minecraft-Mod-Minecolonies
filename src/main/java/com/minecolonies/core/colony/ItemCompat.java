package com.minecolonies.core.colony;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

/**
 * Replacements for the item class checks ({@code ArmorItem}, {@code SwordItem}, {@code DiggerItem}) removed in 26.x vanilla.
 */
public final class ItemCompat
{
    private ItemCompat()
    {
    }

    /**
     * Replacement for {@code stack.getItem() instanceof ArmorItem}: an equippable humanoid armor piece that grants armor (or is tagged as armor).
     */
    public static boolean isArmor(final ItemStack stack)
    {
        return getArmorSlot(stack) != null;
    }

    /**
     * Replacement for {@code stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot}.
     */
    public static boolean isArmor(final ItemStack stack, final EquipmentSlot slot)
    {
        return getArmorSlot(stack) == slot;
    }

    /**
     * Get the armor slot of a stack, or null if the stack is not armor.
     */
    @Nullable
    public static EquipmentSlot getArmorSlot(final ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return null;
        }
        final Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot().getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
        {
            return null;
        }
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR))
        {
            return equippable.slot();
        }
        final boolean grantsArmor = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                                      .modifiers()
                                      .stream()
                                      .anyMatch(e -> e.attribute().is(Attributes.ARMOR) || e.attribute().is(Attributes.ARMOR_TOUGHNESS));
        return grantsArmor ? equippable.slot() : null;
    }

    /**
     * Replacement for {@code armorItem.getMaterial() == ArmorMaterials.X}: compares the equipment asset.
     */
    public static boolean hasArmorAsset(final ItemStack stack, final ResourceKey<EquipmentAsset> asset)
    {
        final Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return isArmor(stack) && equippable != null && equippable.assetId().map(asset::equals).orElse(false);
    }

    /**
     * Replacement for {@code instanceof SwordItem}.
     */
    public static boolean isSword(final ItemStack stack)
    {
        return stack.is(ItemTags.SWORDS);
    }

    /**
     * Replacement for {@code instanceof DiggerItem}.
     */
    public static boolean isDigger(final ItemStack stack)
    {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
    }
}
