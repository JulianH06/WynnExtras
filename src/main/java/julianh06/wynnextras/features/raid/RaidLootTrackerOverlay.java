package julianh06.wynnextras.features.raid;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.*;

public class RaidLootTrackerOverlay {

    private static final List<String> RAID_FILTERS = Arrays.asList("All", "NOTG", "NOL", "TCC", "TNA");
    private static int selectedFilterIndex = 0;

    // Position - loaded from config
    private static int xPos = 5;
    private static int yPos = 5;
    private static final int WIDTH = 165;
    private static final int LINE_HEIGHT = 10;
    private static final float TEXT_SCALE = 1.0f;

    // Dragging state
    private static boolean isDragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    // Hidden lines - loaded from config
    private static Set<String> hiddenLines = new HashSet<>();
    private static boolean configLoaded = false;

    // Line identifiers
    public static final String LINE_EMERALDS = "emeralds";
    public static final String LINE_AMPLIFIERS = "amplifiers";
    public static final String LINE_AMP_T1 = "amp_t1";
    public static final String LINE_AMP_T2 = "amp_t2";
    public static final String LINE_AMP_T3 = "amp_t3";
    public static final String LINE_BAGS = "bags";
    public static final String LINE_BAGS_STUFFED = "bags_stuffed";
    public static final String LINE_BAGS_PACKED = "bags_packed";
    public static final String LINE_BAGS_VARIED = "bags_varied";
    public static final String LINE_TOMES = "tomes";
    public static final String LINE_TOMES_MYTHIC = "tomes_mythic";
    public static final String LINE_TOMES_FABLED = "tomes_fabled";
    public static final String LINE_CHARMS = "charms";
    public static final String LINE_COMPLETIONS = "completions";

    // Track line positions for click detection
    private static final Map<String, int[]> linePositions = new HashMap<>();

    // Click regions for filter row
    private static int[] leftArrowBounds = new int[4];  // x1, y1, x2, y2
    private static int[] rightArrowBounds = new int[4];
    private static int[] filterNameBounds = new int[4];
    // Click regions for mode selector
    private static int[] modeLeftArrowBounds = new int[4];
    private static int[] modeRightArrowBounds = new int[4];
    private static int[] modeNameBounds = new int[4];

    // Reward chest coordinates for proximity check
    private static final Map<String, double[]> REWARD_CHEST_COORDS = Map.of(
            "NOTG", new double[]{10342, 41, 3111},
            "NOL",  new double[]{11005, 58, 2909},
            "TCC",  new double[]{10817, 45, 3901},
            "TNA",  new double[]{24489, 8, -23878}
    );

    // Colors
    private static final CustomColor BRAND_COLOR = CustomColor.fromHexString("7DCEA0");
    private static final CustomColor TITLE_COLOR = CustomColor.fromHexString("FFAA00");
    private static final CustomColor FILTER_COLOR = CustomColor.fromHexString("55FFFF");
    private static final CustomColor FILTER_ARROW_COLOR = CustomColor.fromHexString("AAAAAA");
    private static final CustomColor HEADER_COLOR = CustomColor.fromHexString("FFFFFF");
    private static final CustomColor VALUE_COLOR = CustomColor.fromHexString("AAAAAA");
    private static final CustomColor EMERALD_COLOR = CustomColor.fromHexString("55FF55");
    private static final CustomColor AMPLIFIER_COLOR = CustomColor.fromHexString("FFFF55");
    private static final CustomColor BAG_COLOR = CustomColor.fromHexString("55FFFF");
    private static final CustomColor TOME_COLOR = CustomColor.fromHexString("FF55FF");
    private static final CustomColor CHARM_COLOR = CustomColor.fromHexString("FF5555");
    private static final CustomColor HIDDEN_COLOR = CustomColor.fromHexString("555555");
    private static final CustomColor SESSION_COLOR = CustomColor.fromHexString("55FF55");

    public static void register() {
        HudRenderCallback.EVENT.register(RaidLootTrackerOverlay::render);
    }

    private static void loadConfig() {
        if (configLoaded) return;
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        xPos = config.raidLootTrackerX;
        yPos = config.raidLootTrackerY;
        hiddenLines = new HashSet<>(config.raidLootTrackerHiddenLines);
        configLoaded = true;
    }

