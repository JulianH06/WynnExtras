package julianh06.wynnextras.features.raid;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicLong;

public class PlayerFilter extends EasyTextInput {
    public PlayerFilter(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    @Override
    public void click() {
        isActive = true;
        cursorPos = input.length();
        color = CustomColor.fromHexString("FFEA00");
    }

    public void onInput(KeyInputEvent event) {
        //long handle = MinecraftClient.getInstance().getWindow().getHandle();
        AtomicLong now = new AtomicLong();
        int action = event.getAction();
        int key = event.getKey();
        int scancode = event.getScanCode();

        now.set(System.currentTimeMillis());

        if (action == GLFW.GLFW_RELEASE) {
            cooldowns.remove(key);
        } else {
            if(action == GLFW.GLFW_PRESS) {
                DEBOUNCE_DELAY_MS = 100;

            } else if (action == GLFW.GLFW_REPEAT) {
                DEBOUNCE_DELAY_MS = 10;
            }

            if (isActive && now.get() - lastCharTime.getOrDefault((long) key, 0L) >= cooldowns.getOrDefault(key, DEBOUNCE_DELAY_MS)) {
                if (isValidKey(key)) {
                    if (key == GLFW.GLFW_KEY_SPACE) {
                        input = insertAt(cursorPos, " ", input);
                        //input += " ";
                    } else {
                        input = insertAt(cursorPos, InputUtil.fromKeyCode(key, scancode).getLocalizedText().getLiteralString(), input);
                        //input += InputUtil.fromKeyCode(key, scancode).getLocalizedText().getLiteralString();
                    }
                    cursorPos++;
                } else if (key == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty() && cursorPos > 0) {
                    input = removeAt(cursorPos, input);
                    cursorPos--;
                } else if (key == GLFW.GLFW_KEY_DELETE && !input.isEmpty()) {
                    input = removeAt(cursorPos + 1, input);
                } else if (key == GLFW.GLFW_KEY_LEFT) {
                    cursorPos--;
                } else if (key == GLFW.GLFW_KEY_RIGHT) {
                    cursorPos++;
                }
                lastCharTime.put((long) key, now.get());
                cooldowns.putIfAbsent(key, DEBOUNCE_DELAY_MS);
            }
        }
    }

    private boolean isValidKey(int key) {
        if(key == 32) return true;
        if(key == 39) return true;
        if(key >= 44 && key <= 57) return true;
        if(key == 59) return true;
        if(key == 61) return true;
        if(key >= 65 && key <= 93) return true;
        return false;
    }

    private String insertAt(int i, String value, String src) {
        //Inserts AFTER index
        if(i < 0) {
            return null;
        }
        if(i >= src.length()) {
            src += value;
            return src;
        }
        String leftSub = src.substring(0, i);
        String rightSub = src.substring(i);
        return leftSub + value + rightSub;
    }

    private String removeAt(int i, String src) {
        if(i < 0 || i > src.length() || src.isEmpty()){
            return src;
        }
        String leftSub = src.substring(0, i - 1);
        String rightSub = src.substring(i);
        return leftSub + rightSub;
    }
}
