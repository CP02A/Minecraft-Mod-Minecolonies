package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.PathfindingSync;
import com.minecolonies.gametest.framework.RerunTracking;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Test server only:
 * <ul>
 *     <li>at the end of every tick, wait until MineColonies' pathfinding thread has finished every job it was given, so a path requested
 *     in tick N is always ready in tick N+1 on every machine (docs/e2e-tests/framework.md, "Pathfinding stays deterministic");</li>
 *     <li>re-runs ({@code maxAttempts}) are tracked, so the server waits for them ({@link RerunTracking}).</li>
 * </ul>
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin
{
    @Shadow
    private MultipleTestTracker testTracker;

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void minecoloniesGametest$awaitPathfinding(final BooleanSupplier haveTime, final CallbackInfo ci)
    {
        PathfindingSync.awaitIdle();
    }

    @Inject(method = "startTests", at = @At("TAIL"))
    private void minecoloniesGametest$trackReruns(final ServerLevel level, final CallbackInfo ci)
    {
        RerunTracking.install(testTracker);
    }
}
