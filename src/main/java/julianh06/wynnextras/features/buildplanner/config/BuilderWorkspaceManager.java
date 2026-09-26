package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persists open draft tabs and the active tab in wynnextras/buildplanner/builder-workspace.json,
 * independently of named saved builds. Failed writes retain the in-memory drafts;
 * unreadable files are never replaced automatically.
 * Retained as the legacy migration source; live tool tabs use ToolWorkspaceManager.
 */
public final class BuilderWorkspaceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Workspace workspace = new Workspace(1, List.of(emptyBuild("Build 1")), 0);
    private String writtenJson = "";
    private String error = "";
    private boolean unreadable;

    BuilderWorkspaceManager(Path file) {
        this.file = file;
        if (!Files.exists(file)) {
            return;
        }
        try {
            WorkspaceData loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), WorkspaceData.class);
            if (loaded == null) {
                throw new JsonParseException("Empty workspace");
            }
            workspace = new Workspace(loaded.version(), loaded.tabs(), loaded.activeIndex());
            writtenJson = GSON.toJson(workspace);
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            unreadable = true;
            error = "Cannot read planner tabs. Original file preserved; changes stay in memory.";
            PlannerLog.LOGGER.error("Failed to load WynnBuilder workspace; refusing to overwrite {}.", file, exception);
        }
    }

    public static BuilderWorkspaceManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public synchronized List<SavedBuild> tabs() {
        return workspace.tabs();
    }

    public synchronized int activeIndex() {
        return workspace.activeIndex();
    }

    public synchronized SavedBuild activeBuild() {
        return workspace.tabs().get(workspace.activeIndex());
    }

    public synchronized String error() {
        return error;
    }

    public synchronized boolean updateActive(SavedBuild build) {
        List<SavedBuild> tabs = new ArrayList<>(workspace.tabs());
        tabs.set(workspace.activeIndex(), build);
        workspace = new Workspace(1, tabs, workspace.activeIndex());
        return write();
    }

    public synchronized boolean select(int index) {
        workspace = new Workspace(1, workspace.tabs(), index);
        return write();
    }

    public synchronized boolean addTab() {
        List<SavedBuild> tabs = new ArrayList<>(workspace.tabs());
        int number = 1;
        while (hasName(tabs, "Build " + number)) {
            number++;
        }
        tabs.add(emptyBuild("Build " + number));
        workspace = new Workspace(1, tabs, tabs.size() - 1);
        return write();
    }

    public synchronized boolean closeActiveTab() {
        List<SavedBuild> tabs = new ArrayList<>(workspace.tabs());
        tabs.remove(workspace.activeIndex());
        if (tabs.isEmpty()) {
            tabs.add(emptyBuild("Build 1"));
        }
        workspace = new Workspace(1, tabs, Math.min(workspace.activeIndex(), tabs.size() - 1));
        return write();
    }

    private boolean write() {
        if (unreadable) {
            return false;
        }
        String json = GSON.toJson(workspace);
        if (json.equals(writtenJson)) {
            error = "";
            return true;
        }
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "builder-workspace-", ".tmp");
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            writtenJson = json;
            error = "";
            return true;
        } catch (IOException exception) {
            error = "Could not save planner tabs. Drafts stay in memory; see the log.";
            PlannerLog.LOGGER.error("Failed to save WynnBuilder workspace {}.", file, exception);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    PlannerLog.LOGGER.warn("Could not remove temporary workspace file {}.", temporary, exception);
                }
            }
        }
    }

    private static boolean hasName(List<SavedBuild> tabs, String name) {
        return tabs.stream().anyMatch(build -> build.name().equalsIgnoreCase(name));
    }

    private static SavedBuild emptyBuild(String name) {
        return new SavedBuild(name, "archer", 121, Map.of(), Map.of(), new int[5],
                Map.of(), Map.of(), Map.of(), 0);
    }

    private record Workspace(int version, List<SavedBuild> tabs, int activeIndex) {
        private Workspace {
            if (version != 1 || tabs == null || tabs.isEmpty()
                    || tabs.stream().anyMatch(java.util.Objects::isNull)
                    || activeIndex < 0 || activeIndex >= tabs.size()) {
                throw new IllegalArgumentException("Invalid planner workspace");
            }
            tabs = List.copyOf(tabs);
        }
    }

    // Validate after deserialization so Gson cannot wrap Workspace's validation exceptions.
    private record WorkspaceData(int version, List<SavedBuild> tabs, int activeIndex) {
    }

    private static final class InstanceHolder {
        private static final BuilderWorkspaceManager INSTANCE = new BuilderWorkspaceManager(
                FabricLoader.getInstance().getConfigDir().resolve("wynnextras").resolve("buildplanner").resolve("builder-workspace.json"));
    }
}
