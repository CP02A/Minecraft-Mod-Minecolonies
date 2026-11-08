package com.minecolonies.fabric.mixin.api;

import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes CreativeModeTab#displayItemsGenerator (was public through the NeoForge access transformer).
 */
@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor
{
    @Accessor("displayItemsGenerator")
    CreativeModeTab.DisplayItemsGenerator minecolonies$getDisplayItemsGenerator();
}
