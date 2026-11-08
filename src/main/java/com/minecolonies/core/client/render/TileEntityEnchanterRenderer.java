package com.minecolonies.core.client.render;

import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityEnchanter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TileEntityEnchanterRenderer implements BlockEntityRenderer<TileEntityColonyBuilding, TileEntityEnchanterRenderer.State>
{
    public static final SpriteId TEXTURE_BOOK = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/enchanting_table_book"));

    /**
     * Render state of the enchanter book.
     */
    public static class State extends BlockEntityRenderState
    {
        public boolean isEnchanter;
        public float   tick;
        public float   rotation;
        public float   pageFlip;
        public float   bookSpread;
    }

    /**
     * The book model to be rendered.
     */
    private final BookModel modelBook;

    /**
     * The sprite getter.
     */
    private final SpriteGetter sprites;

    /**
     * Create the renderer.
     *
     * @param context the context.
     */
    public TileEntityEnchanterRenderer(final BlockEntityRendererProvider.Context context)
    {
        super();
        this.modelBook = new BookModel(context.bakeLayer(ModelLayers.BOOK));
        this.sprites = context.sprites();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      @NotNull final TileEntityColonyBuilding ent,
      @NotNull final State state,
      final float partialTicks,
      @NotNull final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(ent, state, partialTicks, cameraPosition, breakProgress);
        if (ent instanceof TileEntityEnchanter entity)
        {
            state.isEnchanter = true;
            state.tick = (float) entity.tickCount + partialTicks;

            double rotVPrev = entity.bookRotation - entity.bookRotationPrev;
            float circleRot = (float) ((rotVPrev + Math.PI % (2 * Math.PI)) - Math.PI);
            state.rotation = entity.bookRotationPrev + circleRot * partialTicks;
            state.pageFlip = Mth.lerp(partialTicks, entity.pageFlipPrev, entity.pageFlip);
            state.bookSpread = Mth.lerp(partialTicks, entity.bookSpreadPrev, entity.bookSpread);
        }
        else
        {
            state.isEnchanter = false;
        }
    }

    @Override
    public void submit(@NotNull final State state, @NotNull final PoseStack matrixStack, @NotNull final SubmitNodeCollector submitNodeCollector, @NotNull final CameraRenderState camera)
    {
        if (!state.isEnchanter)
        {
            return;
        }

        matrixStack.pushPose();
        matrixStack.translate(0.5D, 0.75D, 0.5D);
        matrixStack.translate(0.0D, (0.1F + Mth.sin(state.tick * 0.1F) * 0.01F), 0.0D);
        matrixStack.rotate(Axis.YP.rotation(-state.rotation));
        matrixStack.rotate(Axis.ZP.rotationDegrees(80.0F));
        float flipA = Mth.frac(state.pageFlip + 0.25F) * 1.6F - 0.3F;
        float flipB = Mth.frac(state.pageFlip + 0.75F) * 1.6F - 0.3F;
        final BookModel.State bookState = BookModel.State.forAnimation(state.tick, Mth.clamp(flipA, 0.0F, 1.0F), Mth.clamp(flipB, 0.0F, 1.0F), state.bookSpread);
        submitNodeCollector.submitModel(this.modelBook, bookState, matrixStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TEXTURE_BOOK, this.sprites, 0);
        matrixStack.popPose();
    }
}
