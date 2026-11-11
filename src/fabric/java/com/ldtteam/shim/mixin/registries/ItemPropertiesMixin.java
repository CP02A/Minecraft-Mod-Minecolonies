package com.ldtteam.shim.mixin.registries;

import com.ldtteam.shim.neoforge.registries.RegistrationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Assigns the id of the entry currently registered through a DeferredRegister supplier / RegisterEvent#register to item
 * properties that were created without one.
 */
@Mixin(Item.Properties.class)
public abstract class ItemPropertiesMixin
{
    @Shadow
    @Nullable
    private ResourceKey<Item> id;

    @Inject(method = "itemIdOrThrow", at = @At("HEAD"))
    private void minecolonies$assignRegistrationId(final CallbackInfoReturnable<ResourceKey<Item>> cir)
    {
        if (this.id == null)
        {
            final ResourceKey<Item> key = RegistrationContext.current(Registries.ITEM);
            if (key != null)
            {
                this.id = key;
            }
        }
    }
}
