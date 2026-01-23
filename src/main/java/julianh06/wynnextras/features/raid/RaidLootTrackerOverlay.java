package julianh06.wynnextras.features.raid;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.*;

public class RaidLootTrackerOverlay {

    private static final List<String> RAID_FILTERS = Arrays.asList("All", "TNA", "NOTG", "TCC", "NOL");
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

    // Reward chest coordinates for proximity check
    private static final Map<String, double[]> REWARD_CHEST_COORDS = Map.of(
        "NOTG", new double[]{10342, 41, 3111},
        "NOL",  new double[]{11005, 58, 2909},
        "TCC",  new double[]{10817, 45, 3901},
        "TNA",  new double[]{24489, 8, -23878}
    );

    // Colors
    private static final CustomColor BRAND_COLOR = CustomColor.fromHexString("2ECC71");
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
        if (config.raidLootTrackerOnlyInInventory) return;
        if (config.raidLootTrackerOnlyNearChest && !isNearLootChest()) return;

        loadConfig();
        renderOverlay(context, config, false);
    }

    public static void renderOnScreen(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen == null) return;

        // Only show in player's own inventory, not escape menu, mod settings, etc.
        if (!(mc.currentScreen instanceof InventoryScreen)) return;

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return;
        if (config.raidLootTrackerOnlyNearChest && !isNearLootChest()) return;

        loadConfig();
        renderOverlay(context, config, true);
    }

    private static void renderOverlay(DrawContext context, WynnExtrasConfig config, boolean inInventory) {
        linePositions.clear();
        RaidLootData data = RaidLootConfig.INSTANCE.data;
        data.initSession();

        boolean showSession = config.raidLootTrackerShowSession;
        boolean compact = config.raidLootTrackerCompact;
        String selectedFilter = RAID_FILTERS.get(selectedFilterIndex);

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500);

        int y = yPos;

        // Brand + Title
        drawText(context, "WynnExtras", xPos, y, BRAND_COLOR);
        float brandWidth = getTextWidth("WynnExtras ");
        drawText(context, "Raid Loot", xPos + brandWidth, y, TITLE_COLOR);
        y += LINE_HEIGHT + 2;

        // Raid selector - nicer design
        String modeText = showSession ? "Session" : "All-Time";
        drawText(context, "[\u25C0 ", xPos, y, FILTER_ARROW_COLOR);
        float arrowWidth = getTextWidth("[\u25C0 ");
        drawText(context, selectedFilter, xPos + arrowWidth, y, FILTER_COLOR);
        float filterWidth = getTextWidth(selectedFilter);
        drawText(context, " \u25B6]", xPos + arrowWidth + filterWidth, y, FILTER_ARROW_COLOR);

        // Mode indicator on right
        drawTextRight(context, modeText, xPos + WIDTH, y, showSession ? SESSION_COLOR : VALUE_COLOR);
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
        long totalLE = displayData.liquidEmeralds + (displayData.emeraldBlocks * 64);
        long stacks = totalLE / 4096;
        long remainingLE = (totalLE % 4096) % 64;
        long remainingEB = (totalLE % 4096) / 64;

        if (compact) {
            // Compact mode - just totals
            y = drawCompactLine(context, LINE_EMERALDS, "Ems", stacks + "stx", EMERALD_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_AMPLIFIERS, "Amps", String.valueOf(displayData.getTotalAmplifiers()), AMPLIFIER_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_BAGS, "Bags", String.valueOf(displayData.totalBags), BAG_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_TOMES, "Tomes", String.valueOf(displayData.totalTomes), TOME_COLOR, y, inInventory);
            y = drawCompactLine(context, LINE_CHARMS, "Charms", String.valueOf(displayData.totalCharms), CHARM_COLOR, y, inInventory);
            drawCompactLine(context, LINE_COMPLETIONS, "Runs", String.valueOf(completions), HEADER_COLOR, y, inInventory);
        } else {
            // Full mode
            String emeraldVal = stacks + "stx " + remainingLE + "le " + remainingEB + "eb";
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

        context.getMatrices().pop();
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

    private static int calculateContentHeight(boolean compact) {
        int base = compact ? 8 : 17;
        if (MinecraftClient.getInstance().currentScreen == null) {
            base -= (int) hiddenLines.size();
        }
        return LINE_HEIGHT * base + 10;
    }

    private static void drawText(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context.getMatrices(), StyledText.fromString(text),
                x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static void drawTextRight(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context.getMatrices(), StyledText.fromString(text),
                x, y, color, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static void drawTextStrikethrough(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context.getMatrices(), StyledText.fromString("§m" + text),
                x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static float getTextWidth(String text) {
        return FontRenderer.getInstance().getFont().getWidth(text) * TEXT_SCALE;
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

        if (action == 1) {
            // Ctrl+click to toggle line visibility (only in inventory)
            if (ctrlHeld && button == 0 && inInventoryScreen) {
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

            // Shift+click to toggle session/all-time
            if (shiftHeld && button == 0) {
                config.raidLootTrackerShowSession = !config.raidLootTrackerShowSession;
                WynnExtrasConfig.save();
                return true;
            }

            // Right click while in inventory = start drag
            if (button == 1 && inInventoryScreen) {
                isDragging = true;
                dragOffsetX = (int) mouseX - xPos;
                dragOffsetY = (int) mouseY - yPos;
                return true;
            }

            // Left click = next filter
            if (button == 0 && !ctrlHeld && !shiftHeld) {
                selectedFilterIndex = (selectedFilterIndex + 1) % RAID_FILTERS.size();
                return true;
            }

            // Right click no screen = prev filter
            if (button == 1 && mc.currentScreen == null) {
                selectedFilterIndex = (selectedFilterIndex - 1 + RAID_FILTERS.size()) % RAID_FILTERS.size();
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
