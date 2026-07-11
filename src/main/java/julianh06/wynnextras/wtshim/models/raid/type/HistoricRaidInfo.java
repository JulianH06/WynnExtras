// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Verbatim port for the WynnExtras standalone compat shim (wtshim); only the package changed.
 */
package julianh06.wynnextras.wtshim.models.raid.type;

import java.util.Map;

public record HistoricRaidInfo(
        String name, String abbreviation, Map<Integer, RaidRoomInfo> challenges, long endedTimestamp) {}
