// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.handlers.scoreboard.type;

import java.util.regex.Pattern;

public record SegmentMatcher(Pattern headerPattern) {
    public static SegmentMatcher fromPattern(String pattern) {
        return new SegmentMatcher(Pattern.compile(pattern));
    }
}
