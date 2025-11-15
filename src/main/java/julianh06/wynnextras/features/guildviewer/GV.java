package julianh06.wynnextras.features.guildviewer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@WEModule
public class GV {
    public static boolean inGV = false;
    static boolean commandsInitialized = false;
    private static Command gvCmd;
    private static Command gvCmdNoArgs;

    public static String currentGuild = "";
    public static GuildData currentGuildData;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if(commandsInitialized) return;

            gvCmd = new Command(
                    "gv",
                    "",
                    context -> {
                        String arg = StringArgumentType.getString(context, "guild");
                        open(arg);
                        return 1;
                    },
                    null,
                    List.of(ClientCommandManager.argument("guild", StringArgumentType.word()))
            );

            gvCmdNoArgs = new Command(
                    "gv",
                    "",
                    context -> {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You need to specify the guild you want to view. Usage: /gv [guild prefix]"));
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
        if(inGV) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new GVScreen(currentGuild)));
            inGV = false;
        }
    }

    @SubscribeEvent
    void onInput(KeyInputEvent event) {
        if(event.getKey() != GLFW.GLFW_KEY_ENTER || event.getAction() != GLFW.GLFW_PRESS) return;
        if(GVScreen.searchBar != null) {
            open(GVScreen.searchBar.getInput());
        }
    }

    public static void open(String guild) {
        currentGuildData = null;
        WynncraftApiHandler.fetchGuildData(guild).thenAccept(guildData -> {
            currentGuildData = guildData;
        }).exceptionally(ex -> {
            System.err.println("Error while getting the data: " + ex.getMessage());
            return null;
        });

        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(null));
        currentGuild = guild;
        inGV = true;
    }

    @SubscribeEvent
    void onClick(ClickEvent event) {
        //GVScreen.onClick();
    }
}
