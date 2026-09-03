package julianh06.wynnextras.features.tomes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LocalTomeStorage {
    private static final Type TOME_LIST = new TypeToken<List<EquippedTome>>() {}.getType();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackDeserializer())
            .setPrettyPrinting()
            .create();

    private LocalTomeStorage() {}

    public static void save(String characterId, List<EquippedTome> tomes) {
        Path file = dataFile(characterId);
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(tomes == null ? List.of() : tomes, TOME_LIST, writer);
            }
        } catch (IOException e) {
            WynnExtras.LOGGER.error("Failed to save equipped tomes for " + characterId + ": " + e.getMessage());
        }
    }

    public static List<EquippedTome> load(String characterId) {
        Path file = dataFile(characterId);
        if (file == null || !Files.exists(file)) return List.of();
        try (Reader reader = Files.newBufferedReader(file)) {
            List<EquippedTome> tomes = GSON.fromJson(reader, TOME_LIST);
            return tomes == null ? List.of() : copy(tomes);
        } catch (Exception e) {
            WynnExtras.LOGGER.error("Failed to load equipped tomes for " + characterId + ": " + e.getMessage());
            return List.of();
        }
    }

    private static Path dataFile(String characterId) {
        if (MinecraftUtils.player() == null || characterId == null || !characterId.matches("[a-z0-9]{8}")) return null;
        String uuid = MinecraftUtils.player().getUuidAsString();
        return FabricLoader.getInstance().getConfigDir()
                .resolve("wynnextras").resolve(uuid).resolve("tomes").resolve(characterId + ".json");
    }

    private static List<EquippedTome> copy(List<EquippedTome> tomes) {
        List<EquippedTome> result = new ArrayList<>(tomes.size());
        for (EquippedTome tome : tomes) {
            if (tome != null && tome.type() != null && tome.stack() != null && !tome.stack().isEmpty()) {
                result.add(new EquippedTome(tome.slot(), tome.type(), tome.stack()));
            }
        }
        return List.copyOf(result);
    }
}
