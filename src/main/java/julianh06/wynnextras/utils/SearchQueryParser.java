package julianh06.wynnextras.utils;

import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.gear.type.ConsumableType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.items.properties.ClassableItemProperty;
import com.wynntils.models.items.properties.GearTierItemProperty;
import com.wynntils.models.items.properties.LeveledItemProperty;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.stats.type.StatActualValue;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchQueryParser {

    public record ParsedQuery(
            String textSearch,
            Integer minLevel,
            Integer maxLevel,
            String classType,
            List<String> rarities,
            String profession,
            Float minMainScale,
            Float maxMainScale,
            Boolean crafted,
            String type,
            String slot,
            String idName,
            String idOp,
            Integer idValue,
            Integer materialTier,
            Integer ingredientTier,
            Boolean identified,
            String mountColor
    ) {
        public boolean hasFilters() {
            return minLevel != null || maxLevel != null || classType != null ||
                    (rarities != null && !rarities.isEmpty()) || profession != null ||
                    minMainScale != null || maxMainScale != null ||
                    crafted != null || type != null || slot != null ||
                    idName != null || materialTier != null || ingredientTier != null || identified != null || mountColor != null ||
                    (textSearch != null && !textSearch.isEmpty());
        }
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile("level:(\\d+)(?:-(\\d+)|([+-]))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("class:(warrior|mage|archer|assassin|shaman)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARITY_PATTERN = Pattern.compile("rarity:(common|unique|rare|legendary|fabled|mythic|set)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROF_PATTERN = Pattern.compile("prof:(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAINSCALE_PATTERN = Pattern.compile("@mainscale:(\\d+(?:\\.\\d+)?)(?:-(\\d+(?:\\.\\d+)?))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CRAFTED_PATTERN = Pattern.compile("crafted:(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("type:(gear|box|powder|potion|food|tome|tool|ingredient|pouch|key|horse|scroll|amplifier|charm|trinket|rune|material)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT_PATTERN = Pattern.compile("slot:(helmet|chestplate|leggings|boots|spear|dagger|bow|wand|relik|ring|bracelet|necklace|weapon|armor|accessory)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_PATTERN = Pattern.compile("id:(\\w+)(?:([><])(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATERIAL_TIER_PATTERN = Pattern.compile("materialtier:([1-3])", Pattern.CASE_INSENSITIVE);
    private static final Pattern INGREDIENT_TIER_PATTERN = Pattern.compile("ingredienttier:([0-3])", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPONENT_PROFESSION_TIER_PATTERN = Pattern.compile("profession[_\\s-]*tier[_\\s-]*([0-3])", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIED_PATTERN = Pattern.compile("identified:(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOUNT_COLOR_PATTERN = Pattern.compile("mountcolor:([a-z]+(?:-[a-z]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOUNT_COLOR_LORE_PATTERN = Pattern.compile("([a-z]+)-([a-z]+)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> HORSE_PRIMARY_COLORS = Set.of(
            "cherry", "bay", "chestnut", "gold", "tan", "beige", "black", "gray", "silver", "white");
    private static final Set<String> HORSE_SECONDARY_COLORS = Set.of(
            "sable", "rich", "reddish", "dawn", "dusk", "fawn", "night", "ash", "argent", "pale");
    private static final Set<String> WYVERN_PRIMARY_COLORS = Set.of(
            "azure", "cerulean", "bronze", "ebony", "fledge", "golden", "hollow", "infernal", "jade", "mystic");
    private static final Set<String> WYVERN_SECONDARY_COLORS = Set.of(
            "cinder", "horn", "kander", "onyx", "quartz", "sapphire", "rose", "shell", "ivory", "tusk");
    private static final Set<String> ADASAUR_PRIMARY_COLORS = Set.of(
            "crimson", "dust", "amber", "emerald", "cobalt", "dusk", "plum", "sable", "ash", "albino");
    private static final Set<String> ADASAUR_SECONDARY_COLORS = Set.of(
            "blood", "rose", "tawny", "moss", "royal", "misty", "maroon", "raven", "sage", "bleach");

    private static String cachedInput = null;
    private static ParsedQuery cachedQuery = null;

    public static synchronized ParsedQuery parse(String input) {
        if (input == null || input.isEmpty()) {
            return new ParsedQuery(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        if (input.equals(cachedInput) && cachedQuery != null) {
            return cachedQuery;
        }

        String remaining = input.trim();
        Integer minLevel = null, maxLevel = null;
        String classType = null;
        List<String> rarities = new ArrayList<>();
        String profession = null;
        Float minMainScale = null, maxMainScale = null;

        Matcher levelMatcher = LEVEL_PATTERN.matcher(remaining);
        if (levelMatcher.find()) {
            try {
                minLevel = Integer.parseInt(levelMatcher.group(1));
                if (levelMatcher.group(2) != null) {
                    maxLevel = Integer.parseInt(levelMatcher.group(2));
                } else if ("+".equals(levelMatcher.group(3))) {
                    maxLevel = null;
                } else if ("-".equals(levelMatcher.group(3))) {
                    maxLevel = minLevel;
                    minLevel = null;
                } else {
                    maxLevel = minLevel;
                }
                if (minLevel != null && (minLevel < 0 || minLevel > 1000)) minLevel = null;
                if (maxLevel != null && (maxLevel < 0 || maxLevel > 1000)) maxLevel = null;
                remaining = remaining.replace(levelMatcher.group(), "").trim();
            } catch (NumberFormatException e) {
                minLevel = null;
                maxLevel = null;
            }
        }

        Matcher classMatcher = CLASS_PATTERN.matcher(remaining);
        if (classMatcher.find()) {
            classType = classMatcher.group(1).toLowerCase();
            remaining = remaining.replace(classMatcher.group(), "").trim();
        }

        Matcher rarityMatcher = RARITY_PATTERN.matcher(remaining);
        while (rarityMatcher.find()) {
            rarities.add(rarityMatcher.group(1).toLowerCase());
            remaining = remaining.replace(rarityMatcher.group(), "").trim();
            rarityMatcher = RARITY_PATTERN.matcher(remaining);
        }

        Matcher profMatcher = PROF_PATTERN.matcher(remaining);
        if (profMatcher.find()) {
            profession = profMatcher.group(1).toLowerCase();
            remaining = remaining.replace(profMatcher.group(), "").trim();
        }

        Matcher mainscaleMatcher = MAINSCALE_PATTERN.matcher(remaining);
        if (mainscaleMatcher.find()) {
            minMainScale = Float.parseFloat(mainscaleMatcher.group(1));
            if (mainscaleMatcher.group(2) != null) {
                maxMainScale = Float.parseFloat(mainscaleMatcher.group(2));
            } else {
                maxMainScale = 100f;
            }
            remaining = remaining.replace(mainscaleMatcher.group(), "").trim();
        }

        Boolean crafted = null;
        Matcher craftedMatcher = CRAFTED_PATTERN.matcher(remaining);
        if (craftedMatcher.find()) {
            crafted = craftedMatcher.group(1).equalsIgnoreCase("true");
            remaining = remaining.replace(craftedMatcher.group(), "").trim();
        }

        String type = null;
        Matcher typeMatcher = TYPE_PATTERN.matcher(remaining);
        if (typeMatcher.find()) {
            type = typeMatcher.group(1).toLowerCase();
            remaining = remaining.replace(typeMatcher.group(), "").trim();
        }

        String slot = null;
        Matcher slotMatcher = SLOT_PATTERN.matcher(remaining);
        if (slotMatcher.find()) {
            slot = slotMatcher.group(1).toLowerCase();
            remaining = remaining.replace(slotMatcher.group(), "").trim();
        }

        String idName = null;
        String idOp = null;
        Integer idValue = null;
        Matcher idMatcher = ID_PATTERN.matcher(remaining);
        if (idMatcher.find()) {
            idName = idMatcher.group(1).toLowerCase();
            if (idMatcher.group(2) != null && idMatcher.group(3) != null) {
                idOp = idMatcher.group(2);
                try { idValue = Integer.parseInt(idMatcher.group(3)); } catch (NumberFormatException ignored) {}
            }
            remaining = remaining.replace(idMatcher.group(), "").trim();
        }

        Integer materialTier = null;
        Matcher materialTierMatcher = MATERIAL_TIER_PATTERN.matcher(remaining);
        if (materialTierMatcher.find()) {
            materialTier = Integer.parseInt(materialTierMatcher.group(1));
            remaining = remaining.replace(materialTierMatcher.group(), "").trim();
        }

        Integer ingredientTier = null;
        Matcher ingredientTierMatcher = INGREDIENT_TIER_PATTERN.matcher(remaining);
        if (ingredientTierMatcher.find()) {
            ingredientTier = Integer.parseInt(ingredientTierMatcher.group(1));
            remaining = remaining.replace(ingredientTierMatcher.group(), "").trim();
        }

        Boolean identified = null;
        Matcher identifiedMatcher = IDENTIFIED_PATTERN.matcher(remaining);
        if (identifiedMatcher.find()) {
            identified = identifiedMatcher.group(1).equalsIgnoreCase("true");
            remaining = remaining.replace(identifiedMatcher.group(), "").trim();
        }

        String mountColor = null;
        Matcher mountColorMatcher = MOUNT_COLOR_PATTERN.matcher(remaining);
        if (mountColorMatcher.find()) {
            mountColor = mountColorMatcher.group(1).toLowerCase(Locale.ROOT);
            remaining = remaining.replace(mountColorMatcher.group(), "").trim();
        }

        String textSearch = remaining.isEmpty() ? null : remaining;

        ParsedQuery result = new ParsedQuery(textSearch, minLevel, maxLevel, classType,
                rarities.isEmpty() ? null : rarities, profession, minMainScale, maxMainScale,
                crafted, type, slot, idName, idOp, idValue, materialTier, ingredientTier, identified, mountColor);
        cachedInput = input;
        cachedQuery = result;
        return result;
    }

    private static final Pattern LORE_LEVEL_RANGE_PATTERN = Pattern.compile("Lv\\.? ?Range:? ?§?f?(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_COMBAT_LEVEL_PATTERN = Pattern.compile("Combat Lv\\.? ?Min:? ?§?f?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LORE_LEVEL_MIN_PATTERN = Pattern.compile("(?:Min\\.? )?Lv\\.?:? ?§?f?(\\d+)", Pattern.CASE_INSENSITIVE);

    public static boolean matches(ItemStack stack, WynnItem wynnItem, ParsedQuery query) {
        if (query == null || !query.hasFilters()) {
            return true;
        }

        String itemName = "";
        if (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.CUSTOM_NAME) != null) {
            itemName = stack.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString();
        } else if (stack.getCustomName() != null) {
            itemName = stack.getCustomName().getString();
        } else {
            itemName = stack.getName().getString();
        }
        itemName = itemName.replaceAll("§[0-9a-fk-or]", "").toLowerCase();

        if (query.textSearch != null && !query.textSearch.isEmpty()) {
            String searchLower = query.textSearch.toLowerCase();
            if (!itemName.contains(searchLower)) {
                return false;
            }
        }

        String fullLore = null;
        if (query.minLevel != null || query.maxLevel != null) {
            fullLore = getLoreAsString(stack);
            Integer itemLevel = getItemLevel(wynnItem, fullLore);
            if (itemLevel == null) {
                return false;
            }
            if (query.minLevel != null && itemLevel < query.minLevel) {
                return false;
            }
            if (query.maxLevel != null && itemLevel > query.maxLevel) {
                return false;
            }
        }

        if (query.classType != null) {
            if (!matchesClass(wynnItem, query.classType)) {
                return false;
            }
        }

        if (query.rarities != null && !query.rarities.isEmpty()) {
            String itemRarity = null;

            if (wynnItem instanceof GearTierItemProperty tierItem) {
                GearTier tier = tierItem.getGearTier();
                if (tier != null) {
                    itemRarity = tier.name().toLowerCase();
                }
            }

            if (itemRarity == null) {
                if (fullLore == null) fullLore = getLoreAsString(stack);
                itemRarity = parseRarityFromLore(fullLore);
            }

            if (itemRarity == null) {
                return false;
            }

            final String finalRarity = itemRarity;
            boolean matchesAnyRarity = query.rarities.stream()
                    .anyMatch(r -> finalRarity.contains(r));
            if (!matchesAnyRarity) {
                return false;
            }
        }

        if (query.minMainScale != null) {
            // TODO: Integrate with weight calculation system
        }

        if (query.crafted != null) {
            boolean isCrafted = wynnItem instanceof CraftedGearItem || wynnItem instanceof CraftedConsumableItem;
            if (query.crafted != isCrafted) {
                return false;
            }
        }

        if (query.profession != null) {
            if (!matchesProfession(wynnItem, query.profession)) {
                return false;
            }
        }

        if (query.type != null) {
            if (!matchesType(wynnItem, query.type)) {
                return false;
            }
        }

        if (query.slot != null) {
            if (!matchesSlot(wynnItem, query.slot)) {
                return false;
            }
        }

        if (query.idName != null) {
            if (!matchesId(wynnItem, query.idName, query.idOp, query.idValue)) {
                return false;
            }
        }

        if (query.materialTier != null) {
            Integer materialTier = getMaterialTier(stack, wynnItem);
            if (materialTier == null || !materialTier.equals(query.materialTier)) {
                return false;
            }
        }

        if (query.ingredientTier != null) {
            Integer ingredientTier = getIngredientTier(stack, wynnItem);
            if (ingredientTier == null || !ingredientTier.equals(query.ingredientTier)) {
                return false;
            }
        }

        if (query.identified != null) {
            boolean isIdentified = wynnItem instanceof GearItem;
            boolean isUnidentified = wynnItem instanceof GearBoxItem;
            if (query.identified && !isIdentified) return false;
            if (!query.identified && !isUnidentified) return false;
        }

        if (query.mountColor != null && !matchesMountColor(stack, query.mountColor)) {
            return false;
        }

        return true;
    }

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

    private static Integer getMaterialTier(ItemStack stack, WynnItem wynnItem) {
        if (wynnItem instanceof MaterialItem materialItem) {
            int tier = materialItem.getQualityTier();
            if (tier >= 1 && tier <= 3) {
                return tier;
            }
        } else if (wynnItem != null) {
            return null;
        }

        String components = String.valueOf(stack.getComponents()).toLowerCase();
        if (!components.contains("profession_material")) {
            return null;
        }
        return getComponentProfessionTier(components);
    }

    private static Integer getIngredientTier(ItemStack stack, WynnItem wynnItem) {
        if (wynnItem instanceof IngredientItem ingredientItem) {
            int tier = ingredientItem.getQualityTier();
            return tier >= 0 && tier <= 3 ? tier : null;
        } else if (wynnItem != null) {
            return null;
        }

        String components = String.valueOf(stack.getComponents()).toLowerCase();
        if (!components.contains("profession_ingredient")) {
            return null;
        }
        return getComponentProfessionTier(components);
    }

    private static Integer getComponentProfessionTier(String components) {
        Matcher matcher = COMPONENT_PROFESSION_TIER_PATTERN.matcher(components == null ? "" : components);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }
  
    private static boolean matchesMountColor(ItemStack stack, String searchedColor) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;

        for (Text line : lore.lines()) {
            Matcher matcher = MOUNT_COLOR_LORE_PATTERN.matcher(line.getString().toLowerCase(Locale.ROOT));
            while (matcher.find()) {
                String primary = matcher.group(1);
                String secondary = matcher.group(2);
                if (!isMountColorPair(primary, secondary)) continue;
                if (primary.contains(searchedColor)
                        || secondary.contains(searchedColor)
                        || (primary + "-" + secondary).contains(searchedColor)) return true;
            }
        }
        return false;
    }

    private static boolean isMountColorPair(String primary, String secondary) {
        return HORSE_PRIMARY_COLORS.contains(primary) && HORSE_SECONDARY_COLORS.contains(secondary)
                || WYVERN_PRIMARY_COLORS.contains(primary) && WYVERN_SECONDARY_COLORS.contains(secondary)
                || ADASAUR_PRIMARY_COLORS.contains(primary) && ADASAUR_SECONDARY_COLORS.contains(secondary);
    }

    private static Integer parseLevelFromLore(String lore) {
        Matcher rangeMatcher = LORE_LEVEL_RANGE_PATTERN.matcher(lore);
        if (rangeMatcher.find()) {
            try {
                int minLv = Integer.parseInt(rangeMatcher.group(1));
                int maxLv = Integer.parseInt(rangeMatcher.group(2));
                return (minLv + maxLv) / 2;
            } catch (NumberFormatException ignored) {}
        }

        Matcher combatMatcher = LORE_COMBAT_LEVEL_PATTERN.matcher(lore);
        if (combatMatcher.find()) {
            try {
                return Integer.parseInt(combatMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        Matcher lvMatcher = LORE_LEVEL_MIN_PATTERN.matcher(lore);
        if (lvMatcher.find()) {
            try {
                return Integer.parseInt(lvMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private static Integer getItemLevel(WynnItem wynnItem, String lore) {
        if (wynnItem instanceof LeveledItemProperty leveledItem) {
            int level = leveledItem.getLevel();
            if (level > 0) return level;
        }
        return parseLevelFromLore(lore);
    }

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

    private static boolean matchesClass(WynnItem wynnItem, String classType) {
        ClassType requiredClass = getRequiredClass(wynnItem);
        if (requiredClass == null || requiredClass == ClassType.NONE) return false;
        return requiredClass.name().equalsIgnoreCase(classType)
                || requiredClass.getName().equalsIgnoreCase(classType)
                || requiredClass.getFullName().equalsIgnoreCase(classType);
    }

    private static ClassType getRequiredClass(WynnItem wynnItem) {
        if (wynnItem instanceof ClassableItemProperty classableItem) return classableItem.getRequiredClass();
        if (wynnItem instanceof CharmItem charmItem) return charmItem.getRequiredClass();
        if (wynnItem instanceof TomeItem tomeItem) return tomeItem.getRequiredClass();
        if (wynnItem instanceof CraftedConsumableItem consumableItem) return consumableItem.getRequiredClass();
        return null;
    }

    /** Matches the profession query against the item's profession metadata.
     *  Currently only IngredientItem exposes profession types directly via Wynntils.
     *  Crafted items don't have a simple profession getter, so users can fall back
     *  to combining `crafted:true` with a text search for the profession name. */
    private static boolean matchesProfession(WynnItem wynnItem, String prof) {
        if (wynnItem == null || prof == null) return false;
        String needle = prof.toLowerCase();
        if (wynnItem instanceof IngredientItem ing) {
            for (ProfessionType t : ing.getProfessionTypes()) {
                if (t.name().toLowerCase().contains(needle)) return true;
            }
        }
        return false;
    }

    private static boolean matchesType(WynnItem wynnItem, String type) {
        if (wynnItem == null) return false;
        return switch (type) {
            case "gear" -> wynnItem instanceof GearItem || wynnItem instanceof CraftedGearItem;
            case "box" -> wynnItem instanceof GearBoxItem;
            case "powder" -> wynnItem instanceof PowderItem;
            case "potion" -> wynnItem instanceof PotionItem || wynnItem instanceof MultiHealthPotionItem
                    || isCraftedConsumableType(wynnItem, ConsumableType.POTION);
            case "food" -> isCraftedConsumableType(wynnItem, ConsumableType.FOOD);
            case "tome" -> wynnItem instanceof TomeItem;
            case "tool" -> wynnItem instanceof GatheringToolItem;
            case "ingredient" -> wynnItem instanceof IngredientItem;
            case "pouch" -> wynnItem instanceof EmeraldPouchItem;
            case "key" -> wynnItem instanceof DungeonKeyItem;
            case "horse" -> wynnItem instanceof MountItem;
            case "scroll" -> wynnItem instanceof TeleportScrollItem
                    || isCraftedConsumableType(wynnItem, ConsumableType.SCROLL);
            case "amplifier" -> wynnItem instanceof AmplifierItem;
            case "charm" -> wynnItem instanceof CharmItem;
            case "trinket" -> wynnItem instanceof TrinketItem;
            case "rune" -> wynnItem instanceof RuneItem;
            case "material" -> wynnItem instanceof MaterialItem;
            default -> false;
        };
    }

    private static boolean isCraftedConsumableType(WynnItem wynnItem, ConsumableType type) {
        return wynnItem instanceof CraftedConsumableItem consumableItem
                && consumableItem.getConsumableType() == type;
    }

    private static boolean matchesSlot(WynnItem wynnItem, String slot) {
        GearType gearType = null;
        if (wynnItem instanceof GearItem gear) gearType = gear.getGearType();
        else if (wynnItem instanceof CraftedGearItem gear) gearType = gear.getGearType();
        else if (wynnItem instanceof GearBoxItem box) gearType = box.getGearType();
        if (gearType == null) return false;

        return switch (slot) {
            case "helmet" -> gearType == GearType.HELMET;
            case "chestplate" -> gearType == GearType.CHESTPLATE;
            case "leggings" -> gearType == GearType.LEGGINGS;
            case "boots" -> gearType == GearType.BOOTS;
            case "spear" -> gearType == GearType.SPEAR;
            case "dagger" -> gearType == GearType.DAGGER;
            case "bow" -> gearType == GearType.BOW;
            case "wand" -> gearType == GearType.WAND;
            case "relik" -> gearType == GearType.RELIK;
            case "ring" -> gearType == GearType.RING;
            case "bracelet" -> gearType == GearType.BRACELET;
            case "necklace" -> gearType == GearType.NECKLACE;
            case "weapon" -> gearType == GearType.SPEAR || gearType == GearType.DAGGER ||
                    gearType == GearType.BOW || gearType == GearType.WAND || gearType == GearType.RELIK;
            case "armor" -> gearType == GearType.HELMET || gearType == GearType.CHESTPLATE ||
                    gearType == GearType.LEGGINGS || gearType == GearType.BOOTS;
            case "accessory" -> gearType == GearType.RING || gearType == GearType.BRACELET ||
                    gearType == GearType.NECKLACE;
            default -> false;
        };
    }

    private static boolean matchesId(WynnItem wynnItem, String idName, String op, Integer value) {
        if (!(wynnItem instanceof GearItem gear)) return false;
        List<StatActualValue> ids;
        try { ids = gear.getIdentifications(); } catch (Exception e) { return false; }
        if (ids == null || ids.isEmpty()) return false;

        for (StatActualValue stat : ids) {
            String statName = stat.statType().getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "");
            String apiName = stat.statType().getApiName().toLowerCase().replaceAll("[^a-z0-9]", "");
            String key = stat.statType().getKey().toLowerCase().replaceAll("[^a-z0-9]", "");
            String search = idName.replaceAll("[^a-z0-9]", "");

            if (statName.contains(search) || apiName.contains(search) || key.contains(search)) {
                if (op == null || value == null) return true;
                int actual = stat.value();
                if (">".equals(op)) return actual > value;
                if ("<".equals(op)) return actual < value;
            }
        }
        return false;
    }

    public static boolean hasAdvancedFilters(String input) {
        if (input == null) return false;
        return input.contains(":") || input.contains("@");
    }
}
