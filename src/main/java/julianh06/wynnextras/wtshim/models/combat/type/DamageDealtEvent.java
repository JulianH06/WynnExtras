// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Relocated for the WynnExtras standalone compat shim (wtshim).
 *
 * DEVIATION: this event is NEVER POSTED in the shim — there is no combat/label handler port.
 * RaidModel subscribes to it (verbatim), so per-room damage always stays 0. WynnExtras never
 * reads room damage, so this is a documented, no-op deviation.
 */
package julianh06.wynnextras.wtshim.models.combat.type;

import julianh06.wynnextras.wtshim.models.stats.type.DamageType;
import java.util.Map;
import net.neoforged.bus.api.Event;

/**
 * This event is sent out whenever Wynntils register that a mob has received damage.
 * We cannot tell which mob it is, but we know the damage is caused by melee or
 * spell attack from the current player.
 */
public final class DamageDealtEvent extends Event {
    private final Map<DamageType, Long> damages;

    public DamageDealtEvent(Map<DamageType, Long> damages) {
        this.damages = damages;
    }

    public Map<DamageType, Long> getDamages() {
        return damages;
    }
}
