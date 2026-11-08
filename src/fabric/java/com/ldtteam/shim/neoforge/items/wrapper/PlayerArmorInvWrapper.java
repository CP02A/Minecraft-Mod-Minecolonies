// Adapted from NeoForge 21.1 (net.neoforged.neoforge), licensed LGPL-2.1-only. Ported to Fabric for the MineColonies Fabric shim.
/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package com.ldtteam.shim.neoforge.items.wrapper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PlayerArmorInvWrapper extends RangedWrapper {
    private final Inventory inventoryPlayer;

    public PlayerArmorInvWrapper(Inventory inv) {
        super(new InvWrapper(inv), Inventory.INVENTORY_SIZE, Inventory.INVENTORY_SIZE + 4);
        inventoryPlayer = inv;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        EquipmentSlot equ = null;
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && s.getIndex() == slot) {
                equ = s;
                break;
            }
        }
        // check if it's valid for the armor slot
        if (equ != null && slot < 4 && !stack.isEmpty() && getInventoryPlayer().player.isEquippableInSlot(stack, equ)) {
            return super.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    public Inventory getInventoryPlayer() {
        return inventoryPlayer;
    }
}
