package julianh06.wynnextras.features.achievements;

import julianh06.wynnextras.features.raid.WERaidKind;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@WEModule
public class AchievementTracking {
    public static Achievements achievements;
    private boolean init;
    private static final List<UnlockAnnouncement> pendingUnlockAnnouncements = new ArrayList<>();
    private static int unlockAnnouncementFlushTicks = -1;

    /**
     * How far our self-counted total may run ahead of the Wynncraft API before we assume our
     * count is wrong and snap back to the API. The API lags behind real completions, so a small
     * lead is expected and trusted; a large one is treated as drift.
     */
    private static final int API_MISMATCH_TOLERANCE = 5;
    private static final int UNLOCK_ANNOUNCEMENT_DELAY_TICKS = 60;

    /** Combat level cap a class must reach to count toward the {@code class.levelXXX} achievements. */
    private static final int CLASS_LEVEL_120 = 120;
    private static final int CLASS_LEVEL_121 = 121;

    /**
     * Raw content-completion value that equals 100%. Kept in sync with
     * {@code ClassWidget.MAX_CONTENT_COMPLETION}, which the profile viewer uses for the same purpose.
     */
    private static final int CONTENT_COMPLETION_MAX = 1289;

    /** Wynncraft profession keys, as returned (lowercase) by the player API. */
    private static final List<String> GATHERING_PROFESSIONS = List.of("mining", "woodcutting", "farming", "fishing");
    private static final List<String> CRAFTING_PROFESSIONS = List.of(
            "alchemism", "armouring", "cooking", "jeweling", "scribing", "tailoring", "weaponsmithing", "woodworking");

