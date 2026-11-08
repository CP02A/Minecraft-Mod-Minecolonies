package com.ldtteam.structurize.fabric.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to {@link KeyMapping#clickCount} (was an access transformer on NeoForge).
 */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor
{
    @Accessor("clickCount")
    int structurize$getClickCount();

    @Accessor("clickCount")
    void structurize$setClickCount(int clickCount);
}
