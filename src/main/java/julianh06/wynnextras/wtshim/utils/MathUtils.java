// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 *
 * Minimal subset: only the members the ported StyledText subsystem needs are
 * included. Extend faithfully from the Wynntils original if more are required.
 */
package julianh06.wynnextras.wtshim.utils;

import java.util.Locale;
import java.util.Map;

public final class MathUtils {
    // Verbatim from Wynntils MathUtils — used by RaidModel#getRaidMajorIds.
    private static final Map<Character, Integer> ROMAN_NUMERALS_MAP =
            Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);

    private MathUtils() {}

    public static boolean rangesIntersect(int aMin, int aMax, int bMin, int bMax) {
        return aMin <= bMax && bMin <= aMax;
    }

    public static int integerFromRoman(String numeral) {
        String normalized = numeral.trim()
                .toUpperCase(Locale.ROOT)
                .replace("IV", "IIII")
                .replace("IX", "VIIII")
                .replace("XL", "XXXX")
                .replace("XC", "LXXXX")
                .replace("CD", "CCCC")
                .replace("CM", "DCCCC");

        return normalized
                .chars()
                .map(c -> ROMAN_NUMERALS_MAP.getOrDefault((char) c, 0))
                .sum();
    }
}
