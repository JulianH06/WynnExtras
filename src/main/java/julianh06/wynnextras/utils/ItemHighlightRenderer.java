package julianh06.wynnextras.utils;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ItemHighlightRenderer {
    private static final Identifier HIGHLIGHT_ATLAS = Identifier.of("wynntils", "textures/ui_components/highlight.png");
    private static boolean useHighlightAtlas = false;

    private ItemHighlightRenderer() {}

    public static Texture getConfiguredHighlightTexture() {
        Texture texture = WynntilsBankAdapter.getConfiguredHighlightTexture();
        if (FabricLoader.getInstance().isModLoaded("wynntils")) {
            ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
            useHighlightAtlas = resources.getResource(texture.identifier()).isEmpty()
                    && resources.getResource(HIGHLIGHT_ATLAS).isPresent();
        } else {
            useHighlightAtlas = false;
        }
        return texture;
    }

    public static void drawHighlightTexture(DrawContext context, Texture texture, CustomColor color,
                                            float x, float y, float width, float height) {
        Texture resolved = texture == null ? Texture.HIGHLIGHT_WYNN : texture;
        if (FabricLoader.getInstance().isModLoaded("wynntils")) {
            if (useHighlightAtlas) {
                RenderUtils.drawTexturedRect(context, HIGHLIGHT_ATLAS, color, x, y, width, height,
                        highlightIndex(resolved) * 18f, 0, 18, 18, 256, 256);
            } else {
                RenderUtils.drawSprite(context, resolved, color, x, y, width, height);
            }
        } else {
            drawFallback(context, resolved, color, x, y, width, height);
        }
    }

    public static void drawHighlightTexture(DrawContext context, Texture texture, CustomColor color, float x, float y) {
        Texture resolved = texture == null ? Texture.HIGHLIGHT_WYNN : texture;
        drawHighlightTexture(context, resolved, color, x, y, resolved.width(), resolved.height());
    }

    private static void drawFallback(DrawContext context, Texture texture, CustomColor color,
                                     float x, float y, float width, float height) {
        switch (texture) {
            case HIGHLIGHT_CIRCLE_OUTLINE_LARGE, HIGHLIGHT_CIRCLE_OUTLINE_SMALL, HIGHLIGHT_TAG ->
                    RenderUtils.drawRectBorders(context, color, x, y, width, height, 1);
            case HIGHLIGHT_WYNN ->
                    RenderUtils.drawRect(context, color, x + width / 4f, y + height / 4f, width / 2f, height / 2f);
            case HIGHLIGHT_CIRCLE_TRANSPARENT, HIGHLIGHT_BOX_TRANSPARENT,
                 HIGHLIGHT_CIRCLE_OPAQUE, HIGHLIGHT_BOX_OPAQUE,
                 HIGHLIGHT_BOX_GRADIENT_1, HIGHLIGHT_BOX_GRADIENT_2 ->
                    RenderUtils.drawRect(context, color, x, y, width, height);
            default -> {}
        }
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
