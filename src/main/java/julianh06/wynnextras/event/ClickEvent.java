package julianh06.wynnextras.event;

import julianh06.wynnextras.event.api.WEEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class ClickEvent extends WEEvent {
    private static boolean wasLeftPressed = false;

    public double mouseX;
    public double mouseY;

    public ClickEvent(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean isLeftPressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

            if (isLeftPressed && !wasLeftPressed) {
                new ClickEvent(client.mouse.getX(), client.mouse.getY()).post();
            }

            wasLeftPressed = isLeftPressed;
        });
    }
}
