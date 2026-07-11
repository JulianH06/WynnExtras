// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.models.worlds.type;

public enum ServerRegion {
    WC,
    NA,
    SA,
    AF,
    EU,
    AS,
    AU;

    public static ServerRegion fromString(String text) {
        for (ServerRegion type : values()) {
            if (type.name().equals(text)) {
                return type;
            }
        }

        return WC;
    }
}
