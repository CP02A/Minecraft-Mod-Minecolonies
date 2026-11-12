package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.E2EMode;
import com.minecolonies.gametest.framework.PathfindingSync;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Client game test runs only (watch mode, UI tests): the integrated server waits for MineColonies' pathfinding thread at the end of every
 * tick, exactly like the headless test server does ({@link GameTestServerMixin}), so tests behave the same in a game window. Does nothing
 * in a normal game ({@code runClient}).
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin
{
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void minecoloniesGametest$awaitPathfinding(final BooleanSupplier haveTime, final CallbackInfo ci)
    {
        if (E2EMode.isClientTestRun())
        {
            PathfindingSync.awaitIdle();
        }
    }
}
