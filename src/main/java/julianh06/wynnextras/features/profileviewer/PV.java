package julianh06.wynnextras.features.profileviewer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@WEModule
public class PV {
    public static boolean inPV = false;
    static boolean commandsInitialized = false;

    private static Command pvCmd;
    private static Command pvCmdNoArgs;

    public static String currentPlayer = "";
    public static PlayerData currentPlayerData;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if(commandsInitialized) return;

            pvCmd = new Command(
                    "pv",
                    "",
                    context -> {
                        String arg = StringArgumentType.getString(context, "player");
                        open(arg);
                        return 1;
                    },
                    null,
                    ClientCommandManager.argument("player", StringArgumentType.word())
            );

            pvCmdNoArgs = new Command(
                    "pv",
                    "",
                    context -> {
                        open(McUtils.player().getName().getString());
                        return 1;
                    },
                    null,
                    null
            );
            commandsInitialized = true;
        });
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if(inPV) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new PVScreen(currentPlayer)));
            inPV = false;
        }
    }

    public static void open(String player) {
        WynncraftApiHandler.fetchPlayerData(player).thenAccept(playerData -> {
//            for (Map.Entry<String, CharacterData> entry : playerData.getCharacters().entrySet()) {
//                String uuid = entry.getKey();
//                CharacterData data = entry.getValue();
//                System.out.println("[" + uuid + "] " + data.getType() + " Lv." + data.getLevel());
//            }

//            System.out.println("Spieler: " + playerData.getUsername());
//            System.out.println("Gilde: " + playerData.getGuild().getName());
//            System.out.println("Kills: " + playerData.getGlobalData().getMobsKilled());
            currentPlayerData = playerData;
        }).exceptionally(ex -> {
            System.err.println("Error while getting the data: " + ex.getMessage());
            return null;
        });

        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(null));
        currentPlayer = player;
        inPV = true;
    }
}
