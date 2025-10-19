package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class IconDeserializer implements JsonDeserializer<AbilityMapData.Icon> {
    @Override
    public AbilityMapData.Icon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        AbilityMapData.Icon icon = new AbilityMapData.Icon();
        icon.format = obj.get("format").getAsString();

        JsonElement valueElement = obj.get("value");
        if (valueElement.isJsonObject()) {
            icon.value = context.deserialize(valueElement, AbilityMapData.Icon.IconValue.class);
        } else if (valueElement.isJsonPrimitive()) {
            icon.value = valueElement.getAsString();
        } else {
            icon.value = null;
        }

        return icon;
    }
}

