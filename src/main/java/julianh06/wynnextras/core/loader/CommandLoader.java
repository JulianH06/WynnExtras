package julianh06.wynnextras.core.loader;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import julianh06.wynnextras.wynncraft.state.BombState;
import julianh06.wynnextras.wynncraft.state.CharacterState;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.core.command.ChatCommands;
import julianh06.wynnextras.event.CommandRegistrationEvent;
import julianh06.wynnextras.features.aspects.ScreenTitleDebugger;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.crafting.calc.ProfessionCalculatorScreen;
import julianh06.wynnextras.features.misc.HudEditScreen;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.misc.SlotNumberDebugger;
import julianh06.wynnextras.features.tetris.TetrisScreen;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.screen.Screen;
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
                Screen configScreen = WynnExtrasConfig.createConfigScreen(null);
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(configScreen));
                return 1;
            });

            baseLowerCase.executes(commandContext -> {
                Screen configScreen = WynnExtrasConfig.createConfigScreen(null);
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(configScreen));
                return 1;
            });

            alias.executes(commandContext -> {
                Screen configScreen = WynnExtrasConfig.createConfigScreen(null);
                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(configScreen));
                return 1;
            });

            for (Command cmd : Command.COMMAND_LIST) {
                if ((cmd instanceof SubCommand)) continue;
                base = base.then(buildCommandTree(cmd));
                alias = alias.then(buildCommandTree(cmd));
            }

            var bombshare = ClientCommandManager.literal("bombshare")
                    .executes(ctx -> {
                        executeBombshare("g", null);
                        return 1;
                    })
                    .then(ClientCommandManager.argument("channel", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("all");
                                builder.suggest("guild");
                                builder.suggest("party");
                                builder.suggest("local");
                                builder.suggest("clipboard");
                                builder.suggest("toggle");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                                switch (channel) {
                                    case "all", "a" -> executeBombshareAll(null);
                                    case "guild", "g" -> executeBombshare("g", null);
                                    case "party", "p" -> executeBombshare("p", null);
                                    case "local" -> executeBombshare(null, null);
                                    case "clipboard" -> copyBombshareToClipboard(null);
                                    case "toggle" -> {
                                        WynnExtrasConfig.INSTANCE.bombShareSuggestion = !WynnExtrasConfig.INSTANCE.bombShareSuggestion;
                                        WynnExtrasConfig.save();
                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(WynnExtrasConfig.INSTANCE.bombShareSuggestion
                                                ? "§aBomb share suggestions enabled."
                                                : "§aBomb share suggestions disabled."));
                                    }
                                    default ->
                                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel + ". Use all, guild, party, local, clipboard or toggle."));
                                }
                                return 1;
                            })
                            .then(ClientCommandManager.argument("filter", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        builder.suggest("all");
                                        builder.suggest("prof");
                                        builder.suggest("loot");
                                        builder.suggest("combat");
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                                        String filterStr = StringArgumentType.getString(ctx, "filter").toLowerCase();
                                        Set<String> bombFilter = switch (filterStr) {
                                            case "all" -> null;
                                            case "prof" -> PROF_BOMBS;
                                            case "loot" -> LOOT_BOMBS;
                                            case "combat" -> COMBAT_BOMBS;
                                            default -> { MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown filter: " + filterStr + ". Use prof, loot, or combat.")); yield null; }
                                        };
                                        switch (channel) {
                                            case "all", "a" -> executeBombshareAll(bombFilter);
                                            case "guild", "g" -> executeBombshare("g", bombFilter);
                                            case "party", "p" -> executeBombshare("p", bombFilter);
                                            case "local" -> executeBombshare(null, bombFilter);
                                            case "clipboard" -> copyBombshareToClipboard(bombFilter);
                                            default ->
                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel));
                                        }
                                        return 1;
                                    })));
            base = base.then(bombshare);
            alias = alias.then(bombshare);

            var hide = ClientCommandManager.literal("hide")
                    .executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.playerHiderToggle = !WynnExtrasConfig.INSTANCE.playerHiderToggle;
                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.playerHiderToggle ? "§aEnabled Player Hider" : "§cDisabled Player Hider"));
                        WynnExtrasConfig.save();
                        return 1;
                    })
                    .then(ClientCommandManager.literal("war").executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.hideAllPlayersInWar = !WynnExtrasConfig.INSTANCE.hideAllPlayersInWar;
                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.hideAllPlayersInWar
                                        ? "§aEnabled Hide All Players in Wars (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                                        : "§cDisabled Hide All Players in Wars"));
                        WynnExtrasConfig.save();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("all").executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.hideAllPlayers = !WynnExtrasConfig.INSTANCE.hideAllPlayers;
                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.hideAllPlayers
                                        ? "§aEnabled Hide All Players (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                                        : "§cDisabled Hide All Players"));
                        WynnExtrasConfig.save();
                        return 1;
                    }));
            base = base.then(hide);
            alias = alias.then(hide);

