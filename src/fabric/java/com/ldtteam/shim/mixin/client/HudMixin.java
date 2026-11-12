package com.ldtteam.shim.mixin.client;

import com.ldtteam.shim.neoforge.common.extensions.IItemExtension;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * NeoForge's {@code IItemExtension#getHighlightTip}: the selected item name above the hotbar can be replaced by the item (e.g. the scan
 * tool shows its current slot), and the name shows again when that text changes while the same item stays selected.
 */
@Mixin(Hud.class)
public abstract class HudMixin
{
    @Shadow
    private ItemStack lastToolHighlight;

    @ModifyVariable(
        method = "extractSelectedItemName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;getFont()Lnet/minecraft/client/gui/Font;", ordinal = 0),
        ordinal = 0
    )
    private MutableComponent minecolonies$highlightTip(final MutableComponent name)
    {
        if (lastToolHighlight.getItem() instanceof final IItemExtension extension)
        {
            final Component tip = extension.getHighlightTip(lastToolHighlight, name);
            if (tip != name)
            {
                return Component.empty().append(tip);
            }
        }
        return name;
    }

    @ModifyExpressionValue(
        method = "tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;equals(Ljava/lang/Object;)Z")
    )
    private boolean minecolonies$sameHighlightTip(final boolean sameName, @Local final ItemStack selected)
    {
        if (!sameName || !(selected.getItem() instanceof final IItemExtension extension))
        {
            return sameName;
        }
        return extension.getHighlightTip(selected, selected.getHoverName())
                 .equals(extension.getHighlightTip(lastToolHighlight, lastToolHighlight.getHoverName()));
    }
}
