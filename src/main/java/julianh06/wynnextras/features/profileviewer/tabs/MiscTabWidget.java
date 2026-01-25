package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Global;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class MiscTabWidget extends PVScreen.TabWidget {
    static Identifier miscBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground.png");
    static Identifier miscBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/miscpagebackground_dark.png");


    public MiscTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        Global data = PV.currentPlayerData.getGlobalData();
        if(data == null) {
            ui.drawCenteredText("This player has their misc stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(miscBackgroundTextureDark, miscBackgroundTexture, x + 30, y + 30, 1740, 690, ui);

        ui.drawText("Wars completed: " + data.getWars(), x + 60, y + 60, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Dungeons completed: " + data.getDungeons().getTotal(), x + 60, y + 120, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Unique Caves completed: " + data.getCaves(), x + 60, y + 180, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Unique Lootrun camps completed: " + data.getLootruns(), x + 60, y + 240, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Unique World events completed: " + data.getWorldEvents(), x + 60, y + 300, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Chests opened: " + data.getChestsFound(), x + 60, y + 360, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Mobs killed: " + data.getMobsKilled(), x + 60, y + 420, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Pvp kills: " + data.getPvp().getKills(), x + 60, y + 480, CustomColor.fromHexString("FFFFFF"),6f);
        ui.drawText("Pvp deaths: " + data.getPvp().getDeaths(), x + 60, y + 540, CustomColor.fromHexString("FFFFFF"),6f);
    }
}
