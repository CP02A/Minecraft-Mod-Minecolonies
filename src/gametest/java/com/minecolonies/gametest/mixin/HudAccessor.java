package com.minecolonies.gametest.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * How long the selected item name above the hotbar is still shown (UI-10: it must show again when the highlight tip changes).
 */
@Mixin(Hud.class)
public interface HudAccessor
{
    @Accessor("toolHighlightTimer")
    int minecoloniesGametest$getToolHighlightTimer();
}
