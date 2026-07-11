// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of FontRenderer (text drawing helper).
 * Singleton facade that matches Wynntils' public contract; backing impl uses
 * Minecraft's TextRenderer.
 */
package julianh06.wynnextras.wtshim.utils.render;

import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.render.type.HorizontalAlignment;
import julianh06.wynnextras.wtshim.utils.render.type.TextShadow;
import julianh06.wynnextras.wtshim.utils.render.type.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class FontRenderer {
    private static final FontRenderer INSTANCE = new FontRenderer();

    private FontRenderer() {}

    public static FontRenderer getInstance() {
        return INSTANCE;
    }

    public TextRenderer getFont() {
        return MinecraftClient.getInstance().textRenderer;
    }

    // Most common signature used by WynnExtras: renderText(ctx, Text, x, y, color, hAlign, vAlign, shadow)
    public void renderText(
            DrawContext ctx, Text text, float x, float y,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow) {
        if (text == null || color == null || color == CustomColor.NONE) return;
        TextRenderer tr = getFont();
        int w = tr.getWidth(text);
        int h = tr.fontHeight;
        float drawX = switch (hAlign == null ? HorizontalAlignment.LEFT : hAlign) {
            case LEFT -> x;
            case CENTER -> x - w / 2f;
            case RIGHT -> x - w;
        };
        float drawY = switch (vAlign == null ? VerticalAlignment.TOP : vAlign) {
            case TOP -> y;
            case MIDDLE -> y - h / 2f;
            case BOTTOM -> y - h;
        };
        boolean applyShadow = shadow == TextShadow.NORMAL || shadow == TextShadow.OUTLINE;
        ctx.drawText(tr, text, (int) drawX, (int) drawY, color.asInt(), applyShadow);
    }

    // Scale variant used by a few callers
    public void renderText(
            DrawContext ctx, Text text, float x, float y,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow,
            float scale) {
        if (Math.abs(scale - 1f) < 0.0001f) {
            renderText(ctx, text, x, y, color, hAlign, vAlign, shadow);
            return;
        }
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        renderText(ctx, text, 0, 0, color, hAlign, vAlign, shadow);
        matrices.popMatrix();
    }

    // Plain-string overload for legacy call sites
    public void renderText(
            DrawContext ctx, String text, float x, float y,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow) {
        renderText(ctx, Text.literal(text == null ? "" : text), x, y, color, hAlign, vAlign, shadow);
    }

    // StyledText overload (most common WynnExtras call shape)
    public void renderText(
            DrawContext ctx, julianh06.wynnextras.wtshim.core.text.StyledText text, float x, float y,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow) {
        renderText(ctx, text == null ? Text.empty() : text.getComponent(), x, y, color, hAlign, vAlign, shadow);
    }

    public void renderText(
            DrawContext ctx, julianh06.wynnextras.wtshim.core.text.StyledText text, float x, float y,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow,
            float scale) {
        renderText(ctx, text == null ? Text.empty() : text.getComponent(), x, y, color, hAlign, vAlign, shadow, scale);
    }

    // Aligned-in-box variant used by BankOverlay: places text within a bounding rectangle.
    public void renderAlignedTextInBox(
            DrawContext ctx, julianh06.wynnextras.wtshim.core.text.StyledText text,
            float x1, float x2, float y1, float y2, float maxLines,
            CustomColor color, HorizontalAlignment hAlign, VerticalAlignment vAlign, TextShadow shadow) {
        float cx = switch (hAlign == null ? HorizontalAlignment.LEFT : hAlign) {
            case LEFT -> x1;
            case CENTER -> (x1 + x2) / 2f;
            case RIGHT -> x2;
        };
        float cy = switch (vAlign == null ? VerticalAlignment.TOP : vAlign) {
            case TOP -> y1;
            case MIDDLE -> (y1 + y2) / 2f;
            case BOTTOM -> y2;
        };
        renderText(ctx, text, cx, cy, color, hAlign, vAlign, shadow);
    }
}
