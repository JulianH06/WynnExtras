package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class SliderOption extends ConfigOption {
    final int min, max;
    final Supplier<Integer> getter;
    final Consumer<Integer> setter;
    boolean dragging = false;
    int sliderX, sliderW = 120;

    public SliderOption(String name, String desc, int min, int max, Supplier<Integer> get, Consumer<Integer> set) {
        super(name, desc);
        this.min = min; this.max = max; this.getter = get; this.setter = set;
    }

    @Override
    public int controlWidth() { return 145; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        sliderX = x + w - 130;
        int sy = y + 15, val = getter.get();
        float pct = (float)(val - min) / (max - min);

        ctx.fill(sliderX, sy, sliderX + sliderW, sy + 8, BORDER_DARK);
        ctx.fill(sliderX + 1, sy + 1, sliderX + sliderW - 1, sy + 7, BG_MEDIUM);
        int fill = (int)((sliderW - 2) * pct);
        if (fill > 0) ctx.fill(sliderX + 1, sy + 1, sliderX + 1 + fill, sy + 7, categoryColor);

        int kx = sliderX + (int)(sliderW * pct) - 5;
        ctx.fill(kx, sy - 3, kx + 10, sy + 11, BORDER_DARK);
        ctx.fill(kx + 1, sy - 2, kx + 9, sy + 10, GOLD);

        ctx.drawTextWithShadow(tr, String.valueOf(val), x + w - 135 - tr.getWidth(String.valueOf(val)), sy, GOLD);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        int sy = y + 12;
        if (mx >= sliderX - 5 && mx < sliderX + sliderW + 10 && my >= sy && my < sy + 18) {
            dragging = true;
            updateValue(mx);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (dragging) { dragging = false; return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
        if (dragging) { updateValue(mx); return true; }
        return false;
    }

    void updateValue(double mx) {
        float pct = MathHelper.clamp((float)(mx - sliderX) / sliderW, 0, 1);
        setter.accept(min + Math.round(pct * (max - min)));
    }
}
