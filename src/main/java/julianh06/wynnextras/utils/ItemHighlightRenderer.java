package julianh06.wynnextras.utils;

import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;

public final class ItemHighlightRenderer {
    private ItemHighlightRenderer() {}

    public static Texture getConfiguredHighlightTexture() {
        return WynntilsBankAdapter.getConfiguredHighlightTexture();
    }

    public static void drawHighlightTexture(DrawContext context, Texture texture, CustomColor color,
                                            float x, float y, float width, float height) {
        Texture resolved = texture == null ? Texture.HIGHLIGHT_WYNN : texture;
        if (FabricLoader.getInstance().isModLoaded("wynntils")) {
            RenderUtils.drawSprite(context, resolved, color, x, y, width, height);
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
            case HIGHLIGHT_WYNN, HIGHLIGHT_CIRCLE_OPAQUE, HIGHLIGHT_BOX_OPAQUE,
                 HIGHLIGHT_BOX_GRADIENT_1, HIGHLIGHT_BOX_GRADIENT_2 ->
                    RenderUtils.drawRect(context, color, x, y, width, height);
            default -> {}
        }
    }
}
