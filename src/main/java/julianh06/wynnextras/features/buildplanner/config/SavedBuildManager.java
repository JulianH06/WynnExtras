package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

public final class SavedBuildManager {
    private static final Type BUILD_LIST_TYPE = new TypeToken<List<SavedBuild>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final List<SavedBuild> builds = new ArrayList<>();

    SavedBuildManager(Path file) {
        this.file = file;
        load();
    }

    public static SavedBuildManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public synchronized List<SavedBuild> getAll() {
        return builds.stream()
                .sorted(Comparator.comparingLong(SavedBuild::savedAt).reversed())
                .toList();
    }

    public synchronized boolean save(SavedBuild build) {
        if (build.name().isBlank() || build.characterClass().isBlank()) {
            return false;
        }
        builds.removeIf(existing ->
                normalize(existing.name()).equals(normalize(build.name()))
                        && normalize(existing.characterClass()).equals(normalize(build.characterClass())));
        builds.add(build);
        return write();
    }

    public synchronized boolean remove(SavedBuild build) {
        if (!builds.remove(build)) {
            return false;
        }
        return write();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<SavedBuild> loaded = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), BUILD_LIST_TYPE);
            if (loaded != null) {
                builds.addAll(loaded);
            }
        } catch (IOException | RuntimeException exception) {
            PlannerLog.LOGGER.warn("Failed to load saved WynnExtras planner builds.", exception);
        }
    }

    private boolean write() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(builds, BUILD_LIST_TYPE), StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            PlannerLog.LOGGER.warn("Failed to save WynnExtras planner builds.", exception);
            return false;
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class InstanceHolder {
        private static final SavedBuildManager INSTANCE = new SavedBuildManager(
                FabricLoader.getInstance().getConfigDir()
                        .resolve("wynnextras").resolve("buildplanner").resolve("saved-builds.json"));
    }
}
