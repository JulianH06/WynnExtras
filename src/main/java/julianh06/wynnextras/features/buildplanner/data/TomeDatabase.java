package julianh06.wynnextras.features.buildplanner.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TomeDatabase {
    private static final String RESOURCE = "/assets/wynnextras/buildplanner/data/wynnbuilder_tomes.json";
    private static final Pattern ROMAN_TIER =
            Pattern.compile("\\b(V|IV|III|II|I)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> SUPPORTED_IDS = Set.of(
            "hpBonus", "eDefPct", "tDefPct", "wDefPct", "fDefPct", "aDefPct",
            "mr", "ms", "ls", "hprRaw", "hprPct", "spd", "damPct", "mdRaw",
            "sdRaw", "mdPct", "sdPct", "eDamPct", "tDamPct", "wDamPct", "fDamPct",
            "aDamPct", "ref", "healPct", "eSteal", "lb", "spPct1", "spPct2",
            "spPct3", "spPct4", "str", "dex", "int", "def", "agi", "sprint",
            "sprintReg", "thorns");
    private static final TomeDatabase INSTANCE = new TomeDatabase();

    private final List<WynnTome> tomes;
    private final Map<String, WynnTome> byName;

    private TomeDatabase() {
        List<WynnTome> loaded = load();
        tomes = List.copyOf(loaded);
        Map<String, WynnTome> names = new LinkedHashMap<>();
        for (WynnTome tome : loaded) {
            names.put(tome.displayName().toLowerCase(Locale.ROOT), tome);
        }
        byName = Map.copyOf(names);
    }

    public static TomeDatabase getInstance() {
        return INSTANCE;
    }

    public WynnTome get(String name) {
        return name == null ? null : byName.get(name.toLowerCase(Locale.ROOT));
    }

    public List<WynnTome> search(String type, String query) {
        String normalizedType = type == null ? "" : type;
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return tomes.stream()
                .filter(tome -> tome.type().equals(normalizedType))
                .filter(tome -> normalizedQuery.isEmpty()
                        || tome.displayName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || tome.alias().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(searchOrder())
                .toList();
    }

    static Comparator<WynnTome> searchOrder() {
        return Comparator.comparingInt((WynnTome tome) -> rarityRank(tome.tier()))
                .thenComparing(
                        Comparator.comparingInt(
                                (WynnTome tome) -> romanTier(tome.displayName())).reversed())
                .thenComparing(Comparator.comparingInt(WynnTome::level).reversed())
                .thenComparing(WynnTome::displayName, String.CASE_INSENSITIVE_ORDER);
    }

    private static int rarityRank(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "mythic" -> 0;
            case "fabled" -> 1;
            case "legendary" -> 2;
            case "rare" -> 3;
            case "unique" -> 4;
            default -> 5;
        };
    }

    private static int romanTier(String name) {
        Matcher matcher = ROMAN_TIER.matcher(name);
        if (!matcher.find()) {
            return 0;
        }
        return switch (matcher.group(1).toUpperCase(Locale.ROOT)) {
            case "V" -> 5;
            case "IV" -> 4;
            case "III" -> 3;
            case "II" -> 2;
            default -> 1;
        };
    }

    private static List<WynnTome> load() {
        try (var stream = TomeDatabase.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            List<WynnTome> result = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("tomes")) {
                JsonObject source = element.getAsJsonObject();
                if (source.has("remapID")
                        || "DEPRECATED".equalsIgnoreCase(string(source, "restrict"))) {
                    continue;
                }
                Map<String, Integer> ids = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()
                            && entry.getValue().getAsJsonPrimitive().isNumber()
                            && SUPPORTED_IDS.contains(entry.getKey())) {
                        ids.put(WynnBuilderIdentifications.name(entry.getKey()), entry.getValue().getAsInt());
                    }
                }
                result.add(new WynnTome(
                        string(source, "displayName"),
                        string(source, "alias"),
                        string(source, "type"),
                        string(source, "tier"),
                        source.has("lvl") ? source.get("lvl").getAsInt() : 0,
                        ids));
            }
            return result;
        } catch (RuntimeException | java.io.IOException exception) {
            throw new IllegalStateException("Could not load the packaged WynnBuilder tome database", exception);
        }
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }
}
