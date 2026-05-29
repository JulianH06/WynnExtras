package julianh06.wynnextras.utils;

import com.wynntils.core.components.Managers;
import com.wynntils.features.inventory.ItemHighlightFeature;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import net.minecraft.client.gui.DrawContext;

public final class WynntilsHighlightUtils {
    private WynntilsHighlightUtils() {}

    public static Texture getConfiguredHighlightTexture() {
        try {
            return getConfiguredHighlightTexture(Managers.Feature.getFeatureInstance(ItemHighlightFeature.class));
        } catch (Exception ignored) {
            return Texture.HIGHLIGHT_WYNN;
        }
    }

    public static Texture getConfiguredHighlightTexture(ItemHighlightFeature itemHighlightFeature) {
        try {
            Object value = itemHighlightFeature.getConfigOptionFromString("highlightTexture").get().get();
            if (value instanceof ItemHighlightFeature.HighlightTexture highlightTexture) {
                Texture texture = highlightTexture.texture();
                if (texture != null) return texture;
            }
        } catch (Exception ignored) {}

        return Texture.HIGHLIGHT_WYNN;
    }

    public static void drawHighlightTexture(DrawContext context, Texture texture, CustomColor color, float x, float y, float width, float height) {
        Texture resolvedTexture = texture == null ? Texture.HIGHLIGHT_WYNN : texture;
        RenderUtils.drawSprite(context, resolvedTexture, color, x, y, width, height);
    }
}