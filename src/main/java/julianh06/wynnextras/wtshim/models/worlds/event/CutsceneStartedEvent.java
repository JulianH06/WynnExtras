// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.models.worlds.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class CutsceneStartedEvent extends Event implements ICancellableEvent {
    private final boolean groupCutscene;

    public CutsceneStartedEvent(boolean groupCutscene) {
        this.groupCutscene = groupCutscene;
    }

    public boolean isGroupCutscene() {
        return groupCutscene;
    }
}