    /** Aspect amount that counts as "maxed", by rarity (matches AspectsPage). */
    private static int maxedThreshold(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase()) {
            case "mythic" -> 15;
            case "fabled" -> 75;
            case "legendary" -> 150;
            default -> Integer.MAX_VALUE;
        };
    }

    /** Ensures the startup API sync only runs once per client launch. */
    private boolean raidCountsSynced;

    /** Ensures the aspect achievement sync only dispatches once the aspect catalogue is loaded. */
    private boolean aspectsSynced;
    private volatile boolean syncingAspects;

    public static void reloadAchievementsFromApi() {
        if (achievements == null) Achievements.load();
        AchievementTracking tracking = new AchievementTracking();
        tracking.syncRaidCountsFromApi();
        tracking.trySyncAspectAchievements();
    }

    @SubscribeEvent
    private void onTick(TickEvent event) {
        tickUnlockAnnouncements();

        if (!init) {
            init = true;
            if (getFromServer() != null) {
                achievements = getFromServer();
            }
        }
        if (achievements == null) return;

        // Once per launch, reconcile our self-counted raid totals (and class/content/profession
        // achievements) against the Wynncraft API.
        if (!raidCountsSynced && MinecraftUtils.player() != null) {
            raidCountsSynced = true;
            syncRaidCountsFromApi();
        }

        // Once the aspect catalogue has finished loading, evaluate the aspect achievements.
        if (!aspectsSynced && MinecraftUtils.player() != null) {
            trySyncAspectAchievements();
        }
    }

    /**
     * Fires whenever a raid is completed. We immediately count it ourselves (the API is too slow
     * to reflect it yet), then re-query the API to reconcile in case runs happened elsewhere.
     */
    @SubscribeEvent
    private void onRaidEnded(RaidEndedEvent event) {
        if (!(event instanceof RaidEndedEvent.Completed)) return;
        if (achievements == null || event.getRaid() == null) return;

        RaidType type = RaidType.fromKind(event.getRaid().raidKind());
        if (type == null) return;

        Integer current = achievements.getCount(type.achievementId);
        int newCount = (current == null ? 0 : current) + 1;
        applyCount(type, newCount, true);
        save();

        syncRaidCountsFromApi();
    }

    private void announce(String message) {
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(message)));
    }

    private void tickUnlockAnnouncements() {
        if (!WynnExtrasConfig.INSTANCE.showAchievementUnlockMessages) {
            pendingUnlockAnnouncements.clear();
            unlockAnnouncementFlushTicks = -1;
            return;
        }
        if (unlockAnnouncementFlushTicks < 0) return;

        unlockAnnouncementFlushTicks--;
        if (unlockAnnouncementFlushTicks <= 0) {
            flushUnlockAnnouncements();
        }
    }

    private void flushUnlockAnnouncements() {
        unlockAnnouncementFlushTicks = -1;
        if (pendingUnlockAnnouncements.isEmpty()) return;

        if (pendingUnlockAnnouncements.size() > 3) {
            sendBundledUnlockAnnouncement();
        } else {
            for (UnlockAnnouncement announcement : pendingUnlockAnnouncements) {
                announce(announcement.message);
            }
        }

        announce("§7Use §e/we achievements §7to view your achievement rewards.");
        pendingUnlockAnnouncements.clear();
    }

    private void announceAchievementUnlock(String message, String tooltipLine) {
        if (!WynnExtrasConfig.INSTANCE.showAchievementUnlockMessages) return;

        pendingUnlockAnnouncements.add(new UnlockAnnouncement(message, tooltipLine));
        unlockAnnouncementFlushTicks = UNLOCK_ANNOUNCEMENT_DELAY_TICKS;
    }

    private void sendBundledUnlockAnnouncement() {
        MutableText tooltip = Text.empty();
        for (int i = 0; i < pendingUnlockAnnouncements.size(); i++) {
            if (i > 0) tooltip.append(Text.literal("\n"));
            tooltip.append(Text.literal("§a- §f" + pendingUnlockAnnouncements.get(i).tooltipLine));
        }

        int amount = pendingUnlockAnnouncements.size();
        MutableText message = Text.literal("You have unlocked " + amount + " achievements! ")
                .append(Text.literal("[Hover here to view them]").setStyle(Style.EMPTY
                        .withColor(Formatting.YELLOW)
                        .withHoverEvent(new HoverEvent.ShowText(tooltip))));
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
    }

    /**
     * Pulls the player's raid completion counts from the Wynncraft API and reconciles them with
     * our locally stored counts. Runs the actual mutation on the client thread since the HTTP
     * response arrives on a background thread.
     */
    private void syncRaidCountsFromApi() {
        if (achievements == null || MinecraftUtils.player() == null) return;

        String username = MinecraftUtils.player().getGameProfile().name();
        if (username == null || username.isEmpty()) return;

        WynncraftApiHandler.fetchPlayerData(username)
                .thenAccept(data -> MinecraftClient.getInstance().execute(() -> reconcileWithApi(data)))
                .exceptionally(ex -> {
                    WynnExtras.LOGGER.error("[WynnExtras] Failed to sync raid achievement counts: " + ex.getMessage());
                    return null;
                });
    }

    private void reconcileWithApi(PlayerData data) {
        if (achievements == null || data == null) return;

        boolean changed = false;
        changed |= reconcileRaids(data);
        changed |= evaluateCharacterAchievements(data);
        if (changed) save();
    }

    /** Reconciles raid completion counts against the API. Returns true if anything changed. */
    private boolean reconcileRaids(PlayerData data) {
        if (data.getGlobalData() == null) return false;

        Raids raids = data.getGlobalData().getRaids();
        if (raids == null || raids.getList() == null) return false; // stats private / unavailable — keep local counts
        Map<String, Integer> list = raids.getList();

        boolean changed = false;
        for (RaidType type : RaidType.values()) {
            int apiCount = type.apiCount(list);
            Integer localBoxed = achievements.getCount(type.achievementId);
            int local = localBoxed == null ? 0 : localBoxed;

            int reconciled = reconcile(local, apiCount);
            if (reconciled != local) {
                applyCount(type, reconciled, reconciled > local);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Evaluates the class-level, content-completion and profession achievements from the per-character
     * data in a single Wynncraft API response. Returns true if any achievement state changed.
     */
    private boolean evaluateCharacterAchievements(PlayerData data) {
        Map<String, CharacterData> characters = data.getCharacters();
        if (characters == null || characters.isEmpty()) return false; // stats private / unavailable

        int classesAt120 = 0;
        int classesAt121 = 0;
        boolean contentComplete = false;

        Map<String, Integer> gatheringLevels = new HashMap<>();
        Map<String, Integer> craftingLevels = new HashMap<>();

        for (CharacterData character : characters.values()) {
            if (character == null) continue;

            int level = character.getLevel();
            if (level >= CLASS_LEVEL_120) classesAt120++;
            if (level >= CLASS_LEVEL_121) classesAt121++;

            if (character.getContentCompletion() >= CONTENT_COMPLETION_MAX) contentComplete = true;

            Map<String, Profession> professions = character.getProfessions();
            if (professions == null) continue;

            for (String prof : GATHERING_PROFESSIONS) {
                gatheringLevels.merge(prof, professionLevel(professions, prof), Math::max);
            }

            for (String prof : CRAFTING_PROFESSIONS) {
                craftingLevels.merge(prof, professionLevel(professions, prof), Math::max);
            }
        }

        boolean changed = false;

        changed |= applyTieredCount("class.level120", classesAt120, Achievements.CLASS_COUNT_TARGETS, "class(es) at Level 120");
        changed |= applyTieredCount("class.level121", classesAt121, Achievements.CLASS_COUNT_TARGETS, "class(es) at Level 121");

        if (contentComplete) changed |= unlockSimple("content.completion", "100% Content Completion");

        changed |= applyTieredCount("prof.gather.100", countProfessionsAtLevel(gatheringLevels, GATHERING_PROFESSIONS, 100), Achievements.GATHERING_PROFESSION_TARGETS, "gathering profession(s) at Level 100");
        changed |= applyTieredCount("prof.gather.115", countProfessionsAtLevel(gatheringLevels, GATHERING_PROFESSIONS, 115), Achievements.GATHERING_PROFESSION_TARGETS, "gathering profession(s) at Level 115");
        changed |= applyTieredCount("prof.gather.132", countProfessionsAtLevel(gatheringLevels, GATHERING_PROFESSIONS, 132), Achievements.GATHERING_PROFESSION_TARGETS, "gathering profession(s) at Level 132");
        changed |= applyTieredCount("prof.craft.100", countProfessionsAtLevel(craftingLevels, CRAFTING_PROFESSIONS, 100), Achievements.CRAFTING_PROFESSION_TARGETS, "crafting profession(s) at Level 100");
        changed |= applyTieredCount("prof.craft.115", countProfessionsAtLevel(craftingLevels, CRAFTING_PROFESSIONS, 115), Achievements.CRAFTING_PROFESSION_TARGETS, "crafting profession(s) at Level 115");
        changed |= applyTieredCount("prof.craft.132", countProfessionsAtLevel(craftingLevels, CRAFTING_PROFESSIONS, 132), Achievements.CRAFTING_PROFESSION_TARGETS, "crafting profession(s) at Level 132");

        return changed;
    }

    private static int countProfessionsAtLevel(Map<String, Integer> professionLevels, List<String> professions, int level) {
        int count = 0;
        for (String profession : professions) {
            if (professionLevels.getOrDefault(profession, 0) >= level) count++;
        }
        return count;
    }

    private static int professionLevel(Map<String, Profession> professions, String key) {
        Profession profession = professions.get(key);
        return profession == null ? 0 : profession.getLevel();
    }

    /**
     * Decides the authoritative count given our local count and the API's count:
     * the API wins when it's ahead (we missed runs) or when our count has drifted implausibly
     * far ahead of it; otherwise we keep our local count (the API is simply lagging).
     */
    private static int reconcile(int local, int apiCount) {
        if (apiCount > local) return apiCount;
        if (local - apiCount > API_MISMATCH_TOLERANCE) return apiCount;
        return local;
    }

    /** Applies an absolute count, optionally announcing any newly reached tiers. */
    private void applyCount(RaidType type, int newCount, boolean announce) {
        Integer beforeTier = achievements.getTier(type.achievementId);
        achievements.setCount(type.achievementId, newCount);
        Integer afterTier = achievements.getTier(type.achievementId);

        if (!announce || beforeTier == null || afterTier == null) return;

        if (afterTier <= beforeTier) return;

        int reachedTier = Math.min(afterTier, Achievements.RAID_TARGETS.size()) - 1;
        int milestone = Achievements.RAID_TARGETS.get(reachedTier);
        announceAchievementUnlock(
                "Achievement: Completed " + milestone + " " + type.displayName + " raids!",
                "Completed " + milestone + " " + type.displayName + " raids");
    }

    /**
     * Dispatches the aspect achievement evaluation once the aspect catalogue has finished loading.
     * Failed player-aspect requests are retried later instead of marking the launch as synced.
     */
    private boolean trySyncAspectAchievements() {
        if (achievements == null || MinecraftUtils.player() == null) return false;
        if (syncingAspects) return false;

        List<ApiAspect> catalogue = WynncraftApiHandler.fetchAllAspects(); // kicks off the load on first call
        if (WynncraftApiHandler.INSTANCE.isFetchingAspects.get()) return false; // still loading — retry later
        if (catalogue == null || catalogue.isEmpty()) return false;

        List<ApiAspect> snapshot = new ArrayList<>(catalogue);
        // Only proceed once aspects for every class are present, otherwise totals would be wrong.
        long classCount = snapshot.stream()
                .map(ApiAspect::getRequiredClass)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .distinct()
                .count();
        if (classCount < 5) return false;

        String uuid = MinecraftUtils.player().getUuidAsString();
        syncingAspects = true;
        WynncraftApiHandler.fetchPlayerAspectData(uuid)
                .thenAccept(result -> MinecraftClient.getInstance().execute(() -> {
                    syncingAspects = false;
                    if (applyAspectAchievements(result, snapshot)) {
                        aspectsSynced = true;
                    }
                }))
                .exceptionally(ex -> {
                    syncingAspects = false;
                    WynnExtras.LOGGER.error("[WynnExtras] Failed to sync aspect achievements: " + ex.getMessage());
                    return null;
                });
        return true;
    }

    private boolean applyAspectAchievements(WynncraftApiHandler.FetchResult result, List<ApiAspect> catalogue) {
        if (achievements == null) return false;
        if (result == null || result.status() != WynncraftApiHandler.FetchStatus.OK || result.user() == null) return false;
        List<Aspect> playerAspects = result.user().getAspects();
        if (playerAspects == null) return false;

        // Highest owned amount per aspect name.
        Map<String, Integer> amounts = new HashMap<>();
        for (Aspect aspect : playerAspects) {
            if (aspect.getName() == null) continue;
            amounts.merge(aspect.getName(), aspect.getAmount(), Math::max);
        }

        int maxedAll = 0, maxedMythic = 0, maxedFabled = 0, maxedLegendary = 0;
        Map<String, Integer> maxedByClass = new HashMap<>();

        for (ApiAspect api : catalogue) {
            String rarity = api.getRarity() == null ? "" : api.getRarity().toLowerCase();
            String clazz = api.getRequiredClass() == null ? "" : api.getRequiredClass().toLowerCase();

            boolean maxed = amounts.getOrDefault(api.getName(), 0) >= maxedThreshold(rarity);
            if (!maxed) continue;

            maxedAll++;
            maxedByClass.merge(clazz, 1, Integer::sum);
            switch (rarity) {
                case "mythic" -> maxedMythic++;
                case "fabled" -> maxedFabled++;
                case "legendary" -> maxedLegendary++;
            }
        }

        boolean changed = false;

        changed |= applyTieredCount("aspect.max.all", maxedAll, Achievements.ALL_ASPECT_TARGETS, "maxed aspect(s)");
        changed |= applyTieredCount("aspect.max.all.mythic", maxedMythic, Achievements.MYTHIC_ASPECT_TARGETS, "maxed Mythic aspect(s)");
        changed |= applyTieredCount("aspect.max.all.fabled", maxedFabled, Achievements.FABLED_ASPECT_TARGETS, "maxed Fabled aspect(s)");
        changed |= applyTieredCount("aspect.max.all.legendary", maxedLegendary, Achievements.LEGENDARY_ASPECT_TARGETS, "maxed Legendary aspect(s)");
        changed |= applyTieredCount("aspect.max.all.warrior", maxedByClass.getOrDefault("warrior", 0), Achievements.WARRIOR_ASPECT_TARGETS, "maxed Warrior aspect(s)");
        changed |= applyTieredCount("aspect.max.all.shaman", maxedByClass.getOrDefault("shaman", 0), Achievements.SHAMAN_ASPECT_TARGETS, "maxed Shaman aspect(s)");
        changed |= applyTieredCount("aspect.max.all.mage", maxedByClass.getOrDefault("mage", 0), Achievements.MAGE_ASPECT_TARGETS, "maxed Mage aspect(s)");
        changed |= applyTieredCount("aspect.max.all.archer", maxedByClass.getOrDefault("archer", 0), Achievements.ARCHER_ASPECT_TARGETS, "maxed Archer aspect(s)");
        changed |= applyTieredCount("aspect.max.all.assassin", maxedByClass.getOrDefault("assassin", 0), Achievements.ASSASSIN_ASPECT_TARGETS, "maxed Assassin aspect(s)");

        if (changed) save();
        return true;
    }

    /** Sets a tiered achievement's absolute count, announcing each newly reached tier. Returns true if changed. */
    private boolean applyTieredCount(String id, int newCount, List<Integer> targets, String label) {
        Integer beforeCount = achievements.getCount(id);
        Integer beforeTier = achievements.getTier(id);
        achievements.setCount(id, newCount);
        Integer afterTier = achievements.getTier(id);

        if (beforeTier != null && afterTier != null && afterTier > beforeTier) {
            // Announce only the highest tier reached this update, to avoid a burst on the first sync.
            int reachedTier = Math.min(afterTier, targets.size()) - 1;
            int milestone = targets.get(reachedTier);
            announceAchievementUnlock("Achievement: " + milestone + " " + label + "!", milestone + " " + label);
        }
        Integer afterCount = achievements.getCount(id);
        return !Objects.equals(beforeCount, afterCount) || !Objects.equals(beforeTier, afterTier);
    }

    /** Unlocks a simple achievement, announcing it if it wasn't already unlocked. Returns true if changed. */
    private boolean unlockSimple(String id, String announceName) {
        if (achievements.isUnlocked(id)) return false;
        if (achievements.setCompleted(id)) {
            announceAchievementUnlock("Achievement Unlocked: " + announceName, announceName);
            return true;
        }
        return false;
    }

    private static class UnlockAnnouncement {
        final String message;
        final String tooltipLine;

        UnlockAnnouncement(String message, String tooltipLine) {
            this.message = message;
            this.tooltipLine = tooltipLine;
        }
    }

    private void save() {
        Achievements.save();
    }

    private Achievements getFromServer() {
        return null;
    }

    private Achievements loadFromClient() {
        return null;
    }

    /**
     * The five raids tracked by completion achievements. Matched against the in-game raid via
     * The display names are identical to the keys returned by Wynncraft's API.
     */
    private enum RaidType {
        TNA("raid.tna", "The Nameless Anomaly"),
        NOTG("raid.notg", "Nest of the Grootslangs"),
        NOL("raid.nol", "Orphion's Nexus of Light"),
        TWP("raid.twp", "The Wartorn Palace"),
        TCC("raid.tcc", "The Canyon Colossus");

        final String achievementId;
        final String displayName;

        RaidType(String achievementId, String displayName) {
            this.achievementId = achievementId;
            this.displayName = displayName;
        }

        int apiCount(Map<String, Integer> list) {
            Integer value = list.get(displayName);
            if (value != null) return value;
            // The API has historically reported The Wartorn Palace under the "unknown" key.
            if (this == TWP) {
                value = list.get("unknown");
                if (value != null) return value;
            }
            return 0;
        }

        static RaidType fromKind(WERaidKind kind) {
            if (kind == null) return null;
            String raidName = kind.displayName();
            for (RaidType type : values()) {
                if (type.displayName.equals(raidName)) return type;
            }
            return null;
        }
    }
}
