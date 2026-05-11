package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class AutoStream {
    private static Command toggleCommand = new Command(
            "autostream",
            "",
            context -> {
                WynnExtrasConfig.INSTANCE.autoStreamEnabled = !WynnExtrasConfig.INSTANCE.autoStreamEnabled;
                WynnExtras.sendMessageToClient((WynnExtrasConfig.INSTANCE.autoStreamEnabled ? "§aEnabled" : "§cDisabled") + " auto /stream");
                return 1;
            }
    );

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        if (!WynnExtrasConfig.INSTANCE.autoStreamEnabled) return;
        if (MinecraftClient.getInstance().getNetworkHandler() == null || event.message == null || event.message.getString() == null) return;

        if (event.message.getString().contains("Welcome to Wynncraft")) {
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("stream");
        }
    }
}
