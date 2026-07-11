// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GuildResourceValues (faithful port; Mojmap ChatFormatting -> Yarn Formatting).
 * NOTE: getDefenceColor()/getTreasuryColor() now return Formatting (not CustomColor) to match
 * Wynntils — the guild-map tooltip concatenates them as legacy § colour codes.
 */
package julianh06.wynnextras.wtshim.models.territories.type;

import net.minecraft.util.Formatting;

public enum GuildResourceValues {
    NONE("None", Formatting.WHITE, 0),
    VERY_LOW("Very Low", Formatting.DARK_GREEN, 1),
    LOW("Low", Formatting.GREEN, 2),
    MEDIUM("Medium", Formatting.YELLOW, 3),
    HIGH("High", Formatting.RED, 4),
    VERY_HIGH("Very High", Formatting.DARK_RED, Formatting.AQUA, 5);

    private final String asString;
    private final Formatting defenceColor;
    private final Formatting treasuryColor;
    private final int level;

    GuildResourceValues(String asString, Formatting color, int level) {
        this.asString = asString;
        this.defenceColor = color;
        this.treasuryColor = color;
        this.level = level;
    }

    GuildResourceValues(String asString, Formatting defenceColor, Formatting treasuryColor, int level) {
        this.asString = asString;
        this.defenceColor = defenceColor;
        this.treasuryColor = treasuryColor;
        this.level = level;
    }

    public static GuildResourceValues fromString(String string) {
        for (GuildResourceValues value : values()) {
            if (value.asString.equals(string)) {
                return value;
            }
        }

        return null;
    }

    public String getAsString() {
        return asString;
    }

    public Formatting getDefenceColor() {
        return defenceColor;
    }

    public Formatting getTreasuryColor() {
        return treasuryColor;
    }

    public GuildResourceValues getFilterNext(boolean limited) {
        // ordinal has 1 subtracted as we are using normalValues which has NONE removed
        if (limited) {
            // If is HIGH, go back to LOW since limited
            return (ordinal() - 1) == 3
                    ? normalValues()[1]
                    : normalValues()[((ordinal() - 1) + 1) % normalValues().length];
        } else {
            return normalValues()[((ordinal() - 1) + 1) % normalValues().length];
        }
    }

    public GuildResourceValues getFilterPrevious(boolean limited) {
        // ordinal has 1 subtracted as we are using normalValues which has NONE removed
        if (limited) {
            // If is LOW, go back to HIGH since limited
            return (ordinal() - 1) == 1
                    ? normalValues()[3]
                    : normalValues()[((ordinal() - 1) + normalValues().length - 1) % normalValues().length];
        } else {
            return normalValues()[((ordinal() - 1) + normalValues().length - 1) % normalValues().length];
        }
    }

    public int getLevel() {
        return level;
    }

    // This should be used instead of values() in almost all places as the NONE value is only used for parsing
    // the rare occasion when a territory has a None treasury, but in most scenarios where values is used we are
    // only interested in these values
    public static GuildResourceValues[] normalValues() {
        return new GuildResourceValues[] {VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH};
    }
}
