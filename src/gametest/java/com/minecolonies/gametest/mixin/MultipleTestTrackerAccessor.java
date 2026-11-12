package com.minecolonies.gametest.mixin;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.MultipleTestTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;

/**
 * Gives {@link com.minecolonies.gametest.framework.RerunTracking} the tracked tests, so a failed attempt that is re-run can be replaced by
 * its re-run.
 */
@Mixin(MultipleTestTracker.class)
public interface MultipleTestTrackerAccessor
{
    @Accessor("tests")
    Collection<GameTestInfo> minecoloniesGametest$getTests();
}
