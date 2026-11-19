package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.AtlasPageLayout;
import julianh06.wynnextras.features.buildplanner.data.AtlasSearch;
import julianh06.wynnextras.features.buildplanner.data.AtlasState;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class AtlasScreen extends PlannerScreen {
    private static final int BODY_TOP = 104;
    private static final int BODY_BOTTOM = 48;
    private static final int CONTROL_HEIGHT = 28;
    private static final float CONTROL_TEXT_SCALE = 1.25F;
    private static final float LABEL_TEXT_SCALE = 1.25F;
    private static final float CARD_SCALE = 1.25F;
    private static final int MAX_CARDS_PER_ROW = 5;
    private static final long CARD_LOAD_BUDGET_NANOS = 3_000_000L;
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "WynnExtras-AtlasSearch");
        thread.setDaemon(true);
        return thread;
    });
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
    private final List<Control> controls = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<ThemedButton> fixed = new ArrayList<>();
    private final List<TextInputWidget> inputFields = new ArrayList<>();
    private final Map<String, WynnItemTooltip.Card> cardCache = new HashMap<>();
    private final SmoothScroll scroll = new SmoothScroll();
    private final PlannerScrollbar scrollbar = new PlannerScrollbar(scroll, 14);
    private AtlasState state;
    private AtlasPageLayout layout;
    private long revision = -1;
    private int ticks;
    private int maxScroll;
    private int offset;
    private int searchRequest;
    private int nextCardIndex;
    private int nextCardY;
    private String error = "";
    private boolean searching;
    private boolean cardsLoading;
    private TextInputWidget tabNameField;
    private AtlasItemDetails itemDetails;
    private boolean showLore;

    AtlasScreen(Screen parent, String tabId) {
        super(Text.literal("Atlas"));
        this.parent = parent;
        this.tabId = tabId;
        state = workspace.find(tabId).atlas();
    }

    @Override
    protected void init() {
        super.init();
        for (TextInputWidget field : inputFields) removeRootWidget(field);
        clearChildren();
        controls.clear();
        labels.clear();
        icons.clear();
        fixed.clear();
        inputFields.clear();
        reloadCatalog();
        WorkspaceTabs.add(parent, tabId, 8, 2, width - 16, this::fixed);
        tabNameField = input(188, 62, Math.max(120, width - 202), workspace.find(tabId).name(), "Atlas tab name", value -> {
            try { workspace.rename(tabId, value); }
            catch (IllegalArgumentException ignored) {}
        }, false);
        fixed(new ThemedButton(14, 62, 162, CONTROL_HEIGHT,
                Text.literal(state.ingredients() ? "Item Searching" : "Ingredient Searching"),
                () -> { state = AtlasState.defaults(!state.ingredients()); rebuild(true); })
                .setTextScale(CONTROL_TEXT_SCALE));
        int leftHeight = state.ingredients() ? 318 : 286;
        int rowHeight = 68;
        int filtersHeight = (state.ingredients() ? 130 : 200) + state.filters().size() * rowHeight
                + state.excluded().size() * 35 + state.strings().size() * 66;
        layout = AtlasPageLayout.arrange(width, leftHeight, filtersHeight);
        int w = layout.columnWidth();
        label(0, 0, "Name:");
        field(0, 22, w, state.query(), state.ingredients() ? "Ingredient name (case insensitive)" : "Item name or effect",
                value -> updateInputs(value, state.category(), state.rarity(), state.filters(), state.excluded(), state.strings()));
        List<String> types = state.ingredients() ? PROFESSIONS : ITEM_TYPES;
        selectionHeader("Types:", 64, false);
        if (state.ingredients()) {
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                int column = i % 2;
                int x = column * w / 2;
                int right = (column + 1) * w / 2;
                int cellWidth = right - x - (column == 1 ? 0 : 8);
                int y = 91 + i / 2 * 34;
                button(x, y, cellWidth, CONTROL_HEIGHT,
                        Text.literal((selected(state.category(), type) ? "[x] " : "[ ] ") + display(type)),
                        () -> toggle(false, type));
            }
        } else {
            for (int i = 0; i < types.size(); i++) {
                String type = types.get(i);
                int column = i % 6;
                int x = column * w / 6;
                int right = (column + 1) * w / 6;
                int cellWidth = right - x - (column == 5 ? 0 : 6);
                int y = 91 + (i / 6) * 44;
                var button = button(x, y, cellWidth, 38, Text.empty(), () -> toggle(false, type));
                button.setHoverText(Text.literal(display(type)));
                icons.add(new Icon(type, x, y, cellWidth, selected(state.category(), type)));
            }
        }
        int rarityY = state.ingredients() ? 232 : 184;
        selectionHeader(state.ingredients() ? "Stars:" : "Rarity:", rarityY, true);
        List<String> rarities = rarities();
        for (int i = 0; i < rarities.size(); i++) {
            String rarity = rarities.get(i);
            int x = i * w / rarities.size();
            int right = (i + 1) * w / rarities.size();
            int cellWidth = right - x - (i == rarities.size() - 1 ? 0 : 5);
            int color = state.ingredients() ? 0xFFFFFF55
                    : rarity.equals("normal") ? UiTheme.TEXT : ToolItemCards.color(rarity);
            String name = state.ingredients() ? rarity : display(rarity);
            int textColor = selected(state.rarity(), rarity) ? color : UiTheme.DISABLED;
            Text text = fitted(name, cellWidth, CONTROL_TEXT_SCALE).copy().styled(style -> style.withColor(textColor));
            button(x, rarityY + 27, cellWidth, CONTROL_HEIGHT, text, () -> toggle(true, rarity));
        }
        button(0, leftHeight - CONTROL_HEIGHT, 104, CONTROL_HEIGHT, Text.literal("Search"), () -> {
            refreshResults();
            save();
            scroll.jump(Math.min(layout.resultsY(), maxScroll));
        });
        button(112, leftHeight - CONTROL_HEIGHT, 92, CONTROL_HEIGHT, Text.literal("Reset"), () -> {
            state = AtlasState.defaults(state.ingredients());
            rebuild(true);
        });
        if (!state.ingredients()) {
            button(212, leftHeight - CONTROL_HEIGHT, 120, CONTROL_HEIGHT,
                    Text.literal("Item Lore: " + (showLore ? "On" : "Off")), () -> {
                        showLore = !showLore;
                        cardCache.clear();
                        rebuild(false);
                    });
        }
        buildFilters(rowHeight);
        int y = height - 36;
        fixed(new ThemedButton(width - 176, y, 162, 26, Text.literal("Close"), this::close)
                .setTextScale(CONTROL_TEXT_SCALE));
        refreshResults();
        positionControls();
    }

    private void buildFilters(int rowHeight) {
        int x = layout.filtersX();
        int y = layout.filtersY();
        int w = layout.columnWidth();
        label(x, y, "Filters:");
        y += 25;
        for (int i = 0; i < state.filters().size(); i++) {
            int index = i;
            var filter = state.filters().get(i);
            int selectorWidth = w - 78;
            button(x, y, selectorWidth, CONTROL_HEIGHT,
                    fitted(AtlasSearch.label(filter.key()), selectorWidth, CONTROL_TEXT_SCALE),
                    () -> chooseNumeric(key -> {
                        var list = new ArrayList<>(state.filters());
                        list.set(index, new AtlasState.NumericFilter(key, filter.minimum(), filter.maximum(), filter.descending()));
                        updateLists(list, state.excluded(), state.strings());
                    }));
            button(x + selectorWidth + 6, y, 32, CONTROL_HEIGHT,
                    Text.literal(filter.descending() ? "v" : "^"), () -> {
                var list = new ArrayList<>(state.filters());
                list.set(index, new AtlasState.NumericFilter(filter.key(), filter.minimum(), filter.maximum(), !filter.descending()));
                updateLists(list, state.excluded(), state.strings());
            }).setHoverText(Text.literal("Sort this stat ascending/descending; rows are sort priorities"));
            button(x + selectorWidth + 44, y, 34, CONTROL_HEIGHT, Text.literal("^"), () -> {
                if (index == 0) return;
                var list = new ArrayList<>(state.filters());
                java.util.Collections.swap(list, index, index - 1);
                updateLists(list, state.excluded(), state.strings());
            }).setHoverText(Text.literal("Move this filter up in the sorting order"));
            int by = y + 34;
            int boundWidth = Math.min(100, (w - 104) / 2);
            field(x, by, boundWidth, filter.minimum(), "-inf", value -> changeBound(index, value, true));
            label(x + boundWidth + 10, by + 8, "to");
            field(x + boundWidth + 38, by, boundWidth, filter.maximum(), "+inf", value -> changeBound(index, value, false));
            button(x + w - 32, by, 32, CONTROL_HEIGHT, Text.literal("x"), () -> {
                var list = new ArrayList<>(state.filters());
                list.remove(index);
                updateLists(list, state.excluded(), state.strings());
            });
            y += rowHeight;
        }
        button(x, y, Math.min(w, 190), CONTROL_HEIGHT, Text.literal("+ Add Filter"), () -> chooseNumeric(key -> {
            var list = new ArrayList<>(state.filters());
            list.add(new AtlasState.NumericFilter(key));
            updateLists(list, state.excluded(), state.strings());
        }));
        y += 46;
        label(x, y, "Excluded Filters:");
        y += 25;
        for (int i = 0; i < state.excluded().size(); i++) {
            int index = i;
            String key = state.excluded().get(i);
            button(x, y, w - 38, CONTROL_HEIGHT,
                    fitted(AtlasSearch.label(key), w - 38, CONTROL_TEXT_SCALE), () -> chooseNumeric(chosen -> {
                var list = new ArrayList<>(state.excluded());
                list.set(index, chosen);
                updateLists(state.filters(), list, state.strings());
            }));
            button(x + w - 32, y, 32, CONTROL_HEIGHT, Text.literal("x"), () -> {
                var list = new ArrayList<>(state.excluded());
                list.remove(index);
                updateLists(state.filters(), list, state.strings());
            });
            y += 35;
        }
        button(x, y, Math.min(w, 230), CONTROL_HEIGHT, Text.literal("+ Add Excluded Filter"), () -> chooseNumeric(key -> {
            var list = new ArrayList<>(state.excluded());
            if (!list.contains(key)) list.add(key);
            updateLists(state.filters(), list, state.strings());
        })).setHoverText(Text.literal("Exclude results with a nonzero value for this stat"));
        if (state.ingredients()) return;
        y += 46;
        label(x, y, "String Filters:");
        y += 25;
        for (int i = 0; i < state.strings().size(); i++) {
            int index = i;
            var filter = state.strings().get(i);
            button(x, y, w - 38, CONTROL_HEIGHT,
                    fitted(AtlasSearch.label(filter.key()), w - 38, CONTROL_TEXT_SCALE),
                    () -> chooseString(key -> {
                        var list = new ArrayList<>(state.strings());
                        list.set(index, new AtlasState.StringFilter(key, filter.value()));
                        updateLists(state.filters(), state.excluded(), list);
                    }));
            button(x + w - 32, y, 32, CONTROL_HEIGHT, Text.literal("x"), () -> {
                var list = new ArrayList<>(state.strings());
                list.remove(index);
                updateLists(state.filters(), state.excluded(), list);
            });
            field(x, y + 34, w, filter.value(), "Contains (case insensitive)", value -> {
                var list = new ArrayList<>(state.strings());
                list.set(index, new AtlasState.StringFilter(filter.key(), value));
                updateInputs(state.query(), state.category(), state.rarity(), state.filters(), state.excluded(), list);
            });
            y += 66;
        }
        button(x, y, Math.min(w, 210), CONTROL_HEIGHT, Text.literal("+ Add String Filter"), () -> chooseString(key -> {
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
        label(0, y + 6, title);
        button(w - 112, y, 50, 24, Text.literal("All"), () -> setSelection(rarity, ""));
        var none = button(w - 56, y, 56, 24, Text.literal("None"), () -> setSelection(rarity, "~none"));
        if (rarity && !state.ingredients()) {
            none.setHoverText(Text.literal("Clear rarity selection. No selected rarities shows all rarities."));
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
        updateState(state.query(), rarity ? state.category() : value, rarity ? value : state.rarity(),
                state.filters(), state.excluded(), state.strings());
        rebuild(false);
    }

    private void updateInputs(String query, String category, String rarity, List<AtlasState.NumericFilter> filters,
            List<String> excluded, List<AtlasState.StringFilter> strings) {
        updateState(query, category, rarity, filters, excluded, strings);
        refreshResults();
    }

    private void updateState(String query, String category, String rarity, List<AtlasState.NumericFilter> filters,
            List<String> excluded, List<AtlasState.StringFilter> strings) {
        state = new AtlasState(state.ingredients(), query, category, rarity, state.minLevel(), state.maxLevel(),
                "filters", false, 0, "", filters, excluded, strings);
    }

    private void updateLists(List<AtlasState.NumericFilter> filters, List<String> excluded, List<AtlasState.StringFilter> strings) {
        updateState(state.query(), state.category(), state.rarity(), filters, excluded, strings);
        rebuild(false);
    }

    private void rebuild(boolean top) {
        if (top) scroll.jump(0);
        init();
    }

    private Text fitted(String value, int width) {
        return fitted(value, width, 1.0F);
    }

    private Text fitted(String value, int width, float scale) {
        int logicalWidth = Math.round((width - 8) / scale);
        return Text.literal(textRenderer.getWidth(value) <= logicalWidth ? value
                : textRenderer.trimToWidth(value,
                        Math.max(1, logicalWidth - textRenderer.getWidth("..."))) + "...");
    }

    private static String display(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private ThemedButton fixed(ThemedButton button) {
        fixed.add(button);
        return addButton(button);
    }

    private ThemedButton button(int x, int y, int w, int h, Text title, Runnable action) {
        var button = new AtlasControlButton(layout.left() + x, BODY_TOP + y, w, h, title, action)
                .setTextScale(CONTROL_TEXT_SCALE);
        controls.add(new Control(addButton(button), y, 1));
        return button;
    }

    private void field(int x, int y, int w, String value, String hint, Consumer<String> change) {
        TextInputWidget field = input(layout.left() + x, BODY_TOP + y, w, value, hint, change, true);
        controls.add(new Control(field, y, 2));
    }

    private TextInputWidget input(int x, int y, int width, String value, String hint,
            Consumer<String> change, boolean clipped) {
        TextInputWidget field = new AtlasTextInput(
                x * 2, y * 2, width * 2, CONTROL_HEIGHT * 2, clipped);
        field.setMaxLength(200);
        field.setPlaceholder(hint);
        field.setInputAndMoveCursorToEnd(value);
        field.setBackgroundColor(CustomColor.fromInt(UiTheme.SURFACE));
        field.setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
        field.setTextColor(CustomColor.fromInt(UiTheme.TEXT));
        field.setPlaceholderColor(CustomColor.fromInt(UiTheme.MUTED));
        field.setCursorColor(CustomColor.fromInt(UiTheme.accent()));
        field.setOnChange(change);
        inputFields.add(field);
        addRootWidget(field);
        return field;
    }

    private void label(int x, int y, String text) { labels.add(new Label(x, y, text)); }
    private List<AtlasSearch.Entry> catalog() { return state.ingredients() ? ingredients : items; }

    private void reloadCatalog() {
        long observed = database.revision();
        if (revision != observed) {
            items = AtlasSearch.items(database.allItems());
            cardCache.clear();
            revision = observed;
        }
    }

    private void refreshResults() {
        if (layout == null) return;
        int request = ++searchRequest;
        AtlasState requestedState = state;
        List<AtlasSearch.Entry> requestedCatalog = catalog();
        error = "";
        searching = true;
        results = List.of();
        prepareResultCards();
        scroll.clamp(0, maxScroll);
        CompletableFuture.supplyAsync(() -> AtlasSearch.search(requestedCatalog, requestedState), SEARCH_EXECUTOR)
                .whenComplete((found, failure) -> client.execute(() -> {
                    if (request != searchRequest) return;
                    searching = false;
                    if (failure == null) {
                        results = found;
                    } else {
                        results = List.of();
                        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                        error = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
                    }
                    if (state.page() != 0) state = state.withPage(0, state.selected());
                    prepareResultCards();
                    scroll.clamp(0, maxScroll);
                }));
    }

    private void prepareResultCards() {
        cards.clear();
        nextCardIndex = 0;
        nextCardY = layout.resultsY() + 20;
        cardsLoading = !results.isEmpty();
        maxScroll = Math.max(0, nextCardY - viewportHeight());
    }

    private void loadResultCards() {
        if (!cardsLoading) return;
        long deadline = System.nanoTime() + CARD_LOAD_BUDGET_NANOS;
        do {
            layoutNextResultRow();
        } while (cardsLoading && System.nanoTime() < deadline);
    }

    private void layoutNextResultRow() {
        List<WynnItemTooltip.Card> visuals = new ArrayList<>();
        int rowWidth = 0;
        while (nextCardIndex + visuals.size() < results.size() && visuals.size() < MAX_CARDS_PER_ROW) {
            AtlasSearch.Entry entry = results.get(nextCardIndex + visuals.size());
            WynnItemTooltip.Card visual = cardCache.computeIfAbsent(entry.id(), ignored -> entry.item() != null
                    ? WynnItemTooltip.itemCard(textRenderer, entry.item(), showLore)
                    : WynnItemTooltip.ingredientCard(textRenderer, entry.ingredient()));
            int candidateWidth = rowWidth + (visuals.isEmpty() ? 0 : AtlasPageLayout.RESULT_GAP)
                    + visual.width(CARD_SCALE);
            if (!visuals.isEmpty() && candidateWidth > layout.width()) break;
            visuals.add(visual);
            rowWidth = candidateWidth;
        }
        int rowHeight = 0;
        int x = (layout.width() - rowWidth) / 2;
        for (WynnItemTooltip.Card visual : visuals) {
            AtlasSearch.Entry entry = results.get(nextCardIndex++);
            int cardWidth = visual.width(CARD_SCALE);
            int cardHeight = visual.height(CARD_SCALE);
            cards.add(new Card(entry, visual, x, nextCardY, cardWidth, cardHeight));
            x += cardWidth + AtlasPageLayout.RESULT_GAP;
            rowHeight = Math.max(rowHeight, cardHeight);
        }
        nextCardY += rowHeight + AtlasPageLayout.RESULT_GAP;
        maxScroll = Math.max(0, nextCardY - viewportHeight());
        cardsLoading = nextCardIndex < results.size();
    }

    private int viewportHeight() { return Math.max(1, height - BODY_BOTTOM - BODY_TOP); }

    private void positionControls() {
        for (var control : controls) {
            control.setY(BODY_TOP + control.y - offset);
            boolean visible = control.getY() + control.getHeight() > BODY_TOP
                    && control.getY() < height - BODY_BOTTOM;
            control.setVisible(visible);
            control.setEnabled(visible);
            if (!visible) control.setFocused(false);
        }
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.fill(0, 0, width, height, UiTheme.BACKGROUND);
        UiTheme.drawPanel(context, ui, 6, 6, width - 12, height - 12);
        offset = Math.round(scroll.update(delta));
        positionControls();
        if (itemDetails != null) {
            for (TextInputWidget field : inputFields) field.setVisible(false);
        }
        ui.enableScissor(12, BODY_TOP, width - 12, height - BODY_BOTTOM);
        try {
            for (Label label : labels) drawScaledText(Text.literal(label.text()), layout.left() + label.x(),
                    BODY_TOP + label.y() - offset, UiTheme.TEXT, LABEL_TEXT_SCALE);
            String resultStatus = !workspace.error().isEmpty() ? workspace.error() : !error.isEmpty() ? error
                    : !state.ingredients() && !database.isReady()
                    ? database.isLoading() ? "Loading item database..."
                    : "Item data unavailable: " + database.getLastError()
                    : searching ? "Searching..."
                    : cardsLoading ? "Loading items... " + cards.size() + " / " + results.size()
                    : results.size() + " results";
            drawScaledText(fitted(resultStatus, layout.width(), LABEL_TEXT_SCALE), layout.left(),
                    BODY_TOP + layout.resultsY() - offset,
                    error.isEmpty() && workspace.error().isEmpty() ? UiTheme.TEXT : 0xFFFFAA55,
                    LABEL_TEXT_SCALE);
            for (Card card : cards) {
                int y = BODY_TOP + card.y() - offset;
                if (y + card.height() < BODY_TOP || y > height - BODY_BOTTOM) continue;
                int x = layout.left() + card.x();
                card.visual().render(context, textRenderer, x, y, CARD_SCALE);
            }
            renderVanillaChildren(context, mouseX, mouseY, delta);
            for (Icon icon : icons) {
                int x = layout.left() + icon.x();
                int y = BODY_TOP + icon.y() - offset;
                EquipmentSprites.draw(context, icon.type(), x + (icon.width() - 24) / 2, y + 5, 24);
                if (!icon.selected()) ui.fill(x + 1, y + 1, x + icon.width() - 1, y + 37, 0x88000000);
            }
        } finally { ui.disableScissor(); }
        for (var button : fixed) button.draw(context, mouseX, mouseY, delta, ui);
        scrollbar.configure(width - 11, BODY_TOP, viewportHeight(), maxScroll);
        scrollbar.draw(context, mouseX, mouseY, delta, ui);
        if (itemDetails != null) {
            ThemedButton.takePendingTooltip();
            itemDetails.resize(width, height);
            itemDetails.draw(context, mouseX, mouseY, delta, ui);
        }
    }

    private void drawScaledText(Text text, int x, int y, int color, float scale) {
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(scale, scale);
        ui.drawTextWithShadow(textRenderer, text, 0, 0, color);
        ui.popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        clearUiFocus();
        if (itemDetails != null) {
            itemDetails = null;
            return true;
        }
        Click logical = logicalClick(click);
        if (scrollbar.mouseClicked(logical.x(), logical.y(), logical.button())) return true;
        if (logical.button() == 0 && logical.y() >= BODY_TOP && logical.y() < height - BODY_BOTTOM) {
            for (Card card : cards) {
                double x = logical.x() - layout.left();
                double y = logical.y() - BODY_TOP + offset;
                if (x >= card.x() && x < card.x() + card.width() && y >= card.y() && y < card.y() + card.height()) {
                    if (card.entry().item() != null) {
                        itemDetails = new AtlasItemDetails(card.entry().item(), width, height, showLore);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Click logical = logicalClick(click);
        if (scrollbar.mouseDragged(logical.x(), logical.y(), logical.button(),
                deltaX / matrixScale, deltaY / matrixScale)) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click logical = logicalClick(click);
        if (scrollbar.mouseReleased(logical.x(), logical.y(), logical.button())) return true;
        return super.mouseReleased(click);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (itemDetails != null) {
            itemDetails.scroll(vertical);
            return true;
        }
        if (WorkspaceTabs.scroll(parent, tabId, 8, 2, width - 16,
                logicalCoordinate(x), logicalCoordinate(y), horizontal, vertical)) return true;
        scroll.move((float) -vertical * 30, 0, maxScroll);
        return true;
    }
    private void save() {
        if (workspace.find(tabId) != null) workspace.updateAtlas(tabId, state);
    }
    @Override public void tick() {
        super.tick();
        loadResultCards();
        if (++ticks % 20 == 0 && revision != database.revision()) { reloadCatalog(); refreshResults(); }
        if (ticks % 40 == 0) save();
    }
    @Override public void removed() { save(); super.removed(); }
    @Override public void close() {
        if (itemDetails != null) {
            itemDetails = null;
            return;
        }
        client.setScreen(parent);
    }

    private record Card(AtlasSearch.Entry entry, WynnItemTooltip.Card visual,
            int x, int y, int width, int height) {}

    private final class AtlasControlButton extends ThemedButton {
        private AtlasControlButton(int x, int y, int width, int height, Text message, Runnable action) {
            super(x, y, width, height, message, action);
        }

        @Override
        public boolean contains(int mouseX, int mouseY) {
            return mouseX >= 12 && mouseX < AtlasScreen.this.width - 12
                    && mouseY >= BODY_TOP && mouseY < AtlasScreen.this.height - BODY_BOTTOM
                    && super.contains(mouseX, mouseY);
        }
    }

    private final class AtlasTextInput extends TextInputWidget {
        private final boolean clipped;

        private AtlasTextInput(int x, int y, int width, int height, boolean clipped) {
            super(x, y, width, height, 10, 17, 2.5F);
            this.clipped = clipped;
        }

        @Override
        public void draw(DrawContext context, int mouseX, int mouseY, float delta, UIUtils ui) {
            if (!clipped) {
                super.draw(context, mouseX, mouseY, delta, ui);
                return;
            }
            UiTheme.enableScissor(context, ui, 24, BODY_TOP * 2,
                    (AtlasScreen.this.width - 12) * 2, (AtlasScreen.this.height - BODY_BOTTOM) * 2);
            try {
                super.draw(context, mouseX, mouseY, delta, ui);
            } finally {
                ui.disableScissor();
            }
        }

        @Override
        public boolean contains(int mouseX, int mouseY) {
            return (!clipped || mouseX >= 12 && mouseX < AtlasScreen.this.width - 12
                    && mouseY >= BODY_TOP && mouseY < AtlasScreen.this.height - BODY_BOTTOM)
                    && super.contains(mouseX, mouseY);
        }
    }

    private static final class Control {
        private final Widget widget;
        private final int y;
        private final int coordinateScale;

        private Control(Widget widget, int y, int coordinateScale) {
            this.widget = widget;
            this.y = y;
            this.coordinateScale = coordinateScale;
        }

        private void setY(int value) { widget.setPosition(widget.getX(), value * coordinateScale); }
        private int getY() { return widget.getY() / coordinateScale; }
        private int getHeight() { return widget.getHeight() / coordinateScale; }
        private void setVisible(boolean value) { widget.setVisible(value); }
        private void setEnabled(boolean value) { widget.setEnabled(value); }
        private void setFocused(boolean value) { widget.setFocused(value); }
    }
    private record Label(int x, int y, String text) {}
    private record Icon(String type, int x, int y, int width, boolean selected) {}
}
