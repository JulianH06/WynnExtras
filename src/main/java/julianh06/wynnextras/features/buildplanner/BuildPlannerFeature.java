package julianh06.wynnextras.features.buildplanner;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.WynnBuilderItemIndex;
import julianh06.wynnextras.features.buildplanner.data.WynnBuilderDataFiles;
import julianh06.wynnextras.features.buildplanner.gui.WorkspaceTabs;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@WEModule
public final class BuildPlannerFeature {
    private static final AtomicBoolean FETCH_PENDING = new AtomicBoolean();
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "WynnExtras-PlannerItems");
        thread.setDaemon(true);
        return thread;
    });

    private static final Command COMMAND = new Command(
            "wynnbuilder", context -> openAfterChat(() -> open(null)));

    public BuildPlannerFeature() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                CompletableFuture.runAsync(ItemDatabase.getInstance()::fetchAll, DATABASE_EXECUTOR));
    }

    private static int openAfterChat(Runnable action) {
        MinecraftClient.getInstance().send(action);
        return 1;
    }

    public static void open(Screen parent) {
        loadPlannerData();
        MinecraftClient.getInstance().setScreen(WorkspaceTabs.open(parent));
    }

    private static void loadPlannerData() {
        ItemDatabase database = ItemDatabase.getInstance();
        WynnBuilderItemIndex index = WynnBuilderItemIndex.getInstance();
        for (CharacterClass abilityClass : CharacterClass.playableValues()) {
            AbilityTreeDatabase.getInstance().loadAsync(abilityClass);
        }
        if (!FETCH_PENDING.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture<Void> items = database.isReady()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.runAsync(database::fetchAll, DATABASE_EXECUTOR);
        CompletableFuture<Void> builderData = CompletableFuture.runAsync(() -> {
            WynnBuilderDataFiles.refreshAll();
            if (!index.isReady()) index.fetchAll();
        }, DATABASE_EXECUTOR);
        CompletableFuture.allOf(items, builderData).whenComplete((ignored, error) -> {
            FETCH_PENDING.set(false);
            if (error != null) {
                PlannerLog.LOGGER.error("Failed to initialize planner item data.", error);
            } else if (!database.isReady()) {
                PlannerLog.LOGGER.error("Planner item database unavailable: {}", database.getLastError());
            }
        });
    }
}
