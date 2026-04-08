package com.pallux.allycore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing color codes: &#RRGGBB hex and &x legacy codes.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Map<String, String> cache = new HashMap<>();

    private ColorUtil() {}

    /**
     * Translates a string with &#RRGGBB hex codes and & legacy codes.
     */
    public static String translate(String input) {
        if (input == null) return "";
        if (cache.containsKey(input)) return cache.get(input);

        String result = translateHex(input);
        result = ChatColor.translateAlternateColorCodes('&', result);

        cache.put(input, result);
        return result;
    }

    /**
     * Translates hex codes &#RRGGBB → §x§R§R§G§G§B§B
     */
    public static String translateHex(String input) {
        if (input == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Strips all color codes from a string.
     */
    public static String strip(String input) {
        if (input == null) return "";
        return ChatColor.stripColor(translate(input));
    }

    /**
     * Translates and wraps as a Component (non-italic for item names/lore).
     */
    public static Component component(String input) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(translate(input))
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    /**
     * Creates a non-italic Component without translation (plain text).
     */
    public static Component plain(String text) {
        return Component.text(text)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    /**
     * Clears the color translation cache (on reload).
     */
    public static void clearCache() {
        cache.clear();
    }
}
