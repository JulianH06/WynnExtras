package julianh06.wynnextras.core.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.aspects.ScreenTitleDebugger;
import julianh06.wynnextras.features.crafting.calc.ProfessionCalculatorScreen;
import julianh06.wynnextras.features.misc.HudEditScreen;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.misc.SlotNumberDebugger;
import julianh06.wynnextras.features.raid.PartyIgnoreOnRaid;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import julianh06.wynnextras.features.tetris.TetrisScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import julianh06.wynnextras.wynncraft.state.BombState;
import julianh06.wynnextras.wynncraft.state.CharacterState;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WEModule
public final class WynnExtrasCommands {
    private static final Set<String> PROF_BOMBS = Set.of("PROFESSION_XP", "PROFESSION_SPEED");
    private static final Set<String> LOOT_BOMBS = Set.of("LOOT", "LOOT_CHEST");
    private static final Set<String> COMBAT_BOMBS = Set.of("COMBAT_XP");

    private static final SubCommand RAID_LOOT_RESET_ALL = new SubCommand("all", "", context -> {
        RaidLootConfig.INSTANCE.data.resetAll();
        RaidLootConfig.INSTANCE.save();
        RaidLootTrackerOverlay.refreshData();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset all raid loot data!"));
        return 1;
    }, null, null);

    private static final SubCommand RAID_LOOT_RESET_SESSION = new SubCommand("session", "", context -> {
        RaidLootConfig.INSTANCE.data.resetSession();
        RaidLootTrackerOverlay.refreshData();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset session raid loot data!"));
        return 1;
    }, null, null);

    private static final SubCommand RAID_LOOT_RESET_NOTG = raidResetCommand("notg", "NOTG");
    private static final SubCommand RAID_LOOT_RESET_NOL = raidResetCommand("nol", "NOL");
    private static final SubCommand RAID_LOOT_RESET_TCC = raidResetCommand("tcc", "TCC");
    private static final SubCommand RAID_LOOT_RESET_TNA = raidResetCommand("tna", "TNA");

