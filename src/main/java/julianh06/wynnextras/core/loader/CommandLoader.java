package julianh06.wynnextras.core.loader;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import julianh06.wynnextras.command.ChatCommands;

public class CommandLoader implements WELoader {

    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // Basis Commands
            LiteralArgumentBuilder<FabricClientCommandSource> base = ClientCommandManager.literal("WynnExtras");
            LiteralArgumentBuilder<FabricClientCommandSource> alias = ClientCommandManager.literal("we");

            // Chat-Command anhängen
            base.then(ChatCommands.register());
            alias.then(ChatCommands.register());

            // Dispatcher registrieren
            dispatcher.register(base);
            dispatcher.register(alias);

        });
    }
}
