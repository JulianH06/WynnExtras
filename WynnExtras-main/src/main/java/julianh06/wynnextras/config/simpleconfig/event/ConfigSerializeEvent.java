package julianh06.wynnextras.config.simpleconfig.event;

import julianh06.wynnextras.config.simpleconfig.ConfigData;
import julianh06.wynnextras.config.simpleconfig.ConfigHolder;
import net.minecraft.util.ActionResult;

public final class ConfigSerializeEvent {
    private ConfigSerializeEvent() {
    }

    @FunctionalInterface
    public interface Load<T extends ConfigData> {
        ActionResult onLoad(ConfigHolder<T> var1, T var2);
    }

    @FunctionalInterface
    public interface Save<T extends ConfigData> {
        ActionResult onSave(ConfigHolder<T> var1, T var2);
    }
}