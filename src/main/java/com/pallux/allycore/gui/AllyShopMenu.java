package com.pallux.allycore.gui;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.ItemBuilder;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllyShopMenu implements AllyGUI {

    private final AllyCore plugin;
    private final Player player;
    private Inventory inventory;

    public AllyShopMenu(AllyCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void open() {
        FileConfiguration eco = plugin.getConfigManager().getEconomyConfig();
        String title = eco.getString("shop.shop-title", "&#6C63FF✦ Ally Shop ✦");
        inventory = Bukkit.createInventory(null, 27, ColorUtil.component(title));
        fill();
        GUIRegistry.registerOpenGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    private void fill() {
        FileConfiguration eco = plugin.getConfigManager().getEconomyConfig();

        ItemBuilder bg = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, bg.build());

        boolean hasAlly = plugin.getAllyManager().hasAlly(player.getUniqueId());
        double price = eco.getDouble("shop.ally-price-vault", 5000);
        long memPrice = eco.getLong("shop.ally-price-mementos", 0);
        List<String> desc = eco.getStringList("shop.featured-description");

        // Decorative border
        inventory.setItem(1, new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).name(" ").build());
        inventory.setItem(7, new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).name(" ").build());

        // Main purchase item
        List<String> lore = new ArrayList<>(desc);
        lore.add("");
        lore.add("&#A3A3A3Price: &#FFFFFF$" + String.format("%.0f", price) + (memPrice > 0 ? " &#A3A3A3+ &#6C63FF" + memPrice + " Mementos" : ""));
        lore.add("");
        if (hasAlly) {
            lore.add("&#EF4444✗ You already own an Ally");
        } else {
            lore.add("&#10B981Click to purchase");
        }

        Material mat = hasAlly ? Material.GRAY_STAINED_GLASS_PANE : Material.EMERALD;
        ItemBuilder purchase = new ItemBuilder(mat)
                .name("&#6C63FF✦ Hire an Ally")
                .lore(lore);
        if (!hasAlly) purchase.glow();
        inventory.setItem(13, purchase.build());

        // Balance display
        double bal = plugin.getEconomyManager().getBalance(player);
        long mementos = plugin.getMementoManager().getMementos(player.getUniqueId());
        inventory.setItem(22, new ItemBuilder(Material.AMETHYST_SHARD)
                .name("&#6C63FF✦ Your Balance")
                .lore("&#A3A3A3Vault: &#FFFFFF$" + String.format("%.2f", bal),
                      "&#A3A3A3Mementos: &#6C63FF" + mementos)
                .build());

        // Close
        inventory.setItem(26, new ItemBuilder(Material.BARRIER)
                .name("&#EF4444✗ Close")
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();

        if (slot == 26) { player.closeInventory(); return; }

        if (slot == 13) {
            if (plugin.getAllyManager().hasAlly(player.getUniqueId())) {
                MessageUtil.send(player, "ally-already-exists");
                return;
            }
            FileConfiguration eco = plugin.getConfigManager().getEconomyConfig();
            double price = eco.getDouble("shop.ally-price-vault", 5000);
            long memPrice = eco.getLong("shop.ally-price-mementos", 0);

            if (!plugin.getEconomyManager().has(player, price)) {
                MessageUtil.send(player, "shop-no-money");
                return;
            }

            plugin.getEconomyManager().withdraw(player, price);
            if (memPrice > 0) plugin.getMementoManager().removeMementos(player.getUniqueId(), memPrice);

            plugin.getAllyManager().createAlly(player);
            plugin.getAllyManager().spawnAlly(player);
            MessageUtil.send(player, "shop-purchase-success");
            MessageUtil.playSound(player, "ally-summoned");
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

}
