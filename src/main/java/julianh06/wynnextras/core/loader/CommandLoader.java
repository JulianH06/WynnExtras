package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wynntils.core.components.Models;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.MainScreen;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.command.ChatCommands;
import julianh06.wynnextras.event.CommandRegistrationEvent;
import julianh06.wynnextras.features.aspects.ScreenTitleDebugger;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.crafting.calc.ProfessionCalculatorScreen;
import julianh06.wynnextras.features.misc.HudEditScreen;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.tetris.TetrisScreen;
import julianh06.wynnextras.utils.ItemUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.*;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandLoader implements WELoader {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public CommandLoader() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            new CommandRegistrationEvent().post();

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

            var bombshare = ClientCommandManager.literal("bombshare")
                .executes(ctx -> { executeBombshare("g", false); return 1; })
                .then(ClientCommandManager.argument("channel", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("guild");
                        builder.suggest("party");
                        builder.suggest("all");
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                        switch (channel) {
                            case "guild", "g" -> executeBombshare("g", false);
                            case "party", "p" -> executeBombshare("p", false);
                            case "all" -> executeBombshare(null, false);
                            case "disable" -> {
                                WynnExtrasConfig.INSTANCE.bombShareSuggestion = false;
                                WynnExtrasConfig.save();
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aBomb share suggestions disabled. Re-enable in /we config > Chat."));
                            }
                            default -> McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel + ". Use guild, party, or all."));
                        }
                        return 1;
                    })
                    .then(ClientCommandManager.argument("filter", StringArgumentType.word())
                        .suggests((ctx, builder) -> { builder.suggest("prof"); return builder.buildFuture(); })
                        .executes(ctx -> {
                            String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                            String filter = StringArgumentType.getString(ctx, "filter").toLowerCase();
                            boolean profOnly = filter.equals("prof");
                            switch (channel) {
                                case "guild", "g" -> executeBombshare("g", profOnly);
                                case "party", "p" -> executeBombshare("p", profOnly);
                                case "all" -> executeBombshare(null, profOnly);
                                default -> McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel));
                            }
                            return 1;
                        })));
            base = base.then(bombshare);
            alias = alias.then(bombshare);

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
                    .then(ClientCommandManager.literal("gui")
                        .executes(ctx -> {
                            MinecraftClient.getInstance().send(() -> {
                                MinecraftClient.getInstance().setScreen(new HudEditScreen());
                            });
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("tetris")
                        .executes(ctx -> {
                            TetrisScreen.open();
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("debug")
                        .then(ClientCommandManager.literal("slot")
                            .executes(ctx -> {
                                TradeMarketComparisonPanel.toggleSlotDebug();
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("screen")
                            .executes(ctx -> {
                                ScreenTitleDebugger.toggleDebug();
                                return 1;
                            })
                        )
                    )
                    .then(ClientCommandManager.literal("prof")
                        .executes(ctx -> {
                            WEScreen.open(ProfessionCalculatorScreen::new);
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("profession")
                        .then(ClientCommandManager.literal("reload")
                            .executes(ctx -> {
                                ProfessionOverlay.reload();
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aProfession overlay reloaded! Session XP reset, re-fetching data..."));
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("exact")
                            .executes(ctx -> {
                                WynnExtrasConfig.INSTANCE.professionOverlayExactXp = !WynnExtrasConfig.INSTANCE.professionOverlayExactXp;
                                WynnExtrasConfig.save();
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                    WynnExtrasConfig.INSTANCE.professionOverlayExactXp ? "§aExact XP numbers enabled" : "§7Exact XP numbers disabled (using short format)"));
                                return 1;
                            })
                        )
                        .then(ClientCommandManager.literal("set")
                            .executes(ctx -> {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we profession set <profession> <amount>"));
                                return 1;
                            })
                            .then(ClientCommandManager.argument("profession", StringArgumentType.word())
                                .then(ClientCommandManager.argument("amount", FloatArgumentType.floatArg(0))
                                    .executes(ctx -> {
                                        String profName = StringArgumentType.getString(ctx, "profession");
                                        float amount = FloatArgumentType.getFloat(ctx, "amount");
                                        ProfessionType prof = ProfessionType.fromString(profName);
                                        if (prof == null) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                            return 0;
                                        }
                                        String charId = Models.Character.getId();
                                        String className = Models.Character.getClassType() != null ? Models.Character.getClassType().getName() : "unknown";
                                        if (charId == null || charId.isEmpty()) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected. Make sure you're logged into a class."));
                                            return 0;
                                        }
                                        ProfessionOverlay.setOverflow(prof, amount);
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aSet " + prof.getDisplayName() + " overflow XP to " + String.format("%.0f", amount) + " §7(class: " + className + ")"));
                                        return 1;
                                    })
                                )
                            )
                        )
                        .then(ClientCommandManager.literal("goal")
                            .executes(ctx -> {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we profession goal <profession> <amount|clear>"));
                                return 1;
                            })
                            .then(ClientCommandManager.argument("goalProfession", StringArgumentType.word())
                                .executes(ctx -> {
                                    String profName = StringArgumentType.getString(ctx, "goalProfession");
                                    ProfessionType prof = ProfessionType.fromString(profName);
                                    if (prof == null) {
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                        return 0;
                                    }
                                    float goal = ProfessionOverlay.getGoal(prof);
                                    float overflow = ProfessionOverlay.getOverflow(prof);
                                    if (goal <= 0) {
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7No goal set for " + prof.getDisplayName() + ". Current overflow: " + String.format("%.0f", overflow)));
                                    } else {
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§b" + prof.getDisplayName() + " goal: " + String.format("%.0f", goal) + " | Current: " + String.format("%.0f", overflow) + " | Remaining: " + String.format("%.0f", Math.max(0, goal - overflow))));
                                    }
                                    return 1;
                                })
                                .then(ClientCommandManager.literal("clear")
                                    .executes(ctx -> {
                                        String profName = StringArgumentType.getString(ctx, "goalProfession");
                                        ProfessionType prof = ProfessionType.fromString(profName);
                                        if (prof == null) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                            return 0;
                                        }
                                        String charId = Models.Character.getId();
                                        if (charId == null || charId.isEmpty()) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected."));
                                            return 0;
                                        }
                                        ProfessionOverlay.clearGoal(prof);
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aCleared " + prof.getDisplayName() + " goal."));
                                        return 1;
                                    })
                                )
                                .then(ClientCommandManager.argument("goalAmount", FloatArgumentType.floatArg(1))
                                    .executes(ctx -> {
                                        String profName = StringArgumentType.getString(ctx, "goalProfession");
                                        float amount = FloatArgumentType.getFloat(ctx, "goalAmount");
                                        ProfessionType prof = ProfessionType.fromString(profName);
                                        if (prof == null) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                            return 0;
                                        }
                                        String charId = Models.Character.getId();
                                        String className = Models.Character.getClassType() != null ? Models.Character.getClassType().getName() : "unknown";
                                        if (charId == null || charId.isEmpty()) {
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected."));
                                            return 0;
                                        }
                                        ProfessionOverlay.setGoal(prof, amount);
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aSet " + prof.getDisplayName() + " goal to " + String.format("%.0f", amount) + " overflow XP §7(class: " + className + ")"));
                                        return 1;
                                    })
                                )
                            )
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

    private static final Set<BombType> PROF_BOMBS = Set.of(BombType.PROFESSION_XP, BombType.PROFESSION_SPEED);

    private static void executeBombshare(String chatPrefix, boolean profOnly) {
        if (!Models.WorldState.onWorld()) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cYou must be on a world to use this command."));
            return;
        }

        Map<BombType, List<String>> bombsByType = new LinkedHashMap<>();
        for (BombInfo bomb : Models.Bomb.getBombBells()) {
            if (!bomb.isActive()) continue;
            if (profOnly && !PROF_BOMBS.contains(bomb.bomb())) continue;
            bombsByType.computeIfAbsent(bomb.bomb(), k -> new ArrayList<>()).add(bomb.server());
        }

        String message;
        if (bombsByType.isEmpty()) {
            message = "[WynnExtras] No active Bombs!";
        } else {
            StringBuilder sb = new StringBuilder("[WynnExtras]");
            Map<BombType, String> shortNames = Map.of(
                BombType.PROFESSION_XP, "ProfXP",
                BombType.PROFESSION_SPEED, "ProfSpd",
                BombType.COMBAT_XP, "CombatXP",
                BombType.DUNGEON, "Dungeon",
                BombType.LOOT, "Loot",
                BombType.LOOT_CHEST, "LootChest"
            );
            for (var entry : bombsByType.entrySet()) {
                String name = shortNames.getOrDefault(entry.getKey(), entry.getKey().getDisplayName());
                sb.append(" [").append(name).append("] ").append(String.join(", ", entry.getValue()));
            }
            message = sb.toString();
        }

        if (chatPrefix == null) {
            // "all" - just show locally
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
        } else {
            McUtils.player().networkHandler.sendChatCommand(chatPrefix + " " + message);
        }
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
