package julianh06.wynnextras.features.raid;

import com.wynntils.models.raid.raids.*;
import com.wynntils.models.raid.type.RaidRoomInfo;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.utils.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.neoforged.bus.api.SubscribeEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WEModule
public class RaidListScreen extends Screen {
    // Wynncraft Medieval Theme Colors (matching config screen)
    private static final int BG_DARK = 0xFF1a1410;
    private static final int BG_MEDIUM = 0xFF2d2419;
    private static final int BG_LIGHT = 0xFF3d3222;
    private static final int PARCHMENT = 0xFF4a3c2a;
    private static final int PARCHMENT_LIGHT = 0xFF5c4d3a;
    private static final int GOLD = 0xFFc9a227;
    private static final int GOLD_DARK = 0xFF8b7019;
    private static final int GOLD_LIGHT = 0xFFe8c252;
    private static final int TEXT_LIGHT = 0xFFe8dcc8;
    private static final int TEXT_DIM = 0xFF9a8b70;
    private static final int BORDER_DARK = 0xFF1a1410;
    private static final int BORDER_LIGHT = 0xFF5c4a35;
    private static final int SUCCESS_GREEN = 0xFF4a8c3a;
    private static final int FAIL_RED = 0xFFa83232;

    // Scroll state
    private double scrollOffset = 0;
    private double maxScroll = 0;
    private static final int HEADER_HEIGHT = 90;
    private static final int ITEM_HEIGHT = 50;
    private static final int EXPANDED_HEIGHT = 130;

    // Collapse state
    private List<Boolean> expandedItems = new ArrayList<>();
    private List<Float> expandProgress = new ArrayList<>();

    // Filter state
    private boolean[] raidFilters = {true, true, true, true}; // NOTG, NOL, TCC, TNA
    private boolean pbFilter = false;
    private String filterInput = "";
    private boolean filterActive = false;
    private int cursorPos = 0;
    private long lastBlink = 0;
    private boolean cursorVisible = true;

    // Raid textures
    private final Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/nestofthegrootslangs-small.png");
    private final Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/orphionsnexusoflight-small.png");
    private final Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thecanyoncolossus-small.png");
    private final Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thenamelessanomaly-small.png");
    private final Identifier NOTGTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/nestofthegrootslangs-small-bw.png");
    private final Identifier NOLTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/orphionsnexusoflight-small-bw.png");
    private final Identifier TCCTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thecanyoncolossus-small-bw.png");
    private final Identifier TNATextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/thenamelessanomaly-small-bw.png");
    private final Identifier PBTexture = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small.png");
    private final Identifier PBTextureBW = Identifier.of("wynnextras", "textures/gui/raid/raidicons/trophy-small-bw.png");

    public static List<String> currentPlayers = new ArrayList<>();

    public RaidListScreen() {
        super(Text.of("Raid List"));
    }

    @Override
    protected void init() {
        scrollOffset = 0;
        expandedItems.clear();
        expandProgress.clear();
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        List<RaidData> raids = getFilteredAndSortedRaids();
        int totalHeight = 0;
        for (int i = 0; i < raids.size(); i++) {
            float progress = i < expandProgress.size() ? expandProgress.get(i) : 0;
            totalHeight += ITEM_HEIGHT + (int)(progress);
        }
        int visibleHeight = this.height - HEADER_HEIGHT - 20;
        maxScroll = Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Solid background
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        context.fill(0, 0, this.width, this.height, BG_DARK);

        int panelWidth = Math.min((int)(this.width * 0.8), 700);
        int panelX = (this.width - panelWidth) / 2;

        // Main panel
        drawParchmentPanel(context, panelX - 10, 10, panelWidth + 20, this.height - 20);

        // Header
        drawOrnateHeader(context, panelX, 15, panelWidth, 70);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "Raid History", this.width / 2, 28, GOLD);

        // Filter input field
        int filterY = 45;
        int filterWidth = panelWidth - 20;
        drawFilterInput(context, panelX + 10, filterY, filterWidth, 18, mouseX, mouseY);

        // Raid filter buttons
        int buttonY = 68;
        int buttonSize = 28;
        int buttonSpacing = 8;
        int totalButtonWidth = 5 * buttonSize + 4 * buttonSpacing;
        int buttonStartX = this.width / 2 - totalButtonWidth / 2;

