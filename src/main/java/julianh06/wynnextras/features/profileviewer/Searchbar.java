package julianh06.wynnextras.features.profileviewer;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicLong;

public class Searchbar extends EasyTextInput {
    public Searchbar(int x, int y, int height, int width) {
        super(x, y, height, width);
    }
    int scaleFactor;

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

    @Override
    public void drawWithoutBackground(DrawContext context, CustomColor color) {
        if(input == null) return;
        scaleFactor = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        long now = System.currentTimeMillis();
        if(input.isEmpty() && !isActive) {
            //context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, searchText, x + 3, y + 1, CustomColor.fromHexString("FFFFFF").asInt());
        } else {
            if(cursorPos > input.length()) {
                cursorPos = input.length();
            }
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(input)), x + (float) (3 * 3) / scaleFactor, y + (float) 3 / scaleFactor, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
            if(now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }
            if(blinkToggle && isActive) RenderUtils.drawLine(context.getMatrices(), CustomColor.fromHexString("FFFFFF"), x + (float) (4 * 3) / scaleFactor + (float) (MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)) * 3) / scaleFactor, y, x + (float) (4 * 3) / scaleFactor + (float) (MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)) * 3) / scaleFactor, y + (float) (10 * 3) / scaleFactor, 0, 1f * 3 / scaleFactor);
        }
    }

    public void setSearchText(String value) {
        super.searchText = value;
    }
}
