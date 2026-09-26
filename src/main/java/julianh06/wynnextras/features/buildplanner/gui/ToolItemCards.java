package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

final class ToolItemCards {
    private ToolItemCards() {}

    static ItemInspectionCard.Embedded ingredient(TextRenderer font, CraftedItemCodec.Ingredient ingredient, int width) {
        List<ItemInspection.Row> rows = new ArrayList<>(ingredient.rows());
        if (ingredient.id() == 4000) {
            rows.add(row("Empty ingredient slot.", "", ItemInspection.Kind.NOTE));
        } else {
            rows.add(row("Crafting Lv. Min", Integer.toString(ingredient.level()), ItemInspection.Kind.INLINE));
            rows.add(row("Used in:", "", ItemInspection.Kind.TEXT));
            for (String profession : ingredient.professions()) {
                String name = profession.charAt(0) + profession.substring(1).toLowerCase(Locale.ROOT);
                rows.add(row(name, "", ItemInspection.Kind.TEXT));
            }
        }
        String name = ingredient.name() + (ingredient.id() == 4000 ? "" : " [" + "\u272B".repeat(ingredient.tier()) + "]");
        return ItemInspectionCard.embedded(font, Text.literal(name), rows, width,
                color(Integer.toString(ingredient.tier())), false);
    }

    static int color(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "1", "unique" -> 0xFFFFFF55;
            case "2", "rare" -> 0xFFFF55FF;
            case "3", "legendary" -> 0xFF55FFFF;
            case "mythic" -> 0xFFAA00AA;
            case "fabled" -> 0xFFFF5555;
            case "set" -> 0xFF55FF55;
            case "crafted" -> 0xFF00AAAA;
            default -> 0xFFAAAAAA;
        };
    }

    private static ItemInspection.Row row(String label, String value, ItemInspection.Kind kind) {
        return new ItemInspection.Row(label, value, kind, -1, false);
    }
}
