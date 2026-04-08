package com.pallux.allycore.ally;

import com.pallux.allycore.AllyCore;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Computes the effective stats of an Ally from its data and configuration.
 */
public class AllyStats {

    private final AllyCore plugin;
    private final AllyData data;

    public AllyStats(AllyCore plugin, AllyData data) {
        this.plugin = plugin;
        this.data = data;
    }

    private FileConfiguration ally() {
        return plugin.getConfigManager().getAllyConfig();
    }

    private FileConfiguration upgrades() {
        return plugin.getConfigManager().getUpgradesConfig();
    }

    public double getMaxHealth() {
        double base = ally().getDouble("ally.base-stats.max-health", 20.0);
        double levelBonus = (data.getLevel() - 1) * ally().getDouble("ally.level-up-bonus.max-health", 0.5);
        double tierBonus = data.getHealthTier() * upgrades().getDouble("stat-tree.health.bonus-per-tier", 5.0);
        return base + levelBonus + tierBonus;
    }

    public double getAttackDamage() {
        double base = ally().getDouble("ally.base-stats.attack-damage", 4.0);
        double levelBonus = (data.getLevel() - 1) * ally().getDouble("ally.level-up-bonus.attack-damage", 0.1);
        double tierBonus = data.getAttackTier() * upgrades().getDouble("stat-tree.attack.bonus-per-tier", 1.0);

        // Weapon bonus
        int weaponTier = data.getWeaponTier();
        double weaponBonus = weaponTier * 1.5; // Each weapon tier adds 1.5 base attack

        return base + levelBonus + tierBonus + weaponBonus;
    }

    public double getDefense() {
        double base = ally().getDouble("ally.base-stats.defense", 2.0);
        double tierBonus = data.getDefenseTier() * upgrades().getDouble("stat-tree.defense.bonus-per-tier", 1.0);

        // Armor bonus
        int armorTier = data.getArmorTier();
        double armorBonus = switch (armorTier) {
            case 1 -> 3.0;  // Leather
            case 2 -> 7.0;  // Iron
            case 3 -> 5.0;  // Gold
            case 4 -> 12.0; // Diamond
            case 5 -> 15.0; // Netherite
            default -> 0.0;
        };

        return base + tierBonus + armorBonus;
    }

    public double getSpeed() {
        double base = ally().getDouble("ally.base-stats.speed", 0.28);
        double tierBonus = data.getSpeedTier() * upgrades().getDouble("stat-tree.speed.bonus-per-tier", 0.02);
        return base + tierBonus;
    }

    public double getRegenPerSecond() {
        return data.getRegenerationTier() * upgrades().getDouble("stat-tree.regeneration.bonus-per-tier", 0.5);
    }

    public double getKnockbackResistance() {
        return ally().getDouble("ally.base-stats.knockback-resistance", 0.1);
    }

    /**
     * XP needed to reach the next level.
     */
    public double getXpRequired() {
        double base = ally().getDouble("ally.xp.base", 100);
        double exp = ally().getDouble("ally.xp.exponent", 1.8);
        return base * Math.pow(data.getLevel(), exp);
    }

    public int getMaxLevel() {
        return ally().getInt("ally.max-level", 100);
    }
}
