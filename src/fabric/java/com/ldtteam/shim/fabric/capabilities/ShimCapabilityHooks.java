package com.ldtteam.shim.fabric.capabilities;

import com.ldtteam.shim.fabric.ShimModBuses;
import com.ldtteam.shim.mixin.items.AbstractHorseAccessor;
import com.ldtteam.shim.neoforge.capabilities.Capabilities;
import com.ldtteam.shim.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.ldtteam.shim.neoforge.items.ComponentItemHandler;
import com.ldtteam.shim.neoforge.items.wrapper.CombinedInvWrapper;
import com.ldtteam.shim.neoforge.items.wrapper.EntityArmorInvWrapper;
import com.ldtteam.shim.neoforge.items.wrapper.EntityHandsInvWrapper;
import com.ldtteam.shim.neoforge.items.wrapper.InvWrapper;
import com.ldtteam.shim.neoforge.items.wrapper.PlayerInvWrapper;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.slf4j.Logger;

/**
 * Fires {@link RegisterCapabilitiesEvent} (lazily, once) and registers the vanilla/Fabric fallback providers that NeoForge
 * registers in its own CapabilityHooks.
 */
public final class ShimCapabilityHooks
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean initialized = false;
    private static boolean initializing = false;
    /**
     * Set when the registration event threw: registrations are then partial, and every later query should fail loudly instead
     * of silently finding no capability (a retry would register providers twice).
     */
    private static volatile RuntimeException failure = null;

    private ShimCapabilityHooks() {}

    /**
     * Ensures that the capability registration event was fired. Called on server start, client start and lazily on first query.
     */
    public static void ensureInitialized()
    {
        if (initialized)
        {
            if (failure != null)
            {
                throw new IllegalStateException("Capability registration failed earlier, capabilities are incomplete", failure);
            }
            return;
        }
        synchronized (ShimCapabilityHooks.class)
        {
            if (initialized || initializing)
            {
                return;
            }
            initializing = true;
            try
            {
                registerFallbacks();
                ShimModBuses.post(new RegisterCapabilitiesEvent());
            }
            catch (final RuntimeException e)
            {
                LOGGER.error("Failed to fire RegisterCapabilitiesEvent", e);
                failure = e;
                throw e;
            }
            finally
            {
                initializing = false;
                initialized = true;
            }
        }
    }

    private static void registerFallbacks()
    {
        Capabilities.ItemHandler.BLOCK.registerFallback(FabricTransferBridge::blockFallback);

        Capabilities.ItemHandler.ENTITY.registerFallback((entity, ctx) -> {
            if (entity instanceof Player player)
            {
                return new PlayerInvWrapper(player.getInventory());
            }
            if (entity instanceof ContainerEntity container)
            {
                return new InvWrapper(container);
            }
            if (entity instanceof AbstractHorse horse)
            {
                return new InvWrapper(((AbstractHorseAccessor) horse).shim$getInventory());
            }
            if (entity instanceof LivingEntity living)
            {
                return new CombinedInvWrapper(new EntityHandsInvWrapper(living), new EntityArmorInvWrapper(living));
            }
            return null;
        });

        Capabilities.ItemHandler.ENTITY_AUTOMATION.registerFallback((entity, ctx) -> {
            if (entity instanceof ContainerEntity container)
            {
                return new InvWrapper(container);
            }
            return null;
        });

        Capabilities.ItemHandler.ITEM.registerFallback((stack, ctx) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)
            {
                return new ComponentItemHandler(stack, DataComponents.CONTAINER, 27);
            }
            return null;
        });
    }
}
