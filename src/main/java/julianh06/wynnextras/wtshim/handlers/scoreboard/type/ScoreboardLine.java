// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.handlers.scoreboard.type;

import julianh06.wynnextras.wtshim.core.text.StyledText;

public record ScoreboardLine(StyledText line, int score) implements Comparable<ScoreboardLine> {
    @Override
    public int compareTo(ScoreboardLine other) {
        // Negate the result because we want the highest score to be first
        return -Integer.compare(score, other.score);
    }
}
