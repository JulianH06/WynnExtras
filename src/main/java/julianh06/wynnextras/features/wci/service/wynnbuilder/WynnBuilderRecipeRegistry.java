package julianh06.wynnextras.features.wci.service.wynnbuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WynnBuilderRecipeRegistry {
    private static final String RESOURCE_PATH = "/assets/wynnextras/wci/wynnbuilder_recipes.json";

    private final Map<Integer, Recipe> recipes;

    public WynnBuilderRecipeRegistry() {
        this(loadResource());
    }

    WynnBuilderRecipeRegistry(Map<Integer, Recipe> recipes) {
        this.recipes = Map.copyOf(recipes);
    }

    public String displayName(int id) {
        Recipe recipe = recipes.get(id);
        return recipe == null ? null : recipe.label();
    }

    public Recipe recipe(int id) {
        return recipes.get(id);
    }

    private static Map<Integer, Recipe> loadResource() {
        try (InputStream stream = WynnBuilderRecipeRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing WynnBuilder recipe registry resource: " + RESOURCE_PATH);
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return parse(json);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load WynnBuilder recipe registry resource: " + RESOURCE_PATH, e);
        }
    }

    private static Map<Integer, Recipe> parse(String json) {
        Map<Integer, Recipe> parsed = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            int id = Integer.parseInt(entry.getKey());
            JsonObject body = entry.getValue().getAsJsonObject();
            parsed.put(id, new Recipe(
                    id,
                    stringField(body, "type"),
                    stringField(body, "label"),
                    stringField(body, "skill"),
                    stringField(body, "name"),
                    intField(body, "levelMin"),
                    intField(body, "levelMax"),
                    materials(body)));
        }
        if (parsed.isEmpty()) {
            throw new IllegalStateException("WynnBuilder recipe registry resource is empty: " + RESOURCE_PATH);
        }
        return parsed;
    }

    private static String stringField(JsonObject json, String fieldName) {
        JsonElement value = json.get(fieldName);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int intField(JsonObject json, String fieldName) {
        JsonElement value = json.get(fieldName);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private static List<RecipeMaterial> materials(JsonObject json) {
        JsonArray materials = json.getAsJsonArray("materials");
        if (materials == null) return List.of();

        List<RecipeMaterial> parsed = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            JsonObject material = materials.get(i).getAsJsonObject();
            String item = stringField(material, "item");
            int amount = intField(material, "amount");
            int slot = intField(material, "slot");
            if (!item.isBlank() && amount > 0) {
                parsed.add(new RecipeMaterial(item, amount, slot > 0 ? slot : i));
            }
        }
        return List.copyOf(parsed);
    }

    public record Recipe(
            int id,
            String type,
            String label,
            String skill,
            String name,
            int levelMin,
            int levelMax,
            List<RecipeMaterial> materials) {
        public Recipe {
            type = type == null ? "" : type;
            label = label == null || label.isBlank() ? type : label;
            skill = skill == null ? "" : skill;
            name = name == null ? "" : name;
            materials = materials == null ? List.of() : List.copyOf(materials);
        }
    }

    public record RecipeMaterial(String item, int amount, int slot) {
        public RecipeMaterial {
            item = item == null ? "" : item;
            amount = Math.max(0, amount);
            slot = Math.max(0, slot);
        }
    }
}
