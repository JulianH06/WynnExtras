package julianh06.wynnextras.utils.UI;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

public abstract class WEHandledScreen {
    protected DrawContext drawContext;
    protected double scaleFactor;
    protected double matrixScale = 1.0;
    protected int xStart;
    protected int yStart;
    protected int screenWidth;
    protected int screenHeight;
    protected UIUtils ui;

    protected double getTargetScaleFactor() { return -1; }
    protected int getMinScreenWidth()  { return 0; }
    protected int getMinScreenHeight() { return 0; }

    protected final List<Widget> rootWidgets = new ArrayList<>();
    protected Widget focusedWidget = null;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.drawContext = ctx;
        computeScale();

        if (ui == null)
            ui = new UIUtils(ctx, scaleFactor, xStart, yStart);
        else
            ui.updateContext(ctx, scaleFactor, xStart, yStart);

        int mx = (int)(mouseX / matrixScale);
        int my = (int)(mouseY / matrixScale);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale((float) matrixScale, (float) matrixScale);

        drawBackground(ctx, mx, my, delta);
        drawContent(ctx, mx, my, delta);

        for (Widget w : rootWidgets) {
            w.draw(ctx, mx, my, delta, ui);
        }

        drawForeground(ctx, mx, my, delta);

        ctx.getMatrices().popMatrix();
    }







    public boolean mouseClicked(double x, double y, int button) {
        x /= matrixScale;
        y /= matrixScale;
        clearUiFocus();
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            if (rootWidgets.get(i).mouseClicked(x, y, button)) {
                setFocusedWidget(rootWidgets.get(i));
                return true;
            }
        }

        setFocusedWidget(null);
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        x /= matrixScale;
        y /= matrixScale;
        for (Widget w : rootWidgets)
            if (w.mouseReleased(x, y, button))
                return true;
        return false;
    }

    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        x /= matrixScale;
        y /= matrixScale;
        if (focusedWidget != null && focusedWidget.mouseDragged(x, y, button, dx, dy))
            return true;
        for (Widget w : rootWidgets)
            if (w.mouseDragged(x, y, button, dx, dy))
                return true;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers))
            return true;

        for (Widget w : rootWidgets)
            if (w.keyPressed(keyCode, scanCode, modifiers))
                return true;

        return false;
    }

    public boolean charTyped(char chr, int mods) {
        if (focusedWidget != null && focusedWidget.charTyped(chr, mods))
            return true;

        for (Widget w : rootWidgets)
            if (w.charTyped(chr, mods))
                return true;

        return false;
    }







    protected void setFocusedWidget(Widget w) {
        if (focusedWidget != null) focusedWidget.setFocused(false);
        focusedWidget = w;
        if (w != null) w.setFocused(true);
    }

    protected void clearUiFocus() {
        for (Widget w : rootWidgets) {
            w.clearFocusTree();
        }
        focusedWidget = null;
    }

    public void computeScale() {
        Window w = MinecraftClient.getInstance().getWindow();
        double actualScale = Math.max(1.0, w.getScaleFactor());
        double target = getTargetScaleFactor();
        if (target > 1.0 && actualScale > target) {
            this.matrixScale = target / actualScale;
            this.scaleFactor = target;
        } else {
            this.matrixScale = 1.0;
            this.scaleFactor = actualScale;
        }
        this.screenWidth = (int) Math.round(w.getScaledWidth() / matrixScale);
        this.screenHeight = (int) Math.round(w.getScaledHeight() / matrixScale);

        int minSW = getMinScreenWidth();
        int minSH = getMinScreenHeight();
        double extraScale = 1.0;
        if (minSW > 0 && screenWidth < minSW) extraScale = Math.min(extraScale, (double) screenWidth / minSW);
        if (minSH > 0 && screenHeight < minSH) extraScale = Math.min(extraScale, (double) screenHeight / minSH);
        if (extraScale < 1.0) {
            matrixScale *= extraScale;
            scaleFactor *= extraScale;
            screenWidth = (int) Math.round(w.getScaledWidth() / matrixScale);
            screenHeight = (int) Math.round(w.getScaledHeight() / matrixScale);
        }
    }







    protected abstract void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta);
    protected abstract void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta);
    protected abstract void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta);
}
