// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.event;

import julianh06.wynnextras.wtshim.models.raid.type.RaidInfo;
import net.neoforged.bus.api.Event;

public abstract class RaidEndedEvent extends Event {
    private final RaidInfo raidInfo;

    protected RaidEndedEvent(RaidInfo raidInfo) {
        this.raidInfo = raidInfo;
    }

    public RaidInfo getRaid() {
        return raidInfo;
    }

    public static class Completed extends RaidEndedEvent {
        public Completed(RaidInfo raidInfo) {
            super(raidInfo);
        }
    }

    public static class Failed extends RaidEndedEvent {
        public Failed(RaidInfo raidInfo) {
            super(raidInfo);
        }
    }
}
