package julianh06.wynnextras.features.aspects.pages;

import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;

public class PageWidget extends Widget {
    protected AspectScreen parent;

    public PageWidget(AspectScreen parent) {
        super(0, 0, 0, 0);
        this.parent = parent;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {}

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {}

    @Override
    protected boolean onClick(int button) { return false; }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return super.mouseReleased(mx, my, button);
    }
}
