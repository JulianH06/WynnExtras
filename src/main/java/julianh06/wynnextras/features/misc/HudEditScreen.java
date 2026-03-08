package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class HudEditScreen extends Screen {
    private double focusedMouseX, focusedMouseY;

    private static class HudElement {
        final String id;
        final String preview;
        int x, y, w;
        float scale;
        boolean dragging;
        int dragOffX, dragOffY;
        boolean snappedX, snappedY;
        WynnExtrasConfig.Align alignment;

        HudElement(String id, String preview, int x, int y, float scale, WynnExtrasConfig.Align alignment) {
            this.id = id;
            this.preview = preview;
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alignment = alignment;
        }

        int sw() { return (int) (w * scale); }
        int sh() { return (int) (H * scale); }
        boolean hovered(double mx, double my) {
            return mx >= x - 2 && mx <= x + sw() + 2 && my >= y - 2 && my <= y + sh() + 2;
        }
    }

    private static final int H = 14;
    private static final int SNAP_DIST = 8;
    private final List<HudElement> elements = new ArrayList<>();

    private final Screen parent;

    public HudEditScreen() { this(null); }

    public HudEditScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

        this.parent = parent;

        // Only show elements whose features are enabled
        if (c.provokeTimerToggle) {
            elements.add(new HudElement("provoke", "Provoke: 7s",
                    c.provokeTimerX, c.provokeTimerY, c.provokeTimerScale, c.provokeTimerAlignment));
        }
        if (c.totemTimerEnabled) {
            elements.add(new HudElement("totem", "PlayerName's Totem: 38s",
                    c.totemTimerX, c.totemTimerY, c.totemTimerScale, c.totemTimerAlignment));
        }
        if (c.bloodSorrowTimerEnabled) {
            elements.add(new HudElement("blood", "Blood Sorrow: 1.7s",
                    c.bloodSorrowTimerX, c.bloodSorrowTimerY, c.bloodSorrowTimerScale, c.bloodSorrowAlignment));
        }
        if (c.totemTimerEnabled && c.totemTimerWarningText) {
            int wx = c.totemWarningX;
            if (wx == -1) wx = 200;
            elements.add(new HudElement("warning", "RECAST TOTEM!",
                    wx, c.totemWarningY, c.totemWarningScale, c.totemWarningAlignment));
        }
        // Notification always available
        {
            int nx = c.notifierX;
            if (nx == -1) nx = 200;
            int ny = c.notifierY;
            if (ny == -1) ny = 100;
            elements.add(new HudElement("notifier", "NOTIFICATION",
                    nx, ny, c.notifierScale, c.notifierAlignment));
        }
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void init() {
        super.init();
        for (HudElement e : elements) {
            e.w = textRenderer.getWidth(e.preview) + 6;
            e.x = e.x - e.sw() / 2;
            e.y = e.y - e.sh() / 2;

            if (e.id.equals("warning") && WynnExtrasConfig.INSTANCE.totemWarningX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("notifier") && WynnExtrasConfig.INSTANCE.notifierX == -1) {
                e.x = (width - e.sw()) / 2;
            }
            if (e.id.equals("notifier") && WynnExtrasConfig.INSTANCE.notifierY == -1) {
                e.y = (int) (height * 0.3f);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        focusedMouseX = mouseX;
        focusedMouseY = mouseY;
        ctx.fill(0, 0, width, height, 0x55000000);

        // Draw center crosshair guides
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
                : "Drag to move  |  Scroll to resize  |  Shift + arrow keys (or A/D) to change alignment  |  Esc to save & close ";
        ctx.drawText(textRenderer, hint, 4, height - 12, 0xFFaaaaaa, true);

        for (HudElement e : elements) {
            boolean hovered = e.hovered(mouseX, mouseY);
            int border = (hovered || e.dragging) ? 0xFFFFFFFF : 0xFF888888;
            int sw = e.sw(), sh = e.sh();

            ctx.fill(e.x - 2, e.y - 2, e.x + sw + 2, e.y + sh + 2, 0xCC000000);
            ctx.fill(e.x - 2, e.y - 2, e.x + sw + 2, e.y - 1, border);
            ctx.fill(e.x - 2, e.y + sh + 1, e.x + sw + 2, e.y + sh + 2, border);
            ctx.fill(e.x - 2, e.y - 2, e.x - 1, e.y + sh + 2, border);
            ctx.fill(e.x + sw + 1, e.y - 2, e.x + sw + 2, e.y + sh + 2, border);

            int textColor = e.id.equals("warning") ? 0xFFFF4444
                    : e.id.equals("notifier") ? (WynnExtrasConfig.INSTANCE.textColor.getRGB() | 0xFF000000)
                    : 0xFFFFFFFF;

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(e.x + e.sw() / 2f, e.y + e.sh() / 2f);  // Mittelpunkt des Kästchens
            ctx.getMatrices().scale(e.scale, e.scale);
            int tw = textRenderer.getWidth(e.preview);
            int th = textRenderer.fontHeight;
            ctx.drawText(textRenderer, e.preview, -tw / 2, -th / 2, textColor, true);
            ctx.getMatrices().popMatrix();

            String alignLabel = e.alignment == WynnExtrasConfig.Align.LEFT ? "◀" : e.alignment == WynnExtrasConfig.Align.RIGHT ? "▶" : "◆";
            ctx.drawText(textRenderer, alignLabel, e.x + e.sw() + 4, e.y + 3, 0xFF888888, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() == 0) {
            double mx = click.x(), my = click.y();
            for (HudElement e : elements) {
                if (e.hovered(mx, my)) {
                    e.dragging = true;
                    e.dragOffX = (int) mx - e.x;
                    e.dragOffY = (int) my - e.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
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

        boolean shift = (modifiers & 1) != 0; // GLFW_MOD_SHIFT
        if (shift && (keyCode == 263 || keyCode == 262)) { // Links = 263, Rechts = 262
            for (HudElement e : elements) {
                if (e.hovered(focusedMouseX, focusedMouseY)) { // s.u.
                    if (keyCode == 263) { // links
                        e.alignment = e.alignment == WynnExtrasConfig.Align.CENTER ? WynnExtrasConfig.Align.LEFT : e.alignment == WynnExtrasConfig.Align.RIGHT ? WynnExtrasConfig.Align.CENTER : WynnExtrasConfig.Align.LEFT;
                    } else { // rechts
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
                case "notifier" -> {
                    c.notifierX = e.x + e.sw() / 2; c.notifierY = e.y + e.sh() / 2; c.notifierScale = e.scale; c.notifierAlignment = e.alignment;
                }
            }
        }
        WynnExtrasConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
