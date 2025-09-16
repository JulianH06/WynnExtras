package julianh06.wynnextras.features.raid;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.raid.raids.*;
import com.wynntils.models.raid.type.RaidRoomInfo;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.mixin.Accessor.RaidInfoAccessor;
import julianh06.wynnextras.utils.Pair;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WEModule
public class RaidListScreen extends Screen {
    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms
    public static List<Boolean> currentCollapsed = new ArrayList<>();
    private static List<Float> currentCollapsedProgress = new ArrayList<>();

    public static List<RaidListElement> listElements = new ArrayList<>();
    public static List<String> currentPlayers = new ArrayList<>();

    RaidFilterButton NOTGFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton NOLFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton TCCFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton TNAFilterButton = new RaidFilterButton(-1, -1, 40, 40);
    RaidFilterButton PBFilterButton = new RaidFilterButton(-1, -1, 40, 40);

    RaidListFilter Filter = new RaidListFilter(-1, -1, -1, -1);

    Identifier InputFieldTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/button.png");
    Identifier ButtonBackgroundLeftTexture = Identifier.of("wynnextras", "textures/gui/raid/buttonbackgroundleft.png");
    Identifier ButtonBackgroundMidTexture = Identifier.of("wynnextras", "textures/gui/raid/buttonbackgroundmid.png");
    Identifier ButtonBackgroundRightTexture = Identifier.of("wynnextras", "textures/gui/raid/buttonbackgroundright.png");
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
    Identifier ScrollTextureMiddleLeft = Identifier.of("wynnextras", "textures/gui/raid/scrollmid/scrollmidleft.png");
    Identifier ScrollTextureMiddleMid = Identifier.of("wynnextras", "textures/gui/raid/scrollmid/scrollmidmid.png");
    Identifier ScrollTextureMiddleRight = Identifier.of("wynnextras", "textures/gui/raid/scrollmid/scrollmidright.png");
    Identifier PBTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small.png");
    Identifier PBTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small-bw.png");

    public RaidListScreen() {
        super(Text.of("Raid List"));
        scrollOffset = 0;
        PBFilterButton.isActive = false;
    }

