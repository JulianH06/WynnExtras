package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.StringListEditorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class StringListOption extends ConfigOption {
    final Supplier<List<String>> getter;
    final Consumer<List<String>> setter;
    final String itemName;
    final boolean dualInput;

    public StringListOption(String name, String desc, Supplier<List<String>> get, Consumer<List<String>> set, String itemName, boolean dualInput) {
        super(name, desc);
        this.getter = get; this.setter = set;
        this.itemName = itemName;
        this.dualInput = dualInput;
    }

    @Override
    public int controlWidth() { return 80; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, getter.get().size() + " " + itemName, richDesc, TEXT_LIGHT, TEXT_DIM);

        int bx = x + w - 75, by = y + 12;
        boolean btnHover = mx >= bx && mx < bx + 65 && my >= by && my < by + 20;
        ctx.fill(bx, by, bx + 65, by + 20, BORDER_DARK);
        ctx.fill(bx + 1, by + 1, bx + 64, by + 19, btnHover ? PARCHMENT_HOVER : PARCHMENT);
        ctx.drawCenteredTextWithShadow(tr, "Edit...", bx + 32, by + 6, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        int bx = x + w - 75, by = y + 12;
        if (mx >= bx && mx < bx + 65 && my >= by && my < by + 20) {
            MinecraftClient.getInstance().setScreen(new StringListEditorScreen(
                    MinecraftClient.getInstance().currentScreen, name, getter.get(), setter, dualInput));
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        return false;
    }
}
