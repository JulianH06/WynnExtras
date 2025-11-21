package julianh06.wynnextras.config.simpleconfig;

import julianh06.wynnextras.config.simpleconfig.annotations.Config;
import julianh06.wynnextras.config.simpleconfig.gui.ConfigScreenProvider;
import julianh06.wynnextras.config.simpleconfig.gui.DefaultGuiProviders;
import julianh06.wynnextras.config.simpleconfig.gui.DefaultGuiTransformers;
import julianh06.wynnextras.config.simpleconfig.gui.registry.ComposedGuiRegistryAccess;
import julianh06.wynnextras.config.simpleconfig.gui.registry.DefaultGuiRegistryAccess;
import julianh06.wynnextras.config.simpleconfig.gui.registry.GuiRegistry;
import julianh06.wynnextras.config.simpleconfig.serializer.ConfigSerializer;
import julianh06.wynnextras.config.simpleconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class SimpleConfig {
    private static final Map<Class<? extends ConfigData>, ConfigHolder<?>> holders = new HashMap<>();
    private static final Map<Class<? extends ConfigData>, GuiRegistry> guiRegistries = new HashMap<>();

    private SimpleConfig() {
    }

    public static <T extends ConfigData> ConfigHolder<T> register(Class<T> configClass) {
        Objects.requireNonNull(configClass);

        if (holders.containsKey((configClass))) {
            throw new RuntimeException(String.format("Config '%s' already registered", configClass));
        }

        Config def = configClass.getAnnotation(Config.class);
        if (def == null) {
            throw new RuntimeException(String.format("Config '%s' must be annotated with @Config", configClass));
        }

        ConfigSerializer<T> serializer = ((ConfigSerializer.Factory<T>) (GsonConfigSerializer::new)).create(def, configClass);
        ConfigManager<T> manager = new ConfigManager<T>(def, configClass, serializer);

        holders.put(configClass, manager);
        return manager;
    }

    public static <T extends ConfigData> ConfigHolder<T> getConfigHolder(Class<T> configClass) {
        Objects.requireNonNull(configClass);

        if (!holders.containsKey(configClass)) {
            throw new RuntimeException(String.format("Config '%s' not registered", configClass));
        }

        return (ConfigHolder<T>) holders.get(configClass);
    }

    public static void save(Class<?> configClass) {
        Objects.requireNonNull(configClass);

        if(!holders.containsKey(configClass)) {
            throw new RuntimeException(String.format("Config '%s' not registered (in save method)", configClass));
        }

        holders.get(configClass).save();
    }

    public static <T extends ConfigData> T getInstance(Class<T> configClass) {
        return getConfigHolder(configClass).getConfig(); //TODO: hier fehler crash dies das
    }

    @Environment(EnvType.CLIENT)
    public static <T extends ConfigData> GuiRegistry getGuiRegistry(Class<T> configClass) {
        return guiRegistries.computeIfAbsent(configClass, (n) -> new GuiRegistry());
    }

    @Environment(EnvType.CLIENT)
    public static <T extends ConfigData> Supplier<Screen> getConfigScreen(Class<T> configClass, Screen parent) {
        return new ConfigScreenProvider<>((ConfigManager<T>) getConfigHolder(configClass),
                new ComposedGuiRegistryAccess(getGuiRegistry(configClass), ClientOnly.defaultGuiRegistry, new DefaultGuiRegistryAccess()),
                parent
        );
    }

    @Environment(EnvType.CLIENT)
    private static class ClientOnly {
        private static final GuiRegistry defaultGuiRegistry = DefaultGuiTransformers.apply(DefaultGuiProviders.apply(new GuiRegistry()));
    }
}
