package julianh06.wynnextras.features.profileviewer;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class Searchbar extends TextInputWidget {
    public Searchbar(int x, int y, int height, int width) {
        super(x, y, width, height, 9, 6, 3f);
        setBackgroundColor(null);
        setTextColor(CustomColor.fromHexString("FFFFFF"));
        setPlaceholderColor(CustomColor.fromHexString("AAAAAA"));
        setCursorColor(CustomColor.fromHexString("FFFFFF"));
        setSelectionColor(CustomColor.fromInt(0xAA3366CC));
    }

    public void click() {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        setFocused(true);
    }

    public void setActive(boolean active) {
        setFocused(active);
    }

    public boolean isActive() {
        return isFocused();
    }

    public void setSearchText(String value) {
        setPlaceholder(value);
    }

    public void setInputAndKeepStartVisible(String value) {
        setInput(value);
        cursorPos = 0;
        selectionAnchor = 0;
        horizontalTextOffset = 0;
    }

    public void setX(int value) {
        setPosition(value, getY());
    }

    public void setY(int value) {
        setPosition(getX(), value);
    }

    public void setWidth(int value) {
        setSize(value, getHeight());
    }

    public void setHeight(int value) {
        setSize(getWidth(), value);
    }

    public boolean isClickInBounds(int mouseX, int mouseY) {
        return mouseX >= getX()
                && mouseY >= getY()
                && mouseX < getX() + getWidth()
                && mouseY < getY() + getHeight();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isClickInBounds((int) mx, (int) my)) {
            clearInput();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            setFocused(true);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
