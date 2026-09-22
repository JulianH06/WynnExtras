package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import julianh06.wynnextras.features.leaderboardviewer.LV;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.core.command.ChatCommands;
import julianh06.wynnextras.event.CommandRegistrationEvent;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import net.minecraft.client.gui.screen.Screen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandLoader implements WELoader {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            new CommandRegistrationEvent().post();

            for (String rootName : List.of("WynnExtras", "wynnextras", "we", "We", "WE")) {
                dispatcher.register(buildRootCommand(rootName));
            }
            dispatcher.register(ChatCommands.register());
            dispatcher.register(ChatCommands.registerAlias());

            dispatcher.register(
                    ClientCommandManager.literal("pv")
                            .executes(ctx -> {
                                PV.open(MinecraftUtils.playerName());
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

            dispatcher.register(
                    ClientCommandManager.literal("gv")
                            .executes(ctx -> {
                                GV.openOwnGuild();
                                return 1;
                            })
                            .then(
                                    ClientCommandManager.argument("prefix", StringArgumentType.word())
                                            .executes(ctx -> {
                                                String arg = StringArgumentType.getString(ctx, "prefix");
                                                GV.open(arg);
                                                return 1;
                                            })
                            )
            );

            dispatcher.register(
                    ClientCommandManager.literal("lv")
                            .executes(ctx -> {
                                LV.open();
                                return 1;
                            })
            );

            dispatcher.register(
                ClientCommandManager.literal("dwoc").executes(ctx -> {
                    if (MinecraftUtils.player() == null) return 0;
                    MinecraftUtils.player().networkHandler.sendChatCommand("emote explode");
                    SCHEDULER.schedule(() -> {
                        MinecraftClient.getInstance().execute(() -> {
                            MinecraftUtils.playSoundUI(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                        });
                    }, 600, TimeUnit.MILLISECONDS);
                    return 1;
                })
            );

        });
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildRootCommand(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal(name)
                .executes(commandContext -> {
                    Screen configScreen = WynnExtrasConfig.createConfigScreen(null);
                    MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(configScreen));
                    return 1;
                });
        for (Command command : Command.COMMAND_LIST) {
            if (!(command instanceof SubCommand)) root = root.then(buildCommandTree(command));
        }
        return root;
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(Command cmd) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal(cmd.getName());

        ArgumentBuilder<FabricClientCommandSource, ?> current = root;

        for (Command sub : cmd.getSubCommands()) {
            if (sub != null) current = current.then(buildCommandTree(sub));
        }

        ArgumentBuilder<FabricClientCommandSource, ?> args = chainArguments(cmd.getArguments(), cmd);
        if (args != null) current = current.then(args);

        current.executes(cmd::onExecute);

        return root;
    }

    public static ArgumentBuilder<FabricClientCommandSource, ?> chainArguments(
            List<ArgumentBuilder<FabricClientCommandSource, ?>> args,
            Command cmd
    ) {
        if (args.isEmpty()) return null;

        ArgumentBuilder<FabricClientCommandSource, ?> head = args.getFirst();
        if (args.size() == 1) {
            return head.executes(cmd::onExecute);
        } else {
            return head.then(chainArguments(args.subList(1, args.size()), cmd));
        }
    }

}