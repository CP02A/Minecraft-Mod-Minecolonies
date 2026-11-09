package com.ldtteam.shim.fabric.capabilities;

import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Exposes a NeoForge-style {@link IItemHandler} as a Fabric Transfer API {@code SlottedStorage<ItemVariant>}.
 * <p>
 * Changes are staged per slot while a transaction is open: each slot keeps a pending copy of its contents, which reads and
 * further operations in the same transaction see, and which {@link SnapshotParticipant} rolls back when a (nested) transaction
 * aborts. The handler itself is only touched once the outermost transaction commits ({@link Slot#onFinalCommit()}).
 * This keeps simulations (e.g. {@code StorageUtil.move} probing with an aborted nested transaction) free of side effects on the
 * handler, such as colony warehouse updates, and works the same for modifiable and non-modifiable handlers.
 */
public class ItemHandlerStorage implements SlottedStorage<ItemVariant>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final IItemHandler handler;
    private final List<Slot> slots = new ArrayList<>();

    public ItemHandlerStorage(final IItemHandler handler)
    {
        this.handler = handler;
    }

    public IItemHandler getHandler()
    {
        return handler;
    }

    @Override
    public int getSlotCount()
    {
        return handler.getSlots();
    }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot(final int slot)
    {
        while (slots.size() <= slot)
        {
            slots.add(new Slot(slots.size()));
        }
        return slots.get(slot);
    }

    @Override
    public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long inserted = 0;
        final int count = getSlotCount();
        // First pass: stack onto matching slots, second pass: empty slots (same as vanilla/NeoForge helpers).
        for (int pass = 0; pass < 2 && inserted < maxAmount; pass++)
        {
            for (int i = 0; i < count && inserted < maxAmount; i++)
            {
                final SingleSlotStorage<ItemVariant> slot = getSlot(i);
                final boolean matches = !slot.isResourceBlank() && slot.getResource().equals(resource);
                if ((pass == 0) == matches)
                {
                    inserted += slot.insert(resource, maxAmount - inserted, transaction);
                }
            }
        }
        return inserted;
    }

    @Override
    public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
    {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        long extracted = 0;
        final int count = getSlotCount();
        for (int i = 0; i < count && extracted < maxAmount; i++)
        {
            extracted += getSlot(i).extract(resource, maxAmount - extracted, transaction);
        }
        return extracted;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator()
    {
        final int count = getSlotCount();
        final List<StorageView<ItemVariant>> views = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            views.add(getSlot(i));
        }
        return views.iterator();
    }

    /**
     * Snapshot of a slot's staged contents; {@code stack == null} means nothing is staged (the handler's contents apply).
     */
    private record Staged(@Nullable ItemStack stack)
    {
    }

    private class Slot extends SnapshotParticipant<Staged> implements SingleSlotStorage<ItemVariant>
    {
        private final int index;

        /**
         * Contents of the slot as seen by the open transaction, or null if nothing is staged.
         */
        @Nullable
        private ItemStack staged;

        private Slot(final int index)
        {
            this.index = index;
        }

        private boolean valid()
        {
            return index < handler.getSlots();
        }

        private ItemStack current()
        {
            return staged != null ? staged : handler.getStackInSlot(index);
        }

        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!valid() || maxAmount == 0)
            {
                return 0;
            }
            final ItemStack current = current();
            if (!current.isEmpty() && !resource.matches(current))
            {
                return 0;
            }
            final ItemStack probe = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            if (!handler.isItemValid(index, probe))
            {
                return 0;
            }
            // Room left given what is already staged in this transaction.
            final int limit = Math.min(handler.getSlotLimit(index), probe.getMaxStackSize());
            long accepted = Math.min(maxAmount, limit - current.getCount());
            // The handler may restrict insertion further (input-only slots etc.); ask it with the real contents.
            final int handlerAccepts = probe.getCount() - handler.insertItem(index, probe, true).getCount();
            accepted = Math.min(accepted, handlerAccepts);
            if (accepted <= 0)
            {
                return 0;
            }
            updateSnapshots(transaction);
            staged = current.isEmpty() ? resource.toStack((int) accepted) : current.copyWithCount(current.getCount() + (int) accepted);
            return accepted;
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!valid() || maxAmount == 0)
            {
                return 0;
            }
            final ItemStack current = current();
            if (current.isEmpty() || !resource.matches(current))
            {
                return 0;
            }
            long extracted = Math.min(maxAmount, current.getCount());
            // The handler may forbid or limit extraction (output rules etc.); ask it while it still holds the real contents.
            final ItemStack real = handler.getStackInSlot(index);
            if (!real.isEmpty() && resource.matches(real))
            {
                final ItemStack handlerGives = handler.extractItem(index, (int) Math.min(extracted, Integer.MAX_VALUE), true);
                if (handlerGives.isEmpty())
                {
                    return 0;
                }
                if (staged == null)
                {
                    extracted = Math.min(extracted, handlerGives.getCount());
                }
            }
            if (extracted <= 0)
            {
                return 0;
            }
            updateSnapshots(transaction);
            staged = current.getCount() == extracted ? ItemStack.EMPTY : current.copyWithCount(current.getCount() - (int) extracted);
            return extracted;
        }

        @Override
        public boolean isResourceBlank()
        {
            return !valid() || current().isEmpty();
        }

        @Override
        public ItemVariant getResource()
        {
            return valid() ? ItemVariant.of(current()) : ItemVariant.blank();
        }

        @Override
        public long getAmount()
        {
            return valid() ? current().getCount() : 0;
        }

        @Override
        public long getCapacity()
        {
            if (!valid())
            {
                return 0;
            }
            final ItemStack stack = current();
            final int limit = handler.getSlotLimit(index);
            return stack.isEmpty() ? limit : Math.min(limit, stack.getMaxStackSize());
        }

        @Override
        protected Staged createSnapshot()
        {
            return new Staged(staged == null ? null : staged.copy());
        }

        @Override
        protected void readSnapshot(final Staged snapshot)
        {
            staged = snapshot.stack();
        }

        /**
         * Applies the staged contents to the handler once the outermost transaction committed.
         */
        @Override
        protected void onFinalCommit()
        {
            final ItemStack target = staged;
            staged = null;
            if (target == null || !valid())
            {
                return;
            }

            ItemStack real = handler.getStackInSlot(index);
            if (!real.isEmpty() && (target.isEmpty() || !ItemStack.isSameItemSameComponents(real, target)))
            {
                handler.extractItem(index, real.getCount(), false);
                real = handler.getStackInSlot(index);
            }

            final int diff = target.getCount() - real.getCount();
            if (diff > 0)
            {
                final ItemStack remainder = handler.insertItem(index, target.copyWithCount(diff), false);
                if (!remainder.isEmpty())
                {
                    LOGGER.warn("Item handler slot {} rejected {} committed by a transfer transaction", index, remainder);
                }
            }
            else if (diff < 0)
            {
                handler.extractItem(index, -diff, false);
            }
        }
    }
}
