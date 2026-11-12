package com.minecolonies.gametest.client;

import com.minecolonies.api.blocks.AbstractBlockBarrel;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.blocks.types.BarrelType;
import com.minecolonies.core.compatibility.jei.RenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * UI-12: a screen that draws blocks the way MineColonies' JEI recipe categories do ({@link RenderHelper#renderBlock}, same arguments as
 * CompostRecipeCategory and GenericRecipeCategory), with the block's item icon below each for comparison. JEI itself is not in the test
 * runtime; RenderHelper doesn't depend on it.
 */
public class BlockRenderTestScreen extends Screen
{
    private static final int TEXT = 0xFFFFFFFF;

    /**
     * Background color of the drawing area.
     */
    static final int BACKGROUND = 0xFF5A5A5A;

    /**
     * Anchor (x, y of RenderHelper#renderBlock) of the first block of each row, and the distance between blocks in a row (GUI units).
     */
    static final int FIRST_X          = 50;
    static final int STEP_X           = 70;
    static final int BARREL_Y         = 70;
    static final int INTERMEDIATE_Y   = 172;

    public BlockRenderTestScreen()
    {
        super(Component.literal("JEI block rendering"));
    }

    /**
     * Composting barrel states as CompostRecipeCategory shows them (the fill level changes with a timer there).
     */
    static List<BlockState> barrels()
    {
        return List.of(barrel(BarrelType.ZERO), barrel(BarrelType.SIXTY), barrel(BarrelType.DONE));
    }

    /**
     * Intermediate blocks as GenericRecipeCategory shows them: a furnace (smelting) and a grown wheat crop (a block without its own item).
     */
    static List<BlockState> intermediates()
    {
        return List.of(Blocks.FURNACE.defaultBlockState(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
    }

    private static BlockState barrel(final BarrelType type)
    {
        return ModBlocks.blockBarrel.defaultBlockState().setValue(AbstractBlockBarrel.FACING, Direction.SOUTH).setValue(AbstractBlockBarrel.VARIANT, type);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a)
    {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.fill(10, 10, this.width - 10, this.height - 10, BACKGROUND);
        graphics.text(this.font, "RenderHelper#renderBlock (JEI categories); item icon below for comparison", 16, 16, TEXT);

        // Composting: like CompostRecipeCategory#draw (anchor 40, 20 in a 120 wide category, pitch -30, yaw 20, scale 25).
        graphics.text(this.font, "composting barrel: 0%, 60%, done", 16, 32, TEXT);
        int x = FIRST_X;
        for (final BlockState barrel : barrels())
        {
            RenderHelper.renderBlock(graphics, barrel, x, BARREL_Y, 100, -30F, 20F, 25F);
            graphics.item(new ItemStack(barrel.getBlock()), x - 8, BARREL_Y + 42);
            x += STEP_X;
        }

        // Crafter intermediate blocks: like GenericRecipeCategory#draw (pitch -30, yaw 30, scale 16).
        graphics.text(this.font, "intermediate: furnace, wheat crop (item: seeds)", 16, 140, TEXT);
        x = FIRST_X;
        for (final BlockState block : intermediates())
        {
            RenderHelper.renderBlock(graphics, block, x, INTERMEDIATE_Y, 100, -30F, 30F, 16F);
            graphics.item(new ItemStack(block.getBlock()), x - 8, INTERMEDIATE_Y + 28);
            x += STEP_X;
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
