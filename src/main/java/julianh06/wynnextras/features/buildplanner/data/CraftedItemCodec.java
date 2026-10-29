package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBitCursor;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderCraft;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderCraftCodec;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderEncoding;
import julianh06.wynnextras.wynncraft.item.WynncraftPowders;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** WynnCrafter v1/v2 item codes using the current cached WynnBuilder data. */
public final class CraftedItemCodec {
    private static final Set<String> WEAPONS = Set.of("bow", "wand", "spear", "dagger", "relik");
    private static final Set<String> ARMOR = Set.of("helmet", "chestplate", "leggings", "boots");
    private static final Set<String> ACCESSORIES = Set.of("ring", "bracelet", "necklace");
    private static final Set<String> CONSUMABLES = Set.of("potion", "scroll", "food");
    private static final String[] SKILLS = {"str", "dex", "int", "def", "agi"};
    private static final String[] SPEEDS = {"slow", "normal", "fast"};
    private static final double[] MATERIAL_MULTIPLIERS = {1, 1.25, 1.4};
    private static final Map<String, WynnItem> CACHE = new LinkedHashMap<>();
    private static Map<Integer, JsonObject> ingredients;
    private static Map<Integer, JsonObject> recipes;

    private CraftedItemCodec() {
    }

    public static synchronized WynnItem decode(String input) {
        String hash = hash(input);
        WynnItem cached = CACHE.get(hash);
        if (cached != null) {
            return cached;
        }
        Craft craft = readCraft(input);
        WynnItem result = calculate(hash, require(recipes, craft.recipeId(), "recipe"),
                craft.ingredients().stream().mapToInt(Integer::intValue).toArray(),
                new int[]{craft.material1() - 1, craft.material2() - 1}, craft.speed());
        if ("consumable".equals(result.type())) {
            throw new IllegalArgumentException("Consumables can be previewed in Crafter, but cannot be equipped");
        }
        if (CACHE.size() >= 128) {
            CACHE.remove(CACHE.keySet().iterator().next());
        }
        CACHE.put(hash, result);
        return result;
    }

    public static synchronized Craft readCraft(String input) {
        String hash = hash(input);
        load();
        if (hash.charAt(0) == '1' && hash.length() != 17) {
            throw new IllegalArgumentException("A legacy crafted code must contain 17 characters after CR-");
        }
        WynnBuilderCraft decoded = WynnBuilderCraftCodec.decode(hash);
        if (decoded == null) throw new IllegalArgumentException("Unsupported or malformed crafted code");
        JsonObject recipe = require(recipes, decoded.recipeId(), "recipe");
        boolean weapon = WEAPONS.contains(text(recipe, "type").toLowerCase(Locale.ROOT));
        if (hash.charAt(0) != '1' && !WynnBuilderCraftCodec.encode(decoded, weapon).equals(hash)) {
            throw new IllegalArgumentException("Crafted code contains extra data or invalid padding");
        }
        int speed = weapon ? decoded.attackSpeed() : 0;
        if (speed < 0 || speed >= SPEEDS.length || decoded.material1Tier() < 1
                || decoded.material1Tier() > 3 || decoded.material2Tier() < 1 || decoded.material2Tier() > 3) {
            throw new IllegalArgumentException("Invalid crafting material tier or attack speed");
        }
        for (int id : decoded.ingredientIds()) {
            require(ingredients, id, "ingredient");
        }
        return new Craft(decoded.recipeId(), Arrays.stream(decoded.ingredientIds()).boxed().toList(),
                decoded.material1Tier(), decoded.material2Tier(), speed);
    }

    public static synchronized String encode(Craft craft) {
        load();
        boolean weapon = WEAPONS.contains(text(require(recipes, craft.recipeId(), "recipe"), "type")
                .toLowerCase(Locale.ROOT));
        for (int id : craft.ingredients()) {
            require(ingredients, id, "ingredient");
        }
        WynnBuilderCraft encoded = new WynnBuilderCraft(
                craft.ingredients().stream().mapToInt(Integer::intValue).toArray(), craft.recipeId(),
                craft.material1(), craft.material2(), craft.speed());
        return "CR-" + WynnBuilderCraftCodec.encode(encoded, weapon);
    }

