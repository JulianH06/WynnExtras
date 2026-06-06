package julianh06.wynnextras.utils.UI;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class TextInputWidget extends Widget {
    protected String input = "";
    protected String placeholder = "Search...";
    protected int cursorPos = 0;
    protected int selectionAnchor = 0;
    protected int horizontalTextOffset = 0;
    protected boolean draggingSelection = false;

    protected boolean blinkToggle = true;
    protected long lastBlink = 0;

    protected CustomColor backgroundColor = null;
    protected CustomColor focusedColor = null;
    protected CustomColor textColor = CustomColor.fromHexString("FFFFFF");
    protected CustomColor placeholderColor = CustomColor.fromHexString("AAAAAA");
    protected CustomColor cursorColor = CustomColor.fromHexString("FFFFFF");
    protected CustomColor selectionColor = CustomColor.fromInt(0xAA3366CC);

    int textXOffset, textYOffset;
    protected float textScale;
    protected int maxLength = -1;
    protected Predicate<Character> characterFilter = character -> true;
    protected Consumer<String> onChange = null;

    public TextInputWidget(int x, int y, int width, int height, int textXOffset, int textYOffset) {
        this(x, y, width, height, textXOffset, textYOffset, 3);
    }

    public TextInputWidget(int x, int y, int width, int height, int textXOffset, int textYOffset, float textScale) {
        super(x, y, width, height);
        this.textXOffset = textXOffset;
        this.textYOffset = textYOffset;
        this.textScale = textScale;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(backgroundColor == null) return;
        CustomColor bg = hovered && focusedColor != null ? focusedColor : backgroundColor;
        ui.drawRect(x, y, width, height, bg);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer font = client.textRenderer;

        int textX = x + textXOffset;
        int textY = y + textYOffset;
        int textWidth = Math.max(0, width - textXOffset * 2);
        int textHeight = (int) Math.ceil(Math.max(height, font.fontHeight * textScale));

        if (input.isEmpty() && !isFocused()) {
            ui.drawText(placeholder, textX, textY, placeholderColor, textScale);
        } else {
            clampCursorAndSelection();
            ensureCursorVisible(textWidth);
            ctx.enableScissor((int) ui.sx(textX), (int) ui.sy(y), (int) ui.sx(textX + textWidth), (int) ui.sy(y + textHeight));

            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();
            if (isFocused() && selectionStart != selectionEnd) {
                int startX = textX + textWidth(input.substring(0, selectionStart)) - horizontalTextOffset;
                int endX = textX + textWidth(input.substring(0, selectionEnd)) - horizontalTextOffset;
                ui.drawRect(startX, y + 3, Math.max(1, endX - startX), Math.max(1, height - 6), selectionColor);
            }

            ui.drawText(input, textX - horizontalTextOffset, textY, textColor, textScale);

            long now = System.currentTimeMillis();
            if (now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }

            if ((blinkToggle || input.isEmpty()) && isFocused()) {
                int cursorX = textX + textWidth(input.substring(0, cursorPos)) - horizontalTextOffset;
                ui.drawLine(cursorX, textY - 2 * textScale, cursorX, textY + 10 * textScale, Math.max(1f, 0.75f * textScale), cursorColor);
            }

            ctx.disableScissor();
        }
    }

    @Override
    public boolean onClick(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            clearInput();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            setFocused(true);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        setFocused(true);
        cursorPos = input.length();
        selectionAnchor = cursorPos;
        resetBlink();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !enabled) return false;
        if (!contains((int) mx, (int) my)) {
            draggingSelection = false;
            if (focused) setFocused(false);
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            clearInput();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            setFocused(true);
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        setFocused(true);
        cursorPos = getCursorIndexAt(mx);
        selectionAnchor = isShiftDown() ? selectionAnchor : cursorPos;
        draggingSelection = true;
        resetBlink();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !enabled || !draggingSelection || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        cursorPos = getCursorIndexAt(mouseX);
        resetBlink();
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingSelection) {
            draggingSelection = false;
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            selectionAnchor = cursorPos;
            draggingSelection = false;
        }
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clip = mc.keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                String clean = clip.replaceAll("[\\r\\n\\t]", " ");
                insertText(clean);
            }
            return true;
        }
        if (ctrl && (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_X)) {
            if (hasSelection()) {
                mc.keyboard.setClipboard(input.substring(getSelectionStart(), getSelectionEnd()));
                if (keyCode == GLFW.GLFW_KEY_X) deleteSelection();
            } else if (keyCode == GLFW.GLFW_KEY_C) {
                mc.keyboard.setClipboard(input);
            }
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            cursorPos = input.length();
            selectionAnchor = 0;
            resetBlink();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) deleteSelection();
            else if ((ctrl || shift) && cursorPos > 0) deleteWordBackward();
            else if (cursorPos > 0) {
                input = removeAt(cursorPos, input);
                cursorPos--;
                selectionAnchor = cursorPos;
                notifyChanged();
            }
            resetBlink();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) deleteSelection();
            else if ((ctrl || shift) && cursorPos < input.length()) deleteWordForward();
            else if (cursorPos < input.length()) {
                input = removeAt(cursorPos + 1, input);
                selectionAnchor = cursorPos;
                notifyChanged();
            }
            resetBlink();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            moveCursor(ctrl ? previousWord(cursorPos) : Math.max(0, cursorPos - 1), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            moveCursor(ctrl ? nextWord(cursorPos) : Math.min(input.length(), cursorPos + 1), shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            moveCursor(0, shift);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            moveCursor(input.length(), shift);
            return true;
        }

        return false;
    }

    @Override
    protected boolean onCharTyped(char chr, int modifiers) {
        if (!isFocused() || Character.isISOControl(chr)) return false;
        if (!characterFilter.test(chr)) return false;
        insertText(String.valueOf(chr));
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
        this.input = sanitize(input == null ? "" : input);
        this.cursorPos = Math.min(this.input.length(), cursorPos);
        this.selectionAnchor = cursorPos;
        ensureCursorVisible(Math.max(0, width - textXOffset * 2));
    }

    public void setInputAndMoveCursorToEnd(String input) {
        this.input = sanitize(input == null ? "" : input);
        this.cursorPos = this.input.length();
        this.selectionAnchor = cursorPos;
        ensureCursorVisible(Math.max(0, width - textXOffset * 2));
    }

    public void clearInput() {
        if (input.isEmpty() && cursorPos == 0 && selectionAnchor == 0) return;
        input = "";
        cursorPos = 0;
        selectionAnchor = 0;
        horizontalTextOffset = 0;
        draggingSelection = false;
        resetBlink();
        notifyChanged();
    }

    public void setBlinkToggle(boolean blinkToggle) {
        this.blinkToggle = blinkToggle;
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
    public void setPlaceholderColor(CustomColor placeholderColor) { this.placeholderColor = placeholderColor; }

    public void setCursorColor(CustomColor cursorColor) { this.cursorColor = cursorColor; }
    public void setSelectionColor(CustomColor selectionColor) { this.selectionColor = selectionColor; }
    public void setTextScale(float textScale) { this.textScale = textScale; }
    public void setTextOffset(int textXOffset, int textYOffset) {
        this.textXOffset = textXOffset;
        this.textYOffset = textYOffset;
    }
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        if (maxLength >= 0 && input.length() > maxLength) {
            input = input.substring(0, maxLength);
            cursorPos = Math.min(cursorPos, input.length());
            selectionAnchor = Math.min(selectionAnchor, input.length());
            notifyChanged();
        }
    }
    public void setCharacterFilter(Predicate<Character> characterFilter) {
        this.characterFilter = characterFilter == null ? character -> true : characterFilter;
        String sanitized = sanitize(input);
        if (!sanitized.equals(input)) {
            input = sanitized;
            cursorPos = Math.min(cursorPos, input.length());
            selectionAnchor = Math.min(selectionAnchor, input.length());
            notifyChanged();
        }
    }
    public void setOnChange(Consumer<String> onChange) { this.onChange = onChange; }

    protected void insertText(String value) {
        if (value == null || value.isEmpty()) return;
        String clean = sanitize(value);
        if (clean.isEmpty()) return;
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        int available = maxLength < 0 ? clean.length() : maxLength - (input.length() - (selectionEnd - selectionStart));
        if (available <= 0) return;
        if (clean.length() > available) clean = clean.substring(0, available);
        input = input.substring(0, selectionStart) + clean + input.substring(selectionEnd);
        cursorPos = selectionStart + clean.length();
        selectionAnchor = cursorPos;
        resetBlink();
        notifyChanged();
    }

    protected void deleteSelection() {
        if (!hasSelection()) return;
        deleteRange(getSelectionStart(), getSelectionEnd());
    }

    protected void deleteWordBackward() {
        deleteRange(previousWord(cursorPos), cursorPos);
    }

    protected void deleteWordForward() {
        deleteRange(cursorPos, nextWord(cursorPos));
    }

    protected void deleteRange(int start, int end) {
        start = Math.clamp(start, 0, input.length());
        end = Math.clamp(end, 0, input.length());
        if (start == end) return;
        int selectionStart = Math.min(start, end);
        int selectionEnd = Math.max(start, end);
        input = input.substring(0, selectionStart) + input.substring(selectionEnd);
        cursorPos = selectionStart;
        selectionAnchor = cursorPos;
        resetBlink();
        notifyChanged();
    }

    protected boolean hasSelection() {
        return cursorPos != selectionAnchor;
    }

    protected int getSelectionStart() {
        return Math.min(cursorPos, selectionAnchor);
    }

    protected int getSelectionEnd() {
        return Math.max(cursorPos, selectionAnchor);
    }

    protected void moveCursor(int newCursorPos, boolean selecting) {
        cursorPos = Math.clamp(newCursorPos, 0, input.length());
        if (!selecting) selectionAnchor = cursorPos;
        resetBlink();
    }

    protected int previousWord(int from) {
        int index = Math.clamp(from, 0, input.length());
        while (index > 0 && Character.isWhitespace(input.charAt(index - 1))) index--;
        while (index > 0 && !Character.isWhitespace(input.charAt(index - 1))) index--;
        return index;
    }

    protected int nextWord(int from) {
        int index = Math.clamp(from, 0, input.length());
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        while (index < input.length() && !Character.isWhitespace(input.charAt(index))) index++;
        return index;
    }

    protected int getCursorIndexAt(double mouseX) {
        int logicalMouseX = (int) Math.round((mouseX - ui.getXStart()) * ui.getScaleFactor());
        int localTextX = logicalMouseX - (x + textXOffset) + horizontalTextOffset;
        if (localTextX <= 0) return 0;
        for (int i = 1; i <= input.length(); i++) {
            int previousWidth = textWidth(input.substring(0, i - 1));
            int currentWidth = textWidth(input.substring(0, i));
            if (localTextX < previousWidth + (currentWidth - previousWidth) / 2) return i - 1;
        }
        return input.length();
    }

    protected void ensureCursorVisible(int textWidth) {
        if (textWidth <= 0) {
            horizontalTextOffset = 0;
            return;
        }
        int cursorX = textWidth(input.substring(0, Math.min(cursorPos, input.length())));
        if (cursorX - horizontalTextOffset > textWidth - 2) {
            horizontalTextOffset = cursorX - textWidth + 2;
        } else if (cursorX - horizontalTextOffset < 0) {
            horizontalTextOffset = cursorX;
        }
        horizontalTextOffset = Math.max(0, horizontalTextOffset);
    }

    protected int textWidth(String text) {
        return (int) Math.ceil(MinecraftClient.getInstance().textRenderer.getWidth(text) * textScale);
    }

    protected void clampCursorAndSelection() {
        cursorPos = Math.clamp(cursorPos, 0, input.length());
        selectionAnchor = Math.clamp(selectionAnchor, 0, input.length());
    }

    protected String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isISOControl(character) || !characterFilter.test(character)) continue;
            if (maxLength >= 0 && builder.length() >= maxLength) break;
            builder.append(character);
        }
        return builder.toString();
    }

    protected void resetBlink() {
        blinkToggle = true;
        lastBlink = System.currentTimeMillis();
    }

    protected void notifyChanged() {
        if (onChange != null) onChange.accept(input);
    }

    private boolean isShiftDown() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
