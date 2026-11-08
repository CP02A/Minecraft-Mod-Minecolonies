package com.ldtteam.domumornamentum.client.model.item;

import com.ldtteam.domumornamentum.util.Constants;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Range-select item model property used by Domum's generated item models
 * ({@code domum_ornamentum:door_type}, {@code fancy_door_type}, {@code trapdoor_type}, ...): the index of the item's
 * {@code type} block state property value (from the {@code minecraft:block_state} component, or the block's default
 * state) within the property's possible values.
 */
public final class BlockTypeItemProperty implements RangeSelectItemModelProperty
{
    private static final String[] IDS = {"door_type", "fancy_door_type", "fancy_trapdoor_type", "panel_type", "post_type", "trapdoor_type"};

    private final MapCodec<BlockTypeItemProperty> codec;

    private BlockTypeItemProperty()
    {
        this.codec = MapCodec.unit(this);
    }

    /**
     * Registers all type properties (client init, before model loading).
     */
    private static boolean registered = false;

    public static synchronized void register()
    {
        if (registered)
        {
            return;
        }
        registered = true;
        for (final String id : IDS)
        {
            final Identifier key = Identifier.fromNamespaceAndPath(Constants.MOD_ID, id);
            RangeSelectItemModelProperties.ID_MAPPER.put(key, new BlockTypeItemProperty().codec);
        }
    }

    @Override
    public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed)
    {
        if (!(itemStack.getItem() instanceof final BlockItem blockItem))
        {
            return 0;
        }
        final Block block = blockItem.getBlock();
        final Property<?> property = block.getStateDefinition().getProperty(Constants.TYPE_BLOCK_PROPERTY);
        if (property == null)
        {
            return 0;
        }
        return index(property, block, itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY));
    }

    private static <T extends Comparable<T>> float index(final Property<T> property, final Block block, final BlockItemStateProperties stateProperties)
    {
        T value = stateProperties.get(property);
        if (value == null)
        {
            value = block.defaultBlockState().getValue(property);
        }
        final List<T> values = new ArrayList<>(property.getPossibleValues());
        return Math.max(0, values.indexOf(value));
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type()
    {
        return codec;
    }
}
