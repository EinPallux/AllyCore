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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmoryMenu implements AllyGUI {

    private final AllyCore plugin;
    private final Player player;
    private final AllyEntity allyEntity;
    private final AllyData data;
    private final AllyStats stats;
    private Inventory inventory;

    public ArmoryMenu(AllyCore plugin, Player player, AllyEntity allyEntity) {
        this.plugin = plugin;
        this.player = player;
        this.allyEntity = allyEntity;
        this.data = allyEntity.getData();
        this.stats = allyEntity.getStats();
    }

    @Override
    public void open() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("armory-menu.title", "&#F97316⚔ Armory ✦");
        inventory = Bukkit.createInventory(null, 54, ColorUtil.component(title));
        fill();
        GUIRegistry.registerOpenGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    private void fill() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        FileConfiguration upgrades = plugin.getConfigManager().getUpgradesConfig();

        ItemBuilder bg = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg.build());

        long mementos = plugin.getMementoManager().getMementos(player.getUniqueId());
        double vaultBal = plugin.getEconomyManager().getBalance(player);

        // Currency display
        inventory.setItem(4, new ItemBuilder(Material.AMETHYST_SHARD)
                .name("&#6C63FF✦ Your Currency")
                .lore("&#A3A3A3Vault: &#FFFFFF$" + String.format("%.2f", vaultBal),
                      "&#A3A3A3Mementos: &#6C63FF" + mementos)
                .build());

        // Current armor display
        int curArmorSlot = gui.getInt("armory-menu.current-armor-display-slot", 11);
        buildCurrentDisplay(curArmorSlot, true);

        // Current weapon display
        int curWeaponSlot = gui.getInt("armory-menu.current-weapon-display-slot", 15);
        buildCurrentDisplay(curWeaponSlot, false);

        // Armor upgrade button
        int armorSlot = gui.getInt("armory-menu.armor-slot", 20);
        inventory.setItem(armorSlot, buildUpgradeItem("armor", upgrades));

        // Weapon upgrade button
        int weaponSlot = gui.getInt("armory-menu.weapon-slot", 24);
        inventory.setItem(weaponSlot, buildUpgradeItem("weapon", upgrades));

        // All armor tiers display (row 3)
        buildTierRow(29, "armor", upgrades);
        buildTierRow(38, "weapon", upgrades);

        // Back button
        int backSlot = gui.getInt("armory-menu.back-button.slot", 49);
        inventory.setItem(backSlot, new ItemBuilder(Material.ARROW)
                .name("&#A3A3A3← Back")
                .lore("&#A3A3A3Return to Ally Menu")
                .build());
    }

    private void buildCurrentDisplay(int slot, boolean isArmor) {
        FileConfiguration upgrades = plugin.getConfigManager().getUpgradesConfig();
        String type = isArmor ? "armor" : "weapon";
        int tier = isArmor ? data.getArmorTier() : data.getWeaponTier();
        String name = upgrades.getString("armory." + type + "-tiers." + tier + ".display-name", "None");
        String desc = upgrades.getString("armory." + type + "-tiers." + tier + ".description", "");
        Material mat = isArmor ? Material.IRON_CHESTPLATE : Material.IRON_SWORD;

        inventory.setItem(slot, new ItemBuilder(mat)
                .name("&#A8A4FF" + (isArmor ? "Current Armor" : "Current Weapon"))
                .lore("&#A3A3A3" + name, "", "&#A3A3A3" + desc)
                .hideAll()
                .build());
    }

    private org.bukkit.inventory.ItemStack buildUpgradeItem(String type, FileConfiguration upgrades) {
        int currentTier = type.equals("armor") ? data.getArmorTier() : data.getWeaponTier();
        int maxTier = upgrades.getConfigurationSection("armory." + type + "-tiers").getKeys(false).size() - 1;
        boolean isMaxed = currentTier >= maxTier;
        int nextTier = currentTier + 1;

        String currentName = upgrades.getString("armory." + type + "-tiers." + currentTier + ".display-name", "None");
        List<String> lore = new ArrayList<>();
        lore.add("&#A3A3A3Current: &#FFFFFF" + currentName);
        lore.add("");

        if (isMaxed) {
            lore.add("&#F59E0B✦ FULLY UPGRADED");
        } else {
            String nextName = upgrades.getString("armory." + type + "-tiers." + nextTier + ".display-name", "");
            double vault = upgrades.getDouble("armory." + type + "-tiers." + nextTier + ".vault", 0);
            long mem = upgrades.getLong("armory." + type + "-tiers." + nextTier + ".mementos", 0);
            lore.add("&#A8A4FFNext: &#FFFFFF" + nextName);
            lore.add("&#A3A3A3Cost: &#FFFFFF$" + String.format("%.0f", vault) + " &#A3A3A3+ &#6C63FF" + mem + " Mementos");
            lore.add("");
            lore.add("&#A8A4FFClick to upgrade");
        }

        Material icon = type.equals("armor") ? Material.DIAMOND_CHESTPLATE : Material.DIAMOND_SWORD;
        ItemBuilder ib = new ItemBuilder(icon)
                .name("&#F97316⚔ Upgrade " + (type.equals("armor") ? "Armor" : "Weapon"))
                .lore(lore)
                .hideAll();
        if (!isMaxed) ib.glow();
        return ib.build();
    }

    private void buildTierRow(int startSlot, String type, FileConfiguration upgrades) {
        var section = upgrades.getConfigurationSection("armory." + type + "-tiers");
        if (section == null) return;
        int i = 0;
        for (String key : section.getKeys(false)) {
            int tier = Integer.parseInt(key);
            int curTier = type.equals("armor") ? data.getArmorTier() : data.getWeaponTier();
            String name = upgrades.getString("armory." + type + "-tiers." + tier + ".display-name", key);
            String desc = upgrades.getString("armory." + type + "-tiers." + tier + ".description", "");
            boolean unlocked = curTier >= tier;

            Material m = type.equals("armor") ? Material.IRON_CHESTPLATE : Material.IRON_SWORD;
            List<String> lore = new ArrayList<>();
            lore.add("&#A3A3A3" + desc);
            lore.add("");
            lore.add(unlocked ? "&#10B981✔ Unlocked" : "&#EF4444✗ Locked");

            inventory.setItem(startSlot + i, new ItemBuilder(m)
                    .name(name)
                    .lore(lore)
                    .hideAll()
                    .build());
            i++;
            if (i >= 9) break;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        int slot = event.getSlot();
        int backSlot = gui.getInt("armory-menu.back-button.slot", 49);
        int armorSlot = gui.getInt("armory-menu.armor-slot", 20);
        int weaponSlot = gui.getInt("armory-menu.weapon-slot", 24);

        if (slot == backSlot) {
            new MainAllyMenu(plugin, player, allyEntity).open();
        } else if (slot == armorSlot) {
            purchaseUpgrade("armor");
        } else if (slot == weaponSlot) {
            purchaseUpgrade("weapon");
        }
        MessageUtil.playSound(player, "menu-click");
    }

    private void purchaseUpgrade(String type) {
        FileConfiguration upgrades = plugin.getConfigManager().getUpgradesConfig();
        int currentTier = type.equals("armor") ? data.getArmorTier() : data.getWeaponTier();
        int maxTier = upgrades.getConfigurationSection("armory." + type + "-tiers").getKeys(false).size() - 1;

        if (currentTier >= maxTier) {
            MessageUtil.send(player, "upgrade-max-tier");
            return;
        }

        int nextTier = currentTier + 1;
        double vault = upgrades.getDouble("armory." + type + "-tiers." + nextTier + ".vault", 0);
        long mem = upgrades.getLong("armory." + type + "-tiers." + nextTier + ".mementos", 0);

        if (!plugin.getEconomyManager().has(player, vault)) {
            MessageUtil.send(player, "upgrade-no-money", Map.of("vault", String.format("%.0f", vault)));
            return;
        }
        if (plugin.getMementoManager().getMementos(player.getUniqueId()) < mem) {
            MessageUtil.send(player, "upgrade-no-mementos", Map.of("mementos", String.valueOf(mem)));
            return;
        }

        plugin.getEconomyManager().withdraw(player, vault);
        plugin.getMementoManager().removeMementos(player.getUniqueId(), mem);

        if (type.equals("armor")) {
            data.setArmorTier(nextTier);
            MessageUtil.send(player, "armor-upgrade-success", Map.of("tier",
                    upgrades.getString("armory.armor-tiers." + nextTier + ".display-name", String.valueOf(nextTier))));
        } else {
            data.setWeaponTier(nextTier);
            MessageUtil.send(player, "weapon-upgrade-success", Map.of("tier",
                    upgrades.getString("armory.weapon-tiers." + nextTier + ".display-name", String.valueOf(nextTier))));
        }

        allyEntity.refreshAttributes();
        plugin.getAllyManager().saveAlly(player.getUniqueId());
        MessageUtil.playSound(player, "upgrade-purchased");
        fill();
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
