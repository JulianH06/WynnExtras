package julianh06.wynnextras.features.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.mc.McUtils;
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
import java.util.List;
import java.util.Map;
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

    private boolean registerDefaultAchievements(boolean onlyMissing) {
        boolean changed = false;
        changed |= registerDefault(simple("simple.level.120", "Reach Level 120", "Reach Combat Level 120", false), onlyMissing);
        changed |= registerDefault(simple("simple.level.121", "Reach Level 121", "Reach Combat Level 121", false), onlyMissing);
        changed |= registerDefault(progress("test.progress", "Test Progress", "progress", false, 100), onlyMissing);
        changed |= registerDefault(tiered("test.tiered", "Test Tiered", "tiered", false, List.of(10, 50, 200)), onlyMissing);

        changed |= registerDefault(tiered("raid.tna",  "The Nameless Anomaly",     "Complete The Nameless Anomaly",      false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.notg", "Nest of the Grootslangs",  "Complete Nest of the Grootslangs",   false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.nol",  "Orphion's Nexus of Light", "Complete Orphion's Nexus of Light",  false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.twp",  "The Wartorn Palace",       "Complete The Wartorn Palace",        false, RAID_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("raid.tcc",  "The Canyon Colossus",      "Complete The Canyon Colossus",       false, RAID_TARGETS), onlyMissing);

        // Class level milestones — number of classes reaching the combat level cap.
        changed |= registerDefault(tiered("class.level120", "Level 120 Classes", "Get classes to Combat Level 120", false, CLASS_COUNT_TARGETS), onlyMissing);
        changed |= registerDefault(tiered("class.level121", "Level 121 Classes", "Get classes to Combat Level 121", false, CLASS_COUNT_TARGETS), onlyMissing);

        // Content completion.
        changed |= registerDefault(simple("content.completion", "Completionist", "Reach 100% content completion on a class", false), onlyMissing);

        // Aspect milestones. "Max all" targets are filled in from the live aspect catalogue at sync time.
        changed |= registerDefault(simple("aspect.max.one", "Aspect Master", "Max an aspect", false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.mythic", "Mythic Mastery", "Max a Mythic aspect", false), onlyMissing);
        changed |= registerDefault(progress("aspect.max.legendary.10", "Legendary Collector", "Max 10 Legendary aspects", false, 10), onlyMissing);
        changed |= registerDefault(progress("aspect.max.fabled.10", "Fabled Collector", "Max 10 Fabled aspects", false, 10), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.warrior",  "Warrior Ascended",  "Max all Warrior aspects",  false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.shaman",   "Shaman Ascended",   "Max all Shaman aspects",   false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.mage",     "Mage Ascended",     "Max all Mage aspects",     false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.archer",   "Archer Ascended",   "Max all Archer aspects",   false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.assassin", "Assassin Ascended", "Max all Assassin aspects", false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.legendary", "Legendary Completionist", "Max all Legendary aspects", false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.fabled",    "Fabled Completionist",    "Max all Fabled aspects",    false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all.mythic",    "Mythic Completionist",    "Max all Mythic aspects",    false), onlyMissing);
        changed |= registerDefault(simple("aspect.max.all", "Aspect Completionist", "Max all aspects", false), onlyMissing);

        // Gathering professions.
        changed |= registerDefault(simple("prof.gather.one.100", "Gatherer",              "Reach level 100 in a gathering profession",    false), onlyMissing);
        changed |= registerDefault(simple("prof.gather.one.132", "Master Gatherer",       "Reach level 132 in a gathering profession",    false), onlyMissing);
        changed |= registerDefault(simple("prof.gather.all.100", "Seasoned Gatherer",     "Reach level 100 in all gathering professions", false), onlyMissing);
        changed |= registerDefault(simple("prof.gather.all.132", "Gathering Grandmaster", "Reach level 132 in all gathering professions", false), onlyMissing);

        // Crafting professions.
        changed |= registerDefault(simple("prof.craft.one.100", "Crafter",              "Reach level 100 in a crafting profession",    false), onlyMissing);
        changed |= registerDefault(simple("prof.craft.one.132", "Master Crafter",       "Reach level 132 in a crafting profession",    false), onlyMissing);
        changed |= registerDefault(simple("prof.craft.all.100", "Seasoned Crafter",     "Reach level 100 in all crafting professions", false), onlyMissing);
        changed |= registerDefault(simple("prof.craft.all.132", "Crafting Grandmaster", "Reach level 132 in all crafting professions", false), onlyMissing);
        return changed;
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
                Achievements loaded = gson.fromJson(reader, Achievements.class);
                if (loaded != null) {
                    loaded.rebuildIndex();
                    boolean changed = loaded.registerDefaultAchievements(true);
                    AchievementTracking.achievements = loaded;
                    if (changed) {
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
            gson.toJson(AchievementTracking.achievements, writer);
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
        JsonObject payload = Achievements.gson.toJsonTree(AchievementTracking.achievements).getAsJsonObject();
        payload.addProperty("modVersion", CurrentVersionData.INSTANCE.version);

        return MojangAuth.getWEToken().thenCompose(wynnextrasToken ->
        {
        if (wynnextrasToken == null) {
            WynnExtras.LOGGER.error("Failed to authenticate with Mojang for aspect upload");
            // Don't show duplicate error - MojangAuth already showed the error
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