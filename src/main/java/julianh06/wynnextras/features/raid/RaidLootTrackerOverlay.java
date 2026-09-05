package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.utils.text.StyledText;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.TextShadow;
import julianh06.wynnextras.utils.render.VerticalAlignment;
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
    private static final List<String> RAID_FILTERS = Arrays.asList("All", "NOTG", "NOL", "TCC", "TNA", "TWP");
    private static int selectedFilterIndex = 0;

    // Position - loaded from config
    private static int xPos = 5;
    private static int yPos = 5;
    private static final int WIDTH = 165;
    private static int effectiveWidth = WIDTH;
    private static int effectiveHeight;
    private static final int LINE_HEIGHT = 10;
    private static final float TEXT_SCALE = 1.0f;

    // Dragging state
    private static boolean isDragging = false;
    private static boolean hasDragged = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static double dragStartX = 0;
    private static double dragStartY = 0;
    private static ArrowClick pendingArrowClick = ArrowClick.NONE;
    private static boolean pendingEditClick = false;
    private static String pendingLineClick = null;
    private static int activeButton = -1;
    private static boolean editMode = false;

    private enum ArrowClick {
        NONE,
        FILTER_PREVIOUS,
        FILTER_NEXT,
        MODE_PREVIOUS,
        MODE_NEXT
    }

    // Hidden lines - loaded from config
    private static Set<String> hiddenLines = new HashSet<>();
    private static boolean configLoaded = false;

    // Line identifiers
    public static final String LINE_EMERALDS = "emeralds";
    public static final String LINE_AMPLIFIERS = "amplifiers";
    public static final String LINE_AMP_T1 = "amp_t1";
    public static final String LINE_AMP_T2 = "amp_t2";
    public static final String LINE_AMP_T3 = "amp_t3";
    public static final String LINE_AMP_T4 = "amp_t4";
    public static final String LINE_BAGS = "bags";
    public static final String LINE_BAGS_STUFFED = "bags_stuffed";
    public static final String LINE_BAGS_PACKED = "bags_packed";
    public static final String LINE_BAGS_VARIED = "bags_varied";
    public static final String LINE_TOMES = "tomes";
    public static final String LINE_TOMES_MYTHIC = "tomes_mythic";
    public static final String LINE_TOMES_FABLED = "tomes_fabled";
    public static final String LINE_CHARMS = "charms";
    public static final String LINE_POWDERS = "powders";
    public static final String LINE_POWDERS_T1 = "powders_t1";
    public static final String LINE_POWDERS_T2 = "powders_t2";
    public static final String LINE_POWDERS_T3 = "powders_t3";
    public static final String LINE_POWDERS_T4 = "powders_t4";
    public static final String LINE_POWDERS_T5 = "powders_t5";
    public static final String LINE_POWDERS_T6 = "powders_t6";
    public static final String LINE_POWDERS_T7 = "powders_t7";
    public static final String LINE_WARDS = "wards";
    public static final String LINE_ASPECTS = "aspects";
    public static final String LINE_ASPECTS_MYTHIC = "aspects_mythic";
    public static final String LINE_ASPECTS_FABLED = "aspects_fabled";
    public static final String LINE_ASPECTS_LEGENDARY = "aspects_legendary";
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
    private static int[] editButtonBounds = new int[4];

    // Reward chest coordinates for proximity check
    private static final Map<String, double[]> REWARD_CHEST_COORDS = Map.of(
            "NOTG", new double[]{10342, 41, 3111},
            "NOL", new double[]{11005, 58, 2909},
            "TCC", new double[]{10817, 45, 3901},
            "TNA", new double[]{24489, 8, -23878},
            "TWP", new double[]{-19065, 125, -1819}
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
    private static final CustomColor POWDER_COLOR = CustomColor.fromHexString("FFAA00");
    private static final CustomColor WARD_COLOR = CustomColor.fromHexString("f9508e");
    private static final CustomColor ASPECT_COLOR = CustomColor.fromHexString("AA55FF");
    private static final CustomColor HIDDEN_COLOR = CustomColor.fromHexString("555555");
    private static final CustomColor SESSION_COLOR = CustomColor.fromHexString("55FF55");
    private static final String[] POWDER_TIER_NAMES = {"I", "II", "III", "IV", "V", "VI", "VII"};
    private static final String[] POWDER_TIER_LINES = {
            LINE_POWDERS_T1, LINE_POWDERS_T2, LINE_POWDERS_T3, LINE_POWDERS_T4,
            LINE_POWDERS_T5, LINE_POWDERS_T6, LINE_POWDERS_T7
    };
    private static final String[] POWDER_ELEMENT_NAMES = {"Earth", "Thunder", "Water", "Fire", "Air"};
    private static final String[] POWDER_ELEMENT_COLORS = {"§2", "§e", "§b", "§c", "§f"};

    public enum mode { ALL, SESSION, LATEST }

    private static final String[] MODES = {"All-Time", "Session", "Latest"};
    private static final CustomColor[] MODE_COLORS = {AMPLIFIER_COLOR, SESSION_COLOR, FILTER_COLOR};

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
        if (mc.options.hudHidden) return;

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.toggleRaidLootTracker) return;
        if (!config.raidLootTrackerRenderInHud) return;
        if (config.raidLootTrackerOnlyNearChest && !isNearLootChest()) return;

        loadConfig();
        renderOverlay(context, config, false, -1, -1);
    }

    private static final String RAID_CHEST_TITLE = "\uDAFF\uDFEA\uE00E";

    public static void renderOnScreen(DrawContext context, int mouseX, int mouseY) {
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
        renderOverlay(context, config, isInventory || isChat, mouseX, mouseY);
    }

    private static void renderOverlay(DrawContext context, WynnExtrasConfig config, boolean canEdit,
                                      int mouseX, int mouseY) {
        linePositions.clear();
        RaidLootData data = RaidLootConfig.INSTANCE.data;
        data.initSession();

        boolean compact = config.raidLootTrackerCompact;
        boolean showHiddenLines = canEdit && editMode;
        String selectedFilter = RAID_FILTERS.get(selectedFilterIndex);

        // Compute effective width (background + layout) so long labels/values don't overflow
        effectiveWidth = Math.max(WIDTH, cachedMaxContentWidth(config, compact, selectedFilter));

        // Get appropriate data
        RaidLootData.RaidSpecificLoot displayData;
        int completions;

        if(config.raidLootTrackerMode == mode.LATEST) {
            displayData = data.latestData;
            completions = 1;
        } else {
            if (selectedFilter.equals("All")) {
                if (config.raidLootTrackerMode == mode.SESSION) {
                    displayData = data.sessionData;
                    completions = data.sessionData.completionCount;
                } else {
                    displayData = RaidLootData.createAggregateData(data);
                    completions = data.perRaidData.values().stream().mapToInt(r -> r.completionCount).sum();
                }
            } else {
                if (config.raidLootTrackerMode == mode.SESSION) {
                    displayData = data.sessionPerRaidData != null ?
                            data.sessionPerRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot()) :
                            new RaidLootData.RaidSpecificLoot();
                } else {
                    displayData = data.perRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot());
                }
                completions = displayData.completionCount;
            }
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

        int dataY = yPos + LINE_HEIGHT + 2 + LINE_HEIGHT + 3;
        int contentBottom = renderDataLines(null, config, displayData, completions, compact,
                showHiddenLines, dataY, stacks, le, eb);
        effectiveHeight = contentBottom - yPos;

        if (config.raidLootTrackerBackground) {
            int padX = 4;
            int padY = 3;
            int bgX = xPos - padX;
            int bgY = yPos - padY;
            int bgWidth = effectiveWidth + padX * 2;
            int bgHeight = effectiveHeight + padY * 2;
            int bgColor = 0xCC1a1a1a;
            drawBackground(context, bgX, bgY, bgX + bgWidth, bgY + bgHeight, bgColor);
        }

        int renderedDataY = renderHeader(context, config, canEdit, selectedFilter);
        renderDataLines(context, config, displayData, completions, compact,
                showHiddenLines, renderedDataY, stacks, le, eb);

        String hoveredLine = getHoveredLine(mouseX, mouseY);
        int hoveredPowderTier = getPowderTier(hoveredLine);
        if (hoveredPowderTier >= 0) {
            drawPowderTooltip(context, displayData, hoveredPowderTier, mouseX, mouseY);
        } else if (showHiddenLines && !config.automaticAspectScanning && isAspectLine(hoveredLine)) {
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, List.of(
                    Text.literal("§cAutomatic aspect scanning is disabled."),
                    Text.literal("§7Aspect lines only show and update when it's enabled.")
            ), mouseX, mouseY);
        }
    }

    private static int renderHeader(DrawContext context, WynnExtrasConfig config, boolean canEdit,
                                    String selectedFilter) {
        int y = yPos;
        Text pillWithTitle = WynnExtras.addWynnExtrasPrefix(Text.literal("Raid Loot").styled(s -> s.withColor(TITLE_COLOR.asInt())));
        FontRenderer.getInstance().renderText(
                context, StyledText.fromComponent(pillWithTitle),
                xPos, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
        if (canEdit) {
            String editText = editMode ? "[Edit: ON]" : "[Edit]";
            float editWidth = getTextWidth(editText);
            drawTextRight(context, editText, xPos + effectiveWidth, y, editMode ? SESSION_COLOR : FILTER_ARROW_COLOR);
            editButtonBounds = new int[]{(int) (xPos + effectiveWidth - editWidth), y, xPos + effectiveWidth, y + LINE_HEIGHT};
        } else {
            editButtonBounds = new int[4];
        }
        y += LINE_HEIGHT + 2;

        String modeText = MODES[config.raidLootTrackerMode.ordinal()];
        CustomColor modeColor = MODE_COLORS[config.raidLootTrackerMode.ordinal()];
        String leftArrowText = "[\u25C0";
        drawText(context, leftArrowText, xPos, y, FILTER_ARROW_COLOR);
        float leftArrowWidth = getTextWidth(leftArrowText);
        leftArrowBounds = new int[]{xPos, y, (int) (xPos + leftArrowWidth), y + LINE_HEIGHT};

        drawText(context, " ", xPos + leftArrowWidth, y, FILTER_ARROW_COLOR);
        float spaceWidth = getTextWidth(" ");
        float filterStartX = xPos + leftArrowWidth + spaceWidth;
        drawText(context, selectedFilter, filterStartX, y, FILTER_COLOR);
        float filterWidth = getTextWidth(selectedFilter);
        filterNameBounds = new int[]{(int) filterStartX, y, (int) (filterStartX + filterWidth), y + LINE_HEIGHT};

        String rightArrowText = " \u25B6]";
        drawText(context, rightArrowText, xPos + leftArrowWidth + spaceWidth + filterWidth, y, FILTER_ARROW_COLOR);
        float rightArrowWidth = getTextWidth(rightArrowText);
        rightArrowBounds = new int[]{(int) (xPos + leftArrowWidth + spaceWidth + filterWidth), y,
                (int) (xPos + leftArrowWidth + spaceWidth + filterWidth + rightArrowWidth), y + LINE_HEIGHT};

        String modeRightArrow = "\u25B6]";
        float modeRightArrowWidth = getTextWidth(modeRightArrow);
        drawTextRight(context, modeRightArrow, xPos + effectiveWidth, y, FILTER_ARROW_COLOR);
        modeRightArrowBounds = new int[]{(int) (xPos + effectiveWidth - modeRightArrowWidth), y,
                xPos + effectiveWidth, y + LINE_HEIGHT};

        float modeNameWidth = getTextWidth(modeText);
        float modeNameEndX = xPos + effectiveWidth - modeRightArrowWidth - spaceWidth;
        drawTextRight(context, modeText, modeNameEndX, y, modeColor);
        modeNameBounds = new int[]{(int) (modeNameEndX - modeNameWidth), y, (int) modeNameEndX, y + LINE_HEIGHT};

        String modeLeftArrow = "[\u25C0 ";
        float modeLeftArrowWidth = getTextWidth(modeLeftArrow);
        float modeLeftArrowX = modeNameEndX - modeNameWidth;
        drawTextRight(context, modeLeftArrow, modeLeftArrowX, y, FILTER_ARROW_COLOR);
        modeLeftArrowBounds = new int[]{(int) (modeLeftArrowX - modeLeftArrowWidth), y,
                (int) modeLeftArrowX, y + LINE_HEIGHT};
        return y + LINE_HEIGHT + 3;
    }

    private static int renderDataLines(DrawContext context, WynnExtrasConfig config,
                                       RaidLootData.RaidSpecificLoot displayData, int completions,
                                       boolean compact, boolean showHiddenLines, int y,
                                       long stacks, long le, long eb) {
        if (compact) {
            y = drawCompactLine(context, LINE_EMERALDS, "Ems", formatEmeraldsCompact(stacks, le, eb), EMERALD_COLOR, y, showHiddenLines);
            y = drawCompactLine(context, LINE_AMPLIFIERS, "Amps", String.valueOf(displayData.getTotalAmplifiers()), AMPLIFIER_COLOR, y, showHiddenLines);
            y = drawCompactLine(context, LINE_BAGS, "Bags", String.valueOf(displayData.totalBags), BAG_COLOR, y, showHiddenLines);
            y = drawCompactLine(context, LINE_TOMES, "Tomes", String.valueOf(displayData.totalTomes), TOME_COLOR, y, showHiddenLines);
            y = drawCompactLine(context, LINE_CHARMS, "Charms", String.valueOf(displayData.totalCharms), CHARM_COLOR, y, showHiddenLines);
            for (int tier = 0; tier < POWDER_TIER_NAMES.length; tier++) {
                y = drawCompactLine(context, POWDER_TIER_LINES[tier], "Powder " + POWDER_TIER_NAMES[tier],
                        String.valueOf(displayData.getPowderTierTotal(tier)), POWDER_COLOR, y, showHiddenLines);
            }
            y = drawCompactLine(context, LINE_WARDS, "Wards", String.valueOf(displayData.totalWards), WARD_COLOR, y, showHiddenLines);
            if (config.automaticAspectScanning) {
                y = drawCompactLine(context, LINE_ASPECTS, "Aspects", String.valueOf(displayData.mythicAspects + displayData.fabledAspects + displayData.legendaryAspects), ASPECT_COLOR, y, showHiddenLines);
            } else if (showHiddenLines) {
                y = drawDisabledLine(context, LINE_ASPECTS, "Aspects", String.valueOf(displayData.mythicAspects + displayData.fabledAspects + displayData.legendaryAspects), y);
            }
            if (config.raidLootTrackerMode != mode.LATEST) {
                y = drawCompactLine(context, LINE_COMPLETIONS, "Runs", String.valueOf(completions), HEADER_COLOR, y, showHiddenLines);
            }
            return y;
        }

        y = drawLine(context, LINE_EMERALDS, "Emeralds", formatEmeralds(stacks, le, eb), EMERALD_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_AMPLIFIERS, "Amplifiers", String.valueOf(displayData.getTotalAmplifiers()), AMPLIFIER_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_AMP_T1, "  Tier I", String.valueOf(displayData.amplifierTier1), AMPLIFIER_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_AMP_T2, "  Tier II", String.valueOf(displayData.amplifierTier2), AMPLIFIER_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_AMP_T3, "  Tier III", String.valueOf(displayData.amplifierTier3), AMPLIFIER_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_AMP_T4, "  Tier IV", String.valueOf(displayData.amplifierTier4), AMPLIFIER_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_BAGS, "Crafter Bags", String.valueOf(displayData.totalBags), BAG_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_BAGS_STUFFED, "  Stuffed", String.valueOf(displayData.stuffedBags), BAG_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_BAGS_PACKED, "  Packed", String.valueOf(displayData.packedBags), BAG_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_BAGS_VARIED, "  Varied", String.valueOf(displayData.variedBags), BAG_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_TOMES, "Tomes", String.valueOf(displayData.totalTomes), TOME_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_TOMES_MYTHIC, "  Mythic", String.valueOf(displayData.mythicTomes), TOME_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_TOMES_FABLED, "  Fabled", String.valueOf(displayData.fabledTomes), TOME_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_CHARMS, "Charms", String.valueOf(displayData.totalCharms), CHARM_COLOR, y, showHiddenLines);
        y = drawLine(context, LINE_POWDERS, "Powders", String.valueOf(displayData.getTotalPowders()), POWDER_COLOR, y, showHiddenLines);
        for (int tier = 0; tier < POWDER_TIER_NAMES.length; tier++) {
            y = drawLine(context, POWDER_TIER_LINES[tier], "  Tier " + POWDER_TIER_NAMES[tier],
                    String.valueOf(displayData.getPowderTierTotal(tier)), POWDER_COLOR, y, showHiddenLines);
        }
        y = drawLine(context, LINE_WARDS, "Wards", String.valueOf(displayData.totalWards), WARD_COLOR, y, showHiddenLines);

        if (config.automaticAspectScanning) {
            y = drawLine(context, LINE_ASPECTS, "Aspects", String.valueOf(displayData.mythicAspects + displayData.fabledAspects + displayData.legendaryAspects), ASPECT_COLOR, y, showHiddenLines);
            y = drawLine(context, LINE_ASPECTS_MYTHIC, "  Mythic", String.valueOf(displayData.mythicAspects), ASPECT_COLOR, y, showHiddenLines);
            y = drawLine(context, LINE_ASPECTS_FABLED, "  Fabled", String.valueOf(displayData.fabledAspects), ASPECT_COLOR, y, showHiddenLines);
            y = drawLine(context, LINE_ASPECTS_LEGENDARY, "  Legendary", String.valueOf(displayData.legendaryAspects), ASPECT_COLOR, y, showHiddenLines);
        } else if (showHiddenLines) {
            y = drawDisabledLine(context, LINE_ASPECTS, "Aspects", String.valueOf(displayData.mythicAspects + displayData.fabledAspects + displayData.legendaryAspects), y);
            y = drawDisabledLine(context, LINE_ASPECTS_MYTHIC, "  Mythic", String.valueOf(displayData.mythicAspects), y);
            y = drawDisabledLine(context, LINE_ASPECTS_FABLED, "  Fabled", String.valueOf(displayData.fabledAspects), y);
            y = drawDisabledLine(context, LINE_ASPECTS_LEGENDARY, "  Legendary", String.valueOf(displayData.legendaryAspects), y);
        }

        if (config.raidLootTrackerMode != mode.LATEST && shouldRenderLine(LINE_COMPLETIONS, showHiddenLines)) {
            y += 2;
            y = drawLine(context, LINE_COMPLETIONS, "Runs", String.valueOf(completions), HEADER_COLOR, y, showHiddenLines);
        }
        return y;
    }


    private static int drawLine(DrawContext context, String lineId, String label, String value,
                                CustomColor color, int y, boolean inInventory) {
        boolean isHidden = hiddenLines.contains(lineId);

        if (isHidden && !inInventory) return y;

        if (context != null) {
            linePositions.put(lineId, new int[]{y, y + LINE_HEIGHT});
            if (isHidden && inInventory) {
                drawTextStrikethrough(context, label + ": " + value, xPos, y, HIDDEN_COLOR);
            } else {
                drawText(context, label + ":", xPos, y, color);
                drawTextRight(context, value, xPos + effectiveWidth, y, VALUE_COLOR);
            }
        }

        return y + LINE_HEIGHT;
    }

    private static int drawCompactLine(DrawContext context, String lineId, String label, String value,
                                       CustomColor color, int y, boolean inInventory) {
        boolean isHidden = hiddenLines.contains(lineId);
        if (isHidden && !inInventory) return y;

        if (context != null) {
            linePositions.put(lineId, new int[]{y, y + LINE_HEIGHT});
            if (isHidden && inInventory) {
                drawTextStrikethrough(context, label + ":" + value, xPos, y, HIDDEN_COLOR);
            } else {
                drawText(context, label + ":", xPos, y, color);
                drawTextRight(context, value, xPos + effectiveWidth, y, VALUE_COLOR);
            }
        }

        return y + LINE_HEIGHT;
    }

    private static int drawDisabledLine(DrawContext context, String lineId, String label, String value, int y) {
        if (context != null) {
            linePositions.put(lineId, new int[]{y, y + LINE_HEIGHT});
            drawText(context, label + ":", xPos, y, HIDDEN_COLOR);
            drawTextRight(context, value, xPos + effectiveWidth, y, HIDDEN_COLOR);
        }
        return y + LINE_HEIGHT;
    }

    private static int cachedContentWidth = -1;
    private static long cachedContentWidthAt = 0L;
    private static boolean cachedContentWidthCompact;
    private static String cachedContentWidthFilter;
    private static mode cachedContentWidthMode;
    private static final long CONTENT_WIDTH_CACHE_MS = 250L;

    private static int cachedMaxContentWidth(WynnExtrasConfig config, boolean compact, String selectedFilter) {
        long now = System.currentTimeMillis();
        boolean sameInputs = cachedContentWidth >= 0
                && cachedContentWidthCompact == compact
                && cachedContentWidthMode == config.raidLootTrackerMode
                && selectedFilter.equals(cachedContentWidthFilter);
        if (sameInputs && now - cachedContentWidthAt < CONTENT_WIDTH_CACHE_MS) return cachedContentWidth;

        cachedContentWidth = calculateMaxContentWidth(config, compact, selectedFilter);
        cachedContentWidthCompact = compact;
        cachedContentWidthMode = config.raidLootTrackerMode;
        cachedContentWidthFilter = selectedFilter;
        cachedContentWidthAt = now;
        return cachedContentWidth;
    }

    /** Compute the widest label+value pair needed so the background hugs the content. */
    private static int calculateMaxContentWidth(WynnExtrasConfig config, boolean compact, String selectedFilter) {
        RaidLootData data = RaidLootConfig.INSTANCE.data;
        RaidLootData.RaidSpecificLoot d;
        int completions;
        if (config.raidLootTrackerMode == mode.LATEST) {
            d = data.latestData;
            completions = 1;
        } else if (selectedFilter.equals("All")) {
            if (config.raidLootTrackerMode == mode.SESSION) {
                d = data.sessionData;
                completions = data.sessionData.completionCount;
            } else {
                d = RaidLootData.createAggregateData(data);
                completions = data.perRaidData.values().stream().mapToInt(r -> r.completionCount).sum();
            }
        } else {
            if (config.raidLootTrackerMode == mode.SESSION) {
                d = data.sessionPerRaidData != null
                        ? data.sessionPerRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot())
                        : new RaidLootData.RaidSpecificLoot();
            } else {
                d = data.perRaidData.getOrDefault(selectedFilter, new RaidLootData.RaidSpecificLoot());
            }
            completions = d.completionCount;
        }

        long totalEmeralds = (d.liquidEmeralds * 64L * 64L) + (d.emeraldBlocks * 64L);
        long stacks = totalEmeralds / 262144;
        long rem1 = totalEmeralds % 262144;
        long le = rem1 / 4096;
        long rem2 = rem1 % 4096;
        long eb = rem2 / 64;

        String emVal = compact ? formatEmeraldsCompact(stacks, le, eb) : formatEmeralds(stacks, le, eb);
        String[][] pairs;
        if (compact) {
            pairs = new String[][]{
                {"Ems", emVal}, {"Amps", String.valueOf(d.getTotalAmplifiers())},
                {"Bags", String.valueOf(d.totalBags)}, {"Tomes", String.valueOf(d.totalTomes)},
                {"Charms", String.valueOf(d.totalCharms)},
                {"Powder I", String.valueOf(d.getPowderTierTotal(0))},
                {"Powder II", String.valueOf(d.getPowderTierTotal(1))},
                {"Powder III", String.valueOf(d.getPowderTierTotal(2))},
                {"Powder IV", String.valueOf(d.getPowderTierTotal(3))},
                {"Powder V", String.valueOf(d.getPowderTierTotal(4))},
                {"Powder VI", String.valueOf(d.getPowderTierTotal(5))},
                {"Powder VII", String.valueOf(d.getPowderTierTotal(6))},
                {"Wards", String.valueOf(d.totalWards)},
                {"Aspects", String.valueOf(d.mythicAspects + d.fabledAspects + d.legendaryAspects)},
                {"Runs", String.valueOf(completions)}
            };
        } else {
            pairs = new String[][]{
                {"Emeralds", emVal},
                {"Amplifiers", String.valueOf(d.getTotalAmplifiers())},
                {"Crafter Bags", String.valueOf(d.totalBags)},
                {"Tomes", String.valueOf(d.totalTomes)},
                {"Charms", String.valueOf(d.totalCharms)},
                {"Powders", String.valueOf(d.getTotalPowders())},
                {"Wards", String.valueOf(d.totalWards)},
                {"Aspects", String.valueOf(d.mythicAspects + d.fabledAspects + d.legendaryAspects)},
                {"Runs", String.valueOf(completions)}
            };
        }

        float max = 0;
        float gap = getTextWidth("  ");
        for (String[] p : pairs) {
            float w = getTextWidth(p[0] + ":") + gap + getTextWidth(p[1]);
            if (w > max) max = w;
        }

        // Also account for the filter/mode header row
        String modeText = MODES[config.raidLootTrackerMode.ordinal()];
        float headerW = getTextWidth("[\u25C0 " + selectedFilter + " \u25B6]") + gap +
                        getTextWidth("[\u25C0 " + modeText + " \u25B6]");
        if (headerW > max) max = headerW;

        return (int) Math.ceil(max);
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

    private static boolean shouldRenderLine(String lineId, boolean showHiddenLines) {
        return showHiddenLines || !hiddenLines.contains(lineId);
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

    private static String getHoveredLine(double mouseX, double mouseY) {
        if (mouseX < xPos || mouseX > xPos + effectiveWidth) return null;
        return getLineClick(mouseY);
    }

    private static int getPowderTier(String lineId) {
        for (int tier = 0; tier < POWDER_TIER_LINES.length; tier++) {
            if (POWDER_TIER_LINES[tier].equals(lineId)) return tier;
        }
        return -1;
    }

    private static void drawPowderTooltip(DrawContext context, RaidLootData.RaidSpecificLoot data,
                                          int tier, int mouseX, int mouseY) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal("§6§lPowder Tier " + POWDER_TIER_NAMES[tier]));
        for (int element = 0; element < POWDER_ELEMENT_NAMES.length; element++) {
            tooltip.add(Text.literal(POWDER_ELEMENT_COLORS[element] + POWDER_ELEMENT_NAMES[element]
                    + ": §f" + data.getPowderCount(element, tier)));
        }
        context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, mouseX, mouseY);
    }

    private static boolean isAspectLine(String lineId) {
        return LINE_ASPECTS.equals(lineId) || LINE_ASPECTS_MYTHIC.equals(lineId)
                || LINE_ASPECTS_FABLED.equals(lineId) || LINE_ASPECTS_LEGENDARY.equals(lineId);
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
        int contentHeight = effectiveHeight;

        boolean inBounds = mouseX >= xPos - 2 && mouseX <= xPos + effectiveWidth + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + contentHeight + 4;

        boolean inInventoryScreen = mc.currentScreen instanceof InventoryScreen;
        boolean inChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean inRaidChest = mc.currentScreen != null && mc.currentScreen.getTitle().getString().equals(RAID_CHEST_TITLE);
        boolean canInteract = inInventoryScreen || inChatScreen || inRaidChest;

        if (action == 0) {
            if (button != activeButton) return false;

            boolean wasDragging = isDragging;
            boolean editClicked = !hasDragged && pendingEditClick && isInBounds(mouseX, mouseY, editButtonBounds);
            boolean arrowClicked = !hasDragged && isArrowInBounds(pendingArrowClick, mouseX, mouseY);
            boolean lineClicked = !hasDragged && pendingLineClick != null && inBounds && pendingLineClick.equals(getLineClick(mouseY));

            if (wasDragging) {
                isDragging = false;
                if (hasDragged) saveConfig();
            }

            if (editClicked) editMode = !editMode;
            if (arrowClicked) applyArrowClick(pendingArrowClick, config);
            if (lineClicked) toggleLine(pendingLineClick);

            pendingArrowClick = ArrowClick.NONE;
            pendingEditClick = false;
            pendingLineClick = null;
            activeButton = -1;
            hasDragged = false;
            return wasDragging || editClicked || arrowClicked || lineClicked;
        }

        if (!inBounds || !canInteract || button < 0 || button > 1) return false;

        pendingEditClick = (inInventoryScreen || inChatScreen) && isInBounds(mouseX, mouseY, editButtonBounds);
        pendingArrowClick = pendingEditClick ? ArrowClick.NONE : getArrowClick(mouseX, mouseY);
        pendingLineClick = !pendingEditClick && pendingArrowClick == ArrowClick.NONE && editMode &&
                (inInventoryScreen || inChatScreen) ? getLineClick(mouseY) : null;
        activeButton = button;
        isDragging = true;
        hasDragged = false;
        dragStartX = mouseX;
        dragStartY = mouseY;
        dragOffsetX = (int) mouseX - xPos;
        dragOffsetY = (int) mouseY - yPos;

        return true;
    }

    private static ArrowClick getArrowClick(double mouseX, double mouseY) {
        if (isInBounds(mouseX, mouseY, leftArrowBounds)) return ArrowClick.FILTER_PREVIOUS;
        if (isInBounds(mouseX, mouseY, rightArrowBounds)) return ArrowClick.FILTER_NEXT;
        if (isInBounds(mouseX, mouseY, modeLeftArrowBounds)) return ArrowClick.MODE_PREVIOUS;
        if (isInBounds(mouseX, mouseY, modeRightArrowBounds)) return ArrowClick.MODE_NEXT;
        return ArrowClick.NONE;
    }

    private static boolean isArrowInBounds(ArrowClick arrowClick, double mouseX, double mouseY) {
        return switch (arrowClick) {
            case FILTER_PREVIOUS -> isInBounds(mouseX, mouseY, leftArrowBounds);
            case FILTER_NEXT -> isInBounds(mouseX, mouseY, rightArrowBounds);
            case MODE_PREVIOUS -> isInBounds(mouseX, mouseY, modeLeftArrowBounds);
            case MODE_NEXT -> isInBounds(mouseX, mouseY, modeRightArrowBounds);
            case NONE -> false;
        };
    }

    private static String getLineClick(double mouseY) {
        for (Map.Entry<String, int[]> entry : linePositions.entrySet()) {
            int[] bounds = entry.getValue();
            if (mouseY >= bounds[0] && mouseY < bounds[1]) return entry.getKey();
        }
        return null;
    }

    private static void toggleLine(String lineId) {
        if (!WynnExtrasConfig.INSTANCE.automaticAspectScanning && isAspectLine(lineId)) return;
        if (hiddenLines.contains(lineId)) {
            hiddenLines.remove(lineId);
        } else {
            hiddenLines.add(lineId);
        }
        saveConfig();
    }

    private static void applyArrowClick(ArrowClick arrowClick, WynnExtrasConfig config) {
        switch (arrowClick) {
            case FILTER_PREVIOUS -> selectedFilterIndex =
                    (selectedFilterIndex - 1 + RAID_FILTERS.size()) % RAID_FILTERS.size();
            case FILTER_NEXT -> selectedFilterIndex = (selectedFilterIndex + 1) % RAID_FILTERS.size();
            case MODE_PREVIOUS -> {
                config.raidLootTrackerMode = mode.values()[
                        (config.raidLootTrackerMode.ordinal() - 1 + mode.values().length) % mode.values().length];
                WynnExtrasConfig.save();
            }
            case MODE_NEXT -> {
                config.raidLootTrackerMode = mode.values()[
                        (config.raidLootTrackerMode.ordinal() + 1) % mode.values().length];
                WynnExtrasConfig.save();
            }
            case NONE -> {
            }
        }
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (!isDragging) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            isDragging = false;
            hasDragged = false;
            pendingArrowClick = ArrowClick.NONE;
            pendingEditClick = false;
            pendingLineClick = null;
            activeButton = -1;
            return;
        }

        if (!hasDragged) {
            double deltaX = mouseX - dragStartX;
            double deltaY = mouseY - dragStartY;
            if (deltaX * deltaX + deltaY * deltaY < 9) return;
            hasDragged = true;
        }

        xPos = (int) mouseX - dragOffsetX;
        yPos = (int) mouseY - dragOffsetY;

        if (mc.getWindow() != null) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            xPos = Math.max(0, Math.min(xPos, screenWidth - effectiveWidth));
            yPos = Math.max(0, Math.min(yPos, screenHeight - 100));
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
