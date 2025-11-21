package julianh06.wynnextras.utils.UI;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public abstract class WEElement<T> {
    // Datenreferenz (optional)
    public final T model; // kann null sein, oder konkreter generischer Typ via subclass cast

    // Layout
    protected int x, y, width, height;

    // State
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean focused = false;
    protected boolean hovered = false;
    protected boolean selected = false;

    // Interaktive Kinder zum Delegieren (Buttons, Inputs)
    protected final List<Widget> childWidgets = new ArrayList<>();

    // Textures / styling optional
    protected Identifier backgroundTexture;
    protected Identifier hoverTexture;

    protected UIUtils ui;

    public WEElement(int x, int y, int width, int height, T model) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        this.model = model;
    }

    // Layout / Position aktualisieren (wird vom Container aufgerufen)
    public void setBounds(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
        onResize(width, height);
    }

    protected void onResize(int newWidth, int newHeight) { /* override if needed */ }

    // Lebenszyklus
    public void init() { /* init children / resources */ }
    public void tick() { /* per-frame updates */ }

    // Rendering
    public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
        this.ui = ui;
        if(!visible || this.ui == null) return;
        hovered = isMouseOver(mouseX, mouseY);
//        System.out.println(hovered);
        drawBackground(ctx, mouseX, mouseY, tickDelta);
        drawContent(ctx, mouseX, mouseY, tickDelta);
        drawChildren(ctx, mouseX, mouseY, tickDelta);
        if(focused) drawFocus(ctx);
    }

    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(backgroundTexture != null) {
            // draw texture at x,y,width,height (implement texture draw)
        }
    }

    protected abstract void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta);

    protected void drawChildren(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        for(Widget w : childWidgets) {
            if(w.visible) w.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }
    }

    protected void drawFocus(DrawContext ctx) { /* draw focus outline */ }

    // Input
    public boolean mouseClicked(double mx, double my, int button) {
        if(!visible || !enabled || !isMouseOver(mx, my)) return false;
        // delegate to children first (reverse order for z)
        for(int i = childWidgets.size()-1; i >= 0; i--) {
            Widget w = childWidgets.get(i);
            if(w.mouseClicked(mx, my, button)) return true;
        }
        return onClick((int)mx, (int)my, button);
    }

    protected boolean onClick(int mouseX, int mouseY, int button) { return false; }

    public boolean mouseReleased(double mx, double my, int button) {
        for(Widget w : childWidgets) if(w.mouseReleased(mx, my, button)) return true;
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        for(Widget w : childWidgets) if(w.mouseScrolled(mx, my, delta)) return true;
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for(Widget w : childWidgets) if(w.keyPressed(keyCode, scanCode, modifiers)) return true;
        return onKeyPress(keyCode, scanCode, modifiers);
    }

    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) { return false; }

    public boolean charTyped(char chr, int modifiers) {
        for(Widget w : childWidgets) if(w.charTyped(chr, modifiers)) return true;
        return false;
    }

    // Utilities
    public boolean isMouseOver(double mx, double my) {
        return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
    }

    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isFocused() { return focused; }

    public void addChild(Widget widget) {
        childWidgets.add(widget);
        // position child relative to this element if needed
    }

    public void removeChild(Widget widget) { childWidgets.remove(widget); }

    public void setUi(UIUtils ui) {
        this.ui = ui;
    }
}

