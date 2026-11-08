package com.ldtteam.shim.fml.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import org.jetbrains.annotations.Nullable;

/**
 * A config spec (NeoForge/FML shim, mirrors FML 4 IConfigSpec).
 */
public interface IConfigSpec
{
    /**
     * @return true if the spec has no values (the file will not be created).
     */
    boolean isEmpty();

    /**
     * Validate the spec for the given config (e.g. restart types).
     */
    void validateSpec(ModConfig config);

    /**
     * @return true if the config matches the spec.
     */
    boolean isCorrect(UnmodifiableCommentedConfig config);

    /**
     * Correct the config to match the spec.
     */
    void correct(CommentedConfig config);

    /**
     * Set the loaded config (or null when unloading). Called after correction.
     */
    void acceptConfig(@Nullable ILoadedConfig config);

    /**
     * A loaded config.
     */
    interface ILoadedConfig
    {
        CommentedConfig config();

        /**
         * Save the config to disk (if file based) and fire the reloading event.
         */
        void save();
    }
}
