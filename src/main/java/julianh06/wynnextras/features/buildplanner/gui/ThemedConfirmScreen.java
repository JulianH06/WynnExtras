package julianh06.wynnextras.features.buildplanner.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

final class ThemedConfirmScreen extends PlannerScreen {
    private final Consumer<Boolean> callback;
    private final Text message;
    private List<OrderedText> lines = List.of();
    private int panelWidth;
    private int panelHeight;
    private boolean answered;

    ThemedConfirmScreen(Consumer<Boolean> callback, Text title, Text message) {
        super(title);
        this.callback = callback;
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(420, width - 24);
        lines = textRenderer.wrapLines(message, panelWidth - 36);
        panelHeight = 82 + lines.size() * 12;
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int buttonWidth = Math.min(104, (panelWidth - 48) / 2);
        int buttonY = top + panelHeight - 30;
        addButton(new ThemedButton(
                left + 18, buttonY, buttonWidth, 20, ScreenTexts.YES, () -> answer(true)));
        addButton(new ThemedButton(
                left + panelWidth - buttonWidth - 18, buttonY, buttonWidth, 20,
                ScreenTexts.NO, () -> answer(false)));
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        UiTheme.drawPanel(ui, left, top, panelWidth, panelHeight);
        ui.drawCenteredTextWithShadow(textRenderer,
                textRenderer.trimToWidth(getTitle().getString(), panelWidth - 36),
                width / 2, top + 14, UiTheme.accentRgb());
        for (int i = 0; i < lines.size(); i++) {
            ui.drawCenteredTextWithShadow(
                    textRenderer, lines.get(i), width / 2, top + 38 + i * 12, 0xFFE8E8E8);
        }
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    @Override
    public Text getNarratedTitle() {
        return getTitle().copy().append(". ").append(message);
    }

    private void answer(boolean confirmed) {
        if (!answered) {
            answered = true;
            callback.accept(confirmed);
        }
    }

    @Override
    public void close() {
        answer(false);
    }
}
