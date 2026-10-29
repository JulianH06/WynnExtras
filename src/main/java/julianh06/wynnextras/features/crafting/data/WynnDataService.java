package julianh06.wynnextras.features.crafting.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.features.crafting.model.*;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import julianh06.wynnextras.wynncraft.item.WynncraftPowders;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class WynnDataService {
    public enum State {
        NOT_LOADED,
        LOADING,
        READY,
        UNAVAILABLE
    }

    public record Material(String item, int amount) {}
    public record StatValue(Double minimum, Double raw, Double maximum, Double value) {
        public boolean isRange() {
            return minimum != null && maximum != null;
        }
    }

    public record ItemIconData(String id, String model, int customModelData) {
        private static final ItemIconData EMPTY = new ItemIconData("", "", 0);
    }

    public record ItemData(
            String internalName,
            String displayName,
            String type,
            String subType,
            String tier,
            String restriction,
            String dropRestriction,
            String attackSpeed,
            Integer powderSlots,
            Map<String, String> requirements,
            Map<String, StatValue> baseStats,
            Map<String, StatValue> identifications,
            Map<String, String> majorIds,
            String lore,
            String emblem,
            ItemIconData icon
    ) {
        public ItemData {
            requirements = Map.copyOf(requirements);
            baseStats = Map.copyOf(baseStats);
            identifications = Map.copyOf(identifications);
            majorIds = Map.copyOf(majorIds);
            lore = lore == null ? "" : lore;
            emblem = emblem == null ? "" : emblem;
            icon = icon == null ? ItemIconData.EMPTY : icon;
        }
    }

    public record ItemSelector(String name, String type, String subType, String tier) {
        public ItemSelector {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Item name must not be blank");
        }

        public static ItemSelector named(String name) {
            return new ItemSelector(name, null, null, null);
        }
    }

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

    public record WynnDataSnapshot(
            List<ItemData> items,
            Map<String, List<ItemData>> itemsByDisplayName,
            Map<String, List<ItemData>> itemsByInternalName,
            Map<String, IngredientInfo> ingredientsByName,
            Map<String, IngredientInfo> ingredientsByInternalName,
            Map<Integer, String> ingredientNamesByWynnBuilderId,
            Map<CraftableType, Map<String, RecipeData>> recipesByTypeAndLevel,
            Map<Integer, RecipeData> recipesByWynnBuilderId
    ) {
        public WynnDataSnapshot {
            items = List.copyOf(items);
            itemsByDisplayName = immutableListMap(itemsByDisplayName);
            itemsByInternalName = immutableListMap(itemsByInternalName);
            ingredientsByName = Map.copyOf(ingredientsByName);
            ingredientsByInternalName = Map.copyOf(ingredientsByInternalName);
            ingredientNamesByWynnBuilderId = Map.copyOf(ingredientNamesByWynnBuilderId);
            recipesByTypeAndLevel = immutableRecipeMap(recipesByTypeAndLevel);
            recipesByWynnBuilderId = Map.copyOf(recipesByWynnBuilderId);
        }
    }

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
    private static final URI WYNNBUILDER_RECIPES_URI = URI.create("https://raw.githubusercontent.com/wynnbuilder-beta/wynnbuilder-beta.github.io/master/data/baseline/recipes_clean.json");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int CACHE_SCHEMA_VERSION = 1;
    private static final Gson CACHE_GSON = new GsonBuilder().create();
    // WynnBuilder synthesizes these IDs in js/load_ing.js; no JSON mapping exists.
    private static final int FIRST_POWDER_INGREDIENT_ID = 4001;
    private static final WynnDataService INSTANCE = new WynnDataService();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Set<String> loggedAmbiguities = ConcurrentHashMap.newKeySet();
    private volatile State state = State.NOT_LOADED;
    private volatile String unavailableReason = "";
    private volatile WynnDataSnapshot data;
    private volatile List<String> ingredientNames = List.of();
    private CompletableFuture<WynnDataSnapshot> initialization;
    private CompletableFuture<List<ItemData>> itemInitialization;

    private WynnDataService() {}

    public static WynnDataService getInstance() {
        return INSTANCE;
    }

    public synchronized CompletableFuture<WynnDataSnapshot> initialize() {
        if (initialization != null) return initialization;
        state = State.LOADING;

        CachedPayloads cachedPayloads = readCache(cachePath());
        if (cachedPayloads != null) {
            try {
                setLoadedData(buildSnapshot(cachedPayloads.recipes(), cachedPayloads.items(),
                        cachedPayloads.ingredientMap(), cachedPayloads.wynnBuilderRecipes()));
                WynnExtras.LOGGER.info("Loaded Wynn data from local cache");
            } catch (RuntimeException ex) {
                WynnExtras.LOGGER.warn("Ignoring invalid Wynn data cache: " + rootMessage(ex));
            }
        }

        CompletableFuture<String> recipes = fetch("Wynncraft recipes", RECIPES_URI);
        CompletableFuture<ParsedItems> items = fetch("Wynncraft items", ITEMS_URI).thenApply(WynnDataService::parseItemPayload);
        itemInitialization = items.thenApply(parsed -> parsed.catalog().items());
        CompletableFuture<String> ingredientMap = fetch("WynnBuilder ingredient map", INGREDIENT_MAP_URI);
        CompletableFuture<String> wynnBuilderRecipes = fetch("WynnBuilder recipes", WYNNBUILDER_RECIPES_URI);

        initialization = CompletableFuture.allOf(recipes, items, ingredientMap, wynnBuilderRecipes)
                .thenApply(ignored -> buildSnapshot(recipes.join(), items.join(), ingredientMap.join(),
                        wynnBuilderRecipes.join()))
                .handle((loaded, throwable) -> {
                    if (throwable != null) {
                        String reason = rootMessage(throwable);
                        if (data == null) {
                            unavailableReason = reason;
                            state = State.UNAVAILABLE;
                            WynnExtras.LOGGER.error("Wynn data unavailable for this session: " + reason);
                            throw new IllegalStateException(reason, throwable);
                        } else {
                            WynnExtras.LOGGER.warn("Could not refresh Wynn data; using local cache: " + reason);
                        }
                        return data;
                    }
                    setLoadedData(loaded);
                    writeCache(cachePath(), new CachedPayloads(
                            CACHE_SCHEMA_VERSION,
                            System.currentTimeMillis(),
                            recipes.join(),
                            items.join().json(),
                            ingredientMap.join(),
                            wynnBuilderRecipes.join()));
                    WynnExtras.LOGGER.info("Loaded " + loaded.items().size() + " items, "
                            + loaded.ingredientsByName().size() + " crafting ingredients and "
                            + loaded.recipesByWynnBuilderId().size() + " recipes");
                    return loaded;
                });
        return initialization;
    }

    public synchronized CompletableFuture<List<ItemData>> initializeItems() {
        if (itemInitialization == null) initialize();
        return itemInitialization;
    }

    private void setLoadedData(WynnDataSnapshot loaded) {
        data = loaded;
        ingredientNames = buildIngredientNames(loaded);
        unavailableReason = "";
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
            WynnExtras.LOGGER.warn("Could not write Wynn data cache: " + ex.getMessage());
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public State getState() {
        return state;
    }

    public WynnDataSnapshot snapshot() {
        return data;
    }

    public String getStatusMessage() {
        return switch (state) {
            case NOT_LOADED, LOADING -> "Loading current Wynn data...";
            case READY -> "";
            case UNAVAILABLE -> "Wynn data unavailable: " + unavailableReason;
        };
    }

    public List<ItemData> findByDisplayName(String name) {
        WynnDataSnapshot current = data;
        if (current == null || name == null) return List.of();
        return current.itemsByDisplayName().getOrDefault(normalizeName(name), List.of());
    }

    public List<ItemData> findByInternalName(String name) {
        WynnDataSnapshot current = data;
        if (current == null || name == null) return List.of();
        return current.itemsByInternalName().getOrDefault(normalizeName(name), List.of());
    }

    public Optional<ItemData> resolveItem(ItemSelector selector) {
        WynnDataSnapshot current = data;
        List<ItemData> matches = matchingItems(current, selector);
        if (matches.size() == 1) return Optional.of(matches.getFirst());
        if (matches.size() > 1) {
            String key = normalizeName(selector.name()) + '|' + selector.type() + '|' + selector.subType() + '|'
                    + selector.tier();
            if (loggedAmbiguities.add(key)) {
                WynnExtras.LOGGER.warn("Ambiguous Wynn item lookup for " + selector + ": " + matches.size()
                        + " candidates");
            }
        }
        return Optional.empty();
    }

    static Optional<ItemData> resolveItem(WynnDataSnapshot snapshot, ItemSelector selector) {
        List<ItemData> matches = matchingItems(snapshot, selector);
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private static List<ItemData> matchingItems(WynnDataSnapshot snapshot, ItemSelector selector) {
        if (snapshot == null || selector == null) return List.of();
        List<ItemData> candidates = snapshot.itemsByDisplayName()
                .getOrDefault(normalizeName(selector.name()), List.of());
        if (candidates.isEmpty()) {
            candidates = snapshot.itemsByInternalName().getOrDefault(normalizeName(selector.name()), List.of());
        }
        return candidates.stream()
                .filter(item -> matches(item.type(), selector.type()))
                .filter(item -> matches(item.subType(), selector.subType()))
                .filter(item -> matches(item.tier(), selector.tier()))
                .toList();
    }

    private static boolean matches(String actual, String expected) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(actual);
    }

    public IngredientInfo getIngredient(String name) {
        WynnDataSnapshot current = data;
        if (current == null || name == null) return null;
        IngredientInfo ingredient = current.ingredientsByName().get(name);
        return ingredient != null ? ingredient : createPowderIngredient(name);
    }

    public WynncraftPowders.Powder getPowder(String name) {
        return WynncraftPowders.fromName(name);
    }

    public String getIngredientNameByWynnBuilderId(int id) {
        WynnDataSnapshot current = data;
        if (current == null) return null;
        String name = current.ingredientNamesByWynnBuilderId().get(id);
        if (name != null) return name;
        int powderIndex = id - FIRST_POWDER_INGREDIENT_ID;
        WynncraftPowders.Powder powder = WynncraftPowders.fromId(powderIndex);
        return powder == null ? null : powder.name();
    }

    public List<String> getIngredientNames() {
        return ingredientNames;
    }

    private static List<String> buildIngredientNames(WynnDataSnapshot loaded) {
        if (loaded == null) return List.of();
        List<String> names = new ArrayList<>(loaded.ingredientsByName().keySet());
        for (int element = 0; element < WynncraftPowders.ELEMENT_COUNT; element++) {
            for (int tier = 1; tier <= WynncraftPowders.TIER_COUNT; tier++) {
                names.add(WynncraftPowders.get(element, tier).name());
            }
        }
        return names.stream()
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(String::compareTo))
                .toList();
    }

    public RecipeData getRecipe(CraftableType type, Vector2i level) {
        WynnDataSnapshot current = data;
        if (current == null || type == null || level == null) return null;
        Map<String, RecipeData> recipes = current.recipesByTypeAndLevel().get(type);
        return recipes == null ? null : recipes.get(levelKey(level.x, level.y));
    }

    public RecipeData getRecipeByWynnBuilderId(int id) {
        WynnDataSnapshot current = data;
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

    static WynnDataSnapshot buildSnapshot(String recipesJson, String itemsJson, String ingredientMapJson,
                                          String wynnBuilderRecipesJson) {
        return buildSnapshot(recipesJson, parseItemPayload(itemsJson), ingredientMapJson, wynnBuilderRecipesJson);
    }

    private static WynnDataSnapshot buildSnapshot(String recipesJson, ParsedItems parsedItems,
            String ingredientMapJson, String wynnBuilderRecipesJson) {
        Map<String, Integer> ingredientIds = parseIdMap(ingredientMapJson, "ingredient");
        Map<String, Integer> recipeIds = parseWynnBuilderRecipeIds(wynnBuilderRecipesJson);
        validateUniqueIds(ingredientIds, "ingredient");
        validateUniqueIds(recipeIds, "recipe");
        JsonArray itemArray = parsedItems.root();
        ItemCatalog itemCatalog = parsedItems.catalog();
        IngredientCatalog ingredientCatalog = parseIngredients(itemArray);
        Map<Integer, String> ingredientNamesById = new HashMap<>();
        for (Map.Entry<String, IngredientInfo> entry : ingredientCatalog.byInternalName().entrySet()) {
            Integer id = ingredientIds.get(entry.getKey());
            if (id == null) {
                WynnExtras.LOGGER.warn("Skipping unmapped Wynncraft ingredient {}.", entry.getKey());
                continue;
            }
            if (id >= 4000) {
                WynnExtras.LOGGER.warn("Skipping Wynncraft ingredient {} with reserved WynnBuilder ID {}.",
                        entry.getKey(), id);
                continue;
            }
            ingredientNamesById.put(id, entry.getValue().name());
        }

        Map<CraftableType, Map<String, RecipeData>> recipesByType = new EnumMap<>(CraftableType.class);
        Map<Integer, RecipeData> recipesById = new HashMap<>();
        JsonElement root = JsonParser.parseString(recipesJson);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("Wynncraft recipe response is not a non-empty array");
        }
        for (JsonElement element : root.getAsJsonArray()) {
            try {
                JsonObject object = requiredObject(element, "recipe");
                String internalName = requiredString(object, "internalName");
                String mappingName = toWynnBuilderRecipeName(internalName);
                Integer id = recipeIds.get(mappingName);
                if (id == null) {
                    WynnExtras.LOGGER.warn("Skipping unmapped Wynncraft recipe {}.", mappingName);
                    continue;
                }

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
                        duration, basicDuration, level, mappingName, id);
                RecipeData duplicateRecipe = recipesByType.computeIfAbsent(type, ignored -> new HashMap<>())
                        .putIfAbsent(levelKey(level.x, level.y), recipe);
                if (duplicateRecipe != null) throw new IllegalStateException("Duplicate Wynncraft recipe for " + type);
                if (recipesById.putIfAbsent(id, recipe) != null) {
                    recipesByType.get(type).remove(levelKey(level.x, level.y), recipe);
                    throw new IllegalStateException("Duplicate WynnBuilder recipe ID " + id);
                }
            } catch (RuntimeException exception) {
                WynnExtras.LOGGER.warn("Skipping malformed Wynncraft recipe entry.", exception);
            }
        }
        if (recipesById.size() != recipeIds.size()) {
            WynnExtras.LOGGER.warn("Recipe API/map mismatch: resolved {} of {} WynnBuilder recipes.",
                    recipesById.size(), recipeIds.size());
        }

        return new WynnDataSnapshot(itemCatalog.items(), itemCatalog.byDisplayName(), itemCatalog.byInternalName(),
                ingredientCatalog.byDisplayName(), ingredientCatalog.byInternalName(), ingredientNamesById,
                recipesByType, recipesById);
    }

    private record ItemCatalog(List<ItemData> items, Map<String, List<ItemData>> byDisplayName,
                               Map<String, List<ItemData>> byInternalName) {}

    private record ParsedItems(String json, JsonArray root, ItemCatalog catalog) {}

    private record IngredientCatalog(Map<String, IngredientInfo> byDisplayName,
                                     Map<String, IngredientInfo> byInternalName) {}

    private static ParsedItems parseItemPayload(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            throw new IllegalStateException("Wynncraft item response is not a non-empty array");
        }
        JsonArray items = root.getAsJsonArray();
        return new ParsedItems(json, items, parseItems(items));
    }

    private static ItemCatalog parseItems(JsonArray root) {
        List<ItemData> items = new ArrayList<>();
        Map<String, List<ItemData>> byDisplayName = new HashMap<>();
        Map<String, List<ItemData>> byInternalName = new HashMap<>();
        for (JsonElement element : root) {
            try {
                JsonObject object = requiredObject(element, "item");
                ItemData item = parseItem(object);
                items.add(item);
                byDisplayName.computeIfAbsent(normalizeName(item.displayName()), ignored -> new ArrayList<>()).add(item);
                byInternalName.computeIfAbsent(normalizeName(item.internalName()), ignored -> new ArrayList<>()).add(item);
            } catch (RuntimeException exception) {
                WynnExtras.LOGGER.warn("Skipping malformed Wynncraft item entry.", exception);
            }
        }
        return new ItemCatalog(List.copyOf(items), immutableListMap(byDisplayName), immutableListMap(byInternalName));
    }

    private static ItemData parseItem(JsonObject object) {
        String internalName = requiredString(object, "internalName");
        String displayName = requiredString(object, "displayName");
        return new ItemData(internalName, displayName, requiredString(object, "type"),
                optionalString(object, "subType"), optionalString(object, "tier"),
                optionalString(object, "restriction"), optionalString(object, "dropRestriction"),
                optionalString(object, "attackSpeed"), optionalInt(object, "powderSlots"),
                parseScalarMap(object.get("requirements")), parseStatMap(object.get("base")),
                parseStatMap(object.get("identifications")), parseTextMap(object.get("majorIds")),
                cleanItemText(optionalString(object, "lore")), optionalString(object, "emblem"),
                parseItemIcon(object.get("icon")));
    }

    private static IngredientCatalog parseIngredients(JsonArray root) {
        Map<String, IngredientInfo> byDisplayName = new HashMap<>();
        Map<String, IngredientInfo> byInternalName = new HashMap<>();
        for (JsonElement element : root) {
            try {
            JsonObject object = requiredObject(element, "item");
            if (!"ingredient".equals(optionalString(object, "type"))) continue;
            String name = requiredString(object, "displayName");
            String internalName = requiredString(object, "internalName");
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
            IngredientInfo ingredient = new IngredientInfo(name, tier, level, Optional.of(internalName),
                    null, List.copyOf(professions), skillRequirements, Map.copyOf(positionModifiers), List.of(),
                    requiredInt(consumable, "duration"), requiredInt(consumable, "charges"),
                    requiredInt(itemOnly, "durabilityModifier") / 1000, List.copyOf(identifications));
            if (byDisplayName.containsKey(name)) {
                throw new IllegalStateException("Duplicate ingredient display name " + name);
            }
            if (byInternalName.containsKey(internalName)) {
                throw new IllegalStateException("Duplicate ingredient internal name " + internalName);
            }
            byDisplayName.put(name, ingredient);
            byInternalName.put(internalName, ingredient);
            } catch (RuntimeException exception) {
                WynnExtras.LOGGER.warn("Skipping malformed Wynncraft ingredient entry.", exception);
            }
        }
        if (byDisplayName.isEmpty()) throw new IllegalStateException("Wynncraft API returned no ingredients");
        return new IngredientCatalog(Map.copyOf(byDisplayName), Map.copyOf(byInternalName));
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
            String name = requiredString(recipe, "name");
            if (result.put(name, requiredInt(recipe, "id")) != null) {
                throw new IllegalStateException("Duplicate WynnBuilder recipe name " + name);
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("WynnBuilder recipe list is empty");
        return result;
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
        WynncraftPowders.Powder powder = WynncraftPowders.fromName(name);
        if (powder == null) return null;
        List<Pair<Skill, Integer>> requirements = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            requirements.add(new Pair<>(skill, skill.ordinal() == powder.element() ? powder.requirement() : 0));
        }
        Map<IngredientPosition, Integer> positions = new EnumMap<>(IngredientPosition.class);
        for (IngredientPosition position : IngredientPosition.values()) positions.put(position, 0);
        return new IngredientInfo(name, 0, 0, Optional.empty(), null,
                List.of(WEProfessionType.ARMOURING, WEProfessionType.TAILORING, WEProfessionType.WEAPONSMITHING,
                        WEProfessionType.WOODWORKING, WEProfessionType.JEWELING),
                List.copyOf(requirements), Map.copyOf(positions), List.of(), 0, 0,
                (int) powder.durability(), List.of());
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

    private static Integer optionalInt(JsonObject object, String field) {
        JsonElement element = object.get(field);
        return element == null || element.isJsonNull() ? null : element.getAsInt();
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement element = object.get(field);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new IllegalStateException("Missing integer " + field);
        }
        return element.getAsInt();
    }

    private static Map<String, String> parseScalarMap(JsonElement element) {
        if (element == null || element.isJsonNull()) return Map.of();
        JsonObject object = requiredObject(element, "scalar map");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value != null && value.isJsonPrimitive()) result.put(entry.getKey(), value.getAsString());
        }
        return Map.copyOf(result);
    }

    private static Map<String, String> parseTextMap(JsonElement element) {
        if (element == null || element.isJsonNull()) return Map.of();
        JsonObject object = requiredObject(element, "text map");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                result.put(entry.getKey(), cleanItemText(entry.getValue().getAsString()));
            }
        }
        return Map.copyOf(result);
    }

    private static ItemIconData parseItemIcon(JsonElement element) {
        if (element == null || !element.isJsonObject()) return ItemIconData.EMPTY;
        JsonElement valueElement = element.getAsJsonObject().get("value");
        if (valueElement == null || !valueElement.isJsonObject()) return ItemIconData.EMPTY;
        JsonObject value = valueElement.getAsJsonObject();
        int customModelData = 0;
        JsonElement modelDataElement = value.get("customModelData");
        if (modelDataElement != null && modelDataElement.isJsonObject()) {
            JsonElement rangeDispatch = modelDataElement.getAsJsonObject().get("rangeDispatch");
            if (rangeDispatch != null && rangeDispatch.isJsonArray() && !rangeDispatch.getAsJsonArray().isEmpty()) {
                customModelData = rangeDispatch.getAsJsonArray().get(0).getAsInt();
            }
        }
        return new ItemIconData(orEmpty(optionalString(value, "id")),
                orEmpty(optionalString(value, "name")), customModelData);
    }

    private static String cleanItemText(String value) {
        return orEmpty(value).replaceAll("(?i)<br\\s*/?>|</p>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&emsp;", " ")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replace("&amp;", "&").trim();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, StatValue> parseStatMap(JsonElement element) {
        if (element == null || element.isJsonNull()) return Map.of();
        JsonObject object = requiredObject(element, "stat map");
        Map<String, StatValue> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            StatValue value = parseStatValue(entry.getValue());
            if (value != null) result.put(entry.getKey(), value);
        }
        return Map.copyOf(result);
    }

    private static StatValue parseStatValue(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return new StatValue(null, null, null, element.getAsDouble());
        }
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        return new StatValue(firstDouble(object, "min", "minimum"), optionalDouble(object, "raw"),
                firstDouble(object, "max", "maximum"), optionalDouble(object, "value"));
    }

    private static Double firstDouble(JsonObject object, String first, String second) {
        Double value = optionalDouble(object, first);
        return value != null ? value : optionalDouble(object, second);
    }

    private static Double optionalDouble(JsonObject object, String field) {
        JsonElement element = object.get(field);
        return element == null || element.isJsonNull() || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber() ? null : element.getAsDouble();
    }

    private static void validateUniqueIds(Map<String, Integer> ids, String description) {
        Map<Integer, String> namesById = new HashMap<>();
        for (Map.Entry<String, Integer> entry : ids.entrySet()) {
            Integer id = entry.getValue();
            if (id == null || id < 0) {
                throw new IllegalStateException("Invalid WynnBuilder " + description + " ID for " + entry.getKey());
            }
            String duplicate = namesById.put(id, entry.getKey());
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate WynnBuilder " + description + " ID " + id
                        + " for " + duplicate + " and " + entry.getKey());
            }
        }
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, List<ItemData>> immutableListMap(Map<String, List<ItemData>> source) {
        Map<String, List<ItemData>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static Map<CraftableType, Map<String, RecipeData>> immutableRecipeMap(
            Map<CraftableType, Map<String, RecipeData>> source) {
        Map<CraftableType, Map<String, RecipeData>> result = new EnumMap<>(CraftableType.class);
        source.forEach((key, value) -> result.put(key, Map.copyOf(value)));
        return Map.copyOf(result);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
