package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.buildplanner.BuildPlannerRuntime;
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

public final class SavedBuildManager {
    private static final Type BUILD_LIST_TYPE = new TypeToken<List<SavedBuild>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final List<SavedBuild> builds = new ArrayList<>();
    private boolean unreadable;

    SavedBuildManager(Path file) {
        this.file = file;
        load();
    }

    public static SavedBuildManager getInstance() {
        return Holder.INSTANCE;
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

    public synchronized boolean contains(String name, String characterClass) {
        return builds.stream().anyMatch(build -> normalize(build.name()).equals(normalize(name))
                && normalize(build.characterClass()).equals(normalize(characterClass)));
    }

    public synchronized boolean remove(SavedBuild build) {
        if (!builds.remove(build)) {
            return false;
        }
        return write();
    }

    public synchronized SavedBuild rename(SavedBuild build, String name) {
        String cleaned = name == null ? "" : name.trim();
        int index = builds.indexOf(build);
        if (index < 0 || cleaned.isEmpty() || cleaned.length() > 64
                || builds.stream().anyMatch(existing -> existing != build
                        && normalize(existing.name()).equals(normalize(cleaned))
                        && normalize(existing.characterClass()).equals(normalize(build.characterClass())))) {
            return null;
        }
        SavedBuild renamed = new SavedBuild(cleaned, build.characterClass(), build.level(),
                build.equipment(), build.powders(), build.offhands(), build.assignedSkills(), build.tomes(),
                build.aspects(), build.boosts(), build.abilityTrees(), build.savedAt());
        builds.set(index, renamed);
        write();
        return renamed;
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            var entries = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
            for (var entry : entries) {
                try {
                    SavedBuild build = GSON.fromJson(entry, SavedBuild.class);
                    if (build == null || build.name().isBlank() || build.characterClass().isBlank()) {
                        throw new IllegalArgumentException("Invalid saved build");
                    }
                    builds.add(build);
                } catch (RuntimeException exception) {
                    unreadable = true;
                    PlannerLog.LOGGER.warn("Could not read a saved build; preserving the original file.", exception);
                }
            }
        } catch (IOException | RuntimeException exception) {
            unreadable = true;
            PlannerLog.LOGGER.warn("Failed to load saved WynnExtras planner builds.", exception);
        }
    }

    private boolean write() {
        if (unreadable) return false;
        try {
            AtomicConfigWriter.write(file, GSON.toJson(builds, BUILD_LIST_TYPE), "saved-builds-");
            return true;
        } catch (IOException exception) {
            PlannerLog.LOGGER.warn("Failed to save WynnExtras planner builds.", exception);
            return false;
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Holder {
        private static final SavedBuildManager INSTANCE = new SavedBuildManager(
                BuildPlannerRuntime.configDirectory().resolve("saved-builds.json"));
    }
}
