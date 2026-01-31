package julianh06.wynnextras.features.raid;

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
            System.out.println("[WynnExtras] Saved RaidLootTracker data to " + path);
        } catch (Exception e) {
            System.err.println("[WynnExtras] Couldn't save RaidLootTracker");
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
                System.out.println("[WynnExtras] Loaded RaidLootTracker data successfully");
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Couldn't load RaidLootTracker");
            e.printStackTrace();
        }
    }
}
