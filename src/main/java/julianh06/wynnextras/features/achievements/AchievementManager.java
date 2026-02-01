package julianh06.wynnextras.features.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.core.components.Models;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.Global;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.utils.MojangAuth;
import net.neoforged.bus.api.SubscribeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@WEModule
public class AchievementManager {
    public static final AchievementManager INSTANCE = new AchievementManager();

    private final Map<String, Achievement> achievements = new LinkedHashMap<>();
    private boolean initialized = false;
    private boolean syncedThisSession = false;

    public AchievementManager() {
        // Empty constructor for @WEModule
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;

        AchievementStorage.load();
        AchievementDefinitions.registerAll(this);

        // Load saved progress into achievements
        for (Achievement achievement : achievements.values()) {
            AchievementStorage.INSTANCE.loadAchievement(achievement);
        }

        System.out.println("[WynnExtras] Loaded " + achievements.size() + " achievements");
    }

    /**
     * Called when player joins a world - sync achievements once per session
     */
    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        if (!initialized) initialize();
        if (syncedThisSession) return;
        if (!WynnExtrasConfig.INSTANCE.achievementsEnabled) return;

        // Wait a bit for Models to be ready, then sync
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000); // Wait 3 seconds for everything to load

                // Check if we're on Wynncraft
                if (!Models.WorldState.onWorld()) {
                    System.out.println("[WynnExtras] Not on Wynncraft, skipping achievement sync");
                    return;
                }

                syncedThisSession = true;
                syncFromWynncraftAPI();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void registerAchievement(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }

    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }

    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }

    public List<Achievement> getAchievementsByCategory(AchievementCategory category) {
        List<Achievement> result = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (achievement.getCategory() == category) {
                result.add(achievement);
            }
        }
        return result;
    }

    public void updateProgress(String achievementId, int amount) {
        if (!WynnExtrasConfig.INSTANCE.achievementsEnabled) return;

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null) return;

        if (achievement instanceof TieredAchievement tiered) {
            TieredAchievement.TierLevel newTier = tiered.progress(amount);
            if (newTier != null) {
                AchievementToast.showTierUp(tiered, newTier);
                saveAndSync();
            }
        } else if (achievement instanceof ProgressAchievement progress) {
            boolean wasUnlocked = progress.isUnlocked();
            progress.progress(amount);
            if (!wasUnlocked && progress.isUnlocked()) {
                AchievementToast.showUnlock(progress);
                saveAndSync();
            }
        }
    }

    public void unlockAchievement(String achievementId) {
        if (!WynnExtrasConfig.INSTANCE.achievementsEnabled) return;

        Achievement achievement = achievements.get(achievementId);
        if (achievement == null || achievement.isUnlocked()) return;

        achievement.unlock();
        AchievementToast.showUnlock(achievement);
        saveAndSync();
    }

    private static final String ACHIEVEMENTS_API_URL = "http://wynnextras.com/achievements";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson GSON = new GsonBuilder().create();

    private void saveAndSync() {
        // Save all achievements to storage
        for (Achievement achievement : achievements.values()) {
            AchievementStorage.INSTANCE.saveAchievement(achievement);
        }
        AchievementStorage.save();

        // Sync to WynnExtras server
        syncToServer();
    }

    /**
     * Sync achievements to WynnExtras server (like aspects)
     */
    private void syncToServer() {
        MojangAuth.getAuthData().thenAccept(authData -> {
            if (authData == null) {
                System.err.println("[WynnExtras] Failed to get auth data for achievement sync");
                return;
            }

            try {
                // Build achievement data payload
                JsonObject payload = new JsonObject();
                JsonArray achievementsArray = new JsonArray();

                for (Achievement achievement : achievements.values()) {
                    JsonObject achObj = new JsonObject();
                    achObj.addProperty("id", achievement.getId());
                    achObj.addProperty("unlocked", achievement.isUnlocked());

                    if (achievement instanceof TieredAchievement tiered) {
                        achObj.addProperty("current", tiered.getCurrent());
                        achObj.addProperty("tier", tiered.getHighestUnlockedTier().name());
                    } else if (achievement instanceof ProgressAchievement progress) {
                        achObj.addProperty("current", progress.getCurrent());
                    }

                    achievementsArray.add(achObj);
                }

                payload.add("achievements", achievementsArray);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ACHIEVEMENTS_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Username", authData.username)
                        .header("Server-ID", authData.serverId)
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                        .timeout(Duration.ofSeconds(8))
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            int code = response.statusCode();
                            if (code == 200) {
                                System.out.println("[WynnExtras] Achievements synced to server");
                            } else if (code == 404) {
                                // API endpoint doesn't exist yet - that's fine
                                System.out.println("[WynnExtras] Achievements API not available yet");
                            } else {
                                System.err.println("[WynnExtras] Achievement sync failed: " + code);
                            }
                        })
                        .exceptionally(ex -> {
                            System.err.println("[WynnExtras] Achievement sync error: " + ex.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("[WynnExtras] Error syncing achievements: " + e.getMessage());
            }
        });
    }

    // ==================== EVENT HANDLERS ====================

    @SubscribeEvent
    public void onRaidCompleted(RaidEndedEvent.Completed event) {
        if (!initialized) initialize();

        RaidInfo raid = event.getRaid();
        if (raid == null) return;

        String raidName = raid.getRaidKind().getRaidName();

        // Update raid completion achievements
        switch (raidName) {
            case "The Nameless Anomaly":
                updateProgress("raid_tna_completions", 1);
                break;
            case "Nest of the Grootslangs":
                updateProgress("raid_notg_completions", 1);
                break;
            case "Orphion's Nexus of Light":
                updateProgress("raid_nol_completions", 1);
                break;
            case "The Canyon Colossus":
                updateProgress("raid_tcc_completions", 1);
                break;
        }

        // Update total raid completions
        updateProgress("raid_total_completions", 1);
    }

    @SubscribeEvent
    public void onRaidFailed(RaidEndedEvent.Failed event) {
        if (!initialized) initialize();

        // Check for "fail in first room" achievement
        // This would need additional tracking to determine if it was the first room
    }

    @SubscribeEvent
    public void onChatMessage(ChatEvent event) {
        if (!initialized) initialize();

        String message = event.message.getString();

        // Drunk achievement
        if (message.contains("You feel drunk")) {
            unlockAchievement("misc_drunk");
        }

        // Easter egg achievement
        if (message.contains(":steamhappy:") && message.toLowerCase().contains("legendaryvirus")) {
            unlockAchievement("misc_steamhappy");
        }
    }

    // ==================== PUBLIC API FOR OTHER FEATURES ====================

    /**
     * Called during aspect upload to set the TOTAL count of maxed aspects.
     * This replaces the old count, it doesn't increment.
     */
    public void setMaxedAspectCounts(int legendary, int fabled, int mythic) {
        if (!initialized) initialize();

        System.out.println("[WynnExtras] Setting maxed aspect counts - Legendary: " + legendary + ", Fabled: " + fabled + ", Mythic: " + mythic);

        // Set legendary count
        Achievement legAch = achievements.get("aspect_max_legendary");
        if (legAch instanceof TieredAchievement tiered) {
            int current = tiered.getCurrent();
            if (legendary > current) {
                TieredAchievement.TierLevel newTier = tiered.progress(legendary - current);
                if (newTier != null) {
                    AchievementToast.showTierUp(tiered, newTier);
                }
            }
        }

        // Set fabled count
        Achievement fabAch = achievements.get("aspect_max_fabled");
        if (fabAch instanceof TieredAchievement tiered) {
            int current = tiered.getCurrent();
            if (fabled > current) {
                TieredAchievement.TierLevel newTier = tiered.progress(fabled - current);
                if (newTier != null) {
                    AchievementToast.showTierUp(tiered, newTier);
                }
            }
        }

        // Set mythic count
        Achievement mythAch = achievements.get("aspect_max_mythic");
        if (mythAch instanceof TieredAchievement tiered) {
            int current = tiered.getCurrent();
            if (mythic > current) {
                TieredAchievement.TierLevel newTier = tiered.progress(mythic - current);
                if (newTier != null) {
                    AchievementToast.showTierUp(tiered, newTier);
                }
            }
        }

        saveAndSync();
    }

    /**
     * @deprecated Use setMaxedAspectCounts instead for batch updates
     */
    @Deprecated
    public void onAspectMaxed(String aspectName, String rarity) {
        // Keep for backwards compatibility but don't do anything
        // Counting is now done in batch via setMaxedAspectCounts
    }

    public void onProfessionLevelUp(String profession, int level) {
        if (!initialized) initialize();

        String profLower = profession.toLowerCase();
        boolean isGathering = GATHERING_PROFS.contains(profLower);
        boolean isCrafting = CRAFTING_PROFS.contains(profLower);

        if (!isGathering && !isCrafting) return;

        // Update the profession achievement with the new level
        // The API sync will correct if needed
        if (isGathering) {
            updateProfessionLevelAchievement("profession_gathering", level);
        } else {
            updateProfessionLevelAchievement("profession_crafting", level);
        }
        saveAndSync();
    }

    public void onWarCompleted() {
        if (!initialized) initialize();
        updateProgress("war_completions", 1);
    }

    public void onLootrunChestOpened() {
        if (!initialized) initialize();
        updateProgress("lootrun_pulls", 1);
    }

    public void onLootrunMythicFound() {
        if (!initialized) initialize();
        updateProgress("lootrun_mythics", 1);
    }

    public void onClassLeveledTo106(int classCount) {
        if (!initialized) initialize();
        // classCount is the total number of classes at 106
        Achievement achievement = achievements.get("misc_classes_106");
        if (achievement instanceof TieredAchievement tiered) {
            int current = tiered.getCurrent();
            if (classCount > current) {
                updateProgress("misc_classes_106", classCount - current);
            }
        }
    }

    // ==================== WYNNCRAFT API SYNC ====================

    /**
     * Sync achievements from Wynncraft API.
     * Fetches player data and updates achievements based on:
     * - Profession levels (100, 132)
     * - Raid completions (total and per-raid)
     * - War completions
     * - Content completion (100%)
     * - Classes at level 106
     */
    public void syncFromWynncraftAPI() {
        if (!initialized) initialize();
        if (!WynnExtrasConfig.INSTANCE.achievementsEnabled) return;

        String playerName = McUtils.playerName();
        if (playerName == null || playerName.isEmpty()) {
            System.err.println("[WynnExtras] Cannot sync achievements - player name unknown");
            return;
        }

        System.out.println("[WynnExtras] Syncing achievements from Wynncraft API for " + playerName);

        // Sync from Wynncraft API (raids, profs, etc.)
        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(playerData -> {
            if (playerData == null) {
                System.err.println("[WynnExtras] Failed to fetch player data for achievements sync");
                return;
            }

            processPlayerDataForAchievements(playerData);
        }).exceptionally(ex -> {
            System.err.println("[WynnExtras] Error syncing achievements from Wynncraft API: " + ex.getMessage());
            return null;
        });

        // Sync aspects from WynnExtras API
        syncAspectsFromWynnExtrasAPI();
    }

    /**
     * Fetch aspect data from WynnExtras API and count maxed aspects
     */
    private void syncAspectsFromWynnExtrasAPI() {
        if (McUtils.player() == null) return;

        String playerUuid = McUtils.player().getUuidAsString().replace("-", "");
        System.out.println("[WynnExtras] Fetching aspects from WynnExtras API for UUID: " + playerUuid);

        // Fetch own aspects (passing same UUID for both params since we're fetching our own)
        WynncraftApiHandler.fetchPlayerAspectData(playerUuid, playerUuid).thenAccept(result -> {
            if (result.status() != WynncraftApiHandler.FetchStatus.OK || result.user() == null) {
                System.out.println("[WynnExtras] Could not fetch aspects: " + result.status());
                return;
            }

            // Count maxed aspects by rarity
            // Max amounts: Mythic=15, Fabled=75, Legendary=150
            int maxedLegendary = 0;
            int maxedFabled = 0;
            int maxedMythic = 0;

            for (julianh06.wynnextras.features.profileviewer.data.Aspect aspect : result.user().getAspects()) {
                int amount = aspect.getAmount();
                String rarity = aspect.getRarity();

                if (rarity == null) continue;

                boolean isMaxed = switch (rarity.toLowerCase()) {
                    case "mythic" -> amount >= 15;
                    case "fabled" -> amount >= 75;
                    case "legendary" -> amount >= 150;
                    default -> false;
                };

                if (isMaxed) {
                    switch (rarity.toLowerCase()) {
                        case "legendary" -> maxedLegendary++;
                        case "fabled" -> maxedFabled++;
                        case "mythic" -> maxedMythic++;
                    }
                }
            }

            System.out.println("[WynnExtras] Maxed aspects from API - Legendary: " + maxedLegendary +
                    ", Fabled: " + maxedFabled + ", Mythic: " + maxedMythic);

            // Update achievements with these counts
            setMaxedAspectCounts(maxedLegendary, maxedFabled, maxedMythic);
        }).exceptionally(ex -> {
            System.err.println("[WynnExtras] Error fetching aspects: " + ex.getMessage());
            return null;
        });
    }

    // Gathering professions
    private static final Set<String> GATHERING_PROFS = Set.of("mining", "woodcutting", "farming", "fishing");
    // Crafting professions
    private static final Set<String> CRAFTING_PROFS = Set.of("weaponsmithing", "armouring", "tailoring", "jeweling", "scribing", "cooking", "alchemism", "woodworking");

    /**
     * Process player data from Wynncraft API and update achievements
     */
    private void processPlayerDataForAchievements(PlayerData playerData) {
        System.out.println("[WynnExtras] processPlayerDataForAchievements called");

        if (playerData == null) {
            System.err.println("[WynnExtras] PlayerData is null!");
            return;
        }

        if (playerData.getCharacters() == null) {
            System.err.println("[WynnExtras] Characters map is null!");
            return;
        }

        System.out.println("[WynnExtras] Found " + playerData.getCharacters().size() + " characters");

        int classesAt106 = 0;
        boolean has100Percent = false;

        // Track best profession levels across ALL characters
        Map<String, Integer> bestProfessionLevels = new HashMap<>();

        for (CharacterData character : playerData.getCharacters().values()) {
            if (character == null) continue;

            // Count classes at level 106
            if (character.getLevel() >= 106) {
                classesAt106++;
            }

            // Check for 100% content completion
            if (character.getContentCompletion() >= 100) {
                has100Percent = true;
            }

            // Track best profession levels across all characters
            Map<String, Profession> professions = character.getProfessions();
            if (professions != null) {
                for (Map.Entry<String, Profession> entry : professions.entrySet()) {
                    String profName = entry.getKey().toLowerCase();
                    int level = entry.getValue().getLevel();
                    bestProfessionLevels.merge(profName, level, Math::max);
                }
            }
        }

        // Get global data for raids and wars
        Global globalData = playerData.getGlobalData();
        int totalRaids = 0;
        int totalWars = 0;
        Map<String, Integer> raidCounts = new HashMap<>();

        System.out.println("[WynnExtras] GlobalData is " + (globalData != null ? "present" : "NULL"));

        if (globalData != null) {
            totalWars = globalData.getWars();
            System.out.println("[WynnExtras] Wars from global data: " + totalWars);

            Raids globalRaids = globalData.getRaids();
            System.out.println("[WynnExtras] Raids object is " + (globalRaids != null ? "present" : "NULL"));

            if (globalRaids != null) {
                totalRaids = globalRaids.getTotal();
                System.out.println("[WynnExtras] Total raids: " + totalRaids);

                Map<String, Integer> raidList = globalRaids.getList();
                System.out.println("[WynnExtras] Raid list is " + (raidList != null ? "present with " + raidList.size() + " entries" : "NULL"));

                if (raidList != null) {
                    raidCounts.putAll(raidList);
                    System.out.println("[WynnExtras] Raid list contents: " + raidList);
                }
            }
        } else {
            System.err.println("[WynnExtras] GlobalData is null - player may have stats private");
        }

        // Update achievements based on collected data
        System.out.println("[WynnExtras] Processing achievements - Classes at 106: " + classesAt106 +
                ", Total raids: " + totalRaids + ", Total wars: " + totalWars);
        System.out.println("[WynnExtras] Raid counts: " + raidCounts);

        // Classes at 106
        if (classesAt106 > 0) {
            onClassLeveledTo106(classesAt106);
        }

        // 100% content completion
        if (has100Percent) {
            unlockAchievement("misc_100_percent");
        }

        // Total raid completions
        Achievement totalRaidAch = achievements.get("raid_total_completions");
        if (totalRaidAch instanceof TieredAchievement tiered && totalRaids > tiered.getCurrent()) {
            int delta = totalRaids - tiered.getCurrent();
            if (delta > 0) updateProgress("raid_total_completions", delta);
        }

        // Per-raid achievements
        updateRaidAchievement("The Nameless Anomaly", "raid_tna_completions", raidCounts);
        updateRaidAchievement("Nest of the Grootslangs", "raid_notg_completions", raidCounts);
        updateRaidAchievement("Orphion's Nexus of Light", "raid_nol_completions", raidCounts);
        updateRaidAchievement("The Canyon Colossus", "raid_tcc_completions", raidCounts);

        // War completions
        Achievement warAch = achievements.get("war_completions");
        if (warAch instanceof TieredAchievement tiered && totalWars > tiered.getCurrent()) {
            int delta = totalWars - tiered.getCurrent();
            if (delta > 0) updateProgress("war_completions", delta);
        }

        // Profession achievements - find highest level in each category
        int highestGathering = 0;
        int highestCrafting = 0;

        for (Map.Entry<String, Integer> entry : bestProfessionLevels.entrySet()) {
            String profName = entry.getKey();
            int level = entry.getValue();

            if (GATHERING_PROFS.contains(profName)) {
                highestGathering = Math.max(highestGathering, level);
            } else if (CRAFTING_PROFS.contains(profName)) {
                highestCrafting = Math.max(highestCrafting, level);
            }
        }

        System.out.println("[WynnExtras] Profession levels - Highest Gathering: " + highestGathering +
                ", Highest Crafting: " + highestCrafting);

        // Update gathering profession achievement (tiers at 100/120/132)
        updateProfessionLevelAchievement("profession_gathering", highestGathering);

        // Update crafting profession achievement (tiers at 100/120/132)
        updateProfessionLevelAchievement("profession_crafting", highestCrafting);

        // Check for total profession level 1690
        int totalProfLevel = bestProfessionLevels.values().stream().mapToInt(Integer::intValue).sum();
        if (totalProfLevel >= 1690) {
            unlockAchievement("profession_total_1690");
        }

        saveAndSync();
        System.out.println("[WynnExtras] Achievements synced from Wynncraft API");
    }

    private void updateProfessionLevelAchievement(String achievementId, int highestLevel) {
        if (highestLevel <= 0) return;

        Achievement ach = achievements.get(achievementId);
        if (ach instanceof TieredAchievement tiered) {
            // The tiers are level thresholds (100, 120, 132)
            // Set the current value to the highest level reached
            int current = tiered.getCurrent();
            if (highestLevel > current) {
                int delta = highestLevel - current;
                TieredAchievement.TierLevel newTier = tiered.progress(delta);
                if (newTier != null) {
                    AchievementToast.showTierUp(tiered, newTier);
                }
            }
        }
    }

    private void updateRaidAchievement(String raidName, String achievementId, Map<String, Integer> raidCounts) {
        Integer count = raidCounts.get(raidName);
        if (count == null || count <= 0) return;

        Achievement ach = achievements.get(achievementId);
        if (ach instanceof TieredAchievement tiered && count > tiered.getCurrent()) {
            int delta = count - tiered.getCurrent();
            if (delta > 0) updateProgress(achievementId, delta);
        }
    }
}
