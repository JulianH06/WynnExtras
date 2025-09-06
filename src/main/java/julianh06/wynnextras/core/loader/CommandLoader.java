package julianh06.wynnextras.core.loader;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import julianh06.wynnextras.command.ChatCommands;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;

public class CommandLoader implements WELoader {

    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // Basis Commands
            LiteralArgumentBuilder<FabricClientCommandSource> base = ClientCommandManager.literal("WynnExtras");
            LiteralArgumentBuilder<FabricClientCommandSource> alias = ClientCommandManager.literal("we");

            // Chat-Command an alle anhängen
            base.then(ChatCommands.register());
            alias.then(ChatCommands.register());

            // Chat auch als eigenes Kommando ohne Prefix
            dispatcher.register(ChatCommands.register());

            // Eigene Commands aus COMMAND_LIST
            for (Command cmd : Command.COMMAND_LIST) {
                if (cmd instanceof SubCommand) continue;
                base.then(buildCommandTree(cmd));
                alias.then(buildCommandTree(cmd));
            }

            // Dispatcher registrieren
            dispatcher.register(base);
            dispatcher.register(alias);
        });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(Command cmd) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal(cmd.getName());

        for (Command sub : cmd.getSubCommands()) {
            if (sub != null) {
                root.then(buildCommandTree(sub));
            }
        }

        for (var arg : cmd.getArguments()) {
            if (arg != null) root.then(arg.executes(cmd::onExecute));
        }

        root.executes(cmd::onExecute);
        return root;
    }
}


