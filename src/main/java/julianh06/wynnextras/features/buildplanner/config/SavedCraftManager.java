package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import julianh06.wynnextras.features.buildplanner.BuildPlannerRuntime;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SavedCraftManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final List<SavedCraft> crafts = new ArrayList<>();
    private boolean unreadable;

    SavedCraftManager(Path file) {
        this.file = file;
        if (!Files.exists(file)) return;
        try {
            var entries = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
            for (var entry : entries) {
                try {
                    SavedCraft craft = GSON.fromJson(entry, SavedCraft.class);
                    if (craft == null) throw new IllegalArgumentException("Missing saved craft");
                    crafts.add(craft);
                } catch (RuntimeException exception) {
                    unreadable = true;
                    PlannerLog.LOGGER.warn("Could not read a saved craft; preserving the original file.", exception);
                }
            }
        } catch (IOException | RuntimeException exception) {
            unreadable = true;
            PlannerLog.LOGGER.warn("Could not load saved crafts; preserving the original file.", exception);
        }
    }

    public static SavedCraftManager getInstance() { return Holder.INSTANCE; }

    public synchronized List<SavedCraft> getAll() {
        return crafts.stream().sorted(Comparator.comparingLong(SavedCraft::savedAt).reversed()).toList();
    }

    public synchronized boolean contains(String name) {
        return crafts.stream().anyMatch(craft -> craft.name().equalsIgnoreCase(name.trim()));
    }

    public synchronized boolean save(SavedCraft craft) {
        crafts.removeIf(existing -> existing.name().equalsIgnoreCase(craft.name()));
        crafts.add(craft);
        return write();
    }

    public synchronized boolean remove(SavedCraft craft) {
        return crafts.remove(craft) && write();
    }

    public synchronized SavedCraft rename(SavedCraft craft, String name) {
        String cleaned = name == null ? "" : name.trim();
        int index = crafts.indexOf(craft);
        if (index < 0 || cleaned.isEmpty() || cleaned.length() > 64
                || crafts.stream().anyMatch(existing -> existing != craft
                        && existing.name().equalsIgnoreCase(cleaned))) {
            return null;
        }
        SavedCraft renamed = new SavedCraft(cleaned, craft.code(), craft.savedAt());
        crafts.set(index, renamed);
        write();
        return renamed;
    }

    private boolean write() {
        if (unreadable) return false;
        try {
            AtomicConfigWriter.write(file, GSON.toJson(crafts), "saved-crafts-");
            return true;
        } catch (IOException exception) {
            PlannerLog.LOGGER.warn("Could not write saved crafts.", exception);
            return false;
        }
    }

    private static final class Holder {
        private static final SavedCraftManager INSTANCE = new SavedCraftManager(
                BuildPlannerRuntime.configDirectory().resolve("saved-crafts.json"));
    }
}
