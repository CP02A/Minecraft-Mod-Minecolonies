// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.ItemAbilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import com.ldtteam.shim.neoforge.common.extensions.IItemExtension;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * NeoForge item abilities. Fabric items have no {@code canPerformAction}, so abilities are approximated through vanilla/conventional
 * item tags and item classes, plus {@link IItemExtension} implementations and predicates registered through {@link #register(ItemAbility, Predicate)}.
 */
public class ItemAbilities
{
    public static final ItemAbility AXE_DIG = ItemAbility.get("axe_dig");
    public static final ItemAbility PICKAXE_DIG = ItemAbility.get("pickaxe_dig");
    public static final ItemAbility SHOVEL_DIG = ItemAbility.get("shovel_dig");
    public static final ItemAbility HOE_DIG = ItemAbility.get("hoe_dig");
    public static final ItemAbility SWORD_DIG = ItemAbility.get("sword_dig");
    public static final ItemAbility SHEARS_DIG = ItemAbility.get("shears_dig");
    public static final ItemAbility AXE_STRIP = ItemAbility.get("axe_strip");
    public static final ItemAbility AXE_SCRAPE = ItemAbility.get("axe_scrape");
    public static final ItemAbility AXE_WAX_OFF = ItemAbility.get("axe_wax_off");
    public static final ItemAbility SHOVEL_FLATTEN = ItemAbility.get("shovel_flatten");
    public static final ItemAbility SHOVEL_DOUSE = ItemAbility.get("shovel_douse");
    public static final ItemAbility SWORD_SWEEP = ItemAbility.get("sword_sweep");
    public static final ItemAbility SHEARS_HARVEST = ItemAbility.get("shears_harvest");
    public static final ItemAbility SHEARS_REMOVE_ARMOR = ItemAbility.get("shears_remove_armor");
    public static final ItemAbility SHEARS_CARVE = ItemAbility.get("shears_carve");
    public static final ItemAbility SHEARS_DISARM = ItemAbility.get("shears_disarm");
    public static final ItemAbility SHEARS_TRIM = ItemAbility.get("shears_trim");
    public static final ItemAbility HOE_TILL = ItemAbility.get("till");
    public static final ItemAbility SHIELD_BLOCK = ItemAbility.get("shield_block");
    public static final ItemAbility FISHING_ROD_CAST = ItemAbility.get("fishing_rod_cast");
    public static final ItemAbility TRIDENT_THROW = ItemAbility.get("trident_throw");
    public static final ItemAbility BRUSH_BRUSH = ItemAbility.get("brush_brush");
    public static final ItemAbility FIRESTARTER_LIGHT = ItemAbility.get("firestarter_light");

    public static final Set<ItemAbility> DEFAULT_AXE_ACTIONS = of(AXE_DIG, AXE_STRIP, AXE_SCRAPE, AXE_WAX_OFF);
    public static final Set<ItemAbility> DEFAULT_HOE_ACTIONS = of(HOE_DIG, HOE_TILL);
    public static final Set<ItemAbility> DEFAULT_SHOVEL_ACTIONS = of(SHOVEL_DIG, SHOVEL_FLATTEN, SHOVEL_DOUSE);
    public static final Set<ItemAbility> DEFAULT_PICKAXE_ACTIONS = of(PICKAXE_DIG);
    public static final Set<ItemAbility> DEFAULT_SWORD_ACTIONS = of(SWORD_DIG, SWORD_SWEEP);
    public static final Set<ItemAbility> DEFAULT_SHEARS_ACTIONS = of(SHEARS_DIG, SHEARS_HARVEST, SHEARS_REMOVE_ARMOR, SHEARS_CARVE, SHEARS_DISARM, SHEARS_TRIM);
    public static final Set<ItemAbility> DEFAULT_SHIELD_ACTIONS = of(SHIELD_BLOCK);
    public static final Set<ItemAbility> DEFAULT_FISHING_ROD_ACTIONS = of(FISHING_ROD_CAST);
    public static final Set<ItemAbility> DEFAULT_TRIDENT_ACTIONS = of(TRIDENT_THROW);
    public static final Set<ItemAbility> DEFAULT_BRUSH_ACTIONS = of(BRUSH_BRUSH);
    public static final Set<ItemAbility> DEFAULT_FLINT_ACTIONS = of(FIRESTARTER_LIGHT);
    public static final Set<ItemAbility> DEFAULT_FIRECHARGE_ACTIONS = of(FIRESTARTER_LIGHT);

    private static final Map<ItemAbility, List<Predicate<ItemStack>>> PREDICATES = new ConcurrentHashMap<>();

    static
    {
        final Predicate<ItemStack> axe = s -> s.is(ItemTags.AXES);
        final Predicate<ItemStack> pickaxe = s -> s.is(ItemTags.PICKAXES);
        final Predicate<ItemStack> shovel = s -> s.is(ItemTags.SHOVELS);
        final Predicate<ItemStack> hoe = s -> s.is(ItemTags.HOES);
        final Predicate<ItemStack> sword = s -> s.is(ItemTags.SWORDS);
        final Predicate<ItemStack> shears = s -> s.getItem() instanceof ShearsItem || s.is(ConventionalItemTags.SHEAR_TOOLS);
        DEFAULT_AXE_ACTIONS.forEach(a -> register(a, axe));
        DEFAULT_PICKAXE_ACTIONS.forEach(a -> register(a, pickaxe));
        DEFAULT_SHOVEL_ACTIONS.forEach(a -> register(a, shovel));
        DEFAULT_HOE_ACTIONS.forEach(a -> register(a, hoe));
        DEFAULT_SWORD_ACTIONS.forEach(a -> register(a, sword));
        DEFAULT_SHEARS_ACTIONS.forEach(a -> register(a, shears));
        register(SHIELD_BLOCK, s -> s.has(DataComponents.BLOCKS_ATTACKS) || s.is(ConventionalItemTags.SHIELD_TOOLS));
        register(FISHING_ROD_CAST, s -> s.getItem() instanceof FishingRodItem || s.is(ConventionalItemTags.FISHING_ROD_TOOLS));
        register(TRIDENT_THROW, s -> s.getItem() instanceof TridentItem || s.is(ConventionalItemTags.TRIDENT_TOOLS));
        register(BRUSH_BRUSH, s -> s.getItem() instanceof BrushItem || s.is(ConventionalItemTags.BRUSH_TOOLS));
        register(FIRESTARTER_LIGHT, s -> s.getItem() instanceof FlintAndSteelItem || s.getItem() instanceof FireChargeItem || s.is(ConventionalItemTags.IGNITER_TOOLS));
    }

    /**
     * Registers an additional predicate granting the given ability.
     */
    public static void register(final ItemAbility ability, final Predicate<ItemStack> predicate)
    {
        PREDICATES.computeIfAbsent(ability, a -> new CopyOnWriteArrayList<>()).add(predicate);
    }

    /**
     * Replacement for NeoForge's {@code stack.canPerformAction(ability)}.
     */
    public static boolean canPerformAction(final ItemStack stack, final ItemAbility ability)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        if (stack.getItem() instanceof IItemExtension ext && ext.canPerformAction(stack, ability))
        {
            return true;
        }
        for (final Predicate<ItemStack> predicate : PREDICATES.getOrDefault(ability, List.of()))
        {
            if (predicate.test(stack))
            {
                return true;
            }
        }
        return false;
    }

    private static Set<ItemAbility> of(final ItemAbility... actions)
    {
        return Stream.of(actions).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
