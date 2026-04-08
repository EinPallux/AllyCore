package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.gui.AnvilRenameRegistry;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;

public class AnvilRenameListener implements Listener {

    private final AllyCore plugin;

    public AnvilRenameListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!AnvilRenameRegistry.isRenaming(player.getUniqueId())) return;

        ItemStack result = event.getResult();
        if (result == null) return;

        String name = event.getInventory().getRenameText();
        if (name == null || name.isBlank()) return;

        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(name));
            result.setItemMeta(meta);
        }
        event.setResult(result);
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!AnvilRenameRegistry.isRenaming(player.getUniqueId())) return;
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;

        if (event.getRawSlot() != 2) return;
        if (event.getCurrentItem() == null) return;

        event.setCancelled(true);

        String rawName = anvil.getRenameText();
        if (rawName == null || rawName.isBlank()) {
            MessageUtil.send(player, "rename-invalid");
            return;
        }

        int maxLen = 32;
        if (rawName.length() > maxLen) {
            MessageUtil.send(player, "rename-too-long", Map.of("max", String.valueOf(maxLen)));
            return;
        }

        AllyData data = plugin.getAllyManager().getAllyData(player.getUniqueId());
        if (data == null) return;

        data.setCustomName(rawName);
        plugin.getAllyManager().saveAlly(player.getUniqueId());

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(player.getUniqueId());
        if (ae != null && ae.getHologram() != null) ae.getHologram().update();

        MessageUtil.send(player, "rename-success", Map.of("name", rawName));
        MessageUtil.playSound(player, "upgrade-purchased");

        AnvilRenameRegistry.setRenaming(player.getUniqueId(), false);
        player.closeInventory();
    }
}
