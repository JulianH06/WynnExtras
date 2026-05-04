package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.colors.WynncraftShaderColor;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.GuildRaids;
import julianh06.wynnextras.features.profileviewer.data.Raids;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static julianh06.wynnextras.features.profileviewer.PVScreen.getClassName;
import static julianh06.wynnextras.features.profileviewer.PVScreen.selectedCharacter;

public class RaidsTabWidget extends PVScreen.TabWidget {
    static Identifier raidBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/raid/background.png");
    static Identifier raidBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/raid/background_dark.png");

    static Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
    static Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
    static Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
    static Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

    public RaidsTabWidget() {
        super(0, 0, 0, 0);

        typeSwitcher = new TypeSwitcher();
        addChild(typeSwitcher);
    }

    private enum Status { ALL, GRAIDS, NONGRAIDS }

    private static Status currentStatus = Status.ALL;

    private static long twpComps(Map<String, Integer> list) {
        if (list == null) return 0;
        Integer val = list.get("The Wartorn Palace");
        if (val != null) return val;
        val = list.get("unknown");
        return val != null ? val : 0;
    }

    private TypeSwitcher typeSwitcher;

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        DecimalFormat formatter = new DecimalFormat("#,###");
        if(PV.currentPlayerData.getGlobalData() == null) {
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
                ui.drawCenteredText(line, x + 900, apiKeyInfoY, CustomColor.fromHexString("FF0000"));
                apiKeyInfoY += 30;
            }

