package com.pallux.allycore.commands;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.gui.AllyShopMenu;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AllyShopCommand implements CommandExecutor, TabCompleter {

    private final AllyCore plugin;

    public AllyShopCommand(AllyCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }
        if (!player.hasPermission("allycore.player")) {
            MessageUtil.send(player, "no-permission");
            return true;
        }
        new AllyShopMenu(plugin, player).open();
        MessageUtil.playSound(player, "menu-open");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return Collections.emptyList();
    }
}
