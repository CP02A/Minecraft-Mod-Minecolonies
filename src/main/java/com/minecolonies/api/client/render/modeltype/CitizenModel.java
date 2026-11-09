package com.minecolonies.api.client.render.modeltype;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.NotNull;

/**
 * Citizen model.
 */
// FABRIC-PORT: models now animate from a CitizenRenderState (26.x render states); T kept for source compatibility.
public class CitizenModel<T extends AbstractEntityCitizen> extends HumanoidModel<CitizenRenderState>
{
    /**
     * Working render meta.
     */
    private static final String RENDER_META_WORKING = "working";

    public static boolean isItApril1st = false;

    public CitizenModel(final ModelPart part)
    {
        super(part, RenderTypes::entityCutout);
    }

    @Override
    public void setupAnim(@NotNull final CitizenRenderState citizen)
    {
        super.setupAnim(citizen);
        // FABRIC-PORT: 1.21.1 HumanoidModel set arm zRot to 0 every frame, so the arm zRots in the Blockbench meshes never showed.
        // 26.x keeps the mesh value (resetPose) and adds the idle bob on top; remove the mesh part to keep the old look.
        rightArm.zRot -= rightArm.getInitialPose().zRot();
        leftArm.zRot -= leftArm.getInitialPose().zRot();

        // FABRIC-PORT: 1.21.1 reset body.xRot to 0 before this; 26.x resets to the mesh pose (e.g. the courier body is tilted in its mesh).
        // Only replace the rest pose, keep rotations applied by vanilla animations (crouching etc.).
        if (body.xRot == body.getInitialPose().xRot())
        {
            body.xRot = getActualRotation(citizen);
        }

        // The head pitch is always set from the look direction (both versions), so 0 still means "looking straight ahead".
        if (head.xRot == 0)
        {
            head.xRot = getActualRotation(citizen);
        }

        if (citizen.getCitizenDataView() != null && citizen.getCitizenDataView().getCustomTextureUUID() != null)
        {
            head.visible = false;
            hat.visible = false;
        }
        else
        {
            head.visible = true;
            hat.visible = true;
        }

        if (isItApril1st)
        {
            switch (citizen.getCivilianID() % 7)
            {
                case 0:
                    leftArm.visible = false;
                    break;
                case 1:
                    rightArm.visible = false;
                    break;
                case 2:
                    body.visible = false;
                    break;
                case 3:
                    head.visible = false;
                    break;
                case 4:
                    hat.visible = false;
                    break;
                case 5:
                    leftLeg.visible = false;
                    break;
                case 6:
                    rightLeg.visible = false;
                    break;
            }
        }
    }

    public static LayerDefinition createMesh()
    {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    /**
     * Override to change body rotation.
     *
     * @return the rotation.
     */
    public float getActualRotation(@NotNull final CitizenRenderState entity)
    {
        return 0;
    }

    /**
     * Check if the citizen is supposed to be working.
     * @param citizen the citizen entity to check.
     * @return true if so.
     */
    public boolean isWorking(final CitizenRenderState citizen)
    {
        return citizen.getRenderMetadata().contains(RENDER_META_WORKING);
    }

    /**
     * Check if the hat should be displayed.
     * @param citizen the citizen entity to check.
     * @return true if so.
     */
    public boolean displayHat(final CitizenRenderState citizen)
    {
        if (citizen.getPose() == Pose.SLEEPING || !citizen.getItemBySlot(EquipmentSlot.HEAD).isEmpty())
        {
            return false;
        }
        return citizen.getCitizenDataView() == null || (citizen.getCitizenDataView().getDisplayArmor(EquipmentSlot.HEAD).isEmpty() && citizen.getCitizenDataView().getCustomTextureUUID() == null);
    }
}
