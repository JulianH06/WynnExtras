package julianh06.wynnextras.utils.overlays;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

import static julianh06.wynnextras.features.waypoints.WaypointScreen.*;

public class EasySlider extends EasyElement {
    private final float min;
    private final float max;
    private float value = 50f;

    public boolean dragging = false;
    private boolean useFloatInsteadOfInt;
    private final int handleWidth = 30;

    private final Consumer<Float> onValueChanged;

    Identifier sliderButtonTexture = Identifier.of("wynnextras", "textures/gui/waypoints/sliderbutton.png");
    Identifier sliderTexture = Identifier.of("wynnextras", "textures/gui/waypoints/sliderbackground.png");

    public EasySlider(int x, int y, int height, int width, float min, float max, float defaultValue, boolean useFloatInsteadOfInt, Consumer<Float> onValueChanged) {
        super(x, y, height, width);
        this.min = min;
        this.max = max;
        this.value = defaultValue;
        this.useFloatInsteadOfInt = useFloatInsteadOfInt;
        this.onValueChanged = onValueChanged;
    }

    @Override
    public void draw(DrawContext context) {
        RenderUtils.drawTexturedRect(context, sliderTexture, CustomColor.NONE, x, y + height / 2 - 2f * 3 / scaleFactor, width, 4f * 3 / scaleFactor, (int) width, 4 * 3 / scaleFactor);

        // Prevent division by zero
        float range = max - min;
        float widthRange = width - (float) handleWidth / scaleFactor;
        int handleX = (range != 0 && widthRange != 0)
            ? x + (int) ((value - min) / range * widthRange)
            : x;
        RenderUtils.drawTexturedRect(context, sliderButtonTexture, CustomColor.NONE, handleX, y, (float) handleWidth / scaleFactor, height,handleWidth / scaleFactor, (int) height);

        FontRenderer.getInstance().renderText(context,
                StyledText.fromComponent(Text.of(String.format("%.2f", value))),
                x + width + 8f * 3 / scaleFactor, y + height / 2f,
                CustomColor.fromHexString("FFFFFF"),
                HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE,
                TextShadow.NORMAL, 1f * 3 / scaleFactor);
    }

    @Override
    public void click() {
        if (isClickInBounds(mouseX, mouseY)) {
            dragging = true;
            updateValueFromMouse(mouseX);
        }
    }

    public void updateValueFromMouse(int mouseX) {
        float widthRange = width - (float) handleWidth / scaleFactor;
        float ratio = (widthRange != 0)
            ? Math.max(0, Math.min(1, (float)(mouseX - x - 15f / scaleFactor) / widthRange))
            : 0;
        value = min + ratio * (max - min);
        onValueChanged.accept(value);
    }

}
