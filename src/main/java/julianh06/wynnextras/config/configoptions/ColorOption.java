package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class ColorOption extends ConfigOption {
    private static final int PICKER_W = 240;
    private static final int PICKER_H = 220;
    private static final int SV_CELL = 4;
    private static final int[] PRESET_COLORS = {
            0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA55FF, 0x888888, 0x000000
    };

    private final Supplier<Integer> getter;
    private final Consumer<Integer> setter;
    private final int resetValue;
    private final int fallbackColor;

    private boolean open = false;
    private float colorH = 0f, colorS = 0f, colorV = 1f;
    private int dragMode = 0;
    private float svCacheHue = -1f;
    private int[] svCacheColors = null;
    private int svCacheCols = 0, svCacheRows = 0;
    private int[] hueCacheColors = null;
    private int hueCacheRows = 0;

    public ColorOption(String name, String desc, Supplier<Integer> get, Consumer<Integer> set, int resetValue, int fallbackColor) {
        super(name, desc);
        this.getter = get;
        this.setter = set;
        this.resetValue = resetValue;
        this.fallbackColor = fallbackColor & 0xFFFFFF;
    }

    @Override
    public int controlWidth() { return 135; }

    @Override
    public int getHeight(int contentW) {
        return open ? super.getHeight(contentW) + PICKER_H + 12 : super.getHeight(contentW);
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        int closedH = super.getHeight(w);
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int swatchX = x + w - 128;
        int swatchY = y + 10;
        int color = getter.get();
        drawSwatch(ctx, swatchX, swatchY, color < 0 ? fallbackColor : color);
        ctx.fill(swatchX + 36, swatchY, swatchX + 126, swatchY + 22, BORDER_DARK);
        ctx.fill(swatchX + 37, swatchY + 1, swatchX + 125, swatchY + 21,
                isIn(mx, my, swatchX + 36, swatchY, 90, 22) ? PARCHMENT_HOVER : PARCHMENT);
        String label = color < 0 ? "Default" : String.format("#%06X", color & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, label, swatchX + 81, swatchY + 7, TEXT_LIGHT);

        if (open) {
            renderPicker(ctx, x + w - PICKER_W - 10, y + closedH + 2);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        if (btn != 0) return open;

        int closedH = super.getHeight(w);
        int swatchX = x + w - 128;
        int swatchY = y + 10;
        if (isIn(mx, my, swatchX, swatchY, 126, 22)) {
            open = !open;
            if (open) setPickerColor(getter.get() < 0 ? fallbackColor : getter.get());
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        if (!open) return false;

        int px = x + w - PICKER_W - 10;
        int py = y + closedH + 2;
        if (!isIn(mx, my, px, py, PICKER_W, PICKER_H)) {
            open = false;
            return true;
        }

        int[] cb = closeButtonBounds(px, py);
        if (isIn(mx, my, cb[0], cb[1], cb[2], cb[3])) {
            open = false;
            return true;
        }

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
                return true;
            }
        }

        for (int i = 0; i < 3; i++) {
            int[] bb = buttonBounds(px, py, i);
            if (isIn(mx, my, bb[0], bb[1], bb[2], bb[3])) {
                if (i == 0) setter.accept(resetValue);
                if (i == 1) setter.accept(hsvToRgb(colorH, colorS, colorV));
                open = false;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragMode = 0;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
        if (!open || dragMode == 0) return false;
        int py = y + super.getHeight(w) + 2;
        int px = x + w - PICKER_W - 10;
        if (dragMode == 1) updateHue(my, hueBarBounds(px, py));
        if (dragMode == 2) updateSv(mx, my, svBoxBounds(px, py));
        return true;
    }

    private void renderPicker(DrawContext ctx, int px, int py) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(px, py, px + PICKER_W, py + PICKER_H, 0xFF222222);
        ctx.fill(px, py, px + PICKER_W, py + 1, 0xFFFFAA00);
        ctx.fill(px, py + PICKER_H - 1, px + PICKER_W, py + PICKER_H, 0xFFFFAA00);
        ctx.fill(px, py, px + 1, py + PICKER_H, 0xFFFFAA00);
        ctx.fill(px + PICKER_W - 1, py, px + PICKER_W, py + PICKER_H, 0xFFFFAA00);
        ctx.drawCenteredTextWithShadow(tr, "Color: " + name, px + PICKER_W / 2, py + 6, 0xFFFFAA00);

        int[] sv = svBoxBounds(px, py);
        ensureSvCache(sv[2], sv[3]);
        for (int row = 0; row < svCacheRows; row++) {
            int y0 = sv[1] + row * SV_CELL;
            int y1 = Math.min(sv[1] + sv[3], y0 + SV_CELL);
            for (int col = 0; col < svCacheCols; col++) {
                int x0 = sv[0] + col * SV_CELL;
                int x1 = Math.min(sv[0] + sv[2], x0 + SV_CELL);
                ctx.fill(x0, y0, x1, y1, svCacheColors[row * svCacheCols + col]);
            }
        }
        drawColorCursor(ctx, sv[0] + (int) (colorS * (sv[2] - 1)), sv[1] + (int) ((1f - colorV) * (sv[3] - 1)));

        int[] hb = hueBarBounds(px, py);
        ensureHueCache(hb[3]);
        for (int row = 0; row < hueCacheRows; row++) {
            ctx.fill(hb[0], hb[1] + row, hb[0] + hb[2], hb[1] + row + 1, hueCacheColors[row]);
        }
        int hueCy = hb[1] + (int) ((colorH / 360f) * (hb[3] - 1));
        ctx.fill(hb[0] - 2, hueCy - 2, hb[0] + hb[2] + 2, hueCy - 1, 0xFFFFFFFF);
        ctx.fill(hb[0] - 2, hueCy + 1, hb[0] + hb[2] + 2, hueCy + 2, 0xFFFFFFFF);
        ctx.fill(hb[0] - 2, hueCy - 1, hb[0] - 1, hueCy + 1, 0xFFFFFFFF);
        ctx.fill(hb[0] + hb[2] + 1, hueCy - 1, hb[0] + hb[2] + 2, hueCy + 1, 0xFFFFFFFF);

        int currentRgb = hsvToRgb(colorH, colorS, colorV);
        int previewX = hb[0] + hb[2] + 10;
        int previewW = Math.min(40, px + PICKER_W - previewX - 8);
        ctx.fill(previewX, sv[1], previewX + previewW, sv[1] + 30, 0xFF000000 | currentRgb);
        ctx.fill(previewX - 1, sv[1] - 1, previewX + previewW + 1, sv[1], 0xFFAAAAAA);
        ctx.fill(previewX - 1, sv[1] + 30, previewX + previewW + 1, sv[1] + 31, 0xFFAAAAAA);
        ctx.fill(previewX - 1, sv[1], previewX, sv[1] + 30, 0xFFAAAAAA);
        ctx.fill(previewX + previewW, sv[1], previewX + previewW + 1, sv[1] + 30, 0xFFAAAAAA);
        ctx.drawTextWithShadow(tr, String.format("#%06X", currentRgb & 0xFFFFFF), previewX, sv[1] + 36, 0xFFFFFFFF);

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int[] pb = presetBounds(px, py, i);
            ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + pb[3], 0xFF000000 | PRESET_COLORS[i]);
            ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + 1, 0xFF555555);
            ctx.fill(pb[0], pb[1] + pb[3] - 1, pb[0] + pb[2], pb[1] + pb[3], 0xFF555555);
        }

        String[] labels = {"Reset", "Apply", "Close"};
        int[] btnColors = {0xFFAA3333, 0xFF338833, 0xFF3388AA};
        for (int i = 0; i < 3; i++) {
            int[] bb = buttonBounds(px, py, i);
            ctx.fill(bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3], btnColors[i]);
            ctx.drawCenteredTextWithShadow(tr, labels[i], bb[0] + bb[2] / 2, bb[1] + 5, 0xFFFFFFFF);
        }

        int[] cb = closeButtonBounds(px, py);
        ctx.fill(cb[0], cb[1], cb[0] + cb[2], cb[1] + cb[3], 0xFF552222);
        ctx.drawCenteredTextWithShadow(tr, "X", cb[0] + cb[2] / 2, cb[1] + 2, 0xFFFFFFFF);
    }

    private void drawSwatch(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 28, y + 22, BORDER_DARK);
        ctx.fill(x + 1, y + 1, x + 27, y + 21, 0xFF000000 | (color & 0xFFFFFF));
    }

    private int[] closeButtonBounds(int px, int py) {
        return new int[]{px + PICKER_W - 16, py + 4, 12, 12};
    }

    private int[] svBoxBounds(int px, int py) {
        return new int[]{px + 12, py + 22, 120, 120};
    }

    private int[] hueBarBounds(int px, int py) {
        int[] sv = svBoxBounds(px, py);
        return new int[]{sv[0] + sv[2] + 10, sv[1], 14, sv[3]};
    }

    private int[] presetBounds(int px, int py, int presetIdx) {
        int cellW = (PICKER_W - 20) / PRESET_COLORS.length;
        return new int[]{px + 10 + presetIdx * cellW, py + 155, cellW - 2, 12};
    }

    private int[] buttonBounds(int px, int py, int btnIdx) {
        int gap = 6;
        int btnW = (PICKER_W - 16 - gap * 2) / 3;
        int btnH = 18;
        int startX = px + 8;
        int y = py + PICKER_H - btnH - 8;
        return new int[]{startX + btnIdx * (btnW + gap), y, btnW, btnH};
    }

    private void ensureSvCache(int width, int height) {
        int cols = (int) Math.ceil((double) width / SV_CELL);
        int rows = (int) Math.ceil((double) height / SV_CELL);
        if (svCacheColors != null && cols == svCacheCols && rows == svCacheRows && colorH == svCacheHue) return;
        svCacheCols = cols;
        svCacheRows = rows;
        svCacheColors = new int[cols * rows];
        svCacheHue = colorH;
        for (int row = 0; row < rows; row++) {
            float v = 1f - (row * SV_CELL) / (float) (height - 1);
            if (v < 0) v = 0;
            for (int col = 0; col < cols; col++) {
                float s = (col * SV_CELL) / (float) (width - 1);
                if (s > 1) s = 1;
                svCacheColors[row * cols + col] = 0xFF000000 | hsvToRgb(colorH, s, v);
            }
        }
    }

    private void ensureHueCache(int height) {
        if (hueCacheColors != null && hueCacheRows == height) return;
        hueCacheRows = height;
        hueCacheColors = new int[height];
        for (int row = 0; row < height; row++) {
            float h = (row / (float) (height - 1)) * 360f;
            hueCacheColors[row] = 0xFF000000 | hsvToRgb(h, 1f, 1f);
        }
    }

    private void updateSv(double mx, double my, int[] sv) {
        colorS = Math.max(0f, Math.min(1f, (float) (mx - sv[0]) / (sv[2] - 1)));
        colorV = Math.max(0f, Math.min(1f, 1f - (float) (my - sv[1]) / (sv[3] - 1)));
    }

    private void updateHue(double my, int[] hb) {
        colorH = Math.max(0f, Math.min(360f, (float) (my - hb[1]) / (hb[3] - 1) * 360f));
    }

    private void setPickerColor(int color) {
        int c = color & 0xFFFFFF;
        float[] hsv = rgbToHsv((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
        colorH = hsv[0];
        colorS = hsv[1];
        colorV = hsv[2];
    }

    private static void drawColorCursor(DrawContext ctx, int cx, int cy) {
        ctx.fill(cx - 4, cy - 4, cx + 5, cy - 3, 0xFF000000);
        ctx.fill(cx - 4, cy + 3, cx + 5, cy + 4, 0xFF000000);
        ctx.fill(cx - 4, cy - 3, cx - 3, cy + 3, 0xFF000000);
        ctx.fill(cx + 3, cy - 3, cx + 4, cy + 3, 0xFF000000);
        ctx.fill(cx - 3, cy - 3, cx + 4, cy - 2, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy + 2, cx + 4, cy + 3, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy - 2, cx - 2, cy + 2, 0xFFFFFFFF);
        ctx.fill(cx + 2, cy - 2, cx + 3, cy + 2, 0xFFFFFFFF);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = v - c;
        float rf = 0, gf = 0, bf = 0;
        if (h < 60) { rf = c; gf = x; }
        else if (h < 120) { rf = x; gf = c; }
        else if (h < 180) { gf = c; bf = x; }
        else if (h < 240) { gf = x; bf = c; }
        else if (h < 300) { rf = x; bf = c; }
        else { rf = c; bf = x; }
        int r = Math.round((rf + m) * 255);
        int g = Math.round((gf + m) * 255);
        int b = Math.round((bf + m) * 255);
        return (r << 16) | (g << 8) | b;
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h = 0;
        if (d != 0) {
            if (max == rf) h = 60 * (((gf - bf) / d) % 6);
            else if (max == gf) h = 60 * ((bf - rf) / d + 2);
            else h = 60 * ((rf - gf) / d + 4);
        }
        if (h < 0) h += 360;
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
