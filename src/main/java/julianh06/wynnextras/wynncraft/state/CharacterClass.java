package julianh06.wynnextras.wynncraft.state;

import java.util.Locale;

public enum CharacterClass {
    UNKNOWN("Unknown", "", 0xFFFFFFFF),
    WARRIOR("Warrior", "spear", 0xFFFF5555),
    MAGE("Mage", "wand", 0xFF55FFFF),
    ASSASSIN("Assassin", "dagger", 0xFFFF55FF),
    ARCHER("Archer", "bow", 0xFF55FF55),
    SHAMAN("Shaman", "relik", 0xFFFFFF55);

    private static final CharacterClass[] PLAYABLE_VALUES = {
            ARCHER, WARRIOR, ASSASSIN, MAGE, SHAMAN
    };

    private final String displayName;
    private final String weaponSubtype;
    private final int color;

    CharacterClass(String displayName, String weaponSubtype, int color) {
        this.displayName = displayName;
        this.weaponSubtype = weaponSubtype;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public String apiName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String weaponSubtype() {
        return weaponSubtype;
    }

    public int color() {
        return color;
    }

    public static CharacterClass[] playableValues() {
        return PLAYABLE_VALUES.clone();
    }

    public static CharacterClass fromWeaponSubtype(String subtype) {
        if (subtype != null) {
            for (CharacterClass value : PLAYABLE_VALUES) {
                if (value.weaponSubtype.equalsIgnoreCase(subtype)) return value;
            }
        }
        return ARCHER;
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
