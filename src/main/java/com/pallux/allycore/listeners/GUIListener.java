package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;

public class GUIListener implements Listener {

    private final AllyCore plugin;

    public GUIListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AllyGUI gui = GUIRegistry.getOpenGUI(player.getUniqueId());
        // Only cancel if they are interacting with the GUI view
        if (gui != null && event.getView().getTopInventory().equals(gui.getInventory())) {
            event.setCancelled(true);

            // Process clicks only if they clicked inside the top inventory (not their own inventory)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(gui.getInventory())) {
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        AllyGUI gui = GUIRegistry.getOpenGUI(player.getUniqueId());
        // Only remove from registry if the closed inventory matches the current registered GUI
        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            GUIRegistry.removeOpenGUI(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AllyGUI gui = GUIRegistry.getOpenGUI(player.getUniqueId());
        if (gui != null && event.getView().getTopInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
        }
    }
}