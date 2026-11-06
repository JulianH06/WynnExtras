package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

final class UiTheme {
    static final int BACKGROUND = 0xFF09090B;
    static final int PANEL = 0xFF141416;
    static final int SURFACE = 0xFF202023;
    static final int BORDER = 0xFF3B3B40;
    static final int TEXT = 0xFFF0ECE4;
    static final int MUTED = 0xFFAAA7A2;
    static final int DISABLED = 0xFF77777D;
    static final int SHADOW = 0x88000000;
    private static final int ACCENT = 0xFFD6C9AF;

    private UiTheme() {}

    static int accent() {
        return ACCENT;
    }

    static int accentRgb() {
        return ACCENT & 0xFFFFFF;
    }

    static int hoverBackground() {
        return 0xFF323235;
    }

    static float approach(float current, float target, float speed, float elapsedSeconds) {
        if (elapsedSeconds <= 0.0F) {
            return current;
        }
        float amount = 1.0F - (float) Math.exp(-speed * Math.min(elapsedSeconds, 0.1F));
        return current + (target - current) * amount;
    }

    static int lerpColor(int from, int to, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int alpha = lerpChannel(from >>> 24, to >>> 24, clamped);
        int red = lerpChannel(from >>> 16 & 0xFF, to >>> 16 & 0xFF, clamped);
        int green = lerpChannel(from >>> 8 & 0xFF, to >>> 8 & 0xFF, clamped);
        int blue = lerpChannel(from & 0xFF, to & 0xFF, clamped);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0xFFFFFF;
    }

    static void drawPanel(DrawContext context, UIUtils ui, int x, int y, int width, int height) {
        fillRounded(ui, x + 2, y + 3, width, height, SHADOW);
        drawRoundedBox(context, ui, x, y, width, height, PANEL, BORDER);
        if (width > 12 && height > 4) {
            ui.fill(x + 6, y + 1, x + width - 6, y + 2, lerpColor(BORDER, ACCENT, 0.25F));
        }
    }

    static void drawSquarePanel(UIUtils ui, int x, int y, int width, int height) {
        ui.fill(x + 2, y + 3, x + width + 2, y + height + 3, SHADOW);
        ui.fill(x, y, x + width, y + height, BORDER);
        ui.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL);
        if (width > 12 && height > 4) {
            ui.fill(x + 6, y + 1, x + width - 6, y + 2, lerpColor(BORDER, ACCENT, 0.25F));
        }
    }

    static void drawRoundedBox(
            DrawContext context, UIUtils ui, int x, int y, int width, int height, int background, int border) {
        if (width <= 0 || height <= 0) return;
        var matrices = context.getMatrices();
        float guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        float scaleX = matrices.m00() * guiScale;
        float scaleY = matrices.m11() * guiScale;
        int left = Math.round((matrices.m00() * ui.sx(x) + matrices.m20()) * guiScale);
        int top = Math.round((matrices.m11() * ui.sy(y) + matrices.m21()) * guiScale);
        int right = Math.round((matrices.m00() * ui.sx(x + width) + matrices.m20()) * guiScale);
        int bottom = Math.round((matrices.m11() * ui.sy(y + height) + matrices.m21()) * guiScale);
        int strokeX = Math.max(1, Math.round(scaleX / ui.getScaleFactorF()));
        int strokeY = Math.max(1, Math.round(scaleY / ui.getScaleFactorF()));
        double uiScale = ui.getScaleFactor();
        int offsetX = ui.getXStart();
        int offsetY = ui.getYStart();

        matrices.pushMatrix();
        try {
            matrices.translate((left / guiScale - matrices.m20()) / matrices.m00(),
                    (top / guiScale - matrices.m21()) / matrices.m11());
            matrices.scale(1.0F / scaleX, 1.0F / scaleY);
            ui.setScaleFactor(1.0);
            ui.setOffset(0, 0);
            int pixelWidth = right - left;
            int pixelHeight = bottom - top;
            fillRoundedPixels(ui, 0, 0, pixelWidth, pixelHeight, strokeX, strokeY, border);
            fillRoundedPixels(ui, strokeX, strokeY, pixelWidth - 2 * strokeX, pixelHeight - 2 * strokeY,
                    strokeX, strokeY, background);
        } finally {
            ui.setScaleFactor(uiScale);
            ui.setOffset(offsetX, offsetY);
            matrices.popMatrix();
        }
    }

    private static void fillRoundedPixels(UIUtils ui, int x, int y, int width, int height,
            int stepX, int stepY, int color) {
        if (width <= 0 || height <= 0) return;
        if (width < 6 * stepX || height < 6 * stepY) {
            ui.fill(x, y, x + width, y + height, color);
            return;
        }
        ui.fill(x + 2 * stepX, y, x + width - 2 * stepX, y + stepY, color);
        ui.fill(x + stepX, y + stepY, x + width - stepX, y + 2 * stepY, color);
        ui.fill(x, y + 2 * stepY, x + width, y + height - 2 * stepY, color);
        ui.fill(x + stepX, y + height - 2 * stepY, x + width - stepX, y + height - stepY, color);
        ui.fill(x + 2 * stepX, y + height - stepY, x + width - 2 * stepX, y + height, color);
    }

    static void enableScissor(DrawContext context, UIUtils ui, int left, int top, int right, int bottom) {
        var matrices = context.getMatrices();
        int x = Math.round(ui.sx(left));
        int y = Math.round(ui.sy(top));
        context.enableScissor(x, y,
                scissorEnd(x, Math.round(ui.sx(right)), matrices.m00(), matrices.m20()),
                scissorEnd(y, Math.round(ui.sy(bottom)), matrices.m11(), matrices.m21()));
    }

    private static int scissorEnd(int start, int end, float scale, float translation) {
        // ScreenRect.transform floors the origin AND the extent instead of rounding the far edge out.
        float transformedStart = scale * start + translation;
        int extent = (int) Math.ceil(scale * end + translation) - (int) Math.floor(transformedStart);
        int length = (int) Math.ceil(extent / (double) scale);
        while (Math.floor(scale * (start + length) + translation - transformedStart) < extent) length++;
        return start + length;
    }

    private static void fillRounded(UIUtils ui, int x, int y, int width, int height, int color) {
        fillRoundedPixels(ui, x, y, width, height, 1, 1, color);
    }

    private static int lerpChannel(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }
}
