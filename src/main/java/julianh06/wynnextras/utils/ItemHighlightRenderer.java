package julianh06.wynnextras.utils;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.compat.wynntils.WynntilsCompat;
import julianh06.wynnextras.config.ScaleBackgroundShape;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ItemHighlightRenderer {
    private static final Identifier HIGHLIGHT_ATLAS = Identifier.of("wynntils", "textures/ui_components/highlight.png");
    private static final Identifier CIRCLE =
            Identifier.of("wynnextras", "textures/gui/profileviewer/circle.png");
    private static Texture configuredHighlightTexture;
    private static boolean useHighlightAtlas = false;

    private ItemHighlightRenderer() {}

    public static void refreshWynntilsHighlightTexture() {
        if (!WynntilsCompat.isLoaded()) {
            configuredHighlightTexture = null;
            useHighlightAtlas = false;
            return;
        }

        configuredHighlightTexture = WynntilsBankAdapter.getConfiguredHighlightTexture();
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
        useHighlightAtlas = resources.getResource(configuredHighlightTexture.identifier()).isEmpty()
                && resources.getResource(HIGHLIGHT_ATLAS).isPresent();
    }

    public static void drawWynntilsHighlightTexture(DrawContext context, CustomColor color,
                                                    float x, float y, float width, float height) {
        if (!WynntilsCompat.isLoaded() || configuredHighlightTexture == null) return;

        if (useHighlightAtlas) {
            RenderUtils.drawTexturedRect(context, HIGHLIGHT_ATLAS, color, x, y, width, height,
                    highlightIndex(configuredHighlightTexture) * 18f, 0, 18, 18, 256, 256);
        } else {
            RenderUtils.drawSprite(context, configuredHighlightTexture, color, x, y, width, height);
        }
    }

    public static void drawStandaloneHighlight(DrawContext context, CustomColor color,
                                               float x, float y, float width, float height) {
        drawCircleTexture(context, color.withAlpha(0.35f), x, y, width, height);
    }

    public static void drawScaleBackground(DrawContext context, ScaleBackgroundShape shape, CustomColor color,
                                           float x, float y, float width, float height) {
        int left = Math.round(x);
        int top = Math.round(y);
        int pixelWidth = Math.max(1, Math.round(width));
        int pixelHeight = Math.max(1, Math.round(height));

        if (shape == ScaleBackgroundShape.BOX) {
            context.fill(left, top, left + pixelWidth, top + pixelHeight, color.asInt());
        } else {
            drawCircleTexture(context, color, left, top, pixelWidth, pixelHeight);
        }
    }

    private static void drawCircleTexture(DrawContext context, CustomColor color,
                                          float x, float y, float width, float height) {
        RenderUtils.drawTexturedRect(context, CIRCLE, color, x, y, width, height, 64, 64);
    }

    private static int highlightIndex(Texture texture) {
        return switch (texture) {
            case HIGHLIGHT_TAG -> 1;
            case HIGHLIGHT_CIRCLE_TRANSPARENT -> 2;
            case HIGHLIGHT_CIRCLE_OPAQUE -> 3;
            case HIGHLIGHT_CIRCLE_OUTLINE_LARGE -> 4;
            case HIGHLIGHT_CIRCLE_OUTLINE_SMALL -> 5;
            case HIGHLIGHT_BOX_TRANSPARENT -> 6;
            case HIGHLIGHT_BOX_OPAQUE -> 7;
            case HIGHLIGHT_BOX_GRADIENT_1 -> 8;
            case HIGHLIGHT_BOX_GRADIENT_2 -> 9;
            default -> 0;
        };
    }
}
