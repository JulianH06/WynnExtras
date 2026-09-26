package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * WynnBuilder's tree-only hash: ordered DFS decisions packed least-significant bit first.
 * The root is implicit. Class and data version are not stored in the hash.
 */
public final class WynnBuilderTreeCodec {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-";
    private static final String RESOURCE = "/assets/wynnextras/buildplanner/data/wynnbuilder_atree_2.2.3.0.json";
    private static final Map<AbilityTreeClass, Graph> GRAPHS = new EnumMap<>(AbilityTreeClass.class);

    private WynnBuilderTreeCodec() {
    }

    public static String encode(AbilityTreeDefinition definition, Set<String> selected) {
        Graph graph = graph(definition.abilityClass());
        Map<Integer, String> ids = matchNodes(graph, definition);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Select the root ability before copying to WynnBuilder");
        }
        if (!selected.contains(ids.get(graph.root()))) {
            throw new IllegalArgumentException("The tree must include its root ability");
        }
        Set<String> unmatched = new LinkedHashSet<>(selected);
        unmatched.removeAll(ids.values());
        if (!unmatched.isEmpty()) {
            throw new IllegalArgumentException("Selected abilities are missing from WynnBuilder's tree data");
        }

        List<Boolean> bits = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Set<String> encoded = new HashSet<>();
        encoded.add(ids.get(graph.root()));
        encodeChildren(graph, graph.root(), ids, selected, visited, encoded, bits);
        if (!encoded.equals(selected)) {
            throw new IllegalArgumentException("The selection is not connected in WynnBuilder's tree");
        }

