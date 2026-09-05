package julianh06.wynnextras.features.misc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.crafting.calc.CraftXpCalculator;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;

@WEModule
public class ProfessionLevelGoalCommand {
    private static final RequiredArgumentBuilder<FabricClientCommandSource, String> PROFESSION_ARGUMENT =
            ClientCommandManager.argument("profession", StringArgumentType.word())
                    .executes(context -> showGoal(StringArgumentType.getString(context, "profession")))
                    .then(ClientCommandManager.literal("clear")
                            .executes(context -> clearGoal(StringArgumentType.getString(context, "profession"))))
                    .then(ClientCommandManager.argument("level",
                                    IntegerArgumentType.integer(1, CraftXpCalculator.CURVE_MAX_LEVEL))
                            .executes(context -> setGoal(
                                    StringArgumentType.getString(context, "profession"),
                                    IntegerArgumentType.getInteger(context, "level"))));

    private static final SubCommand LEVEL_GOAL_COMMAND = new SubCommand(
            "levelgoal",
            "sets a target profession level",
            context -> {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§eUsage: /we profession levelgoal <profession> <level|clear>"));
                return 1;
            },
            null,
            List.of(PROFESSION_ARGUMENT)
    );

    private static final Command PROFESSION_COMMAND = new Command(
            "profession",
            "",
            context -> 1,
            List.of(LEVEL_GOAL_COMMAND),
            null
    );

    private static int showGoal(String professionName) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;

        int goal = ProfessionOverlay.getLevelGoal(profession);
        String message = goal > 0
                ? "§b" + profession.getDisplayName() + " level goal: §f" + goal
                : "§7No level goal set for " + profession.getDisplayName() + ".";
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
        return 1;
    }

    private static int setGoal(String professionName, int level) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;

        ProfessionOverlay.setLevelGoal(profession, level);
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aSet " + profession.getDisplayName() + " goal to §flevel " + level + "§a."));
        return 1;
    }

    private static int clearGoal(String professionName) {
        WEProfessionType profession = parseProfession(professionName);
        if (profession == null) return 0;

        ProfessionOverlay.clearLevelGoal(profession);
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§aCleared " + profession.getDisplayName() + " level goal."));
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
}
