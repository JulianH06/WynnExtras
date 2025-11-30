package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Map;

import static julianh06.wynnextras.features.profileviewer.PVScreen.getProfTexture;
import static julianh06.wynnextras.features.profileviewer.PVScreen.selectedCharacter;

public class ProfessionsTabWidget extends PVScreen.TabWidget {
    static Identifier profBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground.png");
    static Identifier profBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profs/profbackground_dark.png");

    public ProfessionsTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        if(selectedCharacter == null) {
            ui.drawCenteredText("Select a character to view professions.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }
        Map<String, Profession> profs = selectedCharacter.getProfessions();
        if(profs == null) {
            ui.drawCenteredText("This player has their profession stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(profBackgroundTextureDark, profBackgroundTexture, x + 30, y + 30, 1740, 690, ui);

        int i = 0;
        for(Map.Entry<String, Profession> prof : profs.entrySet()) {
            Identifier profTexture = getProfTexture(prof.getKey());
            int level = prof.getValue().getLevel();
            CustomColor levelColor;
            if(i < 4) {
                ui.drawImage(profTexture, x + 210 + i * 408, y + 60, 192, 192);
                if(level == 132) {
                    levelColor = CommonColors.RAINBOW;
                } else if (level >= 110) {
                    levelColor = CommonColors.YELLOW;
                } else {
                    levelColor = CustomColor.fromHexString("FFFFFF");
                }
                ui.drawCenteredText("Level " + level, x + 306 + i * 408, y + 300, levelColor, 6f);
                if(level < 132) {
                    ui.drawCenteredText("Progress to next Level: " + prof.getValue().getXpPercent() + "%", x + 306 + i * 408, y + 340, levelColor, 2.4f);
                }
            } else {
                ui.drawImage(profTexture, x + 132 + (i - 4) * 204, y + 600, 96, 96);
                if(level == 132) {
                    levelColor = CommonColors.RAINBOW;
                } else if (level >= 103) {
                    levelColor = CommonColors.YELLOW;
                } else {
                    levelColor = CustomColor.fromHexString("FFFFFF");
                }
                ui.drawCenteredText("Level " + level, x + 180 + (i - 4) * 204, y + 575, levelColor, 3.6f);
                if(level < 132) {
                    ui.drawCenteredText("Progress to", x + 180 + (i - 4) * 204, y + 520, levelColor, 2.4f);
                    ui.drawCenteredText("next Level: " + prof.getValue().getXpPercent() + "%", x + 180 + (i - 4) * 204,y + 544, levelColor, 2.4f);
                }
            }

            i++;
        }
    }
}
