package com.ldtteam.shim.fabric.capabilities;

import com.ldtteam.shim.neoforge.items.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes a Fabric Transfer API {@code Storage<ItemVariant>} (from vanilla or other Fabric mods) as a NeoForge-style {@link IItemHandler}.
 * <p>
 * Slotted storages map 1:1 onto slots. Non-slotted storages expose one slot per storage view plus one extra empty "virtual" slot that
 * inserts into the storage as a whole.
 */
public class StorageItemHandler implements IItemHandler
{
    private final Storage<ItemVariant> storage;
    private final SlottedStorage<ItemVariant> slotted;
    private List<StorageView<ItemVariant>> views;

    public StorageItemHandler(final Storage<ItemVariant> storage)
    {
        this.storage = storage;
        this.slotted = storage instanceof SlottedStorage<ItemVariant> s ? s : null;
    }

    public Storage<ItemVariant> getStorage()
    {
        return storage;
    }

    @SuppressWarnings("deprecation")
    private static Transaction open()
    {
        return Transaction.openNested(Transaction.getCurrentUnsafe());
    }

    private List<StorageView<ItemVariant>> views()
    {
        final List<StorageView<ItemVariant>> list = new ArrayList<>();
        try (Transaction ignored = open())
        {
            for (final StorageView<ItemVariant> view : storage)
            {
                list.add(view);
            }
        }
        views = list;
        return list;
    }

    private StorageView<ItemVariant> view(final int slot)
    {
        if (slotted != null)
        {
            return slot >= 0 && slot < slotted.getSlotCount() ? slotted.getSlot(slot) : null;
        }
        final List<StorageView<ItemVariant>> list = views == null || slot >= views.size() ? views() : views;
        return slot >= 0 && slot < list.size() ? list.get(slot) : null;
    }

    @Override
    public int getSlots()
    {
        return slotted != null ? slotted.getSlotCount() : views().size() + 1;
    }

    @Override
    public ItemStack getStackInSlot(final int slot)
    {
        final StorageView<ItemVariant> view = view(slot);
        if (view == null || view.isResourceBlank() || view.getAmount() <= 0)
        {
            return ItemStack.EMPTY;
        }
        return view.getResource().toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        final Storage<ItemVariant> target;
        if (slotted != null)
        {
            if (slot < 0 || slot >= slotted.getSlotCount())
            {
                return stack;
            }
            target = slotted.getSlot(slot);
        }
        else
        {
            final StorageView<ItemVariant> view = view(slot);
            if (view == null)
            {
                // Virtual slot: insert into the storage as a whole.
                target = storage;
            }
            else if (view instanceof Storage<?>)
            {
                @SuppressWarnings("unchecked") final Storage<ItemVariant> viewStorage = (Storage<ItemVariant>) view;
                target = viewStorage;
            }
            else
            {
                return stack;
            }
        }
        if (!target.supportsInsertion())
        {
            return stack;
        }
        final long inserted;
        try (Transaction tx = open())
        {
            inserted = target.insert(ItemVariant.of(stack), stack.getCount(), tx);
            if (!simulate)
            {
                tx.commit();
            }
        }
        if (inserted <= 0)
        {
            return stack;
        }
        if (inserted >= stack.getCount())
        {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(stack.getCount() - (int) inserted);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate)
    {
        if (amount <= 0)
        {
            return ItemStack.EMPTY;
        }
        final StorageView<ItemVariant> view = view(slot);
        if (view == null || view.isResourceBlank() || view.getAmount() <= 0)
        {
            return ItemStack.EMPTY;
        }
        final ItemVariant resource = view.getResource();
        final long extracted;
        try (Transaction tx = open())
        {
            extracted = view.extract(resource, Math.min(amount, resource.getItem().getDefaultMaxStackSize()), tx);
            if (!simulate)
            {
                tx.commit();
            }
        }
        return extracted <= 0 ? ItemStack.EMPTY : resource.toStack((int) extracted);
    }

    @Override
    public int getSlotLimit(final int slot)
    {
        final StorageView<ItemVariant> view = view(slot);
        if (view == null)
        {
            return 64;
        }
        return (int) Math.min(view.getCapacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        final ItemStack remainder = insertItem(slot, stack.copyWithCount(1), true);
        return remainder.isEmpty();
    }
}
