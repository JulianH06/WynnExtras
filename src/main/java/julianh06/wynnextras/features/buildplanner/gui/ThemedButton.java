package julianh06.wynnextras.features.buildplanner.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

final class ThemedButton extends ClickableWidget {
    private final Runnable onPress;
    private long lastFrameNanos = System.nanoTime();
    private float hoverProgress;
    private float pressProgress;

    ThemedButton(int x, int y, int width, int height, Text message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (this.active) {
            pressProgress = 1.0F;
            onPress.run();
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float elapsed = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;

        float hoverTarget = (this.isHovered() || this.isFocused()) && this.active ? 1.0F : 0.0F;
        hoverProgress = UiTheme.approach(hoverProgress, hoverTarget, 13.0F, elapsed);
        pressProgress = UiTheme.approach(pressProgress, 0.0F, 18.0F, elapsed);

        int background = this.active
                ? UiTheme.lerpColor(UiTheme.SURFACE, UiTheme.hoverBackground(), hoverProgress)
                : UiTheme.PANEL;
        background = UiTheme.lerpColor(background, UiTheme.BACKGROUND, pressProgress * 0.35F);
        int border = this.active ? UiTheme.lerpColor(UiTheme.BORDER, UiTheme.accent(), hoverProgress)
                : UiTheme.lerpColor(UiTheme.PANEL, UiTheme.BORDER, 0.5F);
        int text = this.active
                ? UiTheme.lerpColor(UiTheme.TEXT, 0xFFFFFFFF, hoverProgress)
                : UiTheme.DISABLED;

        UiTheme.drawRoundedBox(context, getX(), getY(), getWidth(), getHeight(), background, border);
        if (getMessage().getString().equals("X")) {
            var color = getMessage().getStyle().getColor();
            drawCloseIcon(context, this.active && color != null ? 0xFF000000 | color.getRgb() : text);
        } else {
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    getMessage(),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    text);
        }
    }

    private void drawCloseIcon(DrawContext context, int color) {
        int size = Math.min(7, Math.min(getWidth() - 4, getHeight() - 4));
        if (size % 2 == 0) size--;
        int left = getX() + (getWidth() - size) / 2;
        int top = getY() + (getHeight() - size) / 2;
        for (int i = 0; i < size; i++) {
            context.fill(left + i, top + i, left + i + 1, top + i + 1, color);
            if (i != size / 2) {
                context.fill(left + size - 1 - i, top + i, left + size - i, top + i + 1, color);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
