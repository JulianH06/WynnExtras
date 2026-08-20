package julianh06.wynnextras.wynncraft.item;

import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MountColorParser {
    public record MountColors(String primary, CustomColor primaryColor, String secondary) {}

    private record Palette(Map<String, CustomColor> primary, Set<String> secondary) {}
    private record CacheEntry(LoreComponent lore, Optional<MountColors> colors) {}

    private static final Pattern COLOR_PAIR = Pattern.compile("([a-z]+)-([a-z]+)", Pattern.CASE_INSENSITIVE);
    private static final List<Palette> PALETTES = List.of(
            new Palette(
                    Map.of(
                            "bay", color(0x5F2611),
                            "black", color(0x1E1E20),
                            "gray", color(0x353535),
                            "cherry", color(0x44120D),
                            "silver", color(0x797877),
                            "chestnut", color(0x6A3A16),
                            "beige", color(0xA68B70),
                            "tan", color(0x91724D),
                            "gold", color(0x825F33),
                            "white", color(0xB5B0AD)),
                    Set.of("argent", "night", "dawn", "reddish", "fawn", "pale", "dusk", "ash", "rich", "sable")),
            new Palette(
                    Map.of(
                            "azure", color(0x27898F),
                            "ebony", color(0x47414B),
                            "infernal", color(0x97442B),
                            "golden", color(0xC29729),
                            "cerulean", color(0x01427A),
                            "bronze", color(0x84A32F),
                            "hollow", color(0x786460),
                            "jade", color(0x315D39),
                            "fledge", color(0xA68B8D),
                            "mystic", color(0x774A8A)),
                    Set.of("cinder", "kander", "horn", "tusk", "ivory", "rose", "onyx", "quartz", "shell", "sapphire")),
            new Palette(
                    Map.of(
                            "cobalt", color(0x265B8E),
                            "crimson", color(0x773025),
                            "ash", color(0x5E5A57),
                            "dusk", color(0x354463),
                            "amber", color(0xB88F26),
                            "emerald", color(0x899247),
                            "albino", color(0xA9A29A),
                            "plum", color(0x4F326F),
                            "sable", color(0x33323B),
                            "dust", color(0xA05833)),
                    Set.of("moss", "bleach", "tawny", "sage", "blood", "raven", "misty", "rose", "royal", "maroon"))
    );
    private static final Map<ItemStack, CacheEntry> CACHE = new WeakHashMap<>();

    private MountColorParser() {}

    public static Optional<MountColors> parse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return Optional.empty();

        synchronized (CACHE) {
            CacheEntry cached = CACHE.get(stack);
            if (cached != null && cached.lore() == lore) return cached.colors();
        }

        Optional<MountColors> parsed = parseLore(lore);
        synchronized (CACHE) {
            CACHE.put(stack, new CacheEntry(lore, parsed));
        }
        return parsed;
    }

    private static Optional<MountColors> parseLore(LoreComponent lore) {
        for (Text line : lore.lines()) {
            Matcher matcher = COLOR_PAIR.matcher(line.getString().toLowerCase(Locale.ROOT));
            while (matcher.find()) {
                String primary = matcher.group(1);
                String secondary = matcher.group(2);
                for (Palette palette : PALETTES) {
                    CustomColor primaryColor = palette.primary().get(primary);
                    if (primaryColor != null && palette.secondary().contains(secondary)) {
                        return Optional.of(new MountColors(primary, primaryColor, secondary));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static CustomColor color(int rgb) {
        return CustomColor.fromInt(rgb);
    }
}
