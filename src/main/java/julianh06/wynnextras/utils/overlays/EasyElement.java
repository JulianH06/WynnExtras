package julianh06.wynnextras.utils.overlays;

import net.minecraft.client.gui.DrawContext;

public abstract class EasyElement {
    protected int x;
    protected int y;
    protected int height;
    protected int width;

    public EasyElement(int x, int y, int height, int width) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.width = width;
    }

    public boolean isClickInBounds(int x, int y) {
        if(x < this.x) return false;
        if(y < this.y) return false;
        if(x > this.x + width) return false;
        if(y > this.y + height) return false;
        System.out.println("click worked!");
        return true;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public abstract void draw(DrawContext context);

    public abstract void click();
}
