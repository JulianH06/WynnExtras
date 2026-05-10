package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.core.WynnExtras;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import julianh06.wynnextras.config.WynnExtrasConfig.CharIdentity;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.utils.TickScheduler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;;

public class ClassSelectionOverlay extends WEHandledScreen {

    @Override protected double getTargetScaleFactor() { return 2.0; }
    @Override protected int getMinScreenWidth() { return 700; }
    @Override protected int getMinScreenHeight() { return 500; }

    public static final String CLASS_SELECTION_TITLE = "\uDAFF\uDFD5\uE01F";
    public static final String CLASS_EDIT_TITLE = "\uDAFF\uDFD0\uE020";
    public static final String ICON_EDIT_TITLE = "\uDAFF\uDFDB\uE023";

    public enum ScreenMode { CLASS_SELECTION, CLASS_EDIT, ICON_EDIT }

    private ScreenMode mode;
    private HandledScreen<?> screen;

    // ==================== CLASS SELECTION SLOTS ====================
    private static final int[] CHARACTER_SLOTS = {9, 10, 11, 18, 19, 20, 27, 28, 29, 36, 37, 38, 45, 46, 47};
    private static final int COLS = 3;

    private static final int SLOT_CANCEL_DELETION = 7;
    private static final int SLOT_BACKUPS = 25;
    private static final int SLOT_MUSIC = 51;
    private static final int SLOT_AUTO_OPEN = 53;

    // ==================== CLASS EDIT SLOTS ====================
    private static final int SLOT_EDIT_NICKNAME = 7;
    private static final int SLOT_FAVORITE = 19;
    private static final int SLOT_CHANGE_SCENE = 25;
    private static final int SLOT_RESKIN_CLASS = 37;
    private static final int SLOT_EDIT_ICON = 43;

    // ==================== ICON EDIT SLOTS ====================
    private static final int[] ICON_COLOR_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final int ICON_COLS = 3;
    private static final int[] ICON_SUB_SLOTS = {37, 38, 39, 40, 41};
    private static final int ICON_BACK_SLOT = 10;
    private static final int ICON_PREVIEW_SLOT = 43;
    private static final String[] ICON_COLOR_NAMES = {"Gray", "Red", "Orange", "Yellow", "Green", "Blue", "Dark Blue", "Purple", "Pink"};

    // Hover state
    private int hoveredCharVisIdx = -1;
    private int hoveredSettingSlot = -1;
    private int hoveredEditOption = -1;
    private int hoveredIconColor = -1;
    private int hoveredIconSub = -1;
    private boolean hoveredToggle = false;
    private boolean hoveredColorToggle = false;
    private boolean hoveredBack = false;
    private List<Text> hoveredTooltip = new ArrayList<>();

    // Drag state
    private int pressedVisIdx = -1;
    private int pressedButton = 0;
    private double pressStartX, pressStartY;
    private boolean isDragging = false;
    private static final double DRAG_THRESHOLD = 5.0;

    // Card layout cache (logical coords)
    private float[] cardLX = new float[15];
    private float[] cardLY = new float[15];
    private float cardLW, cardLH;
    private int visibleCardCount = 0;
    // visOrder[i] = the CHARACTER_SLOTS array index for the i-th visible card
    private int[] visOrder = new int[15];
    // visCharId[i] = the UUID for the i-th visible card
    private String[] visCharId = new String[15];
    // Only run identity matching once per screen open
    private boolean identityMatched = false;

    // Track which character we're editing (set when right-clicking a card)
    private static String editingCharId = "";

    // Vanilla toggle
    public static boolean vanillaMode = false;

    // Toggle/back button bounds (logical)
    private float toggleLX, toggleLY, toggleLW, toggleLH;
    private float colorToggleLX, colorToggleLY, colorToggleLW, colorToggleLH;
    private float backLX, backLY, backLW, backLH;

    // Custom background from config/wynnextras/customscreen/
    private static Identifier bgTexture = null;
    private static boolean bgScanned = false;
    private static int bgImgW = 1, bgImgH = 1;

    // Nickname input state (static so BankOverlay event handlers can forward)
    public static boolean nicknameInputActive = false;
    private static String nicknameText = "";
    private static int nicknameCursor = 0;
    private static String nicknameCharId = ""; // character identifier we're editing
    private static boolean nicknameHasRank = false; // true = send via chat, false = client-side only

    public ClassSelectionOverlay(HandledScreen<?> screen, ScreenMode mode) {
        this.screen = screen;
        this.mode = mode;
    }

    public ScreenMode getMode() { return mode; }

    // ==================== CHARACTER IDENTIFICATION ====================

    /** Extract current character data from an ItemStack in the class selection screen */
    private static boolean charDataDebugLogged = false;
    private CharIdentity extractCharData(ItemStack stack) {
        CharIdentity data = new CharIdentity();
        data.name = cleanName(stack.getName().getString());
        data.classType = extractClassName(stack);
        data.color = extractPotionColor(stack);
        data.timePlayed = extractTimePlayed(stack);
        data.level = extractLevel(stack);
        data.xpPercent = extractXpPercent(stack);
        return data;
    }

