// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — WynnUtils helpers. */
package julianh06.wynnextras.wtshim.utils.wynn;

public final class WynnUtils {
    private WynnUtils() {}

    public static String normalizeBadString(String s) {
        if (s == null) return "";
        // strip color codes (§x) and trim
        return s.replaceAll("§[0-9a-fk-or]", "").trim();
    }
}
