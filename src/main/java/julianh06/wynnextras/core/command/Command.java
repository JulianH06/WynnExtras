package julianh06.wynnextras.core.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Command {
    static public final List<Command> COMMAND_LIST = new ArrayList<>();

    private final String name;

    private final String description;

    private final Function<CommandContext<FabricClientCommandSource>, Integer> onExecute;

    private final List<Command> subCommands = new ArrayList<>();

    private final List<ArgumentBuilder<FabricClientCommandSource, ?>> arguments = new ArrayList<>();

    public Command(String name, String description, Function<CommandContext<FabricClientCommandSource>, Integer> onExecute, List<Command> subCommands, List<ArgumentBuilder<FabricClientCommandSource, ?>> args) {
        this.name = name;
        this.description = description;
        this.onExecute = onExecute;
        if(subCommands != null) this.subCommands.addAll(subCommands);
        if(args != null) arguments.addAll(args);

        COMMAND_LIST.add(this);
    }

    public Command(String name, String description, Function<CommandContext<FabricClientCommandSource>, Integer> onExecute) {
        this(name, description, onExecute, null, null);
    }

    public Command(String name, Function<CommandContext<FabricClientCommandSource>, Integer> onExecute) {
        this(name, "", onExecute, null, null);
    }

    public String getName() {
        return name;
    }

    public List<Command> getSubCommands() {
        return subCommands;
    }

    public List<ArgumentBuilder<FabricClientCommandSource, ?>> getArguments() {
        return arguments;
    }

    public Integer onExecute(CommandContext<FabricClientCommandSource> ctx) {
        if (onExecute != null) {
            return onExecute.apply(ctx);
        }
        return null;
    }
}
