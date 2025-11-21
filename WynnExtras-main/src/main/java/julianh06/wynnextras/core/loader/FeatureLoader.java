package julianh06.wynnextras.core.loader;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.api.WEEventBus;
import org.reflections.Reflections;

import julianh06.wynnextras.core.Core;
import java.util.Set;

public class FeatureLoader implements WELoader {
    public FeatureLoader() {
        Reflections reflections = new Reflections("julianh06.wynnextras");

        Set<Class<?>> featureClasses = reflections.getTypesAnnotatedWith(WEModule.class);

        for (Class<?> clazz: featureClasses) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                WEEventBus.registerEventListener(instance);
            } catch (Exception e) {
                Core.LOGGER.logError("Failed to load module: " + clazz.getName(), e);
            }
        }
    }
}
