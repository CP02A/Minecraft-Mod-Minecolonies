package com.minecolonies.core.client.gui.containers;

import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.inventory.container.ContainerCitizenInventory;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Pose;
import org.joml.Vector3f;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.Optional;

/**
 * ------------ Class not Documented ------------
 */
public class WindowCitizenInventory extends AbstractContainerScreen<ContainerCitizenInventory>
{
    /**
     * Texture res loc.
     */
    private static final Identifier TEXT = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/citizen_container.png");

    /**
     * Offset inside the texture to use.
     */
    private static final int TEXTURE_OFFSET = 130;

    /**
     * Offset of each slot.
     */
    private static final int SLOT_OFFSET = 18;

    /**
     * Size of the custom texture.
     */
    private static final int TEXTURE_SIZE = 350;

    /**
     * Offet of the screen for the texture.
     */
    private static final int TEXTURE_HEIGHT = 96;

    /**
     * General y offset.
     */
    private static final int Y_OFFSET = 114;

    /**
     * Amount of slots each row.
     */
    private static final int SLOTS_EACH_ROW = 9;

    /**
     * Current active citizen inventory window
     */
    public static WindowCitizenInventory activeCitizenInventory = null;

    /**
     * Citizen of this UI
     */
    private ICitizen citizenData;

    /**
     * window height is calculated with these values; the more rows, the heigher
     */
    private final int inventoryRows;

    public WindowCitizenInventory(final ContainerCitizenInventory container, final Inventory playerInventory, final Component iTextComponent)
    {
        super(container, playerInventory, iTextComponent, 245, Y_OFFSET + Math.min(SLOTS_EACH_ROW, (container.getItems().size() - 36) / 9) * SLOT_OFFSET);
        this.inventoryRows = (container.getItems().size() - 36) / 9;
        activeCitizenInventory = this;
        citizenData = container.getCitizenData();
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    @Override
    protected void extractLabels(@NotNull final GuiGraphicsExtractor stack, final int mouseX, final int mouseY)
    {
        stack.text(this.font, this.menu.getDisplayName(), 80, 9, 0xFF404040, false);
        stack.text(this.font, this.playerInventoryTitle.getString(), 8, 25 + this.inventoryRows * SLOT_OFFSET, 0xFF404040, false);
    }

    /**
     * Draws the background layer of this container (behind the items).
     */
    @Override
    public void extractBackground(@NotNull final GuiGraphicsExtractor stack, int mouseX, int mouseY, float partialTicks)
    {
        super.extractBackground(stack, mouseX, mouseY, partialTicks);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;


        stack.blit(RenderPipelines.GUI_TEXTURED, TEXT, i, j, 0, 0, this.imageWidth,  10 + this.inventoryRows * SLOT_OFFSET + 12, TEXTURE_SIZE, TEXTURE_SIZE);


        stack.blit(RenderPipelines.GUI_TEXTURED, TEXT, i, j + 10 + this.inventoryRows * SLOT_OFFSET + 12, 0, TEXTURE_OFFSET, this.imageWidth, TEXTURE_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);


        //stack.blit(RenderPipelines.GUI_TEXTURED, TEXT, i, j, 0, 0, this.imageWidth,  this.inventoryRows * SLOT_OFFSET + 12, TEXTURE_SIZE, TEXTURE_SIZE);


        stack.blit(RenderPipelines.GUI_TEXTURED, TEXT, i + 172, j + 22, 0, 227, 49, 72, TEXTURE_SIZE, TEXTURE_SIZE);

        for (int index = 0; index < 4; index++)
        {
            stack.blit(RenderPipelines.GUI_TEXTURED, TEXT, i + 222, j + 22 + index * 18, 0, 300, 18, 18, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        renderEntityInInventoryFollowsMouse(stack, i + 172, j + 22, i + 222, j + 94, 30, (float)(i + 51) - mouseX, (float)(j + 75 - 50) - mouseY, this.menu.getEntity());
    }


    /**
     * Render the entity in a box, rotated following the given (relative) mouse offsets.
     */
    public static void renderEntityInInventoryFollowsMouse(
      GuiGraphicsExtractor stack, int x0, int y0, int x1, int y1, int scale, float mouseX, float mouseY, Optional<? extends Entity> optionalEntity)
    {
        optionalEntity.ifPresent(entity -> {
            if (!(entity instanceof LivingEntity livingEntity))
            {
                return;
            }
            float xAngle = (float) Math.atan(mouseX / 40.0F);
            float yAngle = (float) Math.atan(mouseY / 40.0F);
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20.0F * (float) (Math.PI / 180.0));
            rotation.mul(xRotation);

            final EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            final EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(livingEntity);
            final EntityRenderState renderState = renderer.createRenderState(livingEntity, 1.0F);
            renderState.shadowPieces.clear();
            renderState.outlineColor = 0;
            if (renderState instanceof LivingEntityRenderState livingRenderState)
            {
                livingRenderState.bodyRot = 180.0F + xAngle * 20.0F;
                livingRenderState.yRot = xAngle * 20.0F;
                livingRenderState.xRot = livingRenderState.pose != Pose.FALL_FLYING ? -yAngle * 20.0F : 0.0F;
                livingRenderState.boundingBoxWidth = livingRenderState.boundingBoxWidth / livingRenderState.scale;
                livingRenderState.boundingBoxHeight = livingRenderState.boundingBoxHeight / livingRenderState.scale;
                livingRenderState.scale = 1.0F;
            }

            final Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
            stack.entity(renderState, scale, translation, rotation, xRotation, x0, y0, x1, y1);
        });
    }

    @Override
    public void onClose()
    {
        activeCitizenInventory = null;
        super.onClose();
    }

    /**
     * Get the citizen for this UI
     *
     * @return
     */
    public ICitizen getCitizenData()
    {
        return citizenData;
    }
}
