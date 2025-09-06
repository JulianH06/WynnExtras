package julianh06.wynnextras.features.profileviewer;

import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PVScreen extends Screen {
    public enum Rank {NONE, VIP, VIPPLUS, HERO, HEROPLUS, CHAMPION}

    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground.png");
    Identifier vip = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vip.png");
    Identifier vipplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/vipplus.png");
    Identifier hero = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/hero.png");
    Identifier heroplus = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/heroplus.png");
    Identifier champion = Identifier.of("wynnextras", "textures/gui/profileviewer/ranks/champion.png");

    String player;

    public PVScreen(String player) {
        super(Text.of("Player Viewer"));
        this.player = player;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = 400;
        int height = 250;
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenHeight / 2 - height / 2;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
        RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, xStart, yStart, width, height, width, height);
        if(PV.currentPlayerData != null) {
            Identifier rankBadge = getRankBadge();
            int rankBadgeWidth = getRankBadgeWidth();
            if(rankBadge != null) {
                RenderUtils.drawTexturedRect(context.getMatrices(), rankBadge, xStart + 10, yStart + 9.5f, (float) rankBadgeWidth / 2, 9, rankBadgeWidth / 2, 9);
            }
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(" " + PV.currentPlayerData.getUsername()), xStart + 10 + (float) rankBadgeWidth / 2, yStart + 10, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
        }
    }

    @Override
    public void close() {
        PV.currentPlayer = "";
        PV.currentPlayerData = null;
        super.close();
    }

    public Rank getRank() {
        return switch (PV.currentPlayerData.getSupportRank()) {
            case "vip" -> Rank.VIP;
            case "vipplus" -> Rank.VIPPLUS;
            case "hero" -> Rank.HERO;
            case "heroplus" -> Rank.HEROPLUS;
            case "champion" -> Rank.CHAMPION;
            case null, default -> Rank.NONE;
        };
    }

    public Identifier getRankBadge() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> vip;
            case VIPPLUS -> vipplus;
            case HERO -> hero;
            case HEROPLUS -> heroplus;
            case CHAMPION -> champion;
            default -> null;
        };
    }

    public int getRankBadgeWidth() {
        Rank rank = getRank();
        return switch (rank) {
            case VIP -> 44;
            case VIPPLUS -> 58;
            case HERO -> 62;
            case HEROPLUS -> 76;
            case CHAMPION -> 106;
            default -> 0;
        };
    }
}
