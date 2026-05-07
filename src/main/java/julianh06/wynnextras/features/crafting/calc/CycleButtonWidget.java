package julianh06.wynnextras.features.crafting.calc;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.function.Consumer;

/**
 * A button widget that cycles through a list of options on click.
 * Left-click cycles forward, right-click cycles backward.
 */
public class CycleButtonWidget extends Widget {

    private final String label;
    private String[] options;
    private int selectedIndex = 0;
    private Consumer<Integer> onChange;

    public CycleButtonWidget(String label, String[] options) {
        super(0, 0, 0, 0);
        this.label = label;
        this.options = options;
    }

    public CycleButtonWidget(String label, String[] options, Consumer<Integer> onChange) {
        this(label, options);
        this.onChange = onChange;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (ui == null) return;
        ui.drawButton(x, y, width, height, hovered);

        String display = "§7" + label + ": §f" + options[selectedIndex];
        ui.drawCenteredText(display, x + width / 2f, y + height / 2f,
                hovered ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"), 2.8f);
    }

    @Override
    protected boolean onClick(int button) {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        if (button == 0) {
            selectedIndex = (selectedIndex + 1) % options.length;
        } else if (button == 1) {
            selectedIndex = (selectedIndex - 1 + options.length) % options.length;
        }
        if (onChange != null) onChange.accept(selectedIndex);
        return true;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedOption() {
        return options[selectedIndex];
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.length) {
            this.selectedIndex = index;
        }
    }

    public void setSelectedIndexSilent(int index) {
        if (index >= 0 && index < options.length) {
            this.selectedIndex = index;
        }
    }

    public void setOptions(String[] newOptions) {
        this.options = newOptions;
        if (this.selectedIndex >= newOptions.length) {
            this.selectedIndex = 0;
        }
    }

    public void setOnChange(Consumer<Integer> onChange) {
        this.onChange = onChange;
    }
}
