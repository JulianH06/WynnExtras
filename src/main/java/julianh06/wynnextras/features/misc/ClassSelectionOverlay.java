package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.core.WynnExtras;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.colors.WynncraftShaderColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.Widget;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.data.CrossClassBankSearch;
import julianh06.wynnextras.features.misc.ClassSelectionData.CharIdentity;
import julianh06.wynnextras.utils.TickScheduler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassSelectionOverlay extends WEHandledScreen {
    @Override protected double getTargetScaleFactor() { return 4.0; }
    @Override protected int getMinScreenWidth() { return 800; }
    @Override protected int getMinScreenHeight() { return Math.round(getClassSelectionPanelHeightPx(5) + 72); }

    public static final String CLASS_SELECTION_TITLE = "\uDAFF\uDFD5\uE01F";

    public enum ScreenMode { CLASS_SELECTION }

    private ScreenMode mode;
    private HandledScreen<?> screen;

    // ==================== CLASS SELECTION SLOTS ====================
    private static final int[] CHARACTER_SLOTS = {9, 10, 11, 18, 19, 20, 27, 28, 29, 36, 37, 38, 45, 46, 47};
    private static final int COLS = 3;

    private static final int SLOT_CANCEL_DELETION = 7;
    private static final int SLOT_BACKUPS = 25;
    private static final int SLOT_MUSIC = 51;
    private static final int SLOT_AUTO_OPEN = 53;
    private static final float CARD_W_PX = 247;
    private static final float CARD_GAP_X_PX = 12;
    private static final float CARD_GAP_Y_PX = 7;
    private static final float TITLE_H_PX = 30;
    private static final float SETTINGS_H_PX = 22;
    private static final float PANEL_MARGIN_PX = 9;

    // Hover state
    private int hoveredCharVisIdx = -1;
    private int hoveredSettingSlot = -1;
    private List<Text> hoveredTooltip = new ArrayList<>();

    // Drag state
    private int pressedVisIdx = -1;
    private int pressedButton = 0;
    private double pressStartX, pressStartY;
    private boolean isDragging = false;
    private static final double DRAG_THRESHOLD = 5.0;
    private static final int MIN_FUZZY_MATCH_SCORE = 35;
    private static final int AMBIGUITY_SCORE_MARGIN = 6;
    private static final int MAX_FUZZY_CANDIDATES_PER_CHARACTER = 8;
    private static final int STABLE_MATCH_SCORE = 1000;

    // Card layout cache (logical coords)
    private float[] cardLX = new float[15];
    private float[] cardLY = new float[15];
    private float cardLW, cardLH;
    private int visibleCardCount = 0;
    // visOrder[i] = the CHARACTER_SLOTS array index for the i-th visible card
    private int[] visOrder = new int[15];
    // visCharId[i] = the UUID for the i-th visible card
    private String[] visCharId = new String[15];
    private final Map<String, String> lastHeldWeaponDetailCache = new HashMap<>();
    // Only run identity matching once per screen open
    private boolean identityMatched = false;

    // Vanilla toggle
    public static boolean vanillaMode = false;
    private static final int CLASS_OVERLAY_TOGGLE_W = 100;
    private static final int CLASS_OVERLAY_TOGGLE_H = 15;
    private static final int CLASS_OVERLAY_TOGGLE_MARGIN = 2;
    private static final float CLASS_OVERLAY_TOGGLE_TEXT_SCALE = 0.8f;
    private static final ClassOverlayToggleWidget VANILLA_TOGGLE_WIDGET = new ClassOverlayToggleWidget();

    // Toggle button bounds (logical)
    private final ClassOverlayToggleWidget overlayToggleWidget = new ClassOverlayToggleWidget();

    // Custom background from config/wynnextras/customscreen/
    private static Identifier bgTexture = null;
    private static boolean bgScanned = false;
    private static int bgImgW = 1, bgImgH = 1;

    private static boolean descriptionInputActive = false;
    private static String descriptionText = "";
    private static String descriptionCharId = "";
    private static final int DESCRIPTION_MAX_LENGTH = 40;
    private static TextInputWidget descriptionInputWidget = null;
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern CONTENT_COUNT_PATTERN = Pattern.compile("(\\d+)\\s+of\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static float getClassSelectionCardHeightPx() {
        WynnExtrasConfig.ClassSelectionContentProgressStyle progressStyle = getContentProgressStyle();
        int lineCount = getVisibleConfiguredLineCount(progressStyle);

        int detailStart = 24;
        int detailSpacing = 14;
        float descY = Math.max(66, detailStart + lineCount * detailSpacing + 6);
        if (progressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.PROGRESS_BAR) {
            float progressLabelY = Math.max(68, detailStart + lineCount * detailSpacing + 20);
            return Math.max(90, progressLabelY + 39);
        }
        return Math.max(72, descY + 9);
    }

    private static int getVisibleConfiguredLineCount(WynnExtrasConfig.ClassSelectionContentProgressStyle progressStyle) {
        List<String> activeLines = WynnExtrasConfig.INSTANCE.classSelectionActiveLines;
        if (activeLines == null) return WynnExtrasConfig.CLASS_SELECTION_BASE_LINE_IDS.size();

        int count = 0;
        for (String lineId : activeLines) {
            if (WynnExtrasConfig.CLASS_SELECTION_LINE_CONTENT_PROGRESS.equals(lineId)
                    && progressStyle != WynnExtrasConfig.ClassSelectionContentProgressStyle.LINE) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static float getClassSelectionPanelHeightPx(int rows) {
        float cardHPx = getClassSelectionCardHeightPx();
        float gridHPx = rows * cardHPx + Math.max(0, rows - 1) * CARD_GAP_Y_PX;
        return TITLE_H_PX + gridHPx + SETTINGS_H_PX + PANEL_MARGIN_PX * 3 + 16;
    }

    private static WynnExtrasConfig.ClassSelectionContentProgressStyle getContentProgressStyle() {
        WynnExtrasConfig.ClassSelectionContentProgressStyle style = WynnExtrasConfig.INSTANCE.classSelectionContentProgressStyle;
        return style == null ? WynnExtrasConfig.ClassSelectionContentProgressStyle.PROGRESS_BAR : style;
    }

    public ClassSelectionOverlay(HandledScreen<?> screen, ScreenMode mode) {
        this.screen = screen;
        this.mode = mode;
    }

    public ScreenMode getMode() { return mode; }

    // ==================== CHARACTER IDENTIFICATION ====================

    /** Extract current character data from an ItemStack in the class selection screen */
    private static boolean charDataDebugLogged = false;
    private CharIdentity extractCharData(ItemStack stack, int characterSlotIndex) {
        CharIdentity data = new CharIdentity();
        data.stableId = extractStableCharacterId(stack);
        data.name = cleanName(stack.getName().getString());
        data.classType = extractClassName(stack);
        data.color = extractPotionColor(stack);
        data.timePlayed = extractTimePlayed(stack);
        data.level = extractLevel(stack);
        data.xpPercent = extractXpPercent(stack);
        data.slotId = createSlotCharacterId(characterSlotIndex);
        data.fallbackId = createFallbackCharacterId(data, characterSlotIndex);
        return data;
    }

    private String createSlotCharacterId(int characterSlotIndex) {
        return "slot-" + characterSlotIndex + "-" + Integer.toHexString(Objects.hash(getCurrentPlayerKey(), characterSlotIndex));
    }

    private String createFallbackCharacterId(CharIdentity data, int characterSlotIndex) {
        String nameKey = safeString(data.name).toLowerCase(Locale.ROOT);
        String classKey = safeString(data.classType).toLowerCase(Locale.ROOT);
        int detailHash = Objects.hash(createSlotCharacterId(characterSlotIndex), nameKey, classKey, data.color);
        return "detail-" + characterSlotIndex + "-" + Integer.toHexString(detailHash);
    }

    private String extractStableCharacterId(ItemStack stack) {
        try {
            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null || customData.isEmpty()) return "";
            return findStableCharacterId(customData.copyNbt(), "");
        } catch (Exception e) {
            return "";
        }
    }

    private String findStableCharacterId(NbtCompound compound, String path) {
        for (Map.Entry<String, NbtElement> entry : compound.entrySet()) {
            String key = entry.getKey();
            NbtElement value = entry.getValue();
            String lowerKey = key.toLowerCase(Locale.ROOT);
            String lowerPath = path.toLowerCase(Locale.ROOT);

            if (value instanceof NbtCompound nested) {
                String nestedResult = findStableCharacterId(nested, path + "." + key);
                if (!nestedResult.isEmpty()) return nestedResult;
                continue;
            }

            if (!isLikelyCharacterIdKey(lowerKey, lowerPath)) continue;
            String candidate = value.asString().orElse("").trim();
            if (isLikelyStableCharacterId(candidate)) return candidate;
        }
        return "";
    }

    private boolean isLikelyCharacterIdKey(String key, String path) {
        String combined = path + "." + key;
        if (combined.contains("player")) return false;
        return key.equals("characterid")
                || key.equals("character_id")
                || key.equals("characteruuid")
                || key.equals("character_uuid")
                || key.equals("classid")
                || key.equals("class_id")
                || key.equals("classuuid")
                || key.equals("class_uuid")
                || key.equals("profileid")
                || key.equals("profile_id")
                || key.equals("profileuuid")
                || key.equals("profile_uuid")
                || (key.equals("id") && (path.contains("character") || path.contains("class") || path.contains("profile")))
                || (key.equals("uuid") && (path.contains("character") || path.contains("class") || path.contains("profile")));
    }

    private boolean isLikelyStableCharacterId(String value) {
        if (value.length() < 8 || value.length() > 80) return false;
        return value.matches("[A-Za-z0-9_:-]+");
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
        String storedStableId = safeString(stored.stableId);
        String currentStableId = safeString(current.stableId);
        if (!storedStableId.isEmpty() && storedStableId.equals(currentStableId)) return STABLE_MATCH_SCORE;

        String storedClass = safeString(stored.classType);
        String currentClass = safeString(current.classType);
        boolean canCompareClass = !storedClass.isEmpty() && !currentClass.isEmpty();
        if (canCompareClass && !storedClass.equals(currentClass)) return 0;

        int score = canCompareClass ? 20 : 0;
        String storedName = safeString(stored.name);
        String currentName = safeString(current.name);
        if (!storedName.isEmpty() && storedName.equals(currentName)) score += 25;
        if (stored.color != 0 && stored.color == current.color) score += 6;
        // Time played: within 24h = strong, within 72h = weaker
        double timeDiff = Math.abs(stored.timePlayed - current.timePlayed);
        if (stored.timePlayed > 0 && current.timePlayed > 0) {
            if (timeDiff <= 1.0) score += 14;
            else if (timeDiff <= 24.0) score += 10;
            else if (timeDiff <= 72.0) score += 5;
        }
        // Level: exact or close (accounts for leveling up between visits)
        if (stored.level > 0 && current.level > 0) {
            int levelDiff = current.level - stored.level; // should be >= 0 (leveled up)
            if (levelDiff == 0) score += 10;
            else if (levelDiff > 0 && levelDiff <= 10) score += 8;
            else if (Math.abs(levelDiff) <= 2) score += 4; // small backward = rounding
        }
        if (stored.xpPercent > 0 && current.xpPercent > 0) {
            int xpDiff = Math.abs(stored.xpPercent - current.xpPercent);
            if (xpDiff == 0) score += 3;
            else if (xpDiff <= 5) score += 1;
        }

        return score >= MIN_FUZZY_MATCH_SCORE ? score : 0;
    }

    /** Match current characters to stored identities, returning UUID for each.
     *  Uses stable ids when available, then an unambiguous optimal fuzzy assignment.
     *  Updates stored identities only for accepted matches and new characters. */
    private String[] matchCharacters(List<CharIdentity> currentChars) {
        Map<String, CharIdentity> stored = ClassSelectionData.getCharIdentities();
        if (stored == null) {
            stored = new HashMap<>();
        }

        String[] uuids = new String[currentChars.size()];
        boolean[] currentUsed = new boolean[currentChars.size()];
        Set<String> storedUsed = new HashSet<>();
        Set<String> assignedUuids = new HashSet<>();
        boolean configChanged = false;

        Map<String, String> stableIdToUuid = new HashMap<>();
        Set<String> duplicateStableIds = new HashSet<>();
        for (Map.Entry<String, CharIdentity> entry : stored.entrySet()) {
            String stableId = safeString(entry.getValue().stableId);
            if (stableId.isEmpty()) continue;
            if (stableIdToUuid.containsKey(stableId)) {
                duplicateStableIds.add(stableId);
            } else {
                stableIdToUuid.put(stableId, entry.getKey());
            }
        }
        for (String duplicate : duplicateStableIds) {
            stableIdToUuid.remove(duplicate);
        }

        for (int i = 0; i < currentChars.size(); i++) {
            String stableId = safeString(currentChars.get(i).stableId);
            if (stableId.isEmpty()) continue;
            String uuid = stableIdToUuid.get(stableId);
            if (uuid == null || storedUsed.contains(uuid)) continue;
            uuids[i] = uuid;
            currentUsed[i] = true;
            storedUsed.add(uuid);
            assignedUuids.add(uuid);
        }

        List<Integer> fuzzyCurrentIndices = new ArrayList<>();
        for (int i = 0; i < currentChars.size(); i++) {
            if (!currentUsed[i]) fuzzyCurrentIndices.add(i);
        }

        List<String> fuzzyStoredUuids = new ArrayList<>();
        for (String uuid : stored.keySet()) {
            if (!storedUsed.contains(uuid)) fuzzyStoredUuids.add(uuid);
        }

        int[][] fuzzyScores = buildFuzzyScoreMatrix(currentChars, fuzzyCurrentIndices, fuzzyStoredUuids, stored);
        int[] fuzzyAssignments = findBestFuzzyAssignment(fuzzyCurrentIndices, fuzzyStoredUuids, fuzzyScores);
        for (int localCurrent = 0; localCurrent < fuzzyCurrentIndices.size(); localCurrent++) {
            int storedIdx = fuzzyAssignments[localCurrent];
            if (storedIdx < 0) continue;
            int currentIdx = fuzzyCurrentIndices.get(localCurrent);
            String uuid = fuzzyStoredUuids.get(storedIdx);
            int score = fuzzyScores[localCurrent][storedIdx];
            if (isAmbiguousFuzzyMatch(localCurrent, storedIdx, fuzzyScores)) {
                WynnExtras.LOGGER.info("[WynnExtras] Skipping ambiguous class identity match for "
                        + describeChar(currentChars.get(currentIdx)) + " (score " + score + ")");
                continue;
            }

            uuids[currentIdx] = uuid;
            currentUsed[currentIdx] = true;
            storedUsed.add(uuid);
            assignedUuids.add(uuid);
        }

        Map<String, String> slotIdToUuid = buildUniqueIdentityIndex(stored, true, storedUsed);
        for (int i = 0; i < currentChars.size(); i++) {
            if (currentUsed[i]) continue;
            String slotId = safeString(currentChars.get(i).slotId);
            if (slotId.isEmpty()) continue;
            String uuid = slotIdToUuid.get(slotId);
            if (uuid == null || storedUsed.contains(uuid)) continue;
            uuids[i] = uuid;
            currentUsed[i] = true;
            storedUsed.add(uuid);
            assignedUuids.add(uuid);
        }

        // Assign deterministic ids for unmatched characters so the saved order remains stable.
        for (int i = 0; i < currentChars.size(); i++) {
            if (!currentUsed[i]) {
                String newUuid = createDeterministicCharUuid(currentChars.get(i), assignedUuids);
                uuids[i] = newUuid;
                assignedUuids.add(newUuid);
            }
        }

        // Update stored identities with fresh data only after matching is decided.
        for (int i = 0; i < currentChars.size(); i++) {
            CharIdentity fresh = currentChars.get(i);
            fresh.uuid = uuids[i];
            if (!sameIdentity(stored.get(uuids[i]), fresh)) {
                stored.put(uuids[i], fresh);
                configChanged = true;
            }
        }
        if (configChanged) {
            ClassSelectionData.saveCharIdentities();
        }

        return uuids;
    }

    private Map<String, String> buildUniqueIdentityIndex(Map<String, CharIdentity> stored, boolean useSlotId, Set<String> excludedUuids) {
        Map<String, String> result = new HashMap<>();
        Set<String> duplicates = new HashSet<>();
        for (Map.Entry<String, CharIdentity> entry : stored.entrySet()) {
            if (excludedUuids.contains(entry.getKey())) continue;
            String key = useSlotId ? safeString(entry.getValue().slotId) : safeString(entry.getValue().stableId);
            if (useSlotId && key.isEmpty() && safeString(entry.getValue().fallbackId).startsWith("slot-")) {
                key = safeString(entry.getValue().fallbackId);
            }
            if (key.isEmpty()) continue;
            if (result.containsKey(key)) {
                duplicates.add(key);
            } else {
                result.put(key, entry.getKey());
            }
        }
        for (String duplicate : duplicates) {
            result.remove(duplicate);
        }
        return result;
    }

    private int[][] buildFuzzyScoreMatrix(List<CharIdentity> currentChars, List<Integer> currentIndices,
                                          List<String> storedUuids, Map<String, CharIdentity> stored) {
        int[][] scores = new int[currentIndices.size()][storedUuids.size()];
        for (int i = 0; i < currentIndices.size(); i++) {
            CharIdentity current = currentChars.get(currentIndices.get(i));
            for (int j = 0; j < storedUuids.size(); j++) {
                scores[i][j] = matchScore(stored.get(storedUuids.get(j)), current);
            }
        }
        return scores;
    }

    private int[] findBestFuzzyAssignment(List<Integer> currentIndices, List<String> storedUuids, int[][] scores) {
        FuzzySearch search = new FuzzySearch(currentIndices.size(), storedUuids.size(), scores);
        search.run();
        return search.bestAssignment;
    }

    private boolean isAmbiguousFuzzyMatch(int currentIdx, int storedIdx, int[][] scores) {
        int score = scores[currentIdx][storedIdx];
        if (score <= 0) return true;

        for (int otherStored = 0; otherStored < scores[currentIdx].length; otherStored++) {
            if (otherStored == storedIdx) continue;
            int otherScore = scores[currentIdx][otherStored];
            if (otherScore > 0 && score - otherScore <= AMBIGUITY_SCORE_MARGIN) return true;
        }

        for (int otherCurrent = 0; otherCurrent < scores.length; otherCurrent++) {
            if (otherCurrent == currentIdx) continue;
            int otherScore = scores[otherCurrent][storedIdx];
            if (otherScore > 0 && score - otherScore <= AMBIGUITY_SCORE_MARGIN) return true;
        }

        return false;
    }

    private String createDeterministicCharUuid(CharIdentity data, Set<String> usedUuids) {
        String base = safeString(data.stableId);
        if (!base.isEmpty()) {
            base = "stable-" + Integer.toHexString(base.hashCode());
        } else {
            base = safeString(data.fallbackId);
        }
        if (base.isEmpty()) {
            base = "char-" + Integer.toHexString(Objects.hash(
                    getCurrentPlayerKey(),
                    safeString(data.name).toLowerCase(Locale.ROOT),
                    safeString(data.classType).toLowerCase(Locale.ROOT),
                    data.color
            ));
        }

        String uuid = base;
        int suffix = 2;
        while (usedUuids.contains(uuid)) {
            uuid = base + "-" + suffix;
            suffix++;
        }
        return uuid;
    }

    private boolean sameIdentity(CharIdentity a, CharIdentity b) {
        if (a == null || b == null) return false;
        return Objects.equals(safeString(a.uuid), safeString(b.uuid))
                && Objects.equals(safeString(a.stableId), safeString(b.stableId))
                && Objects.equals(safeString(a.fallbackId), safeString(b.fallbackId))
                && Objects.equals(safeString(a.slotId), safeString(b.slotId))
                && Objects.equals(safeString(a.name), safeString(b.name))
                && Objects.equals(safeString(a.classType), safeString(b.classType))
                && a.color == b.color
                && Double.compare(a.timePlayed, b.timePlayed) == 0
                && a.level == b.level
                && a.xpPercent == b.xpPercent;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String getCurrentPlayerKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return "";
        return client.player.getUuidAsString();
    }

    private String describeChar(CharIdentity data) {
        String name = safeString(data.name);
        String classType = safeString(data.classType);
        if (!name.isEmpty() && !classType.isEmpty()) return name + "/" + classType;
        if (!name.isEmpty()) return name;
        if (!classType.isEmpty()) return classType;
        return "unknown";
    }

    private class FuzzySearch {
        private final int currentCount;
        private final int storedCount;
        private final int[][] scores;
        private final List<Integer>[] candidates;
        private final List<Integer> searchOrder = new ArrayList<>();
        private final boolean[] usedStored;
        private final int[] workingAssignment;
        private final int[] bestAssignment;
        private final int[] maxRemainingScore;
        private int bestScore = -1;
        private int bestMatches = -1;

        @SuppressWarnings("unchecked")
        private FuzzySearch(int currentCount, int storedCount, int[][] scores) {
            this.currentCount = currentCount;
            this.storedCount = storedCount;
            this.scores = scores;
            this.candidates = new List[currentCount];
            this.usedStored = new boolean[storedCount];
            this.workingAssignment = new int[currentCount];
            this.bestAssignment = new int[currentCount];
            this.maxRemainingScore = new int[currentCount + 1];
            Arrays.fill(workingAssignment, -1);
            Arrays.fill(bestAssignment, -1);
        }

        private void run() {
            for (int currentIdx = 0; currentIdx < currentCount; currentIdx++) {
                List<Integer> currentCandidates = new ArrayList<>();
                for (int storedIdx = 0; storedIdx < storedCount; storedIdx++) {
                    if (scores[currentIdx][storedIdx] > 0) currentCandidates.add(storedIdx);
                }
                final int sortCurrentIdx = currentIdx;
                currentCandidates.sort((a, b) -> Integer.compare(scores[sortCurrentIdx][b], scores[sortCurrentIdx][a]));
                if (currentCandidates.size() > MAX_FUZZY_CANDIDATES_PER_CHARACTER) {
                    currentCandidates = new ArrayList<>(currentCandidates.subList(0, MAX_FUZZY_CANDIDATES_PER_CHARACTER));
                }
                candidates[currentIdx] = currentCandidates;
                searchOrder.add(currentIdx);
            }

            searchOrder.sort(Comparator
                    .comparingInt((Integer idx) -> candidates[idx].size())
                    .thenComparingInt(idx -> idx));

            for (int pos = currentCount - 1; pos >= 0; pos--) {
                int currentIdx = searchOrder.get(pos);
                int bestCandidateScore = candidates[currentIdx].isEmpty()
                        ? 0
                        : scores[currentIdx][candidates[currentIdx].get(0)];
                maxRemainingScore[pos] = maxRemainingScore[pos + 1] + bestCandidateScore;
            }

            search(0, 0, 0);
        }

        private void search(int pos, int score, int matches) {
            if (score + maxRemainingScore[pos] < bestScore) return;

            if (pos >= currentCount) {
                if (score > bestScore || (score == bestScore && matches > bestMatches)) {
                    bestScore = score;
                    bestMatches = matches;
                    System.arraycopy(workingAssignment, 0, bestAssignment, 0, workingAssignment.length);
                }
                return;
            }

            int currentIdx = searchOrder.get(pos);
            for (int storedIdx : candidates[currentIdx]) {
                if (usedStored[storedIdx]) continue;
                usedStored[storedIdx] = true;
                workingAssignment[currentIdx] = storedIdx;
                search(pos + 1, score + scores[currentIdx][storedIdx], matches + 1);
                workingAssignment[currentIdx] = -1;
                usedStored[storedIdx] = false;
            }
            search(pos + 1, score, matches);
        }
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
        ClassSelectionData.setClassCardOrder(order);
    }

    public static boolean isClassSelectionScreen(String title) { return CLASS_SELECTION_TITLE.equals(title); }

    /** Convert desired screen pixels to logical UIUtils coordinates */
    private float px(float screenPx) { return screenPx * (float) scaleFactor; }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WynnExtrasConfig.INSTANCE.classSelectionBackgroundEnabled) {
            ctx.fillGradient(0, 0, screenWidth, screenHeight, 0xC0101010, 0xD0101010);
            return;
        }

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
        hoveredTooltip = new ArrayList<>();

        ui.drawCenteredText(WynnExtras.addWynnExtrasPrefix("§6Class selection overlay"),
                px(screenWidth / 2f), px(12), 1.25f * ui.getScaleFactorF());

        drawClassSelection(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (isDragging && pressedVisIdx >= 0 && mode == ScreenMode.CLASS_SELECTION) {
            drawDraggedCard(ctx, mouseX, mouseY);
        }
        if (!hoveredTooltip.isEmpty() && !isTextInputActive()) {
            // ctx.drawTooltip expects GUI-scale coordinates and clamps against screen.width/height.
            // mouseX/mouseY here are in logical space (divided by matrixScale), so undo the
            // matrix transform before calling to prevent boundary clamping going wrong at high GUI scales.
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale((float)(1.0 / matrixScale), (float)(1.0 / matrixScale));
            ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(),
                    (int)(mouseX * matrixScale), (int)(mouseY * matrixScale));
            ctx.getMatrices().popMatrix();
        }
        drawDescriptionInput(ctx, mouseX, mouseY);
    }

    // ==================== CLASS SELECTION ====================

    private void drawClassSelection(DrawContext ctx, int mouseX, int mouseY) {
        List<ItemStack> stacks = getStacks();
        if (stacks == null || stacks.isEmpty()) return;

        float cardWPx = CARD_W_PX, cardHPx = getClassSelectionCardHeightPx(), gapXPx = CARD_GAP_X_PX, gapYPx = CARD_GAP_Y_PX;
        float titleHPx = TITLE_H_PX, settingsHPx = SETTINGS_H_PX, marginPx = PANEL_MARGIN_PX;

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
                charDataList.add(extractCharData(stack, i));
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
                List<String> savedOrder = ClassSelectionData.getClassCardOrder();
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
        float panelHPx = titleHPx + gridHPx + settingsHPx + marginPx * 3 + 16;
        float panelXPx = (screenWidth - panelWPx) / 2f;
        float panelYPx = (screenHeight - panelHPx) / 2f;

        drawPanel(panelXPx, panelYPx, panelWPx, panelHPx);

        // Title bar
        float titleYPx = panelYPx + marginPx;
        ui.drawRect(px(panelXPx + marginPx), px(titleYPx) - px(2),
                px(panelWPx - marginPx * 2), px(titleHPx), CustomColor.fromHexString("2e251c"));
        drawOverlayCenteredText("Select Your Character",
                px(panelXPx + panelWPx / 2f), px(titleYPx + titleHPx / 2f),
                CustomColor.fromHexString("FFAA00"), 4.5f);

        if (!WynnExtrasConfig.INSTANCE.hideClassSelectionQuickToggleButton) {
            layoutOverlayToggleWidget(overlayToggleWidget);
            overlayToggleWidget.draw(ctx, mouseX, mouseY, 0, ui);
        }

        // Separator
        float sepYPx = titleYPx + titleHPx + 2;
        ui.drawRect(px(panelXPx + marginPx + 10), px(sepYPx),
                px(panelWPx - (marginPx + 10) * 2), px(1), CustomColor.fromHexString("5d4736"));

        // Character cards
        float gridStartXPx = panelXPx + marginPx;
        float gridStartYPx = sepYPx + 6;
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
        drawOverlayCenteredText("\u00A77Left Click: Play  |  Right Click: Edit  |  Shift Right/Middle Click: Edit description  |  Drag: Rearrange",
                px(panelXPx + panelWPx / 2f), px(settingsYPx + settingsHPx + 5),
                CustomColor.fromHexString("666666"), 2.5f);
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
        Text gamemodeIcons = extractGamemodeIcons(stack);
        ContentProgress progress = extractContentProgress(stack);
        WynnExtrasConfig.ClassSelectionContentProgressStyle progressStyle = getContentProgressStyle();
        List<String> details = extractClassDetails(stack, charId, progress, progressStyle);
        String description = ClassSelectionData.getClassDescription(charId);
        boolean hasDescription = description != null && !description.isBlank();
        int detailStartYPx = hasDescription ? 23 : 24;
        int detailSpacingPx = hasDescription ? 12 : 14;
        float descriptionStartYPx = Math.max(66, detailStartYPx + details.size() * detailSpacingPx + 6);

        // Left accent bar
        ui.drawRect(cx, cy, px(3), cardLH, accent);

        // Top highlight on hover/drop
        if (hovered || dropTarget) {
            CustomColor highlightColor = dropTarget ? CustomColor.fromHexString("FFAA00") : accent;
            ui.drawRect(cx, cy, cardLW, px(2), highlightColor);
        }

        // Item icon
        float iconAreaPx = 44;
        float iconYPx = Math.max(6, (descriptionStartYPx - iconAreaPx) / 2f);
        ctx.getMatrices().pushMatrix();
        float sIX = ui.sx(cx + px(10));
        float sIY = ui.sy(cy + px(iconYPx));
        ctx.getMatrices().translate(sIX, sIY);
        float iScale = iconAreaPx / 16f;
        ctx.getMatrices().scale(iScale, iScale);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().popMatrix();

        if (gamemodeIcons != null && !gamemodeIcons.getString().isBlank()) {
            drawOverlayText(gamemodeIcons, cx + cardLW - px(8), cy + cardLH - px(16),
                    CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 2.75f);
        }

        // Character name - use client nickname if set, otherwise cleaned name
        String charName = cleanName(stack.getName().getString());
        String clientNick = WynnExtrasConfig.INSTANCE.clientNicknames.get(charId);
        if (clientNick != null && !clientNick.isEmpty()) {
            charName = clientNick;
        }
        if (progressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.COMPACT && progress.found) {
            charName = charName + " (" + Math.round(progress.percent) + "%)";
        }
        boolean completionChroma = hasCompletionChroma(progress);
        CustomColor charNameColor = completionChroma && usesCompletionChromaForName()
                ? WynncraftShaderColor.RAINBOW.color
                : CustomColor.fromHexString("FFFFFF");
        charName = truncate(charName, 24);
        float textX = cx + px(iconAreaPx + 21);
        float textNameY = cy + px(7);
        drawOverlayText(charName, textX, textNameY, charNameColor, 2.55f);

        for (int i = 0; i < details.size(); i++) {
            drawOverlayText(details.get(i), textX, cy + px(detailStartYPx + i * detailSpacingPx),
                    completionChroma && usesCompletionChromaForLines()
                            ? WynncraftShaderColor.RAINBOW.color
                            : accent, 2.25f);
        }

        if (hasDescription) {
            drawOverlayText(truncate(description.trim(), DESCRIPTION_MAX_LENGTH), cx + px(10), cy + px(descriptionStartYPx),
                    CustomColor.fromHexString("BBBBBB"), 2.1f);
        }

        if (progressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.PROGRESS_BAR && progress.found) {
            float progressX = cx + px(10);
            float progressW = cardLW - px(20);
            float progressLabelYPx = hasDescription
                    ? descriptionStartYPx + 20
                    : Math.max(68, detailStartYPx + details.size() * detailSpacingPx + 20);
            float progressLabelY = cy + px(progressLabelYPx);
            drawOverlayCenteredText("Content Progress", cx + cardLW / 2f, progressLabelY,
                    CustomColor.fromHexString("CCCCCC"), 1.95f);
            drawContentProgress(progress, progressX, progressLabelY + px(8), progressW);
            if (progress.total > 0) {
                drawOverlayCenteredText(progress.completed + " of " + progress.total,
                        cx + cardLW / 2f, progressLabelY + px(28),
                        CustomColor.fromHexString("AAAAAA"), 1.8f);
            }
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

    private boolean hasCompletionChroma(ContentProgress progress) {
        return progress.found
                && progress.percent >= 100f
                && !WynnExtrasConfig.INSTANCE.removeChroma
                && getCompletionChromaMode() != WynnExtrasConfig.ClassSelectionCompletionChromaMode.NONE;
    }

    private boolean usesCompletionChromaForName() {
        return getCompletionChromaMode() == WynnExtrasConfig.ClassSelectionCompletionChromaMode.NAME_AND_LINES
                || getCompletionChromaMode() == WynnExtrasConfig.ClassSelectionCompletionChromaMode.NAME_ONLY;
    }

    private boolean usesCompletionChromaForLines() {
        return getCompletionChromaMode() == WynnExtrasConfig.ClassSelectionCompletionChromaMode.NAME_AND_LINES;
    }

    private WynnExtrasConfig.ClassSelectionCompletionChromaMode getCompletionChromaMode() {
        WynnExtrasConfig.ClassSelectionCompletionChromaMode mode = WynnExtrasConfig.INSTANCE.classSelectionCompletionChromaMode;
        return mode == null ? WynnExtrasConfig.ClassSelectionCompletionChromaMode.NAME_AND_LINES : mode;
    }

    private void drawSettingsButtons(DrawContext ctx, List<ItemStack> stacks, int mouseX, int mouseY,
                                      float panelXPx, float settingsYPx, float panelWPx) {
        int[] slots = {SLOT_CANCEL_DELETION, SLOT_BACKUPS, SLOT_MUSIC, SLOT_AUTO_OPEN};
        float btnWPx = 112, btnHPx = 18, gapPx = 8;
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
            String label = truncate(cleanName(stack.getName().getString()), 18);
            float textScale = getFittingButtonTextScale(label, btnWPx, 2.15f);
            ui.drawCenteredText(label, bx + bw / 2f, by + bh / 2f,
                    CustomColor.fromHexString("FFFFFF"), textScale);
        }
    }

    private float getFittingButtonTextScale(String text, float buttonWidthPx, float preferredTextScale) {
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        if (textWidth <= 0) return preferredTextScale;

        float availableWidthPx = buttonWidthPx - 16f;
        float maxTextScale = availableWidthPx * (float) scaleFactor / textWidth;
        return Math.min(preferredTextScale, maxTextScale);
    }

    private void drawOverlayText(String text, float x, float y, CustomColor color, float textScale) {
        ui.drawText(text, x, y, color, getOverlayTextScale(textScale));
    }

    private void drawOverlayText(String text, float x, float y, CustomColor color,
                                 HorizontalAlignment hAlign, VerticalAlignment vAlign, float textScale) {
        ui.drawText(text, x, y, color, hAlign, vAlign, getOverlayTextScale(textScale));
    }

    private void drawOverlayText(Text text, float x, float y, CustomColor color,
                                 HorizontalAlignment hAlign, VerticalAlignment vAlign, float textScale) {
        ui.drawText(text, x, y, color, hAlign, vAlign, getOverlayTextScale(textScale));
    }

    private void drawOverlayCenteredText(String text, float x, float y, CustomColor color, float textScale) {
        ui.drawCenteredText(text, x, y, color, getOverlayTextScale(textScale));
    }

    private void drawContentProgress(ContentProgress progress, float x, float y, float width) {
        float height = px(11);
        float clamped = Math.max(0f, Math.min(1f, progress.percent / 100f));
        ui.drawRect(x, y, width, height, CustomColor.fromHexString("161616"));
        if (clamped > 0) {
            ui.drawRect(x + px(1), y + px(1), Math.max(px(1), (width - px(2)) * clamped), height - px(2),
                    CustomColor.fromInt(0xFF000000 | getProgressColor(progress.percent)));
        }
        ui.drawRect(x, y, width, px(1), CustomColor.fromHexString("5d4736"));
        ui.drawRect(x, y + height - px(1), width, px(1), CustomColor.fromHexString("5d4736"));
        ui.drawRect(x, y, px(1), height, CustomColor.fromHexString("5d4736"));
        ui.drawRect(x + width - px(1), y, px(1), height, CustomColor.fromHexString("5d4736"));
        drawOverlayCenteredText(Math.round(progress.percent) + "%", x + width / 2f, y + height / 2f,
                CustomColor.fromHexString("FFFFFF"), 2.75f);
    }

    private float getOverlayTextScale(float textScale) {
        return textScale * (float) (scaleFactor / Math.max(scaleFactor, 2.0));
    }

    private int getProgressColor(float percent) {
        float clamped = Math.max(0f, Math.min(100f, percent));
        if (clamped <= 50f) {
            return interpolateColor(0xCC3333, 0xE6D34A, clamped / 50f);
        }
        return interpolateColor(0xE6D34A, 0x55AA55, (clamped - 50f) / 50f);
    }

    private int interpolateColor(int from, int to, float t) {
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
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
        if (isTextInputActive()) {
            ensureDescriptionInputWidget();
            if (!descriptionInputWidget.mouseClicked(x, y, button)) {
                descriptionInputWidget.setFocused(true);
            }
            return true;
        }

        if (mode == ScreenMode.CLASS_SELECTION
                && !WynnExtrasConfig.INSTANCE.hideClassSelectionQuickToggleButton
                && overlayToggleWidget.mouseClicked(x, y, button)) {
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
        }
        return true; // consume all clicks when overlay is active
    }

    /** Called from mixin on mouseDragged */
    public void onMouseDragged(double x, double y, int button, double dx, double dy) {
        x /= matrixScale;
        y /= matrixScale;
        if (isTextInputActive()) {
            ensureDescriptionInputWidget();
            descriptionInputWidget.mouseDragged(x, y, button, dx / matrixScale, dy / matrixScale);
            return;
        }
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
        if (isTextInputActive()) {
            ensureDescriptionInputWidget();
            descriptionInputWidget.mouseReleased(x, y, button);
            return;
        }
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
                if (pressedButton == 2 || (pressedButton == 1 && isShiftDown())) {
                    openDescriptionInput(visCharId[pressedVisIdx]);
                } else {
                    clickSlot(slotIndex, pressedButton);
                }
            }
            pressedVisIdx = -1;
            isDragging = false;
        }
    }

    private boolean isShiftDown() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private int findVisualSlotAt(double mx, double my) {
        for (int vis = 0; vis < visibleCardCount; vis++) {
            if (isInBounds(mx, my, cardLX[vis], cardLY[vis], cardLW, cardLH)) return vis;
        }
        return -1;
    }

    // ==================== VANILLA MODE TOGGLE (static, used by mixin) ====================

    public static void renderVanillaToggleButton(DrawContext ctx, HandledScreen<?> screen) {
        if (WynnExtrasConfig.INSTANCE.hideClassSelectionQuickToggleButton) {
            vanillaMode = false;
            return;
        }
        if (WynnExtrasConfig.INSTANCE.customClassSelectionEnabled) return;
        String title = screen.getTitle().getString();
        if (!isClassSelectionScreen(title)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        double mx = mc.mouse.getX() * sw / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        UIUtils vanillaUi = new UIUtils(ctx, 1, 0, 0);
        layoutToggleWidget(VANILLA_TOGGLE_WIDGET, sw, 1f);
        VANILLA_TOGGLE_WIDGET.draw(ctx, (int) mx, (int) my, 0, vanillaUi);
    }

    public static boolean handleVanillaToggleClick(double mx, double my, HandledScreen<?> screen) {
        if (WynnExtrasConfig.INSTANCE.hideClassSelectionQuickToggleButton) {
            vanillaMode = false;
            return false;
        }
        if (WynnExtrasConfig.INSTANCE.customClassSelectionEnabled) return false;
        String title = screen.getTitle().getString();
        if (!isClassSelectionScreen(title)) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        layoutToggleWidget(VANILLA_TOGGLE_WIDGET, mc.getWindow().getScaledWidth(), 1f);
        return VANILLA_TOGGLE_WIDGET.mouseClicked(mx, my, 0);
    }

    private void layoutOverlayToggleWidget(ClassOverlayToggleWidget widget) {
        float visibleScreenWidth = (float) (screenWidth * matrixScale);
        float inverseMatrixScale = (float) (1.0 / matrixScale);
        layoutToggleWidget(widget, visibleScreenWidth, (float) scaleFactor * inverseMatrixScale);
    }

    private static void layoutToggleWidget(ClassOverlayToggleWidget widget, int screenWidth, float logicalScale) {
        layoutToggleWidget(widget, (float) screenWidth, logicalScale);
    }

    private static void layoutToggleWidget(ClassOverlayToggleWidget widget, float screenWidth, float logicalScale) {
        int x = Math.round((screenWidth - CLASS_OVERLAY_TOGGLE_W - CLASS_OVERLAY_TOGGLE_MARGIN) * logicalScale);
        int y = Math.round(CLASS_OVERLAY_TOGGLE_MARGIN * logicalScale);
        int w = Math.round(CLASS_OVERLAY_TOGGLE_W * logicalScale);
        int h = Math.round(CLASS_OVERLAY_TOGGLE_H * logicalScale);
        widget.setBounds(x, y, w, h);
        widget.setTextScale(logicalScale * CLASS_OVERLAY_TOGGLE_TEXT_SCALE);
    }

    private static class ClassOverlayToggleWidget extends Widget {
        private float textScale = 0.8f;

        private void setTextScale(float textScale) {
            this.textScale = textScale;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(WynnExtrasConfig.INSTANCE.customClassSelectionEnabled ? "Disable class overlay" : "Enable class overlay",
                    x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), textScale);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            WynnExtrasConfig.INSTANCE.customClassSelectionEnabled = !WynnExtrasConfig.INSTANCE.customClassSelectionEnabled;
            vanillaMode = !WynnExtrasConfig.INSTANCE.customClassSelectionEnabled;
            WynnExtrasConfig.save();
            return true;
        }
    }

    // ==================== DESCRIPTION INPUT ====================

    /** Called from BankOverlay onInput (KeyInputEvent). Returns true if consumed. */
    public static boolean handleKeyInput(KeyInputEvent event) {
        return descriptionInputActive;
    }

    /** Called from BankOverlay onChar (CharInputEvent). Returns true if consumed. */
    public static boolean handleCharInput(CharInputEvent event) {
        return descriptionInputActive && handleDescriptionCharInput(event);
    }

    public static boolean isTextInputActive() {
        return descriptionInputActive;
    }

    public static boolean handleScreenKeyInput(int keyCode, int scanCode, int modifiers) {
        if (!descriptionInputActive) return false;
        return handleDescriptionKey(keyCode, scanCode, modifiers);
    }

    private static boolean handleDescriptionKey(int key, int scanCode, int modifiers) {
        ensureDescriptionInputWidget();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            descriptionInputActive = false;
            descriptionInputWidget.setFocused(false);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirmDescription();
            return true;
        }
        descriptionInputWidget.keyPressed(key, scanCode, modifiers);
        return true;
    }

    private static boolean handleDescriptionCharInput(CharInputEvent event) {
        ensureDescriptionInputWidget();
        descriptionInputWidget.charTyped(event.getCharacter(), 0);
        return true;
    }

    private static void confirmDescription() {
        descriptionInputActive = false;
        ensureDescriptionInputWidget();
        descriptionInputWidget.setFocused(false);
        String description = descriptionInputWidget.getInput().trim();
        ClassSelectionData.setClassDescription(descriptionCharId, description);
    }

    private void openDescriptionInput(String charId) {
        ensureDescriptionInputWidget();
        descriptionCharId = charId;
        String existing = ClassSelectionData.getClassDescription(charId);
        descriptionText = existing != null ? existing : "";
        descriptionInputWidget.setInputAndMoveCursorToEnd(descriptionText);
        descriptionInputWidget.setFocused(true);
        descriptionInputActive = true;
    }

    private static void ensureDescriptionInputWidget() {
        if (descriptionInputWidget != null) return;
        descriptionInputWidget = new TextInputWidget(0, 0, 0, 0, 6, 11, 4f);
        descriptionInputWidget.setMaxLength(DESCRIPTION_MAX_LENGTH);
        descriptionInputWidget.setTextColor(CustomColor.fromHexString("FFFFFF"));
        descriptionInputWidget.setPlaceholderColor(CustomColor.fromHexString("777777"));
        descriptionInputWidget.setCursorColor(CustomColor.fromHexString("FFFFFF"));
        descriptionInputWidget.setSelectionColor(CustomColor.fromInt(0xAA3366CC));
        descriptionInputWidget.setOnChange(value -> descriptionText = value);
    }

    private void drawDescriptionInput(DrawContext ctx, int mouseX, int mouseY) {
        if (!descriptionInputActive) return;

        ctx.fill(0, 0, screenWidth, screenHeight, 0x88000000);

        float boxWPx = 416, boxHPx = 146;
        float boxXPx = (screenWidth - boxWPx) / 2f;
        float boxYPx = (screenHeight - boxHPx) / 2f;

        ctx.fill((int)(boxXPx - 2), (int)(boxYPx - 2),
                (int)(boxXPx + boxWPx + 2), (int)(boxYPx + boxHPx + 2), 0xFF5d4736);
        ctx.fill((int)boxXPx, (int)boxYPx,
                (int)(boxXPx + boxWPx), (int)(boxYPx + boxHPx), 0xFF1a1410);

        MinecraftClient mc = MinecraftClient.getInstance();
        String title = "Set Class Description";
        int titleW = mc.textRenderer.getWidth(title);
        ctx.drawText(mc.textRenderer, title,
                (int)(boxXPx + (boxWPx - titleW) / 2f), (int)(boxYPx + 14), 0xFFFFAA00, true);

        float fieldX = boxXPx + 22, fieldY = boxYPx + 46;
        float fieldW = boxWPx - 44, fieldH = 31;
        ctx.fill((int)fieldX, (int)fieldY, (int)(fieldX + fieldW), (int)(fieldY + fieldH), 0xFF333333);
        ctx.fill((int)(fieldX + 1), (int)(fieldY + 1), (int)(fieldX + fieldW - 1), (int)(fieldY + fieldH - 1), 0xFF111111);
        ensureDescriptionInputWidget();
        descriptionInputWidget.setBounds((int) px(fieldX), (int) px(fieldY + 2), (int) px(fieldW), (int) px(fieldH));
        descriptionInputWidget.draw(ctx, mouseX, mouseY, 0, ui);

        String counter = descriptionText.length() + "/" + DESCRIPTION_MAX_LENGTH;
        ctx.drawText(mc.textRenderer, counter,
                (int)(fieldX + fieldW - mc.textRenderer.getWidth(counter)), (int)(fieldY + fieldH + 6), 0xFF888888, false);

        String hint = "Enter to confirm  |  Escape to cancel";
        int hintW = mc.textRenderer.getWidth(hint);
        ctx.drawText(mc.textRenderer, hint,
                (int)(boxXPx + (boxWPx - hintW) / 2f), (int)(boxYPx + boxHPx - 24), 0xFF666666, false);
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
            String str = cleanTooltipLine(line);
            if (str.contains("Class")) return str;
        }
        return "";
    }

    private Text extractGamemodeIcons(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String raw = stripFormattingCodes(line.getString());
            String cleaned = cleanTooltipLine(line);
            if (!cleaned.contains("Class")) continue;

            int classLabelIdx = raw.indexOf("Class: ");
            if (classLabelIdx < 0) continue;
            String afterLabel = raw.substring(classLabelIdx + "Class:".length());
            int classNameIdx = findClassNameIndex(afterLabel);
            if (classNameIdx <= 0) continue;

            return sliceStyledText(line, classLabelIdx + "Class: ".length() + 1, classLabelIdx + "Class:".length() + classNameIdx + 1);
        }
        return Text.empty();
    }

    private int findClassNameIndex(String text) {
        String[] classNames = {"Warrior", "Knight", "Mage", "Dark Wizard", "Assassin", "Ninja",
                "Archer", "Hunter", "Shaman", "Skyseer"};
        int bestIdx = -1;
        for (String className : classNames) {
            int idx = text.indexOf(className);
            if (idx >= 0 && (bestIdx < 0 || idx < bestIdx)) bestIdx = idx;
        }
        return bestIdx;
    }

    private List<String> extractClassDetails(ItemStack stack, String charId, ContentProgress progress,
                                             WynnExtrasConfig.ClassSelectionContentProgressStyle progressStyle) {
        List<String> detectedDetails = new ArrayList<>();
        boolean afterClassLine = false;
        boolean skippingContentProgress = false;
        for (Text line : getTooltipLines(stack)) {
            String str = cleanTooltipLine(line);
            if (str.isEmpty()) continue;

            if (str.equalsIgnoreCase("Content Progress")) {
                skippingContentProgress = true;
                continue;
            }
            if (skippingContentProgress) {
                if (PERCENT_PATTERN.matcher(str).find()) continue;
                if (CONTENT_COUNT_PATTERN.matcher(str).find()) {
                    skippingContentProgress = false;
                    continue;
                }
                skippingContentProgress = false;
            }

            if (afterClassLine) {
                detectedDetails.add(truncate(formatClassDetail(str), 30));
                if (detectedDetails.size() >= WynnExtrasConfig.CLASS_SELECTION_BASE_LINE_IDS.size()) break;
            } else if (str.contains("Class")) {
                afterClassLine = true;
            }
        }

        WynnExtrasConfig.INSTANCE.syncClassSelectionLines();
        Map<String, String> detectedById = classifyClassDetails(detectedDetails);
        List<String> details = new ArrayList<>();
        for (String lineId : WynnExtrasConfig.INSTANCE.classSelectionActiveLines) {
            if (WynnExtrasConfig.CLASS_SELECTION_LINE_CONTENT_PROGRESS.equals(lineId)) {
                if (progressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.LINE && progress.found) {
                    details.add("- Content Progress: " + Math.round(progress.percent) + "%");
                }
                continue;
            }
            if (WynnExtrasConfig.CLASS_SELECTION_LINE_LAST_HELD_WEAPON.equals(lineId)) {
                String weaponDetail = getLastHeldWeaponDetail(charId);
                if (weaponDetail != null) details.add(weaponDetail);
                continue;
            }

            String detail = detectedById.get(lineId);
            if (detail == null && WynnExtrasConfig.CLASS_SELECTION_LINE_LEVEL.equals(lineId)) {
                detail = extractLevelDetail(stack);
            }
            if (detail != null) {
                details.add(detail);
            }
        }
        return details;
    }

    private String extractLevelDetail(ItemStack stack) {
        for (Text line : getTooltipLines(stack)) {
            String str = cleanTooltipLine(line);
            if (!str.contains("Level:")) continue;
            String after = str.substring(str.indexOf("Level:") + "Level:".length()).trim();
            if (after.isEmpty()) continue;
            return truncate("- Level: " + after, 30);
        }
        return null;
    }

    private String getLastHeldWeaponDetail(String charId) {
        if (lastHeldWeaponDetailCache.containsKey(charId)) {
            String cached = lastHeldWeaponDetailCache.get(charId);
            return cached == null || cached.isEmpty() ? "- Weapon: unknown" : cached;
        }

        CharIdentity identity = ClassSelectionData.getCharIdentities().get(charId);
        if (identity != null) {
            ItemStack weapon = CrossClassBankSearch.findLastHeldWeaponForClassSelection(
                    identity.stableId,
                    identity.name,
                    identity.classType,
                    identity.level,
                    hasVisibleDuplicateClassCard(identity)
            );
            if (weapon != null && !weapon.isEmpty()) {
                String detail = truncate("- Weapon: " + cleanName(weapon.getName().getString()), 30);
                lastHeldWeaponDetailCache.put(charId, detail);
                return detail;
            }
        }

        lastHeldWeaponDetailCache.put(charId, "");
        return "- Weapon: unknown";
    }

    private boolean hasVisibleDuplicateClassCard(CharIdentity identity) {
        if (identity == null || identity.level <= 0) return true;

        int matches = 0;
        for (int i = 0; i < visibleCardCount; i++) {
            String visibleCharId = visCharId[i];
            if (visibleCharId == null || visibleCharId.isBlank()) continue;

            CharIdentity visibleIdentity = ClassSelectionData.getCharIdentities().get(visibleCharId);
            if (!isSameVisibleClass(identity, visibleIdentity)) continue;

            matches++;
            if (matches > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameVisibleClass(CharIdentity left, CharIdentity right) {
        if (left == null || right == null) return false;
        if (left.level <= 0 || right.level <= 0 || left.level != right.level) return false;

        String leftName = normalizeClassIdentityText(left.name);
        String leftClass = normalizeClassIdentityText(left.classType);
        String rightName = normalizeClassIdentityText(right.name);
        String rightClass = normalizeClassIdentityText(right.classType);
        return (!leftName.isEmpty() && (leftName.equals(rightName) || leftName.equals(rightClass)))
                || (!leftClass.isEmpty() && (leftClass.equals(rightName) || leftClass.equals(rightClass)));
    }

    private String normalizeClassIdentityText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Map<String, String> classifyClassDetails(List<String> detectedDetails) {
        Map<String, String> detectedById = new HashMap<>();
        boolean[] assigned = new boolean[detectedDetails.size()];

        for (int i = 0; i < detectedDetails.size(); i++) {
            String lineId = classifyClassDetail(detectedDetails.get(i));
            if (lineId == null || detectedById.containsKey(lineId)) continue;
            detectedById.put(lineId, detectedDetails.get(i));
            assigned[i] = true;
        }

        for (int i = 0; i < detectedDetails.size(); i++) {
            if (assigned[i]) continue;
            for (String fallbackId : WynnExtrasConfig.CLASS_SELECTION_BASE_LINE_IDS) {
                if (!detectedById.containsKey(fallbackId)) {
                    detectedById.put(fallbackId, detectedDetails.get(i));
                    break;
                }
            }
        }
        return detectedById;
    }

    private String classifyClassDetail(String detail) {
        String lower = detail.toLowerCase(Locale.ROOT);
        if (lower.contains("playtime") || lower.contains("time played")) return WynnExtrasConfig.CLASS_SELECTION_LINE_PLAYTIME;
        if (lower.contains("location") || lower.contains("world") || lower.contains("server")) return WynnExtrasConfig.CLASS_SELECTION_LINE_LOCATION;
        if (lower.contains("level") || lower.contains("lvl")) return WynnExtrasConfig.CLASS_SELECTION_LINE_LEVEL;
        return null;
    }

    private Text sliceStyledText(Text text, int start, int end) {
        List<StyledTextSegment> segments = new ArrayList<>();
        int[] pos = {0};
        text.visit((style, string) -> {
            int segmentStart = pos[0];
            int segmentEnd = segmentStart + string.length();
            int from = Math.max(start, segmentStart);
            int to = Math.min(end, segmentEnd);
            if (from < to) {
                segments.add(new StyledTextSegment(string.substring(from - segmentStart, to - segmentStart), style));
            }
            pos[0] = segmentEnd;
            return Optional.empty();
        }, Style.EMPTY);

        trimStyledSegments(segments);
        MutableText result = Text.empty();
        int remainingChars = 0;
        for (StyledTextSegment segment : segments) {
            remainingChars += segment.text.codePointCount(0, segment.text.length());
        }

        for (StyledTextSegment segment : segments) {
            for (int offset = 0; offset < segment.text.length();) {
                int codePoint = segment.text.codePointAt(offset);
                String glyph = new String(Character.toChars(codePoint));
                result.append(Text.literal(glyph).setStyle(segment.style));
                remainingChars--;
                if (remainingChars > 0) {
                    result.append(Text.literal(" ").setStyle(segment.style));
                }
                offset += Character.charCount(codePoint);
            }
        }
        return result;
    }

    private void trimStyledSegments(List<StyledTextSegment> segments) {
        while (!segments.isEmpty()) {
            StyledTextSegment first = segments.get(0);
            String trimmed = first.text.replaceFirst("^[\\s-]+", "");
            first.text = trimmed;
            if (first.text.isEmpty()) {
                segments.remove(0);
                continue;
            }
            break;
        }

        while (!segments.isEmpty()) {
            StyledTextSegment last = segments.get(segments.size() - 1);
            String trimmed = last.text.replaceFirst("[\\s-]+$", "");
            last.text = trimmed;
            if (last.text.isEmpty()) {
                segments.remove(segments.size() - 1);
                continue;
            }
            break;
        }
    }

    private static class StyledTextSegment {
        String text;
        Style style;

        StyledTextSegment(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }

    private String formatClassDetail(String detail) {
        if (!detail.contains("Time Played:")) return detail;

        String prefix = detail.substring(0, detail.indexOf("Time Played:"));
        String formatted = detail.replace("Time Played:", "Playtime:");
        int valueStart = formatted.indexOf("Playtime:") + "Playtime:".length();
        String value = formatted.substring(valueStart).trim();
        String numberText = value.replace("hours", "").replace("hour", "").trim();
        try {
            int roundedHours = (int) Math.round(Double.parseDouble(numberText));
            return prefix + "Playtime: " + roundedHours + "h";
        } catch (NumberFormatException e) {
            return formatted.replace("hours", "h").replace("hour", "h");
        }
    }

    private String cleanTooltipLine(Text line) {
        return cleanName(line.getString().replaceAll("\u00A7[0-9a-fk-or]", ""));
    }

    private String stripFormattingCodes(String raw) {
        return raw.replaceAll("\u00A7[0-9a-fk-or]", "");
    }

    private ContentProgress extractContentProgress(ItemStack stack) {
        List<Text> lines = getTooltipLines(stack);
        for (int i = 0; i < lines.size(); i++) {
            String str = cleanTooltipLine(lines.get(i));
            if (!str.equalsIgnoreCase("Content Progress")) continue;

            ContentProgress progress = new ContentProgress();
            progress.found = true;
            for (int j = i + 1; j < Math.min(lines.size(), i + 5); j++) {
                String progressLine = cleanTooltipLine(lines.get(j));
                Matcher percentMatcher = PERCENT_PATTERN.matcher(progressLine);
                if (percentMatcher.find()) {
                    try { progress.percent = Float.parseFloat(percentMatcher.group(1)); }
                    catch (NumberFormatException e) { progress.percent = 0; }
                }

                Matcher countMatcher = CONTENT_COUNT_PATTERN.matcher(progressLine);
                if (countMatcher.find()) {
                    try {
                        progress.completed = Integer.parseInt(countMatcher.group(1));
                        progress.total = Integer.parseInt(countMatcher.group(2));
                    } catch (NumberFormatException e) {
                        progress.completed = 0;
                        progress.total = 0;
                    }
                }
            }
            return progress;
        }
        return new ContentProgress();
    }

    private static class ContentProgress {
        boolean found = false;
        float percent = 0;
        int completed = 0;
        int total = 0;
    }

    private CustomColor getClassColor(String classInfo) {
        String classKey = getClassColorKey(classInfo);
        if (WynnExtrasConfig.INSTANCE.useCustomClassColors) {
            Integer customColor = WynnExtrasConfig.INSTANCE.classCardAccentColors.get(classKey);
            if (customColor != null && customColor >= 0) {
                return CustomColor.fromInt(0xFF000000 | (customColor & 0xFFFFFF));
            }
        }
        return CustomColor.fromInt(0xFF000000 | getDefaultClassColor(classKey));
    }

    private String getClassColorKey(String classInfo) {
        String l = classInfo.toLowerCase();
        if (l.contains("knight")) return "knight";
        if (l.contains("warrior")) return "warrior";
        if (l.contains("dark wizard")) return "dark_wizard";
        if (l.contains("mage")) return "mage";
        if (l.contains("ninja")) return "ninja";
        if (l.contains("assassin")) return "assassin";
        if (l.contains("hunter")) return "hunter";
        if (l.contains("archer")) return "archer";
        if (l.contains("skyseer")) return "skyseer";
        if (l.contains("shaman")) return "shaman";
        return "";
    }

    private int getDefaultClassColor(String classKey) {
        return switch (classKey) {
            case "warrior", "knight" -> 0xCC4444;
            case "mage", "dark_wizard" -> 0x55BBFF;
            case "assassin", "ninja" -> 0xFF55FF;
            case "archer", "hunter" -> 0x55FF55;
            case "shaman", "skyseer" -> 0xFFFF55;
            default -> 0x5d4736;
        };
    }
}
