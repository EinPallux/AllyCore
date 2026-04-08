package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyEntity;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class AllyDamageListener implements Listener {

    private final AllyCore plugin;

    public AllyDamageListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercept damage to the Ally entity.
     * Apply defense stat reduction, then check for death.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAllyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        UUID ownerUUID = plugin.getAllyManager().getOwnerOfEntity(zombie.getUniqueId());
        if (ownerUUID == null) return;

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(ownerUUID);
        if (ae == null) return;

        // Apply defense: reduce damage by defense / (defense + 10)
        double defense = ae.getStats().getDefense();
        double reductionFactor = defense / (defense + 10.0);
        double reducedDamage = event.getFinalDamage() * (1.0 - reductionFactor);
        event.setDamage(reducedDamage);

        // Defend the ally — notify it was attacked
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent ede) {
            if (ede.getDamager() instanceof LivingEntity attacker) {
                ae.onOwnerOrAllyAttacked(attacker);
            }
        }

        // Check if this will kill the ally
        double resultHp = zombie.getHealth() - event.getFinalDamage();
        if (resultHp <= 0.5) {
            event.setCancelled(true);
            ae.onDeath();
        }
    }

    /**
     * Prevent the ally entity's death event from firing vanilla loot/XP.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAllyDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!plugin.getAllyManager().isAllyEntity(zombie.getUniqueId())) return;

        // Suppress drops and XP
        boolean dropItems = plugin.getConfigManager().getAllyConfig()
                .getBoolean("ally.death.drop-items-on-death", false);
        if (!dropItems) event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
