package julianh06.wynnextras.utils.UI;

import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for overlays that extend (rather than replace) a vanilla HandledScreen.
 * Renders proportionally alongside the vanilla GUI — scales naturally with GUI scale.
 * Use WEHandledScreen for full overlays that replace the vanilla GUI.
 */
public abstract class WEMenuExtension {
    protected DrawContext drawContext;
    protected double scaleFactor;
    protected int xStart;
    protected int yStart;
    protected int screenWidth;
    protected int screenHeight;
    protected UIUtils ui;

    protected final List<Widget> rootWidgets = new ArrayList<>();
    protected final List<WEElement<?>> listElements = new ArrayList<>();
    protected Widget focusedWidget = null;
    protected WEElement<?> focusedElement = null;
    protected float listX, listY, listWidth, listHeight;
    protected float listItemHeight;
    protected float listSpacing;
    protected float listScrollOffset = 0f;
    protected int firstVisibleIndex = 0;
    protected int lastVisibleIndex = -1;

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.drawContext = ctx;
        computeScale();

        if (ui == null)
            ui = new UIUtils(ctx, scaleFactor, xStart, yStart);
        else
            ui.updateContext(ctx, scaleFactor, xStart, yStart);

        drawBackground(ctx, mouseX, mouseY, delta);
        drawContent(ctx, mouseX, mouseY, delta);

        for (Widget w : rootWidgets) {
            w.draw(ctx, mouseX, mouseY, delta, ui);
        }

        updateVisibleListRange();
        layoutListElements();

        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            listElements.get(i).draw(ctx, mouseX, mouseY, delta, ui);
        }

        drawForeground(ctx, mouseX, mouseY, delta);
    }

    public void computeScale() {
        Window w = MinecraftClient.getInstance().getWindow();
        // scaleFactor = 1.0 so that logical units == GUI units.
        // GUI unit values from the HandledScreen (getX, getBackgroundWidth, etc.) can
        // be used directly in setBounds without conversion, and automatically scale
        // proportionally with the physical pixel size as GUI scale changes.
        this.scaleFactor  = 1.0;
        this.screenWidth  = w.getScaledWidth();
        this.screenHeight = w.getScaledHeight();
    }

    // --- HandledScreen helpers (logical coords = GUI units * scaleFactor) ---

    protected float hsX(HandledScreen<?> screen) {
        return ((HandledScreenAccessor) screen).getX() * (float) scaleFactor;
    }

    protected float hsY(HandledScreen<?> screen) {
        return ((HandledScreenAccessor) screen).getY() * (float) scaleFactor;
    }

    protected float hsWidth(HandledScreen<?> screen) {
        return ((HandledScreenAccessor) screen).getBackgroundWidth() * (float) scaleFactor;
    }

    protected float hsHeight(HandledScreen<?> screen) {
        return ((HandledScreenAccessor) screen).getBackgroundHeight() * (float) scaleFactor;
    }

    // --- List ---

    protected void scrollList(float delta) {
        float contentHeight = listElements.size() * (listItemHeight + listSpacing) - listSpacing;
        listScrollOffset -= delta;
        float maxScroll = Math.max(0, contentHeight - listHeight);
        listScrollOffset = Math.max(0, Math.min(listScrollOffset, maxScroll));
    }

    protected void layoutListElements() {
        float yy = listY - listScrollOffset;
        for (WEElement<?> e : listElements) {
            e.setBounds((int) listX, (int) yy, (int) listWidth, (int) listItemHeight);
            yy += listItemHeight + listSpacing;
        }
    }

    protected void updateVisibleListRange() {
        if (listElements.isEmpty() || listItemHeight <= 0) {
            firstVisibleIndex = 0;
            lastVisibleIndex = -1;
            return;
        }
        float slot = listItemHeight + listSpacing;
        int start = (int) Math.floor(listScrollOffset / slot);
        int visibleCount = (int) Math.ceil(listHeight / slot) + 1;
        firstVisibleIndex = Math.max(0, start);
        lastVisibleIndex = Math.min(listElements.size() - 1, start + visibleCount);
    }

    // --- Input ---

    public boolean mouseClicked(double x, double y, int button) {
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            if (rootWidgets.get(i).mouseClicked(x, y, button)) {
                setFocusedWidget(rootWidgets.get(i));
                setFocusedElement(null);
                return true;
            }
        }
        for (int i = lastVisibleIndex; i >= firstVisibleIndex; i--) {
            WEElement<?> e = listElements.get(i);
            if (e.mouseClicked(x, y, button)) {
                setFocusedElement(e);
                setFocusedWidget(null);
                return true;
            }
        }
        setFocusedElement(null);
        setFocusedWidget(null);
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        for (Widget w : rootWidgets)
            if (w.mouseReleased(x, y, button)) return true;
        return false;
    }

    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        for (Widget w : rootWidgets)
            if (w.mouseDragged(x, y, button, dx, dy)) return true;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (Widget w : rootWidgets)
            if (w.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    public boolean charTyped(char chr, int mods) {
        if (focusedWidget != null && focusedWidget.charTyped(chr, mods)) return true;
        for (Widget w : rootWidgets)
            if (w.charTyped(chr, mods)) return true;
        return false;
    }

    protected void setFocusedElement(WEElement<?> e) {
        if (focusedElement != null) focusedElement.setFocused(false);
        focusedElement = e;
        if (e != null) e.setFocused(true);
    }

    protected void setFocusedWidget(Widget w) {
        if (focusedWidget != null) focusedWidget.setFocused(false);
        focusedWidget = w;
        if (w != null) w.setFocused(true);
    }

    protected abstract void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta);
    protected abstract void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta);
    protected abstract void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta);
}
