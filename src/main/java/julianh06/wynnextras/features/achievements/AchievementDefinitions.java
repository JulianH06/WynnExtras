package julianh06.wynnextras.features.achievements;

import java.util.List;

public class AchievementDefinitions {

    public static void registerAll(AchievementManager manager) {
        registerRaidingAchievements(manager);
        registerAspectAchievements(manager);
        registerMiscAchievements(manager);
        registerProfessionAchievements(manager);
        registerWarAchievements(manager);
        registerLootrunAchievements(manager);
    }

    private static void registerRaidingAchievements(AchievementManager manager) {
        // Total raid completions
        manager.registerAchievement(new TieredAchievement(
                "raid_total_completions",
                "Raid Veteran",
                "Complete raids",
                AchievementCategory.RAIDING,
                List.of(100, 250, 500)
        ));

        // TNA completions
        manager.registerAchievement(new TieredAchievement(
                "raid_tna_completions",
                "Anomaly Hunter",
                "Complete The Nameless Anomaly",
                AchievementCategory.RAIDING,
                List.of(100, 250, 500)
        ));

        // NOTG completions
        manager.registerAchievement(new TieredAchievement(
                "raid_notg_completions",
                "Grootslang Slayer",
                "Complete Nest of the Grootslangs",
                AchievementCategory.RAIDING,
                List.of(100, 250, 500)
        ));

        // NOL completions
        manager.registerAchievement(new TieredAchievement(
                "raid_nol_completions",
                "Light Bringer",
                "Complete Orphion's Nexus of Light",
                AchievementCategory.RAIDING,
                List.of(100, 250, 500)
        ));

        // TCC completions
        manager.registerAchievement(new TieredAchievement(
                "raid_tcc_completions",
                "Colossus Conqueror",
                "Complete The Canyon Colossus",
                AchievementCategory.RAIDING,
                List.of(100, 250, 500)
        ));

        // Speed achievements
        manager.registerAchievement(new TieredAchievement(
                "raid_room_speed",
                "Speed Demon",
                "Complete a raid room quickly",
                AchievementCategory.RAIDING,
                List.of(35, 30, 27), // seconds thresholds (lower is better)
                false,
                List.of("Complete a room in under 35 seconds")
        ));

        // No death run
        manager.registerAchievement(new ProgressAchievement(
                "raid_no_deaths",
                "Flawless",
                "Complete a raid with no party deaths",
                AchievementCategory.RAIDING,
                1
        ));

        // Fail in first room (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "raid_first_room_fail",
                "Bad Start",
                "Fail a raid in the first room",
                AchievementCategory.RAIDING,
                1,
                true,
                List.of("Sometimes things don't go as planned...")
        ));

        // Last alive boss kill (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "raid_last_alive",
                "Last One Standing",
                "Defeat a raid boss as the last player alive",
                AchievementCategory.RAIDING,
                1,
                true,
                List.of("Clutch!")
        ));
    }

    private static void registerAspectAchievements(AchievementManager manager) {
        // Max Legendary aspects (tiered: 1, 20, 51)
        manager.registerAchievement(new TieredAchievement(
                "aspect_max_legendary",
                "Legendary Collector",
                "Max Legendary aspects",
                AchievementCategory.ASPECTS,
                List.of(1, 20, 51)
        ));

        // Max Fabled aspects (tiered: 1, 20, 43)
        manager.registerAchievement(new TieredAchievement(
                "aspect_max_fabled",
                "Fabled Collector",
                "Max Fabled aspects",
                AchievementCategory.ASPECTS,
                List.of(1, 20, 43)
        ));

        // Max Mythic aspects (tiered: 1, 5, 15)
        manager.registerAchievement(new TieredAchievement(
                "aspect_max_mythic",
                "Mythic Collector",
                "Max Mythic aspects",
                AchievementCategory.ASPECTS,
                List.of(1, 5, 15)
        ));

        // Max all aspects of a class
        manager.registerAchievement(new TieredAchievement(
                "aspect_max_class",
                "Class Completionist",
                "Max all aspects for classes",
                AchievementCategory.ASPECTS,
                List.of(1, 3, 5)
        ));

        // Max all aspects of a rarity
        manager.registerAchievement(new TieredAchievement(
                "aspect_max_rarity",
                "Rarity Collector",
                "Max all aspects of rarities",
                AchievementCategory.ASPECTS,
                List.of(1, 2, 3) // legendary, fabled, mythic
        ));

        // Max ALL aspects
        manager.registerAchievement(new ProgressAchievement(
                "aspect_max_all",
                "Aspect Master",
                "Max every single aspect",
                AchievementCategory.ASPECTS,
                1
        ));
    }

    private static void registerMiscAchievements(AchievementManager manager) {
        // Bank page achievements
        manager.registerAchievement(new ProgressAchievement(
                "misc_misc_bucket_pages",
                "Bucket Collector",
                "Unlock all misc bucket pages",
                AchievementCategory.MISC,
                1
        ));

        manager.registerAchievement(new ProgressAchievement(
                "misc_tome_shelf_pages",
                "Librarian",
                "Unlock all tome shelf pages",
                AchievementCategory.MISC,
                1
        ));

        manager.registerAchievement(new ProgressAchievement(
                "misc_account_bank_pages",
                "Banker",
                "Unlock all account bank pages",
                AchievementCategory.MISC,
                1
        ));

        // Fill a bank page with STX
        manager.registerAchievement(new ProgressAchievement(
                "misc_stx_page",
                "Stacks on Stacks",
                "Fill a bank page with STX",
                AchievementCategory.MISC,
                1
        ));

        // Level classes to 106
        manager.registerAchievement(new TieredAchievement(
                "misc_classes_106",
                "Multi-Classer",
                "Level classes to 106",
                AchievementCategory.MISC,
                List.of(1, 3, 5)
        ));

        // 100% content completion
        manager.registerAchievement(new ProgressAchievement(
                "misc_100_percent",
                "Completionist",
                "Reach 100% content on a class",
                AchievementCategory.MISC,
                1
        ));

        // Drunk achievement (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "misc_drunk",
                "Party Animal",
                "Get drunk",
                AchievementCategory.MISC,
                1,
                true,
                List.of("You feel... different")
        ));

        // Steam happy easter egg (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "misc_steamhappy",
                "Steam Happy",
                ":steamhappy:",
                AchievementCategory.MISC,
                1,
                true,
                List.of("Say hi to a legend")
        ));
    }

    private static void registerProfessionAchievements(AchievementManager manager) {
        // Gathering professions - single badge, tiers for 100/120/132
        // Tracks highest level reached in ANY gathering prof
        // Bronze: Lv100, Silver: Lv120, Gold: Lv132
        manager.registerAchievement(new TieredAchievement(
                "profession_gathering",
                "Gathering Prof",
                "Reach level 100 in a gathering profession",
                AchievementCategory.PROFESSIONS,
                List.of(100, 120, 132) // Level thresholds
        ));

        // Crafting professions - single badge, tiers for 100/120/132
        // Tracks highest level reached in ANY crafting prof
        // Bronze: Lv100, Silver: Lv120, Gold: Lv132
        manager.registerAchievement(new TieredAchievement(
                "profession_crafting",
                "Crafting Prof",
                "Reach level 100 in a crafting profession",
                AchievementCategory.PROFESSIONS,
                List.of(100, 120, 132) // Level thresholds
        ));

        // Total level 1690
        manager.registerAchievement(new ProgressAchievement(
                "profession_total_1690",
                "Professional",
                "Reach total profession level 1690 on one class",
                AchievementCategory.PROFESSIONS,
                1
        ));
    }

    private static void registerWarAchievements(AchievementManager manager) {
        // War completions
        manager.registerAchievement(new TieredAchievement(
                "war_completions",
                "War Veteran",
                "Complete wars",
                AchievementCategory.WARS,
                List.of(50, 150, 300)
        ));

        // Solo war
        manager.registerAchievement(new ProgressAchievement(
                "war_solo",
                "Lone Wolf",
                "Complete a war solo",
                AchievementCategory.WARS,
                1
        ));

        // VHigh war
        manager.registerAchievement(new ProgressAchievement(
                "war_vhigh",
                "High Roller",
                "Complete a VHigh difficulty war",
                AchievementCategory.WARS,
                1
        ));

        // VLow war
        manager.registerAchievement(new ProgressAchievement(
                "war_vlow",
                "Low Effort",
                "Complete a VLow difficulty war",
                AchievementCategory.WARS,
                1
        ));
    }

    private static void registerLootrunAchievements(AchievementManager manager) {
        // Lootrun pulls
        manager.registerAchievement(new TieredAchievement(
                "lootrun_pulls",
                "Treasure Hunter",
                "Open lootrun chests",
                AchievementCategory.LOOTRUNS,
                List.of(1000, 2500, 5000)
        ));

        // Lootrun mythics
        manager.registerAchievement(new TieredAchievement(
                "lootrun_mythics",
                "Mythic Finder",
                "Find mythics in lootruns",
                AchievementCategory.LOOTRUNS,
                List.of(5, 15, 30)
        ));

        // Back-to-back mythics (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "lootrun_b2b_mythic",
                "Lucky Streak",
                "Find back-to-back mythics",
                AchievementCategory.LOOTRUNS,
                1,
                true,
                List.of("Twice the luck!")
        ));

        // 5k chests dry (hidden)
        manager.registerAchievement(new ProgressAchievement(
                "lootrun_5k_dry",
                "Unlucky",
                "Go 5000 chests without a mythic",
                AchievementCategory.LOOTRUNS,
                1,
                true,
                List.of("The RNG gods are not with you...")
        ));

        // Rainbow beacon
        manager.registerAchievement(new ProgressAchievement(
                "lootrun_rainbow_beacon",
                "Rainbow Chaser",
                "Find a rainbow beacon drop",
                AchievementCategory.LOOTRUNS,
                1
        ));

        // Shiny drop
        manager.registerAchievement(new ProgressAchievement(
                "lootrun_shiny",
                "Shiny Hunter",
                "Find a shiny item",
                AchievementCategory.LOOTRUNS,
                1
        ));
    }

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
