package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class RaidLootConfig {

    public static RaidLootConfig INSTANCE = new RaidLootConfig();
    public RaidLootData data = new RaidLootData();

    private static Path getPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras/raidLootTracker.json");
    }

    public void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path path = getPath();

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(this, writer);
            }
            WynnExtras.LOGGER.info("[WynnExtras] Saved RaidLootTracker data to " + path);
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't save RaidLootTracker");
            e.printStackTrace();
        }
    }

    public void load() {
        Path path = getPath();
        if (!Files.exists(path)) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Reader reader = Files.newBufferedReader(path)) {
            RaidLootConfig loaded =
                    gson.fromJson(reader, RaidLootConfig.class);
            if (loaded != null && loaded.data != null) {
                // Copy the data into the existing instance instead of replacing INSTANCE
                this.data = loaded.data;
                // Ensure perRaidData is initialized for backward compatibility
                if (this.data.perRaidData == null) {
                    this.data.perRaidData = new HashMap<>();
                }
                // One-time migration: TWP's reward chest coords were missing from
                // RaidLootTracker.REWARD_CHEST_COORDS, so every TWP run got logged under
                // "UNKNOWN". Fold that bucket into TWP so users don't lose their history.
                RaidLootData.RaidSpecificLoot unknown = this.data.perRaidData.remove("UNKNOWN");
                if (unknown != null) {
                    this.data.getOrCreateRaidData("TWP").mergeFrom(unknown);
                    WynnExtras.LOGGER.info("[WynnExtras] Migrated UNKNOWN raid loot -> TWP");
                    save();
                }
                WynnExtras.LOGGER.info("[WynnExtras] Loaded RaidLootTracker data successfully");
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Couldn't load RaidLootTracker");
            e.printStackTrace();
        }
    }
}
