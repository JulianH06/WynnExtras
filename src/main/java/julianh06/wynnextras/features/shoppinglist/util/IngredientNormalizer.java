package julianh06.wynnextras.features.shoppinglist.util;

import java.text.Normalizer;
import java.util.Locale;

public final class IngredientNormalizer {
    private IngredientNormalizer() {}

    public static String key(String name) {
        String normalized = clean(name).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        return normalized.replaceAll("^_|_$", "");
    }

    public static String displayName(String name) {
        String cleaned = clean(name).toLowerCase(Locale.ROOT);
        StringBuilder title = new StringBuilder(cleaned.length());
        boolean capitalize = true;
        for (char character : cleaned.toCharArray()) {
            if (Character.isWhitespace(character)) {
                title.append(character);
                capitalize = true;
            } else if (capitalize) {
                title.append(Character.toTitleCase(character));
                capitalize = false;
            } else {
                title.append(character);
            }
        }
        return title.toString();
    }

    private static String clean(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC);
        normalized = normalized.trim().replace('_', ' ').replace('-', ' ');
        return normalized.replaceAll("\\s+", " ").trim();
    }
}
