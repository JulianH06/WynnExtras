package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class NodeDeserializer implements JsonDeserializer<AbilityMapData.Node> {
    @Override
    public AbilityMapData.Node deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        AbilityMapData.Node node = new AbilityMapData.Node();

        node.type = getAsStringOrNull(obj, "type");
        if (obj.has("coordinates") && obj.get("coordinates").isJsonObject()) {
            node.coordinates = ctx.deserialize(obj.get("coordinates"), AbilityMapData.Coordinates.class);
        }
        if (obj.has("family") && obj.get("family").isJsonArray()) {
            node.family = new ArrayList<>();
            for (JsonElement e : obj.get("family").getAsJsonArray()) node.family.add(e.getAsString());
        }

        if (obj.has("meta") && obj.get("meta").isJsonObject()) {
            JsonObject metaObj = obj.getAsJsonObject("meta");
            AbilityMapData.Node.Meta meta = new AbilityMapData.Node.Meta();

            // page
            if (metaObj.has("page") && metaObj.get("page").isJsonPrimitive()) {
                meta.page = metaObj.get("page").getAsInt();
            }

            // id (only for ability)
            if (metaObj.has("id") && metaObj.get("id").isJsonPrimitive()) {
                meta.id = metaObj.get("id").getAsString();
            }

            // icon can be string or object
            if (metaObj.has("icon")) {
                JsonElement iconEl = metaObj.get("icon");
                if (iconEl.isJsonPrimitive()) {
                    meta.icon = iconEl.getAsString();
                } else if (iconEl.isJsonObject()) {
                    // Try to parse as AbilityMapData.Icon (format + value) or as simple Icon object
                    try {
                        AbilityMapData.Icon parsed = ctx.deserialize(iconEl, AbilityMapData.Icon.class);
                        meta.icon = parsed;
                    } catch (JsonParseException ex) {
                        // fallback to raw object
                        meta.icon = iconEl;
                    }
                } else {
                    meta.icon = iconEl;
                }
            }

            node.meta = meta;
        }

        return node;
    }

    private String getAsStringOrNull(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
