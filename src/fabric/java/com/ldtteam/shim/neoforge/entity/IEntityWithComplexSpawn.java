// Adapted from NeoForge 21.1 (net.neoforged.neoforge.entity.IEntityWithComplexSpawn), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Entities implementing this interface get extra spawn data sent to the client. On Fabric the data is sent in a separate payload right
 * after the vanilla spawn packet (see {@code com.ldtteam.shim.fabric.ComplexSpawnSync}).
 */
public interface IEntityWithComplexSpawn
{
    void writeSpawnData(RegistryFriendlyByteBuf buffer);

    void readSpawnData(RegistryFriendlyByteBuf additionalData);
}
