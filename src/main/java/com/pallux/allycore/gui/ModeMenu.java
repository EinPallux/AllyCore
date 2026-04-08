package com.pallux.allycore.gui;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.CombatMode;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.ItemBuilder;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class ModeMenu implements AllyGUI {

    private final AllyCore plugin;
    private final Player player;
    private final AllyEntity allyEntity;
    private final AllyData data;
    private Inventory inventory;

    private static final Map<CombatMode, Material> MODE_MATERIALS = Map.of(
            CombatMode.DEFENSIVE, Material.SHIELD,
            CombatMode.AGGRESSIVE, Material.NETHERITE_SWORD,
            CombatMode.NEUTRAL, Material.WHITE_BANNER,
            CombatMode.ALLROUND, Material.COMPASS
    );

    public ModeMenu(AllyCore plugin, Player player, AllyEntity allyEntity) {
        this.plugin = plugin;
        this.player = player;
        this.allyEntity = allyEntity;
        this.data = allyEntity.getData();
    }

    @Override
    public void open() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("mode-menu.title", "&#F59E0B⚙ Combat Mode ✦");
        inventory = Bukkit.createInventory(null, 27, ColorUtil.component(title));
        fill();
        GUIRegistry.registerOpenGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
    }

    private void fill() {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        FileConfiguration ally = plugin.getConfigManager().getAllyConfig();

        ItemBuilder bg = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, bg.build());

        for (CombatMode mode : CombatMode.values()) {
            int slot = gui.getInt("mode-menu.slots." + mode.name(),
                    switch (mode) { case DEFENSIVE -> 10; case AGGRESSIVE -> 12; case NEUTRAL -> 14; case ALLROUND -> 16; });

            String display = ally.getString("modes." + mode.name() + ".display", mode.name());
            String desc = ally.getString("modes." + mode.name() + ".description", "");
            String color = ally.getString("modes." + mode.name() + ".color", "&#FFFFFF");
            boolean active = data.getMode() == mode;

            ItemBuilder ib = new ItemBuilder(MODE_MATERIALS.getOrDefault(mode, Material.PAPER))
                    .name(color + display)
                    .lore("&#A3A3A3" + desc, "", active ? "&#10B981► Currently Active" : "&#A8A4FFClick to select")
                    .hideAll();
            if (active) ib.glow();
            inventory.setItem(slot, ib.build());
        }

        // Back button
        int backSlot = gui.getInt("mode-menu.back-button.slot", 26);
        inventory.setItem(backSlot, new ItemBuilder(Material.ARROW)
                .name("&#A3A3A3← Back")
                .lore("&#A3A3A3Return to Ally Menu")
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        int slot = event.getSlot();
        int backSlot = gui.getInt("mode-menu.back-button.slot", 26);

        if (slot == backSlot) {
            new MainAllyMenu(plugin, player, allyEntity).open();
            return;
        }

        for (CombatMode mode : CombatMode.values()) {
            int modeSlot = gui.getInt("mode-menu.slots." + mode.name(),
                    switch (mode) { case DEFENSIVE -> 10; case AGGRESSIVE -> 12; case NEUTRAL -> 14; case ALLROUND -> 16; });
            if (slot == modeSlot) {
                data.setMode(mode);
                String display = plugin.getConfigManager().getAllyConfig()
                        .getString("modes." + mode.name() + ".display", mode.name());
                MessageUtil.send(player, "mode-changed", Map.of("mode", display));
                plugin.getAllyManager().saveAlly(player.getUniqueId());
                if (allyEntity.getHologram() != null) allyEntity.getHologram().update();
                fill();
                MessageUtil.playSound(player, "menu-click");
                return;
            }
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
