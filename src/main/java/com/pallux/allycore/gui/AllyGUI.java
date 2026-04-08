package com.pallux.allycore.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface AllyGUI {
    void open();
    void handleClick(InventoryClickEvent event);
    Inventory getInventory();
}
