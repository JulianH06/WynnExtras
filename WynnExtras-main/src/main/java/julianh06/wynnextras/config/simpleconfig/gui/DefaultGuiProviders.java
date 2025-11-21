package julianh06.wynnextras.config.simpleconfig.gui;


import julianh06.wynnextras.config.simpleconfig.Utils;
import julianh06.wynnextras.config.simpleconfig.annotations.ConfigEntry;
import julianh06.wynnextras.config.simpleconfig.gui.registry.GuiRegistry;
import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiRegistryAccess;
import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static julianh06.wynnextras.config.simpleconfig.Utils.*;

@Environment(EnvType.CLIENT)
@SuppressWarnings("unchecked")
public class DefaultGuiProviders {
    private static final ConfigEntryBuilder ENTRY_BUILDER = ConfigEntryBuilder.create();

    private DefaultGuiProviders() {}

    public static GuiRegistry apply(GuiRegistry registry) {
        // Annotations
        registry.registerAnnotationProvider((name, field, config, defaults, guiProvider) ->
                        Collections.emptyList(),
                ConfigEntry.Excluded.class);

        registry.registerAnnotationProvider((name, field, config, defaults, guiProvider) ->
                        Collections.singletonList(ENTRY_BUILDER.startTextDescription(Text.literal(getUnsafely(field, config, ""))).build())
                , (field -> field.getType() == String.class), ConfigEntry.Text.class);

        registry.registerAnnotationProvider((name, field, config, defaults, guiProvider) -> {
            ConfigEntry.ColorPicker colorPicker = field.getAnnotation(ConfigEntry.ColorPicker.class);
            return Collections.singletonList(
                    ENTRY_BUILDER.startColorField(Text.literal(name), Utils.getUnsafely(field, config, 0))
                            .setAlphaMode(colorPicker.allowAlpha())
                            .setDefaultValue(() -> Utils.getUnsafely(field, defaults))
                            .setSaveConsumer((newValue) -> Utils.setUnsafely(field, config, newValue))
                            .build()
            );
        }, (field) -> field.getType() == Integer.TYPE || field.getType() == Integer.class, ConfigEntry.ColorPicker.class);

        registry.registerAnnotationProvider((name, field, config, defaults, guiProvider) -> {
            ConfigEntry.Dropdown dropdown = field.getAnnotation(ConfigEntry.Dropdown.class);
            return Collections.singletonList(
                    ENTRY_BUILDER.startStringDropdownMenu(Text.literal(name), getUnsafely(field, config, dropdown.values()[0]))
                            .setSelections(Arrays.asList(dropdown.values()))
                            .setSuggestionMode(false)
                            .setDefaultValue(getUnsafely(field, config, dropdown.values()[0]))
                            .setSaveConsumer(newValue -> Utils.setUnsafely(field, config, newValue))
                            .build()
            );
        }, (field) -> field.getType() == String.class, ConfigEntry.Dropdown.class);

        registry.registerAnnotationProvider((name, field, config, defaults, guiProvider) -> {
            List<?> children = getChildren(field, config, defaults, guiProvider);

            Object listObj = getUnsafely(field, config);

            if (field.getType() == List.class || field.getType().isArray()) {
                List<?> listValues;
                Class<?> elementType;

                if (List.class.isAssignableFrom(field.getType())) {
                    ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                    elementType = (Class<?>) genericType.getActualTypeArguments()[0];
                    listValues = listObj != null ? (List<?>) listObj : Collections.emptyList();
                } else {
                    elementType = field.getType().getComponentType();
                    listValues = Arrays.asList((Object[]) listObj);
                }

                Object dummyDefault = constructUnsafely(elementType);
                AtomicInteger index = new AtomicInteger();

                children = listValues.stream().filter(Objects::nonNull).map(elem -> {
                    String classNameOrNull = getClassNameOrNull(elementType, elem);
                    String finalClassName = classNameOrNull != null
                            ? classNameOrNull
                            : elementType.getSimpleName() + " #" + index.getAndIncrement();
                    index.incrementAndGet();

                    List<?> obj;
                    if (elementType.isPrimitive() || elementType == String.class) {
                        obj = guiProvider.getAndTransform(finalClassName, field, elem, dummyDefault, guiProvider);
                    } else {
                        obj = getChildren(elementType, elem, dummyDefault, guiProvider);
                    }

                    ConfigEntry.Collapsible ann = elementType.getAnnotation(ConfigEntry.Collapsible.class);
                    if (ann != null) {
                        return ENTRY_BUILDER.startSubCategory(Text.literal(finalClassName), (List<AbstractConfigListEntry>) obj)
                                .setExpanded(ann.startExpanded()).build();
                    }

                    return new MultiElementListEntry(Text.literal(finalClassName), obj,
                            getChildren(elementType, elem, dummyDefault, guiProvider), true);
                }).toList();
            }

            return Collections.singletonList(ENTRY_BUILDER.startSubCategory(Text.literal(name), (List<AbstractConfigListEntry>) children)
                    .setExpanded((field.getAnnotation(ConfigEntry.Collapsible.class)).startExpanded()).build()
            );
        }, (field) -> !field.getType().isPrimitive(), ConfigEntry.Collapsible.class);

        // Predicates
        registry.registerPredicateProvider((name, field, config, defaults, registry1) ->
                Collections.singletonList(ENTRY_BUILDER.startIntList(Text.literal(name), getUnsafely(field, config))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), isListOfType(Integer.class));

        registry.registerPredicateProvider((name, field, config, defaults, registry1) ->
                Collections.singletonList(ENTRY_BUILDER.startLongList(Text.literal(name), getUnsafely(field, config))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), isListOfType(Long.class));

        registry.registerPredicateProvider((name, field, config, defaults, registry1) ->
                Collections.singletonList(ENTRY_BUILDER.startFloatList(Text.literal(name), getUnsafely(field, config))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), isListOfType(Float.class));

        registry.registerPredicateProvider((name, field, config, defaults, registry1) ->
                Collections.singletonList(ENTRY_BUILDER.startDoubleList(Text.literal(name), getUnsafely(field, config))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), isListOfType(Double.class));

        registry.registerPredicateProvider((name, field, config, defaults, registry1) ->
                Collections.singletonList(ENTRY_BUILDER.startStrList(Text.literal(name), getUnsafely(field, config))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), isListOfType(String.class));

        registry.registerPredicateProvider((name, field, config, defaults, registry1) -> {
            List<Object> configValue = getUnsafely(field, config);
            Class<?> fieldTypeParam = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            Object defaultElemValue = constructUnsafely(fieldTypeParam);

            AtomicInteger listIndex = new AtomicInteger();
            return Collections.singletonList(new NestedListListEntry(Text.literal(name), configValue, false, null,
                    (newValue) -> setUnsafely(field, config, newValue), () -> getUnsafely(field, defaults), ENTRY_BUILDER.getResetButtonKey(),
                    true, false, nestedNewCellProvider(listIndex, fieldTypeParam, defaultElemValue, registry1)));
        }, isNotListOfType(Integer.class, Long.class, Float.class, Double.class, String.class));

        // Types
        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startBooleanToggle(Text.literal(name), getUnsafely(field, config, false))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue))
                        .setYesNoTextSupplier((bool) -> Text.literal(bool ? "§aYes" : "§cNo")).build()
                ), Boolean.TYPE, Boolean.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startIntField(Text.literal(name), getUnsafely(field, config, 0))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), Integer.TYPE, Integer.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startIntList(Text.literal(name), Lists.newArrayList(getUnsafely(field, config, new Integer[0])))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList((Integer[])getUnsafely(field, defaults)))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.toArray(new Integer[0]))).build()
                ), Integer[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startIntList(Text.literal(name), Lists.newArrayList((Iterable)IntStream.of(getUnsafely(field, config, new int[0])).boxed().collect(Collectors.toList())))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList(Arrays.asList(ArrayUtils.toObject((int[])getUnsafely(field, defaults)))))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.stream().mapToInt(Integer::intValue).toArray())).build()
                ), int[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startLongField(Text.literal(name), getUnsafely(field, config, 0L))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), Long.TYPE, Long.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startLongList(Text.literal(name), Lists.newArrayList(getUnsafely(field, config, new Long[0])))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList((Long[])getUnsafely(field, defaults)))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.toArray(new Long[0]))).build()
                ), Long[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startLongList(Text.literal(name), Lists.newArrayList((Iterable)LongStream.of(getUnsafely(field, config, new long[0])).boxed().collect(Collectors.toList())))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList(Arrays.asList(ArrayUtils.toObject((long[])getUnsafely(field, defaults)))))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.stream().mapToLong(Long::longValue).toArray())).build()
                ), long[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startFloatField(Text.literal(name), getUnsafely(field, config, 0.0F))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), Float.TYPE, Float.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startFloatList(Text.literal(name), Lists.newArrayList(getUnsafely(field, config, new Float[0])))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList((Float[])getUnsafely(field, defaults)))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.toArray(new Float[0]))).build()
                ), Float[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startFloatList(Text.literal(name), Lists.newArrayList(Arrays.asList(ArrayUtils.toObject(getUnsafely(field, config, new float[0])))))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList(Arrays.asList(ArrayUtils.toObject((float[])getUnsafely(field, defaults)))))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, ArrayUtils.toPrimitive(newValue.toArray(new Float[0])))).build()
                ), float[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startDoubleField(Text.literal(name), getUnsafely(field, config, (double)0.0F))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), Double.TYPE, Double.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startDoubleList(Text.literal(name), Lists.newArrayList(getUnsafely(field, config, new Double[0])))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList((Double[])getUnsafely(field, defaults)))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.toArray(new Double[0]))).build()
                ), Double[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startDoubleList(Text.literal(name), Lists.newArrayList(Arrays.asList(ArrayUtils.toObject(getUnsafely(field, config, new double[0])))))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList(Arrays.asList(ArrayUtils.toObject((double[])getUnsafely(field, defaults)))))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, ArrayUtils.toPrimitive(newValue.toArray(new Double[0])))).build()
                ), double[].class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startStrField(Text.literal(name), getUnsafely(field, config, ""))
                        .setDefaultValue(() -> getUnsafely(field, defaults))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue)).build()
                ), String.class);

        registry.registerTypeProvider((name, field, config, defaults, guiProvider) ->
                Collections.singletonList(ENTRY_BUILDER.startStrList(Text.literal(name), Lists.newArrayList(getUnsafely(field, config, new String[0])))
                        .setDefaultValue(() -> defaults == null ? null : Lists.newArrayList((String[])getUnsafely(field, defaults)))
                        .setSaveConsumer((newValue) -> setUnsafely(field, config, newValue.toArray(new String[0]))).build()
                ), String[].class);

        // Misc
        registry.registerPredicateProvider((name, field, config, defaults, registry1) -> {
            List<Object> configValueAsList = getConfigValueAsList(field, config);

            Class<?> fieldTypeParam = field.getType().getComponentType();
            Object defaultElemValue = constructUnsafely(fieldTypeParam);

            AtomicInteger listIndex = new AtomicInteger();

            return Collections.singletonList(new NestedListListEntry(Text.literal(name), configValueAsList, false, null, (newValue) -> {
                List<?> newValueList = (List<?>) newValue;

                Object[] newArray = (Object[]) Array.newInstance(fieldTypeParam, newValueList.size());

                for(int i = 0; i < newValueList.size(); ++i) {
                    Array.set(newArray, i, newValueList.get(i));
                }

                setUnsafely(field, config, newArray);
            }, () -> getConfigValueAsList(field, defaults), ENTRY_BUILDER.getResetButtonKey(), true, false,
                    nestedNewCellProvider(listIndex, fieldTypeParam, defaultElemValue, registry1)));
        }, (field) -> field.getType().isArray() && field.getType() != String[].class && field.getType() != int[].class && field.getType() != Integer[].class
                && field.getType() != long[].class && field.getType() != Long[].class && field.getType() != float[].class && field.getType() != Float[].class
                && field.getType() != double[].class && field.getType() != Double[].class);

        return registry;
    }

    private static List<Object> getConfigValueAsList(Field field, Object config) {
        Object configValue = getUnsafely(field, config);
        List<Object> configValueAsList = new ArrayList<>(Array.getLength(configValue));

        for(int i = 0; i < Array.getLength(configValue); ++i) {
            configValueAsList.add(Array.get(configValue, i));
        }

        return configValueAsList;
    }

    private static List<AbstractConfigListEntry<?>> getChildren(Field field, Object config, Object defaults, GuiRegistryAccess guiProvider) {
        return getChildren(field.getType(), getUnsafely(field, config), getUnsafely(field, defaults), guiProvider);
    }

    private static List<AbstractConfigListEntry<?>> getChildren(Class<?> fieldType, Object iConfig, Object iDefaults, GuiRegistryAccess guiProvider) {
        return Arrays.stream(fieldType.getDeclaredFields()).map((iField) -> {
            ConfigEntry.Name ann = iField.getAnnotation(ConfigEntry.Name.class);
            String classNameOrNull = getClassNameOrNull(iField.getType(), getUnsafely(iField, iConfig));
            String name = classNameOrNull != null
                    ? classNameOrNull
                    : (ann != null ? ann.value() : iField.getName());

            return guiProvider.getAndTransform(name, iField, iConfig, iDefaults, guiProvider);
        }).filter(Objects::nonNull).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static Predicate<Field> isListOfType(Type... types) {
        return (field) -> {
            if (List.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                return args.length == 1 && Stream.of(types).anyMatch((type) -> Objects.equals(args[0], type));
            } else {
                return false;
            }
        };
    }

    private static Predicate<Field> isNotListOfType(Type... types) {
        return (field) -> {
            if (List.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
                return args.length == 1 && Stream.of(types).noneMatch((type) -> Objects.equals(args[0], type));
            } else {
                return false;
            }
        };
    }

    private static BiFunction<?,?,?> nestedNewCellProvider(AtomicInteger listIndex, Class<?> fieldTypeParam, Object defaultElemValue, GuiRegistryAccess registry1) {
        return (elem, nestedListListEntry) -> {
            String classNameOrNull = getClassNameOrNull(fieldTypeParam, elem);
            String finalClassName = classNameOrNull != null
                    ? classNameOrNull
                    : fieldTypeParam.getSimpleName() + " #" + listIndex.get();
            listIndex.incrementAndGet();

            if (elem == null) {
                Object newDefaultElemValue = constructUnsafely(fieldTypeParam);
                return new MultiElementListEntry(Text.literal(finalClassName), newDefaultElemValue,
                        getChildren(fieldTypeParam, newDefaultElemValue, defaultElemValue, registry1), true);
            } else {
                return new MultiElementListEntry(Text.literal(finalClassName), elem,
                        getChildren(fieldTypeParam, elem, defaultElemValue, registry1), true);
            }
        };
    }
}

