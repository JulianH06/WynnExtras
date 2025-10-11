package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.Map;

import static julianh06.wynnextras.features.profileviewer.PVScreen.getClassName;
import static julianh06.wynnextras.features.profileviewer.PVScreen.selectedCharacter;

public class RaidsTabWidget extends PVScreen.TabWidget {
    static Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground.png");
    static Identifier raidBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/raidbackground_dark.png");

    static Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
    static Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
    static Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
    static Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");


    public RaidsTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        if(PV.currentPlayerData.getGlobalData() == null) {
            ui.drawCenteredText("This player has their raid stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(raidBackgroundTextureDark, x + 30, y + 90, 825, 300);
            ui.drawImage(raidBackgroundTextureDark, x + 945, y + 90, 825, 300);
            ui.drawImage(raidBackgroundTextureDark, x + 30, y + 420, 825, 300);
            ui.drawImage(raidBackgroundTextureDark, x + 945, y + 420, 825, 300);
        } else {
            ui.drawImage(raidBackgroundTexture, x + 30, y + 90, 825, 300);
            ui.drawImage(raidBackgroundTexture, x + 945, y + 90, 825, 300);
            ui.drawImage(raidBackgroundTexture, x + 30, y + 420, 825, 300);
            ui.drawImage(raidBackgroundTexture, x + 945, y + 420, 825, 300);
        }
        ui.drawImage(NOTGTexture, x + 30, y + 90, 300, 300);
        ui.drawImage(TCCTexture, x + 1470, y + 90, 300, 300);
        ui.drawImage(NOLTexture, x + 30, y + 420, 300, 300);
        ui.drawImage(TNATexture, x + 1470, y + 420, 300, 300);

        Map<String, Long> ranking = null;
        if(selectedCharacter == null) {
            ranking = PV.currentPlayerData.getRanking();
        }
        long NOTGRank;
        long NOLRank;
        long TCCRank;
        long TNARank;
        CustomColor notgColor = CustomColor.fromHexString("FFFFFF");
        CustomColor nolColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tccColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tnaColor = CustomColor.fromHexString("FFFFFF");
        if(ranking != null) {

            NOTGRank = ranking.getOrDefault("grootslangCompletion", -1L);
            if(NOTGRank <= 100 && NOTGRank > 0) notgColor = CommonColors.RAINBOW;

            NOLRank = ranking.getOrDefault("orphionCompletion", -1L);
            if(NOLRank <= 100 && NOLRank > 0) nolColor = CommonColors.RAINBOW;

            TCCRank = ranking.getOrDefault("colossusCompletion", -1L);
            if(TCCRank <= 100 && TCCRank > 0) tccColor = CommonColors.RAINBOW;

            TNARank = ranking.getOrDefault("namelessCompletion", -1L);
            if(TNARank <= 100 && TNARank > 0) tnaColor = CommonColors.RAINBOW;

            if(NOTGRank != -1) {
                ui.drawText("Rank #" + formatter.format(NOTGRank), x + 345f, y + 255f, notgColor, 3.9f);
            }
            if(NOLRank != -1) {
                ui.drawText("Rank #" + formatter.format(NOLRank), x + 345f, y + 585f, nolColor, 3.9f);
            }
            if(TCCRank != -1) {
                ui.drawText("Rank #" + formatter.format(TCCRank), x + 1470f, y + 255f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
            }
            if(TNARank != -1) {
                ui.drawText("Rank #" + formatter.format(TNARank), x + 1470f, y + 585f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
            }
        }

        Raids raids;
        String characterNameString;
        if(selectedCharacter != null && selectedCharacter.getRaids() != null) {
            characterNameString = " on " + getClassName(selectedCharacter) + ": ";
            raids = selectedCharacter.getRaids();
        } else {
            characterNameString = ": ";
            raids = PV.currentPlayerData.getGlobalData().getRaids();
        }

        ui.drawText("Nest of the Grootslangs", x + 345f, y + 165f, notgColor, 3.9f);
        ui.drawText("Orphion's Nexus of Light", x + 345f, y + 495f, nolColor, 3.9f);
        ui.drawText("The Canyon Colossus", x + 1470f, y + 165f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
        ui.drawText("The Nameless Anomaly", x + 1470f, y + 495f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);

        if(raids != null) {
            long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
            long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
            long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
            long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
            long TotalComps = raids.getTotal();

            ui.drawText(formatter.format(NOTGComps) + " Completions", x + 345f, y + 210f, notgColor, 3.9f);
            ui.drawText(formatter.format(NOLComps) + " Completions", x + 345f, y + 540f, nolColor, 3.9f);
            ui.drawText(formatter.format(TCCComps) + " Completions", x + 1470f, y + 210f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);
            ui.drawText(formatter.format(TNAComps) + " Completions", x + 1470f, y + 540f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3.9f);

            ui.drawCenteredText("Total Completions" + characterNameString + formatter.format(TotalComps), x + 900f, y + 48f, CustomColor.fromHexString("FFFFFF"), 3.9f);
        }
    }
}
