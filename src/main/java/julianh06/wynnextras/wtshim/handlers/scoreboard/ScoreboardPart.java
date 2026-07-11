// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.handlers.scoreboard;

import julianh06.wynnextras.wtshim.handlers.scoreboard.type.SegmentMatcher;

/**
 * This is the "segment handler" that is implemented by the different models that want to
 * take part in the scoreboard handler.
 */
public abstract class ScoreboardPart {
    public abstract SegmentMatcher getSegmentMatcher();

    public abstract void onSegmentChange(ScoreboardSegment newValue);

    public abstract void onSegmentRemove(ScoreboardSegment segment);

    public abstract void reset();

    @Override
    public abstract String toString();
}
