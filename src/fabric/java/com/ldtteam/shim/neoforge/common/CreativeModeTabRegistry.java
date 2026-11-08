// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.CreativeModeTabRegistry), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Creative tab lookup; ordering is the vanilla/Fabric order from {@link CreativeModeTabs#allTabs()}.
 */
public final class CreativeModeTabRegistry
{
    private CreativeModeTabRegistry() {}

    public static List<CreativeModeTab> getSortedCreativeModeTabs()
    {
        return CreativeModeTabs.allTabs();
    }

    public static List<CreativeModeTab> getDefaultTabs()
    {
        return CreativeModeTabs.allTabs();
    }

    @Nullable
    public static CreativeModeTab getTab(final Identifier name)
    {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getValue(name);
    }

    @Nullable
    public static Identifier getName(final CreativeModeTab tab)
    {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
    }
}
