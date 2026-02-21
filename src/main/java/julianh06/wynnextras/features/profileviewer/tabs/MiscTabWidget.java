package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Global;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MiscTabWidget extends PVScreen.TabWidget {
    static Identifier miscBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground.png");
    static Identifier miscBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground_dark.png");

    static List<String> gameModeKeys = List.of("huntedContent", "craftsmanContent", "huicContent", "ironmanContent", "ultimateIronmanContent", "hardcoreLegacyLevel", "hardcoreContent", "huichContent", "hicContent", "hichContent");

    public MiscTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        Global data = PV.currentPlayerData.getGlobalData();
        Map<String, Long> rankingData = PV.currentPlayerData.getRanking();

        int gameModeRankingsY = 400;
        if(data == null) {
            gameModeRankingsY = 140;
            if(rankingData.isEmpty()) {
                ui.drawCenteredText("This player has their misc stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
                return;
            }
            PVScreen.DarkModeToggleWidget.drawImageWithFade(miscBackgroundTextureDark, miscBackgroundTexture, x + 30, y + 30, 1740, 690, ui);
            ui.drawText("Misc stats not available due to privacy settings.", x + 60, y + 60, CustomColor.fromHexString("FFFFFF"),3f);
        } else {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(miscBackgroundTextureDark, miscBackgroundTexture, x + 30, y + 30, 1740, 690, ui);
            ui.drawText("Wars completed: " + data.getWars(), x + 60, y + 50, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Dungeons completed: " + data.getDungeons().getTotal(), x + 60, y + 80, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Unique Caves completed: " + data.getCaves(), x + 60, y + 110, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Unique Lootrun camps completed: " + data.getLootruns(), x + 60, y + 140, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Unique World events completed: " + data.getWorldEvents(), x + 60, y + 170, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Chests opened: " + data.getChestsFound(), x + 60, y + 200, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Mobs killed: " + data.getMobsKilled(), x + 60, y + 230, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Pvp kills: " + data.getPvp().getKills(), x + 60, y + 260, CustomColor.fromHexString("FFFFFF"),3f);
            ui.drawText("Pvp deaths: " + data.getPvp().getDeaths(), x + 60, y + 290, CustomColor.fromHexString("FFFFFF"),3f);
        }

        Map<String, Long> gameModeData = rankingData.entrySet()
                .stream()
                .filter(entry -> gameModeKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if(gameModeData.isEmpty()) return;

        ui.drawText("Gamemode rankings: ", x + 60, y + gameModeRankingsY - 40, CustomColor.fromHexString("FFFFFF"),4f);

        for(String key : gameModeData.keySet()) {
            if(rankingData.get(key) <= 0) continue;
            ui.drawText(key + ": " + rankingData.get(key), x + 60, y + gameModeRankingsY, CustomColor.fromHexString("FFFFFF"), 3f);
            gameModeRankingsY += 30;
        }
    }
}
