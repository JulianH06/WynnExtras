// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — Skill enum. */
package julianh06.wynnextras.wtshim.models.elements.type;

import net.minecraft.util.Formatting;

public enum Skill {
    STRENGTH("Strength", Formatting.DARK_GREEN),
    DEXTERITY("Dexterity", Formatting.YELLOW),
    INTELLIGENCE("Intelligence", Formatting.AQUA),
    DEFENCE("Defence", Formatting.WHITE),
    AGILITY("Agility", Formatting.LIGHT_PURPLE);

    private final String displayName;
    private final Formatting color;

    Skill(String displayName, Formatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public Formatting getColorCode() { return color; }
}
