package com.pallux.allycore.commands;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.ally.AllyStats;
import com.pallux.allycore.gui.MainAllyMenu;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.FluidCollisionMode;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.*;

public class AllyCommand implements CommandExecutor, TabCompleter {

    private final AllyCore plugin;

    public AllyCommand(AllyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("player-only")));
            return true;
        }
        if (!player.hasPermission("allycore.player")) {
            MessageUtil.send(player, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            AllyEntity ae = plugin.getAllyManager().getActiveAlly(player.getUniqueId());
            if (ae == null) {
                if (!plugin.getAllyManager().hasAlly(player.getUniqueId())) {
                    MessageUtil.send(player, "no-ally");
                } else {
                    AllyData d = plugin.getAllyManager().getAllyData(player.getUniqueId());
                    if (d != null && !d.isAlive()) {
                        MessageUtil.send(player, "ally-dead");
                    } else {
                        MessageUtil.send(player, "ally-dismissed");
                    }
                }
                return true;
            }
            new MainAllyMenu(plugin, player, ae).open();
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "summon" -> handleSummon(player);
            case "dismiss" -> handleDismiss(player);
            case "info" -> handleInfo(player);
            case "rename" -> handleRename(player, args);
            case "attack" -> handleAttack(player, args);
            case "follow" -> handleFollow(player);
            case "stay" -> handleStay(player);
            case "mode" -> handleMode(player, args);
            case "revive" -> handleRevive(player);
            case "mementos" -> handleMementos(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player p) {
        String prefix = plugin.getConfigManager().getMessage("prefix");
        p.sendMessage(ColorUtil.translate(prefix + "&#6C63FF═══ AllyCore Commands ═══"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally &#A3A3A3- Open Ally Menu"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally summon &#A3A3A3- Summon your Ally"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally dismiss &#A3A3A3- Dismiss your Ally"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally info &#A3A3A3- View Ally stats"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally rename <name> &#A3A3A3- Rename your Ally"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally attack [cancel] &#A3A3A3- Command Ally to attack your target"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally follow &#A3A3A3- Toggle follow mode"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally stay &#A3A3A3- Toggle stay mode"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally mode <mode> &#A3A3A3- Set combat mode"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally revive &#A3A3A3- Revive fallen Ally"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/ally mementos &#A3A3A3- Check Memento balance"));
        p.sendMessage(ColorUtil.translate("&#A8A4FF/allyshop &#A3A3A3- Open the Ally Shop"));
    }

    private void handleSummon(Player p) {
        if (!plugin.getAllyManager().hasAlly(p.getUniqueId())) { MessageUtil.send(p, "no-ally"); return; }
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (!data.isAlive()) { MessageUtil.send(p, "ally-dead"); return; }
        if (data.isSummoned()) { MessageUtil.send(p, "ally-already-exists"); return; }
        plugin.getAllyManager().spawnAlly(p);
        MessageUtil.send(p, "ally-summoned");
        MessageUtil.playSound(p, "ally-summoned");
    }

    private void handleDismiss(Player p) {
        if (!plugin.getAllyManager().hasAlly(p.getUniqueId())) { MessageUtil.send(p, "no-ally"); return; }
        plugin.getAllyManager().despawnAlly(p.getUniqueId());
        plugin.getAllyManager().saveAlly(p.getUniqueId());
        MessageUtil.send(p, "ally-dismissed");
        MessageUtil.playSound(p, "ally-dismissed");
    }

    private void handleInfo(Player p) {
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (data == null) { MessageUtil.send(p, "no-ally"); return; }
        AllyStats stats = new AllyStats(plugin, data);
        String prefix = plugin.getConfigManager().getMessage("prefix");
        long mementos = plugin.getMementoManager().getMementos(p.getUniqueId());
        p.sendMessage(ColorUtil.translate(prefix + "&#6C63FF═══ " + data.getDisplayName() + " ═══"));
        p.sendMessage(ColorUtil.translate("  &#A8A4FFLevel: &#FFFFFF" + data.getLevel() + "  &#A3A3A3| XP: &#FFFFFF" + String.format("%.0f", data.getXp()) + "&#A3A3A3/&#FFFFFF" + String.format("%.0f", stats.getXpRequired())));
        p.sendMessage(ColorUtil.translate("  &#EF4444❤ HP: &#FFFFFF" + String.format("%.1f", stats.getMaxHealth())));
        p.sendMessage(ColorUtil.translate("  &#F97316⚔ ATK: &#FFFFFF" + String.format("%.1f", stats.getAttackDamage())));
        p.sendMessage(ColorUtil.translate("  &#3B82F6🛡 DEF: &#FFFFFF" + String.format("%.1f", stats.getDefense())));
        p.sendMessage(ColorUtil.translate("  &#10B981⚡ SPD: &#FFFFFF" + String.format("%.3f", stats.getSpeed())));
        p.sendMessage(ColorUtil.translate("  &#6C63FF✦ Mementos: &#FFFFFF" + mementos));
        p.sendMessage(ColorUtil.translate("  &#A8A4FFMode: &#FFFFFF" + data.getMode().name()));
        p.sendMessage(ColorUtil.translate("  &#A8A4FFStatus: " + (data.isAlive() ? "&#10B981Alive" : "&#EF4444Dead")));
    }

    private void handleRename(Player p, String[] args) {
        if (!plugin.getAllyManager().hasAlly(p.getUniqueId())) { MessageUtil.send(p, "no-ally"); return; }
        if (args.length < 2) {
            p.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#EF4444Usage: /ally rename <name>"));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (name.length() > 32) { MessageUtil.send(p, "rename-too-long", Map.of("max", "32")); return; }
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        data.setCustomName(name);
        plugin.getAllyManager().saveAlly(p.getUniqueId());
        AllyEntity ae = plugin.getAllyManager().getActiveAlly(p.getUniqueId());
        if (ae != null && ae.getHologram() != null) ae.getHologram().update();
        MessageUtil.send(p, "rename-success", Map.of("name", name));
    }

    private void handleAttack(Player p, String[] args) {
        AllyEntity ae = plugin.getAllyManager().getActiveAlly(p.getUniqueId());
        if (ae == null) { MessageUtil.send(p, "no-ally"); return; }

        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            ae.cancelForcedAttack();
            MessageUtil.send(p, "attack-cancel");
            return;
        }

        // Raycast for target
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                20.0,
                entity -> entity != p && entity != ae.getEntity() && entity instanceof LivingEntity
        );

        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            MessageUtil.send(p, "attack-no-target");
            return;
        }

        ae.forceAttack(target);
        String targetName = (target instanceof Player tp) ? tp.getName() : target.getName();
        MessageUtil.send(p, "attack-target", Map.of("target", targetName));
    }

    private void handleFollow(Player p) {
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (data == null) { MessageUtil.send(p, "no-ally"); return; }
        data.setFollowing(true);
        plugin.getAllyManager().saveAlly(p.getUniqueId());
        MessageUtil.send(p, "ally-following");
    }

    private void handleStay(Player p) {
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (data == null) { MessageUtil.send(p, "no-ally"); return; }
        data.setFollowing(false);
        plugin.getAllyManager().saveAlly(p.getUniqueId());
        MessageUtil.send(p, "ally-staying");
    }

    private void handleMode(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix")
                    + "&#EF4444Usage: /ally mode <DEFENSIVE|AGGRESSIVE|NEUTRAL|ALLROUND>"));
            return;
        }
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (data == null) { MessageUtil.send(p, "no-ally"); return; }
        try {
            var mode = com.pallux.allycore.ally.CombatMode.fromString(args[1]);
            data.setMode(mode);
            plugin.getAllyManager().saveAlly(p.getUniqueId());
            String display = plugin.getConfigManager().getAllyConfig()
                    .getString("modes." + mode.name() + ".display", mode.name());
            MessageUtil.send(p, "mode-changed", Map.of("mode", display));
        } catch (Exception e) {
            p.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#EF4444Invalid mode."));
        }
    }

    private void handleRevive(Player p) {
        AllyData data = plugin.getAllyManager().getAllyData(p.getUniqueId());
        if (data == null) { MessageUtil.send(p, "no-ally"); return; }
        if (data.isAlive()) {
            p.sendMessage(ColorUtil.translate(plugin.getConfigManager().getMessage("prefix") + "&#F59E0BYour Ally is already alive!"));
            return;
        }
        double vault = plugin.getConfigManager().getEconomyConfig().getDouble("revive.vault-cost", 500);
        long mem = plugin.getConfigManager().getEconomyConfig().getLong("revive.mementos-cost", 25);
        if (!plugin.getEconomyManager().has(p, vault) && !p.hasPermission("allycore.bypass.revive")) {
            MessageUtil.send(p, "revive-no-money", Map.of("vault", String.format("%.0f", vault)));
            return;
        }
        if (plugin.getMementoManager().getMementos(p.getUniqueId()) < mem && !p.hasPermission("allycore.bypass.revive")) {
            MessageUtil.send(p, "revive-no-mementos", Map.of("mementos", String.valueOf(mem)));
            return;
        }
        if (!p.hasPermission("allycore.bypass.revive")) {
            plugin.getEconomyManager().withdraw(p, vault);
            plugin.getMementoManager().removeMementos(p.getUniqueId(), mem);
        }
        plugin.getAllyManager().reviveAlly(p);
        MessageUtil.send(p, "revive-success");
        MessageUtil.playSound(p, "ally-revived");
    }

    private void handleMementos(Player p) {
        long bal = plugin.getMementoManager().getMementos(p.getUniqueId());
        MessageUtil.send(p, "memento-balance", Map.of("amount", String.valueOf(bal)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("help","summon","dismiss","info","rename","attack","follow","stay","mode","revive","mementos");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            return List.of("DEFENSIVE","AGGRESSIVE","NEUTRAL","ALLROUND");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("attack")) {
            return List.of("cancel");
        }
        return Collections.emptyList();
    }
}
