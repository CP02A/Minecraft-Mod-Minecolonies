package com.ldtteam.blockui.fabric.mixin;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Vanilla button sprites (NeoForge access transformer replacement).
 */
@Mixin(AbstractButton.class)
public interface AbstractButtonAccessor
{
    @Accessor("SPRITES")
    static WidgetSprites blockui$getSprites()
    {
        throw new AssertionError();
    }
}
