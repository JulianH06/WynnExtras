package julianh06.wynnextras.features.profileviewer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.misc.StyledTextAdapter;
import julianh06.wynnextras.features.profileviewer.data.OffsetDateTimeAdapter;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.raid.RaidKindAdapter;
import julianh06.wynnextras.features.raid.RaidListData;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@WEModule
public class WynncraftApiHandler {
    public static WynncraftApiHandler INSTANCE = new WynncraftApiHandler();

    private static Command apiKeyCmd = new Command(
            "apikey",
            "",
            context -> {
                String arg = StringArgumentType.getString(context, "key");
                INSTANCE.API_KEY = arg;
                save();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You have successfully set your api key." +
                        " It has been saved in your config. Don't share it publicly.")));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("key", StringArgumentType.word()))
    );

    private static Command apiKeyCmdNoArgs = new Command(
            "apikey",
            "",
            context -> {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Add an API key like this: \"/WynnExtras apikey <your key>\". " +
                        "You can find a tutorial on how to get your api key in #infos on our discord. " +
                        "Run \"/WynnExtras discord\" to join.")));
                return 1;
            },
            null,
            null
    );

    private static final String BASE_URL = "https://api.wynncraft.com/v3/player/";

    private String API_KEY;

    public static CompletableFuture<PlayerData> fetchPlayerData(String playerName) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request;

        if(INSTANCE.API_KEY == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§4You currently don't have an api key set, some stats may be hidden to you." +
                    " Run \"/WynnExtras apikey\" to learn more.")));


            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + playerName + "?fullResult"))
                    .GET()
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + playerName + "?fullResult"))
                    .header("Authorization", "Bearer " + INSTANCE.API_KEY)
                    .GET()
                    .build();
        }

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(WynncraftApiHandler::parsePlayerData);
    }

    private static PlayerData parsePlayerData(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();
        return gson.fromJson(json, PlayerData.class);
    }

    static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();


    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras/apikeyDoNotShare.json");

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                WynncraftApiHandler loaded = gson.fromJson(reader, WynncraftApiHandler.class);
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
