package com.ldtteam.shim.mixin.registries;

import com.ldtteam.shim.neoforge.registries.RegistrationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Assigns the id of the entry currently registered through a DeferredRegister supplier / RegisterEvent#register to block
 * properties that were created without one (NeoForge code paths constructing blocks inside a supplier).
 */
@Mixin(BlockBehaviour.Properties.class)
public abstract class BlockPropertiesMixin
{
    @Shadow
    @Nullable
    private ResourceKey<Block> id;

    @Inject(method = {"effectiveDescriptionId", "effectiveDrops"}, at = @At("HEAD"))
    private void minecolonies$assignRegistrationId(final CallbackInfoReturnable<?> cir)
    {
        if (this.id == null)
        {
            final ResourceKey<Block> key = RegistrationContext.current(Registries.BLOCK);
            if (key != null)
            {
                this.id = key;
            }
        }
    }
}
