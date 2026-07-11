// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — TerritoryPoi.
 *
 * Phase 8b: the 8a data-shaped POI is EXTENDED with the render side (getLocation/getWidth/
 * getHeight/renderAt) rather than replaced — the profile is still supplier-backed and refreshes
 * with TerritoryModel's poll. Location/size are computed live from the current profile and are
 * null/zero when no profile is loaded yet (AbstractMapScreen skips null-location POIs, matching
 * Wynntils' guard) so an un-downloaded map never crashes.
 *
 * DEVIATIONS vs Wynntils' renderAt: no Models.Guild.getColor (no GuildModel in the fork — guild
 * colour falls back to a stable hash of the guild name) and no Models.GuildAttackTimer overlay
 * (no attack-timer model). Resource-mode colouring + HQ text label are kept.
 */
package julianh06.wynnextras.wtshim.services.map.pois;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.models.territories.TerritoryInfo;
import julianh06.wynnextras.wtshim.models.territories.profile.TerritoryProfile;
import julianh06.wynnextras.wtshim.screens.maps.GuildMapScreen;
import julianh06.wynnextras.wtshim.utils.colors.CommonColors;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.mc.type.PoiLocation;
import julianh06.wynnextras.wtshim.utils.render.FontRenderer;
import julianh06.wynnextras.wtshim.utils.render.RenderUtils;
import julianh06.wynnextras.wtshim.utils.render.type.HorizontalAlignment;
import julianh06.wynnextras.wtshim.utils.render.type.TextShadow;
import julianh06.wynnextras.wtshim.utils.render.type.VerticalAlignment;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;

public class TerritoryPoi implements Poi {
    private final Supplier<TerritoryProfile> profileSupplier;
    private final TerritoryInfo info;

    public TerritoryPoi() {
        this(() -> null, null);
    }

    public TerritoryPoi(TerritoryProfile profile, TerritoryInfo info) {
        this(() -> profile, info);
    }

    public TerritoryPoi(Supplier<TerritoryProfile> profileSupplier, TerritoryInfo info) {
        this.profileSupplier = profileSupplier;
        this.info = info;
    }

    public TerritoryProfile getTerritoryProfile() {
        return profileSupplier == null ? null : profileSupplier.get();
    }

    @Override
    public TerritoryInfo getTerritoryInfo() {
        return info;
    }

    @Override
    public String getName() {
        TerritoryProfile profile = getTerritoryProfile();
        return profile != null ? profile.getName() : "";
    }

    @Override
    public PoiLocation getLocation() {
        TerritoryProfile p = getTerritoryProfile();
        if (p == null) return null;
        int w = p.getEndX() - p.getStartX();
        int h = p.getEndZ() - p.getStartZ();
        return new PoiLocation(p.getStartX() + w / 2, null, p.getStartZ() + h / 2);
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        TerritoryProfile p = getTerritoryProfile();
        return p == null ? 0 : (int) ((p.getEndX() - p.getStartX()) * mapZoom);
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        TerritoryProfile p = getTerritoryProfile();
        return p == null ? 0 : (int) ((p.getEndZ() - p.getStartZ()) * mapZoom);
    }

    @Override
    public boolean isVisible(float zoomRenderScale, float zoomLevel) {
        return getTerritoryProfile() != null;
    }

    @Override
    public void renderAt(
            DrawContext ctx,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float zoomRenderScale,
            float zoomLevel,
            boolean showLabels) {
        TerritoryProfile profile = getTerritoryProfile();
        if (profile == null) return;

        final float renderWidth = (profile.getEndX() - profile.getStartX()) * zoomRenderScale;
        final float renderHeight = (profile.getEndZ() - profile.getStartZ()) * zoomRenderScale;
        final float actualRenderX = renderX - renderWidth / 2f;
        final float actualRenderZ = renderY - renderHeight / 2f;

        CustomColor color = resolveColor(profile);

        RenderUtils.drawRect(ctx, color.withAlpha(80), actualRenderX, actualRenderZ, renderWidth, renderHeight);
        RenderUtils.drawRectBorders(
                ctx, color, actualRenderX, actualRenderZ, actualRenderX + renderWidth, actualRenderZ + renderHeight, 1);

        String mapText;
        boolean nameMode = McUtils.mc().currentScreen instanceof GuildMapScreen g && g.isTerritoryNameMode();
        if (info != null && info.isHeadquarters()) {
            mapText = "[HQ] " + (info.getGuildPrefix() != null ? info.getGuildPrefix() : "");
        } else if (nameMode) {
            mapText = Arrays.stream(profile.getName().split(" "))
                    .map(s -> s.isEmpty() ? "" : s.substring(0, 1))
                    .collect(Collectors.joining());
        } else {
            mapText = info != null ? info.getGuildPrefix() : profile.getGuildPrefix();
        }

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        ctx,
                        StyledText.fromString(mapText == null ? "" : mapText),
                        actualRenderX,
                        actualRenderX + renderWidth,
                        actualRenderZ,
                        actualRenderZ + renderHeight,
                        0,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);

        if (hovered) {
            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            ctx,
                            StyledText.fromString(profile.getFriendlyName()),
                            actualRenderX,
                            actualRenderX + renderWidth,
                            actualRenderZ,
                            actualRenderZ + renderHeight,
                            0,
                            CommonColors.WHITE,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }
    }

    private CustomColor resolveColor(TerritoryProfile profile) {
        if (info != null
                && McUtils.mc().currentScreen instanceof GuildMapScreen g
                && g.isResourceMode()) {
            List<CustomColor> resourceColors = info.getResourceColors();
            if (resourceColors != null && !resourceColors.isEmpty()) {
                return resourceColors.get(0);
            }
        }

        if (profile.getGuildInfo() != null && profile.getGuildInfo().color().isPresent()) {
            return profile.getGuildInfo().color().get();
        }

        // Deviation: no Models.Guild.getColor — derive a stable colour from the guild name.
        String guild = info != null && info.getGuildName() != null ? info.getGuildName() : profile.getGuild();
        return colorFromString(guild);
    }

    private static CustomColor colorFromString(String s) {
        if (s == null || s.isEmpty()) return CommonColors.WHITE;
        int hash = s.hashCode();
        // Bias each channel up so guild rectangles stay readable on the dark map.
        int r = 96 + ((hash >> 16) & 0x7F);
        int g = 96 + ((hash >> 8) & 0x7F);
        int b = 96 + (hash & 0x7F);
        return new CustomColor(r, g, b, 255);
    }
}
