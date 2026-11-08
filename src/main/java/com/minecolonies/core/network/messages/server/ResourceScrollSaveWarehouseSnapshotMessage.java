package com.minecolonies.core.network.messages.server;

import com.ldtteam.common.network.AbstractServerPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.items.component.BuildingId;
import com.minecolonies.api.items.component.WarehouseSnapshot;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.items.ItemResourceScroll;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.ldtteam.shim.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Message sent to the server when the client saves a new snapshot by clicking on a warehouse.
 */
public class ResourceScrollSaveWarehouseSnapshotMessage extends AbstractServerPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "resource_scroll_save_warehouse_snapshot", ResourceScrollSaveWarehouseSnapshotMessage::new);

    /**
     * The position of the builder.
     */
    private final BlockPos builderPos;

    /**
     * The warehouse snapshot mapping.
     */
    @NotNull
    private final Map<String, Integer> snapshot;

    /**
     * The hash of the current work order (if any).
     */
    @NotNull
    private final String workOrderHash;

    /**
     * Empty constructor used when registering the message.
     */
    public ResourceScrollSaveWarehouseSnapshotMessage(final BlockPos builderPos)
    {
        this(builderPos, Map.of(), "");
    }

    /**
     * Empty constructor used when registering the message.
     */
    public ResourceScrollSaveWarehouseSnapshotMessage(final BlockPos builderPos, @NotNull final Map<String, Integer> snapshot, @NotNull final String workOrderHash)
    {
        super(TYPE);
        this.builderPos = builderPos;
        this.snapshot = snapshot;
        this.workOrderHash = workOrderHash;
    }

    protected ResourceScrollSaveWarehouseSnapshotMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        builderPos = buf.readBoolean() ? buf.readBlockPos() : null;
        final int size = buf.readVarInt();
        snapshot = new HashMap<>(size);
        for (int i = 0; i < size; i++)
        {
            snapshot.put(buf.readUtf(), buf.readInt());
        }
        workOrderHash = buf.readUtf(32767);
    }

    @Override
    protected void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        buf.writeBoolean(builderPos != null);
        if (builderPos != null)
        {
            buf.writeBlockPos(builderPos);
        }
        buf.writeVarInt(snapshot.size());
        for (final Map.Entry<String, Integer> entry : snapshot.entrySet())
        {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeUtf(workOrderHash);
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player)
    {
        player.getInventory().getNonEquipmentItems().stream()
          .filter(stack -> stack.getItem() instanceof ItemResourceScroll)
          .filter(stack -> Objects.equals(builderPos, BuildingId.readFromItemStack(stack).id()))
          .forEach(stack -> new WarehouseSnapshot(snapshot, workOrderHash).writeToItemStack(stack));
    }
}