//            var changelog = ClientCommandManager.literal("changelog").executes(ctx -> {
//                MinecraftClient.getInstance().send(() ->
//                        MinecraftClient.getInstance().setScreen(new julianh06.wynnextras.config.ChangelogScreen()));
//                return 1;
//            });
//            base = base.then(changelog);
//            alias = alias.then(changelog);

            var ignorelist = ClientCommandManager.literal("ignorelist").executes(ctx -> {
                Set<String> ignored = julianh06.wynnextras.features.raid.PartyIgnoreOnRaid.getTrackedIgnored();
                if (ignored.isEmpty()) {
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7No players tracked as ignored yet. Run /ignore add <player> and the list will populate."));
                } else {
                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Ignored players (" + ignored.size() + "): §f" + String.join(", ", ignored)));
                }
                return 1;
            });
            base = base.then(ignorelist);
            alias = alias.then(ignorelist);

            dispatcher.register(base);
            dispatcher.register(baseLowerCase);
            dispatcher.register(alias);
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

//            dispatcher.register(
//                    ClientCommandManager.literal("ri")
//                            .executes(ctx -> { sendRaidInfo(MinecraftUtils.playerName()); return 1; })
//                            .then(ClientCommandManager.argument("player", StringArgumentType.word())
//                                    .executes(ctx -> { sendRaidInfo(StringArgumentType.getString(ctx, "player")); return 1; }))
//            );
//
//            dispatcher.register(
//                    ClientCommandManager.literal("stats")
//                            .executes(ctx -> { sendStats(MinecraftUtils.playerName()); return 1; })
//                            .then(ClientCommandManager.argument("player", StringArgumentType.word())
//                                    .executes(ctx -> { sendStats(StringArgumentType.getString(ctx, "player")); return 1; }))
//            );

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

            // Raid Loot Tracker reset commands and debug commands - combined under single /we
            dispatcher.register(
                    ClientCommandManager.literal("we")
                            .then(ClientCommandManager.literal("raidloot")
                                    .then(ClientCommandManager.literal("reset")
                                            .executes(ctx -> {
                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we raidloot reset <all|session|notg|nol|tcc|tna>"));
                                                return 1;
                                            })
                                            .then(ClientCommandManager.literal("all")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetAll();
                                                        RaidLootConfig.INSTANCE.save();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset all raid loot data!"));
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("session")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetSession();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset session raid loot data!"));
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("notg")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetRaid("NOTG");
                                                        RaidLootConfig.INSTANCE.save();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset NOTG raid loot data!"));
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("nol")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetRaid("NOL");
                                                        RaidLootConfig.INSTANCE.save();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset NOL raid loot data!"));
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("tcc")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetRaid("TCC");
                                                        RaidLootConfig.INSTANCE.save();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset TCC raid loot data!"));
                                                        return 1;
                                                    }))
                                            .then(ClientCommandManager.literal("tna")
                                                    .executes(ctx -> {
                                                        RaidLootConfig.INSTANCE.data.resetRaid("TNA");
                                                        RaidLootConfig.INSTANCE.save();
                                                        RaidLootTrackerOverlay.refreshData();
                                                        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset TNA raid loot data!"));
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
                                                SlotNumberDebugger.toggle();
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
                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aProfession overlay reloaded! Session XP reset, re-fetching data..."));
                                                return 1;
                                            })
                                    )
                                    .then(ClientCommandManager.literal("exact")
                                            .executes(ctx -> {
                                                WynnExtrasConfig.INSTANCE.professionOverlayExactXp = !WynnExtrasConfig.INSTANCE.professionOverlayExactXp;
                                                WynnExtrasConfig.save();
                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                                        WynnExtrasConfig.INSTANCE.professionOverlayExactXp ? "§aExact XP numbers enabled" : "§7Exact XP numbers disabled (using short format)"));
                                                return 1;
                                            })
                                    )
                                    .then(ClientCommandManager.literal("set")
                                            .executes(ctx -> {
                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we profession set <profession> <amount>"));
                                                return 1;
                                            })
                                            .then(ClientCommandManager.argument("profession", StringArgumentType.word())
                                                    .then(ClientCommandManager.argument("amount", FloatArgumentType.floatArg(0))
                                                            .executes(ctx -> {
                                                                String profName = StringArgumentType.getString(ctx, "profession");
                                                                float amount = FloatArgumentType.getFloat(ctx, "amount");
                                                                WEProfessionType prof = WEProfessionType.fromString(profName);
                                                                if (prof == null) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                                                    return 0;
                                                                }
                                                                String charId = CharacterState.id().orElse(null);
                                                                String className = CharacterState.className().orElse("unknown");
                                                                if (charId == null || charId.isEmpty()) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected. Make sure you're logged into a class."));
                                                                    return 0;
                                                                }
                                                                ProfessionOverlay.setOverflow(prof, amount);
                                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aSet " + prof.getDisplayName() + " overflow XP to " + String.format("%.0f", amount) + " §7(class: " + className + ")"));
                                                                return 1;
                                                            })
                                                    )
                                            )
                                    )
                                    .then(ClientCommandManager.literal("goal")
                                            .executes(ctx -> {
                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we profession goal <profession> <amount|clear>"));
                                                return 1;
                                            })
                                            .then(ClientCommandManager.argument("goalProfession", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String profName = StringArgumentType.getString(ctx, "goalProfession");
                                                        WEProfessionType prof = WEProfessionType.fromString(profName);
                                                        if (prof == null) {
                                                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                                            return 0;
                                                        }
                                                        float goal = ProfessionOverlay.getGoal(prof);
                                                        float overflow = ProfessionOverlay.getOverflow(prof);
                                                        if (goal <= 0) {
                                                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7No goal set for " + prof.getDisplayName() + ". Current overflow: " + String.format("%.0f", overflow)));
                                                        } else {
                                                            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§b" + prof.getDisplayName() + " goal: " + String.format("%.0f", goal) + " | Current: " + String.format("%.0f", overflow) + " | Remaining: " + String.format("%.0f", Math.max(0, goal - overflow))));
                                                        }
                                                        return 1;
                                                    })
                                                    .then(ClientCommandManager.literal("clear")
                                                            .executes(ctx -> {
                                                                String profName = StringArgumentType.getString(ctx, "goalProfession");
                                                                WEProfessionType prof = WEProfessionType.fromString(profName);
                                                                if (prof == null) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                                                    return 0;
                                                                }
                                                                String charId = CharacterState.id().orElse(null);
                                                                if (charId == null || charId.isEmpty()) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected."));
                                                                    return 0;
                                                                }
                                                                ProfessionOverlay.clearGoal(prof);
                                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aCleared " + prof.getDisplayName() + " goal."));
                                                                return 1;
                                                            })
                                                    )
                                                    .then(ClientCommandManager.argument("goalAmount", FloatArgumentType.floatArg(1))
                                                            .executes(ctx -> {
                                                                String profName = StringArgumentType.getString(ctx, "goalProfession");
                                                                float amount = FloatArgumentType.getFloat(ctx, "goalAmount");
                                                                WEProfessionType prof = WEProfessionType.fromString(profName);
                                                                if (prof == null) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown profession: " + profName));
                                                                    return 0;
                                                                }
                                                                String charId = CharacterState.id().orElse(null);
                                                                String className = CharacterState.className().orElse("unknown");
                                                                if (charId == null || charId.isEmpty()) {
                                                                    MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected."));
                                                                    return 0;
                                                                }
                                                                ProfessionOverlay.setGoal(prof, amount);
                                                                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aSet " + prof.getDisplayName() + " goal to " + String.format("%.0f", amount) + " overflow XP §7(class: " + className + ")"));
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
            if (sub != null) current = current.then(buildCommandTree(sub));
        }

        ArgumentBuilder<FabricClientCommandSource, ?> args = chainArguments(cmd.getArguments(), cmd);
        if (args != null) current = current.then(args);

        current.executes(cmd::onExecute);

        return root;
    }

    private static final Set<String> PROF_BOMBS = Set.of("PROFESSION_XP", "PROFESSION_SPEED");
    private static final Set<String> LOOT_BOMBS = Set.of("LOOT", "LOOT_CHEST");
    private static final Set<String> COMBAT_BOMBS = Set.of("COMBAT_XP");

    private static String filterName(Set<String> filter) {
        if (filter == null) return "";
        if (filter.equals(PROF_BOMBS)) return " prof";
        if (filter.equals(LOOT_BOMBS)) return " loot";
        if (filter.equals(COMBAT_BOMBS)) return " combat";
        return "";
    }

    private static String buildBombshare(Set<String> filter) {
        Map<String, List<String>> bombsByType = new LinkedHashMap<>();
        Map<String, String> displayNames = new HashMap<>();
        for (BombState.Bomb bomb : BombState.bombs()) {
            if (!bomb.active() || filter != null && !filter.contains(bomb.type())) continue;
            displayNames.put(bomb.type(), bomb.displayName());
            bombsByType.computeIfAbsent(bomb.type(), ignored -> new ArrayList<>()).add(bomb.server());
        }
        if (bombsByType.isEmpty()) return "[WynnExtras] No active" + filterName(filter) + " bombs!";

        Map<String, String> shortNames = Map.of(
                "PROFESSION_XP", "ProfXP", "PROFESSION_SPEED", "ProfSpeed",
                "COMBAT_XP", "CombatXP", "DUNGEON", "Dungeon", "LOOT", "Loot", "LOOT_CHEST", "LootChest");
        StringBuilder message = new StringBuilder("[WynnExtras]");
        for (var entry : bombsByType.entrySet()) {
            String name = shortNames.getOrDefault(entry.getKey(), displayNames.getOrDefault(entry.getKey(), entry.getKey()));
            message.append(" [").append(name).append("] ").append(String.join(", ", entry.getValue()));
        }
        return message.toString();
    }

    private static boolean canShareBombs() {
        if (MinecraftUtils.isOnWynncraft()) return true;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cYou must be on a world to use this command."));
        return false;
    }

    private static void executeBombshare(String chatPrefix, Set<String> filter) {
        if (!canShareBombs()) return;
        String message = buildBombshare(filter);
        if (chatPrefix == null) MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
        else if (MinecraftUtils.player() != null) MinecraftUtils.player().networkHandler.sendChatCommand(chatPrefix + " " + message);
    }

    private static void executeBombshareAll(Set<String> filter) {
        if (canShareBombs() && MinecraftUtils.player() != null) {
            MinecraftUtils.player().networkHandler.sendChatMessage(buildBombshare(filter));
        }
    }

    private static void copyBombshareToClipboard(Set<String> filter) {
        if (!canShareBombs()) return;
        MinecraftClient.getInstance().keyboard.setClipboard(buildBombshare(filter));
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Copied bombshare to clipboard."));
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

//    private static void sendRaidInfo(String playerName) {
//        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Fetching raid info for §e" + playerName + "§7..."));
//        MinecraftClient mc = MinecraftClient.getInstance();
//        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(data -> mc.execute(() -> {
//            if (data == null || data.getUsername() == null) {
//                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo data found for " + playerName + " (API returned empty or error)."));
//                return;
//            }
//            Raids raids = (data.getGlobalData() != null) ? data.getGlobalData().getRaids() : null;
//            Map<String, Integer> list;
//            int total;
//            if (raids != null && raids.getList() != null) {
//                list = raids.getList();
//                total = raids.getTotal();
//            } else {
//                // Fallback: aggregate raid completions from per-character data.
//                list = new HashMap<>();
//                total = 0;
//                if (data.getCharacters() != null) {
//                    for (CharacterData ch : data.getCharacters().values()) {
//                        if (ch.getRaids() == null) continue;
//                        total += ch.getRaids().getTotal();
//                        if (ch.getRaids().getList() != null) {
//                            for (Map.Entry<String, Integer> e : ch.getRaids().getList().entrySet()) {
//                                list.merge(e.getKey(), e.getValue(), Integer::sum);
//                            }
//                        }
//                    }
//                }
//            }
//            if (list.isEmpty() && total == 0) {
//                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
//                        "§cNo raid data returned by the API. Run §e/we apikey§c for info on setting an API key."));
//                return;
//            }
//            int twp = list.getOrDefault("The Wartorn Palace", 0);
//            if (twp == 0) twp = list.getOrDefault("unknown", 0);
//
//            StringBuilder sb = new StringBuilder();
//            sb.append("§6§l").append(data.getUsername()).append("§r §7— §eTotal: §f").append(total).append("\n");
//            sb.append("§7NOTG: §f").append(list.getOrDefault("Nest of the Grootslangs", 0));
//            sb.append(" §7| NOL: §f").append(list.getOrDefault("Orphion's Nexus of Light", 0));
//            sb.append(" §7| TCC: §f").append(list.getOrDefault("The Canyon Colossus", 0));
//            sb.append("\n§7TNA: §f").append(list.getOrDefault("The Nameless Anomaly", 0));
//            sb.append(" §7| TWP: §f").append(twp);
//            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
//        })).exceptionally(ex -> {
//            mc.execute(() -> MinecraftUtils.sendMessageToClient(
//                    WynnExtras.addWynnExtrasPrefix("§cError fetching data: " + ex.getMessage())));
//            return null;
//        });
//    }
//
//    private static void sendStats(String playerName) {
//        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Fetching stats for §e" + playerName + "§7..."));
//        MinecraftClient mc = MinecraftClient.getInstance();
//        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(data -> mc.execute(() -> {
//            if (data == null || data.getUsername() == null) {
//                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
//                        "§cNo data for " + playerName + ". If this is an authenticated request (fullResult), your API key may be missing/invalid. Run §e/we apikey§c for info."));
//                return;
//            }
//            StringBuilder sb = new StringBuilder();
//            int charCount = data.getCharacters() != null ? data.getCharacters().size() : 0;
//            sb.append("§6§l").append(data.getUsername()).append("§r §7— ").append(charCount).append(" characters");
//            if (data.getGlobalData() != null && data.getGlobalData().getRaids() != null) {
//                sb.append(" §7| Raids: §f").append(data.getGlobalData().getRaids().getTotal());
//            }
//            sb.append("\n");
//
//            if (data.getCharacters() == null || data.getCharacters().isEmpty()) {
//                sb.append("§7(per-character data not in response — run §e/we apikey§7 for info on setting an API key)");
//                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
//                return;
//            }
//
//            List<Map.Entry<String, CharacterData>> sorted = new ArrayList<>(data.getCharacters().entrySet());
//            sorted.sort((a, b) -> Integer.compare(b.getValue().getLevel(), a.getValue().getLevel()));
//
//            int i = 0;
//            for (Map.Entry<String, CharacterData> e : sorted) {
//                CharacterData ch = e.getValue();
//                String uuid = e.getKey();
//                String className = ch.getType() != null ? formatClassName(ch.getType()) : "?";
//
//                sb.append("§e").append(className).append(" §7Lv§f").append(ch.getLevel())
//                  .append(" §7(Total §f").append(ch.getTotalLevel()).append("§7)");
//
//                // Raids completed
//                if (ch.getRaids() != null) {
//                    sb.append(" §7Raids:§f").append(ch.getRaids().getTotal());
//                }
//
//                // Profession summary - show highest 3
//                if (ch.getProfessions() != null && !ch.getProfessions().isEmpty()) {
//                    List<Map.Entry<String, Profession>> profs = new ArrayList<>(ch.getProfessions().entrySet());
//                    profs.sort((a, b) -> Integer.compare(b.getValue().getLevel(), a.getValue().getLevel()));
//                    sb.append(" §7Profs: ");
//                    int shown = 0;
//                    for (Map.Entry<String, Profession> p : profs) {
//                        if (shown >= 3) break;
//                        if (p.getValue().getLevel() <= 0) continue;
//                        if (shown > 0) sb.append("§7,");
//                        sb.append("§f").append(p.getKey(), 0, Math.min(4, p.getKey().length()))
//                          .append(" §f").append(p.getValue().getLevel());
//                        shown++;
//                    }
//                }
//                sb.append("\n");
//                if (++i >= 10) break; // cap at 10 chars so chat doesn't explode
//            }
//            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
//        })).exceptionally(ex -> {
//            mc.execute(() -> MinecraftUtils.sendMessageToClient(
//                    WynnExtras.addWynnExtrasPrefix("§cError fetching data: " + ex.getMessage())));
//            return null;
//        });
//    }

    private static String formatClassName(String type) {
        if (type == null || type.isEmpty()) return "?";
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
    }

}

//TODO: clean up this mess