    public static synchronized Boolean isWeaponRecipe(int recipeId) {
        load();
        JsonObject recipe = recipes.get(recipeId);
        return recipe == null ? null : WEAPONS.contains(text(recipe, "type").toLowerCase(Locale.ROOT));
    }

    public static synchronized String encode(WynnBuilderCraft craft) {
        Boolean weapon = isWeaponRecipe(craft.recipeId());
        if (weapon == null) throw new IllegalArgumentException("Unknown WynnBuilder recipe ID: " + craft.recipeId());
        return "CR-" + WynnBuilderCraftCodec.encode(craft, weapon);
    }

    public static synchronized WynnItem preview(Craft craft) {
        String code = encode(craft);
        return calculate(code.substring(3), require(recipes, craft.recipeId(), "recipe"),
                craft.ingredients().stream().mapToInt(Integer::intValue).toArray(),
                new int[]{craft.material1() - 1, craft.material2() - 1}, craft.speed());
    }

    public static synchronized String shareText(Craft craft) {
        String code = encode(craft);
        String name = text(require(recipes, craft.recipeId(), "recipe"), "name");
        int separator = name.indexOf('-');
        StringBuilder result = new StringBuilder("https://wynnbuilder.github.io/crafter/#")
                .append(code.substring(3)).append("\n > ").append(name, 0, separator)
                .append(" Lv. ").append(name.substring(separator + 1))
                .append(" (").append(craft.material1()).append("\u272B, ")
                .append(craft.material2()).append("\u272B)\n");
        for (int row = 0; row < 3; row++) {
            result.append(row == 0 ? " > [" : " >  ")
                    .append(text(require(ingredients, craft.ingredients().get(row * 2), "ingredient"), "name"))
                    .append(" | ")
                    .append(text(require(ingredients, craft.ingredients().get(row * 2 + 1), "ingredient"), "name"))
                    .append(row == 2 ? "]" : "\n");
        }
        return result.toString();
    }

    public static synchronized List<Recipe> recipeChoices() {
        load();
        return recipes.values().stream().map(recipe -> {
            List<Material> materials = new ArrayList<>();
            for (JsonElement entry : recipe.getAsJsonArray("materials")) {
                JsonObject material = entry.getAsJsonObject();
                materials.add(new Material(text(material, "item"), material.get("amount").getAsInt()));
            }
            return new Recipe(recipe.get("id").getAsInt(), text(recipe, "name"),
                    text(recipe, "type").toLowerCase(Locale.ROOT), text(recipe, "skill"),
                    range(recipe, "lvl", "minimum"), range(recipe, "lvl", "maximum"), List.copyOf(materials));
        }).sorted(java.util.Comparator.comparing(Recipe::type).thenComparingInt(Recipe::levelLow)
                .thenComparing(Recipe::name)).toList();
    }

