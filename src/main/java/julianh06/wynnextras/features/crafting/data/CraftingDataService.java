package julianh06.wynnextras.features.crafting.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.features.crafting.model.*;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.joml.Vector2i;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CraftingDataService {
    public enum State {
        NOT_LOADED,
        LOADING,
        READY,
        UNAVAILABLE
    }

    public record Material(String item, int amount) {}
    public record PowderData(int element, int tier, int damageMinimum, int damageMaximum,
                             int conversion, int defencePlus, int defenceMinus) {}

    public record RecipeData(
            CraftableType type,
            WEProfessionType skill,
            List<Material> materials,
            Vector2i healthOrDamage,
            Vector2i durability,
            Vector2i duration,
            Vector2i basicDuration,
            Vector2i level,
            String name,
            int wynnBuilderId
    ) {
        public RecipeData {
            materials = List.copyOf(materials);
            healthOrDamage = copy(healthOrDamage);
            durability = copy(durability);
            duration = copy(duration);
            basicDuration = copy(basicDuration);
            level = copy(level);
        }

        private static Vector2i copy(Vector2i value) {
            return value == null ? null : new Vector2i(value);
        }
    }

    private record LoadedData(
            Map<String, IngredientInfo> ingredientsByName,
            Map<Integer, String> ingredientNamesByWynnBuilderId,
            Map<CraftableType, Map<String, RecipeData>> recipesByTypeAndLevel,
            Map<Integer, RecipeData> recipesByWynnBuilderId,
            Map<String, JsonObject> itemDatabase
    ) {}

    record CachedPayloads(
            int schemaVersion,
            long fetchedAt,
            String recipes,
            String items,
            String ingredientMap,
            String wynnBuilderRecipes
    ) {}

    private static final URI RECIPES_URI = URI.create("https://api.wynncraft.com/v3/item/recipe/database?full_result");
    private static final URI ITEMS_URI = URI.create("https://api.wynncraft.com/v3/item/database?fullResult");
    private static final URI INGREDIENT_MAP_URI = URI.create("https://raw.githubusercontent.com/wynnbuilder-beta/wynnbuilder-beta.github.io/master/data/baseline/maps/ing_map.json");
    private static final URI WYNNBUILDER_DATA_URI = URI.create("https://api.github.com/repos/wynnbuilder-beta/wynnbuilder-beta.github.io/contents/data?ref=master");
    private static final String WYNNBUILDER_RAW_DATA_BASE = "https://raw.githubusercontent.com/wynnbuilder-beta/wynnbuilder-beta.github.io/master/data/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int CACHE_SCHEMA_VERSION = 1;
    private static final Gson CACHE_GSON = new GsonBuilder().create();
    // WynnBuilder synthesizes these IDs in js/load_ing.js; no JSON mapping exists.
    private static final int FIRST_POWDER_INGREDIENT_ID = 4001;
    private static final int POWDER_ELEMENTS = 5;
    private static final int POWDER_TIERS = 7;
    private static final String[] POWDER_ELEMENT_NAMES = {"Earth", "Thunder", "Water", "Fire", "Air"};
    private static final String[] POWDER_TIER_NAMES = {"I", "II", "III", "IV", "V", "VI", "VII"};
    private static final int[] POWDER_DURABILITY = {-35, -52, -70, -91, -112, -133, -154};
    private static final int[] POWDER_REQUIREMENT = {0, 0, 10, 20, 28, 36, 44};
    // The API has no powder records. These are WynnBuilder's current crafted-powder protocol values.
    private static final int[][] POWDER_STATS = {
            {4,5,17,2,1}, {6,7,21,5,2}, {7,9,25,9,3}, {8,9,31,14,4}, {9,11,38,22,7}, {11,12,46,29,7}, {12,14,52,37,12},
            {1,8,9,2,1}, {1,12,11,4,1}, {2,14,13,8,2}, {2,15,17,13,3}, {3,17,22,20,5}, {4,19,28,28,6}, {5,21,32,36,11},
            {3,4,13,3,1}, {5,6,15,6,1}, {6,8,17,11,3}, {7,8,21,16,4}, {8,10,26,23,6}, {10,13,32,32,10}, {11,15,38,40,15},
            {2,5,14,3,1}, {4,7,16,6,1}, {5,9,19,10,2}, {6,9,24,15,3}, {7,11,30,22,5}, {9,14,37,31,9}, {10,16,44,39,14},
            {2,6,11,3,1}, {3,9,14,6,2}, {4,11,17,10,3}, {5,11,22,16,5}, {7,12,28,23,7}, {8,15,35,30,8}, {9,17,42,38,13}
    };
    private static final CraftingDataService INSTANCE = new CraftingDataService();

    private final AtomicBoolean started = new AtomicBoolean();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private volatile State state = State.NOT_LOADED;
    private volatile String unavailableReason = "";
    private volatile LoadedData data;
    private volatile List<String> ingredientNames = List.of();

    private CraftingDataService() {}

    public static CraftingDataService getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (!started.compareAndSet(false, true)) return;
        state = State.LOADING;

        CachedPayloads cachedPayloads = readCache(cachePath());
        if (cachedPayloads != null) {
            try {
                setLoadedData(buildData(cachedPayloads.recipes(), cachedPayloads.items(),
                        cachedPayloads.ingredientMap(), cachedPayloads.wynnBuilderRecipes()));
                WynnExtras.LOGGER.info("Loaded crafting data from local cache");
            } catch (RuntimeException ex) {
                WynnExtras.LOGGER.warn("Ignoring invalid crafting data cache: " + rootMessage(ex));
            }
        }

        CompletableFuture<String> recipes = fetch("Wynncraft recipes", RECIPES_URI);
        CompletableFuture<String> items = fetch("Wynncraft items", ITEMS_URI);
        CompletableFuture<String> ingredientMap = fetch("WynnBuilder ingredient map", INGREDIENT_MAP_URI);
        CompletableFuture<String> wynnBuilderRecipes = fetch("WynnBuilder data versions", WYNNBUILDER_DATA_URI)
                .thenCompose(dataVersions -> fetch("WynnBuilder recipes", latestWynnBuilderRecipesUri(dataVersions)));

        CompletableFuture.allOf(recipes, items, ingredientMap, wynnBuilderRecipes)
                .thenApplyAsync(ignored -> buildData(recipes.join(), items.join(), ingredientMap.join(), wynnBuilderRecipes.join()),
                        MinecraftClient.getInstance())
                .whenComplete((loaded, throwable) -> {
                    if (throwable != null) {
                        String reason = rootMessage(throwable);
                        if (data == null) {
                            unavailableReason = reason;
                            state = State.UNAVAILABLE;
                            WynnExtras.LOGGER.error("Crafting data unavailable for this session: " + reason);
                        } else {
                            WynnExtras.LOGGER.warn("Could not refresh crafting data; using local cache: " + reason);
                        }
                        return;
                    }
                    setLoadedData(loaded);
                    writeCache(cachePath(), new CachedPayloads(
                            CACHE_SCHEMA_VERSION,
                            System.currentTimeMillis(),
                            recipes.join(),
                            items.join(),
                            ingredientMap.join(),
                            wynnBuilderRecipes.join()));
                    WynnExtras.LOGGER.info("Loaded " + loaded.ingredientsByName().size() + " crafting ingredients and "
                            + loaded.recipesByWynnBuilderId().size() + " recipes");
                });
    }

    private void setLoadedData(LoadedData loaded) {
        data = loaded;
        ingredientNames = buildIngredientNames(loaded);
        unavailableReason = "";
        WynncraftApiHandler.setCachedItemDatabase(loaded.itemDatabase());
        state = State.READY;
    }

    private static Path cachePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras")
                .resolve("cache")
                .resolve("crafting_data.json");
    }

    static CachedPayloads readCache(Path path) {
        if (path == null || !Files.isRegularFile(path)) return null;
        try {
            CachedPayloads cached = CACHE_GSON.fromJson(Files.readString(path), CachedPayloads.class);
            if (cached == null
                    || cached.schemaVersion() != CACHE_SCHEMA_VERSION
                    || isBlank(cached.recipes())
                    || isBlank(cached.items())
                    || isBlank(cached.ingredientMap())
                    || isBlank(cached.wynnBuilderRecipes())) {
                return null;
            }
            return cached;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    static boolean writeCache(Path path, CachedPayloads payloads) {
        if (path == null || payloads == null) return false;
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temp, CACHE_GSON.toJson(payloads));
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            WynnExtras.LOGGER.warn("Could not write crafting data cache: " + ex.getMessage());
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public State getState() {
        return state;
    }

    public String getStatusMessage() {
        return switch (state) {
            case NOT_LOADED, LOADING -> "Loading current crafting data...";
            case READY -> "";
            case UNAVAILABLE -> "Crafting data unavailable: " + unavailableReason;
        };
    }

    public IngredientInfo getIngredient(String name) {
        LoadedData current = data;
        if (current == null || name == null) return null;
        IngredientInfo ingredient = current.ingredientsByName().get(name);
        return ingredient != null ? ingredient : createPowderIngredient(name);
    }

    public PowderData getPowder(String name) {
        int index = powderIndex(name);
        if (index < 0) return null;
        int[] stats = POWDER_STATS[index];
        return new PowderData(index / POWDER_TIERS, index % POWDER_TIERS + 1,
                stats[0], stats[1], stats[2], stats[3], stats[4]);
    }

    public String getIngredientNameByWynnBuilderId(int id) {
        LoadedData current = data;
        if (current == null) return null;
        String name = current.ingredientNamesByWynnBuilderId().get(id);
        if (name != null) return name;
        int powderIndex = id - FIRST_POWDER_INGREDIENT_ID;
        if (powderIndex < 0 || powderIndex >= POWDER_ELEMENTS * POWDER_TIERS) return null;
        return POWDER_ELEMENT_NAMES[powderIndex / POWDER_TIERS] + " Powder "
                + POWDER_TIER_NAMES[powderIndex % POWDER_TIERS];
    }

    public List<String> getIngredientNames() {
        return ingredientNames;
    }

    private static List<String> buildIngredientNames(LoadedData loaded) {
        if (loaded == null) return List.of();
        List<String> names = new ArrayList<>(loaded.ingredientsByName().keySet());
        for (int element = 0; element < POWDER_ELEMENTS; element++) {
            for (String tier : POWDER_TIER_NAMES) {
                names.add(POWDER_ELEMENT_NAMES[element] + " Powder " + tier);
            }
        }
        return names.stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(String::compareTo))
                .toList();
    }

    public RecipeData getRecipe(CraftableType type, Vector2i level) {
        LoadedData current = data;
        if (current == null || type == null || level == null) return null;
        Map<String, RecipeData> recipes = current.recipesByTypeAndLevel().get(type);
        return recipes == null ? null : recipes.get(levelKey(level.x, level.y));
    }

    public RecipeData getRecipeByWynnBuilderId(int id) {
        LoadedData current = data;
        return current == null ? null : current.recipesByWynnBuilderId().get(id);
    }

    private CompletableFuture<String> fetch(String source, URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException(source + " returned HTTP " + response.statusCode());
                    }
                    return response.body();
                });
    }

    private LoadedData buildData(String recipesJson, String itemsJson, String ingredientMapJson, String wynnBuilderRecipesJson) {
        Map<String, Integer> ingredientIds = parseIdMap(ingredientMapJson, "ingredient");
        Map<String, Integer> recipeIds = parseWynnBuilderRecipeIds(wynnBuilderRecipesJson);
        Map<String, JsonObject> itemDatabase = parseItemDatabase(itemsJson);
        Map<String, IngredientInfo> ingredients = parseIngredients(itemsJson);
        Map<Integer, String> ingredientNamesById = new HashMap<>();
        for (Map.Entry<String, Integer> entry : ingredientIds.entrySet()) {
            if (!ingredients.containsKey(entry.getKey())) continue;
            if (ingredientNamesById.put(entry.getValue(), entry.getKey()) != null) {
                throw new IllegalStateException("Duplicate WynnBuilder ingredient ID " + entry.getValue());
            }
        }

        Map<CraftableType, Map<String, RecipeData>> recipesByType = new EnumMap<>(CraftableType.class);
        Map<Integer, RecipeData> recipesById = new HashMap<>();
        JsonElement root = JsonParser.parseString(recipesJson);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("Wynncraft recipe response is not a non-empty array");
        }
        for (JsonElement element : root.getAsJsonArray()) {
            JsonObject object = requiredObject(element, "recipe");
            String internalName = requiredString(object, "internalName");
            String mappingName = toWynnBuilderRecipeName(internalName);
            Integer id = recipeIds.get(mappingName);

            CraftableType type = parseCraftableType(requiredString(object, "type"));
            WEProfessionType profession = parseProfession(requiredString(object, "skill"));
            Vector2i level = range(object, "level", 1);
            Vector2i durability = type.isConsumable() ? optionalRange(object, "durability", 1000)
                    : range(object, "durability", 1000);
            Vector2i healthOrDamage = rangeOrZero(object, "healthOrDamage", 1);
            Vector2i duration = type.isConsumable() ? range(object, "duration", 1)
                    : optionalRange(object, "duration", 1);
            Vector2i basicDuration = type.isConsumable() ? range(object, "basicDuration", 1)
                    : optionalRange(object, "basicDuration", 1);
            List<Material> materials = parseMaterials(object);
            if (materials.size() != 2) throw new IllegalStateException(mappingName + " does not have exactly two materials");

            RecipeData recipe = new RecipeData(type, profession, materials, healthOrDamage, durability,
                    duration, basicDuration, level, mappingName, id == null ? -1 : id);
            recipesByType.computeIfAbsent(type, ignored -> new HashMap<>())
                    .put(levelKey(level.x, level.y), recipe);
            if (id != null && recipesById.put(id, recipe) != null) {
                throw new IllegalStateException("Duplicate WynnBuilder recipe ID " + id);
            }
        }
        if (recipesById.size() != recipeIds.size()) {
            throw new IllegalStateException("Recipe API/map mismatch: API resolved " + recipesById.size()
                    + " of " + recipeIds.size() + " WynnBuilder recipes");
        }

        Map<CraftableType, Map<String, RecipeData>> immutableRecipes = new EnumMap<>(CraftableType.class);
        recipesByType.forEach((key, value) -> immutableRecipes.put(key, Map.copyOf(value)));
        return new LoadedData(Map.copyOf(ingredients), Map.copyOf(ingredientNamesById),
                Collections.unmodifiableMap(immutableRecipes), Map.copyOf(recipesById), itemDatabase);
    }

    private static Map<String, JsonObject> parseItemDatabase(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("Wynncraft item response is not a non-empty array");
        }
        Map<String, JsonObject> result = new HashMap<>();
        for (JsonElement element : root.getAsJsonArray()) {
            JsonObject object = requiredObject(element, "item");
            String name = requiredString(object, "internalName");
            if (result.put(name, object.deepCopy()) != null) throw new IllegalStateException("Duplicate item " + name);
        }
        return Map.copyOf(result);
    }

    private Map<String, IngredientInfo> parseIngredients(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("Wynncraft item response is not a non-empty array");
        }
        Map<String, IngredientInfo> result = new HashMap<>();
        for (JsonElement element : root.getAsJsonArray()) {
            JsonObject object = requiredObject(element, "item");
            if (!"ingredient".equals(optionalString(object, "type"))) continue;
            String name = requiredString(object, "displayName");
            JsonObject requirements = requiredObject(object.get("requirements"), name + ".requirements");
            int level = requiredInt(requirements, "level");
            List<WEProfessionType> professions = new ArrayList<>();
            for (JsonElement skill : requiredArray(requirements, "skills")) {
                professions.add(parseProfession(skill.getAsString()));
            }

            List<Pair<StatType, RangedValue>> identifications = new ArrayList<>();
            JsonElement identificationsElement = object.get("identifications");
            if (identificationsElement != null && !identificationsElement.isJsonNull()) {
                JsonObject ids = requiredObject(identificationsElement, name + ".identifications");
                for (Map.Entry<String, JsonElement> entry : ids.entrySet()) {
                    StatType statType = StatType.fromApiName(entry.getKey());
                    if (statType == null) throw new IllegalStateException("Unknown Wynncraft identification " + entry.getKey());
                    JsonObject range = requiredObject(entry.getValue(), name + "." + entry.getKey());
                    identifications.add(new Pair<>(statType,
                            RangedValue.of(requiredInt(range, "min"), requiredInt(range, "max"))));
                }
            }

            JsonObject position = requiredObject(object.get("ingredientPositionModifiers"), name + ".positionModifiers");
            Map<IngredientPosition, Integer> positionModifiers = new EnumMap<>(IngredientPosition.class);
            positionModifiers.put(IngredientPosition.LEFT, requiredInt(position, "left"));
            positionModifiers.put(IngredientPosition.RIGHT, requiredInt(position, "right"));
            positionModifiers.put(IngredientPosition.ABOVE, requiredInt(position, "above"));
            positionModifiers.put(IngredientPosition.UNDER, requiredInt(position, "under"));
            positionModifiers.put(IngredientPosition.TOUCHING, requiredInt(position, "touching"));
            positionModifiers.put(IngredientPosition.NOT_TOUCHING, requiredInt(position, "notTouching"));

            JsonObject itemOnly = requiredObject(object.get("itemOnlyIDs"), name + ".itemOnlyIDs");
            List<Pair<Skill, Integer>> skillRequirements = List.of(
                    new Pair<>(Skill.STRENGTH, requiredInt(itemOnly, "strengthRequirement")),
                    new Pair<>(Skill.DEXTERITY, requiredInt(itemOnly, "dexterityRequirement")),
                    new Pair<>(Skill.INTELLIGENCE, requiredInt(itemOnly, "intelligenceRequirement")),
                    new Pair<>(Skill.DEFENCE, requiredInt(itemOnly, "defenceRequirement")),
                    new Pair<>(Skill.AGILITY, requiredInt(itemOnly, "agilityRequirement"))
            );
            JsonObject consumable = requiredObject(object.get("consumableOnlyIDs"), name + ".consumableOnlyIDs");
            int tier = parseTier(requiredString(object, "tier"));
            IngredientInfo ingredient = new IngredientInfo(name, tier, level, Optional.of(requiredString(object, "internalName")),
                    ItemStack.EMPTY, List.copyOf(professions), skillRequirements, Map.copyOf(positionModifiers), List.of(),
                    requiredInt(consumable, "duration") / 1000, requiredInt(consumable, "charges"),
                    requiredInt(itemOnly, "durabilityModifier") / 1000, List.copyOf(identifications));
            if (result.put(name, ingredient) != null) throw new IllegalStateException("Duplicate ingredient " + name);
        }
        if (result.isEmpty()) throw new IllegalStateException("Wynncraft API returned no ingredients");
        return result;
    }

    private static List<Material> parseMaterials(JsonObject recipe) {
        List<Material> materials = new ArrayList<>();
        for (JsonElement element : requiredArray(recipe, "materials")) {
            JsonObject material = requiredObject(element, "recipe material");
            materials.add(new Material(requiredString(material, "item"), requiredInt(material, "amount")));
        }
        return materials;
    }

    private static Map<String, Integer> parseIdMap(String json, String description) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject() || root.getAsJsonObject().isEmpty()) {
            throw new IllegalStateException("WynnBuilder " + description + " map is empty");
        }
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsInt());
        }
        return result;
    }

    private static Map<String, Integer> parseWynnBuilderRecipeIds(String json) {
        JsonObject root = requiredObject(JsonParser.parseString(json), "WynnBuilder recipes");
        Map<String, Integer> result = new HashMap<>();
        for (JsonElement element : requiredArray(root, "recipes")) {
            JsonObject recipe = requiredObject(element, "WynnBuilder recipe");
            result.put(requiredString(recipe, "name"), requiredInt(recipe, "id"));
        }
        if (result.isEmpty()) throw new IllegalStateException("WynnBuilder recipe list is empty");
        return result;
    }

    private static URI latestWynnBuilderRecipesUri(String dataVersionsJson) {
        JsonElement root = JsonParser.parseString(dataVersionsJson);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("WynnBuilder data version response is empty");
        }
        String latest = null;
        for (JsonElement element : root.getAsJsonArray()) {
            JsonObject object = requiredObject(element, "WynnBuilder data entry");
            if (!"dir".equals(optionalString(object, "type"))) continue;
            String name = requiredString(object, "name");
            if (!isVersionDirectory(name)) continue;
            if (latest == null || compareVersions(name, latest) > 0) latest = name;
        }
        if (latest == null) throw new IllegalStateException("No WynnBuilder data version directory found");
        return URI.create(WYNNBUILDER_RAW_DATA_BASE + latest + "/recipes.json");
    }

    private static boolean isVersionDirectory(String name) {
        String[] parts = name.split("\\.");
        if (parts.length == 0) return false;
        for (String part : parts) {
            if (part.isEmpty()) return false;
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) return false;
            }
        }
        return true;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? Integer.parseInt(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? Integer.parseInt(rightParts[i]) : 0;
            if (leftValue != rightValue) return Integer.compare(leftValue, rightValue);
        }
        return 0;
    }

    private static CraftableType parseCraftableType(String value) {
        try {
            return CraftableType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown recipe type " + value, e);
        }
    }

    private static WEProfessionType parseProfession(String value) {
        try {
            return WEProfessionType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown crafting profession " + value, e);
        }
    }

    private static int parseTier(String value) {
        if (!value.startsWith("TIER_")) throw new IllegalStateException("Unknown ingredient tier " + value);
        return Integer.parseInt(value.substring(5));
    }

    private static String toWynnBuilderRecipeName(String internalName) {
        int digit = -1;
        for (int i = 0; i < internalName.length(); i++) {
            if (Character.isDigit(internalName.charAt(i))) {
                digit = i;
                break;
            }
        }
        if (digit < 1) throw new IllegalStateException("Invalid recipe internalName " + internalName);
        String type = internalName.substring(0, digit);
        if ("Pants".equals(type)) type = "Leggings";
        return type + "-" + internalName.substring(digit);
    }

    private static String levelKey(int minimum, int maximum) {
        return minimum + "-" + maximum;
    }

    private static IngredientInfo createPowderIngredient(String name) {
        int index = powderIndex(name);
        if (index < 0) return null;
        int element = index / POWDER_TIERS;
        int tier = index % POWDER_TIERS;
        List<Pair<Skill, Integer>> requirements = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            requirements.add(new Pair<>(skill, skill.ordinal() == element ? POWDER_REQUIREMENT[tier] : 0));
        }
        Map<IngredientPosition, Integer> positions = new EnumMap<>(IngredientPosition.class);
        for (IngredientPosition position : IngredientPosition.values()) positions.put(position, 0);
        return new IngredientInfo(name, 0, 0, Optional.empty(), ItemStack.EMPTY,
                List.of(WEProfessionType.ARMOURING, WEProfessionType.TAILORING, WEProfessionType.WEAPONSMITHING,
                        WEProfessionType.WOODWORKING, WEProfessionType.JEWELING),
                List.copyOf(requirements), Map.copyOf(positions), List.of(), 0, 0,
                POWDER_DURABILITY[tier], List.of());
    }

    private static int powderIndex(String name) {
        if (name == null) return -1;
        for (int element = 0; element < POWDER_ELEMENT_NAMES.length; element++) {
            for (int tier = 0; tier < POWDER_TIER_NAMES.length; tier++) {
                if ((POWDER_ELEMENT_NAMES[element] + " Powder " + POWDER_TIER_NAMES[tier]).equals(name)) {
                    return element * POWDER_TIERS + tier;
                }
            }
        }
        return -1;
    }

    private static Vector2i range(JsonObject parent, String field, int divisor) {
        JsonObject object = requiredObject(parent.get(field), field);
        return new Vector2i(requiredInt(object, "minimum") / divisor, requiredInt(object, "maximum") / divisor);
    }

    private static Vector2i optionalRange(JsonObject parent, String field, int divisor) {
        JsonElement element = parent.get(field);
        return element == null || element.isJsonNull() ? null : range(parent, field, divisor);
    }

    private static Vector2i rangeOrZero(JsonObject parent, String field, int divisor) {
        JsonObject object = requiredObject(parent.get(field), field);
        if (object.isEmpty()) return new Vector2i();
        return new Vector2i(requiredInt(object, "minimum") / divisor, requiredInt(object, "maximum") / divisor);
    }

    private static JsonObject requiredObject(JsonElement element, String field) {
        if (element == null || !element.isJsonObject()) throw new IllegalStateException("Missing object " + field);
        return element.getAsJsonObject();
    }

    private static com.google.gson.JsonArray requiredArray(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonArray()) throw new IllegalStateException("Missing array " + field);
        return element.getAsJsonArray();
    }

    private static String requiredString(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Missing string " + field);
        }
        return element.getAsString();
    }

    private static String optionalString(JsonObject object, String field) {
        JsonElement element = object.get(field);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Missing integer " + field);
        }
        return element.getAsInt();
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
