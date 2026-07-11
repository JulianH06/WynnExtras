// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim).
 */
package julianh06.wynnextras.wtshim.handlers.bossbar.event;

import julianh06.wynnextras.wtshim.handlers.bossbar.TrackedBar;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class BossBarAddedEvent extends Event implements ICancellableEvent {
    private final TrackedBar trackedBar;

    public BossBarAddedEvent(TrackedBar trackedBar) {
        this.trackedBar = trackedBar;
    }

    public TrackedBar getTrackedBar() {
        return trackedBar;
    }
}
