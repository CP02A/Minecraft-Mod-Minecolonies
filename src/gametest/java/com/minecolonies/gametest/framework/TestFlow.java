package com.minecolonies.gametest.framework;

import com.minecolonies.gametest.mixin.GameTestHelperAccessor;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A test as a list of steps that run one after another, driven by game ticks: actions, waits for an outcome and checks. Built on the vanilla
 * {@link GameTestSequence}, with three additions:
 * <ul>
 *     <li>every wait has a description and an optional "what was found" supplier, so a timeout says what the test expected and what it
 *     found ("Timed out after 600 ticks waiting for: 4 citizens. Found: 2 citizens");</li>
 *     <li>a wait can have its own tick limit, shorter than the test's {@code maxTicks};</li>
 *     <li>any exception in a step (not only assertion errors) fails the test with the step name instead of crashing the server tick.</li>
 * </ul>
 * Example:
 * <pre>{@code
 * fixture.flow()
 *     .run("hire builder", () -> ...)
 *     .awaitUntil("builder reaches his hut", 1200, () -> distance() < 3, () -> "distance " + distance())
 *     .check("hut is level 1", () -> hut.getBuildingLevel() == 1, () -> "level " + hut.getBuildingLevel())
 *     .succeed();
 * }</pre>
 * Steps run in the order they were added. A step that finishes at once (an action, a check, a wait whose condition already holds) lets the
 * next step run in the same tick; a wait that does not hold yet is tried again every tick. A failure stops the flow and fails the test.
 */
public final class TestFlow
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestFlow.class);

    private final GameTestHelper    helper;
    private final GameTestInfo      info;
    private final GameTestSequence  sequence;

    TestFlow(final GameTestHelper helper)
    {
        this.helper = helper;
        this.info = ((GameTestHelperAccessor) helper).minecoloniesGametest$getTestInfo();
        this.sequence = helper.startSequence();
    }

    /**
     * Runs an action once. Any exception, including a failed {@code helper.assertX(...)}, fails the test at once.
     */
    public TestFlow run(final String what, final Runnable action)
    {
        sequence.thenWaitUntil(() -> {
            stopIfFinished();
            try
            {
                action.run();
            }
            catch (final GameTestException e)
            {
                failNow(e);
            }
            catch (final Exception e)
            {
                failNow(what, e);
            }
        });
        return this;
    }

    /**
     * Waits until the condition holds. Limited only by the test's {@code maxTicks}.
     */
    public TestFlow awaitUntil(final String what, final BooleanSupplier condition)
    {
        return awaitUntil(what, -1, condition, () -> "");
    }

    /**
     * Waits until the condition holds. Limited only by the test's {@code maxTicks}.
     *
     * @param found describes the current state, shown in the failure message.
     */
    public TestFlow awaitUntil(final String what, final BooleanSupplier condition, final Supplier<String> found)
    {
        return awaitUntil(what, -1, condition, found);
    }

    /**
     * Waits until the condition holds, at most {@code maxTicks} ticks (counted from the tick this step starts), then fails the test. When
     * the condition holds, the number of ticks it took is logged ("Test ...: 'what' after N ticks"), to help choose budgets.
     *
     * @param maxTicks tick limit for this step, or -1 for no limit besides the test's {@code maxTicks}.
     * @param found    describes the current state, shown in the failure message.
     */
    public TestFlow awaitUntil(final String what, final int maxTicks, final BooleanSupplier condition, final Supplier<String> found)
    {
        final long[] start = {Long.MIN_VALUE};
        sequence.thenWaitUntil(() -> {
            stopIfFinished();
            if (start[0] == Long.MIN_VALUE)
            {
                start[0] = info.getTick();
            }
            if (evaluate(what, condition))
            {
                LOGGER.info("Test {}: '{}' after {} ticks (test tick {})", info.id(), what, info.getTick() - start[0], info.getTick());
                return;
            }

            final long waited = info.getTick() - start[0];
            final String message = describe(what, waited, found);
            if (maxTicks >= 0 && waited >= maxTicks)
            {
                failNow(helper.assertionException(Component.literal("Timed out after " + waited + " ticks waiting for: " + message)));
            }
            if (info.getTick() > info.getTimeoutTicks())
            {
                failNow(helper.assertionException(Component.literal(
                  "Test timed out (maxTicks " + info.getTimeoutTicks() + ") after " + waited + " ticks waiting for: " + message)));
            }
            // Retried next tick. If the test's maxTicks runs out first, this message becomes the failure.
            throw helper.assertionException(Component.literal("Still waiting (" + waited + " ticks) for: " + message));
        });
        return this;
    }

    /**
     * Checks a condition once; fails the test if it doesn't hold.
     *
     * @param found describes the current state, shown in the failure message.
     */
    public TestFlow check(final String what, final BooleanSupplier condition, final Supplier<String> found)
    {
        sequence.thenWaitUntil(() -> {
            stopIfFinished();
            if (!evaluate(what, condition))
            {
                failNow(helper.assertionException(Component.literal("Check failed: " + describe(what, -1, found))));
            }
        });
        return this;
    }

    /**
     * Lets the given number of ticks pass. Use only to let something settle that has no observable outcome; to wait for an outcome use
     * {@link #awaitUntil}.
     */
    public TestFlow idle(final int ticks)
    {
        sequence.thenIdle(ticks);
        return this;
    }

    /**
     * Ends the flow: the test passes when every step before has finished.
     */
    public void succeed()
    {
        sequence.thenSucceed();
    }

    private static String describe(final String what, final long waited, final Supplier<String> found)
    {
        String state;
        try
        {
            state = found.get();
        }
        catch (final Exception e)
        {
            state = "<could not describe state: " + e + ">";
        }
        return state == null || state.isEmpty() ? what : what + ". Found: " + state;
    }

    /**
     * Evaluates a condition. A failed {@code helper.assertX(...)} inside counts as "not yet"; any other exception fails the test at once.
     */
    private boolean evaluate(final String what, final BooleanSupplier condition)
    {
        try
        {
            return condition.getAsBoolean();
        }
        catch (final GameTestException e)
        {
            return false;
        }
        catch (final Exception e)
        {
            failNow(what, e);
            return false;
        }
    }

    /**
     * Vanilla ticks a test's sequences once more in the tick it notices the test failed (after the test's {@code maxTicks} even with
     * "fail if not complete"). Steps must not run again then, and must not replace the test's failure: the sequence records whatever
     * assertion a step throws as the test's error, so the existing error is thrown again.
     */
    private void stopIfFinished()
    {
        if (info.isDone() || info.hasFailed())
        {
            final GameTestException error = info.getError();
            if (error instanceof final GameTestAssertException assertion)
            {
                throw assertion;
            }
            throw helper.assertionException(error != null ? error.getDescription() : Component.literal("Test already finished"));
        }
    }

    private void failNow(final String what, final Exception e)
    {
        LOGGER.error("Test {}: step '{}' threw", info.id(), what, e);
        failNow(helper.assertionException(Component.literal("Step '" + what + "' threw " + e)));
    }

    /**
     * Fails the test and stops the sequence: the vanilla sequence stops at the first {@link GameTestAssertException} of a tick, and the test
     * ends at its next tick because it has an error.
     */
    private void failNow(final GameTestException error)
    {
        info.fail(error);
        throw error instanceof final GameTestAssertException assertion ? assertion : helper.assertionException(error.getDescription());
    }
}
