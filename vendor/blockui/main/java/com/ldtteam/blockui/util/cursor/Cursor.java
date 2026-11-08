package com.ldtteam.blockui.util.cursor;

import com.ldtteam.blockui.mod.BlockUI;
import com.ldtteam.blockui.util.SafeError;
import com.ldtteam.blockui.util.texture.CursorTexture;
import com.ldtteam.blockui.util.texture.IsOurTexture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import com.ldtteam.blockui.fabric.mixin.CursorTypeAccessor;
import org.lwjgl.sdl.SDLMouse;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Interface to wrap various cursors
 */
public class Cursor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Cursor.class);

    /** Probably arrow, but OS dependend */
    public static final CursorType DEFAULT = CursorType.DEFAULT;
    public static final CursorType ARROW = CursorTypes.ARROW;
    public static final CursorType TEXT_CURSOR = CursorTypes.IBEAM;
    public static final CursorType CROSSHAIR = CursorTypes.CROSSHAIR;
    public static final CursorType HAND = CursorTypes.POINTING_HAND;
    public static final CursorType HORIZONTAL_RESIZE = CursorTypes.RESIZE_EW;
    public static final CursorType VERTICAL_RESIZE = CursorTypes.RESIZE_NS;
    public static final CursorType RESIZE_NWSE = CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "resize_nwse", Cursor.DEFAULT);
    public static final CursorType RESIZE_NESW = CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE, "resize_nesw", Cursor.DEFAULT);
    public static final CursorType RESIZE = CursorTypes.RESIZE_ALL;
    public static final CursorType NOT_ALLOWED = CursorTypes.NOT_ALLOWED;

    private static final Map<String, CursorType> CURSOR_MAP = Map.ofEntries(
        Map.entry(DEFAULT.toString(), DEFAULT),
        Map.entry(ARROW.toString(), ARROW),
        Map.entry(TEXT_CURSOR.toString(), TEXT_CURSOR),
        Map.entry(CROSSHAIR.toString(), CROSSHAIR),
        Map.entry(HAND.toString(), HAND),
        Map.entry(HORIZONTAL_RESIZE.toString(), HORIZONTAL_RESIZE),
        Map.entry(VERTICAL_RESIZE.toString(), VERTICAL_RESIZE),
        Map.entry(RESIZE_NWSE.toString(), RESIZE_NWSE),
        Map.entry(RESIZE_NESW.toString(), RESIZE_NESW),
        Map.entry(RESIZE.toString(), RESIZE),
        Map.entry(NOT_ALLOWED.toString(), NOT_ALLOWED)
    );

    public static CursorType of(final Identifier resLoc)
    {
        if ((BlockUI.MOD_ID + "_std").equalsIgnoreCase(resLoc.getNamespace()))
        {
            return SafeError.requireNonNull(CURSOR_MAP.get(resLoc.getPath()), Cursor.DEFAULT, "Invalid built-in cursor: " + resLoc.toString());
        }

        final TextureManager texManager = Minecraft.getInstance().getTextureManager();
        final AbstractTexture texture = texManager.getTexture(resLoc);
        if (!(texture instanceof CursorTexture))
        {
            if (IsOurTexture.isOur(texture))
            {
                LOGGER.warn("Trying to use special BlockUI texture as cursor? Things may not work well: " + resLoc.toString());
            }

            texManager.registerAndLoad(resLoc, new CursorTexture(resLoc));
        }

        return TEXTURED_BY_RESLOC.computeIfAbsent(resLoc, key -> {
            final CursorType type = CursorTypeAccessor.blockui$create(BlockUI.MOD_ID + "_tex_cursor:" + key, 0L);
            TEXTURED.put(type, key);
            return type;
        });
    }

    /** Textured cursor types (created through the private vanilla constructor) and their texture. */
    private static final Map<Identifier, CursorType> TEXTURED_BY_RESLOC = new ConcurrentHashMap<>();
    private static final Map<CursorType, Identifier> TEXTURED = new ConcurrentHashMap<>();

    /**
     * Fabric: called from {@code CursorTypeMixin#select} - selects the textured cursor if the type is one of ours.
     *
     * @return true if handled
     */
    public static boolean selectTextured(final CursorType type)
    {
        final Identifier resLoc = TEXTURED.get(type);
        if (resLoc == null)
        {
            return false;
        }

        final AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resLoc);
        if (!(texture instanceof final CursorTexture cursorTexture))
        {
            throw new IllegalArgumentException("Did you forget to load CursorTexture (or create CursorType) for: " + resLoc);
        }

        SDLMouse.SDL_SetCursor(cursorTexture.getGlfwCursorAddress());
        return true;
    }
}
