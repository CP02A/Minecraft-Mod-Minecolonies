package com.minecolonies.gametest.mixin;

import com.minecolonies.gametest.framework.SerialBatches;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

/**
 * Vanilla runs all tests of one environment in one batch, in parallel. Tests in the colony environment must run one at a time (their
 * colonies claim chunks around the town hall), so every colony-environment batch is split into batches of one test. Batches run one after
 * the other. Covers both the first run and re-runs ({@code maxAttempts}).
 */
@Mixin(GameTestBatchFactory.class)
public abstract class GameTestBatchFactoryMixin
{
    @Inject(method = "divideIntoBatches", at = @At("RETURN"), cancellable = true)
    private static void minecoloniesGametest$serialColonyBatches(
      final Collection<Holder.Reference<GameTestInstance>> allTests,
      final GameTestBatchFactory.TestDecorator decorator,
      final MinecraftServer server,
      final CallbackInfoReturnable<List<GameTestBatch>> cir)
    {
        cir.setReturnValue(SerialBatches.split(cir.getReturnValue()));
    }

    @Inject(method = "fromGameTestInfo(I)Lnet/minecraft/gametest/framework/GameTestRunner$GameTestBatcher;", at = @At("RETURN"), cancellable = true)
    private static void minecoloniesGametest$serialColonyRerunBatches(final int maxTestsPerBatch, final CallbackInfoReturnable<GameTestRunner.GameTestBatcher> cir)
    {
        final GameTestRunner.GameTestBatcher inner = cir.getReturnValue();
        cir.setReturnValue(infos -> SerialBatches.split(inner.batch(infos)));
    }
}
