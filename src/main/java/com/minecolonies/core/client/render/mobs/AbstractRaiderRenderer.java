package com.minecolonies.core.client.render.mobs;

import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.client.render.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;

/**
 * Shared base for all raider renderers (humanoid model, armor, held items).
 * FABRIC-PORT: 26.x renderers work on render states; the texture is resolved from the entity during extraction.
 */
public abstract class AbstractRaiderRenderer<T extends AbstractEntityMinecoloniesMonster, M extends HumanoidModel<HumanoidRenderState>>
  extends HumanoidMobRenderer<T, HumanoidRenderState, M>
{
    /**
     * Render state key holding the texture of the raider.
     */
    public static final RenderStateDataKey<Identifier> TEXTURE = RenderStateDataKey.create(() -> "minecolonies:raider_texture");

    protected AbstractRaiderRenderer(final EntityRendererProvider.Context context, final M model, final float shadowSize)
    {
        super(context, model, shadowSize);
        this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new), context.getEquipmentRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState()
    {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(final T raider, final HumanoidRenderState state, final float partialTicks)
    {
        super.extractRenderState(raider, state, partialTicks);
        state.rightArmPose = RenderUtils.getArmPose(raider, InteractionHand.MAIN_HAND);
        state.leftArmPose = RenderUtils.getArmPose(raider, InteractionHand.OFF_HAND);
        state.setData(TEXTURE, getTextureLocation(raider));
    }

    /**
     * Get the texture of the given raider.
     *
     * @param entity the raider.
     * @return the texture.
     */
    @NotNull
    public abstract Identifier getTextureLocation(final T entity);

    @NotNull
    @Override
    public Identifier getTextureLocation(final HumanoidRenderState state)
    {
        return state.getData(TEXTURE);
    }

    /**
     * Creates the old (1.21.1) full body outer armor humanoid model (deformation 1.0, 64x32), used as body model by some raiders.
     *
     * @return the model.
     */
    public static HumanoidModel<HumanoidRenderState> outerArmorBodyModel()
    {
        return new HumanoidModel<>(createArmorBody(new CubeDeformation(1.0F)));
    }

    /**
     * Creates the old (1.21.1) full body inner armor humanoid model (deformation 0.5, 64x32), used as body model by some raiders.
     *
     * @return the model.
     */
    public static HumanoidModel<HumanoidRenderState> innerArmorBodyModel()
    {
        return new HumanoidModel<>(createArmorBody(new CubeDeformation(0.5F)));
    }

    private static ModelPart createArmorBody(final CubeDeformation g)
    {
        final MeshDefinition mesh = HumanoidModel.createMesh(g, 0.0F);
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, g.extend(-0.1F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
