package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.qol.AttackTimerMenu;
import julianh06.wynnextras.features.raid.RaidSessionTracker;
import julianh06.wynnextras.features.raid.TreeRoomMinimap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudEditScreen extends Screen {
    private double focusedMouseX, focusedMouseY;

    private static class HudElement {
        final String id;
        final String preview;
        final int defaultColor;
        int x, y, w;
        int customH = -1; // -1 = use default H
        boolean fixedSize = false; // true = don't allow scaling, don't center-shift in init
        boolean topLeft = false;  // true = position stored as top-left (not center)
        float scale;
        boolean dragging;
        int dragOffX, dragOffY;
        boolean snappedX, snappedY;
        WynnExtrasConfig.Align alignment;

        HudElement(String id, String preview, int x, int y, float scale, WynnExtrasConfig.Align alignment) {
            this(id, preview, x, y, scale, alignment, 0xFFFFFFFF);
        }

        HudElement(String id, String preview, int x, int y, float scale, WynnExtrasConfig.Align alignment, int defaultColor) {
            this.id = id;
            this.preview = preview;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alignment = alignment;
            this.defaultColor = defaultColor | 0xFF000000;
        }

        int sw() { return (int) (w * scale); }
        int sh() { return (int) ((customH > 0 ? customH : H) * scale); }
        boolean hovered(double mx, double my) {
            return mx >= x - 2 && mx <= x + sw() + 2 && my >= y - 2 && my <= y + sh() + 2;
        }
    }

    private static final int H = 14;
    private static final int SNAP_DIST = 8;
    private final List<HudElement> elements = new ArrayList<>();

    // Color edit popup state (HSV internally, converted to RGB on save)
    private HudElement colorEditTarget = null;
    private float colorH = 0f, colorS = 0f, colorV = 1f; // H: 0-360, S/V: 0-1
    private int colorDragMode = 0; // 0=none, 1=hue, 2=sv

    // Cached SV square (rebuilt when hue changes) and hue bar (built once).
    private static final int SV_CELL = 4; // px per cell (lower = more detail, more draws)
    private float svCacheHue = -1f;
    private int[] svCacheColors = null; // row-major, cells[row * cols + col]
    private int svCacheCols = 0, svCacheRows = 0;
    private int[] hueCacheColors = null; // 1 per row
    private int hueCacheRows = 0;

    private static final int[] PRESET_COLORS = {
            0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
            0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA55FF, 0x888888, 0x000000
    };

    private final Screen parent;

    public HudEditScreen() { this(null); }

    public HudEditScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

        this.parent = parent;

        if (c.provokeTimerToggle) {
            int provokeColor = c.provokeTimerColor.getRGB();
            elements.add(new HudElement("provoke", "Provoke: 7s",
                    c.provokeTimerX, c.provokeTimerY, c.provokeTimerScale, c.provokeTimerAlignment, provokeColor));
        }
        if (c.totemTimerEnabled) {
            String totemText = "PlayerName's Totem: 38s";
            int totemColor = 0xFF44FF44;
            List<TotemTimer.TotemInfo> totems = TotemTimer.getTotems();
            if (!totems.isEmpty()) {
                TotemTimer.TotemInfo t = totems.get(0);
                totemText = t.owner() + "'s Totem: " + t.timeText();
            }
            elements.add(new HudElement("totem", totemText,
                    c.totemTimerX, c.totemTimerY, c.totemTimerScale, c.totemTimerAlignment, totemColor));
        }
        if (c.bloodSorrowTimerEnabled) {
            elements.add(new HudElement("blood", "Blood Sorrow: 1.7s",
                    c.bloodSorrowTimerX, c.bloodSorrowTimerY, c.bloodSorrowTimerScale, c.bloodSorrowAlignment));
        }
        if (c.totemTimerEnabled && c.totemTimerWarningText) {
            int wx = c.totemWarningX;
            if (wx == -1) wx = 200;
            int warningColor = c.totemTimerWarningTextColor.getRGB();
            elements.add(new HudElement("warning", "RECAST TOTEM!",
                    wx, c.totemWarningY, c.totemWarningScale, c.totemWarningAlignment, warningColor));
        }
        if (c.radiantHudEnabled) {
            String radiantText = "Radiant 1:30";
            int radiantColor = 0xFF44FF44;
            List<RadiantHud.CachedEntry> radiant = RadiantHud.getCachedEntries();
            if (!radiant.isEmpty()) {
                RadiantHud.CachedEntry entry = radiant.get(0);
                radiantText = entry.display();
                radiantColor = entry.color();
            }
            HudElement radiantEl = new HudElement("radiant", radiantText,
                    c.radiantHudX, c.radiantHudY, c.radiantHudScale, WynnExtrasConfig.Align.CENTER, radiantColor);
            radiantEl.topLeft = true;
            elements.add(radiantEl);
        }
        if (c.professionOverlayEnabled) {
            HudElement professionEl = new HudElement("profession", "Mining Lv. 87  1234/5678 (21.7%)",
                    c.professionOverlayX, c.professionOverlayY, c.professionOverlayScale, WynnExtrasConfig.Align.CENTER, 0xFFFFFF00);
            professionEl.topLeft = true;
            elements.add(professionEl);
        }
        if (c.tnaTreeMap) {
            HudElement treemap = new HudElement("treemap", "Tree Minimap",
                    c.treeMapX, c.treeMapY, c.tnaTreeMapScale, WynnExtrasConfig.Align.LEFT);
            treemap.customH = 130;
            treemap.w = 130;
            treemap.fixedSize = true;
            elements.add(treemap);
        }

        int nx = c.notifierX;
        if (nx == -1) nx = 200;
        int ny = c.notifierY;
        if (ny == -1) ny = 100;
        elements.add(new HudElement("notifier", "NOTIFICATION",
                nx, ny, c.notifierScale, c.notifierAlignment));

        if (c.weeklyWarCountEnabled) {
            HudElement weeklyWarsEl = new HudElement("weeklyWars", "5 wars",
                    c.weeklyWarCountX, c.weeklyWarCountY, 1.5f, WynnExtrasConfig.Align.LEFT, 0xFFFF55FF);
            weeklyWarsEl.topLeft = true;
            elements.add(weeklyWarsEl);
        }
        if (c.warDpsEnabled) {
            HudElement warDpsEl = new HudElement("warDps", "War Info: Tower EHP 234K",
                    c.warDpsX, c.warDpsY, 1.0f, WynnExtrasConfig.Align.LEFT);
            warDpsEl.topLeft = true;
            elements.add(warDpsEl);
        }
        if (c.attackTimerMenuEnabled) {
            String attackText = "13:47 Otherworldly Monolith";
            List<String> attacks = AttackTimerMenu.getUpcomingAttacks();
            if (!attacks.isEmpty()) attackText = attacks.get(0);
            HudElement attackTimerEl = new HudElement("attackTimer", attackText,
                    c.attackTimerX, c.attackTimerY, 1.0f, WynnExtrasConfig.Align.LEFT, 0xFFFFAA00);
            attackTimerEl.topLeft = true;
            elements.add(attackTimerEl);
        }
        if (c.raidSessionEnabled) {
            String raidText = "Raids: 42 | 8.5/hr | Completed: 40 | Failed: 2";
            String live = RaidSessionTracker.getStatsString();
            if (live != null) raidText = live;
            HudElement raidSessionEl = new HudElement("raidSession", raidText,
                    c.raidSessionHudX, c.raidSessionHudY, c.raidSessionHudScale, WynnExtrasConfig.Align.LEFT, 0xFFAAFFAA);
            raidSessionEl.topLeft = true;
            elements.add(raidSessionEl);
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void init() {
        super.init();
        for (HudElement e : elements) {
            if (!e.fixedSize) {
                e.w = textRenderer.getWidth(e.preview) + 6;
                if (!e.topLeft) {
                    e.x = e.x - e.sw() / 2;
                    e.y = e.y - e.sh() / 2;
                }
            }

            if (e.id.equals("totem") && WynnExtrasConfig.INSTANCE.totemTimerX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("provoke") && WynnExtrasConfig.INSTANCE.provokeTimerX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("blood") && WynnExtrasConfig.INSTANCE.bloodSorrowTimerX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("warning") && WynnExtrasConfig.INSTANCE.totemWarningX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("notifier") && WynnExtrasConfig.INSTANCE.notifierX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("notifier") && WynnExtrasConfig.INSTANCE.notifierY == -1) {
                e.y = (int) (height * 0.3f);
            }

            // Clamp on-screen so elements placed at small coords (e.g. x=5) aren't shoved
            // off the top-left edge by the center-shift above.
            e.x = Math.max(4, Math.min(width - e.sw() - 4, e.x));
            e.y = Math.max(4, Math.min(height - e.sh() - 4, e.y));
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        focusedMouseX = mouseX;
        focusedMouseY = mouseY;
        ctx.fill(0, 0, width, height, 0x55000000);

        int centerX = width / 2;
        int centerY = height / 2;
        boolean anySnappedX = false, anySnappedY = false;
        for (HudElement e : elements) {
            if (e.snappedX) anySnappedX = true;
            if (e.snappedY) anySnappedY = true;
        }
        if (anySnappedX) {
            ctx.fill(centerX, 0, centerX + 1, height, 0x4400FF00);
        }
        if (anySnappedY) {
            ctx.fill(0, centerY, width, centerY + 1, 0x4400FF00);
        }

        String hint = elements.isEmpty()
                ? "No HUD elements enabled  |  Esc to close"
                : "Drag to move  |  Scroll to resize  |  Right-click: color  |  Esc to save";
        ctx.drawText(textRenderer, hint, 4, height - 12, 0xFFaaaaaa, true);

        // Draw unfocused elements first.
        for (HudElement e : elements) {
            if (e != colorEditTarget) drawElement(ctx, e, mouseX, mouseY);
        }
        // Dim happens inside renderColorPicker. Focused element is drawn AFTER the dim there.

        // Color picker popup
        if (colorEditTarget != null) {
            renderColorPicker(ctx);
        }
    }

    private void drawElement(DrawContext ctx, HudElement e, int mouseX, int mouseY) {
        boolean hovered = e.hovered(mouseX, mouseY);
        boolean focused = (e == colorEditTarget);
        int border = focused ? 0xFFFFAA00 : (hovered || e.dragging) ? 0xFFFFFFFF : 0xFF888888;
        int sw = e.sw(), sh = e.sh();

        ctx.fill(e.x - 2, e.y - 2, e.x + sw + 2, e.y + sh + 2, 0xCC000000);
        ctx.fill(e.x - 2, e.y - 2, e.x + sw + 2, e.y - 1, border);
        ctx.fill(e.x - 2, e.y + sh + 1, e.x + sw + 2, e.y + sh + 2, border);
        ctx.fill(e.x - 2, e.y - 2, e.x - 1, e.y + sh + 2, border);
        ctx.fill(e.x + sw + 1, e.y - 2, e.x + sw + 2, e.y + sh + 2, border);

        Integer override = WynnExtrasConfig.INSTANCE.hudColorOverrides.get(e.id);
        int textColor;
        if (focused) {
            textColor = 0xFF000000 | hsvToRgb(colorH, colorS, colorV);
        } else if (override != null) {
            textColor = override | 0xFF000000;
        } else if (e.id.equals("notifier")) {
            textColor = WynnExtrasConfig.INSTANCE.textColor.getRGB() | 0xFF000000;
        } else {
            textColor = e.defaultColor;
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(e.x + e.sw() / 2f, e.y + e.sh() / 2f);
        ctx.getMatrices().scale(e.scale, e.scale);
        int tw = textRenderer.getWidth(e.preview);
        int th = textRenderer.fontHeight;
        ctx.drawText(textRenderer, e.preview, -tw / 2, -th / 2, textColor, true);
        ctx.getMatrices().popMatrix();
    }

    private int[] colorPickerBounds() {
        int popupW = 240, popupH = 220;
        int x, y;
        if (colorEditTarget != null) {
            // Try to open the picker beside the element so the element stays visible.
            int ex = colorEditTarget.x, ey = colorEditTarget.y, esw = colorEditTarget.sw(), esh = colorEditTarget.sh();
            int rightSpace = width - (ex + esw);
            if (rightSpace >= popupW + 16) {
                x = ex + esw + 8;
            } else if (ex >= popupW + 16) {
                x = ex - popupW - 8;
            } else {
                x = width / 2 - popupW / 2;
            }
            y = ey + esh / 2 - popupH / 2;
            x = Math.max(4, Math.min(width - popupW - 4, x));
            y = Math.max(4, Math.min(height - popupH - 4, y));
        } else {
            x = width / 2 - popupW / 2;
            y = height / 2 - popupH / 2;
        }
        return new int[]{x, y, popupW, popupH};
    }

    private int[] closeButtonBounds() {
        int[] b = colorPickerBounds();
        int size = 12;
        return new int[]{b[0] + b[2] - size - 4, b[1] + 4, size, size};
    }

    private int[] svBoxBounds() {
        int[] b = colorPickerBounds();
        int size = 120;
        return new int[]{b[0] + 12, b[1] + 22, size, size};
    }

    private int[] hueBarBounds() {
        int[] b = colorPickerBounds();
        int[] sv = svBoxBounds();
        return new int[]{sv[0] + sv[2] + 10, sv[1], 14, sv[3]};
    }

    private int[] presetBounds(int presetIdx) {
        int[] b = colorPickerBounds();
        int cols = PRESET_COLORS.length;
        int cellW = (b[2] - 20) / cols;
        int px = b[0] + 10 + presetIdx * cellW;
        int py = b[1] + 155;
        return new int[]{px, py, cellW - 2, 12};
    }

    private int[] buttonBounds(int btnIdx) {
        int[] b = colorPickerBounds();
        int gap = 6;
        int available = b[2] - 16; // inner padding
        int btnW = (available - gap * 2) / 3;
        int btnH = 18;
        int y = b[1] + b[3] - btnH - 8;
        int startX = b[0] + 8;
        return new int[]{startX + btnIdx * (btnW + gap), y, btnW, btnH};
    }

    private void renderColorPicker(DrawContext ctx) {
        int[] b = colorPickerBounds();
        int px = b[0], py = b[1], popupW = b[2], popupH = b[3];

        ctx.fill(0, 0, width, height, 0x88000000);
        // Redraw the focused element above the dim so it stays bright.
        if (colorEditTarget != null) drawElement(ctx, colorEditTarget, -1, -1);
        ctx.fill(px, py, px + popupW, py + popupH, 0xFF222222);
        ctx.fill(px, py, px + popupW, py + 1, 0xFFFFAA00);
        ctx.fill(px, py + popupH - 1, px + popupW, py + popupH, 0xFFFFAA00);
        ctx.fill(px, py, px + 1, py + popupH, 0xFFFFAA00);
        ctx.fill(px + popupW - 1, py, px + popupW, py + popupH, 0xFFFFAA00);

        ctx.drawCenteredTextWithShadow(textRenderer, "Color: " + colorEditTarget.id, px + popupW / 2, py + 6, 0xFFFFAA00);

        // Saturation/Value square — draw from a cached grid of block colors.
        int[] sv = svBoxBounds();
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
        // SV cursor: circular-ish ring (8x8 outline) around the selected point.
        int svCx = sv[0] + (int)(colorS * (sv[2] - 1));
        int svCy = sv[1] + (int)((1f - colorV) * (sv[3] - 1));
        drawColorCursor(ctx, svCx, svCy);

        // Hue bar (vertical rainbow) — cached.
        int[] hb = hueBarBounds();
        ensureHueCache(hb[3]);
        for (int row = 0; row < hueCacheRows; row++) {
            ctx.fill(hb[0], hb[1] + row, hb[0] + hb[2], hb[1] + row + 1, hueCacheColors[row]);
        }
        // Hue cursor: simple horizontal ring.
        int hueCy = hb[1] + (int)((colorH / 360f) * (hb[3] - 1));
        ctx.fill(hb[0] - 2, hueCy - 2, hb[0] + hb[2] + 2, hueCy - 1, 0xFFFFFFFF);
        ctx.fill(hb[0] - 2, hueCy + 1, hb[0] + hb[2] + 2, hueCy + 2, 0xFFFFFFFF);
        ctx.fill(hb[0] - 2, hueCy - 1, hb[0] - 1, hueCy + 1, 0xFFFFFFFF);
        ctx.fill(hb[0] + hb[2] + 1, hueCy - 1, hb[0] + hb[2] + 2, hueCy + 1, 0xFFFFFFFF);

        // Preview swatch + hex (positioned to the right of hue bar, within popup)
        int currentRgb = hsvToRgb(colorH, colorS, colorV);
        int previewColor = 0xFF000000 | currentRgb;
        int previewX = hb[0] + hb[2] + 10;
        int previewW = Math.min(40, px + popupW - previewX - 8);
        ctx.fill(previewX, sv[1], previewX + previewW, sv[1] + 30, previewColor);
        ctx.fill(previewX - 1, sv[1] - 1, previewX + previewW + 1, sv[1], 0xFFAAAAAA);
        ctx.fill(previewX - 1, sv[1] + 30, previewX + previewW + 1, sv[1] + 31, 0xFFAAAAAA);
        ctx.fill(previewX - 1, sv[1], previewX, sv[1] + 30, 0xFFAAAAAA);
        ctx.fill(previewX + previewW, sv[1], previewX + previewW + 1, sv[1] + 30, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, String.format("#%06X", currentRgb & 0xFFFFFF), previewX, sv[1] + 36, 0xFFFFFFFF);

        // Presets
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int[] pb = presetBounds(i);
            int c = 0xFF000000 | PRESET_COLORS[i];
            ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + pb[3], c);
            ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + 1, 0xFF555555);
            ctx.fill(pb[0], pb[1] + pb[3] - 1, pb[0] + pb[2], pb[1] + pb[3], 0xFF555555);
        }

        String[] labels = {"Reset", "Apply", "Apply to all"};
        int[] btnColors = {0xFFAA3333, 0xFF338833, 0xFF3388AA};
        for (int i = 0; i < 3; i++) {
            int[] bb = buttonBounds(i);
            ctx.fill(bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3], btnColors[i]);
            ctx.drawCenteredTextWithShadow(textRenderer, labels[i], bb[0] + bb[2] / 2, bb[1] + 5, 0xFFFFFFFF);
        }

        // Close X in top-right corner of popup
        int[] cb = closeButtonBounds();
        ctx.fill(cb[0], cb[1], cb[0] + cb[2], cb[1] + cb[3], 0xFF552222);
        ctx.drawCenteredTextWithShadow(textRenderer, "X", cb[0] + cb[2] / 2, cb[1] + 2, 0xFFFFFFFF);
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
            float v = 1f - (row * SV_CELL) / (float)(height - 1);
            if (v < 0) v = 0;
            for (int col = 0; col < cols; col++) {
                float s = (col * SV_CELL) / (float)(width - 1);
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
            float h = (row / (float)(height - 1)) * 360f;
            hueCacheColors[row] = 0xFF000000 | hsvToRgb(h, 1f, 1f);
        }
    }

    private static void drawColorCursor(DrawContext ctx, int cx, int cy) {
        // Double-ring cursor: black outer border, white inner ring for contrast on any background.
        // 9x9 outer square (black), 7x7 middle ring (white), 5x5 inner cutout.
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
        h = ((h % 360f) + 360f) % 360f;
        float c = v * s;
        float x = c * (1f - Math.abs(((h / 60f) % 2f) - 1f));
        float m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return (ri << 16) | (gi << 8) | bi;
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

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x(), my = click.y();

        // Color picker handling
        if (colorEditTarget != null) {
            if (click.button() != 0) return true;

            // Close X
            int[] cb = closeButtonBounds();
            if (mx >= cb[0] && mx < cb[0] + cb[2] && my >= cb[1] && my < cb[1] + cb[3]) {
                colorEditTarget = null;
                return true;
            }

            // SV box
            int[] sv = svBoxBounds();
            if (mx >= sv[0] && mx < sv[0] + sv[2] && my >= sv[1] && my < sv[1] + sv[3]) {
                colorDragMode = 2;
                colorS = Math.max(0f, Math.min(1f, (float)(mx - sv[0]) / (sv[2] - 1)));
                colorV = Math.max(0f, Math.min(1f, 1f - (float)(my - sv[1]) / (sv[3] - 1)));
                return true;
            }

            // Hue bar
            int[] hb = hueBarBounds();
            if (mx >= hb[0] && mx < hb[0] + hb[2] && my >= hb[1] && my < hb[1] + hb[3]) {
                colorDragMode = 1;
                colorH = Math.max(0f, Math.min(360f, (float)(my - hb[1]) / (hb[3] - 1) * 360f));
                return true;
            }

            // Preset colors
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int[] pb = presetBounds(i);
                if (mx >= pb[0] && mx < pb[0] + pb[2] && my >= pb[1] && my < pb[1] + pb[3]) {
                    int c = PRESET_COLORS[i];
                    float[] hsv = rgbToHsv((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
                    colorH = hsv[0]; colorS = hsv[1]; colorV = hsv[2];
                    return true;
                }
            }

            // Buttons: 0=Reset, 1=Only (this), 2=All
            for (int i = 0; i < 3; i++) {
                int[] bb = buttonBounds(i);
                if (mx >= bb[0] && mx < bb[0] + bb[2] && my >= bb[1] && my < bb[1] + bb[3]) {
                    int c = hsvToRgb(colorH, colorS, colorV);
                    if (i == 0) {
                        WynnExtrasConfig.INSTANCE.hudColorOverrides.remove(colorEditTarget.id);
                        WynnExtrasConfig.save();
                    } else if (i == 1) {
                        WynnExtrasConfig.INSTANCE.hudColorOverrides.put(colorEditTarget.id, c);
                        WynnExtrasConfig.save();
                    } else if (i == 2) {
                        for (HudElement el : elements) {
                            WynnExtrasConfig.INSTANCE.hudColorOverrides.put(el.id, c);
                        }
                        WynnExtrasConfig.save();
                    }
                    colorEditTarget = null;
                    return true;
                }
            }
            return true; // block other clicks while popup is open
        }

        if (click.button() == 0) {
            for (HudElement e : elements) {
                if (e.hovered(mx, my)) {
                    e.dragging = true;
                    e.dragOffX = (int) mx - e.x;
                    e.dragOffY = (int) my - e.y;
                    return true;
                }
            }
        } else if (click.button() == 1) {
            // Right-click: open color picker for hovered element
            for (HudElement e : elements) {
                if (e.hovered(mx, my)) {
                    colorEditTarget = e;
                    Integer existing = WynnExtrasConfig.INSTANCE.hudColorOverrides.get(e.id);
                    int c = existing != null ? existing : 0xFFFFFF;
                    float[] hsv = rgbToHsv((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
                    colorH = hsv[0]; colorS = hsv[1]; colorV = hsv[2];
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubleClick);
    }


    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        if (colorEditTarget != null && colorDragMode > 0) {
            if (colorDragMode == 1) {
                int[] hb = hueBarBounds();
                colorH = Math.max(0f, Math.min(360f, (float)(my - hb[1]) / (hb[3] - 1) * 360f));
            } else if (colorDragMode == 2) {
                int[] sv = svBoxBounds();
                colorS = Math.max(0f, Math.min(1f, (float)(mx - sv[0]) / (sv[2] - 1)));
                colorV = Math.max(0f, Math.min(1f, 1f - (float)(my - sv[1]) / (sv[3] - 1)));
            }
            return true;
        }
        for (HudElement e : elements) {
            if (e.dragging) {
                int newX = Math.max(0, Math.min(width - e.sw(), (int) mx - e.dragOffX));
                int newY = Math.max(0, Math.min(height - e.sh(), (int) my - e.dragOffY));

                // Snap to horizontal center
                int elemCenterX = newX + e.sw() / 2;
                int screenCenterX = width / 2;
                if (Math.abs(elemCenterX - screenCenterX) < SNAP_DIST) {
                    newX = screenCenterX - e.sw() / 2;
                    e.snappedX = true;
                } else {
                    e.snappedX = false;
                }

                // Snap to vertical center
                int elemCenterY = newY + e.sh() / 2;
                int screenCenterY = height / 2;
                if (Math.abs(elemCenterY - screenCenterY) < SNAP_DIST) {
                    newY = screenCenterY - e.sh() / 2;
                    e.snappedY = true;
                } else {
                    e.snappedY = false;
                }

                e.x = newX;
                e.y = newY;
                return true;
            }
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (HudElement e : elements) {
            if (e.hovered(mouseX, mouseY)) {
                e.scale = Math.max(0.5f, Math.min(4.0f, e.scale + (float) verticalAmount * 0.1f));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(Click click) {
        colorDragMode = 0;
        for (HudElement e : elements) {
            e.dragging = false;
            e.snappedX = false;
            e.snappedY = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int modifiers = input.modifiers();

        // Color picker: Esc cancels
        if (colorEditTarget != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                colorEditTarget = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                int c = hsvToRgb(colorH, colorS, colorV);
                WynnExtrasConfig.INSTANCE.hudColorOverrides.put(colorEditTarget.id, c);
                WynnExtrasConfig.save();
                colorEditTarget = null;
                return true;
            }
            return true;
        }

        boolean shift = (modifiers & 1) != 0;
        if (shift && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_D)) { // Left arrow = 263, Right arrow = 262
            for (HudElement e : elements) {
                if (e.hovered(focusedMouseX, focusedMouseY)) {
                    if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_A) { // left
                        e.alignment = e.alignment == WynnExtrasConfig.Align.CENTER ? WynnExtrasConfig.Align.LEFT : e.alignment == WynnExtrasConfig.Align.RIGHT ? WynnExtrasConfig.Align.CENTER : WynnExtrasConfig.Align.LEFT;
                    } else { // right
                        e.alignment = e.alignment == WynnExtrasConfig.Align.CENTER ? WynnExtrasConfig.Align.RIGHT : e.alignment == WynnExtrasConfig.Align.LEFT ? WynnExtrasConfig.Align.CENTER : WynnExtrasConfig.Align.RIGHT;
                    }
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        for (HudElement e : elements) {
            switch (e.id) {
                case "provoke" -> {
                    c.provokeTimerX = e.x + e.sw() / 2; c.provokeTimerY = e.y + e.sh() / 2; c.provokeTimerScale = e.scale; c.provokeTimerAlignment = e.alignment;
                }
                case "totem" -> {
                    c.totemTimerX = e.x + e.sw() / 2; c.totemTimerY = e.y + e.sh() / 2; c.totemTimerScale = e.scale; c.totemTimerAlignment = e.alignment;
                }
                case "blood" -> {
                    c.bloodSorrowTimerX = e.x + e.sw() / 2; c.bloodSorrowTimerY = e.y + e.sh() / 2; c.bloodSorrowTimerScale = e.scale; c.bloodSorrowAlignment = e.alignment;
                }
                case "warning" -> {
                    c.totemWarningX = e.x + e.sw() / 2; c.totemWarningY = e.y + e.sh() / 2; c.totemWarningScale = e.scale; c.totemWarningAlignment = e.alignment;
                }
                case "radiant" -> {
                    c.radiantHudX = e.x; c.radiantHudY = e.y; c.radiantHudScale = e.scale;
                }
                case "profession" -> {
                    c.professionOverlayX = e.x; c.professionOverlayY = e.y; c.professionOverlayScale = e.scale;
                }
                case "notifier" -> {
                    c.notifierX = e.x + e.sw() / 2; c.notifierY = e.y + e.sh() / 2; c.notifierScale = e.scale; c.notifierAlignment = e.alignment;
                }
                case "treemap" -> {
                    c.treeMapX = e.x; c.treeMapY = e.y; c.tnaTreeMapScale = e.scale;
                }
                case "weeklyWars" -> {
                    c.weeklyWarCountX = e.x; c.weeklyWarCountY = e.y;
                }
                case "warDps" -> {
                    c.warDpsX = e.x; c.warDpsY = e.y;
                }
                case "attackTimer" -> {
                    c.attackTimerX = e.x; c.attackTimerY = e.y;
                }
                case "raidSession" -> {
                    c.raidSessionHudX = e.x; c.raidSessionHudY = e.y; c.raidSessionHudScale = e.scale;
                }
            }
        }
        WynnExtrasConfig.save();
        TreeRoomMinimap.syncFromConfig();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
