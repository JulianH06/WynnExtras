package julianh06.wynnextras.config.configoptions;

import julianh06.wynnextras.utils.UI.ColorPickerWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import net.minecraft.client.gui.DrawContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class ColorOption extends ConfigOption {
    private final Supplier<Integer> getter;
    private final int fallbackColor;
    private final ColorPickerWidget colorPicker;
    private UIUtils ui;

    public ColorOption(String name, String desc, Supplier<Integer> get, Consumer<Integer> set, int resetValue, int fallbackColor) {
        super(name, desc);
        this.getter = get;
        this.fallbackColor = fallbackColor & 0xFFFFFF;
        this.colorPicker = ColorPickerWidget.config(
                () -> {
                    int color = getter.get();
                    return color < 0 ? this.fallbackColor : color & 0xFFFFFF;
                },
                set,
                resetValue,
                fallbackColor).setTitle("Color: " + name);
        this.colorPicker.setBounds(0, 0, 126, 22);
    }

    @Override
    public int controlWidth() {
        return 135;
    }

    @Override
    public int getHeight(int contentW) {
        return colorPicker.isOpen()
                ? super.getHeight(contentW) + 220 + 12
                : super.getHeight(contentW);
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        if (ui == null) ui = new UIUtils(ctx, 1, 0, 0);
        else ui.updateContext(ctx, 1, 0, 0);

        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int pickerX = x + w - 128;
        colorPicker.setBounds(pickerX, y + 10, 126, 22);
        colorPicker.draw(ctx, mx, my, 1f, ui);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        return colorPicker.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        return colorPicker.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
        return colorPicker.mouseDragged(mx, my, 0, 0, 0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return colorPicker.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return colorPicker.charTyped(chr, modifiers);
    }
}
