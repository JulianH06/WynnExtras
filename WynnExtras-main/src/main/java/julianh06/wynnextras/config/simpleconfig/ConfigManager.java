package julianh06.wynnextras.config.simpleconfig;

import julianh06.wynnextras.config.simpleconfig.annotations.Config;
import julianh06.wynnextras.config.simpleconfig.event.ConfigSerializeEvent;
import julianh06.wynnextras.config.simpleconfig.serializer.ConfigSerializer;
import net.minecraft.util.ActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager<T extends ConfigData> implements ConfigHolder<T> {
    private final Logger logger = LogManager.getLogger();
    private final Config definition;
    private final Class<T> configClass;
    private final ConfigSerializer<T> serializer;
    private final List<ConfigSerializeEvent.Save<T>> saveEvent = new ArrayList<>();
    private final List<ConfigSerializeEvent.Load<T>> loadEvent = new ArrayList<>();
    private T config;

    ConfigManager(Config definition, Class<T> configClass, ConfigSerializer<T> serializer) {
        this.definition = definition;
        this.configClass = configClass;
        this.serializer = serializer;
        if (this.load()) {
            this.save();
        }

    }

    public Config getDefinition() {
        return this.definition;
    }

    public @NotNull Class<T> getConfigClass() {
        return this.configClass;
    }

    public ConfigSerializer<T> getSerializer() {
        return this.serializer;
    }

    public void save() {
        for(ConfigSerializeEvent.Save<T> save : this.saveEvent) {
            ActionResult result = save.onSave(this, this.config);
            if (result == ActionResult.FAIL) {
                return;
            }

            if (result != ActionResult.PASS) {
                break;
            }
        }

        try {
            this.serializer.serialize(this.config);
        } catch (ConfigSerializer.SerializationException e) {
            this.logger.error("Failed to save config '{}'", this.configClass, e);
        }

    }

    public boolean load() {
        try {
            T deserialized = this.serializer.deserialize();

            for(ConfigSerializeEvent.Load<T> load : this.loadEvent) {
                ActionResult result = load.onLoad(this, deserialized);
                if (result == ActionResult.FAIL) {
                    this.config = this.serializer.createDefault();
                    this.config.validatePostLoad();
                    return false;
                }

                if (result != ActionResult.PASS) {
                    break;
                }
            }

            this.config = deserialized;
            this.config.validatePostLoad();
            return true;
        } catch (ConfigData.ValidationException | ConfigSerializer.SerializationException e) {
            this.logger.error("Failed to load config '{}', using default!", this.configClass, e);
            this.resetToDefault();
            return false;
        }
    }

    public T getConfig() {
        return this.config;
    }

    public void registerSaveListener(ConfigSerializeEvent.Save<T> save) {
        this.saveEvent.add(save);
    }

    public void registerLoadListener(ConfigSerializeEvent.Load<T> load) {
        this.loadEvent.add(load);
    }

    public void resetToDefault() {
        this.config = this.serializer.createDefault();

        try {
            this.config.validatePostLoad();
        } catch (ConfigData.ValidationException v) {
            throw new RuntimeException("result of createDefault() was invalid!", v);
        }
    }

    public void setConfig(T config) {
        this.config = config;
    }
}
