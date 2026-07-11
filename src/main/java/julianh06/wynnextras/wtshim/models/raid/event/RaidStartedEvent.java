// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.event;

import julianh06.wynnextras.wtshim.models.raid.raids.RaidKind;
import net.neoforged.bus.api.Event;

public class RaidStartedEvent extends Event {
    private final RaidKind raidKind;

    public RaidStartedEvent(RaidKind raidKind) {
        this.raidKind = raidKind;
    }

    public RaidKind getRaidKind() {
        return raidKind;
    }
}
