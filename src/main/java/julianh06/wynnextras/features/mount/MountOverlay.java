package julianh06.wynnextras.features.mount;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

@WEModule
public class MountOverlay {
    private static final String MOUNT_FEEDER_ICON = "\uDAFF\uDFED\uE058";
    private static final int[] ROW_SLOTS = {9, 18, 27, 36, 45};
    private static final int[] MATERIAL_LEVELS = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 105, 110, 115};
    private static final String[] STAT_NAMES = {
            "Speed", "Acceleration", "Altitude", "Energy", "Handling", "Toughness", "Boost", "Training"
    };
    private static final int MATERIAL_SLOT_OFFSET = 2;
    private static final int STANDARD_SESSION_SIZE = 5;
    private static final int SUBSCRIBER_SESSION_SIZE = 7;
    private static final int CONTROL_WIDTH = 64;
    private static final int CONTROL_HEIGHT = 16;
    private static final int ARROW_WIDTH = 16;
    private static final int PANEL_WIDTH = 75;
    private static final int ADVANCED_PANEL_WIDTH = 116;
    private static final int PANEL_CONTAINER_GAP = 11;
    private static final int PANEL_VERTICAL_PADDING = 11;
    private static final int ROLE_BUTTON_WIDTH = 38;
    private static final int CONTROL_GAP = 3;
    private static final int STAT_BUTTON_HEIGHT = 14;
    private static final float PREVIEW_ALPHA = 0.55f;
    private static final int STAT_COUNT = MountStat.values().length;
    private static final int MATERIAL_COUNT = MaterialType.values().length;

    private static final Map<Integer, CachedPlan> PLAN_CACHE = new HashMap<>();
    private static final List<Widget> INTERACTIVE_CONTROLS = new ArrayList<>();
    private static Screen activeScreen;
    private static int[] selectedLevels = new int[ROW_SLOTS.length];
    private static boolean[] sevenFedRows = new boolean[ROW_SLOTS.length];
    private static boolean[] baseMountRows = new boolean[ROW_SLOTS.length];
    private static boolean[] selectedStats = createSelectedStats();
    private static boolean levelsInitialized;
    private static int configuredDefaultLevel;
    private static boolean advancedMode;

    private static boolean[] createSelectedStats() {
        boolean[] selected = new boolean[STAT_COUNT];
        Arrays.fill(selected, true);
        return selected;
    }

    public record StatEntry(Integer current, Integer limit, Integer max) {}

    private record MaterialPick(MaterialType type, Identifier texture, String name) {}

    private record InsertedMaterial(MaterialType type, int level) {}

    private record CachedPlan(ItemStack saddle, int materialLevel, String inputSignature,
                              List<InsertedMaterial> insertedMaterials, List<MaterialPick> picks) {}

    public static void render(DrawContext context, int mouseX, int mouseY, float delta) {
        GenericContainerScreen container = getMountFeederScreen();
        if (container == null) {
            clearScreenState();
            return;
        }

        if (activeScreen != container) {
            activeScreen = container;
            int defaultLevel = WynnExtrasConfig.INSTANCE.mountHelperDefaultMaterialLevel == null
                    ? 0
                    : WynnExtrasConfig.INSTANCE.mountHelperDefaultMaterialLevel.getLevel();
            if (!levelsInitialized || configuredDefaultLevel != defaultLevel) {
                Arrays.fill(selectedLevels, defaultLevel);
                levelsInitialized = true;
                configuredDefaultLevel = defaultLevel;
            }
            PLAN_CACHE.clear();
        }

        List<Slot> slots = container.getScreenHandler().slots;
        if (slots.size() < ROW_SLOTS[ROW_SLOTS.length - 1] + SUBSCRIBER_SESSION_SIZE + MATERIAL_SLOT_OFFSET) {
            return;
        }

        HandledScreenAccessor screen = (HandledScreenAccessor) container;
        UIUtils ui = new UIUtils(context, 1, 0, 0);
        int sessionSize = hasLockedBonusColumns(slots) ? STANDARD_SESSION_SIZE : SUBSCRIBER_SESSION_SIZE;
        INTERACTIVE_CONTROLS.clear();
        List<MaterialPreviewWidget> previewWidgets = new ArrayList<>();
        List<ExtraMaterialsWidget> extraWidgets = new ArrayList<>();

        int firstRowY = screen.getY() + slots.get(ROW_SLOTS[0]).y;
        int lastRowY = screen.getY() + slots.get(ROW_SLOTS[ROW_SLOTS.length - 1]).y;
        int allY = firstRowY - 20;
        int advancedY = lastRowY + 20;
        int targetStatsY = advancedY + 31;
        int targetStatsHeight = 4 * STAT_BUTTON_HEIGHT + 3 * CONTROL_GAP;
        int panelWidth = advancedMode ? ADVANCED_PANEL_WIDTH : PANEL_WIDTH;
        int panelX = screen.getX() - PANEL_CONTAINER_GAP - panelWidth;
        int panelY = allY - PANEL_VERTICAL_PADDING;
        int panelBottom = advancedMode
                ? targetStatsY + targetStatsHeight
                : advancedY + CONTROL_HEIGHT;
        int panelHeight = panelBottom + PANEL_VERTICAL_PADDING - panelY;
        int controlX = panelX + (panelWidth - CONTROL_WIDTH) / 2;
        int rowControlX = advancedMode
                ? panelX + (panelWidth - CONTROL_WIDTH - CONTROL_GAP - ROLE_BUTTON_WIDTH) / 2
                : controlX;
        new MountHelperPanelWidget(panelX, panelY, panelWidth, panelHeight)
                .draw(context, mouseX, mouseY, delta, ui);

        LevelControlWidget allControl = new LevelControlWidget(controlX, allY, -1);
        INTERACTIVE_CONTROLS.add(allControl);
        allControl.draw(context, mouseX, mouseY, delta, ui);

        for (int row = 0; row < ROW_SLOTS.length; row++) {
            Slot saddleSlot = slots.get(ROW_SLOTS[row]);
            int rowY = screen.getY() + saddleSlot.y;
            List<Slot> materialSlots = new ArrayList<>(sessionSize);
            for (int index = 0; index < sessionSize; index++) {
                materialSlots.add(slots.get(ROW_SLOTS[row] + MATERIAL_SLOT_OFFSET + index));
            }
            List<MaterialPick> picks = getPlan(row, saddleSlot, materialSlots);
            int currentControlX = advancedMode && baseMountRows[row] ? rowControlX : controlX;
            LevelControlWidget control = new LevelControlWidget(currentControlX, rowY, row);
            INTERACTIVE_CONTROLS.add(control);
            control.draw(context, mouseX, mouseY, delta, ui);
            if (advancedMode && baseMountRows[row]) {
                FeedRoleWidget role = new FeedRoleWidget(
                        currentControlX + CONTROL_WIDTH + CONTROL_GAP, rowY, row);
                INTERACTIVE_CONTROLS.add(role);
                role.draw(context, mouseX, mouseY, delta, ui);
            }
            List<Slot> previewSlots = materialSlots.stream()
                    .filter(slot -> isPreviewable(slot.getStack()))
                    .toList();
            int visibleCount = Math.min(previewSlots.size(), picks.size());
            for (int index = 0; index < visibleCount; index++) {
                Slot materialSlot = previewSlots.get(index);
                MaterialPick pick = picks.get(index);
                MaterialPreviewWidget preview = new MaterialPreviewWidget(
                        screen.getX() + materialSlot.x,
                        screen.getY() + materialSlot.y,
                        pick.texture(),
                        pick.name());
                previewWidgets.add(preview);
                preview.draw(context, mouseX, mouseY, delta, ui);
            }

            if (picks.size() > visibleCount) {
                ExtraMaterialsWidget extra = new ExtraMaterialsWidget(
                        screen.getX() + screen.getBackgroundWidth() + 5,
                        rowY + 3,
                        picks.subList(visibleCount, picks.size()));
                extraWidgets.add(extra);
                extra.draw(context, mouseX, mouseY, delta, ui);
            }
        }

        int advancedToggleWidth = 48;
        int advancedControlsWidth = advancedToggleWidth + CONTROL_GAP + CONTROL_HEIGHT;
        int advancedX = panelX + (panelWidth - advancedControlsWidth) / 2;
        AdvancedModeToggleWidget advancedToggle = new AdvancedModeToggleWidget(
                advancedX, advancedY, advancedToggleWidth);
        INTERACTIVE_CONTROLS.add(advancedToggle);
        advancedToggle.draw(context, mouseX, mouseY, delta, ui);
        AdvancedInfoWidget advancedInfo = new AdvancedInfoWidget(
                advancedX + advancedToggleWidth + CONTROL_GAP, advancedY);
        advancedInfo.draw(context, mouseX, mouseY, delta, ui);
        if (advancedMode) {
            new TargetStatsLabelWidget(panelX, advancedY + 20, panelWidth, 10)
                    .draw(context, mouseX, mouseY, delta, ui);
            int statHorizontalPadding = 6;
            int statButtonWidth = (panelWidth - statHorizontalPadding * 2 - CONTROL_GAP) / 2;
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                int column = stat % 2;
                int statRow = stat / 2;
                TargetStatWidget statWidget = new TargetStatWidget(
                        panelX + statHorizontalPadding + column * (statButtonWidth + CONTROL_GAP),
                        targetStatsY + statRow * (STAT_BUTTON_HEIGHT + CONTROL_GAP),
                        statButtonWidth,
                        stat);
                INTERACTIVE_CONTROLS.add(statWidget);
                statWidget.draw(context, mouseX, mouseY, delta, ui);
            }
        }

        for (ExtraMaterialsWidget extra : extraWidgets) {
            extra.drawTooltip(context, mouseX, mouseY);
        }
        for (MaterialPreviewWidget preview : previewWidgets) {
            preview.drawTooltip(context, mouseX, mouseY);
        }
        advancedInfo.drawTooltip(context, mouseX, mouseY);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        GenericContainerScreen container = getMountFeederScreen();
        if (button != 0 || container == null || activeScreen != container) return false;
        for (int i = INTERACTIVE_CONTROLS.size() - 1; i >= 0; i--) {
            if (INTERACTIVE_CONTROLS.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    private static GenericContainerScreen getMountFeederScreen() {
        if (!WynnExtrasConfig.INSTANCE.showMountHelper) return null;
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (!(currentScreen instanceof GenericContainerScreen container)) return null;
        List<Text> siblings = currentScreen.getTitle().getSiblings();
        if (siblings == null || siblings.isEmpty()) return null;
        return MOUNT_FEEDER_ICON.equals(siblings.getFirst().getString()) ? container : null;
    }

    private static void clearScreenState() {
        activeScreen = null;
        PLAN_CACHE.clear();
        INTERACTIVE_CONTROLS.clear();
    }

    private static void changeLevel(int row, int direction) {
        if (row >= 0) {
            selectedLevels[row] = adjacentLevel(selectedLevels[row], direction);
            PLAN_CACHE.remove(row);
            return;
        }
        for (int i = 0; i < selectedLevels.length; i++) {
            selectedLevels[i] = adjacentLevel(selectedLevels[i], direction);
        }
        PLAN_CACHE.clear();
    }

    private static void toggleAdvancedMode() {
        advancedMode = !advancedMode;
        PLAN_CACHE.clear();
    }

    private static void toggleFeedRole(int row) {
        sevenFedRows[row] = !sevenFedRows[row];
        PLAN_CACHE.remove(row);
    }

    private static void toggleTargetStat(int stat) {
        selectedStats[stat] = !selectedStats[stat];
        PLAN_CACHE.clear();
    }

    private static int adjacentLevel(int current, int direction) {
        int index = 0;
        for (int i = 0; i < MATERIAL_LEVELS.length; i++) {
            if (MATERIAL_LEVELS[i] == current) {
                index = i;
                break;
            }
        }
        return MATERIAL_LEVELS[Math.clamp(index + direction, 0, MATERIAL_LEVELS.length - 1)];
    }

    private static List<MaterialPick> getPlan(int row, Slot saddleSlot, List<Slot> materialSlots) {
        ItemStack saddle = saddleSlot.getStack();
        if (saddle == null || saddle.isEmpty()) {
            baseMountRows[row] = false;
            PLAN_CACHE.remove(row);
            return List.of();
        }

        String inputSignature = inputSignature(materialSlots);
        List<InsertedMaterial> insertedMaterials = new ArrayList<>();
        for (Slot materialSlot : materialSlots) {
            InsertedMaterial inserted = identifyMaterial(materialSlot.getStack());
            if (inserted != null) insertedMaterials.add(inserted);
        }
        CachedPlan cached = PLAN_CACHE.get(row);
        if (cached != null
                && cached.materialLevel() == selectedLevels[row]
                && ItemStack.areItemsAndComponentsEqual(cached.saddle(), saddle)) {
            if (cached.inputSignature().equals(inputSignature)) return cached.picks();

            List<MaterialPick> remainingPicks = removeNewlyInsertedRecommendations(
                    cached.picks(), cached.insertedMaterials(), insertedMaterials, selectedLevels[row]);
            if (remainingPicks != null) {
                PLAN_CACHE.put(row, new CachedPlan(saddle.copy(), selectedLevels[row], inputSignature,
                        List.copyOf(insertedMaterials), remainingPicks));
                return remainingPicks;
            }
        }

        Map<MountStat, StatEntry> stats = getStats(saddle);
        if (stats.size() != STAT_COUNT) {
            baseMountRows[row] = false;
            PLAN_CACHE.remove(row);
            return List.of();
        }
        baseMountRows[row] = isBaseMount(stats);

        Map<MountStat, Integer> needed = new EnumMap<>(MountStat.class);
        for (MountStat stat : MountStat.values()) {
            if (advancedMode && !selectedStats[stat.ordinal()]) continue;
            StatEntry entry = stats.get(stat);
            int deficit = Math.max(0, entry.max() - entry.limit());
            if (deficit > 0) needed.put(stat, deficit);
        }
        for (InsertedMaterial inserted : insertedMaterials) {
            for (Map.Entry<MountStat, Integer> contribution
                    : MaterialStats.get(inserted.type(), inserted.level()).getStats().entrySet()) {
                int remaining = needed.getOrDefault(contribution.getKey(), 0) - contribution.getValue();
                if (remaining <= 0) needed.remove(contribution.getKey());
                else needed.put(contribution.getKey(), remaining);
            }
        }

        List<MaterialType> orderedTypes;
        if (advancedMode && baseMountRows[row]) {
            int targetCount = sevenFedRows[row] ? 7 : 3;
            orderedTypes = computeFixedCountPlan(
                    stats,
                    insertedMaterials,
                    selectedStats,
                    selectedLevels[row],
                    Math.max(0, targetCount - insertedMaterials.size()));
        } else {
            Map<MaterialType, Integer> counts = optimizeNeeded(selectedLevels[row], needed);
            orderedTypes = orderMaterials(selectedLevels[row], needed, counts);
        }
        List<MaterialPick> picks = orderedTypes.stream()
                .map(type -> new MaterialPick(type, type.getTexture(selectedLevels[row]), type.getName(selectedLevels[row])))
                .toList();
        PLAN_CACHE.put(row, new CachedPlan(saddle.copy(), selectedLevels[row], inputSignature,
                List.copyOf(insertedMaterials), picks));
        return picks;
    }

    private static List<MaterialPick> removeNewlyInsertedRecommendations(
            List<MaterialPick> recommendations, List<InsertedMaterial> previousInserted,
            List<InsertedMaterial> currentInserted, int materialLevel) {
        List<InsertedMaterial> added = new ArrayList<>(currentInserted);
        for (InsertedMaterial previous : previousInserted) {
            if (!added.remove(previous)) return null;
        }
        if (added.isEmpty()) return null;
        for (InsertedMaterial inserted : added) {
            if (inserted.level() != materialLevel) return null;
        }

        List<MaterialType> remainingTypes = removeInsertedRecommendations(
                recommendations.stream().map(MaterialPick::type).toList(),
                added.stream().map(InsertedMaterial::type).toList());
        if (remainingTypes == null) return null;
        return remainingTypes.stream()
                .map(type -> new MaterialPick(type, type.getTexture(materialLevel), type.getName(materialLevel)))
                .toList();
    }

    static List<MaterialType> removeInsertedRecommendations(
            List<MaterialType> recommendations, List<MaterialType> inserted) {
        List<MaterialType> remaining = new ArrayList<>(recommendations);
        for (MaterialType type : inserted) {
            if (!remaining.remove(type)) return null;
        }
        return List.copyOf(remaining);
    }

    private static String inputSignature(List<Slot> materialSlots) {
        StringBuilder signature = new StringBuilder();
        for (Slot slot : materialSlots) {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) {
                signature.append('|');
            } else {
                signature.append('|').append(stack.getName().getString()).append(':').append(stack.getCount());
            }
        }
        return signature.toString();
    }

    private static InsertedMaterial identifyMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String itemName = stack.getName().getString().toLowerCase(Locale.ROOT);
        for (int level : MATERIAL_LEVELS) {
            for (MaterialType type : MaterialType.values()) {
                if (itemName.contains(type.getName(level).toLowerCase(Locale.ROOT))) {
                    return new InsertedMaterial(type, level);
                }
            }
        }
        return null;
    }

    private static boolean isBaseMount(Map<MountStat, StatEntry> stats) {
        int totalMax = 0;
        for (StatEntry entry : stats.values()) totalMax += entry.max();
        return totalMax == 240;
    }

    private static List<MaterialType> computeFixedCountPlan(Map<MountStat, StatEntry> stats,
                                                            List<InsertedMaterial> insertedMaterials,
                                                            boolean[] selected,
                                                            int materialLevel, int itemCount) {
        int[] working = new int[STAT_COUNT];
        int[] maximum = new int[STAT_COUNT];
        for (MountStat stat : MountStat.values()) {
            StatEntry entry = stats.get(stat);
            working[stat.ordinal()] = entry.limit();
            maximum[stat.ordinal()] = entry.max();
        }
        for (InsertedMaterial inserted : insertedMaterials) {
            for (Map.Entry<MountStat, Integer> contribution
                    : MaterialStats.get(inserted.type(), inserted.level()).getStats().entrySet()) {
                int stat = contribution.getKey().ordinal();
                working[stat] = Math.min(maximum[stat], working[stat] + contribution.getValue());
            }
        }

        int[][] materialStats = makeMaterialStatsTable(materialLevel);
        int[] maxGain = new int[STAT_COUNT];
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                maxGain[stat] = Math.max(maxGain[stat], materialStats[material][stat]);
            }
        }

        List<MaterialType> result = new ArrayList<>();
        List<Integer> targetStats = new ArrayList<>();
        for (int stat = 0; stat < STAT_COUNT; stat++) {
            if (selected[stat]) targetStats.add(stat);
        }
        if (targetStats.isEmpty()) return result;
        int nextStat = 0;
        for (int item = 0; item < itemCount; item++) {
            int targetStat = -1;
            for (int attempt = 0; attempt < targetStats.size(); attempt++) {
                int candidate = targetStats.get(nextStat++ % targetStats.size());
                if (working[candidate] < maximum[candidate]) {
                    targetStat = candidate;
                    break;
                }
            }
            if (targetStat < 0) break;

            int bestMaterial = -1;
            double bestScore = -1;
            for (int material = 0; material < MATERIAL_COUNT; material++) {
                int primary = Math.min(
                        materialStats[material][targetStat],
                        maximum[targetStat] - working[targetStat]);
                if (primary <= 0) continue;
                double bonus = 0;
                for (int stat : targetStats) {
                    if (stat == targetStat || maxGain[stat] == 0) continue;
                    int remaining = maximum[stat] - working[stat];
                    if (remaining > 0) {
                        bonus += (double) Math.min(materialStats[material][stat], remaining) / maxGain[stat];
                    }
                }
                double score = primary + bonus * 0.001;
                if (score > bestScore) {
                    bestScore = score;
                    bestMaterial = material;
                }
            }
            if (bestMaterial < 0) break;
            result.add(MaterialType.values()[bestMaterial]);
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                working[stat] = Math.min(maximum[stat], working[stat] + materialStats[bestMaterial][stat]);
            }
        }
        return groupMaterials(result);
    }

    public static Map<MountStat, StatEntry> getStats(ItemStack stack) {
        List<Text> tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player,
                TooltipType.BASIC);
        Map<MountStat, StatEntry> result = new EnumMap<>(MountStat.class);
        for (Text line : tooltip) {
            Matcher matcher = MountStat.PATTERN.matcher(line.getString());
            if (!matcher.matches()) continue;
            MountStat stat = MountStat.fromString(matcher.group("stat"));
            if (stat == null) continue;
            result.put(stat, new StatEntry(
                    Integer.parseInt(matcher.group("current")),
                    Integer.parseInt(matcher.group("limit")),
                    Integer.parseInt(matcher.group("max"))));
        }
        return result;
    }

    private static Map<MaterialType, Integer> optimizeNeeded(int materialLevel, Map<MountStat, Integer> needed) {
        Map<MaterialType, Integer> result = new EnumMap<>(MaterialType.class);
        if (needed.isEmpty()) return result;

        int[][] materialStats = makeMaterialStatsTable(materialLevel);
        int[] need = new int[STAT_COUNT];
        int[] maxPerStat = new int[STAT_COUNT];
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                maxPerStat[stat] = Math.max(maxPerStat[stat], materialStats[material][stat]);
            }
        }
        for (Map.Entry<MountStat, Integer> entry : needed.entrySet()) {
            int stat = entry.getKey().ordinal();
            need[stat] = entry.getValue();
            if (maxPerStat[stat] == 0) return result;
        }

        int upperBound = greedyUpperBound(need, materialStats);
        if (upperBound < 0) return result;
        int lowerBound = 0;
        for (int stat = 0; stat < STAT_COUNT; stat++) {
            if (need[stat] > 0) {
                lowerBound = Math.max(lowerBound, (need[stat] + maxPerStat[stat] - 1) / maxPerStat[stat]);
            }
        }

        int[][] maxFromIndex = new int[MATERIAL_COUNT + 1][STAT_COUNT];
        for (int material = MATERIAL_COUNT - 1; material >= 0; material--) {
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                maxFromIndex[material][stat] = Math.max(
                        materialStats[material][stat],
                        maxFromIndex[material + 1][stat]);
            }
        }

        int[] counts = new int[MATERIAL_COUNT];
        int[] solution = null;
        for (int total = lowerBound; total <= upperBound; total++) {
            Arrays.fill(counts, 0);
            if (search(counts, 0, total, need, new int[STAT_COUNT], materialStats, maxFromIndex)) {
                solution = counts.clone();
                break;
            }
        }
        if (solution == null) return result;
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            if (solution[material] > 0) result.put(MaterialType.values()[material], solution[material]);
        }
        return result;
    }

    private static int greedyUpperBound(int[] need, int[][] materialStats) {
        int[] remaining = need.clone();
        for (int steps = 0; steps < 2000; steps++) {
            if (allCovered(remaining)) return steps;
            int bestMaterial = -1;
            int bestScore = 0;
            for (int material = 0; material < MATERIAL_COUNT; material++) {
                int score = 0;
                for (int stat = 0; stat < STAT_COUNT; stat++) {
                    if (remaining[stat] > 0) {
                        score += Math.min(remaining[stat], materialStats[material][stat]);
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestMaterial = material;
                }
            }
            if (bestMaterial < 0) return -1;
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                remaining[stat] = Math.max(0, remaining[stat] - materialStats[bestMaterial][stat]);
            }
        }
        return -1;
    }

    private static boolean allCovered(int[] remaining) {
        for (int amount : remaining) {
            if (amount > 0) return false;
        }
        return true;
    }

    private static boolean search(int[] counts, int materialIndex, int slotsLeft, int[] need, int[] coverage,
                                  int[][] materialStats, int[][] maxFromIndex) {
        if (materialIndex == MATERIAL_COUNT) return slotsLeft == 0 && meetsNeed(coverage, need);
        if (!canStillReach(coverage, need, materialIndex, slotsLeft, maxFromIndex)) return false;

        int[] material = materialStats[materialIndex];
        for (int take = slotsLeft; take >= 0; take--) {
            counts[materialIndex] = take;
            int[] nextCoverage = new int[STAT_COUNT];
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                nextCoverage[stat] = coverage[stat] + take * material[stat];
            }
            if (search(counts, materialIndex + 1, slotsLeft - take, need, nextCoverage,
                    materialStats, maxFromIndex)) return true;
        }
        counts[materialIndex] = 0;
        return false;
    }

    private static boolean canStillReach(int[] coverage, int[] need, int materialIndex, int slotsLeft,
                                         int[][] maxFromIndex) {
        for (int stat = 0; stat < STAT_COUNT; stat++) {
            int remaining = need[stat] - coverage[stat];
            if (remaining <= 0) continue;
            if (maxFromIndex[materialIndex][stat] == 0
                    || remaining > slotsLeft * maxFromIndex[materialIndex][stat]) return false;
        }
        return true;
    }

    private static boolean meetsNeed(int[] coverage, int[] need) {
        for (int stat = 0; stat < STAT_COUNT; stat++) {
            if (coverage[stat] < need[stat]) return false;
        }
        return true;
    }

    private static int[][] makeMaterialStatsTable(int materialLevel) {
        int[][] table = new int[MATERIAL_COUNT][STAT_COUNT];
        for (MaterialType type : MaterialType.values()) {
            for (Map.Entry<MountStat, Integer> entry : MaterialStats.get(type, materialLevel).getStats().entrySet()) {
                table[type.ordinal()][entry.getKey().ordinal()] = entry.getValue();
            }
        }
        return table;
    }

    private static List<MaterialType> orderMaterials(int materialLevel, Map<MountStat, Integer> needed,
                                                     Map<MaterialType, Integer> counts) {
        Map<MaterialType, Integer> available = new EnumMap<>(counts);
        int[] remaining = new int[STAT_COUNT];
        needed.forEach((stat, amount) -> remaining[stat.ordinal()] = amount);
        int[][] materialStats = makeMaterialStatsTable(materialLevel);
        int[] maxGain = new int[STAT_COUNT];
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                maxGain[stat] = Math.max(maxGain[stat], materialStats[material][stat]);
            }
        }
        List<MaterialType> result = new ArrayList<>();

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        for (int pick = 0; pick < total; pick++) {
            MaterialType bestType = null;
            double bestScore = -1;
            for (MaterialType type : MaterialType.values()) {
                if (available.getOrDefault(type, 0) <= 0) continue;
                double score = 0;
                for (int stat = 0; stat < STAT_COUNT; stat++) {
                    if (remaining[stat] > 0 && maxGain[stat] > 0) {
                        score += (double) Math.min(materialStats[type.ordinal()][stat], remaining[stat])
                                / maxGain[stat];
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestType = type;
                }
            }
            if (bestType == null) break;
            result.add(bestType);
            available.put(bestType, available.get(bestType) - 1);
            for (int stat = 0; stat < STAT_COUNT; stat++) {
                remaining[stat] = Math.max(0, remaining[stat] - materialStats[bestType.ordinal()][stat]);
            }
        }

        return groupMaterials(result);
    }

    private static List<MaterialType> groupMaterials(List<MaterialType> materials) {
        Map<MaterialType, Integer> grouped = new LinkedHashMap<>();
        for (MaterialType type : materials) {
            grouped.merge(type, 1, Integer::sum);
        }
        List<MaterialType> groupedResult = new ArrayList<>(materials.size());
        grouped.forEach((type, amount) -> {
            for (int i = 0; i < amount; i++) groupedResult.add(type);
        });
        return groupedResult;
    }

    private static boolean hasLockedBonusColumns(List<Slot> slots) {
        for (int rowSlot : ROW_SLOTS) {
            for (int offset = STANDARD_SESSION_SIZE; offset < SUBSCRIBER_SESSION_SIZE; offset++) {
                ItemStack stack = slots.get(rowSlot + MATERIAL_SLOT_OFFSET + offset).getStack();
                if (isMembershipLock(stack)) return true;
            }
        }
        return false;
    }

    private static boolean isMembershipLock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.POTION)) return false;
        Text customName = stack.getCustomName();
        if (customName != null
                && customName.getString().toLowerCase(Locale.ROOT).contains("silverbull membership")) return true;

        try {
            for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player,
                    TooltipType.BASIC)) {
                String text = line.getString().toLowerCase(Locale.ROOT);
                if (text.contains("silverbull membership")
                        || text.contains("required to use this slot")) return true;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    private static boolean isPreviewable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        return !containsAsciiLetterOrDigit(stack.getName().getString());
    }

    private static boolean containsAsciiLetterOrDigit(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character >= '0' && character <= '9'
                    || character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z') return true;
        }
        return false;
    }

    private static final class LevelControlWidget extends Widget {
        private final int row;

        private LevelControlWidget(int x, int y, int row) {
            super(x, y, CONTROL_WIDTH, CONTROL_HEIGHT);
            this.row = row;
            addChild(new LevelArrowButtonWidget(x, y, -1, row));
            addChild(new LevelArrowButtonWidget(x + CONTROL_WIDTH - ARROW_WIDTH, y, 1, row));
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawCenteredText(row < 0 ? "All" : String.valueOf(selectedLevels[row]),
                    x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 1);
        }
    }

    private static final class MountHelperPanelWidget extends Widget {
        private MountHelperPanelWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawVanillaPanel(x, y - 7, width, height + 3, 4, 24, 24, 15, 24);
            ui.drawText("§6Mount helper", x + width / 2f, y - 3, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, 1);
        }
    }

    private static final class LevelArrowButtonWidget extends Widget {
        private final int direction;
        private final int row;

        private LevelArrowButtonWidget(int x, int y, int direction, int row) {
            super(x, y, ARROW_WIDTH, CONTROL_HEIGHT);
            this.direction = direction;
            this.row = row;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(direction < 0 ? "-" : "+", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFFFFF"), 1);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            changeLevel(row, direction);
            return true;
        }
    }

    private static final class FeedRoleWidget extends Widget {
        private final int row;

        private FeedRoleWidget(int x, int y, int row) {
            super(x, y, ROLE_BUTTON_WIDTH, CONTROL_HEIGHT);
            this.row = row;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(sevenFedRows[row] ? "7-fed" : "3-fed", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString(sevenFedRows[row] ? "55FFFF" : "FFAA00"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            toggleFeedRole(row);
            return true;
        }
    }

    private static final class AdvancedModeToggleWidget extends Widget {
        private AdvancedModeToggleWidget(int x, int y, int width) {
            super(x, y, width, CONTROL_HEIGHT);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(advancedMode ? "Adv: ON" : "Adv: OFF", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString(advancedMode ? "55FF55" : "AAAAAA"), 0.9f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            toggleAdvancedMode();
            return true;
        }
    }

    private static final class AdvancedInfoWidget extends Widget {
        private AdvancedInfoWidget(int x, int y) {
            super(x, y, CONTROL_HEIGHT, CONTROL_HEIGHT);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText("i", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("55FFFF"), 0.85f);
        }

        private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
            if (!hovered) return;
            List<Text> lines = List.of(
                    WynnExtras.addWynnExtrasPrefix("§6Advanced 7 + 3 strategy"),
                    Text.literal("§8As the title already states, this is advanced."),
                    Text.literal("§8The normal mode is good enough for the average user"),
                    Text.literal("Prepare one fresh mount with 3 materials"),
                    Text.literal("and another with 7, then breed them together."),
                    Text.literal("The first feeds spread points across the selected"),
                    Text.literal("stats. After breeding, only those stats are maxed."),
                    Text.literal("Continue by breeding the results in a pyramid."),
                    Text.literal("This uses more saddles and needs more frequent"),
                    Text.literal("feeder visits, but is designed to save time."),
                    Text.literal("The 3-fed/7-fed limit only applies to unbred mounts."));
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
        }
    }

    private static final class TargetStatsLabelWidget extends Widget {
        private TargetStatsLabelWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawCenteredText("Target stats", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFAA00"), 0.8f);
        }
    }

    private static final class TargetStatWidget extends Widget {
        private final int stat;

        private TargetStatWidget(int x, int y, int width, int stat) {
            super(x, y, width, STAT_BUTTON_HEIGHT);
            this.stat = stat;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(STAT_NAMES[stat], x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString(selectedStats[stat] ? "55FF55" : "777777"), 0.65f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return false;
            toggleTargetStat(stat);
            return true;
        }
    }

    private static final class MaterialPreviewWidget extends Widget {
        private final Identifier texture;
        private final String materialName;

        private MaterialPreviewWidget(int x, int y, Identifier texture, String materialName) {
            super(x, y, 18, 18);
            this.texture = texture;
            this.materialName = materialName;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawImage(texture, x, y, 16, 16, PREVIEW_ALPHA);
        }

        private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
            if (!hovered) return;
            context.drawTooltip(Text.literal("Recommended material: " + materialName), mouseX, mouseY);
        }
    }

    private static final class ExtraMaterialsWidget extends Widget {
        private final List<MaterialPick> materials;

        private ExtraMaterialsWidget(int x, int y, List<MaterialPick> materials) {
            super(x, y, MinecraftClient.getInstance().textRenderer.getWidth("+" + materials.size()) + 2, 10);
            this.materials = List.copyOf(materials);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            ui.drawText("+" + materials.size(), x, y, CustomColor.fromHexString("FFAA00"), 1);
        }

        private void drawTooltip(DrawContext context, int mouseX, int mouseY) {
            if (!hovered) return;
            Map<String, Integer> grouped = new LinkedHashMap<>();
            for (MaterialPick material : materials) {
                grouped.merge(material.name(), 1, Integer::sum);
            }
            List<Text> lines = new ArrayList<>();
            lines.add(WynnExtras.addWynnExtrasPrefix("§6Mount helper"));
            lines.add(Text.literal("Still needed:"));
            grouped.forEach((name, quantity) -> lines.add(Text.literal(quantity + "x " + name)));
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, lines, mouseX, mouseY);
        }
    }
}
