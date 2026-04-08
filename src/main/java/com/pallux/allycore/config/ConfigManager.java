package com.pallux.allycore.config;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final AllyCore plugin;

    private FileConfiguration mainConfig;
    private FileConfiguration allyConfig;
    private FileConfiguration upgradesConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration economyConfig;
    private FileConfiguration guiConfig;

    public ConfigManager(AllyCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();

        allyConfig    = loadOrCreate("config/ally.yml");
        upgradesConfig = loadOrCreate("config/upgrades.yml");
        messagesConfig = loadOrCreate("config/messages.yml");
        economyConfig  = loadOrCreate("config/economy.yml");
        guiConfig      = loadOrCreate("config/gui.yml");
    }

    private FileConfiguration loadOrCreate(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Merge missing defaults from jar resource
        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
        return config;
    }

    public void saveConfig(String resourcePath, FileConfiguration config) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + resourcePath, e);
        }
    }

    /**
     * Retrieves a translated message string by key.
     */
    public String getMessage(String key) {
        if (key.equals("prefix")) {
            return ColorUtil.translate(mainConfig.getString("plugin.prefix",
                    "&#6C63FF&lAlly&r&#A8A4FF &8»&r "));
        }
        String raw = messagesConfig.getString("messages." + key, "&cMissing: " + key);
        return ColorUtil.translate(raw);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public FileConfiguration getMainConfig()     { return mainConfig;     }
    public FileConfiguration getAllyConfig()      { return allyConfig;     }
    public FileConfiguration getUpgradesConfig()  { return upgradesConfig; }
    public FileConfiguration getMessagesConfig()  { return messagesConfig; }
    public FileConfiguration getEconomyConfig()   { return economyConfig;  }
    public FileConfiguration getGuiConfig()       { return guiConfig;      }
}
