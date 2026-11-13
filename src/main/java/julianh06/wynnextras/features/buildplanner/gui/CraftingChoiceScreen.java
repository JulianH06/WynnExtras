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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

final class CraftingChoiceScreen extends PlannerScreen {
    private static final int MAX_WIDTH = 780;
    private static final int MAX_HEIGHT = 490;
    private static final int HORIZONTAL_MARGIN = 80;
    private static final int VERTICAL_MARGIN = 70;
    private static final int PADDING = 20;
    private static final int SEARCH_TOP = 47;
    private static final int SEARCH_HEIGHT = 32;
    private static final int LIST_TOP = 88;
    private static final int LIST_BOTTOM = 60;
    private static final int BUTTON_HEIGHT = 34;
    private static final int ROW_HEIGHT = 38;
    private static final float TEXT_SCALE = 1.5F;
    private final Screen parent;
    private final List<Choice> choices;
    private final Consumer<Choice> select;
    private final boolean showTooltips;
    private final Runnable remove;
    private final boolean showHeading;
    private final SmoothScroll scroll = new SmoothScroll();
    private final Map<Integer, ItemStack> ingredientIcons = new HashMap<>();
    private String query = "";
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
        this(parent, title, choices, select, showTooltips, null, true);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, Runnable remove, boolean showHeading) {
        this(parent, title, choices, select, true, remove, showHeading);
    }

    private CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, boolean showTooltips, Runnable remove, boolean showHeading) {
        super(Text.literal(title));
        this.parent = parent;
        this.choices = List.copyOf(choices);
        this.select = select;
        this.showTooltips = showTooltips;
        this.remove = remove;
        this.showHeading = showHeading;
    }

    @Override
    protected int getMinLogicalWidth() {
        return parent instanceof CrafterScreen ? (int) Math.round(1400 * scaleFactor) : super.getMinLogicalWidth();
    }

    @Override
    protected boolean shouldRenderBlur() {
        return !(parent instanceof CrafterScreen);
    }

    @Override
    protected boolean shouldRenderBackground() {
        return !(parent instanceof CrafterScreen);
    }

    @Override
    protected void init() {
        if (parent instanceof CrafterScreen crafter) crafter.init();
        super.init();
        clearChildren();
        removeRootWidget(search);
        removeRootWidget(list);
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (parent instanceof CrafterScreen) parent.render(context, -1, -1, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    private void rebuild() {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        List<Choice> filtered = choices.stream().filter(choice -> normalized.isBlank()
                || (choice.label() + " " + choice.description()).toLowerCase(Locale.ROOT).contains(normalized)).toList();
        List<PlannerPickerList.Row<Choice>> rows = filtered.stream()
                .map(choice -> PlannerPickerList.Row.value(choice, ROW_HEIGHT)).toList();
        if (list == null) {
            list = new PlannerPickerList<>(left + PADDING, top + LIST_TOP,
                    panelWidth - PADDING * 2, panelHeight - LIST_TOP - LIST_BOTTOM,
                    rows, scroll, TEXT_SCALE, this::renderRow,
                    showTooltips ? this::renderTooltip : null,
                    choice -> {
                        select.accept(choice);
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
            ItemStack fallback = CrafterScreen.ingredientFallback(ingredient);
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
        renderPlannerTooltip(context,
                List.of(Text.literal(choice.label()), Text.literal(choice.description())), mouseX, mouseY);
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
        if (showHeading) {
            ui.pushMatrix();
            ui.translate(left + PADDING, top + 22);
            ui.scale(1.65F, 1.65F);
            ui.drawTextWithShadow(textRenderer,
                    Text.literal("Choose " + pickerLabel()).styled(style -> style.withColor(UiTheme.accentRgb())),
                    0, -4, 0xFFFFFFFF);
            ui.popMatrix();
        }
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    private String pickerLabel() {
        String title = getTitle().getString();
        return title.regionMatches(true, 0, "Choose ", 0, 7) ? title.substring(7) : title;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    record Choice(
            int id,
            String label,
            String description,
            Ingredient ingredient,
            Integer color,
            ChoiceIcon icon,
            ItemStack itemIcon,
            List<Text> tooltip
    ) {
        Choice {
            tooltip = List.copyOf(tooltip == null ? List.of() : tooltip);
        }

        Choice(int id, String label, String description) {
            this(id, label, description, null, null, null, null, List.of());
        }

        Choice(int id, String label, String description, Ingredient ingredient) {
            this(id, label, description, ingredient, null, null, null, List.of());
        }

        Choice(int id, String label, String description, int color, ChoiceIcon icon, List<Text> tooltip) {
            this(id, label, description, null, color, icon, null, tooltip);
        }

        Choice(int id, String label, String description, ItemStack itemIcon, List<Text> tooltip) {
            this(id, label, description, null, null, null, itemIcon, tooltip);
        }
    }

    record ChoiceIcon(Identifier texture, int u, int v, int textureWidth, int textureHeight) {}
}
