// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Services registry. Phase 8b adds Services.Map (guild-map tile service).
 * Fields must be public static final so WynntilsCompatInit.collectComponents() sweeps them
 * for bus registration + registerDownloads.
 */
package julianh06.wynnextras.wtshim.core.components;

import julianh06.wynnextras.wtshim.services.map.MapService;

public final class Services {
    public static final MapService Map = new MapService();

    private Services() {}
}
