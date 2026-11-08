package com.ldtteam.shim.neoforge.network.handling;

import com.ldtteam.shim.fml.LogicalSide;
import net.minecraft.network.protocol.PacketFlow;

/**
 * Direction of a payload (NeoForge shim). NeoForge returns the vanilla PacketFlow (with the NeoForge extension
 * getReceptionSide()); vanilla on Fabric has no such extension, so the shim uses this mirror enum.
 */
public enum PayloadFlow
{
    SERVERBOUND,
    CLIENTBOUND;

    public LogicalSide getReceptionSide()
    {
        return this == SERVERBOUND ? LogicalSide.SERVER : LogicalSide.CLIENT;
    }

    public boolean isServerbound()
    {
        return this == SERVERBOUND;
    }

    public boolean isClientbound()
    {
        return this == CLIENTBOUND;
    }

    public PacketFlow toVanilla()
    {
        return this == SERVERBOUND ? PacketFlow.SERVERBOUND : PacketFlow.CLIENTBOUND;
    }
}
