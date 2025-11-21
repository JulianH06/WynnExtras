package julianh06.wynnextras.config.simpleconfig.serializer;

import julianh06.wynnextras.config.simpleconfig.ConfigData;
import julianh06.wynnextras.config.simpleconfig.annotations.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class GsonConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {
    private Config definition;
    private Class<T> configClass;
    private Gson gson;

    public GsonConfigSerializer(Config definition, Class<T> configClass, Gson gson) {
        this.definition = definition;
        this.configClass = configClass;
        this.gson = gson;
    }

    public GsonConfigSerializer(Config definition, Class<T> configClass) {
        this(definition, configClass, (new GsonBuilder()).setPrettyPrinting().create());
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(this.definition.name() + ".json");
    }

    public void serialize(T config) throws SerializationException {
        Path configPath = this.getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());
            BufferedWriter writer = Files.newBufferedWriter(configPath);
            this.gson.toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    public T deserialize() throws SerializationException {
        Path configPath = this.getConfigPath();
        if (Files.exists(configPath, new LinkOption[0])) {
            try {
                BufferedReader reader = Files.newBufferedReader(configPath);
                T ret = this.gson.fromJson(reader, this.configClass);
                reader.close();
                return ret;
            } catch (JsonParseException | IOException e) {
                throw new SerializationException(e);
            }
        } else {
            return this.createDefault();
        }
    }

    public T createDefault() {
        try {
            Constructor<T> constructor = this.configClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