    public static synchronized List<Ingredient> ingredientChoices() {
        load();
        return ingredients.entrySet().stream().map(entry -> {
            JsonObject ingredient = entry.getValue();
            List<String> skills = new ArrayList<>();
            ingredient.getAsJsonArray("skills").forEach(skill -> skills.add(skill.getAsString()));
            List<String> details = new ArrayList<>();
            List<ItemInspection.Row> rows = new ArrayList<>();
            for (String section : List.of("ids", "posMods", "itemIDs", "consumableIDs")) {
                if (!ingredient.has(section)) continue;
                ingredient.getAsJsonObject(section).entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .forEach(stat -> {
                            if (stat.getValue().isJsonObject()) {
                                JsonObject rolls = stat.getValue().getAsJsonObject();
                                int min = rolls.get("minimum").getAsInt();
                                int max = rolls.get("maximum").getAsInt();
                                if (min != 0 || max != 0) {
                                    String key = WynnBuilderIdentifications.name(stat.getKey());
                                    details.add(ItemInspection.readable(key) + ": " + IdentificationUnits.format(key, min)
                                            + (min == max ? "" : " to " + IdentificationUnits.format(key, max)));
                                    boolean reversed = key.matches("(?i)(raw)?[1-4](st|nd|rd|th)SpellCost");
                                    rows.add(new ItemInspection.Row(ItemInspection.readable(key)
                                            + (IdentificationUnits.unit(key) == IdentificationUnits.Unit.PERCENT ? " %" : ""),
                                            IdentificationUnits.format(key, min) + " to " + IdentificationUnits.format(key, max),
                                            ItemInspection.Kind.IDENTIFICATION, -1, reversed ? max < 0 : max > 0));
                                }
                            } else if (stat.getValue().getAsDouble() != 0) {
                                String label = stat.getKey();
                                if ("dura".equals(label)) {
                                    label = section.equals("consumableIDs") ? "Duration" : "Durability";
                                } else if (section.equals("posMods")) {
                                    label = "Effectiveness " + label;
                                }
                                details.add(ItemInspection.readable(label) + ": " + stat.getValue().getAsString()
                                        + (section.equals("posMods") ? "%" : ""));
                                label = switch (label) {
                                    case "strReq" -> "Strength Min";
                                    case "dexReq" -> "Dexterity Min";
                                    case "intReq" -> "Intelligence Min";
                                    case "defReq" -> "Defense Min";
                                    case "agiReq" -> "Agility Min";
                                    default -> ItemInspection.readable(label);
                                };
                                boolean lowerBetter = section.equals("itemIDs") && stat.getKey().endsWith("Req");
                                rows.add(new ItemInspection.Row(label,
                                        (stat.getValue().getAsDouble() > 0 ? "+" : "") + stat.getValue().getAsString()
                                                + (section.equals("posMods") ? "%" : ""),
                                        ItemInspection.Kind.MODIFIER, -1,
                                        lowerBetter ? stat.getValue().getAsDouble() < 0 : stat.getValue().getAsDouble() > 0));
                            }
                        });
            }
            return new Ingredient(entry.getKey(), text(ingredient, "name"), ingredient.get("lvl").getAsInt(),
                    ingredient.has("tier") ? ingredient.get("tier").getAsInt() : 0,
                    List.copyOf(skills), List.copyOf(details), List.copyOf(rows), ingredientIcon(ingredient));
        }).sorted(java.util.Comparator.comparing(Ingredient::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static IngredientIcon ingredientIcon(JsonObject ingredient) {
        if (!ingredient.has("icon") || !ingredient.get("icon").isJsonObject()) return IngredientIcon.EMPTY;
        JsonObject icon = ingredient.getAsJsonObject("icon");
        String format = icon.has("format") ? icon.get("format").getAsString() : "";
        JsonElement valueElement = icon.get("value");
        if ("skin".equals(format) && valueElement != null && valueElement.isJsonPrimitive()) {
            return new IngredientIcon("", "", 0, valueElement.getAsString());
        }
        if (valueElement == null || !valueElement.isJsonObject()) return IngredientIcon.EMPTY;
        JsonObject value = valueElement.getAsJsonObject();
        int modelData = 0;
        if (value.has("customModelData") && value.get("customModelData").isJsonObject()) {
            JsonArray values = value.getAsJsonObject("customModelData").getAsJsonArray("rangeDispatch");
            if (values != null && !values.isEmpty()) modelData = values.get(0).getAsInt();
        }
        return new IngredientIcon(value.has("id") ? value.get("id").getAsString() : "",
                value.has("name") ? value.get("name").getAsString() : "", modelData, "");
    }

    public static synchronized boolean isPowderIngredient(int id) {
        load();
        return require(ingredients, id, "ingredient").has("pid");
    }

    public static synchronized Map<String, Double> ingredientStats(int id) {
        load();
        JsonObject ingredient = require(ingredients, id, "ingredient");
        Map<String, Double> values = new java.util.LinkedHashMap<>();
        values.put("lvl", ingredient.get("lvl").getAsDouble());
        for (String section : List.of("ids", "posMods", "itemIDs", "consumableIDs")) {
            if (!ingredient.has(section)) continue;
            ingredient.getAsJsonObject(section).entrySet().forEach(entry -> {
                String key = entry.getKey();
                if (section.equals("ids")) key = WynnBuilderIdentifications.name(key);
                else if (section.equals("posMods")) key = "effectiveness " + key;
                else if (key.equals("dura")) key = section.equals("itemIDs") ? "durability" : "duration";
                values.put(key, entry.getValue().isJsonObject()
                        ? entry.getValue().getAsJsonObject().get("maximum").getAsDouble() : entry.getValue().getAsDouble());
            });
        }
        return Map.copyOf(values);
    }

    public record Material(String name, int amount) {}
    public record Recipe(int id, String name, String type, String profession, int levelLow, int levelHigh,
            List<Material> materials) {}
    public record Ingredient(int id, String name, int level, int tier, List<String> professions, List<String> details,
            List<ItemInspection.Row> rows, IngredientIcon icon) {
        public boolean supports(Recipe recipe) {
            return level <= recipe.levelHigh() && professions.contains(recipe.profession());
        }
    }
    public record IngredientIcon(String itemId, String model, int customModelData, String skinHash) {
        private static final IngredientIcon EMPTY = new IngredientIcon("", "", 0, "");
    }
    public record Craft(int recipeId, List<Integer> ingredients, int material1, int material2, int speed) {
        public Craft {
            if (recipeId < 0 || recipeId > 4095 || ingredients == null || ingredients.size() != 6
                    || ingredients.stream().anyMatch(id -> id == null || id < 0 || id > 4095)
                    || material1 < 1 || material1 > 3 || material2 < 1 || material2 > 3 || speed < 0 || speed > 2) {
                throw new IllegalArgumentException("Invalid crafting recipe, ingredient slots, material tiers or speed");
            }
            ingredients = List.copyOf(ingredients);
        }

        public Craft withIngredient(int index, int id) {
            List<Integer> changed = new ArrayList<>(ingredients);
            changed.set(index, id);
            return new Craft(recipeId, changed, material1, material2, speed);
        }

        public Craft swap(int first, int second) {
            List<Integer> changed = new ArrayList<>(ingredients);
            java.util.Collections.swap(changed, first, second);
            return new Craft(recipeId, changed, material1, material2, speed);
        }

        public Craft withRecipe(int id) {
            return new Craft(id, ingredients, material1, material2, speed);
        }
    }

    public static Craft emptyCraft(String type) {
        Recipe recipe = recipeChoices().stream().filter(entry -> entry.type().equals(type))
                .min(java.util.Comparator.comparingInt(entry -> Math.abs(entry.levelHigh() - 105)))
                .orElseThrow(() -> new IllegalArgumentException("Unknown crafting type: " + type));
        return new Craft(recipe.id(), java.util.Collections.nCopies(6, 4000), 3, 3, 1);
    }

    public static boolean isReference(String reference) {
        return reference != null && reference.startsWith("CR-");
    }

    public static boolean matchesSlot(WynnItem item, String slot) {
        return "weapon".equals(slot) ? "weapon".equals(item.type())
                : slot.equals(item.subType()) || item.isCrafted()
                        && ACCESSORIES.contains(slot) && ACCESSORIES.contains(item.subType());
    }

    public static synchronized WynnItem decodeForSlot(String input, String slot) {
        WynnItem item = decode(input);
        if (!matchesSlot(item, slot)) {
            throw new IllegalArgumentException("This is a " + item.subType() + ", not a " + slot + ".");
        }
        if (!ACCESSORIES.contains(slot) || item.subType().equals(slot)) {
            return item;
        }
        String hash = item.craftedCode().substring(3);
        int sourceRecipeId;
        if (hash.charAt(0) == '1') {
            sourceRecipeId = integer(hash, 13, 2);
        } else {
            WynnBuilderBitCursor bits = new WynnBuilderBitCursor(hash);
            bits.position(80);
            sourceRecipeId = bits.read(12);
        }
        String sourceName = text(require(recipes, sourceRecipeId, "recipe"), "name");
        String targetName = slot + sourceName.substring(sourceName.indexOf('-'));
        // Match the recipe name like WynnBuilder; some recipe level metadata disagrees with its name.
        JsonObject target = recipes.values().stream()
                .filter(recipe -> text(recipe, "name").equalsIgnoreCase(targetName))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No matching accessory recipe"));
        int recipeId = target.get("id").getAsInt();
        if (hash.charAt(0) == '1') {
            hash = hash.substring(0, 13) + WynnBuilderEncoding.ALPHABET.charAt(recipeId / 64)
                    + WynnBuilderEncoding.ALPHABET.charAt(recipeId % 64) + hash.substring(15);
        } else {
            char[] characters = hash.toCharArray();
            // Recipe ID follows the 8-bit header and six 12-bit ingredient IDs.
            for (int bit = 0; bit < 12; bit++) {
                int offset = 80 + bit;
                int value = WynnBuilderEncoding.value(characters[offset / 6]);
                value = (value & ~(1 << (offset % 6))) | ((recipeId >> bit & 1) << (offset % 6));
                characters[offset / 6] = WynnBuilderEncoding.ALPHABET.charAt(value);
            }
            hash = new String(characters);
        }
        return decode(hash);
    }

    private static String hash(String input) {
        if (input == null || input.length() > 2048) {
            throw new IllegalArgumentException("Paste a WynnCrafter CR- code or link");
        }
        String value = input.trim();
        if (value.startsWith("https://") || value.startsWith("http://")) {
            value = value.lines().findFirst().orElseThrow();
            URI uri;
            try {
                uri = URI.create(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid WynnCrafter link", exception);
            }
            if (!Set.of("wynnbuilder.github.io", "hppeng-wynn.github.io").contains(
                    uri.getHost() == null ? "" : uri.getHost())
                    || !Set.of("/crafter/", "/crafter", "/crafter.html").contains(uri.getPath())
                    || uri.getFragment() == null) {
                throw new IllegalArgumentException("Paste a WynnCrafter item link, not a full build link");
            }
            value = uri.getFragment();
        }
        if (value.startsWith("CR-")) {
            value = value.substring(3);
        }
        if (value.length() < 17 || value.length() > 18) {
            throw new IllegalArgumentException("Crafted code is incomplete or has extra characters");
        }
        if (!WynnBuilderEncoding.isValid(value)) {
            throw new IllegalArgumentException("Crafted code contains an invalid character");
        }
        return value;
    }

    private static WynnItem calculate(String hash, JsonObject recipe, int[] ingredientIds, int[] materials, int speed) {
        String subtype = text(recipe, "type").toLowerCase(Locale.ROOT);
        boolean weapon = WEAPONS.contains(subtype);
        boolean armor = ARMOR.contains(subtype);
        boolean consumable = CONSUMABLES.contains(subtype);
        if (!weapon && !armor && !ACCESSORIES.contains(subtype) && !consumable) {
            throw new IllegalArgumentException("Only crafted weapons, armor and accessories can be equipped");
        }
        int levelMin = range(recipe, "lvl", "minimum");
        int levelMax = range(recipe, "lvl", "maximum");
        String profession = text(recipe, "skill");
        List<JsonObject> selected = new ArrayList<>();
        for (int id : ingredientIds) {
            JsonObject ingredient = require(ingredients, id, "ingredient");
            boolean allowed = false;
            for (JsonElement skill : ingredient.getAsJsonArray("skills")) {
                allowed |= skill.getAsString().equals(profession);
            }
            if (!allowed || ingredient.get("lvl").getAsInt() > levelMax) {
                throw new IllegalArgumentException(text(ingredient, "name") + " cannot be used in " + text(recipe, "name"));
            }
            selected.add(ingredient);
        }
        JsonArray amounts = recipe.getAsJsonArray("materials");
        int firstAmount = amounts.get(0).getAsJsonObject().get("amount").getAsInt();
        int secondAmount = amounts.get(1).getAsJsonObject().get("amount").getAsInt();
        double multiplier = (MATERIAL_MULTIPLIERS[materials[0]] * firstAmount
                + MATERIAL_MULTIPLIERS[materials[1]] * secondAmount) / (firstAmount + secondAmount);
        boolean empty = Arrays.stream(ingredientIds).allMatch(id -> id == 4000);
        double durabilityMin = consumable ? 0 : Math.round(range(recipe, "durability", "minimum") * multiplier);
        double durabilityMax = consumable ? 0 : Math.round(range(recipe, "durability", "maximum") * multiplier);
        String durationKey = empty ? "basicDuration" : "duration";
        int durationMin = consumable ? (int) Math.round(range(recipe, durationKey, "minimum") * multiplier) : 0;
        int durationMax = consumable ? (int) Math.round(range(recipe, durationKey, "maximum") * multiplier) : 0;
        int charges = empty ? 3 : levelMin < 30 ? 1 : levelMin < 70 ? 2 : 3;
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("lvl", levelMax);
        stats.put("lvlLow", levelMin);
        stats.put("powderSlots", weapon || armor ? levelMin < 30 ? 1 : levelMin < 70 ? 2 : 3 : 0);
        stats.put("materialTier1", materials[0] + 1);
        stats.put("materialTier2", materials[1] + 1);
        int lowBase = (int) Math.floor(range(recipe, "healthOrDamage", "minimum") * multiplier);
        int highBase = (int) Math.floor(range(recipe, "healthOrDamage", "maximum") * multiplier);
        if (armor || consumable && empty) {
            stats.put("hp", highBase);
            stats.put("hpLow", lowBase);
        } else if (weapon) {
            double ratio = speed == 0 ? 2.05 / 1.5 : speed == 2 ? 2.05 / 2.5 : 1;
            highBase = (int) Math.floor(highBase * ratio);
            lowBase = (int) Math.floor(lowBase * ratio);
            stats.put("nDamMin", (int) Math.floor(highBase * 0.9));
            stats.put("nDamMax", (int) Math.floor(highBase * 1.1));
            stats.put("nDamBaseLow", lowBase);
            stats.put("nDamBaseHigh", highBase);
        }
        int[] effectiveness = effectiveness(selected);
        Map<String, Identification> ids = new LinkedHashMap<>();
        List<Integer> powders = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            JsonObject ingredient = selected.get(index);
            boolean powder = ingredient.has("pid");
            if (powder) {
                int pid = ingredient.get("pid").getAsInt();
                if (weapon) {
                    powders.add(pid);
                } else if (!consumable) {
                    BuildCalculator.addIngredientArmorPowder(stats, pid);
                }
            }
            if (consumable) {
                JsonObject modifiers = ingredient.getAsJsonObject("consumableIDs");
                durationMin += modifiers.get("dura").getAsInt();
                durationMax += modifiers.get("dura").getAsInt();
                charges += modifiers.get("charges").getAsInt();
            }
            double effect = effectiveness[index] / 100.0;
            stats.put("ingredientEffectiveness" + index, effectiveness[index]);
            for (Map.Entry<String, JsonElement> entry : ingredient.getAsJsonObject("itemIDs").entrySet()) {
                double value = entry.getValue().getAsDouble();
                if (entry.getKey().equals("dura")) {
                    durabilityMin += value;
                    durabilityMax += value;
                } else if (!consumable) {
                    stats.merge(entry.getKey(), (int) Math.floor(value * (powder ? 1 : effect) + 0.5 + 1e-9), Integer::sum);
                }
            }
            for (Map.Entry<String, JsonElement> entry : ingredient.getAsJsonObject("ids").entrySet()) {
                JsonObject rolls = entry.getValue().getAsJsonObject();
                int high = rolls.get("maximum").getAsInt();
                if (high == 0) {
                    continue;
                }
                int first = (int) Math.floor(rolls.get("minimum").getAsInt() * effect);
                int second = (int) Math.floor(high * effect);
                String key = WynnBuilderIdentifications.name(entry.getKey());
                Identification previous = ids.getOrDefault(key, new Identification(0, 0, 0));
                int min = previous.min() + Math.min(first, second);
                int max = previous.max() + Math.max(first, second);
                ids.put(key, new Identification(min, max, max));
            }
        }
        stats.put("durabilityMin", Math.max(0, (int) Math.floor(durabilityMin)));
        stats.put("durabilityMax", Math.max(0, (int) Math.floor(durabilityMax)));
        if (consumable) {
            stats.put("durationClamped", !empty && (durationMin < 1 || durationMax < 1) ? 1 : 0);
            stats.put("durationMin", empty ? durationMin : Math.max(1, durationMin));
            stats.put("durationMax", empty ? durationMax : Math.max(1, durationMax));
            stats.put("charges", Math.max(1, charges));
        }
        if (!consumable && durabilityMax < 1) {
            throw new IllegalArgumentException("This recipe has no remaining durability");
        }
        String name = "Crafted " + Character.toUpperCase(subtype.charAt(0)) + subtype.substring(1)
                + " (" + levelMin + "-" + levelMax + ")";
        return new WynnItem(name, weapon ? "weapon" : armor ? "armour" : consumable ? "consumable" : "accessory",
                subtype, "crafted", weapon ? SPEEDS[speed] : "", ids, stats, "", "", 0, "CR-" + hash, powders);
    }

    private static int[] effectiveness(List<JsonObject> selected) {
        int[] effects = new int[6];
        Arrays.fill(effects, 100);
        for (int source = 0; source < 6; source++) {
            for (Map.Entry<String, JsonElement> entry : selected.get(source).getAsJsonObject("posMods").entrySet()) {
                for (int target = 0; target < 6; target++) {
                    int dy = target / 2 - source / 2;
                    int dx = target % 2 - source % 2;
                    boolean affected = switch (entry.getKey()) {
                        case "left" -> dy == 0 && dx == -1;
                        case "right" -> dy == 0 && dx == 1;
                        case "above" -> dx == 0 && dy < 0;
                        case "under" -> dx == 0 && dy > 0;
                        case "touching" -> Math.abs(dx) + Math.abs(dy) == 1;
                        case "notTouching" -> Math.abs(dy) > 1 || Math.abs(dy) == 1 && Math.abs(dx) == 1;
                        default -> throw new IllegalStateException("Unknown ingredient position modifier");
                    };
                    if (affected) {
                        effects[target] += entry.getValue().getAsInt();
                    }
                }
            }
        }
        return effects;
    }

    private static void load() {
        if (ingredients != null) {
            return;
        }
        Map<Integer, JsonObject> loadedIngredients = read(WynnBuilderDataFiles.File.INGREDIENTS);
        Map<Integer, JsonObject> loadedRecipes = read(WynnBuilderDataFiles.File.RECIPES);
        loadedIngredients.put(4000, powderIngredient(-1));
        for (int powder = 0; powder < 35; powder++) {
            loadedIngredients.put(4001 + powder, powderIngredient(powder));
        }
        ingredients = Map.copyOf(loadedIngredients);
        recipes = Map.copyOf(loadedRecipes);
    }

    private static JsonObject powderIngredient(int pid) {
        WynncraftPowders.Powder powder = WynncraftPowders.fromId(pid);
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("name", powder == null ? "No Ingredient" : powder.name());
        ingredient.addProperty("lvl", 0);
        if (pid >= 0) {
            ingredient.addProperty("pid", pid);
        }
        JsonArray skills = new JsonArray();
        for (String skill : List.of("ARMOURING", "TAILORING", "WEAPONSMITHING", "WOODWORKING", "JEWELING")) {
            skills.add(skill);
        }
        if (pid < 0) {
            for (String skill : List.of("ALCHEMISM", "SCRIBING", "COOKING")) {
                skills.add(skill);
            }
        }
        ingredient.add("skills", skills);
        ingredient.add("ids", new JsonObject());
        ingredient.add("posMods", new JsonObject());
        JsonObject itemIds = new JsonObject();
        itemIds.addProperty("dura", powder == null ? 0 : powder.durability());
        for (int skill = 0; skill < SKILLS.length; skill++) {
            itemIds.addProperty(SKILLS[skill] + "Req",
                    powder != null && powder.element() == skill ? powder.requirement() : 0);
        }
        ingredient.add("itemIDs", itemIds);
        JsonObject consumableIds = new JsonObject();
        consumableIds.addProperty("dura", 0);
        consumableIds.addProperty("charges", 0);
        ingredient.add("consumableIDs", consumableIds);
        return ingredient;
    }

    private static Map<Integer, JsonObject> read(WynnBuilderDataFiles.File file) {
        try {
            JsonElement root = WynnBuilderDataFiles.read(file);
            JsonArray data = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray(
                            file == WynnBuilderDataFiles.File.INGREDIENTS ? "ingredients" : "recipes");
            if (data == null) throw new IllegalStateException("Missing crafting data array in " + file);
            Map<Integer, JsonObject> result = new HashMap<>();
            for (JsonElement element : data) {
                JsonObject object = element.getAsJsonObject();
                if (result.put(object.get("id").getAsInt(), object) != null) {
                    throw new IllegalStateException("Duplicate crafting data ID in " + file);
                }
            }
            return result;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not read current WynnBuilder crafting data", exception);
        }
    }

    private static JsonObject require(Map<Integer, JsonObject> data, int id, String kind) {
        JsonObject value = data.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown crafting " + kind + " ID: " + id);
        }
        return value;
    }

    private static int range(JsonObject object, String key, String bound) {
        return object.getAsJsonObject(key).get(bound).getAsInt();
    }

    private static String text(JsonObject object, String key) {
        return object.get(key).getAsString();
    }

    private static int integer(String hash, int offset, int length) {
        return WynnBuilderEncoding.bigEndianValue(hash, offset, length);
    }
}
