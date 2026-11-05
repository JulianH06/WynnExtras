package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.colors.CustomColor;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

class ThemedButton extends Widget {
    private static List<Text> pendingTooltip;
    private final Runnable onPress;
    private Text message;
    private List<Text> hoverText;
    private boolean selected;
    private Integer selectedColor;
    private boolean selectedBorderHighlight;
    private Integer textColor;
    private float textScale = 1.0F;
    private Identifier iconTexture;
    private int iconU;
    private int iconV;
    private int iconTextureWidth;
    private int iconTextureHeight;
    private long lastFrameNanos = System.nanoTime();
    private float hoverProgress;
    private float pressProgress;

    ThemedButton(int x, int y, int width, int height, Text message, Runnable onPress) {
        super(x, y, width, height);
        setSkipRender(true);
        this.message = message;
        this.onPress = onPress;
    }

    Text getMessage() {
        return message;
    }

    void setMessage(Text message) {
        this.message = message;
    }

    void setX(int x) {
        setPosition(x, y);
    }

    void setY(int y) {
        setPosition(x, y);
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

    protected boolean onClick(int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        pressProgress = 1.0F;
        playClickSound();
        onPress.run();
        return true;
    }

    @Override
    public boolean contains(int mouseX, int mouseY) {
        int localX = mouseX - getX();
        int localY = mouseY - getY();
        if (localX < 0 || localY < 0 || localX >= getWidth() || localY >= getHeight()) return false;
        if (getWidth() < 6 || getHeight() < 6) return true;
        if (localY == 0 || localY == getHeight() - 1) {
            return localX >= 2 && localX < getWidth() - 2;
        }
        if (localY == 1 || localY == getHeight() - 2) {
            return localX >= 1 && localX < getWidth() - 1;
        }
        return true;
    }

    protected final void playClickSound() {
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float elapsed = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;

        float hoverTarget = isHovered() && isEnabled() ? 1.0F : 0.0F;
        hoverProgress = UiTheme.approach(hoverProgress, hoverTarget, 13.0F, elapsed);
        pressProgress = UiTheme.approach(pressProgress, 0.0F, 18.0F, elapsed);

        boolean borderOnlySelection = selected && selectedBorderHighlight;
        boolean highlightedSelection = selected && !selectedBorderHighlight;
        int selection = selectedColor == null ? 0xFF44BB66 : selectedColor;
        int baseBackground = highlightedSelection
                ? UiTheme.lerpColor(UiTheme.SURFACE, selection, 0.28F) : UiTheme.SURFACE;
        int hoverBackground = highlightedSelection ? UiTheme.lerpColor(UiTheme.SURFACE, selection, 0.42F)
                : UiTheme.hoverBackground();
        int background = isEnabled()
                ? UiTheme.lerpColor(baseBackground, hoverBackground, hoverProgress)
                : UiTheme.PANEL;
        background = UiTheme.lerpColor(background, UiTheme.BACKGROUND, pressProgress * 0.35F);
        int baseBorder = borderOnlySelection
                ? UiTheme.accent() : highlightedSelection ? selection : UiTheme.BORDER;
        int hoverBorder = borderOnlySelection
                ? UiTheme.lerpColor(UiTheme.accent(), 0xFFFFFFFF, 0.25F)
                : highlightedSelection ? UiTheme.lerpColor(selection, 0xFFFFFFFF, 0.25F) : UiTheme.accent();
        int border = isEnabled() ? UiTheme.lerpColor(baseBorder, hoverBorder, hoverProgress)
                : UiTheme.lerpColor(UiTheme.PANEL, UiTheme.BORDER, 0.5F);
        int baseText = highlightedSelection
                ? UiTheme.lerpColor(selection, 0xFFFFFFFF, 0.58F) : UiTheme.TEXT;
        if (textColor != null) baseText = textColor;
        int text = isEnabled()
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
        } else if (getMessage().getString().equalsIgnoreCase("x")) {
            var messageColor = getMessage().getStyle().getColor();
            drawRemoveIcon(ui, isEnabled() && messageColor != null ? 0xFF000000 | messageColor.getRgb() : text);
        } else if (getMessage().getString().equals("i") || getMessage().getString().equals("+")) {
            var renderer = MinecraftClient.getInstance().textRenderer;
            int textX = getX() + Math.round((getWidth() - renderer.getWidth(getMessage())) / 2.0F);
            int textY = getY() + Math.round((getHeight() - renderer.fontHeight) / 2.0F);
            ui.drawText(renderer, getMessage(), textX, textY, text, false);
        } else {
            drawScaledText(ui, getX() + getWidth() / 2, getY() + getHeight() / 2, text, true);
        }
        if (isHovered() && hoverText != null) pendingTooltip = hoverText;
    }

    private void drawScaledText(UIUtils ui, int x, int centerY, int color, boolean centered) {
        ui.pushMatrix();
        ui.translate(x, centerY);
        ui.scale(textScale, textScale);
        var renderer = MinecraftClient.getInstance().textRenderer;
        if (centered) {
            ui.drawCenteredTextWithShadow(
                    renderer, getMessage(), 0, -4, color);
        } else {
            ui.drawTextWithShadow(renderer, getMessage(), 0, -4, color);
        }
        ui.popMatrix();
    }

    private void drawRemoveIcon(UIUtils ui, int color) {
        ui.drawCenteredText("x", getX() + 0.5F + getWidth() / 2.0F, getY() + getHeight() / 2.0F,
                CustomColor.fromInt(color), 1.5F);
    }
}
