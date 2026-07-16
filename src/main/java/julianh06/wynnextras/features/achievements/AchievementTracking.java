package julianh06.wynnextras.features.achievements;

import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.qol.AttackTimer;
import julianh06.wynnextras.utils.BossBarUtils;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.screen.slot.Slot;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int PROFESSION_LEVEL_MAX = 132;
    private static final long RICH_BANK_SCAN_INTERVAL_MS = 10_000L;
    private static final long WAR_API_REFRESH_INTERVAL_MS = 60_000L;
    private static final long WAR_RESULT_GRACE_MS = 15_000L;
    private static final Pattern TERRITORY_CAPTURED_PATTERN = Pattern.compile(
            "Territory Captured.*?- Captured \\\"(?<territory>[^\\\"]+)\\\"", Pattern.DOTALL);

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
    private long nextRichBankScanAt;
    private long nextWarApiSyncAt;
    private boolean warBossBarActive;
    private String awaitingWarResultTerritory;
    private long awaitingWarResultUntil;
    private String lastWarDefenseDebugState;

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

        checkRichBankAchievement();
        trackWarAchievements();

        // Once per launch, reconcile raid, war, class, content and profession achievements against the API.
        if (!raidCountsSynced && McUtils.player() != null) {
            raidCountsSynced = true;
            nextWarApiSyncAt = System.currentTimeMillis() + WAR_API_REFRESH_INTERVAL_MS;
            syncRaidCountsFromApi();
        } else {
            syncWarCountFromApiIfDue();
        }

        // Once the aspect catalogue has finished loading, evaluate the aspect achievements.
        if (!aspectsSynced && McUtils.player() != null) {
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

        RaidType type = RaidType.fromKind(event.getRaid().getRaidKind());
        if (type == null) return;

        Integer current = achievements.getCount(type.achievementId);
        int newCount = (current == null ? 0 : current) + 1;
        applyCount(type, newCount, true);
        save();

        syncRaidCountsFromApi();
    }

    /** Checks the currently open bank page at most once every ten seconds. */
    private void checkRichBankAchievement() {
        if (achievements.isUnlocked("bank.rich")) return;

        long now = System.currentTimeMillis();
        if (now < nextRichBankScanAt) return;
        nextRichBankScanAt = now + RICH_BANK_SCAN_INTERVAL_MS;

        if (BankOverlay.currentOverlayType != BankOverlayType.ACCOUNT
                && BankOverlay.currentOverlayType != BankOverlayType.CHARACTER) return;
        if (BankOverlay.activeInvSlots.size() < 45) return;

        for (int i = 0; i < 45; i++) {
            Slot slot = BankOverlay.activeInvSlots.get(i);
            if (slot == null || slot.getStack().getItem() != EmeraldUnits.LIQUID_EMERALD.getItemType()
                    || slot.getStack().getCount() != 64) {
                return;
            }
        }

        if (unlockSimple("bank.rich", "Rich")) save();
    }

    /** Records the active tower; the achievement is only granted after the capture message. */
    private void trackWarAchievements() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        for (ClientBossBar bar : BossBarUtils.getBossBars(client.inGameHud.getBossBarHud())) {
            String name = bar.getName().getString();
            if (name == null || !name.replaceAll("§[0-9a-fk-or]", "").contains("Tower")) continue;
            String territoryName = getWarTerritoryName(name);

            if (!warBossBarActive) {
                warBossBarActive = true;
                WynnExtras.LOGGER.info("[WE Achievement Debug] Tower boss bar detected: '{}', territory='{}'", name, territoryName);
            }
            awaitingWarResultTerritory = territoryName;
            awaitingWarResultUntil = Long.MAX_VALUE;
            return;
        }

        if (warBossBarActive) {
            WynnExtras.LOGGER.info("[WE Achievement Debug] Tower boss bar disappeared");
            awaitingWarResultUntil = System.currentTimeMillis() + WAR_RESULT_GRACE_MS;
        } else if (awaitingWarResultTerritory != null && System.currentTimeMillis() > awaitingWarResultUntil) {
            awaitingWarResultTerritory = null;
            awaitingWarResultUntil = 0;
        }
        warBossBarActive = false;
        lastWarDefenseDebugState = null;
    }

    @SubscribeEvent
    private void onWarCaptured(ChatEvent event) {
        if (achievements == null || awaitingWarResultTerritory == null) return;

        Matcher matcher = TERRITORY_CAPTURED_PATTERN.matcher(event.message.getString());
        if (!matcher.find()) return;
        String territoryName = matcher.group("territory");
        if (!awaitingWarResultTerritory.equalsIgnoreCase(territoryName)
                || System.currentTimeMillis() > awaitingWarResultUntil) return;

        String queuedDefense = AttackTimer.getQueuedAttackDefense(territoryName);
        WarDefense defense = WarDefense.fromName(queuedDefense);
        if (defense == null) {
            logWarDefenseDebug("no-queued-defense:" + territoryName,
                    "No defence snapshot from the queue for '" + territoryName + "'; not unlocking a war defence achievement");
        } else {
            AttackTimer.markQueuedAttackDefenseUsed(territoryName);
            boolean alreadyUnlocked = achievements.isUnlocked(defense.achievementId);
            boolean changed = unlockSimple(defense.achievementId, defense.displayName + " Defense tower");
            logWarDefenseDebug("defense:" + queuedDefense,
                    "Captured tower '" + territoryName + "' had queued defence '" + queuedDefense + "'");
            WynnExtras.LOGGER.info("[WE Achievement Debug] War defence '{}' mapped to '{}'; alreadyUnlocked={}, changed={}",
                    defense.displayName, defense.achievementId, alreadyUnlocked, changed);
            if (changed) save();
        }

        awaitingWarResultTerritory = null;
        awaitingWarResultUntil = 0;
    }

    private void syncWarCountFromApiIfDue() {
        if (McUtils.player() == null) return;
        long now = System.currentTimeMillis();
        if (now < nextWarApiSyncAt) return;

        nextWarApiSyncAt = now + WAR_API_REFRESH_INTERVAL_MS;
        syncRaidCountsFromApi();
    }

    private String getWarTerritoryName(String bossBarName) {
        String name = bossBarName.replaceAll("§[0-9a-fk-or]", "");
        int towerIndex = name.indexOf(" Tower");
        if (towerIndex == -1) return null;

        name = name.substring(0, towerIndex).replaceFirst("^\\[[^]]+]\\s*", "").trim();
        return name.isEmpty() ? null : name;
    }

    private void logWarDefenseDebug(String state, String message) {
        if (state.equals(lastWarDefenseDebugState)) return;
        lastWarDefenseDebugState = state;
        WynnExtras.LOGGER.info("[WE Achievement Debug] {}", message);
    }

    private void announce(String message) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(message)));
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
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
    }

    /**
     * Pulls the player's raid completion counts from the Wynncraft API and reconciles them with
     * our locally stored counts. Runs the actual mutation on the client thread since the HTTP
     * response arrives on a background thread.
     */
    private void syncRaidCountsFromApi() {
        if (achievements == null || McUtils.player() == null) return;

        String username = McUtils.player().getGameProfile().name();
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
        changed |= reconcileWars(data);
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

    private boolean reconcileWars(PlayerData data) {
        if (data.getGlobalData() == null) return false;
        return applyTieredCount("war.completion", Math.max(0, data.getGlobalData().getWars()),
                Achievements.WAR_TARGETS, "wars completed");
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
        boolean ultimateCompletionist = false;
        boolean maxLevel = false;

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

            if (character.getContentCompletion() >= CONTENT_COMPLETION_MAX
                    && allProfessionsAtLevel(professions, PROFESSION_LEVEL_MAX)) {
                ultimateCompletionist = true;
            }
            if (character.getLevel() >= CLASS_LEVEL_121
                    && allProfessionsAtLevel(professions, PROFESSION_LEVEL_MAX)) {
                maxLevel = true;
            }

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
        if (ultimateCompletionist) changed |= unlockSimple("content.ultimate_completionist", "Ultimate Completionist");
        if (maxLevel) changed |= unlockSimple("class.max_level", "Max Level");

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

    private static boolean allProfessionsAtLevel(Map<String, Profession> professions, int level) {
        for (String profession : GATHERING_PROFESSIONS) {
            if (professionLevel(professions, profession) < level) return false;
        }
        for (String profession : CRAFTING_PROFESSIONS) {
            if (professionLevel(professions, profession) < level) return false;
        }
        return true;
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
        if (achievements == null || McUtils.player() == null) return false;
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

        String uuid = McUtils.player().getUuidAsString();
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

    private enum WarDefense {
        VERY_LOW("war.defence.very_low", "Very Low"),
        LOW("war.defence.low", "Low"),
        MEDIUM("war.defence.medium", "Medium"),
        HIGH("war.defence.high", "High"),
        VERY_HIGH("war.defence.very_high", "Very High");

        final String achievementId;
        final String displayName;

        WarDefense(String achievementId, String displayName) {
            this.achievementId = achievementId;
            this.displayName = displayName;
        }

        static WarDefense fromName(String name) {
            if (name == null) return null;
            for (WarDefense defense : values()) {
                if (defense.displayName.equalsIgnoreCase(name)) return defense;
            }
            return null;
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
     * {@link RaidKind#getRaidName()}, which is identical to the key Wynncraft's API uses in the
     * raid completion map.
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

        static RaidType fromKind(RaidKind kind) {
            if (kind == null) return null;
            String raidName = kind.getRaidName();
            for (RaidType type : values()) {
                if (type.displayName.equals(raidName)) return type;
            }
            return null;
        }
    }
}
