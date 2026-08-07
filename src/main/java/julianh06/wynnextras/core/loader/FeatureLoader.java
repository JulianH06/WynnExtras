package julianh06.wynnextras.core.loader;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.api.WEEventBus;
import org.reflections.Reflections;

import julianh06.wynnextras.core.Core;
import java.util.Set;

public class FeatureLoader implements WELoader {
    public FeatureLoader() {
        Set<Class<?>> featureClasses;
        try {
            Reflections reflections = new Reflections("julianh06.wynnextras");
            featureClasses = reflections.getTypesAnnotatedWith(WEModule.class);
        } catch (Throwable throwable) {
            Core.LOGGER.logError("Failed to discover WynnExtras modules", throwable);
            return;
        }

        for (Class<?> clazz: featureClasses) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                WEEventBus.registerEventListener(instance);
            } catch (Throwable throwable) {
                Core.LOGGER.logError("Failed to load module: " + clazz.getName(), throwable);
            }
        }
    }
}
