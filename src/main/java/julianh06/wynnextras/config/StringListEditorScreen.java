package julianh06.wynnextras.config;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class StringListEditorScreen extends Screen {
    final Screen parent;
    final List<String> items;
    final Consumer<List<String>> setter;
    final boolean dualInput;
    String input1 = "";
    String input2 = "";
    int activeField = 0;
    int editingIndex = -1;
    double scroll = 0;

    public StringListEditorScreen(Screen parent, String title, List<String> items, Consumer<List<String>> setter, boolean dualInput) {
        super(Text.literal("Edit: " + title));
        this.parent = parent;
        this.items = new ArrayList<>(items);
        this.setter = setter;
        this.dualInput = dualInput;
    }

    private String getActiveInput() {
        return activeField == 0 ? input1 : input2;
    }

    private void setActiveInput(String val) {
        if (activeField == 0) input1 = val;
        else input2 = val;
    }

    private void clearInputs() {
        input1 = "";
        input2 = "";
        editingIndex = -1;
        activeField = 0;
    }

    private void loadItemForEditing(int index) {
        if (index < 0 || index >= items.size()) return;
        String item = items.get(index);
        editingIndex = index;
        if (dualInput && item.contains("|")) {
            String[] parts = item.split("\\|", 2);
            input1 = parts[0];
            input2 = parts.length > 1 ? parts[1] : "";
        } else {
            input1 = item;
            input2 = "";
        }
        activeField = 0;
    }

    private void saveCurrentInput() {
        String value = dualInput ? input1 + "|" + input2 : input1;
        if (value.isEmpty() || (dualInput && input1.isEmpty())) return;

        if (editingIndex >= 0 && editingIndex < items.size()) {
            items.set(editingIndex, value);
        } else {
            items.add(value);
        }
        clearInputs();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        boolean isEditing = editingIndex >= 0;

        ctx.fill(0, 0, width, height, BG_DARK);

        int px = width / 2 - 180, pw = 360;
        ctx.fill(px, 20, px + pw, height - 20, BG_MEDIUM);
        ctx.fill(px + 2, 22, px + pw - 2, height - 22, BG_LIGHT);

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 35, GOLD);
        ctx.fill(px + 20, 48, px + pw - 20, 49, GOLD_DARK);

        int inputY = 65;
        if (dualInput) {
            if (isEditing) {
                int fieldW = (pw - 140) / 2;

                ctx.drawTextWithShadow(textRenderer, "Trigger:", px + 15, inputY - 10, TEXT_DIM);
                ctx.fill(px + 15, inputY, px + 15 + fieldW, inputY + 24, BORDER_DARK);
                ctx.fill(px + 16, inputY + 1, px + 14 + fieldW, inputY + 23, activeField == 0 ? PARCHMENT_LIGHT : PARCHMENT);
                ctx.drawTextWithShadow(textRenderer, input1 + (activeField == 0 ? "_" : ""), px + 20, inputY + 8, TEXT_LIGHT);

                ctx.drawTextWithShadow(textRenderer, "Display:", px + 20 + fieldW, inputY - 10, TEXT_DIM);
                ctx.fill(px + 20 + fieldW, inputY, px + 20 + fieldW * 2, inputY + 24, BORDER_DARK);
                ctx.fill(px + 21 + fieldW, inputY + 1, px + 19 + fieldW * 2, inputY + 23, activeField == 1 ? PARCHMENT_LIGHT : PARCHMENT);
                ctx.drawTextWithShadow(textRenderer, input2 + (activeField == 1 ? "_" : ""), px + 25 + fieldW, inputY + 8, TEXT_LIGHT);
            } else {
                int fieldW = (pw - 90) / 2;

                ctx.drawTextWithShadow(textRenderer, "Trigger:", px + 15, inputY - 10, TEXT_DIM);
                ctx.fill(px + 15, inputY, px + 15 + fieldW, inputY + 24, BORDER_DARK);
                ctx.fill(px + 16, inputY + 1, px + 14 + fieldW, inputY + 23, activeField == 0 ? PARCHMENT_LIGHT : PARCHMENT);
                String t1 = input1.length() > 18 ? input1.substring(0, 16) + ".." : input1;
                ctx.drawTextWithShadow(textRenderer, t1 + (activeField == 0 ? "_" : ""), px + 20, inputY + 8, TEXT_LIGHT);

                ctx.drawTextWithShadow(textRenderer, "Display:", px + 23 + fieldW, inputY - 10, TEXT_DIM);
                ctx.fill(px + 23 + fieldW, inputY, px + 23 + fieldW * 2, inputY + 24, BORDER_DARK);
                ctx.fill(px + 24 + fieldW, inputY + 1, px + 22 + fieldW * 2, inputY + 23, activeField == 1 ? PARCHMENT_LIGHT : PARCHMENT);
                String t2 = input2.length() > 18 ? input2.substring(0, 16) + ".." : input2;
                ctx.drawTextWithShadow(textRenderer, t2 + (activeField == 1 ? "_" : ""), px + 28 + fieldW, inputY + 8, TEXT_LIGHT);
            }
        } else {
            if (isEditing) {
                ctx.fill(px + 15, inputY, px + pw - 120, inputY + 24, BORDER_DARK);
                ctx.fill(px + 16, inputY + 1, px + pw - 121, inputY + 23, PARCHMENT);
                ctx.drawTextWithShadow(textRenderer, input1 + "_", px + 20, inputY + 8, TEXT_LIGHT);
            } else {
                ctx.fill(px + 15, inputY, px + pw - 65, inputY + 24, BORDER_DARK);
                ctx.fill(px + 16, inputY + 1, px + pw - 66, inputY + 23, PARCHMENT);
                ctx.drawTextWithShadow(textRenderer, input1 + "_", px + 20, inputY + 8, TEXT_LIGHT);
            }
        }

        if (isEditing) {
            boolean saveH = mx >= px + pw - 115 && mx < px + pw - 68 && my >= inputY && my < inputY + 24;
            ctx.fill(px + pw - 115, inputY, px + pw - 68, inputY + 24, BORDER_DARK);
            ctx.fill(px + pw - 114, inputY + 1, px + pw - 69, inputY + 23, saveH ? TOGGLE_ON : PARCHMENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Save", px + pw - 91, inputY + 8, TEXT_LIGHT);

            boolean cancelEditH = mx >= px + pw - 63 && mx < px + pw - 16 && my >= inputY && my < inputY + 24;
            ctx.fill(px + pw - 63, inputY, px + pw - 16, inputY + 24, BORDER_DARK);
            ctx.fill(px + pw - 62, inputY + 1, px + pw - 17, inputY + 23, cancelEditH ? ACCENT_RED : PARCHMENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", px + pw - 39, inputY + 8, TEXT_LIGHT);
        } else {
            boolean addH = mx >= px + pw - 60 && mx < px + pw - 15 && my >= inputY && my < inputY + 24;
            ctx.fill(px + pw - 60, inputY, px + pw - 15, inputY + 24, BORDER_DARK);
            ctx.fill(px + pw - 59, inputY + 1, px + pw - 16, inputY + 23, addH ? TOGGLE_ON : PARCHMENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "+ Add", px + pw - 37, inputY + 8, TEXT_LIGHT);
        }

        int listTop = inputY + 30;
        ctx.enableScissor(px + 10, listTop, px + pw - 10, height - 70);
        int y = listTop - (int)scroll;
        for (int i = 0; i < items.size(); i++) {
            if (y + 24 > listTop && y < height - 70) {
                boolean isSelected = i == editingIndex;
                boolean itemHover = mx >= px + 15 && mx < px + pw - 50 && my >= y && my < y + 24;
                ctx.fill(px + 15, y, px + pw - 50, y + 24, isSelected ? PARCHMENT_LIGHT : (itemHover ? PARCHMENT_HOVER : PARCHMENT));
                String t = items.get(i);
                if (t.length() > 35) t = t.substring(0, 33) + "..";
                ctx.drawTextWithShadow(textRenderer, t, px + 20, y + 8, isSelected ? GOLD : TEXT_LIGHT);

                boolean delH = mx >= px + pw - 45 && mx < px + pw - 15 && my >= y && my < y + 24;
                ctx.fill(px + pw - 45, y, px + pw - 15, y + 24, BORDER_DARK);
                ctx.fill(px + pw - 44, y + 1, px + pw - 16, y + 23, delH ? ACCENT_RED : PARCHMENT);
                ctx.drawCenteredTextWithShadow(textRenderer, "X", px + pw - 30, y + 8, TEXT_LIGHT);
            }
            y += 28;
        }
        ctx.disableScissor();

        if (items.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, "No items", width / 2, height / 2, TEXT_DIM);

        int by = height - 55;
        boolean doneH = mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24;
        boolean cancelH = mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24;

        ctx.fill(width / 2 - 105, by, width / 2 - 5, by + 24, BORDER_DARK);
        ctx.fill(width / 2 - 104, by + 1, width / 2 - 6, by + 23, doneH ? TOGGLE_ON : PARCHMENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Done", width / 2 - 55, by + 8, TEXT_LIGHT);

        ctx.fill(width / 2 + 5, by, width / 2 + 105, by + 24, BORDER_DARK);
        ctx.fill(width / 2 + 6, by + 1, width / 2 + 104, by + 23, cancelH ? ACCENT_RED : PARCHMENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", width / 2 + 55, by + 8, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x();
        double my = click.y();

        int px = width / 2 - 180, pw = 360;
        int inputY = 65;
        boolean isEditing = editingIndex >= 0;

        if (dualInput) {
            if (isEditing) {
                int fieldW = (pw - 140) / 2;
                if (mx >= px + 15 && mx < px + 15 + fieldW && my >= inputY && my < inputY + 24) {
                    activeField = 0;
                    return true;
                }
                if (mx >= px + 20 + fieldW && mx < px + 20 + fieldW * 2 && my >= inputY && my < inputY + 24) {
                    activeField = 1;
                    return true;
                }
            } else {
                int fieldW = (pw - 90) / 2;
                if (mx >= px + 15 && mx < px + 15 + fieldW && my >= inputY && my < inputY + 24) {
                    activeField = 0;
                    return true;
                }
                if (mx >= px + 23 + fieldW && mx < px + 23 + fieldW * 2 && my >= inputY && my < inputY + 24) {
                    activeField = 1;
                    return true;
                }
            }
        }

        if (isEditing) {
            if (mx >= px + pw - 115 && mx < px + pw - 68 && my >= inputY && my < inputY + 24) {
                if (!input1.isEmpty()) {
                    saveCurrentInput();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
            if (mx >= px + pw - 63 && mx < px + pw - 16 && my >= inputY && my < inputY + 24) {
                clearInputs();
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        } else {
            if (mx >= px + pw - 60 && mx < px + pw - 15 && my >= inputY && my < inputY + 24) {
                if (!input1.isEmpty()) {
                    saveCurrentInput();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
        }

        int by = height - 55;
        if (mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24) {
            setter.accept(items);
            client.setScreen(parent);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        if (mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24) {
            client.setScreen(parent);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        int listTop = inputY + 30;
        int y = listTop - (int)scroll;
        for (int i = 0; i < items.size(); i++) {
            if (my >= y && my < y + 24) {
                if (mx >= px + pw - 45 && mx < px + pw - 15) {
                    items.remove(i);
                    if (editingIndex == i) clearInputs();
                    else if (editingIndex > i) editingIndex--;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                if (mx >= px + 15 && mx < px + pw - 50) {
                    loadItemForEditing(i);
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }
            y += 28;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl && key == GLFW.GLFW_KEY_V) {
            String clipboard = client.keyboard.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                setActiveInput(getActiveInput() + clipboard.replaceAll("[\\r\\n\\t]", ""));
            }
            return true;
        } else if (ctrl && key == GLFW.GLFW_KEY_C) {
            client.keyboard.setClipboard(getActiveInput());
            return true;
        } else if (ctrl && key == GLFW.GLFW_KEY_X) {
            client.keyboard.setClipboard(getActiveInput());
            setActiveInput("");
            return true;
        }

        String current = getActiveInput();
        if (key == 259 && !current.isEmpty()) {
            setActiveInput(current.substring(0, current.length() - 1));
            return true;
        }
        if (key == 257) {
            if (!input1.isEmpty()) saveCurrentInput();
            return true;
        }
        if (key == 258 && dualInput) {
            activeField = activeField == 0 ? 1 : 0;
            return true;
        }
        if (key == 256) {
            if (editingIndex >= 0) clearInputs();
            else client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        long window = client.getWindow().getHandle();
        boolean ctrlHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (ctrlHeld) return true;
        int c = charInput.codepoint();
        if (c >= 32) {
            setActiveInput(getActiveInput() + (char) c);
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int max = Math.max(0, items.size() * 28 - (height - 165));
        scroll = MathHelper.clamp(scroll - v * 25, 0, max);
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }
}
