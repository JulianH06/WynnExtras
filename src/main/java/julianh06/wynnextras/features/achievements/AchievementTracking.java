package julianh06.wynnextras.features.achievements;

import com.wynntils.core.components.Models;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.RaidEndedEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;

@WEModule
public class AchievementTracking {
    public static Achievements achievements;
    private boolean init;

    /**
     * How far our self-counted total may run ahead of the Wynncraft API before we assume our
     * count is wrong and snap back to the API. The API lags behind real completions, so a small
     * lead is expected and trusted; a large one is treated as drift.
     */
    private static final int API_MISMATCH_TOLERANCE = 5;

    /** Ensures the startup API sync only runs once per client launch. */
    private boolean raidCountsSynced;

    @SubscribeEvent
    private void onTick(TickEvent event) {
        if (!init) {
            init = true;
            if (getFromServer() != null) {
                achievements = getFromServer();
            }
        }
        if (achievements == null) return;

        CappedValue combatLevel = Models.CombatXp.getCombatLevel();
        int currentLevel = combatLevel.current();

        unlockLevelAchievement(currentLevel, 120);
        unlockLevelAchievement(currentLevel, 121);

        // Once per launch, reconcile our self-counted raid totals against the Wynncraft API.
        if (!raidCountsSynced && McUtils.player() != null) {
            raidCountsSynced = true;
            syncRaidCountsFromApi();
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

    private void unlockLevelAchievement(int currentLevel, int requiredLevel) {
        if (currentLevel < requiredLevel) return;

        String id = "simple.level." + requiredLevel;
        if (achievements.isUnlocked(id)) return;

        if (achievements.setCompleted(id)) {
            WynnExtras.addWynnExtrasPrefix(Text.of("Achievement Unlocked: Level " + requiredLevel));
            save();
        }
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
        if (achievements == null || data == null || data.getGlobalData() == null) return;

        Raids raids = data.getGlobalData().getRaids();
        if (raids == null || raids.getList() == null) return; // stats private / unavailable — keep local counts
        Map<String, Integer> list = raids.getList();

        boolean changed = false;
        for (RaidType type : RaidType.values()) {
            int apiCount = type.apiCount(list);
            Integer localBoxed = achievements.getCount(type.achievementId);
            int local = localBoxed == null ? 0 : localBoxed;

            int reconciled = reconcile(local, apiCount);
            if (reconciled != local) {
                // API reconciliation is silent: only real completions in onRaidEnded announce.
                applyCount(type, reconciled, false);
                changed = true;
            }
        }
        if (changed) save();
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

        for (int tier = beforeTier; tier < afterTier && tier < Achievements.RAID_TARGETS.size(); tier++) {
            int milestone = Achievements.RAID_TARGETS.get(tier);
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Achievement: Completed " + milestone + " " + type.displayName + " raids!")));
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
