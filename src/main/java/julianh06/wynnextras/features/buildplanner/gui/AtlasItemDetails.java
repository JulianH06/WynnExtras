package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.Identification;
import julianh06.wynnextras.features.buildplanner.data.IdentificationUnits;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

final class AtlasItemDetails extends Widget {
    private static final float TOOLTIP_SCALE = 1.25F;
    private static final float HEADING_SCALE = 1.35F;
    private static final float TEXT_SCALE = 1.15F;
    private static final int VIEWPORT_MARGIN = 24;
    private static final int GAP = 16;
    private static final int COST_CARD_MIN_WIDTH = 190;
    private static final int COST_CARD_HEIGHT = 130;
    private static final int PROBABILITY_ROW_HEIGHT = 104;
    private static final int OVERALL_SCALE = 100;
    private static final String[] AMPLIFIER_LABELS = {"None", "I", "II", "III", "IV"};
    private final WynnItem item;
    private final WynnItemTooltip.Card tooltip;
    private final SmoothScroll scroll = new SmoothScroll();
    private final PlannerScrollbar scrollbar = new PlannerScrollbar(scroll, 14);
    private final ThemedButton closeButton;
    private final List<ProbabilityRow> probabilityRows = new ArrayList<>();
    private final List<AmplifierButton> amplifierButtons = new ArrayList<>();
    private OverallDistribution overallDistribution;
    private int amplifierTier;
    private int maxScroll;

    AtlasItemDetails(WynnItem item, int width, int height, boolean showLore, Runnable close) {
        super(0, 0, width, height);
        this.item = item;
        closeButton = new ThemedButton(0, 0, 110, 28, Text.literal("Close"), close)
                .setTextScale(TEXT_SCALE);
        tooltip = WynnItemTooltip.itemCard(MinecraftClient.getInstance().textRenderer, item, showLore);
        for (int tier = 0; tier <= 4; tier++) amplifierButtons.add(new AmplifierButton(tier));
        for (Map.Entry<String, Identification> entry : item.identifications().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            Identification identification = entry.getValue();
            if (identification.isRollable() && identification.raw() != 0) {
                probabilityRows.add(new ProbabilityRow(entry.getKey(), identification.raw()));
            }
        }
        rebuildDistributions();
    }

    void resize(int width, int height) {
        setBounds(0, 0, width, height);
    }

    void scroll(double amount) {
        scroll.move((float) -Math.signum(amount) * 24.0F, 0.0F, maxScroll);
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int viewportHeight = Math.max(1, height - VIEWPORT_MARGIN * 2);
        int contentWidth = Math.min(1500, width - VIEWPORT_MARGIN * 2);
        int contentLeft = (width - contentWidth) / 2;
        int tooltipWidth = tooltip.width(TOOLTIP_SCALE);
        int tooltipHeight = tooltip.height(TOOLTIP_SCALE);
        int rightLeft = contentLeft + tooltipWidth + GAP;
        int rightWidth = Math.max(320, contentLeft + contentWidth - rightLeft);
        List<IdentificationCost> costs = identificationCosts();
        int costColumns = Math.max(1, Math.min(Math.max(1, costs.size()),
                Math.max(1, (rightWidth - 14) / (COST_CARD_MIN_WIDTH + 10))));
        int costRows = Math.max(1, (costs.size() + costColumns - 1) / costColumns);
        int costsHeight = costs.isEmpty() ? 76 : 48 + costRows * (COST_CARD_HEIGHT + 10);
        List<OrderedText> dropDetails = wrappedDropDetails(tooltipWidth);
        int additionalHeight = 56 + dropDetails.size() * 18;
        int probabilityHeight = probabilityRows.isEmpty() ? 86
                : 142 + probabilityRows.size() * PROBABILITY_ROW_HEIGHT;
        int leftHeight = tooltipHeight + GAP + additionalHeight;
        int rightHeight = costsHeight + GAP + probabilityHeight;
        int closeY = height - VIEWPORT_MARGIN - 28;
        int leftViewportHeight = Math.max(1, closeY - GAP - VIEWPORT_MARGIN);

        maxScroll = Math.max(0, Math.max(leftHeight - leftViewportHeight, rightHeight - viewportHeight));
        scroll.clamp(0.0F, maxScroll);
        int contentTop = VIEWPORT_MARGIN - Math.round(scroll.update(delta));
        ui.fill(0, 0, width, height, 0xA8000000);

        enableViewportScissor();
        try {
            renderIdentificationCosts(context, rightLeft, contentTop, rightWidth, costsHeight,
                    costs, costColumns);
            renderProbabilities(context, rightLeft, contentTop + costsHeight + GAP,
                    rightWidth, probabilityHeight, mouseX, mouseY, delta);
        } finally {
            ui.disableScissor();
        }

        ui.enableScissor(VIEWPORT_MARGIN, VIEWPORT_MARGIN,
                Math.min(width - VIEWPORT_MARGIN, rightLeft - GAP + 4), closeY - GAP);
        try {
            tooltip.render(context, textRenderer(), contentLeft, contentTop, TOOLTIP_SCALE);
            renderAdditionalInfo(context, contentLeft, contentTop + tooltipHeight + GAP,
                    tooltipWidth, additionalHeight, dropDetails);
        } finally {
            ui.disableScissor();
        }

        closeButton.setBounds(contentLeft, closeY, Math.min(110, tooltipWidth), 28);
        closeButton.draw(context, mouseX, mouseY, delta, ui);
        scrollbar.configure(contentLeft + contentWidth + 8, VIEWPORT_MARGIN, 8, viewportHeight, maxScroll);
        scrollbar.draw(context, mouseX, mouseY, delta, ui);
    }

