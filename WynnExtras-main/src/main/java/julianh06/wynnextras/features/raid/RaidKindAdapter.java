package julianh06.wynnextras.features.raid;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.wynntils.models.raid.raids.*;

import java.io.IOException;

public class RaidKindAdapter extends TypeAdapter<RaidKind> {

    private final Gson gson;

    public RaidKindAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, RaidKind value) throws IOException {
        JsonObject obj = gson.toJsonTree(value).getAsJsonObject();

        // Optional: explizit Typ hinzufügen
        obj.addProperty("type", value.getAbbreviation());

        gson.toJson(obj, out);
    }

    @Override
    public RaidKind read(JsonReader in) throws IOException {
        JsonObject obj = JsonParser.parseReader(in).getAsJsonObject();

        String abbreviation = obj.get("abbreviation").getAsString();

        switch (abbreviation) {
            case "TNA":
                return gson.fromJson(obj, TheNamelessAnomalyRaid.class);
            case "TCC":
                return gson.fromJson(obj, TheCanyonColossusRaid.class);
            case "NOL":
                return gson.fromJson(obj, OrphionsNexusOfLightRaid.class);
            case "NOG":
                return gson.fromJson(obj, NestOfTheGrootslangsRaid.class);
            default:
                throw new JsonParseException("Unknown raid: " + abbreviation);
        }
    }
}

