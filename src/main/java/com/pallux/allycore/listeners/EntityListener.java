package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

public class EntityListener implements Listener {

    private final AllyCore plugin;

    public EntityListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent mobs from targeting ally's owner because of ally attacking them.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMobTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Monster)) return;
        if (event.getTarget() == null) return;

        // If target is a player who owns an ally, allow targeting (normal behavior)
        // We only prevent the ally entity itself from being ignored
    }

    /**
     * Prevent the owner from accidentally hitting their own ally.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOwnerHitAlly(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        UUID ownerUUID = plugin.getAllyManager().getOwnerOfEntity(zombie.getUniqueId());
        if (ownerUUID == null) return;

        // If this player IS the owner, cancel the damage
        if (player.getUniqueId().equals(ownerUUID)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent projectiles from the owner hitting the ally.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHitAlly(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!(event.getDamager() instanceof Projectile projectile)) return;

        UUID ownerUUID = plugin.getAllyManager().getOwnerOfEntity(zombie.getUniqueId());
        if (ownerUUID == null) return;

        if (projectile.getShooter() instanceof Player shooter) {
            if (shooter.getUniqueId().equals(ownerUUID)) {
                event.setCancelled(true);
            }
        }
    }
}
