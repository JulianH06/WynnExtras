package julianh06.wynnextras.features.raid;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RaidSnapshotAdapter implements JsonSerializer<RaidSnapshot>, JsonDeserializer<RaidSnapshot> {
    @Override
    public JsonElement serialize(RaidSnapshot snapshot, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("raidKind", snapshot.raidKind().name());
        object.addProperty("raidStartTime", snapshot.raidStartTime());
        object.addProperty("timeInRaid", snapshot.timeInRaid());
        object.addProperty("timeInRooms", snapshot.timeInRooms());
        object.add("challenges", context.serialize(snapshot.challenges()));
        return object;
    }

    @Override
    public RaidSnapshot deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        WERaidKind kind = readKind(object.get("raidKind"));
        Map<Integer, RaidRoomData> rooms = readRooms(object.get("challenges"));
        long start = longValue(object, "raidStartTime");
        long total = longValue(object, "timeInRaid");
        long roomTotal = object.has("timeInRooms") ? longValue(object, "timeInRooms")
                : rooms.values().stream().mapToLong(room -> Math.max(0, room.totalTime())).sum();
        return new RaidSnapshot(kind, rooms, start, total, roomTotal);
    }

    private static WERaidKind readKind(JsonElement element) {
        if (element == null || element.isJsonNull()) return WERaidKind.UNKNOWN;
        if (element.isJsonPrimitive()) {
            try {
                return WERaidKind.valueOf(element.getAsString());
            } catch (IllegalArgumentException ignored) {
                return WERaidKind.from(element.getAsString(), null);
            }
        }
        JsonObject object = element.getAsJsonObject();
        return WERaidKind.from(stringValue(object, "abbreviation"), stringValue(object, "raidName"));
    }

    private static Map<Integer, RaidRoomData> readRooms(JsonElement element) {
        Map<Integer, RaidRoomData> rooms = new LinkedHashMap<>();
        if (element == null || !element.isJsonObject()) return rooms;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            try {
                int index = Integer.parseInt(entry.getKey());
                JsonObject room = entry.getValue().getAsJsonObject();
                String name = room.has("name") ? stringValue(room, "name") : stringValue(room, "roomName");
                long total = room.has("totalTime") ? longValue(room, "totalTime") : longValue(room, "roomTotalTime");
                long end = room.has("endTime") ? longValue(room, "endTime") : longValue(room, "roomEndTime");
                rooms.put(index, new RaidRoomData(name, total, end));
            } catch (NumberFormatException ignored) {}
        }
        return rooms;
    }

    private static String stringValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private static long longValue(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : 0;
    }
}
