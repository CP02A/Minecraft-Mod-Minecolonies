// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.util.FakePlayer), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

/**
 * NeoForge-style fake player, built on Fabric's {@link net.fabricmc.fabric.api.entity.FakePlayer}.
 * Note: fake players created by other Fabric mods are instances of Fabric's class only; to detect any fake player use
 * {@code instanceof net.fabricmc.fabric.api.entity.FakePlayer} or {@link #isFake(Object)}.
 */
public class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer
{
    public FakePlayer(final ServerLevel level, final GameProfile name)
    {
        super(level, name);
    }

    /**
     * @return true if the given object is any Fabric fake player (including NeoForge-shim fake players).
     */
    public static boolean isFake(final Object entity)
    {
        return entity instanceof net.fabricmc.fabric.api.entity.FakePlayer;
    }

    @Override
    public boolean canHarmPlayer(final Player player)
    {
        return false;
    }

    @Override
    public void die(final DamageSource source) {}

    public boolean isFakePlayer()
    {
        return true;
    }
}
