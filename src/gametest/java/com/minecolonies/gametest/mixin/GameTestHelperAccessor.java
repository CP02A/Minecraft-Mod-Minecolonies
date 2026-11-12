package com.minecolonies.gametest.mixin;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives the fixture the running test's {@link GameTestInfo} (to register end-of-test cleanup and to fail a test from a wait step).
 */
@Mixin(GameTestHelper.class)
public interface GameTestHelperAccessor
{
    @Accessor("testInfo")
    GameTestInfo minecoloniesGametest$getTestInfo();
}
