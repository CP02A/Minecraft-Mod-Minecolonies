package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.client.UiTestSupport;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Records the text the HUD draws as the selected item name above the hotbar (UI-16, highlight tips), without changing it.
 */
@Mixin(Hud.class)
public abstract class HudSelectedItemNameCaptureMixin
{
    @ModifyArg(
        method = "extractSelectedItemName",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"),
        index = 1
    )
    private Component minecoloniesGametest$recordSelectedItemName(final Component text)
    {
        UiTestSupport.lastSelectedItemName = text.getString();
        return text;
    }
}
