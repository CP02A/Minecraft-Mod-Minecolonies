package com.minecolonies.api.colony.interactionhandling;

import com.minecolonies.api.colony.interactionhandling.registry.InteractionResponseHandlerEntry;
import com.minecolonies.api.util.constant.Constants;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.neoforge.registries.DeferredHolder;

/**
 * List of mod interaction handlers.
 */
public final class ModInteractionResponseHandlers
{
    /**
     * List of IDs.
     */
    public static final Identifier STANDARD            = new Identifier(Constants.MOD_ID, "standard");
    public static final Identifier SIMPLE_NOTIFICATION = new Identifier(Constants.MOD_ID, "simplenotification");
    public static final Identifier POS                 = new Identifier(Constants.MOD_ID, "pos");
    public static final Identifier REQUEST             = new Identifier(Constants.MOD_ID, "request");
    public static final Identifier RECRUITMENT         = new Identifier(Constants.MOD_ID, "recruitment");
    public static final Identifier QUEST               = new Identifier(Constants.MOD_ID, "quest");
    public static final Identifier QUEST_ACTION        = new Identifier(Constants.MOD_ID, "questaction");

    /**
     * List of entries.
     */
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> standard;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> simpleNotification;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> pos;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> request;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> recruitment;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> quest;
    public static DeferredHolder<InteractionResponseHandlerEntry, InteractionResponseHandlerEntry> questAction;

    private ModInteractionResponseHandlers()
    {
        throw new IllegalStateException("Tried to initialize: ModJobs but this is a Utility class.");
    }
}
