package julianh06.wynnextras.utils.overlays;

import net.minecraft.client.gui.DrawContext;

public abstract class EasyElement {
    protected int x;
    protected int y;
    protected float height;
    protected float width;

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

    public void setWidth(float width) {
        this.width = width;
    }

    public float getWidth() {
        return width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    public abstract void draw(DrawContext context);

    public abstract void click();

    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }
}
