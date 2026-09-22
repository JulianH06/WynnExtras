package julianh06.wynnextras.features.leaderboardviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LeaderboardValueFormatter {
    private static final Set<String> PROFESSION_LEADERBOARDS = Set.of(
            "woodcuttingLevel", "miningLevel", "fishingLevel", "farmingLevel",
            "alchemismLevel", "armouringLevel", "cookingLevel", "jewelingLevel",
            "scribingLevel", "tailoringLevel", "weaponsmithingLevel", "woodworkingLevel"
    );

    private LeaderboardValueFormatter() {
    }

    public static String format(LeaderboardDefinition leaderboard, LeaderboardEntry entry) {
        return format(leaderboard, entry, null);
    }

    public static String format(LeaderboardDefinition leaderboard, LeaderboardEntry entry,
                                LeaderboardEntry nextEntry) {
        String id = leaderboard.id();
        if (id != null && id.startsWith("guildSeason")) {
            String value = formatNumber(entry.score()) + " SR";
            if (nextEntry == null || nextEntry.rank() != entry.rank() + 1
                    || entry.score() == null || nextEntry.score() == null) {
                return value + " (+?)";
            }
            return value + " (+" + formatNumber(entry.score() - nextEntry.score()) + ")";
        }
        if (PROFESSION_LEADERBOARDS.contains(id)) {
            return formatNumber(entry.score()) + " - " + formatCompact(getNumber(entry.metadata(), "xp")) + " XP";
        }
        if (isPlayerRaid(id, "Completion")) {
            return formatNumber(entry.score()) + " completions - "
                    + formatDecimal(getNumber(entry.metadata(), "playtime")) + "h";
        }
        if (isPlayerRaid(id, "SrPlayers") || isPlayerRaid(id, "SrGPlayers")) {
            return formatNumber(entry.score()) + " rating - "
                    + formatNumber(getNumber(entry.metadata(), "completions")) + " completions - "
                    + formatNumber(getNumber(entry.metadata(), "gambits")) + " gambits";
        }

        Double guildValue = switch (id) {
            case "guildLevel" -> getNumber(entry.rawData(), "level");
            case "guildTerritories" -> getNumber(entry.rawData(), "territories");
            case "guildWars" -> getNumber(entry.rawData(), "wars");
            case "guildTotalRaids" -> getNumber(entry.rawData(), "totalRaids");
            default -> null;
        };
        if (guildValue != null) {
            return formatNumber(guildValue) + ("guildTotalRaids".equals(id) ? " SR" : "");
        }

        List<String> values = new ArrayList<>();
        if (entry.score() != null) {
            values.add(formatNumber(entry.score()) + (isGuildSeasonRating(id) ? " SR" : ""));
        }
        if (entry.metadata() != null) {
            for (var metadataEntry : entry.metadata().entrySet()) {
                if (!metadataEntry.getValue().isJsonPrimitive()
                        || !metadataEntry.getValue().getAsJsonPrimitive().isNumber()) continue;
                Double value = metadataEntry.getValue().getAsDouble();
                values.add(formatMetadata(metadataEntry.getKey(), value));
            }
        }
        return values.isEmpty() ? "-" : String.join(" - ", values);
    }

    private static boolean isPlayerRaid(String id, String suffix) {
        return id != null && id.endsWith(suffix)
                && (id.startsWith("grootslang") || id.startsWith("orphion") || id.startsWith("colossus")
                || id.startsWith("nameless") || id.startsWith("fruma"));
    }

    private static boolean isGuildSeasonRating(String id) {
        return id != null && ("guildTotalRaids".equals(id)
                || id.endsWith("SrGuilds") || id.startsWith("guildSeason"));
    }

    private static String formatMetadata(String key, Double value) {
        return switch (key) {
            case "xp" -> formatCompact(value) + " XP";
            case "playtime" -> formatDecimal(value) + "h";
            case "completions" -> formatNumber(value) + " completions";
            case "gambits" -> formatNumber(value) + " gambits";
            case "totalLevel" -> formatNumber(value) + " total Level";
            default -> formatNumber(value) + " " + key;
        };
    }

    private static Double getNumber(JsonObject object, String key) {
        if (object == null) return null;
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                ? null : value.getAsDouble();
    }

    private static String formatNumber(Double value) {
        if (value == null || !Double.isFinite(value)) return "-";
        return formatter(value == Math.rint(value) ? "#,##0" : "#,##0.##").format(value);
    }

    private static String formatDecimal(Double value) {
        if (value == null || !Double.isFinite(value)) return "-";
        return formatter("#,##0.##").format(value);
    }

    private static String formatCompact(Double value) {
        if (value == null || !Double.isFinite(value)) return "-";
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000_000) return formatter("0.##").format(value / 1_000_000_000) + "B";
        if (absolute >= 1_000_000) return formatter("0.##").format(value / 1_000_000) + "M";
        if (absolute >= 1_000) return formatter("0.##").format(value / 1_000) + "K";
        return formatNumber(value);
    }

    private static DecimalFormat formatter(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
    }
}
