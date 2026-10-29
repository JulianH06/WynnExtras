package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;

final class PlannerScrollbar extends Widget {
    private final SmoothScroll scroll;
    private final int minimumThumbHeight;
    private int maxScroll;
    private boolean dragging;

    PlannerScrollbar(SmoothScroll scroll, int minimumThumbHeight) {
        this.scroll = scroll;
        this.minimumThumbHeight = minimumThumbHeight;
    }

    void configure(int x, int y, int height, int maxScroll) {
        setBounds(x, y, 6, Math.max(0, height));
        this.maxScroll = Math.max(0, maxScroll);
        setVisible(this.maxScroll > 0 && height > 0);
        if (!isVisible()) dragging = false;
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        if (maxScroll <= 0) return;
        int thumbHeight = thumbHeight();
        int thumbY = y + Math.round(scroll.position() / maxScroll * (height - thumbHeight));
        ui.fill(x + 2, y, x + 4, y + height, UiTheme.BORDER);
        ui.fill(x + 2, thumbY, x + 4, thumbY + thumbHeight,
                dragging || hovered ? UiTheme.accent() : 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isVisible() || !contains((int) mouseX, (int) mouseY)) return false;
        dragging = true;
        moveTo(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || !dragging) return false;
        moveTo(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !dragging) return false;
        dragging = false;
        return true;
    }

    private void moveTo(double mouseY) {
        int thumbHeight = thumbHeight();
        int travel = height - thumbHeight;
        if (travel <= 0) {
            scroll.jump(0);
            return;
        }
        float relative = (float) mouseY - y - thumbHeight / 2.0F;
        scroll.jump(Math.clamp(relative / travel * maxScroll, 0.0F, maxScroll));
    }

    private int thumbHeight() {
        return Math.max(minimumThumbHeight, height * height / Math.max(1, height + maxScroll));
    }
}
