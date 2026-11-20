package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

final class PlannerSlider extends Widget {
    private static final int KNOB_WIDTH = 16;
    private final IntSupplier value;
    private final IntSupplier maximum;
    private final IntConsumer change;
    private boolean dragging;

    PlannerSlider(int x, int y, int width, int height,
            IntSupplier value, IntSupplier maximum, IntConsumer change) {
        super(x, y, width, height);
        this.value = value;
        this.maximum = maximum;
        this.change = change;
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int trackHeight = 4;
        int trackY = y + (height - trackHeight) / 2;
        int maximum = maximum();
        double progress = maximum == 0 ? 0.0D : current() / (double) maximum;
        int knobX = x + (int) Math.round(progress * Math.max(0, width - KNOB_WIDTH));
        UiTheme.drawRoundedBox(context, ui, x, trackY, width, trackHeight,
                UiTheme.SURFACE, UiTheme.BORDER);
        if (knobX > x) ui.fill(x + 1, trackY + 1, knobX, trackY + trackHeight - 1, UiTheme.TEXT);
        UiTheme.drawRoundedBox(context, ui, knobX, y + 2, KNOB_WIDTH, height - 4,
                hovered || dragging || focused ? UiTheme.hoverBackground() : UiTheme.SURFACE, UiTheme.TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled || button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || !contains((int) mouseX, (int) mouseY)) return false;
        dragging = true;
        setFocused(true);
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        update(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        update(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        dragging = false;
        return true;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN) {
            setValue(current() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP) {
            setValue(current() + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setValue(0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            setValue(maximum());
            return true;
        }
        return false;
    }

    private void update(double mouseX) {
        int maximum = maximum();
        double progress = Math.clamp((mouseX - x - KNOB_WIDTH / 2.0D)
                / Math.max(1.0D, width - KNOB_WIDTH), 0.0D, 1.0D);
        setValue((int) Math.round(progress * maximum));
    }

    private int current() {
        return Math.clamp(value.getAsInt(), 0, maximum());
    }

    private int maximum() {
        return Math.max(0, maximum.getAsInt());
    }

    private void setValue(int value) {
        int clamped = Math.clamp(value, 0, maximum());
        if (clamped != current()) change.accept(clamped);
    }
}
