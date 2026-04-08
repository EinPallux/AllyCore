package com.pallux.allycore.gui;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Static registry tracking which players are in the anvil-rename flow.
 * Decouples MainAllyMenu from AnvilRenameListener.
 */
public final class AnvilRenameRegistry {

    private static final Set<UUID> renamingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AnvilRenameRegistry() {}

    public static void setRenaming(UUID uuid, boolean value) {
        if (value) renamingPlayers.add(uuid);
        else renamingPlayers.remove(uuid);
    }

    public static boolean isRenaming(UUID uuid) {
        return renamingPlayers.contains(uuid);
    }
}
