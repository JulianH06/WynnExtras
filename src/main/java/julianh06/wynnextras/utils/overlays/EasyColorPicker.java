package julianh06.wynnextras.utils.overlays;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import static julianh06.wynnextras.features.waypoints.old.WaypointScreen.scaleFactor;

import java.awt.*;

public class EasyColorPicker extends EasyElement{
    public boolean expanded = false;
    int mouseX = 0;
    int mouseY = 0;
    private CustomColor selectedColor;

    Identifier hsvhueTexture = Identifier.of("wynnextras", "textures/gui/waypoints/hsvhue.png");
    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/colorpickerbackground.png");

    private final int pickerWidth = 200;
    private final int pickerHeight = 200;

    public EasyColorPicker(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void draw(DrawContext context) {
        if(selectedColor == null) selectedColor = CustomColor.fromHexString("ffffff");
        RenderUtils.drawRect(context, selectedColor, x, y, width, height);

        if (expanded) {
            int pickerX = (int) (x + width + 25f / scaleFactor);
            int pickerY = y;

            //RenderUtils.drawRect(context.getMatrices(), selectedColor, pickerX, pickerY, 0, (float) pickerWidth / scaleFactor, (float) pickerHeight / scaleFactor);

            RenderUtils.drawTexturedRect(context, hsvhueTexture, CustomColor.NONE, pickerX, pickerY, (float) pickerWidth / scaleFactor, (float) pickerHeight / scaleFactor, pickerWidth / scaleFactor, pickerHeight / scaleFactor);
            RenderUtils.drawTexturedRect(context, backgroundTexture, CustomColor.NONE, pickerX - 5f / scaleFactor, pickerY - 5f / scaleFactor,  210f / scaleFactor, 210f / scaleFactor, 210 / scaleFactor, 210 / scaleFactor);

            //RenderUtils.drawTexturedRect(context.getMatrices(), grayscaleTexture, pickerX + (float) pickerWidth / scaleFactor + 10f / scaleFactor, pickerY, (float) 20 / scaleFactor, (float) pickerHeight / scaleFactor, 20 / scaleFactor, pickerHeight / scaleFactor);

        }
    }

    @Override
    public void click() {}

    public void click(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (!expanded) {
            expanded = true;
        } else {
            int pickerX = (int) (x + width + 5);
            int pickerY = y;

            if (mouseX >= pickerX && mouseX < pickerX + pickerWidth &&
                    mouseY >= pickerY && mouseY < pickerY + pickerHeight) {

                int relX = mouseX - pickerX;
                int relY = mouseY - pickerY;

                float hue = (float) relX / pickerWidth * scaleFactor;
                float brightness = 1f - ((float) relY / pickerHeight) * scaleFactor;
                Color color = Color.getHSBColor(hue, 1f, brightness);
                selectedColor = new CustomColor(color.getRed(), color.getGreen(), color.getBlue(), 255);
            }

            //expanded = false;
        }
    }

    public CustomColor getSelectedColor() {
        if(selectedColor == null) selectedColor = CustomColor.fromHexString("ffffff");
        return selectedColor;
    }

    public void setSelectedColor(CustomColor color) {
        selectedColor = color;
    }


    @Override
    public boolean isClickInBounds(int x, int y) {
        if(expanded) {
            int pickerX = (int) (this.x + this.width + 5);
            int pickerY = this.y;

            int pickerRight = pickerX + pickerWidth / scaleFactor;
            int pickerBottom = pickerY + pickerHeight / scaleFactor;


            boolean inPicker = x >= pickerX && x <= pickerRight &&
                    y >= pickerY && y <= pickerBottom;

            if (inPicker) return true;
            return false;
        }
        if(x < this.x) return false;
        if(y < this.y) return false;
        if(x > this.x + width) return false;
        if(y > this.y + height) return false;
        return true;
    }
}
