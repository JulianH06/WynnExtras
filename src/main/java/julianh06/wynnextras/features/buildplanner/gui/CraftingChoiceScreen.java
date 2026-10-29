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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

final class CraftingChoiceScreen extends PlannerScreen {
    private static final int ROW_HEIGHT = 24;
    private final Screen parent;
    private final List<Choice> choices;
    private final Consumer<Choice> select;
    private final boolean showTooltips;
    private final SmoothScroll scroll = new SmoothScroll();
    private final Map<Integer, ItemStack> ingredientIcons = new HashMap<>();
    private String query = "";
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int count;
    private TextInputWidget search;
    private PlannerPickerList<Choice> list;

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices, Consumer<Choice> select) {
        this(parent, title, choices, select, true);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices,
            Consumer<Choice> select, boolean showTooltips) {
        super(Text.literal(title));
        this.parent = parent;
        this.choices = List.copyOf(choices);
        this.select = select;
        this.showTooltips = showTooltips;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        removeRootWidget(search);
        removeRootWidget(list);
        panelWidth = Math.min(520, width - 60);
        panelHeight = Math.min(326, height - 50);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        search = new TextInputWidget((left + 14) * 2, (top + 34) * 2,
                (panelWidth - 28) * 2, 40, 8, 10, 2.0F);
        search.setPlaceholder("Search names or effects...");
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
        addDrawableChild(new ThemedButton(left + panelWidth - 78, top + panelHeight - 28,
                64, 20, Text.literal("Cancel"), this::close));
        rebuild();
        setFocusedWidget(search);
    }

    private void rebuild() {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        List<Choice> filtered = choices.stream().filter(choice -> normalized.isBlank()
                || (choice.label() + " " + choice.description()).toLowerCase(Locale.ROOT).contains(normalized)).toList();
        count = filtered.size();
        List<PlannerPickerList.Row<Choice>> rows = filtered.stream()
                .map(choice -> PlannerPickerList.Row.value(choice, ROW_HEIGHT)).toList();
        if (list == null) {
            list = new PlannerPickerList<>(left + 14, top + 62, panelWidth - 28, panelHeight - 100,
                    rows, scroll, this::renderRow,
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
        UiTheme.drawRoundedBox(ui, x, y, width, height - 2,
                hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                hovered ? UiTheme.accent() : UiTheme.BORDER);
        int textX = x + 8;
        int availableWidth = width - 16;
        if (choice.ingredient() != null) {
            drawIngredientIcon(ui, choice.ingredient(), x + 4, y + 3, 18);
            textX = x + 27;
            availableWidth -= 19;
        }
        String label = textRenderer.getWidth(choice.label()) <= availableWidth ? choice.label()
                : textRenderer.trimToWidth(choice.label(), Math.max(0, availableWidth - 12)) + "...";
        ui.drawTextWithShadow(textRenderer, label, textX, y + 7, 0xFFFFFFFF);
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
        UiTheme.drawPanel(ui, left, top, panelWidth, panelHeight);
        ui.drawTextWithShadow(textRenderer,
                getTitle().copy().styled(style -> style.withColor(UiTheme.accentRgb())), left + 14, top + 13, 0xFFFFFFFF);
        ui.drawTextWithShadow(textRenderer, count == 0 ? "No matching choices" : count + " choices",
                left + 14, top + panelHeight - 24, UiTheme.MUTED);
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    record Choice(int id, String label, String description, Ingredient ingredient) {
        Choice(int id, String label, String description) {
            this(id, label, description, null);
        }
    }
}
