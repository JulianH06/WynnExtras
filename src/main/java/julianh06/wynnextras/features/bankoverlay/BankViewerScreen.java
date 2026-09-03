package julianh06.wynnextras.features.bankoverlay;

import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class BankViewerScreen extends WEScreen {
    private BankOverlay2 bankOverlay;

    public BankViewerScreen() {
        super(Text.of("WynnExtras Bank Viewer"));
    }

    @Override
    protected void init() {
        rootWidgets.clear();
        bankOverlay = BankOverlay2.createReadOnlyViewer();
        registerScrolling();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (bankOverlay != null) bankOverlay.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        return bankOverlay != null && bankOverlay.readOnlyMouseClicked(click.x(), click.y(), click.button())
                || super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return bankOverlay != null && bankOverlay.readOnlyMouseReleased(click.x(), click.y(), click.button())
                || super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return bankOverlay != null && bankOverlay.readOnlyMouseDragged(
                click.x(), click.y(), click.button(), deltaX, deltaY)
                || super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    protected void scrollList(float delta) {
        if (bankOverlay != null) bankOverlay.scrollReadOnlyViewer(delta);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    public void removed() {
        if (bankOverlay != null) bankOverlay.closeReadOnlyViewer();
        bankOverlay = null;
        super.removed();
    }
}
