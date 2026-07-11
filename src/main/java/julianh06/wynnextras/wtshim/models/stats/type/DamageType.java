// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — DamageType enum. */
package julianh06.wynnextras.wtshim.models.stats.type;

import net.minecraft.util.Formatting;

public enum DamageType {
    NEUTRAL(Formatting.GOLD),
    EARTH(Formatting.DARK_GREEN),
    THUNDER(Formatting.YELLOW),
    WATER(Formatting.AQUA),
    FIRE(Formatting.RED),
    AIR(Formatting.WHITE);

    private final Formatting color;
    DamageType(Formatting color) { this.color = color; }
    public Formatting getColor() { return color; }
    public String getName() { return name(); }
}
