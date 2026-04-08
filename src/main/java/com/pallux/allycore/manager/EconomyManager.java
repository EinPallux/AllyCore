package com.pallux.allycore.manager;

import com.pallux.allycore.AllyCore;
import org.bukkit.entity.Player;

public class EconomyManager {

    private final AllyCore plugin;

    public EconomyManager(AllyCore plugin) {
        this.plugin = plugin;
    }

    // ─── Vault ───────────────────────────────────────────────────────────────

    public double getBalance(Player player) {
        if (!plugin.isVaultEnabled()) return 0;
        return plugin.getVaultEconomy().getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (!plugin.isVaultEnabled()) return player.hasPermission("allycore.bypass.cost");
        return player.hasPermission("allycore.bypass.cost") || plugin.getVaultEconomy().has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (player.hasPermission("allycore.bypass.cost")) return true;
        if (!plugin.isVaultEnabled()) return false;
        return plugin.getVaultEconomy().withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(Player player, double amount) {
        if (!plugin.isVaultEnabled()) return;
        plugin.getVaultEconomy().depositPlayer(player, amount);
    }

    // ─── Combined cost check & charge ────────────────────────────────────────

    public boolean canAfford(Player player, double vaultCost, long mementoCost) {
        if (player.hasPermission("allycore.bypass.cost")) return true;
        boolean vault = vaultCost <= 0 || has(player, vaultCost);
        boolean mementos = mementoCost <= 0 || plugin.getMementoManager().getMementos(player.getUniqueId()) >= mementoCost;
        return vault && mementos;
    }

    /**
     * Attempt to charge both vault and mementos. Returns false if insufficient funds.
     */
    public boolean charge(Player player, double vaultCost, long mementoCost) {
        if (player.hasPermission("allycore.bypass.cost")) return true;
        if (!canAfford(player, vaultCost, mementoCost)) return false;
        if (vaultCost > 0) withdraw(player, vaultCost);
        if (mementoCost > 0) plugin.getMementoManager().removeMementos(player.getUniqueId(), mementoCost);
        return true;
    }
}
