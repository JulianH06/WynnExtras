package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class MetaDeserializer implements JsonDeserializer<AbilityTreeData.Node.Meta> {
    @Override
    public AbilityTreeData.Node.Meta deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        AbilityTreeData.Node.Meta meta = new AbilityTreeData.Node.Meta();

        // icon: entweder String oder Objekt
        JsonElement iconElement = obj.get("icon");
        if (iconElement.isJsonPrimitive()) {
            meta.icon = iconElement.getAsString();
        } else if (iconElement.isJsonObject()) {
            meta.icon = context.deserialize(iconElement, AbilityTreeData.Icon.class);
        }

        meta.page = obj.get("page").getAsInt();

        if (obj.has("id")) {
            meta.id = obj.get("id").getAsString();
        }

        return meta;
    }
}

