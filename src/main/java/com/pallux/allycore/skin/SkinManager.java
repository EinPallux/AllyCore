package com.pallux.allycore.skin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.*;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.pallux.allycore.AllyCore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
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
     */
    public void applySkin(Zombie entity, Player owner) {
        if (protocolManager == null) return;
        if (skinValue.isEmpty()) return;

        UUID fakeUUID = entity.getUniqueId();
        String name = "Ally_" + owner.getName().substring(0, Math.min(5, owner.getName().length()));

        // Create the Profile using Paper API to completely bypass Authlib version incompatibilities
        Object nativeProfile = createNativeGameProfile(fakeUUID, name, skinValue, skinSignature);
        WrappedGameProfile profile;

        if (nativeProfile != null) {
            profile = WrappedGameProfile.fromHandle(nativeProfile);
        } else {
            // Fallback (no skin, but prevents crashes)
            profile = new WrappedGameProfile(fakeUUID, name);
        }

        profileCache.put(fakeUUID, profile);

        // Update for existing players viewing the entity
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendSkinPackets(viewer, profile);
        }
    }

    /**
     * Uses Paper's native API to build a Mojang GameProfile properly handling Immutable property maps safely.
     */
    private Object createNativeGameProfile(UUID uuid, String name, String texture, String signature) {
        try {
            // 1. Let Paper API handle the complex internal Authlib GameProfile creation & immutability securely
            PlayerProfile paperProfile = Bukkit.createProfile(uuid, name);
            paperProfile.setProperty(new ProfileProperty("textures", texture, signature));

            // 2. Extract the native Mojang GameProfile object out of the Paper implementation
            for (Method m : paperProfile.getClass().getMethods()) {
                if (m.getReturnType().getSimpleName().equals("GameProfile")) {
                    m.setAccessible(true);
                    return m.invoke(paperProfile); // Returns the perfectly built native GameProfile
                }
            }

            plugin.getLogger().warning("[SkinManager] Could not extract native GameProfile from Paper PlayerProfile.");
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[SkinManager] Failed to create GameProfile via Paper API:", e);
            return null;
        }
    }

    private void sendSkinPackets(Player viewer, WrappedGameProfile profile) {
        if (protocolManager == null) return;
        try {
            // Attempt 1.19.3+ Packet Data Structure
            PacketContainer infoPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            infoPacket.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            PlayerInfoData infoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);
            infoPacket.getPlayerInfoDataLists().write(1, Collections.singletonList(infoData));
            protocolManager.sendServerPacket(viewer, infoPacket);
        } catch (Exception e) {
            try {
                // Fallback attempt for older packet structures
                PacketContainer infoPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                infoPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                PlayerInfoData infoData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);
                infoPacket.getPlayerInfoDataLists().write(0, Collections.singletonList(infoData));
                protocolManager.sendServerPacket(viewer, infoPacket);
            } catch (Exception ex) {
                // Graceful fail
            }
        }
    }

    public String getSkinValue()     { return skinValue;     }
    public String getSkinSignature() { return skinSignature; }
}