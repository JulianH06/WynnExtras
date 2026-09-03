package julianh06.wynnextras.utils.render;

import net.minecraft.util.Identifier;

// These textures are unavailable without Wynntils installed, avoid using them
public enum Texture {
    FAVORITE_ICON("icons/generic/favorite_icon.png", 18, 18),
    WYNN_MAP_TEXTURES("map/map_borders/wynn_map_textures.png", 126, 256),
    MAP_INFO_NAME_BOX("map/map_components/map_info_name_box.png", 200, 20),
    MAP_INFO_TOOLTIP_CENTER("map/map_components/map_info_tooltip_center.png", 200, 5),
    MAP_INFO_TOOLTIP_TOP("map/map_components/map_info_tooltip_top.png", 200, 10),
    EMERALD_COUNT_BACKGROUND("ui_components/emerald_count_background.png", 24, 24),
    HIGHLIGHT_WYNN("ui_components/sprites/highlight_wynn.png", 32, 32),
    HIGHLIGHT_TAG("ui_components/sprites/highlight_tag.png", 32, 32),
    HIGHLIGHT_CIRCLE_TRANSPARENT("ui_components/sprites/highlight_circle_transparent.png", 32, 32),
    HIGHLIGHT_CIRCLE_OPAQUE("ui_components/sprites/highlight_circle_opaque.png", 32, 32),
    HIGHLIGHT_CIRCLE_OUTLINE_LARGE("ui_components/sprites/highlight_circle_outline_large.png", 32, 32),
    HIGHLIGHT_CIRCLE_OUTLINE_SMALL("ui_components/sprites/highlight_circle_outline_small.png", 32, 32),
    HIGHLIGHT_BOX_TRANSPARENT("ui_components/sprites/highlight_box_transparent.png", 32, 32),
    HIGHLIGHT_BOX_OPAQUE("ui_components/sprites/highlight_box_opaque.png", 32, 32),
    HIGHLIGHT_BOX_GRADIENT_1("ui_components/sprites/highlight_box_gradient_1.png", 32, 32),
    HIGHLIGHT_BOX_GRADIENT_2("ui_components/sprites/highlight_box_gradient_2.png", 32, 32);

    private final Identifier identifier;
    private final int width;
    private final int height;

    Texture(String path, int width, int height) {
        this.identifier = Identifier.of("wynntils", "textures/" + path);
        this.width = width;
        this.height = height;
    }

    public Identifier identifier() {
        return identifier;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
