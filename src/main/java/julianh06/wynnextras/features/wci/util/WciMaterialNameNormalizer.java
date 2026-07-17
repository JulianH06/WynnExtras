package julianh06.wynnextras.features.wci.util;

import julianh06.wynnextras.features.wci.service.WciTextCleaner;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WciMaterialNameNormalizer {
    private static final Pattern REFINED_PREFIX = Pattern.compile("(?i)^refined\\b\\s*");
    private static final Pattern EXPLICIT_TIER = Pattern.compile("(?i)\\b(?:t|tier)\\s*[:#-]?\\s*([1-3])\\b");
    private static final Pattern COMPONENT_TIER = Pattern.compile("(?i)\\bprofession[_\\s-]*tier[_\\s-]*([1-3])\\b");
    private static final Pattern UNICODE_STAR_TIER = Pattern.compile("[★☆✫✪✦✧✩⭐✭✮✯✰⭑]{1,3}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final List<String> MOJIBAKE_STAR_MARKERS = List.of(
            "â˜…",
            "â˜†",
            "âœ«",
            "âœª",
            "âœ¦",
            "âœ§",
            "âœ©",
            "â­",
            "âœ­",
            "âœ®",
            "âœ¯",
            "âœ°",
            "â­‘");

    private WciMaterialNameNormalizer() {}

    public static String tradeMarketQuery(String displayName) {
        return baseName(displayName);
    }

    public static String key(String displayName) {
        return IngredientNormalizer.key(baseName(displayName));
    }

    public static String baseName(String displayName) {
        String cleaned = cleanMaterialText(displayName);
        cleaned = EXPLICIT_TIER.matcher(cleaned).replaceAll(" ");
        cleaned = removeStarMarkers(cleaned);
        cleaned = collapseWhitespace(cleaned);
        cleaned = REFINED_PREFIX.matcher(cleaned).replaceFirst("");
        return collapseWhitespace(cleaned);
    }

    public static Integer detectTier(String displayName, List<String> loreLines) {
        Integer tier = parseTier(displayName);
        if (tier != null) {
            return tier;
        }
        if (loreLines == null) {
            return null;
        }
        for (String line : loreLines) {
            tier = parseTier(line);
            if (tier != null) {
                return tier;
            }
        }
        return null;
    }

    public static Integer detectTierFromComponentStrings(Collection<String> componentStrings) {
        if (componentStrings == null) {
            return null;
        }
        for (String componentString : componentStrings) {
            if (componentString == null || componentString.isBlank()) {
                continue;
            }
            Matcher matcher = COMPONENT_TIER.matcher(componentString);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return null;
    }

    public static Integer parseTier(String text) {
        String cleaned = cleanMaterialText(text);
        if (cleaned.isBlank()) {
            return null;
        }

        Matcher explicitTier = EXPLICIT_TIER.matcher(cleaned);
        if (explicitTier.find()) {
            return Integer.parseInt(explicitTier.group(1));
        }

        Matcher unicodeStars = UNICODE_STAR_TIER.matcher(cleaned);
        if (unicodeStars.find()) {
            int tier = unicodeStars.group().length();
            return tier >= 1 && tier <= 3 ? tier : null;
        }

        for (String marker : MOJIBAKE_STAR_MARKERS) {
            int occurrences = countOccurrences(cleaned, marker);
            if (occurrences >= 1 && occurrences <= 3) {
                return occurrences;
            }
        }
        return null;
    }

    public static boolean needsLoreForTier(String displayName) {
        String cleaned = cleanMaterialText(displayName);
        return parseTier(cleaned) == null && REFINED_PREFIX.matcher(cleaned).find();
    }

    private static String cleanMaterialText(String text) {
        return WciTextCleaner.clean(text).replace('_', ' ').replace('-', ' ');
    }

    private static String removeStarMarkers(String text) {
        String withoutStars = UNICODE_STAR_TIER.matcher(text).replaceAll(" ");
        for (String marker : MOJIBAKE_STAR_MARKERS) {
            withoutStars = withoutStars.replace(marker, " ");
        }
        return withoutStars;
    }

    private static String collapseWhitespace(String text) {
        return WHITESPACE.matcher(text == null ? "" : text).replaceAll(" ").trim();
    }

    private static int countOccurrences(String text, String marker) {
        if (text == null || text.isEmpty() || marker == null || marker.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(marker, index)) >= 0) {
            count++;
            index += marker.length();
        }
        return count;
    }
}
