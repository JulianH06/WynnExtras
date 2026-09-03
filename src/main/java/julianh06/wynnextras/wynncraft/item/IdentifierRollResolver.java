package julianh06.wynnextras.wynncraft.item;

import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IdentifierRollResolver {
    private static final Pattern LEVEL_RANGE = Pattern.compile("(?i)(\\d{1,3})\\s*-\\s*(\\d{1,3})\\s+Level Range");
    private static final Pattern NAME_THEN_VALUE = Pattern.compile(
            "^\\s*([\\p{L}\\d][\\p{L}\\d .'-]*?)\\s+([+-][\\d,]+)\\s*(%|tier|/[35]s)?(?:\\s.*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_THEN_NAME = Pattern.compile(
            "^\\s*([+-][\\d,]+)\\s*(%|tier|/[35]s)?\\s+([\\p{L}\\d][\\p{L}\\d .'-]*?)(?:\\s.*)?$",
            Pattern.CASE_INSENSITIVE);

    private record ActualStat(String name, String unit, int value) {}
    private record CandidateScore(WynnDataService.ItemData item, double penalty, int matches) {}

    private IdentifierRollResolver() {}

    public static List<String> possibleItemNames(ItemStack stack) {
        WynnItemData input = WynnItemParser.parse(stack).orElse(null);
        String fallbackName = cleanDisplayName(stack.getName().getString());
        if (input == null || input.category() != ItemCategory.GEAR_BOX) return List.of(fallbackName);

        int[] levelRange = levelRange(input.lore());
        WynnDataService.WynnDataSnapshot snapshot = WynnDataService.getInstance().snapshot();
        if (levelRange == null || snapshot == null) return List.of(fallbackName);

        String subType = input.gearType().name().toLowerCase(Locale.ROOT);
        String tier = input.tier().name().toLowerCase(Locale.ROOT);
        List<String> names = snapshot.items().stream()
                .filter(item -> subType.equals(normalized(item.subType())))
                .filter(item -> tier.equals(normalized(item.tier())))
                .filter(item -> isBoxEligible(item, levelRange[0], levelRange[1]))
                .map(WynnDataService.ItemData::displayName)
                .map(IdentifierRollResolver::cleanDisplayName)
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return names.isEmpty() ? List.of(fallbackName) : names;
    }

    public static OptionalDouble overallPercentage(ItemStack stack) {
        Map<String, ActualStat> actualStats = actualStats(stack);
        if (actualStats.isEmpty()) return OptionalDouble.empty();
        WynnDataService.ItemData item = selectItemData(findItemData(stack), actualStats);
        if (item == null) return OptionalDouble.empty();

        double sum = 0;
        int count = 0;
        Set<String> countedStats = new HashSet<>();
        for (Map<String, WynnDataService.StatValue> ranges : rollableRanges(item)) {
            for (Map.Entry<String, WynnDataService.StatValue> entry : ranges.entrySet()) {
                WynnDataService.StatValue possible = entry.getValue();
                String statName = normalized(entry.getKey());
                if (!possible.isRange() || possible.minimum().equals(possible.maximum())
                        || countedStats.contains(statName)) continue;
                ActualStat actual = findActualStat(entry.getKey(), actualStats);
                if (actual == null) continue;
                countedStats.add(statName);

                double percentage = (actual.value() - possible.minimum())
                        / (possible.maximum() - possible.minimum()) * 100d;
                if (isInvertedRoll(entry.getKey(), actual)) percentage = 100d - percentage;
                sum += Math.clamp(percentage, 0d, 100d);
                count++;
            }
        }
        return count == 0 ? OptionalDouble.empty() : OptionalDouble.of(sum / count);
    }

    public static String cleanDisplayName(String value) {
        String stripped = Formatting.strip(value == null ? "" : value);
        if (stripped == null) return "";
        return stripped.replace("À", "")
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("(?i)^\\s*Shiny\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static List<WynnDataService.ItemData> findItemData(ItemStack stack) {
        String name = cleanDisplayName(stack.getName().getString());
        List<WynnDataService.ItemData> matches = WynnDataService.getInstance().findByDisplayName(name);
        if (!matches.isEmpty()) return matches;

        WynnDataService.WynnDataSnapshot snapshot = WynnDataService.getInstance().snapshot();
        if (snapshot == null) return List.of();
        String normalizedName = normalized(name.replaceAll("(?i)\\s*\\[shiny]$", ""));
        return snapshot.items().stream()
                .filter(item -> normalized(item.displayName()).equals(normalizedName))
                .toList();
    }

    private static WynnDataService.ItemData selectItemData(
            List<WynnDataService.ItemData> candidates, Map<String, ActualStat> actualStats) {
        CandidateScore best = null;
        for (WynnDataService.ItemData candidate : candidates) {
            double penalty = 0;
            int matches = 0;
            Set<String> matchedStats = new HashSet<>();
            for (Map<String, WynnDataService.StatValue> ranges : rollableRanges(candidate)) {
                for (Map.Entry<String, WynnDataService.StatValue> entry : ranges.entrySet()) {
                    WynnDataService.StatValue range = entry.getValue();
                    String statName = normalized(entry.getKey());
                    if (!range.isRange() || matchedStats.contains(statName)) continue;
                    ActualStat actual = findActualStat(entry.getKey(), actualStats);
                    if (actual == null) continue;
                    matchedStats.add(statName);
                    matches++;
                    double low = Math.min(range.minimum(), range.maximum());
                    double high = Math.max(range.minimum(), range.maximum());
                    double span = Math.max(1d, high - low);
                    if (actual.value() < low) penalty += (low - actual.value()) / span;
                    else if (actual.value() > high) penalty += (actual.value() - high) / span;
                }
            }
            CandidateScore score = new CandidateScore(candidate, penalty, matches);
            if (best == null || score.penalty() < best.penalty()
                    || Double.compare(score.penalty(), best.penalty()) == 0 && score.matches() > best.matches()) {
                best = score;
            }
        }
        return best == null || best.matches() == 0 ? null : best.item();
    }

    private static Map<String, ActualStat> actualStats(ItemStack stack) {
        Map<String, ActualStat> result = new LinkedHashMap<>();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return result;
        for (Text text : lore.lines()) {
            String[] parsed = WeightDisplay.extractStatFromLine(text.getString());
            String name;
            String unit;
            String value;
            if (parsed != null) {
                name = WeightDisplay.resolveIdentKey(parsed[0], parsed[1])[0];
                value = parsed[1];
                unit = value.endsWith("%") ? "%" : value.contains("/") ? value.substring(value.indexOf('/')) : "";
            } else {
                String line = cleanTooltipLine(text.getString());
                Matcher matcher = NAME_THEN_VALUE.matcher(line);
                if (matcher.matches()) {
                    name = matcher.group(1);
                    value = matcher.group(2);
                    unit = matcher.group(3);
                } else {
                    matcher = VALUE_THEN_NAME.matcher(line);
                    if (!matcher.matches()) continue;
                    value = matcher.group(1);
                    unit = matcher.group(2);
                    name = matcher.group(3);
                }
            }
            try {
                Matcher number = Pattern.compile("[+-]?[\\d,]+").matcher(value);
                if (!number.find()) continue;
                ActualStat stat = new ActualStat(normalized(name), unit == null ? "" : unit.toLowerCase(Locale.ROOT),
                        Integer.parseInt(number.group().replace(",", "")));
                result.put(stat.name() + stat.unit(), stat);
            } catch (NumberFormatException ignored) {}
        }
        WynnItemParser.parse(stack).ifPresent(item -> item.identifications().forEach((name, value) -> {
            ActualStat stat = new ActualStat(normalized(name), "", value);
            result.putIfAbsent(stat.name(), stat);
        }));
        return result;
    }

    private static ActualStat findActualStat(String apiName, Map<String, ActualStat> actualStats) {
        String normalizedApiName = normalized(apiName);
        if (!normalizedApiName.startsWith("raw")) {
            for (ActualStat stat : actualStats.values()) {
                if (statNamesMatch(normalizedApiName, stat.name()) && stat.unit().equals("%")) return stat;
            }
        }
        for (ActualStat stat : actualStats.values()) {
            if (statNamesMatch(normalizedApiName, stat.name())) return stat;
        }
        if (normalizedApiName.startsWith("raw")) {
            String displayName = normalizedApiName.substring(3);
            for (ActualStat stat : actualStats.values()) {
                if (!stat.unit().equals("%") && statNamesMatch(displayName, stat.name())) return stat;
            }
        }
        return null;
    }

    private static List<Map<String, WynnDataService.StatValue>> rollableRanges(WynnDataService.ItemData item) {
        return List.of(item.identifications(), item.baseStats());
    }

    private static boolean statNamesMatch(String apiName, String actualName) {
        if (apiName.equals(actualName)) return true;
        return switch (apiName) {
            case "damagefrommobs" -> actualName.equals("damagetakenfrommobs");
            case "leveledlootbonus" -> actualName.equals("lootbonus");
            case "leveledxpbonus" -> actualName.equals("combatexperience")
                    || actualName.equals("experiencebonus") || actualName.equals("xpbonus");
            case "gatheringexperience" -> actualName.equals("gatheringxp");
            default -> false;
        };
    }

    private static boolean isInvertedRoll(String apiName, ActualStat actual) {
        String normalizedName = normalized(apiName);
        if (normalizedName.equals("damagefrommobs")) return true;
        return normalizedName.contains("spellcost") ^ actual.value() < 0;
    }

    private static boolean isBoxEligible(WynnDataService.ItemData item, int minimumLevel, int maximumLevel) {
        String restriction = normalized(item.restriction());
        if (!restriction.isEmpty() && !restriction.equals("none")) return false;
        if (normalized(item.dropRestriction()).equals("never")) return false;
        try {
            int level = Integer.parseInt(item.requirements().getOrDefault("level", "-1"));
            return level >= minimumLevel && level <= maximumLevel;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static int[] levelRange(List<String> lore) {
        for (String line : lore) {
            Matcher matcher = LEVEL_RANGE.matcher(line);
            if (!matcher.find()) continue;
            try {
                return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String cleanTooltipLine(String value) {
        StringBuilder result = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (Character.getType(codePoint) == Character.PRIVATE_USE) result.append(' ');
            else result.appendCodePoint(codePoint);
        });
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
