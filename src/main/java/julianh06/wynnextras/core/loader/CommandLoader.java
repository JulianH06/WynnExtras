package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.profileviewer.PV;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandLoader implements WELoader {
    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> base = literal("WynnExtras");
            LiteralArgumentBuilder<FabricClientCommandSource> alias = literal("we");
//            LiteralArgumentBuilder<FabricClientCommandSource> pvAlias = literal("pv");

            for (Command cmd: Command.COMMAND_LIST) {
                if((cmd instanceof SubCommand)) continue;
                base = base.then(buildCommandTree(cmd));
                alias = alias.then(buildCommandTree(cmd));

//                if(cmd.getName().equalsIgnoreCase("pv")) {
//                    dispatcher.register(pvAlias.redirect(buildCommandTree(cmd).build()));
//                }
            }

            dispatcher.register(base);
            dispatcher.register(alias);
            dispatcher.register(
                    literal("pv")
                            .executes(ctx -> {
                                // Kein Argument
                                PV.open(McUtils.playerName());
                                return 1;
                            })
                            .then(
                                    argument("player", StringArgumentType.word())
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
        LiteralArgumentBuilder<FabricClientCommandSource> root = literal(cmd.getName());

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
