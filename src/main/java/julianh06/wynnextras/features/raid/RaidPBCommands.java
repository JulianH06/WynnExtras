package julianh06.wynnextras.features.raid;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.chat.RaidChatNotifier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@WEModule
public class RaidPBCommands {
    private static final SubCommand listCmd = new SubCommand(
            "list",
            "",
            context -> {
                String filter = getOptionalArgument(context, "filter");
                Map<String, Long> pbs = RaidChatNotifier.getRaidPBs();
                List<Map.Entry<String, Long>> filteredPBs = pbs.entrySet().stream()
                        .filter(entry -> filter == null || entry.getKey().toLowerCase().contains(filter.toLowerCase()))
                        .sorted(Map.Entry.comparingByKey())
                        .toList();

                if (filteredPBs.isEmpty()) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(filter == null ? "§7No raid PBs saved." : "§7No raid PBs found for §e" + filter + "§7."));
                    return 1;
                }

                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(filter == null ? "§eRaid PBs:" : "§eRaid PBs matching §f" + filter + "§e:"));
                filteredPBs.stream()
                        .forEach(entry -> McUtils.sendMessageToClient(Text.of("§7" + entry.getKey() + ": §f" + RaidChatNotifier.formatTime(entry.getValue()))));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("filter", StringArgumentType.greedyString()))
    );

    private static final SubCommand resetAllCmd = new SubCommand(
            "all",
            "",
            context -> {
                int count = RaidChatNotifier.resetAllPBs();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset " + count + " raid PB" + (count == 1 ? "" : "s") + "."));
                return 1;
            },
            null,
            null
    );

    private static final SubCommand resetCmd = new SubCommand(
            "reset",
            "",
            RaidPBCommands::resetPB,
            List.of(resetAllCmd),
            List.of(ClientCommandManager.argument("pb", StringArgumentType.greedyString())
                    .suggests(RaidPBCommands::suggestPBKeys))
    );

    private static final Command pbsCmd = new Command(
            "pbs",
            "",
            context -> {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§eUsage: /we pbs list, /we pbs reset <pb>, /we pbs reset all"));
                return 1;
            },
            List.of(listCmd, resetCmd),
            null
    );

    private static int resetPB(CommandContext<FabricClientCommandSource> context) {
        String key;
        try {
            key = StringArgumentType.getString(context, "pb");
        } catch (IllegalArgumentException ignored) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUsage: /we pbs reset <pb> or /we pbs reset all"));
            return 0;
        }

        String matchingKey = findPBKey(key);
        if (matchingKey != null && RaidChatNotifier.resetPB(matchingKey)) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aReset raid PB §e" + matchingKey + "§a."));
            return 1;
        }

        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnknown raid PB: " + key));
        return 0;
    }

    private static String getOptionalArgument(CommandContext<FabricClientCommandSource> context, String name) {
        try {
            return StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String findPBKey(String input) {
        return RaidChatNotifier.getRaidPBs().keySet().stream()
                .filter(key -> key.equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    private static CompletableFuture<Suggestions> suggestPBKeys(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        RaidChatNotifier.getRaidPBs().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}