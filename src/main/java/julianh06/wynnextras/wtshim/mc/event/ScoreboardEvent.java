// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.mc.event;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import net.neoforged.bus.api.Event;

public abstract class ScoreboardEvent extends Event {
    private final StyledText owner;
    private final String objectiveName;

    protected ScoreboardEvent(StyledText owner, String objectiveName) {
        this.owner = owner;
        this.objectiveName = objectiveName;
    }

    public StyledText getOwner() {
        return owner;
    }

    public String getObjectiveName() {
        return objectiveName;
    }

    public static class Set extends ScoreboardEvent {
        private final int score;

        public Set(StyledText owner, String objectiveName, int score) {
            super(owner, objectiveName);
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }

    public static class Reset extends ScoreboardEvent {
        public Reset(StyledText owner, String objectiveName) {
            super(owner, objectiveName);
        }
    }
}
