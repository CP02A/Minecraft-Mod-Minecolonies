package com.ldtteam.structurize.client;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.client.gui.AbstractBlueprintManipulationWindow;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.ldtteam.shim.neoforge.client.event.RegisterKeyMappingsEvent;
import com.ldtteam.shim.neoforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class ModKeyMappings
{
    // FABRIC-PORT: lang key changed from key.structurize.categories.general to key.category.structurize.general (26.x category format)
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "general"));

    // SDL scancodes (26.3 input is SDL based)
    private static final int KEY_KP_PLUS = 87;
    private static final int KEY_KP_MINUS = 86;

    /**
     * Key conflict context: active only while a blueprint manipulation window is open.
     */
    public static final BooleanSupplier BLUEPRINT_WINDOW = () -> Minecraft.getInstance().gui.screen() instanceof BOScreen screen
        && screen.getWindow() instanceof AbstractBlueprintManipulationWindow;

    /**
     * Key conflict context: active only in game (no screen open).
     */
    public static final BooleanSupplier IN_GAME = () -> Minecraft.getInstance().gui.screen() == null;

    /**
     * Teleport using active Scan Tool
     */
    public static final Lazy<StructurizeKeyMapping> TELEPORT = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.teleport",
            IN_GAME, false, InputConstants.UNKNOWN.getValue()));

    /**
     * Move build previews
     */
    public static final Lazy<StructurizeKeyMapping> MOVE_FORWARD = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_forward",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_UP));
    public static final Lazy<StructurizeKeyMapping> MOVE_BACK = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_back",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_DOWN));
    public static final Lazy<StructurizeKeyMapping> MOVE_LEFT = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_left",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_LEFT));
    public static final Lazy<StructurizeKeyMapping> MOVE_RIGHT = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_right",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_RIGHT));
    public static final Lazy<StructurizeKeyMapping> MOVE_UP = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_up",
            BLUEPRINT_WINDOW, false, KEY_KP_PLUS));
    public static final Lazy<StructurizeKeyMapping> MOVE_DOWN = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.move_down",
            BLUEPRINT_WINDOW, false, KEY_KP_MINUS));
    public static final Lazy<StructurizeKeyMapping> ROTATE_CW = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.rotate_cw",
            BLUEPRINT_WINDOW, true, InputConstants.KEY_RIGHT));
    public static final Lazy<StructurizeKeyMapping> ROTATE_CCW = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.rotate_ccw",
            BLUEPRINT_WINDOW, true, InputConstants.KEY_LEFT));
    public static final Lazy<StructurizeKeyMapping> MIRROR = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.mirror",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_M));
    public static final Lazy<StructurizeKeyMapping> PLACE = Lazy.of(() -> new StructurizeKeyMapping("key.structurize.place",
            BLUEPRINT_WINDOW, false, InputConstants.KEY_RETURN));

    /**
     * Register key mappings
     */
    public static void register(@NotNull final RegisterKeyMappingsEvent event)
    {
        event.register(TELEPORT.get());
        event.register(MOVE_FORWARD.get());
        event.register(MOVE_BACK.get());
        event.register(MOVE_LEFT.get());
        event.register(MOVE_RIGHT.get());
        event.register(MOVE_UP.get());
        event.register(MOVE_DOWN.get());
        event.register(ROTATE_CW.get());
        event.register(ROTATE_CCW.get());
        event.register(MIRROR.get());
        event.register(PLACE.get());
    }

    /**
     * Key mapping with a conflict context and an optional shift modifier
     * (replacement for NeoForge's IKeyConflictContext/KeyModifier KeyMapping extensions).
     */
    public static class StructurizeKeyMapping extends KeyMapping
    {
        private final BooleanSupplier context;
        private final boolean requiresShift;

        public StructurizeKeyMapping(final String name, final BooleanSupplier context, final boolean requiresShift, final int keyCode)
        {
            super(name, InputConstants.Type.KEYBOARD, keyCode, CATEGORY);
            this.context = context;
            this.requiresShift = requiresShift;
        }

        /**
         * @return true if the context is active, the modifier state matches and the key matches
         */
        public boolean isActiveAndMatches(final InputConstants.Key keyCode)
        {
            return context.getAsBoolean() && Minecraft.getInstance().hasShiftDown() == requiresShift && matches(keyCode);
        }
    }

    /**
     * Private constructor to hide the implicit one.
     */
    private ModKeyMappings()
    {
        /*
         * Intentionally left empty.
         */
    }
}
