package com.minecolonies.core.client.render.projectile;

import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Custom renderer for the fire arrows.
 */
public class FireArrowRenderer extends ArrowRenderer<AbstractArrow>
{
    /**
     * Array of different textures.
     */
    private static final Identifier[] RES = new Identifier[]
                                                    {
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow1.png"),
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow2.png"),
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow3.png"),
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow4.png"),
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow5.png"),
                                                      new Identifier(Constants.MOD_ID, "textures/item/magicalarrows/magical_arrow6.png")
                                                    };

    public FireArrowRenderer(final EntityRendererProvider.Context context)
    {
        super(context);
    }

    @NotNull
    @Override
    public Identifier getTextureLocation(@NotNull final AbstractArrow entity)
    {
        return RES[entity.tickCount % 6];
    }
}
