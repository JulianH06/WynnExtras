package julianh06.wynnextras.features.lootruns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tracks lootrun statistics for the current session and all-time
 */
public class LootrunStatistics {
    public static LootrunStatistics INSTANCE = new LootrunStatistics();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/lootrun_stats.json");

    // Session statistics (reset on game restart)
    public transient int sessionPulls = 0;
    public transient int sessionMythics = 0;
    public transient int sessionFabled = 0;
    public transient int sessionLegendary = 0;
    public transient int sessionRare = 0;
    public transient int sessionUnique = 0;
    public transient int sessionShiny = 0;
    public transient long sessionStartTime = System.currentTimeMillis();
    public transient int sessionDryStreak = 0; // Chests since last mythic
    public transient boolean lastPullWasMythic = false;

    // All-time statistics (persisted)
    public int totalPulls = 0;
    public int totalMythics = 0;
    public int totalFabled = 0;
    public int totalLegendary = 0;
    public int totalRare = 0;
    public int totalUnique = 0;
    public int totalShiny = 0;
    public int longestDryStreak = 0;
    public int backToBackMythics = 0;

    // Computed stats
    public float getSessionPullsPerMythic() {
        if (sessionMythics == 0) return sessionPulls;
        return (float) sessionPulls / sessionMythics;
    }

    public float getAllTimePullsPerMythic() {
        if (totalMythics == 0) return totalPulls;
        return (float) totalPulls / totalMythics;
    }

    public float getSessionTimePerPull() {
        if (sessionPulls == 0) return 0;
        long elapsedMs = System.currentTimeMillis() - sessionStartTime;
        return (float) elapsedMs / 1000f / sessionPulls;
    }

    public String getSessionDuration() {
        long elapsedMs = System.currentTimeMillis() - sessionStartTime;
        long seconds = elapsedMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        seconds = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public void recordPull() {
        sessionPulls++;
        totalPulls++;
        sessionDryStreak++;
        lastPullWasMythic = false;
    }

    public void recordMythic() {
        sessionMythics++;
        totalMythics++;

        // Check for back-to-back
        if (lastPullWasMythic) {
            backToBackMythics++;
        }

        // Update dry streak
        if (sessionDryStreak > longestDryStreak) {
            longestDryStreak = sessionDryStreak;
        }
        sessionDryStreak = 0;
        lastPullWasMythic = true;
    }

    public void recordFabled() {
        sessionFabled++;
        totalFabled++;
    }

    public void recordLegendary() {
        sessionLegendary++;
        totalLegendary++;
    }

    public void recordRare() {
        sessionRare++;
        totalRare++;
    }

    public void recordUnique() {
        sessionUnique++;
        totalUnique++;
    }

    public void recordShiny() {
        sessionShiny++;
        totalShiny++;
    }

    public void resetSession() {
        sessionPulls = 0;
        sessionMythics = 0;
        sessionFabled = 0;
        sessionLegendary = 0;
        sessionRare = 0;
        sessionUnique = 0;
        sessionShiny = 0;
        sessionStartTime = System.currentTimeMillis();
        sessionDryStreak = 0;
        lastPullWasMythic = false;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                LootrunStatistics loaded = GSON.fromJson(reader, LootrunStatistics.class);
                if (loaded != null) {
                    // Only load persisted fields
                    INSTANCE.totalPulls = loaded.totalPulls;
                    INSTANCE.totalMythics = loaded.totalMythics;
                    INSTANCE.totalFabled = loaded.totalFabled;
                    INSTANCE.totalLegendary = loaded.totalLegendary;
                    INSTANCE.totalRare = loaded.totalRare;
                    INSTANCE.totalUnique = loaded.totalUnique;
                    INSTANCE.totalShiny = loaded.totalShiny;
                    INSTANCE.longestDryStreak = loaded.longestDryStreak;
                    INSTANCE.backToBackMythics = loaded.backToBackMythics;
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read lootrun stats: " + e.getMessage());
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't save lootrun stats: " + e.getMessage());
        }
    }
}
