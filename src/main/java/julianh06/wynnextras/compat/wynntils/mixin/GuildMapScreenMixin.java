package julianh06.wynnextras.compat.wynntils.mixin;

import julianh06.wynnextras.compat.wynntils.WynntilsGuildAdapter;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.TextShadow;
import julianh06.wynnextras.utils.render.Texture;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import julianh06.wynnextras.utils.text.StyledText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.wynntils.screens.maps.GuildMapScreen", remap = false)
public class GuildMapScreenMixin {
    @Inject(method = "renderPois(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"), remap = false, require = 0)
    private void fixTradeRoutes(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (WynnExtrasConfig.INSTANCE.territoryEstimateToggle) WynntilsGuildAdapter.fixTradeRoutes();
    }

    @Inject(method = "renderTerritoryTooltip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void renderTerritoryTooltip(DrawContext context, int xOffset, int yOffset,
                                               @Coerce Object territoryPoi, CallbackInfo ci) {
        if (!WynnExtrasConfig.INSTANCE.territoryEstimateToggle) return;
        WynntilsGuildAdapter.Territory territory = WynntilsGuildAdapter.territory(territoryPoi);
        if (territory == null) return;

        int textureWidth = Texture.MAP_INFO_TOOLTIP_CENTER.width();
        int resourceLines = 0;
        for (WynntilsGuildAdapter.Resource resource : territory.resources()) {
            if (resource.generation() != 0) resourceLines++;
            if (resource.stored()) resourceLines++;
        }
        float centerHeight = 75 + resourceLines * 10 + (territory.headquarters() ? 20 : 0)
                + (territory.estimates() == null ? 0 : 20 + 10 * territory.estimates().size());

        RenderUtils.drawTexturedRect(context, Texture.MAP_INFO_TOOLTIP_TOP, xOffset, yOffset);
        RenderUtils.drawScalingTexturedRect(context, Texture.MAP_INFO_TOOLTIP_CENTER.identifier(), xOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + yOffset, textureWidth, centerHeight,
                textureWidth, Texture.MAP_INFO_TOOLTIP_CENTER.height());
        RenderUtils.drawTexturedRect(context, Texture.MAP_INFO_NAME_BOX, xOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + yOffset);

        draw(context, "%s [%s]".formatted(territory.guildName(), territory.guildPrefix()),
                10 + xOffset, 10 + yOffset, CustomColor.MAGENTA);
        float renderY = 20 + yOffset;
        for (WynntilsGuildAdapter.Resource resource : territory.resources()) {
            if (resource.generation() != 0) {
                draw(context, "%s+%d %s per Hour".formatted(resource.symbol(), resource.generation(), resource.name()),
                        10 + xOffset, 10 + renderY, CustomColor.WHITE);
                renderY += 10;
            }
            if (resource.stored()) {
                draw(context, "%s%d/%d %s stored".formatted(resource.symbol(), resource.current(), resource.max(), resource.name()),
                        10 + xOffset, 10 + renderY, CustomColor.WHITE);
                renderY += 10;
            }
        }
        renderY += 10;
        draw(context, Formatting.GRAY + "✦ Treasury: " + territory.treasury(), 10 + xOffset, 10 + renderY, CustomColor.WHITE);
        renderY += 10;
        draw(context, Formatting.GRAY + "Territory Defences: " + territory.defences(), 10 + xOffset, 10 + renderY, CustomColor.WHITE);
        if (territory.headquarters()) {
            renderY += 20;
            draw(context, "Guild Headquarters", 10 + xOffset, 10 + renderY, CustomColor.RED);
        }
        if (territory.estimates() != null) {
            renderY += 20;
            draw(context, Formatting.GRAY + "Estimated Defences: " + Formatting.DARK_GRAY + "(by @drzxm)",
                    10 + xOffset, 10 + renderY, CustomColor.WHITE);
            for (String line : territory.estimates()) {
                renderY += 10;
                draw(context, line, 10 + xOffset, 10 + renderY, CustomColor.WHITE);
            }
        }
        renderY += 20;
        draw(context, Formatting.GRAY + "Time Held: " + territory.timeHeld(), 10 + xOffset, 10 + renderY, CustomColor.WHITE);
        FontRenderer.getInstance().renderAlignedTextInBox(context, StyledText.fromString(territory.name()),
                7 + xOffset, textureWidth + xOffset, Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + yOffset,
                Texture.MAP_INFO_TOOLTIP_TOP.height() + centerHeight + Texture.MAP_INFO_NAME_BOX.height() + yOffset,
                0, CustomColor.WHITE, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.OUTLINE);
        ci.cancel();
    }

    private static void draw(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(context, StyledText.fromString(text), x, y, color,
                HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.OUTLINE);
    }
}
