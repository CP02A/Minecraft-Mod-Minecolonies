// Adapted from NeoForge 21.1 (net.neoforged.neoforge.client.gui.VanillaGuiLayers), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * Identifiers of the vanilla GUI layers, mapped onto Fabric's {@link VanillaHudElements} ids (which are the ids passed to
 * {@link com.ldtteam.shim.neoforge.client.event.RenderGuiLayerEvent#getName()}).
 */
public final class VanillaGuiLayers
{
    public static final Identifier CAMERA_OVERLAYS = VanillaHudElements.MISC_OVERLAYS;
    public static final Identifier CROSSHAIR = VanillaHudElements.CROSSHAIR;
    public static final Identifier HOTBAR = VanillaHudElements.HOTBAR;
    public static final Identifier JUMP_METER = VanillaHudElements.INFO_BAR;
    public static final Identifier EXPERIENCE_BAR = VanillaHudElements.INFO_BAR;
    public static final Identifier PLAYER_HEALTH = VanillaHudElements.HEALTH_BAR;
    public static final Identifier ARMOR_LEVEL = VanillaHudElements.ARMOR_BAR;
    public static final Identifier FOOD_LEVEL = VanillaHudElements.FOOD_BAR;
    public static final Identifier VEHICLE_HEALTH = VanillaHudElements.MOUNT_HEALTH;
    public static final Identifier AIR_LEVEL = VanillaHudElements.AIR_BAR;
    public static final Identifier SELECTED_ITEM_NAME = VanillaHudElements.HELD_ITEM_TOOLTIP;
    public static final Identifier SPECTATOR_TOOLTIP = VanillaHudElements.SPECTATOR_TOOLTIP;
    public static final Identifier EXPERIENCE_LEVEL = VanillaHudElements.EXPERIENCE_LEVEL;
    public static final Identifier EFFECTS = VanillaHudElements.MOB_EFFECTS;
    public static final Identifier BOSS_OVERLAY = VanillaHudElements.BOSS_BAR;
    public static final Identifier SLEEP_OVERLAY = VanillaHudElements.SLEEP;
    public static final Identifier DEMO_OVERLAY = VanillaHudElements.DEMO_TIMER;
    public static final Identifier DEBUG_OVERLAY = Identifier.withDefaultNamespace("debug_overlay");
    public static final Identifier SCOREBOARD_SIDEBAR = VanillaHudElements.SCOREBOARD;
    public static final Identifier OVERLAY_MESSAGE = VanillaHudElements.OVERLAY_MESSAGE;
    public static final Identifier TITLE = VanillaHudElements.TITLE_AND_SUBTITLE;
    public static final Identifier CHAT = VanillaHudElements.CHAT;
    public static final Identifier TAB_LIST = VanillaHudElements.PLAYER_LIST;
    public static final Identifier SUBTITLE_OVERLAY = VanillaHudElements.SUBTITLES;
    public static final Identifier SAVING_INDICATOR = Identifier.withDefaultNamespace("saving_indicator");

    private VanillaGuiLayers() {}
}
