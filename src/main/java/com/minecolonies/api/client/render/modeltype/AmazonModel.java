package com.minecolonies.api.client.render.modeltype;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.NotNull;

/**
 * Amazon model.
 */
public class AmazonModel<T extends AbstractEntityMinecoloniesMonster> extends HumanoidModel<HumanoidRenderState>
{
    public AmazonModel(final ModelPart part)
    {
        super(part);
    }

    @Override
    public void setupAnim(@NotNull final HumanoidRenderState entity)
    {
        super.setupAnim(entity);
        // FABRIC-PORT: 1.21.1 reset parts to fixed vanilla positions (head 0, arms 2, legs 12) before this, so the offsets were
        // subtracted here. 26.x resets to the mesh pose, and the amazon meshes already contain the offsets (head -3, arms 0, legs 9).
        // The legs ended at 12 - 3.5 = 8.5 in 1.21.1, half a pixel above the mesh pivot; keep that (relative, so crouching still applies).
        rightLeg.y += 8.5F - rightLeg.getInitialPose().y();
        leftLeg.y += 8.5F - leftLeg.getInitialPose().y();
    }
}
