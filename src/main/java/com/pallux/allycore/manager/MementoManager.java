package com.pallux.allycore.manager;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MementoManager {

    private final AllyCore plugin;
    // Backed by storage; cached in memory
    private final Map<UUID, Long> cache = new HashMap<>();

    public MementoManager(AllyCore plugin) {
        this.plugin = plugin;
    }

    public long getMementos(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        long stored = plugin.getStorageManager().getMementos(uuid);
        cache.put(uuid, stored);
        return stored;
    }

    public void addMementos(UUID uuid, long amount) {
        long current = getMementos(uuid);
        long newVal = current + amount;
        cache.put(uuid, newVal);
        plugin.getStorageManager().setMementos(uuid, newVal);
    }

    public boolean removeMementos(UUID uuid, long amount) {
        long current = getMementos(uuid);
        if (current < amount) return false;
        long newVal = current - amount;
        cache.put(uuid, newVal);
        plugin.getStorageManager().setMementos(uuid, newVal);
        return true;
    }

    public void setMementos(UUID uuid, long amount) {
        cache.put(uuid, amount);
        plugin.getStorageManager().setMementos(uuid, amount);
    }

    /**
     * Award mementos to a player and notify them.
     */
    public void award(Player player, long amount) {
        addMementos(player.getUniqueId(), amount);

        boolean useAB = plugin.getConfigManager().getMessagesConfig()
                .getBoolean("messages.use-actionbar", true);
        String msg = plugin.getConfigManager().getMessage("mementos-earned")
                .replace("{amount}", String.valueOf(amount));
        if (useAB) {
            MessageUtil.sendActionBar(player, msg);
        } else {
            player.sendMessage(msg);
        }
        MessageUtil.playSound(player, "mementos-earned");
    }

    /**
     * Compute mementos earned for a kill, factoring in ally level.
     */
    public long computeKillReward(int allyLevel, boolean isBoss, boolean isPlayer) {
        long base;
        if (isPlayer) {
            base = plugin.getConfigManager().getEconomyConfig().getLong("mementos.earn.per-player-kill", 10);
        } else if (isBoss) {
            base = plugin.getConfigManager().getEconomyConfig().getLong("mementos.earn.per-mob-kill", 2)
                    + plugin.getConfigManager().getEconomyConfig().getLong("mementos.earn.boss-kill-bonus", 25);
        } else {
            base = plugin.getConfigManager().getEconomyConfig().getLong("mementos.earn.per-mob-kill", 2);
        }
        double mult = 1.0 + allyLevel * plugin.getConfigManager().getEconomyConfig()
                .getDouble("mementos.earn.level-multiplier", 0.02);
        return Math.max(1, Math.round(base * mult));
    }

    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
    }
}
