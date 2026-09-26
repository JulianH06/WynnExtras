package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AspectDatabase {
    private static final String RESOURCE =
            "/assets/wynnextras/buildplanner/data/wynnbuilder_aspects_2.2.3.0.json";
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final AspectDatabase INSTANCE = new AspectDatabase();

    private final Map<AbilityTreeClass, List<WynnAspect>> byClass;
    private final Map<AbilityTreeClass, Map<String, WynnAspect>> byName;

    private AspectDatabase() {
        Map<AbilityTreeClass, List<WynnAspect>> loaded = load();
        byClass = Map.copyOf(loaded);
        Map<AbilityTreeClass, Map<String, WynnAspect>> names = new EnumMap<>(AbilityTreeClass.class);
        loaded.forEach((abilityClass, aspects) -> {
            Map<String, WynnAspect> classNames = new LinkedHashMap<>();
            aspects.forEach(aspect ->
                    classNames.put(aspect.displayName().toLowerCase(Locale.ROOT), aspect));
            names.put(abilityClass, Map.copyOf(classNames));
        });
        byName = Map.copyOf(names);
    }

    public static AspectDatabase getInstance() {
        return INSTANCE;
    }

    public WynnAspect get(AbilityTreeClass abilityClass, String name) {
        if (name == null) {
            return null;
        }
        return byName.getOrDefault(abilityClass, Map.of()).get(name.toLowerCase(Locale.ROOT));
    }

    public List<WynnAspect> search(AbilityTreeClass abilityClass, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return byClass.getOrDefault(abilityClass, List.of()).stream()
                .filter(aspect -> normalized.isEmpty()
                        || aspect.displayName().toLowerCase(Locale.ROOT).contains(normalized)
                        || aspect.aliases().stream()
                                .anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(normalized)))
                .sorted(Comparator
                        .comparingInt((WynnAspect aspect) -> rarityOrder(aspect.rarity()))
                        .thenComparing(WynnAspect::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Map<AbilityTreeClass, List<WynnAspect>> load() {
        try (var stream = AspectDatabase.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing packaged WynnBuilder aspect database");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<AbilityTreeClass, List<WynnAspect>> result =
                    new EnumMap<>(AbilityTreeClass.class);
            for (AbilityTreeClass abilityClass : AbilityTreeClass.values()) {
                JsonArray source = root.getAsJsonArray(abilityClass.displayName());
                List<WynnAspect> aspects = new ArrayList<>();
                if (source != null) {
                    for (JsonElement element : source) {
                        JsonObject aspect = element.getAsJsonObject();
                        aspects.add(parseAspect(abilityClass, aspect));
                    }
                }
                result.put(abilityClass, List.copyOf(aspects));
            }
            return result;
        } catch (RuntimeException | java.io.IOException exception) {
            throw new IllegalStateException(
                    "Could not load the packaged WynnBuilder aspect database", exception);
        }
    }

    private static WynnAspect parseAspect(
            AbilityTreeClass abilityClass, JsonObject source
    ) {
        List<String> aliases = new ArrayList<>();
        if (source.has("aliases") && source.get("aliases").isJsonArray()) {
            source.getAsJsonArray("aliases").forEach(alias -> aliases.add(alias.getAsString()));
        }
        List<WynnAspect.Tier> tiers = new ArrayList<>();
        for (JsonElement tierElement : source.getAsJsonArray("tiers")) {
            JsonObject tier = tierElement.getAsJsonObject();
            List<JsonObject> abilities = new ArrayList<>();
            JsonArray rawAbilities = tier.getAsJsonArray("abilities");
            if (rawAbilities != null) {
                rawAbilities.forEach(ability -> abilities.add(ability.getAsJsonObject()));
            }
            tiers.add(new WynnAspect.Tier(
                    tier.get("threshold").getAsInt(),
                    cleanDescription(tier.get("description").getAsString()),
                    abilities));
        }
        return new WynnAspect(
                abilityClass,
                source.get("displayName").getAsString(),
                source.get("id").getAsInt(),
                source.get("tier").getAsString(),
                aliases,
                tiers);
    }

    private static int rarityOrder(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "mythic" -> 0;
            case "fabled" -> 1;
            case "legendary" -> 2;
            default -> 3;
        };
    }

    private static String cleanDescription(String value) {
        String withBreaks = value.replaceAll("(?i)</?br\\s*/?>", " ");
        return HTML_TAG.matcher(withBreaks)
                .replaceAll("")
                .replace("&emsp;", " ")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
