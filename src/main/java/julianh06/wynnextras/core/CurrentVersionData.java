package julianh06.wynnextras.core;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class CurrentVersionData {
    public static CurrentVersionData INSTANCE = new CurrentVersionData();

    public String version;

    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/version.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                CurrentVersionData loaded = gson.fromJson(reader, CurrentVersionData.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                } else {
                    System.err.println("[WynnExtras] Deserialized data was null, keeping default INSTANCE.");
                }
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't read the version file:");
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[WynnExtras] Couldn't write the version file:");
            e.printStackTrace();
        }
    }

    private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/cjWpppr5/version";

    public static String fetchLatestVersion() {
        try {
            URL url = new URL(MODRINTH_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            JsonArray versions = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonArray();

            String latest = null;
            String latestDate = "";

            for (JsonElement el : versions) {
                JsonObject obj = el.getAsJsonObject();
                String version = obj.get("version_number").getAsString();
                String date = obj.get("date_published").getAsString();
                String gameVersion = obj.get("game_versions").getAsString();

                if(!gameVersion.equals(SharedConstants.getGameVersion().name())) continue;

                if (date.compareTo(latestDate) > 0) {
                    latest = version;
                    latestDate = date;
                }
            }

            return latest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
