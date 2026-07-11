// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of RenderUtils.
 * Thin drawing helpers over yarn-mapped DrawContext. Signatures match Wynntils' public API
 * so WynnExtras call sites bind. Some advanced methods (arc, scaling texture) use
 * placeholder implementations and should be verified visually in-game (Phase 9).
 */
package julianh06.wynnextras.wtshim.utils.render;

import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class RenderUtils {
    private RenderUtils() {}

    // Placeholders for Wynntils' statically-exposed constants, referenced in a few call sites.
    public static final Object FILLED_BOX = new Object();
    public static final Object INSTANCE_WAYPOINTS = new Object();
    public static final Object allocator = new Object();

    // ---- rectangles ----
    public static void drawRect(DrawContext ctx, CustomColor color, int x, int y, int w, int h) {
        if (color == null || color == CustomColor.NONE) return;
        ctx.fill(x, y, x + w, y + h, color.asInt());
    }

    public static void drawRect(DrawContext ctx, CustomColor color, float x, float y, float w, float h) {
        drawRect(ctx, color, (int) x, (int) y, (int) w, (int) h);
    }

    // Overload with a z-offset (unused by vanilla DrawContext.fill but kept for API parity)
    public static void drawRect(DrawContext ctx, CustomColor color, int x, int y, int z, int w, int h) {
        drawRect(ctx, color, x, y, w, h);
    }

    /** Draw a Texture-enum sprite scaled into (w × h), tinted with the given color. */
    public static void drawSprite(DrawContext ctx, Texture texture, CustomColor color,
                                  float x, float y, float w, float h) {
        if (texture == null) return;
        drawTexturedRect(ctx, texture.identifier(), color, x, y, w, h, texture.width(), texture.height());
    }

    public static void drawRectBorders(
            DrawContext ctx, CustomColor color, int x1, int y1, int x2, int y2, int thickness) {
        if (color == null || color == CustomColor.NONE) return;
        int c = color.asInt();
        ctx.fill(x1, y1, x2, y1 + thickness, c);                 // top
        ctx.fill(x1, y2 - thickness, x2, y2, c);                 // bottom
        ctx.fill(x1, y1 + thickness, x1 + thickness, y2 - thickness, c); // left
        ctx.fill(x2 - thickness, y1 + thickness, x2, y2 - thickness, c); // right
    }

    public static void drawRectBorders(
            DrawContext ctx, CustomColor color, float x1, float y1, float x2, float y2, float thickness) {
        drawRectBorders(ctx, color, (int) x1, (int) y1, (int) x2, (int) y2, (int) thickness);
    }

    // ---- lines ----
    public static void drawLine(
            DrawContext ctx, CustomColor color, float x1, float y1, float x2, float y2, float thickness) {
        if (color == null || color == CustomColor.NONE) return;
        // Minimal fallback: draw a thin axis-aligned rect between endpoints.
        int c = color.asInt();
        int ix1 = (int) Math.min(x1, x2);
        int iy1 = (int) Math.min(y1, y2);
        int ix2 = (int) Math.max(x1, x2);
        int iy2 = (int) Math.max(y1, y2);
        if (ix2 - ix1 <= 1) {
            ctx.fill(ix1, iy1, ix1 + Math.max(1, (int) thickness), iy2, c);
        } else if (iy2 - iy1 <= 1) {
            ctx.fill(ix1, iy1, ix2, iy1 + Math.max(1, (int) thickness), c);
        } else {
            ctx.fill(ix1, iy1, ix2, iy2, c);
        }
    }

    // ---- textured rects ----
    // Signature matches WynnExtras call sites: drawTexturedRect(ctx, Identifier, color, x, y, w, h, texW, texH)
    public static void drawTexturedRect(
            DrawContext ctx, Identifier texture, CustomColor tint,
            float x, float y, float w, float h, int texW, int texH) {
        drawTexturedRect(ctx, texture, tint, x, y, w, h, 0, 0, texW, texH);
    }

    /** 10-arg: render the whole texture scaled into (w × h). */
    public static void drawTexturedRect(
            DrawContext ctx, Identifier texture, CustomColor tint,
            float x, float y, float w, float h, int uOffset, int vOffset, int texW, int texH) {
        drawTexturedRectFull(ctx, texture,
                (int) x, (int) y, (int) w, (int) h,
                (float) uOffset, (float) vOffset,
                texW, texH,  // sample the full remaining texture area (or a single sprite if tex==w×h)
                texW, texH, tintToArgb(tint));
    }

    /** 13-arg with tint and explicit sample region (u/v = sub-sprite width/height). */
    public static void drawTexturedRect(
            DrawContext ctx, Identifier texture, CustomColor tint,
            float x, float y, float w, float h,
            float uOffset, float vOffset, float u, float v, int texW, int texH) {
        drawTexturedRectFull(ctx, texture,
                (int) x, (int) y, (int) w, (int) h,
                uOffset, vOffset, (int) u, (int) v, texW, texH, tintToArgb(tint));
    }

    /** 12-arg (no tint) with explicit sample region. */
    public static void drawTexturedRect(
            DrawContext ctx, Identifier texture,
            float x, float y, float w, float h,
            float uOffset, float vOffset, float u, float v, int texW, int texH) {
        drawTexturedRectFull(ctx, texture,
                (int) x, (int) y, (int) w, (int) h,
                uOffset, vOffset, (int) u, (int) v, texW, texH);
    }

    /** Texture-enum overload matching the 12-arg call in TreeRoomMinimap. */
    public static void drawTexturedRect(
            DrawContext ctx, Texture texture,
            float x, float y, float w, float h,
            float uOffset, float vOffset, float u, float v, int texW, int texH) {
        if (texture == null) return;
        drawTexturedRectFull(ctx, texture.identifier(),
                (int) x, (int) y, (int) w, (int) h,
                uOffset, vOffset, (int) u, (int) v, texW, texH);
    }

    /**
     * Core helper. Calls yarn 1.21.11 DrawContext.drawTexture with explicit sample width/height
     * so sub-sprite sampling works correctly instead of stretching a larger atlas to fit.
     * Signature: drawTexture(pipeline, id, x, y, u, v, renderW, renderH, sampleW, sampleH, texW, texH[, argb]).
     */
    private static void drawTexturedRectFull(
            DrawContext ctx, Identifier texture,
            int x, int y, int renderW, int renderH,
            float u, float v, int sampleW, int sampleH, int texW, int texH) {
        drawTexturedRectFull(ctx, texture, x, y, renderW, renderH, u, v, sampleW, sampleH, texW, texH, 0xFFFFFFFF);
    }

    private static void drawTexturedRectFull(
            DrawContext ctx, Identifier texture,
            int x, int y, int renderW, int renderH,
            float u, float v, int sampleW, int sampleH, int texW, int texH, int argb) {
        if (texture == null) return;
        ctx.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, texture,
                x, y, u, v, renderW, renderH, sampleW, sampleH, texW, texH, argb);
    }

    /**
     * CustomColor tint → ARGB int for DrawContext.drawTexture. CustomColor.NONE (and any
     * negative component, e.g. from NONE.withAlpha(x)) means "no tint" → white. UIUtils'
     * drawImage(alpha) relies on this: NONE.withAlpha(a) must become pure alpha fade.
     */
    private static int tintToArgb(CustomColor tint) {
        if (tint == null) return 0xFFFFFFFF;
        int r = tint.r() < 0 ? 255 : Math.min(tint.r(), 255);
        int g = tint.g() < 0 ? 255 : Math.min(tint.g(), 255);
        int b = tint.b() < 0 ? 255 : Math.min(tint.b(), 255);
        int a = tint.a() < 0 ? 255 : Math.min(tint.a(), 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawScalingTexturedRect(
            DrawContext ctx, Identifier texture, CustomColor tint,
            float x, float y, float w, float h, int texW, int texH) {
        drawTexturedRect(ctx, texture, tint, x, y, w, h, texW, texH);
    }

    // Overload without tint, with 8 int args (used by BankOverlay sign-rendering).
    public static void drawScalingTexturedRect(
            DrawContext ctx, Identifier texture,
            int x, int y, int w, int h, int texW, int texH) {
        drawTexturedRect(ctx, texture, CustomColor.NONE, x, y, w, h, texW, texH);
    }

    public static void drawScalingTexturedRect(
            DrawContext ctx, Identifier texture,
            float x, float y, float w, float h, int texW, int texH) {
        drawTexturedRect(ctx, texture, CustomColor.NONE, x, y, w, h, texW, texH);
    }

    // Overload accepting a julianh06.wynnextras.wtshim.utils.render.Texture directly (extracts Identifier).
    public static void drawScalingTexturedRect(
            DrawContext ctx, Texture texture, CustomColor tint, float x, float y, float w, float h) {
        if (texture == null) return;
        drawTexturedRect(ctx, texture.identifier(), tint, x, y, w, h, texture.width(), texture.height());
    }

    public static void drawTexturedRect(
            DrawContext ctx, Texture texture, int x, int y) {
        if (texture == null) return;
        drawTexturedRect(ctx, texture.identifier(), CustomColor.NONE,
                x, y, texture.width(), texture.height(), texture.width(), texture.height());
    }

    public static void drawTexturedRect(
            DrawContext ctx, Texture texture, float x, float y) {
        if (texture == null) return;
        drawTexturedRect(ctx, texture.identifier(), CustomColor.NONE,
                x, y, texture.width(), texture.height(), texture.width(), texture.height());
    }

    public static void drawTexturedRect(
            DrawContext ctx, Texture texture, CustomColor tint, float x, float y) {
        if (texture == null) return;
        drawTexturedRect(ctx, texture.identifier(), tint,
                x, y, texture.width(), texture.height(), texture.width(), texture.height());
    }

    // ---- arcs / progress circles — placeholder, visual fidelity in Phase 9 ----
    public static void drawArc(
            DrawContext ctx, CustomColor color, float cx, float cy,
            float fraction, int innerRadius, int outerRadius) {
        // Simplified placeholder: draw a small rectangle proportional to fraction.
        if (color == null || color == CustomColor.NONE) return;
        int r = Math.round(outerRadius * Math.min(1f, Math.max(0f, fraction)));
        ctx.fill(
                (int) (cx - r), (int) (cy - r),
                (int) (cx + r), (int) (cy + r),
                color.asInt());
    }

    // ---- world-space helpers (stubbed — used only by waypoint/world features) ----
    public static Object getViewerPos() {
        return null;
    }

    public static int[] calculateEdges(int x, int y, int w, int h) {
        return new int[] {x, y, x + w, y + h};
    }

    // Simple colored-text helper used in a handful of places
    public static void drawText(
            DrawContext ctx, net.minecraft.text.Text text, float x, float y, CustomColor color) {
        if (text == null || color == null || color == CustomColor.NONE) return;
        ctx.drawText(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                text, (int) x, (int) y, color.asInt(), false);
    }
}
