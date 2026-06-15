package julianh06.wynnextras.utils.UI;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorPickerWidget extends Widget {
    private enum Style { LARGE, CONFIG }

    private static final int CONFIG_PICKER_W = 240;
    private static final int CONFIG_PICKER_H = 220;
    private static final int ALPHA_PICKER_EXTRA_H = 40;
    private static final float LARGE_SCALE = 1.8f;
    private static final int SV_CELL = 5;
    private static final int[] PRESET_COLORS = {
            0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA55FF, 0x888888, 0x000000
    };

    private final Supplier<Integer> colorSupplier;
    private final Consumer<Integer> colorConsumer;
    private final Supplier<Float> alphaSupplier;
    private final Consumer<Float> alphaConsumer;
    private final boolean showAlphaSlider;
    private final int resetValue;
    private final int fallbackColor;
    private final Style style;
    private String title = "Color";

    private boolean open = false;
    private float colorH = 0f;
    private float colorS = 0f;
    private float colorV = 1f;
    private int dragMode = 0;
    private boolean hexFocused = false;
    private String hexInput = "";
    private int hexCursor = 0;
    private boolean hexSelectAll = false;
    private boolean openLeft = false;

    public ColorPickerWidget(Supplier<Integer> colorSupplier, Consumer<Integer> colorConsumer) {
        this(colorSupplier, colorConsumer, null, null, false, 0xFFFFFF, 0xFFFFFF, Style.LARGE);
    }

    public ColorPickerWidget(Supplier<Integer> colorSupplier, Consumer<Integer> colorConsumer, int resetValue) {
        this(colorSupplier, colorConsumer, null, null, false, resetValue, 0xFFFFFF, Style.LARGE);
    }

    public ColorPickerWidget(Supplier<Integer> colorSupplier, Consumer<Integer> colorConsumer, Supplier<Float> alphaSupplier, Consumer<Float> alphaConsumer) {
        this(colorSupplier, colorConsumer, alphaSupplier, alphaConsumer, true, 0xFFFFFF, 0xFFFFFF, Style.LARGE);
    }

    public static ColorPickerWidget config(Supplier<Integer> colorSupplier, Consumer<Integer> colorConsumer, int resetValue, int fallbackColor) {
        return new ColorPickerWidget(colorSupplier, colorConsumer, null, null, false, resetValue, fallbackColor, Style.CONFIG);
    }

    public ColorPickerWidget setTitle(String title) {
        this.title = title == null || title.isEmpty() ? "Color" : title;
        return this;
    }

    public ColorPickerWidget openToLeft() {
        this.openLeft = true;
        return this;
    }

    private ColorPickerWidget(Supplier<Integer> colorSupplier, Consumer<Integer> colorConsumer, Supplier<Float> alphaSupplier, Consumer<Float> alphaConsumer, boolean showAlphaSlider, int resetValue, int fallbackColor, Style style) {
        this.colorSupplier = colorSupplier;
        this.colorConsumer = colorConsumer;
        this.alphaSupplier = alphaSupplier;
        this.alphaConsumer = alphaConsumer;
        this.showAlphaSlider = showAlphaSlider;
        this.resetValue = resetValue;
        this.fallbackColor = fallbackColor & 0xFFFFFF;
        this.style = style;
        setPickerColor(currentRgb());
    }

    public boolean isOpen() {
        return open;
    }

    public int getExpandedHeight() {
        return height + 10 + pickerHeight();
    }

    public void close() {
        open = false;
        dragMode = 0;
    }

    @Override
    public boolean contains(int mx, int my) {
        return super.contains(mx, my) || open && isIn(mx, my, pickerX(), pickerY(), pickerWidth(), pickerHeight());
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        drawConfigClosed(mouseX, mouseY);
        if (open) renderPicker(ctx, pickerX(), pickerY(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !enabled || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        int[] toggle = toggleBounds();
        if (isIn(mx, my, toggle[0], toggle[1], toggle[2], toggle[3])) {
            open = !open;
            setPickerColor(currentRgb());
            dragMode = 0;
            hexFocused = false;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        if (!open) return false;

        int px = pickerX();
        int py = pickerY();
        if (!isIn(mx, my, px, py, pickerWidth(), pickerHeight())) {
            close();
            return true;
        }

        int[] hex = hexFieldBounds(px, py);
        if (isIn(mx, my, hex[0], hex[1], hex[2], hex[3])) {
            boolean wasFocused = hexFocused;
            hexFocused = true;
            hexCursor = cursorForMouse(mx, hex[0] + 4);
            hexSelectAll = !wasFocused;
            return true;
        }
        hexFocused = false;
        hexSelectAll = false;

        int[] sv = svBoxBounds(px, py);
        if (isIn(mx, my, sv[0], sv[1], sv[2], sv[3])) {
            dragMode = 2;
            updateSv(mx, my, sv);
            return true;
        }

        int[] hb = hueBarBounds(px, py);
        if (isIn(mx, my, hb[0], hb[1], hb[2], hb[3])) {
            dragMode = 1;
            updateHue(my, hb);
            return true;
        }

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int[] pb = presetBounds(px, py, i);
            if (isIn(mx, my, pb[0], pb[1], pb[2], pb[3])) {
                setPickerColor(PRESET_COLORS[i]);
                setColor(PRESET_COLORS[i]);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        for (int i = 0; i < 2; i++) {
            int[] bb = buttonBounds(px, py, i);
            if (isIn(mx, my, bb[0], bb[1], bb[2], bb[3])) {
                if (i == 0) {
                    setPickerColor(resetValue);
                    setColor(resetValue);
                }
                close();
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (showAlphaSlider) {
            int[] ab = alphaBarBounds(px, py);
            if (isIn(mx, my, ab[0], ab[1], ab[2], ab[3])) {
                dragMode = 3;
                updateAlpha(mx, ab);
                return true;
            }
        }

        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!open || !hexFocused) return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        MinecraftClient client = MinecraftClient.getInstance();

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String clipboard = client.keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) insertHexText(clipboard);
            applyHexInput();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            client.keyboard.setClipboard(hexInput);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_X) {
            client.keyboard.setClipboard(hexInput);
            hexInput = "";
            hexCursor = 0;
            hexSelectAll = false;
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            hexCursor = hexInput.length();
            hexSelectAll = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hexSelectAll) {
                hexInput = "";
                hexCursor = 0;
                hexSelectAll = false;
            } else if (hexCursor > 0) {
                hexInput = hexInput.substring(0, hexCursor - 1) + hexInput.substring(hexCursor);
                hexCursor--;
                applyHexInput();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hexSelectAll) {
                hexInput = "";
                hexCursor = 0;
                hexSelectAll = false;
            } else if (hexCursor < hexInput.length()) {
                hexInput = hexInput.substring(0, hexCursor) + hexInput.substring(hexCursor + 1);
                applyHexInput();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            hexSelectAll = false;
            if (hexCursor > 0) hexCursor--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            hexSelectAll = false;
            if (hexCursor < hexInput.length()) hexCursor++;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            hexSelectAll = false;
            hexCursor = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            hexSelectAll = false;
            hexCursor = hexInput.length();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            hexFocused = false;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onCharTyped(char chr, int modifiers) {
        if (!open || !hexFocused || Character.isISOControl(chr)) return false;
        if (chr == '#' || isHexChar(chr)) {
            insertHexText(String.valueOf(chr));
            applyHexInput();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!visible || !enabled || !open || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || dragMode == 0) return false;
        int px = pickerX();
        int py = pickerY();
        if (dragMode == 1) updateHue(mouseY, hueBarBounds(px, py));
        if (dragMode == 2) updateSv(mouseX, mouseY, svBoxBounds(px, py));
        if (dragMode == 3) updateAlpha(mouseX, alphaBarBounds(px, py));
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragMode == 0) return false;
        dragMode = 0;
        return true;
    }

    private void renderPicker(DrawContext ctx, int px, int py, int mouseX, int mouseY) {
        renderConfigPickerFrame(px, py);

        int[] sv = svBoxBounds(px, py);
        int svCell = u(SV_CELL);
        for (int row = 0; row < sv[3]; row += svCell) {
            float v = 1f - row / (float) Math.max(1, sv[3] - 1);
            for (int col = 0; col < sv[2]; col += svCell) {
                float s = col / (float) Math.max(1, sv[2] - 1);
                ui.drawRect(sv[0] + col, sv[1] + row, svCell, svCell, CustomColor.fromInt(hsvToRgb(colorH, s, v)));
            }
        }

        int markerX = sv[0] + Math.round(colorS * sv[2]);
        int markerY = sv[1] + Math.round((1f - colorV) * sv[3]);
        ui.drawRect(markerX - u(5), markerY - u(5), u(10), u(2), CustomColor.fromInt(0xFFFFFFFF));
        ui.drawRect(markerX - u(5), markerY + u(4), u(10), u(2), CustomColor.fromInt(0xFFFFFFFF));
        ui.drawRect(markerX - u(5), markerY - u(5), u(2), u(10), CustomColor.fromInt(0xFFFFFFFF));
        ui.drawRect(markerX + u(4), markerY - u(5), u(2), u(10), CustomColor.fromInt(0xFFFFFFFF));

        int[] hb = hueBarBounds(px, py);
        for (int row = 0; row < hb[3]; row += u(2)) {
            float h = row / (float) Math.max(1, hb[3] - 1) * 360f;
            ui.drawRect(hb[0], hb[1] + row, hb[2], u(2), CustomColor.fromInt(hsvToRgb(h, 1f, 1f)));
        }
        int hueY = hb[1] + Math.round(colorH / 360f * hb[3]);
        ui.drawRect(hb[0] - u(3), hueY - u(2), hb[2] + u(6), u(4), CustomColor.fromInt(0xFFFFFFFF));

        int[] preview = previewBounds(px, py);
        ui.drawRect(preview[0], preview[1], preview[2], preview[3], CustomColor.fromInt(hsvToRgb(colorH, colorS, colorV)));
        renderHexField(ctx, px, py);
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int[] pb = presetBounds(px, py, i);
            ui.drawRect(pb[0], pb[1], pb[2], pb[3], CustomColor.fromInt(PRESET_COLORS[i]));
        }

        String[] labels = {"Reset", "Close"};
        int[] colors = {0xFFa83232, 0xFF3388AA};
        for (int i = 0; i < labels.length; i++) {
            int[] bb = buttonBounds(px, py, i);
            drawButton(bb[0], bb[1], bb[2], bb[3], labels[i], colors[i], isIn(mouseX, mouseY, bb[0], bb[1], bb[2], bb[3]));
        }

        if (showAlphaSlider) renderAlphaSlider(px, py);
    }

    private void drawButton(int x, int y, int width, int height, String text, int accent, boolean hovered) {
        ui.drawRect(x, y, width, height, CustomColor.fromInt(hovered ? 0xFF80603f : 0xFF6c4f36));
        ui.drawRect(x, y + height - u(3), width, u(2), CustomColor.fromInt(accent));
        ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromInt(0xFFe8dcc8), t(1.45f));
    }

    private int pickerX() {
        if (style == Style.CONFIG) return x + width - CONFIG_PICKER_W;
        return openLeft ? x - pickerWidth() - u(12) : x;
    }

    private int pickerY() {
        if (style == Style.CONFIG) return y + height + 2;
        return openLeft ? y : y + height + u(10);
    }

    private int[] svBoxBounds(int px, int py) {
        return new int[]{px + u(12), py + u(22), u(120), u(120)};
    }

    private int[] hueBarBounds(int px, int py) {
        int[] sv = svBoxBounds(px, py);
        return new int[]{sv[0] + sv[2] + u(10), sv[1], u(14), sv[3]};
    }

    private int[] presetBounds(int px, int py, int index) {
        int cellW = (pickerWidth() - u(20)) / PRESET_COLORS.length;
        return new int[]{px + u(10) + index * cellW, py + u(155), cellW - u(2), u(12)};
    }

    private int[] buttonBounds(int px, int py, int index) {
        int gap = u(6);
        int btnW = (pickerWidth() - u(16) - gap) / 2;
        int btnH = u(18);
        int startX = px + u(8);
        int y = py + pickerHeight() - btnH - u(8);
        return new int[]{startX + index * (btnW + gap), y, btnW, btnH};
    }

    private int[] hexFieldBounds(int px, int py) {
        int[] hb = hueBarBounds(px, py);
        int fieldX = hb[0] + hb[2] + u(10);
        int fieldW = Math.min(u(66), px + pickerWidth() - fieldX - u(8));
        return new int[]{fieldX, svBoxBounds(px, py)[1] + u(34), fieldW, u(16)};
    }

    private int[] alphaBarBounds(int px, int py) {
        return new int[]{px + u(10), py + u(184), pickerWidth() - u(20), u(16)};
    }

    private int pickerHeight() {
        return u(CONFIG_PICKER_H + (showAlphaSlider ? ALPHA_PICKER_EXTRA_H : 0));
    }

    private int pickerWidth() {
        return u(CONFIG_PICKER_W);
    }

    private int[] toggleBounds() {
        return new int[]{x, y, Math.min(width, u(126)), Math.min(height, u(22))};
    }

    private int[] previewBounds(int px, int py) {
        int[] hb = hueBarBounds(px, py);
        int previewX = hb[0] + hb[2] + u(10);
        return new int[]{previewX, svBoxBounds(px, py)[1], Math.min(u(40), px + pickerWidth() - previewX - u(8)), u(30)};
    }

    private void drawConfigClosed(int mouseX, int mouseY) {
        int color = colorSupplier == null ? fallbackColor : colorSupplier.get();
        int rgb = color < 0 ? fallbackColor : color & 0xFFFFFF;
        int[] toggle = toggleBounds();
        ui.drawRect(x, y, u(28), u(22), CustomColor.fromInt(0xFF3a2d24));
        ui.drawRect(x + u(1), y + u(1), u(26), u(20), CustomColor.fromInt(0xFF000000 | rgb));
        ui.drawRect(x + u(36), y, u(90), u(22), CustomColor.fromInt(0xFF3a2d24));
        ui.drawRect(x + u(37), y + u(1), u(88), u(20), CustomColor.fromInt(isIn(mouseX, mouseY, toggle[0], toggle[1], toggle[2], toggle[3]) ? 0xFF705030 : 0xFF6c4f36));
        String label = color < 0 ? "Default" : String.format("#%06X", rgb);
        ui.drawCenteredText(label, x + u(81), y + u(11), CustomColor.fromInt(0xFFe8dcc8), t(1.45f));
    }

    private void renderConfigPickerFrame(int px, int py) {
        ui.drawRect(px, py, pickerWidth(), pickerHeight(), CustomColor.fromInt(0xFF222222));
        ui.drawRect(px, py, pickerWidth(), u(1), CustomColor.fromInt(0xFFFFAA00));
        ui.drawRect(px, py + pickerHeight() - u(1), pickerWidth(), u(1), CustomColor.fromInt(0xFFFFAA00));
        ui.drawRect(px, py, u(1), pickerHeight(), CustomColor.fromInt(0xFFFFAA00));
        ui.drawRect(px + pickerWidth() - u(1), py, u(1), pickerHeight(), CustomColor.fromInt(0xFFFFAA00));
        ui.drawCenteredText(title, px + pickerWidth() / 2f, py + u(10), CustomColor.fromInt(0xFFFFAA00), t(1.45f));
    }

    private void updateHue(double my, int[] hb) {
        colorH = MathHelper.clamp((float) (my - ui.sy(hb[1])) / Math.max(1, ui.sh(hb[3])) * 360f, 0f, 360f);
        applyCurrentColor();
    }

    private void updateSv(double mx, double my, int[] sv) {
        colorS = MathHelper.clamp((float) (mx - ui.sx(sv[0])) / Math.max(1, ui.sw(sv[2])), 0f, 1f);
        colorV = 1f - MathHelper.clamp((float) (my - ui.sy(sv[1])) / Math.max(1, ui.sh(sv[3])), 0f, 1f);
        applyCurrentColor();
    }

    private void updateAlpha(double mx, int[] ab) {
        if (alphaConsumer == null) return;
        alphaConsumer.accept(MathHelper.clamp((float) (mx - ui.sx(ab[0])) / Math.max(1, ui.sw(ab[2])), 0f, 1f));
    }

    private void renderAlphaSlider(int px, int py) {
        int[] ab = alphaBarBounds(px, py);
        float alpha = currentAlpha();
        ui.drawText("Alpha " + Math.round(alpha * 100) + "%", ab[0], ab[1] - u(12), CustomColor.fromInt(0xFF9a8b70), HorizontalAlignment.LEFT, VerticalAlignment.TOP, t(1.15f));
        int rgb = hsvToRgb(colorH, colorS, colorV);
        for (int col = 0; col < ab[2]; col += 4) {
            float aFloat = col / (float) Math.max(1, ab[2] - 1);
            int a = Math.round(aFloat * 255f);
            ui.drawRect(ab[0] + col, ab[1], 4, ab[3], CustomColor.fromInt((a << 24) | rgb));
        }
        int handleX = ab[0] + Math.round(alpha * ab[2]);
        ui.drawRect(handleX - u(2), ab[1] - u(2), u(4), ab[3] + u(4), CustomColor.fromInt(0xFFFFFFFF));
    }

    private int currentRgb() {
        return colorSupplier == null ? 0xFFFFFF : colorSupplier.get() & 0xFFFFFF;
    }

    private float currentAlpha() {
        return alphaSupplier == null ? 1f : MathHelper.clamp(alphaSupplier.get(), 0f, 1f);
    }

    private void setColor(int rgb) {
        if (colorConsumer != null) colorConsumer.accept(rgb < 0 ? rgb : rgb & 0xFFFFFF);
    }

    private void setPickerColor(int color) {
        int c = color & 0xFFFFFF;
        float[] hsv = rgbToHsv((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
        colorH = hsv[0];
        colorS = hsv[1];
        colorV = hsv[2];
        syncHexInput();
    }

    private void applyCurrentColor() {
        syncHexInput();
        setColor(hsvToRgb(colorH, colorS, colorV));
    }

    private void renderHexField(DrawContext ctx, int px, int py) {
        int[] hf = hexFieldBounds(px, py);
        boolean validHex = parseHexInput() != null;
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        int textColor = validHex ? 0xFFFFFFFF : 0xFFFF6666;
        ui.drawRect(hf[0], hf[1], hf[2], hf[3], CustomColor.fromInt(hexFocused ? 0xFFFFAA00 : 0xFFAAAAAA));
        ui.drawRect(hf[0] + u(1), hf[1] + u(1), hf[2] - u(2), hf[3] - u(2), CustomColor.fromInt(0xFF111111));
        if (hexFocused && hexSelectAll && !hexInput.isEmpty()) {
            ui.drawRect(hf[0] + u(3), hf[1] + u(3), Math.min(hf[2] - u(6), Math.round(tr.getWidth(hexInput) * textWidthScale()) + u(2)), hf[3] - u(6), CustomColor.fromInt(0xFF335577));
        }
        ui.drawText(hexInput, hf[0] + u(4), hf[1] + u(4), CustomColor.fromInt(textColor), HorizontalAlignment.LEFT, VerticalAlignment.TOP, t(1.1f));
        if (hexFocused && !hexSelectAll && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursor = Math.max(0, Math.min(hexCursor, hexInput.length()));
            int cx = hf[0] + u(4) + Math.min(hf[2] - u(8), Math.round(tr.getWidth(hexInput.substring(0, cursor)) * textWidthScale()));
            ui.drawRect(cx, hf[1] + u(4), u(1), hf[3] - u(8), CustomColor.fromInt(0xFFFFFFFF));
        }
    }

    private void syncHexInput() {
        hexInput = String.format("#%06X", hsvToRgb(colorH, colorS, colorV) & 0xFFFFFF);
        hexCursor = hexInput.length();
        hexSelectAll = false;
    }

    private void insertHexText(String value) {
        String clean = value.replaceAll("[^#0-9a-fA-F]", "");
        if (clean.isEmpty()) return;
        if (hexSelectAll) {
            hexInput = "";
            hexCursor = 0;
            hexSelectAll = false;
        }
        hexCursor = Math.max(0, Math.min(hexCursor, hexInput.length()));
        hexInput = hexInput.substring(0, hexCursor) + clean + hexInput.substring(hexCursor);
        if (hexInput.length() > 7) hexInput = hexInput.substring(0, 7);
        hexCursor = Math.min(hexInput.length(), hexCursor + clean.length());
    }

    private void applyHexInput() {
        Integer parsed = parseHexInput();
        if (parsed == null) return;
        int oldCursor = hexCursor;
        setPickerColor(parsed);
        setColor(parsed);
        hexCursor = Math.min(hexInput.length(), oldCursor);
    }

    private Integer parseHexInput() {
        String value = hexInput.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) return null;
        for (int i = 0; i < value.length(); i++) {
            if (!isHexChar(value.charAt(i))) return null;
        }
        return Integer.parseInt(value, 16);
    }

    private int cursorForMouse(double mx, int textX) {
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        int cursor = 0;
        while (cursor < hexInput.length()) {
            int charMid = textX + Math.round(tr.getWidth(hexInput.substring(0, cursor + 1)) * textWidthScale()) - Math.round(tr.getWidth(String.valueOf(hexInput.charAt(cursor))) * textWidthScale()) / 2;
            if (mx < charMid) break;
            cursor++;
        }
        return cursor;
    }

    private int u(int value) {
        return style == Style.CONFIG ? value : Math.round(value * LARGE_SCALE);
    }

    private float t(float value) {
        return style == Style.CONFIG ? value : value * LARGE_SCALE;
    }

    private float textWidthScale() {
        return t(1.1f) / 1.1f;
    }

    private static boolean isHexChar(char chr) {
        return (chr >= '0' && chr <= '9')
                || (chr >= 'a' && chr <= 'f')
                || (chr >= 'A' && chr <= 'F');
    }

    private boolean isIn(double mx, double my, int x, int y, int width, int height) {
        return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
    }

    private int hsvToRgb(float h, float s, float v) {
        return java.awt.Color.HSBtoRGB((h % 360f) / 360f, s, v) & 0xFFFFFF;
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        return new float[]{hsb[0] * 360f, hsb[1], hsb[2]};
    }
}