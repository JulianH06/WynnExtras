// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — AbstractMapScreen (phase 8b).
 *
 * Extends vanilla Screen (NOT WEScreen) so the julianh06 GuildMapScreenMixin can subclass-inject
 * into the Wynntils hierarchy. This is a SLIM port of Wynntils' AbstractMapScreen carrying only
 * what GuildMapScreen needs: the render/pan/zoom camera, map-tile rendering, and the POI render
 * loop (renderPois 6-arg — the mixin's callback target).
 *
 * DEVIATIONS vs Wynntils (main-map/lootrun-only members stripped): no map buttons / zoom slider /
 * bordered FULLSCREEN_MAP_BORDER texture / player cursor / chunk-mapping overlay. Zoom is via mouse
 * scroll + '+/-' keys; the border is a plain outline. Field/method names + the 6-arg renderPois
 * descriptor consumed by the mixin are preserved exactly.
 */
package julianh06.wynnextras.wtshim.screens.maps;

import julianh06.wynnextras.wtshim.core.components.Services;
import julianh06.wynnextras.wtshim.services.map.MapTexture;
import julianh06.wynnextras.wtshim.services.map.pois.Poi;
import julianh06.wynnextras.wtshim.utils.colors.CommonColors;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.mc.type.PoiLocation;
import julianh06.wynnextras.wtshim.utils.render.MapRenderer;
import julianh06.wynnextras.wtshim.utils.render.RenderUtils;
import julianh06.wynnextras.wtshim.utils.type.BoundingBox;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractMapScreen extends Screen {
    protected static final float SCREEN_SIDE_OFFSET = 10;
    protected static final int MAP_CENTER_X = -360;
    protected static final int MAP_CENTER_Z = -3000;
    private static final float BORDER_OFFSET = 6;
    private static final int MAX_X = 1650;
    private static final int MAX_Z = -150;
    private static final int MIN_X = -2400;
    private static final int MIN_Z = -6600;
    private static final int CENTER_ZOOM_LEVEL = 20;

    protected boolean firstInit = true;
    protected boolean shouldCenterMap = true;
    protected boolean isPanning = false;

    protected float renderWidth;
    protected float renderHeight;
    protected float renderX;
    protected float renderY;

    protected float renderedBorderXOffset;
    protected float renderedBorderYOffset;

    protected float mapWidth;
    protected float mapHeight;
    protected float centerX;
    protected float centerZ;

    protected float mapCenterX;
    protected float mapCenterZ;

    protected float zoomLevel = MapRenderer.DEFAULT_ZOOM_LEVEL;
    protected float zoomRenderScale = MapRenderer.getZoomRenderScaleFromLevel(zoomLevel);

    protected Poi hovered = null;

    protected AbstractMapScreen() {
        super(Text.literal("Map"));
        centerMapAroundPlayer();
    }

    protected AbstractMapScreen(float mapCenterX, float mapCenterZ, float zoomLevel) {
        super(Text.literal("Map"));
        updateMapCenter(mapCenterX, mapCenterZ);
        setZoomLevel(zoomLevel);
        shouldCenterMap = false;
    }

    @Override
    protected void init() {
        renderWidth = this.width - SCREEN_SIDE_OFFSET * 2f;
        renderHeight = this.height - SCREEN_SIDE_OFFSET * 2f;
        renderX = SCREEN_SIDE_OFFSET;
        renderY = SCREEN_SIDE_OFFSET;

        renderedBorderXOffset = BORDER_OFFSET;
        renderedBorderYOffset = BORDER_OFFSET;

        mapWidth = renderWidth - renderedBorderXOffset * 2f;
        mapHeight = renderHeight - renderedBorderYOffset * 2f;
        centerX = renderX + renderedBorderXOffset + mapWidth / 2f;
        centerZ = renderY + renderedBorderYOffset + mapHeight / 2f;
    }

    protected void renderMap(DrawContext ctx) {
        ctx.enableScissor(
                (int) (renderX + renderedBorderXOffset),
                (int) (renderY + renderedBorderYOffset),
                (int) (renderX + renderedBorderXOffset + mapWidth),
                (int) (renderY + renderedBorderYOffset + mapHeight));

        // Background black void colour
        RenderUtils.drawRect(
                ctx, CommonColors.BLACK, renderX + renderedBorderXOffset, renderY + renderedBorderYOffset,
                mapWidth, mapHeight);

        BoundingBox view =
                BoundingBox.centered(mapCenterX, mapCenterZ, mapWidth / zoomRenderScale, mapHeight / zoomRenderScale);

        for (MapTexture map : Services.Map.getMapsForBoundingBox(view)) {
            MapRenderer.renderMapTile(ctx, map, mapCenterX, mapCenterZ, centerX, centerZ, zoomRenderScale, view);
        }

        ctx.disableScissor();
    }

    protected void renderMapBorder(DrawContext ctx) {
        RenderUtils.drawRectBorders(
                ctx, CommonColors.DARK_GRAY, renderX, renderY, renderX + renderWidth, renderY + renderHeight, 2);
    }

    // 6-arg render loop — the target of GuildMapScreenMixin's renderPois callback.
    // FIX (handoff bug #2): the 4th parameter is `float poiScale` (the mixin passes `1`), not an int.
    protected void renderPois(
            List<Poi> pois,
            DrawContext ctx,
            BoundingBox textureBoundingBox,
            float poiScale,
            int mouseX,
            int mouseY) {
        hovered = null;

        List<Poi> filteredPois = getRenderedPois(pois, textureBoundingBox, poiScale, mouseX, mouseY);

        // Reverse and render (hovered first in the list → drawn last / on top)
        for (int i = filteredPois.size() - 1; i >= 0; i--) {
            Poi poi = filteredPois.get(i);

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, zoomRenderScale);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, zoomRenderScale);

            poi.renderAt(ctx, poiRenderX, poiRenderZ, hovered == poi, poiScale, zoomRenderScale, zoomLevel, true);
        }
    }

    protected List<Poi> getRenderedPois(
            List<Poi> pois, BoundingBox textureBoundingBox, float poiScale, int mouseX, int mouseY) {
        List<Poi> filteredPois = new ArrayList<>();

        for (int i = pois.size() - 1; i >= 0; i--) {
            Poi poi = pois.get(i);
            PoiLocation location = poi.getLocation();
            if (location == null) continue;

            if (!poi.isVisible(zoomRenderScale, zoomLevel)) continue;

            float poiRenderX = MapRenderer.getRenderX(poi, mapCenterX, centerX, zoomRenderScale);
            float poiRenderZ = MapRenderer.getRenderZ(poi, mapCenterZ, centerZ, zoomRenderScale);

            float poiWidth = poi.getWidth(zoomRenderScale, poiScale);
            float poiHeight = poi.getHeight(zoomRenderScale, poiScale);

            BoundingBox filterBox = BoundingBox.centered(location.getX(), location.getZ(), poiWidth, poiHeight);
            BoundingBox mouseBox = BoundingBox.centered(poiRenderX, poiRenderZ, poiWidth, poiHeight);

            if (textureBoundingBox.intersects(filterBox)) {
                filteredPois.add(poi);
                if (hovered == null && mouseBox.contains(mouseX, mouseY)) {
                    hovered = poi;
                }
            }
        }

        if (hovered != null) {
            filteredPois.remove(hovered);
            filteredPois.addFirst(hovered);
        }

        return filteredPois;
    }

    @Override
    public boolean mouseDragged(Click click, double dragX, double dragY) {
        if (click.button() == 0
                && click.x() >= renderX
                && click.x() <= renderX + renderWidth
                && click.y() >= renderY
                && click.y() <= renderY + renderHeight) {
            isPanning = true;
            updateMapCenter(
                    (float) (mapCenterX - dragX / zoomRenderScale), (float) (mapCenterZ - dragY / zoomRenderScale));
            return true;
        }
        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        isPanning = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        setZoomLevel(zoomLevel + (float) (2f * verticalAmount));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) {
            setZoomLevel(zoomLevel + 2);
            return true;
        }
        if (key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) {
            setZoomLevel(zoomLevel - 2);
            return true;
        }
        return super.keyPressed(input);
    }

    protected void centerMapAroundPlayer() {
        PlayerEntity player = McUtils.player();
        if (player == null) {
            updateMapCenter(MAP_CENTER_X, MAP_CENTER_Z);
            return;
        }
        updateMapCenter((float) player.getX(), (float) player.getZ());
    }

    protected void centerMapOnWorld() {
        if (!shouldCenterMap) return;
        updateMapCenter(MAP_CENTER_X, MAP_CENTER_Z);
        setZoomLevel(CENTER_ZOOM_LEVEL);
    }

    protected boolean isPlayerInsideMainArea() {
        PlayerEntity player = McUtils.player();
        if (player == null) return false;
        int x = (int) player.getX();
        int z = (int) player.getZ();
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }

    protected void setZoomLevel(float newZoomLevel) {
        this.zoomLevel = Math.max(1, Math.min(newZoomLevel, MapRenderer.ZOOM_LEVELS));
        this.zoomRenderScale = MapRenderer.getZoomRenderScaleFromLevel(this.zoomLevel);
    }

    protected void updateMapCenter(float newX, float newZ) {
        this.mapCenterX = newX;
        this.mapCenterZ = newZ;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
