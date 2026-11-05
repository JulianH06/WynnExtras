package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WynnBuilderSetBonuses {
    private static final Data EMPTY = new Data(Map.of(), Map.of(), Map.of());
    private static volatile Data data;

    private WynnBuilderSetBonuses() {}

    public static Map<String, Integer> forItems(List<WynnItem> items) {
        Data current = data();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WynnItem item : items) {
            for (String set : current.itemSets.getOrDefault(normalize(item.displayName()), List.of())) {
                counts.merge(set, 1, Integer::sum);
            }
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        counts.forEach((set, count) -> {
            List<Map<String, Integer>> tiers = current.bonuses.getOrDefault(set, List.of());
            if (count <= tiers.size()) {
                tiers.get(count - 1).forEach((key, value) -> result.merge(key, value, Integer::sum));
            }
        });
        return result;
    }

    public static List<SetInfo> forItem(WynnItem item) {
        Data current = data();
        return current.itemSets.getOrDefault(normalize(item.displayName()), List.of()).stream()
                .map(name -> new SetInfo(name, current.setItems.getOrDefault(name, List.of()),
                        current.bonuses.getOrDefault(name, List.of())))
                .toList();
    }

    public static List<ActiveSet> activeSets(List<WynnItem> items) {
        Data current = data();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (WynnItem item : items) {
            for (String set : current.itemSets.getOrDefault(normalize(item.displayName()), List.of())) {
                counts.merge(set, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().map(entry -> {
            String name = entry.getKey();
            SetInfo info = new SetInfo(name, current.setItems.getOrDefault(name, List.of()),
                    current.bonuses.getOrDefault(name, List.of()));
            return new ActiveSet(info, entry.getValue(), info.bonus(entry.getValue()));
        }).toList();
    }

    private static Data data() {
        Data current = data;
        if (current != null) return current;
        synchronized (WynnBuilderSetBonuses.class) {
            if (data != null) return data;
            Data loaded = load();
            if (loaded != null) data = loaded;
            return loaded == null ? EMPTY : loaded;
        }
    }

    private static Data load() {
        try {
            JsonElement root = WynnBuilderDataFiles.readCached(WynnBuilderDataFiles.File.ITEMS);
            if (root == null) return null;
            JsonObject sets = root.getAsJsonObject().getAsJsonObject("sets");
            Map<String, List<String>> itemSets = new LinkedHashMap<>();
            Map<String, List<String>> setItems = new LinkedHashMap<>();
            Map<String, List<Map<String, Integer>>> bonuses = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : sets.entrySet()) {
                try {
                    JsonObject set = entry.getValue().getAsJsonObject();
                    List<String> items = new ArrayList<>();
                    for (JsonElement item : set.getAsJsonArray("items")) {
                        String itemName = item.getAsString();
                        items.add(itemName);
                        itemSets.computeIfAbsent(normalize(itemName), ignored -> new ArrayList<>())
                                .add(entry.getKey());
                    }
                    setItems.put(entry.getKey(), List.copyOf(items));
                    List<Map<String, Integer>> tiers = new ArrayList<>();
                    for (JsonElement tier : set.getAsJsonArray("bonuses")) tiers.add(parseBonus(tier));
                    bonuses.put(entry.getKey(), List.copyOf(tiers));
                } catch (RuntimeException exception) {
                    PlannerLog.LOGGER.debug("Skipping malformed WynnBuilder set {}.", entry.getKey(), exception);
                }
            }
            return new Data(immutableLists(itemSets), Map.copyOf(setItems), Map.copyOf(bonuses));
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not load WynnBuilder set bonuses.", exception);
            return EMPTY;
        }
    }

    private static Map<String, Integer> parseBonus(JsonElement element) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) continue;
            try {
                result.put(WynnBuilderIdentifications.name(entry.getKey()), entry.getValue().getAsInt());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, List<String>> immutableLists(Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    public record SetInfo(String name, List<String> items, List<Map<String, Integer>> bonuses) {
        public SetInfo {
            items = List.copyOf(items);
            bonuses = List.copyOf(bonuses);
        }

        public int equippedCount(List<WynnItem> equipped) {
            return (int) equipped.stream().filter(item -> items.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(item.displayName()))).count();
        }

        public Map<String, Integer> bonus(int pieces) {
            return pieces > 0 && pieces <= bonuses.size() ? bonuses.get(pieces - 1) : Map.of();
        }

        public int nextBonusPieces(int pieces) {
            for (int index = Math.max(0, pieces); index < bonuses.size(); index++) {
                if (!bonuses.get(index).isEmpty()) return index + 1;
            }
            return -1;
        }
    }

    public record ActiveSet(SetInfo info, int pieces, Map<String, Integer> bonus) {
        public ActiveSet {
            bonus = Map.copyOf(bonus);
        }
    }

    private record Data(
            Map<String, List<String>> itemSets,
            Map<String, List<String>> setItems,
            Map<String, List<Map<String, Integer>>> bonuses
    ) {}
}
