package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class IconValueDeserializer implements JsonDeserializer<AbilityTreeData.Icon.IconValue> {
    @Override
    public AbilityTreeData.Icon.IconValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        AbilityTreeData.Icon.IconValue value = new AbilityTreeData.Icon.IconValue();
        value.id = obj.get("id").getAsString();
        value.name = obj.get("name").getAsString();

        JsonElement cmd = obj.get("customModelData");
        if (cmd.isJsonPrimitive()) {
            value.customModelData = cmd.getAsString();
        } else {
            value.customModelData = cmd.toString(); // oder als Objekt parsen, je nach Bedarf
        }

        return value;
    }
}

