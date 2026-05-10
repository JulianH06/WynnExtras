package julianh06.wynnextras.features.aspects;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.*;
import com.wynntils.utils.mc.McUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static julianh06.wynnextras.utils.WynncraftApiHandler.parseAspectAmount;

public class LocalAspectStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getPlayerDir() {
        if (McUtils.player() == null) return null;
        String uuid = McUtils.player().getUuidAsString();
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras").resolve("aspects").resolve(uuid);
        try { Files.createDirectories(dir); } catch (IOException e) {
            WynnExtras.LOGGER.error("Failed to create aspects directory: " + e.getMessage());
        }
        return dir;
    }

    private static Path getDataFile() {
        Path dir = getPlayerDir();
        return dir != null ? dir.resolve("data.json") : null;
    }

    private static Path getClassFile(String classId) {
        Path dir = getPlayerDir();
        return dir != null ? dir.resolve(classId + ".json") : null;
    }

    public static void save(Map<String, Pair<String, String>> map) {
        if (McUtils.player() == null) return;
        Path file = getDataFile();
        if (file == null) return;

        Map<String, JsonObject> existing = new LinkedHashMap<>();
        JsonArray current = load();
        if (current != null) {
            for (JsonElement el : current)  {
                JsonObject obj = el.getAsJsonObject();
                existing.put(obj.get("name").getAsString(), obj);
            }
        }

        for (Map.Entry<String, Pair<String, String>> entry : map.entrySet()) {
            try {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", entry.getKey());
                obj.addProperty("rarity", entry.getValue().getRight());
                obj.addProperty("amount", parseAspectAmount(entry.getValue()));
                existing.put(entry.getKey(), obj);
            } catch (Exception e) {
                WynnExtras.LOGGER.error("Failed to serialize aspect " + entry.getKey() + ": " + e.getMessage());
            }
        }

        JsonArray merged = new JsonArray();
        existing.values().forEach(merged::add);
        try { Files.writeString(file, GSON.toJson(merged)); }
        catch (IOException e) { WynnExtras.LOGGER.error("Failed to save aspects: " + e.getMessage()); }
    }

    public static JsonArray load() {
        Path file = getDataFile();
        if (file == null || !Files.exists(file)) return null;
        try { return JsonParser.parseString(Files.readString(file)).getAsJsonArray(); }
        catch (Exception e) { WynnExtras.LOGGER.error("Failed to load aspects: " + e.getMessage()); return null; }
    }

    public static void saveActiveAspects(String classId, Map<String, String> activeAspects) {
        Path file = getClassFile(classId);
        if (file == null) return;

        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> e : activeAspects.entrySet()) {
            obj.addProperty(e.getKey(), e.getValue());
        }
        try { Files.writeString(file, GSON.toJson(obj)); }
        catch (IOException e) { WynnExtras.LOGGER.error("Failed to save active aspects: " + e.getMessage()); }
    }

    public static Map<String, String> loadActiveAspects(String classId) {
        Path file = getClassFile(classId);
        Map<String, String> result = new LinkedHashMap<>();
        if (file == null || !Files.exists(file)) return result;
        try {
            JsonObject obj = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                result.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception e) { WynnExtras.LOGGER.error("Failed to load active aspects: " + e.getMessage()); }
        return result;
    }
}