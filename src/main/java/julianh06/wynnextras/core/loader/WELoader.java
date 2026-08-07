package julianh06.wynnextras.core.loader;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.api.WEEventBus;
import org.reflections.Reflections;

import java.util.Set;

public interface WELoader {
    static void loadAll() {
        Set<Class<? extends WELoader>> loaderClasses;
        try {
            Reflections reflections = new Reflections("julianh06.wynnextras.core.loader");
            loaderClasses = reflections.getSubTypesOf(WELoader.class);
        } catch (Throwable throwable) {
            WynnExtras.LOGGER.error("Failed to discover WynnExtras loaders", throwable);
            return;
        }

        for (Class<? extends WELoader> clazz : loaderClasses) {
            try {
                WELoader loader = clazz.getDeclaredConstructor().newInstance();
                WEEventBus.registerEventListener(loader);
            } catch (Throwable throwable) {
                WynnExtras.LOGGER.error("Failed to load WELoader: " + clazz.getName(), throwable);
            }
        }
    }
}
