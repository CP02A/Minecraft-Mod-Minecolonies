// Adapted from NeoForge 21.1 (net.neoforged.neoforge.common.extensions.IMenuTypeExtension), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
package com.ldtteam.shim.neoforge.common.extensions;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * NeoForge-style menu types carrying a raw extra-data buffer, implemented with Fabric's {@link ExtendedMenuType} (the buffer is sent as a byte array).
 * Menus of such types must be opened with {@link #openMenu(ServerPlayer, MenuProvider, Consumer)} (replacement for NeoForge's
 * {@code ServerPlayer#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)}).
 */
public interface IMenuTypeExtension<T>
{
    /**
     * Factory for menus that receive extra data from the server.
     */
    @FunctionalInterface
    interface IContainerFactory<T extends AbstractContainerMenu>
    {
        T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data);
    }

    static <T extends AbstractContainerMenu> MenuType<T> create(final IContainerFactory<T> factory)
    {
        return new ExtendedMenuType<T, byte[]>(
          (windowId, inv, bytes) -> factory.create(windowId, inv, new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), inv.player.registryAccess())),
          ByteBufCodecs.BYTE_ARRAY);
    }

    /**
     * Opens a menu with extra data (NeoForge {@code ServerPlayer#openMenu(provider, extraDataWriter)} replacement).
     */
    static OptionalInt openMenu(final ServerPlayer player, final MenuProvider provider, @Nullable final Consumer<RegistryFriendlyByteBuf> extraDataWriter)
    {
        if (extraDataWriter == null)
        {
            return player.openMenu(provider);
        }
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        final byte[] data;
        try
        {
            extraDataWriter.accept(buf);
            data = new byte[buf.readableBytes()];
            buf.readBytes(data);
        }
        finally
        {
            buf.release();
        }
        return player.openMenu(new ExtendedMenuProvider<byte[]>()
        {
            @Override
            public byte[] getScreenOpeningData(final ServerPlayer serverPlayer)
            {
                return data;
            }

            @Override
            public Component getDisplayName()
            {
                return provider.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player p)
            {
                return provider.createMenu(containerId, inventory, p);
            }

            @Override
            public boolean shouldCloseCurrentScreen()
            {
                return provider.shouldCloseCurrentScreen();
            }
        });
    }

    /**
     * Opens a menu whose extra data only contains a block position.
     */
    static OptionalInt openMenu(final ServerPlayer player, final MenuProvider provider, final net.minecraft.core.BlockPos pos)
    {
        return openMenu(player, provider, buf -> buf.writeBlockPos(pos));
    }
}
