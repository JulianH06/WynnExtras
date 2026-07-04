package julianh06.wynnextras.features.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
import julianh06.wynnextras.utils.ApiRequestHelper;
import julianh06.wynnextras.utils.InstantTypeAdapter;
import julianh06.wynnextras.utils.MojangAuth;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Achievements {
    private final ArrayList<ProgressAchievement> progressAchievements = new ArrayList<>();
    private final ArrayList<Achievement> achievements = new ArrayList<>();
    private final ArrayList<TieredAchievement> tieredAchievements = new ArrayList<>();

    private transient final Map<String, Achievement> byId = new HashMap<>();

    public void populateAll() {
        registerDefaultAchievements(false);
    }

    private Achievement simple(String id, String title, String description, boolean secret) {
        Achievement achievement = new Achievement();
        achievement.id = id;
        achievement.title = title;
        achievement.description = description;
        achievement.secret = secret;
        return achievement;
    }

    private ProgressAchievement progress(String id, String title, String description, boolean secret, int target) {
        ProgressAchievement achievement = new ProgressAchievement();
        achievement.id = id;
        achievement.title = title;
        achievement.description = description;
        achievement.secret = secret;
        achievement.current = 0;
        achievement.target = target;
        return achievement;
    }

    private TieredAchievement tiered(String id, String title, String description, boolean secret, List<Integer> levelTargets) {
        TieredAchievement achievement = new TieredAchievement();
        achievement.id = id;
        achievement.title = title;
        achievement.description = description;
        achievement.secret = secret;
        achievement.current = 0;
        achievement.currentLevel = 0;
        achievement.levelTargets = List.copyOf(levelTargets);
        return achievement;
    }

    /** Tier targets shared by every per-raid completion achievement. */
    public static final List<Integer> RAID_TARGETS = List.of(5, 25, 100, 250, 1000);

    /** Tier targets for "get N classes to a level cap" achievements. */
    public static final List<Integer> CLASS_COUNT_TARGETS = List.of(1, 2, 3, 4, 5);

    public static final List<Integer> ALL_ASPECT_TARGETS = List.of(1, 10, 25, 50, 100, 128);
    public static final List<Integer> MYTHIC_ASPECT_TARGETS = List.of(1, 5, 10, 15, 20);
    public static final List<Integer> FABLED_ASPECT_TARGETS = List.of(1, 10, 25, 49);
    public static final List<Integer> LEGENDARY_ASPECT_TARGETS = List.of(1, 10, 25, 59);
    public static final List<Integer> WARRIOR_ASPECT_TARGETS = List.of(1, 5, 10, 20, 27);
    public static final List<Integer> SHAMAN_ASPECT_TARGETS = List.of(1, 5, 10, 20, 25);
    public static final List<Integer> MAGE_ASPECT_TARGETS = List.of(1, 5, 10, 20, 25);
    public static final List<Integer> ARCHER_ASPECT_TARGETS = List.of(1, 5, 10, 20, 25);
    public static final List<Integer> ASSASSIN_ASPECT_TARGETS = List.of(1, 5, 10, 20, 26);

    /** Tier targets for "get N gathering professions to a level milestone" achievements. */
    public static final List<Integer> GATHERING_PROFESSION_TARGETS = List.of(1, 2, 3, 4);

    /** Tier targets for "get N crafting professions to a level milestone" achievements. */
    public static final List<Integer> CRAFTING_PROFESSION_TARGETS = List.of(1, 3, 5, 8);

    private static final Set<String> KNOWN_ACHIEVEMENT_IDS = knownAchievementIds();

    private boolean registerDefaultAchievements(boolean onlyMissing) {
        boolean changed = false;
        changed |= registerDefault(tiered("raid.tna",  "The Nameless Anomaly",     "Complete The Nameless Anomaly",      false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.notg", "Nest of the Grootslangs",  "Complete Nest of the Grootslangs",   false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.nol",  "Orphion's Nexus of Light", "Complete Orphion's Nexus of Light",  false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.twp",  "The Wartorn Palace",       "Complete The Wartorn Palace",        false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.tcc",  "The Canyon Colossus",      "Complete The Canyon Colossus",       false, RAID_TARGETS), onlyMissing);

        changed |= registerDefault(tiered("class.level120", "Level 120 Classes", "Get classes to Combat Level 120", false, CLASS_COUNT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("class.level121", "Level 121 Classes", "Get classes to Combat Level 121", false, CLASS_COUNT_TARGETS), onlyMissing);

        changed |= registerDefault(simple("content.completion", "Completionist", "Reach 100% content completion on a class", false), onlyMissing);

        changed |= registerDefault(tiered("aspect.max.all", "Aspect Completionist", "Max aspects", false, ALL_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.mythic", "Mythic Completionist", "Max Mythic aspects", false, MYTHIC_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.fabled", "Fabled Completionist", "Max Fabled aspects", false, FABLED_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.legendary", "Legendary Completionist", "Max Legendary aspects", false, LEGENDARY_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.warrior", "Warrior Completionist", "Max Warrior aspects", false, WARRIOR_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.shaman", "Shaman Completionist", "Max Shaman aspects", false, SHAMAN_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.mage", "Mage Completionist", "Max Mage aspects", false, MAGE_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.archer", "Archer Completionist", "Max Archer aspects", false, ARCHER_ASPECT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("aspect.max.all.assassin", "Assassin Completionist", "Max Assassin aspects", false, ASSASSIN_ASPECT_TARGETS), onlyMissing);

        changed |= registerDefault(tiered("prof.gather.100", "Level 100 Gathering", "Reach level 100 in gathering professions", false, GATHERING_PROFESSION_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("prof.gather.115", "Level 115 Gathering", "Reach level 115 in gathering professions", false, GATHERING_PROFESSION_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("prof.gather.132", "Level 132 Gathering", "Reach level 132 in gathering professions", false, GATHERING_PROFESSION_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("prof.craft.100", "Level 100 Crafting", "Reach level 100 in crafting professions", false, CRAFTING_PROFESSION_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("prof.craft.115", "Level 115 Crafting", "Reach level 115 in crafting professions", false, CRAFTING_PROFESSION_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("prof.craft.132", "Level 132 Crafting", "Reach level 132 in crafting professions", false, CRAFTING_PROFESSION_TARGETS), onlyMissing);

        return changed;
    }

    public static boolean isKnownAchievement(String id) {
        return KNOWN_ACHIEVEMENT_IDS.contains(id);
    }

    private static Set<String> knownAchievementIds() {
        Achievements achievements = new Achievements();
        achievements.populateAll();
        return Collections.unmodifiableSet(new HashSet<>(achievements.byId.keySet()));
    }

    private boolean registerDefault(Achievement achievement, boolean onlyMissing) {
        if (onlyMissing && byId.containsKey(achievement.id)) return false;

        register(achievement);
        return true;
    }

    public void register(Achievement a) {
        if (a == null) throw new IllegalArgumentException("Achievement is null");
        if (a.id == null || a.id.isEmpty()) throw new IllegalArgumentException("Achievement id must not be null/empty");

        Achievement old = byId.get(a.id);
        if (old != null) {
            removeFromLists(old);
        }

        byId.put(a.id, a);

        if (a instanceof TieredAchievement) tieredAchievements.add((TieredAchievement) a);
        else if (a instanceof ProgressAchievement) progressAchievements.add((ProgressAchievement) a);
        else achievements.add(a);
    }

    private void removeFromLists(Achievement a) {
        if (a instanceof TieredAchievement) tieredAchievements.remove(a);
        else if (a instanceof ProgressAchievement) progressAchievements.remove(a);
        else achievements.remove(a);
    }

    public Achievement getById(String id) {
        return byId.get(id);
    }

    public List<Achievement> allSimple() {
        return Collections.unmodifiableList(achievements);
    }

    public List<ProgressAchievement> allProgress() {
        return Collections.unmodifiableList(progressAchievements);
    }

    public List<TieredAchievement> allTiered() {
        return Collections.unmodifiableList(tieredAchievements);
    }

    public boolean setUnlocked(String id, boolean unlocked) {
        Achievement a = byId.get(id);
        if (a == null) return false;

        if (unlocked) {
            a.unlock();
        } else {
            a.unlocked = false;
            a.unlockedAt = null;

            if (a instanceof ProgressAchievement) {
                ((ProgressAchievement) a).current = 0;
            }
            if (a instanceof TieredAchievement) {
                ((TieredAchievement) a).currentLevel = 0;
            }
        }
        return true;
    }

    public boolean setCompleted(String id) {
        return setUnlocked(id, true);
    }

    /** Current raw progress count of a progress/tiered achievement, or null if it isn't one. */
    public Integer getCount(String id) {
        Achievement a = byId.get(id);
        if (a instanceof ProgressAchievement) return ((ProgressAchievement) a).current;
        return null;
    }

    /** Sets the absolute progress count of a tiered achievement, recomputing its tier level. */
    public boolean setCount(String id, int count) {
        Achievement a = byId.get(id);
        if (!(a instanceof TieredAchievement)) return false;
        ((TieredAchievement) a).setCurrent(count);
        return true;
    }

    /**
     * Sets the absolute progress and target of a plain {@link ProgressAchievement} (not a tiered one),
     * unlocking it if the goal is met. The target is updated too so callers can supply a value derived
     * from live data (e.g. the total number of aspects of a rarity). Returns true if this call newly
     * unlocked the achievement.
     */
    public boolean setProgressGoal(String id, int current, int target) {
        Achievement a = byId.get(id);
        if (!(a instanceof ProgressAchievement) || a instanceof TieredAchievement) return false;
        ProgressAchievement p = (ProgressAchievement) a;
        boolean wasUnlocked = p.unlocked;
        p.target = target;
        p.setCurrentAbsolute(current);
        return p.unlocked && !wasUnlocked;
    }

    public boolean addProgress(String id, int amount) {
        Achievement a = byId.get(id);
        if (a == null) return false;
        if (a instanceof ProgressAchievement) {
            ((ProgressAchievement) a).progress(amount);
            return true;
        }
        return false;
    }

    public boolean isUnlocked(String id) {
        Achievement a = byId.get(id);
        return a != null && a.isUnlocked();
    }

    public Float getProgressPercent(String id) {
        Achievement a = byId.get(id);
        if (a == null) return null;
        return a.getProgress() * 100f;
    }

    public Integer getTier(String id) {
        Achievement a = byId.get(id);
        if (a == null) return null;
        if (a instanceof TieredAchievement) {
            return ((TieredAchievement) a).currentLevel;
        }
        return null;
    }

    public Integer getCurrentTierTarget(String id) {
        Achievement a = byId.get(id);
        if (!(a instanceof TieredAchievement)) return null;
        TieredAchievement t = (TieredAchievement) a;
        int lvl = t.currentLevel;
        if (lvl < 0 || lvl >= t.levelTargets.size()) return null;
        return t.levelTargets.get(lvl);
    }

    public Integer getNextTierTarget(String id) {
        Achievement a = byId.get(id);
        if (!(a instanceof TieredAchievement)) return null;
        TieredAchievement t = (TieredAchievement) a;
        int next = t.currentLevel + 1;
        if (next < 0 || next >= t.levelTargets.size()) return null;
        return t.levelTargets.get(next);
    }

    static GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(StyledText.class, new StyledTextAdapter());

    static Gson gson = builder
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .setPrettyPrinting()
            .create();


    public static void load() {
        Path configPath = getConfigPath("achievements.json");

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Achievements loaded = fromJson(json);
                if (loaded != null) {
                    AchievementTracking.achievements = loaded;
                    if (!isCompactFormat(json)) {
                        save();
                    }
                } else {
                    createDefaultAchievements();
                }
            } catch (Exception e) {
                WynnExtras.LOGGER.error("[WynnExtras] Couldn't load achievements file, recreating defaults.", e);
                createDefaultAchievements();
            }
        } else {
            createDefaultAchievements();
        }
    }

    private static void createDefaultAchievements() {
        AchievementTracking.achievements = new Achievements();
        AchievementTracking.achievements.populateAll();
    }

    public static void save() {
        Path configPath = getConfigPath("achievements.json");
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't create achievements config directory:", e);
            return;
        }

        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(toCompactJson(AchievementTracking.achievements, false), writer);
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write the achievements file:");
            e.printStackTrace();
        }
        scheduleServerSave();
    }

    /** Debounce window: how long to wait after the last change before uploading. */
    private static final long UPLOAD_DEBOUNCE_SECONDS = 15;
    /** Upper bound on how long an upload can be deferred while changes keep arriving. */
    private static final long UPLOAD_MAX_WAIT_SECONDS = 60;

    private static final ScheduledExecutorService ACHIEVEMENT_UPLOAD_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "WynnExtras-Achievement-Upload");
                t.setDaemon(true);
                return t;
            });

    private static ScheduledFuture<?> pendingAchievementUpload;
    private static boolean uploadInFlight;
    private static boolean uploadAgainAfterCurrent;
    /** Nanotime of the first change of the current debounce window (0 when none is pending). */
    private static long firstPendingChangeNanos;

    /**
     * Schedules a debounced server upload. Every change pushes the upload out by
     * {@link #UPLOAD_DEBOUNCE_SECONDS}, but never past {@link #UPLOAD_MAX_WAIT_SECONDS} after the
     * first pending change, so a steady stream of changes still gets flushed periodically.
     */
    private static synchronized void scheduleServerSave() {
        if (!shouldUploadAchievements()) {
            if (pendingAchievementUpload != null) {
                pendingAchievementUpload.cancel(false);
                pendingAchievementUpload = null;
            }
            firstPendingChangeNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        if (firstPendingChangeNanos == 0L) {
            firstPendingChangeNanos = now;
        }

        long maxWaitRemaining = TimeUnit.SECONDS.toNanos(UPLOAD_MAX_WAIT_SECONDS) - (now - firstPendingChangeNanos);
        long delayNanos = Math.max(0L, Math.min(TimeUnit.SECONDS.toNanos(UPLOAD_DEBOUNCE_SECONDS), maxWaitRemaining));

        if (pendingAchievementUpload != null) {
            pendingAchievementUpload.cancel(false);
        }
        pendingAchievementUpload = ACHIEVEMENT_UPLOAD_EXECUTOR.schedule(
                Achievements::runScheduledUpload, delayNanos, TimeUnit.NANOSECONDS);
    }

    private static void runScheduledUpload() {
        synchronized (Achievements.class) {
            pendingAchievementUpload = null;
            firstPendingChangeNanos = 0L;
        }
        if (!shouldUploadAchievements()) return;
        uploadNow();
    }

    /**
     * Uploads the current state immediately, ensuring only one upload runs at a time. If an upload
     * is already in flight, the latest state is re-sent once it finishes.
     */
    private static CompletableFuture<?> uploadNow() {
        synchronized (Achievements.class) {
            if (uploadInFlight) {
                uploadAgainAfterCurrent = true;
                return CompletableFuture.completedFuture(null);
            }
            uploadInFlight = true;
        }

        return serverSave().whenComplete((ignored, error) -> {
            synchronized (Achievements.class) {
                uploadInFlight = false;
                if (uploadAgainAfterCurrent) {
                    uploadAgainAfterCurrent = false;
                    scheduleServerSave();
                }
            }
        });
    }

    /**
     * Cancels any pending debounce and uploads the latest state right away. Intended for
     * disconnect/shutdown so a change made within the debounce window isn't lost. Returns a future
     * that completes when the upload finishes (callers may block on it during shutdown).
     */
    public static CompletableFuture<?> flushServerSave() {
        if (!shouldUploadAchievements()) {
            synchronized (Achievements.class) {
                if (pendingAchievementUpload != null) {
                    pendingAchievementUpload.cancel(false);
                    pendingAchievementUpload = null;
                }
                firstPendingChangeNanos = 0L;
                uploadAgainAfterCurrent = false;
            }
            return CompletableFuture.completedFuture(null);
        }

        boolean hadPending;
        synchronized (Achievements.class) {
            hadPending = pendingAchievementUpload != null;
            if (hadPending) {
                pendingAchievementUpload.cancel(false);
                pendingAchievementUpload = null;
            }
            firstPendingChangeNanos = 0L;
        }
        if (!hadPending) {
            // Nothing was waiting to be sent; the server already has the latest state.
            return CompletableFuture.completedFuture(null);
        }
        return uploadNow();
    }

    private static CompletableFuture<?> serverSave(){
        if (!shouldUploadAchievements()) return CompletableFuture.completedFuture(null);

        JsonObject payload = toCompactJson(AchievementTracking.achievements, true);

        return MojangAuth.getWEToken().thenCompose(wynnextrasToken ->
        {
        if (wynnextrasToken == null) {
            WynnExtras.LOGGER.error("Failed to authenticate with Mojang for achievement upload");
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://wynnextras.com/achievements"))
                .header("Content-Type", "application/json")
                .header("Authorization", wynnextrasToken)
                .POST(HttpRequest.BodyPublishers.ofString(Achievements.gson.toJson(payload)))
                .timeout(Duration.ofSeconds(8))
                .build();
        return ApiRequestHelper.sendWithAuthRetry(request, payload)
                .thenAccept(response -> {
                    int code = response.statusCode();
                    if(code == 401) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cAuthentication failed"));
                    } else if(code != 200) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError uploading achievements: " + code));
                    }
                })
                .exceptionally(ex -> {
                    WynnExtras.LOGGER.error("Failed to upload achievements: " + ex.getMessage());
                    return null;
                });
        });
    }

    private static boolean shouldUploadAchievements() {
        return WynnExtrasConfig.INSTANCE.uploadAchievements;
    }

    private static boolean isCompactFormat(JsonObject json) {
        return json != null && json.has("schemaVersion") && json.has("achievements")
                && json.get("achievements").isJsonArray();
    }

    private static JsonObject toCompactJson(Achievements achievements, boolean includeModVersion) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        if (includeModVersion) {
            root.addProperty("modVersion", CurrentVersionData.INSTANCE.version);
        }

        JsonArray states = new JsonArray();
        if (achievements != null) {
            for (Achievement achievement : achievements.allSimple()) {
                states.add(toStateJson(achievement));
            }
            for (ProgressAchievement achievement : achievements.allProgress()) {
                states.add(toStateJson(achievement));
            }
            for (TieredAchievement achievement : achievements.allTiered()) {
                states.add(toStateJson(achievement));
            }
        }
        root.add("achievements", states);
        return root;
    }

    private static JsonObject toStateJson(Achievement achievement) {
        JsonObject state = new JsonObject();
        state.addProperty("id", achievement.id);
        state.addProperty("unlocked", achievement.unlocked);
        if (achievement instanceof ProgressAchievement progressAchievement) {
            state.addProperty("current", progressAchievement.current);
        }
        return state;
    }

    private static Achievements fromJson(JsonObject json) {
        if (json == null) return null;

        Achievements achievements = new Achievements();
        achievements.populateAll();

        if (isCompactFormat(json)) {
            applyCompactStates(achievements, json.getAsJsonArray("achievements"));
            return achievements;
        }

        Achievements oldFormat = gson.fromJson(json, Achievements.class);
        if (oldFormat == null) return achievements;
        oldFormat.rebuildIndex();
        applyOldStates(achievements, oldFormat.achievements);
        applyOldStates(achievements, oldFormat.progressAchievements);
        applyOldStates(achievements, oldFormat.tieredAchievements);
        return achievements;
    }

    private static void applyCompactStates(Achievements achievements, JsonArray states) {
        if (states == null) return;
        for (JsonElement element : states) {
            if (!element.isJsonObject()) continue;

            JsonObject state = element.getAsJsonObject();
            if (!state.has("id") || !state.get("id").isJsonPrimitive()) continue;

            String id = state.get("id").getAsString();
            Achievement achievement = achievements.getById(id);
            if (achievement == null) continue;

            Integer current = null;
            if (state.has("current") && state.get("current").isJsonPrimitive()) {
                try {
                    current = state.get("current").getAsInt();
                } catch (NumberFormatException ignored) {
                }
            }
            applyState(achievement, state.has("unlocked") && state.get("unlocked").getAsBoolean(), current);
        }
    }

    private static void applyOldStates(Achievements achievements, List<? extends Achievement> oldStates) {
        if (oldStates == null) return;
        for (Achievement oldState : oldStates) {
            if (oldState == null || oldState.id == null) continue;
            Achievement achievement = achievements.getById(oldState.id);
            if (achievement == null) continue;

            Integer current = oldState instanceof ProgressAchievement progressAchievement ? progressAchievement.current : null;
            applyState(achievement, oldState.unlocked, current);
        }
    }

    private static void applyState(Achievement achievement, boolean unlocked, Integer current) {
        if (current != null && achievement instanceof TieredAchievement tieredAchievement) {
            tieredAchievement.setCurrent(current);
        } else if (current != null && achievement instanceof ProgressAchievement progressAchievement) {
            progressAchievement.setCurrentAbsolute(current);
        }

        if (unlocked) {
            achievement.unlock();
        }
    }

    private static Path getConfigPath(String fileName) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras/"
                        + MinecraftClient.getInstance().player.getUuid()
                        + "/" + fileName);
    }
    public void rebuildIndex() {
        byId.clear();
        rebuildList(achievements);
        rebuildList(progressAchievements);
        rebuildList(tieredAchievements);
    }

    private void rebuildList(List<? extends Achievement> list) {
        if (list == null) return;

        list.removeIf(a -> a == null || a.id == null || a.id.isEmpty());

        for (Achievement a : list) {
            byId.put(a.id, a);
        }
    }
}