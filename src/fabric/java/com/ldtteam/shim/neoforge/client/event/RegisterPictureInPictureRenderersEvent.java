// Adapted from NeoForge 26.3 (net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.event;

import com.ldtteam.shim.bus.api.Event;
import com.ldtteam.shim.fml.event.IModBusEvent;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

import java.util.function.Supplier;

/**
 * Mod bus event to register picture-in-picture GUI renderers; forwarded to Fabric's {@link PictureInPictureRendererRegistry}.
 */
public final class RegisterPictureInPictureRenderersEvent extends Event implements IModBusEvent
{
    public RegisterPictureInPictureRenderersEvent() {}

    public <T extends PictureInPictureRenderState> void register(final Class<T> stateClass, final Supplier<? extends PictureInPictureRenderer<T>> factory)
    {
        PictureInPictureRendererRegistry.register(ctx -> factory.get());
    }
}
