package julianh06.wynnextras.features.crafting.calc;

import com.wynntils.core.components.Models;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.crafting.calc.CraftXpCalculator.MaterialType;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ProfessionCalculatorScreen extends WEScreen {

    private static final String[] CRAFTING_PROFESSIONS = {
            "Alchemism", "Armouring", "Cooking", "Jeweling",
            "Scribing", "Tailoring", "Weaponsmithing", "Woodworking"
    };

    // Recipe names per profession
    private static final String[][] RECIPE_NAMES = {
            {"Potion"},                          // Alchemism
            {"Helmet", "Chestplate"},            // Armouring
            {"Food"},                            // Cooking
            {"Ring", "Bracelet", "Necklace"},    // Jeweling
            {"Scroll"},                          // Scribing
            {"Leggings", "Boots"},              // Tailoring
            {"Spear", "Dagger"},                // Weaponsmithing
            {"Bow", "Wand", "Relik"},           // Woodworking
    };

    // Material names per recipe: [profession][recipe] -> {mat1_name, mat2_name}
    private static final String[][][] RECIPE_MATS = {
            {{"Oil", "Grains"}},                                                   // Alchemism
            {{"Paper", "Ingot"}, {"Paper", "Ingot"}},                              // Armouring
            {{"Meat", "Grains"}},                                                  // Cooking
            {{"Gem", "Oil"}, {"Gem", "Oil"}, {"Gem", "Oil"}},                      // Jeweling
            {{"Oil", "Paper"}},                                                    // Scribing
            {{"Ingot", "String"}, {"Ingot", "String"}},                            // Tailoring
            {{"Wood", "Ingot"}, {"Wood", "Ingot"}},                                // Weaponsmithing
            {{"Wood", "String"}, {"Wood", "String"}, {"Wood", "Oil"}},             // Woodworking
    };

    // Material base ratios per recipe: [profession][recipe] -> {mat1_ratio, mat2_ratio}
    // Actual amounts at Sky/Dernic tier = ratio * 3
    private static final int[][][] RECIPE_RATIOS = {
            {{1, 2}},                       // Alchemism: Potion
            {{2, 1}, {1, 2}},              // Armouring: Helmet, Chestplate
            {{2, 1}},                       // Cooking: Food
            {{1, 1}, {2, 1}, {3, 1}},      // Jeweling: Ring, Bracelet, Necklace
            {{1, 1}},                       // Scribing: Scroll
            {{2, 1}, {1, 2}},              // Tailoring: Leggings, Boots
            {{2, 1}, {1, 2}},              // Weaponsmithing: Spear, Dagger
            {{1, 2}, {2, 1}, {1, 2}},      // Woodworking: Bow, Wand, Relik
    };

    private static final int MAT_AMOUNT_SCALE = 3; // At Sky/Dernic tier, actual amount = ratio * 3

    private static final String[] MATERIAL_TYPES = {"Dernic", "Sky"};
    private static final String[] XP_MULTIPLIERS = {"1x", "1.5x", "2x", "2.5x", "3x", "3.5x", "4x", "4.5x", "5x"};
    private static final double[] XP_MULT_VALUES = {1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};
    private static final String[] PROF_SPEED_OPTIONS = {"On", "Off"};
    private static final String[] ING_TIER_FILTER_OPTIONS = {"All", "T1 Only", "T2 Only", "T3 Only"};

    // All 9 material tier combinations: {mat1_tier, mat2_tier}
    private static final int[][] MAT_COMBOS = {
            {1, 1}, {1, 2}, {1, 3},
            {2, 1}, {2, 2}, {2, 3},
            {3, 1}, {3, 2}, {3, 3}
    };

    private CycleButtonWidget professionButton;
    private CycleButtonWidget recipeButton;
    private CycleButtonWidget materialTypeButton;
    private CycleButtonWidget xpMultButton;
    private CycleButtonWidget profSpeedButton;
    private CycleButtonWidget ingTierFilterButton;
    private TextInputWidget fromLevelInput;
    private TextInputWidget toLevelInput;
    private TextInputWidget currentOverflowInput;
    private TextInputWidget overflowGoalInput;
    private TextInputWidget topNInput;

    // Price inputs: [tier 0=T1, 1=T2, 2=T3], values in eb
    private final TextInputWidget[] ingPriceInputs = new TextInputWidget[3];
    private final TextInputWidget[] mat1PriceInputs = new TextInputWidget[3];
    private final TextInputWidget[] mat2PriceInputs = new TextInputWidget[3];

    private TableRow[] tableRows;
    private long lastInputHash = 0;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    private record TableRow(int ingTier, int matTier1, int matTier2, double xpPerCraft, int craftsNeeded, double totalCost) {}

    public ProfessionCalculatorScreen() {
        super(Text.literal("Profession XP Calculator"));
    }

    @Override
    protected void init() {
        super.init();
        rootWidgets.clear();
        scrollOffset = 0;

        professionButton = new CycleButtonWidget("Profession", CRAFTING_PROFESSIONS, idx -> onProfessionChanged());
        recipeButton = new CycleButtonWidget("Recipe", RECIPE_NAMES[0], idx -> recalculate());
        materialTypeButton = new CycleButtonWidget("Material", MATERIAL_TYPES, idx -> recalculate());
        xpMultButton = new CycleButtonWidget("XP Bonus", XP_MULTIPLIERS, idx -> recalculate());
        profSpeedButton = new CycleButtonWidget("Prof Speed", PROF_SPEED_OPTIONS, idx -> recalculate());
        ingTierFilterButton = new CycleButtonWidget("Ing Tier", ING_TIER_FILTER_OPTIONS, idx -> {});

        fromLevelInput = createStyledInput("Level (99-132)");
        toLevelInput = createStyledInput("Level (99-132)");
        toLevelInput.setInput("132");
        currentOverflowInput = createStyledInput("e.g. 200M");
        overflowGoalInput = createStyledInput("e.g. 500M");
        topNInput = createStyledInput("All");

        for (int t = 0; t < 3; t++) {
            ingPriceInputs[t] = createStyledInput("Price (eb)");
            mat1PriceInputs[t] = createStyledInput("Price (eb)");
            mat2PriceInputs[t] = createStyledInput("Price (eb)");
        }

        addRootWidget(professionButton);
        addRootWidget(recipeButton);
        addRootWidget(materialTypeButton);
        addRootWidget(xpMultButton);
        addRootWidget(profSpeedButton);
        addRootWidget(ingTierFilterButton);
        addRootWidget(fromLevelInput);
        addRootWidget(toLevelInput);
        addRootWidget(currentOverflowInput);
        addRootWidget(overflowGoalInput);
        addRootWidget(topNInput);
        for (int t = 0; t < 3; t++) {
            addRootWidget(ingPriceInputs[t]);
            addRootWidget(mat1PriceInputs[t]);
            addRootWidget(mat2PriceInputs[t]);
        }

        autoDetect();
        recalculate();
    }

    private TextInputWidget createStyledInput(String placeholder) {
        TextInputWidget input = new TextInputWidget(0, 0, 0, 0, 12, 8, 3);
        input.setPlaceholder(placeholder);
        input.setPlaceholderColor(CustomColor.fromHexString("666666"));
        input.setBackgroundColor(CustomColor.fromHexString("40333333"));
        input.setFocusedColor(CustomColor.fromHexString("40555555"));
        input.setTextColor(CustomColor.fromHexString("FFFFFF"));
        return input;
    }

    private void onProfessionChanged() {
        int profIdx = professionButton.getSelectedIndex();
        recipeButton.setOptions(RECIPE_NAMES[profIdx]);
        detectLevelForSelected();
        recalculate();
    }

    private void autoDetect() {
        ProfessionType lastProf = ProfessionOverlay.getLastProfession();
        if (lastProf != null) {
            String name = lastProf.getDisplayName();
            for (int i = 0; i < CRAFTING_PROFESSIONS.length; i++) {
                if (CRAFTING_PROFESSIONS[i].equalsIgnoreCase(name)) {
                    professionButton.setSelectedIndexSilent(i);
                    recipeButton.setOptions(RECIPE_NAMES[i]);
                    break;
                }
            }
        }
        detectLevelForSelected();
    }

    private void detectLevelForSelected() {
        ProfessionType selectedProf = getSelectedProfession();
        if (selectedProf == null) return;

        int level = Models.Profession.getLevel(selectedProf);
        if (level >= 99 && level <= 132) {
            fromLevelInput.setInput(String.valueOf(level));
        }

        if (level >= 132) {
            float overflow = ProfessionOverlay.getOverflow(selectedProf);
            currentOverflowInput.setInput(overflow > 0 ? formatXp(overflow) : "");
            float goal = ProfessionOverlay.getGoal(selectedProf);
            overflowGoalInput.setInput(goal > 0 ? formatXp(goal) : "");
        } else {
            currentOverflowInput.setInput("");
            overflowGoalInput.setInput("");
        }
    }

    private ProfessionType getSelectedProfession() {
        if (professionButton == null) return null;
        return ProfessionType.fromString(CRAFTING_PROFESSIONS[professionButton.getSelectedIndex()].toLowerCase());
    }

    private String[] getCurrentMatNames() {
        int profIdx = professionButton.getSelectedIndex();
        int recipeIdx = recipeButton.getSelectedIndex();
        return RECIPE_MATS[profIdx][recipeIdx];
    }

    private int[] getCurrentMatRatios() {
        int profIdx = professionButton.getSelectedIndex();
        int recipeIdx = recipeButton.getSelectedIndex();
        return RECIPE_RATIOS[profIdx][recipeIdx];
    }

    private int parseLevel(TextInputWidget input, int fallback) {
        String text = input.getInput().trim();
        if (text.isEmpty()) return fallback;
        try { return Math.max(99, Math.min(132, Integer.parseInt(text))); }
        catch (NumberFormatException e) { return fallback; }
    }

    private int getFromLevel() { return parseLevel(fromLevelInput, 99); }
    private int getToLevel() { return parseLevel(toLevelInput, 132); }
    private MaterialType getMaterialType() { return materialTypeButton.getSelectedIndex() == 0 ? MaterialType.DERNIC : MaterialType.SKY; }
    private double getXpMultiplier() { return XP_MULT_VALUES[xpMultButton.getSelectedIndex()]; }
    private boolean hasProfSpeed() { return profSpeedButton.getSelectedIndex() == 0; }

    private double parseNumber(String text) {
        text = text.trim().toLowerCase().replace(',', '.');
        if (text.isEmpty()) return 0;
        try {
            double mult = 1;
            if (text.endsWith("b")) { mult = 1_000_000_000; text = text.substring(0, text.length() - 1); }
            else if (text.endsWith("m")) { mult = 1_000_000; text = text.substring(0, text.length() - 1); }
            else if (text.endsWith("k")) { mult = 1_000; text = text.substring(0, text.length() - 1); }
            return Double.parseDouble(text.trim()) * mult;
        } catch (NumberFormatException e) { return 0; }
    }

    private double getCurrentOverflow() { return parseNumber(currentOverflowInput.getInput()); }
    private double getOverflowGoal() { return parseNumber(overflowGoalInput.getInput()); }
    private double getPrice(TextInputWidget input) { return parseNumber(input.getInput()); }
    private boolean hasPriceSet(TextInputWidget input) { return !input.getInput().trim().isEmpty(); }

    private int getTopN() {
        String text = topNInput.getInput().trim();
        if (text.isEmpty()) return 0;
        try { return Math.max(0, Integer.parseInt(text)); }
        catch (NumberFormatException e) { return 0; }
    }

    private String formatXp(double xp) {
        if (xp >= 1_000_000_000) return String.format("%.1fB", xp / 1_000_000_000);
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000);
        if (xp >= 1_000) return String.format("%.1fK", xp / 1_000);
        return String.format("%.0f", xp);
    }

    private String formatEmeralds(double ebAmount) {
        long eb = Math.round(ebAmount);
        if (eb <= 0) return "0eb";
        long stx = eb / 4096;
        long le = (eb % 4096) / 64;
        long remainder = eb % 64;

        StringBuilder sb = new StringBuilder();
        if (stx > 0) sb.append(String.format("%,d", stx)).append("stx ");
        if (le > 0) sb.append(le).append("le ");
        if (remainder > 0 || sb.isEmpty()) sb.append(remainder).append("eb");
        return sb.toString().trim();
    }

    private long computeInputHash() {
        long hash = 0;
        for (TextInputWidget w : new TextInputWidget[]{fromLevelInput, toLevelInput, currentOverflowInput, overflowGoalInput, topNInput}) {
            if (w != null) hash = hash * 31 + w.getInput().hashCode();
        }
        for (int t = 0; t < 3; t++) {
            if (ingPriceInputs[t] != null) hash = hash * 31 + ingPriceInputs[t].getInput().hashCode();
            if (mat1PriceInputs[t] != null) hash = hash * 31 + mat1PriceInputs[t].getInput().hashCode();
            if (mat2PriceInputs[t] != null) hash = hash * 31 + mat2PriceInputs[t].getInput().hashCode();
        }
        return hash;
    }

    private void recalculate() {
        if (professionButton == null || recipeButton == null || materialTypeButton == null
                || xpMultButton == null || profSpeedButton == null || fromLevelInput == null
                || toLevelInput == null || currentOverflowInput == null || overflowGoalInput == null
                || ingPriceInputs[0] == null) return;

        int fromLevel = getFromLevel();
        int toLevel = getToLevel();
        MaterialType matType = getMaterialType();
        double bonusMult = getXpMultiplier(); // Prof speed does NOT affect XP
        int recipeLevel = matType.recipeLevel;
        double overflowGoal = getOverflowGoal();
        double currentOverflow = (fromLevel >= 132) ? getCurrentOverflow() : 0;
        double overflowNeeded = Math.max(0, overflowGoal - currentOverflow);

        // Persist goal to ProfessionOverlay so it shows on the HUD
        ProfessionType selectedProf = getSelectedProfession();
        if (selectedProf != null && fromLevel >= 132) {
            if (overflowGoal > 0) {
                ProfessionOverlay.setGoal(selectedProf, (float) overflowGoal);
            } else {
                ProfessionOverlay.clearGoal(selectedProf);
            }
        }

        int[] matRatios = getCurrentMatRatios();
        boolean profSpeed = hasProfSpeed();
        int matScale = profSpeed ? MAT_AMOUNT_SCALE : MAT_AMOUNT_SCALE * 2; // No speed = double materials

        // Parse prices in eb
        double[] ingPrices = new double[4];
        double[] mat1Prices = new double[4];
        double[] mat2Prices = new double[4];
        boolean[] ingPriceSet = new boolean[4];
        boolean[] mat1PriceSet = new boolean[4];
        boolean[] mat2PriceSet = new boolean[4];
        for (int t = 0; t < 3; t++) {
            ingPrices[t + 1] = getPrice(ingPriceInputs[t]);
            mat1Prices[t + 1] = getPrice(mat1PriceInputs[t]);
            mat2Prices[t + 1] = getPrice(mat2PriceInputs[t]);
            ingPriceSet[t + 1] = hasPriceSet(ingPriceInputs[t]);
            mat1PriceSet[t + 1] = hasPriceSet(mat1PriceInputs[t]);
            mat2PriceSet[t + 1] = hasPriceSet(mat2PriceInputs[t]);
        }

        int mat1Amt = matRatios[0] * matScale;
        int mat2Amt = matRatios[1] * matScale;

        tableRows = new TableRow[27];
        int rowIdx = 0;

        for (int ingTier = 1; ingTier <= 3; ingTier++) {
            double ingBase = CraftXpCalculator.computeIngBaseFullTier(recipeLevel, ingTier);

            for (int[] matCombo : MAT_COMBOS) {
                // Weighted material multiplier using recipe ratios
                double matMult = CraftXpCalculator.computeMatMult(matCombo[0], matCombo[1], matRatios[0], matRatios[1]);
                double xpPerCraft = CraftXpCalculator.computeXpPerCraft(ingBase, matMult, bonusMult, Math.min(fromLevel, 132), matType);

                int crafts;
                if (fromLevel >= 132) {
                    crafts = CraftXpCalculator.estimateCraftsForOverflow(overflowNeeded, ingBase, matMult, bonusMult, matType);
                } else if (toLevel <= 132 && overflowGoal <= 0) {
                    crafts = CraftXpCalculator.estimateCraftsToLevel(fromLevel, Math.max(fromLevel, toLevel), ingBase, matMult, bonusMult, matType);
                } else {
                    int craftsTo132 = CraftXpCalculator.estimateCraftsToLevel(fromLevel, 132, ingBase, matMult, bonusMult, matType);
                    int overflowCrafts = CraftXpCalculator.estimateCraftsForOverflow(overflowNeeded, ingBase, matMult, bonusMult, matType);
                    crafts = craftsTo132 + overflowCrafts;
                }

                // Cost in EB
                boolean allPricesSet = ingPriceSet[ingTier] && mat1PriceSet[matCombo[0]] && mat2PriceSet[matCombo[1]];
                double costPerCraft = allPricesSet
                        ? 6 * ingPrices[ingTier] + mat1Amt * mat1Prices[matCombo[0]] + mat2Amt * mat2Prices[matCombo[1]]
                        : -1;
                double totalCost = (allPricesSet && crafts < Integer.MAX_VALUE && crafts > 0) ? crafts * costPerCraft : -1;

                tableRows[rowIdx++] = new TableRow(ingTier, matCombo[0], matCombo[1], xpPerCraft, crafts, totalCost);
            }
        }
    }

    private List<Integer> getFilteredIndices() {
        if (tableRows == null) return List.of();

        int ingFilter = ingTierFilterButton.getSelectedIndex(); // 0=All, 1=T1, 2=T2, 3=T3
        boolean hasPrices = hasAnyPrice();

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < tableRows.length; i++) {
            if (tableRows[i] == null) continue;
            if (ingFilter > 0 && tableRows[i].ingTier != ingFilter) continue;
            indices.add(i);
        }

        // Sort by cost (if prices) or crafts
        indices.sort((a, b) -> {
            if (hasPrices) {
                double ca = tableRows[a].totalCost >= 0 ? tableRows[a].totalCost : Double.MAX_VALUE;
                double cb = tableRows[b].totalCost >= 0 ? tableRows[b].totalCost : Double.MAX_VALUE;
                return Double.compare(ca, cb);
            } else {
                return Integer.compare(tableRows[a].craftsNeeded, tableRows[b].craftsNeeded);
            }
        });

        int topN = getTopN();
        if (topN > 0 && topN < indices.size()) {
            indices = indices.subList(0, topN);
        }

        return indices;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (ui == null) return;

        int logicalW = getLogicalWidth();
        int centerX = logicalW / 2;
        int sy = -scrollOffset;

        long hash = computeInputHash();
        if (hash != lastInputHash) {
            lastInputHash = hash;
            recalculate();
        }

        // ── Title ──
        ui.drawCenteredText(WynnExtras.addWynnExtrasPrefix("\u00a76\u00a7lProfession XP Calculator"), centerX, 35 + sy);

        // ── Layout ──
        int btnGap = 12;
        int labelH = 18;

        // ── Row 1: Profession, Recipe, Material (3 buttons) ──
        int btnW3 = Math.min(300, (logicalW - 80) / 3);
        int btnH = 42;
        int row1Width = btnW3 * 3 + btnGap * 2;
        int row1X = (logicalW - row1Width) / 2;
        int row1Y = 72 + sy;

        professionButton.setBounds(row1X, row1Y, btnW3, btnH);
        recipeButton.setBounds(row1X + btnW3 + btnGap, row1Y, btnW3, btnH);
        materialTypeButton.setBounds(row1X + (btnW3 + btnGap) * 2, row1Y, btnW3, btnH);

        // ── Row 2: XP Bonus, Prof Speed, Ing Tier, Top N ──
        int btnW4 = Math.min(220, (logicalW - 100) / 4);
        int row2Width = btnW4 * 4 + btnGap * 3;
        int row2X = (logicalW - row2Width) / 2;
        int row2Y = row1Y + btnH + btnGap;

        xpMultButton.setBounds(row2X, row2Y, btnW4, btnH);
        profSpeedButton.setBounds(row2X + btnW4 + btnGap, row2Y, btnW4, btnH);
        ingTierFilterButton.setBounds(row2X + (btnW4 + btnGap) * 2, row2Y, btnW4, btnH);

        // Top N input
        int topNX = row2X + (btnW4 + btnGap) * 3;
        ui.drawText("\u00a77Top", topNX, row2Y - 1, CustomColor.fromHexString("AAAAAA"), 1.8f);
        topNInput.setBounds(topNX + 40, row2Y, btnW4 - 40, btnH);

        // ── Row 3: From [___] To [___] ──
        int inputW2 = Math.min(300, (logicalW - 100) / 2);
        int row3Width = inputW2 * 2 + btnGap;
        int row3X = (logicalW - row3Width) / 2;
        int row3Y = row2Y + btnH + btnGap + labelH;

        ui.drawText("\u00a77From Level", row3X, row3Y - labelH, CustomColor.fromHexString("AAAAAA"), 2f);
        ui.drawText("\u00a77To Level", row3X + inputW2 + btnGap, row3Y - labelH, CustomColor.fromHexString("AAAAAA"), 2f);
        fromLevelInput.setBounds(row3X, row3Y, inputW2, btnH);
        toLevelInput.setBounds(row3X + inputW2 + btnGap, row3Y, inputW2, btnH);

        // ── Row 4: Overflow ──
        int fromLevel = getFromLevel();
        boolean showCurrentOverflow = fromLevel >= 132;
        int row4Y = row3Y + btnH + btnGap + labelH;

        if (showCurrentOverflow) {
            ui.drawText("\u00a77Current Overflow", row3X, row4Y - labelH, CustomColor.fromHexString("AAAAAA"), 2f);
            ui.drawText("\u00a77Overflow Goal", row3X + inputW2 + btnGap, row4Y - labelH, CustomColor.fromHexString("AAAAAA"), 2f);
            currentOverflowInput.setBounds(row3X, row4Y, inputW2, btnH);
            overflowGoalInput.setBounds(row3X + inputW2 + btnGap, row4Y, inputW2, btnH);
        } else {
            currentOverflowInput.setBounds(-9999, -9999, 0, 0);
            int goalW = Math.min(400, logicalW - 100);
            int goalX = (logicalW - goalW) / 2;
            ui.drawText("\u00a77Overflow Goal", goalX, row4Y - labelH, CustomColor.fromHexString("AAAAAA"), 2f);
            overflowGoalInput.setBounds(goalX, row4Y, goalW, btnH);
        }

        // ── Prices ──
        String[] matNames = getCurrentMatNames();
        int[] matRatios = getCurrentMatRatios();
        boolean profSpeed = hasProfSpeed();
        int matScale = profSpeed ? MAT_AMOUNT_SCALE : MAT_AMOUNT_SCALE * 2;
        int mat1Amt = matRatios[0] * matScale;
        int mat2Amt = matRatios[1] * matScale;

        int priceInputW = Math.min(200, (logicalW - 140) / 3);
        int priceRowWidth = priceInputW * 3 + btnGap * 2;
        int priceX = (logicalW - priceRowWidth) / 2;
        int priceH = 36;

        String[] priceLabels = {
                "\u00a7eIngredient (\u00d76)",
                "\u00a7e" + matNames[0] + " (\u00d7" + mat1Amt + ")",
                "\u00a7e" + matNames[1] + " (\u00d7" + mat2Amt + ")"
        };
        TextInputWidget[][] priceRows = {ingPriceInputs, mat1PriceInputs, mat2PriceInputs};

        int priceStartY = row4Y + btnH + btnGap + 2;
        ui.drawCenteredText("\u00a77\u00a7lPrices \u00a78(per item, in eb)", centerX, priceStartY, CustomColor.fromHexString("AAAAAA"), 2.0f);
        priceStartY += 20;

        for (int row = 0; row < 3; row++) {
            int py = priceStartY + row * (priceH + btnGap + labelH);
            for (int t = 0; t < 3; t++) {
                int px = priceX + t * (priceInputW + btnGap);
                String label = priceLabels[row] + " T" + (t + 1);
                ui.drawText(label, px, py - labelH, CustomColor.fromHexString("AAAAAA"), 1.8f);
                priceRows[row][t].setBounds(px, py, priceInputW, priceH);
            }
        }

        // ── Info line ──
        int toLevel = getToLevel();
        double overflowGoal = getOverflowGoal();
        double currentOverflow = showCurrentOverflow ? getCurrentOverflow() : 0;

        int infoY = priceStartY + 3 * (priceH + btnGap + labelH) + 2;

        if (showCurrentOverflow && currentOverflow > 0 && overflowGoal > 0) {
            double remaining = Math.max(0, overflowGoal - currentOverflow);
            ui.drawCenteredText("\u00a77Remaining: \u00a7a" + formatXp(remaining) + " \u00a77overflow XP",
                    centerX, infoY, CustomColor.fromHexString("FFFFFF"), 2.0f);
            infoY += 20;
        }

        String modeText;
        if (fromLevel >= 132) {
            double remaining = Math.max(0, overflowGoal - currentOverflow);
            modeText = "\u00a77Mode: \u00a7eOverflow \u00a77(" + formatXp(remaining) + " XP)";
        } else {
            modeText = "\u00a77Mode: \u00a7eLv " + fromLevel + " \u00a77-> \u00a7eLv " + toLevel;
            if (overflowGoal > 0) {
                modeText += " \u00a77+ \u00a7e" + formatXp(overflowGoal) + " \u00a77overflow";
            }
        }
        ui.drawCenteredText(modeText, centerX, infoY);
        infoY += 22;

        // ── Table ──
        if (tableRows == null) return;

        List<Integer> filtered = getFilteredIndices();
        boolean hasPrices = hasAnyPrice();

        // Find best row among filtered
        int bestIdx = -1;
        if (!filtered.isEmpty()) {
            bestIdx = filtered.get(0); // already sorted, first is best
            // But only if it has valid data
            if (hasPrices && tableRows[bestIdx].totalCost < 0) bestIdx = -1;
            if (!hasPrices && tableRows[bestIdx].craftsNeeded >= Integer.MAX_VALUE) bestIdx = -1;
        }

        int tableTopY = infoY;
        int tablePadding = 16;
        int tableW = Math.min(1400, logicalW - 40);
        int tableX = (logicalW - tableW) / 2;

        float rowH = 22;
        int visibleRows = filtered.size();
        int tableH = (int) (visibleRows * rowH + 36);
        ui.drawRect(tableX, tableTopY - 6, tableW, tableH,
                CustomColor.fromHexString("222222").withAlpha(0.7f));

        // Column headers - use recipe material names
        String matsHeader = matNames[0] + " + " + matNames[1];
        String mat1Header = matNames[0] + " Req";
        String mat2Header = matNames[1] + " Req";

        float col1, col2, col3, col4, col5, col6, col7, col8;
        if (hasPrices) {
            col1 = tableX + tablePadding;
            col2 = tableX + tableW * 0.05f;
            col3 = tableX + tableW * 0.19f;
            col4 = tableX + tableW * 0.30f;
            col5 = tableX + tableW * 0.40f;
            col6 = tableX + tableW * 0.51f;
            col7 = tableX + tableW * 0.62f;
            col8 = tableX + tableW * 0.78f;
        } else {
            col1 = tableX + tablePadding;
            col2 = tableX + tableW * 0.06f;
            col3 = tableX + tableW * 0.22f;
            col4 = tableX + tableW * 0.36f;
            col5 = tableX + tableW * 0.50f;
            col6 = tableX + tableW * 0.66f;
            col7 = tableX + tableW * 0.82f;
            col8 = 0;
        }

        float headerY = tableTopY;
        float headerScale = 2.1f;
        CustomColor headerColor = CustomColor.fromHexString("FFD700");

        ui.drawText("Ing", col1, headerY, headerColor, headerScale);
        ui.drawText(matsHeader, col2, headerY, headerColor, headerScale);
        ui.drawText("XP/Craft", col3, headerY, headerColor, headerScale);
        ui.drawText("Crafts", col4, headerY, headerColor, headerScale);
        ui.drawText("Ing Req", col5, headerY, headerColor, headerScale);
        ui.drawText(mat1Header, col6, headerY, headerColor, headerScale);
        ui.drawText(mat2Header, col7, headerY, headerColor, headerScale);
        if (hasPrices) ui.drawText("Total Cost", col8, headerY, headerColor, headerScale);

        float sepY = headerY + 18;
        ui.drawRect(tableX + tablePadding - 5, sepY, tableW - tablePadding * 2 + 10, 1,
                CustomColor.fromHexString("FFD700").withAlpha(0.4f));

        CustomColor greenColor = CustomColor.fromHexString("55FF55");
        CustomColor[] tierColors = {null,
                CustomColor.fromHexString("55FF55"),
                CustomColor.fromHexString("FFFF55"),
                CustomColor.fromHexString("FF5555")};

        float dataScale = 2.0f;

        for (int vi = 0; vi < filtered.size(); vi++) {
            int i = filtered.get(vi);
            TableRow row = tableRows[i];

            float rowY = sepY + 5 + vi * rowH;
            boolean isBest = (i == bestIdx);

            if (vi % 2 == 0) {
                ui.drawRect(tableX + tablePadding - 8, rowY - 2,
                        tableW - tablePadding * 2 + 16, rowH,
                        CustomColor.fromHexString("FFFFFF").withAlpha(0.04f));
            }

            // Ing tier + Mats + XP/Craft
            if (isBest) {
                ui.drawText("\u00a7lT" + row.ingTier, col1, rowY, greenColor, dataScale);
                ui.drawText("T" + row.matTier1 + " + T" + row.matTier2, col2, rowY, greenColor, dataScale);
                ui.drawText(CraftXpCalculator.formatXp(row.xpPerCraft), col3, rowY, greenColor, dataScale);
            } else {
                ui.drawText("\u00a7lT" + row.ingTier, col1, rowY, tierColors[row.ingTier], dataScale);
                ui.drawText("T" + row.matTier1 + " + T" + row.matTier2, col2, rowY,
                        CustomColor.fromHexString("BBBBBB"), dataScale);
                ui.drawText(CraftXpCalculator.formatXp(row.xpPerCraft), col3, rowY,
                        CustomColor.fromHexString("55FFFF"), dataScale);
            }

            // Crafts + Requirements
            if (row.craftsNeeded >= Integer.MAX_VALUE || row.craftsNeeded == 0) {
                CustomColor dashColor = CustomColor.fromHexString("555555");
                ui.drawText("\u2014", col4, rowY, dashColor, dataScale);
                ui.drawText("\u2014", col5, rowY, dashColor, dataScale);
                ui.drawText("\u2014", col6, rowY, dashColor, dataScale);
                ui.drawText("\u2014", col7, rowY, dashColor, dataScale);
                if (hasPrices) ui.drawText("\u2014", col8, rowY, dashColor, dataScale);
            } else {
                String craftsStr = CraftXpCalculator.formatNumber(row.craftsNeeded);
                String ingReqStr = String.format("%,d", (long) row.craftsNeeded * 6);
                String mat1ReqStr = String.format("%,d", (long) row.craftsNeeded * mat1Amt);
                String mat2ReqStr = String.format("%,d", (long) row.craftsNeeded * mat2Amt);

                if (isBest) {
                    ui.drawText(craftsStr, col4, rowY, greenColor, dataScale);
                    ui.drawText(ingReqStr, col5, rowY, greenColor, dataScale);
                    ui.drawText(mat1ReqStr, col6, rowY, greenColor, dataScale);
                    ui.drawText(mat2ReqStr, col7, rowY, greenColor, dataScale);
                } else {
                    CustomColor craftsColor;
                    if (row.craftsNeeded < 1000) craftsColor = CustomColor.fromHexString("55FF55");
                    else if (row.craftsNeeded < 10000) craftsColor = CustomColor.fromHexString("FFFF55");
                    else craftsColor = CustomColor.fromHexString("FF5555");
                    ui.drawText(craftsStr, col4, rowY, craftsColor, dataScale);
                    ui.drawText(ingReqStr, col5, rowY, CustomColor.fromHexString("DDDDDD"), dataScale);
                    ui.drawText(mat1ReqStr, col6, rowY, CustomColor.fromHexString("DDDDDD"), dataScale);
                    ui.drawText(mat2ReqStr, col7, rowY, CustomColor.fromHexString("DDDDDD"), dataScale);
                }

                if (hasPrices) {
                    if (row.totalCost >= 0) {
                        ui.drawText(formatEmeralds(row.totalCost), col8, rowY,
                                isBest ? greenColor : CustomColor.fromHexString("FFAA00"), dataScale);
                    } else {
                        ui.drawText("\u2014", col8, rowY, CustomColor.fromHexString("555555"), dataScale);
                    }
                }
            }
        }

        float footerY = sepY + 5 + filtered.size() * rowH + 8;
        ui.drawCenteredText("\u00a78Scroll to see more  |  Prices in eb  |  Accepts K/M/B",
                centerX, footerY, CustomColor.fromHexString("666666"), 1.8f);

        contentHeight = (int)(footerY + 30 + scrollOffset);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int)(verticalAmount * 30);
        int maxScroll = Math.max(0, contentHeight - getLogicalHeight());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        return true;
    }

    private boolean hasAnyPrice() {
        for (int t = 0; t < 3; t++) {
            if (hasPriceSet(ingPriceInputs[t])) return true;
            if (hasPriceSet(mat1PriceInputs[t])) return true;
            if (hasPriceSet(mat2PriceInputs[t])) return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
