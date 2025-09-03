package julianh06.wynnextras.utils.overlays;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.features.inventory.BankOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class EasyTextInput extends EasyElement{
    protected boolean isActive = false;
    protected CustomColor color = CustomColor.fromHexString("FFFFFF");
    protected String input = "";
    protected String searchText = "Search...";

    protected int DEBOUNCE_DELAY_MS = 100;
    protected HashMap<Long, Long> lastCharTime = new HashMap<>();
    protected HashMap<Integer, Integer> cooldowns = new HashMap<>();
    protected int cursorPos = 0;

    protected long lastBlink = 0;
    protected boolean blinkToggle = true;

    public EasyTextInput() {
        super(-1, -1, 0, 0);
        cursorPos = 0;
    }

    public EasyTextInput(int x, int y, int height, int width) {
        super(x, y, height, width);
        cursorPos = 0;
        //detectInput();
    }

    @Override
    public void draw(DrawContext context) {
        long now = System.currentTimeMillis();
        RenderUtils.drawRect(context.getMatrices(), color, x, y, 0.0f, width, height);
        if(input.isEmpty() && !isActive) {
            context.drawText(MinecraftClient.getInstance().textRenderer, searchText, x + 1, y + 3, CustomColor.fromHexString("000000").asInt(), false);
        } else {
            if(cursorPos > input.length()) {
                cursorPos = input.length();
            }
            context.drawText(MinecraftClient.getInstance().textRenderer, input, x + 1, y + 3, CustomColor.fromHexString("000000").asInt(), false);
            if(now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }
            if(blinkToggle && isActive) RenderUtils.drawLine(context.getMatrices(), CustomColor.fromHexString("000000"), x + 1 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 2, x + 1 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 11, 0, 1);
        }
    }

    public void drawWithoutBackground(DrawContext context, CustomColor color) {
        long now = System.currentTimeMillis();
        if(input.isEmpty() && !isActive) {
            //context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, searchText, x + 3, y + 1, CustomColor.fromHexString("FFFFFF").asInt());
        } else {
            if(cursorPos > input.length()) {
                cursorPos = input.length();
            }
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, input, x + 3, y + 1, color.asInt());
            if(now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }
            if(blinkToggle && isActive) RenderUtils.drawLine(context.getMatrices(), CustomColor.fromHexString("FFFFFF"), x + 3 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y, x + 3 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 9, 0, 1);
        }
    }

    public void drawWithTexture(DrawContext context, Identifier texture) {
        long now = System.currentTimeMillis();
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, 0.0f, width, height, width, height);
        if(input.isEmpty() && !isActive) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, searchText, x + 2, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
        } else {
            if(cursorPos > input.length()) {
                cursorPos = input.length();
            }
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, input, x + 2, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
            if(now - lastBlink > 500) {
                blinkToggle = !blinkToggle;
                lastBlink = now;
            }
            if(blinkToggle && isActive) RenderUtils.drawLine(context.getMatrices(), CustomColor.fromHexString("FFFFFF"), x + 2 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 2, x + 2 + MinecraftClient.getInstance().textRenderer.getWidth(input.substring(0, cursorPos)), y + 11, 0, 1);
        }
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
        isActive = false;
        color = CustomColor.fromHexString("FFFFFF");
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean newValue) {
        isActive = newValue;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String value) {
        input = value;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int value) {
        height = value;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int value) {
        width = value;
    }

    public void clearInput() {
        input = "";
        cursorPos = 0;
    }

    public void onInput(KeyInputEvent event) {
        if(!isActive || !BankOverlay.isBank) return;

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

//        if (action == GLFW.GLFW_RELEASE) {
//            cooldowns.remove(key);
//        } else {
//            if(action == GLFW.GLFW_PRESS) {
//                DEBOUNCE_DELAY_MS = 100;
//
//            } else if (action == GLFW.GLFW_REPEAT) {
//                DEBOUNCE_DELAY_MS = 10;
//            }
//
//            if (isActive && now.get() - lastCharTime.getOrDefault((long) key, 0L) >= cooldowns.getOrDefault(key, DEBOUNCE_DELAY_MS)) {
//                if (isValidKey(key)) {
//                    if (key == GLFW.GLFW_KEY_SPACE) {
//                        input = insertAt(cursorPos, " ", input);
//                        //input += " ";
//                    } else {
//                        input = insertAt(cursorPos, InputUtil.fromKeyCode(key, scancode).getLocalizedText().getLiteralString(), input);
//                        //input += InputUtil.fromKeyCode(key, scancode).getLocalizedText().getLiteralString();
//                    }
//                    cursorPos++;
//                } else if (key == GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty() && cursorPos > 0) {
//                    input = removeAt(cursorPos, input);
//                    cursorPos--;
//                } else if (key == GLFW.GLFW_KEY_DELETE && !input.isEmpty()) {
//                    input = removeAt(cursorPos + 1, input);
//                } else if (key == GLFW.GLFW_KEY_LEFT) {
//                    cursorPos--;
//                } else if (key == GLFW.GLFW_KEY_RIGHT) {
//                    cursorPos++;
//                }
//                lastCharTime.put((long) key, now.get());
//                cooldowns.putIfAbsent(key, DEBOUNCE_DELAY_MS);
//            }
//        }
    }

    public void onCharInput(CharInputEvent event) {
        EasyTextInput ti = this;
        if (ti != null && ti.isActive()) {
            char c = event.getCharacter();
            // nur druckbare Zeichen akzeptieren
            if (!Character.isISOControl(c)) {
                ti.insertCharAtCursor(c);
            }
        }
    }


    protected boolean isValidKey(int key) {
        if(key == 32) return true;
        if(key == 39) return true;
        if(key >= 44 && key <= 57) return true;
        if(key == 59) return true;
        if(key == 61) return true;
        if(key >= 65 && key <= 93) return true;
        return false;
    }

    protected String insertAt(int i, String value, String src) {
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

    public void insertCharAtCursor(char c) {
        // Einfügen an der Cursor‐Position
        input = insertAt(cursorPos, String.valueOf(c), input);
        cursorPos++;
    }


    protected String removeAt(int i, String src) {
        if(i < 0 || i > src.length() || src.isEmpty()){
            return src;
        }
        String leftSub = src.substring(0, i - 1);
        String rightSub = src.substring(i);
        return leftSub + rightSub;
    }
}
