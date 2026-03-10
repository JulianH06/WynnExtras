package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
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

            List<String> apiKeyInfo = new ArrayList<>();
            if(MinecraftClient.getInstance().player != null && WynncraftApiHandler.INSTANCE.API_KEY == null || WynncraftApiHandler.INSTANCE.API_KEY.isEmpty()) {
                if(PV.currentPlayer.equalsIgnoreCase(MinecraftClient.getInstance().player.getName().getString())) {
                    apiKeyInfo.add("To get access to your private stats you need to set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                } else {
                    apiKeyInfo.add("You might be able to see them if you set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                }
            }


            int apiKeyInfoY = y + 385;
            for(String line : apiKeyInfo) {
                ui.drawCenteredText(line, x + 1185, apiKeyInfoY, CustomColor.fromHexString("FF0000"));
                apiKeyInfoY += 30;
            }

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
                    levelColor = WynnExtrasConfig.INSTANCE.removeChroma ? CommonColors.RED : CommonColors.RAINBOW;
                } else if (level >= 110) {
                    levelColor = CommonColors.YELLOW;
                } else {
                    levelColor = CustomColor.fromHexString("FFFFFF");
                }
                ui.drawCenteredText("Level " + level, x + 306 + i * 408, y + 300, levelColor, 6f);
                if(level < 132) {
                    ui.drawCenteredText("Progress to next Level: " + prof.getValue().getXpPercent() + "%", x + 306 + i * 408, y + 340, levelColor, 2.4f);
                } else {
                    long overflowXP = prof.getValue().getXpPercent() * 66287449L / 100;

                    ui.drawCenteredText("Overflow XP:", x + 306 + i * 408, y + 340, levelColor, 2.4f);
                    ui.drawCenteredText(formatNumber(overflowXP), x + 306 + i * 408,y + 370, levelColor, 2.4f);
                }
            } else {
                ui.drawImage(profTexture, x + 132 + (i - 4) * 204, y + 600, 96, 96);
                if(level == 132) {
                    levelColor = WynnExtrasConfig.INSTANCE.removeChroma ? CommonColors.RED : CommonColors.RAINBOW;
                } else if (level >= 103) {
                    levelColor = CommonColors.YELLOW;
                } else {
                    levelColor = CustomColor.fromHexString("FFFFFF");
                }
                ui.drawCenteredText("Level " + level, x + 180 + (i - 4) * 204, y + 575, levelColor, 3.6f);
                if(level < 132) {
                    ui.drawCenteredText("Progress to", x + 180 + (i - 4) * 204, y + 520, levelColor, 2.4f);
                    ui.drawCenteredText("next Level: " + prof.getValue().getXpPercent() + "%", x + 180 + (i - 4) * 204,y + 544, levelColor, 2.4f);
                } else {
                    long overflowXP = prof.getValue().getXpPercent() * 66287449L / 100;

                    ui.drawCenteredText("Overflow XP:", x + 180 + (i - 4) * 204, y + 520, levelColor, 2.4f);
                    ui.drawCenteredText(formatNumber(overflowXP), x + 180 + (i - 4) * 204,y + 544, levelColor, 2.4f);
                }
            }

            i++;
        }
    }

    public static String formatNumber(long number) {
        if (number < 0) return "-" + formatNumber(-number);

        if (number >= 1_000_000_000_000L) {
            return String.format("%.2fT", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000L) {
            return String.format("%.2fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000L) {
            return String.format("%.2fM", number / 1_000_000.0);
        } else if (number >= 1_000L) {
            return String.format("%.2fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
}
