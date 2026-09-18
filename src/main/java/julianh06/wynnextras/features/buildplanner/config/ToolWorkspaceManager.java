package julianh06.wynnextras.features.buildplanner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.data.AtlasState;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/** Saves by stable tab ID: a removed screen must not overwrite the newly active tool's state. */
public final class ToolWorkspaceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final List<Tab> tabs = new ArrayList<>();
    private final Map<String, CraftTarget> craftTargets = new java.util.LinkedHashMap<>();
    private String activeId;
    private String written = "";
    private String error = "";
    private boolean unreadable;

    ToolWorkspaceManager(Path file, BuilderWorkspaceManager legacy, CrafterDraftManager crafter) {
        this.file = file;
        try {
            if (Files.exists(file)) {
                WorkspaceData data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), WorkspaceData.class);
                if (data == null || data.version() != 1 || data.tabs() == null || data.tabs().isEmpty()) {
                    throw new JsonParseException("Invalid tool workspace");
                }
                List<Tab> loaded = new ArrayList<>();
                HashSet<String> ids = new HashSet<>();
                for (TabData entry : data.tabs()) {
                    if (entry == null) throw new JsonParseException("Missing tab");
                    Tab tab = new Tab(entry.id(), entry.type(), entry.name(), entry.build(), entry.craftCode(), entry.atlas());
                    if (!ids.add(tab.id())) throw new JsonParseException("Duplicate tab ID");
                    loaded.add(tab);
                }
                if (!ids.contains(data.activeId())) throw new JsonParseException("Missing active tab");
                tabs.addAll(loaded);
                activeId = data.activeId();
                if (data.craftTargets() != null) {
                    for (var entry : data.craftTargets().entrySet()) {
                        requireType(entry.getKey(), Type.CRAFTER);
                        CraftTarget target = entry.getValue();
                        if (target == null || target.buildId() == null || !validSlot(target.slot())) {
                            throw new JsonParseException("Invalid craft destination");
                        }
                        craftTargets.put(entry.getKey(), target);
                    }
                }
                written = json();
            } else {
                if (!legacy.error().isEmpty() || !crafter.error().isEmpty()) {
                    throw new JsonParseException("Original build tabs or crafter draft could not be read");
                }
                for (SavedBuild build : legacy.tabs()) {
                    tabs.add(new Tab(id(), Type.BUILDER, build.name(), build, null, null));
                }
                activeId = tabs.get(legacy.activeIndex()).id();
                // Copy the old standalone craft without modifying its file.
                if (Files.exists(file.getParent().resolve("crafter-draft.json"))) {
                    tabs.add(new Tab(id(), Type.CRAFTER, "Craft 1", null,
                            CraftedItemCodec.encode(crafter.draft()), null));
                }
                write();
            }
        } catch (IOException | JsonParseException | IllegalArgumentException exception) {
            unreadable = true;
            error = "Cannot load tool tabs. Original files preserved; changes stay in memory.";
            PlannerLog.LOGGER.error("Could not load/migrate tool workspace {}.", file, exception);
            tabs.clear();
            craftTargets.clear();
            Tab fallback = builder("Build 1", emptyBuild("Build 1"));
            tabs.add(fallback);
            activeId = fallback.id();
        }
    }

    public static ToolWorkspaceManager getInstance() { return Holder.INSTANCE; }
    public synchronized List<Tab> tabs() { return List.copyOf(tabs); }
    public synchronized String activeId() { return activeId; }
    public synchronized Tab activeTab() { return require(activeId); }
    public synchronized String error() { return error; }
    public synchronized CraftTarget craftTarget(String id) { return craftTargets.get(id); }
    public synchronized Tab find(String id) { return tabs.stream().filter(tab -> tab.id().equals(id)).findFirst().orElse(null); }

    public synchronized boolean select(String id) {
        require(id);
        activeId = id;
        return write();
    }

    public synchronized String add(Type type) {
        String prefix = switch (type) { case BUILDER -> "Build "; case CRAFTER -> "Craft "; case ATLAS -> "Atlas "; };
        int number = 1;
        while (hasName(prefix + number)) number++;
        String name = prefix + number;
        Tab tab = switch (type) {
            case BUILDER -> builder(name, emptyBuild(name));
            case CRAFTER -> new Tab(id(), type, name, null, CraftedItemCodec.encode(CraftedItemCodec.emptyCraft("bow")), null);
            case ATLAS -> new Tab(id(), type, name, null, null, AtlasState.defaults());
        };
        tabs.add(tab);
        activeId = tab.id();
        write();
        return tab.id();
    }

    public synchronized boolean updateBuild(String id, SavedBuild build) {
        Tab tab = requireType(id, Type.BUILDER);
        return replace(new Tab(id, tab.type(), build.name(), build, null, null));
    }

    public synchronized String defaultBuildName(String id) {
        requireType(id, Type.BUILDER);
        int number = 1;
        while (true) {
            String name = "Build " + number;
            if (tabs.stream().noneMatch(tab -> !tab.id().equals(id) && tab.name().equalsIgnoreCase(name))) {
                return name;
            }
            number++;
        }
    }

    public synchronized boolean updateCraft(String id, CraftedItemCodec.Craft craft) {
        Tab tab = requireType(id, Type.CRAFTER);
        return replace(new Tab(id, tab.type(), tab.name(), null, CraftedItemCodec.encode(craft), null));
    }

    public synchronized boolean updateAtlas(String id, AtlasState state) {
        Tab tab = requireType(id, Type.ATLAS);
        return replace(new Tab(id, tab.type(), tab.name(), null, null, state));
    }

    public synchronized boolean bindCraft(String id, String buildId, String slot) {
        requireType(id, Type.CRAFTER);
        requireType(buildId, Type.BUILDER);
        if (!validSlot(slot)) throw new IllegalArgumentException("Unknown equipment slot");
        craftTargets.put(id, new CraftTarget(buildId, slot));
        return write();
    }

    public synchronized boolean close(String id) {
        Tab closing = require(id);
        int index = tabs.indexOf(closing);
        tabs.remove(index);
        craftTargets.entrySet().removeIf(entry -> entry.getKey().equals(id) || entry.getValue().buildId().equals(id));
        if (tabs.isEmpty()) tabs.add(builder("Build 1", emptyBuild("Build 1")));
        if (activeId.equals(id)) activeId = tabs.get(Math.min(index, tabs.size() - 1)).id();
        return write();
    }

    private boolean replace(Tab changed) {
        tabs.set(tabs.indexOf(require(changed.id())), changed);
        return write();
    }

    private Tab require(String id) {
        Tab tab = find(id);
        if (tab == null) throw new IllegalArgumentException("The tool tab is no longer open: " + id);
        return tab;
    }

    private Tab requireType(String id, Type type) {
        Tab tab = require(id);
        if (tab.type() != type) throw new IllegalArgumentException("Wrong tool tab type");
        return tab;
    }

    private boolean hasName(String name) { return tabs.stream().anyMatch(tab -> tab.name().equalsIgnoreCase(name)); }
    private static String id() { return UUID.randomUUID().toString(); }
    private static Tab builder(String name, SavedBuild build) { return new Tab(id(), Type.BUILDER, name, build, null, null); }
    public static SavedBuild emptyBuild(String name) {
        return new SavedBuild(name, "archer", 121, Map.of(), Map.of(), new int[5], Map.of(), Map.of(), Map.of(), 0);
    }
    private String json() { return GSON.toJson(new WorkspaceData(1,
            tabs.stream().map(tab -> new TabData(tab.id(), tab.type(), tab.name(), tab.build(), tab.craftCode(), tab.atlas())).toList(),
            activeId, Map.copyOf(craftTargets))); }

    private static boolean validSlot(String slot) {
        return slot != null && List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "WEAPON",
                "RING_1", "RING_2", "BRACELET", "NECKLACE").contains(slot);
    }

    public record CraftTarget(String buildId, String slot) {
        public String slotType() {
            return slot.startsWith("RING_") ? "ring" : slot.toLowerCase(java.util.Locale.ROOT);
        }
    }

    private boolean write() {
        if (unreadable) return false;
        String json = json();
        if (json.equals(written)) { error = ""; return true; }
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), "tool-workspace-", ".tmp");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            written = json;
            error = "";
            return true;
        } catch (IOException exception) {
            error = "Could not save tool tabs. Changes stay in memory; see the log.";
            PlannerLog.LOGGER.error("Could not save tool workspace {}.", file, exception);
            return false;
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); }
                catch (IOException exception) { PlannerLog.LOGGER.warn("Could not clean tool workspace temporary file.", exception); }
            }
        }
    }

    public enum Type { BUILDER, ATLAS, CRAFTER }
    public record Tab(String id, Type type, String name, SavedBuild build, String craftCode, AtlasState atlas) {
        public Tab {
            if (id == null || id.isBlank() || type == null || name == null || name.isBlank()) {
                throw new IllegalArgumentException("Invalid tool tab");
            }
            switch (type) {
                case BUILDER -> { if (build == null || craftCode != null || atlas != null) throw new IllegalArgumentException("Invalid build tab"); }
                case CRAFTER -> {
                    if (craftCode == null || build != null || atlas != null) throw new IllegalArgumentException("Invalid crafter tab");
                    CraftedItemCodec.readCraft(craftCode);
                }
                case ATLAS -> { if (atlas == null || build != null || craftCode != null) throw new IllegalArgumentException("Invalid atlas tab"); }
            }
        }
    }
    private record TabData(String id, Type type, String name, SavedBuild build, String craftCode, AtlasState atlas) {}
    private record WorkspaceData(int version, List<TabData> tabs, String activeId, Map<String, CraftTarget> craftTargets) {}
    private static final class Holder {
        private static final ToolWorkspaceManager INSTANCE = new ToolWorkspaceManager(
                FabricLoader.getInstance().getConfigDir().resolve("wynnextras").resolve("buildplanner").resolve("tool-workspace.json"),
                BuilderWorkspaceManager.getInstance(), CrafterDraftManager.getInstance());
    }
}
