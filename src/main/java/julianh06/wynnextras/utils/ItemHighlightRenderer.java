package julianh06.wynnextras.utils;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.compat.wynntils.WynntilsCompat;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ItemHighlightRenderer {
    private static final int DEFAULT_HIGHLIGHT_SIZE = 32;
    private static final Identifier HIGHLIGHT_ATLAS = Identifier.of("wynntils", "textures/ui_components/highlight.png");
    private static final Identifier CIRCLE =
            Identifier.of("wynnextras", "textures/gui/profileviewer/circle.png");
    private static boolean useHighlightAtlas = false;

    private ItemHighlightRenderer() {}

    public static boolean usesWynntilsHighlights() {
        return WynntilsCompat.isLoaded();
    }

    public static Texture getConfiguredHighlightTexture() {
        Texture texture = WynntilsBankAdapter.getConfiguredHighlightTexture();
        if (usesWynntilsHighlights()) {
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
        if (!usesWynntilsHighlights() || texture == null) {
            RenderUtils.drawTexturedRect(context, CIRCLE, color.withAlpha(0.35f), x, y, width, height, 64, 64);
            return;
        }

        if (useHighlightAtlas) {
            RenderUtils.drawTexturedRect(context, HIGHLIGHT_ATLAS, color, x, y, width, height,
                    highlightIndex(texture) * 18f, 0, 18, 18, 256, 256);
        } else {
            RenderUtils.drawSprite(context, texture, color, x, y, width, height);
        }
    }

    public static void drawHighlightTexture(DrawContext context, Texture texture, CustomColor color, float x, float y) {
        float width = texture == null ? DEFAULT_HIGHLIGHT_SIZE : texture.width();
        float height = texture == null ? DEFAULT_HIGHLIGHT_SIZE : texture.height();
        drawHighlightTexture(context, texture, color, x, y, width, height);
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
