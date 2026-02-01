package julianh06.wynnextras.features.achievements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class AchievementStorage {
    public static AchievementStorage INSTANCE = new AchievementStorage();

    private Map<String, AchievementProgress> progress = new HashMap<>();

    public static class AchievementProgress {
        public int current;
        public int currentTierIndex;
        public boolean unlocked;
        public Instant unlockedAt;

        public AchievementProgress() {}

        public AchievementProgress(int current, int currentTierIndex, boolean unlocked, Instant unlockedAt) {
            this.current = current;
            this.currentTierIndex = currentTierIndex;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
        }
    }

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/achievements.json");

    public Map<String, AchievementProgress> getProgress() {
        return progress;
    }

    public AchievementProgress getProgress(String achievementId) {
        return progress.get(achievementId);
    }

    public void setProgress(String achievementId, AchievementProgress prog) {
        progress.put(achievementId, prog);
    }

    public void saveAchievement(Achievement achievement) {
        AchievementProgress prog = new AchievementProgress();
        prog.unlocked = achievement.isUnlocked();
        prog.unlockedAt = achievement.getUnlockedAt();

        if (achievement instanceof TieredAchievement tiered) {
            prog.current = tiered.getCurrent();
            prog.currentTierIndex = tiered.getCurrentTierIndex();
        } else if (achievement instanceof ProgressAchievement progAch) {
            prog.current = progAch.getCurrent();
            prog.currentTierIndex = 0;
        } else {
            prog.current = achievement.isUnlocked() ? 1 : 0;
            prog.currentTierIndex = 0;
        }

        progress.put(achievement.getId(), prog);
    }

    public void loadAchievement(Achievement achievement) {
        AchievementProgress prog = progress.get(achievement.getId());
        if (prog == null) return;

        achievement.setUnlocked(prog.unlocked, prog.unlockedAt);

        if (achievement instanceof TieredAchievement tiered) {
            tiered.setCurrent(prog.current);
            tiered.setCurrentTierIndex(prog.currentTierIndex);
        } else if (achievement instanceof ProgressAchievement progAch) {
            progAch.setCurrent(prog.current);
        }
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                AchievementStorage loaded = gson.fromJson(reader, AchievementStorage.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    if (INSTANCE.progress == null) {
                        INSTANCE.progress = new HashMap<>();
                    }
                } else {
                    System.err.println("[WynnExtras] Deserialized achievement data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the achievements file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                gson.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the achievements file:");
            e.printStackTrace();
        }
    }

    private static class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            String value = in.nextString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return Instant.parse(value);
        }
    }
}
