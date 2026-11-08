package com.minecolonies.api.client.render.modeltype;

import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * FABRIC-PORT: render state for citizens (26.x entity renderers work on render states instead of entities).
 * Mirrors the entity accessors the citizen models used before, so the model code stays unchanged.
 */
public class CitizenRenderState extends HumanoidRenderState
{
    /**
     * The client side data view of the citizen, may be null.
     */
    @Nullable
    public ICitizenDataView citizenDataView;

    /**
     * Render metadata of the citizen.
     */
    public String renderMetadata = "";

    /**
     * Civilian id.
     */
    public int civilianId;

    /**
     * If the citizen is female.
     */
    public boolean female;

    /**
     * Texture to use.
     */
    @Nullable
    public Identifier texture;

    /**
     * Model to use for this citizen.
     */
    @Nullable
    public CitizenModel<AbstractEntityCitizen> citizenModel;

    /**
     * The status icon to render above the name, null if none.
     */
    @Nullable
    public Identifier statusIcon;

    /**
     * Attachment point for the status icon.
     */
    @Nullable
    public Vec3 statusIconAttachment;

    /**
     * Copies of all equipment slot contents.
     */
    public final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    /**
     * Fill the citizen specific fields from the entity.
     *
     * @param citizen the citizen.
     */
    public void extractCitizen(final AbstractEntityCitizen citizen)
    {
        this.citizenDataView = citizen.getCitizenDataView();
        this.renderMetadata = citizen.getRenderMetadata();
        this.civilianId = citizen.getCivilianID();
        this.female = citizen.isFemale();
        this.equipment.clear();
        for (final EquipmentSlot slot : EquipmentSlot.values())
        {
            final ItemStack stack = citizen.getItemBySlot(slot);
            this.equipment.put(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    @Nullable
    public ICitizenDataView getCitizenDataView()
    {
        return citizenDataView;
    }

    public String getRenderMetadata()
    {
        return renderMetadata;
    }

    public int getCivilianID()
    {
        return civilianId;
    }

    public Pose getPose()
    {
        return pose;
    }

    public ItemStack getItemBySlot(final EquipmentSlot slot)
    {
        return equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    public boolean hasItemInSlot(final EquipmentSlot slot)
    {
        return !getItemBySlot(slot).isEmpty();
    }

    public ItemStack getMainHandItem()
    {
        return getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public boolean isFemale()
    {
        return female;
    }
}
