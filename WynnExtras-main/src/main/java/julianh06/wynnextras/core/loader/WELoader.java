package julianh06.wynnextras.core.loader;

import julianh06.wynnextras.event.api.WEEventBus;
import org.reflections.Reflections;

import java.util.Set;

public interface WELoader {
    static void loadAll() {
        Reflections reflections = new Reflections("julianh06.wynnextras.core.loader");

        Set<Class<? extends WELoader>> loaderClasses = reflections.getSubTypesOf(WELoader.class);

        for (Class<? extends WELoader> clazz : loaderClasses) {
            try {
                WELoader loader = clazz.getDeclaredConstructor().newInstance();
                WEEventBus.registerEventListener(loader);
            } catch (Exception e) {
                System.err.println("Failed to load WELoader: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}
