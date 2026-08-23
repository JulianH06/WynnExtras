package julianh06.wynnextras.compat.wynntils;

import julianh06.wynnextras.utils.render.Texture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WynntilsBankAdapter {
    public record EmeraldUnit(String symbol, ItemStack stack) {}
    public record CrafterBag(String raid, String tier) {}
    public record ConsumableRender(int count, boolean renderOne) {}
    public record FeatureHandle(Object value) {}
    public record StorageHandle(Object value) {}
    public record AnnotationHandle(Object value) {}
    private record ModelsBinding(Object bank, Method getCurrentPage, Object emerald, Method getAmount) {}
    private record FeaturesBinding(Object manager, Method getFeatureInstance) {}
    private record EmeraldBinding(Object[] units, Method symbol, Method stack) {}
    private record ColorBinding(Method red, Method green, Method blue, Method alpha) {}
    private record FeatureMethodKey(Class<?> type, String name, int parameters) {}

    private static final Map<FeatureMethodKey, Optional<Method>> FEATURE_METHODS = new ConcurrentHashMap<>();

    private static final WynntilsCapability<ModelsBinding> MODELS = new WynntilsCapability<>("bank-models", () -> {
        Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
        Object bank = models.getField("Bank").get(null);
        Object emerald = models.getField("Emerald").get(null);
        return new ModelsBinding(bank, bank.getClass().getMethod("getCurrentPage"), emerald,
                emerald.getClass().getMethod("getAmountInContainer"));
    });
    private static final WynntilsCapability<FeaturesBinding> FEATURES = new WynntilsCapability<>("feature-manager", () -> {
        Class<?> managers = WynntilsCompat.requireClass("com.wynntils.core.components.Managers");
        Object manager = managers.getField("Feature").get(null);
        return new FeaturesBinding(manager, manager.getClass().getMethod("getFeatureInstance", Class.class));
    });
    private static final WynntilsCapability<EmeraldBinding> EMERALD_UNITS = new WynntilsCapability<>("emerald-units", () -> {
        Class<? extends Enum> type = WynntilsCompat.requireClass(
                "com.wynntils.models.emeralds.type.EmeraldUnits").asSubclass(Enum.class);
        return new EmeraldBinding(type.getEnumConstants(), type.getMethod("getSymbol"), type.getMethod("getItemStack"));
    });
    private static final WynntilsCapability<ColorBinding> COLORS = new WynntilsCapability<>("bank-highlight-colors", () -> {
        Class<?> type = WynntilsCompat.requireClass("com.wynntils.utils.colors.CustomColor");
        return new ColorBinding(type.getMethod("r"), type.getMethod("g"), type.getMethod("b"), type.getMethod("a"));
    });

    private WynntilsBankAdapter() {}

    public static boolean isPersonalStorageWidget(Object element) {
        return element != null && element.getClass().getName().equals(
                "com.wynntils.screens.container.widgets.PersonalStorageUtilitiesWidget");
    }

    public static StorageHandle storage(Object value) {
        return value == null ? null : new StorageHandle(value);
    }

    public static Optional<FeatureHandle> getFeature(String simpleClassName) {
        return FEATURES.invoke(binding -> {
            Class<?> type = null;
            for (String packageName : List.of("com.wynntils.features.inventory.", "com.wynntils.features.tooltips.")) {
                try {
                    type = WynntilsCompat.requireClass(packageName + simpleClassName);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (type == null) return null;
            Object feature = binding.getFeatureInstance.invoke(binding.manager, type);
            if (feature != null) {
                cacheFeatureMethod(feature.getClass(), "isEnabled", 0);
                switch (simpleClassName) {
                    case "ItemHighlightFeature" -> cacheFeatureMethod(feature.getClass(), "getHighlightColor", 2);
                    case "ItemTextOverlayFeature" -> cacheFeatureMethod(feature.getClass(), "drawTextOverlay", 5);
                    case "UnidentifiedItemIconFeature" -> cacheFeatureMethod(feature.getClass(), "drawIcon", 5);
                    case "ItemFavoriteFeature" -> cacheFeatureMethod(feature.getClass(), "isFavorited", 1);
                    case "DurabilityOverlayFeature" -> {
                        cacheFeatureMethod(feature.getClass(), "drawDurability", 4);
                        cacheFeatureMethod(feature.getClass(), "drawDurabilityArc", 4);
                        cacheFeatureMethod(feature.getClass(), "drawDurabilityBar", 4);
                        cacheFeatureMethod(feature.getClass(), "drawDurabilityPercentage", 4);
                    }
                    case "EmeraldPouchFillArcFeature" -> cacheFeatureMethod(feature.getClass(), "drawFilledArc", 4);
                    case "InventoryEmeraldCountFeature" ->
                            cacheFeatureMethod(feature.getClass(), "getRenderableEmeraldAmounts", 1);
                }
            }
            return feature == null ? null : new FeatureHandle(feature);
        });
    }

    public static Texture getConfiguredHighlightTexture() {
        FeatureHandle handle = getFeature("ItemHighlightFeature").orElse(null);
        Object feature = handle == null ? null : handle.value;
        if (feature == null) return Texture.HIGHLIGHT_WYNN;
        try {
            Object option = feature.getClass().getMethod("getConfigOptionFromString", String.class)
                    .invoke(feature, "highlightTexture");
            Object config = option instanceof Optional<?> optional ? optional.orElse(null) : null;
            if (config == null) return Texture.HIGHLIGHT_WYNN;
            Object highlight = config.getClass().getMethod("get").invoke(config);
            if (highlight instanceof Enum<?> value) {
                try {
                    return Texture.valueOf("HIGHLIGHT_" + value.name());
                } catch (IllegalArgumentException ignored) {}
            }
            Object texture = highlight.getClass().getMethod("texture").invoke(highlight);
            if (texture instanceof Enum<?> value) return Texture.valueOf(value.name());
        } catch (Throwable ignored) {}
        return Texture.HIGHLIGHT_WYNN;
    }

    public static boolean isEnabled(FeatureHandle feature) {
        Object value = invokeFeature(feature, "isEnabled");
        return value instanceof Boolean enabled && enabled;
    }

    private static Object configValue(FeatureHandle feature, String name) {
        Object optional = invoke(feature == null ? null : feature.value, "getConfigOptionFromString", name);
        if (!(optional instanceof Optional<?> first) || first.isEmpty()) return null;
        Object setting = first.get();
        return invoke(setting, "get");
    }

    public static boolean booleanConfig(FeatureHandle feature, String name) {
        return configValue(feature, name) instanceof Boolean value && value;
    }

    public static String enumConfigName(FeatureHandle feature, String name, String fallback) {
        Object value = configValue(feature, name);
        return value instanceof Enum<?> enumValue ? enumValue.name() : fallback;
    }

    public static boolean drawFeature(FeatureHandle feature, String method, DrawContext context, ItemStack stack,
                                      int x, int y, Object... extra) {
        List<Object> args = new ArrayList<>(List.of(context, stack, x, y));
        for (Object value : extra) args.add(value);
        return invokeFeatureVoid(feature, method, args.toArray());
    }

    public static boolean isFavorited(FeatureHandle feature, ItemStack stack) {
        Object result = invokeFeature(feature, "isFavorited", stack);
        return result instanceof Boolean value && value;
    }

    public static Optional<julianh06.wynnextras.utils.colors.CustomColor> getHighlightColor(
            FeatureHandle feature, ItemStack stack) {
        Object color = invokeFeature(feature, "getHighlightColor", stack, false);
        if (color == null) return Optional.empty();
        return COLORS.invoke(binding -> {
            int red = ((Number) binding.red.invoke(color)).intValue();
            int green = ((Number) binding.green.invoke(color)).intValue();
            int blue = ((Number) binding.blue.invoke(color)).intValue();
            int alpha = ((Number) binding.alpha.invoke(color)).intValue();
            return Optional.of(new julianh06.wynnextras.utils.colors.CustomColor(red, green, blue, alpha));
        }).orElseGet(Optional::empty);
    }

    public static void setLastPage(StorageHandle handle, int page) {
        Object feature = handle == null ? null : handle.value;
        if (feature == null) return;
        try {
            Field field = feature.getClass().getDeclaredField("lastPage");
            if (field.trySetAccessible()) field.setInt(feature, page);
        } catch (Throwable ignored) {}
    }

    public static int getCurrentPage() {
        return MODELS.invoke(binding -> ((Number) binding.getCurrentPage.invoke(binding.bank)).intValue()).orElse(-1);
    }

    public static int getEmeraldAmount() {
        return MODELS.invoke(binding -> ((Number) binding.getAmount.invoke(binding.emerald)).intValue()).orElse(0);
    }

    public static String[] getRenderableEmeraldAmounts(FeatureHandle feature, int amount) {
        Object result = invokeFeature(feature, "getRenderableEmeraldAmounts", amount);
        return result instanceof String[] values ? values : new String[0];
    }

    public static List<EmeraldUnit> getEmeraldUnits() {
        return EMERALD_UNITS.invoke(binding -> {
            List<EmeraldUnit> result = new ArrayList<>(binding.units.length);
            for (Object unit : binding.units) result.add(new EmeraldUnit(
                    String.valueOf(binding.symbol.invoke(unit)), (ItemStack) binding.stack.invoke(unit)));
            return result;
        }).orElseGet(List::of);
    }

    public static Optional<CrafterBag> getCrafterBag(ItemStack stack) {
        Object item = getAnnotation(stack).map(AnnotationHandle::value)
                .or(() -> WynntilsItemUiAdapter.getInternalItem(stack)).orElse(null);
        if (!WynntilsItemUiAdapter.isInternalType(item, "CrafterBagItem")) return Optional.empty();
        try {
            Object raid = item.getClass().getMethod("getRaidKind").invoke(item);
            Object tier = item.getClass().getMethod("getGearTier").invoke(item);
            String abbreviation = raid == null ? "?" : String.valueOf(raid.getClass().getMethod("getAbbreviation").invoke(raid));
            String tierName = tier instanceof Enum<?> value ? value.name() : "?";
            return Optional.of(new CrafterBag(abbreviation, tierName));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static Optional<Float> getEmeraldPouchFraction(Object item) {
        if (!WynntilsItemUiAdapter.isInternalType(item, "EmeraldPouchItem")) return Optional.empty();
        Object capacity = invoke(item, "getCapacity");
        Object value = invoke(item, "getValue");
        if (!(capacity instanceof Number max) || !(value instanceof Number current) || max.intValue() <= 0) {
            return Optional.empty();
        }
        return Optional.of(current.floatValue() / max.floatValue());
    }

    public static Optional<ConsumableRender> getConsumableRender(Object item) {
        if (WynntilsItemUiAdapter.isInternalType(item, "PotionItem")
                || WynntilsItemUiAdapter.isInternalType(item, "MultiHealthPotionItem")) {
            Object uses = invoke(item, "getUses");
            Object current = invoke(uses, "current");
            if (current instanceof Number number) {
                boolean renderOne = WynntilsItemUiAdapter.isInternalType(item, "MultiHealthPotionItem") && number.intValue() == 1;
                return Optional.of(new ConsumableRender(number.intValue(), renderOne));
            }
        }
        if (WynntilsItemUiAdapter.isInternalType(item, "CraftedConsumableItem")) {
            Object count = invoke(item, "getCount");
            if (count instanceof Number number) return Optional.of(new ConsumableRender(number.intValue(), false));
        }
        return Optional.empty();
    }

    public static Optional<AnnotationHandle> getAnnotation(ItemStack stack) {
        return Optional.ofNullable(invoke(stack, "getAnnotation")).map(AnnotationHandle::new);
    }

    public static void setAnnotation(ItemStack stack, AnnotationHandle annotation) {
        invoke(stack, "setAnnotation", annotation == null ? null : annotation.value);
    }

    public static Text getOriginalName(ItemStack stack) {
        Object styled = invoke(stack, "getOriginalName");
        Object component = invoke(styled, "getComponent");
        return component instanceof Text text ? text : null;
    }

    public static void setOriginalName(ItemStack stack, Text name) {
        try {
            Class<?> styled = WynntilsCompat.requireClass("com.wynntils.core.text.StyledText");
            Object value = styled.getMethod("fromComponent", Text.class).invoke(null, name);
            invoke(stack, "setOriginalName", value);
        } catch (Throwable ignored) {}
    }

    public static Optional<Integer> scrollButton(Object container, HandledScreen<?> screen, boolean previousPage) {
        Object result = invoke(container, "getScrollButton", screen, previousPage);
        if (!(result instanceof Optional<?> optional) || optional.isEmpty()) return Optional.empty();
        return optional.get() instanceof Integer value ? Optional.of(value) : Optional.empty();
    }

    private static Object invoke(Object target, String name, Object... args) {
        if (target == null) return null;
        try {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    if (!method.canAccess(target) && !method.trySetAccessible()) continue;
                    return method.invoke(target, args);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void cacheFeatureMethod(Class<?> type, String name, int parameters) {
        FeatureMethodKey key = new FeatureMethodKey(type, name, parameters);
        FEATURE_METHODS.computeIfAbsent(key, ignored -> {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (!method.getName().equals(name) || method.getParameterCount() != parameters) continue;
                    if (!method.trySetAccessible()) return Optional.empty();
                    return Optional.of(method);
                }
            }
            return Optional.empty();
        });
    }

    private static Object invokeFeature(FeatureHandle feature, String name, Object... args) {
        if (feature == null || feature.value == null) return null;
        FeatureMethodKey key = new FeatureMethodKey(feature.value.getClass(), name, args.length);
        cacheFeatureMethod(key.type, name, args.length);
        Method method = FEATURE_METHODS.getOrDefault(key, Optional.empty()).orElse(null);
        if (method == null) return null;
        try {
            return method.invoke(feature.value, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean invokeFeatureVoid(FeatureHandle feature, String name, Object... args) {
        if (feature == null || feature.value == null) return false;
        FeatureMethodKey key = new FeatureMethodKey(feature.value.getClass(), name, args.length);
        cacheFeatureMethod(key.type, name, args.length);
        Method method = FEATURE_METHODS.getOrDefault(key, Optional.empty()).orElse(null);
        if (method == null) return false;
        try {
            method.invoke(feature.value, args);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
