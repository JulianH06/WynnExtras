// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — ItemHighlightFeature stand-in. Phase 8 — mixin targets only. */
package julianh06.wynnextras.wtshim.features.inventory;

import julianh06.wynnextras.wtshim.core.consumers.features.Feature;
import julianh06.wynnextras.wtshim.core.persisted.config.Config;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.render.Texture;
import java.util.Optional;
import net.minecraft.item.ItemStack;

public class ItemHighlightFeature extends Feature {
    public enum HighlightTexture {
        NONE, NORMAL, BORDER, GLOW, SOLID, CIRCLE_TRANSPARENT;

        /** The sprite to draw for this highlight style, or null for NONE. */
        public Texture texture() {
            return this == NONE ? null : Texture.HIGHLIGHT_WYNN;
        }
    }

    public final Config<Boolean> identificationDecorations = new Config<>(true);

    public Optional<Config<?>> getConfigOptionFromString(String name) {
        return Optional.empty();
    }

    public Object getIdentificationDecorator() { return null; }

    /** Mixin target used by WynnExtras' ItemHighlightFeatureInvoker. */
    public CustomColor getHighlightColor(ItemStack stack, boolean hotbarHighlight) {
        return CustomColor.NONE;
    }
}
