package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.Map;

public class RankingsTabWidget extends PVScreen.TabWidget {
    static Identifier miningTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/mining.png");
    static Identifier woodcuttingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodcutting.png");
    static Identifier farmingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/farming.png");
    static Identifier fishingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/fishing.png");
    static Identifier armouringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/armouring.png");
    static Identifier tailoringTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tailoring.png");
    static Identifier weaponsmithingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/weaponsmithing.png");
    static Identifier woodworkingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/woodworking.png");
    static Identifier jewelingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/jeweling.png");
    static Identifier alchemismTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/alchemism.png");
    static Identifier scribingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/scribing.png");
    static Identifier cookingTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/cooking.png");
    static Identifier warsCompletionTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/warscompletion.png");
    static Identifier playerContentTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/playercontent.png");
    static Identifier globalPlayerContent = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/globalplayercontent.png");
    static Identifier combatLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/combatlevel.png");
    static Identifier totalLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/totallevel.png");
    static Identifier professionLevelTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/professionlevel.png");
    static Identifier rankingBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground.png");
    static Identifier rankingBackgroundWideTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide.png");
    static Identifier rankingBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackground_dark.png");
    static Identifier rankingBackgroundWideTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/rankingbackgroundwide_dark.png");

    public RankingsTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        Map<String, Long> rankings = PV.currentPlayerData.getRanking();
        if(rankings == null) {
            ui.drawCenteredText("This player has their rankings private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        for(int i = 0; i < 20; i++) {
            if(i == 15) continue;
            if(i > 18) continue;
            int xPos = x + 30 + (435 * (i % 4));
            int yPos = y + 30 + (138 * Math.floorDiv(i, 4));

            Identifier texture = switch (i) {
                case 0 -> fishingTexture;
                case 1 -> woodcuttingTexture;
                case 2 -> miningTexture;
                case 3 -> farmingTexture;
                case 4 -> scribingTexture;
                case 5 -> jewelingTexture;
                case 6 -> alchemismTexture;
                case 7 -> cookingTexture;
                case 8 -> weaponsmithingTexture;
                case 9 -> tailoringTexture;
                case 10 -> woodworkingTexture;
                case 11 -> armouringTexture;
                case 12 -> warsCompletionTexture;
                case 13 -> playerContentTexture;
                case 14 -> globalPlayerContent;
                case 16 -> combatLevelTexture;
                case 17 -> totalLevelTexture;
                case 18 -> professionLevelTexture;
                default -> null;
            };

            String text = switch (i) {
                case 0 -> "Fishing";
                case 1 -> "Woodcutting";
                case 2 -> "Mining";
                case 3 -> "Farming";
                case 4 -> "Scribing";
                case 5 -> "Jeweling";
                case 6 -> "Alchemism";
                case 7 -> "Cooking";
                case 8 -> "Weaponsmithing";
                case 9 -> "Tailoring";
                case 10 -> "Woodworking";
                case 11 -> "Armouring";
                case 12 -> "Wars completed";
                case 13 -> "Player content completion";
                case 14 -> "Global content completion";
                case 16 -> "Combat level";
                case 17 -> "Total level";
                case 18 -> "Profession level";
                default -> null;
            };

            Long globalPlacement = switch (i) {
                case 0 -> rankings.get("fishingLevel");
                case 1 -> rankings.get("woodcuttingLevel");
                case 2 -> rankings.get("miningLevel");
                case 3 -> rankings.get("farmingLevel");
                case 4 -> rankings.get("scribingLevel");
                case 5 -> rankings.get("jewelingLevel");
                case 6 -> rankings.get("alchemismLevel");
                case 7 -> rankings.get("cookingLevel");
                case 8 -> rankings.get("weaponsmithingLevel");
                case 9 -> rankings.get("tailoringLevel");
                case 10 -> rankings.get("woodworkingLevel");
                case 11 -> rankings.get("armouringLevel");
                case 12 -> rankings.get("warsCompletion");
                case 13 -> rankings.get("playerContent");
                case 14 -> rankings.get("globalPlayerContent");
                case 16 -> rankings.get("combatGlobalLevel");
                case 17 -> rankings.get("totalGlobalLevel");
                case 18 -> rankings.get("professionsGlobalLevel");
                default -> null;
            };

            Long soloPlacement = switch (i) {
                case 16 -> rankings.get("combatSoloLevel");
                case 17 -> rankings.get("totalSoloLevel");
                case 18 -> rankings.get("professionsSoloLevel");
                default -> null;
            };

            DecimalFormat formatter = new DecimalFormat("#,###");

            String globalPlacementString;
            String soloPlacementString;

            if(globalPlacement == null) {
                globalPlacement = -1L;
                globalPlacementString = "???";
            } else {
                globalPlacementString = formatter.format(globalPlacement);
            }

            if(soloPlacement == null) {
                soloPlacement = -1L;
                soloPlacementString = "???";
            } else {
                soloPlacementString = formatter.format(soloPlacement);
            }


            CustomColor textColor = CustomColor.fromHexString("FFFFFF");
            if(globalPlacement <= 100 && globalPlacement > 0) {
                textColor = CommonColors.RAINBOW;
            }

            if(i < 12) {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(rankingBackgroundTextureDark, rankingBackgroundTexture, xPos, yPos, 420, 126, ui);

                ui.drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                ui.drawText(text, xPos + 111, yPos + 36, textColor);
                ui.drawText("#" + globalPlacementString, xPos + 111, yPos + 66, textColor);
            } else if(i < 16){
                xPos += (144 * (i % 3));
                PVScreen.DarkModeToggleWidget.drawImageWithFade(rankingBackgroundWideTextureDark, rankingBackgroundWideTexture, xPos, yPos, 567, 126, ui);

                ui.drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                ui.drawText(text, xPos + 111f, yPos + 36f, textColor);
                ui.drawText("#" + globalPlacementString, xPos + 111f, yPos + 66f, textColor);
            } else {
                xPos += (144 * ((i - 1) % 3));
                PVScreen.DarkModeToggleWidget.drawImageWithFade(rankingBackgroundWideTextureDark, rankingBackgroundWideTexture, xPos, yPos, 567, 126, ui);

                ui.drawImage(texture, xPos + 12, yPos + 18, 90, 90);
                ui.drawText(text, xPos + 111f, yPos + 21f, textColor);
                ui.drawText("#" + globalPlacementString, xPos + 111f, yPos + 51f, textColor);
                ui.drawText("Solo #" + soloPlacementString, xPos + 111f, yPos + 81f, textColor);
            }
        }
    }
}
