package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.core.WynnExtras;
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
    public void write(JsonWriter out, RaidKind value) {
        JsonObject obj = gson.toJsonTree(value).getAsJsonObject();

        obj.addProperty("type", value.getAbbreviation());

        gson.toJson(obj, out);
    }

    @Override
    public RaidKind read(JsonReader in) {
        JsonObject obj = JsonParser.parseReader(in).getAsJsonObject();

        String abbreviation = obj.get("abbreviation").getAsString();

        return switch (abbreviation) {
            case "TNA" -> gson.fromJson(obj, TheNamelessAnomalyRaid.class);
            case "TCC" -> gson.fromJson(obj, TheCanyonColossusRaid.class);
            case "NOL" -> gson.fromJson(obj, OrphionsNexusOfLightRaid.class);
            case "NOG" -> gson.fromJson(obj, NestOfTheGrootslangsRaid.class);
            case "TWP" -> gson.fromJson(obj, TheWartornPalaceRaid.class);
            default -> {
                WynnExtras.LOGGER.error("Unknown raid: " + abbreviation);
                yield null;
            }
        };
    }
}

