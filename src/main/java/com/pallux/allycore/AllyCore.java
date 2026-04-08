package com.pallux.allycore;

import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.commands.AllyAdminCommand;
import com.pallux.allycore.commands.AllyCommand;
import com.pallux.allycore.commands.AllyShopCommand;
import com.pallux.allycore.config.ConfigManager;
import com.pallux.allycore.listeners.*;
import com.pallux.allycore.manager.AllyManager;
import com.pallux.allycore.manager.EconomyManager;
import com.pallux.allycore.manager.MementoManager;
import com.pallux.allycore.placeholder.AllyPlaceholderExpansion;
import com.pallux.allycore.skin.SkinManager;
import com.pallux.allycore.storage.StorageManager;
import com.pallux.allycore.util.ColorUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class AllyCore extends JavaPlugin {

    private static AllyCore instance;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private AllyManager allyManager;
    private EconomyManager economyManager;
    private MementoManager mementoManager;
    private SkinManager skinManager;

    private Economy vaultEconomy;
    private boolean vaultEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private boolean protocolLibEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("╔═══════════════════════════════╗");
        getLogger().info("║      AllyCore  v" + getDescription().getVersion() + "         ║");
        getLogger().info("║      by Pallux                ║");
        getLogger().info("╚═══════════════════════════════╝");

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Setup storage
        storageManager = new StorageManager(this);
        storageManager.initialize();

        // Setup economy
        economyManager = new EconomyManager(this);
        setupVault();

        // Setup mementos
        mementoManager = new MementoManager(this);

        // Setup skin manager
        skinManager = new SkinManager(this);
        setupProtocolLib();

        // Setup ally manager (load all saved allies)
        allyManager = new AllyManager(this);
        allyManager.loadAllAllies();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Setup PlaceholderAPI
        setupPlaceholderAPI();

        // Start auto-save task
        startAutoSave();

        getLogger().info("AllyCore enabled successfully! ✦");
    }

    @Override
    public void onDisable() {
        if (allyManager != null) {
            allyManager.saveAllAllies();
            allyManager.despawnAllAllies();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        getLogger().info("AllyCore disabled. Allies saved.");
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy features will be disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found! Economy features will be disabled.");
            return;
        }
        vaultEconomy = rsp.getProvider();
        vaultEnabled = true;
        getLogger().info("Vault hooked successfully!");
    }

    private void setupProtocolLib() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            protocolLibEnabled = true;
            skinManager.initialize();
            getLogger().info("ProtocolLib hooked successfully!");
        } else {
            getLogger().warning("ProtocolLib not found! Ally skins will use default appearance.");
        }
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AllyPlaceholderExpansion(this).register();
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI hooked successfully!");
        }
    }

    private void registerCommands() {
        AllyCommand allyCommand = new AllyCommand(this);
        getCommand("ally").setExecutor(allyCommand);
        getCommand("ally").setTabCompleter(allyCommand);

        AllyAdminCommand adminCommand = new AllyAdminCommand(this);
        getCommand("allyadmin").setExecutor(adminCommand);
        getCommand("allyadmin").setTabCompleter(adminCommand);

        AllyShopCommand shopCommand = new AllyShopCommand(this);
        getCommand("allyshop").setExecutor(shopCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new AllyInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new AllyDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilRenameListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
    }

    private void startAutoSave() {
        int interval = configManager.getMainConfig().getInt("plugin.auto-save-interval", 300) * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (allyManager != null) {
                allyManager.saveAllAllies();
            }
        }, interval, interval);
    }

    public void reload() {
        configManager.loadAll();
        skinManager.reload();
        ColorUtil.clearCache();
        getLogger().info("AllyCore reloaded.");
    }

    // ─── Getters ───────────────────────────────────────────────────────────────

    public static AllyCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public AllyManager getAllyManager() { return allyManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public MementoManager getMementoManager() { return mementoManager; }
    public SkinManager getSkinManager() { return skinManager; }
    public Economy getVaultEconomy() { return vaultEconomy; }
    public boolean isVaultEnabled() { return vaultEnabled; }
    public boolean isPlaceholderAPIEnabled() { return placeholderAPIEnabled; }
    public boolean isProtocolLibEnabled() { return protocolLibEnabled; }
}