    @Override
    protected void init() {
        listElements.clear();
        //currentCollapsed = -1;
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
                scrollOffset -= 30 / (int) MinecraftClient.getInstance().getWindow().getScaleFactor(); //Scroll up
            } else {
                scrollOffset += 30 / (int) MinecraftClient.getInstance().getWindow().getScaleFactor(); //Scroll down
            }
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = (int) Math.max(screenWidth * 0.5, 400);
        int xStart = screenWidth / 2 - width / 2;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);

        List<RaidData> filteredList = filterList(RaidListData.INSTANCE.raids.reversed());
        List<RaidData> sortedList = sort(filteredList);
        int i = 0;
        for(RaidData raid : sortedList) {
            while (i >= currentCollapsedProgress.size()) currentCollapsedProgress.add(0f);
            while (i >= currentCollapsed.size()) currentCollapsed.add(false);

            float yPos = getElementY(i);
            yPos += 10 - scrollOffset;
            yPos += 50 * i;


            if (yPos + 80 + currentCollapsedProgress.get(i) >= 0 && yPos <= screenHeight) {
                Identifier raidTexture = getTexture(raid.raidInfo.getRaidKind());
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopLeft, xStart, yPos, 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopMid, xStart + 12, yPos, 0, width - 24, 20, width - 24, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureTopRight, xStart - 12 + width, yPos, 0, 12, 20, 12, 20);
                if(currentCollapsedProgress.get(i) >= 0) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleLeft, xStart, yPos + 20, 0, 16, 20, 16, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleMid, xStart + 16, yPos + 20, 0, width - 32, 20, width - 32, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleRight, xStart + width - 16, yPos + 20, 0, 16, 20, 16, 20);
                }

                if(currentCollapsedProgress.get(i) >= 20) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleLeft, xStart, yPos + 40, 0, 16, 20, 16, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleMid, xStart + 16, yPos + 40, 0, width - 32, 20, width - 32, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleRight, xStart + width - 16, yPos + 40, 0, 16, 20, 16, 20);
                }

                if(currentCollapsedProgress.get(i) >= 40) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleLeft, xStart, yPos + 60, 0, 16, 20, 16, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleMid, xStart + 16, yPos + 60, 0, width - 32, 20, width - 32, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleRight, xStart + width - 16, yPos + 60, 0, 16, 20, 16, 20);
                }

                if(currentCollapsedProgress.get(i) >= 60) {
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleLeft, xStart, yPos + 80, 0, 16, 20, 16, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleMid, xStart + 16, yPos + 80, 0, width - 32, 20, width - 32, 20);
                    RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureMiddleRight, xStart + width - 16, yPos + 80, 0, 16, 20, 16, 20);
                }

                for (int j = 0; j < 4; j++) {
                    String name;
                    if(j >= raid.players.size()) {
                        name = "Player " + j;
                    } else {
                        name = raid.players.get(j);
                    }

                     if(currentCollapsedProgress.get(i) >= 20 * j) {

                         FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(name), xStart + 20, yPos + 26 + j * 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                    }
                }
                Map<Integer, RaidRoomInfo> challenges = ((RaidInfoAccessor)raid.raidInfo).getChallenges();
                if(challenges != null) {
                    for(int j = 0; j < challenges.size(); j++) {
                        if(currentCollapsedProgress.get(i) >= 20 * j) {
                            RaidRoomInfo room = challenges.get(j + 1);
                            if (room == null) continue;
                            long roomDuration = room.getRoomTotalTime();
                            if(room.getRoomEndTime() == -1) roomDuration = -1;
                            String roomString = room.getRoomName() + ": " + formatDuration(roomDuration);
//                                16 for nol because parasite
                            if(raid.raidInfo.getRaidKind() instanceof OrphionsNexusOfLightRaid) {
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(roomString), xStart + width - textRenderer.getWidth(roomString) - 20, yPos + 24 + j * 16, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                            } else {
                                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(roomString), xStart + width - textRenderer.getWidth(roomString) - 20, yPos + 26 + j * 20, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                            }
                        }
                    }
                }

                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomLeft, xStart, yPos + 20 + currentCollapsedProgress.get(i), 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomMid, xStart + 12, yPos + 20 + currentCollapsedProgress.get(i), 0, width - 24, 20, width - 24, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), ScrollTextureBottomRight, xStart - 12 + width, yPos + 20 + currentCollapsedProgress.get(i), 0, 12, 20, 12, 20);
                RenderUtils.drawTexturedRect(context.getMatrices(), raidTexture, xStart + width / 2 - 15, yPos - 5, 0, 30, 30, 30, 30);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(raid.raidInfo.getRaidKind().getRaidName()), xStart + 10, (int) (yPos + 6), CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(convertTime(raid.raidEndTime)), xStart + width - textRenderer.getWidth(convertTime(raid.raidEndTime)) - 8, (int) (yPos + 6), CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(formatDuration(raid.duration)), xStart + width - textRenderer.getWidth(formatDuration(raid.duration)) - 8, (int) (yPos + 26 + currentCollapsedProgress.get(i)), CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                if(raid.completed) {
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString("Completed"), xStart + 10, (int) (yPos + 26 + currentCollapsedProgress.get(i)), CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                } else {
                    FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString("FAILED"), xStart + 10, (int) (yPos + 26 + currentCollapsedProgress.get(i)), CustomColor.fromHexString("FF0000"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);
                }
                //listElements.get(i).draw(context);
            }
            i++;
        }

        int MidButtonBackgroundWidth = width - 80;

        RenderUtils.drawTexturedRect(context.getMatrices(), ButtonBackgroundLeftTexture,  xStart - 70, 0, 0, 120, 80, 120, 80);
        RenderUtils.drawTexturedRect(context.getMatrices(), ButtonBackgroundMidTexture, xStart + 40, 0, 0, MidButtonBackgroundWidth, 80, MidButtonBackgroundWidth, 80);
        RenderUtils.drawTexturedRect(context.getMatrices(), ButtonBackgroundRightTexture, xStart + width - 50, 0, 0, 120, 80, 120, 80);

        NOTGFilterButton.setX(xStart + width / 2 - 180);
        NOTGFilterButton.setY(16);
        NOLFilterButton.setX(xStart + width / 2 - 100);
        NOLFilterButton.setY(16);
        PBFilterButton.setX(xStart + width / 2 - 20);
        PBFilterButton.setY(16);
        TCCFilterButton.setX(xStart + width / 2 + 60);
        TCCFilterButton.setY(16);
        TNAFilterButton.setX(xStart + width / 2 + 140);
        TNAFilterButton.setY(16);

        Filter.setHeight(14);
        Filter.setWidth(width);
        Filter.setX(xStart);
        Filter.setY(2);
        Filter.setSearchText("Example: from:-7d4h until:2025-01-31/23:59 players:player1,player2,player3");
        Filter.drawWithTexture(context, InputFieldTexture);

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

        Pair<Integer, Integer> runs = getCompletedAndFailed(sortedList);
        String completed = "§aCompleted: " + runs.getFirst();
        String total = "§fTotal: " + sortedList.size();
        String failed =  "§cFailed: " + runs.getSecond();
        String combined = completed + " " + total + " " + failed;
        int totalStringStart = Math.round(xStart + width / 2f - textRenderer.getWidth(total) / 2f) - 4;

        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromString(combined), totalStringStart - textRenderer.getWidth(completed), 57, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1.0f);


        for (int j = 0; j < listElements.size(); j++) {
            if(currentCollapsed.get(j)) {
                while (j >= currentCollapsedProgress.size()) currentCollapsedProgress.add(0f);
                if(currentCollapsedProgress.get(j) < 80) {
                    currentCollapsedProgress.set(j, currentCollapsedProgress.get(j) + 10 * delta);
                    listElements.get(j).setHeight(listElements.get(j).getHeight() + 10 * delta);
                } else {
                    currentCollapsedProgress.set(j, 80f);
                    listElements.get(j).setHeight(120);
                }
            } else {
                while (j >= currentCollapsedProgress.size()) currentCollapsedProgress.add(0f);
                if (currentCollapsedProgress.get(j) > 0) {
                    currentCollapsedProgress.set(j, currentCollapsedProgress.get(j) - 10 * delta);
                    RaidListElement currentElement = listElements.get(j);
                    listElements.get(j).setHeight(listElements.get(j).getHeight() - 10 * delta);
                } else {
                    currentCollapsed.set(j, false);
                    currentCollapsedProgress.set(j, 0f);
                    listElements.get(j).setHeight(40);
                }
            }
        }
    }

    @Override
    public void close() {
        currentCollapsed.clear();
        currentCollapsedProgress.clear();
        super.close();
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

        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((!Filter.isActive() && Filter.isClickInBounds((int) mouseX, (int) mouseY))
                || Filter.isActive() && !Filter.isClickInBounds((int) mouseX, (int) mouseY)) {
            Filter.click();
            return true;
        }

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
                if(currentCollapsed.get(i)) {
                    currentCollapsed.set(i, false);
                } else {
                    element.click();
                    currentCollapsed.set(i, true);
                    currentCollapsedProgress.set(i, 0f);
                }
                return true;
            }
        }
        return false;
    }

    private int getElementY(int index) {
        int y = 80;

        for (int i = 0; i < listElements.size(); i++) {
            if(index <= i) break;
            while (i >= currentCollapsedProgress.size()) currentCollapsedProgress.add(0f);
            while (i >= currentCollapsed.size()) currentCollapsed.add(false);

            y += currentCollapsedProgress.get(i);
        }

        return y;
    }

    public static String formatDuration(long durationMs) {
        if(durationMs == -1) return "§cFAILED";
        long minutes = durationMs / 60000;
        long seconds = (durationMs % 60000) / 1000;
        long millis = durationMs % 1000;

        return String.format("%02d:%02d:%03d", minutes, seconds, millis);
    }

    public List<RaidData> filterList(List<RaidData> rawList) {
        List<RaidData> result = new ArrayList<>();

        RaidParser parsed = RaidParser.parse(Filter.getInput());

        LocalDateTime from = parsed.from;
        ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/Oslo");
        long fromEpoch = 0;
        if(from != null) {
            fromEpoch = from.atZone(zoneId).toEpochSecond() * 1000;
        }
        LocalDateTime until = parsed.until;
        long untilEpoch = 0;
        if(until != null) {
            untilEpoch = until.atZone(zoneId).toEpochSecond() * 1000;
        }
        List<String> players = parsed.players;

        for(RaidData raid : rawList) {
            if(from != null && fromEpoch != 0) {
                if(raid.raidEndTime < fromEpoch) {
                    continue;
                }
            }

            if(until != null && untilEpoch != 0) {
                if(raid.raidEndTime > untilEpoch) {
                    continue;
                }
            }

            if(!players.isEmpty()) {
                boolean playerNotContained = false;
                for (String player : players) {
                    if(raid.players.stream().noneMatch(p -> p.equalsIgnoreCase(player))) {
                        playerNotContained = true;
                        break;
                    }
                }
                if(playerNotContained) continue;
            }

            if(PBFilterButton.isActive && !raid.completed) {
                continue;
            }

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
        Filter.onInput(event);
    }

    @SubscribeEvent
    public void onChar(CharInputEvent event) {
        Filter.onCharInput(event);
    }

    public Pair<Integer, Integer> getCompletedAndFailed(List<RaidData> rawList) {
        int completed = 0;
        int failed = 0;
        for(RaidData data : rawList) {
            if(data.completed) completed++;
            else failed ++;
        }
        return new Pair<>(completed, failed);
    }
}
