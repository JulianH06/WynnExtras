package julianh06.wynnextras.features.aspects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores and manages loot pool data for the 4 raids
 * Data is saved locally in config folder
 * Loot pools reset weekly on Friday at 19:00 CET (temporarily 21:10 CET for testing)
 */
public class LootPoolData {
    public static final LootPoolData INSTANCE = new LootPoolData();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/wynnextras";
    private static final String DATA_FILE = "aspects_lootpools.json";

    // Reset time: Friday 19:00 CET
    private static final int RESET_HOUR = 19;
    private static final int RESET_MINUTE = 0;

    // Map: Raid -> List of (AspectName, Rarity)
    public Map<String, List<AspectEntry>> lootPools = new HashMap<>();
    public long savedTimestamp = 0; // When the data was last saved (epoch millis)

    private LootPoolData() {
        // Initialize empty pools for all raids
        lootPools.put("NOTG", new ArrayList<>());
        lootPools.put("NOL", new ArrayList<>());
        lootPools.put("TCC", new ArrayList<>());
        lootPools.put("TNA", new ArrayList<>());
    }

    public static class AspectEntry {
        public String name;
        public String rarity; // MYTHIC, FABLED, LEGENDARY
        public String tierInfo; // "Tier II [8/10]" or "[MAX]"
        public String description; // What the aspect does (if available)

        public AspectEntry(String name, String rarity) {
            this.name = name;
            this.rarity = rarity;
            this.tierInfo = "";
            this.description = "";
        }

        public AspectEntry(String name, String rarity, String tierInfo, String description) {
            this.name = name;
            this.rarity = rarity;
            this.tierInfo = tierInfo != null ? tierInfo : "";
            this.description = description != null ? description : "";
        }
    }

    /**
     * Get the last reset time (most recent Friday RESET_HOUR:RESET_MINUTE CET before now)
     */
    private long getLastResetTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime resetThisWeek = now.with(DayOfWeek.FRIDAY).withHour(RESET_HOUR).withMinute(RESET_MINUTE).withSecond(0).withNano(0);

        // If we haven't reached reset time this Friday, or it's before Friday, last reset was last week
        if (now.isBefore(resetThisWeek)) {
            resetThisWeek = resetThisWeek.minusWeeks(1);
        }

        return resetThisWeek.toInstant().toEpochMilli();
    }

    /**
     * Check if saved data is still valid (saved after last reset)
     */
    public boolean isDataValid() {
        long lastReset = getLastResetTime();
        return savedTimestamp >= lastReset;
    }

    /**
     * Save a raid's loot pool (simple - just name and rarity)
     */
    public void saveLootPool(String raid, Map<String, String> aspects) {
        List<AspectEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : aspects.entrySet()) {
            entries.add(new AspectEntry(entry.getKey(), entry.getValue()));
        }
        lootPools.put(raid, entries);
        savedTimestamp = System.currentTimeMillis();
        save();
    }

    /**
     * Save a raid's loot pool with full data (tier info + description)
     */
    public void saveLootPoolFull(String raid, List<AspectEntry> aspects) {
        lootPools.put(raid, new ArrayList<>(aspects));
        savedTimestamp = System.currentTimeMillis();
        save();
    }

    /**
     * Get a raid's loot pool
     */
    public List<AspectEntry> getLootPool(String raid) {
        // Check if data is still valid (not past reset time)
        if (!isDataValid()) {
            // Data is stale, clear it
            clear();
            return new ArrayList<>();
        }
        return lootPools.getOrDefault(raid, new ArrayList<>());
    }

    /**
     * Check if we have valid data for a raid
     */
    public boolean hasData(String raid) {
        // Check if data is still valid (not past reset time)
        if (!isDataValid()) {
            return false;
        }
        List<AspectEntry> pool = lootPools.get(raid);
        return pool != null && !pool.isEmpty();
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
            System.err.println("Failed to save loot pool data: " + e.getMessage());
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
                LootPoolData loaded = GSON.fromJson(reader, LootPoolData.class);
                if (loaded != null) {
                    if (loaded.lootPools != null) {
                        this.lootPools = loaded.lootPools;
                    }
                    this.savedTimestamp = loaded.savedTimestamp;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load loot pool data: " + e.getMessage());
        }
    }

    /**
     * Clear all data
     */
    public void clear() {
        lootPools.clear();
        lootPools.put("NOTG", new ArrayList<>());
        lootPools.put("NOL", new ArrayList<>());
        lootPools.put("TCC", new ArrayList<>());
        lootPools.put("TNA", new ArrayList<>());
        save();
    }
}
