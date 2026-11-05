package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

class ThemedButton extends ClickableWidget {
    private static List<Text> pendingTooltip;
    private final Runnable onPress;
    private List<Text> hoverText;
    private boolean selected;
    private Integer selectedColor;
    private boolean selectedBorderHighlight;
    private Integer textColor;
    private float textScale = 1.0F;
    private boolean textShadow = true;
    private float textOffsetX;
    private float textOffsetY;
    private Identifier iconTexture;
    private int iconU;
    private int iconV;
    private int iconTextureWidth;
    private int iconTextureHeight;
    private long lastFrameNanos = System.nanoTime();
    private float hoverProgress;
    private float pressProgress;

    ThemedButton(int x, int y, int width, int height, Text message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    ThemedButton setHoverText(Text hoverText) {
        return setHoverLines(hoverText == null ? null : List.of(hoverText));
    }

    ThemedButton setHoverLines(List<Text> hoverText) {
        this.hoverText = hoverText == null ? null : List.copyOf(hoverText);
        if (this.isHovered() && this.hoverText != null) pendingTooltip = this.hoverText;
        return this;
    }

    ThemedButton setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    ThemedButton setSelectedColor(int color) {
        selectedColor = color;
        return this;
    }

    ThemedButton setSelectedBorderHighlight() {
        selectedBorderHighlight = true;
        return this;
    }

    ThemedButton setTextColor(int color) {
        textColor = color;
        return this;
    }

    ThemedButton setTextScale(float textScale) {
        this.textScale = textScale;
        return this;
    }

    ThemedButton setTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    ThemedButton setTextOffset(float x, float y) {
        textOffsetX = x;
        textOffsetY = y;
        return this;
    }

    ThemedButton setIcon(Identifier texture, int u, int v, int textureWidth, int textureHeight) {
        iconTexture = texture;
        iconU = u;
        iconV = v;
        iconTextureWidth = textureWidth;
        iconTextureHeight = textureHeight;
        return this;
    }

    static void renderPendingTooltip(DrawContext context, int mouseX, int mouseY) {
        if (pendingTooltip != null) {
            new UIUtils(context, 1.0, 0, 0).drawTooltip(
                    MinecraftClient.getInstance().textRenderer, pendingTooltip, mouseX, mouseY);
            pendingTooltip = null;
        }
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (this.active) {
            pressProgress = 1.0F;
            playClickSound();
            onPress.run();
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER
                || input.key() == GLFW.GLFW_KEY_SPACE) {
            if (this.active && this.isFocused()) {
                pressProgress = 1.0F;
                playClickSound();
                onPress.run();
                return true;
            }
            return false;
        }
        return super.keyPressed(input);
    }

    protected final void playClickSound() {
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        UIUtils ui = new UIUtils(context, 1.0, 0, 0);
        long now = System.nanoTime();
        float elapsed = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;

        float hoverTarget = (this.isHovered() || this.isFocused()) && this.active ? 1.0F : 0.0F;
        hoverProgress = UiTheme.approach(hoverProgress, hoverTarget, 13.0F, elapsed);
        pressProgress = UiTheme.approach(pressProgress, 0.0F, 18.0F, elapsed);

        boolean borderOnlySelection = selected && selectedBorderHighlight;
        boolean highlightedSelection = selected && !selectedBorderHighlight;
        int selection = selectedColor == null ? 0xFF44BB66 : selectedColor;
        int baseBackground = highlightedSelection
                ? UiTheme.lerpColor(UiTheme.SURFACE, selection, 0.28F) : UiTheme.SURFACE;
        int hoverBackground = highlightedSelection ? UiTheme.lerpColor(UiTheme.SURFACE, selection, 0.42F)
                : UiTheme.hoverBackground();
        int background = this.active
                ? UiTheme.lerpColor(baseBackground, hoverBackground, hoverProgress)
                : UiTheme.PANEL;
        background = UiTheme.lerpColor(background, UiTheme.BACKGROUND, pressProgress * 0.35F);
        int baseBorder = borderOnlySelection
                ? UiTheme.accent() : highlightedSelection ? selection : UiTheme.BORDER;
        int hoverBorder = borderOnlySelection
                ? UiTheme.lerpColor(UiTheme.accent(), 0xFFFFFFFF, 0.25F)
                : highlightedSelection ? UiTheme.lerpColor(selection, 0xFFFFFFFF, 0.25F) : UiTheme.accent();
        int border = this.active ? UiTheme.lerpColor(baseBorder, hoverBorder, hoverProgress)
                : UiTheme.lerpColor(UiTheme.PANEL, UiTheme.BORDER, 0.5F);
        int baseText = highlightedSelection
                ? UiTheme.lerpColor(selection, 0xFFFFFFFF, 0.58F) : UiTheme.TEXT;
        if (textColor != null) baseText = textColor;
        int text = this.active
                ? textColor == null ? UiTheme.lerpColor(baseText, 0xFFFFFFFF, hoverProgress) : baseText
                : textColor == null ? UiTheme.DISABLED : textColor;

        UiTheme.drawRoundedBox(ui, getX(), getY(), getWidth(), getHeight(), background, border);
        if (iconTexture != null) {
            int iconSize = Math.min(18, getHeight() - 4);
            int iconX = getX() + 5;
            int iconY = getY() + (getHeight() - iconSize) / 2;
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, iconTexture,
                    iconX, iconY, iconU, iconV, iconSize, iconSize,
                    16, 16, iconTextureWidth, iconTextureHeight);
            drawScaledText(ui, iconX + iconSize + 5, getY() + getHeight() / 2, text, false);
        } else if (getMessage().getString().equals("X")) {
            var color = getMessage().getStyle().getColor();
            drawCloseIcon(ui, this.active && color != null ? 0xFF000000 | color.getRgb() : text);
        } else if (getMessage().getString().equals("i") || getMessage().getString().equals("+")) {
            var renderer = MinecraftClient.getInstance().textRenderer;
            int textX = getX() + Math.round((getWidth() - renderer.getWidth(getMessage())) / 2.0F);
            int textY = getY() + Math.round((getHeight() - renderer.fontHeight) / 2.0F);
            ui.drawText(renderer, getMessage(), textX, textY, text, false);
        } else {
            drawScaledText(ui, getX() + getWidth() / 2, getY() + getHeight() / 2, text, true);
        }
        if (this.isHovered() && hoverText != null) pendingTooltip = hoverText;
    }

