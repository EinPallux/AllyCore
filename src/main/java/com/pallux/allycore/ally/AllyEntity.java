package com.pallux.allycore.ally;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.hologram.AllyHologram;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Represents a live, spawned Ally NPC entity.
 * Uses a Zombie entity disguised via ProtocolLib as a Player entity.
 */
public class AllyEntity {

    private final AllyCore plugin;
    private final AllyData data;
    private final AllyStats stats;

    private Zombie entity;               // Underlying entity
    private Player owner;
    private AllyHologram hologram;

    private LivingEntity target;         // Current combat target
    private boolean forceAttack = false; // Player-commanded attack
    private long lastAttackTime = 0;
    private long lastWalkXPTime = 0;
    private Location lastWalkLocation;

    private BukkitTask aiTask;
    private BukkitTask regenTask;

    public AllyEntity(AllyCore plugin, AllyData data, Player owner) {
        this.plugin = plugin;
        this.data = data;
        this.stats = new AllyStats(plugin, data);
        this.owner = owner;
    }

    // ─── Spawn / Despawn ─────────────────────────────────────────────────────

    public void spawn(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        entity = world.spawn(location, Zombie.class, z -> {
            z.setAI(true); // Enable AI for proper pathfinding physics
            z.setAware(true);
            z.setSilent(false);
            z.setBaby(false);
            z.setRemoveWhenFarAway(false);
            z.setPersistent(true);
            z.setCustomNameVisible(false); // Hologram handles name
            z.setCollidable(true);
            z.setInvulnerable(false);
            z.setCanPickupItems(false);

            // Strip default vanilla zombie AI goals (so it doesn't randomly wander or attack villagers)
            Bukkit.getMobGoals().removeAllGoals(z);

            // Apply attributes
            applyAttributes(z);

            // Apply equipment
            applyEquipment(z);

            // Tag entity for identification
            z.getPersistentDataContainer().set(
                    plugin.getSkinManager().getAllyKey(),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    data.getOwnerUUID().toString()
            );
        });

        // Apply skin via ProtocolLib / NMS
        if (plugin.isProtocolLibEnabled()) {
            plugin.getSkinManager().applySkin(entity, owner);
        }

        // Spawn hologram
        hologram = new AllyHologram(plugin, data, stats, entity);
        hologram.spawn();

        // Start AI loop
        startAI();
        startRegen();

        lastWalkLocation = location.clone();
    }

    public void despawn() {
        stopAI();
        if (hologram != null) {
            hologram.remove();
            hologram = null;
        }
        if (entity != null && entity.isValid()) {
            entity.remove();
            entity = null;
        }
    }