        drawRaidFilterButton(context, buttonStartX, buttonY, buttonSize, 0, mouseX, mouseY); // NOTG
        drawRaidFilterButton(context, buttonStartX + buttonSize + buttonSpacing, buttonY, buttonSize, 1, mouseX, mouseY); // NOL
        drawRaidFilterButton(context, buttonStartX + 2 * (buttonSize + buttonSpacing), buttonY, buttonSize, 2, mouseX, mouseY); // TCC
        drawRaidFilterButton(context, buttonStartX + 3 * (buttonSize + buttonSpacing), buttonY, buttonSize, 3, mouseX, mouseY); // TNA
        drawPBFilterButton(context, buttonStartX + 4 * (buttonSize + buttonSpacing), buttonY, buttonSize, mouseX, mouseY);

        // Stats line
        List<RaidData> raids = getFilteredAndSortedRaids();
        Pair<Integer, Integer> stats = getCompletedAndFailed(raids);
        String statsText = "\u00A7aCompleted: " + stats.getFirst() + "  \u00A7fTotal: " + raids.size() + "  \u00A7cFailed: " + stats.getSecond();
        context.drawCenteredTextWithShadow(this.textRenderer, statsText, this.width / 2, HEADER_HEIGHT - 5, TEXT_LIGHT);

        // Raid list
        int listTop = HEADER_HEIGHT + 5;
        int listBottom = this.height - 25;
        context.enableScissor(panelX, listTop, panelX + panelWidth, listBottom);

        // Ensure lists are sized correctly
        while (expandedItems.size() < raids.size()) expandedItems.add(false);
        while (expandProgress.size() < raids.size()) expandProgress.add(0f);

        int y = listTop - (int)scrollOffset;
        for (int i = 0; i < raids.size(); i++) {
            RaidData raid = raids.get(i);
            float progress = expandProgress.get(i);
            int itemHeight = ITEM_HEIGHT + (int)progress;

            if (y + itemHeight > listTop - 20 && y < listBottom + 20) {
                drawRaidEntry(context, panelX + 15, y, panelWidth - 30, itemHeight, raid, i, mouseX, mouseY, progress);
            }
            y += itemHeight + 5;

            // Animate expansion
            if (expandedItems.get(i)) {
                if (expandProgress.get(i) < EXPANDED_HEIGHT - ITEM_HEIGHT) {
                    expandProgress.set(i, Math.min(expandProgress.get(i) + 8 * delta, EXPANDED_HEIGHT - ITEM_HEIGHT));
                }
            } else {
                if (expandProgress.get(i) > 0) {
                    expandProgress.set(i, Math.max(expandProgress.get(i) - 8 * delta, 0));
                }
            }
        }

