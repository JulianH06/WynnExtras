package julianh06.wynnextras.features.aspects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores current day's gambits
 * Gambits reset daily at 19:00 CET (temporarily 21:10 CET for testing)
 */
public class GambitData {
    public static final GambitData INSTANCE = new GambitData();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/wynnextras";
    private static final String DATA_FILE = "aspects_gambits.json";

    // Reset time: 19:00 CET
    private static final int RESET_HOUR = 19;
    private static final int RESET_MINUTE = 0;

    public long savedTimestamp = 0; // When the data was saved (epoch millis)
    public List<GambitEntry> gambits = new ArrayList<>();

    public static class GambitEntry {
        public String name;
        public String description;

        public GambitEntry(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    /**
     * Save gambits for today
     */
    public void saveGambits(List<GambitEntry> gambitList) {
        this.savedTimestamp = System.currentTimeMillis();
        this.gambits = new ArrayList<>(gambitList);
        save();
    }

    /**
     * Get the last reset time (most recent RESET_HOUR:RESET_MINUTE CET before now)
     */
    public static ZonedDateTime getLastResetTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime resetToday = now.withHour(RESET_HOUR).withMinute(RESET_MINUTE).withSecond(0).withNano(0);

        // If we haven't reached reset time today, last reset was yesterday
        if (now.isBefore(resetToday)) {
            resetToday = resetToday.minusDays(1);
        }

        return resetToday;
    }

    /**
     * Check if we have valid gambits (saved after the last reset)
     */
    public boolean hasToday() {
        if (gambits.isEmpty()) return false;

        long lastReset = getLastResetTime().toInstant().toEpochMilli();
        return savedTimestamp >= lastReset;
    }

    /**
     * Save to file
     */
    public void save() {
        try {
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File file = new File(configDir, DATA_FILE);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save gambit data: " + e.getMessage());
        }
    }

    /**
     * Load from file
     */
    public void load() {
        try {
            File file = new File(CONFIG_DIR, DATA_FILE);
            if (!file.exists()) {
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                GambitData loaded = GSON.fromJson(reader, GambitData.class);
                if (loaded != null) {
                    this.savedTimestamp = loaded.savedTimestamp;
                    this.gambits = loaded.gambits != null ? loaded.gambits : new ArrayList<>();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load gambit data: " + e.getMessage());
        }
    }
}
