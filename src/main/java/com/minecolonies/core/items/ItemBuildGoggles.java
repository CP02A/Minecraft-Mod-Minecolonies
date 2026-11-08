package com.minecolonies.core.items;

import com.minecolonies.core.client.render.worldevent.ColonyBlueprintRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.minecolonies.apiimp.initializer.ModItemsInitializer.GOGGLES;

public class ItemBuildGoggles extends Item
{
    /**
     * Constructor
     *
     * @param name            the name.
     * @param properties      the item properties.
     */
    public ItemBuildGoggles(
            @NotNull final String name,
            final Item.Properties properties)
    {
        // setNoRepair(): drop the repairable component added by humanoidArmor
        super(properties.humanoidArmor(GOGGLES, net.minecraft.world.item.equipment.ArmorType.HELMET)
                .component(net.minecraft.core.component.DataComponents.REPAIRABLE, null)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack,
                                @Nullable final Item.TooltipContext ctx,
                                final net.minecraft.world.item.component.TooltipDisplay tooltipDisplay, @NotNull final java.util.function.Consumer<Component> components,
                                @NotNull final TooltipFlag flags)
    {
        super.appendHoverText(stack, ctx, tooltipDisplay, components, flags);

        components.accept(Component.translatableEscape("\"%s\"",
                        Component.translatableEscape("item.minecolonies.build_goggles.lore")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        components.accept(Component.translatableEscape(ColonyBlueprintRenderer.willRenderBlueprints()
                ? "item.minecolonies.build_goggles.enabled" : "item.minecolonies.build_goggles.disabled")
                .withStyle(ChatFormatting.GRAY));
    }
}
