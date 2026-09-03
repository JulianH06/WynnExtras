package julianh06.wynnextras.compat.wynntils;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WynntilsItemUiAdapter {
    private record ItemBinding(Object model, Method getWynnItem) {}
    private record AnnotationBinding(Object handler, Method calculate, Method styledTextFactory) {}
    private record GuessBinding(Object feature, Class<?> gearBoxType, Method tooltipAddon) {}

    private static final WynntilsCapability<ItemBinding> ITEMS = new WynntilsCapability<>("optional-item-model", () -> {
        Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
        Object model = models.getField("Item").get(null);
        return new ItemBinding(model, model.getClass().getMethod("getWynnItem", ItemStack.class));
    });

    private static final WynntilsCapability<AnnotationBinding> ANNOTATION = new WynntilsCapability<>(
            "optional-item-annotation", () -> {
        Class<?> handlers = WynntilsCompat.requireClass("com.wynntils.core.components.Handlers");
        Object handler = handlers.getField("Item").get(null);
        Class<?> styledText = WynntilsCompat.requireClass("com.wynntils.core.text.StyledText");
        Method factory = styledText.getMethod("fromComponent", Text.class);
        Method calculate = handler.getClass().getDeclaredMethod("calculateAnnotation", ItemStack.class, styledText);
        if (!calculate.trySetAccessible()) throw new IllegalAccessException("calculateAnnotation");
        return new AnnotationBinding(handler, calculate, factory);
    });

    private static final WynntilsCapability<GuessBinding> ITEM_GUESS = new WynntilsCapability<>(
            "optional-item-guess-tooltip", () -> {
        Class<?> managers = WynntilsCompat.requireClass("com.wynntils.core.components.Managers");
        Object featureManager = managers.getField("Feature").get(null);
        Class<?> featureClass = WynntilsCompat.requireClass("com.wynntils.features.tooltips.ItemGuessFeature");
        Class<?> gearBoxType = WynntilsCompat.requireClass("com.wynntils.models.items.items.game.GearBoxItem");
        Object feature = featureManager.getClass().getMethod("getFeatureInstance", Class.class)
                .invoke(featureManager, featureClass);
        Method tooltipAddon = featureClass.getDeclaredMethod("getTooltipAddon", gearBoxType);
        if (!tooltipAddon.trySetAccessible()) throw new IllegalAccessException("getTooltipAddon");
        return new GuessBinding(feature, gearBoxType, tooltipAddon);
    });

    private WynntilsItemUiAdapter() {}

    public static Optional<WynntilsBankAdapter.AnnotationHandle> calculateAnnotation(ItemStack stack, Text name) {
        return ANNOTATION.invoke(binding -> {
            Object value = binding.calculate.invoke(binding.handler, stack,
                    binding.styledTextFactory.invoke(null, name));
            return value == null ? null : new WynntilsBankAdapter.AnnotationHandle(value);
        });
    }

    public static List<Text> getItemGuessTooltip(ItemStack stack) {
        Object gearBox = getInternalItem(stack).orElse(null);
        if (gearBox == null) return List.of();
        return ITEM_GUESS.invoke(binding -> {
            if (!binding.gearBoxType.isInstance(gearBox)) return List.<Text>of();
            Object result = binding.tooltipAddon.invoke(binding.feature, gearBox);
            if (!(result instanceof List<?> values)) return List.<Text>of();
            List<Text> tooltip = new ArrayList<>(values.size());
            for (Object value : values) if (value instanceof Text text) tooltip.add(text);
            return tooltip;
        }).orElseGet(List::of);
    }

    static Optional<Object> getInternalItem(ItemStack stack) {
        return ITEMS.invoke(binding -> {
            Object result = binding.getWynnItem.invoke(binding.model, stack);
            return result instanceof Optional<?> optional ? optional.orElse(null) : null;
        });
    }

    static boolean isInternalType(Object item, String simpleName) {
        if (item == null) return false;
        for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().equals(simpleName)) return true;
            for (Class<?> iface : type.getInterfaces()) if (iface.getSimpleName().equals(simpleName)) return true;
        }
        return false;
    }
}
