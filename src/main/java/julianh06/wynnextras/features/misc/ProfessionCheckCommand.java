package julianh06.wynnextras.features.misc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.crafting.calc.CraftXpCalculator;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.PlayerData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@WEModule
public class ProfessionCheckCommand {
    private static final Command profCheckCmd = new Command(
            "profcheck",
            "",
            context -> {
                check(StringArgumentType.getString(context, "name"));
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("name", StringArgumentType.word()))
    );

    private static final Command profCheckNoArgsCmd = new Command(
            "profcheck",
            "",
            context -> {
                check(McUtils.player().getName().getString());
                return 1;
            }
    );

    private static void check(String name) {
        WynncraftApiHandler.fetchPlayerData(name, true).thenAccept(playerData -> {
            MinecraftClient.getInstance().send(() -> sendResult(name, playerData));
        }).exceptionally(ex -> {
            MinecraftClient.getInstance().send(() -> WynnExtras.sendMessageToClient("§cError fetching profession data: " + ex.getMessage()));
            return null;
        });
    }

    private static void sendResult(String name, PlayerData playerData) {
        if (playerData == null || playerData.getCharacters() == null || playerData.getCharacters().isEmpty()) {
            WynnExtras.sendMessageToClient("§cNo profession data found for " + name + ".");
            return;
        }

        int classCount = playerData.getCharacters().size();
        int totalProfessionLevel = 0;
        long totalProfessionXp = 0;
        long totalProfessionOverflowXp = 0;

        for (CharacterData characterData : playerData.getCharacters().values()) {
            Map<String, Profession> professions = characterData.getProfessions();
            if (professions == null) continue;

            for (Profession profession : professions.values()) {
                totalProfessionLevel += profession.getLevel();

                if (profession.getLevel() >= 132) {
                    long overflowXp = CraftXpCalculator.estimateProfessionOverflowXp(profession.getXpPercent());
                    totalProfessionXp += CraftXpCalculator.estimateProfessionXp(profession.getLevel(), 0) + overflowXp;
                    totalProfessionOverflowXp += overflowXp;
                } else {
                    totalProfessionXp += CraftXpCalculator.estimateProfessionXp(profession.getLevel(), profession.getXpPercent());
                }
            }
        }

        double averageProfessionLevel = classCount == 0 ? 0 : (double) totalProfessionLevel / classCount;
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                "§6Profcheck " + name + "\n" +
                "§7Total profession level: §b" + totalProfessionLevel + "\n"
                        + "§7Total profession xp: §b" + String.format(Locale.US, "%,d", totalProfessionXp) + "\n"
                        + "§7Total profession xp (with overflow): §b" + String.format(Locale.US, "%,d", totalProfessionXp + totalProfessionOverflowXp) + "\n"
                        + "§7Average profession level per class (" + classCount + " classes): §b" + String.format(Locale.US, "%.2f", averageProfessionLevel) + "\n"
                        + "§7Average profession level per profession (" + classCount + " classes): §b" + String.format(Locale.US, "%.2f", averageProfessionLevel / 12)
        ));
    }
}