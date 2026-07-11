// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 * Not posted by the shim RaidModel (matches Wynntils, where it is posted from boss-bar handling
 * not ported here); included for completeness of the raid event surface.
 */
package julianh06.wynnextras.wtshim.models.raid.event;

import julianh06.wynnextras.wtshim.models.raid.raids.RaidKind;
import net.neoforged.bus.api.Event;

public class RaidBossStartedEvent extends Event {
    private final RaidKind raidKind;

    public RaidBossStartedEvent(RaidKind raidKind) {
        this.raidKind = raidKind;
    }

    public RaidKind getRaid() {
        return raidKind;
    }
}
