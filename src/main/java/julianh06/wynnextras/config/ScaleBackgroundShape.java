package julianh06.wynnextras.config;

import com.wynntils.utils.render.Texture;

public enum ScaleBackgroundShape {
    CIRCLE(Texture.HIGHLIGHT_CIRCLE_OPAQUE),
    CIRCLE_OUTLINE_LARGE(Texture.HIGHLIGHT_CIRCLE_OUTLINE_LARGE),
    CIRCLE_OUTLINE_SMALL(Texture.HIGHLIGHT_CIRCLE_OUTLINE_SMALL),
    BOX(Texture.HIGHLIGHT_BOX_OPAQUE),
    BOX_GRADIENT_1(Texture.HIGHLIGHT_BOX_GRADIENT_1),
    BOX_GRADIENT_2(Texture.HIGHLIGHT_BOX_GRADIENT_2),
    WYNN(Texture.HIGHLIGHT_WYNN),
    TAG(Texture.HIGHLIGHT_TAG);

    private final Texture texture;

    ScaleBackgroundShape(Texture texture) {
        this.texture = texture;
    }

    public Texture texture() {
        return texture;
    }
}