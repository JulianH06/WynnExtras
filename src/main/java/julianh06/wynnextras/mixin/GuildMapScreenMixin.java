package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.marker.MarkerModel;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.profile.TerritoryProfile;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.screens.maps.AbstractMapScreen;
import com.wynntils.screens.maps.GuildMapScreen;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.pois.TerritoryPoi;
import com.wynntils.services.map.type.TerritoryDefenseFilterType;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.BoundingBox;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.duckInterfaces.TerritoryInfoMixinDuck;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(value = GuildMapScreen.class, remap = false)
public class GuildMapScreenMixin extends AbstractMapScreen {
    @Shadow private boolean territoryDefenseFilterEnabled = false;
    @Shadow private boolean hybridMode = true;
    @Shadow private GuildResourceValues territoryDefenseFilterLevel;
    @Shadow private TerritoryDefenseFilterType territoryDefenseFilterType;


    @Unique
    private void fixTradeRoutes(List<TerritoryPoi> advancementPois) {
        int fixes = 0;

        Map<String, TerritoryPoi> poiByName = advancementPois.stream()
                .collect(Collectors.toMap(TerritoryPoi::getName, Function.identity(), (a, b) -> a));

        for (TerritoryPoi territoryPoi : advancementPois) {
            for (String route : territoryPoi.getTerritoryInfo().getTradingRoutes()) {
                TerritoryPoi routePoi = poiByName.get(route);
                if (routePoi == null) {
                    continue;
                }

                List<String> tradeRoutes = routePoi.getTerritoryInfo().getTradingRoutes();
                if (!tradeRoutes.contains(territoryPoi.getName())) {
                    tradeRoutes.add(territoryPoi.getName());
                    fixes++;
                }
            }
        }

        if (fixes > 0) System.out.println("Applied " + fixes + " trade route fixes");
    }


    @Inject(method = "renderPois(Lnet/minecraft/client/util/math/MatrixStack;II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void renderPois(MatrixStack poseStack, int mouseX, int mouseY, CallbackInfo ci) {
        List<TerritoryPoi> advancementPois = territoryDefenseFilterEnabled
                ? Models.Territory.getFilteredTerritoryPoisFromAdvancement(
                territoryDefenseFilterLevel.getLevel(), territoryDefenseFilterType)
                : Models.Territory.getTerritoryPoisFromAdvancement();

        fixTradeRoutes(advancementPois);

        List<Poi> renderedPois = new ArrayList<>();

        if (hybridMode) {
            // We base hybrid mode on the advancement pois, it should be more consistent

            for (TerritoryPoi poi : advancementPois) {
                TerritoryProfile territoryProfile = Models.Territory.getTerritoryProfile(poi.getName());

                // If the API and advancement pois don't match, we use the API pois without advancement info
                if (territoryProfile != null
                        && territoryProfile
                        .getGuild()
                        .equals(poi.getTerritoryInfo().getGuildName())) {
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
                poseStack,
                BoundingBox.centered(mapCenterX, mapCenterZ, width / zoomRenderScale, height / zoomRenderScale),
                1,
                mouseX,
                mouseY);

        ci.cancel();
    }

    @Inject(method = "renderTerritoryTooltip", at = @At("HEAD"), remap = false, cancellable = true)
    private static void renderTerritoryTooltip(
            MatrixStack poseStack, int xOffset, int yOffset, TerritoryPoi territoryPoi, CallbackInfo ci) {
        final TerritoryInfo territoryInfo = territoryPoi.getTerritoryInfo();
        final TerritoryProfile territoryProfile = territoryPoi.getTerritoryProfile();

        final int textureWidth = Texture.MAP_INFO_TOOLTIP_CENTER.width();

        final float centerHeight = 75
                + (territoryInfo.getStorage().size()
                + territoryInfo.getGenerators().size())
                * 10
                + (territoryInfo.isHeadquarters() ? 20 : 0)
                + (((TerritoryInfoMixinDuck) territoryInfo).wynnextras$getEstimatedDefences() != null
                ? 20 + 10 * ((TerritoryInfoMixinDuck) territoryInfo).wynnextras$getEstimatedDefences().size()
                : 0);

        RenderUtils.drawTexturedRect(poseStack, Texture.MAP_INFO_TOOLTIP_TOP, xOffset, yOffset);
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.MAP_INFO_TOOLTIP_CENTER.resource(),
                xOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + yOffset,
                textureWidth,
                centerHeight,
                textureWidth,
                Texture.MAP_INFO_TOOLTIP_CENTER.height());
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.MAP_INFO_NAME_BOX,
                xOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + yOffset);

        // guild
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
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
                                poseStack,
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
                                poseStack,
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
                        poseStack,
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
                        poseStack,
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
                            poseStack,
                            StyledText.fromString("Guild Headquarters"),
                            10 + xOffset,
                            10 + renderYOffset,
                            CommonColors.RED,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
        }

        List<String> estimatedDefences = ((TerritoryInfoMixinDuck) territoryInfo).wynnextras$getEstimatedDefences();
        if (estimatedDefences != null) {
            renderYOffset += 20;
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(Formatting.GRAY + "Estimated Defences: "
                                    + Formatting.DARK_GRAY + "(@drzxm)"),
                            10 + xOffset,
                            10 + renderYOffset,
                            CommonColors.WHITE,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.OUTLINE);
            for (String line : estimatedDefences) {
                renderYOffset += 10;
                FontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                StyledText.fromString(line),
                                10 + xOffset,
                                10 + renderYOffset,
                                CommonColors.WHITE,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);
            }
        }

        renderYOffset += 20;

        String timeHeldString = territoryProfile.getGuild().equals(territoryInfo.getGuildName())
                ? territoryProfile.getTimeAcquiredColor() + territoryProfile.getReadableRelativeTimeAcquired()
                : "-";
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(Formatting.GRAY + "Time Held: " + timeHeldString),
                        10 + xOffset,
                        10 + renderYOffset,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.OUTLINE);

        // Territory name
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
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

        ci.cancel();
    }
}
