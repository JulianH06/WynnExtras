package julianh06.wynnextras.config.configoptions;

import julianh06.wynnextras.wtshim.utils.mc.McUtils;
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
    private final int defaultKey;
    private boolean listening = false;

    public KeybindOption(String name, String desc, Supplier<Integer> get, Consumer<Integer> set, int defaultKey) {
        super(name, desc);
        this.getter = get;
        this.setter = set;
        this.defaultKey = defaultKey;
    }

    private String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "UNBOUND";
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
    public int controlWidth() { return 152; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int resetX = x + w - 147, bx = x + w - 90, by = y + 10;
        boolean resetHover = mx >= resetX && mx < resetX + 52 && my >= by && my < by + 24;
        boolean btnHover = mx >= bx && mx < bx + 80 && my >= by && my < by + 24;
        ctx.fill(resetX, by, resetX + 52, by + 24, BORDER_DARK);
        ctx.fill(resetX + 1, by + 1, resetX + 51, by + 23, resetHover ? PARCHMENT_HOVER : PARCHMENT);
        ctx.drawCenteredTextWithShadow(tr, "Reset", resetX + 26, by + 8, getter.get() == defaultKey ? TEXT_DIM : TEXT_LIGHT);

        ctx.fill(bx, by, bx + 80, by + 24, listening ? categoryColor : BORDER_DARK);
        ctx.fill(bx + 1, by + 1, bx + 79, by + 23, listening ? PARCHMENT_HOVER : (btnHover ? PARCHMENT_HOVER : PARCHMENT));
        String label = listening ? "[ ... ]" : "[ " + keyName(getter.get()) + " ]";
        ctx.drawCenteredTextWithShadow(tr, label, bx + 40, by + 8,
                listening ? 0xFFFFDD44 : TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        int resetX = x + w - 147, bx = x + w - 90, by = y + 10;
        if (mx >= resetX && mx < resetX + 52 && my >= by && my < by + 24) {
            setter.accept(defaultKey);
            listening = false;
            WynnExtrasConfig.save();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
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
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
            setter.accept(GLFW.GLFW_KEY_UNKNOWN);
            listening = false;
            WynnExtrasConfig.save();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        setter.accept(key);
        listening = false;
        WynnExtrasConfig.save();
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        return true;
    }
}
