package com.woodspixel.bazaars.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class BazaarHolder implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}
