package julianh06.wynnextras.features.waypoints;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.utils.overlays.EasyButton;
import julianh06.wynnextras.utils.overlays.EasyDropdown;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static julianh06.wynnextras.features.waypoints.WaypointScreen.scaleFactor;

public class WaypointElement {
    public Waypoint waypoint;

    public int x;
    public int y;
    public int width;
    public int height;

    public WaypointInput xInput;
    public WaypointInput yInput;
    public WaypointInput zInput;
    public WaypointInput nameInput;

    public EasyButton deleteButton;

    public EasyButton showButton;
    public EasyButton showNameButton;
    public EasyButton showDistanceButton;
    public EasyDropdown categoryDropdown;

    Identifier deleteTexture = Identifier.of("wynnextras", "textures/gui/waypoints/deletebutton.png");
    Identifier inavtiveTexture = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxinactive.png");
    Identifier activeTexture = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxactive.png");
    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/waypointbackground2.png");
    Identifier coordsBackgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/coordinateinput.png");
    Identifier nameBackgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxsingle.png");

    public WaypointElement(int x, int y, int width, int height, Waypoint waypoint) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.waypoint = waypoint;

        deleteButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                WaypointData.INSTANCE.waypoints.remove(waypoint);
            }
        };

        showButton = new EasyButton(-1, -1, -1, -1) {
            public boolean isActive = false;

            @Override
            public void click() {
                waypoint.show = !waypoint.show;
                isActive = waypoint.show;
            }
        };

        showNameButton = new EasyButton(-1, -1, -1, -1) {
            public boolean isActive = false;

            @Override
            public void click() {
                waypoint.showName = !waypoint.showName;
                isActive = waypoint.showName;
            }
        };

        showDistanceButton = new EasyButton(-1, -1, -1, -1) {
            public boolean isActive = false;

            @Override
            public void click() {
                waypoint.showDistance = !waypoint.showDistance;
                isActive = waypoint.showDistance;
            }
        };

        categoryDropdown = new EasyDropdown(-1, -1, -1, -1);

        nameInput = new WaypointInput(x, y, 10, width);
        nameInput.setInput(waypoint.name);
        xInput = new WaypointInput(x, y, 10, width);
        xInput.setInput(String.valueOf(waypoint.x));
        yInput = new WaypointInput(x, y, 10, width);
        yInput.setInput(String.valueOf(waypoint.y));
        zInput = new WaypointInput(x, y, 10, width);
        zInput.setInput(String.valueOf(waypoint.z));
    }

    public WaypointElement(Waypoint waypoint) {
        this(-1, -1, -1, -1, waypoint);
    }

    public void draw(int x, int y, int width, int height, DrawContext context) {
        this.width = width;
        this.height = height;
        RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, x, y, width + 1, height + 2, width + 1, height + 2);

        RenderUtils.drawTexturedRect(context.getMatrices(), nameBackgroundTexture, x + 3 * 3f / scaleFactor, y + 2.5f * 3 / scaleFactor, (float) (90 * 3) / scaleFactor, (float) (13 * 3) / scaleFactor, 90 * 3 / scaleFactor, 13 * 3 / scaleFactor);
        nameInput.setX(x + 3 * 3 / scaleFactor);
        nameInput.setY(y + 4 * 3 / scaleFactor);
        nameInput.setWidth(89 * 3 / scaleFactor);
        nameInput.setHeight(12 * 3 / scaleFactor);
        nameInput.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));
        //nameInput.draw(context);

        RenderUtils.drawTexturedRect(context.getMatrices(), nameBackgroundTexture, x + 93.5f * 3 / scaleFactor, y + 2.5f * 3 / scaleFactor, (float) (90 * 3) / scaleFactor, (float) (13 * 3) / scaleFactor, 90 * 3 / scaleFactor, 13 * 3 / scaleFactor);

        categoryDropdown.setX(x + 94 * 3 / scaleFactor);
        categoryDropdown.setY(y + 4 * 3 / scaleFactor);
        categoryDropdown.setWidth((float) (89 * 3) / scaleFactor);
        categoryDropdown.setHeight((float) (12 * 3) / scaleFactor);

        if(waypoint.getCategory() == null) {
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Select a category")), x + 97f * 3 / scaleFactor, y + 6f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);
        } else {
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(waypoint.getCategory().name)), x + 97f * 3 / scaleFactor, y + 6f * 3 / scaleFactor, waypoint.getCategory().color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);
        }

        RenderUtils.drawTexturedRect(context.getMatrices(), coordsBackgroundTexture, x + 3 * 3f / scaleFactor, y + 18f * 3 / scaleFactor, (float) (65 * 3) / scaleFactor, (float) (13 * 3) / scaleFactor, 65 * 3 / scaleFactor, 13 * 3 / scaleFactor);
        xInput.setX(x + 15 * 3 / scaleFactor);
        xInput.setY(y + 19 * 3 / scaleFactor);
        xInput.setWidth(width / 3 - 13 * 3 / scaleFactor);
        xInput.setHeight(13 * 3 / scaleFactor);
        xInput.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("x:")), x + (float) (6 * 3) / scaleFactor, y + (float) (25 * 3) / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);
        //xInput.draw(context);


        RenderUtils.drawTexturedRect(context.getMatrices(), coordsBackgroundTexture, x + 2 + (float) width / 3, y + 18f * 3 / scaleFactor, 65f * 3 / scaleFactor, (float) (13 * 3) / scaleFactor, 65 * 3 / scaleFactor, 13 * 3 / scaleFactor);
        yInput.setX(x + width / 3 + 14 * 3 / scaleFactor);
        yInput.setY(y + 19 * 3 / scaleFactor);
        yInput.setWidth(width / 3 - 13 * 3 / scaleFactor);
        yInput.setHeight(13 * 3 / scaleFactor);
        yInput.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("y:")), x + (float) width / 3 + (float) (4 * 3) / scaleFactor, y + (float) (24 * 3) / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);
        //yInput.draw(context);


        RenderUtils.drawTexturedRect(context.getMatrices(), coordsBackgroundTexture, x + 1 + (float) 2 * width / 3, y + 18f * 3 / scaleFactor, 65f * 3 / scaleFactor, (float) (13 * 3) / scaleFactor, 65 * 3 / scaleFactor, 13 * 3 / scaleFactor);
        zInput.setX(x + 2 * width / 3 + 13 * 3 / scaleFactor);
        zInput.setY(y + 19 * 3 / scaleFactor);
        zInput.setWidth(width / 3 - 13 * 3 / scaleFactor);
        zInput.setHeight(13 * 3 / scaleFactor);
        zInput.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("z:")), x + (float) 2 * width / 3 + 3.5f * 3 / scaleFactor, y + (float) (24 * 3) / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);
        //zInput.draw(context);

        //FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(waypoint.name + " ")), x, y + 10, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f / scaleFactor);
        deleteButton.setX(x + width - 16 * 3 / scaleFactor);
        deleteButton.setY(y + 2 * 3 / scaleFactor);
        deleteButton.setWidth(15f * 3 / scaleFactor);
        deleteButton.setHeight(15f * 3 / scaleFactor);
        deleteButton.drawWithTexture(context, deleteTexture);

        showButton.setX(x + 3 * 3 / scaleFactor);
        showButton.setY(y + 33 * 3 / scaleFactor);
        showButton.setWidth((float) (15 * 3) / scaleFactor);
        showButton.setHeight((float) (15 * 3) / scaleFactor);
        if(waypoint.show) {
            showButton.drawWithTexture(context, activeTexture);
        } else {
            showButton.drawWithTexture(context, inavtiveTexture);
        }
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Highlight")), x + (float) (20 * 3) / scaleFactor, y + 37f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Block")), x + (float) (20 * 3) / scaleFactor, y + 45.5f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);

        showNameButton.setX(x + 3 * 3 / scaleFactor + width / 3);
        showNameButton.setY(y + 33 * 3 / scaleFactor);
        showNameButton.setWidth((float) (15 * 3) / scaleFactor);
        showNameButton.setHeight((float) (15 * 3) / scaleFactor);
        if(waypoint.showName) {
            showNameButton.drawWithTexture(context, activeTexture);
        } else {
            showNameButton.drawWithTexture(context, inavtiveTexture);
        }
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Show")), x + (float) (20 * 3) / scaleFactor + (float) width / 3, y + 37f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Name")), x + (float) (20 * 3) / scaleFactor + (float) width / 3, y + 45.5f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);

        showDistanceButton.setX(x + 2 * 3 / scaleFactor + 2 * width / 3);
        showDistanceButton.setY(y + 33 * 3 / scaleFactor);
        showDistanceButton.setWidth((float) (15 * 3) / scaleFactor);
        showDistanceButton.setHeight((float) (15 * 3) / scaleFactor);
        if(waypoint.showDistance) {
            showDistanceButton.drawWithTexture(context, activeTexture);
        } else {
            showDistanceButton.drawWithTexture(context, inavtiveTexture);
        }
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Show")), x + (float) (20 * 3) / scaleFactor + (float) 2 * width / 3, y + 37f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Distance")), x + (float) (20 * 3) / scaleFactor + (float) 2 * width / 3, y + 45.5f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
    }

    public void click(int mouseX, int mouseY) {
        if(categoryDropdown.isClickInBounds(mouseX, mouseY)) {
            WaypointCategory category = categoryDropdown.clickAndGetCategory();
            if(category != null) {
                System.out.println(category.name);
                waypoint.setCategory(category);
                WaypointData.save();
            }
            nameInput.setActive(false);
            xInput.setActive(false);
            yInput.setActive(false);
            zInput.setActive(false);
            return;
        } else if (categoryDropdown.isExpanded) {
            categoryDropdown.isExpanded = false;
            nameInput.setActive(false);
            xInput.setActive(false);
            yInput.setActive(false);
            zInput.setActive(false);
            return;
        }

        if(showButton.isClickInBounds(mouseX, mouseY)) showButton.click();
        if(showNameButton.isClickInBounds(mouseX, mouseY)) showNameButton.click();
        if(showDistanceButton.isClickInBounds(mouseX, mouseY)) showDistanceButton.click();

        if(nameInput.isClickInBounds(mouseX, mouseY)) {
            nameInput.click();
        } else {
            nameInput.setActive(false);
        }

        if(xInput.isClickInBounds(mouseX, mouseY)) {
            xInput.click();
        } else {
            xInput.setActive(false);
        }

        if(yInput.isClickInBounds(mouseX, mouseY)) {
            yInput.click();
        } else {
            yInput.setActive(false);
        }

        if(zInput.isClickInBounds(mouseX, mouseY)) {
            zInput.click();
        } else {
            zInput.setActive(false);
        }
    }
}
