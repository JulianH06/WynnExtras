package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class BooleanOption extends ConfigOption {
    final Supplier<Boolean> getter;
    final Consumer<Boolean> setter;

    public BooleanOption(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        super(name, desc);
        this.getter = get;
        this.setter = set;
    }

    @Override
    public int controlWidth() { return 60; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int tx = x + w - 55, ty = y + 12;
        boolean val = getter.get();
        ctx.fill(tx, ty, tx + 44, ty + 20, BORDER_DARK);
        ctx.fill(tx + 1, ty + 1, tx + 43, ty + 19, val ? TOGGLE_ON : TOGGLE_OFF);
        int kx = val ? tx + 24 : tx + 2;
        ctx.fill(kx, ty + 2, kx + 18, ty + 18, BORDER_DARK);
        ctx.fill(kx + 1, ty + 3, kx + 17, ty + 17, GOLD);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        int tx = x + w - 55, ty = y + 12;
        if (mx >= tx && mx < tx + 44 && my >= ty && my < ty + 20) {
            setter.accept(!getter.get());
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        return false;
    }
}
