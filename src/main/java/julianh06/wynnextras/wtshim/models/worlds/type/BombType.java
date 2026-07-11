// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — BombType enum with display names, parse-name aliases, and duration. */
package julianh06.wynnextras.wtshim.models.worlds.type;

import java.util.List;

public enum BombType {
    COMBAT_XP("Combat XP", List.of("Combat XP", "Combat Experience"), 20),
    DUNGEON("Dungeon", List.of("Dungeon", "Free Dungeon Entry"), 10),
    LOOT("Loot", List.of("Loot"), 20),
    PROFESSION_SPEED("Profession Speed", List.of("Profession Speed"), 10),
    PROFESSION_XP("Profession XP", List.of("Profession XP", "Profession Experience"), 20),
    LOOT_CHEST("Loot Chest", List.of("Loot Chest", "More Chest Loot"), 20);

    private final String displayName;
    private final List<String> parseNames;
    private final int activeMinutes;

    BombType(String displayName, List<String> parseNames, int activeMinutes) {
        this.displayName = displayName;
        this.parseNames = parseNames;
        this.activeMinutes = activeMinutes;
    }

    public String getName() { return name(); }
    public String getDisplayName() { return displayName; }
    public int getActiveMinutes() { return activeMinutes; }

    /** Case-insensitive lookup against all declared parse names. */
    public static BombType fromString(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        for (BombType t : values()) {
            for (String alias : t.parseNames) {
                if (alias.equalsIgnoreCase(trimmed)) return t;
            }
        }
        return null;
    }
}
