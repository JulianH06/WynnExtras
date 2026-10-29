package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/** Session navigation between tool tabs. Build edits deliberately do not create history entries. */
final class WorkspaceNavigation {
    private static final int LIMIT = 100;
    private static final Deque<String> BACK = new ArrayDeque<>();
    private static final Deque<String> FORWARD = new ArrayDeque<>();

    private WorkspaceNavigation() {}

    static boolean canGoBack() { return hasExisting(BACK); }
    static boolean canGoForward() { return hasExisting(FORWARD); }

    static void open(Screen parent, String id) {
        var workspace = ToolWorkspaceManager.getInstance();
        if (workspace.find(id) == null || Objects.equals(workspace.activeId(), id)) return;
        if (workspace.activeId() != null) BACK.addLast(workspace.activeId());
        while (BACK.size() > LIMIT) BACK.removeFirst();
        FORWARD.clear();
        workspace.select(id);
        MinecraftClient.getInstance().setScreen(WorkspaceTabs.open(parent));
    }

    static void add(Screen parent, ToolWorkspaceManager.Type type) {
        var workspace = ToolWorkspaceManager.getInstance();
        String previous = workspace.activeId();
        workspace.add(type);
        if (previous != null) BACK.addLast(previous);
        while (BACK.size() > LIMIT) BACK.removeFirst();
        FORWARD.clear();
        MinecraftClient.getInstance().setScreen(WorkspaceTabs.open(parent));
    }

    static void show(Screen parent, ToolWorkspaceManager.Type type) {
        var workspace = ToolWorkspaceManager.getInstance();
        if (workspace.activeType() == type) return;
        if (workspace.activeId() != null) BACK.addLast(workspace.activeId());
        while (BACK.size() > LIMIT) BACK.removeFirst();
        FORWARD.clear();
        workspace.showType(type);
        MinecraftClient.getInstance().setScreen(WorkspaceTabs.open(parent));
    }

    static void back(Screen parent) { move(parent, BACK, FORWARD); }
    static void forward(Screen parent) { move(parent, FORWARD, BACK); }

    private static void move(Screen parent, Deque<String> source, Deque<String> destination) {
        var workspace = ToolWorkspaceManager.getInstance();
        while (!source.isEmpty()) {
            String id = source.removeLast();
            if (workspace.find(id) == null || Objects.equals(workspace.activeId(), id)) continue;
            if (workspace.activeId() != null) destination.addLast(workspace.activeId());
            workspace.select(id);
            MinecraftClient.getInstance().setScreen(WorkspaceTabs.open(parent));
            return;
        }
    }

    private static boolean hasExisting(Deque<String> history) {
        var workspace = ToolWorkspaceManager.getInstance();
        return history.stream().anyMatch(id -> workspace.find(id) != null
                && !Objects.equals(workspace.activeId(), id));
    }
}
