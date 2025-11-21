package julianh06.wynnextras.config.simpleconfig;

import julianh06.wynnextras.config.simpleconfig.event.ConfigSerializeEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@ApiStatus.NonExtendable
public interface ConfigHolder<T extends ConfigData> extends Supplier<T> {
    @NotNull Class<T> getConfigClass();

    void save();

    boolean load();

    T getConfig();

    void registerSaveListener(ConfigSerializeEvent.Save<T> var1);

    void registerLoadListener(ConfigSerializeEvent.Load<T> var1);

    default T get() {
        return (T)this.getConfig();
    }

    void resetToDefault();

    void setConfig(T var1);
}
