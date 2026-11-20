package julianh06.wynnextras.features.buildplanner.data;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

public final class AtlasSearch {
    private static final String ANY_EFFECTIVENESS = "effectiveness any";
    private static final java.util.regex.Pattern SPACES = java.util.regex.Pattern.compile("[\\s_]+");
    private AtlasSearch() {}

    public static List<Entry> items(List<WynnItem> items) {
        return items.stream().filter(item -> Set.of("weapon", "armour", "accessory").contains(item.type()))
                .map(item -> new Entry("item:" + item.reference(), item.displayName(), item.stat("lvl"),
                        item.subType(), item.tier().toLowerCase(Locale.ROOT),
                        item.displayName() + " " + String.join(" ", item.identifications().keySet())
                                + " " + String.join(" ", item.majorIds().keySet()),
                        item, null, itemStats(item))).toList();
    }

    public static List<Entry> ingredients(List<CraftedItemCodec.Ingredient> ingredients) {
        return ingredients.stream().filter(ingredient -> ingredient.id() != 4000)
                .filter(ingredient -> !CraftedItemCodec.isPowderIngredient(ingredient.id()))
                .map(ingredient -> {
                    Map<String, Double> stats = new LinkedHashMap<>(CraftedItemCodec.ingredientStats(ingredient.id()));
                    boolean anyEffectiveness = stats.entrySet().stream()
                            .anyMatch(entry -> entry.getKey().startsWith("effectiveness ") && entry.getValue() != 0D);
                    stats.put(ANY_EFFECTIVENESS, anyEffectiveness ? 1D : 0D);
                    return new Entry("ingredient:" + ingredient.id(), ingredient.name(), ingredient.level(),
                            String.join(",", ingredient.professions()), Integer.toString(ingredient.tier()),
                            ingredient.name() + " " + String.join(" ", ingredient.details()),
                            null, ingredient, stats);
                }).toList();
    }

