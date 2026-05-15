package julianh06.wynnextras.features.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClassSelectionData {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static String loadedPlayerUuid = "";
    private static Data data = new Data();

    private static class Data {
        List<String> classCardOrder = new ArrayList<>();
    }

    public static List<String> getClassCardOrder() {
        load();
        return data.classCardOrder;
    }

    public static void setClassCardOrder(List<String> order) {
        load();
        data.classCardOrder = order == null ? new ArrayList<>() : new ArrayList<>(order);
        save();
    }

    private static void load() {
        String playerUuid = getPlayerUuid();
        if (playerUuid.isEmpty()) return;
        if (playerUuid.equals(loadedPlayerUuid)) return;

        loadedPlayerUuid = playerUuid;
        data = new Data();

        Path path = getPath(playerUuid);
        if (Files.exists(path)) {
            try {
                Data loaded = GSON.fromJson(Files.readString(path), Data.class);
                if (loaded != null) data = loaded;
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Failed to load class selection data: " + e.getMessage());
            }
        }

        if (data.classCardOrder == null) data.classCardOrder = new ArrayList<>();
    }

    private static void save() {
        if (loadedPlayerUuid.isEmpty()) return;

        try {
            Path path = getPath(loadedPlayerUuid);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save class selection data: " + e.getMessage());
        }
    }

    private static Path getPath(String playerUuid) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras")
                .resolve(playerUuid)
                .resolve("class_selection.json");
    }

    private static String getPlayerUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return "";
        return client.player.getUuidAsString();
    }
}
