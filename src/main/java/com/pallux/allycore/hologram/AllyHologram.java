package com.pallux.allycore.hologram;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyStats;
import com.pallux.allycore.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the floating hologram above an Ally entity.
 * Uses invisible ArmorStands as hologram lines.
 */
public class AllyHologram {

    private final AllyCore plugin;
    private final AllyData data;
    private final AllyStats stats;
    private final Zombie attachedEntity;

    private final List<ArmorStand> lines = new ArrayList<>();
    private static final double LINE_HEIGHT = 0.28;
    private static final double BASE_OFFSET = 2.1;

    public AllyHologram(AllyCore plugin, AllyData data, AllyStats stats, Zombie attachedEntity) {
        this.plugin = plugin;
        this.data = data;
        this.stats = stats;
        this.attachedEntity = attachedEntity;
    }

    public void spawn() {
        remove();
        if (attachedEntity == null || !attachedEntity.isValid()) return;

        String nameLine    = formatLine("hologram.name-line");
        String levelLine   = formatLine("hologram.level-line");
        String healthLine  = formatLine("hologram.health-line");
        String statsLine   = formatLine("hologram.stats-line");
        String modeLine    = formatLine("hologram.mode-line");

        String[] lineTexts = { modeLine, statsLine, healthLine, levelLine, nameLine };

        Location base = attachedEntity.getLocation().add(0, BASE_OFFSET, 0);
        World world = base.getWorld();
        if (world == null) return;

        for (int i = 0; i < lineTexts.length; i++) {
            Location lineLoc = base.clone().add(0, i * LINE_HEIGHT, 0);
            ArmorStand stand = (ArmorStand) world.spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            stand.customName(ColorUtil.component(lineTexts[i]));
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            lines.add(stand);
        }
    }

    public void update() {
        if (attachedEntity == null || !attachedEntity.isValid()) return;
        if (lines.isEmpty()) spawn();

        String nameLine    = formatLine("hologram.name-line");
        String levelLine   = formatLine("hologram.level-line");
        String healthLine  = formatLine("hologram.health-line");
        String statsLine   = formatLine("hologram.stats-line");
        String modeLine    = formatLine("hologram.mode-line");

        if (data.isAlive() == false) {
            String deadLine = plugin.getConfigManager().getMessagesConfig().getString("messages.hologram.dead-line", "&#EF4444💀 Fallen");
            updateLine(0, deadLine);
            return;
        }

        String[] lineTexts = { modeLine, statsLine, healthLine, levelLine, nameLine };
        Location base = attachedEntity.getLocation().add(0, BASE_OFFSET, 0);

        for (int i = 0; i < lines.size() && i < lineTexts.length; i++) {
            ArmorStand stand = lines.get(i);
            if (!stand.isValid()) {
                spawn();
                return;
            }
            Location lineLoc = base.clone().add(0, i * LINE_HEIGHT, 0);
            stand.teleport(lineLoc);
            stand.customName(ColorUtil.component(lineTexts[i]));
        }
    }

    private void updateLine(int index, String text) {
        if (index < lines.size() && lines.get(index).isValid()) {
            lines.get(index).customName(ColorUtil.component(text));
        }
    }

    public void remove() {
        for (ArmorStand stand : lines) {
            if (stand.isValid()) stand.remove();
        }
        lines.clear();
    }

    private String formatLine(String configKey) {
        String template = plugin.getConfigManager().getMessagesConfig()
                .getString("messages." + configKey, "");

        double maxHp = stats.getMaxHealth();
        double currentHp = (attachedEntity != null && attachedEntity.isValid())
                ? attachedEntity.getHealth() : maxHp;

        // Mode color
        String modeKey = data.getMode().name();
        String modeColor = plugin.getConfigManager().getAllyConfig()
                .getString("modes." + modeKey + ".color", "&#A3A3A3");
        String modeDisplay = plugin.getConfigManager().getAllyConfig()
                .getString("modes." + modeKey + ".display", modeKey);
        String modeIcon = "⚔";

        return template
                .replace("{name}", data.getDisplayName())
                .replace("{level}", String.valueOf(data.getLevel()))
                .replace("{health}", String.format("%.1f", currentHp))
                .replace("{max_health}", String.format("%.1f", maxHp))
                .replace("{atk}", String.format("%.1f", stats.getAttackDamage()))
                .replace("{def}", String.format("%.1f", stats.getDefense()))
                .replace("{mode}", modeDisplay)
                .replace("{mode_color}", modeColor)
                .replace("{mode_icon}", modeIcon);
    }
}
