package com.ldtteam.structurize.event;

import com.ldtteam.blockui.BOScreen;
import com.ldtteam.structurize.api.IScrollableItem;
import com.ldtteam.structurize.api.ISpecialBlockPickItem;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.client.BlueprintHandler;
import com.ldtteam.structurize.client.ModKeyMappings;
import com.ldtteam.structurize.client.gui.WindowExtendedBuildTool;
import com.ldtteam.structurize.items.ItemScanTool;
import com.ldtteam.structurize.network.messages.ItemMiddleMouseMessage;
import com.ldtteam.structurize.network.messages.ScanToolTeleportMessage;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.ldtteam.structurize.storage.rendering.types.BoxPreviewData;
import net.minecraft.client.Minecraft;
import com.ldtteam.structurize.fabric.mixin.client.KeyMappingAccessor;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.ldtteam.shim.bus.api.SubscribeEvent;
import com.ldtteam.shim.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import com.ldtteam.shim.neoforge.client.event.ClientTickEvent;
import com.ldtteam.shim.neoforge.client.event.InputEvent;
import com.ldtteam.shim.neoforge.client.event.RenderGuiLayerEvent;
import com.ldtteam.shim.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;

public class ClientEventSubscriber
{
    @SubscribeEvent
    public static void renderWorldLastEvent(final RenderGuiLayerEvent.Pre event)
    {
        if ((event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) && Minecraft.getInstance().gui.screen() instanceof BOScreen &&
              ((BOScreen) Minecraft.getInstance().gui.screen()).getWindow() instanceof WindowExtendedBuildTool)
        {
             event.setCanceled(true);
        }
    }


    /**
     * Used to catch the clientTickEvent.
     * Call renderer cache cleaning every 5 secs (100 ticks).
     *
     * @param event the catched event.
     */
    @SubscribeEvent
    public static void onClientTickEvent(final ClientTickEvent.Post event)
    {
        final Minecraft mc = Minecraft.getInstance();
        Profiler.get().push("structurize");

        if (mc.level != null && mc.level.getGameTime() % (Constants.TICKS_SECOND * BlueprintHandler.CACHE_EXPIRE_CHECK_SECONDS) == 0)
        {
            Profiler.get().push("blueprint_manager_tick");
            BlueprintHandler.getInstance().cleanCache();
            Profiler.get().pop();
        }

        if (ModKeyMappings.TELEPORT.get().consumeClick() && mc.level != null && mc.player != null &&
            mc.player.getMainHandItem().getItem() instanceof ItemScanTool tool)
        {
            if (tool.onTeleport(mc.player, mc.player.getMainHandItem()))
            {
                new ScanToolTeleportMessage().sendToServer();
            }
        }

        Profiler.get().pop();
    }

    @SubscribeEvent
    public static void onPreClientTickEvent(@NotNull final ClientTickEvent.Pre event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || mc.level == null) return;

        if (mc.options.keyPickItem.consumeClick())
        {
            BlockPos pos = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK ? ((BlockHitResult)mc.hitResult).getBlockPos() : null;
            if (pos != null && mc.level.getBlockState(pos).isAir())
            {
                pos = null;
            }

            final ItemStack current = mc.player.getInventory().getSelectedItem();
            if (current.getItem() instanceof ISpecialBlockPickItem clickableItem)
            {
                final boolean ctrlKey = mc.hasControlDown();
                final InteractionResult result = clickableItem.onBlockPick(mc.player, current, pos, ctrlKey);
                if (result instanceof InteractionResult.Pass)
                {
                    incrementClickCount(mc);
                }
                else if (!(result instanceof InteractionResult.Fail))
                {
                    new ItemMiddleMouseMessage(pos, ctrlKey).sendToServer();
                }
            }
            else
            {
                incrementClickCount(mc);
            }
        }

        for (Iterator<Map.Entry<String, BoxPreviewData>> iterator = RenderingCache.boxRenderingCache.entrySet().iterator(); iterator.hasNext(); )
        {
            final var entry = iterator.next();
            if (entry.getValue().isExpired())
            {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseWheel(final InputEvent.MouseScrollingEvent event)
    {
        final Minecraft mc = Minecraft.getInstance();
        if (event.isCanceled() || mc.player == null || mc.gui.screen() != null || mc.level == null) return;
        if (!mc.player.isShiftKeyDown()) return;

        final ItemStack current = mc.player.getInventory().getSelectedItem();
        if (current.getItem() instanceof IScrollableItem scrollableItem)
        {
            final boolean ctrlKey = mc.hasControlDown();
            final InteractionResult result = scrollableItem.onMouseScroll(mc.player, current, event.getScrollDeltaX(), event.getScrollDeltaY(), ctrlKey);
            if (result instanceof InteractionResult.Fail)
            {
                event.setCanceled(true);
            }
            else if (!(result instanceof InteractionResult.Pass))
            {
                event.setCanceled(true);
                new ItemMiddleMouseMessage(event.getScrollDeltaX(), event.getScrollDeltaY(), ctrlKey).sendToServer();
            }
        }
    }

    @SubscribeEvent
    public static void onDisconnect(final LoggingOut event)
    {
        // clear local caches
        WindowExtendedBuildTool.clearStaticData();
    }

    private static void incrementClickCount(final Minecraft mc)
    {
        final KeyMappingAccessor accessor = (KeyMappingAccessor) mc.options.keyPickItem;
        accessor.structurize$setClickCount(accessor.structurize$getClickCount() + 1);
    }
}
