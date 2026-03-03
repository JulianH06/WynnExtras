package julianh06.wynnextras.features.spellhider;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@WEModule
public class SpellHiderCommands {
    @SubscribeEvent
    public void registerCommands(CommandRegistrationEvent empty) {
        RequiredArgumentBuilder<FabricClientCommandSource, String> nameSpaceArg =
                ClientCommandManager.argument("namespace", StringArgumentType.string())
                        .suggests(SpellHiderCommands::nameSpaceSelector);

        SubCommand modifyCmd = new SubCommand(
                "modify",
                "modify a spells vfx",
                context -> {
                    String nameSpace = StringArgumentType.getString(context, "namespace");
                    SpellModifier modifier = SpellModifier.from(StringArgumentType.getString(context, "modifier"));
                    String value = StringArgumentType.getString(context, "value");
                    if (modifier == null) {
                        ChatUtils.sendMessage("invalid modifier");
                        return 0;
                    }
                    if (nameSpace == null || nameSpace.isEmpty()) { // TODO force existing namespace
                        ChatUtils.sendMessage("invalid namespace");
                        return 0;
                    }
                    Object parsedValue = null;
                    switch (modifier) {
                        case SCALE -> parsedValue = modifier.parseValue(value, Vector3f.class);
                        case VISIBLE -> parsedValue = modifier.parseValue(value, Boolean.class);
                    }
                    if (parsedValue == null) {
                        ChatUtils.sendMessage("invalid value");
                        return 0;
                    }
                    boolean modify = SpellNamespace.from(nameSpace).modify(modifier, parsedValue);
                    if (modify) {
                        ChatUtils.sendMessage("set " + nameSpace + "'s " + modifier.name() + " to " + value);
                        return 1;
                    } else {
                        ChatUtils.sendMessage("Somehow parsed to wrong class (my fault not yours)");
                        return 0;
                    }
                },
                null,
                List.of(
                        nameSpaceArg,
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

        SubCommand getModificationsCmd = new SubCommand(
                "getModifications",
                "display all modified spells",
                context -> {
                    return 0;
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
                    boolean success = ModelDataLogger.progressQueue(nameSpace);
                    if (success) ChatUtils.sendMessage("Added " + itemPath + " to " + nameSpace);
                    return 1;
                },
                null,
                List.of(nameSpaceArg)
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

        SubCommand mapModelToNamespaceCmd = new SubCommand(
                "mapModelToNamespace",
                "assigns a name to a custom model data float",
                context -> {
                    return 0;
                },
                null,
                List.of(

                )
        );


        new Command(
                "spellhider",
                "modify the appearance of skill vfx",
                context -> 0,
                List.of(
                        modifyCmd,
                        getModificationsCmd,
                        modelDataLoggerCmd,
                        progressQueueCmd,
                        mapModelToNamespaceCmd
                ),
                null
        );
    }

    private static CompletableFuture<Suggestions> nameSpaceSelector(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String userInput = builder.getRemaining().toLowerCase().replace('"', ' ').trim();
        SpellHider.getAllCurrentNamespaces().stream()
                .filter(spellNameSpace -> spellNameSpace.isRelevant(userInput))
                .forEach(nameSpace -> builder.suggest('"' + nameSpace.getFQName() + '"'));
        return builder.buildFuture();
    }


    private static CompletableFuture<Suggestions> skillModifierSelector(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (SpellModifier spellModifier : SpellModifier.values()) {
            builder.suggest('"' + spellModifier.name() + '"');
        }
        return builder.buildFuture();
    }

}
