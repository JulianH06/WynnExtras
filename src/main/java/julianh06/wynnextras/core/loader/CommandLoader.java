package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.MainScreen;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.command.ChatCommands;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.utils.ItemUtils;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
            LiteralArgumentBuilder<FabricClientCommandSource> base = ClientCommandManager.literal("WynnExtras");
            LiteralArgumentBuilder<FabricClientCommandSource> baseLowerCase = ClientCommandManager.literal("wynnextras");
            LiteralArgumentBuilder<FabricClientCommandSource> alias = ClientCommandManager.literal("we");

            base.executes(commandContext -> {
                MainScreen.open();
                return 1;
            });

            baseLowerCase.executes(commandContext -> {
                MainScreen.open();
                return 1;
            });

            alias.executes(commandContext -> {
                MainScreen.open();
                return 1;
            });

            for (Command cmd: Command.COMMAND_LIST) {
                if((cmd instanceof SubCommand)) continue;
                base = base.then(buildCommandTree(cmd));
                alias = alias.then(buildCommandTree(cmd));
            }

            dispatcher.register(base);
            dispatcher.register(baseLowerCase);
            dispatcher.register(alias);
            dispatcher.register(ChatCommands.register());
            dispatcher.register(
                    ClientCommandManager.literal("pv")
                            .executes(ctx -> {
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

            dispatcher.register(
                    ClientCommandManager.literal("gv")
                            .executes(ctx -> {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You need to specify the guild you want to view. Usage: /gv [guild prefix]"));
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
                ClientCommandManager.literal("dwoc").executes(ctx -> {
                    McUtils.player().networkHandler.sendChatCommand("emote explode");
                    SCHEDULER.schedule(() -> {
                        MinecraftClient.getInstance().execute(() -> {
                            McUtils.playSoundUI(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                        });
                    }, 600, TimeUnit.MILLISECONDS);
                    return 1;
                })
            );

            // Raid Loot Tracker reset commands and debug commands - combined under single /we
            dispatcher.register(
                ClientCommandManager.literal("we")
                    .then(ClientCommandManager.literal("raidloot")
                        .then(ClientCommandManager.literal("reset")
                            .executes(ctx -> {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we raidloot reset <all|session|notg|nol|tcc|tna>"));
                                return 1;
                            })
                            .then(ClientCommandManager.literal("all")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetAll();
                                    RaidLootConfig.INSTANCE.save();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset all raid loot data!"));
                                    return 1;
                                }))
                            .then(ClientCommandManager.literal("session")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetSession();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset session raid loot data!"));
                                    return 1;
                                }))
                            .then(ClientCommandManager.literal("notg")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetRaid("NOTG");
                                    RaidLootConfig.INSTANCE.save();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset NOTG raid loot data!"));
                                    return 1;
                                }))
                            .then(ClientCommandManager.literal("nol")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetRaid("NOL");
                                    RaidLootConfig.INSTANCE.save();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset NOL raid loot data!"));
                                    return 1;
                                }))
                            .then(ClientCommandManager.literal("tcc")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetRaid("TCC");
                                    RaidLootConfig.INSTANCE.save();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset TCC raid loot data!"));
                                    return 1;
                                }))
                            .then(ClientCommandManager.literal("tna")
                                .executes(ctx -> {
                                    RaidLootConfig.INSTANCE.data.resetRaid("TNA");
                                    RaidLootConfig.INSTANCE.save();
                                    RaidLootTrackerOverlay.refreshData();
                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset TNA raid loot data!"));
                                    return 1;
                                }))
                        )
                    )
                    .then(ClientCommandManager.literal("debug")
                        .then(ClientCommandManager.literal("slot")
                            .executes(ctx -> {
                                TradeMarketComparisonPanel.toggleSlotDebug();
                                return 1;
                            })
                        )
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

        ArgumentBuilder<FabricClientCommandSource, ?> args = chainArguments(cmd.getArguments(), cmd);
        if(args != null) current = current.then(args);

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