    private void renderAdditionalInfo(
            DrawContext context, int x, int y, int width, int height, List<OrderedText> dropDetails) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Additional Info", x + width / 2, y + 12);
        for (int i = 0; i < dropDetails.size(); i++) {
            text(dropDetails.get(i), x + 14, y + 43 + i * 18, UiTheme.TEXT);
        }
    }

    private List<OrderedText> wrappedDropDetails(int width) {
        int textWidth = Math.max(1, (int) ((width - 28) / TEXT_SCALE));
        List<OrderedText> lines = new ArrayList<>();
        for (String detail : dropDetails()) {
            lines.addAll(textRenderer().wrapLines(Text.literal(detail), textWidth));
        }
        return List.copyOf(lines);
    }

    private List<String> dropDetails() {
        java.util.Optional<WynnDataService.ItemData> source = sourceItem();
        if (source.isEmpty()) return List.of("Drop Type: No drop metadata found");
        WynnDataService.ItemData itemData = source.get();
        WynnDataService.DropMeta meta = itemData.dropMeta();
        String quest = itemData.requirements().getOrDefault("quest", "");
        String type = meta != null && !meta.types().isEmpty()
                ? meta.types().stream().map(AtlasItemDetails::readable).collect(java.util.stream.Collectors.joining(" / "))
                : !quest.isBlank() ? "quest"
                : "never".equalsIgnoreCase(itemData.dropRestriction()) ? "unknown"
                : itemData.dropRestriction();
        List<String> details = new ArrayList<>();
        details.add("Drop Type: " + (type.isBlank() ? "Unknown" : readable(type)));
        if (meta == null) {
            if (!quest.isBlank()) details.add("Quest: " + quest);
            return List.copyOf(details);
        }
        if (!meta.name().isBlank()) {
            String sourceLabel = meta.types().size() == 1 ? readable(meta.types().getFirst()) : "Source";
            details.add(sourceLabel + ": " + meta.name());
        }
        if (!meta.event().isBlank()) details.add("Event: " + readable(meta.event()));
        if (meta.coordinates().size() >= 3
                && meta.coordinates().stream().limit(3).noneMatch(coordinate -> Math.abs(coordinate) >= 99999)) {
            details.add("Coordinates: " + meta.coordinates().get(0) + ", " + meta.coordinates().get(1)
                    + ", " + meta.coordinates().get(2));
        }
        return List.copyOf(details);
    }

    private void renderIdentificationCosts(DrawContext context, int x, int y, int width, int height,
            List<IdentificationCost> costs, int columns) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Identification Costs", x + width / 2, y + 12);
        if (costs.isEmpty()) {
            text("This item has no identification cost", x + 14, y + 45, UiTheme.MUTED);
            return;
        }
        int cardWidth = (width - 24 - (columns - 1) * 10) / columns;
        for (int i = 0; i < costs.size(); i++) {
            IdentificationCost cost = costs.get(i);
            int cardX = x + 12 + i % columns * (cardWidth + 10);
            int cardY = y + 42 + i / columns * (COST_CARD_HEIGHT + 10);
            UiTheme.drawRoundedBox(context, ui, cardX, cardY, cardWidth, COST_CARD_HEIGHT,
                    UiTheme.SURFACE, UiTheme.BORDER);
            String title = cost.reroll() == 0 ? "Initial Roll" : "Reroll to [" + (cost.reroll() + 1) + "]";
            centeredText(title, cardX + cardWidth / 2, cardY + 8, UiTheme.TEXT);
            centeredText(formatNumber(cost.emeralds()) + " Emeralds",
                    cardX + cardWidth / 2, cardY + 27, UiTheme.TEXT);
            ui.fill(cardX + 10, cardY + 45, cardX + cardWidth - 10, cardY + 46, UiTheme.BORDER);
            int rowY = cardY + 53;
            EmeraldBreakdown breakdown = cost.breakdown();
            rowY = currencyLine(new ItemStack(Items.EXPERIENCE_BOTTLE), breakdown.stacks(),
                    "STX", cardX, cardWidth, rowY, true);
            rowY = currencyLine(new ItemStack(Items.EXPERIENCE_BOTTLE), breakdown.liquidEmeralds(),
                    "LE", cardX, cardWidth, rowY, false);
            rowY = currencyLine(new ItemStack(Items.EMERALD_BLOCK), breakdown.emeraldBlocks(),
                    "EB", cardX, cardWidth, rowY, false);
            currencyLine(new ItemStack(Items.EMERALD), breakdown.emeralds(),
                    "Emeralds", cardX, cardWidth, rowY, false);
        }
    }

    private int currencyLine(ItemStack icon, long amount, String label, int x, int width, int y,
            boolean stacked) {
        if (amount == 0) return y;
        if (stacked) {
            ui.drawItem(icon, x + 13, y + 2);
            ui.drawItem(icon, x + 16, y + 4);
        } else ui.drawItem(icon, x + 15, y + 3);
        text(formatNumber(amount), x + 39, y + 6, UiTheme.TEXT);
        text(label, x + width - 12, y + 6, UiTheme.MUTED, true);
        return y + 18;
    }

    private void renderProbabilities(DrawContext context, int x, int y, int width, int height,
            int mouseX, int mouseY, float delta) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Identification Probabilities", x + width / 2, y + 12);
        if (probabilityRows.isEmpty()) {
            text("This item has no rollable identifications", x + 14, y + 48, UiTheme.MUTED);
            return;
        }
        text("Corkian Amplifier:", x + 14, y + 66, UiTheme.TEXT);
        int buttonX = x + 170;
        for (AmplifierButton button : amplifierButtons) {
            int buttonWidth = button.tier == 0 ? 58 : 34;
            button.setBounds(buttonX, y + 58, buttonWidth, 25);
            button.setVisible(inViewport(button.getY(), button.getHeight()));
            button.draw(context, mouseX, mouseY, delta, ui);
            buttonX += buttonWidth + 6;
        }
        renderOverallProbabilities(context, x + width - 12, y + 43);
        int rowY = y + 137;
        for (ProbabilityRow row : probabilityRows) {
            renderProbabilityRow(context, row, x + 12, rowY, width - 24, mouseX, mouseY, delta);
            rowY += PROBABILITY_ROW_HEIGHT;
        }
    }

    private void renderProbabilityRow(DrawContext context, ProbabilityRow row, int x, int y, int width,
            int mouseX, int mouseY, float delta) {
        Distribution distribution = row.distribution;
        UiTheme.drawRoundedBox(context, ui, x, y, width, PROBABILITY_ROW_HEIGHT - 8,
                UiTheme.SURFACE, UiTheme.BORDER);
        CharacterClass spellClass = "weapon".equals(item.type())
                ? CharacterClass.fromWeaponSubtype(item.subType()) : CharacterClass.UNKNOWN;
        text(ItemInspection.identificationLabel(row.key, spellClass), x + 10, y + 8, UiTheme.TEXT);
        Formatting rollColor = distribution.positive ? Formatting.GREEN : Formatting.RED;
        MutableText baseAndRange = Text.literal("Base ").formatted(Formatting.WHITE)
                .append(Text.literal(IdentificationUnits.format(row.key, row.base)).formatted(rollColor))
                .append(Text.literal("  [").formatted(Formatting.WHITE))
                .append(Text.literal(IdentificationUnits.format(row.key, distribution.values.getFirst()))
                        .formatted(rollColor))
                .append(Text.literal(", ").formatted(Formatting.WHITE))
                .append(Text.literal(IdentificationUnits.format(row.key, distribution.values.getLast()))
                        .formatted(rollColor))
                .append(Text.literal("]").formatted(Formatting.WHITE));
        richText(baseAndRange, x + 10, y + 27);
        int selected = distribution.values.get(row.selectedIndex);
        int count = distribution.counts.getOrDefault(selected, 0);
        int better = 0;
        for (int i = row.selectedIndex; i < distribution.values.size(); i++) {
            better += distribution.counts.getOrDefault(distribution.values.get(i), 0);
        }
        richText(probabilityText("Roll exactly ", row, selected,
                count * 100.0D / distribution.totalRolls), x + 10, y + 53);
        richText(probabilityText("Roll ", row, selected,
                better * 100.0D / distribution.totalRolls, " or better: "), x + 10, y + 72);
        int controlX = x + Math.max(280, width / 2);
        int controlWidth = Math.max(100, x + width - controlX - 12);
        row.slider.setBounds(controlX, y + 16, controlWidth, 18);
        row.input.setBounds(controlX + (controlWidth - 100) / 2, y + 47, 100, 26);
        boolean visible = inViewport(y, PROBABILITY_ROW_HEIGHT - 8);
        row.slider.setVisible(visible);
        row.input.setVisible(visible);
        if (!visible) return;
        row.slider.draw(context, mouseX, mouseY, delta, ui);
        row.input.draw(context, mouseX, mouseY, delta, ui);
        enableViewportScissor();
    }

    private void renderOverallProbabilities(DrawContext context, int right, int y) {
        int boxWidth = 280;
        int boxHeight = 80;
        int x = right - boxWidth;
        UiTheme.drawRoundedBox(context, ui, x, y, boxWidth, boxHeight, UiTheme.SURFACE, UiTheme.BORDER);
        int selectedOverall = selectedOverall();
        double exactChance = overallDistribution.probabilities[selectedOverall] * 100.0D;
        double orBetterChance = 0.0D;
        for (int roll = selectedOverall; roll < overallDistribution.probabilities.length; roll++) {
            orBetterChance += overallDistribution.probabilities[roll] * 100.0D;
        }
        String overall = overallPercent(selectedOverall);
        centeredText("Overall Roll: " + overall, x + boxWidth / 2, y + 8, UiTheme.TEXT);
        richText(Text.literal("Roll exactly ").formatted(Formatting.WHITE)
                .append(Text.literal(overall).formatted(Formatting.GREEN))
                .append(Text.literal(": ").formatted(Formatting.WHITE))
                .append(Text.literal(readableProbability(exactChance)).formatted(Formatting.AQUA)),
                x + 12, y + 31);
        richText(Text.literal("Roll ").formatted(Formatting.WHITE)
                .append(Text.literal(overall).formatted(Formatting.GREEN))
                .append(Text.literal(" or better: ").formatted(Formatting.WHITE))
                .append(Text.literal(readableProbability(orBetterChance)).formatted(Formatting.AQUA)),
                x + 12, y + 52);
    }

    private int selectedOverall() {
        int sum = 0;
        for (ProbabilityRow row : probabilityRows) {
            sum += overallScore(row.distribution, row.selectedValue());
        }
        return Math.clamp((int) Math.round(sum / (double) probabilityRows.size()), 0, 100 * OVERALL_SCALE);
    }

    private static Text probabilityText(String prefix, ProbabilityRow row, int selected, double chance) {
        return probabilityText(prefix, row, selected, chance, ": ");
    }

    private static Text probabilityText(
            String prefix, ProbabilityRow row, int selected, double chance, String separator) {
        Formatting rollColor = row.distribution.positive ? Formatting.GREEN : Formatting.RED;
        return Text.literal(prefix).formatted(Formatting.WHITE)
                .append(Text.literal(IdentificationUnits.format(row.key, selected)).formatted(rollColor))
                .append(Text.literal(separator).formatted(Formatting.WHITE))
                .append(Text.literal(percent(chance)).formatted(Formatting.AQUA));
    }

    private void enableViewportScissor() {
        ui.enableScissor(VIEWPORT_MARGIN, VIEWPORT_MARGIN,
                width - VIEWPORT_MARGIN, height - VIEWPORT_MARGIN);
    }

    private boolean inViewport(int controlY, int controlHeight) {
        return controlY + controlHeight > VIEWPORT_MARGIN && controlY < height - VIEWPORT_MARGIN;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closeButton.mouseClicked(mouseX, mouseY, button)) return true;
        for (ProbabilityRow row : probabilityRows) {
            if (!row.input.contains((int) mouseX, (int) mouseY)) row.input.setFocused(false);
        }
        for (AmplifierButton amplifierButton : amplifierButtons) {
            if (amplifierButton.mouseClicked(mouseX, mouseY, button)) return true;
        }
        for (ProbabilityRow row : probabilityRows) {
            if (row.input.mouseClicked(mouseX, mouseY, button)
                    || row.slider.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return scrollbar.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (ProbabilityRow row : probabilityRows) {
            if (row.slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
                    || row.input.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (ProbabilityRow row : probabilityRows) {
            if (row.slider.mouseReleased(mouseX, mouseY, button)
                    || row.input.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return scrollbar.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            for (ProbabilityRow row : probabilityRows) {
                if (row.input.isFocused()) {
                    row.commitInput();
                    return true;
                }
            }
        }
        for (ProbabilityRow row : probabilityRows) {
            if (row.input.keyPressed(keyCode, scanCode, modifiers)
                    || row.slider.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (ProbabilityRow row : probabilityRows) {
            if (row.input.charTyped(chr, modifiers)) return true;
        }
        return false;
    }

    private void setAmplifierTier(int tier) {
        if (amplifierTier == tier) tier = 0;
        amplifierTier = tier;
        rebuildDistributions();
    }

    private void rebuildDistributions() {
        for (ProbabilityRow row : probabilityRows) {
            Integer previous = row.distribution == null ? null : row.selectedValue();
            row.distribution = distribution(row.key, row.base, amplifierTier);
            row.selectedIndex = previous == null ? row.distribution.values.size() - 1
                    : nearestIndex(row.distribution.values, previous);
            row.input.setInputAndMoveCursorToEnd(String.valueOf(row.selectedValue()));
        }
        overallDistribution = buildOverallDistribution();
    }

    private OverallDistribution buildOverallDistribution() {
        if (probabilityRows.isEmpty()) return new OverallDistribution(new double[]{1.0D});
        double[] sums = {1.0D};
        int maximumSum = 0;
        for (ProbabilityRow row : probabilityRows) {
            Map<Integer, Double> outcomes = new HashMap<>();
            for (int value : row.distribution.values) {
                int score = overallScore(row.distribution, value);
                double chance = row.distribution.counts.getOrDefault(value, 0)
                        / (double) row.distribution.totalRolls;
                outcomes.merge(score, chance, Double::sum);
            }
            double[] next = new double[maximumSum + 100 * OVERALL_SCALE + 1];
            for (int sum = 0; sum < sums.length; sum++) {
                if (sums[sum] == 0.0D) continue;
                for (Map.Entry<Integer, Double> outcome : outcomes.entrySet()) {
                    next[sum + outcome.getKey()] += sums[sum] * outcome.getValue();
                }
            }
            sums = next;
            maximumSum += 100 * OVERALL_SCALE;
        }
        double[] probabilities = new double[100 * OVERALL_SCALE + 1];
        for (int sum = 0; sum < sums.length; sum++) {
            if (sums[sum] == 0.0D) continue;
            int overall = Math.clamp((int) Math.round(sum / (double) probabilityRows.size()),
                    0, probabilities.length - 1);
            probabilities[overall] += sums[sum];
        }
        return new OverallDistribution(probabilities);
    }

    private static int overallScore(Distribution distribution, int value) {
        int worst = distribution.values.getFirst();
        int best = distribution.values.getLast();
        if (worst == best) return 100 * OVERALL_SCALE;
        return Math.clamp((int) Math.round((value - worst) * 100.0D * OVERALL_SCALE / (best - worst)),
                0, 100 * OVERALL_SCALE);
    }

    private static Distribution distribution(String key, int base, int amplifierTier) {
        boolean reversed = key.matches("(?i)(raw)?[1-4](st|nd|rd|th)SpellCost");
        boolean positive = (base > 0) != reversed;
        int firstRoll = positive ? 30 : 70;
        int total = 131 - firstRoll;
        Map<Integer, Integer> counts = new HashMap<>();
        for (int roll = firstRoll; roll <= 130; roll++) {
            double multiplier = roll / 100.0D;
            if (positive && amplifierTier > 0) {
                multiplier += (1.30D - multiplier) * (0.05D * amplifierTier);
                multiplier = Math.round(multiplier * 100.0D) / 100.0D;
            }
            counts.merge((int) Math.round(multiplier * base), 1, Integer::sum);
        }
        List<Integer> values = new ArrayList<>(counts.keySet());
        values.sort(reversed ? Comparator.reverseOrder() : Comparator.naturalOrder());
        return new Distribution(List.copyOf(values), Map.copyOf(counts), total, positive);
    }

    private static int nearestIndex(List<Integer> values, int target) {
        int nearest = 0;
        int distance = Integer.MAX_VALUE;
        for (int i = 0; i < values.size(); i++) {
            int nextDistance = Math.abs(values.get(i) - target);
            if (nextDistance < distance) {
                nearest = i;
                distance = nextDistance;
            }
        }
        return nearest;
    }

    private List<IdentificationCost> identificationCosts() {
        long initial = switch (item.tier().toLowerCase(Locale.ROOT)) {
            case "unique" -> Math.round(0.5D * item.stat("lvl") + 3);
            case "rare" -> Math.round(1.2D * item.stat("lvl") + 8);
            case "legendary" -> Math.round(4.5D * item.stat("lvl") + 12);
            case "fabled" -> Math.round(12D * item.stat("lvl") + 26);
            case "mythic" -> Math.round(18D * item.stat("lvl") + 90);
            case "set" -> Math.round(1.5D * item.stat("lvl") + 8);
            default -> -1;
        };
        if (initial <= 0) return List.of();
        List<IdentificationCost> costs = new ArrayList<>();
        long cost = initial;
        for (int reroll = 0; reroll < 12 && inventorySlots(cost) <= 28; reroll++) {
            costs.add(new IdentificationCost(reroll, cost, emeraldBreakdown(cost)));
            if (cost > Long.MAX_VALUE / 5) break;
            cost = initial * (long) Math.pow(5, reroll + 1);
        }
        return List.copyOf(costs);
    }

    private java.util.Optional<WynnDataService.ItemData> sourceItem() {
        WynnDataService service = WynnDataService.getInstance();
        java.util.Optional<WynnDataService.ItemData> exact = service.findByInternalName(item.displayName()).stream()
                .filter(candidate -> candidate.subType().equalsIgnoreCase(item.subType())).findFirst();
        if (exact.isPresent()) return exact;
        return service.findByDisplayName(item.displayName()).stream()
                .filter(candidate -> candidate.subType().equalsIgnoreCase(item.subType()))
                .filter(candidate -> candidate.tier().equalsIgnoreCase(item.tier())).findFirst();
    }

    private static int inventorySlots(long emeralds) {
        long stacks = emeralds / 262144;
        emeralds %= 262144;
        long le = emeralds / 4096;
        emeralds %= 4096;
        long eb = emeralds / 64;
        long e = emeralds % 64;
        return (int) (stacks + ceilingStacks(le) + ceilingStacks(eb) + ceilingStacks(e));
    }

    private static int ceilingStacks(long value) {
        return value == 0 ? 0 : (int) ((value + 63) / 64);
    }

    private static EmeraldBreakdown emeraldBreakdown(long emeralds) {
        long stacks = emeralds / 262144;
        emeralds %= 262144;
        long liquidEmeralds = emeralds / 4096;
        emeralds %= 4096;
        long emeraldBlocks = emeralds / 64;
        return new EmeraldBreakdown(stacks, liquidEmeralds, emeraldBlocks, emeralds % 64);
    }

    private void heading(String value, int centerX, int y) {
        ui.pushMatrix();
        ui.translate(centerX, y);
        ui.scale(HEADING_SCALE, HEADING_SCALE);
        ui.drawCenteredTextWithShadow(textRenderer(), Text.literal(value), 0, 0, UiTheme.TEXT);
        ui.popMatrix();
    }

    private void centeredText(String value, int centerX, int y, int color) {
        ui.pushMatrix();
        ui.translate(centerX, y);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        ui.drawCenteredTextWithShadow(textRenderer(), Text.literal(value), 0, 0, color);
        ui.popMatrix();
    }

    private void text(String value, int x, int y, int color) {
        text(value, x, y, color, false);
    }

    private void text(OrderedText value, int x, int y, int color) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        ui.drawTextWithShadow(textRenderer(), value, 0, 0, color);
        ui.popMatrix();
    }

    private void text(String value, int x, int y, int color, boolean rightAligned) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        int textX = rightAligned ? -textRenderer().getWidth(value) : 0;
        ui.drawTextWithShadow(textRenderer(), value, textX, 0, color);
        ui.popMatrix();
    }

    private void richText(Text value, int x, int y) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        ui.drawTextWithShadow(textRenderer(), value, 0, 0, UiTheme.TEXT);
        ui.popMatrix();
    }

    private static String readable(String value) {
        String spaced = value.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return spaced.isBlank() ? "" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String formatNumber(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.3f%%", value);
    }

    private static String overallPercent(int scaledValue) {
        return BigDecimal.valueOf(scaledValue, 2).stripTrailingZeros().toPlainString() + "%";
    }

    private static String readableProbability(double value) {
        if (value > 0.0D && value < 0.00000001D) return "<0.00000001%";
        return BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + "%";
    }

    private static net.minecraft.client.font.TextRenderer textRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private final class ProbabilityRow {
        private final String key;
        private final int base;
        private final PlannerSlider slider;
        private final RollInput input;
        private Distribution distribution;
        private int selectedIndex;

        private ProbabilityRow(String key, int base) {
            this.key = key;
            this.base = base;
            slider = new PlannerSlider(0, 0, 0, 0,
                    () -> selectedIndex, () -> distribution.values.size() - 1, this::select);
            input = new RollInput(this);
        }

        private int selectedValue() {
            return distribution.values.get(selectedIndex);
        }

        private void select(int index) {
            selectedIndex = Math.clamp(index, 0, distribution.values.size() - 1);
            input.setInputAndMoveCursorToEnd(String.valueOf(selectedValue()));
        }

        private void commitInput() {
            try {
                select(nearestIndex(distribution.values, Integer.parseInt(input.getInput())));
            } catch (NumberFormatException ignored) {
                input.setInputAndMoveCursorToEnd(String.valueOf(selectedValue()));
            }
        }
    }

    private final class RollInput extends TextInputWidget {
        private final ProbabilityRow row;

        private RollInput(ProbabilityRow row) {
            super(0, 0, 100, 26, 6, 8, TEXT_SCALE);
            this.row = row;
            setMaxLength(12);
            setCharacterFilter(character -> Character.isDigit(character) || character == '-');
            setBackgroundColor(CustomColor.fromInt(UiTheme.PANEL));
            setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
            setTextColor(CustomColor.fromInt(UiTheme.TEXT));
            setCursorColor(CustomColor.fromInt(UiTheme.TEXT));
            setCentered(true);
        }

        @Override
        protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                row.commitInput();
                return true;
            }
            return super.onKeyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void setFocused(boolean focused) {
            boolean wasFocused = isFocused();
            super.setFocused(focused);
            if (wasFocused && !focused) row.commitInput();
        }
    }

    private final class AmplifierButton extends Widget {
        private final int tier;

        private AmplifierButton(int tier) {
            this.tier = tier;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean selected = amplifierTier == tier;
            UiTheme.drawRoundedBox(context, ui, x, y, width, height,
                    selected ? UiTheme.hoverBackground() : hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                    selected ? UiTheme.TEXT : UiTheme.BORDER);
            centeredText(AMPLIFIER_LABELS[tier], x + width / 2, y + 7,
                    UiTheme.TEXT);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            setAmplifierTier(tier);
            return true;
        }
    }

    private record IdentificationCost(int reroll, long emeralds, EmeraldBreakdown breakdown) {}
    private record EmeraldBreakdown(long stacks, long liquidEmeralds, long emeraldBlocks, long emeralds) {}
    private record Distribution(
            List<Integer> values, Map<Integer, Integer> counts, int totalRolls, boolean positive) {}
    private record OverallDistribution(double[] probabilities) {}
}
