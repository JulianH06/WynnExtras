package julianh06.wynnextras.config.simpleconfig.serializer;

import julianh06.wynnextras.config.simpleconfig.ConfigData;
import julianh06.wynnextras.config.simpleconfig.annotations.Config;

public interface ConfigSerializer<T extends ConfigData> {
    void serialize(T var1) throws SerializationException;

    T deserialize() throws SerializationException;

    T createDefault();

    public static class SerializationException extends Exception {
        public SerializationException(Throwable cause) {
            super(cause);
        }
    }

    @FunctionalInterface
    public interface Factory<T extends ConfigData> {
        ConfigSerializer<T> create(Config var1, Class<T> var2);
    }
}