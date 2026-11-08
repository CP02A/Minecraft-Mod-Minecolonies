package com.ldtteam.blockui.fabric.mixin;

import com.mojang.blaze3d.platform.cursor.CursorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Creates cursor types for BlockUI textured cursors (vanilla constructor is private).
 */
@Mixin(CursorType.class)
public interface CursorTypeAccessor
{
    @Invoker("<init>")
    static CursorType blockui$create(final String name, final long handle)
    {
        throw new AssertionError();
    }
}