    private void drawScaledText(UIUtils ui, int x, int centerY, int color, boolean centered) {
        ui.pushMatrix();
        ui.translate(x + textOffsetX, centerY + textOffsetY);
        ui.scale(textScale, textScale);
        var renderer = MinecraftClient.getInstance().textRenderer;
        if (centered && textShadow) {
            ui.drawCenteredTextWithShadow(
                    renderer, getMessage(), 0, -4, color);
        } else if (centered) {
            ui.drawText(renderer, getMessage(), -renderer.getWidth(getMessage()) / 2,
                    -renderer.fontHeight / 2, color, false);
        } else if (textShadow) {
            ui.drawTextWithShadow(renderer, getMessage(), 0, -4, color);
        } else {
            ui.drawText(renderer, getMessage(), 0, -renderer.fontHeight / 2, color, false);
        }
        ui.popMatrix();
    }

    private void drawCloseIcon(UIUtils ui, int color) {
        int size = Math.min(7, Math.min(getWidth() - 4, getHeight() - 4));
        if (size % 2 == 0) size--;
        int left = getX() + (getWidth() - size) / 2;
        int top = getY() + (getHeight() - size) / 2;
        for (int i = 0; i < size; i++) {
            ui.fill(left + i, top + i, left + i + 1, top + i + 1, color);
            if (i != size / 2) {
                ui.fill(left + size - 1 - i, top + i, left + size - i, top + i + 1, color);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
