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
 * Special item renderer for the spear (was a BlockEntityWithoutLevelRenderer before 26.x).
 * FABRIC-PORT TODO: now a {@link SpecialModelRenderer}; it has to be referenced from the spear's item model definition
 * ("type": "minecraft:special", "model": {"type": "minecolonies:spear"}) and {@link #register()} has to be called during client init.
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
