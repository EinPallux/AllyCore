package com.pallux.allycore.ally;

import java.util.UUID;

/**
 * Persistent data model for an Ally owned by a player.
 */
public class AllyData {

    private UUID ownerUUID;
    private String ownerName;

    private String customName;          // Player-set name
    private int level;
    private double xp;

    // Stat tree upgrade tiers
    private int healthTier;
    private int attackTier;
    private int defenseTier;
    private int speedTier;
    private int regenerationTier;

    // Equipment tiers
    private int armorTier;
    private int weaponTier;

    // Current state
    private boolean alive;
    private boolean summoned;
    private boolean following;
    private CombatMode mode;

    // Current health (persisted)
    private double currentHealth;

    public AllyData(UUID ownerUUID, String ownerName) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.customName = null;
        this.level = 1;
        this.xp = 0;
        this.healthTier = 0;
        this.attackTier = 0;
        this.defenseTier = 0;
        this.speedTier = 0;
        this.regenerationTier = 0;
        this.armorTier = 0;
        this.weaponTier = 0;
        this.alive = true;
        this.summoned = false;
        this.following = true;
        this.mode = CombatMode.DEFENSIVE;
        this.currentHealth = -1; // -1 = full health
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String name) { this.ownerName = name; }

    public String getCustomName() { return customName; }
    public void setCustomName(String name) { this.customName = name; }

    public String getDisplayName() {
        return customName != null ? customName : "Ally";
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = xp; }

    public int getHealthTier() { return healthTier; }
    public void setHealthTier(int t) { this.healthTier = t; }

    public int getAttackTier() { return attackTier; }
    public void setAttackTier(int t) { this.attackTier = t; }

    public int getDefenseTier() { return defenseTier; }
    public void setDefenseTier(int t) { this.defenseTier = t; }

    public int getSpeedTier() { return speedTier; }
    public void setSpeedTier(int t) { this.speedTier = t; }

    public int getRegenerationTier() { return regenerationTier; }
    public void setRegenerationTier(int t) { this.regenerationTier = t; }

    public int getArmorTier() { return armorTier; }
    public void setArmorTier(int t) { this.armorTier = t; }

    public int getWeaponTier() { return weaponTier; }
    public void setWeaponTier(int t) { this.weaponTier = t; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isSummoned() { return summoned; }
    public void setSummoned(boolean summoned) { this.summoned = summoned; }

    public boolean isFollowing() { return following; }
    public void setFollowing(boolean following) { this.following = following; }

    public CombatMode getMode() { return mode; }
    public void setMode(CombatMode mode) { this.mode = mode; }

    public double getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(double h) { this.currentHealth = h; }
}
