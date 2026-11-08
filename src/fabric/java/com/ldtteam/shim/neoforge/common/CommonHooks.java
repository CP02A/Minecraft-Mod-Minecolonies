// Parts adapted from NeoForge 21.1.80 (common/CommonHooks.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import com.ldtteam.shim.fabric.events.DamageContainerHolder;
import com.ldtteam.shim.neoforge.common.damagesource.DamageContainer;
import com.ldtteam.shim.neoforge.event.entity.living.LivingShieldBlockEvent;
import com.ldtteam.shim.neoforge.event.level.BlockEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Subset of NeoForge's CommonHooks used by the ported mods.
 */
public final class CommonHooks
{
    private CommonHooks()
    {
    }

    /**
     * Fires the LivingShieldBlockEvent; the caller applies the result.
     */
    public static LivingShieldBlockEvent onDamageBlock(final LivingEntity blocker, final DamageContainer container, final boolean originalBlocked)
    {
        final LivingShieldBlockEvent event = new LivingShieldBlockEvent(blocker, container, originalBlocked);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /**
     * The NeoForge {@code LivingEntity#damageContainers} stack: one container per hurtServer call in progress (shim mixin).
     */
    public static Deque<DamageContainer> getDamageContainers(final LivingEntity entity)
    {
        return ((DamageContainerHolder) entity).shim$getDamageContainers();
    }

    /**
     * @return true if the farmland may be trampled.
     */
    public static boolean onFarmlandTrample(final Level level, final BlockPos pos, final BlockState state, final double fallDistance, final Entity entity)
    {
        final BlockEvent.FarmlandTrampleEvent event = new BlockEvent.FarmlandTrampleEvent(level, pos, state, (float) fallDistance, entity);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    static final Pattern URL_PATTERN = Pattern.compile(
      "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"§ \n]|$))",
      Pattern.CASE_INSENSITIVE);

    public static Component newChatWithLinks(final String string)
    {
        return newChatWithLinks(string, true);
    }

    public static Component newChatWithLinks(final String string, final boolean allowMissingHeader)
    {
        MutableComponent ichat = null;
        final Matcher matcher = URL_PATTERN.matcher(string);
        int lastEnd = 0;

        while (matcher.find())
        {
            final int start = matcher.start();
            final int end = matcher.end();

            final String part = string.substring(lastEnd, start);
            if (!part.isEmpty())
            {
                if (ichat == null)
                {
                    ichat = Component.literal(part);
                }
                else
                {
                    ichat.append(part);
                }
            }
            lastEnd = end;
            String url = string.substring(start, end);
            final MutableComponent link = Component.literal(url);

            URI uri;
            try
            {
                uri = new URI(url);
                if (uri.getScheme() == null)
                {
                    if (!allowMissingHeader)
                    {
                        if (ichat == null)
                        {
                            ichat = Component.literal(url);
                        }
                        else
                        {
                            ichat.append(url);
                        }
                        continue;
                    }
                    url = "http://" + url;
                    uri = new URI(url);
                }
            }
            catch (final URISyntaxException e)
            {
                if (ichat == null)
                {
                    ichat = Component.literal(url);
                }
                else
                {
                    ichat.append(url);
                }
                continue;
            }

            final ClickEvent click = new ClickEvent.OpenUrl(uri);
            link.setStyle(link.getStyle().withClickEvent(click).withUnderlined(true).withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE)));
            if (ichat == null)
            {
                ichat = Component.literal("");
            }
            ichat.append(link);
        }

        final String end = string.substring(lastEnd);
        if (ichat == null)
        {
            ichat = Component.literal(end);
        }
        else if (!end.isEmpty())
        {
            ichat.append(Component.literal(string.substring(lastEnd)));
        }
        return ichat;
    }
}
