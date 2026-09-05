package julianh06.wynnextras.wynncraft.item;

import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.wynncraft.state.SkillPoint;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WynnItemParser {
    private record CacheKey(String itemId, int componentsHash, int count) {}

    private static final int MAX_CACHE_SIZE = 4096;
    private static final Map<CacheKey, Optional<WynnItemData>> CACHE = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Optional<WynnItemData>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    private static final Pattern LEVEL = Pattern.compile("(?i)(?:combat\\s+)?(?:lv\\.?|level)(?:\\s+min)?\\D{0,6}(\\d{1,3})");
    private static final Pattern DURABILITY = Pattern.compile("(?i)durability\\D*(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern USES = Pattern.compile("(?i)(?:uses|charges)\\D*(\\d+)(?:\\s*/\\s*\\d+)?");
    private static final Pattern POUCH = Pattern.compile("(?i)(?:emeralds?|value|capacity)\\D*(\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)");
    private static final Pattern POUCH_TIER = Pattern.compile("(?i)emerald pouch\\s*\\[tier\\s+(\\d+)]");
    private static final Pattern POUCH_VALUE = Pattern.compile("^([\\d\\s,]+)²");
    private static final Pattern CLASS = Pattern.compile("(?i)class\\s+(?:req(?:uirement)?|required|type)[^a-z\\n]*([a-z]+(?:[ /]+[a-z]+)*)");
    private static final Pattern IDENTIFICATION = Pattern.compile("^([+-]\\d+)\\s+(.+)$");

    private WynnItemParser() {}

    public static Optional<WynnItemData> parse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        try {
            CacheKey key = new CacheKey(Registries.ITEM.getId(stack.getItem()).toString(),
                    stack.getComponents().hashCode(), stack.getCount());
            synchronized (CACHE) {
                Optional<WynnItemData> cached = CACHE.get(key);
                if (cached != null) return cached;
            }
            Optional<WynnItemData> parsed = parseUncached(stack);
            synchronized (CACHE) {
                CACHE.put(key, parsed);
            }
            return parsed;
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private static Optional<WynnItemData> parseUncached(ItemStack stack) {
        String name = clean(stack.getName().getString());
        List<String> lore = lore(stack);
        if (name.isEmpty() && lore.isEmpty()) return Optional.empty();
        String all = String.join("\n", lore);
        String lower = (name + "\n" + all).toLowerCase(Locale.ROOT);

        ItemTier tier = tier(stack, lower);
        boolean crafted = tier == ItemTier.CRAFTED || lower.contains("crafted item") || lower.contains("crafted by");
        boolean unidentified = lower.contains("unidentified");
        Matcher classMatcher = CLASS.matcher(all);
        CharacterClass requiredClass = classMatcher.find() ? CharacterClass.parse(classMatcher.group(1)) : CharacterClass.UNKNOWN;
        GearType gearType = gearType(stack, lower, requiredClass);
        ItemCategory category = category(lower, tier, gearType, crafted, unidentified);
        if (category == ItemCategory.UNKNOWN && tier == ItemTier.UNKNOWN) return Optional.empty();

        EnumMap<SkillPoint, Integer> requirements = new EnumMap<>(SkillPoint.class);
        EnumMap<SkillPoint, Integer> bonuses = new EnumMap<>(SkillPoint.class);
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (String line : lore) {
            Matcher identification = IDENTIFICATION.matcher(line.trim());
            if (identification.find()) ids.put(normalize(identification.group(2)), number(identification.group(1)));
        }
        WynnTooltipParser.SkillStats styledStats = WynnTooltipParser.parseSkillStats(styledLore(stack));
        requirements.putAll(styledStats.requirements());
        bonuses.putAll(styledStats.bonuses());

        Integer level = firstNumber(LEVEL, all);
        WynnItemData.Amount durability = amount(DURABILITY, all);
        Integer uses = firstNumber(USES, all);
        WynnItemData.Amount pouch = category == ItemCategory.EMERALD_POUCH ? emeraldPouch(name, lore, all) : null;
        String profession = profession(lower);

        return Optional.of(new WynnItemData(name, category, gearType, tier, requirements, bonuses,
                durability, uses, pouch, crafted, unidentified, level, requiredClass, profession, ids, lore));
    }

    private static List<String> lore(ItemStack stack) {
        LoreComponent component = stack.get(DataComponentTypes.LORE);
        if (component == null) return List.of();
        List<String> result = new ArrayList<>(component.lines().size());
        for (Text line : component.lines()) result.add(clean(line.getString()));
        return result;
    }

    private static List<List<WynnTooltipParser.Segment>> styledLore(ItemStack stack) {
        LoreComponent component = stack.get(DataComponentTypes.LORE);
        if (component == null) return List.of();
        List<List<WynnTooltipParser.Segment>> result = new ArrayList<>(component.lines().size());
        for (Text line : component.lines()) {
            List<WynnTooltipParser.Segment> segments = new ArrayList<>();
            line.visit((style, string) -> {
                String font = null;
                if (style.getFont() instanceof StyleSpriteSource.Font(Identifier id)) font = id.toString();
                segments.add(new WynnTooltipParser.Segment(font, string));
                return Optional.empty();
            }, Style.EMPTY);
            result.add(List.copyOf(segments));
        }
        return result;
    }

    private static ItemTier tier(ItemStack stack, String text) {
        CustomModelDataComponent modelData = stack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (modelData != null) {
            for (String value : modelData.strings()) {
                if (!value.startsWith("item_tier_")) continue;
                try {
                    return ItemTier.valueOf(value.substring("item_tier_".length()).toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (text.contains("mythic")) return ItemTier.MYTHIC;
        if (text.contains("fabled")) return ItemTier.FABLED;
        if (text.contains("legendary")) return ItemTier.LEGENDARY;
        if (text.contains("unique")) return ItemTier.UNIQUE;
        if (text.contains("rare item")) return ItemTier.RARE;
        if (text.contains("set item")) return ItemTier.SET;
        if (text.contains("crafted")) return ItemTier.CRAFTED;
        if (text.contains("normal item") || text.contains("common item")) return ItemTier.NORMAL;
        return ItemTier.UNKNOWN;
    }

    private static WynnItemData.Amount emeraldPouch(String name, List<String> lore, String all) {
        Matcher tierMatcher = POUCH_TIER.matcher(name);
        if (!tierMatcher.matches()) return amount(POUCH, all);

        int tier = number(tierMatcher.group(1));
        if (tier <= 0) return null;

        int value = 0;
        if (!lore.isEmpty()) {
            Matcher valueMatcher = POUCH_VALUE.matcher(lore.getFirst());
            if (valueMatcher.find()) value = formattedNumber(valueMatcher.group(1));
        }

        int slots = switch (tier % 3) {
            case 0 -> 54;
            case 1 -> 9;
            case 2 -> 27;
            default -> 0;
        };
        long capacity = tier >= 7
                ? (long) (tier - 6) * 262_144
                : tier >= 4 ? (long) slots * 4_096 : (long) slots * 64;
        if (capacity <= 0 || capacity > Integer.MAX_VALUE) return null;
        return new WynnItemData.Amount(value, (int) capacity);
    }

    private static ItemCategory category(String text, ItemTier tier, GearType gearType, boolean crafted, boolean unidentified) {
        if (unidentified) return ItemCategory.GEAR_BOX;
        if (text.contains("emerald pouch")) return ItemCategory.EMERALD_POUCH;
        if (text.contains("crafter bag")) return ItemCategory.CRAFTER_BAG;
        if (text.contains("ingredient")) return ItemCategory.INGREDIENT;
        if (text.contains("powder")) return ItemCategory.POWDER;
        if (text.contains("tome")) return ItemCategory.TOME;
        if (text.contains("gathering tool") || text.contains(" tool")) return ItemCategory.TOOL;
        if (text.contains("dungeon key")) return ItemCategory.DUNGEON_KEY;
        if (text.contains("teleport scroll") || text.contains(" scroll")) return ItemCategory.TELEPORT_SCROLL;
        if (text.contains("horse")) return ItemCategory.HORSE;
        if (text.contains("amplifier")) return ItemCategory.AMPLIFIER;
        if (text.contains("charm")) return ItemCategory.CHARM;
        if (text.contains("trinket")) return ItemCategory.TRINKET;
        if (text.contains("rune")) return ItemCategory.RUNE;
        if (text.contains("material")) return ItemCategory.MATERIAL;
        if (text.contains("potion") || crafted && text.contains("potion")) return ItemCategory.POTION;
        if (text.contains("food") || crafted && text.contains("food")) return ItemCategory.FOOD;
        if (gearType != GearType.UNKNOWN || tier != ItemTier.UNKNOWN) return ItemCategory.GEAR;
        return ItemCategory.UNKNOWN;
    }

    private static GearType gearType(ItemStack stack, String text, CharacterClass requiredClass) {
        String itemId = Registries.ITEM.getId(stack.getItem()).getPath();
        if (itemId.endsWith("helmet") || text.contains(" helmet")) return GearType.HELMET;
        if (itemId.endsWith("chestplate") || text.contains(" chestplate")) return GearType.CHESTPLATE;
        if (itemId.endsWith("leggings") || text.contains(" leggings")) return GearType.LEGGINGS;
        if (itemId.endsWith("boots") || text.contains(" boots")) return GearType.BOOTS;
        if (text.contains(" bracelet")) return GearType.BRACELET;
        if (text.contains(" necklace")) return GearType.NECKLACE;
        if (text.contains(" ring")) return GearType.RING;

        boolean weaponStats = WynnTooltipParser.hasWeaponStats(text);
        if (weaponStats) {
            CharacterClass weaponClass = requiredClass == CharacterClass.UNKNOWN
                    ? CharacterClass.parse(text)
                    : requiredClass;
            GearType classGearType = switch (weaponClass) {
                case WARRIOR -> GearType.SPEAR;
                case MAGE -> GearType.WAND;
                case ASSASSIN -> GearType.DAGGER;
                case ARCHER -> GearType.BOW;
                case SHAMAN -> GearType.RELIK;
                default -> GearType.UNKNOWN;
            };
            if (classGearType != GearType.UNKNOWN) return classGearType;
        }

        if (text.contains(" spear")) return GearType.SPEAR;
        if (text.contains(" dagger")) return GearType.DAGGER;
        if (itemId.equals("bow") || text.contains(" bow")) return GearType.BOW;
        if (text.contains(" wand")) return GearType.WAND;
        if (text.contains(" relik")) return GearType.RELIK;
        return GearType.UNKNOWN;
    }

    private static String profession(String text) {
        for (String profession : List.of("armouring", "tailoring", "weaponsmithing", "woodworking",
                "jeweling", "alchemism", "scribing", "cooking")) {
            if (text.contains(profession)) return profession;
        }
        return null;
    }

    private static WynnItemData.Amount amount(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        int current = number(matcher.group(1));
        int maximum = number(matcher.group(2));
        return maximum <= 0 ? null : new WynnItemData.Amount(current, maximum);
    }

    private static Integer firstNumber(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? number(matcher.group(1)) : null;
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int formattedNumber(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[\\s,]", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("§[0-9a-fk-or]", "").trim();
    }
}
