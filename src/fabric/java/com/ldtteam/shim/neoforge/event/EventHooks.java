// Parts adapted from NeoForge 26.3.0.101 (event/EventHooks.java), licensed under LGPL-2.1. Modified for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.event;

import com.ldtteam.shim.neoforge.common.NeoForge;
import com.ldtteam.shim.neoforge.event.entity.ProjectileImpactEvent;
import com.ldtteam.shim.neoforge.event.entity.living.LivingConversionEvent;
import com.ldtteam.shim.neoforge.event.entity.living.MobDespawnEvent;
import com.ldtteam.shim.neoforge.event.entity.living.MobSpawnEvent;
import com.ldtteam.shim.neoforge.event.entity.player.ArrowLooseEvent;
import com.ldtteam.shim.neoforge.event.entity.player.ArrowNockEvent;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Subset of NeoForge's EventHooks used by the ported mods (and by the shim mixins).
 */
public final class EventHooks
{
    private EventHooks()
    {
    }

    /**
     * @return the action to return from Item#use, or null to continue (no listener decided).
     */
    public static @Nullable InteractionResult onArrowNock(final ItemStack item, final Level level, final Player player, final InteractionHand hand, final boolean hasAmmo)
    {
        final ArrowNockEvent event = new ArrowNockEvent(player, item, hand, level, hasAmmo);
        if (NeoForge.EVENT_BUS.post(event).isCanceled())
        {
            return InteractionResult.FAIL;
        }
        return event.getAction();
    }

    /**
     * @return the new charge, or -1 if the shot was canceled.
     */
    public static int onArrowLoose(final ItemStack stack, final Level level, final Player player, final int charge, final boolean hasAmmo)
    {
        final ArrowLooseEvent event = new ArrowLooseEvent(player, stack, level, charge, hasAmmo);
        if (NeoForge.EVENT_BUS.post(event).isCanceled())
        {
            return -1;
        }
        return event.getCharge();
    }

    public static int onArrowLoose(final ItemStack stack, final Level level, final Player player, final int charge)
    {
        return onArrowLoose(stack, level, player, charge, true);
    }

    /**
     * @return true if the impact was canceled.
     */
    public static boolean onProjectileImpact(final Projectile projectile, final HitResult ray)
    {
        return NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(projectile, ray)).isCanceled();
    }

    public static boolean canLivingConvert(final LivingEntity entity, final EntityType<? extends LivingEntity> outcome, final @Nullable Consumer<Integer> timer)
    {
        return !NeoForge.EVENT_BUS.post(new LivingConversionEvent.Pre(entity, outcome, timer == null ? t -> {} : timer)).isCanceled();
    }

    public static void onLivingConvert(final LivingEntity entity, final LivingEntity outcome)
    {
        NeoForge.EVENT_BUS.post(new LivingConversionEvent.Post(entity, outcome));
    }

    /**
     * Fires MobSpawnEvent.PositionCheck.
     *
     * @return the result of the event (DEFAULT means run the vanilla checks).
     */
    public static MobSpawnEvent.PositionCheck.Result checkSpawnPositionResult(final Mob mob, final ServerLevelAccessor level, final EntitySpawnReason spawnType, final @Nullable BaseSpawner spawner)
    {
        final MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(mob, level, spawnType, spawner);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    public static boolean checkSpawnPosition(final Mob mob, final ServerLevelAccessor level, final EntitySpawnReason spawnType)
    {
        final MobSpawnEvent.PositionCheck.Result result = checkSpawnPositionResult(mob, level, spawnType, null);
        if (result == MobSpawnEvent.PositionCheck.Result.DEFAULT)
        {
            return mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level);
        }
        return result == MobSpawnEvent.PositionCheck.Result.SUCCEED;
    }

    /**
     * @return true if the event decided (vanilla despawn logic must be skipped).
     */
    public static boolean checkMobDespawn(final Mob mob)
    {
        final MobDespawnEvent event = new MobDespawnEvent(mob, (ServerLevel) mob.level());
        NeoForge.EVENT_BUS.post(event);

        switch (event.getResult())
        {
            case ALLOW -> mob.discard();
            case DEFAULT -> {}
            case DENY -> mob.setNoActionTime(0);
        }
        return event.getResult() != MobDespawnEvent.Result.DEFAULT;
    }

    /**
     * Runs the display items generator of a tab into the output, plus the modifications other mods make to the tab. On Fabric those
     * come from Fabric API's {@link CreativeModeTabEvents} (NeoForge's BuildCreativeModeTabContentsEvent has no listener in the
     * ported mods); they are applied the way Fabric API applies them when vanilla builds a tab (CreativeModeTab#buildContents),
     * without touching the tab's own cached contents.
     */
    public static void onCreativeModeTabBuildContents(final CreativeModeTab tab, final @Nullable ResourceKey<CreativeModeTab> tabKey,
        final CreativeModeTab.DisplayItemsGenerator originalGenerator, final CreativeModeTab.ItemDisplayParameters params, final CreativeModeTab.Output output)
    {
        final Set<ItemStack> display = ItemStackLinkedSet.createTypeAndComponentsSet();
        final Set<ItemStack> search = ItemStackLinkedSet.createTypeAndComponentsSet();
        originalGenerator.accept(params, (stack, visibility) -> {
            if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)
            {
                display.add(stack);
            }
            if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)
            {
                search.add(stack);
            }
        });

        final ResourceKey<CreativeModeTab> key = tabKey != null ? tabKey : BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElse(null);
        final List<ItemStack> displayStacks = new LinkedList<>(display);
        final List<ItemStack> searchStacks = new LinkedList<>(search);
        // Same rules as Fabric API's CreativeModeTabMixin: right-aligned tabs (except the operator tab) are not modified,
        // the operator tab only for players with permissions.
        if (key != null && (!tab.isAlignedRight() || key == CreativeModeTabs.OP_BLOCKS) && (key != CreativeModeTabs.OP_BLOCKS || params.hasPermissions()))
        {
            final FabricCreativeModeTabOutput modifications = new FabricCreativeModeTabOutput(params, displayStacks, searchStacks);
            CreativeModeTabEvents.modifyOutputEvent(key).invoker().modifyOutput(modifications);
            CreativeModeTabEvents.MODIFY_OUTPUT_ALL.invoker().modifyOutput(tab, modifications);
        }

        final Set<ItemStack> searchOnly = ItemStackLinkedSet.createTypeAndComponentsSet();
        searchOnly.addAll(searchStacks);
        for (final ItemStack stack : displayStacks)
        {
            output.accept(stack, searchOnly.remove(stack) ? CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS : CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        for (final ItemStack stack : searchOnly)
        {
            output.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
        }
    }
}
