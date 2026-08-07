package julianh06.wynnextras.features.raid;

public enum WERaidKind {
    NOTG("NOG", "Nest of the Grootslangs"),
    NOL("NOL", "Orphion's Nexus of Light"),
    TCC("TCC", "The Canyon Colossus"),
    TNA("TNA", "The Nameless Anomaly"),
    TWP("WTP", "The Wartorn Palace"),
    UNKNOWN("UNKNOWN", "Unknown Raid");

    private final String abbreviation;
    private final String displayName;

    WERaidKind(String abbreviation, String displayName) {
        this.abbreviation = abbreviation;
        this.displayName = displayName;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String displayName() {
        return displayName;
    }

    public static WERaidKind from(String abbreviation, String displayName) {
        String normalized = abbreviation == null ? "" : abbreviation.toUpperCase(java.util.Locale.ROOT);
        if (normalized.equals("TWP") || normalized.equals("WTP")) return TWP;
        for (WERaidKind kind : values()) {
            if (kind.abbreviation.equals(normalized) || kind.displayName.equals(displayName)) return kind;
        }
        return UNKNOWN;
    }
}
