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
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.command.ChatCommands;
import julianh06.wynnextras.event.CommandRegistrationEvent;
import julianh06.wynnextras.features.aspects.ScreenTitleDebugger;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.utils.WynncraftApiHandler;
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
import net.minecraft.client.gui.screen.Screen;
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
                        executeBombshare("g", false);
                        return 1;
                    })
                    .then(ClientCommandManager.argument("channel", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("guild");
                                builder.suggest("party");
                                builder.suggest("local");
                                builder.suggest("clipboard");
                                builder.suggest("disable");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                                switch (channel) {
                                    case "guild", "g" -> executeBombshare("g", (Set<BombType>) null);
                                    case "party", "p" -> executeBombshare("p", (Set<BombType>) null);
                                    case "local" -> executeBombshare(null, (Set<BombType>) null);
                                    case "clipboard" -> copyBombshareToClipboard(null);
                                    case "disable" -> {
                                        WynnExtrasConfig.INSTANCE.bombShareSuggestion = false;
                                        WynnExtrasConfig.save();
                                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aBomb share suggestions disabled. Re-enable in /we config > Chat."));
                                    }
                                    default ->
                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel + ". Use guild, party, local or clipboard."));
                                }
                                return 1;
                            })
                            .then(ClientCommandManager.argument("filter", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        builder.suggest("prof");
                                        builder.suggest("loot");
                                        builder.suggest("combat");
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        String channel = StringArgumentType.getString(ctx, "channel").toLowerCase();
                                        String filterStr = StringArgumentType.getString(ctx, "filter").toLowerCase();
                                        Set<BombType> bombFilter = switch (filterStr) {
                                            case "prof" -> PROF_BOMBS;
                                            case "loot" -> LOOT_BOMBS;
                                            case "combat" -> COMBAT_BOMBS;
                                            default -> { McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown filter: " + filterStr + ". Use prof, loot, or combat.")); yield null; }
                                        };
                                        if (bombFilter == null) return 1;
                                        switch (channel) {
                                            case "guild", "g" -> executeBombshare("g", bombFilter);
                                            case "party", "p" -> executeBombshare("p", bombFilter);
                                            case "local" -> executeBombshare(null, bombFilter);
                                            case "clipboard" -> copyBombshareToClipboard(bombFilter);
                                            default ->
                                                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown channel: " + channel));
                                        }
                                        return 1;
                                    })));
            base = base.then(bombshare);
            alias = alias.then(bombshare);

            var hide = ClientCommandManager.literal("hide")
                    .executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.playerHiderToggle = !WynnExtrasConfig.INSTANCE.playerHiderToggle;
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.playerHiderToggle ? "§aEnabled Player Hider" : "§cDisabled Player Hider"));
                        WynnExtrasConfig.save();
                        return 1;
                    })
                    .then(ClientCommandManager.literal("war").executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.hideAllPlayersInWar = !WynnExtrasConfig.INSTANCE.hideAllPlayersInWar;
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.hideAllPlayersInWar
                                        ? "§aEnabled Hide All Players in Wars (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                                        : "§cDisabled Hide All Players in Wars"));
                        WynnExtrasConfig.save();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("all").executes(ctx -> {
                        WynnExtrasConfig.INSTANCE.hideAllPlayers = !WynnExtrasConfig.INSTANCE.hideAllPlayers;
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                                WynnExtrasConfig.INSTANCE.hideAllPlayers
                                        ? "§aEnabled Hide All Players (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                                        : "§cDisabled Hide All Players"));
                        WynnExtrasConfig.save();
                        return 1;
                    }));
            base = base.then(hide);
            alias = alias.then(hide);

            var changelog = ClientCommandManager.literal("changelog").executes(ctx -> {
                MinecraftClient.getInstance().send(() ->
                        MinecraftClient.getInstance().setScreen(new julianh06.wynnextras.config.ChangelogScreen()));
                return 1;
            });
            base = base.then(changelog);
            alias = alias.then(changelog);

            var ignorelist = ClientCommandManager.literal("ignorelist").executes(ctx -> {
                Set<String> ignored = julianh06.wynnextras.features.raid.PartyIgnoreOnRaid.getTrackedIgnored();
                if (ignored.isEmpty()) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7No players tracked as ignored yet. Run /ignore add <player> and the list will populate."));
                } else {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Ignored players (" + ignored.size() + "): §f" + String.join(", ", ignored)));
                }
                return 1;
            });
            base = base.then(ignorelist);
            alias = alias.then(ignorelist);

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

