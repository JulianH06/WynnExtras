package julianh06.wynnextras.config.simpleconfig.gui;

import julianh06.wynnextras.config.simpleconfig.ConfigData;
import julianh06.wynnextras.config.simpleconfig.ConfigManager;
import julianh06.wynnextras.config.simpleconfig.annotations.ConfigEntry;
import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ConfigScreenProvider<T extends ConfigData> implements Supplier<Screen> {
    private final ConfigManager<T> manager;
    private final GuiRegistryAccess registry;
    private final Screen parent;

    public ConfigScreenProvider(ConfigManager<T> manager, GuiRegistryAccess registry, Screen parent) {
        this.manager = manager;
        this.registry = registry;
        this.parent = parent;
    }

    public Screen get() {
        T config = this.manager.getConfig();
        T defaults = this.manager.getSerializer().createDefault();

        String name = this.manager.getDefinition().name();
        String title = this.manager.getDefinition().title();

        ConfigBuilder configBuilder = ConfigBuilder.create().setParentScreen(this.parent).setTitle(Text.literal(title));
        ConfigManager<T> configManager = this.manager;
        Objects.requireNonNull(configManager);
        ConfigBuilder builder = configBuilder.setSavingRunnable(configManager::save);
        Class<T> configClass = this.manager.getConfigClass();

        builder.transparentBackground().setDefaultBackgroundTexture(null);

        Arrays.stream(configClass.getDeclaredFields()).collect(Collectors.groupingBy((field) -> this.getOrCreateCategoryForField(field, builder, name), LinkedHashMap::new, Collectors.toList()))
                .forEach((key, value) -> value.forEach((field) -> {
                    ConfigEntry.Name ann = field.getAnnotation(ConfigEntry.Name.class);
                    String fieldName = ann != null ? ann.value() : field.getName();

                    List<AbstractConfigListEntry<?>> transformed = this.registry.getAndTransform(fieldName, field, config, defaults, this.registry);
                    Objects.requireNonNull(key);
                    transformed.forEach(key::addEntry);
                }));

        return builder.build();
    }

    private ConfigCategory getOrCreateCategoryForField(Field field, ConfigBuilder screenBuilder, String name) {
        String categoryName = "default";
        if (field.isAnnotationPresent(ConfigEntry.Category.class)) {
            categoryName = field.getAnnotation(ConfigEntry.Category.class).value();
        }

        Text categoryKey = Text.literal(categoryName);
        return screenBuilder.getOrCreateCategory(categoryKey);
    }
}
