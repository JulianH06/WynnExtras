// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Poi marker.
 *
 * Phase 8b: extended from the 8a data-only interface to the rendering interface Wynntils uses,
 * so AbstractMapScreen.renderPois can position + paint POIs. getName()/getTerritoryInfo() keep
 * their data-side defaults (the only implementor is TerritoryPoi; MarkerModel returns none).
 * DEVIATION: Wynntils' getDisplayPriority()/hasStaticLocation() are dropped — the fork's slim
 * renderPois neither sorts by priority nor branches on static location.
 */
package julianh06.wynnextras.wtshim.services.map.pois;

import julianh06.wynnextras.wtshim.models.territories.TerritoryInfo;
import julianh06.wynnextras.wtshim.utils.mc.type.PoiLocation;
import net.minecraft.client.gui.DrawContext;

public interface Poi {
    /** World-space center of this POI, or null if it currently has no location (skipped in rendering). */
    PoiLocation getLocation();

    void renderAt(
            DrawContext ctx,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float zoomRenderScale,
            float zoomLevel,
            boolean showLabels);

    int getWidth(float mapZoom, float scale);

    int getHeight(float mapZoom, float scale);

    default String getName() { return ""; }

    default TerritoryInfo getTerritoryInfo() { return null; }

    default boolean isVisible(float zoomRenderScale, float zoomLevel) { return true; }
}
