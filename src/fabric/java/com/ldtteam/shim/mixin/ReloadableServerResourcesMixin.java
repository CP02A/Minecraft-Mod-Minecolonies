package com.ldtteam.shim.mixin;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.AddReloadListenerEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires AddReloadListenerEvent once per server resource reload and appends the added listeners (like NeoForge).
 */
@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin
{
    @Unique
    private RegistryAccess shim$registryAccess;
    @Unique
    private HolderLookup.Provider shim$lookup;
    @Unique
    private List<PreparableReloadListener> shim$modListeners;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void shim$captureRegistries(final net.minecraft.server.ReloadableServerRegistries.LoadResult loadingContext,
        final net.minecraft.world.flag.FeatureFlagSet enabledFeatures,
        final net.minecraft.commands.Commands.CommandSelection commandSelection,
        final List<?> postponedTags,
        final net.minecraft.server.permissions.PermissionSet functionCompilationPermissions,
        final List<?> newComponents,
        final CallbackInfo ci)
    {
        shim$registryAccess = loadingContext.layers().compositeAccess();
        shim$lookup = loadingContext.lookupWithUpdatedTags();
    }

    @Inject(method = "listeners", at = @At("RETURN"), cancellable = true)
    private void shim$addModListeners(final CallbackInfoReturnable<List<PreparableReloadListener>> cir)
    {
        if (shim$modListeners == null)
        {
            com.ldtteam.shim.fabric.events.ShimReloadContext.set(shim$registryAccess, shim$lookup);
            final AddReloadListenerEvent event = new AddReloadListenerEvent((ReloadableServerResources) (Object) this, shim$registryAccess, shim$lookup);
            NeoForge.EVENT_BUS.post(event);
            shim$modListeners = event.getListeners();
        }
        if (!shim$modListeners.isEmpty())
        {
            final List<PreparableReloadListener> list = new ArrayList<>(cir.getReturnValue());
            list.addAll(shim$modListeners);
            cir.setReturnValue(list);
        }
    }
}
