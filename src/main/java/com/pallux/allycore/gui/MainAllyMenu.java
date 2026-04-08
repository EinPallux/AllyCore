package com.pallux.allycore.gui;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.AllyStats;
import com.pallux.allycore.gui.AnvilRenameRegistry;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.ItemBuilder;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class MainAllyMenu implements AllyGUI {

    private final AllyCore plugin;
    private final Player player;
    private final AllyEntity allyEntity;
    private final AllyData data;
    private final AllyStats stats;
    private Inventory inventory;

    private static final int[] BORDER_SLOTS = {0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
    private static final int[] ACCENT_SLOTS = {4,10,16,28,34,37,43};

    public MainAllyMenu(AllyCore plugin, Player player, AllyEntity allyEntity) {
        this.plugin = plugin;
        this.player = player;
        this.allyEntity = allyEntity;
        this.data = allyEntity.getData();
        this.stats = allyEntity.getStats();
    }

    @Override
    public void open() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("main-menu.title", "&#6C63FF✦ Ally Menu ✦");
        int rows = gui.getInt("main-menu.rows", 6);
        inventory = Bukkit.createInventory(null, rows * 9, ColorUtil.component(title));

        fill();
        GUIRegistry.registerOpenGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    private void fill() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        // Background
        String bgMat = gui.getString("main-menu.background-item.material", "BLACK_STAINED_GLASS_PANE");
        ItemStack bg = new ItemBuilder(Material.valueOf(bgMat)).name(" ").build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

        // Accent
        String accentMat = gui.getString("main-menu.accent-item.material", "PURPLE_STAINED_GLASS_PANE");
        ItemStack accent = new ItemBuilder(Material.valueOf(accentMat)).name(" ").build();
        for (int s : ACCENT_SLOTS) inventory.setItem(s, accent);

        double hp = allyEntity.isSpawned() ? allyEntity.getEntity().getHealth() : data.getCurrentHealth();
        double maxHp = stats.getMaxHealth();
        long mementos = plugin.getMementoManager().getMementos(player.getUniqueId());
        double xpNeeded = stats.getXpRequired();

        String modeDisplay = plugin.getConfigManager().getAllyConfig()
                .getString("modes." + data.getMode().name() + ".display", data.getMode().name());

        // Stat-Tree button
        int statSlot = gui.getInt("main-menu.buttons.stat-tree.slot", 20);
        inventory.setItem(statSlot, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(gui.getString("main-menu.buttons.stat-tree.name", "&#A8A4FF✦ Stat Tree"))
                .lore(gui.getStringList("main-menu.buttons.stat-tree.lore"))
                .build());

        // Armory button
        int armorySlot = gui.getInt("main-menu.buttons.armory.slot", 22);
        inventory.setItem(armorySlot, new ItemBuilder(Material.IRON_SWORD)
                .name(gui.getString("main-menu.buttons.armory.name", "&#F97316⚔ Armory"))
                .lore(gui.getStringList("main-menu.buttons.armory.lore"))
                .hideAll()
                .build());

        // Mode button
        int modeSlot = gui.getInt("main-menu.buttons.modes.slot", 24);
        List<String> modeLore = gui.getStringList("main-menu.buttons.modes.lore");
        modeLore.replaceAll(s -> s.replace("{mode}", modeDisplay));
        inventory.setItem(modeSlot, new ItemBuilder(Material.COMPASS)
                .name(gui.getString("main-menu.buttons.modes.name", "&#F59E0B⚙ Combat Mode"))
                .lore(modeLore)
                .build());

        // Info (center)
        int infoSlot = gui.getInt("main-menu.buttons.info.slot", 4);
        List<String> infoLore = gui.getStringList("main-menu.buttons.info.lore");
        infoLore.replaceAll(s -> s
                .replace("{level}", String.valueOf(data.getLevel()))
                .replace("{health}", String.format("%.1f", hp))
                .replace("{max_health}", String.format("%.1f", maxHp))
                .replace("{attack}", String.format("%.1f", stats.getAttackDamage()))
                .replace("{defense}", String.format("%.1f", stats.getDefense()))
                .replace("{speed}", String.format("%.2f", stats.getSpeed()))
                .replace("{xp}", String.format("%.0f", data.getXp()))
                .replace("{xp_needed}", String.format("%.0f", xpNeeded))
                .replace("{mementos}", String.valueOf(mementos)));
        String infoName = gui.getString("main-menu.buttons.info.name", "&#6C63FF{ally_name}")
                .replace("{ally_name}", data.getDisplayName());
        inventory.setItem(infoSlot, new ItemBuilder(Material.PLAYER_HEAD)
                .name(infoName)
                .lore(infoLore)
                .build());

        // Rename
        int renameSlot = gui.getInt("main-menu.buttons.rename.slot", 31);
        List<String> renameLore = gui.getStringList("main-menu.buttons.rename.lore");
        renameLore.replaceAll(s -> s.replace("{ally_name}", data.getDisplayName()));
        inventory.setItem(renameSlot, new ItemBuilder(Material.NAME_TAG)
                .name(gui.getString("main-menu.buttons.rename.name", "&#10B981✏ Rename Ally"))
                .lore(renameLore)
                .build());

        // Summon/Dismiss
        int summonSlot = gui.getInt("main-menu.buttons.summon-dismiss.slot", 33);
        String statusStr = data.isSummoned() ? "&#10B981Active" : "&#A3A3A3Dismissed";
        List<String> summonLore = gui.getStringList("main-menu.buttons.summon-dismiss.lore");
        summonLore.replaceAll(s -> s.replace("{status}", statusStr));
        inventory.setItem(summonSlot, new ItemBuilder(Material.ENDER_PEARL)
                .name(gui.getString("main-menu.buttons.summon-dismiss.name", "&#A8A4FF⊕ Summon / Dismiss"))
                .lore(summonLore)
                .build());

        // Follow/Stay
        int followSlot = gui.getInt("main-menu.buttons.follow-stay.slot", 29);
        String followStatus = data.isFollowing() ? "&#10B981Following" : "&#F59E0BStaying";
        List<String> followLore = gui.getStringList("main-menu.buttons.follow-stay.lore");
        followLore.replaceAll(s -> s.replace("{follow_status}", followStatus));
        inventory.setItem(followSlot, new ItemBuilder(Material.FEATHER)
                .name(gui.getString("main-menu.buttons.follow-stay.name", "&#A8A4FF➤ Follow / Stay"))
                .lore(followLore)
                .build());

        // Revive (only shown/enabled when dead)
        int reviveSlot = gui.getInt("main-menu.buttons.revive.slot", 49);
        double reviveVault = plugin.getConfigManager().getEconomyConfig().getDouble("revive.vault-cost", 500);
        long reviveMementos = plugin.getConfigManager().getEconomyConfig().getLong("mementos-cost", 25);
        List<String> reviveLore = gui.getStringList("main-menu.buttons.revive.lore");
        reviveLore.replaceAll(s -> s
                .replace("{vault_cost}", String.format("%.0f", reviveVault))
                .replace("{mementos_cost}", String.valueOf(reviveMementos)));
        Material reviveMat = data.isAlive() ? Material.GRAY_STAINED_GLASS_PANE : Material.TOTEM_OF_UNDYING;
        inventory.setItem(reviveSlot, new ItemBuilder(reviveMat)
                .name(gui.getString("main-menu.buttons.revive.name", "&#EF4444💀 Revive Ally"))
                .lore(reviveLore)
                .build());

        // Close
        int closeSlot = gui.getInt("main-menu.buttons.close.slot", 53);
        inventory.setItem(closeSlot, new ItemBuilder(Material.BARRIER)
                .name(gui.getString("main-menu.buttons.close.name", "&#EF4444✗ Close"))
                .lore(gui.getStringList("main-menu.buttons.close.lore"))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        int slot = event.getSlot();

        if (slot == gui.getInt("main-menu.buttons.stat-tree.slot", 20)) {
            new StatTreeMenu(plugin, player, allyEntity).open();
        } else if (slot == gui.getInt("main-menu.buttons.armory.slot", 22)) {
            new ArmoryMenu(plugin, player, allyEntity).open();
        } else if (slot == gui.getInt("main-menu.buttons.modes.slot", 24)) {
            new ModeMenu(plugin, player, allyEntity).open();
        } else if (slot == gui.getInt("main-menu.buttons.rename.slot", 31)) {
            openRenameAnvil();
        } else if (slot == gui.getInt("main-menu.buttons.summon-dismiss.slot", 33)) {
            toggleSummon();
        } else if (slot == gui.getInt("main-menu.buttons.follow-stay.slot", 29)) {
            toggleFollow();
        } else if (slot == gui.getInt("main-menu.buttons.revive.slot", 49)) {
            if (!data.isAlive()) handleRevive();
        } else if (slot == gui.getInt("main-menu.buttons.close.slot", 53)) {
            player.closeInventory();
        }

        MessageUtil.playSound(player, "menu-click");
    }

    private void toggleSummon() {
        if (!data.isAlive()) {
            MessageUtil.send(player, "ally-dead");
            return;
        }
        if (data.isSummoned()) {
            plugin.getAllyManager().despawnAlly(player.getUniqueId());
            MessageUtil.send(player, "ally-dismissed");
        } else {
            plugin.getAllyManager().spawnAlly(player);
            MessageUtil.send(player, "ally-summoned");
        }
        plugin.getAllyManager().saveAlly(player.getUniqueId());
        player.closeInventory();
    }

    private void toggleFollow() {
        data.setFollowing(!data.isFollowing());
        String msg = data.isFollowing()
                ? plugin.getConfigManager().getMessage("ally-following")
                : plugin.getConfigManager().getMessage("ally-staying");
        player.sendMessage(msg);
        plugin.getAllyManager().saveAlly(player.getUniqueId());
        player.closeInventory();
    }

    private void handleRevive() {
        double vaultCost = plugin.getConfigManager().getEconomyConfig().getDouble("revive.vault-cost", 500);
        long mementoCost = plugin.getConfigManager().getEconomyConfig().getLong("revive.mementos-cost", 25);

        if (!plugin.getEconomyManager().has(player, vaultCost) && !player.hasPermission("allycore.bypass.revive")) {
            MessageUtil.send(player, "revive-no-money", Map.of("vault", String.format("%.0f", vaultCost)));
            return;
        }
        if (plugin.getMementoManager().getMementos(player.getUniqueId()) < mementoCost && !player.hasPermission("allycore.bypass.revive")) {
            MessageUtil.send(player, "revive-no-mementos", Map.of("mementos", String.valueOf(mementoCost)));
            return;
        }

        if (!player.hasPermission("allycore.bypass.revive")) {
            plugin.getEconomyManager().withdraw(player, vaultCost);
            plugin.getMementoManager().removeMementos(player.getUniqueId(), mementoCost);
        }

        plugin.getAllyManager().reviveAlly(player);
        MessageUtil.send(player, "revive-success");
        MessageUtil.playSound(player, "ally-revived");
        player.closeInventory();
    }

    private void openRenameAnvil() {
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            AnvilRenameRegistry.setRenaming(player.getUniqueId(), true);
            player.openAnvil(player.getLocation(), true);
        }, 1L);
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