            ui.drawCenteredText("This player has their raid stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            typeSwitcher.setBounds(-100, -100, 0, 0);
            return;
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 30, y + 90, 825, 195, ui);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 945, y + 90, 825, 195, ui);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 30, y + 307, 825, 195, ui);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 945, y + 307, 825, 195, ui);

        PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 475, y + 525, 825, 195, ui);

        //PVScreen.DarkModeToggleWidget.drawImageWithFade(raidBackgroundTextureDark, raidBackgroundTexture, x + 945, y + 420, 825, 300, ui);

        ui.drawImage(NOTGTexture, x + 30, y + 90, 195, 195);
        ui.drawImage(TCCTexture, x + 1575, y + 90, 195, 195);
        ui.drawImage(NOLTexture, x + 30, y + 315, 195, 195);
        ui.drawImage(TNATexture, x + 1575, y + 315, 195, 195);

        Map<String, Long> ranking = null;

        if(selectedCharacter == null) {
            ranking = PV.currentPlayerData.getRanking();
        }

        long NOTGRank;
        long NOLRank;
        long TCCRank;
        long TNARank;
        long TWPRank;

        long NOTGSRRank;
        long NOLSRRank;
        long TCCSRRank;
        long TNASRRank;
        long TWPSRRank;

        CustomColor notgColor = CustomColor.fromHexString("FFFFFF");
        CustomColor nolColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tccColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tnaColor = CustomColor.fromHexString("FFFFFF");
        CustomColor twpColor = CustomColor.fromHexString("FFFFFF");

        CustomColor notgSRColor = CustomColor.fromHexString("FFFFFF");
        CustomColor nolSRColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tccSRColor = CustomColor.fromHexString("FFFFFF");
        CustomColor tnaSRColor = CustomColor.fromHexString("FFFFFF");
        CustomColor twpSRColor = CustomColor.fromHexString("FFFFFF");

        if(ranking != null && currentStatus == Status.ALL) {
            NOTGRank = ranking.getOrDefault("grootslangCompletion", -1L);
            if(NOTGRank <= 100 && NOTGRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) notgColor = WynncraftShaderColor.RAINBOW.color;

            NOLRank = ranking.getOrDefault("orphionCompletion", -1L);
            if(NOLRank <= 100 && NOLRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) nolColor = WynncraftShaderColor.RAINBOW.color;

            TCCRank = ranking.getOrDefault("colossusCompletion", -1L);
            if(TCCRank <= 100 && TCCRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) tccColor = WynncraftShaderColor.RAINBOW.color;

            TNARank = ranking.getOrDefault("namelessCompletion", -1L);
            if(TNARank <= 100 && TNARank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) tnaColor = WynncraftShaderColor.RAINBOW.color;

            TWPRank = ranking.getOrDefault("frumaCompletion", -1L);
            if(TWPRank <= 100 && TWPRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) twpColor = WynncraftShaderColor.RAINBOW.color;

            NOTGSRRank = ranking.getOrDefault("grootslangSrPlayers", -1L);
            if(NOTGSRRank <= 100 && NOTGSRRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) notgSRColor = WynncraftShaderColor.RAINBOW.color;

            NOLSRRank = ranking.getOrDefault("orphionSrPlayers", -1L);
            if(NOLSRRank <= 100 && NOLSRRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) nolSRColor = WynncraftShaderColor.RAINBOW.color;

            TCCSRRank = ranking.getOrDefault("colossusSrPlayers", -1L);
            if(TCCSRRank <= 100 && TCCSRRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) tccSRColor = WynncraftShaderColor.RAINBOW.color;

            TNASRRank = ranking.getOrDefault("namelessSrPlayers", -1L);
            if(TNASRRank <= 100 && TNASRRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) tnaSRColor = WynncraftShaderColor.RAINBOW.color;

            TWPSRRank = ranking.getOrDefault("frumaSrPlayers", -1L);
            if(TWPSRRank <= 100 && TWPSRRank > 0 && !WynnExtrasConfig.INSTANCE.removeChroma) twpSRColor = WynncraftShaderColor.RAINBOW.color;

            if(NOTGRank != -1) {
                ui.drawText("Completion Rank #" + formatter.format(NOTGRank), x + 240f, y + 195f, notgColor, 3f);
            }
            if(NOLRank != -1) {
                ui.drawText("Completion Rank #" + formatter.format(NOLRank), x + 240f, y + 415f, nolColor, 3f);
            }
            if(TCCRank != -1) {
                ui.drawText("Completion Rank #" + formatter.format(TCCRank), x + 1565f, y + 195f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
            }
            if(TNARank != -1) {
                ui.drawText("Completion Rank #" + formatter.format(TNARank), x + 1565f, y + 415f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
            }
            if(TWPRank != -1) {
                ui.drawCenteredText("Completion Rank #" + formatter.format(TWPRank), x + 900f, y + 645f, twpColor, 3f);
            }

            if(NOTGSRRank != -1) {
                ui.drawText("SR Rank #" + formatter.format(NOTGSRRank), x + 240f, y + 235f, notgSRColor, 3f);
            }
            if(NOLSRRank != -1) {
                ui.drawText("SR Rank #" + formatter.format(NOLSRRank), x + 240f, y + 455f, nolSRColor, 3f);
            }
            if(TCCSRRank != -1) {
                ui.drawText("SR Rank #" + formatter.format(TCCSRRank), x + 1565f, y + 235f, tccSRColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
            }
            if(TNASRRank != -1) {
                ui.drawText("SR Rank #" + formatter.format(TNASRRank), x + 1565f, y + 455f, tnaSRColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
            }
            if(TWPSRRank != -1) {
                ui.drawCenteredText("SR Rank #" + formatter.format(TWPSRRank), x + 900f, y + 680f, twpSRColor, 3f);
            }

            //TODO: aspect and lootrun pages updaten
        }

        Raids raids;
        GuildRaids guildRaids = null;
        String characterNameString;
        if(selectedCharacter != null && selectedCharacter.getRaids() != null) {
            characterNameString = " on " + getClassName(selectedCharacter) + ": ";
            raids = selectedCharacter.getRaids();
        } else {
            characterNameString = ": ";
            raids = PV.currentPlayerData.getGlobalData().getRaids();
            guildRaids = PV.currentPlayerData.getGlobalData().getGuildRaids();
        }

        ui.drawText("Nest of the Grootslangs", x + 240f, y + 125f, notgColor, 3f);
        ui.drawText("Orphion's Nexus of Light", x + 240f, y + 345f, nolColor, 3f);
        ui.drawText("The Canyon Colossus", x + 1565f, y + 125f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
        ui.drawText("The Nameless Anomaly", x + 1565f, y + 345f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
        ui.drawCenteredText("The Wartorn Palace", x + 900, y + 575f, twpColor, 3f);

        if(selectedCharacter != null) {
            currentStatus = Status.ALL;
            typeSwitcher.setBounds(-100, -100, 0, 0);
        }

        if(currentStatus == null) return;

        switch (currentStatus) {
            case ALL -> { if(raids != null) {
                long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
                long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
                long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
                long TWPComps = twpComps(raids.getList());
                long TotalComps = raids.getTotal();

                ui.drawText(formatter.format(NOTGComps) + " Completions", x + 240f, y + 160f, notgColor, 3f);
                ui.drawText(formatter.format(NOLComps) + " Completions", x + 240f, y + 380f, nolColor, 3f);
                ui.drawText(formatter.format(TCCComps) + " Completions", x + 1565f, y + 160f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawText(formatter.format(TNAComps) + " Completions", x + 1565f, y + 380f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawCenteredText(formatter.format(TWPComps) + " Completions", x + 900f, y + 610f, twpColor, 3f);

                ui.drawCenteredText("Total Completions" + characterNameString + formatter.format(TotalComps), x + 900f, y + 48f, CustomColor.fromHexString("FFFFFF"), 3.9f);
            }}
            case GRAIDS -> { if(guildRaids != null) {
                long NOTGComps = guildRaids.getList().getOrDefault("Nest of the Grootslangs", 0);
                long NOLComps = guildRaids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                long TCCComps = guildRaids.getList().getOrDefault("The Canyon Colossus", 0);
                long TNAComps = guildRaids.getList().getOrDefault("The Nameless Anomaly", 0);
                long TWPComps = twpComps(guildRaids.getList());
                long TotalComps = guildRaids.getTotal();

                ui.drawText(formatter.format(NOTGComps) + " Completions", x + 240f, y + 160f, notgColor, 3f);
                ui.drawText(formatter.format(NOLComps) + " Completions", x + 240f, y + 380f, nolColor, 3f);
                ui.drawText(formatter.format(TCCComps) + " Completions", x + 1565f, y + 160f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawText(formatter.format(TNAComps) + " Completions", x + 1565f, y + 380f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawCenteredText(formatter.format(TWPComps) + " Completions", x + 900f, y + 610f, twpColor, 3f);

                ui.drawCenteredText("Total Guild Raid Completions" + characterNameString + formatter.format(TotalComps), x + 900f, y + 48f, CustomColor.fromHexString("FFFFFF"), 3.9f);
            }}
            case NONGRAIDS -> { if(guildRaids != null && raids != null) {
                long NOTGComps = raids.getList().getOrDefault("Nest of the Grootslangs", 0);
                long NOLComps = raids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                long TCCComps = raids.getList().getOrDefault("The Canyon Colossus", 0);
                long TNAComps = raids.getList().getOrDefault("The Nameless Anomaly", 0);
                long TWPComps = twpComps(raids.getList());
                long TotalComps = raids.getTotal();

                long NOTGGraidComps = guildRaids.getList().getOrDefault("Nest of the Grootslangs", 0);
                long NOLGraidComps = guildRaids.getList().getOrDefault("Orphion's Nexus of Light", 0);
                long TCCGraidComps = guildRaids.getList().getOrDefault("The Canyon Colossus", 0);
                long TNAGraidComps = guildRaids.getList().getOrDefault("The Nameless Anomaly", 0);
                long TWPGraidComps = twpComps(guildRaids.getList());
                long TotalGraidComps = guildRaids.getTotal();

                ui.drawText(formatter.format(NOTGComps - NOTGGraidComps) + " Completions", x + 240f, y + 160f, notgColor, 3f);
                ui.drawText(formatter.format(NOLComps - NOLGraidComps) + " Completions", x + 240f, y + 380f, nolColor, 3f);
                ui.drawText(formatter.format(TCCComps - TCCGraidComps) + " Completions", x + 1565f, y + 160f, tccColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawText(formatter.format(TNAComps - TNAGraidComps) + " Completions", x + 1565f, y + 380f, tnaColor, HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 3f);
                ui.drawCenteredText(formatter.format(TWPComps - TWPGraidComps) + " Completions", x + 900f, y + 610f, twpColor, 3f);

                ui.drawCenteredText("Total Non Guild Raid Completions" + characterNameString + formatter.format(TotalComps - TotalGraidComps), x + 900f, y + 48f, CustomColor.fromHexString("FFFFFF"), 3.9f);
            }}
        }

        int typeSwitcherWidth = switch (currentStatus) {
            case ALL -> 100;
            case GRAIDS -> 200;
            case NONGRAIDS -> 270;
        };

        if(selectedCharacter == null) typeSwitcher.setBounds(x + width - typeSwitcherWidth - 30, y + 20, typeSwitcherWidth, 50);
    }

    public static class TypeSwitcher extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonCustom(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.pvDarkmodeToggle);

            String text = "";
            switch (currentStatus) {
                case ALL -> text = "All";
                case GRAIDS -> text = "Guild Raids";
                case NONGRAIDS -> text = "Non Guild Raids";
            }

            ui.drawCenteredText(text, x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            currentStatus = Status.values()[(currentStatus.ordinal() + (button == 0 ? 1 : -1) + Status.values().length) % Status.values().length];
            return true;
        }
    }
}
