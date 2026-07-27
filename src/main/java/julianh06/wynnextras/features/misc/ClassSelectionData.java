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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassSelectionData {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static String loadedPlayerUuid = "";
    private static Data data = new Data();
    private static String loadedIdentityPlayerUuid = "";
    private static IdentityData identityData = new IdentityData();

    private static class Data {
        List<String> classCardOrder = new ArrayList<>();
    }

    private static class IdentityData {
        Map<String, CharIdentity> charIdentities = new HashMap<>();
        Map<String, String> classDescriptions = new HashMap<>();
    }

    public static class CharIdentity {
        public String uuid = "";
        public String stableId = "";
        public String fallbackId = "";
        public String slotId = "";
        public String name = "";
        public String classType = "";
        public int color = 0;
        public double timePlayed = 0;
        public int level = 0;
        public double xpPercent = -1;
        public double contentProgress = -1;
        public String location = "";
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

    public static Map<String, CharIdentity> getCharIdentities() {
        loadIdentities();
        return identityData.charIdentities;
    }

    public static void saveCharIdentities() {
        loadIdentities();
        saveIdentities();
    }

    public static String getClassDescription(String charId) {
        loadIdentities();
        return identityData.classDescriptions.get(charId);
    }

    public static void setClassDescription(String charId, String description) {
        loadIdentities();
        if (description == null || description.isBlank()) {
            identityData.classDescriptions.remove(charId);
        } else {
            identityData.classDescriptions.put(charId, description);
        }
        saveIdentities();
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

    private static void loadIdentities() {
        String playerUuid = getPlayerUuid();
        if (playerUuid.isEmpty()) return;
        if (playerUuid.equals(loadedIdentityPlayerUuid)) return;

        loadedIdentityPlayerUuid = playerUuid;
        identityData = new IdentityData();

        Path path = getIdentityPath(playerUuid);
        if (Files.exists(path)) {
            try {
                IdentityData loaded = GSON.fromJson(Files.readString(path), IdentityData.class);
                if (loaded != null) identityData = loaded;
            } catch (IOException e) {
                WynnExtras.LOGGER.error("[WynnExtras] Failed to load class identity data: " + e.getMessage());
            }
        }

        if (identityData.charIdentities == null) identityData.charIdentities = new HashMap<>();
        if (identityData.classDescriptions == null) identityData.classDescriptions = new HashMap<>();
    }

    private static void saveIdentities() {
        if (loadedIdentityPlayerUuid.isEmpty()) return;

        try {
            Path path = getIdentityPath(loadedIdentityPlayerUuid);
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(identityData));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save class identity data: " + e.getMessage());
        }
    }

    private static Path getPath(String playerUuid) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras")
                .resolve(playerUuid)
                .resolve("class_selection.json");
    }

    private static Path getIdentityPath(String playerUuid) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras")
                .resolve(playerUuid)
                .resolve("class_identities.json");
    }

    private static String getPlayerUuid() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return "";
        return client.player.getUuidAsString();
    }
}
