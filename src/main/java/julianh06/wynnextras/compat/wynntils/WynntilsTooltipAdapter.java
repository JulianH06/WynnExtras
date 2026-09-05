package julianh06.wynnextras.compat.wynntils;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class WynntilsTooltipAdapter {
    private record Binding(Class<?> eventClass, Method getItemStack, Method getTooltips, Method setTooltips) {}
    private record ChatItemBinding(Class<?> fakeStackType, Class<?> namedItemType, Class<?> tieredItemType,
                                   Method getName, Method getTier) {}

    private static final WynntilsCapability<Binding> EVENT = new WynntilsCapability<>("item-tooltip-event", () -> {
        Class<?> eventClass = WynntilsCompat.requireClass("com.wynntils.mc.event.ItemTooltipRenderEvent$Pre");
        return new Binding(eventClass, eventClass.getMethod("getItemStack"), eventClass.getMethod("getTooltips"),
                eventClass.getMethod("setTooltips", List.class));
    });
    private static final WynntilsCapability<ChatItemBinding> CHAT_ITEM = new WynntilsCapability<>("chat-item-tooltip", () -> {
        Class<?> fakeStackType = WynntilsCompat.requireClass("com.wynntils.models.items.FakeItemStack");
        Class<?> namedItemType = WynntilsCompat.requireClass("com.wynntils.models.items.properties.NamedItemProperty");
        Class<?> tieredItemType = WynntilsCompat.requireClass("com.wynntils.models.items.properties.GearTierItemProperty");
        return new ChatItemBinding(fakeStackType, namedItemType, tieredItemType,
                namedItemType.getMethod("getName"), tieredItemType.getMethod("getGearTier"));
    });

    private WynntilsTooltipAdapter() {}

    public static void initialize() {
        CHAT_ITEM.isAvailable();
    }

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

    public static ItemStack getItemStack(Object event) {
        return EVENT.invoke(binding -> {
            if (!binding.eventClass.isInstance(event)) return null;
            Object result = binding.getItemStack.invoke(event);
            return result instanceof ItemStack stack ? stack : null;
        }).orElse(null);
    }

    public static void setTooltips(Object event, List<Text> tooltips) {
        EVENT.run(binding -> {
            if (binding.eventClass.isInstance(event)) binding.setTooltips.invoke(event, tooltips);
            return null;
        });
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

    public static String getSharedMythicName(ItemStack stack, Object item) {
        if (stack == null || item == null) return null;
        return CHAT_ITEM.invoke(binding -> {
            if (!binding.fakeStackType.isInstance(stack)
                    || !binding.namedItemType.isInstance(item)
                    || !binding.tieredItemType.isInstance(item)) return null;
            Object tier = binding.getTier.invoke(item);
            if (!(tier instanceof Enum<?> enumTier) || !enumTier.name().equals("MYTHIC")) return null;
            Object name = binding.getName.invoke(item);
            return name instanceof String string ? string : null;
        }).orElse(null);
    }
}
