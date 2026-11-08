package com.ldtteam.structurize.network.messages;

import com.ldtteam.common.network.AbstractPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.client.gui.WindowUndoRedo;
import com.ldtteam.structurize.management.Manager;
import com.ldtteam.structurize.util.ChangeStorage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.ldtteam.structurize.api.Tuple;
import net.minecraft.world.entity.player.Player;
import com.ldtteam.shim.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class OperationHistoryMessage extends AbstractPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forBothSides(Constants.MOD_ID, "operation_history", OperationHistoryMessage::new);

    /**
     * List of operations and their IDs
     */
    private final List<Tuple<String, Integer>> operationIDs;

    /**
     * Empty constructor used when registering the
     */
    protected OperationHistoryMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        final int size = buf.readVarInt();
        operationIDs = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
        {
            operationIDs.add(new Tuple<>(buf.readUtf(), buf.readInt()));
        }
    }

    public OperationHistoryMessage()
    {
        super(TYPE);
        operationIDs = new ArrayList<>();
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        buf.writeVarInt(operationIDs.size());
        for (final Tuple<String, Integer> operation : operationIDs)
        {
            buf.writeUtf(operation.getA());
            buf.writeInt(operation.getB());
        }
    }

    @Override
    protected void onClientExecute(final IPayloadContext context, final Player player)
    {
        WindowUndoRedo.lastOperations = operationIDs;
    }

    @Override
    protected void onServerExecute(final IPayloadContext context, final ServerPlayer player)
    {
        final List<ChangeStorage> operations = Manager.getChangeStoragesForPlayer(player.getUUID());
        for (final ChangeStorage storage : operations)
        {
            operationIDs.add(new Tuple<>(storage.getOperation().getString(), storage.getID()));
        }

        this.sendToPlayer(player);        
    }
}
