package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Small runtime index that translates the numeric item IDs stored in WynnBuilder links. */
public final class WynnBuilderItemIndex {
    private static final WynnBuilderItemIndex INSTANCE = new WynnBuilderItemIndex();
    private volatile Map<Integer, String> names = Map.of();
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile String error = "";

    private WynnBuilderItemIndex() {}

    public static WynnBuilderItemIndex getInstance() {
        return INSTANCE;
    }

    public String name(int id) {
        return names.get(id);
    }

    public Integer id(String name) {
        if (name == null) return null;
        return names.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public boolean isReady() {
        return !names.isEmpty();
    }

    public boolean isLoading() {
        return loading.get();
    }

    public String error() {
        return error;
    }

    public void fetchAll() {
        if (!loading.compareAndSet(false, true)) return;
        try {
            replace(WynnBuilderDataFiles.read(WynnBuilderDataFiles.File.ITEMS));
            error = "";
        } catch (RuntimeException exception) {
            error = "Could not load WynnBuilder item IDs.";
            PlannerLog.LOGGER.warn("Could not load WynnBuilder item IDs.", exception);
        } finally {
            loading.set(false);
        }
    }

    private void replace(JsonElement root) {
        if (root == null || !root.isJsonObject()) throw new IllegalArgumentException("Missing item object");
        JsonElement source = root.getAsJsonObject().get("items");
        if (source == null || !source.isJsonArray()) throw new IllegalArgumentException("Missing items array");
        Map<Integer, String> parsed = new LinkedHashMap<>();
        for (JsonElement element : source.getAsJsonArray()) {
            try {
                JsonObject item = element.getAsJsonObject();
                int id = item.get("id").getAsInt();
                String name = string(item, "displayName");
                if (name.isBlank()) name = string(item, "name");
                if (!name.isBlank()) parsed.put(id, name);
            } catch (RuntimeException exception) {
                PlannerLog.LOGGER.debug("Skipping malformed WynnBuilder item index entry.", exception);
            }
        }
        if (parsed.isEmpty()) throw new IllegalArgumentException("WynnBuilder item index is empty");
        names = Map.copyOf(parsed);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
}
