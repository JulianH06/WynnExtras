package julianh06.wynnextras.features.buildplanner.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

final class ThemedConfirmScreen extends Screen {
    private final Consumer<Boolean> callback;
    private final Text message;
    private List<OrderedText> lines = List.of();
    private ThemedButton yesButton;
    private ThemedButton noButton;
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
        yesButton = addDrawableChild(new ThemedButton(
                left + 18, buttonY, buttonWidth, 20, ScreenTexts.YES, () -> answer(true)));
        noButton = addDrawableChild(new ThemedButton(
                left + panelWidth - buttonWidth - 18, buttonY, buttonWidth, 20,
                ScreenTexts.NO, () -> answer(false)));
        setInitialFocus(noButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        UiTheme.drawPanel(context, left, top, panelWidth, panelHeight);
        context.drawCenteredTextWithShadow(textRenderer,
                textRenderer.trimToWidth(getTitle().getString(), panelWidth - 36),
                width / 2, top + 14, UiTheme.accentRgb());
        for (int i = 0; i < lines.size(); i++) {
            context.drawCenteredTextWithShadow(
                    textRenderer, lines.get(i), width / 2, top + 38 + i * 12, 0xFFE8E8E8);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER
                || input.key() == GLFW.GLFW_KEY_SPACE) {
            if (getFocused() == yesButton || getFocused() == noButton) {
                answer(getFocused() == yesButton);
                return true;
            }
        }
        return super.keyPressed(input);
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
