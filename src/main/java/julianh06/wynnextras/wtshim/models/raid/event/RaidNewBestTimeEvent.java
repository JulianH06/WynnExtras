// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.event;

import net.neoforged.bus.api.Event;

public class RaidNewBestTimeEvent extends Event {
    private final String raidName;
    private final long time;

    public RaidNewBestTimeEvent(String raidName, long time) {
        this.raidName = raidName;
        this.time = time;
    }

    public String getRaidName() {
        return raidName;
    }

    public long getTime() {
        return time;
    }
}
