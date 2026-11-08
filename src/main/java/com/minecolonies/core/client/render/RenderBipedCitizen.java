package com.minecolonies.core.client.render;

import com.minecolonies.api.client.render.modeltype.CitizenModel;
import com.minecolonies.api.client.render.modeltype.CitizenRenderState;
import com.minecolonies.api.client.render.modeltype.IModelType;
import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.client.render.modeltype.registry.IModelTypeRegistry;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.apiimp.initializer.ModModelTypeInitializer;
import com.minecolonies.core.client.render.worldevent.RenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renderer for the citizens.
 */
public class RenderBipedCitizen extends MobRenderer<AbstractEntityCitizen, CitizenRenderState, CitizenModel<AbstractEntityCitizen>>
{
    private static final double  SHADOW_SIZE   = 0.5F;
    public static        boolean isItGhostTime = false;

    /**
     * Ghost time alpha.
     */
    private static final int GHOST_TINT = ARGB.colorFromFloat(0.3F, 1.0F, 1.0F, 1.0F);

    /**
     * The default model.
     */
    private final CitizenModel<AbstractEntityCitizen> defaultModel;

    /**
     * Renders model, see {@link MobRenderer}.
     *
     * @param context the context for this Renderer.
     */
    public RenderBipedCitizen(final EntityRendererProvider.Context context)
    {
        super(context, new CitizenModel<>(context.bakeLayer(ModelLayers.PLAYER)), (float) SHADOW_SIZE);
        this.defaultModel = this.model;
        this.addLayer(new CitizenArmorLayer<>(this,
          ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
          context.getEquipmentRenderer(),
          context.getModelSet(),
          context.getPlayerSkinRenderCache()));
        this.addLayer(new ItemInHandLayer<>(this));
        ModModelTypeInitializer.init(context);
    }

    @Override
    public CitizenRenderState createRenderState()
    {
        return new CitizenRenderState();
    }

    @Override
    public void extractRenderState(final AbstractEntityCitizen citizen, final CitizenRenderState state, final float partialTicks)
    {
        super.extractRenderState(citizen, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(citizen, state, partialTicks, this.itemModelResolver);
        state.extractCitizen(citizen);

        state.rightArmPose = RenderUtils.getArmPose(citizen, InteractionHand.MAIN_HAND);
        state.leftArmPose = RenderUtils.getArmPose(citizen, InteractionHand.OFF_HAND);

        // FABRIC-PORT TODO: 1.21.1 HumanoidModel#young scaled head/body of child citizens (AgeableListModel); 26.x needs baby meshes (MeshTransformer), only the entity age scale applies now.
        state.citizenModel = selectModel(citizen);
        state.texture = getTexture(citizen);

        final ICitizenDataView dataView = citizen.getCitizenDataView();
        if (dataView == null || dataView.getInventory() == null)
        {
            // Armor was never rendered for citizens without data.
            state.headEquipment = ItemStack.EMPTY;
            state.chestEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
        }
        else
        {
            state.headEquipment = displayArmor(state.headEquipment, dataView, EquipmentSlot.HEAD);
            state.chestEquipment = displayArmor(state.chestEquipment, dataView, EquipmentSlot.CHEST);
            state.legsEquipment = displayArmor(state.legsEquipment, dataView, EquipmentSlot.LEGS);
            state.feetEquipment = displayArmor(state.feetEquipment, dataView, EquipmentSlot.FEET);
        }

        if (dataView != null && dataView.hasVisibleStatus() && state.distanceToCameraSq <= 4096.0D)
        {
            state.statusIcon = dataView.getStatusIcon();
            state.statusIconAttachment = citizen.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, citizen.getViewYRot(partialTicks));
        }
        else
        {
            state.statusIcon = null;
            state.statusIconAttachment = null;
        }
    }

    /**
     * Get the armor to display in a slot (display armor overrides the actual armor).
     */
    private static ItemStack displayArmor(final ItemStack current, final ICitizenDataView dataView, final EquipmentSlot slot)
    {
        final ItemStack display = dataView.getDisplayArmor(slot);
        if (display.isEmpty())
        {
            return current;
        }
        return HumanoidArmorLayer.shouldRender(display, slot) ? display.copy() : ItemStack.EMPTY;
    }

    private CitizenModel<AbstractEntityCitizen> selectModel(@NotNull final AbstractEntityCitizen citizen)
    {
        final IModelType modelType = IModelTypeRegistry.getInstance().getModelType(citizen.getModelType());
        CitizenModel<AbstractEntityCitizen> citizenModel = citizen.isFemale() ? modelType.getFemaleModel() : modelType.getMaleModel();
        if (citizenModel == null)
        {
            //no if base, or the next condition, get player model!
            citizenModel = citizen.isFemale() ? modelType.getFemaleModel() : modelType.getMaleModel();
        }

        if (citizen.getCitizenDataView() != null && citizen.getCitizenDataView().getCustomTexture() != null)
        {
            citizenModel = IModelTypeRegistry.getInstance().getModelType(ModModelTypes.CUSTOM_ID).getMaleModel();
        }
        return citizenModel;
    }

    @Override
    public void submit(final CitizenRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera)
    {
        // The model differs per citizen (job and gender).
        this.model = state.citizenModel == null ? defaultModel : state.citizenModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected int getModelTint(final CitizenRenderState state)
    {
        return isItGhostTime ? GHOST_TINT : super.getModelTint(state);
    }

    @Override
    protected @Nullable RenderType getRenderType(final CitizenRenderState state, final boolean isBodyVisible, final boolean forceTransparent, final boolean appearGlowing)
    {
        if (isItGhostTime && isBodyVisible && !forceTransparent)
        {
            return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(this.getTextureLocation(state));
        }
        return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    protected void submitNameDisplay(final CitizenRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera)
    {
        super.submitNameDisplay(state, poseStack, submitNodeCollector, camera);

        if (state.nameTag != null && state.statusIcon != null && state.statusIconAttachment != null)
        {
            final Vec3 vec3 = state.statusIconAttachment;

            poseStack.pushPose();
            poseStack.translate(vec3.x, vec3.y + 0.9, vec3.z);
            poseStack.rotate(camera.orientation);
            poseStack.scale(0.025F, -0.025F, 0.025F);

            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.worldEntityIcon(state.statusIcon), (pose, r) -> {
                r.addVertex(pose, -5, 0, 0).setUv(0, 0).setColor(-1);
                r.addVertex(pose, -5, 10, 0).setUv(0, 1).setColor(-1);
                r.addVertex(pose, 5, 10, 0).setUv(1, 1).setColor(-1);
                r.addVertex(pose, 5, 0, 0).setUv(1, 0).setColor(-1);
            });

            poseStack.popPose();
        }
    }

    private static Identifier getTexture(final AbstractEntityCitizen entity)
    {
        if (entity.getCitizenDataView() != null && entity.getCitizenDataView().getCustomTexture() != null)
        {
            return entity.getCitizenDataView().getCustomTexture();
        }
        return entity.getTexture();
    }

    @NotNull
    @Override
    public Identifier getTextureLocation(final CitizenRenderState state)
    {
        return state.texture;
    }
}
