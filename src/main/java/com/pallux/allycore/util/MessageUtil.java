package com.pallux.allycore.util;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public final class MessageUtil {

    private MessageUtil() {}

    /**
     * Sends a message from messages.yml to a player.
     * Supports {prefix} and other placeholders via the replacements map.
     */
    public static void send(Player player, String key, Map<String, String> replacements) {
        AllyCore plugin = AllyCore.getInstance();
        ConfigManager cm = plugin.getConfigManager();

        String prefix = cm.getMessage("prefix");
        String raw = cm.getMessagesConfig().getString("messages." + key, "&cMissing message: " + key);

        raw = raw.replace("{prefix}", prefix);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        player.sendMessage(ColorUtil.translate(raw));
    }

    public static void send(org.bukkit.command.CommandSender sender, String key, Map<String, String> replacements) {
        if (sender instanceof Player player) { send(player, key, replacements); return; }
        if (sender == null) return;
        AllyCore plugin = AllyCore.getInstance();
        String prefix = plugin.getConfigManager().getMessage("prefix");
        String raw = plugin.getConfigManager().getMessagesConfig().getString("messages." + key, "&cMissing message: " + key);
        raw = raw.replace("{prefix}", prefix);
        sender.sendMessage(ColorUtil.translate(raw));
    }

    public static void send(Player player, String key) {
        send(player, key, null);
    }

    /**
     * Sends an action bar message.
     */
    public static void sendActionBar(Player player, String text) {
        player.sendActionBar(ColorUtil.component(text));
    }

    /**
     * Sends a title + subtitle.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(
                ColorUtil.component(title),
                ColorUtil.component(subtitle),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        ));
    }

    /**
     * Plays a sound from the sounds section of messages.yml.
     */
    public static void playSound(Player player, String soundKey) {
        AllyCore plugin = AllyCore.getInstance();
        if (!plugin.getConfigManager().getMessagesConfig().getBoolean("messages.play-sounds", true)) return;

        String soundName = plugin.getConfigManager().getMessagesConfig().getString("sounds." + soundKey);
        if (soundName == null) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * Formats a raw message string (replaces {prefix} and color codes).
     */
    public static String format(String raw, Map<String, String> replacements) {
        AllyCore plugin = AllyCore.getInstance();
        String prefix = plugin.getConfigManager().getMessage("prefix");
        raw = raw.replace("{prefix}", prefix);
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ColorUtil.translate(raw);
    }
}
