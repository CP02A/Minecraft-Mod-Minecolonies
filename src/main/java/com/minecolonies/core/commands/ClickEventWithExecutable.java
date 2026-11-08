package com.minecolonies.core.commands;

import net.minecraft.network.chat.ClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Small utility class to run an executable on a chat click event.
 */
// FABRIC-PORT: ClickEvent is an interface (with records per action) in 26.x; the vanilla client dispatches by type, so the
// actions only run when action() is queried.
public class ClickEventWithExecutable implements ClickEvent
{
    /**
     * The actions to run
     */
    private Runnable[] actions;

    /**
     * Default constructor.
     *
     * @param actions the actions this event should execute.
     */
    public ClickEventWithExecutable(@NotNull final Runnable... actions)
    {
        this.actions = actions;
    }

    /**
     * Triggered when the chat component is clicked.
     */
    @Override
    @NotNull
    public Action action()
    {
        if (actions != null)
        {
            for (Runnable r : actions)
            {
                r.run();
            }
            actions = null;
        }
        return Action.RUN_COMMAND;
    }
}
