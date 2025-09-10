package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.command.ChatCommands;
import julianh06.wynnextras.features.profileviewer.PV;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public class CommandLoader implements WELoader {
    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> base = ClientCommandManager.literal("WynnExtras");
            LiteralArgumentBuilder<FabricClientCommandSource> alias = ClientCommandManager.literal("we");

            for (Command cmd: Command.COMMAND_LIST) {
                if((cmd instanceof SubCommand)) continue;
                base = base.then(buildCommandTree(cmd));
                alias = alias.then(buildCommandTree(cmd));
            }

            dispatcher.register(base);
            dispatcher.register(alias);
            dispatcher.register(ChatCommands.register());
            dispatcher.register(
                    ClientCommandManager.literal("pv")
                            .executes(ctx -> {
                                // Kein Argument
                                PV.open(McUtils.playerName());
                                return 1;
                            })
                            .then(
                                    ClientCommandManager.argument("player", StringArgumentType.word())
                                            .executes(ctx -> {
                                                String arg = StringArgumentType.getString(ctx, "player");
                                                PV.open(arg);
                                                return 1;
                                            })
                            )
            );
        });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(Command cmd) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal(cmd.getName());

        ArgumentBuilder<FabricClientCommandSource, ?> current = root;

        for (Command sub : cmd.getSubCommands()) {
            if(sub != null) current = current.then(buildCommandTree(sub));
        }

        for (ArgumentBuilder<FabricClientCommandSource, ?> arg : cmd.getArguments()) {
            if(arg != null) current = current.then(arg.executes(cmd::onExecute));
        }

        current.executes(cmd::onExecute);

        return root;
    }

}
