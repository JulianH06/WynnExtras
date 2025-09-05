package julianh06.wynnextras.features.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.utils.OptionalTypeAdapter;
import net.fabricmc.loader.api.FabricLoader;
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

public class BankOverlayData {
    public HashMap<Integer, List<ItemStack>> BankPages = new HashMap<>();

    public HashMap<Integer, String> BankPageNames = new HashMap<>();

    public static BankOverlayData INSTANCE = new BankOverlayData();

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

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/bankpages.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BankOverlayData loaded = GSON.fromJson(reader, BankOverlayData.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                } else {
                    System.err.println("[WynnExtras] Deserialized data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the bankpages file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the bankpages file:");
            e.printStackTrace();
        }
    }
}
