package com.minecolonies.core.client.render.mobs.norsemen;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.render.mobs.AbstractRaiderRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Abstract for rendering amazons.
 */
public abstract class AbstractRendererNorsemen<T extends AbstractEntityMinecoloniesMonster, M extends HumanoidModel<HumanoidRenderState>> extends AbstractRaiderRenderer<T, M>
{
    public AbstractRendererNorsemen(final EntityRendererProvider.Context context, final M modelBipedIn, final float shadowSize)
    {
        super(context, modelBipedIn, shadowSize);
    }
}
