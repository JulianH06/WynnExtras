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

    // zentrales Lookup nach id
    private final Map<String, Achievement> byId = new HashMap<>();

    public void populateAll(){
        // Beispiel: lege deine Achievements hier an
        register(simple("simple.level.120", "Reach Level 120", "Reach Combat Level 120", false));
        register(simple("simple.level.121", "Reach Level 121", "Reach Combat Level 121", false));
        register(progress("test.progress", "Test Progress", "fortschrittsbasiert", false, 100));
        register(tiered("test.tiered", "Test Tiered", "mehrstufiger Test", false, List.of(10, 50, 200)));

    }

    // --- Factory-Methoden, erzeugen konkrete Instanzen als anonyme Unterklassen ---
    private Achievement simple(String id, String title, String description, boolean secret) {
        String nid = id;
        String ntitle = title;
        String ndescription = description;
        boolean nsecret = secret;

        return new Achievement() {
            {
                this.id = nid;
                this.title = ntitle;
                this.description = ndescription;
                this.secret = nsecret;
            }

            @Override
            public float getProgress() {
                return unlocked ? 1f : 0f;
            }
        };
    }

    private ProgressAchievement progress(String id, String title, String description, boolean secret, int target) {
        String nid = id;
        String ntitle = title;
        String ndescription = description;
        boolean nsecret = secret;
        int ntarget = target;

        return new ProgressAchievement() {
            {
                this.id = nid;
                this.title = ntitle;
                this.description = ndescription;
                this.secret = nsecret;
                this.current = 0;
                this.target = ntarget;
            }
            // getProgress() kommt aus ProgressAchievement
        };
    }

    private TieredAchievement tiered(String id, String title, String description, boolean secret, List<Integer> levelTargets) {
        String nid = id;
        String ntitle = title;
        String ndescription = description;
        boolean nsecret = secret;
        List<Integer> nlevelTargets = levelTargets;
        return new TieredAchievement() {
            {
                this.id = nid;
                this.title = ntitle;
                this.description = ndescription;
                this.secret = nsecret;
                this.current = 0;
                this.currentLevel = 0;
                this.levelTargets = List.copyOf(nlevelTargets);
            }
            // TieredAchievement hat eigene progress()/getProgress()
        };
    }

    // --- Registrierung / Verwaltung ---
    public void register(Achievement a) {
        if (a == null) throw new IllegalArgumentException("Achievement is null");
        if (a.id == null || a.id.isEmpty()) throw new IllegalArgumentException("Achievement id must not be null/empty");

        // falls schon vorhanden: entferne alten Eintrag (wir überschreiben)
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

    // --- Lookup / Getter ---
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

    // --- Bearbeiten / Abfragen von Status & Fortschritt ---

    /**
     * Setzt unlocked = true (unlock) oder false (reset) für ein Achievement.
     * Gibt false zurück, wenn die ID nicht gefunden wurde.
     */
    public boolean setUnlocked(String id, boolean unlocked) {
        Achievement a = byId.get(id);
        if (a == null) return false;

        if (unlocked) {
            a.unlock();
        } else {
            // Zugriff auf geschützte Felder ist erlaubt, da wir im selben package sind
            a.unlocked = false;
            a.unlockedAt = null;
            // Falls du auch progress zurücksetzen willst, kannst du das hier für Progress/Tiered tun:
            if (a instanceof ProgressAchievement) {
                ((ProgressAchievement) a).current = 0;
            }
            if (a instanceof TieredAchievement) {
                ((TieredAchievement) a).currentLevel = 0;
            }
        }
        return true;
    }

    /** Shortcut um ein Achievement als abgeschlossen zu markieren */
    public boolean setCompleted(String id) {
        return setUnlocked(id, true);
    }

    /**
     * Fügt Fortschritt hinzu. Liefert true wenn das Achievement existiert und progress() aufgerufen wurde.
     * Für Simple-Achievements passiert nichts und wird false zurückgegeben.
     */
    public boolean addProgress(String id, int amount) {
        Achievement a = byId.get(id);
        if (a == null) return false;
        if (a instanceof ProgressAchievement) {
            ((ProgressAchievement) a).progress(amount);
            return true;
        }
        return false;
    }

    /**
     * Gibt zurück, ob Achievement für gegebene id unlocked ist.
     * Returns null wenn id nicht gefunden wurde.
     */
    public boolean isUnlocked(String id) {
        Achievement a = byId.get(id);
        return a != null && a.isUnlocked();
    }

    /**
     * Liefert progress als Prozent (0..100). Gibt null zurück, wenn ID nicht gefunden wurde.
     * Hinweis: intern benutzt getProgress() 0..1; hier multipliziert nach 0..100.
     */
    public Float getProgressPercent(String id) {
        Achievement a = byId.get(id);
        if (a == null) return null;
        return a.getProgress() * 100f;
    }

    /**
     * Liefert das aktuelle Tier (currentLevel) für Tiered-Achievements.
     * Gibt null zurück, wenn ID nicht gefunden wurde oder das Achievement nicht tiered ist.
     */
    public Integer getTier(String id) {
        Achievement a = byId.get(id);
        if (a == null) return null;
        if (a instanceof TieredAchievement) {
            return ((TieredAchievement) a).currentLevel;
        }
        return null;
    }

    /**
     * Weitere Helfer: nextTierTarget / currentTierTarget (null wenn nicht tiered)
     */
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


    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("wynnextras/"
                    + MinecraftClient.getInstance().player.getUuid().toString()
                    + "/achievemtns.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                Achievements loaded = gson.fromJson(reader, Achievements.class);
                if (loaded != null) {
                    loaded.rebuildIndex();
                    AchievementTracking.achievements = loaded;
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
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(AchievementTracking.achievements, writer);
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't write the achievements file:");
            e.printStackTrace();
        }
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