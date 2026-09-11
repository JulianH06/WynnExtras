package julianh06.wynnextras.features.leaderboardviewer;

import java.util.List;

public final class LeaderboardCatalog {
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
                    leaderboard("grootslangCompletion", "Nest of the Grootslangs Completion"),
                    leaderboard("grootslangSrPlayers", "Nest of the Grootslangs SR"),
                    leaderboard("grootslangSrGPlayers", "Nest of the Grootslangs Guild SR"),
                    leaderboard("orphionCompletion", "Orphion's Nexus of Light Completion"),
                    leaderboard("orphionSrPlayers", "Orphion's Nexus of Light SR"),
                    leaderboard("orphionSrGPlayers", "Orphion's Nexus of Light Guild SR"),
                    leaderboard("colossusCompletion", "The Canyon Colossus Completion"),
                    leaderboard("colossusSrPlayers", "The Canyon Colossus SR"),
                    leaderboard("colossusSrGPlayers", "The Canyon Colossus Guild SR"),
                    leaderboard("namelessCompletion", "The Nameless Anomaly Completion"),
                    leaderboard("namelessSrPlayers", "The Nameless Anomaly SR"),
                    leaderboard("namelessSrGPlayers", "The Nameless Anomaly Guild SR"),
                    leaderboard("frumaCompletion", "The Wartorn Palace Completion"),
                    leaderboard("frumaSrPlayers", "The Wartorn Palace SR"),
                    leaderboard("frumaSrGPlayers", "The Wartorn Palace Guild SR")),
            category("Total Level",
                    leaderboard("professionsGlobalLevel", "Profession Level"),
                    leaderboard("combatGlobalLevel", "Combat Level"),
                    leaderboard("totalGlobalLevel", "Total Level"))
    );

    private static final List<LeaderboardCategory> GUILD_CATEGORIES = List.of(
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
                    leaderboard("frumaSrGuilds", "The Wartorn Palace")),
            category("Season", leaderboard("guildSeason32", "Season 32"))
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
            case Guild -> GUILD_CATEGORIES;
            case Player -> PLAYER_CATEGORIES;
            case Gamemode -> GAMEMODE_CATEGORIES;
        };
    }

    private static LeaderboardCategory category(String displayName, LeaderboardDefinition... leaderboards) {
        return new LeaderboardCategory(displayName, List.of(leaderboards));
    }

    private static LeaderboardDefinition leaderboard(String id, String displayName) {
        return new LeaderboardDefinition(id, displayName);
    }
}
