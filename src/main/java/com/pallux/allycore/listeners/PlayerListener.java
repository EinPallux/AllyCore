package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final AllyCore plugin;

    public PlayerListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getAllyManager().onPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getAllyManager().onPlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Teleport ally to respawn location after a tick
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            AllyEntity ae = plugin.getAllyManager().getActiveAlly(player.getUniqueId());
            if (ae != null && ae.isSpawned()) {
                ae.getEntity().teleport(player.getLocation());
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Ally will teleport to player on respawn (handled above)
        AllyEntity ae = plugin.getAllyManager().getActiveAlly(player.getUniqueId());
        if (ae != null && ae.getData() != null) {
            ae.cancelForcedAttack();
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Handled by AI teleport logic when ally finds itself in a different world
    }
}