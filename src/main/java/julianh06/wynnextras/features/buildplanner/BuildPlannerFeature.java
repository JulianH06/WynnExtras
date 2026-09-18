package julianh06.wynnextras.features.buildplanner;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.gui.WorkspaceTabs;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@WEModule
public final class BuildPlannerFeature {
    private static final AtomicBoolean FETCH_PENDING = new AtomicBoolean();
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "WynnExtras-PlannerItems");
        thread.setDaemon(true);
        return thread;
    });

    private static final Command COMMAND = new Command("wynnbuilder", "Open the build planner", context -> {
        var client = context.getSource().getClient();
        // Defer opening until the command's ChatScreen has finished closing.
        client.send(() -> {
            loadItemDatabase();
            client.setScreen(WorkspaceTabs.open(null));
        });
        return 1;
    });

    public BuildPlannerFeature() {}

    private static void loadItemDatabase() {
        ItemDatabase database = ItemDatabase.getInstance();
        if (database.isReady() || !FETCH_PENDING.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.runAsync(database::fetchAll, DATABASE_EXECUTOR).whenComplete((ignored, error) -> {
            FETCH_PENDING.set(false);
            if (error != null) {
                PlannerLog.LOGGER.error("Failed to initialize planner item data.", error);
            } else if (!database.isReady()) {
                PlannerLog.LOGGER.error("Planner item database unavailable: {}", database.getLastError());
            }
        });
    }
}
