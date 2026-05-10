package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class KeybindOption extends ConfigOption {
    final Supplier<Integer> getter;
    final Consumer<Integer> setter;
    private boolean listening = false;

    public KeybindOption(String name, String desc, Supplier<Integer> get, Consumer<Integer> set) {
        super(name, desc);
        this.getter = get;
        this.setter = set;
    }

    private String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE         -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT    -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT   -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT      -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT     -> "RALT";
            case GLFW.GLFW_KEY_LEFT_CONTROL  -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            default -> "KEY_" + key;
        };
    }

    @Override
    public int controlWidth() { return 95; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int bx = x + w - 90, by = y + 10;
        boolean btnHover = mx >= bx && mx < bx + 80 && my >= by && my < by + 24;
        ctx.fill(bx, by, bx + 80, by + 24, listening ? categoryColor : BORDER_DARK);
        ctx.fill(bx + 1, by + 1, bx + 79, by + 23, listening ? PARCHMENT_HOVER : (btnHover ? PARCHMENT_HOVER : PARCHMENT));
        String label = listening ? "[ ... ]" : "[ " + keyName(getter.get()) + " ]";
        ctx.drawCenteredTextWithShadow(tr, label, bx + 40, by + 8,
                listening ? 0xFFFFDD44 : TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        int bx = x + w - 90, by = y + 10;
        if (mx >= bx && mx < bx + 80 && my >= by && my < by + 24) {
            listening = !listening;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        if (listening) { listening = false; return true; }
        return false;
    }

    public boolean onKeyPressed(int key) {
        if (!listening) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) { listening = false; return true; }
        setter.accept(key);
        listening = false;
        WynnExtrasConfig.save();
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        return true;
    }
}
