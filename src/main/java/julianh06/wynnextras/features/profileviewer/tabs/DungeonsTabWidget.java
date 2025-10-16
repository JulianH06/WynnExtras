package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Dungeons;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class DungeonsTabWidget extends PVScreen.TabWidget {
    static Identifier dungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonkey.png");
    static Identifier corruptedDungeonKeyTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/corrupteddungeonkey.png");
    static Identifier dungeonBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground.png");
    static Identifier dungeonBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/dungeonpagebackground_dark.png");
    static Identifier decrepitSewersTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/decrepitsewers.png");
    static Identifier infestedPitTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/infestedpit.png");
    static Identifier underworldCryptTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/underworldcrypt.png");
    static Identifier timelostSanctumTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/timelostsanctum.png");
    static Identifier sandSweptTombTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/sandswepttomb.png");
    static Identifier iceBarrowsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/icebarrows.png");
    static Identifier undergrowthRuinsTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/undergrowthruins.png");
    static Identifier galleonsGraveyardTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/galleonsgraveyard.png");
    static Identifier fallenFactoryTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/fallenfactory.png");
    static Identifier eldritchOutlookTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/dungeons/eldritchoutlook.png");
    static List<Identifier> dungeonTextures = List.of(decrepitSewersTexture, infestedPitTexture, underworldCryptTexture, timelostSanctumTexture, sandSweptTombTexture, iceBarrowsTexture, undergrowthRuinsTexture, galleonsGraveyardTexture, fallenFactoryTexture, eldritchOutlookTexture);

    public DungeonsTabWidget() {
        super(0, 0, 0, 0);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        if(PV.currentPlayerData.getGlobalData() == null) {
            ui.drawCenteredText("This player has their dungeon stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }
        if (PV.currentPlayerData.getGlobalData().getDungeons() == null) {
            ui.drawCenteredText("This player has their dungeon stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }

        Map<String, Integer> normalComps = new HashMap<>();
        Map<String, Integer> corruptedComps = new HashMap<>();

        Dungeons dungeons;
        if(selectedCharacter == null) {
            dungeons = PV.currentPlayerData.getGlobalData().getDungeons();
        } else {
            dungeons = selectedCharacter.getDungeons();

            if(selectedCharacter.getDungeons() == null) {
                dungeons = new Dungeons();
            }
        }

        for (Map.Entry<String, Integer> entry : dungeons.getList().entrySet()) {
            if (entry.getKey().contains("Corrupted")) {
                corruptedComps.put(entry.getKey(), entry.getValue());
            } else {
                normalComps.put(entry.getKey(), entry.getValue());
            }
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(dungeonBackgroundTextureDark, x + 30, y + 87, 1740, 633);
        } else {
            ui.drawImage(dungeonBackgroundTexture, x + 30, y + 87, 1740, 633);
        }

        int i = 0;

        DecimalFormat formatter = new DecimalFormat("#,###");
        for(Identifier dungeon : dungeonTextures) {
            int comps = getDungeonComps(i, normalComps);
            int cComps = getCorruptedComps(i, corruptedComps);
            int dungeonX = x + 90 + 345 * (i % 5);
            int dungeonY = y + 90 + Math.floorDiv(i, 5) * 350;
            if(Math.floorDiv(i, 5) > 0) {
                ui.drawImage(dungeon, dungeonX + 30, dungeonY + 45, 180, 180);
                ui.drawCenteredText(getDungeonName(i), dungeonX + 120, dungeonY + 250, CustomColor.fromHexString("FFFFFF"), 3f);

                if(i < 8) {
                    ui.drawImage(dungeonKeyTexture, dungeonX + 60, dungeonY - 15, 60, 60);
                    ui.drawText(formatter.format(comps), dungeonX + 55, dungeonY, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                } else {
                    ui.drawImage(dungeonKeyTexture, dungeonX + 90, dungeonY - 15, 60, 60);
                    ui.drawText(formatter.format(comps), dungeonX + 90, dungeonY, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                }

                if(i < 8) {
                    ui.drawImage(corruptedDungeonKeyTexture, dungeonX + 120, dungeonY - 15, 60, 60);
                    ui.drawText(formatter.format(cComps), dungeonX + 190, dungeonY, CustomColor.fromHexString("FFFFFF"));
                }
            } else {
                ui.drawImage(dungeon, dungeonX + 30, dungeonY + 45, 180, 180);
                ui.drawCenteredText(getDungeonName(i), dungeonX + 120, dungeonY + 30, CustomColor.fromHexString("FFFFFF"), 3f);

                ui.drawImage(dungeonKeyTexture, dungeonX + 60, dungeonY + 230, 60, 60);
                ui.drawText(formatter.format(comps), dungeonX + 55, dungeonY + 250, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);

                if(i < 8) {
                    ui.drawImage(corruptedDungeonKeyTexture, dungeonX + 120, dungeonY + 230, 60, 60);
                    ui.drawText(formatter.format(cComps), dungeonX + 190, dungeonY + 250, CustomColor.fromHexString("FFFFFF"));
                }
            }
            i++;

            long TotalComps = dungeons.getTotal();
            String characterNameString;
            if(selectedCharacter != null && selectedCharacter.getRaids() != null) {
                characterNameString = " on " + getClassName(selectedCharacter) + ": ";
            } else {
                characterNameString = ": ";
            }

            ui.drawCenteredText("Total Completions" + characterNameString + formatter.format(TotalComps), x + 900, y + 45, CustomColor.fromHexString("FFFFFF"), 3.9f);
        }
    }
}
