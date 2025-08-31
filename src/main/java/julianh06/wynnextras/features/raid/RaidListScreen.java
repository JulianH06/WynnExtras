package julianh06.wynnextras.features.raid;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.raid.raids.*;
import com.wynntils.models.raid.type.RaidRoomInfo;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.SkinUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.mixin.Accessor.RaidInfoAccessor;
import julianh06.wynnextras.utils.SkinManager;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WEModule
public class RaidListScreen extends Screen {
    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms
    public static int currentCollapsed = -1;
    private static float currentCollapsedProgress = 0;
    private static int lastCollapsed = -1;

    public static List<RaidListElement> listElements = new ArrayList<>();

    RaidFilterButton NOTGFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton NOLFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton TCCFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton TNAFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton PBFilterButton = new RaidFilterButton(-1, -1, 40, 40);

    PlayerFilter playerFilter1 = new PlayerFilter(-1, -1, -1, -1);
    PlayerFilter playerFilter2 = new PlayerFilter(-1, -1, -1, -1);
    PlayerFilter playerFilter3 = new PlayerFilter(-1, -1, -1, -1);
    PlayerFilter playerFilter4 = new PlayerFilter(-1, -1, -1, -1);

    List<PlayerFilter> playerFilters = List.of(playerFilter1, playerFilter2, playerFilter3, playerFilter4);

    Identifier ButtonTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/button.png");
    Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/nestofthegrootslangs-small.png");
    Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/orphionsnexusoflight-small.png");
    Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thecanyoncolossus-small.png");
    Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thenamelessanomaly-small.png");
    Identifier NOTGTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/nestofthegrootslangs-small-bw.png");
    Identifier NOLTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/orphionsnexusoflight-small-bw.png");
    Identifier TCCTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thecanyoncolossus-small-bw.png");
    Identifier TNATextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thenamelessanomaly-small-bw.png");
    Identifier ScrollTextureTopLeft = Identifier.of("wynnextras", "textures/gui/raid/scrolltop/scrollleft.png");
    Identifier ScrollTextureTopMid = Identifier.of("wynnextras", "textures/gui/raid/scrolltop/scrollmid.png");
    Identifier ScrollTextureTopRight = Identifier.of("wynnextras", "textures/gui/raid/scrolltop/scrollright.png");
    Identifier ScrollTextureBottomLeft = Identifier.of("wynnextras", "textures/gui/raid/scrollbot/scrollleft.png");
    Identifier ScrollTextureBottomMid = Identifier.of("wynnextras", "textures/gui/raid/scrollbot/scrollmid.png");
    Identifier ScrollTextureBottomRight = Identifier.of("wynnextras", "textures/gui/raid/scrollbot/scrollright.png");
    Identifier ScrollTextureMiddle = Identifier.of("wynnextras", "textures/gui/raid/scrolltestmiddle.png");
    Identifier PBTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small.png");
    Identifier PBTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small-bw.png");

    public RaidListScreen() {
        super(Text.of("Raid List Test"));
        scrollOffset = 0;
        PBFilterButton.isActive = false;
    }

    @Override
    protected void init() {
//        for (RaidData raid : RaidListData.INSTANCE.raids) {
//            for (String name : raid.players) {
//                try {
//                    SkinManager.loadSkinAsync(SkinManager.getUUIDFromUsernameCached(name), name, null);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }

        listElements.clear();
        currentCollapsed = -1;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int width = (int) (screenWidth * 0.5);
        int xStart = screenWidth / 2 - width / 2;
        for (int i = 0; i < RaidListData.INSTANCE.raids.size(); i++) {
            int yPos = 10 + 50 * i;
            listElements.add(new RaidListElement(xStart, yPos, 40, width));
        }
        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return;
            }
            lastScrollTime = now;

