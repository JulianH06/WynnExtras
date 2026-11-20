package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.Ingredient;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.colors.CustomColor;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

final class CraftingChoiceScreen extends PlannerScreen {
    private static final int MAX_WIDTH = 780;
    private static final int MAX_HEIGHT = 490;
    private static final int HORIZONTAL_MARGIN = 80;
    private static final int VERTICAL_MARGIN = 70;
    private static final int PADDING = 20;
    private static final int SEARCH_TOP = 14;
    private static final int SEARCH_HEIGHT = 32;
    private static final int LIST_TOP = 55;
    private static final int TABBED_LIST_TOP = 91;
    private static final int LIST_BOTTOM = 60;
    private static final int BUTTON_HEIGHT = 34;
    private static final int ROW_HEIGHT = 38;
    private static final float TEXT_SCALE = 1.5F;
    private final Screen parent;
    private final List<Choice> choices;
    private final Consumer<Choice> select;
    private final boolean showTooltips;
    private final boolean showGroupTabs;
    private final Runnable remove;
    private final SmoothScroll scroll = new SmoothScroll();
    private final Map<Integer, ItemStack> ingredientIcons = new HashMap<>();
    private final List<GroupButton> groupButtons = new java.util.ArrayList<>();
    private String query = "";
    private String selectedGroup = "";
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private TextInputWidget search;
    private PlannerPickerList<Choice> list;

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices, Consumer<Choice> select) {
        this(parent, title, choices, select, true);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, boolean showTooltips) {
        this(parent, title, choices, select, showTooltips, false, null);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, boolean showTooltips, boolean showGroupTabs) {
        this(parent, title, choices, select, showTooltips, showGroupTabs, null);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, Runnable remove) {
        this(parent, title, choices, select, true, false, remove);
    }

    private CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, boolean showTooltips, boolean showGroupTabs, Runnable remove) {
        super(Text.literal(title));
        this.parent = parent;
        this.choices = List.copyOf(choices);
        this.select = select;
        this.showTooltips = showTooltips;
        this.showGroupTabs = showGroupTabs;
        this.remove = remove;
    }

    @Override
    protected int getMinLogicalWidth() {
        return parent instanceof CrafterScreen ? (int) Math.round(1400 * scaleFactor) : super.getMinLogicalWidth();
    }

    @Override
    protected boolean shouldRenderBlur() {
        return !rendersParentAsOverlay();
    }

    @Override
    protected boolean shouldRenderBackground() {
        return !rendersParentAsOverlay();
    }

    @Override
    protected void init() {
        if (parent instanceof CrafterScreen crafter) crafter.init();
        else if (parent instanceof AtlasScreen atlas) atlas.prepareOverlayRender();
        super.init();
        clearChildren();
        removeRootWidget(search);
        removeRootWidget(list);
        list = null;
        groupButtons.clear();
        panelWidth = Math.min(MAX_WIDTH, width - HORIZONTAL_MARGIN);
        panelHeight = Math.min(MAX_HEIGHT, height - VERTICAL_MARGIN);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        search = new TextInputWidget((left + PADDING) * 2, (top + SEARCH_TOP) * 2,
                (panelWidth - PADDING * 2) * 2, SEARCH_HEIGHT * 2, 8, 10, 2.0F);
        search.setPlaceholder("Search " + pickerLabel().toLowerCase(Locale.ROOT) + "...");
        search.setTextScale(3.6F);
        search.setTextOffset(12, 16);
        search.setMaxLength(100);
        search.setInput(query);
        search.setBackgroundColor(CustomColor.fromInt(UiTheme.SURFACE));
        search.setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
        search.setTextColor(CustomColor.fromInt(UiTheme.TEXT));
        search.setPlaceholderColor(CustomColor.fromInt(UiTheme.MUTED));
        search.setCursorColor(CustomColor.fromInt(UiTheme.accent()));
        search.setOnChange(value -> {
            query = value;
            rebuild();
        });
        addRootWidget(search);
        if (showGroupTabs) addGroupTabs();
        addButton(new ThemedButton(left + panelWidth - PADDING - 96,
                top + panelHeight - BUTTON_HEIGHT - 12,
                96, BUTTON_HEIGHT, Text.literal("Cancel"), this::close).setTextScale(TEXT_SCALE));
        if (remove != null) {
            addButton(new ThemedButton(left + PADDING, top + panelHeight - BUTTON_HEIGHT - 12,
                    96, BUTTON_HEIGHT, Text.literal("Remove"), () -> {
                        remove.run();
                        client.setScreen(parent);
                    }).setTextScale(TEXT_SCALE));
        }
        rebuild();
        setFocusedWidget(search);
    }

    private void addGroupTabs() {
        List<String> groups = choices.stream().map(Choice::group).filter(group -> !group.isBlank()).distinct().toList();
        if (groups.isEmpty()) return;
        if (!groups.contains(selectedGroup)) selectedGroup = groups.getFirst();
        int gap = 6;
        int availableWidth = panelWidth - PADDING * 2;
        int buttonWidth = (availableWidth - gap * (groups.size() - 1)) / groups.size();
        for (int index = 0; index < groups.size(); index++) {
            String group = groups.get(index);
            int x = left + PADDING + index * (buttonWidth + gap);
            int width = index == groups.size() - 1 ? left + panelWidth - PADDING - x : buttonWidth;
            ThemedButton button = new ThemedButton(x, top + LIST_TOP, width, 28, Text.literal(group),
                    () -> selectGroup(group)).setTextScale(1.2F).setSelectedHoverHighlight(group.equals(selectedGroup));
            groupButtons.add(new GroupButton(group, button));
            addButton(button);
        }
    }

    private void selectGroup(String group) {
        selectedGroup = group;
        for (GroupButton groupButton : groupButtons) {
            groupButton.button().setSelected(groupButton.group().equals(group));
        }
        scroll.jump(0);
        rebuild();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (rendersParentAsOverlay()) parent.render(context, -1, -1, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    private boolean rendersParentAsOverlay() {
        return parent instanceof CrafterScreen || parent instanceof AtlasScreen;
    }

    private void rebuild() {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        List<Choice> filtered = choices.stream()
                .filter(choice -> !showGroupTabs || choice.group().equals(selectedGroup))
                .filter(choice -> normalized.isBlank()
                || (choice.label() + " " + choice.description() + " " + choice.group())
                        .toLowerCase(Locale.ROOT).contains(normalized)).toList();
        List<PlannerPickerList.Row<Choice>> rows = new java.util.ArrayList<>();
        String currentGroup = "";
        for (Choice choice : filtered) {
            if (!showGroupTabs && !choice.group().isBlank() && !choice.group().equals(currentGroup)) {
                currentGroup = choice.group();
                rows.add(PlannerPickerList.Row.heading(currentGroup, 28));
            }
            rows.add(PlannerPickerList.Row.value(choice, ROW_HEIGHT));
        }
        if (list == null) {
            int listTop = showGroupTabs ? TABBED_LIST_TOP : LIST_TOP;
            list = new PlannerPickerList<>(left + PADDING, top + listTop,
                    panelWidth - PADDING * 2, panelHeight - listTop - LIST_BOTTOM,
                    rows, scroll, TEXT_SCALE, this::renderRow,
                    showTooltips ? this::renderTooltip : null,
                    choice -> {
                        select.accept(choice);
                        prepareParentReturn();
                        client.setScreen(parent);
                    });
            addRootWidget(list);
        } else {
            list.setRows(rows);
        }
    }

    private void renderRow(DrawContext context, julianh06.wynnextras.utils.UI.UIUtils ui,
            net.minecraft.client.font.TextRenderer textRenderer, Choice choice,
            int x, int y, int width, int height, boolean hovered) {
        UiTheme.drawRoundedBox(context, ui, x, y, width, height - 2,
                hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                hovered ? UiTheme.accent() : UiTheme.BORDER);
        int textX = x + 12;
        int availableWidth = width - 24;
        if (choice.ingredient() != null) {
            drawIngredientIcon(ui, choice.ingredient(), x + 4, y + 3, 30);
            textX = x + 40;
            availableWidth -= 32;
        } else if (choice.itemIcon() != null && !choice.itemIcon().isEmpty()) {
            drawItemIcon(ui, choice.itemIcon(), x + 4, y + 3, 30);
            textX = x + 40;
            availableWidth -= 32;
        } else if (choice.icon() != null) {
            ChoiceIcon icon = choice.icon();
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, icon.texture(),
                    x + 4, y + 3, icon.u(), icon.v(), 30, 30,
                    16, 16, icon.textureWidth(), icon.textureHeight());
            textX = x + 40;
            availableWidth -= 32;
        }
        int textWidth = Math.round(availableWidth / TEXT_SCALE);
        ui.pushMatrix();
        ui.translate(textX, y + height / 2.0F);
        ui.scale(TEXT_SCALE, TEXT_SCALE);
        if (choice.ingredient() != null) {
            renderIngredientLabel(ui, textRenderer, choice.ingredient(), textWidth);
        } else {
            String label = textRenderer.getWidth(choice.label()) <= textWidth ? choice.label()
                    : textRenderer.trimToWidth(choice.label(), Math.max(0, textWidth - 12)) + "...";
            ui.drawTextWithShadow(textRenderer, label, 0, -4,
                    choice.color() == null ? 0xFFFFFFFF : choice.color());
        }
        ui.popMatrix();
    }

    private void renderIngredientLabel(julianh06.wynnextras.utils.UI.UIUtils ui,
            net.minecraft.client.font.TextRenderer textRenderer, Ingredient ingredient, int width) {
        Text tier = WynnItemTooltip.ingredientTier(ingredient.tier());
        Text level = Text.literal(" [Level " + ingredient.level() + "]").formatted(net.minecraft.util.Formatting.GRAY);
        int suffixWidth = textRenderer.getWidth(tier) + textRenderer.getWidth(level);
        String name = ingredient.name();
        if (textRenderer.getWidth(name) + suffixWidth > width) {
            int nameWidth = Math.max(0, width - suffixWidth - textRenderer.getWidth("..."));
            name = textRenderer.trimToWidth(name, nameWidth) + "...";
        }
        ui.drawTextWithShadow(textRenderer, name, 0, -4,
                ToolItemCards.color(Integer.toString(ingredient.tier())));
        int offset = textRenderer.getWidth(name);
        ui.drawTextWithShadow(textRenderer, tier, offset, -4, 0xFFFFFFFF);
        ui.drawTextWithShadow(textRenderer, level, offset + textRenderer.getWidth(tier), -4, 0xFFFFFFFF);
    }

    private void drawItemIcon(julianh06.wynnextras.utils.UI.UIUtils ui, ItemStack stack,
            int x, int y, int size) {
        ui.pushMatrix();
        try {
            ui.translate(x, y);
            ui.scale(size / 16.0F, size / 16.0F);
            ui.drawItem(stack, 0, 0);
        } finally {
            ui.popMatrix();
        }
    }

    private void drawIngredientIcon(julianh06.wynnextras.utils.UI.UIUtils ui, Ingredient ingredient,
            int x, int y, int size) {
        ItemStack stack = ingredientIcons.computeIfAbsent(ingredient.id(),
                ignored -> CrafterScreen.ingredientIcon(ingredient));
        ui.pushMatrix();
        try {
            ui.translate(x, y);
            ui.scale(size / 16.0F, size / 16.0F);
            ui.drawItem(stack, 0, 0);
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not render ingredient picker icon for {} ({}).",
                    ingredient.name(), ingredient.id(), exception);
            ItemStack fallback = CrafterScreen.ingredientFallback();
            ingredientIcons.put(ingredient.id(), fallback);
            ui.drawItem(fallback, 0, 0);
        } finally {
            ui.popMatrix();
        }
    }

    private void renderTooltip(DrawContext context, Choice choice, int mouseX, int mouseY) {
        if (choice.ingredient() != null) {
            try {
                WynnItemTooltip.renderIngredient(this, context, textRenderer, choice.ingredient(), mouseX, mouseY);
            } catch (RuntimeException exception) {
                PlannerLog.LOGGER.warn("Could not render ingredient picker tooltip for {} ({}).",
                        choice.ingredient().name(), choice.ingredient().id(), exception);
            }
            return;
        }
        if (!choice.tooltip().isEmpty()) {
            renderPlannerTooltip(context, choice.tooltip(), mouseX, mouseY);
            return;
        }
        if (choice.description().isBlank()) return;
        List<Text> lines = new java.util.ArrayList<>();
        lines.add(Text.literal(choice.label()).formatted(Formatting.LIGHT_PURPLE));
        String line = "";
        for (String word : choice.description().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && textRenderer.getWidth(candidate) > 280) {
                lines.add(Text.literal(line).formatted(Formatting.GRAY));
                line = word;
            } else {
                line = candidate;
            }
        }
        if (!line.isEmpty()) lines.add(Text.literal(line).formatted(Formatting.GRAY));
        renderPlannerTooltip(context, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double logicalX = logicalCoordinate(mouseX);
        double logicalY = logicalCoordinate(mouseY);
        if (list != null && list.containsLogical(logicalX, logicalY)) list.scroll(vertical);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click logical = logicalClick(click);
        if (logical.button() == 0 && (logical.x() < left || logical.x() >= left + panelWidth
                || logical.y() < top || logical.y() >= top + panelHeight)) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if ((input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) && list != null) {
            list.chooseFirst();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.fill(0, 0, width, height, 0x99000000);
        UiTheme.drawPanel(context, ui, left, top, panelWidth, panelHeight);
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    private String pickerLabel() {
        String title = getTitle().getString();
        return title.regionMatches(true, 0, "Choose ", 0, 7) ? title.substring(7) : title;
    }

    @Override
    public void close() {
        prepareParentReturn();
        client.setScreen(parent);
    }

    private void prepareParentReturn() {
        if (parent instanceof AtlasScreen atlas) atlas.returnFromOverlay();
    }

    record Choice(
            int id,
            String label,
            String description,
            Ingredient ingredient,
            Integer color,
            ChoiceIcon icon,
            ItemStack itemIcon,
            List<Text> tooltip,
            String group
    ) {
        Choice {
            tooltip = List.copyOf(tooltip == null ? List.of() : tooltip);
            group = group == null ? "" : group;
        }

        Choice(int id, String label, String description, Ingredient ingredient, Integer color,
                ChoiceIcon icon, ItemStack itemIcon, List<Text> tooltip) {
            this(id, label, description, ingredient, color, icon, itemIcon, tooltip, "");
        }

        Choice(int id, String label, String description) {
            this(id, label, description, null, null, null, null, List.of(), "");
        }

        Choice(int id, String label, String description, String group) {
            this(id, label, description, null, null, null, null, List.of(), group);
        }

        Choice(int id, String label, String description, Ingredient ingredient) {
            this(id, label, description, ingredient, null, null, null, List.of(), "");
        }

        Choice(int id, String label, String description, int color, ChoiceIcon icon, List<Text> tooltip) {
            this(id, label, description, null, color, icon, null, tooltip, "");
        }

        Choice(int id, String label, String description, ItemStack itemIcon, List<Text> tooltip) {
            this(id, label, description, null, null, null, itemIcon, tooltip, "");
        }
    }

    record ChoiceIcon(Identifier texture, int u, int v, int textureWidth, int textureHeight) {}
    private record GroupButton(String group, ThemedButton button) {}
}
