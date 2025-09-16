package julianh06.wynnextras.utils.overlays;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.features.waypoints.WaypointCategory;
import julianh06.wynnextras.features.waypoints.WaypointData;
import julianh06.wynnextras.features.waypoints.WaypointScreen;
import julianh06.wynnextras.features.waypoints.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static julianh06.wynnextras.features.waypoints.WaypointScreen.scaleFactor;

public class EasyDropdown extends EasyElement{
    public boolean isExpanded = false;
    int mouseX = 0;
    int mouseY = 0;

    Identifier categorySingleTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxsingle.png");
    Identifier categoryTopTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxtop.png");
    Identifier categoryMidTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxmid.png");
    Identifier categoryBotTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxbot.png");

    List<WaypointCategory> categories;

    public EasyDropdown(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    @Override
    public void draw(DrawContext context) {
        categories = WaypointData.INSTANCE.categories;
        if (isExpanded) {
            if(categories.isEmpty()) {
                RenderUtils.drawTexturedRect(context.getMatrices(), categorySingleTexture, x, y + height, width, 39f / scaleFactor, (int) width, 39 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Create a category!")), x + 6f / scaleFactor, y + height + 10f / scaleFactor, CustomColor.fromHexString("FF0000"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.65f / scaleFactor);
                return;
            }
            for(int i = 0; i < categories.size(); i++) {
                Identifier texture = categoryMidTexture;
                if(i == 0) texture = categoryTopTexture;
                if(i == categories.size() - 1) texture = categoryBotTexture;
                RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y + height + i * 39f / scaleFactor, width, 39f / scaleFactor, (int) width, 39 / scaleFactor);
                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(WaypointData.INSTANCE.categories.get(i).name)), x + 6f / scaleFactor, y + height + i * 39f / scaleFactor + 10f / scaleFactor, WaypointData.INSTANCE.categories.get(i).color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f / scaleFactor);
            }
        }
    }

    @Override
    public void click() {

    }

    public void drawWithTexture(DrawContext context, Identifier texture) {
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, width, height, (int) width, (int) height);
    }

    public WaypointCategory clickAndGetCategory() {
        if(!isExpanded) {
            isExpanded = true;
            return null;
        } else {
            int ySection = Math.floorDiv(mouseY - y, (39 / scaleFactor));
            isExpanded = false;
            if(ySection < 1) return null;
            return categories.get(ySection - 1);
        }
    }

    @Override
    public boolean isClickInBounds(int x, int y) {
        mouseX = x;
        mouseY = y;
        if(x < this.x) return false;
        if(y < this.y) return false;
        if(x > this.x + width) return false;
        if(isExpanded) WaypointScreen.clickWhileExpanded = true;
        if(isExpanded && categories != null) {
            if(y > this.y + height + categories.size() * (39f / scaleFactor)) return false;
        } else {
            if(y > this.y + height) return false;
        }
        return true;
    }
}
