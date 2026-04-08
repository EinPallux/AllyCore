package com.pallux.allycore.manager;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.CombatMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all Ally instances.
 * Maps owner UUID -> AllyEntity (if spawned) and owner UUID -> AllyData.
 */
public class AllyManager {

    private final AllyCore plugin;

    // Persisted data for ALL players who own an ally
    private final Map<UUID, AllyData> allyDataMap = new ConcurrentHashMap<>();

    // Live entities (only for currently spawned / online allies)
    private final Map<UUID, AllyEntity> activeAllies = new ConcurrentHashMap<>();

    // Map entity UUID -> owner UUID for quick reverse lookup
    private final Map<UUID, UUID> entityOwnerMap = new ConcurrentHashMap<>();

    public AllyManager(AllyCore plugin) {
        this.plugin = plugin;
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────

    public void loadAllAllies() {
        Map<UUID, AllyData> loaded = plugin.getStorageManager().loadAllAllyData();
        allyDataMap.putAll(loaded);

        // Spawn allies for online players
        for (Map.Entry<UUID, AllyData> entry : allyDataMap.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && entry.getValue().isSummoned() && entry.getValue().isAlive()) {
                spawnAlly(player);
            }
        }
    }

    public void saveAllAllies() {
        // Sync live health before saving
        for (AllyEntity ae : activeAllies.values()) {
            ae.syncHealthToData();
        }
        for (AllyData data : allyDataMap.values()) {
            plugin.getStorageManager().saveAllyData(data);
        }
    }

    public void saveAlly(UUID ownerUUID) {
        AllyData data = allyDataMap.get(ownerUUID);
        if (data == null) return;
        AllyEntity ae = activeAllies.get(ownerUUID);
        if (ae != null) ae.syncHealthToData();
        plugin.getStorageManager().saveAllyData(data);
    }

    public void despawnAllAllies() {
        for (AllyEntity ae : new ArrayList<>(activeAllies.values())) {
            ae.syncHealthToData();
            ae.despawn();
        }
        activeAllies.clear();
        entityOwnerMap.clear();
    }

    // ─── Ally Creation / Deletion ─────────────────────────────────────────────

    public boolean hasAlly(UUID uuid) {
        return allyDataMap.containsKey(uuid);
    }

    public AllyData createAlly(Player player) {
        if (hasAlly(player.getUniqueId())) return allyDataMap.get(player.getUniqueId());
        AllyData data = new AllyData(player.getUniqueId(), player.getName());
        allyDataMap.put(player.getUniqueId(), data);
        plugin.getStorageManager().saveAllyData(data);
        return data;
    }

    public void removeAlly(UUID ownerUUID) {
        despawnAlly(ownerUUID);
        allyDataMap.remove(ownerUUID);
        plugin.getStorageManager().deleteAllyData(ownerUUID);
    }

    // ─── Spawn / Despawn ─────────────────────────────────────────────────────

    public boolean spawnAlly(Player owner) {
        UUID uuid = owner.getUniqueId();
        AllyData data = allyDataMap.get(uuid);
        if (data == null || !data.isAlive()) return false;
        if (activeAllies.containsKey(uuid)) return false; // already spawned

        Location spawnLoc = findSafeLocationNear(owner.getLocation());
        AllyEntity ae = new AllyEntity(plugin, data, owner);
        ae.spawn(spawnLoc);

        if (ae.isSpawned()) {
            activeAllies.put(uuid, ae);
            entityOwnerMap.put(ae.getEntity().getUniqueId(), uuid);
            data.setSummoned(true);
            return true;
        }
        return false;
    }

    public void despawnAlly(UUID ownerUUID) {
        AllyEntity ae = activeAllies.remove(ownerUUID);
        if (ae != null) {
            ae.syncHealthToData();
            if (ae.getEntity() != null) entityOwnerMap.remove(ae.getEntity().getUniqueId());
            ae.despawn();
            AllyData data = allyDataMap.get(ownerUUID);
            if (data != null) data.setSummoned(false);
        }
    }

    public void teleportAllyToPlayer(Player player) {
        AllyEntity ae = activeAllies.get(player.getUniqueId());
        if (ae != null && ae.isSpawned()) {
            ae.getEntity().teleport(findSafeLocationNear(player.getLocation()));
        }
    }

    // ─── Player online/offline ────────────────────────────────────────────────

    public void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        AllyData data = allyDataMap.get(uuid);
        if (data == null) return;
        data.setOwnerName(player.getName());
        // Spawn ally if it was summoned before
        if (data.isSummoned() && data.isAlive()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnAlly(player), 20L);
        }
    }

    public void onPlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        AllyEntity ae = activeAllies.get(uuid);
        if (ae != null) {
            ae.syncHealthToData();
            AllyData data = allyDataMap.get(uuid);
            // Keep summoned=true so it respawns on rejoin
            ae.despawn();
            activeAllies.remove(uuid);
            if (ae.getEntity() != null) entityOwnerMap.remove(ae.getEntity().getUniqueId());
        }
        saveAlly(uuid);
    }

    // ─── Revive ───────────────────────────────────────────────────────────────

    public boolean reviveAlly(Player owner) {
        UUID uuid = owner.getUniqueId();
        AllyData data = allyDataMap.get(uuid);
        if (data == null || data.isAlive()) return false;
        data.setAlive(true);
        data.setCurrentHealth(-1);
        data.setSummoned(true);
        spawnAlly(owner);
        saveAlly(uuid);
        return true;
    }

    // ─── Lookups ─────────────────────────────────────────────────────────────

    public AllyData getAllyData(UUID ownerUUID) {
        return allyDataMap.get(ownerUUID);
    }

    public AllyEntity getActiveAlly(UUID ownerUUID) {
        return activeAllies.get(ownerUUID);
    }

    /**
     * Get the owner of a live ally entity by its entity UUID.
     */
    public UUID getOwnerOfEntity(UUID entityUUID) {
        return entityOwnerMap.get(entityUUID);
    }

    public boolean isAllyEntity(UUID entityUUID) {
        return entityOwnerMap.containsKey(entityUUID);
    }

    public Map<UUID, AllyData> getAllAllyData() {
        return Collections.unmodifiableMap(allyDataMap);
    }

    public Map<UUID, AllyEntity> getActiveAllies() {
        return Collections.unmodifiableMap(activeAllies);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Location findSafeLocationNear(Location center) {
        for (int r = 1; r <= 3; r++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                Location test = center.clone().add(Math.cos(rad) * r, 0, Math.sin(rad) * r);
                if (test.getBlock().getType().isAir()
                        && test.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    return test;
                }
            }
        }
        return center.clone();
    }
}
