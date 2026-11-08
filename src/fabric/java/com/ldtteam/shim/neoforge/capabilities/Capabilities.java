// Adapted from NeoForge 21.1 (net.neoforged.neoforge.capabilities), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.capabilities;

import com.ldtteam.shim.neoforge.items.IItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class Capabilities
{
    public static final class ItemHandler
    {
        public static final BlockCapability<IItemHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(create("item_handler"), IItemHandler.class);
        /**
         * Capability for the inventory of an entity.
         */
        public static final EntityCapability<IItemHandler, @Nullable Void> ENTITY = EntityCapability.createVoid(create("item_handler"), IItemHandler.class);
        /**
         * Capability for an inventory of entity that should be accessible to automation.
         */
        public static final EntityCapability<IItemHandler, @Nullable Direction> ENTITY_AUTOMATION = EntityCapability.createSided(create("item_handler_automation"), IItemHandler.class);
        public static final ItemCapability<IItemHandler, @Nullable Void> ITEM = ItemCapability.createVoid(create("item_handler"), IItemHandler.class);

        private ItemHandler() {}
    }

    private static Identifier create(final String path)
    {
        return Identifier.fromNamespaceAndPath("neoforge", path);
    }

    private Capabilities() {}
}
