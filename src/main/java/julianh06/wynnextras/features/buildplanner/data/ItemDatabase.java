package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;

public final class ItemDatabase {
    private static final URI API_URI = URI.create("https://api.wynncraft.com/v3/item/database?fullResult");
    private static final ItemDatabase INSTANCE = new ItemDatabase();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();
    private final Map<String, WynnItem> itemsByName = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private volatile String lastError = "";
    private volatile long revision;

    public synchronized List<WynnItem> allItems() { return List.copyOf(itemsByName.values()); }
    public long revision() { return revision; }

    ItemDatabase() {
    }

    public static ItemDatabase getInstance() {
        return INSTANCE;
    }

    public void fetchAll() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }

        try {
            loadFromCacheIfPresent();
            fetchFromApi();
        } catch (Exception exception) {
            lastError = "Unexpected error: " + exception.getMessage();
            PlannerLog.LOGGER.error("Unexpected error in item database fetch.", exception);
        } finally {
            loading.set(false);
            ready.set(!itemsByName.isEmpty());
            revision++;
            PlannerLog.LOGGER.info("Item database fetch complete. Items: {}, Error: '{}'", itemsByName.size(), lastError);
        }
    }

    public List<WynnItem> searchItems(String query) {
        return searchItems(query, true);
    }

    public List<WynnItem> searchItems(String query, boolean filterEquipment) {
        var stream = itemsByName.values().stream();

        if (filterEquipment) {
            stream = stream.filter(item -> {
                String t = item.type() != null ? item.type().toLowerCase(Locale.ROOT) : "";
                return t.equals("weapon") || t.equals("armour") || t.equals("accessory");
            });
        }

        if (query != null && !query.isBlank()) {
            String lowered = query.toLowerCase(Locale.ROOT);
            stream = stream.filter(item -> item.displayName().toLowerCase(Locale.ROOT).contains(lowered));
        }

        return stream
            .sorted(Comparator.comparing(WynnItem::displayName, String.CASE_INSENSITIVE_ORDER))
            .limit(100)
            .toList();
    }

    public List<WynnItem> searchItems(String query, String slotType) {
        String normalizedSlot = slotType == null ? "" : slotType.toLowerCase(Locale.ROOT);
        String lowered = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        return itemsByName.values().stream()
                .filter(item -> matchesSlot(item, normalizedSlot))
                .filter(item -> lowered.isEmpty()
                        || item.displayName().toLowerCase(Locale.ROOT).contains(lowered))
                .sorted(Comparator.comparing(WynnItem::displayName, String.CASE_INSENSITIVE_ORDER))
                .limit(100)
                .toList();
    }

    public WynnItem getItem(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (CraftedItemCodec.isReference(name)) {
            try {
                return CraftedItemCodec.decode(name);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                PlannerLog.LOGGER.warn("Could not resolve crafted item reference: {}", exception.getMessage());
                return null;
            }
        }

        WynnItem direct = itemsByName.get(normalizeName(name));
        if (direct != null) {
            return direct;
        }

        return itemsByName.values().stream()
            .filter(item -> item.displayName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public boolean isReady() {
        return ready.get();
    }

    public boolean isLoading() {
        return loading.get();
    }

    public int getLoadedCount() {
        return itemsByName.size();
    }

    public String getLastError() {
        return lastError;
    }

    private void loadFromCacheIfPresent() {
        Path cacheFile = getCacheFile();
        if (!Files.exists(cacheFile)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            List<WynnItem> cachedItems = parseItems(json);
            if (!cachedItems.isEmpty()) {
                replaceItems(cachedItems);
                ready.set(true);
            }
        } catch (Exception exception) {
            PlannerLog.LOGGER.warn("Failed to load cached Wynncraft item database.", exception);
            lastError = "Failed to load cached item database.";
        }
    }

    private void fetchFromApi() {
        try {
            HttpRequest request = HttpRequest.newBuilder(API_URI)
                .header("Accept", "application/json")
                .header("User-Agent", "WynnExtras-BuildPlanner")
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();

            PlannerLog.LOGGER.info("Sending request to Wynncraft API...");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            PlannerLog.LOGGER.info("Wynncraft API responded with status {} and {} bytes.", response.statusCode(), response.body().length());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                lastError = "HTTP " + response.statusCode() + " while fetching item database.";
                PlannerLog.LOGGER.warn(lastError);
                return;
            }

            JsonElement json = JsonParser.parseString(response.body());
            List<WynnItem> fetchedItems = parseItems(json);
            if (fetchedItems.isEmpty()) {
                lastError = "Wynncraft API returned no items.";
                PlannerLog.LOGGER.warn(lastError);
                return;
            }

            replaceItems(fetchedItems);
            saveCache(json);
            ready.set(true);
            lastError = "";
        } catch (Exception exception) {
            lastError = "Failed to fetch Wynncraft items.";
            PlannerLog.LOGGER.warn("Failed to fetch Wynncraft item database.", exception);
        }
    }

    synchronized void replaceItems(List<WynnItem> items) {
        Map<String, WynnItem> replacement = new LinkedHashMap<>();
        for (WynnItem item : items) {
            replacement.put(normalizeName(item.displayName()), item);
        }

        itemsByName.clear();
        itemsByName.putAll(replacement);
        revision++;
    }

    private void saveCache(JsonElement json) throws IOException {
        Path cacheFile = getCacheFile();
        Files.createDirectories(cacheFile.getParent());
        try (Writer writer = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
    }

    List<WynnItem> parseItems(JsonElement root) {
        if (root == null || root.isJsonNull()) {
            return Collections.emptyList();
        }

        List<WynnItem> items = new ArrayList<>();
        if (root.isJsonArray()) {
            parseItemArray(root.getAsJsonArray(), items);
            return items;
        }

        JsonObject rootObject = root.getAsJsonObject();
        if (rootObject.has("items") && rootObject.get("items").isJsonArray()) {
            parseItemArray(rootObject.getAsJsonArray("items"), items);
            return items;
        }

        for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                WynnItem item = parseItemObject(entry.getValue().getAsJsonObject());
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    private void parseItemArray(JsonArray array, List<WynnItem> sink) {
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            WynnItem item = parseItemObject(element.getAsJsonObject());
            if (item != null) {
                sink.add(item);
            }
        }
    }

    private WynnItem parseItemObject(JsonObject object) {
        String displayName = getString(object, "displayName");
        String internalName = getString(object, "internalName");
        if (internalName.startsWith("Masterwork ")) {
            displayName = internalName;
        }
        if (displayName.isBlank()) {
            return null;
        }

        Map<String, Identification> identifications = new LinkedHashMap<>();
        JsonObject identificationObject = object.has("identifications") && object.get("identifications").isJsonObject()
            ? object.getAsJsonObject("identifications")
            : null;

        if (identificationObject != null) {
            for (Map.Entry<String, JsonElement> entry : identificationObject.entrySet()) {
                String key = normalizeIdentificationKey(entry.getKey());
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    int value = entry.getValue().getAsInt();
                    identifications.put(key, new Identification(value, value, value));
                } else if (entry.getValue().isJsonObject()) {
                    JsonObject statObject = entry.getValue().getAsJsonObject();
                    if (!statObject.has("min") || !statObject.has("raw") || !statObject.has("max")) {
                        continue;
                    }

                    identifications.put(key, new Identification(
                        getInt(statObject, "min"),
                        getInt(statObject, "raw"),
                        getInt(statObject, "max")
                    ));
                }
            }
        }

        Map<String, Integer> baseStats = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                baseStats.put(entry.getKey(), value.getAsInt());
            } else if (value.isJsonObject()) {
                JsonObject nested = value.getAsJsonObject();
                if (nested.has("raw") && nested.get("raw").isJsonPrimitive()) {
                    baseStats.put(entry.getKey(), nested.get("raw").getAsInt());
                }
                if (nested.has("min") && nested.get("min").isJsonPrimitive()) {
                    baseStats.put(entry.getKey() + "Min", nested.get("min").getAsInt());
                }
                if (nested.has("max") && nested.get("max").isJsonPrimitive()) {
                    baseStats.put(entry.getKey() + "Max", nested.get("max").getAsInt());
                }
            }
        }
        parseRequirements(object, baseStats);
        parseBaseStats(object, baseStats);
        ItemIcon icon = parseIcon(object);
        Map<String, String> majorIds = new LinkedHashMap<>();
        if (object.has("majorIds") && object.get("majorIds").isJsonObject()) {
            object.getAsJsonObject("majorIds").entrySet().forEach(entry ->
                    majorIds.put(entry.getKey(), cleanItemText(entry.getValue().getAsString())));
        }

        return new WynnItem(
            displayName,
            getString(object, "type"),
            getString(object, "subType"),
            getString(object, "tier"),
            getString(object, "attackSpeed"),
            identifications,
            baseStats,
            icon.id(),
            icon.model(),
            icon.customModelData(),
            "",
            List.of(),
            majorIds,
            cleanItemText(getString(object, "lore")),
            getString(object, "restriction")
        );
    }

    static String cleanItemText(String value) {
        return value.replaceAll("(?i)<br\\s*/?>|</p>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ").replace("&emsp;", " ")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replace("&amp;", "&").trim();
    }

    private ItemIcon parseIcon(JsonObject item) {
        if (!item.has("icon") || !item.get("icon").isJsonObject()) {
            return ItemIcon.EMPTY;
        }
        JsonObject icon = item.getAsJsonObject("icon");
        if (!icon.has("value") || !icon.get("value").isJsonObject()) {
            return ItemIcon.EMPTY;
        }
        JsonObject value = icon.getAsJsonObject("value");
        int customModelData = 0;
        if (value.has("customModelData") && value.get("customModelData").isJsonObject()) {
            JsonObject modelData = value.getAsJsonObject("customModelData");
            if (modelData.has("rangeDispatch") && modelData.get("rangeDispatch").isJsonArray()
                    && !modelData.getAsJsonArray("rangeDispatch").isEmpty()) {
                customModelData = modelData.getAsJsonArray("rangeDispatch").get(0).getAsInt();
            }
        }
        return new ItemIcon(getString(value, "id"), getString(value, "name"), customModelData);
    }

    private void parseRequirements(JsonObject item, Map<String, Integer> stats) {
        if (!item.has("requirements") || !item.get("requirements").isJsonObject()) {
            return;
        }
        JsonObject requirements = item.getAsJsonObject("requirements");
        copyNumber(requirements, stats, "level", "lvl");
        copyNumber(requirements, stats, "strength", "strReq");
        copyNumber(requirements, stats, "dexterity", "dexReq");
        copyNumber(requirements, stats, "intelligence", "intReq");
        copyNumber(requirements, stats, "defence", "defReq");
        copyNumber(requirements, stats, "agility", "agiReq");
    }

    private void parseBaseStats(JsonObject item, Map<String, Integer> stats) {
        if (!item.has("base") || !item.get("base").isJsonObject()) {
            return;
        }
        JsonObject base = item.getAsJsonObject("base");
        copyNumber(base, stats, "baseHealth", "hp");
        copyNumber(base, stats, "baseEarthDefence", "eDef");
        copyNumber(base, stats, "baseThunderDefence", "tDef");
        copyNumber(base, stats, "baseWaterDefence", "wDef");
        copyNumber(base, stats, "baseFireDefence", "fDef");
        copyNumber(base, stats, "baseAirDefence", "aDef");

        copyRange(base, stats, "baseDamage", "nDam");
        copyRange(base, stats, "baseEarthDamage", "eDam");
        copyRange(base, stats, "baseThunderDamage", "tDam");
        copyRange(base, stats, "baseWaterDamage", "wDam");
        copyRange(base, stats, "baseFireDamage", "fDam");
        copyRange(base, stats, "baseAirDamage", "aDam");
    }

    private void copyNumber(JsonObject source, Map<String, Integer> target, String sourceKey, String targetKey) {
        if (source.has(sourceKey) && source.get(sourceKey).isJsonPrimitive()
                && source.get(sourceKey).getAsJsonPrimitive().isNumber()) {
            target.put(targetKey, source.get(sourceKey).getAsInt());
        }
    }

    private void copyRange(JsonObject source, Map<String, Integer> target, String sourceKey, String targetKey) {
        if (!source.has(sourceKey) || !source.get(sourceKey).isJsonObject()) {
            return;
        }
        JsonObject range = source.getAsJsonObject(sourceKey);
        copyNumber(range, target, "min", targetKey + "Min");
        copyNumber(range, target, "max", targetKey + "Max");
    }

    private String normalizeIdentificationKey(String key) {
        return switch (key) {
            case "rawStrength" -> "str";
            case "rawDexterity" -> "dex";
            case "rawIntelligence" -> "int";
            case "rawDefence" -> "def";
            case "rawAgility" -> "agi";
            case "DEXETERITY" -> "dex";
            default -> key;
        };
    }

    private boolean matchesSlot(WynnItem item, String slot) {
        String type = item.type() == null ? "" : item.type().toLowerCase(Locale.ROOT);
        String subType = item.subType() == null ? "" : item.subType().toLowerCase(Locale.ROOT);
        return switch (slot) {
            case "helmet", "chestplate", "leggings", "boots" ->
                    subType.equals(slot) || type.equals(slot);
            case "ring" -> subType.equals("ring") || type.equals("ring");
            case "bracelet" -> subType.equals("bracelet") || type.equals("bracelet");
            case "necklace" -> subType.equals("necklace") || type.equals("necklace");
            case "weapon" -> type.equals("weapon")
                    || subType.equals("dagger") || subType.equals("spear")
                    || subType.equals("wand") || subType.equals("bow") || subType.equals("relik");
            default -> false;
        };
    }

    private record ItemIcon(String id, String model, int customModelData) {
        private static final ItemIcon EMPTY = new ItemIcon("", "", 0);
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private int getInt(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : 0;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }

    private Path getCacheFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("wynnextras").resolve("buildplanner").resolve("item_database.json");
    }
}
