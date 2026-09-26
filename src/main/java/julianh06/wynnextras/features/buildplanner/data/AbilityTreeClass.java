package julianh06.wynnextras.features.buildplanner.data;

import java.util.Locale;

public enum AbilityTreeClass {
    ARCHER("archer", "bow", "Archer", 0xFF55FF55),
    WARRIOR("warrior", "spear", "Warrior", 0xFFFF5555),
    ASSASSIN("assassin", "dagger", "Assassin", 0xFFFF55FF),
    MAGE("mage", "wand", "Mage", 0xFF55FFFF),
    SHAMAN("shaman", "relik", "Shaman", 0xFFFFFF55);

    private final String apiName;
    private final String weaponSubtype;
    private final String displayName;
    private final int color;

    AbilityTreeClass(String apiName, String weaponSubtype, String displayName, int color) {
        this.apiName = apiName;
        this.weaponSubtype = weaponSubtype;
        this.displayName = displayName;
        this.color = color;
    }

    public String apiName() {
        return apiName;
    }

    public String weaponSubtype() {
        return weaponSubtype;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    public static AbilityTreeClass fromWeaponSubtype(String subtype) {
        String normalized = subtype == null ? "" : subtype.toLowerCase(Locale.ROOT);
        for (AbilityTreeClass value : values()) {
            if (value.weaponSubtype.equals(normalized)) {
                return value;
            }
        }
        return ARCHER;
    }
}
