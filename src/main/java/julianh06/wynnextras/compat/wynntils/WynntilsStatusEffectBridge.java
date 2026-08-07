package julianh06.wynnextras.compat.wynntils;

import java.lang.reflect.Method;
import java.util.List;

public final class WynntilsStatusEffectBridge {
    private record Binding(Object model, Method effects) {}

    private static final WynntilsCapability<Binding> STATUS_EFFECTS = new WynntilsCapability<>(
            "status-effects",
            () -> {
                Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
                Object model = models.getField("StatusEffect").get(null);
                return new Binding(model, model.getClass().getMethod("getStatusEffects"));
            }
    );

    private WynntilsStatusEffectBridge() {}

    public static boolean hasEffect(String expectedName) {
        return STATUS_EFFECTS.invoke(binding -> {
            Object result = binding.effects.invoke(binding.model);
            if (!(result instanceof List<?> effects)) return false;
            for (Object effect : effects) {
                if (effect == null) continue;
                Object name = effect.getClass().getMethod("getName").invoke(effect);
                if (name == null) continue;
                String plain = String.valueOf(name.getClass().getMethod("getStringWithoutFormatting").invoke(name));
                if (expectedName.equals(plain)) return true;
            }
            return false;
        }).orElse(false);
    }
}
