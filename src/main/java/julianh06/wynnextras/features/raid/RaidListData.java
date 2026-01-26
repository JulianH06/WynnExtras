package julianh06.wynnextras.features.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.models.raid.type.RaidInfo;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RaidListData {
    public static RaidListData INSTANCE = new RaidListData();

    List<RaidData> raids = new ArrayList<>();

    public List<RaidData> getRaids() {
        return raids;
    }

    static GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(StyledText.class, new StyledTextAdapter());

    static Gson gson = builder
            .registerTypeAdapter(RaidKind.class, new RaidKindAdapter(builder.create()))
            .setPrettyPrinting()
            .create();


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/raidlist.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                RaidListData loaded = gson.fromJson(reader, RaidListData.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                } else {
                    System.err.println("[WynnExtras] Deserialized data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the raidlist file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the raidlist file:");
            e.printStackTrace();
        }
    }
}
