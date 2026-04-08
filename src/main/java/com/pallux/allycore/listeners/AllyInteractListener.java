package com.pallux.allycore.listeners;

import com.pallux.allycore.AllyCore;
import com.pallux.allycore.ally.AllyData;
import com.pallux.allycore.ally.AllyEntity;
import com.pallux.allycore.gui.MainAllyMenu;
import com.pallux.allycore.util.ColorUtil;
import com.pallux.allycore.util.MessageUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AllyInteractListener implements Listener {

    private final AllyCore plugin;

    public AllyInteractListener(AllyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        // Only main hand
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Zombie zombie)) return;

        UUID ownerUUID = plugin.getAllyManager().getOwnerOfEntity(zombie.getUniqueId());
        if (ownerUUID == null) return;

        Player player = event.getPlayer();

        // Only the owner can interact
        if (!player.getUniqueId().equals(ownerUUID)) return;

        event.setCancelled(true);

        if (!player.hasPermission("allycore.player")) {
            MessageUtil.send(player, "no-permission");
            return;
        }

        AllyEntity ae = plugin.getAllyManager().getActiveAlly(ownerUUID);
        if (ae == null || !ae.isSpawned()) return;

        // Check if player is holding food to heal the Ally
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isEdible() && !player.isSneaking()) {
            double maxHealth = ae.getStats().getMaxHealth();
            double currentHealth = ae.getEntity().getHealth();

            if (currentHealth < maxHealth) {
                // Fetch heal amount from config
                double healAmount = plugin.getConfigManager().getAllyConfig().getDouble("ally.combat.food-heal-amount", 4.0);
                ae.getEntity().setHealth(Math.min(maxHealth, currentHealth + healAmount));

                // Consume the item if not in creative mode
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    handItem.setAmount(handItem.getAmount() - 1);
                }

                // Play eating effects
                player.getWorld().playSound(ae.getEntity().getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.HEART, ae.getEntity().getLocation().add(0, 1.5, 0), 4, 0.4, 0.4, 0.4, 0);

                if (ae.getHologram() != null) ae.getHologram().update();
                return; // Stop here so it doesn't also send the quick-info message
            }
        }

        // Shift + Right-Click → open menu
        if (player.isSneaking()) {
            new MainAllyMenu(plugin, player, ae).open();
            MessageUtil.playSound(player, "menu-open");
        }
        // Normal right-click (without food) → show quick info
        else {
            AllyData data = plugin.getAllyManager().getAllyData(ownerUUID);
            if (data == null) return;

            double hp = ae.getEntity().getHealth();

            // Fetch message dynamically from config to prevent hardcoding
            String rawMsg = plugin.getConfigManager().getMessagesConfig().getString("messages.ally-quick-info", "{prefix}&#A8A4FF{name} &#A3A3A3| &#EF4444❤ {health} &#A3A3A3| &#A8A4FFLvl {level} &#A3A3A3| &#FFFFFF{mode}");

            String msg = rawMsg
                    .replace("{prefix}", plugin.getConfigManager().getMessage("prefix"))
                    .replace("{name}", data.getDisplayName())
                    .replace("{health}", String.format("%.1f", hp))
                    .replace("{level}", String.valueOf(data.getLevel()))
                    .replace("{mode}", data.getMode().name());

            player.sendMessage(ColorUtil.translate(msg));
        }
    }
}