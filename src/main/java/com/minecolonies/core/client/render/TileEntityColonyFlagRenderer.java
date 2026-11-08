package com.minecolonies.core.client.render;

import com.ldtteam.structurize.blocks.ModBlocks;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagBanner;
import com.minecolonies.core.blocks.decorative.BlockColonyFlagWallBanner;
import com.minecolonies.core.tileentities.TileEntityColonyFlag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TileEntityColonyFlagRenderer implements BlockEntityRenderer<TileEntityColonyFlag, TileEntityColonyFlagRenderer.State>
{
    /**
     * Flag render state.
     */
    public static class State extends BlockEntityRenderState
    {
        public BannerPatternLayers patterns       = BannerPatternLayers.EMPTY;
        public Transformation      transformation = Transformation.IDENTITY;
        public boolean             wall;
        @Nullable
        public Direction           wallFacing;
        public float               phase;
        public boolean             showPlaceholder;
        public final ItemStackRenderState placeholder = new ItemStackRenderState();
    }

    private final SpriteGetter       sprites;
    private final ItemModelResolver  itemModelResolver;
    private final BannerModel        standingModel;
    private final BannerModel        wallModel;
    private final BannerFlagModel    standingFlagModel;
    private final BannerFlagModel    wallFlagModel;

    public TileEntityColonyFlagRenderer(final BlockEntityRendererProvider.Context context)
    {
        super();
        this.sprites = context.sprites();
        this.itemModelResolver = context.itemModelResolver();
        this.standingModel = new BannerModel(context.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(context.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(context.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      final TileEntityColonyFlag flagIn,
      final State state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(flagIn, state, partialTicks, cameraPosition, breakProgress);
        state.patterns = flagIn.getPatterns();
        state.wall = false;
        state.wallFacing = null;
        state.transformation = BannerRenderer.TRANSFORMATIONS.freeTransformations(0);
        state.showPlaceholder = false;

        long gameTime = 0L;
        if (flagIn.getLevel() != null)
        {
            gameTime = flagIn.getLevel().getGameTime();
            final BlockState blockstate = flagIn.getBlockState();
            if (blockstate.getBlock() instanceof BlockColonyFlagBanner)
            {
                state.transformation = BannerRenderer.TRANSFORMATIONS.freeTransformations(blockstate.getValue(BlockColonyFlagBanner.ROTATION));
            }
            else if (blockstate.getBlock() instanceof BlockColonyFlagWallBanner)
            {
                state.wall = true;
                state.wallFacing = blockstate.getValue(BlockColonyFlagWallBanner.HORIZONTAL_FACING);
                state.transformation = BannerRenderer.TRANSFORMATIONS.wallTransformation(state.wallFacing);
            }

            final Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gameMode != null
                  && mc.player.getMainHandItem().getItem() instanceof BannerItem
                  && mc.gameMode.getPlayerMode() == GameType.CREATIVE)
            {
                state.showPlaceholder = true;
                this.itemModelResolver.updateForTopItem(state.placeholder, new ItemStack(ModBlocks.blockSubstitution.get()), ItemDisplayContext.FIXED, flagIn.getLevel(), null, 0);
            }
        }

        final BlockPos blockpos = flagIn.getBlockPos();
        state.phase = ((float) Math.floorMod((long) (blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13) + gameTime, 100L) + partialTicks) / 100.0F;
    }

    @Override
    public void submit(final State state, final PoseStack transform, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera)
    {
        if (state.showPlaceholder)
        {
            // Same placement as the 1.21.1 renderer.
            transform.pushPose();
            if (state.wall && state.wallFacing != null)
            {
                transform.translate(0.5D, -0.16666667F, 0.5D);
                transform.rotate(Axis.YP.rotationDegrees(-state.wallFacing.toYRot()));
                transform.translate(0.0D, -0.3125D, -0.4375D);
            }
            else
            {
                transform.translate(0.5D, 0.5D, 0.5D);
            }
            transform.translate(0.0D, 0.5D, 0.0D);
            transform.scale(0.75F, 0.75F, 0.75F);
            state.placeholder.submit(transform, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            transform.popPose();
        }

        final BannerModel model = state.wall ? this.wallModel : this.standingModel;
        final BannerFlagModel flagModel = state.wall ? this.wallFlagModel : this.standingFlagModel;

        transform.pushPose();
        transform.mulPose(state.transformation);
        submitNodeCollector.submitModel(model, Unit.INSTANCE, transform, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, Sheets.BANNER_BASE, this.sprites, 0);
        submitNodeCollector.submitModel(flagModel, state.phase, transform, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, Sheets.BANNER_BASE, this.sprites, 0);
        BannerRenderer.submitPatterns(this.sprites, transform, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, flagModel, state.phase, true, DyeColor.WHITE, state.patterns);
        transform.popPose();
    }
}
