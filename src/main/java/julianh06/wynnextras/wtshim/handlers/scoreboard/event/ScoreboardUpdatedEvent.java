// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.handlers.scoreboard.event;

import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardPart;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardSegment;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.Collections;
import java.util.List;
import net.neoforged.bus.api.Event;

public class ScoreboardUpdatedEvent extends Event {
    private final List<Pair<ScoreboardPart, ScoreboardSegment>> scoreboardSegments;

    public ScoreboardUpdatedEvent(List<Pair<ScoreboardPart, ScoreboardSegment>> scoreboardSegments) {
        this.scoreboardSegments = scoreboardSegments;
    }

    public List<Pair<ScoreboardPart, ScoreboardSegment>> getScoreboardSegments() {
        return Collections.unmodifiableList(scoreboardSegments);
    }
}
