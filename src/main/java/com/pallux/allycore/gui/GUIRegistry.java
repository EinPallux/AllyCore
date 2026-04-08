package com.pallux.allycore.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GUIRegistry {
    private static final Map<UUID, AllyGUI> openGUIs = new HashMap<>();

    private GUIRegistry() {}

    public static void registerOpenGUI(UUID uuid, AllyGUI gui) {
        openGUIs.put(uuid, gui);
    }

    public static AllyGUI getOpenGUI(UUID uuid) {
        return openGUIs.get(uuid);
    }

    public static void removeOpenGUI(UUID uuid) {
        openGUIs.remove(uuid);
    }
}
