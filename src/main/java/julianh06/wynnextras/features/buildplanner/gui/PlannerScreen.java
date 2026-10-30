package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/** Common uniformly-scaled canvas for the builder, crafter and atlas workspaces. */
abstract class PlannerScreen extends WEScreen {
    private final Widget content = new PlannerCanvas(false);
    private final Widget foreground = new PlannerCanvas(true);

    protected PlannerScreen(Text title) {
        super(title);
    }

    @Override protected double getTargetScaleFactor() { return 2.0; }
    @Override protected int getMinLogicalWidth() { return (int) Math.round(1100 * scaleFactor); }
    @Override protected int getMinLogicalHeight() { return (int) Math.round(600 * scaleFactor); }

    @Override
    protected void init() {
        super.init();
        computeScaleAndOffsets();
        width = screenWidth;
        height = screenHeight;
    }

    @Override
    protected final void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.updateContext(context, 1.0, 0, 0);
        try {
            content.setBounds(0, 0, width, height);
            content.draw(context, mouseX, mouseY, delta, ui);
        } finally {
            ui.updateContext(context, scaleFactor, xStart, yStart);
        }
    }

    protected abstract void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta);

    @Override
    protected final void drawForeground(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.updateContext(context, 1.0, 0, 0);
        try {
            foreground.setBounds(0, 0, width, height);
            foreground.draw(context, mouseX, mouseY, delta, ui);
        } finally {
            ui.updateContext(context, scaleFactor, xStart, yStart);
        }
    }

    protected void drawPlannerForeground(DrawContext context, int mouseX, int mouseY, float delta) {}

    protected final void renderVanillaChildren(DrawContext context, int mouseX, int mouseY, float delta) {
        for (var child : children()) {
            if (child instanceof Drawable drawable) drawable.render(context, mouseX, mouseY, delta);
        }
        ThemedButton.renderPendingTooltip(context, mouseX, mouseY);
    }

    protected final void renderPlannerTooltip(DrawContext context, List<Text> lines, int mouseX, int mouseY) {
        ui.drawTooltip(textRenderer, lines, mouseX, mouseY);
    }

    protected final void renderPlannerTooltip(
            DrawContext context, List<Text> lines, Identifier style, int mouseX, int mouseY) {
        ui.drawTooltip(textRenderer, lines, style, mouseX, mouseY);
    }

    protected final Click logicalClick(Click click) {
        return new Click(click.x() / matrixScale, click.y() / matrixScale, click.buttonInfo());
    }

    protected final double logicalCoordinate(double coordinate) {
        return coordinate / matrixScale;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        boolean handled = super.mouseClicked(click, doubled);
        if (getFocused() instanceof ThemedButton button) {
            button.setFocused(false);
            setFocused(null);
        }
        return handled;
    }

    private final class PlannerCanvas extends Widget {
        private final boolean foreground;

        private PlannerCanvas(boolean foreground) {
            this.foreground = foreground;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            if (foreground) drawPlannerForeground(context, mouseX, mouseY, delta);
            else drawPlannerContent(context, mouseX, mouseY, delta);
        }
    }
}
