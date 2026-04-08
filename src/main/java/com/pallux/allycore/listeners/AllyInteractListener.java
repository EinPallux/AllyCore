package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.gui.MainAllyMenu;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class AllyInteractListener implements Listener {

    private final AllyCore plugin;

    public AllyInteractListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        // Only main hand
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Zombie zombie)) return;

        UUID ownerUUID = plugin.getAllyManager().getOwnerOfEntity(zombie.getUniqueId());
        if (ownerUUID == null) return;

        Player player = event.getPlayer();

        // Only the owner can interact
        if (!player.getUniqueId().equals(ownerUUID)) return;

        event.setCancelled(true);

        if (!player.hasPermission("allycore.player")) {
            MessageUtil.send(player, "no-permission");
            return;
        }

        // Shift + Right-Click → open menu
        if (player.isSneaking()) {
            AllyEntity ae = plugin.getAllyManager().getActiveAlly(ownerUUID);
            if (ae == null) return;
            new MainAllyMenu(plugin, player, ae).open();
            MessageUtil.playSound(player, "menu-open");
        }
        // Normal right-click → show quick info
        else {
            AllyData data = plugin.getAllyManager().getAllyData(ownerUUID);
            if (data == null) return;
            AllyEntity ae = plugin.getAllyManager().getActiveAlly(ownerUUID);
            double hp = ae != null && ae.isSpawned() ? ae.getEntity().getHealth() : data.getCurrentHealth();
            String msg = plugin.getConfigManager().getMessage("prefix")
                    + "&#A8A4FF" + data.getDisplayName()
                    + " &#A3A3A3| &#EF4444❤ " + String.format("%.1f", hp)
                    + " &#A3A3A3| &#A8A4FFLvl " + data.getLevel()
                    + " &#A3A3A3| &#FFFFFF" + data.getMode().name();
            player.sendMessage(msg);
        }
    }
}