    public static List<Entry> search(List<Entry> catalog, AtlasState state) {
        var ranges = state.filters().stream().map(filter -> {
            double min = bound(filter.minimum(), Double.NEGATIVE_INFINITY);
            double max = bound(filter.maximum(), Double.POSITIVE_INFINITY);
            if (min > max) throw new IllegalArgumentException(label(filter.key()) + ": minimum must not exceed maximum.");
            return new Range(filter.key(), min, max);
        }).toList();
        for (var filter : state.filters()) {
            if (!knownKey(catalog, filter.key())) throw new IllegalArgumentException("Unknown filter: " + filter.key());
        }
        for (String key : state.excluded()) {
            if (!knownKey(catalog, key)) throw new IllegalArgumentException("Unknown excluded filter: " + key);
        }
        for (var filter : state.strings()) {
            if (!List.of("name", "lore", "majorId", "restriction", "attackSpeed").contains(filter.key())) {
                throw new IllegalArgumentException("Unknown item property filter: " + filter.key());
            }
        }
        String query = normalized(state.query());
        Comparator<Entry> order = switch (state.sort()) {
            case "name", "filters" -> Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER);
            case "level" -> Comparator.comparingInt(Entry::level);
            case "rarity" -> Comparator.comparingInt(entry -> rarityRank(entry.rarity()));
            default -> throw new IllegalArgumentException("Unknown Atlas sort order: " + state.sort());
        };
        if (state.descending()) order = order.reversed();
        // Numeric rows are ordered sort priorities, with independent directions.
        Comparator<Entry> priority = null;
        for (var filter : state.sort().equals("filters") ? state.filters() : List.<AtlasState.NumericFilter>of()) {
            Comparator<Entry> next = Comparator.comparingDouble(entry -> entry.stats().getOrDefault(filter.key(), 0D));
            if (filter.descending()) next = next.reversed();
            priority = priority == null ? next : priority.thenComparing(next);
        }
        if (priority != null) order = priority.thenComparing(order);
        return catalog.stream()
                .filter(entry -> (entry.ingredient() != null) == state.ingredients())
                .filter(entry -> matchesSelection(state.category(), entry.category()))
                .filter(entry -> (!state.ingredients() && state.rarity().equals("~none"))
                        || matchesSelection(state.rarity(), entry.rarity()))
                .filter(entry -> ranges.stream().allMatch(range -> range.matches(entry)))
                .filter(entry -> state.excluded().stream().allMatch(key -> entry.stats().getOrDefault(key, 0D) == 0))
                .filter(entry -> state.strings().stream().allMatch(filter ->
                        normalized(stringValue(entry, filter.key())).contains(normalized(filter.value()))))
                .filter(entry -> query.isEmpty() || entry.searchText().contains(query))
                .sorted(order.thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public static boolean matchesSelection(String selection, String values) {
        if (selection.isEmpty()) return true;
        var selected = List.of(selection.split(","));
        return java.util.Arrays.stream(values.split(",")).anyMatch(selected::contains);
    }

    public static List<String> numericKeys(List<Entry> catalog) {
        var keys = new java.util.TreeSet<String>();
        keys.add("lvl");
        catalog.forEach(entry -> keys.addAll(entry.stats().keySet()));
        var uniqueLabels = new LinkedHashMap<String, String>();
        keys.stream().filter(key -> !redundantBaseStat(key))
                .sorted(Comparator.<String>comparingInt(key -> groupRank(group(key)))
                        .thenComparing(AtlasSearch::label).thenComparing(key -> key))
                .forEach(key -> {
                    String label = label(key).toLowerCase(Locale.ROOT);
                    String current = uniqueLabels.get(label);
                    if (current == null || isIdentification(catalog, key) && !isIdentification(catalog, current)) {
                        uniqueLabels.put(label, key);
                    }
                });
        return List.copyOf(uniqueLabels.values());
    }

    private static boolean isIdentification(List<Entry> catalog, String key) {
        return catalog.stream().map(Entry::item).filter(java.util.Objects::nonNull)
                .anyMatch(item -> item.identifications().containsKey(key));
    }

    private static boolean knownKey(List<Entry> catalog, String key) {
        return key.equals("lvl") || catalog.stream().anyMatch(entry -> entry.stats().containsKey(key));
    }

    public static String label(String key) {
        String exact = switch (key) {
            case "lvl" -> "Combat Level";
            case "hp" -> "Base Health";
            case "rawHealth" -> "Health";
            case "baseDamage" -> "Base Neutral Damage (Average)";
            case "str", "rawStrength" -> "Strength";
            case "dex", "rawDexterity" -> "Dexterity";
            case "int", "rawIntelligence" -> "Intelligence";
            case "def", "rawDefence" -> "Defense";
            case "agi", "rawAgility" -> "Agility";
            case "strReq" -> "Strength Requirement";
            case "dexReq" -> "Dexterity Requirement";
            case "intReq" -> "Intelligence Requirement";
            case "defReq" -> "Defense Requirement";
            case "agiReq" -> "Agility Requirement";
            case "rawAttackSpeed" -> "Attack Speed Bonus";
            case "rawMaxMana" -> "Max Mana";
            case "1stSpellCost" -> "1st Spell Cost %";
            case "2ndSpellCost" -> "2nd Spell Cost %";
            case "3rdSpellCost" -> "3rd Spell Cost %";
            case "4thSpellCost" -> "4th Spell Cost %";
            case "raw1stSpellCost" -> "1st Spell Cost Raw";
            case "raw2ndSpellCost" -> "2nd Spell Cost Raw";
            case "raw3rdSpellCost" -> "3rd Spell Cost Raw";
            case "raw4thSpellCost" -> "4th Spell Cost Raw";
            case "combatExperience" -> "Combat XP Bonus";
            case "gatherXpBonus", "gatheringExperience" -> "Gathering XP Bonus";
            case "gatherSpeed" -> "Gathering Speed";
            case "healthRegen" -> "Health Regen";
            case "healthRegenRaw" -> "Health Regen Raw";
            case "mainAttackDamage" -> "Main Attack Damage %";
            case "rawMainAttackDamage" -> "Main Attack Damage Raw";
            case "spellDamage" -> "Spell Damage %";
            case "rawSpellDamage" -> "Spell Damage Raw";
            case "damage" -> "Damage %";
            case "rawDamage" -> "Damage Raw";
            case "walkSpeed" -> "Walk Speed";
            case "sprintRegen" -> "Sprint Regen";
            case "mainAttackRange" -> "Main Attack Range";
            case "criticalDamageBonus" -> "Critical Damage Bonus";
            case "healingEfficiency" -> "Healing Efficiency";
            case "durability" -> "Durability Modifier";
            case "duration" -> "Duration Modifier";
            case "charges" -> "Charge Modifier";
            case "majorId" -> "Major ID";
            case "attackSpeed" -> "Attack Speed";
            case "restriction" -> "Restriction";
            case "lore" -> "Lore contains";
            case "name" -> "Name contains";
            default -> null;
        };
        if (exact != null) return exact;
        if (key.startsWith("effectiveness ")) {
            return "Effectiveness: " + ItemInspection.readable(key.substring("effectiveness ".length()));
        }
        if (key.matches("[netwfa]Dam(Min|Max)")) {
            return elementName(key.charAt(0)) + " Damage (" + (key.endsWith("Min") ? "Minimum" : "Maximum") + ")";
        }
        if (key.matches("(neutral|earth|thunder|water|fire|air|elemental)(MainAttack|Spell)?Damage")) {
            return ItemInspection.readable(key) + " %";
        }
        if (key.matches("(earth|thunder|water|fire|air|elemental)Defence")) {
            return ItemInspection.readable(key).replace("Defence", "Defense") + " %";
        }
        if (key.matches("[etwfa]Def")) return "Base " + elementName(key.charAt(0)) + " Defense";
        if (key.matches("base(Neutral|Earth|Thunder|Water|Fire|Air)?Damage(Min|Max)?")) {
            String value = key.substring(4);
            return "Base " + ItemInspection.readable(value).replace(" Min", " (Minimum)")
                    .replace(" Max", " (Maximum)") + (key.endsWith("Damage") ? " (Average)" : "");
        }
        if (key.startsWith("raw") && key.length() > 3) {
            return ItemInspection.readable(key.substring(3)) + " Raw";
        }
        return ItemInspection.readable(key).replace("Defence", "Defense")
                .replace(" Min", " (Minimum)").replace(" Max", " (Maximum)");
    }

    public static String group(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (Set.of("str", "dex", "int", "def", "agi", "rawstrength", "rawdexterity",
                "rawintelligence", "rawdefence", "rawagility").contains(lower)) return "Skill Points";
        if (lower.endsWith("req") || lower.equals("lvl")) return "Requirements";
        if (lower.startsWith("effectiveness ") || Set.of("durability", "duration", "charges").contains(lower)) {
            return "Ingredient Modifiers";
        }
        if (lower.contains("spellcost") || lower.contains("mana")) return "Mana & Spell Costs";
        if (lower.contains("damage") || lower.matches("[netwfa]dam(min|max)")) return "Damage";
        if (lower.contains("defence") || lower.contains("defense") || lower.matches("[etwfa]def")) return "Defense";
        if (lower.contains("health") || lower.contains("lifesteal") || lower.contains("healing")) return "Health & Sustain";
        if (lower.contains("walk") || lower.contains("sprint") || lower.contains("jump")
                || lower.contains("attack") || lower.contains("knockback")) return "Mobility & Combat";
        if (lower.contains("loot") || lower.contains("experience") || lower.contains("gather")) return "Loot & XP";
        return "Utility";
    }

    private static int groupRank(String group) {
        return switch (group) {
            case "Skill Points" -> 0;
            case "Requirements" -> 1;
            case "Damage" -> 2;
            case "Defense" -> 3;
            case "Health & Sustain" -> 4;
            case "Mana & Spell Costs" -> 5;
            case "Mobility & Combat" -> 6;
            case "Loot & XP" -> 7;
            case "Ingredient Modifiers" -> 8;
            default -> 9;
        };
    }

    private static String elementName(char prefix) {
        return switch (prefix) {
            case 'n' -> "Neutral";
            case 'e' -> "Earth";
            case 't' -> "Thunder";
            case 'w' -> "Water";
            case 'f' -> "Fire";
            case 'a' -> "Air";
            default -> "Elemental";
        };
    }

    private static Map<String, Double> itemStats(WynnItem item) {
        Map<String, Double> stats = new LinkedHashMap<>();
        item.baseStats().forEach((key, value) -> stats.put(key, value.doubleValue()));
        for (String key : List.of("baseDamage", "baseEarthDamage", "baseThunderDamage",
                "baseWaterDamage", "baseFireDamage", "baseAirDamage")) {
            Double minimum = stats.get(key + "Min");
            Double maximum = stats.get(key + "Max");
            if (minimum != null && maximum != null) stats.put(key, (minimum + maximum) / 2D);
        }
        item.identifications().forEach((key, value) -> stats.put(key, (double) value.max()));
        stats.put("lvl", (double) item.stat("lvl"));
        return Map.copyOf(stats);
    }

    private static boolean redundantBaseStat(String key) {
        return key.equals("baseHealth")
                || key.matches("base(Earth|Thunder|Water|Fire|Air)Defence")
                || key.matches("base(Earth|Thunder|Water|Fire|Air)?Damage(Min|Max)")
                || key.matches("[netwfa]Dam(Min|Max)");
    }

    private static String stringValue(Entry entry, String key) {
        if (key.equals("name")) return entry.name();
        var item = entry.item();
        if (item == null) return "";
        return switch (key) {
            case "lore" -> item.lore();
            case "majorId" -> String.join(" ", item.majorIds().keySet()) + " " + String.join(" ", item.majorIds().values());
            case "restriction" -> item.restriction();
            case "attackSpeed" -> item.attackSpeed();
            default -> throw new IllegalArgumentException("Unknown item property filter: " + key);
        };
    }

    private static double bound(String input, double empty) {
        if (input.isBlank()) return empty;
        try {
            double value = Double.parseDouble(input);
            if (!Double.isFinite(value)) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a number for filter bounds, or leave them blank for no limit.");
        }
    }

    private record Range(String key, double min, double max) {
        boolean matches(Entry entry) {
            double value = entry.stats().getOrDefault(key, 0D);
            return (!key.equals(ANY_EFFECTIVENESS) || value != 0D) && value >= min && value <= max;
        }
    }

    private static String normalized(String text) {
        return SPACES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static int rarityRank(String rarity) {
        return switch (rarity) {
            case "0", "normal" -> 0;
            case "1", "unique" -> 1;
            case "2", "rare" -> 2;
            case "3", "legendary" -> 3;
            case "set" -> 4;
            case "fabled" -> 5;
            case "mythic" -> 6;
            default -> 7;
        };
    }

    public record Entry(String id, String name, int level, String category, String rarity, String searchText,
            WynnItem item, CraftedItemCodec.Ingredient ingredient, Map<String, Double> stats) {
        public Entry { searchText = normalized(searchText); stats = Map.copyOf(stats); }
    }
}
