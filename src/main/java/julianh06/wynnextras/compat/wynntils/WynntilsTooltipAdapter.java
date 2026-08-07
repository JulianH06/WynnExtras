package julianh06.wynnextras.compat.wynntils;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class WynntilsTooltipAdapter {
    private record Binding(Class<?> eventClass, Method getTooltips, Method setTooltips) {}
    private record TooltipBinding(Method method, Class<?> itemType) {}

    private static final WynntilsCapability<Binding> EVENT = new WynntilsCapability<>("item-tooltip-event", () -> {
        Class<?> eventClass = WynntilsCompat.requireClass("com.wynntils.mc.event.ItemTooltipRenderEvent$Pre");
        return new Binding(eventClass, eventClass.getMethod("getTooltips"), eventClass.getMethod("setTooltips", List.class));
    });
    private static final WynntilsCapability<TooltipBinding> TOOLTIP = new WynntilsCapability<>("item-tooltip", () -> {
        Class<?> utility = WynntilsCompat.requireClass("com.wynntils.utils.mc.TooltipUtils");
        Class<?> itemType = WynntilsCompat.requireClass("com.wynntils.models.items.WynnItem");
        return new TooltipBinding(utility.getMethod("getWynnItemTooltip", ItemStack.class, itemType), itemType);
    });

    private WynntilsTooltipAdapter() {}

    public static List<Text> getTooltips(Object event) {
        return EVENT.invoke(binding -> {
            if (!binding.eventClass.isInstance(event)) return List.<Text>of();
            Object result = binding.getTooltips.invoke(event);
            if (!(result instanceof List<?> values)) return List.<Text>of();
            List<Text> texts = new ArrayList<>(values.size());
            for (Object value : values) if (value instanceof Text text) texts.add(text);
            return texts;
        }).orElseGet(List::of);
    }

    public static void setTooltips(Object event, List<Text> tooltips) {
        EVENT.run(binding -> {
            if (binding.eventClass.isInstance(event)) binding.setTooltips.invoke(event, tooltips);
            return null;
        });
    }

    public static List<Text> getWynnItemTooltip(ItemStack stack, Object item) {
        if (stack == null || item == null) return List.of();
        return TOOLTIP.invoke(binding -> {
            if (!binding.itemType.isInstance(item)) return List.<Text>of();
            Object result = binding.method.invoke(null, stack, item);
            if (!(result instanceof List<?> values)) return List.<Text>of();
            List<Text> texts = new ArrayList<>(values.size());
            for (Object value : values) if (value instanceof Text text) texts.add(text);
            return texts;
        }).orElseGet(List::of);
    }

    public static boolean isItemStatInfoEnabled() {
        try {
            Class<?> featureClass = WynntilsCompat.requireClass("com.wynntils.features.tooltips.ItemStatInfoFeature");
            Class<?> managers = WynntilsCompat.requireClass("com.wynntils.core.components.Managers");
            Object manager = managers.getField("Feature").get(null);
            Object feature = manager.getClass().getMethod("getFeatureInstance", Class.class).invoke(manager, featureClass);
            return feature != null && (boolean) feature.getClass().getMethod("isEnabled").invoke(feature);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
