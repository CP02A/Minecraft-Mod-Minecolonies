package com.ldtteam.blockui.fabric.mixin;

import com.ldtteam.blockui.util.cursor.Cursor;
import com.mojang.blaze3d.platform.cursor.CursorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Selects BlockUI textured cursors (replaces the 26.1 TexturedCursorType subclass, vanilla CursorType is not extendable).
 */
@Mixin(CursorType.class)
public class CursorTypeMixin
{
    @Inject(method = "select", at = @At("HEAD"), cancellable = true)
    private void blockui$selectTextured(final CallbackInfo ci)
    {
        if (Cursor.selectTextured((CursorType) (Object) this))
        {
            ci.cancel();
        }
    }
}
