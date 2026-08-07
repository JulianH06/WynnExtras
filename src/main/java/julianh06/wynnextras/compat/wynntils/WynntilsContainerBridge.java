package julianh06.wynnextras.compat.wynntils;

import java.lang.reflect.Method;
import java.util.Optional;

public final class WynntilsContainerBridge {
    private record Binding(Object model, Method currentContainer) {}

    private static final WynntilsCapability<Binding> CURRENT_CONTAINER = new WynntilsCapability<>(
            "current-container",
            () -> {
                Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
                Object model = models.getField("Container").get(null);
                return new Binding(model, model.getClass().getMethod("getCurrentContainer"));
            }
    );

    private WynntilsContainerBridge() {}

    public static Optional<Object> current() {
        return CURRENT_CONTAINER.invoke(binding -> binding.currentContainer.invoke(binding.model));
    }

    public static boolean isCurrent(String simpleClassName) {
        return current().map(container -> container.getClass().getSimpleName().equals(simpleClassName)).orElse(false);
    }
}
