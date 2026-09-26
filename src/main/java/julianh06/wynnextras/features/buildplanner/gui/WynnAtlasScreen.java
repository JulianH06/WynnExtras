package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import julianh06.wynnextras.features.buildplanner.data.AtlasPageLayout;
import julianh06.wynnextras.features.buildplanner.data.AtlasSearch;
import julianh06.wynnextras.features.buildplanner.data.AtlasState;
import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

final class WynnAtlasScreen extends Screen {
    private static final int BODY_TOP = 67;
    private static final List<String> PROFESSIONS = List.of("ARMOURING", "TAILORING", "WEAPONSMITHING",
            "WOODWORKING", "JEWELING", "COOKING", "ALCHEMISM", "SCRIBING");
    private static final List<String> ITEM_TYPES = List.of("helmet", "chestplate", "leggings", "boots",
            "ring", "bracelet", "necklace", "bow", "spear", "wand", "dagger", "relik");
    private static final List<String> RARITIES = List.of("normal", "unique", "rare", "legendary", "fabled", "mythic", "set");
    private final Screen parent;
    private final String tabId;
    private final ToolWorkspaceManager workspace = ToolWorkspaceManager.getInstance();
    private final ItemDatabase database = ItemDatabase.getInstance();
    private final List<AtlasSearch.Entry> ingredients = AtlasSearch.ingredients(CraftedItemCodec.ingredientChoices());
    private List<AtlasSearch.Entry> items = List.of();
    private List<AtlasSearch.Entry> results = List.of();
    private final List<Card> cards = new ArrayList<>();
    private final Map<Integer, Card> cardCache = new java.util.HashMap<>();
    private AtlasPageLayout.ResultGrid resultGrid;
    private final List<Control> controls = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<ThemedButton> fixed = new ArrayList<>();
    private final SmoothScroll scroll = new SmoothScroll(18);
    private AtlasState state;
    private AtlasPageLayout layout;
    private long revision = -1;
    private int ticks;
    private int maxScroll;
    private int offset;
    private String error = "";
    private String notice = "";
    private ThemedButton copy;
    private ThemedButton use;
    private Card detailCard;
    private final SmoothScroll detailScroll = new SmoothScroll(18);
    private int detailMaxScroll;

    WynnAtlasScreen(Screen parent, String tabId) {
        super(Text.literal("WynnAtlas"));
        this.parent = parent;
        this.tabId = tabId;
        state = workspace.find(tabId).atlas();
    }

