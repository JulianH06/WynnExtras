package julianh06.wynnextras.utils.enums;

import java.util.Locale;

public enum WEProfessionType {
    ARMOURING("Armouring"), TAILORING("Tailoring"), WEAPONSMITHING("Weaponsmithing"),
    WOODWORKING("Woodworking"), JEWELING("Jeweling"), ALCHEMISM("Alchemism"),
    SCRIBING("Scribing"), COOKING("Cooking"), MINING("Mining"),
    WOODCUTTING("Woodcutting"), FARMING("Farming"), FISHING("Fishing");

    private final String displayName;

    WEProfessionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WEProfessionType fromString(String value) {
        if (value == null) return null;
        String normalized = value.trim().replace(" ", "").replace("_", "").toUpperCase(Locale.ROOT);
        for (WEProfessionType type : values()) {
            if (type.name().replace("_", "").equals(normalized)
                    || type.displayName.replace(" ", "").toUpperCase(Locale.ROOT).equals(normalized)) return type;
        }
        return null;
    }
}
