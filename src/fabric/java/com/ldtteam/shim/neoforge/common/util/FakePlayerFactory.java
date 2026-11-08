// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.util.FakePlayerFactory), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.util;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;

public class FakePlayerFactory
{
    private static final GameProfile MINECRAFT = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");
    private static final Map<FakePlayerKey, FakePlayer> fakePlayers = new MapMaker().weakValues().makeMap();

    public static FakePlayer getMinecraft(final ServerLevel level)
    {
        return get(level, MINECRAFT);
    }

    public static FakePlayer get(final ServerLevel level, final GameProfile username)
    {
        return fakePlayers.computeIfAbsent(new FakePlayerKey(level, username), key -> new FakePlayer(key.level(), key.username()));
    }

    public static void unloadLevel(final ServerLevel level)
    {
        fakePlayers.entrySet().removeIf(entry -> entry.getValue().level() == level);
    }

    private record FakePlayerKey(ServerLevel level, GameProfile username) {}
}