    private static void saveConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.raidLootTrackerX = xPos;
        config.raidLootTrackerY = yPos;
        config.raidLootTrackerHiddenLines = new ArrayList<>(hiddenLines);
        WynnExtrasConfig.save();
    }

    private static boolean isNearLootChest() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        for (double[] pos : REWARD_CHEST_COORDS.values()) {
            double dist = Math.sqrt(Math.pow(px - pos[0], 2) + Math.pow(py - pos[1], 2) + Math.pow(pz - pos[2], 2));
            if (dist <= 100) return true;
        }
        return false;
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Don't render via HUD callback when screen is open
        if (mc.currentScreen != null) return;

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return;
        if (!config.raidLootTrackerRenderInHud) return;
        if (config.raidLootTrackerOnlyNearChest && !isNearLootChest()) return;

        loadConfig();
        renderOverlay(context, config, false);
    }

    private static final String RAID_CHEST_TITLE = "\uDAFF\uDFEA\uE00E";

    public static void renderOnScreen(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen == null) return;

        // Show in player's inventory, chat screen, or raid chest
        boolean isInventory = mc.currentScreen instanceof InventoryScreen;
        boolean isChat = mc.currentScreen instanceof ChatScreen;
        boolean isRaidChest = mc.currentScreen.getTitle().getString().equals(RAID_CHEST_TITLE);
        if (!isInventory && !isChat && !isRaidChest) return;
        if (isChat && !WynnExtrasConfig.INSTANCE.raidLootTrackerRenderInChat) return;
        if (isInventory && !WynnExtrasConfig.INSTANCE.raidLootTrackerRenderInInventory) return;

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return;
        if (config.raidLootTrackerOnlyNearChest && !isNearLootChest()) return;

        loadConfig();
        // Only show crossed out lines in inventory, not in chat/raid chest
        renderOverlay(context, config, isInventory);
    }

    private static void renderOverlay(DrawContext context, WynnExtrasConfig config, boolean inInventory) {
        linePositions.clear();
        RaidLootData data = RaidLootConfig.INSTANCE.data;
        data.initSession();

        boolean showSession = config.raidLootTrackerShowSession;
        boolean compact = config.raidLootTrackerCompact;
        String selectedFilter = RAID_FILTERS.get(selectedFilterIndex);

        // Draw background
        if (config.raidLootTrackerBackground) {
            int contentHeight = calculateContentHeight(compact, inInventory) + LINE_HEIGHT; // +1 line for safety
            int padX = 4;
            int padY = 3;
            int bgX = xPos - padX;
            int bgY = yPos - padY;
            int bgWidth = WIDTH + padX * 2;
            int bgHeight = contentHeight + padY * 2;
            int bgColor = 0xCC1a1a1a; // Dark gray with transparency
            drawBackground(context, bgX, bgY, bgX + bgWidth, bgY + bgHeight, bgColor);
        }

        int y = yPos;

        // Brand pill + Title
        MinecraftClient mc2 = MinecraftClient.getInstance();
        Text pillWithTitle = WynnExtras.addWynnExtrasPrefix(Text.literal("Raid Loot").styled(s -> s.withColor(TITLE_COLOR.asInt())));
        FontRenderer.getInstance().renderText(
                context, StyledText.fromComponent(pillWithTitle),
                xPos, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
        y += LINE_HEIGHT + 2;

        // Raid selector - nicer design with clickable arrows
        String modeText = showSession ? "Session" : "All-Time";

        // Left arrow [◀
        String leftArrowText = "[\u25C0";
        drawText(context, leftArrowText, xPos, y, FILTER_ARROW_COLOR);
        float leftArrowWidth = getTextWidth(leftArrowText);
        leftArrowBounds = new int[]{(int)xPos, y, (int)(xPos + leftArrowWidth), y + LINE_HEIGHT};

        // Filter name
        drawText(context, " ", xPos + leftArrowWidth, y, FILTER_ARROW_COLOR);
        float spaceWidth = getTextWidth(" ");
        float filterStartX = xPos + leftArrowWidth + spaceWidth;
        drawText(context, selectedFilter, filterStartX, y, FILTER_COLOR);
        float filterWidth = getTextWidth(selectedFilter);
        filterNameBounds = new int[]{(int)filterStartX, y, (int)(filterStartX + filterWidth), y + LINE_HEIGHT};

        // Right arrow ▶]
        String rightArrowText = " \u25B6]";
        drawText(context, rightArrowText, xPos + leftArrowWidth + spaceWidth + filterWidth, y, FILTER_ARROW_COLOR);
        float rightArrowWidth = getTextWidth(rightArrowText);
        rightArrowBounds = new int[]{(int)(xPos + leftArrowWidth + spaceWidth + filterWidth), y,
                (int)(xPos + leftArrowWidth + spaceWidth + filterWidth + rightArrowWidth), y + LINE_HEIGHT};

        // Mode selector on right with arrows [◀ Session ▶]
        String modeRightArrow = "\u25B6]";
        float modeRightArrowWidth = getTextWidth(modeRightArrow);
        drawTextRight(context, modeRightArrow, xPos + WIDTH, y, FILTER_ARROW_COLOR);
        modeRightArrowBounds = new int[]{(int)(xPos + WIDTH - modeRightArrowWidth), y, xPos + WIDTH, y + LINE_HEIGHT};

        float modeNameWidth = getTextWidth(modeText);
        float modeNameEndX = xPos + WIDTH - modeRightArrowWidth - getTextWidth(" ");
        drawTextRight(context, modeText, modeNameEndX, y, showSession ? SESSION_COLOR : AMPLIFIER_COLOR);
        modeNameBounds = new int[]{(int)(modeNameEndX - modeNameWidth), y, (int)modeNameEndX, y + LINE_HEIGHT};

        String modeLeftArrow = "[\u25C0 ";
        float modeLeftArrowWidth = getTextWidth(modeLeftArrow);
        float modeLeftArrowX = modeNameEndX - modeNameWidth - getTextWidth(" ");
        drawTextRight(context, modeLeftArrow, modeLeftArrowX, y, FILTER_ARROW_COLOR);
        modeLeftArrowBounds = new int[]{(int)(modeLeftArrowX - modeLeftArrowWidth), y, (int)modeLeftArrowX, y + LINE_HEIGHT};
        y += LINE_HEIGHT + 3;

        // Get appropriate data
        RaidLootData.RaidSpecificLoot displayData;
        int completions;

        if (selectedFilter.equals("All")) {
            if (showSession) {
                displayData = data.sessionData;
                completions = data.sessionData.completionCount;
            } else {
                displayData = createAggregateData(data);
                completions = data.perRaidData.values().stream().mapToInt(r -> r.completionCount).sum();
            }
        } else {
            if (showSession) {
                displayData = data.sessionPerRaidData != null ?
                        data.sessionPerRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot()) :
                        new RaidLootData.RaidSpecificLoot();
            } else {
                displayData = data.perRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot());
            }
            completions = displayData.completionCount;
        }

        // Calculate emerald totals
        // 1 stx = 64 le, 1 le = 64 eb, 1 eb = 64 e
        // So: 1 stx = 262144 e, 1 le = 4096 e, 1 eb = 64 e
        long totalEmeralds = (displayData.liquidEmeralds * 64 * 64) + (displayData.emeraldBlocks * 64);
        long stacks = totalEmeralds / 262144;
        long remainingAfterStx = totalEmeralds % 262144;
        long le = remainingAfterStx / 4096;
        long remainingAfterLE = remainingAfterStx % 4096;
        long eb = remainingAfterLE / 64;

        if (compact) {
            // Compact mode - just totals
            String compactEmeraldVal = formatEmeraldsCompact(stacks, le, eb);
            y = drawCompactLine(context, LINE_EMERALDS, "Ems", compactEmeraldVal, EMERALD_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_AMPLIFIERS, "Amps", String.valueOf(displayData.getTotalAmplifiers()), AMPLIFIER_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_BAGS, "Bags", String.valueOf(displayData.totalBags), BAG_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_TOMES, "Tomes", String.valueOf(displayData.totalTomes), TOME_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_CHARMS, "Charms", String.valueOf(displayData.totalCharms), CHARM_COLOR, y, inInventory);
            drawCompactLine(context, LINE_COMPLETIONS, "Runs", String.valueOf(completions), HEADER_COLOR, y, inInventory);
        } else {
            // Full mode
            String emeraldVal = formatEmeralds(stacks, le, eb);
            y = drawLine(context, LINE_EMERALDS, "Emeralds", emeraldVal, EMERALD_COLOR, y, inInventory);

            int totalAmps = displayData.getTotalAmplifiers();
            y = drawLine(context, LINE_AMPLIFIERS, "Amplifiers", String.valueOf(totalAmps), AMPLIFIER_COLOR, y, inInventory);
            y = drawLine(context, LINE_AMP_T1, "  Tier I", String.valueOf(displayData.amplifierTier1), AMPLIFIER_COLOR, y, inInventory);
            y = drawLine(context, LINE_AMP_T2, "  Tier II", String.valueOf(displayData.amplifierTier2), AMPLIFIER_COLOR, y, inInventory);
            y = drawLine(context, LINE_AMP_T3, "  Tier III", String.valueOf(displayData.amplifierTier3), AMPLIFIER_COLOR, y, inInventory);

            y = drawLine(context, LINE_BAGS, "Crafter Bags", String.valueOf(displayData.totalBags), BAG_COLOR, y, inInventory);
            y = drawLine(context, LINE_BAGS_STUFFED, "  Stuffed", String.valueOf(displayData.stuffedBags), BAG_COLOR, y, inInventory);
            y = drawLine(context, LINE_BAGS_PACKED, "  Packed", String.valueOf(displayData.packedBags), BAG_COLOR, y, inInventory);
            y = drawLine(context, LINE_BAGS_VARIED, "  Varied", String.valueOf(displayData.variedBags), BAG_COLOR, y, inInventory);

            y = drawLine(context, LINE_TOMES, "Tomes", String.valueOf(displayData.totalTomes), TOME_COLOR, y, inInventory);
            y = drawLine(context, LINE_TOMES_MYTHIC, "  Mythic", String.valueOf(displayData.mythicTomes), TOME_COLOR, y, inInventory);
            y = drawLine(context, LINE_TOMES_FABLED, "  Fabled", String.valueOf(displayData.fabledTomes), TOME_COLOR, y, inInventory);

            y = drawLine(context, LINE_CHARMS, "Charms", String.valueOf(displayData.totalCharms), CHARM_COLOR, y, inInventory);

            y += 2;
            drawLine(context, LINE_COMPLETIONS, "Runs", String.valueOf(completions), HEADER_COLOR, y, inInventory);
        }
    }

    private static RaidLootData.RaidSpecificLoot createAggregateData(RaidLootData data) {
        RaidLootData.RaidSpecificLoot agg = new RaidLootData.RaidSpecificLoot();
        agg.emeraldBlocks = data.emeraldBlocks;
        agg.liquidEmeralds = data.liquidEmeralds;
        agg.amplifierTier1 = data.amplifierTier1;
        agg.amplifierTier2 = data.amplifierTier2;
        agg.amplifierTier3 = data.amplifierTier3;
        agg.totalBags = data.totalBags;
        agg.stuffedBags = data.stuffedBags;
        agg.packedBags = data.packedBags;
        agg.variedBags = data.variedBags;
        agg.totalTomes = data.totalTomes;
        agg.mythicTomes = data.mythicTomes;
        agg.fabledTomes = data.fabledTomes;
        agg.totalCharms = data.totalCharms;
        return agg;
    }

    private static int drawLine(DrawContext context, String lineId, String label, String value,
                                CustomColor color, int y, boolean inInventory) {
        boolean isHidden = hiddenLines.contains(lineId);

        if (isHidden && !inInventory) return y;

        linePositions.put(lineId, new int[]{y, y + LINE_HEIGHT});

        if (isHidden && inInventory) {
            drawTextStrikethrough(context, label + ": " + value, xPos, y, HIDDEN_COLOR);
        } else {
            drawText(context, label + ":", xPos, y, color);
            drawTextRight(context, value, xPos + WIDTH, y, VALUE_COLOR);
        }

        return y + LINE_HEIGHT;
    }

    private static int drawCompactLine(DrawContext context, String lineId, String label, String value,
                                       CustomColor color, int y, boolean inInventory) {
        boolean isHidden = hiddenLines.contains(lineId);
        if (isHidden && !inInventory) return y;

        linePositions.put(lineId, new int[]{y, y + LINE_HEIGHT});

        if (isHidden && inInventory) {
            drawTextStrikethrough(context, label + ":" + value, xPos, y, HIDDEN_COLOR);
        } else {
            drawText(context, label + ":", xPos, y, color);
            drawTextRight(context, value, xPos + WIDTH, y, VALUE_COLOR);
        }

        return y + LINE_HEIGHT;
    }

    private static int calculateContentHeight(boolean compact, boolean inInventory) {
        // Title row: LINE_HEIGHT + 2
        // Filter row: LINE_HEIGHT + 3
        int headerHeight = LINE_HEIGHT + 2 + LINE_HEIGHT + 3;

        int dataLines;
        if (compact) {
            dataLines = 6; // Ems, Amps, Bags, Tomes, Charms, Runs
        } else {
            dataLines = 13; // Emeralds, Amplifiers(4), Bags(4), Tomes(3), Charms, Runs
        }

        // Subtract hidden lines when not showing them (not in inventory)
        if (!inInventory) {
            dataLines -= (int) hiddenLines.size();
        }

        // +2 for the gap before Runs in full mode
        int dataHeight = dataLines * LINE_HEIGHT + (compact ? 0 : 2);

        return headerHeight + dataHeight;
    }

    // Overload for click handling which doesn't know inInventory
    private static int calculateContentHeight(boolean compact) {
        boolean inInventory = MinecraftClient.getInstance().currentScreen instanceof InventoryScreen;
        return calculateContentHeight(compact, inInventory);
    }

    private static void drawText(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context, StyledText.fromString(text),
                x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static void drawTextRight(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context, StyledText.fromString(text),
                x, y, color, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static void drawTextStrikethrough(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context, StyledText.fromString("§m" + text),
                x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static float getTextWidth(String text) {
        return FontRenderer.getInstance().getFont().getWidth(text) * TEXT_SCALE;
    }

    private static String formatEmeralds(long stacks, long le, long eb) {
        StringBuilder sb = new StringBuilder();
        if (stacks > 0) {
            sb.append(stacks).append("stx ");
        }
        sb.append(le).append("le ").append(eb).append("eb");
        return sb.toString();
    }

    private static String formatEmeraldsCompact(long stacks, long le, long eb) {
        if (stacks > 0) {
            return stacks + "stx";
        } else if (le > 0) {
            return le + "le";
        } else {
            return eb + "eb";
        }
    }

    private static boolean isInBounds(double mouseX, double mouseY, int[] bounds) {
        return mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3];
    }

    private static void drawBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int r = 3; // corner radius
        // Main center rectangle
        context.fill(x1 + r, y1, x2 - r, y2, color);
        // Left and right strips
        context.fill(x1, y1 + r, x1 + r, y2 - r, color);
        context.fill(x2 - r, y1 + r, x2, y2 - r, color);
        // Corner fills (excluding the actual corner pixel for rounded effect)
        // Top-left
        context.fill(x1 + 1, y1 + 1, x1 + r, y1 + r, color);
        // Top-right
        context.fill(x2 - r, y1 + 1, x2 - 1, y1 + r, color);
        // Bottom-left
        context.fill(x1 + 1, y2 - r, x1 + r, y2 - 1, color);
        // Bottom-right
        context.fill(x2 - r, y2 - r, x2 - 1, y2 - 1, color);
    }

    public static boolean handleClick(double mouseX, double mouseY, int button, int action, boolean ctrlHeld, boolean shiftHeld) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return false;

        loadConfig();
        MinecraftClient mc = MinecraftClient.getInstance();
        int contentHeight = calculateContentHeight(config.raidLootTrackerCompact);

        boolean inBounds = mouseX >= xPos - 2 && mouseX <= xPos + WIDTH + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + contentHeight + 4;

        if (action == 0) {
            if (button == 1 && isDragging) {
                isDragging = false;
                saveConfig();
                return true;
            }
            return false;
        }

        if (!inBounds) return false;

        boolean inInventoryScreen = mc.currentScreen instanceof InventoryScreen;
        boolean inChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean inRaidChest = mc.currentScreen != null && mc.currentScreen.getTitle().getString().equals(RAID_CHEST_TITLE);
        boolean canInteract = inInventoryScreen || inChatScreen || inRaidChest;

        if (action == 1) {
            // Left click handling
            if (button == 0) {
                // Check if clicked on left arrow (previous filter)
                if (isInBounds(mouseX, mouseY, leftArrowBounds)) {
                    selectedFilterIndex = (selectedFilterIndex - 1 + RAID_FILTERS.size()) % RAID_FILTERS.size();
                    return true;
                }

                // Check if clicked on right arrow or filter name (next filter)
                if (isInBounds(mouseX, mouseY, rightArrowBounds) || isInBounds(mouseX, mouseY, filterNameBounds)) {
                    selectedFilterIndex = (selectedFilterIndex + 1) % RAID_FILTERS.size();
                    return true;
                }

                // Check if clicked on mode left arrow (toggle to other mode)
                if (isInBounds(mouseX, mouseY, modeLeftArrowBounds)) {
                    config.raidLootTrackerShowSession = !config.raidLootTrackerShowSession;
                    WynnExtrasConfig.save();
                    return true;
                }

                // Check if clicked on mode right arrow or mode name (toggle to other mode)
                if (isInBounds(mouseX, mouseY, modeRightArrowBounds) || isInBounds(mouseX, mouseY, modeNameBounds)) {
                    config.raidLootTrackerShowSession = !config.raidLootTrackerShowSession;
                    WynnExtrasConfig.save();
                    return true;
                }

                // Left click on data lines to toggle visibility (only in inventory)
                if (inInventoryScreen) {
                    for (Map.Entry<String, int[]> entry : linePositions.entrySet()) {
                        int[] bounds = entry.getValue();
                        if (mouseY >= bounds[0] && mouseY < bounds[1]) {
                            String lineId = entry.getKey();
                            if (hiddenLines.contains(lineId)) {
                                hiddenLines.remove(lineId);
                            } else {
                                hiddenLines.add(lineId);
                            }
                            saveConfig();
                            return true;
                        }
                    }
                }
            }

            // Right click on filter area = prev filter (check before drag)
            if (button == 1 && (isInBounds(mouseX, mouseY, filterNameBounds) || isInBounds(mouseX, mouseY, leftArrowBounds))) {
                selectedFilterIndex = (selectedFilterIndex - 1 + RAID_FILTERS.size()) % RAID_FILTERS.size();
                return true;
            }

            // Right click on mode area = toggle mode (check before drag)
            if (button == 1 && (isInBounds(mouseX, mouseY, modeNameBounds) || isInBounds(mouseX, mouseY, modeLeftArrowBounds))) {
                config.raidLootTrackerShowSession = !config.raidLootTrackerShowSession;
                WynnExtrasConfig.save();
                return true;
            }

            // Right click while in inventory/chat = start drag (only if not on filter/mode)
            if (button == 1 && canInteract) {
                isDragging = true;
                dragOffsetX = (int) mouseX - xPos;
                dragOffsetY = (int) mouseY - yPos;
                return true;
            }
        }

        return inBounds;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (isDragging) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen == null) {
                isDragging = false;
                return;
            }

            xPos = (int) mouseX - dragOffsetX;
            yPos = (int) mouseY - dragOffsetY;

            if (mc.getWindow() != null) {
                int screenWidth = mc.getWindow().getScaledWidth();
                int screenHeight = mc.getWindow().getScaledHeight();
                xPos = Math.max(0, Math.min(xPos, screenWidth - WIDTH));
                yPos = Math.max(0, Math.min(yPos, screenHeight - 100));
            }
        }
    }

    public static boolean isDragging() {
        return isDragging;
    }

    // Reset commands
    public static void resetAll() {
        RaidLootConfig.INSTANCE.data.resetAll();
        RaidLootConfig.INSTANCE.save();
    }

    public static void resetSession() {
        RaidLootConfig.INSTANCE.data.resetSession();
    }

    public static void resetRaid(String raidName) {
        RaidLootConfig.INSTANCE.data.resetRaid(raidName);
        RaidLootConfig.INSTANCE.save();
    }

    // Called after data reset to ensure overlay updates
    public static void refreshData() {
        // Data is read fresh each frame, no caching to clear
        // This method exists for future use if caching is added
    }
}