package com.ldtteam.shim.fabric.capabilities;

import com.ldtteam.shim.neoforge.items.IItemHandler;
import com.ldtteam.shim.neoforge.items.IItemHandlerModifiable;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Exposes a NeoForge-style {@link IItemHandler} as a Fabric Transfer API {@code SlottedStorage<ItemVariant>}.
 * <p>
 * Modifiable handlers are mutated immediately and rolled back through {@link SnapshotParticipant}s if the transaction aborts.
 * Non-modifiable handlers are simulated during the transaction and the real operation is applied when the outermost transaction commits.
 */
public class ItemHandlerStorage implements SlottedStorage<ItemVariant>
{
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
                final ItemStack existing = handler.getStackInSlot(i);
                final boolean matches = !existing.isEmpty() && resource.matches(existing);
                if ((pass == 0) == matches)
                {
                    inserted += getSlot(i).insert(resource, maxAmount - inserted, transaction);
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

    private class Slot extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<ItemVariant>
    {
        private final int index;

        private Slot(final int index)
        {
            this.index = index;
        }

        private boolean valid()
        {
            return index < handler.getSlots();
        }

        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!valid() || maxAmount == 0)
            {
                return 0;
            }
            final ItemStack toInsert = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
            final ItemStack simulated = handler.insertItem(index, toInsert, true);
            final int inserted = toInsert.getCount() - simulated.getCount();
            if (inserted <= 0)
            {
                return 0;
            }
            if (handler instanceof IItemHandlerModifiable)
            {
                updateSnapshots(transaction);
                handler.insertItem(index, resource.toStack(inserted), false);
            }
            else
            {
                transaction.addOuterCloseCallback(result -> {
                    if (result.wasCommitted())
                    {
                        handler.insertItem(index, resource.toStack(inserted), false);
                    }
                });
            }
            return inserted;
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction)
        {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!valid() || maxAmount == 0)
            {
                return 0;
            }
            final ItemStack current = handler.getStackInSlot(index);
            if (current.isEmpty() || !resource.matches(current))
            {
                return 0;
            }
            final int toExtract = (int) Math.min(maxAmount, Integer.MAX_VALUE);
            final ItemStack simulated = handler.extractItem(index, toExtract, true);
            if (simulated.isEmpty() || !resource.matches(simulated))
            {
                return 0;
            }
            final int extracted = simulated.getCount();
            if (handler instanceof IItemHandlerModifiable)
            {
                updateSnapshots(transaction);
                handler.extractItem(index, extracted, false);
            }
            else
            {
                transaction.addOuterCloseCallback(result -> {
                    if (result.wasCommitted())
                    {
                        handler.extractItem(index, extracted, false);
                    }
                });
            }
            return extracted;
        }

        @Override
        public boolean isResourceBlank()
        {
            return !valid() || handler.getStackInSlot(index).isEmpty();
        }

        @Override
        public ItemVariant getResource()
        {
            return valid() ? ItemVariant.of(handler.getStackInSlot(index)) : ItemVariant.blank();
        }

        @Override
        public long getAmount()
        {
            return valid() ? handler.getStackInSlot(index).getCount() : 0;
        }

        @Override
        public long getCapacity()
        {
            if (!valid())
            {
                return 0;
            }
            final ItemStack stack = handler.getStackInSlot(index);
            final int limit = handler.getSlotLimit(index);
            return stack.isEmpty() ? limit : Math.min(limit, stack.getMaxStackSize());
        }

        @Override
        protected ItemStack createSnapshot()
        {
            return handler.getStackInSlot(index).copy();
        }

        @Override
        protected void readSnapshot(final ItemStack snapshot)
        {
            ((IItemHandlerModifiable) handler).setStackInSlot(index, snapshot);
        }
    }
}
