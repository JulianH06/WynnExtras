// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — GuildResource (faithful port; Mojmap ChatFormatting -> Yarn Formatting).
 */
package julianh06.wynnextras.wtshim.models.territories.type;

import net.minecraft.util.Formatting;

public enum GuildResource {
    EMERALDS(Formatting.GREEN, "Emeralds", ""),
    ORE(Formatting.WHITE, "Ore", "Ⓑ"),
    WOOD(Formatting.GOLD, "Wood", "Ⓒ"),
    FISH(Formatting.AQUA, "Fish", "Ⓚ"),
    CROPS(Formatting.YELLOW, "Crops", "Ⓙ");

    private final Formatting color;
    private final String name;
    private final String symbol;

    GuildResource(Formatting color, String name, String symbol) {
        this.color = color;
        this.name = name;
        this.symbol = symbol;
    }

    public static GuildResource fromName(String name) {
        for (GuildResource resource : values()) {
            if (resource.getName().equalsIgnoreCase(name)) {
                return resource;
            }
        }
        return null;
    }

    public static GuildResource fromSymbol(String symbol) {
        for (GuildResource resource : values()) {
            if (resource.getSymbol().equalsIgnoreCase(symbol)) {
                return resource;
            }
        }
        return null;
    }

    public Formatting getColor() {
        return color;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getPrettySymbol() {
        return color + symbol + (symbol.isEmpty() ? "" : " ");
    }

    public boolean isMaterialResource() {
        return this != EMERALDS;
    }
}
