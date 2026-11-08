package com.minecolonies.core.client.render;

import com.minecolonies.api.blocks.huts.AbstractBlockMinecoloniesDefault;
import com.minecolonies.api.tileentities.AbstractTileEntityScarecrow;
import com.minecolonies.api.tileentities.ScareCrowType;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.client.model.ScarecrowModel;
import com.minecolonies.core.event.ClientRegistryHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Class to render the scarecrow.
 */
public class TileEntityScarecrowRenderer implements BlockEntityRenderer<AbstractTileEntityScarecrow, TileEntityScarecrowRenderer.State>
{
    /**
     * Offset to the block middle.
     */
    private static final double BLOCK_MIDDLE = 0.5;

    /**
     * Y-Offset in order to have the scarecrow over ground.
     */
    private static final double YOFFSET = 1.5;

    /**
     * Rotate the model some degrees.
     */
    private static final int ROTATION = 180;

    /**
     * Basic rotation to achieve a certain direction.
     */
    private static final int BASIC_ROTATION = 90;

    /**
     * Rotate by amount to go east.
     */
    private static final int ROTATE_EAST = 1;

    /**
     * Rotate by amount to go south.
     */
    private static final int ROTATE_SOUTH = 2;

    /**
     * Rotate by amount to go west.
     */
    private static final int ROTATE_WEST = 3;

    /**
     * The model of the scarecrow.
     */
    @NotNull
    private ScarecrowModel model;

    public static final SpriteId SCARECROW_A = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/blockscarecrowpumpkin"));
    public static final SpriteId SCARECROW_B = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "block/blockscarecrownormal"));

    /**
     * Block display context for the lantern.
     */
    private static final BlockDisplayContext LANTERN_DISPLAY_CONTEXT = BlockDisplayContext.create();

    /**
     * Scarecrow render state.
     */
    public static class State extends BlockEntityRenderState
    {
        public boolean                     render;
        @Nullable
        public Direction                   facing;
        public boolean                     pumpkinHead;
        public boolean                     lantern;
        public final BlockModelRenderState lanternModel = new BlockModelRenderState();
    }

    /**
     * Sprite getter.
     */
    private final SpriteGetter sprites;

    /**
     * Block model resolver.
     */
    private final BlockModelResolver blockModelResolver;

    /**
     * The public constructor for the renderer.
     *
     * @param context the render context.
     */
    public TileEntityScarecrowRenderer(final BlockEntityRendererProvider.Context context)
    {
        super();
        this.model = new ScarecrowModel(context.bakeLayer(ClientRegistryHandler.SCARECROW));
        this.sprites = context.sprites();
        this.blockModelResolver = context.blockModelResolver();
    }

    @Override
    public State createRenderState()
    {
        return new State();
    }

    @Override
    public void extractRenderState(
      final AbstractTileEntityScarecrow te,
      final State state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress)
    {
        BlockEntityRenderer.super.extractRenderState(te, state, partialTicks, cameraPosition, breakProgress);
        final BlockState blockState = te.getBlockState();
        state.render = blockState.getValue(BlockScarecrow.HALF) != DoubleBlockHalf.UPPER;
        state.facing = null;
        //In the case of worldLags tileEntities may sometimes disappear.
        if (te.getLevel() != null && te.getLevel().getBlockState(te.getBlockPos()).getBlock() instanceof BlockScarecrow)
        {
            state.facing = te.getLevel().getBlockState(te.getBlockPos()).getValue(AbstractBlockMinecoloniesDefault.FACING);
        }
        state.pumpkinHead = te.getScarecrowType() == ScareCrowType.PUMPKINHEAD;
        state.lantern = blockState.getValue(BlockScarecrow.LANTERN);
        if (state.lantern)
        {
            this.blockModelResolver.update(state.lanternModel, Blocks.LANTERN.defaultBlockState(), LANTERN_DISPLAY_CONTEXT);
        }
    }

    @Override
    public void submit(final State state, final PoseStack matrixStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera)
    {
        if (!state.render)
        {
            return;
        }
        //Store the transformation
        matrixStack.pushPose();
        //Set viewport to tile entity position to render it
        matrixStack.translate(BLOCK_MIDDLE, YOFFSET, BLOCK_MIDDLE);
        matrixStack.rotate(Axis.ZP.rotationDegrees(ROTATION));

        if (state.facing != null)
        {
            switch (state.facing)
            {
                case EAST:
                    matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_EAST));
                    break;
                case SOUTH:
                    matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_SOUTH));
                    break;
                case WEST:
                    matrixStack.rotate(Axis.YP.rotationDegrees(BASIC_ROTATION * ROTATE_WEST));
                    break;
                default:
                    //don't rotate at all.
            }
        }

        submitNodeCollector.submitModel(this.model, Unit.INSTANCE, matrixStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.pumpkinHead ? SCARECROW_A : SCARECROW_B, this.sprites, 0);

        if (state.lantern)
        {
            matrixStack.pushPose();
            matrixStack.rotate(Axis.ZP.rotationDegrees(180f));
            matrixStack.translate(0.6f, -0.6f, -0.375f);
            matrixStack.scale(0.75f, 0.65f, 0.75f);
            state.lanternModel.submit(matrixStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrixStack.popPose();
        }

        matrixStack.popPose();
    }
}