        StringBuilder result = new StringBuilder();
        for (int offset = 0; offset < bits.size(); offset += 6) {
            int value = 0;
            for (int bit = 0; bit < 6 && offset + bit < bits.size(); bit++) {
                if (bits.get(offset + bit)) {
                    value |= 1 << bit;
                }
            }
            result.append(ALPHABET.charAt(value));
        }
        return result.toString();
    }

    public static Set<String> decode(AbilityTreeDefinition definition, String hash) {
        Graph graph = graph(definition.abilityClass());
        if (hash == null || hash.isEmpty() || hash.length() > (graph.nodes().size() + 5) / 6) {
            throw new IllegalArgumentException("Paste a WynnBuilder tree code, not a full build link");
        }
        for (int i = 0; i < hash.length(); i++) {
            if (ALPHABET.indexOf(hash.charAt(i)) < 0) {
                throw new IllegalArgumentException("Clipboard is not a WynnBuilder tree code");
            }
        }
        Map<Integer, String> ids = matchNodes(graph, definition);
        Set<String> selected = new LinkedHashSet<>();
        selected.add(requireApiId(graph, ids, graph.root()));
        BitReader bits = new BitReader(hash);
        decodeChildren(graph, graph.root(), ids, new HashSet<>(), selected, bits);
        // Only the unused bits in the final character may be padding.
        if (!encode(definition, selected).equals(hash)) {
            throw new IllegalArgumentException("Tree code has extra data or uses a different class/tree version");
        }
        return selected;
    }

    private static void encodeChildren(
            Graph graph, int parent, Map<Integer, String> ids, Set<String> selected,
            Set<Integer> visited, Set<String> encoded, List<Boolean> bits
    ) {
        for (int child : graph.children().get(parent)) {
            if (!visited.add(child)) {
                continue;
            }
            String apiId = ids.get(child);
            boolean active = apiId != null && selected.contains(apiId);
            bits.add(active);
            if (active) {
                encoded.add(apiId);
                encodeChildren(graph, child, ids, selected, visited, encoded, bits);
            }
        }
    }

    private static void decodeChildren(
            Graph graph, int parent, Map<Integer, String> ids,
            Set<Integer> visited, Set<String> selected, BitReader bits
    ) {
        for (int child : graph.children().get(parent)) {
            if (!visited.add(child)) {
                continue;
            }
            if (bits.next()) {
                selected.add(requireApiId(graph, ids, child));
                decodeChildren(graph, child, ids, visited, selected, bits);
            }
        }
    }

    private static String requireApiId(Graph graph, Map<Integer, String> ids, int id) {
        String apiId = ids.get(id);
        if (apiId == null) {
            throw new IllegalArgumentException("Current tree has no matching ability: " + graph.nodes().get(id).name());
        }
        return apiId;
    }

    private static Map<Integer, String> matchNodes(Graph graph, AbilityTreeDefinition definition) {
        Map<String, List<String>> byName = new HashMap<>();
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            byName.computeIfAbsent(normalize(node.name()), ignored -> new ArrayList<>()).add(node.id());
        }
        Map<Integer, String> result = new HashMap<>();
        Set<String> used = new HashSet<>();
        for (BuilderNode node : graph.nodes().values()) {
            List<String> matches = byName.get(normalize(node.name()));
            if (matches == null && node.name().matches(".* (I|1)$")) {
                matches = byName.get(normalize(node.name().substring(0, node.name().lastIndexOf(' '))));
            }
            if (matches == null && node.name().equals("Nightcloak Knife")) {
                matches = byName.get(normalize("Nightcloak Knives"));
            }
            if (matches != null) {
                if (matches.size() != 1 || !used.add(matches.getFirst())) {
                    throw new IllegalArgumentException("Ambiguous ability name in the current tree: " + node.name());
                }
                result.put(node.id(), matches.getFirst());
            }
        }
        return result;
    }

    private static String normalize(String name) {
        return name.replaceAll("<[^>]+>", "").replaceAll(" 1$", " I")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static synchronized Graph graph(AbilityTreeClass abilityClass) {
        Graph cached = GRAPHS.get(abilityClass);
        if (cached != null) {
            return cached;
        }
        try (var stream = WynnBuilderTreeCodec.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled WynnBuilder ability trees");
            }
            JsonObject trees = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Graph loaded = parseGraph(trees.getAsJsonArray(abilityClass.displayName()));
            GRAPHS.put(abilityClass, loaded);
            return loaded;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled WynnBuilder ability trees", exception);
        }
    }

    private static Graph parseGraph(JsonArray source) {
        Map<Integer, BuilderNode> nodes = new LinkedHashMap<>();
        Map<Integer, List<Integer>> children = new LinkedHashMap<>();
        int root = -1;
        for (JsonElement element : source) {
            JsonObject node = element.getAsJsonObject();
            int id = node.get("id").getAsInt();
            List<Integer> parents = new ArrayList<>();
            for (JsonElement parent : node.getAsJsonArray("parents")) {
                parents.add(parent.getAsInt());
            }
            if (parents.isEmpty()) {
                if (root != -1) {
                    throw new IllegalStateException("WynnBuilder tree has multiple roots");
                }
                root = id;
            }
            if (nodes.put(id, new BuilderNode(id, node.get("display_name").getAsString(), List.copyOf(parents))) != null) {
                throw new IllegalStateException("Duplicate WynnBuilder ability ID");
            }
            children.put(id, new ArrayList<>());
        }
        if (root == -1) {
            throw new IllegalStateException("WynnBuilder tree has no root");
        }
        // Child order is the JSON array order, not display order or API map order.
        for (BuilderNode node : nodes.values()) {
            for (int parent : node.parents()) {
                if (!children.containsKey(parent)) {
                    throw new IllegalStateException("Missing WynnBuilder parent ability");
                }
                children.get(parent).add(node.id());
            }
        }
        children.replaceAll((id, values) -> List.copyOf(values));
        return new Graph(root, Map.copyOf(nodes), Map.copyOf(children));
    }

    private record BuilderNode(int id, String name, List<Integer> parents) {
    }

    private record Graph(int root, Map<Integer, BuilderNode> nodes, Map<Integer, List<Integer>> children) {
    }

    private static final class BitReader {
        private final String hash;
        private int offset;

        BitReader(String hash) {
            this.hash = hash;
        }

        boolean next() {
            if (offset >= hash.length() * 6) {
                throw new IllegalArgumentException("Tree code is incomplete for this class/tree version");
            }
            int value = ALPHABET.indexOf(hash.charAt(offset / 6));
            return (value & (1 << (offset++ % 6))) != 0;
        }
    }
}
