package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.Identification;
import julianh06.wynnextras.features.buildplanner.data.IdentificationUnits;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class AtlasItemDetails extends Widget {
    private static final float TOOLTIP_SCALE = 1.25F;
    private static final float HEADING_SCALE = 1.35F;
    private static final float TEXT_SCALE = 1.15F;
    private static final int VIEWPORT_MARGIN = 24;
    private static final int GAP = 16;
    private final WynnItem item;
    private final WynnItemTooltip.Card tooltip;
    private final SmoothScroll scroll = new SmoothScroll();
    private final PlannerScrollbar scrollbar = new PlannerScrollbar(scroll, 14);
    private int maxScroll;

    AtlasItemDetails(WynnItem item, int width, int height, boolean showLore) {
        super(0, 0, width, height);
        this.item = item;
        tooltip = WynnItemTooltip.itemCard(MinecraftClient.getInstance().textRenderer, item, showLore);
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
        int additionalHeight = 142;
        List<IdentificationCost> costs = identificationCosts();
        int costRows = Math.max(1, (costs.size() + 1) / 2);
        int costsHeight = 45 + costRows * 34;
        int topHeight = Math.max(tooltipHeight, additionalHeight + GAP + costsHeight);
        List<Probability> probabilities = probabilities();
        int probabilityHeight = 54 + Math.max(1, probabilities.size()) * 46;
        int contentHeight = topHeight + GAP + probabilityHeight;

        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scroll.clamp(0.0F, maxScroll);
        int offset = Math.round(scroll.update(delta));
        int contentTop = VIEWPORT_MARGIN - offset;

        ui.fill(0, 0, width, height, 0xA8000000);
        ui.enableScissor(VIEWPORT_MARGIN, VIEWPORT_MARGIN,
                width - VIEWPORT_MARGIN, height - VIEWPORT_MARGIN);
        try {
            tooltip.render(context, textRenderer(), contentLeft, contentTop, TOOLTIP_SCALE);
            renderAdditionalInfo(context, rightLeft, contentTop, rightWidth, additionalHeight);
            renderIdentificationCosts(context, rightLeft, contentTop + additionalHeight + GAP,
                    rightWidth, costsHeight, costs);
            renderProbabilities(context, contentLeft, contentTop + topHeight + GAP,
                    contentWidth, probabilityHeight, probabilities);
        } finally {
            ui.disableScissor();
        }
        scrollbar.configure(width - 15, VIEWPORT_MARGIN, viewportHeight, maxScroll);
        scrollbar.draw(context, mouseX, mouseY, delta, ui);
    }

    private void renderAdditionalInfo(DrawContext context, int x, int y, int width, int height) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Additional Info", x + width / 2, y + 12);
        List<String> rows = new ArrayList<>();
        String drop = sourceItem().map(WynnDataService.ItemData::dropRestriction).orElse("");
        rows.add("Drop Type: " + (drop.isBlank() ? "No drop metadata found" : readable(drop)));
        rows.add("Item Type: " + readable(item.tier()) + " " + readable(item.subType()));
        rows.add("Combat Level: " + item.stat("lvl"));
        rows.add("Powder Slots: " + item.stat("powderSlots"));
        if (!item.restriction().isBlank() && !item.restriction().equalsIgnoreCase("none")) {
            rows.add("Restriction: " + readable(item.restriction()));
        }
        for (int i = 0; i < rows.size(); i++) text(rows.get(i), x + 14, y + 42 + i * 18, UiTheme.TEXT);
    }

    private void renderIdentificationCosts(DrawContext context, int x, int y, int width, int height,
            List<IdentificationCost> costs) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Identification Costs", x + width / 2, y + 12);
        if (costs.isEmpty()) {
            text("This item has no identification cost.", x + 14, y + 43, UiTheme.MUTED);
            return;
        }
        int columnWidth = (width - 36) / 2;
        for (int i = 0; i < costs.size(); i++) {
            IdentificationCost cost = costs.get(i);
            int cellX = x + 12 + i % 2 * (columnWidth + 12);
            int cellY = y + 39 + i / 2 * 34;
            UiTheme.drawRoundedBox(context, ui, cellX, cellY, columnWidth, 28,
                    UiTheme.SURFACE, UiTheme.BORDER);
            String title = cost.reroll() == 0 ? "Initial ID" : "Reroll " + cost.reroll();
            text(title + ": " + formatNumber(cost.emeralds()) + " E", cellX + 8, cellY + 5, UiTheme.TEXT);
            text(cost.breakdown(), cellX + columnWidth - 8, cellY + 5, UiTheme.MUTED, true);
        }
    }

    private void renderProbabilities(DrawContext context, int x, int y, int width, int height,
            List<Probability> probabilities) {
        UiTheme.drawPanel(context, ui, x, y, width, height);
        heading("Identification Probabilities", x + width / 2, y + 12);
        if (probabilities.isEmpty()) {
            text("This item has no rollable identifications.", x + 14, y + 48, UiTheme.MUTED);
            return;
        }
        for (int i = 0; i < probabilities.size(); i++) {
            Probability probability = probabilities.get(i);
            int rowY = y + 43 + i * 46;
            UiTheme.drawRoundedBox(context, ui, x + 12, rowY, width - 24, 40,
                    UiTheme.SURFACE, UiTheme.BORDER);
            String base = "Base " + IdentificationUnits.format(probability.key(), probability.base());
            String range = "[" + IdentificationUnits.format(probability.key(), probability.worst())
                    + ", " + IdentificationUnits.format(probability.key(), probability.best()) + "]";
            CharacterClass spellClass = "weapon".equals(item.type())
                    ? CharacterClass.fromWeaponSubtype(item.subType()) : CharacterClass.UNKNOWN;
            text(ItemInspection.identificationLabel(probability.key(), spellClass),
                    x + 22, rowY + 5, UiTheme.TEXT);
            text(base + "  " + range, x + width / 2, rowY + 5, UiTheme.accent(), true);
            String best = IdentificationUnits.format(probability.key(), probability.best());
            text("Roll exactly " + best + ": " + percent(probability.exactBest()),
                    x + 22, rowY + 22, UiTheme.MUTED);
            text("Roll " + best + " or better: " + percent(probability.bestOrBetter()),
                    x + width - 22, rowY + 22, UiTheme.MUTED, true);
        }
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

    private List<Probability> probabilities() {
        List<Probability> rows = new ArrayList<>();
        item.identifications().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Identification id = entry.getValue();
            if (!id.isRollable() || id.raw() == 0) return;
            boolean reversed = entry.getKey().matches("(?i)(raw)?[1-4](st|nd|rd|th)SpellCost");
            boolean positive = (id.raw() > 0) != reversed;
            int firstRoll = positive ? 30 : 70;
            int total = 131 - firstRoll;
            Map<Integer, Integer> counts = new java.util.HashMap<>();
            for (int roll = firstRoll; roll <= 130; roll++) {
                int value = Math.round(id.raw() * roll / 100.0F);
                counts.merge(value, 1, Integer::sum);
            }
            int worst = reversed ? counts.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)
                    : counts.keySet().stream().mapToInt(Integer::intValue).min().orElse(0);
            int best = reversed ? counts.keySet().stream().mapToInt(Integer::intValue).min().orElse(0)
                    : counts.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            double exact = counts.getOrDefault(best, 0) * 100.0D / total;
            rows.add(new Probability(entry.getKey(), id.raw(), worst, best, exact, exact));
        });
        return List.copyOf(rows);
    }

    private java.util.Optional<WynnDataService.ItemData> sourceItem() {
        return WynnDataService.getInstance().findByDisplayName(item.displayName()).stream()
                .filter(candidate -> candidate.subType().equalsIgnoreCase(item.subType()))
                .findFirst();
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

    private static String emeraldBreakdown(long emeralds) {
        long stacks = emeralds / 262144;
        emeralds %= 262144;
        long le = emeralds / 4096;
        emeralds %= 4096;
        long eb = emeralds / 64;
        long e = emeralds % 64;
        List<String> parts = new ArrayList<>();
        if (stacks != 0) parts.add(stacks + " stx");
        if (le != 0) parts.add(le + " LE");
        if (eb != 0) parts.add(eb + " EB");
        if (e != 0) parts.add(e + " E");
        return String.join("  ", parts);
    }

    private void heading(String value, int centerX, int y) {
        ui.pushMatrix();
        ui.translate(centerX, y);
        ui.scale(HEADING_SCALE, HEADING_SCALE);
        ui.drawCenteredTextWithShadow(textRenderer(), Text.literal(value), 0, 0, UiTheme.accent());
        ui.popMatrix();
    }

    private void text(String value, int x, int y, int color) {
        text(value, x, y, color, false);
    }

    private void text(String value, int x, int y, int color, boolean rightAligned) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        int textX = rightAligned ? -textRenderer().getWidth(value) : 0;
        ui.drawTextWithShadow(textRenderer(), value, textX, 0, color);
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

    private static net.minecraft.client.font.TextRenderer textRenderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private record IdentificationCost(int reroll, long emeralds, String breakdown) {}
    private record Probability(String key, int base, int worst, int best,
            double exactBest, double bestOrBetter) {}
}
