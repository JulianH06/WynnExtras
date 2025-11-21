package julianh06.wynnextras.features.waypoints;

import com.google.gson.Gson;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.overlays.EasyButton;
import julianh06.wynnextras.utils.overlays.EasyColorPicker;
import julianh06.wynnextras.utils.overlays.EasySlider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static julianh06.wynnextras.features.waypoints.WaypointScreen.mouseX;
import static julianh06.wynnextras.features.waypoints.WaypointScreen.scaleFactor;

public class PackageElement {
    public WaypointPackage waypointPackage;

    public int x;
    public int y;
    public int width;
    public int height;

    public WaypointInput nameInput;
    public EasyButton deleteButton;
    public EasyButton enableButton;
    public EasyButton exportButton;
    public EasyButton duplicateButton;
    public EasyButton selectButton;

    Identifier deleteTexture = Identifier.of("wynnextras", "textures/gui/waypoints/deletebutton.png");
    Identifier exportButtonTexture = Identifier.of("wynnextras", "textures/gui/waypoints/exportbutton.png");
    Identifier activeTexture = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxactive.png");
    Identifier inactiveTexture = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxinactive.png");
    Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categorybackground.png");
    Identifier nameBackgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/nameinputcategory.png");

    public PackageElement(int x, int y, int width, int height, WaypointPackage waypointPackage) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.waypointPackage = waypointPackage;

        deleteButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                WaypointData.INSTANCE.deletePackage(waypointPackage.name);
                WaypointScreen.packageElements.clear();
                for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
                    WaypointScreen.packageElements.add(new PackageElement(-1, -1, -1, -1, pkg));
                }
            }
        };

        enableButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                if(waypointPackage == null) return;
                waypointPackage.enabled = !waypointPackage.enabled;
            }
        };

        duplicateButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                WaypointData.INSTANCE.duplicatePackage(waypointPackage.name);
                WaypointScreen.packageElements.clear();
                for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
                    WaypointScreen.packageElements.add(new PackageElement(-1, -1, -1, -1, pkg));
                }
            }
        };

        exportButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                String json = new Gson().toJson(waypointPackage);
                MinecraftClient.getInstance().keyboard.setClipboard(json);
                WaypointData.save();
            }
        };

        selectButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                WaypointData.INSTANCE.activePackage = waypointPackage;

                WaypointScreen.elements = new ArrayList<>();
                WaypointScreen.categories = new ArrayList<>();

                for(Waypoint waypoint : WaypointData.INSTANCE.activePackage.waypoints) {
                    WaypointScreen.elements.add(new WaypointElement(waypoint));
                }

                for(WaypointCategory category : WaypointData.INSTANCE.activePackage.categories) {
                    WaypointScreen.categories.add(new CategoryElement(category));
                }

                WaypointScreen.activeCategories.clear();
                WaypointScreen.activeCategories.addAll(WaypointData.INSTANCE.activePackage.categories);
                WaypointScreen.scrollOffset = 0;
            }
        };

        nameInput = new WaypointInput(x, y, 10, width);
        String input;
        if(waypointPackage.name == null) input = WaypointData.INSTANCE.generateUniqueName("Unnamed Package");
        else input = waypointPackage.name;
        nameInput.setInput(input);
    }

    public void draw(int x, int y, int width, int height, DrawContext context) {
        this.width = width;
        this.height = height;
        RenderUtils.drawTexturedRect(context.getMatrices(), backgroundTexture, x, y, width + 1, height + 2, width + 1, height + 2);

        RenderUtils.drawTexturedRect(context.getMatrices(), nameBackgroundTexture, x + 3 * 3f / scaleFactor, y + 2.5f * 3 / scaleFactor, (float) (180 * 3) / scaleFactor, (float) (13 * 3) / scaleFactor, 180 * 3 / scaleFactor, 13 * 3 / scaleFactor);
        nameInput.setX(x + 3 * 3 / scaleFactor);
        nameInput.setY(y + 4 * 3 / scaleFactor);
        nameInput.setWidth(180 * 3 / scaleFactor);
        nameInput.setHeight(12 * 3 / scaleFactor);
        nameInput.drawWithoutBackgroundButWithSearchtext(context, CustomColor.fromHexString("FFFFFF"));
        //nameInput.draw(context);

        //FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(waypoint.name + " ")), x, y + 10, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f / scaleFactor);
        deleteButton.setX(x + width - 16 * 3 / scaleFactor);
        deleteButton.setY(y + 2 * 3 / scaleFactor);
        deleteButton.setWidth(15f * 3 / scaleFactor);
        deleteButton.setHeight(15f * 3 / scaleFactor);
        deleteButton.drawWithTexture(context, deleteTexture);

        enableButton.setX(x + 9 / scaleFactor);
        enableButton.setY(y + 20 * 3 / scaleFactor);
        enableButton.setWidth(15f * 3 / scaleFactor);
        enableButton.setHeight(15f * 3 / scaleFactor);
        enableButton.drawWithTexture(context, waypointPackage.enabled ? activeTexture : inactiveTexture);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Toggle package")), x + 65f / scaleFactor, y + 24f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);

        selectButton.setX(x + width / 2 - 150 / scaleFactor);
        selectButton.setY(y + 60 * 3 / scaleFactor);
        selectButton.setWidth(300f / scaleFactor);
        selectButton.setHeight(36f / scaleFactor);
        selectButton.drawWithTexture(context, exportButtonTexture);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Edit package")), x + (float) width / 2, y + 62f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);

//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Enable/")), x + (float) (20 * 3) / scaleFactor, y + 25f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Disable")), x + (float) (20 * 3) / scaleFactor, y + 34f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Highlight")), x + (float) (20 * 3) / scaleFactor, y + 43f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("For all")), x + (float) (20 * 3) / scaleFactor, y + 52f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);


        duplicateButton.setX(x + width / 2 - 150 / scaleFactor);
        duplicateButton.setY(y + 45 * 3 / scaleFactor);
        duplicateButton.setWidth(300f / scaleFactor);
        duplicateButton.setHeight(36f / scaleFactor);
        duplicateButton.drawWithTexture(context, exportButtonTexture);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Duplicate Package")), x + (float) width / 2, y + 48f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);

//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Enable/")), x + (float) (81 * 3) / scaleFactor, y + 25f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Disable")), x + (float) (81 * 3) / scaleFactor, y + 34f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Name")), x + (float) (81 * 3) / scaleFactor, y + 43f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("For all")), x + (float) (81 * 3) / scaleFactor, y + 52f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);


        exportButton.setX(x + width / 2 - 150 / scaleFactor);
        exportButton.setY(y + height - 53 / scaleFactor);
        exportButton.setWidth(300f / scaleFactor);
        exportButton.setHeight(36f / scaleFactor);
        exportButton.drawWithTexture(context, exportButtonTexture);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Export to clipboard")), x + (float) width / 2, y + height - 45f / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);

//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Enable/")), x + (float) (136 * 3) / scaleFactor, y + 25f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Disable")), x + (float) (136 * 3) / scaleFactor, y + 34f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Distance")), x + (float) (136 * 3) / scaleFactor, y + 43f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);
//        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("For all")), x + (float) (136 * 3) / scaleFactor, y + 52f * 3 / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 2.75f / scaleFactor);

    }

    public void click(int mouseX, int mouseY) {
        if (enableButton.isClickInBounds(mouseX, mouseY)) {
            enableButton.click();
        }

        if (duplicateButton.isClickInBounds(mouseX, mouseY)) {
            duplicateButton.click();
        }

        if(exportButton.isClickInBounds(mouseX, mouseY)) {
            exportButton.click();
        }

        if(selectButton.isClickInBounds(mouseX, mouseY)) {
            selectButton.click();
        }

        if(deleteButton.isClickInBounds(mouseX, mouseY)) {
            deleteButton.click();
        }

        if(nameInput.isClickInBounds(mouseX, mouseY)) {
            nameInput.click();
        } else {
            nameInput.setActive(false);
        }
    }
}
