// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — MapRenderer (phase 8b, SLIM).
 *
 * Only the members the guild-map screen needs: zoom-scale math, map-tile blit, and POI/world→screen
 * coordinate helpers. Wynntils' renderCursor (player pointer), renderChunks (MappingProgressFeature),
 * and renderLootrunLine (LootrunPathInstance + custom render pipelines) are NOT ported — those deps
 * are not contained in the fork and only the main map / lootrun screens use them.
 */
package julianh06.wynnextras.wtshim.utils.render;

import julianh06.wynnextras.wtshim.services.map.MapTexture;
import julianh06.wynnextras.wtshim.services.map.pois.Poi;
import julianh06.wynnextras.wtshim.utils.colors.CommonColors;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.type.BoundingBox;
import net.minecraft.client.gui.DrawContext;

public final class MapRenderer {
    public static final int ZOOM_LEVELS = 100;
    public static final float DEFAULT_ZOOM_LEVEL = 60;

    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 10f;
    private static final double MIN_ZOOM_LOG = Math.log(MIN_ZOOM);
    private static final double MAX_ZOOM_LOG = Math.log(MAX_ZOOM);

    private MapRenderer() {}

    public static float getZoomRenderScaleFromLevel(float zoomLevel) {
        double guiScale = McUtils.guiScale();
        double logGuiScale = Math.log(guiScale);

        double logMinZoomGuiScale = MIN_ZOOM_LOG - logGuiScale;
        double logMaxZoomGuiScale = MAX_ZOOM_LOG - logGuiScale;

        return (float) Math.exp(
                logMinZoomGuiScale + (logMaxZoomGuiScale - logMinZoomGuiScale) * (zoomLevel - 1) / (ZOOM_LEVELS - 1));
    }

    public static void renderMapTile(
            DrawContext ctx,
            MapTexture map,
            float mapCenterX,
            float mapCenterZ,
            float centerX,
            float centerZ,
            float zoomRenderScale,
            BoundingBox view) {
        float x1 = map.getX1();
        float z1 = map.getZ1();
        float x2 = map.getX2() + 1f;
        float z2 = map.getZ2() + 1f;

        float vx1 = Math.max(view.x1(), x1);
        float vz1 = Math.max(view.y1(), z1);
        float vx2 = Math.min(view.x2(), x2);
        float vz2 = Math.min(view.y2(), z2);

        if (vx1 >= vx2 || vz1 >= vz2) return;

        float sx1 = centerX + (vx1 - mapCenterX) * zoomRenderScale;
        float sy1 = centerZ + (vz1 - mapCenterZ) * zoomRenderScale;
        float sx2 = centerX + (vx2 - mapCenterX) * zoomRenderScale;
        float sy2 = centerZ + (vz2 - mapCenterZ) * zoomRenderScale;

        float u1 = (vx1 - x1);
        float v1 = (vz1 - z1);
        float u2 = (vx2 - x1);
        float v2 = (vz2 - z1);

        RenderUtils.drawTexturedRect(
                ctx,
                map.identifier(),
                CommonColors.WHITE,
                sx1,
                sy1,
                sx2 - sx1,
                sy2 - sy1,
                u1,
                v1,
                u2 - u1,
                v2 - v1,
                map.getTextureWidth(),
                map.getTextureHeight());
    }

    public static float getRenderX(Poi poi, float mapCenterX, float centerX, float currentZoom) {
        double distanceX = poi.getLocation().getX() - mapCenterX;
        return (float) (centerX + distanceX * currentZoom);
    }

    public static float getRenderZ(Poi poi, float mapCenterZ, float centerZ, float currentZoom) {
        double distanceZ = poi.getLocation().getZ() - mapCenterZ;
        return (float) (centerZ + distanceZ * currentZoom);
    }
}