//            dispatcher.register(
//                    ClientCommandManager.literal("ri")
//                            .executes(ctx -> { sendRaidInfo(McUtils.playerName()); return 1; })
//                            .then(ClientCommandManager.argument("player", StringArgumentType.word())
//                                    .executes(ctx -> { sendRaidInfo(StringArgumentType.getString(ctx, "player")); return 1; }))
//            );
//
//            dispatcher.register(
//                    ClientCommandManager.literal("stats")
//                            .executes(ctx -> { sendStats(McUtils.playerName()); return 1; })
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
                    if (McUtils.player() == null) return 0;
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
            if (sub != null) current = current.then(buildCommandTree(sub));
        }

        ArgumentBuilder<FabricClientCommandSource, ?> args = chainArguments(cmd.getArguments(), cmd);
        if (args != null) current = current.then(args);

        current.executes(cmd::onExecute);

        return root;
    }

    private static final Set<BombType> PROF_BOMBS = Set.of(BombType.PROFESSION_XP, BombType.PROFESSION_SPEED);
    private static final Set<BombType> LOOT_BOMBS = Set.of(BombType.LOOT, BombType.LOOT_CHEST);
    private static final Set<BombType> COMBAT_BOMBS = Set.of(BombType.COMBAT_XP);

    private static void executeBombshare(String chatPrefix, boolean profOnly) {
        executeBombshare(chatPrefix, profOnly ? PROF_BOMBS : null);
    }

    private static String filterName(Set<BombType> filter) {
        if (filter == null) return "";
        if (filter.equals(PROF_BOMBS)) return " prof";
        if (filter.equals(LOOT_BOMBS)) return " loot";
        if (filter.equals(COMBAT_BOMBS)) return " combat";
        return "";
    }

    private static void executeBombshare(String chatPrefix, Set<BombType> filter) {
        if (!Models.WorldState.onWorld()) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cYou must be on a world to use this command."));
            return;
        }

        Map<BombType, List<String>> bombsByType = new LinkedHashMap<>();
        for (BombInfo bomb : Models.Bomb.getBombBells()) {
            if (!bomb.isActive()) continue;
            if (filter != null && !filter.contains(bomb.bomb())) continue;
            bombsByType.computeIfAbsent(bomb.bomb(), k -> new ArrayList<>()).add(bomb.server());
        }

        String message;
        if (bombsByType.isEmpty()) {
            message = "[WynnExtras] No active" + filterName(filter) + " bombs!";
        } else {
            StringBuilder sb = new StringBuilder("[WynnExtras]");
            Map<BombType, String> shortNames = Map.of(
                    BombType.PROFESSION_XP, "ProfXP",
                    BombType.PROFESSION_SPEED, "ProfSpeed",
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
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
        } else if (McUtils.player() != null) {
            McUtils.player().networkHandler.sendChatCommand(chatPrefix + " " + message);
        }
    }

    private static void copyBombshareToClipboard(Set<BombType> filter) {
        if (!Models.WorldState.onWorld()) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cYou must be on a world to use this command."));
            return;
        }

        Map<BombType, List<String>> bombsByType = new LinkedHashMap<>();
        for (BombInfo bomb : Models.Bomb.getBombBells()) {
            if (!bomb.isActive()) continue;
            if (filter != null && !filter.contains(bomb.bomb())) continue;
            bombsByType.computeIfAbsent(bomb.bomb(), k -> new ArrayList<>()).add(bomb.server());
        }

        String message;
        if (bombsByType.isEmpty()) {
            message = "[WynnExtras] No active" + filterName(filter) + " bombs!";
        } else {
            StringBuilder sb = new StringBuilder("[WynnExtras]");
            Map<BombType, String> shortNames = Map.of(
                    BombType.PROFESSION_XP, "ProfXP",
                    BombType.PROFESSION_SPEED, "ProfSpeed",
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

        MinecraftClient.getInstance().keyboard.setClipboard(message);
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Copied bombshare to clipboard."));
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
//        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Fetching raid info for §e" + playerName + "§7..."));
//        MinecraftClient mc = MinecraftClient.getInstance();
//        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(data -> mc.execute(() -> {
//            if (data == null || data.getUsername() == null) {
//                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo data found for " + playerName + " (API returned empty or error)."));
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
//                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
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
//            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
//        })).exceptionally(ex -> {
//            mc.execute(() -> McUtils.sendMessageToClient(
//                    WynnExtras.addWynnExtrasPrefix("§cError fetching data: " + ex.getMessage())));
//            return null;
//        });
//    }
//
//    private static void sendStats(String playerName) {
//        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Fetching stats for §e" + playerName + "§7..."));
//        MinecraftClient mc = MinecraftClient.getInstance();
//        WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(data -> mc.execute(() -> {
//            if (data == null || data.getUsername() == null) {
//                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
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
//                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
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
//            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(sb.toString()));
//        })).exceptionally(ex -> {
//            mc.execute(() -> McUtils.sendMessageToClient(
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