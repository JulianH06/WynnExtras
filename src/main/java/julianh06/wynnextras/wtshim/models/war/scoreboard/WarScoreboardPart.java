// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.war.scoreboard;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardPart;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardSegment;
import julianh06.wynnextras.wtshim.handlers.scoreboard.type.SegmentMatcher;

public class WarScoreboardPart extends ScoreboardPart {
    private static final SegmentMatcher WAR_MATCHER = SegmentMatcher.fromPattern("War:");

    @Override
    public SegmentMatcher getSegmentMatcher() {
        return WAR_MATCHER;
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue) {
        Models.War.onWarStart();
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment) {
        Models.War.onWarEnd();
    }

    @Override
    public void reset() {
        Models.War.onWarEnd();
    }

    @Override
    public String toString() {
        return "WarScoreboardPart{}";
    }
}
