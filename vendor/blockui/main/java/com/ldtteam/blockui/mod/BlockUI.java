package com.ldtteam.blockui.mod;

import com.ldtteam.shim.api.distmarker.Dist;
import com.ldtteam.shim.bus.api.IEventBus;
import com.ldtteam.shim.fml.common.Mod;
import com.ldtteam.shim.fml.javafmlmod.FMLModContainer;
import com.ldtteam.shim.neoforge.common.NeoForge;

@Mod(BlockUI.MOD_ID)
public class BlockUI
{
    public static final String MOD_ID = "blockui";

    public BlockUI(final FMLModContainer modContainer, final Dist dist)
    {
        final IEventBus modBus = modContainer.getEventBus();
        final IEventBus forgeBus = NeoForge.EVENT_BUS;

        if (dist.isClient())
        {
            modBus.register(ClientLifecycleSubscriber.class);
            forgeBus.register(ClientEventSubscriber.class);
        }
    }
}
