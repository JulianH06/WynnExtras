package julianh06.wynnextras.features.buildplanner.gui;

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

    static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        fillRounded(context, x + 2, y + 3, width, height, SHADOW);
        drawRoundedBox(context, x, y, width, height, PANEL, BORDER);
        if (width > 12 && height > 4) {
            context.fill(x + 6, y + 1, x + width - 6, y + 2, lerpColor(BORDER, ACCENT, 0.25F));
        }
    }

    static void drawControl(DrawContext context, int x, int y, int width, int height) {
        drawRoundedBox(context, x, y, width, height, SURFACE, BORDER);
    }

    static void drawRoundedBox(
            DrawContext context, int x, int y, int width, int height, int background, int border) {
        fillRounded(context, x, y, width, height, border);
        fillRounded(context, x + 1, y + 1, width - 2, height - 2, background);
    }

    private static void fillRounded(DrawContext context, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        if (width < 6 || height < 6) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.fill(x + 2, y, x + width - 2, y + 1, color);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, color);
        context.fill(x, y + 2, x + width, y + height - 2, color);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, color);
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color);
    }

    private static int lerpChannel(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

}
