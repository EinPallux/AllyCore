package com.pallux.allycore.gui;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.AllyStats;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.ItemBuilder;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatTreeMenu implements AllyGUI {

    private final AllyCore plugin;
    private final Player player;
    private final AllyEntity allyEntity;
    private final AllyData data;
    private final AllyStats stats;
    private Inventory inventory;

    // The stat currently selected for upgrade (null = none)
    private String selectedStat = null;

    // Stat → slot mapping
    private static final Map<String, Integer> STAT_SLOTS = Map.of(
            "health", 20,
            "attack", 22,
            "defense", 24,
            "speed", 30,
            "regeneration", 32
    );

    public StatTreeMenu(AllyCore plugin, Player player, AllyEntity allyEntity) {
        this.plugin = plugin;
        this.player = player;
        this.allyEntity = allyEntity;
        this.data = allyEntity.getData();
        this.stats = allyEntity.getStats();
    }

    @Override
    public void open() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("stat-tree-menu.title", "&#A8A4FF✦ Stat Tree ✦");
        inventory = Bukkit.createInventory(null, 54, ColorUtil.component(title));
        fill();
        GUIRegistry.registerOpenGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    private void fill() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        FileConfiguration upgrades = plugin.getConfigManager().getUpgradesConfig();

        // Background
        ItemBuilder bg = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg.build());

        // Stat buttons
        for (Map.Entry<String, Integer> entry : STAT_SLOTS.entrySet()) {
            String statKey = entry.getKey();
            int slot = gui.getInt("stat-tree-menu.stat-slots." + statKey, entry.getValue());
            inventory.setItem(slot, buildStatItem(statKey, upgrades));
        }

        // If a stat is selected, show upgrade confirm/cancel
        if (selectedStat != null) {
            int confirmSlot = gui.getInt("stat-tree-menu.confirm-slot", 31);
            int cancelSlot = gui.getInt("stat-tree-menu.cancel-slot", 29);

            int currentTier = getStatTier(selectedStat);
            int maxTier = upgrades.getInt("stat-tree." + selectedStat + ".max-tier", 10);
            int nextTier = currentTier + 1;

            if (currentTier < maxTier) {
                double vaultCost = upgrades.getDouble("stat-tree." + selectedStat + ".tiers." + nextTier + ".vault", 0);
                long memCost = upgrades.getLong("stat-tree." + selectedStat + ".tiers." + nextTier + ".mementos", 0);
                String statDisplay = upgrades.getString("stat-tree." + selectedStat + ".display-name", selectedStat);
                double bonusPerTier = upgrades.getDouble("stat-tree." + selectedStat + ".bonus-per-tier", 0);

                inventory.setItem(confirmSlot, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .name("&#10B981✔ Confirm Upgrade")
                        .lore(
                                "&#A3A3A3Upgrade " + statDisplay + " to Tier &#FFFFFF" + nextTier,
                                "",
                                "&#FFFFFF$" + String.format("%.0f", vaultCost) + " &#A3A3A3+ &#6C63FF" + memCost + " Mementos",
                                "&#A3A3A3Bonus: &#FFFFFF+" + bonusPerTier,
                                "",
                                "&#10B981Click to confirm"
                        ).build());

                inventory.setItem(cancelSlot, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                        .name("&#EF4444✗ Cancel")
                        .lore("&#A3A3A3Click to go back")
                        .build());
            }
        }

        // Back button
        int backSlot = gui.getInt("stat-tree-menu.back-button.slot", 49);
        inventory.setItem(backSlot, new ItemBuilder(Material.ARROW)
                .name("&#A3A3A3← Back")
                .lore("&#A3A3A3Return to Ally Menu")
                .build());

        // Currency display
        long mementos = plugin.getMementoManager().getMementos(player.getUniqueId());
        double vaultBal = plugin.getEconomyManager().getBalance(player);
        inventory.setItem(4, new ItemBuilder(Material.AMETHYST_SHARD)
                .name("&#6C63FF✦ Your Currency")
                .lore(
                        "&#A3A3A3Vault: &#FFFFFF$" + String.format("%.2f", vaultBal),
                        "&#A3A3A3Mementos: &#6C63FF" + mementos
                ).build());
    }

    private org.bukkit.inventory.ItemStack buildStatItem(String statKey) {
        return buildStatItem(statKey, plugin.getConfigManager().getUpgradesConfig());
    }

    private org.bukkit.inventory.ItemStack buildStatItem(String statKey, FileConfiguration upgrades) {
        int tier = getStatTier(statKey);
        int maxTier = upgrades.getInt("stat-tree." + statKey + ".max-tier", 10);
        String display = upgrades.getString("stat-tree." + statKey + ".display-name", statKey);
        String desc = upgrades.getString("stat-tree." + statKey + ".description", "");

        boolean isSelected = statKey.equals(selectedStat);
        boolean isMaxed = tier >= maxTier;

        Material mat = getStatMaterial(statKey);
        List<String> lore = new ArrayList<>();
        lore.add("&#A3A3A3" + desc);
        lore.add("");
        lore.add("&#A8A4FFCurrent Tier: &#FFFFFF" + tier + "&#A3A3A3/&#FFFFFF" + maxTier);
        lore.add("&#A8A4FFBonus: &#FFFFFF+" + (tier * upgrades.getDouble("stat-tree." + statKey + ".bonus-per-tier", 0)));
        lore.add("");

        if (isMaxed) {
            lore.add("&#F59E0B✦ MAXED OUT");
        } else {
            int nextTier = tier + 1;
            double vaultCost = upgrades.getDouble("stat-tree." + statKey + ".tiers." + nextTier + ".vault", 0);
            long memCost = upgrades.getLong("stat-tree." + statKey + ".tiers." + nextTier + ".mementos", 0);
            lore.add("&#A3A3A3Next tier cost:");
            lore.add("  &#FFFFFF$" + String.format("%.0f", vaultCost) + " &#A3A3A3+ &#6C63FF" + memCost + " Mementos");
            lore.add("");
            lore.add(isSelected ? "&#10B981► Selected — confirm below" : "&#A8A4FFClick to select");
        }

        // Build progress bar
        lore.add(buildProgressBar(tier, maxTier));

        ItemBuilder builder = new ItemBuilder(mat).name(display).lore(lore);
        if (isSelected) builder.glow();

        return builder.build();
    }

    private String buildProgressBar(int tier, int max) {
        StringBuilder sb = new StringBuilder("&#A3A3A3[");
        int filled = (max > 0) ? (int) Math.round((double) tier / max * 10) : 0;
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "&#6C63FF■" : "&#383838■");
        }
        sb.append("&#A3A3A3]");
        return sb.toString();
    }

    private Material getStatMaterial(String stat) {
        return switch (stat) {
            case "health" -> Material.APPLE;
            case "attack" -> Material.IRON_SWORD;
            case "defense" -> Material.IRON_CHESTPLATE;
            case "speed" -> Material.SUGAR;
            case "regeneration" -> Material.GLISTERING_MELON_SLICE;
            default -> Material.PAPER;
        };
    }

    private int getStatTier(String stat) {
        return switch (stat) {
            case "health" -> data.getHealthTier();
            case "attack" -> data.getAttackTier();
            case "defense" -> data.getDefenseTier();
            case "speed" -> data.getSpeedTier();
            case "regeneration" -> data.getRegenerationTier();
            default -> 0;
        };
    }

    private void setStatTier(String stat, int tier) {
        switch (stat) {
            case "health" -> data.setHealthTier(tier);
            case "attack" -> data.setAttackTier(tier);
            case "defense" -> data.setDefenseTier(tier);
            case "speed" -> data.setSpeedTier(tier);
            case "regeneration" -> data.setRegenerationTier(tier);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        FileConfiguration upgrades = plugin.getConfigManager().getUpgradesConfig();
        int slot = event.getSlot();
        int backSlot = gui.getInt("stat-tree-menu.back-button.slot", 49);
        int confirmSlot = gui.getInt("stat-tree-menu.confirm-slot", 31);
        int cancelSlot = gui.getInt("stat-tree-menu.cancel-slot", 29);

        if (slot == backSlot) {
            new MainAllyMenu(plugin, player, allyEntity).open();
            return;
        }

        if (slot == cancelSlot && selectedStat != null) {
            selectedStat = null;
            fill();
            return;
        }

        if (slot == confirmSlot && selectedStat != null) {
            purchaseUpgrade(selectedStat, upgrades);
            return;
        }

        // Check stat slot clicks
        for (Map.Entry<String, Integer> entry : STAT_SLOTS.entrySet()) {
            int statSlot = gui.getInt("stat-tree-menu.stat-slots." + entry.getKey(), entry.getValue());
            if (slot == statSlot) {
                String stat = entry.getKey();
                int currentTier = getStatTier(stat);
                int maxTier = upgrades.getInt("stat-tree." + stat + ".max-tier", 10);

                if (currentTier >= maxTier) {
                    MessageUtil.send(player, "upgrade-max-tier");
                    return;
                }

                // Toggle selection
                selectedStat = stat.equals(selectedStat) ? null : stat;
                fill();
                return;
            }
        }
    }

    private void purchaseUpgrade(String stat, FileConfiguration upgrades) {
        int currentTier = getStatTier(stat);
        int maxTier = upgrades.getInt("stat-tree." + stat + ".max-tier", 10);
        if (currentTier >= maxTier) {
            MessageUtil.send(player, "upgrade-max-tier");
            selectedStat = null;
            fill();
            return;
        }

        int nextTier = currentTier + 1;
        double vaultCost = upgrades.getDouble("stat-tree." + stat + ".tiers." + nextTier + ".vault", 0);
        long memCost = upgrades.getLong("stat-tree." + stat + ".tiers." + nextTier + ".mementos", 0);

        // Check vault
        if (!plugin.getEconomyManager().has(player, vaultCost)) {
            MessageUtil.send(player, "upgrade-no-money", Map.of("vault", String.format("%.0f", vaultCost)));
            return;
        }
        // Check mementos
        if (plugin.getMementoManager().getMementos(player.getUniqueId()) < memCost) {
            MessageUtil.send(player, "upgrade-no-mementos", Map.of("mementos", String.valueOf(memCost)));
            return;
        }

        // Charge
        plugin.getEconomyManager().withdraw(player, vaultCost);
        plugin.getMementoManager().removeMementos(player.getUniqueId(), memCost);

        // Apply
        setStatTier(stat, nextTier);
        allyEntity.refreshAttributes();
        plugin.getAllyManager().saveAlly(player.getUniqueId());

        String displayName = upgrades.getString("stat-tree." + stat + ".display-name", stat);
        MessageUtil.send(player, "upgrade-success", Map.of(
                "stat", displayName,
                "tier", String.valueOf(nextTier)
        ));
        MessageUtil.playSound(player, "upgrade-purchased");

        selectedStat = null;
        fill();
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
