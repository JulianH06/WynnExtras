package julianh06.wynnextras.features.buildplanner.data;

import julianh06.wynnextras.wynncraft.state.CharacterClass;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AspectDatabase {
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final AspectDatabase INSTANCE = new AspectDatabase();

    private final Map<CharacterClass, List<WynnAspect>> byClass;
    private final Map<CharacterClass, Map<String, WynnAspect>> byName;

    private AspectDatabase() {
        Map<CharacterClass, List<WynnAspect>> loaded = load();
        byClass = Map.copyOf(loaded);
        Map<CharacterClass, Map<String, WynnAspect>> names = new EnumMap<>(CharacterClass.class);
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

    public WynnAspect get(CharacterClass abilityClass, String name) {
        if (name == null) {
            return null;
        }
        return byName.getOrDefault(abilityClass, Map.of()).get(name.toLowerCase(Locale.ROOT));
    }

    public WynnAspect get(CharacterClass abilityClass, int id) {
        return byClass.getOrDefault(abilityClass, List.of()).stream()
                .filter(aspect -> aspect.id() == id).findFirst().orElse(null);
    }

    public List<WynnAspect> search(CharacterClass abilityClass, String query) {
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

    private static Map<CharacterClass, List<WynnAspect>> load() {
        try {
            JsonObject root = WynnBuilderDataFiles.read(
                    WynnBuilderDataFiles.File.ASPECTS).getAsJsonObject();
            Map<CharacterClass, List<WynnAspect>> result =
                    new EnumMap<>(CharacterClass.class);
            for (CharacterClass abilityClass : CharacterClass.playableValues()) {
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
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Could not load the current WynnBuilder aspect database", exception);
        }
    }

    private static WynnAspect parseAspect(
            CharacterClass abilityClass, JsonObject source
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
