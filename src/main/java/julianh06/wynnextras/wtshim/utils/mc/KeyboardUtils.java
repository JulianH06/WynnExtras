// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of KeyboardUtils.
 */
package julianh06.wynnextras.wtshim.utils.mc;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class KeyboardUtils {
    private KeyboardUtils() {}

    private static long window() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    public static boolean isKeyDown(int key) {
        return GLFW.glfwGetKey(window(), key) == GLFW.GLFW_PRESS;
    }

    public static boolean isControlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isAltDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);
    }
}
