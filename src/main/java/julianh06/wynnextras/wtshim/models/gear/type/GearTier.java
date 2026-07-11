// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — GearTier enum. */
package julianh06.wynnextras.wtshim.models.gear.type;

import net.minecraft.util.Formatting;

public enum GearTier {
    // Source: Wynntils/common/.../models/gear/type/GearTier.java — exact ChatFormatting values.
    NORMAL(Formatting.WHITE),
    UNIQUE(Formatting.YELLOW),
    RARE(Formatting.LIGHT_PURPLE),
    /** Deprecated in Wynntils; color is GRAY, not GREEN. */
    SET(Formatting.GRAY),
    LEGENDARY(Formatting.AQUA),
    FABLED(Formatting.RED),
    MYTHIC(Formatting.DARK_PURPLE),
    CRAFTED(Formatting.DARK_AQUA);

    private final Formatting color;
    GearTier(Formatting color) { this.color = color; }
    public Formatting getChatFormatting() { return color; }
    public String getName() { return name(); }

    /** Maps a Wynncraft gear.json "tier" string (e.g. "mythic", "legendary") to a GearTier. */
    public static GearTier fromString(String tier) {
        if (tier == null) return NORMAL;
        try {
            return valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
