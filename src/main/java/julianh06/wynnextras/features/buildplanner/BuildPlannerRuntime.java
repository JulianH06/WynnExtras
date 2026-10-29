package julianh06.wynnextras.features.buildplanner;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Shared runtime paths for the build-planner tools. */
public final class BuildPlannerRuntime {
    private BuildPlannerRuntime() {}

    public static Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras").resolve("buildplanner-merged");
    }
}
