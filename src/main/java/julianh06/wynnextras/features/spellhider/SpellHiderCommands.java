package julianh06.wynnextras.features.spellhider;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.event.CommandRegistrationEvent;
import julianh06.wynnextras.utils.ChatUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.neoforged.bus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@WEModule
public class SpellHiderCommands {
    @SubscribeEvent
    public void registerCommands(CommandRegistrationEvent empty) {
        SubCommand getModificationsCmd = new SubCommand(
                "getModifications",
                "display all modified spells",
                context -> {
                    String s = SpellHider.getAllModifiersAsDisplay();
                    ChatUtils.sendMessage(s);
                    return 1;
                },
                null,
                null
        );

        SubCommand progressQueueCmd = new SubCommand(
                "progressQueue",
                "add the opened file to the provides namespace and open the next file",
                context -> {
                    String nameSpace = StringArgumentType.getString(context, "namespace");
                    String itemPath = ModelDataLogger.peekQueue();
                    boolean b = ModelDataLogger.progressQueue(nameSpace);
                    if (b) ChatUtils.sendMessage("Added " + itemPath + " to " + nameSpace);
                    return 1;
                },
                null,
                List.of(ClientCommandManager.argument("namespace", StringArgumentType.string())
                        .suggests(SpellHiderCommands::nameSpaceSelector))
        );

        SubCommand massAddCmd = new SubCommand(
                "massAdd",
                "add all seen since the last addition to the given namespace",
                context -> {
                    String nameSpace = StringArgumentType.getString(context, "namespace");
                    ModelDataLogger.addAll(nameSpace);
                    return 1;
                },
                null,
                List.of(ClientCommandManager.argument("namespace", StringArgumentType.string())
                        .suggests(SpellHiderCommands::nameSpaceSelector))
        );

        SubCommand modelDataLoggerCmd = new SubCommand(
                "modelDataLogger",
                "set the state of the model data logger",
                context -> {
                    ModelDataLogger.State state = ModelDataLogger.State.from(StringArgumentType.getString(context, "state"));
                    if (state == null) {
                        ChatUtils.sendMessage("invalid state");
                        return 0;
                    }
                    if (state == ModelDataLogger.State.GET_CURRENT) {
                        ChatUtils.sendMessage("current state is " + ModelDataLogger.getCurrentState().name());
                        return 1;
                    }
                    ModelDataLogger.setState(state);
                    ChatUtils.sendMessage("set state to " + state);
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("state", StringArgumentType.string())
                                .suggests((CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) -> {
                                    Arrays.stream(ModelDataLogger.State.values()).forEach(state -> builder.suggest(state.name()));
                                    return builder.buildFuture();
                                })
                )
        );

        SubCommand displayModeCmd = new SubCommand(
                "displayMode",
                "set the state of the display",
                context -> {
                    ModelDataLogger.DisplayState state = ModelDataLogger.DisplayState.from(StringArgumentType.getString(context, "state"));
                    if (state == null) {
                        ChatUtils.sendMessage("invalid state");
                        return 0;
                    }
                    if (state == ModelDataLogger.DisplayState.GET_CURRENT) {
                        ChatUtils.sendMessage("current state is " + ModelDataLogger.getDisplayState().name() + " (Warning: this feature is experimental!)");
                        return 1;
                    }
                    ModelDataLogger.setDisplayState(state);
                    ChatUtils.sendMessage("set state to " + state + " (Warning: this feature is experimental!)");
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("state", StringArgumentType.string())
                                .suggests((CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) -> {
                                    Arrays.stream(ModelDataLogger.DisplayState.values()).forEach(state -> builder.suggest(state.name()));
                                    return builder.buildFuture();
                                })
                )
        );

        SubCommand saveCmd = new SubCommand(
                "save",
                "save mappings",
                context -> {
                    SpellHiderMappings.saveToFile();
                    return 1;
                },
                null,
                null
        );

        SubCommand loadCmd = new SubCommand(
                "reloadFromFile",
                "load mappings",
                context -> {
                    SpellHiderMappings.reloadFromFile();
                    return 1;
                },
                null,
                null
        );

        SubCommand renameCmd = new SubCommand(
                "renameNamespace",
                "change the name of a namespace",
                context -> {
                    String oldName = StringArgumentType.getString(context, "oldName");
                    String newName = StringArgumentType.getString(context, "newName");
                    SpellHiderMappings.INSTANCE.changeNamespace(oldName, newName);
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("oldName", StringArgumentType.string())
                                .suggests(SpellHiderCommands::nameSpaceSelector),
                        ClientCommandManager.argument("newName", StringArgumentType.string())
                                .suggests(SpellHiderCommands::nameSpaceSelector)
                )
        );

        SubCommand recentAdditionsCmd = new SubCommand(
                "listNextBatch",
                "list additions since lass mass add call",
                context -> {
                    Set<Integer> recentHashes = ModelDataLogger.getRecentHashes();
                    for (Integer recentHash : recentHashes) {
                        ChatUtils.sendMessage(String.valueOf(recentHash));
                    }
                    ChatUtils.sendMessage(recentHashes.size() + " new unknown hashes (Warning: this feature is experimental!)");
                    return 1;
                },
                null,
                null
        );

        SubCommand fineTuneCmd = new SubCommand(
                "fineTune",
                "adds everything in a namespace to the queue for potential remapping",
                context -> {
                    String FQName = StringArgumentType.getString(context, "namespace");
                    Set<SpellData> current = SpellHider.getFromName(FQName);
                    ModelDataLogger.addForFineTuning(current);
                    ModelDataLogger.progressQueue("");
                    ChatUtils.sendMessage("added " + current.size() + " items to queue (Warning: this feature is experimental!)");
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("namespace", StringArgumentType.string())
                                .suggests(SpellHiderCommands::nameSpaceSelector)
                )
        );

        SubCommand modifyCmd = new SubCommand(
                "modify",
                "modify a spells vfx",
                context -> {
                    Triple<SpellNamespace, SpellModifier, Object> data = validateModifiers(context);
                    if (data == null) return 0;

                    boolean modify = data.getLeft().modify(data.getMiddle(), data.getRight());
                    if (modify) {
                        ChatUtils.sendMessage("set " + data.getLeft().getFQName() + "'s " + data.getMiddle().name() + " to " + data.getRight() + "  (Warning: this feature is experimental!)");
                        SpellHider.saveModifiers();
                        return 1;
                    } else {
                        ChatUtils.sendMessage("Somehow parsed to wrong class (my fault not yours)");
                        return 0;
                    }
                },
                null,
                List.of(
                        ClientCommandManager.argument("namespace", StringArgumentType.string())
                                .suggests(SpellHiderCommands::nameSpaceSelector),
                        ClientCommandManager.argument("modifier", StringArgumentType.string())
                                .suggests(SpellHiderCommands::skillModifierSelector),
                        ClientCommandManager.argument("value", StringArgumentType.string())
                                .suggests((CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) -> {
                                    SpellModifier modifier = SpellModifier.from(StringArgumentType.getString(context, "modifier"));
                                    if (modifier != null) {
                                        modifier.getSuggestions().forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                )
        );

        SubCommand addToProfileCmd = new SubCommand(
                "addToProfile",
                "add a modification to a profile",
                context -> {
                    String profileName = StringArgumentType.getString(context, "profile");
                    if (profileName == null || profileName.isEmpty() || !SpellProfiles.getProfileNames().contains(profileName)) {
                        ChatUtils.sendMessage("invalid profile");
                        return 0;
                    }

                    Triple<SpellNamespace, SpellModifier, Object> data = validateModifiers(context);
                    if (data == null) return 0;

                    boolean modify = SpellProfiles.addToProfile(profileName, data.getLeft(), data.getMiddle(), data.getRight());
                    if (modify) {
                        ChatUtils.sendMessage("set " + data.getLeft().getFQName() + "'s " + data.getMiddle().name() + " to " + data.getRight() + " in profile " + profileName + "  (Warning: this feature is experimental)");
                        return 1;
                    } else {
                        ChatUtils.sendMessage("profile doesnt exist");
                        return 0;
                    }
                },
                null,
                List.of(
                        ClientCommandManager.argument("profile", StringArgumentType.string())
                                .suggests(SpellHiderCommands::profileSelector),
                        ClientCommandManager.argument("namespace", StringArgumentType.string())
                                .suggests(SpellHiderCommands::nameSpaceSelector),
                        ClientCommandManager.argument("modifier", StringArgumentType.string())
                                .suggests(SpellHiderCommands::skillModifierSelector),
                        ClientCommandManager.argument("value", StringArgumentType.string())
                                .suggests((CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) -> {
                                    SpellModifier modifier = SpellModifier.from(StringArgumentType.getString(context, "modifier"));
                                    if (modifier != null) {
                                        modifier.getSuggestions().forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                )
        );

        SubCommand createProfileCmd = new SubCommand(
                "createProfile",
                "create a new profile",
                context -> {
                    String name = StringArgumentType.getString(context, "name");
                    SpellProfiles.createProfile(name);
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("name", StringArgumentType.string())
                )
        );

        SubCommand saveToProfileCmd = new SubCommand(
                "saveCurrentToProfileCmd",
                "saves your current modification as a profile",
                context -> {
                    String name = StringArgumentType.getString(context, "name");
                    SpellProfiles.createProfile(name);
                    SpellProfiles.addAll(SpellHider.getAllModifiers());
                    return 1;
                },
                null,
                List.of(
                        ClientCommandManager.argument("name", StringArgumentType.string())
                )
        );

        SubCommand devCmd = new SubCommand(
                "development",
                "assigns a name to a custom model data float",
                context -> 0,
                List.of(
                        progressQueueCmd,
                        modelDataLoggerCmd,
                        massAddCmd,
                        renameCmd,
                        recentAdditionsCmd,
                        displayModeCmd,
                        fineTuneCmd,
                        createProfileCmd,
                        addToProfileCmd,
                        saveCmd,
                        loadCmd
                ),
                null
        );

        SubCommand resetCmd = new SubCommand(
                "reset",
                "reset the spell hider to default settings",
                context -> {
                    SpellHider.modifiersMap.clear();
                    SpellHider.saveModifiers();
                    SpellHider.resetRenderedState();
                    ChatUtils.sendMessage("Spell hider reset to defaults.");
                    return 1;
                },
                null,
                null
        );

        new Command(
                "spellhider",
                "modify the appearance of skill vfx",
                context -> 0,
                List.of(
                        modifyCmd,
                        devCmd,
                        getModificationsCmd,
                        resetCmd
                ),
                null
        );
    }

    private static CompletableFuture<Suggestions> nameSpaceSelector(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String userInput = builder.getRemaining().toLowerCase().replace('"', ' ').trim();
        SpellHiderMappings.INSTANCE.getAllNamespaces().stream()
                .filter(spellNameSpace -> spellNameSpace.isRelevant(userInput))
                .forEach(nameSpace -> builder.suggest('"' + nameSpace.getFQName() + '"'));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> profileSelector(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String userInput = builder.getRemaining().toLowerCase().replace('"', ' ').trim();
        SpellProfiles.getProfileNames().stream()
                .filter(profileName -> profileName.contains(userInput))
                .forEach(profileName -> builder.suggest('"' + profileName + '"'));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> skillModifierSelector(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (SpellModifier spellModifier : SpellModifier.values()) {
            builder.suggest('"' + spellModifier.name() + '"');
        }
        return builder.buildFuture();
    }

    private static Triple<SpellNamespace, SpellModifier, Object> validateModifiers(CommandContext<FabricClientCommandSource> context) {
        String FQName = StringArgumentType.getString(context, "namespace");
        SpellModifier modifier = SpellModifier.from(StringArgumentType.getString(context, "modifier"));
        String value = StringArgumentType.getString(context, "value");
        if (modifier == null) {
            ChatUtils.sendMessage("invalid modifier");
            return null;
        }
        if (FQName == null || FQName.isEmpty() || !SpellHiderMappings.INSTANCE.namespaceExists(FQName)) {
            ChatUtils.sendMessage("invalid namespace");
            return null;
        }
        Object parsedValue = null;
        switch (modifier) {
            case SCALE -> parsedValue = modifier.parseValue(value, Vector3f.class);
            case VISIBLE -> parsedValue = modifier.parseValue(value, Boolean.class);
        }
        if (parsedValue == null) {
            ChatUtils.sendMessage("invalid value");
            return null;
        }
        return Triple.of(SpellNamespace.from(FQName), modifier, parsedValue);
    }

}
