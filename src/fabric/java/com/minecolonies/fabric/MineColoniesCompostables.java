package com.minecolonies.fabric;

import com.minecolonies.api.util.constant.Constants;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * FABRIC-PORT: makes MineColonies food and crops compostable in the vanilla composter.
 * <p>
 * On NeoForge 1.21.1 this came from the {@code neoforge:compostables} data map. In 26.x vanilla uses the {@code minecraft:compostable}
 * item component, which references one of vanilla's chance tiers ({@code data/minecraft/context_int_provider/compostable/}). The tiers
 * are only referenced, not copied: a datapack that changes a vanilla tier changes these items too. Chances that have no exact vanilla
 * tier use the closest one (the breads: 83% on NeoForge, {@code medium_high} = 85% here).
 */
public final class MineColoniesCompostables
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceKey<ContextIntProvider>, List<String>> ITEMS_BY_TIER = Map.of(
        // 100% (NeoForge chance 1.0)
        ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE, List.of(
            "apple_pie", "baked_salmon", "borscht", "cabochis", "cheddar_cheese", "cheese_pizza", "cheese_ravioli", "chicken_broth",
            "composted_dirt", "congee", "cooked_rice", "corn_chowder", "eggdrop_soup", "eggplant_dolma", "feta_cheese", "fish_dinner",
            "fish_n_chips", "flatbread", "fried_rice", "hand_pie", "kebab", "kimchi", "lamb_stew", "lembas_scone", "manchet_bread",
            "meat_ravioli", "mint_jelly", "mint_tea", "mintchoco_cheesecake", "muffin", "mushroom_pizza", "mutton_dinner", "pasta_plain",
            "pasta_tomato", "pea_soup", "pepper_hummus", "pierogi", "pita_hummus", "plain_cheesecake", "polenta", "potato_soup", "pottage",
            "ramen", "rice_ball", "schnitzel", "spicy_eggplant", "spicy_grilled_chicken", "squash_soup", "steak_dinner", "stew_trencher",
            "stuffed_pepper", "stuffed_pita", "sugary_bread", "sushi_roll", "tacos", "tofu", "tortillas", "veggie_quiche", "veggie_ravioli",
            "veggie_soup", "yogurt", "yogurt_with_berries"),
        // 85% (NeoForge chance 0.83)
        ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH, List.of(
            "chorus_bread", "golden_bread", "milky_bread"),
        // 50% (NeoForge chance 0.5)
        ContextIntProviders.COMPOSTABLE_LOW_MEDIUM, List.of(
            "bell_pepper", "butternut_squash", "cabbage", "chickpea", "corn", "durum", "eggplant", "garlic", "mint", "mistletoe",
            "nether_pepper", "onion", "peas", "rice", "soybean", "tomato"));

    private MineColoniesCompostables()
    {
    }

    /**
     * Call from the main entrypoint. The component is added when Fabric computes the default item components (after registration).
     */
    public static void init()
    {
        DefaultItemComponentEvents.MODIFY.register(context -> ITEMS_BY_TIER.forEach((tier, names) -> {
            final Compostable compostable = new Compostable(tier);
            for (final String name : names)
            {
                final Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
                final Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null)
                {
                    LOGGER.warn("Compostable item {} is not registered", id);
                    continue;
                }
                context.modify(item, builder -> builder.set(DataComponents.COMPOSTABLE, compostable));
            }
        }));
    }
}
