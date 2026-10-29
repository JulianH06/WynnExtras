package julianh06.wynnextras.compat.wynntils;

import java.lang.reflect.Method;
import java.util.Optional;

public final class WynntilsCustomModelAdapter {
    private record Binding(Object service, Method getFloat) {}

    private static final WynntilsCapability<Binding> CUSTOM_MODELS = new WynntilsCapability<>(
            "custom-model-data", () -> {
        Class<?> services = WynntilsCompat.requireClass("com.wynntils.core.components.Services");
        Object service = services.getField("CustomModel").get(null);
        return new Binding(service, service.getClass().getMethod("getFloat", String.class));
    });

    private WynntilsCustomModelAdapter() {}

    public static Optional<Float> getFloat(String modelName) {
        if (modelName == null || modelName.isBlank()) return Optional.empty();
        return CUSTOM_MODELS.invoke(binding -> {
            Object result = binding.getFloat.invoke(binding.service, modelName);
            if (!(result instanceof Optional<?> optional)) return null;
            Object value = optional.orElse(null);
            return value instanceof Number number ? number.floatValue() : null;
        });
    }
}
