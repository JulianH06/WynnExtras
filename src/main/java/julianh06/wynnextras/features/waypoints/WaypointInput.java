package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicLong;

public class WaypointInput extends EasyTextInput {
    protected String searchText = "";

    public WaypointInput(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    @Override
    public void click() {
        if(!isActive) {
            isActive = true;
            BankOverlay.activeTextInput = this;
            cursorPos = input.length();
            color = CustomColor.fromHexString("FFEA00");
            return;
        }
    }

    @Override
    public void onInput(KeyInputEvent event) {
        if(!isActive) return;

        AtomicLong now = new AtomicLong();
        int action = event.getAction();
        int key = event.getKey();
        int scancode = event.getScanCode();
        //char character = event.getCharacter();

        now.set(System.currentTimeMillis());

        if (action == GLFW.GLFW_RELEASE) {
            cooldowns.remove(key);
        } else {
            // Backspace
            if (key == GLFW.GLFW_KEY_BACKSPACE && cursorPos > 0) {
                input = removeAt(cursorPos, input);
                cursorPos--;
            }
            // Delete
            else if (key == GLFW.GLFW_KEY_DELETE && cursorPos < input.length()) {
                input = removeAt(cursorPos + 1, input);
            }
            // left arrow
            else if (key == GLFW.GLFW_KEY_LEFT && cursorPos > 0) {
                cursorPos--;
            }
            // right arrow
            else if (key == GLFW.GLFW_KEY_RIGHT && cursorPos < input.length()) {
                cursorPos++;
            }
        }
    }
}
