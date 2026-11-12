package com.minecolonies.gametest.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Hands the colony that {@link UiColonySetup} prepared on the integrated server to the UI client test, and tells the setup test when the
 * client is done with it. Client and integrated server run in the same JVM; the fields are volatile and only written by one side each.
 */
public final class UiBridge
{
    /**
     * Research the setup starts (in progress), shown in the research tree (UI-05).
     */
    public static final Identifier STARTED_RESEARCH = Identifier.parse("minecolonies:civilian/stamina");

    /**
     * Branch of {@link #STARTED_RESEARCH}.
     */
    public static final Identifier RESEARCH_BRANCH = Identifier.parse("minecolonies:civilian");

    public record Colony(int colonyId, BlockPos townHall, BlockPos builder, BlockPos warehouse, BlockPos university, BlockPos postBox, int workerId,
                         int secondId)
    {
    }

    /**
     * Set by the setup test when the colony is ready.
     */
    public static volatile Colony colony;

    /**
     * Set by the setup test if setting up the colony threw.
     */
    public static volatile String setupError;

    /**
     * Set by the client test when it has taken all screenshots; the setup test then passes and its colony is deleted.
     */
    public static volatile boolean clientDone;

    private UiBridge()
    {
    }

    public static void reset()
    {
        colony = null;
        setupError = null;
        clientDone = false;
    }
}
