package julianh06.wynnextras.features.wci.service.wynnbuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WynnBuilderIngredientRegistry {
    private static final String RESOURCE_PATH = "/assets/wynnextras/wci/wynnbuilder_ingredients.json";
    private static final Pattern ENTRY = Pattern.compile("\"(\\d+)\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");

    private final Map<Integer, String> ingredients;

    public WynnBuilderIngredientRegistry() {
        this(loadResource());
    }

    WynnBuilderIngredientRegistry(Map<Integer, String> ingredients) {
        this.ingredients = Map.copyOf(ingredients);
    }

    public String displayName(int id) {
        return ingredients.get(id);
    }

    private static Map<Integer, String> loadResource() {
        try (InputStream stream = WynnBuilderIngredientRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing WynnBuilder ingredient registry resource: " + RESOURCE_PATH);
            }
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return parse(json);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load WynnBuilder ingredient registry resource: " + RESOURCE_PATH, e);
        }
    }

    private static Map<Integer, String> parse(String json) {
        Map<Integer, String> parsed = new HashMap<>();
        Matcher matcher = ENTRY.matcher(json);
        while (matcher.find()) {
            parsed.put(Integer.parseInt(matcher.group(1)), unescapeJsonString(matcher.group(2)));
        }
        if (parsed.isEmpty()) {
            throw new IllegalStateException("WynnBuilder ingredient registry resource is empty: " + RESOURCE_PATH);
        }
        return parsed;
    }

    private static String unescapeJsonString(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\') {
                out.append(current);
                continue;
            }
            if (++i >= value.length()) throw new IllegalStateException("Invalid WynnBuilder ingredient registry escape sequence");
            char escaped = value.charAt(i);
            switch (escaped) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 >= value.length()) {
                        throw new IllegalStateException("Invalid WynnBuilder ingredient registry unicode escape");
                    }
                    out.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> throw new IllegalStateException("Invalid WynnBuilder ingredient registry escape: \\" + escaped);
            }
        }
        return out.toString();
    }
}
