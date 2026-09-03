package julianh06.wynnextras.config;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.colors.CustomColor;
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
    final TextInputWidget input1 = createInput();
    final TextInputWidget input2 = createInput();
    UIUtils ui;
    int editingIndex = -1;
    double scroll = 0;

    public StringListEditorScreen(Screen parent, String title, List<String> items, Consumer<List<String>> setter, boolean dualInput) {
        super(Text.literal("Edit: " + title));
        this.parent = parent;
        this.items = new ArrayList<>(items);
        this.setter = setter;
        this.dualInput = dualInput;
        input1.setFocused(true);
    }

    private static TextInputWidget createInput() {
        TextInputWidget input = new TextInputWidget(0, 0, 0, 0, 5, 8, 1);
        input.setPlaceholder("");
        input.setTextColor(CustomColor.fromInt(TEXT_LIGHT));
        input.setCursorColor(CustomColor.fromInt(TEXT_LIGHT));
        return input;
    }

    private void clearInputs() {
        input1.clearInput();
        input2.clearInput();
        editingIndex = -1;
        focusInput(input1);
    }

    private void loadItemForEditing(int index) {
        if (index < 0 || index >= items.size()) return;
        String item = items.get(index);
        editingIndex = index;
        if (dualInput && item.contains("|")) {
            String[] parts = item.split("\\|", 2);
            input1.setInputAndMoveCursorToEnd(parts[0]);
            input2.setInputAndMoveCursorToEnd(parts.length > 1 ? parts[1] : "");
        } else {
            input1.setInputAndMoveCursorToEnd(item);
            input2.clearInput();
        }
        focusInput(input1);
    }

    private void saveCurrentInput() {
        String first = input1.getInput();
        String value = dualInput ? first + "|" + input2.getInput() : first;
        if (value.isEmpty() || (dualInput && first.isEmpty())) return;

        if (editingIndex >= 0 && editingIndex < items.size()) {
            items.set(editingIndex, value);
        } else {
            items.add(value);
        }
        clearInputs();
    }

    private void focusInput(TextInputWidget input) {
        input1.setFocused(input == input1);
        input2.setFocused(input == input2);
    }

    private void updateInputBounds(int px, int pw, int inputY, boolean isEditing) {
        if (dualInput) {
            int fieldW = (pw - (isEditing ? 140 : 90)) / 2;
            input1.setBounds(px + 15, inputY, fieldW, 24);
            input2.setBounds(px + (isEditing ? 20 : 23) + fieldW, inputY, fieldW, 24);
        } else {
            input1.setBounds(px + 15, inputY, pw - (isEditing ? 135 : 80), 24);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        boolean isEditing = editingIndex >= 0;
        if (ui == null) ui = new UIUtils(ctx, 1, 0, 0);
        else ui.updateContext(ctx, 1, 0, 0);

        ctx.fill(0, 0, width, height, BG_DARK);

        int px = width / 2 - 180, pw = 360;
        ctx.fill(px, 20, px + pw, height - 20, BG_MEDIUM);
        ctx.fill(px + 2, 22, px + pw - 2, height - 22, BG_LIGHT);

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 35, GOLD);
        ctx.fill(px + 20, 48, px + pw - 20, 49, GOLD_DARK);

        int inputY = 65;
        updateInputBounds(px, pw, inputY, isEditing);
        if (dualInput) {
            ctx.drawTextWithShadow(textRenderer, "Trigger:", input1.getX(), inputY - 10, TEXT_DIM);
            ctx.fill(input1.getX(), inputY, input1.getX() + input1.getWidth(), inputY + 24, BORDER_DARK);
            ctx.fill(input1.getX() + 1, inputY + 1, input1.getX() + input1.getWidth() - 1, inputY + 23,
                    input1.isFocused() ? PARCHMENT_LIGHT : PARCHMENT);

            ctx.drawTextWithShadow(textRenderer, "Display:", input2.getX(), inputY - 10, TEXT_DIM);
            ctx.fill(input2.getX(), inputY, input2.getX() + input2.getWidth(), inputY + 24, BORDER_DARK);
            ctx.fill(input2.getX() + 1, inputY + 1, input2.getX() + input2.getWidth() - 1, inputY + 23,
                    input2.isFocused() ? PARCHMENT_LIGHT : PARCHMENT);
        } else {
            ctx.fill(input1.getX(), inputY, input1.getX() + input1.getWidth(), inputY + 24, BORDER_DARK);
            ctx.fill(input1.getX() + 1, inputY + 1, input1.getX() + input1.getWidth() - 1, inputY + 23,
                    input1.isFocused() ? PARCHMENT_LIGHT : PARCHMENT);
        }
        input1.draw(ctx, mx, my, delta, ui);
        if (dualInput) input2.draw(ctx, mx, my, delta, ui);

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
        boolean cancelH = mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24;
        boolean doneH = mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24;

        ctx.fill(width / 2 - 105, by, width / 2 - 5, by + 24, BORDER_DARK);
        ctx.fill(width / 2 - 104, by + 1, width / 2 - 6, by + 23, cancelH ? ACCENT_RED : PARCHMENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", width / 2 - 55, by + 8, TEXT_LIGHT);

        ctx.fill(width / 2 + 5, by, width / 2 + 105, by + 24, BORDER_DARK);
        ctx.fill(width / 2 + 6, by + 1, width / 2 + 104, by + 23, doneH ? TOGGLE_ON : PARCHMENT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Done", width / 2 + 55, by + 8, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x();
        double my = click.y();

        int px = width / 2 - 180, pw = 360;
        int inputY = 65;
        boolean isEditing = editingIndex >= 0;
        updateInputBounds(px, pw, inputY, isEditing);

        if (input1.mouseClicked(mx, my, click.button())) {
            input2.setFocused(false);
            return true;
        }
        if (dualInput && input2.mouseClicked(mx, my, click.button())) {
            input1.setFocused(false);
            return true;
        }

        if (isEditing) {
            if (mx >= px + pw - 115 && mx < px + pw - 68 && my >= inputY && my < inputY + 24) {
                if (!input1.getInput().isEmpty()) {
                    saveCurrentInput();
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
            if (mx >= px + pw - 63 && mx < px + pw - 16 && my >= inputY && my < inputY + 24) {
                clearInputs();
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        } else {
            if (mx >= px + pw - 60 && mx < px + pw - 15 && my >= inputY && my < inputY + 24) {
                if (!input1.getInput().isEmpty()) {
                    saveCurrentInput();
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                return true;
            }
        }

        int by = height - 55;
        if (mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24) {
            client.setScreen(parent);
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
        if (mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24) {
            setter.accept(items);
            client.setScreen(parent);
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                if (mx >= px + 15 && mx < px + pw - 50) {
                    loadItemForEditing(i);
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
        if (input1.keyPressed(key, input.scancode(), input.modifiers())
                || dualInput && input2.keyPressed(key, input.scancode(), input.modifiers())) return true;
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (!input1.getInput().isEmpty()) saveCurrentInput();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB && dualInput) {
            focusInput(input1.isFocused() ? input2 : input1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingIndex >= 0) clearInputs();
            else client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        char chr = (char) charInput.codepoint();
        if (input1.charTyped(chr, charInput.modifiers())
                || dualInput && input2.charTyped(chr, charInput.modifiers())) return true;
        return super.charTyped(charInput);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (input1.mouseDragged(click.x(), click.y(), click.button(), dx, dy)
                || dualInput && input2.mouseDragged(click.x(), click.y(), click.button(), dx, dy)) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (input1.mouseReleased(click.x(), click.y(), click.button())
                || dualInput && input2.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
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
