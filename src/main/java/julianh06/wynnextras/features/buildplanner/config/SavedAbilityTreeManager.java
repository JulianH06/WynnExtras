package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.buildplanner.BuildPlannerRuntime;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SavedAbilityTreeManager {
    private static final Type TREE_LIST_TYPE =
            new TypeToken<List<SavedAbilityTree>>() { }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final List<SavedAbilityTree> trees = new ArrayList<>();
    private boolean unreadable;

    SavedAbilityTreeManager(Path file) {
        this.file = file;
        load();
    }

    public static SavedAbilityTreeManager getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized List<SavedAbilityTree> getAll(CharacterClass abilityClass) {
        String className = abilityClass.apiName();
        return trees.stream()
                .filter(tree -> className.equalsIgnoreCase(tree.characterClass()))
                .sorted(Comparator.comparingLong(SavedAbilityTree::savedAt).reversed())
                .toList();
    }

    public synchronized boolean save(SavedAbilityTree tree) {
        if (visibleName(tree.name()).isBlank() || tree.characterClass().isBlank()) {
            return false;
        }
        trees.removeIf(existing ->
                normalize(existing.name()).equals(normalize(tree.name()))
                        && normalize(existing.characterClass())
                                .equals(normalize(tree.characterClass())));
        trees.add(tree);
        return write();
    }

    public synchronized boolean remove(SavedAbilityTree tree) {
        if (!trees.remove(tree)) {
            return false;
        }
        return write();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<SavedAbilityTree> loaded = GSON.fromJson(
                    Files.readString(file, StandardCharsets.UTF_8), TREE_LIST_TYPE);
            if (loaded != null) {
                loaded.stream()
                        .filter(tree -> tree != null
                                && !tree.name().isBlank()
                                && !tree.characterClass().isBlank())
                        .forEach(trees::add);
            }
        } catch (IOException | RuntimeException exception) {
            unreadable = true;
            PlannerLog.LOGGER.warn("Failed to load saved WynnExtras planner ability trees.", exception);
        }
    }

    private boolean write() {
        if (unreadable) return false;
        try {
            AtomicConfigWriter.write(file, GSON.toJson(trees, TREE_LIST_TYPE), "saved-ability-trees-");
            return true;
        } catch (IOException exception) {
            PlannerLog.LOGGER.warn("Failed to save WynnExtras planner ability trees.", exception);
            return false;
        }
    }

    private static String normalize(String value) {
        return visibleName(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String visibleName(String value) {
        return value == null
                ? ""
                : value.replaceAll("(?i)[&\u00a7][0-9a-fk-or]", "");
    }

    private static final class Holder {
        private static final SavedAbilityTreeManager INSTANCE =
                new SavedAbilityTreeManager(
                        BuildPlannerRuntime.configDirectory()
                                .resolve("saved-ability-trees.json"));
    }
}
