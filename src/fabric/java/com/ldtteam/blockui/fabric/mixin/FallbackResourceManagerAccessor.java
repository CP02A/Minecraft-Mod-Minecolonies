package com.ldtteam.blockui.fabric.mixin;

import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.InputStream;

/**
 * Resource metadata parsing for out-of-jar textures (NeoForge access transformer replacement).
 */
@Mixin(FallbackResourceManager.class)
public interface FallbackResourceManagerAccessor
{
    @Invoker("convertToMetadata")
    static IoSupplier<ResourceMetadata> blockui$convertToMetadata(IoSupplier<InputStream> input)
    {
        throw new AssertionError();
    }
}