    private static final SubCommand RAID_LOOT_RESET = new SubCommand(
            "reset",
            "resets raid loot data",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§eUsage: /we raidloot reset <all|session|notg|nol|tcc|tna>"));
                return 1;
            },
            List.of(RAID_LOOT_RESET_ALL, RAID_LOOT_RESET_SESSION, RAID_LOOT_RESET_NOTG,
                    RAID_LOOT_RESET_NOL, RAID_LOOT_RESET_TCC, RAID_LOOT_RESET_TNA),
            null
    );

    private static final Command RAID_LOOT = new Command(
            "raidloot",
            "raid loot tracker commands",
            context -> 1,
            List.of(RAID_LOOT_RESET),
            null
    );

    private static final Command GUI = new Command("gui", "", context -> {
        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new HudEditScreen()));
        return 1;
    }, List.of("hud"));

    private static final Command TETRIS = new Command("tetris", context -> {
        TetrisScreen.open();
        return 1;
    });

    private static final SubCommand DEBUG_SLOT = new SubCommand("slot", "", context -> {
        SlotNumberDebugger.toggle();
        return 1;
    }, null, null);

    private static final SubCommand DEBUG_SCREEN = new SubCommand("screen", "", context -> {
        ScreenTitleDebugger.toggleDebug();
        return 1;
    }, null, null);

    private static final Command DEBUG = new Command(
            "debug", "debug commands", context -> 1, List.of(DEBUG_SLOT, DEBUG_SCREEN), null);

    private static final Command PROF = new Command("prof", context -> {
        WEScreen.open(ProfessionCalculatorScreen::new);
        return 1;
    });

    private static final SubCommand PROFESSION_RELOAD = new SubCommand("reload", "", context -> {
        ProfessionOverlay.reload();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aProfession overlay reloaded! Session XP reset, re-fetching data..."));
        return 1;
    }, null, null);

    private static final SubCommand PROFESSION_EXACT = new SubCommand("exact", "", context -> {
        WynnExtrasConfig.INSTANCE.professionOverlayExactXp = !WynnExtrasConfig.INSTANCE.professionOverlayExactXp;
        WynnExtrasConfig.save();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                WynnExtrasConfig.INSTANCE.professionOverlayExactXp
                        ? "§aExact XP numbers enabled"
                        : "§7Exact XP numbers disabled (using short format)"));
        return 1;
    }, null, null);

    private static final SubCommand PROFESSION_SET = new SubCommand(
            "set",
            "sets profession overflow XP",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§eUsage: /we profession set <profession> <amount>"));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("profession", StringArgumentType.word())
                    .then(ClientCommandManager.argument("amount", FloatArgumentType.floatArg(0))
                            .executes(context -> setProfessionOverflow(
                                    StringArgumentType.getString(context, "profession"),
                                    FloatArgumentType.getFloat(context, "amount")))))
    );

    private static final SubCommand PROFESSION_GOAL = new SubCommand(
            "goal",
            "manages profession overflow XP goals",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§eUsage: /we profession goal <profession> <amount|clear>"));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("goalProfession", StringArgumentType.word())
                    .executes(context -> showProfessionGoal(StringArgumentType.getString(context, "goalProfession")))
                    .then(ClientCommandManager.literal("clear")
                            .executes(context -> clearProfessionGoal(
                                    StringArgumentType.getString(context, "goalProfession"))))
                    .then(ClientCommandManager.argument("goalAmount", FloatArgumentType.floatArg(1))
                            .executes(context -> setProfessionGoal(
                                    StringArgumentType.getString(context, "goalProfession"),
                                    FloatArgumentType.getFloat(context, "goalAmount")))))
    );

    private static final Command PROFESSION = new Command(
            "profession",
            "profession overlay commands",
            context -> 1,
            List.of(PROFESSION_RELOAD, PROFESSION_EXACT, PROFESSION_SET, PROFESSION_GOAL),
            null
    );

    private static final Command HIDE = new Command(
            "hide",
            "toggles player hiding",
            context -> togglePlayerHider(),
            List.of(
                    new SubCommand("war", "", context -> toggleWarHiding(), null, null),
                    new SubCommand("all", "", context -> toggleAllHiding(), null, null)
            ),
            null
    );

    private static final Command IGNORE_LIST = new Command("ignorelist", context -> {
        Set<String> ignored = PartyIgnoreOnRaid.getTrackedIgnored();
        if (ignored.isEmpty()) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§7No players tracked as ignored yet. Run /ignore add <player> and the list will populate."));
        } else {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§7Ignored players (" + ignored.size() + "): §f" + String.join(", ", ignored)));
        }
        return 1;
    });

    private static final Command BOMBSHARE = new Command(
            "bombshare",
            "shares active bombs",
            context -> {
                executeBombshare("g", null);
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("channel", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("all");
                        builder.suggest("guild");
                        builder.suggest("party");
                        builder.suggest("local");
                        builder.suggest("clipboard");
                        builder.suggest("toggle");
                        return builder.buildFuture();
                    })
                    .executes(context -> executeBombshare(
                            StringArgumentType.getString(context, "channel"), null))
                    .then(ClientCommandManager.argument("filter", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                builder.suggest("all");
                                builder.suggest("prof");
                                builder.suggest("loot");
                                builder.suggest("combat");
                                return builder.buildFuture();
                            })
                            .executes(context -> executeBombshare(
                                    StringArgumentType.getString(context, "channel"),
                                    StringArgumentType.getString(context, "filter")))))
    );

    private static SubCommand raidResetCommand(String commandName, String raidName) {
        return new SubCommand(commandName, "", context -> {
            RaidLootConfig.INSTANCE.data.resetRaid(raidName);
            RaidLootConfig.INSTANCE.save();
            RaidLootTrackerOverlay.refreshData();
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§aReset " + raidName + " raid loot data!"));
            return 1;
        }, null, null);
    }

    private static int setProfessionOverflow(String professionName, float amount) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;
        String characterId = CharacterState.id().orElse(null);
        String className = CharacterState.className().orElse("unknown");
        if (characterId == null || characterId.isEmpty()) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§cNo character detected. Make sure you're logged into a class."));
            return 0;
        }
        ProfessionOverlay.setOverflow(profession, amount);
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aSet " + profession.getDisplayName() + " overflow XP to " + String.format("%.0f", amount)
                        + " §7(class: " + className + ")"));
        return 1;
    }

    private static int showProfessionGoal(String professionName) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;
        float goal = ProfessionOverlay.getGoal(profession);
        float overflow = ProfessionOverlay.getOverflow(profession);
        if (goal <= 0) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§7No goal set for " + profession.getDisplayName() + ". Current overflow: "
                            + String.format("%.0f", overflow)));
        } else {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§b" + profession.getDisplayName() + " goal: " + String.format("%.0f", goal)
                            + " | Current: " + String.format("%.0f", overflow)
                            + " | Remaining: " + String.format("%.0f", Math.max(0, goal - overflow))));
        }
        return 1;
    }

    private static int clearProfessionGoal(String professionName) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null || !hasCharacter()) return 0;
        ProfessionOverlay.clearGoal(profession);
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aCleared " + profession.getDisplayName() + " goal."));
        return 1;
    }

    private static int setProfessionGoal(String professionName, float amount) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;
        String className = CharacterState.className().orElse("unknown");
        if (!hasCharacter()) return 0;
        ProfessionOverlay.setGoal(profession, amount);
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aSet " + profession.getDisplayName() + " goal to " + String.format("%.0f", amount)
                        + " overflow XP §7(class: " + className + ")"));
        return 1;
    }

    private static WEProfessionType parseProfession(String professionName) {
        WEProfessionType profession = WEProfessionType.fromString(professionName);
        if (profession == null) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "§cUnknown profession: " + professionName));
        }
        return profession;
    }

    private static boolean hasCharacter() {
        String characterId = CharacterState.id().orElse(null);
        if (characterId != null && !characterId.isEmpty()) return true;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo character detected."));
        return false;
    }

    private static int togglePlayerHider() {
        WynnExtrasConfig.INSTANCE.playerHiderToggle = !WynnExtrasConfig.INSTANCE.playerHiderToggle;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                WynnExtrasConfig.INSTANCE.playerHiderToggle
                        ? "§aEnabled Player Hider"
                        : "§cDisabled Player Hider"));
        WynnExtrasConfig.save();
        return 1;
    }

    private static int toggleWarHiding() {
        WynnExtrasConfig.INSTANCE.hideAllPlayersInWar = !WynnExtrasConfig.INSTANCE.hideAllPlayersInWar;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                WynnExtrasConfig.INSTANCE.hideAllPlayersInWar
                        ? "§aEnabled Hide All Players in Wars (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                        : "§cDisabled Hide All Players in Wars"));
        WynnExtrasConfig.save();
        return 1;
    }

    private static int toggleAllHiding() {
        WynnExtrasConfig.INSTANCE.hideAllPlayers = !WynnExtrasConfig.INSTANCE.hideAllPlayers;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                WynnExtrasConfig.INSTANCE.hideAllPlayers
                        ? "§aEnabled Hide All Players (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")"
                        : "§cDisabled Hide All Players"));
        WynnExtrasConfig.save();
        return 1;
    }

    private static int executeBombshare(String channel, String filterName) {
        Set<String> filter;
        switch (filterName == null ? "all" : filterName.toLowerCase()) {
            case "all" -> filter = null;
            case "prof" -> filter = PROF_BOMBS;
            case "loot" -> filter = LOOT_BOMBS;
            case "combat" -> filter = COMBAT_BOMBS;
            default -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§cUnknown filter: " + filterName + ". Use prof, loot, or combat."));
                return 0;
            }
        }
        switch (channel.toLowerCase()) {
            case "all", "a" -> executeBombshareAll(filter);
            case "guild", "g" -> sendBombshareCommand("g", filter);
            case "party", "p" -> sendBombshareCommand("p", filter);
            case "local" -> MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(buildBombshare(filter)));
            case "clipboard" -> copyBombshareToClipboard(filter);
            case "toggle" -> toggleBombshareSuggestions();
            default -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§cUnknown channel: " + channel + ". Use all, guild, party, local, clipboard or toggle."));
                return 0;
            }
        }
        return 1;
    }

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
            String name = shortNames.getOrDefault(entry.getKey(),
                    displayNames.getOrDefault(entry.getKey(), entry.getKey()));
            message.append(" [").append(name).append("] ").append(String.join(", ", entry.getValue()));
        }
        return message.toString();
    }

    private static void sendBombshareCommand(String chatPrefix, Set<String> filter) {
        if (MinecraftUtils.player() != null) {
            MinecraftUtils.player().networkHandler.sendChatCommand(chatPrefix + " " + buildBombshare(filter));
        }
    }

    private static void executeBombshareAll(Set<String> filter) {
        if (MinecraftUtils.player() != null) {
            MinecraftUtils.player().networkHandler.sendChatMessage(buildBombshare(filter));
        }
    }

    private static void copyBombshareToClipboard(Set<String> filter) {
        MinecraftClient.getInstance().keyboard.setClipboard(buildBombshare(filter));
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Copied bombshare to clipboard."));
    }

    private static void toggleBombshareSuggestions() {
        WynnExtrasConfig.INSTANCE.bombShareSuggestion = !WynnExtrasConfig.INSTANCE.bombShareSuggestion;
        WynnExtrasConfig.save();
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                WynnExtrasConfig.INSTANCE.bombShareSuggestion
                        ? "§aBomb share suggestions enabled."
                        : "§aBomb share suggestions disabled."));
    }
}
