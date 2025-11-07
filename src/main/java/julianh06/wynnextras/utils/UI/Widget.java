package julianh06.wynnextras.utils.UI;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Widget {
    protected int x, y, width, height;

    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean hovered = false;
    protected boolean focused = false;

    protected Widget parent = null;
    protected final List<Widget> children = new ArrayList<>();

    protected Consumer<Widget> onClickCallback = null;
    protected Consumer<Widget> onFocusCallback = null;
    protected Consumer<Widget> onBlurCallback = null;

    protected UIUtils ui;

    public Widget(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    // ---- Bounds / Layout ----
    public final void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        onResize(width, height);
    }

    protected void onResize(int newWidth, int newHeight) { /* override in subclasses if needed */ }

    public void setPosition(int x, int y) { setBounds(x, y, this.width, this.height); }
    public void setSize(int width, int height) { setBounds(this.x, this.y, width, height); }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // ---- Visibility / State ----
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isHovered() { return hovered; }
    public boolean isFocused() { return focused; }

    // ---- Children ----
    public void addChild(Widget child) {
        if (child == null) return;
        child.parent = this;
        children.add(child);
    }

    public void addAllChildren(List<Widget> children) {
        if(children == null || children.isEmpty()) return;
        for(Widget child : children) {
            child.parent = this;
            this.children.add(child);
        }
    }

    public void removeChild(Widget child) {
        if (child == null) return;
        children.remove(child);
        child.parent = null;
    }

    public void clearChildren() {
        for (Widget c : children) c.parent = null;
        children.clear();
    }

    // ---- Drawing Lifecycle ----
    public final void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
        this.ui = ui;
        if(!visible || this.ui == null) return;
        // update hover state for this widget
        hovered = contains(mouseX, mouseY);
        updateValues();
        drawBackground(ctx, mouseX, mouseY, tickDelta);
        drawContent(ctx, mouseX, mouseY, tickDelta);
        // draw children in insertion order (lower z first)
        for (Widget child : children) {
            child.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }
        drawForeground(ctx, mouseX, mouseY, tickDelta);
    }

    protected void updateValues() {}

    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) { /* override */ }
    protected abstract void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta);
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) { /* override */ }

    // ---- Input / Event propagation ----
    /**
     * Mouse click event propagation.
     * Returns true if the event was consumed.
     */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible || !enabled) return false;
        // propagate to children in reverse order (topmost first)
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mx, my, button)) return true;
        }
        // if this widget contains the click, handle it
        if (contains((int) mx, (int) my)) {
            // manage focus
            setFocused(true);
            if (onClickCallback != null) onClickCallback.accept(this);
            return onClick(button);
        } else {
            // clicking outside removes focus
            if (focused) setFocused(false);
        }
        return false;
    }

    protected boolean onClick(int button) { return false; }

    public boolean mouseReleased(double mx, double my, int button) {
        if (!visible) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseReleased(mx, my, button)) return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!visible) return false;
        // children first
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).mouseScrolled(mx, my, delta)) return true;
        }
        return onScroll(delta);
    }

    protected boolean onScroll(double delta) { return false; }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !enabled) return false;
        // dispatch to focused child if present
        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return onKeyPressed(keyCode, scanCode, modifiers);
    }

    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) { return false; }

    public boolean charTyped(char chr, int modifiers) {
        if (!visible || !enabled) return false;
        for (Widget child : children) {
            if (child.charTyped(chr, modifiers)) return true;
        }
        return onCharTyped(chr, modifiers);
    }

    protected boolean onCharTyped(char chr, int modifiers) { return false; }

    // ---- Focus ----
    public void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        if (focused) {
            if (onFocusCallback != null) onFocusCallback.accept(this);
            // blur siblings / other focus handling should be done by container (e.g., WEScreen)
        } else {
            if (onBlurCallback != null) onBlurCallback.accept(this);
        }
    }

    // ---- Callbacks ----
    public void setOnClick(Consumer<Widget> callback) { this.onClickCallback = callback; }
    public void setOnFocus(Consumer<Widget> callback) { this.onFocusCallback = callback; }
    public void setOnBlur(Consumer<Widget> callback) { this.onBlurCallback = callback; }

    // ---- Utilities ----
    protected boolean contains(int mx, int my) {
        if(ui == null) return false;
        return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
    }

    public void translate(int dx, int dy) {
        this.x += dx; this.y += dy;
        for (Widget c : children) c.translate(dx, dy);
    }

    public void tick() {
        // per-frame update hook; propagate to children
        for (Widget c : children) c.tick();
    }

    public void setUi(UIUtils ui) {
        this.ui = ui;
    }
}
