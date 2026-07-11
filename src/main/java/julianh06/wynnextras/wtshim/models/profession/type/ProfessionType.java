// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ProfessionType enum. */
package julianh06.wynnextras.wtshim.models.profession.type;

public enum ProfessionType {
    MINING, WOODCUTTING, FISHING, FARMING,
    ARMOURING, WEAPONSMITHING, TAILORING, WOODWORKING,
    ALCHEMISM, COOKING, SCRIBING, JEWELING,
    COMBAT;

    public String getName() { return name(); }

    public String getDisplayName() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }

    /** Short abbreviation used in some HUD overlays (e.g. "MIN" for Mining). */
    public String getAbbreviation() {
        return name().substring(0, Math.min(3, name().length()));
    }

    public boolean isGathering() {
        return this == MINING || this == WOODCUTTING || this == FISHING || this == FARMING;
    }

    public static ProfessionType fromString(String s) {
        if (s == null) return null;
        String upper = s.trim().toUpperCase();
        for (ProfessionType t : values()) {
            if (t.name().equals(upper)) return t;
        }
        return null;
    }
}
