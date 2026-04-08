package com.pallux.allycore.ally;

public enum CombatMode {
    DEFENSIVE,
    AGGRESSIVE,
    NEUTRAL,
    ALLROUND;

    public static CombatMode fromString(String s) {
        if (s == null) return DEFENSIVE;
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFENSIVE;
        }
    }
}
