package julianh06.wynnextras.features.spellhider;

import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public enum SpellModifier {
    SCALE(Vector3f.class, "{1.0, 1.0, 1.0}", s -> {
        String trimmed = s.replace("{", "").replace("}", "").trim();
        String[] parts = trimmed.split(",");
        return new Vector3f(
                Float.parseFloat(parts[0].trim()),
                Float.parseFloat(parts[1].trim()),
                Float.parseFloat(parts[2].trim())
        );
    }),
    VISIBLE(Boolean.class, List.of("false", "true"), Boolean::parseBoolean);
    // TODO FORCE_TO_GROUND

    private final Class<?> type;
    private final List<String> examples;
    private final Function<String, ?> parser;

    <T> SpellModifier(Class<?> type, List<String> examples, Function<String, T> parser) {
        this.type = type;
        this.examples = examples;
        this.parser = parser;
    }

    <T> SpellModifier(Class<?> type, String example, Function<String, T> parser) {
        this(type, List.of(example), parser);
    }

    public Class<?> getType() {
        return type;
    }

    public List<String> getSuggestions() {
        return examples.stream().map(value -> '"' + value + '"').toList();
    }

    @SuppressWarnings("unchecked")
    public <T> T parseValue(String string, Class<T> expectedType) {
        if (expectedType != type) {
            throw new IllegalArgumentException("Expected " + type + " but got " + expectedType);
        }
        try {
            return (T) parser.apply(string);
        } catch (Exception e) {
            return null;
        }
    }

    public static SpellModifier from(String string) {
        try {
            return SpellModifier.valueOf(string.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