    private int extractPotionColor(ItemStack stack) {
        try {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null && contents.customColor().isPresent()) {
                return contents.customColor().get();
            }
        } catch (Exception e) {}
        return 0;
    }

    private double extractTimePlayed(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String str = line.getString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            if (str.contains("Time Played:")) {
                String after = str.substring(str.indexOf("Time Played:") + "Time Played:".length()).trim();
                after = after.replace("hours", "").replace("hour", "").trim();
                try { return Double.parseDouble(after); }
                catch (NumberFormatException e) { return 0; }
            }
        }
        return 0;
    }

    private int extractLevel(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String str = line.getString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            if (str.contains("Level:")) {
                String after = str.substring(str.indexOf("Level:") + "Level:".length()).trim();
                // "106 (0%)" → extract 106
                String levelStr = after.split("[^0-9]")[0];
                try { return Integer.parseInt(levelStr); }
                catch (NumberFormatException e) { return 0; }
            }
        }
        return 0;
    }

    private int extractXpPercent(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String str = line.getString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            if (str.contains("Level:") && str.contains("%")) {
                int pIdx = str.indexOf('(');
                int eIdx = str.indexOf('%');
                if (pIdx >= 0 && eIdx > pIdx) {
                    try { return Integer.parseInt(str.substring(pIdx + 1, eIdx).trim()); }
                    catch (NumberFormatException e) { return 0; }
                }
            }
        }
        return 0;
    }

    /** Compute match score between a stored identity and current character data.
     *  Higher = better match. */
    private int matchScore(CharIdentity stored, CharIdentity current) {
        int score = 0;
        // Name match (strongest signal when names are set)
        if (!stored.name.isEmpty() && stored.name.equals(current.name)) score += 10;
        // Class type match
        if (!stored.classType.isEmpty() && stored.classType.equals(current.classType)) score += 10;
        // Color match — very strong signal when different classes have different colors
        if (stored.color != 0 && stored.color == current.color) score += 15;
        // Time played: within 24h = strong, within 72h = weaker
        double timeDiff = Math.abs(stored.timePlayed - current.timePlayed);
        if (stored.timePlayed > 0 && current.timePlayed > 0) {
            if (timeDiff <= 24.0) score += 8;
            else if (timeDiff <= 72.0) score += 4;
        }
        // Level: exact or close (accounts for leveling up between visits)
        if (stored.level > 0 && current.level > 0) {
            int levelDiff = current.level - stored.level; // should be >= 0 (leveled up)
            if (levelDiff == 0) score += 8;
            else if (levelDiff > 0 && levelDiff <= 10) score += 6;
            else if (Math.abs(levelDiff) <= 2) score += 4; // small backward = rounding
        }
        return score;
    }

    /** Match current characters to stored identities, returning UUID for each.
     *  Assigns best matches first, creates new UUIDs for unmatched characters.
     *  Updates stored identities with fresh data. */
    private String[] matchCharacters(List<CharIdentity> currentChars) {
        Map<String, CharIdentity> stored = WynnExtrasConfig.INSTANCE.charIdentities;
        String[] uuids = new String[currentChars.size()];
        boolean[] currentUsed = new boolean[currentChars.size()];
        Set<String> storedUsed = new HashSet<>();

        // Build score matrix: [currentIdx][storedUuid] → score
        List<String> storedUuids = new ArrayList<>(stored.keySet());
        int[][] scores = new int[currentChars.size()][storedUuids.size()];
        for (int i = 0; i < currentChars.size(); i++) {
            for (int j = 0; j < storedUuids.size(); j++) {
                scores[i][j] = matchScore(stored.get(storedUuids.get(j)), currentChars.get(i));
            }
        }

        // Greedy assignment: pick best score first, assign, repeat
        int minScoreThreshold = 15; // must match on at least name+class or class+time
        for (int round = 0; round < currentChars.size(); round++) {
            int bestI = -1, bestJ = -1, bestScore = minScoreThreshold - 1;
            for (int i = 0; i < currentChars.size(); i++) {
                if (currentUsed[i]) continue;
                for (int j = 0; j < storedUuids.size(); j++) {
                    if (storedUsed.contains(storedUuids.get(j))) continue;
                    if (scores[i][j] > bestScore) {
                        bestScore = scores[i][j];
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            if (bestI >= 0) {
                String uuid = storedUuids.get(bestJ);
                uuids[bestI] = uuid;
                currentUsed[bestI] = true;
                storedUsed.add(uuid);
            } else {
                break; // no more good matches
            }
        }

        // Assign new UUIDs for unmatched characters
        for (int i = 0; i < currentChars.size(); i++) {
            if (!currentUsed[i]) {
                String newUuid = UUID.randomUUID().toString().substring(0, 8);
                uuids[i] = newUuid;
            }
        }

        // Update stored identities with fresh data
        for (int i = 0; i < currentChars.size(); i++) {
            CharIdentity fresh = currentChars.get(i);
            fresh.uuid = uuids[i];
            stored.put(uuids[i], fresh);
        }
        WynnExtrasConfig.save();

        return uuids;
    }

    /** Extract just the class type name (without level) */
    private String extractClassName(ItemStack stack) {
        String[] classNames = {"Warrior", "Knight", "Mage", "Dark Wizard", "Assassin", "Ninja",
                "Archer", "Hunter", "Shaman", "Skyseer"};
        for (Text line : getTooltipLines(stack)) {
            String str = line.getString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            for (String cn : classNames) {
                if (str.contains(cn)) return cn;
            }
        }
        return "";
    }

    private void saveCardOrder() {
        List<String> order = new ArrayList<>();
        for (int i = 0; i < visibleCardCount; i++) {
            order.add(visCharId[i]);
        }
        WynnExtrasConfig.INSTANCE.classCardOrder = order;
        WynnExtrasConfig.save();
    }

    public static boolean isClassSelectionScreen(String title) { return CLASS_SELECTION_TITLE.equals(title); }
    public static boolean isClassEditScreen(String title) { return CLASS_EDIT_TITLE.equals(title); }
    public static boolean isIconEditScreen(String title) { return ICON_EDIT_TITLE.equals(title); }

    /** Convert desired screen pixels to logical UIUtils coordinates */
    private float px(float screenPx) { return screenPx * (float) scaleFactor; }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, screenWidth, screenHeight, 0xFF101010);

        // Custom background image from config/wynnextras/customscreen/ folder
        if (!bgScanned) {
            bgScanned = true;
            scanCustomBackgrounds();
        }
        if (bgTexture != null) {
            RenderUtils.drawTexturedRect(ctx, bgTexture, CommonColors.WHITE,
                    0, 0, screenWidth, screenHeight,
                    0, 0, bgImgW, bgImgH, bgImgW, bgImgH);
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredCharVisIdx = -1;
        hoveredSettingSlot = -1;
        hoveredEditOption = -1;
        hoveredIconColor = -1;
        hoveredIconSub = -1;
        hoveredToggle = false;
        hoveredColorToggle = false;
        hoveredBack = false;
        hoveredTooltip = new ArrayList<>();

        if (mode == ScreenMode.CLASS_SELECTION) {
            drawClassSelection(ctx, mouseX, mouseY);
        } else if (mode == ScreenMode.CLASS_EDIT) {
            drawClassEdit(ctx, mouseX, mouseY);
        } else if (mode == ScreenMode.ICON_EDIT) {
            drawIconEdit(ctx, mouseX, mouseY);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (isDragging && pressedVisIdx >= 0 && mode == ScreenMode.CLASS_SELECTION) {
            drawDraggedCard(ctx, mouseX, mouseY);
        }
        if (!hoveredTooltip.isEmpty() && !nicknameInputActive) {
            // ctx.drawTooltip expects GUI-scale coordinates and clamps against screen.width/height.
            // mouseX/mouseY here are in logical space (divided by matrixScale), so undo the
            // matrix transform before calling to prevent boundary clamping going wrong at high GUI scales.
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale((float)(1.0 / matrixScale), (float)(1.0 / matrixScale));
            ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(),
                    (int)(mouseX * matrixScale), (int)(mouseY * matrixScale));
            ctx.getMatrices().popMatrix();
        }
        drawNicknameInput(ctx, mouseX, mouseY);
    }

    // ==================== CLASS SELECTION ====================

    private void drawClassSelection(DrawContext ctx, int mouseX, int mouseY) {
        List<ItemStack> stacks = getStacks();
        if (stacks == null || stacks.isEmpty()) return;

        float cardWPx = 200, cardHPx = 56, gapXPx = 14, gapYPx = 10;
        float titleHPx = 36, settingsHPx = 28, marginPx = 16;

        // Build visible card list with fuzzy identity matching
        if (!identityMatched) {
            // Collect all non-empty characters
            List<Integer> charSlotIndices = new ArrayList<>();
            List<CharIdentity> charDataList = new ArrayList<>();
            for (int i = 0; i < CHARACTER_SLOTS.length; i++) {
                int slotIdx = CHARACTER_SLOTS[i];
                if (slotIdx >= stacks.size()) continue;
                ItemStack stack = stacks.get(slotIdx);
                if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) continue;
                charSlotIndices.add(i);
                charDataList.add(extractCharData(stack));
            }

            if (!charDataList.isEmpty()) {
                // Debug: log all character data once
                if (!charDataDebugLogged) {
                    charDataDebugLogged = true;
                    for (CharIdentity cd : charDataList) {
                        WynnExtras.LOGGER.info("[WynnExtras] Char: name=" + cd.name + " class=" + cd.classType
                                + " color=" + cd.color + " time=" + cd.timePlayed + " lv=" + cd.level + " xp=" + cd.xpPercent + "%");
                    }
                }

                // Match current characters to stored identities → get UUIDs
                String[] uuids = matchCharacters(charDataList);

                // Sort by saved order (UUIDs), unmatched go at end
                List<String> savedOrder = WynnExtrasConfig.INSTANCE.classCardOrder;
                visibleCardCount = 0;
                boolean[] used = new boolean[charSlotIndices.size()];
                if (savedOrder != null) {
                    for (String savedUuid : savedOrder) {
                        for (int j = 0; j < uuids.length; j++) {
                            if (!used[j] && uuids[j].equals(savedUuid)) {
                                visOrder[visibleCardCount] = charSlotIndices.get(j);
                                visCharId[visibleCardCount] = uuids[j];
                                visibleCardCount++;
                                used[j] = true;
                                break;
                            }
                        }
                    }
                }
                for (int j = 0; j < charSlotIndices.size(); j++) {
                    if (!used[j]) {
                        visOrder[visibleCardCount] = charSlotIndices.get(j);
                        visCharId[visibleCardCount] = uuids[j];
                        visibleCardCount++;
                    }
                }
                identityMatched = true;

                // Auto-select character if coming from cross-class bank page click
                tryAutoSelectCharacter(charDataList, charSlotIndices, stacks);
            }
        }

        int rows = (visibleCardCount + COLS - 1) / COLS;
        float gridWPx = COLS * cardWPx + (COLS - 1) * gapXPx;
        float gridHPx = rows * cardHPx + (rows - 1) * gapYPx;

        float panelWPx = gridWPx + marginPx * 2;
        float panelHPx = titleHPx + gridHPx + settingsHPx + marginPx * 3 + 24;
        float panelXPx = (screenWidth - panelWPx) / 2f;
        float panelYPx = (screenHeight - panelHPx) / 2f;

        drawPanel(panelXPx, panelYPx, panelWPx, panelHPx);

        // Title bar
        float titleYPx = panelYPx + marginPx;
        ui.drawRect(px(panelXPx + marginPx), px(titleYPx) - px(2),
                px(panelWPx - marginPx * 2), px(titleHPx), CustomColor.fromHexString("2e251c"));
        ui.drawCenteredText("Select Your Character",
                px(panelXPx + panelWPx / 2f), px(titleYPx + titleHPx / 2f),
                CustomColor.fromHexString("FFAA00"), 4.5f);

        // "Vanilla" toggle button in title bar right side
        float tbWPx = 50, tbHPx = 16;
        float tbXPx = panelXPx + panelWPx - marginPx - tbWPx - 4;
        float tbYPx = titleYPx + (titleHPx - tbHPx) / 2f;
        toggleLX = px(tbXPx); toggleLY = px(tbYPx); toggleLW = px(tbWPx); toggleLH = px(tbHPx);
        hoveredToggle = isInBounds(mouseX, mouseY, toggleLX, toggleLY, toggleLW, toggleLH);
        ui.drawButton(toggleLX, toggleLY, toggleLW, toggleLH, hoveredToggle);
        ui.drawCenteredText("Vanilla", toggleLX + toggleLW / 2f, toggleLY + toggleLH / 2f,
                CustomColor.fromHexString("AAAAAA"), 2f);

        // Color toggle button (class colors vs. brown)
        float ctWPx = 60, ctHPx = 16;
        float ctXPx = tbXPx - ctWPx - 6;
        float ctYPx = tbYPx;
        colorToggleLX = px(ctXPx); colorToggleLY = px(ctYPx); colorToggleLW = px(ctWPx); colorToggleLH = px(ctHPx);
        hoveredColorToggle = isInBounds(mouseX, mouseY, colorToggleLX, colorToggleLY, colorToggleLW, colorToggleLH);
        ui.drawButton(colorToggleLX, colorToggleLY, colorToggleLW, colorToggleLH, hoveredColorToggle);
        boolean colored = WynnExtrasConfig.INSTANCE.classCardColoredAccents;
        ui.drawCenteredText(colored ? "§aColored" : "§7Brown",
                colorToggleLX + colorToggleLW / 2f, colorToggleLY + colorToggleLH / 2f,
                colored ? CustomColor.fromHexString("55FF55") : CustomColor.fromHexString("5d4736"), 2f);

        // Separator
        float sepYPx = titleYPx + titleHPx + 4;
        ui.drawRect(px(panelXPx + marginPx + 10), px(sepYPx),
                px(panelWPx - (marginPx + 10) * 2), px(1), CustomColor.fromHexString("5d4736"));

        // Character cards
        float gridStartXPx = panelXPx + marginPx;
        float gridStartYPx = sepYPx + 8;
        cardLW = px(cardWPx);
        cardLH = px(cardHPx);

        for (int vis = 0; vis < visibleCardCount; vis++) {
            int slotIdx = CHARACTER_SLOTS[visOrder[vis]];
            ItemStack stack = stacks.get(slotIdx);

            int col = vis % COLS;
            int row = vis / COLS;
            float cxPx = gridStartXPx + col * (cardWPx + gapXPx);
            float cyPx = gridStartYPx + row * (cardHPx + gapYPx);
            float cx = px(cxPx), cy = px(cyPx);
            cardLX[vis] = cx;
            cardLY[vis] = cy;

            // Placeholder for dragged card
            if (isDragging && vis == pressedVisIdx) {
                ui.drawRect(cx, cy, cardLW, cardLH, CustomColor.fromHexString("0d0d0d"));
                continue;
            }

            boolean hovered = !isDragging && isInBounds(mouseX, mouseY, cx, cy, cardLW, cardLH);
            boolean dropTarget = isDragging && vis != pressedVisIdx
                    && isInBounds(mouseX, mouseY, cx, cy, cardLW, cardLH);

            if (hovered) {
                hoveredCharVisIdx = vis;
                hoveredTooltip = getTooltipLines(stack);
            }

            drawCharCard(ctx, stack, visCharId[vis], cx, cy, hovered, dropTarget);
        }

        // Settings buttons
        float settingsYPx = gridStartYPx + rows * (cardHPx + gapYPx) + 4;
        drawSettingsButtons(ctx, stacks, mouseX, mouseY, panelXPx, settingsYPx, panelWPx);

        // Hint text
        ui.drawCenteredText("\u00A77Left Click: Play  |  Right Click: Edit  |  Drag: Rearrange",
                px(panelXPx + panelWPx / 2f), px(settingsYPx + settingsHPx + 4),
                CustomColor.fromHexString("666666"), 2.2f);
    }

    private void drawCharCard(DrawContext ctx, ItemStack stack, String charId, float cx, float cy,
                               boolean hovered, boolean dropTarget) {
        CustomColor bgColor;
        if (dropTarget) bgColor = CustomColor.fromHexString("5a4530");
        else if (hovered) bgColor = CustomColor.fromHexString("4d3c2d");
        else bgColor = CustomColor.fromHexString("2e251c");
        ui.drawRect(cx, cy, cardLW, cardLH, bgColor);

        String classInfo = extractClassInfo(stack);
        CustomColor accent = getClassColor(classInfo);

        // Left accent bar
        ui.drawRect(cx, cy, px(3), cardLH, accent);

        // Top highlight on hover/drop
        if (hovered || dropTarget) {
            CustomColor highlightColor = dropTarget ? CustomColor.fromHexString("FFAA00") : accent;
            ui.drawRect(cx, cy, cardLW, px(2), highlightColor);
        }

        // Item icon
        float iconAreaPx = 44;
        ctx.getMatrices().pushMatrix();
        float sIX = ui.sx(cx + px(10));
        float sIY = ui.sy(cy + px(6));
        ctx.getMatrices().translate(sIX, sIY);
        float iScale = iconAreaPx / 16f;
        ctx.getMatrices().scale(iScale, iScale);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().popMatrix();

        // Character name - use client nickname if set, otherwise cleaned name
        String charName = cleanName(stack.getName().getString());
        String clientNick = WynnExtrasConfig.INSTANCE.clientNicknames.get(charId);
        if (clientNick != null && !clientNick.isEmpty()) {
            charName = clientNick;
        }
        charName = truncate(charName, 20);
        float textX = cx + px(iconAreaPx + 18);
        float textNameY = cy + px(20);
        ui.drawText(charName, textX, textNameY, CustomColor.fromHexString("FFFFFF"), 2.6f);

        // Class/level info
        if (!classInfo.isEmpty()) {
            ui.drawText(classInfo, textX, textNameY + px(14), accent, 2.2f);
        }
    }

    private void drawDraggedCard(DrawContext ctx, int mouseX, int mouseY) {
        List<ItemStack> stacks = getStacks();
        if (stacks == null || pressedVisIdx < 0 || pressedVisIdx >= visibleCardCount) return;
        int slotIdx = CHARACTER_SLOTS[visOrder[pressedVisIdx]];
        if (slotIdx >= stacks.size()) return;
        ItemStack stack = stacks.get(slotIdx);
        if (stack == null || stack.isEmpty()) return;

        float sf = (float) scaleFactor;
        float cx = mouseX * sf - cardLW / 2f;
        float cy = mouseY * sf - cardLH / 2f;
        drawCharCard(ctx, stack, visCharId[pressedVisIdx], cx, cy, true, false);
    }

    private void drawSettingsButtons(DrawContext ctx, List<ItemStack> stacks, int mouseX, int mouseY,
                                      float panelXPx, float settingsYPx, float panelWPx) {
        int[] slots = {SLOT_CANCEL_DELETION, SLOT_BACKUPS, SLOT_MUSIC, SLOT_AUTO_OPEN};
        float btnWPx = 100, btnHPx = 22, gapPx = 10;
        float totalW = slots.length * btnWPx + (slots.length - 1) * gapPx;
        float startXPx = panelXPx + (panelWPx - totalW) / 2f;

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] >= stacks.size()) continue;
            ItemStack stack = stacks.get(slots[i]);
            if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) continue;

            float bxPx = startXPx + i * (btnWPx + gapPx);
            float bx = px(bxPx), by = px(settingsYPx), bw = px(btnWPx), bh = px(btnHPx);
            boolean hovered = isInBounds(mouseX, mouseY, bx, by, bw, bh);
            if (hovered) {
                hoveredSettingSlot = slots[i];
                hoveredTooltip = getTooltipLines(stack);
            }
            ui.drawButton(bx, by, bw, bh, hovered);
            String label = truncate(cleanName(stack.getName().getString()), 14);
            ui.drawCenteredText(label, bx + bw / 2f, by + bh / 2f,
                    CustomColor.fromHexString("FFFFFF"), 1.7f);
        }
    }

    // ==================== CLASS EDIT ====================

    private void drawClassEdit(DrawContext ctx, int mouseX, int mouseY) {
        List<ItemStack> stacks = getStacks();
        if (stacks == null || stacks.isEmpty()) return;

        int[] editSlots = {SLOT_EDIT_NICKNAME, SLOT_FAVORITE, SLOT_CHANGE_SCENE, SLOT_RESKIN_CLASS, SLOT_EDIT_ICON};
        float btnWPx = 220, btnHPx = 36, gapPx = 10;
        float titleHPx = 36, marginPx = 20, backHPx = 28;

        int visCount = 0;
        for (int s : editSlots) {
            if (s < stacks.size()) {
                ItemStack st = stacks.get(s);
                if (st != null && !st.isEmpty() && st.getItem() != Items.AIR) visCount++;
            }
        }

        float contentHPx = visCount * btnHPx + (visCount - 1) * gapPx;
        float panelWPx = btnWPx + marginPx * 2;
        float panelHPx = titleHPx + contentHPx + backHPx + marginPx * 4;
        float panelXPx = (screenWidth - panelWPx) / 2f;
        float panelYPx = (screenHeight - panelHPx) / 2f;

        drawPanel(panelXPx, panelYPx, panelWPx, panelHPx);

        // Title
        float titleYPx = panelYPx + marginPx;
        ui.drawRect(px(panelXPx + marginPx), px(titleYPx) - px(2),
                px(panelWPx - marginPx * 2), px(titleHPx), CustomColor.fromHexString("2e251c"));
        ui.drawCenteredText("Edit Character",
                px(panelXPx + panelWPx / 2f), px(titleYPx + titleHPx / 2f),
                CustomColor.fromHexString("FFAA00"), 4.5f);

        float sepYPx = titleYPx + titleHPx + 4;
        ui.drawRect(px(panelXPx + marginPx + 10), px(sepYPx),
                px(panelWPx - (marginPx + 10) * 2), px(1), CustomColor.fromHexString("5d4736"));

        // Edit option buttons
        float btnStartXPx = panelXPx + marginPx;
        float btnStartYPx = sepYPx + 8;
        int idx = 0;
        for (int i = 0; i < editSlots.length; i++) {
            int slot = editSlots[i];
            if (slot >= stacks.size()) continue;
            ItemStack stack = stacks.get(slot);
            if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) continue;

            float bx = px(btnStartXPx), by = px(btnStartYPx + idx * (btnHPx + gapPx));
            float bw = px(btnWPx), bh = px(btnHPx);
            boolean hovered = isInBounds(mouseX, mouseY, bx, by, bw, bh);
            if (hovered) {
                hoveredEditOption = i;
                hoveredTooltip = getTooltipLines(stack);
            }

            ui.drawButton(bx, by, bw, bh, hovered);

            // Item icon
            ctx.getMatrices().pushMatrix();
            float sIX = ui.sx(bx + px(8));
            float sIY = ui.sy(by + px(2));
            ctx.getMatrices().translate(sIX, sIY);
            float iSc = (btnHPx - 4) / 16f;
            ctx.getMatrices().scale(iSc, iSc);
            ctx.drawItem(stack, 0, 0);
            ctx.getMatrices().popMatrix();

            String label = truncate(cleanName(stack.getName().getString()), 24);
            ui.drawCenteredText(label, bx + bw / 2f + px(14), by + bh / 2f,
                    CustomColor.fromHexString("FFFFFF"), 3f);
            idx++;
        }

        // Back button (clicks slot 0 = return to character selector)
        float backWPx = 80;
        float backXPx = panelXPx + (panelWPx - backWPx) / 2f;
        float backYPx = btnStartYPx + idx * (btnHPx + gapPx) + 8;
        backLX = px(backXPx); backLY = px(backYPx); backLW = px(backWPx); backLH = px(backHPx);
        hoveredBack = isInBounds(mouseX, mouseY, backLX, backLY, backLW, backLH);
        ui.drawButton(backLX, backLY, backLW, backLH, hoveredBack);
        ui.drawCenteredText("\u00A7c\u2190 Back", backLX + backLW / 2f, backLY + backLH / 2f,
                CustomColor.fromHexString("FF6666"), 2.5f);
    }

    // ==================== ICON EDIT ====================

    private void drawIconEdit(DrawContext ctx, int mouseX, int mouseY) {
        List<ItemStack> stacks = getStacks();
        if (stacks == null || stacks.isEmpty()) return;

        float colorBtnPx = 56, gapPx = 8;
        float subBtnPx = 50;
        float titleHPx = 36, marginPx = 16, backHPx = 28, previewPx = 72;

        // Count visible subcategories
        int subCount = 0;
        for (int s : ICON_SUB_SLOTS) {
            if (s < stacks.size() && stacks.get(s) != null && !stacks.get(s).isEmpty() && stacks.get(s).getItem() != Items.AIR)
                subCount++;
        }
        float subRowHPx = subCount > 0 ? subBtnPx + gapPx + 8 : 0;

        float gridWPx = ICON_COLS * colorBtnPx + (ICON_COLS - 1) * gapPx;
        float gridHPx = 3 * colorBtnPx + 2 * gapPx;

        float panelWPx = gridWPx + previewPx + marginPx * 3 + gapPx;
        float panelHPx = titleHPx + gridHPx + subRowHPx + backHPx + marginPx * 3 + 8;
        float panelXPx = (screenWidth - panelWPx) / 2f;
        float panelYPx = (screenHeight - panelHPx) / 2f;

        drawPanel(panelXPx, panelYPx, panelWPx, panelHPx);

        // Title
        float titleYPx = panelYPx + marginPx;
        ui.drawRect(px(panelXPx + marginPx), px(titleYPx) - px(2),
                px(panelWPx - marginPx * 2), px(titleHPx), CustomColor.fromHexString("2e251c"));
        ui.drawCenteredText("Choose Icon Color",
                px(panelXPx + panelWPx / 2f), px(titleYPx + titleHPx / 2f),
                CustomColor.fromHexString("FFAA00"), 4.5f);

        float sepYPx = titleYPx + titleHPx + 4;
        ui.drawRect(px(panelXPx + marginPx + 10), px(sepYPx),
                px(panelWPx - (marginPx + 10) * 2), px(1), CustomColor.fromHexString("5d4736"));

        // Color grid (3x3)
        float gridStartXPx = panelXPx + marginPx;
        float gridStartYPx = sepYPx + 8;

        for (int i = 0; i < ICON_COLOR_SLOTS.length; i++) {
            int slot = ICON_COLOR_SLOTS[i];
            if (slot >= stacks.size()) continue;
            ItemStack stack = stacks.get(slot);

            int col = i % ICON_COLS;
            int row = i / ICON_COLS;
            float bxPx = gridStartXPx + col * (colorBtnPx + gapPx);
            float byPx = gridStartYPx + row * (colorBtnPx + gapPx);
            float bx = px(bxPx), by = px(byPx), bw = px(colorBtnPx), bh = px(colorBtnPx);

            boolean empty = stack == null || stack.isEmpty() || stack.getItem() == Items.AIR;
            boolean hovered = !empty && isInBounds(mouseX, mouseY, bx, by, bw, bh);
            if (hovered) {
                hoveredIconColor = i;
                hoveredTooltip = getTooltipLines(stack);
            }

            ui.drawButton(bx, by, bw, bh, hovered);

            if (!empty) {
                // Item icon centered in button
                float iSc = (colorBtnPx - 22) / 16f;
                float itemRenderSize = 16 * iSc;
                ctx.getMatrices().pushMatrix();
                float sIX = ui.sx(bx + bw / 2f) - itemRenderSize / 2f;
                float sIY = ui.sy(by + bh / 2f) - itemRenderSize / 2f - 2;
                ctx.getMatrices().translate(sIX, sIY);
                ctx.getMatrices().scale(iSc, iSc);
                ctx.drawItem(stack, 0, 0);
                ctx.getMatrices().popMatrix();

                // Color label
                String name = i < ICON_COLOR_NAMES.length ? ICON_COLOR_NAMES[i] : cleanName(stack.getName().getString());
                ui.drawCenteredText(truncate(name, 10), bx + bw / 2f, by + bh - px(4),
                        CustomColor.fromHexString("CCCCCC"), 1.6f);
            }
        }

        // Preview (slot 43) - right side
        float previewXPx = gridStartXPx + ICON_COLS * (colorBtnPx + gapPx) + gapPx;
        float previewYPx = gridStartYPx;
        float pvx = px(previewXPx), pvy = px(previewYPx), pvw = px(previewPx), pvh = px(previewPx);
        ui.drawRect(pvx, pvy, pvw, pvh, CustomColor.fromHexString("2e251c"));

        if (ICON_PREVIEW_SLOT < stacks.size()) {
            ItemStack previewStack = stacks.get(ICON_PREVIEW_SLOT);
            if (previewStack != null && !previewStack.isEmpty() && previewStack.getItem() != Items.AIR) {
                ctx.getMatrices().pushMatrix();
                float sIX = ui.sx(pvx + pvw / 2f - px(20));
                float sIY = ui.sy(pvy + pvh / 2f - px(20));
                ctx.getMatrices().translate(sIX, sIY);
                float iSc = (previewPx - 16) / 16f;
                ctx.getMatrices().scale(iSc, iSc);
                ctx.drawItem(previewStack, 0, 0);
                ctx.getMatrices().popMatrix();
            }
        }
        ui.drawCenteredText("Preview", pvx + pvw / 2f, pvy + pvh + px(4),
                CustomColor.fromHexString("888888"), 2f);

        // Subcategory row
        if (subCount > 0) {
            float subStartYPx = gridStartYPx + 3 * (colorBtnPx + gapPx) + 4;
            int subIdx = 0;
            for (int i = 0; i < ICON_SUB_SLOTS.length; i++) {
                int slot = ICON_SUB_SLOTS[i];
                if (slot >= stacks.size()) continue;
                ItemStack stack = stacks.get(slot);
                if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) continue;

                float bxPx = gridStartXPx + subIdx * (subBtnPx + gapPx);
                float bx = px(bxPx), by = px(subStartYPx), bw = px(subBtnPx), bh = px(subBtnPx);

                boolean hovered = isInBounds(mouseX, mouseY, bx, by, bw, bh);
                if (hovered) {
                    hoveredIconSub = i;
                    hoveredTooltip = getTooltipLines(stack);
                }

                ui.drawButton(bx, by, bw, bh, hovered);

                // Item icon centered
                float iSc = (subBtnPx - 18) / 16f;
                float itemRenderSize = 16 * iSc;
                ctx.getMatrices().pushMatrix();
                float sIX = ui.sx(bx + bw / 2f) - itemRenderSize / 2f;
                float sIY = ui.sy(by + bh / 2f) - itemRenderSize / 2f - 2;
                ctx.getMatrices().translate(sIX, sIY);
                ctx.getMatrices().scale(iSc, iSc);
                ctx.drawItem(stack, 0, 0);
                ctx.getMatrices().popMatrix();

                // Label
                String label = truncate(cleanName(stack.getName().getString()), 8);
                ui.drawCenteredText(label, bx + bw / 2f, by + bh - px(3),
                        CustomColor.fromHexString("CCCCCC"), 1.4f);
                subIdx++;
            }
        }

        // Back button (slot 10 = back to edit character)
        float backWPx = 80;
        float backYOffset = gridHPx + subRowHPx + 8;
        float backXPx = panelXPx + (panelWPx - backWPx) / 2f;
        float backYPx = gridStartYPx + backYOffset;
        backLX = px(backXPx); backLY = px(backYPx); backLW = px(backWPx); backLH = px(backHPx);
        hoveredBack = isInBounds(mouseX, mouseY, backLX, backLY, backLW, backLH);
        ui.drawButton(backLX, backLY, backLW, backLH, hoveredBack);
        ui.drawCenteredText("\u00A7c\u2190 Back", backLX + backLW / 2f, backLY + backLH / 2f,
                CustomColor.fromHexString("FF6666"), 2.5f);
    }

    // ==================== SHARED RENDERING ====================

    private void drawPanel(float xPx, float yPx, float wPx, float hPx) {
        ui.drawRect(px(xPx) - px(2), px(yPx) - px(2), px(wPx) + px(4), px(hPx) + px(4),
                CustomColor.fromHexString("5d4736"));
        ui.drawRect(px(xPx), px(yPx), px(wPx), px(hPx),
                CustomColor.fromHexString("1a1410"));
    }

    // ==================== INPUT ====================

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        x /= matrixScale;
        y /= matrixScale;
        // If nickname input is active, consume all clicks (Escape/Enter to close)
        if (nicknameInputActive) return true;

        // Vanilla toggle
        if (mode == ScreenMode.CLASS_SELECTION && hoveredToggle) {
            vanillaMode = true;
            return true;
        }

        // Color/brown toggle
        if (mode == ScreenMode.CLASS_SELECTION && hoveredColorToggle) {
            WynnExtrasConfig.INSTANCE.classCardColoredAccents = !WynnExtrasConfig.INSTANCE.classCardColoredAccents;
            WynnExtrasConfig.save();
            return true;
        }

        // Back button
        if (hoveredBack) {
            if (mode == ScreenMode.CLASS_EDIT) {
                clickSlot(0, 0);
            } else if (mode == ScreenMode.ICON_EDIT) {
                clickSlot(ICON_BACK_SLOT, 0);
            }
            return true;
        }

        if (mode == ScreenMode.CLASS_SELECTION) {
            if (hoveredCharVisIdx >= 0) {
                pressedVisIdx = hoveredCharVisIdx;
                pressedButton = button;
                pressStartX = x;
                pressStartY = y;
                isDragging = false;
                return true;
            }
            if (hoveredSettingSlot >= 0) {
                clickSlot(hoveredSettingSlot, 0);
                return true;
            }
        } else if (mode == ScreenMode.CLASS_EDIT) {
            if (hoveredEditOption >= 0) {
                int[] editSlots = {SLOT_EDIT_NICKNAME, SLOT_FAVORITE, SLOT_CHANGE_SCENE, SLOT_RESKIN_CLASS, SLOT_EDIT_ICON};
                if (hoveredEditOption < editSlots.length) {
                    if (hoveredEditOption == 0) {
                        // Edit Nickname - open custom input
                        openNicknameInput(editingCharId);
                        return true;
                    }
                    clickSlot(editSlots[hoveredEditOption], 0);
                    return true;
                }
            }
        } else if (mode == ScreenMode.ICON_EDIT) {
            if (hoveredIconColor >= 0) {
                clickSlot(ICON_COLOR_SLOTS[hoveredIconColor], 0);
                return true;
            }
            if (hoveredIconSub >= 0) {
                clickSlot(ICON_SUB_SLOTS[hoveredIconSub], 0);
                return true;
            }
        }
        return true; // consume all clicks when overlay is active
    }

    /** Called from mixin on mouseDragged */
    public void onMouseDragged(double x, double y) {
        x /= matrixScale;
        y /= matrixScale;
        if (pressedVisIdx >= 0 && mode == ScreenMode.CLASS_SELECTION) {
            double dist = Math.sqrt(Math.pow(x - pressStartX, 2) + Math.pow(y - pressStartY, 2));
            if (dist > DRAG_THRESHOLD) {
                isDragging = true;
            }
        }
    }

    /** Called from mixin on mouseReleased */
    public void onMouseReleased(double x, double y, int button) {
        x /= matrixScale;
        y /= matrixScale;
        if (pressedVisIdx >= 0 && mode == ScreenMode.CLASS_SELECTION) {
            if (isDragging) {
                int targetVis = findVisualSlotAt(x, y);
                if (targetVis >= 0 && targetVis != pressedVisIdx) {
                    // Swap in the visual arrays
                    int tmpOrder = visOrder[pressedVisIdx];
                    visOrder[pressedVisIdx] = visOrder[targetVis];
                    visOrder[targetVis] = tmpOrder;
                    String tmpId = visCharId[pressedVisIdx];
                    visCharId[pressedVisIdx] = visCharId[targetVis];
                    visCharId[targetVis] = tmpId;
                    saveCardOrder();
                }
            } else {
                int slotIndex = CHARACTER_SLOTS[visOrder[pressedVisIdx]];
                // If right-click (edit), remember which character we're editing
                if (pressedButton == 1) {
                    editingCharId = visCharId[pressedVisIdx];
                }
                clickSlot(slotIndex, pressedButton);
            }
            pressedVisIdx = -1;
            isDragging = false;
        }
    }

    private int findVisualSlotAt(double mx, double my) {
        for (int vis = 0; vis < visibleCardCount; vis++) {
            if (isInBounds(mx, my, cardLX[vis], cardLY[vis], cardLW, cardLH)) return vis;
        }
        return -1;
    }

    // ==================== VANILLA MODE TOGGLE (static, used by mixin) ====================

    public static void renderVanillaToggleButton(DrawContext ctx, HandledScreen<?> screen) {
        if (!vanillaMode) return;
        String title = screen.getTitle().getString();
        if (!isClassSelectionScreen(title) && !isClassEditScreen(title) && !isIconEditScreen(title)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int bw = 80, bh = 20;
        int bx = sw - bw - 5, by = 5;

        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF666666);
        ctx.fill(bx, by, bx + bw, by + bh, 0xFF333333);
        int textW = mc.textRenderer.getWidth("Custom View");
        ctx.drawText(mc.textRenderer, "Custom View", bx + (bw - textW) / 2, by + (bh - 8) / 2, 0xFFFFFFFF, true);
    }

    public static boolean handleVanillaToggleClick(double mx, double my, HandledScreen<?> screen) {
        if (!vanillaMode) return false;
        String title = screen.getTitle().getString();
        if (!isClassSelectionScreen(title) && !isClassEditScreen(title) && !isIconEditScreen(title)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int bw = 80, bh = 20;
        int bx = sw - bw - 5, by = 5;
        if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
            vanillaMode = false;
            return true;
        }
        return false;
    }

    // ==================== NICKNAME INPUT ====================

    /** Called from BankOverlay onInput (KeyInputEvent). Returns true if consumed. */
    public static boolean handleKeyInput(KeyInputEvent event) {
        if (!nicknameInputActive) return false;
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return true;
        int key = event.getKey();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            nicknameInputActive = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirmNickname();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (nicknameCursor > 0) {
                nicknameText = nicknameText.substring(0, nicknameCursor - 1) + nicknameText.substring(nicknameCursor);
                nicknameCursor--;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (nicknameCursor < nicknameText.length()) {
                nicknameText = nicknameText.substring(0, nicknameCursor) + nicknameText.substring(nicknameCursor + 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            if (nicknameCursor > 0) nicknameCursor--;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            if (nicknameCursor < nicknameText.length()) nicknameCursor++;
            return true;
        }
        return true; // consume all keys when input active
    }

    /** Called from BankOverlay onChar (CharInputEvent). Returns true if consumed. */
    public static boolean handleCharInput(CharInputEvent event) {
        if (!nicknameInputActive) return false;
        char c = event.getCharacter();
        if (c < 32) return true; // ignore control chars
        if (nicknameText.length() < 30) {
            nicknameText = nicknameText.substring(0, nicknameCursor) + c + nicknameText.substring(nicknameCursor);
            nicknameCursor++;
        }
        return true;
    }

    private static void confirmNickname() {
        nicknameInputActive = false;
        String name = nicknameText.trim();
        if (name.isEmpty()) return;

        // Save client-side nickname keyed by character identifier
        WynnExtrasConfig.INSTANCE.clientNicknames.put(nicknameCharId, name);
        WynnExtrasConfig.save();

        if (nicknameHasRank) {
            // Click the nickname slot first to open chat input
            try {
                ContainerUtils.clickOnSlot(SLOT_EDIT_NICKNAME, McUtils.containerMenu().syncId,
                        0, McUtils.containerMenu().getStacks());
            } catch (Exception e) {}
            // Send the nickname in chat after a short delay
            TickScheduler.runAfterTicks(10, () -> {
                if (McUtils.player() != null) {
                    McUtils.player().networkHandler.sendChatMessage(name);
                }
            });
        }
    }

    private void openNicknameInput(String charId) {
        // Check if the player has the rank by examining the edit nickname slot
        List<ItemStack> stacks = getStacks();
        nicknameHasRank = false;
        if (stacks != null && SLOT_EDIT_NICKNAME < stacks.size()) {
            ItemStack nickStack = stacks.get(SLOT_EDIT_NICKNAME);
            if (nickStack != null && !nickStack.isEmpty()) {
                // If the item is not a barrier/red item, player has rank
                // Check tooltip for "rank" or similar text
                for (Text line : getTooltipLines(nickStack)) {
                    String s = line.getString().toLowerCase();
                    if (s.contains("champion") || s.contains("hero") || s.contains("vip") || s.contains("rename")) {
                        nicknameHasRank = true;
                        break;
                    }
                }
                // Also check if item is not a barrier (barriers = no rank)
                if (nickStack.getItem() != Items.BARRIER) {
                    nicknameHasRank = true;
                }
            }
        }
        nicknameCharId = charId;
        // Pre-fill with existing client nickname if any
        String existing = WynnExtrasConfig.INSTANCE.clientNicknames.get(charId);
        nicknameText = existing != null ? existing : "";
        nicknameCursor = nicknameText.length();
        nicknameInputActive = true;
    }

    private void drawNicknameInput(DrawContext ctx, int mouseX, int mouseY) {
        if (!nicknameInputActive) return;

        // Dim background
        ctx.fill(0, 0, screenWidth, screenHeight, 0x88000000);

        // Modal box
        float boxWPx = 280, boxHPx = 100;
        float boxXPx = (screenWidth - boxWPx) / 2f;
        float boxYPx = (screenHeight - boxHPx) / 2f;

        // Border
        ctx.fill((int)(boxXPx - 2), (int)(boxYPx - 2),
                (int)(boxXPx + boxWPx + 2), (int)(boxYPx + boxHPx + 2), 0xFF5d4736);
        // Background
        ctx.fill((int)boxXPx, (int)boxYPx,
                (int)(boxXPx + boxWPx), (int)(boxYPx + boxHPx), 0xFF1a1410);

        MinecraftClient mc = MinecraftClient.getInstance();

        // Title
        String title = nicknameHasRank ? "Set Nickname (Server + Client)" : "Set Nickname (Client-Side Only)";
        int titleW = mc.textRenderer.getWidth(title);
        ctx.drawText(mc.textRenderer, title,
                (int)(boxXPx + (boxWPx - titleW) / 2f), (int)(boxYPx + 8), 0xFFFFAA00, true);

        // Text input field
        float fieldX = boxXPx + 16, fieldY = boxYPx + 28;
        float fieldW = boxWPx - 32, fieldH = 20;
        ctx.fill((int)fieldX, (int)fieldY, (int)(fieldX + fieldW), (int)(fieldY + fieldH), 0xFF333333);
        ctx.fill((int)(fieldX + 1), (int)(fieldY + 1), (int)(fieldX + fieldW - 1), (int)(fieldY + fieldH - 1), 0xFF111111);

        // Text with cursor
        String displayText = nicknameText;
        String beforeCursor = nicknameText.substring(0, nicknameCursor);
        int cursorX = mc.textRenderer.getWidth(beforeCursor);
        ctx.drawText(mc.textRenderer, displayText, (int)(fieldX + 4), (int)(fieldY + 6), 0xFFFFFFFF, false);

        // Blinking cursor
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            ctx.fill((int)(fieldX + 4 + cursorX), (int)(fieldY + 4),
                    (int)(fieldX + 5 + cursorX), (int)(fieldY + fieldH - 4), 0xFFFFFFFF);
        }

        // Hint text
        String hint = "Enter to confirm  |  Escape to cancel";
        int hintW = mc.textRenderer.getWidth(hint);
        ctx.drawText(mc.textRenderer, hint,
                (int)(boxXPx + (boxWPx - hintW) / 2f), (int)(boxYPx + boxHPx - 18), 0xFF666666, false);

        if (!nicknameHasRank) {
            String note = "\u00A7eNote: This rename is only visible to you";
            int noteW = mc.textRenderer.getWidth(note);
            ctx.drawText(mc.textRenderer, note,
                    (int)(boxXPx + (boxWPx - noteW) / 2f), (int)(fieldY + fieldH + 6), 0xFFFFFF55, false);
        }
    }

    // ==================== CUSTOM BACKGROUND ====================

    private static void scanCustomBackgrounds() {
        try {
            Path folder = FabricLoader.getInstance().getConfigDir()
                    .resolve("wynnextras").resolve("customscreen");
            Files.createDirectories(folder);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, entry -> {
                String name = entry.getFileName().toString().toLowerCase();
                return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
            })) {
                for (Path entry : stream) {
                    loadBgTexture(entry.toAbsolutePath().toString());
                    break; // use first image found
                }
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to scan custom backgrounds: " + e.getMessage());
        }
    }

    private static void loadBgTexture(String path) {
        try {
            NativeImage image;
            try {
                // Try direct PNG loading first
                InputStream is = new FileInputStream(path);
                image = NativeImage.read(is);
                is.close();
            } catch (Exception e) {
                // Fall back to ImageIO for JPEG and other formats
                BufferedImage buffered = ImageIO.read(new File(path));
                if (buffered == null) return;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(buffered, "png", baos);
                image = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
            }
            bgImgW = image.getWidth();
            bgImgH = image.getHeight();
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "wynnextras_class_bg", image);
            bgTexture = Identifier.of("wynnextras", "class_bg_" + System.currentTimeMillis());
            MinecraftClient.getInstance().getTextureManager().registerTexture(bgTexture, texture);
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load custom background: " + e.getMessage());
        }
    }

    public static void invalidateBackground() {
        if (bgTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(bgTexture);
            bgTexture = null;
        }
        bgScanned = false;
    }

    // ==================== UTILITY ====================

    private void clickSlot(int slotIndex, int mouseButton) {
        try {
            ContainerUtils.clickOnSlot(slotIndex, McUtils.containerMenu().syncId,
                    mouseButton, McUtils.containerMenu().getStacks());
        } catch (Exception e) {}
    }

    /**
     * Auto-select a character if coming from a cross-class bank page click.
     * Matches by name + level, or by level alone as fallback. Only clicks if exactly one match.
     */
    private void tryAutoSelectCharacter(List<CharIdentity> charDataList, List<Integer> charSlotIndices, List<ItemStack> stacks) {
        String targetName = BankOverlay2.getTargetCharacterNameForClassMenu();
        int targetLevel = BankOverlay2.getTargetCharacterLevelForClassMenu();

        // Clear the target so it doesn't trigger again
        BankOverlay2.clearTargetCharacterForClassMenu();

        if (targetName == null && targetLevel <= 0) return;

        // Find matching visible cards
        int matchCount = 0;
        int matchVisIdx = -1;

        for (int vis = 0; vis < visibleCardCount; vis++) {
            int arrayIdx = visOrder[vis]; // index into CHARACTER_SLOTS (0-14)
            if (arrayIdx >= CHARACTER_SLOTS.length) continue;
            int origIdx = -1;
            for (int j = 0; j < charSlotIndices.size(); j++) {
                if (charSlotIndices.get(j) == arrayIdx) {
                    origIdx = j;
                    break;
                }
            }
            if (origIdx < 0 || origIdx >= charDataList.size()) continue;

            CharIdentity card = charDataList.get(origIdx);

            boolean match = false;
            if (targetName != null && !targetName.isEmpty()) {
                // Name + level matching
                boolean nameMatch = targetName.equalsIgnoreCase(card.name)
                        || targetName.equalsIgnoreCase(card.classType);
                boolean levelMatch = targetLevel <= 0 || card.level <= 0
                        || Math.abs(card.level - targetLevel) <= 5;
                match = nameMatch && levelMatch;
            }

            if (match) {
                matchCount++;
                matchVisIdx = vis;
            }
        }

        // Only auto-click if exactly one match (100% identifiable)
        if (matchCount == 1 && matchVisIdx >= 0) {
            int slotIdx = CHARACTER_SLOTS[visOrder[matchVisIdx]];
            // Delay the click slightly to let the screen finish rendering
            TickScheduler.runAfterTicks(2, () -> {
                clickSlot(slotIdx, 0);
            });
        }
    }

    private List<ItemStack> getStacks() {
        try { return McUtils.containerMenu().getStacks(); }
        catch (Exception e) { return null; }
    }

    private boolean isInBounds(double mx, double my, float lx, float ly, float lw, float lh) {
        float sx = ui.sx(lx), sy = ui.sy(ly);
        int sw = ui.sw(lw), sh = ui.sh(lh);
        return mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh;
    }

    private List<Text> getTooltipLines(ItemStack stack) {
        return stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
    }

    /** Strip formatting codes, brackets, and Wynncraft resource pack glyphs */
    private String cleanName(String raw) {
        String stripped = raw.replaceAll("\u00A7[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("^\\[|\\]$", "");
        // Strip surrogate pairs and Private Use Area characters (Wynncraft glyphs)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isSurrogate(c)) continue;
            if (c >= 0xE000 && c <= 0xF8FF) continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private String truncate(String text, int maxLen) {
        if (text.length() > maxLen) return text.substring(0, maxLen - 2) + "..";
        return text;
    }

    private String extractClassInfo(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String str = line.getString().replaceAll("\u00A7[0-9a-fk-or]", "").trim();
            if (str.contains("Class")) return str;
        }
        return "";
    }

    private CustomColor getClassColor(String classInfo) {
        if (!WynnExtrasConfig.INSTANCE.classCardColoredAccents) return CustomColor.fromHexString("5d4736");
        String l = classInfo.toLowerCase();
        if (l.contains("warrior") || l.contains("knight")) return CustomColor.fromHexString("CC4444");
        if (l.contains("mage") || l.contains("dark wizard")) return CustomColor.fromHexString("55BBFF");
        if (l.contains("assassin") || l.contains("ninja")) return CustomColor.fromHexString("FF55FF");
        if (l.contains("archer") || l.contains("hunter")) return CustomColor.fromHexString("55FF55");
        if (l.contains("shaman") || l.contains("skyseer")) return CustomColor.fromHexString("FFFF55");
        return CustomColor.fromHexString("5d4736");
    }
}
