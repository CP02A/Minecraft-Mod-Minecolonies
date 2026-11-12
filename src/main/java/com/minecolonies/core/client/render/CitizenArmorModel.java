package com.minecolonies.core.client.render;

import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.NotNull;

/**
 * Armor model of citizens: the player armor mesh, drawn with child proportions for child citizens.
 * FABRIC-PORT: in 1.21.1 the armor layer copied HumanoidModel#young from the citizen model; 26.x armor models pose themselves from the render
 * state, so the child scaling is applied here as well (see {@link CitizenModel#applyChildProportions}).
 */
public class CitizenArmorModel extends HumanoidModel<CitizenRenderState>
{
    public CitizenArmorModel(final ModelPart root)
    {
        super(root);
    }

    @Override
    public void setupAnim(@NotNull final CitizenRenderState state)
    {
        super.setupAnim(state);
        if (state.isChild)
        {
            CitizenModel.applyChildProportions(this);
        }
    }
}
