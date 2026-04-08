package com.pallux.allycore.skin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import com.pallux.allycore.AllyCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Applies custom skins to Ally entities using ProtocolLib.
 * Sends fake Player packets so the Ally visually appears as a custom-skinned player model.
 */
public class SkinManager {

    private final AllyCore plugin;
    private ProtocolManager protocolManager;

    private final NamespacedKey allyKey;

    // Cached skin data
    private String skinValue = "";
    private String skinSignature = "";

    // Map entity UUID -> fake GameProfile for re-sending on reload
    private final Map<UUID, WrappedGameProfile> profileCache = new ConcurrentHashMap<>();

    public SkinManager(AllyCore plugin) {
        this.plugin = plugin;
        this.allyKey = new NamespacedKey(plugin, "ally_owner");
    }

    public NamespacedKey getAllyKey() {
        return allyKey;
    }

    public void initialize() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        reload();
        registerPacketListeners();
    }

    private void registerPacketListeners() {
        if (protocolManager == null) return;

        // Intercept entity spawn packets and change zombies to players if they are an Ally
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                UUID entityUuid = packet.getUUIDs().read(0);

                if (profileCache.containsKey(entityUuid)) {
                    // Ensure the client has the PlayerInfoData before spawning the entity
                    sendSkinPackets(event.getPlayer(), profileCache.get(entityUuid));

                    // Change EntityType from ZOMBIE to PLAYER
                    packet.getEntityTypeModifier().write(0, EntityType.PLAYER);
                }
            }
        });
    }

    public void reload() {
        if (!plugin.isProtocolLibEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::fetchSkin);
    }

    private void fetchSkin() {
        String mode = plugin.getConfigManager().getMainConfig().getString("skin.mode", "USERNAME");

        if (mode.equalsIgnoreCase("CUSTOM")) {
            skinValue = plugin.getConfigManager().getMainConfig().getString("skin.texture-value", "");
            skinSignature = plugin.getConfigManager().getMainConfig().getString("skin.texture-signature", "");
            plugin.getLogger().info("[SkinManager] Using custom texture.");
        } else {
            String username = plugin.getConfigManager().getMainConfig().getString("skin.username", "AllyKnight");
            try {
                // Step 1: UUID lookup
                URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                JsonObject uuidJson = JsonParser.parseReader(new InputStreamReader(uuidUrl.openStream())).getAsJsonObject();
                String uuid = uuidJson.get("id").getAsString();

                // Step 2: Profile lookup with textures
                URL profileUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                JsonObject profileJson = JsonParser.parseReader(new InputStreamReader(profileUrl.openStream())).getAsJsonObject();
                var properties = profileJson.getAsJsonArray("properties");
                for (var prop : properties) {
                    JsonObject p = prop.getAsJsonObject();
                    if (p.get("name").getAsString().equals("textures")) {
                        skinValue = p.get("value").getAsString();
                        skinSignature = p.get("signature").getAsString();
                        break;
                    }
                }
                plugin.getLogger().info("[SkinManager] Fetched skin for: " + username);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[SkinManager] Could not fetch skin for '" + username + "': " + e.getMessage());
            }
        }
    }

    /**
     * Apply the configured skin to a Zombie entity visually for all players.
     * We tag the entity with PDC; actual visual disguise is via ProtocolLib packet spoofing.
     */
    public void applySkin(Zombie entity, Player owner) {
        if (protocolManager == null) return;
        if (skinValue.isEmpty()) return;

        // Build a fake GameProfile with skin textures
        UUID fakeUUID = entity.getUniqueId();
        WrappedGameProfile profile = new WrappedGameProfile(fakeUUID, "Ally_" + owner.getName().substring(0, Math.min(5, owner.getName().length())));
        profile.getProperties().put("textures", new WrappedSignedProperty("textures", skinValue, skinSignature));
        profileCache.put(fakeUUID, profile);

        // Update for existing players viewing the entity
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendSkinPackets(viewer, profile);
        }
    }

    private void sendSkinPackets(Player viewer, WrappedGameProfile profile) {
        if (protocolManager == null) return;
        try {
            // Send PlayerInfo packet to register the skin
            PacketContainer infoPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            // ProtocolLib handles the EnumWrappers
            infoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            PlayerInfoData infoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);
            infoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(infoData));
            protocolManager.sendServerPacket(viewer, infoPacket);
        } catch (Exception e) {
            // ProtocolLib API may vary; gracefully skip
        }
    }

    public String getSkinValue()     { return skinValue;     }
    public String getSkinSignature() { return skinSignature; }
}