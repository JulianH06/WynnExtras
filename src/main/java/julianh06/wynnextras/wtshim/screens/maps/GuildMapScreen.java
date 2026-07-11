// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GuildMapScreen (phase 8b).
 *
 * Extends AbstractMapScreen (vanilla Screen hierarchy). Field names match Wynntils' GuildMapScreen
 * so the WynnExtras GuildMapScreenAccessor @Accessor(inferred) bindings resolve:
 *   territoryDefenseFilterType / territoryDefenseFilterLevel / territoryDefenseFilterEnabled / hybridMode.
 *
 * renderPois(DrawContext,int,int) and the static renderTerritoryTooltip(DrawContext,int,int,TerritoryPoi)
 * are the HEAD-cancellable @Inject targets of GuildMapScreenMixin — when the territoryEstimate feature
 * is on, the mixin fully replaces these bodies. The bodies below run only with the feature off.
 *
 * DEVIATIONS vs Wynntils: the map filter/mode BUTTONS are dropped (no MapButton/texture port) — the
 * filter/mode fields keep their defaults; hybridMode defaults true (Wynntils' default). Season-pass /
 * account gating dropped. renderTerritoryTooltipWithFakeInfo folded into a single tooltip path.
 */
package julianh06.wynnextras.wtshim.screens.maps;

import julianh06.wynnextras.wtshim.core.components.Models;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.models.marker.MarkerModel;
import julianh06.wynnextras.wtshim.models.territories.TerritoryInfo;
import julianh06.wynnextras.wtshim.models.territories.profile.TerritoryProfile;
import julianh06.wynnextras.wtshim.models.territories.type.GuildResource;
import julianh06.wynnextras.wtshim.models.territories.type.GuildResourceValues;
import julianh06.wynnextras.wtshim.services.map.pois.Poi;
import julianh06.wynnextras.wtshim.services.map.pois.TerritoryPoi;
import julianh06.wynnextras.wtshim.services.map.type.TerritoryFilterType;
import julianh06.wynnextras.wtshim.utils.colors.CommonColors;
import julianh06.wynnextras.wtshim.utils.render.FontRenderer;
import julianh06.wynnextras.wtshim.utils.render.RenderUtils;
import julianh06.wynnextras.wtshim.utils.render.Texture;
import julianh06.wynnextras.wtshim.utils.render.type.HorizontalAlignment;
import julianh06.wynnextras.wtshim.utils.render.type.TextShadow;
import julianh06.wynnextras.wtshim.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.wtshim.utils.type.BoundingBox;
import julianh06.wynnextras.wtshim.utils.type.CappedValue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class GuildMapScreen extends AbstractMapScreen {
    // SHADOWED by WynnExtras GuildMapScreenAccessor — do not rename.
    protected TerritoryFilterType territoryDefenseFilterType = TerritoryFilterType.DEFAULT;
    protected GuildResourceValues territoryDefenseFilterLevel = GuildResourceValues.VERY_HIGH;
    protected boolean territoryDefenseFilterEnabled = false;
    protected boolean hybridMode = true;

    // Used only by the feature-off render path.
    protected TerritoryFilterType territoryTreasuryFilterType = TerritoryFilterType.DEFAULT;
    protected GuildResourceValues territoryTreasuryFilterLevel = GuildResourceValues.VERY_HIGH;
    protected boolean territoryTreasuryFilterEnabled = false;

    protected boolean resourceMode = false;
    protected boolean territoryNameMode = false;

    protected GuildMapScreen() {
        super();
    }

    protected GuildMapScreen(float mapCenterX, float mapCenterZ, float zoomLevel) {
        super(mapCenterX, mapCenterZ, zoomLevel);
    }

    public static GuildMapScreen create() {
        return new GuildMapScreen();
    }

    @Override
    protected void init() {
        super.init();
        if (firstInit) {
            if (!isPlayerInsideMainArea()) {
                centerMapOnWorld();
            }
            firstInit = false;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderMap(ctx);

        ctx.enableScissor(
                (int) (renderX + renderedBorderXOffset),
                (int) (renderY + renderedBorderYOffset),
                (int) (renderX + renderedBorderXOffset + mapWidth),
                (int) (renderY + renderedBorderYOffset + mapHeight));

        renderPois(ctx, mouseX, mouseY);

        ctx.disableScissor();

        renderMapBorder(ctx);

        renderHoveredTerritoryInfo(ctx);
    }

    public boolean isResourceMode() {
        return resourceMode;
    }

    public boolean isTerritoryNameMode() {
        return territoryNameMode;
    }

    // @Inject(HEAD, cancellable) target for GuildMapScreenMixin — descriptor
    // (Lnet/minecraft/client/gui/DrawContext;II)V. Body runs only with the feature off.
    protected void renderPois(DrawContext ctx, int mouseX, int mouseY) {
        List<TerritoryPoi> advancementPois = Models.Territory.getTerritoryPoisFromAdvancement().stream()
                .filter(this::filterDefense)
                .filter(this::filterTreasury)
                .toList();

        List<Poi> renderedPois = new ArrayList<>();

        if (hybridMode) {
            for (TerritoryPoi poi : advancementPois) {
                TerritoryProfile territoryProfile = Models.Territory.getTerritoryProfile(poi.getName());

                if (territoryProfile != null
                        && territoryProfile.getGuild().equals(poi.getTerritoryInfo().getGuildName())) {
                    renderedPois.add(poi);
                } else {
                    renderedPois.add(new TerritoryPoi(territoryProfile, poi.getTerritoryInfo()));
                }
            }
        } else {
            renderedPois.addAll(advancementPois);
        }

        MarkerModel.USER_WAYPOINTS_PROVIDER.getPois().forEach(renderedPois::add);

        renderPois(
                renderedPois,
                ctx,
                BoundingBox.centered(mapCenterX, mapCenterZ, width / zoomRenderScale, height / zoomRenderScale),
                1,
                mouseX,
                mouseY);
    }

    private void renderHoveredTerritoryInfo(DrawContext ctx) {
        if (!(hovered instanceof TerritoryPoi territoryPoi)) return;

        int xOffset = (int) (width - SCREEN_SIDE_OFFSET - 250);
        int yOffset = (int) (SCREEN_SIDE_OFFSET + 40);

        renderTerritoryTooltip(ctx, xOffset, yOffset, territoryPoi);
    }

    private boolean filterDefense(TerritoryPoi territoryArea) {
        return !territoryDefenseFilterEnabled
                || filterTerritory(
                        territoryArea,
                        territoryDefenseFilterType,
                        territoryDefenseFilterLevel,
                        area -> area.getTerritoryInfo().getDefences());
    }

    private boolean filterTreasury(TerritoryPoi territoryArea) {
        return !territoryTreasuryFilterEnabled
                || filterTerritory(
                        territoryArea,
                        territoryTreasuryFilterType,
                        territoryTreasuryFilterLevel,
                        area -> area.getTerritoryInfo().getTreasury());
    }

    private boolean filterTerritory(
            TerritoryPoi territoryArea,
            TerritoryFilterType filterType,
            GuildResourceValues filterLevel,
            Function<TerritoryPoi, GuildResourceValues> getter) {
        GuildResourceValues guildResourceValue = getter.apply(territoryArea);
        if (guildResourceValue == null) return false;

        return switch (filterType) {
            case HIGHER -> guildResourceValue.getLevel() >= filterLevel.getLevel();
            case LOWER -> guildResourceValue.getLevel() <= filterLevel.getLevel();
            case DEFAULT -> guildResourceValue.getLevel() == filterLevel.getLevel();
        };
    }

    // @Inject(HEAD, cancellable) target for GuildMapScreenMixin.
    // FIX (handoff bug #1): arg order is (DrawContext, int xOffset, int yOffset, TerritoryPoi) to match
    // the mixin inject target — NOT the old stub's (DrawContext, TerritoryPoi, int, int).
    protected static void renderTerritoryTooltip(
            DrawContext ctx, int xOffset, int yOffset, TerritoryPoi territoryPoi) {
        final TerritoryInfo territoryInfo = territoryPoi.getTerritoryInfo();
        final TerritoryProfile territoryProfile = territoryPoi.getTerritoryProfile();
        if (territoryInfo == null || territoryProfile == null) return;

        final int textureWidth = Texture.MAP_INFO_TOOLTIP_CENTER.width();

        final float centerHeight = 75
                + (territoryInfo.getStorage().size() + territoryInfo.getGenerators().size()) * 10
                + (territoryInfo.isHeadquarters() ? 20 : 0);

        RenderUtils.drawTexturedRect(ctx, Texture.MAP_INFO_TOOLTIP_TOP, xOffset, yOffset);
        RenderUtils.drawScalingTexturedRect(
                ctx,
                Texture.MAP_INFO_TOOLTIP_CENTER.identifier(),
                (float) xOffset,
                (float) (Texture.MAP_INFO_TOOLTIP_TOP.height() + yOffset),
                (float) textureWidth,
                centerHeight,
                textureWidth,
                Texture.MAP_INFO_TOOLTIP_CENTER.height());
        RenderUtils.drawTexturedRect(
                ctx,
                Texture.MAP_INFO_NAME_BOX,
                xOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + yOffset);

        FontRenderer.getInstance()
                .renderText(
                        ctx,
                        StyledText.fromString(
                                "%s [%s]".formatted(territoryInfo.getGuildName(), territoryInfo.getGuildPrefix())),
                        10 + xOffset,
                        10 + yOffset,
                        CommonColors.MAGENTA,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        float renderYOffset = 20 + yOffset;

        for (GuildResource value : GuildResource.values()) {
            int generation = territoryInfo.getGeneration(value);
            CappedValue storage = territoryInfo.getStorage(value);

            if (generation != 0) {
                StyledText formattedGenerated = StyledText.fromString(
                        "%s+%d %s per Hour".formatted(value.getPrettySymbol(), generation, value.getName()));
                FontRenderer.getInstance()
                        .renderText(
                                ctx,
                                formattedGenerated,
                                10 + xOffset,
                                10 + renderYOffset,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);
                renderYOffset += 10;
            }

            if (storage != null) {
                StyledText formattedStored = StyledText.fromString("%s%d/%d %s stored"
                        .formatted(value.getPrettySymbol(), storage.current(), storage.max(), value.getName()));
                FontRenderer.getInstance()
                        .renderText(
                                ctx,
                                formattedStored,
                                10 + xOffset,
                                10 + renderYOffset,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);
                renderYOffset += 10;
            }
        }

        renderYOffset += 10;

        StyledText treasury = StyledText.fromString(Formatting.GRAY
                + "✦ Treasury: %s"
                        .formatted(territoryInfo.getTreasury().getTreasuryColor()
                                + territoryInfo.getTreasury().getAsString()));
        StyledText defences = StyledText.fromString(Formatting.GRAY
                + "Territory Defences: %s"
                        .formatted(territoryInfo.getDefences().getDefenceColor()
                                + territoryInfo.getDefences().getAsString()));

        FontRenderer.getInstance()
                .renderText(
                        ctx,
                        treasury,
                        10 + xOffset,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);
        renderYOffset += 10;
        FontRenderer.getInstance()
                .renderText(
                        ctx,
                        defences,
                        10 + xOffset,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        if (territoryInfo.isHeadquarters()) {
            renderYOffset += 20;
            FontRenderer.getInstance()
                    .renderText(
                            ctx,
                            StyledText.fromString("Guild Headquarters"),
                            10 + xOffset,
                            10 + renderYOffset,
                            CommonColors.RED,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }

        renderYOffset += 20;

        String timeHeldString = territoryProfile.getGuild().equals(territoryInfo.getGuildName())
                ? territoryProfile.getTimeAcquiredColor() + territoryProfile.getReadableRelativeTimeAcquired()
                : "-";
        FontRenderer.getInstance()
                .renderText(
                        ctx,
                        StyledText.fromString(Formatting.GRAY + "Time Held: " + timeHeldString),
                        10 + xOffset,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        ctx,
                        StyledText.fromString(territoryPoi.getName()),
                        7 + xOffset,
                        textureWidth + xOffset,
                        Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + yOffset,
                        Texture.MAP_INFO_TOOLTIP_TOP.height()
                                + centerHeight
                                + Texture.MAP_INFO_NAME_BOX.height()
                                + yOffset,
                        0,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);
    }
}
