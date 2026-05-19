package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public abstract class DropdownOption<T> extends ConfigOption {
    public final Supplier<T> getter;
    final Consumer<T> setter;
    private int btnX, btnY, btnW = 125, btnH = 22;
    protected final ConfigScreenContext ctx;

    protected DropdownOption(String name, String desc, Supplier<T> get, Consumer<T> set, ConfigScreenContext ctx) {
        super(name, desc);
        this.getter = get;
        this.setter = set;
        this.ctx = ctx;
    }

    public abstract void setValueByIndex(int idx);
    public abstract Object[] getValues();

    @Override
    public int controlWidth() { return 140; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        btnX = x + w - 135; btnY = y + 10;
        T val = getter.get();
        boolean btnHover = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;

        ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, BORDER_DARK);
        ctx.fill(btnX + 1, btnY + 1, btnX + btnW - 1, btnY + btnH - 1, btnHover ? PARCHMENT_HOVER : PARCHMENT);

        String txt = val.toString();
        if (txt.length() > 14) txt = txt.substring(0, 12) + "..";
        ctx.drawTextWithShadow(tr, txt, btnX + 8, btnY + 7, TEXT_LIGHT);
        ctx.drawTextWithShadow(tr, "▼", btnX + btnW - 14, btnY + 7, TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
            this.ctx.openDropdown(this, btnX, btnY + btnH, btnW);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        return false;
    }
}
