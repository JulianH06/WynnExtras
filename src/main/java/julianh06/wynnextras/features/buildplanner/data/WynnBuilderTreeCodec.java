package julianh06.wynnextras.features.buildplanner.data;

import julianh06.wynnextras.wynncraft.state.CharacterClass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBitCursor;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderBitVector;
import julianh06.wynnextras.wynncraft.codec.WynnBuilderEncoding;
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
    private static final Map<CharacterClass, Graph> GRAPHS = new EnumMap<>(CharacterClass.class);

    private WynnBuilderTreeCodec() {
    }

    public static String encode(AbilityTreeDefinition definition, Set<String> selected) {
        EncodeResult result = encodeBestEffort(definition, selected);
        if (!result.skippedAbilities().isEmpty()) {
            throw new IllegalArgumentException("Abilities missing from WynnBuilder: "
                    + String.join(", ", result.skippedAbilities()));
        }
        return result.hash();
    }

    public static EncodeResult encodeBestEffort(AbilityTreeDefinition definition, Set<String> selected) {
        Graph graph = graph(definition.abilityClass());
        Map<Integer, String> ids = matchNodes(graph, definition);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Select the root ability before copying to WynnBuilder");
        }
        String rootId = ids.get(graph.root());
        if (rootId == null) {
            throw new IllegalArgumentException("WynnBuilder's root ability does not match the current tree");
        }
        if (!selected.contains(rootId)) {
            throw new IllegalArgumentException("The tree must include its root ability");
        }

        List<Boolean> bits = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Set<String> encoded = new HashSet<>();
        encoded.add(rootId);
        encodeChildren(graph, graph.root(), ids, selected, visited, encoded, bits);

        WynnBuilderBitVector result = new WynnBuilderBitVector();
        bits.forEach(result::appendBit);
        Set<String> skippedIds = new LinkedHashSet<>(selected);
        skippedIds.removeAll(encoded);
        List<String> skipped = skippedIds.stream()
                .map(id -> definition.nodes().containsKey(id) ? definition.nodes().get(id).name() : id)
                .toList();
        return new EncodeResult(result.toBase64(), Set.copyOf(encoded), skipped);
    }

    public static Set<String> decode(AbilityTreeDefinition definition, String hash) {
        Decoded decoded = decodeBits(definition, hash);
        String canonical = encode(definition, decoded.selected());
        if (!canonical.equals(hash)) {
            throw new IllegalArgumentException("Tree code has extra data or uses a different class/tree version (decoded as "
                    + canonical + ")");
        }
        return decoded.selected();
    }

    public static Set<String> decodeBuildPayload(
            AbilityTreeDefinition definition,
            String hash,
            int bitLength
    ) {
        return decodeBuildPayloadBestEffort(definition, hash, bitLength).selected();
    }

    public static DecodeResult decodeBuildPayloadBestEffort(
            AbilityTreeDefinition definition,
            String hash,
            int bitLength
    ) {
        if (hash == null || bitLength < 0 || bitLength > hash.length() * 6
                || (bitLength + 5) / 6 != hash.length()) {
            throw new IllegalArgumentException("Invalid ability tree payload length");
        }
        Decoded decoded = decodeBits(definition, hash, true);
        if (decoded.consumedBits() > bitLength || bitLength - decoded.consumedBits() > 5) {
            throw new IllegalArgumentException("Ability tree payload uses a different class/tree version");
        }
        for (int bit = decoded.consumedBits(); bit < hash.length() * 6; bit++) {
            if (bit(hash, bit)) {
                throw new IllegalArgumentException("Ability tree payload contains non-zero padding");
            }
        }
        return new DecodeResult(decoded.selected(), decoded.skippedAbilities());
    }

    private static Decoded decodeBits(AbilityTreeDefinition definition, String hash) {
        return decodeBits(definition, hash, false);
    }

    private static Decoded decodeBits(
            AbilityTreeDefinition definition,
            String hash,
            boolean skipUnmatched
    ) {
        Graph graph = graph(definition.abilityClass());
        if (hash == null || hash.isEmpty() || hash.length() > (graph.nodes().size() + 5) / 6) {
            throw new IllegalArgumentException("Paste a WynnBuilder tree code, not a full build link");
        }
        if (!WynnBuilderEncoding.isValid(hash)) {
            throw new IllegalArgumentException("Clipboard is not a WynnBuilder tree code");
        }
        Map<Integer, String> ids = matchNodes(graph, definition);
        Set<String> selected = new LinkedHashSet<>();
        List<String> skipped = new ArrayList<>();
        String rootId = ids.get(graph.root());
        if (rootId == null) {
            throw new IllegalArgumentException("Current tree has no matching root ability: "
                    + graph.nodes().get(graph.root()).name());
        }
        selected.add(rootId);
        WynnBuilderBitCursor bits = new WynnBuilderBitCursor(hash);
        decodeChildren(graph, graph.root(), ids, new HashSet<>(), selected, skipped, bits, skipUnmatched);
        return new Decoded(Set.copyOf(selected), bits.bitIndex(), List.copyOf(skipped));
    }

    private static boolean bit(String hash, int index) {
        return ((WynnBuilderEncoding.value(hash.charAt(index / 6)) >> (index % 6)) & 1) != 0;
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
            Set<Integer> visited, Set<String> selected, List<String> skipped,
            WynnBuilderBitCursor bits, boolean skipUnmatched
    ) {
        for (int child : graph.children().get(parent)) {
            if (!visited.add(child)) {
                continue;
            }
            if (bits.readBit()) {
                String apiId = ids.get(child);
                if (apiId == null) {
                    if (!skipUnmatched) requireApiId(graph, ids, child);
                    skipped.add(graph.nodes().get(child).name());
                } else {
                    selected.add(apiId);
                }
                decodeChildren(graph, child, ids, visited, selected, skipped, bits, skipUnmatched);
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
                    continue;
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

    private static synchronized Graph graph(CharacterClass abilityClass) {
        Graph cached = GRAPHS.get(abilityClass);
        if (cached != null) {
            return cached;
        }
        try {
            JsonObject trees = WynnBuilderDataFiles.read(
                    WynnBuilderDataFiles.File.ABILITY_TREE).getAsJsonObject();
            Graph loaded = parseGraph(trees.getAsJsonArray(abilityClass.displayName()));
            GRAPHS.put(abilityClass, loaded);
            return loaded;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not read current WynnBuilder ability trees", exception);
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

    public record EncodeResult(String hash, Set<String> encoded, List<String> skippedAbilities) {
    }

    public record DecodeResult(Set<String> selected, List<String> skippedAbilities) {
    }

    private record Decoded(Set<String> selected, int consumedBits, List<String> skippedAbilities) {
    }

}
