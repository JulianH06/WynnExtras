package julianh06.wynnextras.utils;

import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.stats.type.StatActualValue;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
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
            Boolean identified
    ) {
        public boolean hasFilters() {
            return minLevel != null || maxLevel != null || classType != null ||
                    (rarities != null && !rarities.isEmpty()) || profession != null ||
                    minMainScale != null || maxMainScale != null ||
                    crafted != null || type != null || slot != null ||
                    idName != null || identified != null ||
                    (textSearch != null && !textSearch.isEmpty());
        }
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile("level:(\\d+)(?:-(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("class:(warrior|mage|archer|assassin|shaman)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARITY_PATTERN = Pattern.compile("rarity:(common|unique|rare|legendary|fabled|mythic|set)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROF_PATTERN = Pattern.compile("prof:(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAINSCALE_PATTERN = Pattern.compile("@mainscale:(\\d+(?:\\.\\d+)?)(?:-(\\d+(?:\\.\\d+)?))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CRAFTED_PATTERN = Pattern.compile("crafted:(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("type:(gear|craftedgear|craftedconsumable|box|powder|potion|tome|tool|ingredient|pouch|key|horse|scroll|amplifier|charm|aspect|trinket|rune|material|insulator)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT_PATTERN = Pattern.compile("slot:(helmet|chestplate|leggings|boots|spear|dagger|bow|wand|relik|ring|bracelet|necklace|weapon|armor|accessory)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_PATTERN = Pattern.compile("id:(\\w+)(?:([><])(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFIED_PATTERN = Pattern.compile("identified:(true|false)", Pattern.CASE_INSENSITIVE);

    private static String cachedInput = null;
    private static ParsedQuery cachedQuery = null;

    public static ParsedQuery parse(String input) {
        if (input == null || input.isEmpty()) {
            return new ParsedQuery(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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
                } else {
                    maxLevel = minLevel;
                }
                if (minLevel < 0 || minLevel > 1000) minLevel = null;
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

        Boolean identified = null;
        Matcher identifiedMatcher = IDENTIFIED_PATTERN.matcher(remaining);
        if (identifiedMatcher.find()) {
            identified = identifiedMatcher.group(1).equalsIgnoreCase("true");
            remaining = remaining.replace(identifiedMatcher.group(), "").trim();
        }

        String textSearch = remaining.isEmpty() ? null : remaining;

        ParsedQuery result = new ParsedQuery(textSearch, minLevel, maxLevel, classType,
                rarities.isEmpty() ? null : rarities, profession, minMainScale, maxMainScale,
                crafted, type, slot, idName, idOp, idValue, identified);
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

        String fullLore = getLoreAsString(stack);

        if (query.textSearch != null && !query.textSearch.isEmpty()) {
            String searchLower = query.textSearch.toLowerCase();
            if (!itemName.contains(searchLower)) {
                return false;
            }
        }

        if (query.minLevel != null || query.maxLevel != null) {
            Integer itemLevel = parseLevelFromLore(fullLore);
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

        if (query.rarities != null && !query.rarities.isEmpty()) {
            String itemRarity = null;

            if (wynnItem instanceof GearItem gear) {
                GearTier tier = gear.getGearTier();
                if (tier != null) {
                    itemRarity = tier.name().toLowerCase();
                }
            }

            if (itemRarity == null) {
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

        if (query.identified != null) {
            boolean isIdentified = wynnItem instanceof GearItem;
            boolean isUnidentified = wynnItem instanceof GearBoxItem;
            if (query.identified && !isIdentified) return false;
            if (!query.identified && !isUnidentified) return false;
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

    private static boolean matchesType(WynnItem wynnItem, String type) {
        if (wynnItem == null) return false;
        return switch (type) {
            case "gear" -> wynnItem instanceof GearItem;
            case "craftedgear" -> wynnItem instanceof CraftedGearItem;
            case "craftedconsumable" -> wynnItem instanceof CraftedConsumableItem;
            case "box" -> wynnItem instanceof GearBoxItem;
            case "powder" -> wynnItem instanceof PowderItem;
            case "potion" -> wynnItem instanceof PotionItem || wynnItem instanceof MultiHealthPotionItem;
            case "tome" -> wynnItem instanceof TomeItem;
            case "tool" -> wynnItem instanceof GatheringToolItem;
            case "ingredient" -> wynnItem instanceof IngredientItem;
            case "pouch" -> wynnItem instanceof EmeraldPouchItem;
            case "key" -> wynnItem instanceof DungeonKeyItem;
            case "scroll" -> wynnItem instanceof TeleportScrollItem;
            case "amplifier" -> wynnItem instanceof AmplifierItem;
            case "charm" -> wynnItem instanceof CharmItem;
            case "aspect" -> wynnItem instanceof AspectItem;
            case "trinket" -> wynnItem instanceof TrinketItem;
            case "rune" -> wynnItem instanceof RuneItem;
            case "material" -> wynnItem instanceof MaterialItem;
            case "insulator" -> wynnItem instanceof InsulatorItem;
            default -> false;
        };
    }

    private static boolean matchesSlot(WynnItem wynnItem, String slot) {
        GearType gearType = null;
        if (wynnItem instanceof GearItem gear) gearType = gear.getGearType();
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
