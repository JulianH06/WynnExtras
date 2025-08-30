package julianh06.wynnextras.core;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.command.Command;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Optional;

public class Core {
    public static WELogger LOGGER;

    public static Optional<ModContainer> INSTANCE;
    public static String NAME;
    public static String VERSION;
    public static boolean IS_DEV;

    public static boolean init(String modId) {
        INSTANCE = FabricLoader.getInstance().getModContainer(modId);
        LOGGER = new WELogger(modId);

        if (INSTANCE.isEmpty()) {
            LOGGER.logError("Failed to find mod container.");
            return false;
        }

        NAME = INSTANCE.get().getMetadata().getName();
        VERSION = INSTANCE.get().getMetadata().getVersion().getFriendlyString();
        IS_DEV = VERSION.contains("dev");

        SimpleConfig.register(WynnExtrasConfig.class);

        return true;
    }

    public boolean isDev() {
        return IS_DEV;
    }
}