        context.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 5;
            int scrollbarTop = listTop + 5;
            int scrollbarHeight = listBottom - listTop - 10;
            int thumbHeight = Math.max(30, (int)(scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll)));
            int thumbY = scrollbarTop + (int)((scrollbarHeight - thumbHeight) * (scrollOffset / maxScroll));

            // Track
            context.fill(scrollbarX - 3, scrollbarTop, scrollbarX + 3, scrollbarTop + scrollbarHeight, BORDER_DARK);
            context.fill(scrollbarX - 2, scrollbarTop + 1, scrollbarX + 2, scrollbarTop + scrollbarHeight - 1, BG_MEDIUM);
            // Thumb
            context.fill(scrollbarX - 2, thumbY, scrollbarX + 2, thumbY + thumbHeight, GOLD_DARK);
            context.fill(scrollbarX - 1, thumbY + 1, scrollbarX + 1, thumbY + thumbHeight - 1, GOLD);
        }

        // Close button hint
        context.drawCenteredTextWithShadow(this.textRenderer, "Press ESC to close", this.width / 2, this.height - 18, TEXT_DIM);

        updateMaxScroll();
    }

    private void drawParchmentPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, BG_MEDIUM);
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, BG_LIGHT);
        // Borders
        context.fill(x, y, x + width, y + 2, BORDER_DARK);
        context.fill(x, y + height - 2, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 2, y + height, BORDER_DARK);
        context.fill(x + width - 2, y, x + width, y + height, BORDER_DARK);
        // Inner highlight
        context.fill(x + 2, y + 2, x + width - 2, y + 3, BORDER_LIGHT);
        context.fill(x + 2, y + 2, x + 3, y + height - 2, BORDER_LIGHT);
    }

    private void drawOrnateHeader(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PARCHMENT);
        context.fill(x, y, x + width, y + 2, GOLD_DARK);
        context.fill(x + 10, y + height - 2, x + width - 10, y + height, GOLD_DARK);
        // Corner diamonds
        drawDiamond(context, x + 5, y + height / 2, 3, GOLD);
        drawDiamond(context, x + width - 5, y + height / 2, 3, GOLD);
    }

    private void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    private void drawFilterInput(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

        context.fill(x, y, x + width, y + height, BORDER_DARK);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, filterActive ? PARCHMENT_LIGHT : PARCHMENT);

        String displayText;
        if (filterInput.isEmpty() && !filterActive) {
            displayText = "Filter: from:-7d until:2025-01-31 players:name1,name2";
            context.drawTextWithShadow(this.textRenderer, displayText, x + 4, y + 5, TEXT_DIM);
        } else {
            displayText = filterInput;
            context.drawTextWithShadow(this.textRenderer, displayText, x + 4, y + 5, TEXT_LIGHT);

            // Cursor
            if (filterActive) {
                long now = System.currentTimeMillis();
                if (now - lastBlink > 500) {
                    cursorVisible = !cursorVisible;
                    lastBlink = now;
                }
                if (cursorVisible) {
                    int cursorX = x + 4 + this.textRenderer.getWidth(filterInput.substring(0, Math.min(cursorPos, filterInput.length())));
                    context.fill(cursorX, y + 3, cursorX + 1, y + height - 3, TEXT_LIGHT);
                }
            }
        }
    }

    private void drawRaidFilterButton(DrawContext context, int x, int y, int size, int raidIndex, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        boolean active = raidFilters[raidIndex];

        // Button background
        context.fill(x, y, x + size, y + size, BORDER_DARK);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, hovered ? PARCHMENT_LIGHT : PARCHMENT);

        if (!active) {
            context.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x80000000); // Darkening overlay
        }

        // Raid icon
        Identifier texture = switch (raidIndex) {
            case 0 -> active ? NOTGTexture : NOTGTextureBW;
            case 1 -> active ? NOLTexture : NOLTextureBW;
            case 2 -> active ? TCCTexture : TCCTextureBW;
            case 3 -> active ? TNATexture : TNATextureBW;
            default -> NOTGTexture;
        };

        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x + 2, y + 2, 0, size - 4, size - 4, size - 4, size - 4);

        // Gold border if active
        if (active) {
            context.fill(x, y, x + size, y + 1, GOLD);
            context.fill(x, y + size - 1, x + size, y + size, GOLD_DARK);
            context.fill(x, y, x + 1, y + size, GOLD);
            context.fill(x + size - 1, y, x + size, y + size, GOLD_DARK);
        }
    }

    private void drawPBFilterButton(DrawContext context, int x, int y, int size, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;

        context.fill(x, y, x + size, y + size, BORDER_DARK);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, hovered ? PARCHMENT_LIGHT : PARCHMENT);

        if (!pbFilter) {
            context.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0x80000000);
        }

        Identifier texture = pbFilter ? PBTexture : PBTextureBW;
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x + 2, y + 2, 0, size - 4, size - 4, size - 4, size - 4);

        if (pbFilter) {
            context.fill(x, y, x + size, y + 1, GOLD);
            context.fill(x, y + size - 1, x + size, y + size, GOLD_DARK);
            context.fill(x, y, x + 1, y + size, GOLD);
            context.fill(x + size - 1, y, x + size, y + size, GOLD_DARK);
        }
    }

    private void drawRaidEntry(DrawContext context, int x, int y, int width, int height, RaidData raid, int index, int mouseX, int mouseY, float expandProgress) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ITEM_HEIGHT;
        boolean expanded = expandedItems.get(index);

        // Entry background
        int bgColor = hovered ? PARCHMENT_LIGHT : PARCHMENT;
        context.fill(x, y, x + width, y + height, bgColor);

        // Border
        context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 1, y + height, BORDER_LIGHT);
        context.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);

        // Status indicator bar on left
        int statusColor = raid.completed ? SUCCESS_GREEN : FAIL_RED;
        context.fill(x + 2, y + 4, x + 5, y + ITEM_HEIGHT - 4, statusColor);

        // Raid icon
        Identifier raidTexture = getRaidTexture(raid);
        RenderUtils.drawTexturedRect(context.getMatrices(), raidTexture, x + width / 2 - 12, y + 5, 0, 24, 24, 24, 24);

        // Raid name
        String raidName = raid.raidInfo.getRaidKind().getRaidName();
        context.drawTextWithShadow(this.textRenderer, raidName, x + 10, y + 8, TEXT_LIGHT);

        // Date/time
        String dateTime = convertTime(raid.raidEndTime);
        int dateWidth = this.textRenderer.getWidth(dateTime);
        context.drawTextWithShadow(this.textRenderer, dateTime, x + width - dateWidth - 8, y + 8, TEXT_DIM);

        // Duration and status
        String duration = formatDuration(raid.duration);
        String status = raid.completed ? "Completed" : "FAILED";
        int statusTextColor = raid.completed ? SUCCESS_GREEN : FAIL_RED;

        context.drawTextWithShadow(this.textRenderer, status, x + 10, y + 28, statusTextColor);
        int durationWidth = this.textRenderer.getWidth(duration);
        context.drawTextWithShadow(this.textRenderer, duration, x + width - durationWidth - 8, y + 28, GOLD);

        // Expand indicator
        String expandIndicator = expanded ? "\u25B2" : "\u25BC"; // Up/Down arrow
        context.drawCenteredTextWithShadow(this.textRenderer, expandIndicator, x + width / 2, y + 35, TEXT_DIM);

        // Expanded content
        if (expandProgress > 0) {
            int detailY = y + ITEM_HEIGHT + 5;

            // Separator
            context.fill(x + 10, detailY - 3, x + width - 10, detailY - 2, GOLD_DARK);

            // Players
            context.drawTextWithShadow(this.textRenderer, "Players:", x + 10, detailY, GOLD);
            int playerY = detailY + 12;
            for (int i = 0; i < Math.min(4, raid.players.size()); i++) {
                if (playerY - y < expandProgress + ITEM_HEIGHT - 10) {
                    context.drawTextWithShadow(this.textRenderer, "  " + raid.players.get(i), x + 10, playerY, TEXT_LIGHT);
                }
                playerY += 10;
            }

            // Challenge times
            Map<Integer, RaidRoomInfo> challenges = raid.raidInfo.getChallenges();
            if (challenges != null && !challenges.isEmpty()) {
                context.drawTextWithShadow(this.textRenderer, "Challenges:", x + width / 2, detailY, GOLD);
                int challengeY = detailY + 12;
                for (int j = 1; j <= challenges.size(); j++) {
                    RaidRoomInfo room = challenges.get(j);
                    if (room == null) continue;
                    if (challengeY - y < expandProgress + ITEM_HEIGHT - 10) {
                        long roomDuration = room.getRoomEndTime() == -1 ? -1 : room.getRoomTotalTime();
                        String roomText = room.getRoomName() + ": " + formatDuration(roomDuration);
                        context.drawTextWithShadow(this.textRenderer, "  " + roomText, x + width / 2, challengeY, TEXT_LIGHT);
                    }
                    challengeY += 10;
                }
            }
        }
    }

    private Identifier getRaidTexture(RaidData raid) {
        return switch (raid.raidInfo.getRaidKind().getAbbreviation()) {
            case "NOG" -> NOTGTexture;
            case "TNA" -> TNATexture;
            case "NOL" -> NOLTexture;
            case "TCC" -> TCCTexture;
            default -> NOTGTexture;
        };
    }

    private List<RaidData> getFilteredAndSortedRaids() {
        List<RaidData> result = new ArrayList<>();
        RaidParser parsed = RaidParser.parse(filterInput);

        LocalDateTime from = parsed.from;
        ZoneId zoneId = ZoneId.systemDefault();
        long fromEpoch = from != null ? from.atZone(zoneId).toEpochSecond() * 1000 : 0;

        LocalDateTime until = parsed.until;
        long untilEpoch = until != null ? until.atZone(zoneId).toEpochSecond() * 1000 : 0;

        List<String> players = parsed.players;

        for (RaidData raid : RaidListData.INSTANCE.raids.reversed()) {
            // Date filters
            if (from != null && fromEpoch != 0 && raid.raidEndTime < fromEpoch) continue;
            if (until != null && untilEpoch != 0 && raid.raidEndTime > untilEpoch) continue;

            // Player filter
            if (!players.isEmpty()) {
                boolean playerNotContained = false;
                for (String player : players) {
                    if (raid.players.stream().noneMatch(p -> p.equalsIgnoreCase(player))) {
                        playerNotContained = true;
                        break;
                    }
                }
                if (playerNotContained) continue;
            }

            // PB filter (only completed)
            if (pbFilter && !raid.completed) continue;

            // Raid type filters
            boolean matchesRaidType = false;
            if (raidFilters[0] && raid.raidInfo.getRaidKind() instanceof NestOfTheGrootslangsRaid) matchesRaidType = true;
            if (raidFilters[1] && raid.raidInfo.getRaidKind() instanceof OrphionsNexusOfLightRaid) matchesRaidType = true;
            if (raidFilters[2] && raid.raidInfo.getRaidKind() instanceof TheCanyonColossusRaid) matchesRaidType = true;
            if (raidFilters[3] && raid.raidInfo.getRaidKind() instanceof TheNamelessAnomalyRaid) matchesRaidType = true;

            if (matchesRaidType) {
                result.add(raid);
            }
        }

        // Sort by PB if enabled
        if (pbFilter) {
            result.sort(Comparator.comparingLong(raid -> raid.duration));
        }

        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min((int)(this.width * 0.8), 700);
        int panelX = (this.width - panelWidth) / 2;

        // Filter input click
        int filterY = 45;
        int filterWidth = panelWidth - 20;
        if (mouseX >= panelX + 10 && mouseX < panelX + 10 + filterWidth && mouseY >= filterY && mouseY < filterY + 18) {
            filterActive = !filterActive;
            if (filterActive) {
                cursorPos = filterInput.length();
            }
            return true;
        } else if (filterActive) {
            filterActive = false;
        }

        // Raid filter buttons
        int buttonY = 68;
        int buttonSize = 28;
        int buttonSpacing = 8;
        int totalButtonWidth = 5 * buttonSize + 4 * buttonSpacing;
        int buttonStartX = this.width / 2 - totalButtonWidth / 2;

        for (int i = 0; i < 4; i++) {
            int bx = buttonStartX + i * (buttonSize + buttonSpacing);
            if (mouseX >= bx && mouseX < bx + buttonSize && mouseY >= buttonY && mouseY < buttonY + buttonSize) {
                raidFilters[i] = !raidFilters[i];
                return true;
            }
        }

        // PB button
        int pbX = buttonStartX + 4 * (buttonSize + buttonSpacing);
        if (mouseX >= pbX && mouseX < pbX + buttonSize && mouseY >= buttonY && mouseY < buttonY + buttonSize) {
            pbFilter = !pbFilter;
            return true;
        }

        // Raid entry clicks
        int listTop = HEADER_HEIGHT + 5;
        int listBottom = this.height - 25;
        if (mouseY >= listTop && mouseY < listBottom) {
            List<RaidData> raids = getFilteredAndSortedRaids();
            int y = listTop - (int)scrollOffset;
            for (int i = 0; i < raids.size(); i++) {
                float progress = i < expandProgress.size() ? expandProgress.get(i) : 0;
                int itemHeight = ITEM_HEIGHT + (int)progress;

                if (mouseY >= y && mouseY < y + ITEM_HEIGHT && mouseX >= panelX + 15 && mouseX < panelX + panelWidth - 15) {
                    while (expandedItems.size() <= i) expandedItems.add(false);
                    expandedItems.set(i, !expandedItems.get(i));
                    return true;
                }
                y += itemHeight + 5;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = MathHelper.clamp(scrollOffset - verticalAmount * 30, 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterActive) {
            if (keyCode == 259 && cursorPos > 0) { // Backspace
                filterInput = filterInput.substring(0, cursorPos - 1) + filterInput.substring(cursorPos);
                cursorPos--;
                return true;
            }
            if (keyCode == 261 && cursorPos < filterInput.length()) { // Delete
                filterInput = filterInput.substring(0, cursorPos) + filterInput.substring(cursorPos + 1);
                return true;
            }
            if (keyCode == 263 && cursorPos > 0) { // Left arrow
                cursorPos--;
                return true;
            }
            if (keyCode == 262 && cursorPos < filterInput.length()) { // Right arrow
                cursorPos++;
                return true;
            }
            if (keyCode == 257) { // Enter
                filterActive = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (filterActive && chr >= 32) {
            filterInput = filterInput.substring(0, cursorPos) + chr + filterInput.substring(cursorPos);
            cursorPos++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        // Handle via keyPressed instead
    }

    @SubscribeEvent
    public void onChar(CharInputEvent event) {
        // Handle via charTyped instead
    }

    @Override
    public void close() {
        expandedItems.clear();
        expandProgress.clear();
        super.close();
    }

    private static String convertTime(long time) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private static String formatDuration(long durationMs) {
        if (durationMs == -1) return "\u00A7cFAILED";
        long minutes = durationMs / 60000;
        long seconds = (durationMs % 60000) / 1000;
        long millis = durationMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    private Pair<Integer, Integer> getCompletedAndFailed(List<RaidData> raids) {
        int completed = 0, failed = 0;
        for (RaidData data : raids) {
            if (data.completed) completed++;
            else failed++;
        }
        return new Pair<>(completed, failed);
    }
}
