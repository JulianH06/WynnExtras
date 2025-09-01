package julianh06.wynnextras.features.raid;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicLong;

public class RaidListFilter extends EasyTextInput {
    public RaidListFilter(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public void setSearchText(String value) {
        super.searchText = value;
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
            if(action == GLFW.GLFW_PRESS) {
                DEBOUNCE_DELAY_MS = 100;

            } else if (action == GLFW.GLFW_REPEAT) {
                DEBOUNCE_DELAY_MS = 10;
            }

            System.out.println("ISACTIVE: " + this.isActive);
            if (now.get() - lastCharTime.getOrDefault((long) key, 0L) >= cooldowns.getOrDefault(key, DEBOUNCE_DELAY_MS)) {
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

    @Override
    public void drawWithTexture(DrawContext context, Identifier texture) {
        long now = System.currentTimeMillis();
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, 0.0f, width, height, width, height);
        if(input.isEmpty() && !isActive) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, searchText, x + 4, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
        } else {
            if(cursorPos > input.length()) {
                cursorPos = input.length();
            }
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, input, x + 4, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
            if(now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }
            if(blinkToggle && isActive) RenderUtils.drawLine(context.getMatrices(), CustomColor.fromHexString("FFFFFF"), x + 4 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 2, x + 4 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 11, 0, 1);
        }
    }
}
