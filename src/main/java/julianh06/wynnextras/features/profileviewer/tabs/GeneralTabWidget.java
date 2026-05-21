package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.colors.WynncraftShaderColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityPose;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class GeneralTabWidget extends PVScreen.TabWidget {
    static Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");
    static Identifier offlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle.png");

    static Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    static Identifier offlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/offlinecircle_dark.png");

    private boolean createdClassWidgets = false;
    private List<ClassWidget> classWidgets = new ArrayList<>();
    private static boolean draggingAllowed = false;

    private GuildButtonWidget guildButtonWidget = null;

    private PVScreen pvScreen;

    public GeneralTabWidget(PVScreen pvScreen) {
        super(0, 0, 100, 100);
        playerRotationY = 0;
        guildButtonWidget = null;
        classWidgets.clear();
        clearChildren();
        this.pvScreen = pvScreen;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;

        if(!createdClassWidgets) {
            if(PV.currentPlayerData.getCharacters() != null) {
                classWidgets.clear();
                clearChildren();
                Map<String, CharacterData> map = PV.currentPlayerData.getCharacters();
                CharacterData activeCharacter = PV.currentPlayerData.getActiveCharacter() == null ? null : map.get(PV.currentPlayerData.getActiveCharacter().toString());
                List<CharacterData> sortedCharacterList = new ArrayList<>(map.values());

                sortedCharacterList.sort(
                    Comparator.comparing(CharacterData::getLevel).thenComparing(CharacterData::getTotalLevel).thenComparing(CharacterData::getContentCompletion).thenComparing(CharacterData::getPlaytime)
                );

                for (CharacterData entry : sortedCharacterList.reversed()) {
                    boolean isActive = false;
                    if(activeCharacter != null) {
                        if(entry == activeCharacter) isActive = true;
                    }
                    classWidgets.add(new ClassWidget(entry, isActive));
                }

                children.addAll(classWidgets);

                createdClassWidgets = true;
            };
        }

        Identifier rankBadge = getRankBadge();
        int rankBadgeWidth = getRankBadgeWidth();
        String rankColorHexString;
        if (PV.currentPlayerData.getLegacyRankColour() != null) {
            rankColorHexString = PV.currentPlayerData.getLegacyRankColour().getMain();
        } else {
            rankColorHexString = "AAAAAA";
        }
        if (rankBadge != null) {
            ui.drawImage(rankBadge,  x + 15, y + 18, rankBadgeWidth, 27);
        }

        ui.drawText(" " + PV.currentPlayerData.getUsername(), x + 10 + rankBadgeWidth,  y + 21, CustomColor.fromHexString(rankColorHexString), 3f);

        if (PV.currentPlayerData.isOnline()) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(onlineCircleTextureDark, onlineCircleTexture, x + 15, y + 60, 33, 33, ui);

            ui.drawText(PV.currentPlayerData.getServer(), x + 57, y + 66, CustomColor.fromHexString("FFFFFF"), 3f);
        } else {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(offlineCircleTextureDark, offlineCircleTexture, x + 15, y + 60, 33, 33, ui);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String formatted;
            if (PV.currentPlayerData.getLastJoin() == null) {
                formatted = "Unknown!";
            } else {
                formatted = PV.currentPlayerData.getLastJoin().format(formatter);
            }
            ui.drawText("Last seen: " + formatted, x + 57, y + 66, CustomColor.fromHexString("FFFFFF"), 3f);
        }
        if (dummy != null) {
            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                dummy.setPose(EntityPose.CROUCHING);
                drawPlayer(ctx, x + 66, y, dummy, ui.getScaleFactor(), tickDelta); //166 178
            } else if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_RIGHT_SHIFT)) {
                dummy.setPose(EntityPose.SLEEPING);
                drawPlayer(ctx, x + 66, y, dummy, ui.getScaleFactor(), tickDelta); //166 178
            } else {
                dummy.setPose(EntityPose.STANDING);
//                drawPlayer(ctx, x, y, (int) (210 / ui.getScaleFactor()), mouseX, mouseY, dummy, ui.getScaleFactor()); //166 178
                drawPlayer(ctx, x + 66, y, dummy, ui.getScaleFactor(), tickDelta); //166 178
            }
        }

        if (PV.currentPlayerData.getCharacters() != null) {
            int i = 0;
            Map<String, CharacterData> map = PV.currentPlayerData.getCharacters();
            List<CharacterData> sortedCharacterList = new ArrayList<>(map.values());

            sortedCharacterList.sort(
                    Comparator.comparing(CharacterData::getLevel).thenComparing(CharacterData::getTotalLevel).thenComparing(CharacterData::getContentCompletion).thenComparing(CharacterData::getPlaytime)
            );
        } else {
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

            ui.drawText("This player has their classes private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 3f);
        }

        boolean hasGuild = PV.currentPlayerData.getGuild() != null;
        if (hasGuild) {
            String guildString = "[" + PV.currentPlayerData.getGuild().getPrefix() + "] " + PV.currentPlayerData.getGuild().getName();
            String stars = PV.currentPlayerData.getGuild().getRankStars();
            String rankString = (stars == null  ? "" : stars) + " " + PV.currentPlayerData.getGuild().getRank() + " of " + (stars == null  ? "" : stars);
            ui.drawCenteredText(rankString, x + 285, y + 570, CustomColor.fromHexString("00FFFF"), 3f);
            if(guildButtonWidget == null) {
                guildButtonWidget = new GuildButtonWidget(guildString, PV.currentPlayerData.getGuild().getPrefix(), pvScreen);
                children.add(guildButtonWidget);
            }
            int widgetWidth = (int) (MinecraftClient.getInstance().textRenderer.getWidth(guildString) * ui.getScaleFactor() * 1.2);
            guildButtonWidget.setBounds(x + 285 - widgetWidth / 2, y + 590, widgetWidth, 20);
            //ui.drawCenteredText(guildString, x + 285, y + 600, CustomColor.fromHexString("FFFFFF"), 3f);
        } else {
            ui.drawCenteredText("Not part of a Guild", x + 285, y + 570, CustomColor.fromHexString("FFFFFF"), 3f);
        }

        if (PV.currentPlayerData.getFirstJoin() != null) {
            ZoneId zone = ZoneId.systemDefault();

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                    .withZone(zone);

            String formatted = "First joined: ";
            formatted += formatter.format(PV.currentPlayerData.getFirstJoin());
            ui.drawCenteredText(formatted, x + 285, y + (hasGuild ? 630 : 600), CustomColor.fromHexString("FFFFFF"), 3f);
        }

        if (PV.currentPlayerData.getPlaytime() != 0) {
            ui.drawCenteredText("Total Playtime: " + Math.round(PV.currentPlayerData.getPlaytime()) + "h", x + 285, y + (hasGuild ? 660 : 630), CustomColor.fromHexString("FFFFFF"), 3f);
        }

        if(selectedCharacter != null) {
            if(selectedCharacter.getPlaytime() != 0) {
                ui.drawCenteredText("Class Playtime: " + Math.round(selectedCharacter.getPlaytime()) + "h", x + 285, y + (hasGuild ? 690 : 660), CustomColor.fromHexString("FFFFFF"), 3f);
            }
        }

        if(WETeam != null && PV.currentPlayerData.getUsername() != null) {
            if (WETeam.contains(PV.currentPlayerData.getUsername())) {
                ui.drawCenteredText("★★★ WynnExtras Team Member ★★★", x + 285, y + 720, WynncraftShaderColor.SHINE.color, 3f);
            }
        }

        if(WEContributors != null && PV.currentPlayerData.getUsername() != null) {
            if (WEContributors.contains(PV.currentPlayerData.getUsername())) {
                ui.drawCenteredText("★★★ WynnExtras Contributor ★★★", x + 285, y + 720, WynncraftShaderColor.SHINE.color, 3f);
            }
        }
    }
    static float playerRotationY = 0;

    public void drawPlayer(
            DrawContext context,
            int x, int y,
            AbstractClientPlayerEntity player,
            double scaleFactor, float tickDelta
    ) {
        float teslerOffset = 0;
        float sneakOffset = 0;

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);

        if(dummy.getPose() == EntityPose.SLEEPING) {
            rotation.rotateY((float) Math.PI * 0.5f);
        } else if(dummy.getPose() == EntityPose.CROUCHING){
            sneakOffset = 0.137f;
        }

        player.lastHeadYaw = player.headYaw;

        if(PV.currentPlayer.equalsIgnoreCase("teslanator")) {
            teslerOffset = 1.89f;
            rotation.rotateX((float) Math.PI);
            rotation.rotateY((float) Math.toRadians(90 + playerRotationY));
        } else {
            rotation.rotateY((float) Math.toRadians(-28 - playerRotationY));
        }

        EntityRenderManager dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.getPlayerRenderer(player).getAndUpdateRenderState(player, tickDelta);
        state.light = 0xF000F0;

        Vector3f offset = new Vector3f(0f, 0.96f - teslerOffset - sneakOffset, 0f);

        int x1 = x;
        int x2 = x1 + 435;
        int y1 = y + 99;
        int y2 = y1 + 435;

        double m = pvScreen != null ? pvScreen.getMatrixScale() : 1.0;
        context.addEntity(state, (int) (215 * m / scaleFactor), offset, rotation.rotateY(180), null,
                (int) (x1 * m / scaleFactor), (int) (y1 * m / scaleFactor), (int) (x2 * m / scaleFactor), (int) (y2 * m / scaleFactor));
    }

    @Override
    public void updateValues() {
        int i = 0;
        for (ClassWidget classWidget : classWidgets) {
            int entryX = x + 576 + 411 * (i % 3);
            int entryY = y + 15 + 144 * Math.floorDiv(i, 3);
            classWidget.setBounds(entryX, entryY, 390, 132);
            i++;
        }
    }

    @Override
    protected boolean onClick(int button) {
        int dragX = x + 23 * 3;
        int dragY = y + 33 * 3;
        int dragWidth = 143 * 3;
        int dragHeight = 143 * 3;

        if (mouseX >= ui.sx(dragX) && mouseX <= ui.sx(dragX) + ui.sw(dragWidth) &&
                mouseY >= ui.sy(dragY) && mouseY <= ui.sy(dragY) + ui.sh(dragHeight)) {
            draggingAllowed = true;
        } else {
            draggingAllowed = false;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingAllowed) {
            playerRotationY -= (float) (deltaX * 0.5f * ui.getScaleFactor());
            playerRotationY %= 360f;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingAllowed = false;
        return super.mouseReleased(mx, my, button);
    }

    private static class GuildButtonWidget extends Widget {
        String guildString;
        private final Runnable action;

        public GuildButtonWidget(String guildString, String guildPrefix, PVScreen parent) {
            super(0, 0, 0, 0);
            this.guildString = guildString;
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                parent.close();
                GV.open(guildPrefix);
            };
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            //ui.drawRect(x, y, width, height);
            ui.drawCenteredText((hovered ? "§n" : "") + guildString, x + width / 2f, y + 10, hovered ? CustomColor.fromHexString("00FFFF") : CustomColor.fromHexString("FFFFFF"));
        }
    }
}