    // Safely fetch attribute from Registry, supporting both 1.21.1 (generic. prefix) and 1.21.2+ (no prefix)
    private Attribute getAttributeSafe(String key) {
        Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (attr == null && key.startsWith("generic.")) {
            attr = org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key.substring(8)));
        }
        return attr;
    }

    private void applyAttributes(Zombie z) {
        setAttr(z, "generic.max_health", stats.getMaxHealth());
        z.setHealth(data.getCurrentHealth() <= 0 ? stats.getMaxHealth() : Math.min(data.getCurrentHealth(), stats.getMaxHealth()));
        setAttr(z, "generic.movement_speed", stats.getSpeed());
        setAttr(z, "generic.attack_damage", stats.getAttackDamage());
        setAttr(z, "generic.knockback_resistance", stats.getKnockbackResistance());
        setAttr(z, "generic.armor", stats.getDefense());
    }

    private void setAttr(LivingEntity e, String attrKey, double value) {
        Attribute attr = getAttributeSafe(attrKey);
        if (attr != null) {
            AttributeInstance inst = e.getAttribute(attr);
            if (inst != null) {
                inst.setBaseValue(value);
            }
        }
    }

    private void applyEquipment(Zombie z) {
        EntityEquipment eq = z.getEquipment();
        if (eq == null) return;

        eq.setHelmet(getArmorPiece(ArmorSlot.HELMET), false);
        eq.setChestplate(getArmorPiece(ArmorSlot.CHESTPLATE), false);
        eq.setLeggings(getArmorPiece(ArmorSlot.LEGGINGS), false);
        eq.setBoots(getArmorPiece(ArmorSlot.BOOTS), false);
        eq.setItemInMainHand(getWeaponItem(), false);

        // Prevent drops
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    private enum ArmorSlot { HELMET, CHESTPLATE, LEGGINGS, BOOTS }

    private ItemStack getArmorPiece(ArmorSlot slot) {
        Material mat = switch (data.getArmorTier()) {
            case 1 -> switch (slot) {
                case HELMET -> Material.LEATHER_HELMET;
                case CHESTPLATE -> Material.LEATHER_CHESTPLATE;
                case LEGGINGS -> Material.LEATHER_LEGGINGS;
                case BOOTS -> Material.LEATHER_BOOTS;
            };
            case 2 -> switch (slot) {
                case HELMET -> Material.IRON_HELMET;
                case CHESTPLATE -> Material.IRON_CHESTPLATE;
                case LEGGINGS -> Material.IRON_LEGGINGS;
                case BOOTS -> Material.IRON_BOOTS;
            };
            case 3 -> switch (slot) {
                case HELMET -> Material.GOLDEN_HELMET;
                case CHESTPLATE -> Material.GOLDEN_CHESTPLATE;
                case LEGGINGS -> Material.GOLDEN_LEGGINGS;
                case BOOTS -> Material.GOLDEN_BOOTS;
            };
            case 4 -> switch (slot) {
                case HELMET -> Material.DIAMOND_HELMET;
                case CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
                case LEGGINGS -> Material.DIAMOND_LEGGINGS;
                case BOOTS -> Material.DIAMOND_BOOTS;
            };
            case 5 -> switch (slot) {
                case HELMET -> Material.NETHERITE_HELMET;
                case CHESTPLATE -> Material.NETHERITE_CHESTPLATE;
                case LEGGINGS -> Material.NETHERITE_LEGGINGS;
                case BOOTS -> Material.NETHERITE_BOOTS;
            };
            default -> null;
        };
        return mat != null ? new ItemStack(mat) : new ItemStack(Material.AIR);
    }

    private ItemStack getWeaponItem() {
        Material mat = switch (data.getWeaponTier()) {
            case 1 -> Material.WOODEN_SWORD;
            case 2 -> Material.STONE_SWORD;
            case 3 -> Material.IRON_SWORD;
            case 4 -> Material.GOLDEN_SWORD;
            case 5 -> Material.DIAMOND_SWORD;
            case 6 -> Material.NETHERITE_SWORD;
            default -> null;
        };
        return mat != null ? new ItemStack(mat) : new ItemStack(Material.AIR);
    }

    // ─── AI Loop ─────────────────────────────────────────────────────────────

    private void startAI() {
        int tickRate = plugin.getConfigManager().getAllyConfig().getInt("ally.ai-tick-rate", 2);
        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    cancel();
                    return;
                }
                tickAI();
            }
        }.runTaskTimer(plugin, tickRate, tickRate);
    }

    private void startRegen() {
        regenTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid()) { cancel(); return; }
                double regen = stats.getRegenPerSecond();
                if (regen > 0 && entity.getHealth() < stats.getMaxHealth()) {
                    double newHp = Math.min(entity.getHealth() + regen, stats.getMaxHealth());
                    entity.setHealth(newHp);
                    if (hologram != null) hologram.update();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void stopAI() {
        if (aiTask != null) { aiTask.cancel(); aiTask = null; }
        if (regenTask != null) { regenTask.cancel(); regenTask = null; }
    }

    private void tickAI() {
        if (owner == null || !owner.isOnline()) return;

        // Prevent zombie from burning in the sun
        if (entity.getFireTicks() > 0 &&
                entity.getLocation().getBlock().getType() != Material.LAVA &&
                entity.getLocation().getBlock().getType() != Material.FIRE) {
            entity.setFireTicks(0);
        }

        double followDist = plugin.getConfigManager().getAllyConfig().getDouble("ally.follow.follow-distance", 5.0);
        double teleportDist = plugin.getConfigManager().getAllyConfig().getDouble("ally.follow.teleport-distance", 40.0);
        double attackRange = plugin.getConfigManager().getAllyConfig().getDouble("ally.combat.attack-range", 2.5);
        double maxCombatDist = plugin.getConfigManager().getAllyConfig().getDouble("ally.combat.max-combat-distance", 30.0);

        // Handle target acquisition
        if (target == null || !target.isValid() || target.isDead()) {
            target = null;
            forceAttack = false;
            if (data.getMode() != CombatMode.NEUTRAL) {
                target = findTarget();
            }
        }

        // If target is too far from owner, disengage
        if (target != null && target.getLocation().distance(owner.getLocation()) > maxCombatDist) {
            target = null;
            forceAttack = false;
        }

        boolean hasCombatTarget = target != null && target.isValid() && !target.isDead();

        if (hasCombatTarget) {
            // Move towards target
            double distToTarget = entity.getLocation().distance(target.getLocation());
            if (distToTarget > attackRange) {
                navigateTo(target.getLocation(), 1.2); // 1.2 = Sprinting multiplier
            } else {
                // Stop pathfinding and attack!
                entity.getPathfinder().stopPathfinding();
                entity.lookAt(target);
                performAttack();
            }
        } else {
            // Follow owner
            if (!data.isFollowing()) {
                entity.getPathfinder().stopPathfinding();
                return;
            }

            // Different world check
            if (!entity.getWorld().equals(owner.getWorld())) {
                teleportToOwner();
                return;
            }

            double distToOwner = entity.getLocation().distance(owner.getLocation());

            if (distToOwner > teleportDist) {
                teleportToOwner();
            } else if (distToOwner > followDist) {
                double speedMult = 1.0;
                if (distToOwner > followDist * 2) {
                    speedMult = plugin.getConfigManager().getAllyConfig().getDouble("ally.follow.sprint-multiplier", 1.4);
                }
                navigateTo(owner.getLocation(), speedMult);
            } else {
                // Within follow distance, idle and look at owner
                entity.getPathfinder().stopPathfinding();
                entity.lookAt(owner);
            }
        }

        // XP for walking
        tickWalkXP();

        // Update hologram
        if (hologram != null) hologram.update();
    }

    /**
     * Utilizes Paper's native Pathfinder to calculate blocks and jumps smoothly.
     */
    private void navigateTo(Location target, double speedMultiplier) {
        if (entity == null || !entity.isValid()) return;
        entity.getPathfinder().moveTo(target, speedMultiplier);
    }

    private void teleportToOwner() {
        Location safe = findSafeLocationNear(owner.getLocation());
        entity.teleport(safe);
        owner.getWorld().playEffect(safe, Effect.ENDER_SIGNAL, 0);
    }

    private Location findSafeLocationNear(Location center) {
        for (int r = 1; r <= 3; r++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                Location test = center.clone().add(Math.cos(rad) * r, 0, Math.sin(rad) * r);
                test.setY(center.getY());
                if (test.getBlock().isPassable() && test.clone().add(0, 1, 0).getBlock().isPassable()) {
                    return test;
                }
            }
        }
        return center;
    }

    private LivingEntity findTarget() {
        double detectRange = plugin.getConfigManager().getAllyConfig().getDouble("ally.combat.auto-detect-range", 12.0);
        CombatMode mode = data.getMode();

        List<Entity> nearby = entity.getNearbyEntities(detectRange, detectRange, detectRange);
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : nearby) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.getUniqueId().equals(entity.getUniqueId())) continue;
            if (le.getUniqueId().equals(owner.getUniqueId())) continue;

            boolean shouldTarget = false;
            if (e instanceof Monster) {
                shouldTarget = mode == CombatMode.AGGRESSIVE || mode == CombatMode.ALLROUND;
            } else if (e instanceof Player p && !p.getUniqueId().equals(owner.getUniqueId())) {
                shouldTarget = false;
            }

            if (!shouldTarget) continue;

            double dist = entity.getLocation().distance(le.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                closest = le;
            }
        }
        return closest;
    }

    private void performAttack() {
        if (target == null || !target.isValid() || target.isDead()) return;

        long now = System.currentTimeMillis();
        int cooldownTicks = plugin.getConfigManager().getAllyConfig().getInt("ally.combat.attack-cooldown-ticks", 20);
        long cooldownMs = cooldownTicks * 50L;

        if (now - lastAttackTime < cooldownMs) return;
        lastAttackTime = now;

        double damage = stats.getAttackDamage();
        target.damage(damage, entity);

        double kbForce = plugin.getConfigManager().getAllyConfig().getDouble("ally.combat.knockback-force", 0.4);
        org.bukkit.util.Vector kb = target.getLocation().subtract(entity.getLocation()).toVector().normalize().multiply(kbForce);
        kb.setY(0.2);
        target.setVelocity(target.getVelocity().add(kb));

        entity.swingMainHand();
    }

    private void tickWalkXP() {
        if (lastWalkLocation == null) { lastWalkLocation = entity.getLocation().clone(); return; }
        double dist = entity.getLocation().distance(lastWalkLocation);
        if (dist > 0.5) {
            double xpPerBlock = plugin.getConfigManager().getAllyConfig().getDouble("ally.xp.walk-xp-per-block", 0.05);
            addXP(dist * xpPerBlock);
            lastWalkLocation = entity.getLocation().clone();
        }
    }

    // ─── XP & Level Up ───────────────────────────────────────────────────────

    public void addXP(double amount) {
        if (data.getLevel() >= stats.getMaxLevel()) return;
        data.setXp(data.getXp() + amount);

        while (data.getXp() >= stats.getXpRequired() && data.getLevel() < stats.getMaxLevel()) {
            data.setXp(data.getXp() - stats.getXpRequired());
            levelUp();
        }
    }

    private void levelUp() {
        data.setLevel(data.getLevel() + 1);
        refreshAttributes();

        // Heal on level up
        if (entity != null && entity.isValid()) {
            entity.setHealth(stats.getMaxHealth());
        }

        if (owner != null && owner.isOnline()) {
            String lvlMsg = plugin.getConfigManager().getMessage("ally-levelup")
                    .replace("{ally_name}", data.getDisplayName())
                    .replace("{level}", String.valueOf(data.getLevel()));
            MessageUtil.sendTitle(owner,
                    plugin.getConfigManager().getMessage("ally-levelup-title"),
                    plugin.getConfigManager().getMessage("ally-levelup-subtitle").replace("{level}", String.valueOf(data.getLevel())),
                    10, 40, 10);
            MessageUtil.sendActionBar(owner, lvlMsg);
            MessageUtil.playSound(owner, "ally-levelup");

            // Level up particles
            if (entity != null) {
                entity.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, entity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
            }
        }

        if (hologram != null) hologram.update();
    }

    public void refreshAttributes() {
        if (entity == null || !entity.isValid()) return;

        double prevMaxHp = 20.0;
        Attribute attrMaxHealth = getAttributeSafe("generic.max_health");
        if (attrMaxHealth != null) {
            AttributeInstance inst = entity.getAttribute(attrMaxHealth);
            if (inst != null) {
                prevMaxHp = inst.getBaseValue();
            }
        }

        double hpPercent = entity.getHealth() / prevMaxHp;

        applyAttributes(entity);
        entity.setHealth(Math.min(stats.getMaxHealth() * hpPercent, stats.getMaxHealth()));
        applyEquipment(entity);
    }

    // ─── Combat events called from listeners ─────────────────────────────────

    public void onOwnerAttack(LivingEntity attacked) {
        if (data.getMode() == CombatMode.NEUTRAL) return;
        if (attacked instanceof Player p && p.getUniqueId().equals(owner.getUniqueId())) return;
        this.target = attacked;
    }

    public void onOwnerOrAllyAttacked(LivingEntity attacker) {
        if (data.getMode() == CombatMode.NEUTRAL) return;
        if (attacker instanceof Player p && !plugin.getConfigManager().getAllyConfig()
                .getBoolean("ally.combat.defend-from-players", true)) return;
        if (this.target == null || !this.target.isValid()) {
            this.target = attacker;
        }
    }

    public void forceAttack(LivingEntity target) {
        this.target = target;
        this.forceAttack = true;
    }

    public void cancelForcedAttack() {
        if (forceAttack) {
            this.target = null;
            this.forceAttack = false;
        }
    }

    // ─── Health & Death ──────────────────────────────────────────────────────

    public void onDeath() {
        data.setAlive(false);
        data.setSummoned(false);
        data.setCurrentHealth(-1);

        if (owner != null && owner.isOnline()) {
            MessageUtil.send(owner, "ally-died");
            MessageUtil.playSound(owner, "ally-died");
        }

        despawn();
        plugin.getAllyManager().saveAlly(data.getOwnerUUID());
    }

    public void syncHealthToData() {
        if (entity != null && entity.isValid()) {
            data.setCurrentHealth(entity.getHealth());
        }
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public AllyData getData() { return data; }
    public AllyStats getStats() { return stats; }
    public Zombie getEntity() { return entity; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }
    public LivingEntity getTarget() { return target; }
    public AllyHologram getHologram() { return hologram; }
    public boolean isSpawned() { return entity != null && entity.isValid(); }
}