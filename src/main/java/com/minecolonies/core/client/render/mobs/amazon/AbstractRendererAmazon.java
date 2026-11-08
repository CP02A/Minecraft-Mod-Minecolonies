package com.minecolonies.core.client.render.mobs.amazon;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.render.mobs.AbstractRaiderRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Abstract for rendering amazons.
 */
public abstract class AbstractRendererAmazon<T extends AbstractEntityMinecoloniesMonster, M extends HumanoidModel<HumanoidRenderState>> extends AbstractRaiderRenderer<T, M>
{
    public AbstractRendererAmazon(final EntityRendererProvider.Context context, final M modelBipedIn, final float shadowSize)
    {
        super(context, modelBipedIn, shadowSize);
    }
}
