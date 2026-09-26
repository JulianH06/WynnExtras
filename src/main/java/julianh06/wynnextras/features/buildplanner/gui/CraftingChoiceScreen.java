package julianh06.wynnextras.features.buildplanner.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class CraftingChoiceScreen extends Screen {
    private final Screen parent;
    private final List<Choice> choices;
    private final Consumer<Choice> select;
    private final boolean showTooltips;
    private final List<ThemedButton> results = new ArrayList<>();
    private String query = "";
    private int offset;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int count;
    private ThemedButton previous;
    private ThemedButton next;
    private TextFieldWidget search;

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices, Consumer<Choice> select) {
        this(parent, title, choices, select, true);
    }

    CraftingChoiceScreen(Screen parent, String title, List<Choice> choices, Consumer<Choice> select, boolean showTooltips) {
        super(Text.literal(title));
        this.parent = parent;
        this.choices = List.copyOf(choices);
        this.select = select;
        this.showTooltips = showTooltips;
    }

    @Override
    protected void init() {
        clearChildren();
        results.clear();
        panelWidth = Math.min(480, width - 24);
        panelHeight = Math.min(400, height - 24);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        search = new TextFieldWidget(textRenderer, left + 12, top + 30, panelWidth - 24, 20, Text.literal("Search"));
        search.setMaxLength(100);
        search.setText(query);
        search.setPlaceholder(Text.literal("Search names or effects..."));
        addDrawableChild(search);
        search.setChangedListener(value -> {
            query = value;
            offset = 0;
            rebuild();
        });
        int footer = top + panelHeight - 30;
        addDrawableChild(new ThemedButton(left + 12, footer, 70, 20, Text.literal("Back"), this::close));
        previous = addDrawableChild(new ThemedButton(left + panelWidth - 160, footer, 70, 20,
                Text.literal("Previous"), () -> { offset = Math.max(0, offset - visible()); rebuild(); }));
        next = addDrawableChild(new ThemedButton(left + panelWidth - 82, footer, 70, 20,
                Text.literal("Next"), () -> { offset += visible(); rebuild(); }));
        rebuild();
        setInitialFocus(search);
    }

    private int visible() {
        return Math.max(1, (panelHeight - 110) / 24);
    }

    private void rebuild() {
        results.forEach(this::remove);
        results.clear();
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        List<Choice> filtered = choices.stream().filter(choice -> normalized.isBlank()
                || (choice.label() + " " + choice.description()).toLowerCase(Locale.ROOT).contains(normalized)).toList();
        count = filtered.size();
        offset = Math.max(0, Math.min(offset, Math.max(0, count - visible())));
        for (int i = offset; i < Math.min(count, offset + visible()); i++) {
            Choice choice = filtered.get(i);
            String label = textRenderer.getWidth(choice.label()) <= panelWidth - 40 ? choice.label()
                    : textRenderer.trimToWidth(choice.label(), panelWidth - 55) + "...";
            ThemedButton button = new ThemedButton(left + 12, top + 58 + (i - offset) * 24,
                    panelWidth - 24, 20, Text.literal(label), () -> {
                        select.accept(choice);
                        client.setScreen(parent);
                    });
            if (showTooltips) {
                button.setTooltip(Tooltip.of(Text.literal(choice.label() + "\n" + choice.description())));
            }
            results.add(addDrawableChild(button));
        }
        previous.active = offset > 0;
        next.active = offset + visible() < count;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        offset -= (int) Math.signum(vertical);
        rebuild();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiTheme.drawPanel(context, left, top, panelWidth, panelHeight);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, top + 11, UiTheme.accentRgb());
        context.drawCenteredTextWithShadow(textRenderer, count == 0 ? "No matching choices" : count + " choices",
                width / 2, top + panelHeight - 43, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    record Choice(int id, String label, String description) {}
}
