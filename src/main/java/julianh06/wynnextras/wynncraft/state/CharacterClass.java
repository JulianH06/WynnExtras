package julianh06.wynnextras.wynncraft.state;

import java.util.Locale;

public enum CharacterClass {
    UNKNOWN("Unknown"),
    WARRIOR("Warrior"),
    MAGE("Mage"),
    ASSASSIN("Assassin"),
    ARCHER("Archer"),
    SHAMAN("Shaman");

    private final String displayName;

    CharacterClass(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static CharacterClass parse(String value) {
        if (value == null) return UNKNOWN;
        String normalized = value.toUpperCase(Locale.ROOT);
        if (normalized.contains("WARRIOR") || normalized.contains("KNIGHT")) return WARRIOR;
        if (normalized.contains("MAGE") || normalized.contains("DARK WIZARD")) return MAGE;
        if (normalized.contains("ASSASSIN") || normalized.contains("NINJA")) return ASSASSIN;
        if (normalized.contains("ARCHER") || normalized.contains("HUNTER")) return ARCHER;
        if (normalized.contains("SHAMAN") || normalized.contains("SKYSEER")) return SHAMAN;
        return UNKNOWN;
    }
}
