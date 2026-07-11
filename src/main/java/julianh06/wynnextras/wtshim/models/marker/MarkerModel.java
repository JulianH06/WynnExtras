// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — MarkerModel stub. */
package julianh06.wynnextras.wtshim.models.marker;

import julianh06.wynnextras.wtshim.core.components.Model;

public class MarkerModel extends Model {
    /**
     * Static provider referenced by GuildMapScreenMixin for user waypoints POI stream.
     *
     * NOTE: WynnExtras ships its own waypoint system (julianh06/wynnextras/features/waypoints/)
     * which renders independently. Wynntils' MarkerModel.USER_WAYPOINTS_PROVIDER is only used by
     * their guild-map screen to paint user-created markers on the map. Since we don't mirror
     * Wynntils' user-waypoint store, we return an empty POI list — the guild map just won't
     * show Wynntils-style waypoints, which is fine because the user isn't running Wynntils.
     * WynnExtras' own waypoint rendering (world overlay, compass) is unaffected.
     */
    public static final MarkerProvider USER_WAYPOINTS_PROVIDER = new MarkerProvider();

    public static final class MarkerProvider {
        public java.util.List<julianh06.wynnextras.wtshim.services.map.pois.Poi> getPois() {
            return java.util.List.of();
        }
    }
}
