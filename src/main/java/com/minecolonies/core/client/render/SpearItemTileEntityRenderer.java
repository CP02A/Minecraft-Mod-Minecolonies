package com.minecolonies.core.client.render;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.client.model.SpearModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Special item renderer for the spear: draws the 3D spear model in hand, on the head and in item frames (the GUI, the ground and shelves
 * use the flat sprite, like 1.21.1's separate_transforms model did for GUI and ground).
 * FABRIC-PORT: was a BlockEntityWithoutLevelRenderer (IClientItemExtensions#getCustomRenderer) before 26.x; now a {@link SpecialModelRenderer}
 * referenced from assets/minecolonies/items/spear.json ("type": "minecraft:special", "model": {"type": "minecolonies:spear"}), registered
 * by {@link #register()} during client init (ClientRegistryHandler#doClientStuff, before the first resource load). The display transforms
 * come from models/item/spear_in_hand.json and spear_throwing.json, as before; like 1.21.1 the model is drawn without the trident's
 * (1, -1, -1) flip.
 */
public class SpearItemTileEntityRenderer implements NoDataSpecialModelRenderer
{
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entity/spear.png");

    /**
     * Id of this special renderer type.
     */
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "spear");

    private final SpearModel model;

    public SpearItemTileEntityRenderer(final SpearModel model)
    {
        this.model = model;
    }

    /**
     * Register the special renderer type.
     */
    public static void register()
    {
        SpecialModelRenderers.ID_MAPPER.put(ID, Unbaked.MAP_CODEC);
    }

    @Override
    public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final int overlayCoords,
      final boolean hasFoil,
      final int outlineColor)
    {
        poseStack.pushPose();
        if (hasFoil)
        {
            submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, RenderTypes.entitySolidGlint(TEXTURE), lightCoords, overlayCoords, outlineColor);
        }
        else
        {
            submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, TEXTURE, lightCoords, overlayCoords, outlineColor);
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output)
    {
        final PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
    }

    /**
     * Unbaked spear renderer.
     */
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type()
        {
            return MAP_CODEC;
        }

        @Override
        public SpearItemTileEntityRenderer bake(final SpecialModelRenderer.BakingContext context)
        {
            return new SpearItemTileEntityRenderer(new SpearModel(context.entityModelSet().bakeLayer(ModelLayers.TRIDENT)));
        }
    }
}