    @Override
    protected void init() {
        clearChildren();
        controls.clear();
        labels.clear();
        icons.clear();
        fixed.clear();
        detailCard = null;
        reloadCatalog();
        WorkspaceTabs.add(this, parent, tabId, 12, 10, width - 24, this::fixed);
        fixed(new ThemedButton(14, 38, 115, 20,
                Text.literal(state.ingredients() ? "Item Searching" : "Ingredient Searching"),
                () -> { state = AtlasState.defaults(!state.ingredients()); rebuild(true); }));
        fixed(new ThemedButton(width - 130, 38, 116, 20, Text.literal("WynnAtlas"), () -> scroll.jump(0)));
        int leftHeight = state.ingredients() ? 278 : 204;
        var preliminary = AtlasPageLayout.arrange(width, leftHeight, 0);
        int rowHeight = preliminary.columnWidth() >= 360 ? 25 : 46;
        int filtersHeight = 88 + state.filters().size() * rowHeight + state.excluded().size() * 25
                + (state.ingredients() ? 0 : 52 + state.strings().size() * 46);
        layout = AtlasPageLayout.arrange(width, leftHeight, filtersHeight);
        int w = layout.columnWidth();
        label(0, 0, "Name:");
        field(0, 15, w, state.query(), state.ingredients() ? "Ingredient name (case insensitive)" : "Item name or effect",
                value -> updateInputs(value, state.category(), state.rarity(), state.filters(), state.excluded(), state.strings()));
        List<String> types = state.ingredients() ? PROFESSIONS : ITEM_TYPES;
        selectionHeader("Types:", 44, false);
        if (state.ingredients()) {
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                button(0, 60 + i * 19, w, 18,
                        Text.literal((selected(state.category(), type) ? "[x] " : "[ ] ") + display(type)),
                        () -> toggle(false, type));
            }
        } else {
            int cell = w / 6;
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                int x = (i % 6) * cell;
                int y = 62 + (i / 6) * 29;
                var button = button(x, y, cell - 4, 25, Text.empty(), () -> toggle(false, type));
                button.setTooltip(Tooltip.of(Text.literal(display(type)
                        + (selected(state.category(), type) ? " (selected)" : " (not selected)"))));
                icons.add(new Icon(type, x, y, cell - 4, selected(state.category(), type)));
            }
        }
        int rarityY = state.ingredients() ? 221 : 128;
        selectionHeader(state.ingredients() ? "Stars:" : "Rarity:", rarityY, true);
        List<String> rarities = rarities();
        int cell = Math.min(62, w / rarities.size());
        for (int i = 0; i < rarities.size(); i++) {
            String rarity = rarities.get(i);
            int color = state.ingredients() ? 0xFFFFFF55 : ToolItemCards.color(rarity);
            String name = state.ingredients() ? rarity : display(rarity);
            Text text = fitted(name, cell - 3).copy().styled(style -> style.withColor(color)
                    .withUnderline(selected(state.rarity(), rarity)));
            var button = button(i * cell, rarityY + 16, cell - 3, 20, text, () -> toggle(true, rarity));
            button.setTooltip(Tooltip.of(Text.literal(name + (selected(state.rarity(), rarity) ? " (selected)" : " (not selected)"))));
        }
        button(0, leftHeight - 20, 70, 20, Text.literal("Search!"), () -> {
            refreshResults();
            save();
            scroll.jump(Math.min(layout.resultsY(), maxScroll));
        });
        button(78, leftHeight - 20, 62, 20, Text.literal("Reset"), () -> {
            state = AtlasState.defaults(state.ingredients());
            rebuild(true);
        });
        buildFilters(rowHeight);
        int buttonWidth = (width - 38) / 3;
        int y = height - 29;
        fixed(new ThemedButton(14, y, buttonWidth, 20, Text.literal("Back"), this::close));
        copy = fixed(new ThemedButton(19 + buttonWidth, y, buttonWidth, 20, Text.literal("Copy"), () -> {
            var selected = selectedEntry();
            if (selected != null) {
                client.keyboard.setClipboard(selected.name());
                notice = "Name copied.";
            }
        }));
        use = fixed(new ThemedButton(24 + 2 * buttonWidth, y, buttonWidth, 20, Text.literal("Use"), this::useSelected));
        use.setTooltip(Tooltip.of(Text.literal("Open in a NEW build or craft. Existing tabs are preserved.")));
        refreshResults();
        positionControls();
    }

    private void buildFilters(int rowHeight) {
        int x = layout.filtersX();
        int y = layout.filtersY();
        int w = layout.columnWidth();
        label(x, y, "Filters:");
        y += 16;
        for (int i = 0; i < state.filters().size(); i++) {
            int index = i;
            var filter = state.filters().get(i);
            boolean wide = rowHeight == 25;
            int selectorWidth = wide ? w - 182 : w - 50;
            button(x, y, selectorWidth, 20, fitted(AtlasSearch.label(filter.key()), selectorWidth),
                    () -> chooseNumeric(key -> {
                        var list = new ArrayList<>(state.filters());
                        list.set(index, new AtlasState.NumericFilter(key, filter.minimum(), filter.maximum(), filter.descending()));
                        updateLists(list, state.excluded(), state.strings());
                    }));
            button(x + selectorWidth + 3, y, 21, 20, Text.literal(filter.descending() ? "v" : "^"), () -> {
                var list = new ArrayList<>(state.filters());
                list.set(index, new AtlasState.NumericFilter(filter.key(), filter.minimum(), filter.maximum(), !filter.descending()));
                updateLists(list, state.excluded(), state.strings());
            }).setTooltip(Tooltip.of(Text.literal("Sort this stat ascending/descending; rows are sort priorities")));
            button(x + selectorWidth + 27, y, 20, 20, Text.literal("^"), () -> {
                if (index == 0) return;
                var list = new ArrayList<>(state.filters());
                java.util.Collections.swap(list, index, index - 1);
                updateLists(list, state.excluded(), state.strings());
            }).setTooltip(Tooltip.of(Text.literal("Move this filter up in the sorting order")));
            int bx = wide ? x + selectorWidth + 50 : x;
            int by = wide ? y : y + 23;
            field(bx, by, 43, filter.minimum(), "-inf", value -> changeBound(index, value, true));
            label(bx + 47, by + 6, "to");
            field(bx + 64, by, 43, filter.maximum(), "+inf", value -> changeBound(index, value, false));
            button(x + w - 20, by, 20, 20, Text.literal("X"), () -> {
                var list = new ArrayList<>(state.filters());
                list.remove(index);
                updateLists(list, state.excluded(), state.strings());
            });
            y += rowHeight;
        }
        button(x, y, Math.min(w, 150), 20, Text.literal("+ Add Filter"), () -> chooseNumeric(key -> {
            var list = new ArrayList<>(state.filters());
            list.add(new AtlasState.NumericFilter(key));
            updateLists(list, state.excluded(), state.strings());
        }));
        y += 36;
        label(x, y, "Excluded Filters:");
        y += 16;
        for (int i = 0; i < state.excluded().size(); i++) {
            int index = i;
            String key = state.excluded().get(i);
            button(x, y, w - 25, 20, fitted(AtlasSearch.label(key), w - 25), () -> chooseNumeric(chosen -> {
                var list = new ArrayList<>(state.excluded());
                list.set(index, chosen);
                updateLists(state.filters(), list, state.strings());
            }));
            button(x + w - 20, y, 20, 20, Text.literal("X"), () -> {
                var list = new ArrayList<>(state.excluded());
                list.remove(index);
                updateLists(state.filters(), list, state.strings());
            });
            y += 25;
        }
        button(x, y, Math.min(w, 170), 20, Text.literal("+ Add Excluded Filter"), () -> chooseNumeric(key -> {
            var list = new ArrayList<>(state.excluded());
            if (!list.contains(key)) list.add(key);
            updateLists(state.filters(), list, state.strings());
        })).setTooltip(Tooltip.of(Text.literal("Exclude results with a nonzero value for this stat")));
        if (state.ingredients()) return;
        y += 36;
        label(x, y, "String Filters:");
        y += 16;
        for (int i = 0; i < state.strings().size(); i++) {
            int index = i;
            var filter = state.strings().get(i);
            button(x, y, w - 25, 20, fitted(AtlasSearch.label(filter.key()), w - 25),
                    () -> chooseString(key -> {
                        var list = new ArrayList<>(state.strings());
                        list.set(index, new AtlasState.StringFilter(key, filter.value()));
                        updateLists(state.filters(), state.excluded(), list);
                    }));
            button(x + w - 20, y, 20, 20, Text.literal("X"), () -> {
                var list = new ArrayList<>(state.strings());
                list.remove(index);
                updateLists(state.filters(), state.excluded(), list);
            });
            field(x, y + 23, w, filter.value(), "Contains (case insensitive)", value -> {
                var list = new ArrayList<>(state.strings());
                list.set(index, new AtlasState.StringFilter(filter.key(), value));
                updateInputs(state.query(), state.category(), state.rarity(), state.filters(), state.excluded(), list);
            });
            y += 46;
        }
        button(x, y, Math.min(w, 150), 20, Text.literal("+ Add String Filter"), () -> chooseString(key -> {
            var list = new ArrayList<>(state.strings());
            list.add(new AtlasState.StringFilter(key, ""));
            updateLists(state.filters(), state.excluded(), list);
        }));
    }

    private void changeBound(int index, String value, boolean minimum) {
        var list = new ArrayList<>(state.filters());
        var filter = list.get(index);
        list.set(index, new AtlasState.NumericFilter(filter.key(), minimum ? value : filter.minimum(),
                minimum ? filter.maximum() : value, filter.descending()));
        updateInputs(state.query(), state.category(), state.rarity(), list, state.excluded(), state.strings());
    }

    private void chooseNumeric(Consumer<String> selected) {
        choose("Numeric stat (maximum identification roll)", AtlasSearch.numericKeys(catalog()), selected);
    }

    private void chooseString(Consumer<String> selected) {
        choose("String field", List.of("name", "lore", "majorId", "restriction", "attackSpeed"), selected);
    }

    private void choose(String title, List<String> keys, Consumer<String> selected) {
        List<CraftingChoiceScreen.Choice> choices = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) choices.add(new CraftingChoiceScreen.Choice(i, AtlasSearch.label(keys.get(i)), keys.get(i)));
        client.setScreen(new CraftingChoiceScreen(this, title, choices, choice -> selected.accept(keys.get(choice.id())), false));
    }

    private void selectionHeader(String title, int y, boolean rarity) {
        int w = layout.columnWidth();
        label(0, y + 4, title);
        button(w - 78, y - 2, 34, 18, Text.literal("All"), () -> setSelection(rarity, ""));
        var none = button(w - 40, y - 2, 40, 18, Text.literal("None"), () -> setSelection(rarity, "~none"));
        if (rarity && !state.ingredients()) {
            none.setTooltip(Tooltip.of(Text.literal("Clear rarity selection. No selected rarities shows all rarities.")));
        }
    }

    private List<String> rarities() { return state.ingredients() ? List.of("0", "1", "2", "3") : RARITIES; }
    private boolean selected(String selection, String key) { return AtlasSearch.matchesSelection(selection, key); }

    private void toggle(boolean rarity, String key) {
        String current = rarity ? state.rarity() : state.category();
        List<String> options = rarity ? rarities() : state.ingredients() ? PROFESSIONS : ITEM_TYPES;
        List<String> selected = new ArrayList<>(options.stream().filter(option -> selected(current, option)).toList());
        if (!selected.remove(key)) selected.add(key);
        setSelection(rarity, selected.isEmpty() ? "~none" : selected.size() == options.size() ? "" : String.join(",", selected));
    }

    private void setSelection(boolean rarity, String value) {
        updateInputs(state.query(), rarity ? state.category() : value, rarity ? value : state.rarity(),
                state.filters(), state.excluded(), state.strings());
        rebuild(false);
    }

    private void updateInputs(String query, String category, String rarity, List<AtlasState.NumericFilter> filters,
            List<String> excluded, List<AtlasState.StringFilter> strings) {
        state = new AtlasState(state.ingredients(), query, category, rarity, state.minLevel(), state.maxLevel(),
                "filters", false, 0, "", filters, excluded, strings);
        notice = "";
        refreshResults();
    }

    private void updateLists(List<AtlasState.NumericFilter> filters, List<String> excluded, List<AtlasState.StringFilter> strings) {
        updateInputs(state.query(), state.category(), state.rarity(), filters, excluded, strings);
        rebuild(false);
    }

    private void rebuild(boolean top) {
        save();
        if (top) scroll.jump(0);
        init();
    }

    private Text fitted(String value, int width) {
        return Text.literal(textRenderer.getWidth(value) <= width - 8 ? value
                : textRenderer.trimToWidth(value, Math.max(1, width - 20)) + "...");
    }

    private static String display(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private ThemedButton fixed(ThemedButton button) {
        fixed.add(button);
        return addDrawableChild(button);
    }

    private ThemedButton button(int x, int y, int w, int h, Text title, Runnable action) {
        var button = new ThemedButton(layout.left() + x, BODY_TOP + y, w, h, title, action);
        controls.add(new Control(addDrawableChild(button), y));
        return button;
    }

    private void field(int x, int y, int w, String value, String hint, Consumer<String> change) {
        var field = new TextFieldWidget(textRenderer, layout.left() + x, BODY_TOP + y, w, 20, Text.literal(hint));
        field.setMaxLength(200);
        field.setPlaceholder(Text.literal(hint));
        field.setText(value);
        field.setTooltip(Tooltip.of(Text.literal(hint)));
        controls.add(new Control(addDrawableChild(field), y));
        field.setChangedListener(change);
    }

    private void label(int x, int y, String text) { labels.add(new Label(x, y, text)); }
    private List<AtlasSearch.Entry> catalog() { return state.ingredients() ? ingredients : items; }

    private void reloadCatalog() {
        long observed = database.revision();
        if (revision != observed) {
            items = AtlasSearch.items(database.allItems());
            revision = observed;
        }
    }

    private void refreshResults() {
        if (layout == null || copy == null || use == null) return;
        error = "";
        try { results = AtlasSearch.search(catalog(), state); }
        catch (IllegalArgumentException exception) { results = List.of(); error = exception.getMessage(); }
        if (state.page() != 0) state = state.withPage(0, state.selected());
        cards.clear();
        cardCache.clear();
        resultGrid = AtlasPageLayout.results(layout.width(), viewportHeight());
        maxScroll = Math.max(0, layout.resultsY() + 24 + resultGrid.height(results.size()) - viewportHeight());
        scroll.clamp(0, maxScroll);
        copy.active = selectedEntry() != null;
        use.active = selectedEntry() != null;
    }

    private void updateVisibleCards() {
        var range = resultGrid.visibleRange(results.size(), offset - layout.resultsY() - 20, viewportHeight());
        cardCache.keySet().removeIf(index -> index < range.start() || index >= range.end());
        cards.clear();
        for (int index = range.start(); index < range.end(); index++) {
            cards.add(cardCache.computeIfAbsent(index, this::createCard));
        }
    }

    private Card createCard(int index) {
        var entry = results.get(index);
        int cardWidth = resultGrid.cellWidth();
        var visual = entry.ingredient() == null
                ? ItemInspectionCard.embedded(textRenderer, Text.literal(entry.name()),
                        ItemInspection.rows(entry.item(), "", true), cardWidth, ToolItemCards.color(entry.rarity()), true)
                : ToolItemCards.ingredient(textRenderer, entry.ingredient(), cardWidth);
        return new Card(entry, visual,
                index % resultGrid.columns() * (cardWidth + AtlasPageLayout.RESULT_GAP),
                layout.resultsY() + 20 + index / resultGrid.columns() * (resultGrid.cellHeight() + AtlasPageLayout.RESULT_GAP),
                cardWidth, resultGrid.cellHeight());
    }

    private AtlasSearch.Entry selectedEntry() {
        return results.stream().filter(entry -> entry.id().equals(state.selected())).findFirst().orElse(null);
    }

    private void useSelected() {
        var selected = selectedEntry();
        if (selected == null) return;
        if (selected.item() != null) {
            var item = selected.item();
            String slot = item.type().equals("weapon") ? "WEAPON"
                    : item.subType().equals("ring") ? "RING_1" : item.subType().toUpperCase(Locale.ROOT);
            AbilityTreeClass abilityClass = item.type().equals("weapon")
                    ? AbilityTreeClass.fromWeaponSubtype(item.subType()) : AbilityTreeClass.ARCHER;
            var skills = BuildCalculator.optimizeSkillPoints(121,
                    List.of(new BuildCalculator.EquippedItem(item, "", item.type().equals("weapon"))));
            String id = workspace.add(ToolWorkspaceManager.Type.BUILDER);
            workspace.updateBuild(id, new SavedBuild(item.displayName(), abilityClass.apiName(), 121,
                    Map.of(slot, item.reference()), Map.of(), skills.assigned(), Map.of(), Map.of(), Map.of(), 0));
        } else {
            var ingredient = selected.ingredient();
            var recipe = CraftedItemCodec.recipeChoices().stream().filter(ingredient::supports)
                    .min(Comparator.comparingInt(entry -> Math.abs(entry.levelHigh() - Math.max(105, ingredient.level()))))
                    .orElse(null);
            if (recipe == null) { notice = "No compatible recipe is available for this ingredient."; return; }
            var craft = CraftedItemCodec.emptyCraft(recipe.type()).withRecipe(recipe.id()).withIngredient(0, ingredient.id());
            workspace.updateCraft(workspace.add(ToolWorkspaceManager.Type.CRAFTER), craft);
        }
        client.setScreen(WorkspaceTabs.open(parent));
    }

    private int viewportHeight() { return Math.max(1, height - 54 - BODY_TOP); }

    private void positionControls() {
        for (var control : controls) {
            var widget = control.widget();
            widget.setY(BODY_TOP + control.y() - offset);
            widget.visible = widget.getY() + widget.getHeight() > BODY_TOP && widget.getY() < height - 54;
            widget.active = widget.visible && widget.getY() >= BODY_TOP && widget.getY() + widget.getHeight() <= height - 54;
            if (!widget.active) widget.setFocused(false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, UiTheme.BACKGROUND);
        UiTheme.drawPanel(context, 6, 6, width - 12, height - 12);
        offset = Math.round(scroll.update());
        updateVisibleCards();
        positionControls();
        context.enableScissor(12, BODY_TOP, width - 12, height - 54);
        try {
            for (Label label : labels) context.drawTextWithShadow(textRenderer, label.text(), layout.left() + label.x(),
                    BODY_TOP + label.y() - offset, UiTheme.TEXT);
            context.drawTextWithShadow(textRenderer, results.size() + " results", layout.left(),
                    BODY_TOP + layout.resultsY() - offset, UiTheme.accent());
            for (Card card : cards) {
                int y = BODY_TOP + card.y() - offset;
                if (y + card.height() < BODY_TOP || y > height - 54) continue;
                int x = layout.left() + card.x();
                boolean clipped = card.visual().height() > card.height();
                int contentBottom = y + card.height() - (clipped ? 15 : 1);
                UiTheme.drawRoundedBox(context, x, y, card.width(), card.height(), UiTheme.SURFACE, UiTheme.BORDER);
                context.enableScissor(x + 1, y + 1, x + card.width() - 1, Math.max(y + 1, contentBottom));
                try {
                    renderCard(context, card, x, y);
                } finally { context.disableScissor(); }
                if (clipped && card.height() >= 24) {
                    context.drawCenteredTextWithShadow(textRenderer, fitted("Double-click for details", card.width() - 8),
                            x + card.width() / 2, y + card.height() - 11, UiTheme.MUTED);
                }
                if (card.entry().id().equals(state.selected())) {
                    context.fill(layout.left() + card.x(), y, layout.left() + card.x() + card.width(), y + 2, UiTheme.accent());
                }
            }
            super.render(context, mouseX, mouseY, delta);
            for (Icon icon : icons) {
                int x = layout.left() + icon.x();
                int y = BODY_TOP + icon.y() - offset;
                EquipmentSprites.draw(context, icon.type(), x + (icon.width() - 18) / 2, y + 3, 18);
                if (icon.selected()) context.fill(x + 3, y + 23, x + icon.width() - 3, y + 25, UiTheme.accent());
                else context.fill(x + 1, y + 1, x + icon.width() - 1, y + 24, 0x88000000);
            }
        } finally { context.disableScissor(); }
        for (var button : fixed) button.render(context, mouseX, mouseY, delta);
        if (maxScroll > 0) {
            int thumb = Math.max(14, viewportHeight() * viewportHeight() / (viewportHeight() + maxScroll));
            int y = BODY_TOP + Math.round(offset / (float) maxScroll * (viewportHeight() - thumb));
            context.fill(width - 9, BODY_TOP, width - 7, height - 54, UiTheme.BORDER);
            context.fill(width - 9, y, width - 7, y + thumb, UiTheme.accent());
        }
        String status = !workspace.error().isEmpty() ? workspace.error() : !error.isEmpty() ? error
                : !notice.isEmpty() ? notice : !state.ingredients() && !database.isReady()
                ? database.isLoading() ? "Loading item database..." : "Item data unavailable: " + database.getLastError()
                : results.size() + " results | Scroll for more. Types/tiers: OR. Filters: AND. IDs: maximum rolls.";
        context.drawCenteredTextWithShadow(textRenderer, fitted(status, width - 28), width / 2, height - 44, UiTheme.MUTED);
        if (detailCard != null) renderDetail(context);
    }

    private void renderCard(DrawContext context, Card card, int x, int y) {
        card.visual().render(context, textRenderer, x, y, (iconX, iconY, size) -> {
            if (card.entry().item() != null) {
                EquipmentSprites.draw(context, card.entry().item().subType(), iconX, iconY, size);
            }
        });
    }

    private void renderDetail(DrawContext context) {
        int visibleHeight = Math.min(height - 48, detailCard.visual().height());
        int left = (width - detailCard.width()) / 2;
        int top = (height - visibleHeight) / 2;
        detailMaxScroll = Math.max(0, detailCard.visual().height() - visibleHeight);
        detailScroll.clamp(0, detailMaxScroll);
        context.fill(0, 0, width, height, 0xC0000000);
        context.enableScissor(left, top, left + detailCard.width() + 2, top + visibleHeight);
        try {
            renderCard(context, detailCard, left, top - Math.round(detailScroll.update()));
        } finally { context.disableScissor(); }
        context.drawCenteredTextWithShadow(textRenderer, "Scroll to read | Click or Esc to close",
                width / 2, height - 16, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (detailCard != null) {
            detailCard = null;
            return true;
        }
        if (click.button() == 0 && click.y() >= BODY_TOP && click.y() < height - 54) {
            for (Card card : cards) {
                double x = click.x() - layout.left();
                double y = click.y() - BODY_TOP + offset;
                if (x >= card.x() && x < card.x() + card.width() && y >= card.y() && y < card.y() + card.height()) {
                    state = state.withPage(state.page(), card.entry().id());
                    copy.active = true;
                    use.active = true;
                    save();
                    if (doubled) {
                        setFocused(null);
                        detailCard = card;
                        detailScroll.jump(0);
                        detailMaxScroll = Math.max(0, card.visual().height() - (height - 48));
                    } else {
                        notice = "Double-click an item to open its full-size preview.";
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean keyPressed(KeyInput input) {
        if (detailCard != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) detailCard = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (detailCard != null) {
            detailScroll.move((float) -vertical * 30, 0, detailMaxScroll);
            return true;
        }
        if (WorkspaceTabs.scroll(parent, tabId, 12, 10, width - 24,
                x, y, horizontal, vertical)) return true;
        scroll.move((float) -vertical * 30, 0, maxScroll);
        return true;
    }
    private void save() { workspace.updateAtlas(tabId, state); }
    @Override public void tick() {
        super.tick();
        if (++ticks % 20 == 0 && revision != database.revision()) { reloadCatalog(); refreshResults(); }
        if (ticks % 40 == 0) save();
    }
    @Override public void removed() { save(); super.removed(); }
    @Override public void close() {
        if (detailCard != null) { detailCard = null; return; }
        client.setScreen(parent);
    }

    private record Card(AtlasSearch.Entry entry, ItemInspectionCard.Embedded visual,
            int x, int y, int width, int height) {}
    private record Control(ClickableWidget widget, int y) {}
    private record Label(int x, int y, String text) {}
    private record Icon(String type, int x, int y, int width, boolean selected) {}
}
