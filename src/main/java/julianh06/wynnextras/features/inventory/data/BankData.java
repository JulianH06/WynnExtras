package julianh06.wynnextras.features.inventory.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.utils.OptionalTypeAdapter;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public abstract class BankData {
    public int lastPage = 1;
    public HashMap<Integer, List<ItemStack>> BankPages = new HashMap<>();
    public HashMap<Integer, String> BankPageNames = new HashMap<>();

    public abstract Path getConfigPath();

    public void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());

            try (Writer writer = Files.newBufferedWriter(path)) {
                getGson().toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write bank data:");
            e.printStackTrace();
        }
    }

    public void load() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't create config directory:");
            e.printStackTrace();
        }

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                BankData loaded = getGson().fromJson(reader, this.getClass());
                if (loaded != null) {
                    this.BankPages = loaded.BankPages;
                    this.lastPage = loaded.lastPage;
                    this.BankPageNames = loaded.BankPageNames;
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read bank data:");
                e.printStackTrace();
            }
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new TypeAdapterFactory() {
                @SuppressWarnings("unchecked")
                public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
                    if (!Optional.class.isAssignableFrom(type.getRawType())) {
                        return null;
                    }
                    Type actualType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
                    TypeAdapter<?> delegate = gson.getAdapter(TypeToken.get(actualType));
                    return (TypeAdapter<T>) new OptionalTypeAdapter<>(delegate);
                }
            })
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackDeserializer())
            .setPrettyPrinting()
            .create();

    public static Gson getGson() {
        return GSON;
    }
}

