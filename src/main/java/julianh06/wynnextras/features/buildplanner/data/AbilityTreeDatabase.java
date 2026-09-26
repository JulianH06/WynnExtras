package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

public final class AbilityTreeDatabase {
    private static final AbilityTreeDatabase INSTANCE = new AbilityTreeDatabase();
    private static final String API_ROOT = "https://api.wynncraft.com/v3/ability/";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern COLOR_PATTERN = Pattern.compile("color:#([0-9a-fA-F]{6})");

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final Map<AbilityTreeClass, AbilityTreeDefinition> definitions =
            new ConcurrentHashMap<>();
    private final Map<AbilityTreeClass, String> errors = new ConcurrentHashMap<>();
    private final Set<AbilityTreeClass> loading = ConcurrentHashMap.newKeySet();

    private AbilityTreeDatabase() {
    }

    public static AbilityTreeDatabase getInstance() {
        return INSTANCE;
    }

    public AbilityTreeDefinition get(AbilityTreeClass abilityClass) {
        return definitions.get(abilityClass);
    }

    public String error(AbilityTreeClass abilityClass) {
        return errors.getOrDefault(abilityClass, "");
    }

    public boolean isLoading(AbilityTreeClass abilityClass) {
        return loading.contains(abilityClass);
    }

    public void loadAsync(AbilityTreeClass abilityClass) {
        if (definitions.containsKey(abilityClass) || !loading.add(abilityClass)) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                load(abilityClass);
            } finally {
                loading.remove(abilityClass);
            }
        }, "WynnExtras-AbilityTree-" + abilityClass.apiName());
        thread.setDaemon(true);
        thread.start();
    }

    private void load(AbilityTreeClass abilityClass) {
        JsonObject tree = null;
        JsonObject map = null;
        try {
            tree = fetch("tree/" + abilityClass.apiName());
            map = fetch("map/" + abilityClass.apiName());
            saveCache(abilityClass, "tree", tree);
            saveCache(abilityClass, "map", map);
        } catch (Exception exception) {
            PlannerLog.LOGGER.warn("Failed to refresh {} ability tree.", abilityClass.apiName(), exception);
            try {
                tree = readCache(abilityClass, "tree");
                map = readCache(abilityClass, "map");
            } catch (Exception cacheException) {
                errors.put(abilityClass, "Could not load the Wynncraft ability tree");
                PlannerLog.LOGGER.error("No cached {} ability tree is available.", abilityClass.apiName(), cacheException);
                return;
            }
        }

        try {
            definitions.put(abilityClass, parse(abilityClass, tree, map));
            errors.remove(abilityClass);
        } catch (Exception exception) {
            errors.put(abilityClass, "The Wynncraft ability tree response was invalid");
            PlannerLog.LOGGER.error("Failed to parse {} ability tree.", abilityClass.apiName(), exception);
        }
    }

    private JsonObject fetch(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_ROOT + endpoint))
                .header("Accept", "application/json")
                .header("User-Agent", "WynnExtras-BuildPlanner")
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Wynncraft API returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    AbilityTreeDefinition parse(
            AbilityTreeClass abilityClass,
            JsonObject tree,
            JsonObject map
    ) {
        Map<String, AbilityTreeDefinition.Archetype> archetypes = parseArchetypes(tree);
        Map<String, Position> positions = new LinkedHashMap<>();
        List<AbilityTreeDefinition.Connector> connectors = new ArrayList<>();
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        int maxY = parseMap(map, positions, connectors, adjacency);

        Map<String, AbilityTreeDefinition.Node> nodes = new LinkedHashMap<>();
        JsonObject pages = tree.getAsJsonObject("pages");
        for (Map.Entry<String, JsonElement> pageEntry : pages.entrySet()) {
            JsonObject page = pageEntry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> nodeEntry : page.entrySet()) {
                String id = nodeEntry.getKey();
                JsonObject source = nodeEntry.getValue().getAsJsonObject();
                Position position = positions.getOrDefault(
                        id,
                        new Position(
                                getInt(source.getAsJsonObject("coordinates"), "x", 5),
                                getInt(source.getAsJsonObject("coordinates"), "y", 1)));
                List<String> description = stringList(source.getAsJsonArray("description"), true);
                JsonObject requirements = source.getAsJsonObject("requirements");
                List<String> requiredNodes = requirements != null && requirements.has("NODE")
                        ? stringList(requirements.getAsJsonArray("NODE"), false)
                        : List.of();
                AbilityTreeDefinition.ArchetypeRequirement archetypeRequirement = null;
                if (requirements != null
                        && requirements.has("ARCHETYPE")
                        && requirements.get("ARCHETYPE").isJsonObject()) {
                    JsonObject required = requirements.getAsJsonObject("ARCHETYPE");
                    archetypeRequirement = new AbilityTreeDefinition.ArchetypeRequirement(
                            getString(required, "name"), getInt(required, "amount", 0));
                }
                nodes.put(id, new AbilityTreeDefinition.Node(
                        id,
                        clean(getString(source, "name")),
                        description,
                        position.x(),
                        position.y(),
                        getInt(source, "page", Integer.parseInt(pageEntry.getKey())),
                        requirements == null ? 0 : getInt(requirements, "ABILITY_POINTS", 0),
                        requirements == null ? 0 : getInt(requirements, "COMBAT_LEVEL", 0),
                        requiredNodes,
                        archetypeRequirement,
                        source.has("locks") && source.get("locks").isJsonArray()
                                ? stringList(source.getAsJsonArray("locks"), false)
                                : List.of(),
                        findArchetype(description, archetypes),
                        nodeColor(source),
                        nodeIconName(source)));
            }
        }
        nodes = canonicalizeReferences(nodes);

        String root = nodes.values().stream()
                .min((left, right) -> Integer.compare(left.y(), right.y()))
                .map(AbilityTreeDefinition.Node::id)
                .orElse("");
        for (String id : nodes.keySet()) {
            adjacency.computeIfAbsent(id, ignored -> new LinkedHashSet<>());
        }
        return new AbilityTreeDefinition(
                abilityClass,
                Map.copyOf(archetypes),
                Map.copyOf(nodes),
                List.copyOf(connectors),
                immutableAdjacency(adjacency),
                root,
                maxY);
    }

    private Map<String, AbilityTreeDefinition.Archetype> parseArchetypes(JsonObject tree) {
        Map<String, AbilityTreeDefinition.Archetype> result = new LinkedHashMap<>();
        JsonObject source = tree.getAsJsonObject("archetypes");
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            String rawName = getString(value, "name");
            result.put(entry.getKey(), new AbilityTreeDefinition.Archetype(
                    entry.getKey(),
                    clean(rawName),
                    clean(getString(value, "description")),
                    parseColor(rawName, 0xFFFFFFFF)));
        }
        return result;
    }

    private Map<String, AbilityTreeDefinition.Node> canonicalizeReferences(
            Map<String, AbilityTreeDefinition.Node> source
    ) {
        Map<String, String> canonicalIds = new LinkedHashMap<>();
        for (String id : source.keySet()) {
            canonicalIds.put(id.toLowerCase(Locale.ROOT), id);
        }
        Map<String, AbilityTreeDefinition.Node> result = new LinkedHashMap<>();
        for (AbilityTreeDefinition.Node node : source.values()) {
            List<String> requirements = node.requiredNodes().stream()
                    .map(id -> canonicalIds.getOrDefault(id.toLowerCase(Locale.ROOT), id))
                    .toList();
            List<String> locks = node.locks().stream()
                    .map(id -> canonicalIds.getOrDefault(id.toLowerCase(Locale.ROOT), id))
                    .toList();
            result.put(node.id(), new AbilityTreeDefinition.Node(
                    node.id(), node.name(), node.description(), node.x(), node.y(), node.page(),
                    node.abilityPointCost(), node.combatLevel(), requirements,
                    node.archetypeRequirement(), locks, node.archetype(), node.color(),
                    node.iconName()));
        }
        return result;
    }

    private int parseMap(
            JsonObject map,
            Map<String, Position> positions,
            List<AbilityTreeDefinition.Connector> connectors,
            Map<String, Set<String>> adjacency
    ) {
        int maxY = 1;
        for (Map.Entry<String, JsonElement> pageEntry : map.entrySet()) {
            for (JsonElement cellElement : pageEntry.getValue().getAsJsonArray()) {
                JsonObject cell = cellElement.getAsJsonObject();
                JsonObject coordinates = cell.getAsJsonObject("coordinates");
                int x = getInt(coordinates, "x", 5);
                int y = getInt(coordinates, "y", 1);
                maxY = Math.max(maxY, y);
                JsonObject meta = cell.getAsJsonObject("meta");
                if ("ability".equals(getString(cell, "type"))) {
                    positions.put(getString(meta, "id"), new Position(x, y));
                    continue;
                }
                if (!"connector".equals(getString(cell, "type"))) {
                    continue;
                }
                List<AbilityTreeDefinition.Edge> paths = new ArrayList<>();
                if (meta.has("paths") && meta.get("paths").isJsonArray()) {
                    for (JsonElement pathElement : meta.getAsJsonArray("paths")) {
                    JsonArray path = pathElement.getAsJsonArray();
                    if (path.size() < 2) {
                        continue;
                    }
                    String first = path.get(0).getAsString();
                    String second = path.get(1).getAsString();
                    paths.add(new AbilityTreeDefinition.Edge(first, second));
                    adjacency.computeIfAbsent(first, ignored -> new LinkedHashSet<>()).add(second);
                    adjacency.computeIfAbsent(second, ignored -> new LinkedHashSet<>()).add(first);
                    }
                }
                String connectorIcon = getString(meta, "icon");
                connectors.add(new AbilityTreeDefinition.Connector(
                        x, y, connectorDirections(connectorIcon), List.copyOf(paths), connectorIcon));
            }
        }
        return maxY;
    }

    private Set<AbilityTreeDefinition.Direction> connectorDirections(String icon) {
        Set<AbilityTreeDefinition.Direction> directions = new LinkedHashSet<>();
        String normalized = icon.toLowerCase(Locale.ROOT);
        if (normalized.contains("up")) directions.add(AbilityTreeDefinition.Direction.UP);
        if (normalized.contains("right")) directions.add(AbilityTreeDefinition.Direction.RIGHT);
        if (normalized.contains("down")) directions.add(AbilityTreeDefinition.Direction.DOWN);
        if (normalized.contains("left")) directions.add(AbilityTreeDefinition.Direction.LEFT);
        return Set.copyOf(directions);
    }

    private Map<String, Set<String>> immutableAdjacency(Map<String, Set<String>> source) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(result);
    }

    private String findArchetype(
            List<String> description,
            Map<String, AbilityTreeDefinition.Archetype> archetypes
    ) {
        String joined = String.join(" ", description).toLowerCase(Locale.ROOT);
        for (AbilityTreeDefinition.Archetype archetype : archetypes.values()) {
            if (joined.contains(archetype.name().toLowerCase(Locale.ROOT) + " archetype")) {
                return archetype.id();
            }
        }
        return "";
    }

    private int nodeColor(JsonObject source) {
        if (!source.has("icon") || !source.get("icon").isJsonObject()) {
            return 0xFFFFFFFF;
        }
        JsonObject icon = source.getAsJsonObject("icon");
        JsonObject value = icon.has("value") && icon.get("value").isJsonObject()
                ? icon.getAsJsonObject("value")
                : null;
        String name = value == null ? "" : getString(value, "name").toLowerCase(Locale.ROOT);
        if (name.contains("red")) return 0xFFFF5555;
        if (name.contains("purple")) return 0xFFFF55FF;
        if (name.contains("blue")) return 0xFF55FFFF;
        if (name.contains("yellow")) return 0xFFFFFF55;
        if (name.contains("green")
                || name.contains("warrior")
                || name.contains("archer")
                || name.contains("mage")
                || name.contains("assassin")
                || name.contains("shaman")) {
            return 0xFF55FF55;
        }
        return parseColor(getString(source, "name"), 0xFFFFFFFF);
    }

    private String nodeIconName(JsonObject source) {
        if (!source.has("icon") || !source.get("icon").isJsonObject()) {
            return "";
        }
        JsonObject icon = source.getAsJsonObject("icon");
        if (!icon.has("value") || !icon.get("value").isJsonObject()) {
            return "";
        }
        return getString(icon.getAsJsonObject("value"), "name");
    }

    private List<String> stringList(JsonArray array, boolean cleanMarkup) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            String value = cleanMarkup ? clean(element.getAsString()) : element.getAsString();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return TAG_PATTERN.matcher(value)
                .replaceAll("")
                .replaceAll("[\\uE000-\\uF8FF]", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .trim();
    }

    private int parseColor(String markup, int fallback) {
        Matcher matcher = COLOR_PATTERN.matcher(markup == null ? "" : markup);
        if (!matcher.find()) {
            return fallback;
        }
        return 0xFF000000 | Integer.parseInt(matcher.group(1), 16);
    }

    private String getString(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString()
                : "";
    }

    private int getInt(JsonObject object, String key, int fallback) {
        return object != null
                && object.has(key)
                && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt()
                : fallback;
    }

    private void saveCache(AbilityTreeClass abilityClass, String kind, JsonObject json) throws IOException {
        Path path = cachePath(abilityClass, kind);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
    }

    private JsonObject readCache(AbilityTreeClass abilityClass, String kind) throws IOException {
        return JsonParser.parseString(Files.readString(
                cachePath(abilityClass, kind), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private Path cachePath(AbilityTreeClass abilityClass, String kind) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras").resolve("buildplanner")
                .resolve("ability_" + abilityClass.apiName() + "_" + kind + ".json");
    }

    private record Position(int x, int y) {
    }
}
