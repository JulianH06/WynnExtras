package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;

@WEModule
public class LV {
    private static Command lvCmd = new Command(
            "lv",
            "",
            context -> {
                open();
                return 1;
            },
            null,
            null
    );
    private static final Command seasonCmd = new Command(
            "season",
            context -> {
                LeaderboardService.fetchLeaderboardTypes().thenAccept(types -> {
                    String leaderboardId = LeaderboardCatalog.latestGuildSeasonId(types);
                    MinecraftClient.getInstance().execute(() -> {
                        if (leaderboardId == null) open();
                        else open(leaderboardId);
                    });
                }).exceptionally(error -> {
                    MinecraftClient.getInstance().execute(LV::open);
                    return null;
                });
                return 1;
            }
    );

    public static void open() {
        WEScreen.open(LVScreen::new);
    }

    public static void open(String leaderboardId) {
        WEScreen.open(() -> new LVScreen(leaderboardId));
    }
}
