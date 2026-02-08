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
 * Stores and manages loot pool data for the 5 lootrun camps
 * Data is saved locally in config folder
 * Loot pools reset weekly on Friday at 19:00 CET (same as raids)
 */
public class LootrunLootPoolData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/wynnextras";
    private static final String DATA_FILE = "lootrun_lootpools.json";

    // Reset time: Friday 19:00 CET
    private static final int RESET_HOUR = 19;
    private static final int RESET_MINUTE = 0;

    // Camp codes and full names - MUST be defined before INSTANCE
    public static final String[] CAMP_CODES = {"SI", "SE", "CORK", "COTL", "MH"};
    public static final Map<String, String> CAMP_NAMES = Map.of(
        "SI", "Sky Islands",
        "SE", "Silent Expanse",
        "CORK", "Corkus",
        "COTL", "Canyon of the Lost",
        "MH", "Molten Heights"
    );

    // Singleton instance - MUST be after CAMP_CODES
    public static final LootrunLootPoolData INSTANCE = new LootrunLootPoolData();

    /**
     * Represents an item in a lootrun chest
     */
    public static class LootrunItem {
        public String name;
        public String rarity; // Mythic, Fabled, Legendary, Rare, Set, Unique
        public String type;   // normal, shiny, tome
        public String tooltip; // Full tooltip text
        public String shinyStat; // For shiny items: the stat they have (e.g., "Health", "Mana Regen")

        public LootrunItem(String name, String rarity, String type) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.tooltip = "";
            this.shinyStat = "";
        }

        public LootrunItem(String name, String rarity, String type, String tooltip, String shinyStat) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.tooltip = tooltip != null ? tooltip : "";
            this.shinyStat = shinyStat != null ? shinyStat : "";
        }

        /**
         * Determine item type from name
         */
        public static String determineType(String name) {
            if (name == null) return "normal";
            String lower = name.toLowerCase();
            if (lower.contains("tome")) {
                return "tome";
            } else if (lower.contains("shiny")) {
                return "shiny";
            }
            return "normal";
        }
    }

    // Map: Camp -> List of LootrunItem
    public Map<String, List<LootrunItem>> lootPools = new HashMap<>();
    public long savedTimestamp = 0; // When the data was last saved (epoch millis)

    private LootrunLootPoolData() {
        // Initialize empty pools for all camps
        for (String camp : CAMP_CODES) {
            lootPools.put(camp, new ArrayList<>());
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
     * Save a camp's loot pool with items
     */
    public void saveLootPool(String camp, List<LootrunItem> items) {
        lootPools.put(camp, new ArrayList<>(items));
        savedTimestamp = System.currentTimeMillis();
        save();
    }

    /**
     * Get a camp's loot pool
     */
    public List<LootrunItem> getLootPool(String camp) {
        // Check if data is still valid (not past reset time)
        if (!isDataValid()) {
            // Data is stale, clear it
            clear();
            return new ArrayList<>();
        }
        return lootPools.getOrDefault(camp, new ArrayList<>());
    }

    /**
     * Check if we have valid data for a camp
     */
    public boolean hasData(String camp) {
        // Check if data is still valid (not past reset time)
        if (!isDataValid()) {
            return false;
        }
        List<LootrunItem> pool = lootPools.get(camp);
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
            System.err.println("Failed to save lootrun loot pool data: " + e.getMessage());
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
                LootrunLootPoolData loaded = GSON.fromJson(reader, LootrunLootPoolData.class);
                if (loaded != null) {
                    if (loaded.lootPools != null) {
                        this.lootPools = loaded.lootPools;
                    }
                    this.savedTimestamp = loaded.savedTimestamp;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load lootrun loot pool data: " + e.getMessage());
        }
    }

    /**
     * Clear all data
     */
    public void clear() {
        lootPools.clear();
        for (String camp : CAMP_CODES) {
            lootPools.put(camp, new ArrayList<>());
        }
        save();
    }

    /**
     * Get camp code from screen title suffix
     * Titles: SE=\uF00A, SI=\uF009, MH=\uF008, CORK=\uF007, COTL=\uF006
     */
    public static String getCampFromTitle(String screenTitle) {
        if (screenTitle.endsWith("\uF00A")) {
            return "SE";
        } else if (screenTitle.endsWith("\uF009")) {
            return "SI";
        } else if (screenTitle.endsWith("\uF008")) {
            return "MH";
        } else if (screenTitle.endsWith("\uF007")) {
            return "CORK";
        } else if (screenTitle.endsWith("\uF006")) {
            return "COTL";
        }
        return null;
    }

    /**
     * Check if a screen title is a lootrun chest
     */
    public static boolean isLootrunChest(String screenTitle) {
        return screenTitle.equals("\uDAFF\uDFF2\uE00A\uDAFF\uDF6F\uF00A") || // Silent Expanse
               screenTitle.equals("\uDAFF\uDFF2\uE00A\uDAFF\uDF6F\uF009") || // Sky Islands
               screenTitle.equals("\uDAFF\uDFF2\uE00A\uDAFF\uDF6F\uF008") || // Molten Heights
               screenTitle.equals("\uDAFF\uDFF2\uE00A\uDAFF\uDF6F\uF007") || // Corkus
               screenTitle.equals("\uDAFF\uDFF2\uE00A\uDAFF\uDF6F\uF006");   // Canyon of the Lost
    }
}
