package julianh06.wynnextras.features.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.text.StyledText;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
import julianh06.wynnextras.utils.InstantTypeAdapter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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