package com.minecolonies.fabric.mixin.misc;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the raw command source (was accessible on NeoForge).
 */
@Mixin(CommandSourceStack.class)
public interface CommandSourceStackAccessor
{
    @Accessor("source")
    CommandSource minecolonies$getSource();
}
