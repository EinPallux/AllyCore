package com.pallux.allycore.placeholder;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.AllyStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for AllyCore.
 *
 * Available placeholders:
 *   %allycore_has_ally%          - true/false
 *   %allycore_ally_name%         - Display name
 *   %allycore_ally_level%        - Level
 *   %allycore_ally_xp%           - Current XP
 *   %allycore_ally_xp_needed%    - XP needed for next level
 *   %allycore_ally_health%       - Current health
 *   %allycore_ally_max_health%   - Max health
 *   %allycore_ally_attack%       - Attack damage
 *   %allycore_ally_defense%      - Defense
 *   %allycore_ally_speed%        - Speed
 *   %allycore_ally_mode%         - Combat mode
 *   %allycore_ally_alive%        - true/false
 *   %allycore_ally_summoned%     - true/false
 *   %allycore_ally_armor_tier%   - Armor tier
 *   %allycore_ally_weapon_tier%  - Weapon tier
 *   %allycore_mementos%          - Memento balance
 */
public class AllyPlaceholderExpansion extends PlaceholderExpansion {

    private final AllyCore plugin;

    public AllyPlaceholderExpansion(AllyCore plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "allycore"; }
    @Override public @NotNull String getAuthor()     { return "Pallux"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        if (params.equals("mementos")) {
            return String.valueOf(plugin.getMementoManager().getMementos(player.getUniqueId()));
        }

        if (params.equals("has_ally")) {
            return String.valueOf(plugin.getAllyManager().hasAlly(player.getUniqueId()));
        }

        AllyData data = plugin.getAllyManager().getAllyData(player.getUniqueId());
        if (data == null) return "N/A";

        AllyStats stats = new AllyStats(plugin, data);
        AllyEntity ae = plugin.getAllyManager().getActiveAlly(player.getUniqueId());

        return switch (params) {
            case "ally_name"       -> data.getDisplayName();
            case "ally_level"      -> String.valueOf(data.getLevel());
            case "ally_xp"         -> String.format("%.0f", data.getXp());
            case "ally_xp_needed"  -> String.format("%.0f", stats.getXpRequired());
            case "ally_health"     -> ae != null && ae.isSpawned()
                    ? String.format("%.1f", ae.getEntity().getHealth())
                    : String.format("%.1f", data.getCurrentHealth());
            case "ally_max_health" -> String.format("%.1f", stats.getMaxHealth());
            case "ally_attack"     -> String.format("%.1f", stats.getAttackDamage());
            case "ally_defense"    -> String.format("%.1f", stats.getDefense());
            case "ally_speed"      -> String.format("%.3f", stats.getSpeed());
            case "ally_mode"       -> data.getMode().name();
            case "ally_alive"      -> String.valueOf(data.isAlive());
            case "ally_summoned"   -> String.valueOf(data.isSummoned());
            case "ally_armor_tier" -> String.valueOf(data.getArmorTier());
            case "ally_weapon_tier"-> String.valueOf(data.getWeaponTier());
            default                -> "N/A";
        };
    }
}
