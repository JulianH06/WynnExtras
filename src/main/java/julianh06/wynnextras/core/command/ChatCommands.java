package julianh06.wynnextras.command;

import julianh06.wynnextras.features.chat.ChatManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class ChatCommands {

    public static LiteralArgumentBuilder<FabricClientCommandSource> register() {
        return ClientCommandManager.literal("chat")
            .then(ClientCommandManager.literal("p")
                .executes(c -> {
                    ChatManager.setCurrentChannel(ChatManager.ChatChannel.PARTY);
                    return 1;
                }))
            .then(ClientCommandManager.literal("g")
                .executes(c -> {
                    ChatManager.setCurrentChannel(ChatManager.ChatChannel.GUILD);
                    return 1;
                }))
            .then(ClientCommandManager.literal("a")
                .executes(c -> {
                    ChatManager.setCurrentChannel(ChatManager.ChatChannel.ALL);
                    return 1;
                }));
    }
}
