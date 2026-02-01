package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Base class for aspect screen pages
 */
public abstract class AspectPage {
    protected final AspectScreenHost host;
    protected UIUtils ui;

    public AspectPage(AspectScreenHost host) {
        this.host = host;
    }

    public void setUi(UIUtils ui) {
        this.ui = ui;
    }

    /**
     * Render this page's content
     */
    public abstract void render(DrawContext context, int mouseX, int mouseY, float tickDelta);

    /**
     * Handle mouse click on this page
     * @return true if click was consumed
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Handle mouse scroll on this page
     * @return true if scroll was consumed
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /**
     * Handle key press on this page
     * @return true if key was consumed
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Handle character input on this page
     * @return true if input was consumed
     */
    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    /**
     * Called when this page becomes active
     */
    public void onActivate() {}

    /**
     * Called when this page becomes inactive
     */
    public void onDeactivate() {}

    /**
     * Get the page title for navigation
     */
    public abstract String getTitle();

    // === Helper methods that delegate to host ===

    protected int getLogicalWidth() {
        return host.getHostLogicalWidth();
    }

    protected int getLogicalHeight() {
        return host.getHostLogicalHeight();
    }

    protected double getScaleFactor() {
        return host.getScaleFactor();
    }

    protected MinecraftClient getClient() {
        return host.getClient();
    }

    protected void drawCenteredText(DrawContext context, String text, int x, int y) {
        if (ui != null) {
            ui.drawCenteredText(text, x, y, CustomColor.fromInt(0xFFFFFF), 3f);
        }
    }

    protected void drawLeftText(DrawContext context, String text, int x, int y) {
        if (ui != null) {
            ui.drawText(text, x, y, CustomColor.fromInt(0xFFFFFF), 3f);
        }
    }

    protected void drawRect(int x, int y, int width, int height, int color) {
        if (ui != null) {
            ui.drawRect(x, y, width, height, CustomColor.fromInt(color));
        }
    }

    /**
     * Convert screen X coordinate to logical coordinate
     */
    protected int toLogicalX(double screenX) {
        return (int)(screenX * getScaleFactor());
    }

    /**
     * Convert screen Y coordinate to logical coordinate
     */
    protected int toLogicalY(double screenY) {
        return (int)(screenY * getScaleFactor());
    }
}
