// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Texture enum.
 * Identifiers point at vendored Wynntils texture assets bundled under this mod's resources.
 */
package julianh06.wynnextras.wtshim.utils.render;

import net.minecraft.util.Identifier;

public enum Texture {
    HIGHLIGHT("wynnextras:textures/ui_components/highlight.png", 18, 18),
    HIGHLIGHT_WYNN("wynnextras:textures/ui_components/highlight.png", 18, 18),
    FAVORITE_ICON("wynnextras:textures/icons/generic/favorite_icon.png", 9, 9),
    EMERALD_COUNT_BACKGROUND("wynnextras:textures/ui_components/emerald_count_background.png", 128, 32),
    MAP_INFO_NAME_BOX("wynnextras:textures/map/map_components/map_info_name_box.png", 200, 20),
    MAP_INFO_TOOLTIP_CENTER("wynnextras:textures/map/map_components/map_info_tooltip_center.png", 200, 40),
    MAP_INFO_TOOLTIP_TOP("wynnextras:textures/map/map_components/map_info_tooltip_top.png", 200, 20),
    WYNN_MAP_TEXTURES("wynnextras:textures/map/map_borders/wynn_map_textures.png", 512, 512);

    private final Identifier identifier;
    private final int width;
    private final int height;

    Texture(String id, int width, int height) {
        this.identifier = Identifier.tryParse(id);
        this.width = width;
        this.height = height;
    }

    public Identifier identifier() { return identifier; }
    public int width() { return width; }
    public int height() { return height; }
}
