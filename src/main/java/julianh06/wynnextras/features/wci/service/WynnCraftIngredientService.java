package julianh06.wynnextras.features.wci.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.wci.model.IngredientRequirement;
import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.util.IngredientNormalizer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WynnCraftIngredientService {
    private static final String INGREDIENTS_RESOURCE = "/assets/wynnextras/wci/wynnbuilder_ingredients.json";
    private static final String RECIPES_RESOURCE = "/assets/wynnextras/wci/wynnbuilder_recipes.json";

    private final Map<String, RegistryEntry> ingredients;
    private final Map<String, RegistryEntry> recipes;
    private final Map<String, RegistryEntry> materials;

    public WynnCraftIngredientService() {
        this(loadIngredients(INGREDIENTS_RESOURCE), loadRecipes(RECIPES_RESOURCE));
    }

    public WynnCraftIngredientService(List<RegistryEntry> ingredients, List<RegistryEntry> recipes) {
        this.ingredients = index(ingredients);
        this.recipes = index(recipes);
        this.materials = indexMaterials(recipes);
    }

    public Optional<String> ingredientName(String name) {
        return lookup(ingredients, name).map(RegistryEntry::name);
    }

    public Optional<String> recipeName(String name) {
        return lookup(recipes, name).map(RegistryEntry::name);
    }

    public Optional<String> materialName(String name) {
        return lookup(materials, name).map(RegistryEntry::name);
    }

    public String requireName(String name, RequirementType type) {
        return switch (type) {
            case MATERIAL -> materialName(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown WCI material: " + name));
            case INGREDIENT -> ingredientName(name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown WCI ingredient: " + name));
        };
    }

    public IngredientRequirement requireRequirement(
            String name,
            int amount,
            RequirementType type,
            int materialTier,
            String source) {
        if (type == RequirementType.MATERIAL) {
            RegistryEntry material = lookup(materials, name)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown WCI material: " + name));
            return IngredientRequirement.material(
                    material.id(),
                    material.name(),
                    amount,
                    source,
                    materialTier > 0 ? materialTier : 1);
        }

        RegistryEntry ingredient = lookup(ingredients, name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown WCI ingredient: " + name));
        return new IngredientRequirement(
                RequirementType.INGREDIENT,
                ingredient.id(),
                ingredient.name(),
                amount,
                source,
                0);
    }

    public RegistryEntry requireRecipe(String recipeId) {
        return lookup(recipes, recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown WCI recipe: " + recipeId));
    }

    private Optional<RegistryEntry> lookup(Map<String, RegistryEntry> map, String name) {
        return Optional.ofNullable(map.get(IngredientNormalizer.key(name)));
    }

    private static Map<String, RegistryEntry> index(List<RegistryEntry> entries) {
        Map<String, RegistryEntry> indexed = new LinkedHashMap<>();
        for (RegistryEntry entry : entries) {
            addIndex(indexed, entry.id(), entry);
            addIndex(indexed, entry.name(), entry);
            if (entry.aliases() != null) entry.aliases().forEach(alias -> addIndex(indexed, alias, entry));
        }
        return indexed;
    }

    private static Map<String, RegistryEntry> indexMaterials(List<RegistryEntry> recipes) {
        Map<String, RegistryEntry> indexed = new LinkedHashMap<>();
        for (RegistryEntry recipe : recipes) {
            if (recipe.materials() == null || recipe.materials().isEmpty()) {
                addIndex(indexed, recipe.id(), recipe);
                addIndex(indexed, recipe.name(), recipe);
                if (recipe.aliases() != null) recipe.aliases().forEach(alias -> addIndex(indexed, alias, recipe));
                continue;
            }
            for (MaterialRequirement material : recipe.materials()) {
                String id = IngredientNormalizer.key(material.name());
                RegistryEntry entry = new RegistryEntry(id, material.name(), List.of(material.name()));
                addIndex(indexed, id, entry);
                addIndex(indexed, material.name(), entry);
            }
        }
        return indexed;
    }

    private static void addIndex(Map<String, RegistryEntry> indexed, String key, RegistryEntry entry) {
        if (key != null && !key.isBlank()) indexed.putIfAbsent(IngredientNormalizer.key(key), entry);
    }

    private static List<RegistryEntry> loadIngredients(String path) {
        JsonObject root = loadJsonObject(path);
        List<RegistryEntry> entries = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String rawId = entry.getKey();
            String name = entry.getValue().getAsString();
            String normalizedId = IngredientNormalizer.key(name);
            entries.add(new RegistryEntry(normalizedId, name, List.of(rawId, "wb:" + rawId)));
        }
        if (entries.isEmpty()) throw new IllegalStateException("WCI ingredient registry is empty: " + path);
        return List.copyOf(entries);
    }

    private static List<RegistryEntry> loadRecipes(String path) {
        JsonObject root = loadJsonObject(path);
        List<RegistryEntry> entries = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String rawId = entry.getKey();
            JsonObject body = entry.getValue().getAsJsonObject();
            String label = stringField(body, "label");
            String type = stringField(body, "type");
            String name = label.isBlank() ? type : label;
            Set<String> aliases = new LinkedHashSet<>();
            aliases.add(rawId);
            aliases.add("wb:" + rawId);
            aliases.add(type);
            aliases.add(stringField(body, "name"));
            aliases.add(stringField(body, "skill"));
            entries.add(new RegistryEntry(
                    IngredientNormalizer.key(name),
                    name,
                    aliases.stream().filter(alias -> alias != null && !alias.isBlank()).toList(),
                    materials(body)));
        }
        if (entries.isEmpty()) throw new IllegalStateException("WCI recipe registry is empty: " + path);
        return List.copyOf(entries);
    }

    private static JsonObject loadJsonObject(String path) {
        try (var in = WynnCraftIngredientService.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing WCI registry resource: " + path);
            try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load WCI registry resource: " + path, e);
        }
    }

    private static List<MaterialRequirement> materials(JsonObject recipe) {
        JsonArray materials = recipe.getAsJsonArray("materials");
        if (materials == null) return List.of();

        List<MaterialRequirement> parsed = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            JsonObject material = materials.get(i).getAsJsonObject();
            String item = stringField(material, "item");
            int amount = intField(material, "amount");
            int slot = intField(material, "slot");
            if (!item.isBlank() && amount > 0) {
                parsed.add(new MaterialRequirement(item, amount, 0, slot > 0 ? "material2" : "material1"));
            }
        }
        return List.copyOf(parsed);
    }

    private static String stringField(JsonObject json, String fieldName) {
        JsonElement value = json.get(fieldName);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int intField(JsonObject json, String fieldName) {
        JsonElement value = json.get(fieldName);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    public record RegistryEntry(String id, String name, List<String> aliases, List<MaterialRequirement> materials) {
        public RegistryEntry(String id, String name, List<String> aliases) {
            this(id, name, aliases, List.of());
        }

        public RegistryEntry {
            id = id == null || id.isBlank() ? IngredientNormalizer.key(name) : id;
            name = name == null ? "" : name;
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            materials = materials == null ? List.of() : List.copyOf(materials);
        }
    }

    public record MaterialRequirement(String name, int amount, int tier, String source) {
        public MaterialRequirement {
            name = name == null ? "" : name;
            amount = Math.max(0, amount);
            tier = Math.max(0, tier);
            source = source == null ? "" : source;
        }
    }
}
