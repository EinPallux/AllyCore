package com.pallux.allycore.commands;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.CombatMode;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class AllyAdminCommand implements CommandExecutor, TabCompleter {

    private final AllyCore plugin;

    public AllyAdminCommand(AllyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("allycore.admin")) {
            sender.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give"    -> handleGive(sender, args);
            case "remove"  -> handleRemove(sender, args);
            case "reload"  -> handleReload(sender);
            case "setstat" -> handleSetStat(sender, args);
            case "setlevel"-> handleSetLevel(sender, args);
            case "revive"  -> handleRevive(sender, args);
            case "list"    -> handleList(sender);
            case "mementos"-> handleMementos(sender, args);
            case "info"    -> handleInfo(sender, args);
            default        -> sendAdminHelp(sender);
        }
        return true;
    }

    private void sendAdminHelp(CommandSender s) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        s.sendMessage(ColorUtil.translate(prefix + "&#6C63FF═══ AllyCore Admin Commands ═══"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin give <player> &#A3A3A3- Give a player an Ally"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin remove <player> &#A3A3A3- Remove a player's Ally"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin reload &#A3A3A3- Reload all configs"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin setstat <player> <stat> <value> &#A3A3A3- Set a stat tier"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin setlevel <player> <level> &#A3A3A3- Set Ally level"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin revive <player> &#A3A3A3- Revive a player's dead Ally"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin list &#A3A3A3- List all Allies"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin mementos <player> <set|add|remove> <amount> &#A3A3A3- Manage Mementos"));
        s.sendMessage(ColorUtil.translate("&#A8A4FF/allyadmin info <player> &#A3A3A3- View a player's Ally info"));
    }

    private void handleGive(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin give <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { MessageUtil.send(s instanceof Player p ? p : null, "player-not-found", Map.of("player", args[1])); s.sendMessage(ColorUtil.translate("&#EF4444Player not found.")); return; }

        if (plugin.getAllyManager().hasAlly(target.getUniqueId())) {
            s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#F59E0B" + target.getName() + " already has an Ally."));
            return;
        }
        plugin.getAllyManager().createAlly(target);
        plugin.getAllyManager().spawnAlly(target);
        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("admin-give-ally")
                .replace("{player}", target.getName())));
        target.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix")
                + "&#10B981An Admin has given you an Ally! Use &#FFFFFF/ally&#10B981 to manage it."));
    }

    private void handleRemove(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin remove <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        UUID uuid = null;
        String name = args[1];
        if (target != null) { uuid = target.getUniqueId(); name = target.getName(); }
        else {
            // Try offline
            for (Map.Entry<UUID, AllyData> e : plugin.getAllyManager().getAllAllyData().entrySet()) {
                if (e.getValue().getOwnerName().equalsIgnoreCase(args[1])) { uuid = e.getKey(); name = e.getValue().getOwnerName(); break; }
            }
        }
        if (uuid == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not found.")); return; }
        plugin.getAllyManager().removeAlly(uuid);
        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("admin-remove-ally").replace("{player}", name)));
    }

    private void handleReload(CommandSender s) {
        plugin.reload();
        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("plugin-reloaded")));
    }

    private void handleSetStat(CommandSender s, String[] args) {
        if (args.length < 4) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin setstat <player> <stat> <tier>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not online.")); return; }
        AllyData data = plugin.getAllyManager().getAllyData(target.getUniqueId());
        if (data == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player has no Ally.")); return; }

        String stat = args[2].toLowerCase();
        int value;
        try { value = Integer.parseInt(args[3]); } catch (NumberFormatException e) { s.sendMessage(ColorUtil.translate("&#EF4444Invalid number.")); return; }

        switch (stat) {
            case "health" -> data.setHealthTier(value);
            case "attack" -> data.setAttackTier(value);
            case "defense" -> data.setDefenseTier(value);
            case "speed" -> data.setSpeedTier(value);
            case "regeneration", "regen" -> data.setRegenerationTier(value);
            case "armor" -> data.setArmorTier(value);
            case "weapon" -> data.setWeaponTier(value);
            default -> { s.sendMessage(ColorUtil.translate("&#EF4444Unknown stat: " + stat)); return; }
        }

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(target.getUniqueId());
        if (ae != null) ae.refreshAttributes();
        plugin.getAllyManager().saveAlly(target.getUniqueId());

        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("admin-set-stat")
                .replace("{player}", target.getName())
                .replace("{stat}", stat)
                .replace("{value}", String.valueOf(value))));
    }

    private void handleSetLevel(CommandSender s, String[] args) {
        if (args.length < 3) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin setlevel <player> <level>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not online.")); return; }
        AllyData data = plugin.getAllyManager().getAllyData(target.getUniqueId());
        if (data == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player has no Ally.")); return; }

        int level;
        try { level = Integer.parseInt(args[2]); } catch (NumberFormatException e) { s.sendMessage(ColorUtil.translate("&#EF4444Invalid number.")); return; }
        level = Math.max(1, Math.min(level, new com.pallux.allycore.ally.AllyStats(plugin, data).getMaxLevel()));

        data.setLevel(level);
        data.setXp(0);
        AllyEntity ae = plugin.getAllyManager().getActiveAlly(target.getUniqueId());
        if (ae != null) { ae.refreshAttributes(); if (ae.getHologram() != null) ae.getHologram().update(); }
        plugin.getAllyManager().saveAlly(target.getUniqueId());
        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("admin-set-level")
                .replace("{player}", target.getName())
                .replace("{level}", String.valueOf(level))));
    }

    private void handleRevive(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin revive <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not online.")); return; }
        boolean ok = plugin.getAllyManager().reviveAlly(target);
        if (ok) {
            s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("admin-revived").replace("{player}", target.getName())));
            target.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#10B981An admin revived your Ally!"));
        } else {
            s.sendMessage(ColorUtil.translate("&#EF4444Could not revive – player has no dead Ally."));
        }
    }

    private void handleList(CommandSender s) {
        Map<UUID, AllyData> all = plugin.getAllyManager().getAllAllyData();
        String prefix = plugin.getConfigManager().getMessage("prefix");
        s.sendMessage(ColorUtil.translate(prefix + "&#6C63FF═══ All Allies (" + all.size() + ") ═══"));
        for (Map.Entry<UUID, AllyData> e : all.entrySet()) {
            AllyData d = e.getValue();
            boolean online = plugin.getAllyManager().getActiveAlly(e.getKey()) != null;
            s.sendMessage(ColorUtil.translate("  &#FFFFFF" + d.getOwnerName()
                    + " &#A3A3A3| &#A8A4FFLvl " + d.getLevel()
                    + " &#A3A3A3| " + (d.isAlive() ? "&#10B981Alive" : "&#EF4444Dead")
                    + " &#A3A3A3| " + (online ? "&#10B981Online" : "&#A3A3A3Offline")));
        }
    }

    private void handleMementos(CommandSender s, String[] args) {
        if (args.length < 4) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin mementos <player> <set|add|remove> <amount>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not online.")); return; }
        long amount;
        try { amount = Long.parseLong(args[3]); } catch (NumberFormatException e) { s.sendMessage(ColorUtil.translate("&#EF4444Invalid number.")); return; }

        switch (args[2].toLowerCase()) {
            case "set" -> plugin.getMementoManager().setMementos(target.getUniqueId(), amount);
            case "add" -> plugin.getMementoManager().addMementos(target.getUniqueId(), amount);
            case "remove" -> plugin.getMementoManager().removeMementos(target.getUniqueId(), amount);
            default -> { s.sendMessage(ColorUtil.translate("&#EF4444Use set, add, or remove.")); return; }
        }
        s.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#10B981Updated " + target.getName() + "'s Mementos."));
    }

    private void handleInfo(CommandSender s, String[] args) {
        if (args.length < 2) { s.sendMessage(ColorUtil.translate("&#EF4444Usage: /allyadmin info <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player not online.")); return; }
        AllyData data = plugin.getAllyManager().getAllyData(target.getUniqueId());
        if (data == null) { s.sendMessage(ColorUtil.translate("&#EF4444Player has no Ally.")); return; }
        com.pallux.allycore.ally.AllyStats stats = new com.pallux.allycore.ally.AllyStats(plugin, data);
        String prefix = plugin.getConfigManager().getMessage("prefix");
        s.sendMessage(ColorUtil.translate(prefix + "&#6C63FF═══ " + target.getName() + "'s Ally ═══"));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFName: &#FFFFFF" + data.getDisplayName()));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFLevel: &#FFFFFF" + data.getLevel()));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFMode: &#FFFFFF" + data.getMode().name()));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFAlive: " + (data.isAlive() ? "&#10B981Yes" : "&#EF4444No")));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFArmor Tier: &#FFFFFF" + data.getArmorTier() + "  Weapon Tier: &#FFFFFF" + data.getWeaponTier()));
        s.sendMessage(ColorUtil.translate("  &#A8A4FFHP: &#FFFFFF" + String.format("%.1f", stats.getMaxHealth())
                + "  ATK: &#FFFFFF" + String.format("%.1f", stats.getAttackDamage())
                + "  DEF: &#FFFFFF" + String.format("%.1f", stats.getDefense())));
        s.sendMessage(ColorUtil.translate("  &#6C63FFMementos: &#FFFFFF" + plugin.getMementoManager().getMementos(target.getUniqueId())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("allycore.admin")) return Collections.emptyList();
        if (args.length == 1) return List.of("give","remove","reload","setstat","setlevel","revive","list","mementos","info");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("list")) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setstat"))
            return List.of("health","attack","defense","speed","regeneration","armor","weapon");
        if (args.length == 3 && args[0].equalsIgnoreCase("mementos"))
            return List.of("set","add","remove");
        return Collections.emptyList();
    }
}
