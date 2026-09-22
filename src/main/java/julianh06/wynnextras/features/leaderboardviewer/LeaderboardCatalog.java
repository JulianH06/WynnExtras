package julianh06.wynnextras.features.leaderboardviewer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LeaderboardCatalog {
    private static final Pattern GUILD_SEASON_ID = Pattern.compile("guildSeason(\\d+)");
    private static final int FALLBACK_LATEST_GUILD_SEASON = 32;
    private static final List<LeaderboardCategory> PLAYER_CATEGORIES = List.of(
            category("Profession",
                    leaderboard("woodcuttingLevel", "Woodcutting"), leaderboard("miningLevel", "Mining"),
                    leaderboard("fishingLevel", "Fishing"), leaderboard("farmingLevel", "Farming"),
                    leaderboard("alchemismLevel", "Alchemism"), leaderboard("armouringLevel", "Armouring"),
                    leaderboard("cookingLevel", "Cooking"), leaderboard("jewelingLevel", "Jeweling"),
                    leaderboard("scribingLevel", "Scribing"), leaderboard("tailoringLevel", "Tailoring"),
                    leaderboard("weaponsmithingLevel", "Weaponsmithing"), leaderboard("woodworkingLevel", "Woodworking")),
            category("Content",
                    leaderboard("warsCompletion", "Wars"),
                    leaderboard("playerContent", "Content Completion"),
                    leaderboard("globalPlayerContent", "Global Content Completion")),
            category("Raiding",
                    raid("grootslang", "Nest of the Grootslangs"),
                    raid("orphion", "Orphion's Nexus of Light"),
                    raid("colossus", "The Canyon Colossus"),
                    raid("nameless", "The Nameless Anomaly"),
                    raid("fruma", "The Wartorn Palace")),
            category("Total Level",
                    totalLevel("professions", "Profession Level"),
                    totalLevel("combat", "Combat Level"),
                    totalLevel("total", "Total Level"))
    );

    private static final List<LeaderboardCategory> GUILD_BASE_CATEGORIES = List.of(
            category("Global",
                    leaderboard("guildLevel", "Level"),
                    leaderboard("guildTerritories", "Territories"),
                    leaderboard("guildWars", "Wars"),
                    leaderboard("guildTotalRaids", "Total Raids")),
            category("Raid",
                    leaderboard("grootslangSrGuilds", "Nest of the Grootslangs"),
                    leaderboard("orphionSrGuilds", "Orphion's Nexus of Light"),
                    leaderboard("colossusSrGuilds", "The Canyon Colossus"),
                    leaderboard("namelessSrGuilds", "The Nameless Anomaly"),
                    leaderboard("frumaSrGuilds", "The Wartorn Palace"))
    );

    private static final List<LeaderboardCategory> GAMEMODE_CATEGORIES = List.of(
            category("Global",
                    leaderboard("craftsmanContent", "Craftsman"),
                    leaderboard("ironmanContent", "Ironman"),
                    leaderboard("huntedContent", "Hunted"),
                    leaderboard("hardcoreContent", "Hardcore"),
                    leaderboard("ultimateIronmanContent", "Ultimate Ironman")),
            category("Special",
                    leaderboard("huichContent", "Huich"),
                    leaderboard("huicContent", "Huic"),
                    leaderboard("hicContent", "Hic"),
                    leaderboard("hichContent", "Hich")),
            category("Legacy", leaderboard("hardcoreLegacyLevel", "Hardcore"))
    );

    private LeaderboardCatalog() {
    }

    public static List<LeaderboardCategory> categoriesFor(LVScreen.Type type) {
        return switch (type) {
            case Guild -> guildCategories(fallbackGuildSeasonIds());
            case Player -> PLAYER_CATEGORIES;
            case Gamemode -> GAMEMODE_CATEGORIES;
        };
    }

    public static List<LeaderboardCategory> guildCategories(List<String> leaderboardTypes) {
        List<SeasonLeaderboard> seasons = leaderboardTypes.stream()
                .map(GUILD_SEASON_ID::matcher)
                .filter(Matcher::matches)
                .map(matcher -> new SeasonLeaderboard(
                        matcher.group(), Integer.parseInt(matcher.group(1))))
                .sorted(Comparator.comparingInt(SeasonLeaderboard::number).reversed())
                .toList();
        if (seasons.isEmpty()) return guildCategories(fallbackGuildSeasonIds());

        List<LeaderboardCategory> categories = new ArrayList<>(GUILD_BASE_CATEGORIES);
        categories.add(new LeaderboardCategory("Season", seasons.stream()
                .map(season -> leaderboard(season.id(), "Season " + season.number()))
                .toList()));
        return List.copyOf(categories);
    }

    public static String latestGuildSeasonId(List<String> leaderboardTypes) {
        return leaderboardTypes.stream()
                .map(GUILD_SEASON_ID::matcher)
                .filter(Matcher::matches)
                .map(matcher -> new SeasonLeaderboard(
                        matcher.group(), Integer.parseInt(matcher.group(1))))
                .max(Comparator.comparingInt(SeasonLeaderboard::number))
                .map(SeasonLeaderboard::id)
                .orElse(null);
    }

    private static List<String> fallbackGuildSeasonIds() {
        List<String> ids = new ArrayList<>();
        for (int season = FALLBACK_LATEST_GUILD_SEASON; season >= 0; season--) {
            ids.add("guildSeason" + season);
        }
        return ids;
    }

    private static LeaderboardCategory category(String displayName, LeaderboardDefinition... leaderboards) {
        return new LeaderboardCategory(displayName, List.of(leaderboards));
    }

    private static LeaderboardDefinition leaderboard(String id, String displayName) {
        return new LeaderboardDefinition(id, displayName);
    }

    private static LeaderboardDefinition raid(String idPrefix, String displayName) {
        return new LeaderboardDefinition(null, displayName, List.of(
                leaderboard(idPrefix + "SrPlayers", "Rating"),
                leaderboard(idPrefix + "SrGPlayers", "Guild Raid Only"),
                leaderboard(idPrefix + "Completion", "Completion")
        ));
    }

    private static LeaderboardDefinition totalLevel(String idPrefix, String displayName) {
        return new LeaderboardDefinition(null, displayName, List.of(
                leaderboard(idPrefix + "GlobalLevel", "Global"),
                leaderboard(idPrefix + "SoloLevel", "Solo")
        ));
    }

    private record SeasonLeaderboard(String id, int number) {
    }
}