            if (verticalAmount > 0) {
                scrollOffset -= 10; //Scroll up
            } else {
                scrollOffset += 10; //Scroll down
            }
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        System.out.println(textRenderer.getWidth("Orphion's Nexus of Light")); //LONGEST NAME 124 pixel
//        System.out.println(textRenderer.getWidth("Completed")); //49 pixel
        //super.render(context, mouseX, mouseY, delta);
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = (int) Math.max(screenWidth * 0.5, 350);
        int xStart = screenWidth / 2 - width / 2;
        int yStart = screenWidth / 2;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);

        NOTGFilterButton.setX(xStart);
        NOTGFilterButton.setY(5);
        NOLFilterButton.setX(xStart + 40);
        PBFilterButton.setX(xStart + width / 2 - 20);
        PBFilterButton.setY(5);
        NOLFilterButton.setY(5);
        TCCFilterButton.setX(xStart + width - 80);
        TCCFilterButton.setY(5);
        TNAFilterButton.setX(xStart + width - 40);
        TNAFilterButton.setY(5);
        {
            int i = 0;
            for (PlayerFilter filter : playerFilters) {
                filter.setHeight(12);
                filter.setWidth((width - 200) / 2);
                filter.setX(xStart + 80 + Math.floorDiv(i, 2) * (filter.getWidth() + 40));
                filter.setY(9 + (i % 2) * 20);
                i++;
                filter.draw(context);
            }
        }

        if(NOTGFilterButton.isActive) NOTGFilterButton.drawWithTexture(context, NOTGTexture);
        else NOTGFilterButton.drawWithTexture(context, NOTGTextureBW);
        if(NOLFilterButton.isActive) NOLFilterButton.drawWithTexture(context, NOLTexture);
        else NOLFilterButton.drawWithTexture(context, NOLTextureBW);
        if(TCCFilterButton.isActive) TCCFilterButton.drawWithTexture(context, TCCTexture);
        else TCCFilterButton.drawWithTexture(context, TCCTextureBW);
        if(TNAFilterButton.isActive) TNAFilterButton.drawWithTexture(context, TNATexture);
        else TNAFilterButton.drawWithTexture(context, TNATextureBW);
        if(PBFilterButton.isActive) PBFilterButton.drawWithTexture(context, PBTexture);
        else PBFilterButton.drawWithTexture(context, PBTextureBW);

