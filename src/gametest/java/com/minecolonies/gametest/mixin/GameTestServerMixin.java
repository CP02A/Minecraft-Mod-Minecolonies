package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.PathfindingSync;
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Test server only: at the end of every tick, wait until MineColonies' pathfinding thread has finished every job it was given, so a path
 * requested in tick N is always ready in tick N+1 on every machine (docs/e2e-tests/framework.md, "Pathfinding stays deterministic").
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin
{
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void minecoloniesGametest$awaitPathfinding(final BooleanSupplier haveTime, final CallbackInfo ci)
    {
        PathfindingSync.awaitIdle();
    }
}
