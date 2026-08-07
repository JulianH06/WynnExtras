package julianh06.wynnextras.compat.wynntils;

import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public final class WynntilsItemBridge {
    private record AnnotationMethods(Object handler, Method calculate, Method styledTextFactory) {}
    private record HighlightMethods(Object feature, Method getColor, Method red, Method green, Method blue, Method alpha) {}
    private record GuessMethods(Object feature, Class<?> gearBoxType, Method tooltipAddon) {}

    private static final WynntilsCapability<AnnotationMethods> ANNOTATION = new WynntilsCapability<>(
            "item-annotation",
            () -> {
                Class<?> handlers = WynntilsCompat.requireClass("com.wynntils.core.components.Handlers");
                Object handler = handlers.getField("Item").get(null);
                Class<?> styledText = WynntilsCompat.requireClass("com.wynntils.core.text.StyledText");
                Method factory = styledText.getMethod("fromComponent", Text.class);
                Method calculate = handler.getClass().getDeclaredMethod("calculateAnnotation", ItemStack.class, styledText);
                if (!calculate.trySetAccessible()) throw new IllegalAccessException("calculateAnnotation");
                return new AnnotationMethods(handler, calculate, factory);
            }
    );

    private static final WynntilsCapability<HighlightMethods> HIGHLIGHT = new WynntilsCapability<>(
            "item-highlight-color",
            () -> {
                Class<?> managers = WynntilsCompat.requireClass("com.wynntils.core.components.Managers");
                Object featureManager = managers.getField("Feature").get(null);
                Class<?> featureClass = WynntilsCompat.requireClass("com.wynntils.features.inventory.ItemHighlightFeature");
                Object feature = featureManager.getClass().getMethod("getFeatureInstance", Class.class)
                        .invoke(featureManager, featureClass);
                Method getColor = featureClass.getDeclaredMethod("getHighlightColor", ItemStack.class, boolean.class);
                if (!getColor.trySetAccessible()) throw new IllegalAccessException("getHighlightColor");
                Class<?> color = WynntilsCompat.requireClass("com.wynntils.utils.colors.CustomColor");
                return new HighlightMethods(feature, getColor, color.getMethod("r"), color.getMethod("g"),
                        color.getMethod("b"), color.getMethod("a"));
            }
    );

    private static final WynntilsCapability<GuessMethods> ITEM_GUESS = new WynntilsCapability<>(
            "item-guess-tooltip",
            () -> {
                Class<?> managers = WynntilsCompat.requireClass("com.wynntils.core.components.Managers");
                Object featureManager = managers.getField("Feature").get(null);
                Class<?> featureClass = WynntilsCompat.requireClass("com.wynntils.features.tooltips.ItemGuessFeature");
                Class<?> gearBoxType = WynntilsCompat.requireClass("com.wynntils.models.items.items.game.GearBoxItem");
                Object feature = featureManager.getClass().getMethod("getFeatureInstance", Class.class)
                        .invoke(featureManager, featureClass);
                Method tooltipAddon = featureClass.getDeclaredMethod("getTooltipAddon", gearBoxType);
                if (!tooltipAddon.trySetAccessible()) throw new IllegalAccessException("getTooltipAddon");
                return new GuessMethods(feature, gearBoxType, tooltipAddon);
            }
    );

    private WynntilsItemBridge() {}

    public static Optional<Object> calculateAnnotation(ItemStack stack, Text name) {
        return ANNOTATION.invoke(methods -> {
            Object styledName = methods.styledTextFactory.invoke(null, name);
            return methods.calculate.invoke(methods.handler, stack, styledName);
        });
    }

    public static CustomColor getHighlightColor(ItemStack stack) {
        return HIGHLIGHT.invoke(methods -> {
            Object color = methods.getColor.invoke(methods.feature, stack, false);
            if (color == null) return CustomColor.NONE;
            return new CustomColor(
                    ((Number) methods.red.invoke(color)).intValue(),
                    ((Number) methods.green.invoke(color)).intValue(),
                    ((Number) methods.blue.invoke(color)).intValue(),
                    ((Number) methods.alpha.invoke(color)).intValue()
            );
        }).orElse(CustomColor.NONE);
    }

    public static List<Text> getItemGuessTooltip(Object gearBox) {
        if (gearBox == null) return List.of();
        return ITEM_GUESS.invoke(methods -> {
            if (!methods.gearBoxType.isInstance(gearBox)) return List.<Text>of();
            Object result = methods.tooltipAddon.invoke(methods.feature, gearBox);
            if (!(result instanceof List<?> values)) return List.<Text>of();
            List<Text> tooltip = new ArrayList<>(values.size());
            for (Object value : values) if (value instanceof Text text) tooltip.add(text);
            return tooltip;
        }).orElseGet(List::of);
    }
}
