package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.AllyStats;
import com.pallux.allycore.ally.CombatMode;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class CombatListener implements Listener {

    private final AllyCore plugin;

    public CombatListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    /**
     * When the owner attacks something → notify ally.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (target.isDead()) return;

        // Don't react if target is our own ally
        if (plugin.getAllyManager().isAllyEntity(target.getUniqueId())) return;

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(attacker.getUniqueId());
        if (ae == null) return;

        ae.onOwnerAttack(target);
    }

    /**
     * When a mob attacks the owner → notify ally (defend mode).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        // Don't react to own ally hitting the player (shouldn't happen but safety)
        if (plugin.getAllyManager().isAllyEntity(attacker.getUniqueId())) return;

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(victim.getUniqueId());
        if (ae == null) return;

        ae.onOwnerOrAllyAttacked(attacker);
    }

    /**
     * When the ally kills a mob → grant XP and mementos.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.getLastDamageCause() == null) return;

        Entity killer = null;
        if (dead.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent ede) {
            killer = ede.getDamager();
        }

        // Check if killer is an Ally entity
        if (killer == null) return;
        UUID killerOwnerUUID = plugin.getAllyManager().getOwnerOfEntity(killer.getUniqueId());
        if (killerOwnerUUID == null) return;

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(killerOwnerUUID);
        if (ae == null) return;

        AllyData data = ae.getData();
        AllyStats stats = ae.getStats();

        // Determine XP based on entity type
        double xpGain = getXpForEntity(dead);
        ae.addXP(xpGain);

        // Cancel vanilla drops from ally-kills (optional, keep them)
        // Award mementos
        boolean isBoss = dead instanceof Boss;
        boolean isPlayerKill = dead instanceof Player;
        long mementos = plugin.getMementoManager().computeKillReward(data.getLevel(), isBoss, isPlayerKill);

        org.bukkit.entity.Player owner = ae.getOwner();
        if (owner != null && owner.isOnline()) {
            plugin.getMementoManager().award(owner, mementos);
        }

        // Clear target if this was it
        if (ae.getTarget() != null && ae.getTarget().getUniqueId().equals(dead.getUniqueId())) {
            // AllyEntity AI loop will clear it next tick
        }
    }

    private double getXpForEntity(LivingEntity e) {
        var cfg = plugin.getConfigManager().getAllyConfig();
        if (e instanceof Boss || e instanceof EnderDragon || e instanceof WitherSkeleton) {
            return cfg.getDouble("ally.xp.kill-xp.boss", 200);
        }
        if (e instanceof Witch || e instanceof Guardian || e instanceof ElderGuardian) {
            return cfg.getDouble("ally.xp.kill-xp.hard", 40);
        }
        if (e instanceof Creeper || e instanceof Spider || e instanceof CaveSpider) {
            return cfg.getDouble("ally.xp.kill-xp.medium", 15);
        }
        return cfg.getDouble("ally.xp.kill-xp.easy", 5);
    }
}
