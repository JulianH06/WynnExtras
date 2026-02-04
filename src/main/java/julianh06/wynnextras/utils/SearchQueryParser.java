package julianh06.wynnextras.utils;

import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.character.type.ClassType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Wynntils-compatible search queries with WynnExtras extensions.
 *
 * Supported tokens:
 * - level:X or level:X-Y - Level range filter
 * - class:warrior|mage|archer|assassin|shaman - Class filter
 * - rarity:common|unique|rare|legendary|fabled|mythic - Rarity filter
 * - prof:X - Profession filter
 * - @mainscale:X or @mainscale:X-Y - WynnExtras main scale filter
 * - Any other text is treated as a name search
 */
public class SearchQueryParser {

    public record ParsedQuery(
            String textSearch,
            Integer minLevel,
            Integer maxLevel,
            String classType,
            List<String> rarities,
            String profession,
            Float minMainScale,
            Float maxMainScale
    ) {
        public boolean hasFilters() {
            return minLevel != null || maxLevel != null || classType != null ||
                    (rarities != null && !rarities.isEmpty()) || profession != null ||
                    minMainScale != null || maxMainScale != null ||
                    (textSearch != null && !textSearch.isEmpty());
        }
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile("level:(\\d+)(?:-(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("class:(warrior|mage|archer|assassin|shaman)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARITY_PATTERN = Pattern.compile("rarity:(common|unique|rare|legendary|fabled|mythic|set)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROF_PATTERN = Pattern.compile("prof:(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAINSCALE_PATTERN = Pattern.compile("@mainscale:(\\d+(?:\\.\\d+)?)(?:-(\\d+(?:\\.\\d+)?))?", Pattern.CASE_INSENSITIVE);

    public static ParsedQuery parse(String input) {
        if (input == null || input.isEmpty()) {
            return new ParsedQuery(null, null, null, null, null, null, null, null);
        }

        String remaining = input.trim();
        Integer minLevel = null, maxLevel = null;
        String classType = null;
        List<String> rarities = new ArrayList<>();
        String profession = null;
        Float minMainScale = null, maxMainScale = null;

        // Parse level filter
        Matcher levelMatcher = LEVEL_PATTERN.matcher(remaining);
        if (levelMatcher.find()) {
            try {
                minLevel = Integer.parseInt(levelMatcher.group(1));
                if (levelMatcher.group(2) != null) {
                    maxLevel = Integer.parseInt(levelMatcher.group(2));
                } else {
                    maxLevel = minLevel;
                }
                // Sanity check - levels should be reasonable (1-1000)
                if (minLevel < 0 || minLevel > 1000) minLevel = null;
                if (maxLevel != null && (maxLevel < 0 || maxLevel > 1000)) maxLevel = null;
                remaining = remaining.replace(levelMatcher.group(), "").trim();
            } catch (NumberFormatException e) {
                // Invalid number, skip this filter
                minLevel = null;
                maxLevel = null;
            }
        }

        // Parse class filter
        Matcher classMatcher = CLASS_PATTERN.matcher(remaining);
        if (classMatcher.find()) {
            classType = classMatcher.group(1).toLowerCase();
            remaining = remaining.replace(classMatcher.group(), "").trim();
        }

        // Parse rarity filter (can have multiple)
        Matcher rarityMatcher = RARITY_PATTERN.matcher(remaining);
        while (rarityMatcher.find()) {
            rarities.add(rarityMatcher.group(1).toLowerCase());
            remaining = remaining.replace(rarityMatcher.group(), "").trim();
            rarityMatcher = RARITY_PATTERN.matcher(remaining);
        }

        // Parse profession filter
        Matcher profMatcher = PROF_PATTERN.matcher(remaining);
        if (profMatcher.find()) {
            profession = profMatcher.group(1).toLowerCase();
            remaining = remaining.replace(profMatcher.group(), "").trim();
        }

        // Parse main scale filter (WynnExtras extension)
        Matcher mainscaleMatcher = MAINSCALE_PATTERN.matcher(remaining);
        if (mainscaleMatcher.find()) {
            minMainScale = Float.parseFloat(mainscaleMatcher.group(1));
            if (mainscaleMatcher.group(2) != null) {
                maxMainScale = Float.parseFloat(mainscaleMatcher.group(2));
            } else {
                maxMainScale = 100f; // Default to 100% max
            }
            remaining = remaining.replace(mainscaleMatcher.group(), "").trim();
        }

        // Remaining text is the name search
        String textSearch = remaining.isEmpty() ? null : remaining;

        return new ParsedQuery(textSearch, minLevel, maxLevel, classType,
                rarities.isEmpty() ? null : rarities, profession, minMainScale, maxMainScale);
    }

    // Patterns for parsing level from lore
    private static final Pattern LORE_LEVEL_RANGE_PATTERN = Pattern.compile("Lv\\.? ?Range:? ?§?f?(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_COMBAT_LEVEL_PATTERN = Pattern.compile("Combat Lv\\.? ?Min:? ?§?f?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_LEVEL_MIN_PATTERN = Pattern.compile("(?:Min\\.? )?Lv\\.?:? ?§?f?(\\d+)", Pattern.CASE_INSENSITIVE);

    public static boolean matches(ItemStack stack, WynnItem wynnItem, ParsedQuery query) {
        if (query == null || !query.hasFilters()) {
            return true;
        }

        // Get item name - clean up formatting codes
        String itemName = "";
        if (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.CUSTOM_NAME) != null) {
            itemName = stack.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString();
        } else if (stack.getCustomName() != null) {
            itemName = stack.getCustomName().getString();
        } else {
            itemName = stack.getName().getString();
        }
        // Remove color codes
        itemName = itemName.replaceAll("§[0-9a-fk-or]", "").toLowerCase();

        // Get lore for level/rarity checks
        String fullLore = getLoreAsString(stack);

        // Text search - only check item name (top line), not lore
        if (query.textSearch != null && !query.textSearch.isEmpty()) {
            String searchLower = query.textSearch.toLowerCase();
            if (!itemName.contains(searchLower)) {
                return false;
            }
        }

        // Level filter - parse from lore
        if (query.minLevel != null || query.maxLevel != null) {
            Integer itemLevel = parseLevelFromLore(fullLore);
            if (itemLevel == null) {
                // No level found - doesn't match level filter
                return false;
            }
            if (query.minLevel != null && itemLevel < query.minLevel) {
                return false;
            }
            if (query.maxLevel != null && itemLevel > query.maxLevel) {
                return false;
            }
        }

        // Rarity filter - check WynnItem or parse from lore
        if (query.rarities != null && !query.rarities.isEmpty()) {
            String itemRarity = null;

            // Try to get from WynnItem first
            if (wynnItem instanceof GearItem gear) {
                GearTier tier = gear.getGearTier();
                if (tier != null) {
                    itemRarity = tier.name().toLowerCase();
                }
            }

            // If not found, try parsing from lore
            if (itemRarity == null) {
                itemRarity = parseRarityFromLore(fullLore);
            }

            if (itemRarity == null) {
                // No rarity found - doesn't match rarity filter
                return false;
            }

            final String finalRarity = itemRarity;
            boolean matchesAnyRarity = query.rarities.stream()
                    .anyMatch(r -> finalRarity.contains(r));
            if (!matchesAnyRarity) {
                return false;
            }
        }

        // Main scale filter would require additional calculation
        if (query.minMainScale != null) {
            // TODO: Integrate with weight calculation system
        }

        return true;
    }

    /**
     * Get all lore lines as a single string
     */
    private static String getLoreAsString(ItemStack stack) {
        StringBuilder sb = new StringBuilder();
        if (stack.getComponents() == null) return "";

        LoreComponent loreComponent = stack.getComponents().get(DataComponentTypes.LORE);
        if (loreComponent == null) return "";

        for (Text line : loreComponent.lines()) {
            sb.append(line.getString()).append(" ");
        }
        return sb.toString();
    }

    /**
     * Parse level from item lore
     */
    private static Integer parseLevelFromLore(String lore) {
        // Try "Lv. Range: X-Y" first (unidentified items)
        Matcher rangeMatcher = LORE_LEVEL_RANGE_PATTERN.matcher(lore);
        if (rangeMatcher.find()) {
            try {
                int minLv = Integer.parseInt(rangeMatcher.group(1));
                int maxLv = Integer.parseInt(rangeMatcher.group(2));
                return (minLv + maxLv) / 2; // Use average for range
            } catch (NumberFormatException ignored) {}
        }

        // Try "Combat Lv. Min: X" (identified items)
        Matcher combatMatcher = LORE_COMBAT_LEVEL_PATTERN.matcher(lore);
        if (combatMatcher.find()) {
            try {
                return Integer.parseInt(combatMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // Try generic "Lv: X" pattern
        Matcher lvMatcher = LORE_LEVEL_MIN_PATTERN.matcher(lore);
        if (lvMatcher.find()) {
            try {
                return Integer.parseInt(lvMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    /**
     * Parse rarity from item lore (for items without WynnItem annotation)
     */
    private static String parseRarityFromLore(String lore) {
        String loreLower = lore.toLowerCase();
        if (loreLower.contains("mythic")) return "mythic";
        if (loreLower.contains("fabled")) return "fabled";
        if (loreLower.contains("legendary")) return "legendary";
        if (loreLower.contains("rare")) return "rare";
        if (loreLower.contains("unique")) return "unique";
        if (loreLower.contains("set")) return "set";
        if (loreLower.contains("common")) return "common";
        return null;
    }

    /**
     * Quick check if the search string contains any advanced filters
     */
    public static boolean hasAdvancedFilters(String input) {
        if (input == null) return false;
        return input.contains(":") || input.contains("@");
    }
}
