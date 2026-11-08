package com.minecolonies.core.client.render.projectile;

import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.jetbrains.annotations.NotNull;

/**
 * Custom renderer for the fire arrows.
 */
public class FireArrowRenderer extends ArrowRenderer<AbstractArrow, FireArrowRenderer.State>
{
    /**
     * Array of different textures.
     */
    private static final Identifier[] RES = new Identifier[]
                                                    {
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow1.png"),
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow2.png"),
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow3.png"),
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow4.png"),
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow5.png"),
                                                      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow6.png")
                                                    };

    /**
     * Arrow render state with the animation tick.
     */
    public static class State extends ArrowRenderState
    {
        public int tickCount;
    }

    public FireArrowRenderer(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(final AbstractArrow entity, final State state, final float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        state.tickCount = entity.tickCount;
    }

    @NotNull
    @Override
    protected Identifier getTextureLocation(@NotNull final State state)
    {
        return RES[state.tickCount % 6];
    }
}
