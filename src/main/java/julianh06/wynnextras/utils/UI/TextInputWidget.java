package julianh06.wynnextras.utils.UI;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class TextInputWidget extends Widget {
    protected String input = "";
    protected String placeholder = "Search...";
    protected int cursorPos = 0;

    public boolean blinkToggle = true;
    protected long lastBlink = 0;

    protected CustomColor backgroundColor = CustomColor.fromHexString("FFFFFF");
    protected CustomColor focusedColor = CustomColor.fromHexString("FFEA00");
    protected CustomColor textColor = CustomColor.fromHexString("000000");

    int textXOffset, textYOffset;

    public TextInputWidget(int x, int y, int width, int height, int textXOffset, int textYOffset) {
        super(x, y, width, height);
        this.textXOffset = textXOffset;
        this.textYOffset = textYOffset;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(backgroundColor == null) return;
        CustomColor bg = hovered ? focusedColor : backgroundColor;
        ui.drawRect(x, y, width, height, bg);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        int textX = x + textXOffset;
        int textY = y + textYOffset;

        if (input.isEmpty() && !isFocused()) {
            ui.drawText(placeholder, textX, textY, CustomColor.fromHexString("FFFFFF"));
        } else {
            if (cursorPos > input.length()) cursorPos = input.length();
            ui.drawText(input, textX, textY, textColor);

            long now = System.currentTimeMillis();
            if (now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }

            if (blinkToggle && isFocused()) {
                int cursorX = (int) (textX + (font.getWidth(input.substring(0, cursorPos))) * ui.getScaleFactor());
                ui.drawLine(cursorX, textY - 6, cursorX, textY + 28, 2, textColor);
            }
        }
    }

    @Override
    protected boolean onClick(int button) {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        setFocused(true);
        cursorPos = input.length();
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && cursorPos > 0) {
            input = removeAt(cursorPos, input);
            cursorPos--;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE && cursorPos < input.length()) {
            input = removeAt(cursorPos + 1, input);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPos > 0) {
            cursorPos--;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPos < input.length()) {
            cursorPos++;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onCharTyped(char chr, int modifiers) {
        if (!isFocused() || Character.isISOControl(chr)) return false;
        input = insertAt(cursorPos, String.valueOf(chr), input);
        cursorPos++;
        return true;
    }

    protected String insertAt(int i, String value, String src) {
        return src.substring(0, i) + value + src.substring(i);
    }

    protected String removeAt(int i, String src) {
        if (i <= 0 || i > src.length()) return src;
        return src.substring(0, i - 1) + src.substring(i);
    }

    // Optional getter/setter
    public String getInput() { return input; }
    public void setInput(String input) {
        this.input = input;
        this.cursorPos = Math.min(input.length(), cursorPos);
    }


    public CustomColor getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(CustomColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public CustomColor getFocusedColor() {
        return focusedColor;
    }

    public void setFocusedColor(CustomColor focusedColor) {
        this.focusedColor = focusedColor;
    }

    public CustomColor getTextColor() {
        return textColor;
    }

    public void setTextColor(CustomColor textColor) {
        this.textColor = textColor;
    }

    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
}

