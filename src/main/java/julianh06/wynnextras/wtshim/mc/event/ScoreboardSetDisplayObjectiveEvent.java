// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ScoreboardSetDisplayObjectiveEvent extends Event implements ICancellableEvent {
    private final ScoreboardDisplaySlot slot;
    private final String objectiveName;

    public ScoreboardSetDisplayObjectiveEvent(ScoreboardDisplaySlot slot, String objectiveName) {
        this.slot = slot;
        this.objectiveName = objectiveName;
    }

    public ScoreboardDisplaySlot getSlot() {
        return slot;
    }

    public String getObjectiveName() {
        return objectiveName;
    }
}
