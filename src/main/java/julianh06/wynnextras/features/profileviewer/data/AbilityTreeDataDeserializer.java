package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilityTreeDataDeserializer implements JsonDeserializer<AbilityTreeData> {
    @Override
    public AbilityTreeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        AbilityTreeData atd = new AbilityTreeData();
        atd.pages = new HashMap<>();

        JsonElement root = json;

        // Case A: root is an object of pages: { "1": [ ... ], "2": [ ... ] }
        if (root.isJsonObject()) {
            JsonObject rootObj = root.getAsJsonObject();

            // Detect whether it's the "map" style (keys are numeric pages) or a normal object (could also contain archetypes)
            boolean looksLikePages = true;
            for (Map.Entry<String, JsonElement> e : rootObj.entrySet()) {
                String key = e.getKey();
                if (!key.matches("\\d+")) { looksLikePages = false; break; }
            }

            if (looksLikePages) {
                for (Map.Entry<String, JsonElement> entry : rootObj.entrySet()) {
                    int page = Integer.parseInt(entry.getKey());
                    JsonElement arr = entry.getValue();
                    if (!arr.isJsonArray()) continue;
                    List<AbilityTreeData.Node> nodes = new ArrayList<>();
                    for (JsonElement el : arr.getAsJsonArray()) {
                        AbilityTreeData.Node node = ctx.deserialize(el, AbilityTreeData.Node.class);
                        nodes.add(node);
                    }
                    atd.pages.put(page, nodes);
                }
                return atd;
            }
            // Otherwise fall through: maybe the response is an object with other keys (not page map)
        }

        // Case B: root is an array of nodes (character ability map)
        if (root.isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray()) {
                AbilityTreeData.Node node = ctx.deserialize(el, AbilityTreeData.Node.class);
                int page = 0;
                if (node != null && node.meta != null) page = node.meta.page;
                atd.pages.computeIfAbsent(page, k -> new ArrayList<>()).add(node);
            }
            return atd;
        }

        // Case C: root is object but not numeric keys: maybe an object containing "pages" or "map" field, try heuristics
        JsonObject obj = root.getAsJsonObject();
        if (obj.has("pages") && obj.get("pages").isJsonObject()) {
            JsonObject pagesObj = obj.getAsJsonObject("pages");
            for (Map.Entry<String, JsonElement> entry : pagesObj.entrySet()) {
                int page = Integer.parseInt(entry.getKey());
                JsonArray arr = entry.getValue().getAsJsonArray();
                List<AbilityTreeData.Node> nodes = new ArrayList<>();
                for (JsonElement el : arr) nodes.add(ctx.deserialize(el, AbilityTreeData.Node.class));
                atd.pages.put(page, nodes);
            }
            return atd;
        }

        // fallback: empty structure
        return atd;
    }
}
