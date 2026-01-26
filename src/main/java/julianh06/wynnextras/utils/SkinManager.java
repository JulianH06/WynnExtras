package julianh06.wynnextras.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class SkinManager {

    private static final Map<UUID, Identifier> skinCache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Map<String, UUID> uuidCache = new HashMap<>();

    public static UUID getUUIDFromUsernameCached(String username) throws IOException {
        if (uuidCache.containsKey(username)) {
            return uuidCache.get(username);
        }

        UUID uuid = getUUIDFromUsername(username);
        uuidCache.put(username, uuid);
        return uuid;
    }

    public static Identifier getSkin(UUID uuid) {
        return skinCache.getOrDefault(uuid, DefaultSkinHelper.getTexture());
    }

    public static Identifier getSkin(String name) {
        try {
            UUID uuid = getUUIDFromUsername(name);

            if (skinCache.containsKey(uuid)) {
                return skinCache.get(uuid);
            }

            String skinUrl = getSkinURL(uuid);
            NativeImage image = downloadSkin(skinUrl);
            Identifier id = registerSkin(image, uuid);
            skinCache.put(uuid, id);
            return id;

        } catch (IOException e) {
            System.err.println("SkinManager: Error loading skin for " + name + ": " + e.getMessage());
            return DefaultSkinHelper.getTexture();
        }
    }

    public static void loadSkinAsync(UUID uuid, String username, Runnable callback) {
        if (skinCache.containsKey(uuid)) {
            if (callback != null) callback.run();
            return;
        }

        executor.submit(() -> {
            try {
                String skinUrl = getSkinURL(uuid);
                NativeImage image = downloadSkin(skinUrl);
                Identifier id = registerSkin(image, uuid);
                skinCache.put(uuid, id);
            } catch (Exception e) {
                System.err.println("SkinManager: Error loading skin for " + username + ": " + e.getMessage());
            }
            if (callback != null) callback.run();
        });
    }

    private static String getSkinURL(UUID uuid) throws IOException {
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray properties = json.getAsJsonArray("properties");
            for (JsonElement element : properties) {
                JsonObject property = element.getAsJsonObject();
                if (property.get("name").getAsString().equals("textures")) {
                    String value = property.get("value").getAsString();
                    String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                    JsonObject textureJson = JsonParser.parseString(decoded).getAsJsonObject();
                    return textureJson.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                }
            }
            throw new IOException("Skin-URL not found");
        } finally {
            connection.disconnect();
        }
    }

    private static NativeImage downloadSkin(String skinUrl) throws IOException {
        try (InputStream inputStream = new URL(skinUrl).openStream()) {
            return NativeImage.read(inputStream);
        }
    }

    private static Identifier registerSkin(NativeImage image, UUID uuid) {
        Identifier id = Identifier.of("customskin", uuid.toString());
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "customskin/" + uuid, image));
        return id;
    }

    public static UUID getUUIDFromUsername(String username) throws IOException {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + username;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String uuidStr = json.get("id").getAsString();
            return UUID.fromString(uuidStr.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            ));
        } finally {
            connection.disconnect();
        }
    }
}