//        RenderUtils.drawTexturedRect(context.getMatrices(), NOTGTexture, xStart, 0, 0, 30, 30, 30, 30);
//        RenderUtils.drawTexturedRect(context.getMatrices(), NOLTexture, xStart + 30, 0, 0, 30, 30, 30, 30);
//        RenderUtils.drawTexturedRect(context.getMatrices(), TCCTexture, xStart + 60, 0, 0, 30, 30, 30, 30);
//        RenderUtils.drawTexturedRect(context.getMatrices(), TNATexture, xStart + 90, 0, 0, 30, 30, 30, 30);

        List<RaidData> filteredList = filterList(RaidListData.INSTANCE.raids.reversed());
        List<RaidData> sortedList = sort(filteredList);
        int i = 0;
        for(RaidData raid : sortedList) {
            float yPos = 40 + 10 + 50 * i - scrollOffset;
            boolean isCollapsed = currentCollapsed == i || lastCollapsed == i;
            if(!isCollapsed) listElements.get(i).setHeight(40);
            if(currentCollapsed != -1 && currentCollapsed < i) {
                yPos += currentCollapsedProgress;
            } else if(lastCollapsed < i && lastCollapsed != -1) {
                yPos += currentCollapsedProgress;
            }


            if (yPos + 40 + (isCollapsed ? 1 : 0) * currentCollapsedProgress >= 0 && yPos <= screenHeight) {
                Identifier raidTexture = getTexture(raid.raidInfo.getRaidKind());
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopLeft, xStart, yPos, 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopMid, xStart + 12, yPos, 0, width - 24, 20, width - 24, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopRight, xStart - 12 + width, yPos, 0, 12, 20, 12, 20);
                if(isCollapsed) RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddle, xStart, yPos + 20, 0, width, 20, width, 20);
                if(isCollapsed && currentCollapsedProgress >= 20)
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddle, xStart, yPos + 40, 0, width, 20, width, 20);
                if(isCollapsed && currentCollapsedProgress >= 40)
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddle, xStart, yPos + 60, 0, width, 20, width, 20);
                if(isCollapsed && currentCollapsedProgress >= 60)
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddle, xStart, yPos + 80, 0, width, 20, width, 20);
                if(isCollapsed) {
                    for (int j = 0; j < 4; j++) {
                        boolean partyError = false;
                        String name;
                        if(j > raid.players.size()) {
                            name = "Player " + j;
                            partyError = true;
                        }
                        name = raid.players.get(j);

//                        Identifier skin = null;
//                        try {
//                            skin = SkinManager.getSkin(SkinManager.getUUIDFromUsernameCached(name));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(skin);
//
//                        if (texture instanceof NativeImageBackedTexture nativeTex && nativeTex.getImage() != null) {
//                            RenderUtils.drawTexturedRect(context.getMatrices(), skin, xStart + 25, yPos + 22 + j * 20, 0, 16, 16, 8, 8, 8, 8, 64, 64);
//
//                        } else {
//                            RenderUtils.drawTexturedRect(context.getMatrices(), DefaultSkinHelper.getTexture(), xStart + 25, yPos + 22 + j * 20, 0, 16, 16, 8, 8, 8, 8, 64, 64);
//                        }

                         if(currentCollapsedProgress >= 20 * j) {
                             Identifier Skin;
                             if (partyError) {
                                Skin = DefaultSkinHelper.getTexture();
                             } else
                                 try {
                                     Skin = SkinUtils.getSkin(SkinManager.getUUIDFromUsernameCached(name));
                                 } catch (IOException e) {
                                     Skin = DefaultSkinHelper.getTexture();
                                 }

                             RenderUtils.drawTexturedRect(context.getMatrices(), Skin, xStart + 25, yPos + 22 + j * 20, 0, 16, 16, 8, 8, 8, 8, 64, 64);

                             FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(name), xStart + 45, yPos + 26 + j * 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                        }
                    }
                    Map<Integer, RaidRoomInfo> challenges = ((RaidInfoAccessor)raid.raidInfo).getChallenges();
                    if(challenges != null) {
                        for(int j = 0; j < challenges.size(); j++) {
                            if(currentCollapsedProgress >= 20 * j) {
                                RaidRoomInfo room = challenges.get(j + 1);
                                if (room == null) continue;
                                long roomDuration = room.getRoomTotalTime();
                                String roomString = room.getRoomName() + ": " + formatDuration(roomDuration);
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(roomString), xStart + width - textRenderer.getWidth(roomString) - 28, yPos + 26 + j * 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                            }
                        }
                    }
                }
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomLeft, xStart, yPos + 20 + (isCollapsed ? 1 : 0) * currentCollapsedProgress, 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomMid, xStart + 12, yPos + 20 + (isCollapsed ? 1 : 0) * currentCollapsedProgress, 0, width - 24, 20, width - 24, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomRight, xStart - 12 + width, yPos + 20 + (isCollapsed ? 1 : 0) * currentCollapsedProgress, 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), raidTexture, xStart + width / 2 - 15, yPos - 5, 0, 30, 30, 30, 30);
                context.drawText(MinecraftClient.getInstance().textRenderer, raid.raidInfo.getRaidKind().getRaidName(), xStart + 10, (int) (yPos + 6), CustomColor.fromHexString("FFFFFF").asInt(), true);
                context.drawText(MinecraftClient.getInstance().textRenderer, convertTime(raid.raidEndTime), xStart + width - textRenderer.getWidth(convertTime(raid.raidEndTime)) - 8, (int) (yPos + 6), CustomColor.fromHexString("FFFFFF").asInt(), true);
                context.drawText(MinecraftClient.getInstance().textRenderer, formatDuration(raid.duration), xStart + width - textRenderer.getWidth(formatDuration(raid.duration)) - 8, (int) (yPos + 26 + (isCollapsed ? 1 : 0) * currentCollapsedProgress), CustomColor.fromHexString("FFFFFF").asInt(), true);
                if(raid.completed) {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "Completed", xStart + 10, (int) (yPos + 26 + (isCollapsed ? 1 : 0) * currentCollapsedProgress), CustomColor.fromHexString("FFFFFF").asInt(), true);
                } else {
                    context.drawText(MinecraftClient.getInstance().textRenderer, "FAILED", xStart + 10, (int) (yPos + 26 + (isCollapsed ? 1 : 0) * currentCollapsedProgress), CustomColor.fromHexString("FF0000").asInt(), true);
                }
                //listElements.get(i).draw(context);
            }
            i++;
        }
        if(currentCollapsed != -1) {
            if(currentCollapsedProgress < 80) {
                currentCollapsedProgress += 2;
                RaidListElement currentElement = listElements.get(currentCollapsed);
                currentElement.setHeight(currentElement.getHeight() + 2);
            } else {
                currentCollapsedProgress = 80;
            }
        } else {
            if(lastCollapsed != -1) {
                if (currentCollapsedProgress > 0) {
                    currentCollapsedProgress -= 2;
                    RaidListElement currentElement = listElements.get(lastCollapsed);
                    currentElement.setHeight(currentElement.getHeight() - 2);
                } else {
                    lastCollapsed = -1;
                }
            }
        }
    }

    @Override
    public void close() {
        super.close();
        System.out.println("closed");
    }

    public Identifier getTexture(RaidKind raidKind) {
        return switch (raidKind.getAbbreviation()) {
            case "NOG" -> NOTGTexture;
            case "TNA" -> TNATexture;
            case "NOL" -> NOLTexture;
            case "TCC" -> TCCTexture;
            default -> null;
        };
    }

    public static String convertTime(long time) {

        ZonedDateTime dateTime = Instant.ofEpochMilli(time)
                .atZone(ZoneId.systemDefault());

        String formatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        return formatted;
        //System.out.println("Time: " + formatted);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clickedTextInput = false;
        for(EasyTextInput input : playerFilters) {
            if(input.isClickInBounds((int) mouseX, (int) mouseY)) {
                input.click();
                System.out.println("IN BOUNDS");
                clickedTextInput = true;
            } else {
                System.out.println("NOT IN BOUNDS");
                input.setActive(false);
            }
        }
        if(clickedTextInput) return true;

        if(NOTGFilterButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            NOTGFilterButton.click();
            return true;
        }

        if(NOLFilterButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            NOLFilterButton.click();
            return true;
        }

        if(TCCFilterButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            TCCFilterButton.click();
            return true;
        }

        if(TNAFilterButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            TNAFilterButton.click();
            return true;
        }

        if(PBFilterButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            PBFilterButton.click();
            return true;
        }

        for (int i = 0; i < listElements.size(); i++) {
            RaidListElement element = listElements.get(i);
            int yPos = getElementY(i);

            if (element.isClickInBounds((int) mouseX, (int) mouseY - yPos + scrollOffset)) {
                currentCollapsed = (currentCollapsed == i) ? -1 : i;
                if(currentCollapsed == -1) lastCollapsed = i;
                element.click();
                System.out.println("NEW COLLAPSED: " + currentCollapsed);
                return true;
            }
        }
        return false;
    }

    private int getElementY(int index) {
        int y = 40;

        if (currentCollapsed != -1 && index > currentCollapsed) {
            y += currentCollapsedProgress;
        }

        return y;
    }

    public static String formatDuration(long durationMs) {
        long minutes = durationMs / 60000;
        long seconds = (durationMs % 60000) / 1000;
        long millis = durationMs % 1000;

        return String.format("%02d:%02d:%03d", minutes, seconds, millis);
    }

    public List<RaidData> filterList(List<RaidData> rawList) {
        List<RaidData> result = new ArrayList<>();

        for(RaidData raid : rawList) {


            if(NOTGFilterButton.isActive && raid.raidInfo.getRaidKind() instanceof NestOfTheGrootslangsRaid) {
                result.add(raid);
                continue;
            }
            if(NOLFilterButton.isActive && raid.raidInfo.getRaidKind() instanceof OrphionsNexusOfLightRaid) {
                result.add(raid);
                continue;
            }
            if(TCCFilterButton.isActive && raid.raidInfo.getRaidKind() instanceof TheCanyonColossusRaid) {
                result.add(raid);
                continue;
            }
            if(TNAFilterButton.isActive && raid.raidInfo.getRaidKind() instanceof TheNamelessAnomalyRaid) {
                result.add(raid);
                continue;
            }
        }

        return result;
    }

    public List<RaidData> sort(List<RaidData> rawList) {
        if(!PBFilterButton.isActive) return rawList;
        rawList.sort(Comparator.comparingLong(raid -> raid.duration));
        return rawList;
    }

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        System.out.println("a");
        for(PlayerFilter filter : playerFilters) {
            if(filter.isActive()) {
                onInput(event);
                return;
            }
        }
    }
}
