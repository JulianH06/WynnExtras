package julianh06.wynnextras.features.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.wynntils.core.text.StyledText;

import java.io.IOException;

public class StyledTextAdapter extends TypeAdapter<StyledText> {
    @Override
    public void write(JsonWriter out, StyledText value) throws IOException {
        out.beginObject();
        out.name("text").value(value.getString());
        out.endObject();
    }

    @Override
    public StyledText read(JsonReader in) throws IOException {
        JsonObject obj = JsonParser.parseReader(in).getAsJsonObject();
        JsonElement textElement = obj.get("text");

        if (textElement == null || textElement.isJsonNull()) {
            return StyledText.fromString("");
        }

        String text = textElement.getAsString();
        return StyledText.fromString(text);
    }
}