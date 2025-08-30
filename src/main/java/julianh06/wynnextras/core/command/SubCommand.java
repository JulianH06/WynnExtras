package julianh06.wynnextras.core.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;
import java.util.function.Function;

public class SubCommand extends Command{
    public SubCommand(String name, String description, Function<CommandContext<FabricClientCommandSource>, Integer> onExecute, List<Command> subCommands, ArgumentBuilder<FabricClientCommandSource, ?> arg) {
        super(name, description, onExecute, subCommands, arg);
    }
}
