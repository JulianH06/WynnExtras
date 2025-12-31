package julianh06.wynnextras.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import julianh06.wynnextras.utils.overlays.EasyDropdown;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.noise.NoiseParametersKeys;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WaypointScreen extends Screen {
    public static int mouseX = 0;
    public static int mouseY = 0;
    public static int scaleFactor;
    public static int scrollOffset = 0;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms
    static List<WaypointElement> elements = new ArrayList<>();
    static List<CategoryElement> categories = new ArrayList<>();
    public static boolean clickWhileExpanded = false;
    public static boolean inMainScreen = true;

    static EasyButton addNewButton;
    static EasyButton editCategoriesButton;
    static EasyButton importButton;
    static EasyButton backToPackagesButton;
    static List<PackageElement> packageElements = new ArrayList<>();
    public static EasyDropdown categoryDropdown;


    Identifier addNewButtonTexture = Identifier.of("wynnextras", "textures/gui/waypoints/addnewbutton.png");
    Identifier importButtonTexture = Identifier.of("wynnextras", "textures/gui/waypoints/importbutton.png");

    static List<WaypointCategory> activeCategories = new ArrayList<>();

    Identifier categorySingleTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxsingle.png");
    Identifier categoryTopTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxtop.png");
    Identifier categoryMidTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxmid.png");
    Identifier categoryBotTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxbot.png");
    Identifier nameBackgroundTexture = Identifier.of("wynnextras", "textures/gui/waypoints/categoryboxsingle.png");


    protected WaypointScreen() {
        super(Text.of("Waypoints"));

        addNewButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                if(WaypointData.INSTANCE.activePackage == null) {
                    WaypointPackage waypointPackage = new WaypointPackage(WaypointData.INSTANCE.generateUniqueName("New package"));
                    packageElements.add(new PackageElement(-1, -1, -1, -1, waypointPackage));
                    WaypointData.INSTANCE.packages.add(waypointPackage);
                    return;
                }
                if(inMainScreen) {
                    if (MinecraftClient.getInstance().player == null) return;
                    int x = (int) Math.floor(MinecraftClient.getInstance().player.getX());
                    int y = (int) Math.floor(MinecraftClient.getInstance().player.getY()) - 1;
                    int z = (int) Math.floor(MinecraftClient.getInstance().player.getZ());
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                            Text.of("Waypoint added at " + x + " " + y + " " + z)));
                    Waypoint waypoint = new Waypoint(x, y, z);
                    WaypointData.INSTANCE.activePackage.waypoints.add(waypoint);
                    elements.add(new WaypointElement(waypoint));
                } else {
                    WaypointCategory category = new WaypointCategory("New Category");
                    categories.add( new CategoryElement(category));
                    WaypointData.INSTANCE.activePackage.categories.add(category);
                }
                WaypointData.save();
            }

            @Override
            public void draw(DrawContext context) {
                RenderUtils.drawTexturedRect(context.getMatrices(), addNewButtonTexture, x, y, width, height, (int) width, (int) height);
            }
        };

        editCategoriesButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                inMainScreen = !inMainScreen;
                activeCategories.clear();
                activeCategories.addAll(WaypointData.INSTANCE.activePackage.categories);
                scrollOffset = 0;
                if(categories == null) return;
                for (CategoryElement element : categories) {
                    if (element.category != null) {
                        String oldName = element.category.name;
                        String newName = element.nameInput.getInput();

                        if (!oldName.equals(newName)) {
                            element.category.name = newName;

                            for (Waypoint waypoint : WaypointData.INSTANCE.activePackage.waypoints) {
                                if (waypoint.categoryName != null && waypoint.categoryName.equals(oldName)) {
                                    waypoint.categoryName = newName;
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < Math.min(categories.size(), WaypointData.INSTANCE.activePackage.categories.size()); i++) {
                    CategoryElement element = categories.get(i);
                    WaypointData.INSTANCE.activePackage.categories.get(i).name = element.nameInput.getInput();
                    WaypointData.INSTANCE.activePackage.categories.get(i).color = element.colorPicker.getSelectedColor();
                }

                WaypointData.save();
            }

            @Override
            public void draw(DrawContext context) {
                RenderUtils.drawTexturedRect(context.getMatrices(), addNewButtonTexture, x, y, width, height, (int) width, (int) height);
            }
        };

        importButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                String json = MinecraftClient.getInstance().keyboard.getClipboard();
                try {
                    WaypointPackage imported = new Gson().fromJson(json, WaypointPackage.class);

                    if (imported == null) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid package.")));
                        return;
                    }

                    if(imported.name == null || imported.name.isEmpty()) {
                        imported.name = "unnamed package";
                    }

                    for (Waypoint waypoint : imported.waypoints) {
                        if (waypoint.categoryName != null) {
                            for (WaypointCategory cat : imported.categories) {
                                if (cat.name.equals(waypoint.categoryName)) {
                                    waypoint.setCategory(cat);
                                    break;
                                }
                            }
                        }
                    }

                    boolean exists = WaypointData.INSTANCE.packages.stream()
                            .anyMatch(pkg -> pkg.name.equals(imported.name));

                    if (exists) {
                        imported.name = WaypointData.INSTANCE.generateUniqueName(imported.name);
                    }

                    WaypointData.INSTANCE.packages.add(imported);
                    WaypointData.save();

                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Imported package \"" + imported.name + "\" with " +
                            imported.waypoints.size() + (imported.waypoints.size() == 1 ? " waypoint" : " waypoints") + " and " +
                            imported.categories.size() + (imported.categories.size() == 1 ? " category." : " categories."))));

                    WaypointData.INSTANCE.activePackage = null;
                    elements.clear();
                    categories.clear();
                    packageElements.clear();
                    for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
                        packageElements.add(new PackageElement(-1, -1, -1, -1, pkg));
                    }
                } catch (JsonSyntaxException e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid JSON format in clipboard.")));
                    e.printStackTrace();
                }
            }
        };

        backToPackagesButton = new EasyButton(-1, -1, -1, -1) {
            @Override
            public void click() {
                for(WaypointElement element : elements) {
                    element.waypoint.name = element.nameInput.getInput();
                    try {
                        int x = Integer.parseInt(element.xInput.getInput());
                        int y = Integer.parseInt(element.yInput.getInput());
                        int z = Integer.parseInt(element.zInput.getInput());

                        element.waypoint.x = x;
                        element.waypoint.y = y;
                        element.waypoint.z = z;
                    } catch (NumberFormatException e) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§cThe coordinates must be a number")));
                    }
                }
                WaypointData.INSTANCE.activePackage = null;
            }
        };

        packageElements.clear();
        for(WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            packageElements.add(new PackageElement(-1, -1, -1, -1, pkg));
        }

        categoryDropdown = new EasyDropdown(0, 0, -1, -1)  {
            @Override
            public void click() {
                if(!isExpanded) {
                    isExpanded = true;
                } else {
                    int ySection = Math.floorDiv(mouseY - 60 / scaleFactor, (39 / scaleFactor));
                    if(ySection < 1 || ySection > WaypointData.INSTANCE.activePackage.categories.size()) {
                        isExpanded = false;
                        return;
                    }

                    WaypointCategory category = WaypointData.INSTANCE.activePackage.categories.get(ySection - 1);
                    if(activeCategories.contains(category)) {
                        activeCategories.remove(category);
                    } else {
                        activeCategories.add(category);
                    }
                }
            }

            @Override
            public void draw(DrawContext context) {
                List<WaypointCategory> categoriez = WaypointData.INSTANCE.activePackage.categories;

                RenderUtils.drawTexturedRect(context.getMatrices(), nameBackgroundTexture, 0, 60f / scaleFactor, (float) (89 * 3) / scaleFactor, (float) (13 * 3) / scaleFactor, 89 * 3 / scaleFactor, 13 * 3 / scaleFactor);


                FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Category filter")), 8f / scaleFactor, 68f / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.75f / scaleFactor);


                if (isExpanded) {
                    if(categories.size() <= 1) {
                        RenderUtils.drawTexturedRect(context.getMatrices(), categorySingleTexture, x, y + height, width, 39f / scaleFactor, (int) width, 39 / scaleFactor);
                        if(categories.isEmpty()) {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Create a category!")), x + 6f / scaleFactor, y + height + 10f / scaleFactor, CustomColor.fromHexString("FF0000"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2.65f / scaleFactor);
                        } else {
                            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(WaypointData.INSTANCE.activePackage.categories.getFirst().name)), x + 6f / scaleFactor, y + height + 10f / scaleFactor, WaypointData.INSTANCE.activePackage.categories.getFirst().color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f / scaleFactor);
                        }
                        return;
                    }
                    for(int i = 0; i < categoriez.size(); i++) {
                        Identifier texture = categoryMidTexture;
                        if(i == 0) texture = categoryTopTexture;
                        if(i == categoriez.size() - 1) texture = categoryBotTexture;
                        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y + height + i * 39f / scaleFactor, width, 39f / scaleFactor, (int) width, 39 / scaleFactor);
                        CustomColor textColor = CustomColor.fromHexString("808080");
                        if(activeCategories.contains(categoriez.get(i))) {
                            textColor = WaypointData.INSTANCE.activePackage.categories.get(i).color;
                        }
                        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(WaypointData.INSTANCE.activePackage.categories.get(i).name)), x + 6f / scaleFactor, y + height + i * 39f / scaleFactor + 10f / scaleFactor, textColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 3f / scaleFactor);
                    }
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
                if(isExpanded && WaypointData.INSTANCE.activePackage.categories != null) {
                    if(y > this.y + height + WaypointData.INSTANCE.activePackage.categories.size() * (39f / scaleFactor)) return false;
                } else {
                    if(y > this.y + height) return false;
                }
                return true;
            }
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MinecraftClient.getInstance().getWindow() == null) return;
        if(MinecraftClient.getInstance().currentScreen == null) return;
        scaleFactor = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int width = 1800 / scaleFactor;
        int height = 750 / scaleFactor;
        int xStart = screenWidth / 2 - 300 / scaleFactor;
        WaypointScreen.mouseX = mouseX;
        WaypointScreen.mouseY = mouseY;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);

        int i = 0;
        float yOffset = (float) 30 / scaleFactor;

        if(WaypointData.INSTANCE.activePackage == null) {
            int max = Math.min(WaypointData.INSTANCE.packages.size(), packageElements.size());
            int j = 0;
            for (PackageElement element : packageElements) {
                if(j >= WaypointData.INSTANCE.packages.size()) return;
                int x = (int) (xStart + (float) width / 2 - 900f / scaleFactor);
                int y = (int) yOffset + (j * 300) / scaleFactor - scrollOffset;

                element.draw(x, y, 600 / scaleFactor, 275 / scaleFactor, context);
                j++;
            }

            importButton.setX(1);
            importButton.setY(screenHeight - 50 / scaleFactor - 5);
            importButton.setWidth(360f / scaleFactor);
            importButton.setHeight(60f / scaleFactor);
            importButton.drawWithTexture(context, importButtonTexture);
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Import from clipboard")), 180f / scaleFactor, screenHeight - 30f / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);


            addNewButton.setX(xStart + width / 2 - 750 / scaleFactor);
            addNewButton.setY((int) (yOffset + (j * 300f) / scaleFactor - scrollOffset));
            addNewButton.setHeight(60f / scaleFactor);
            addNewButton.setWidth(300f / scaleFactor);

            addNewButton.draw(context);
            FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Add new package")), xStart + (float) width / 2 - 600f / scaleFactor, (int) (yOffset + (j * 300f) / scaleFactor - scrollOffset + 30f / scaleFactor), CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);

            return;
        }

        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("I know that this menu is ugly,")), 5, 25, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f / scaleFactor);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("it will be reworked in the future")), 5, 35, CustomColor.fromHexString("ffffff"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 2f / scaleFactor);

        if(inMainScreen) {
            for (WaypointElement element : elements) {
                if(element.waypoint.getCategory() != null) {
                    if(!WaypointData.INSTANCE.activePackage.categories.contains(element.waypoint.getCategory())) {
                        element.waypoint.setCategory(null);
                    } else if (!activeCategories.contains(element.waypoint.getCategory())) continue;
                }
                element.draw(
                        xStart,
                        (int) yOffset + (i * 180) / scaleFactor - scrollOffset,
                        600 / scaleFactor,
                        150 / scaleFactor,
                        context
                );
                i++;
            }
        } else {
            for(CategoryElement category : categories) {
                if(GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
                    if(category.colorPicker.isClickInBounds(mouseX, mouseY)) {

                        category.colorPicker.click(mouseX, mouseY);
                    }
                }
                category.draw(
                        xStart,
                        (int) yOffset + (i * 305) / scaleFactor - scrollOffset,
                        600 / scaleFactor,
                        275 / scaleFactor,
                        context
                );
                i++;
            }
        }
        int x = (int) (xStart + (float) width / 2 - 750f / scaleFactor);
        int y;
        if(inMainScreen) {
            y = (int) yOffset + (i * 180) / scaleFactor - scrollOffset;
        } else {
            y = (int) yOffset + (i * 305) / scaleFactor - scrollOffset;
        }
        addNewButton.setX(x);
        addNewButton.setY(y);
        addNewButton.setHeight(60f / scaleFactor);
        addNewButton.setWidth(300f / scaleFactor);

        editCategoriesButton.setX(screenWidth - 300 / scaleFactor);
        editCategoriesButton.setY(0);
        editCategoriesButton.setHeight(60f / scaleFactor);
        editCategoriesButton.setWidth(300f / scaleFactor);

        //RenderUtils.drawTexturedRect(context.getMatrices(), addNewButtonTexture, 0, screenHeight - 53f / scaleFactor, 350f / scaleFactor, 50f / scaleFactor, 350 / scaleFactor, 50 / scaleFactor);

        backToPackagesButton.setX(0);
        backToPackagesButton.setY(0);
        backToPackagesButton.setWidth(360f / scaleFactor);
        backToPackagesButton.setHeight(60f / scaleFactor);
        backToPackagesButton.drawWithTexture(context, importButtonTexture);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of("Back to packages")), 180f / scaleFactor, 30f / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);

        String addButtonText;
        String editCategoriesText;

        if(inMainScreen) {
            addButtonText = "Add new Waypoint";
            editCategoriesText = "Edit Categories";

            categoryDropdown.setX(0);
            categoryDropdown.setY(60 / scaleFactor);
            categoryDropdown.setWidth((89f * 3) / scaleFactor);
            categoryDropdown.setHeight((12f * 3) / scaleFactor);
            categoryDropdown.draw(context);
        } else {
            addButtonText = "Add new Category";
            editCategoriesText = "Back to waypoints";
        }
        addNewButton.draw(context);
        editCategoriesButton.draw(context);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(addButtonText)), x + 150f / scaleFactor, y + 30f / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);
        FontRenderer.getInstance().renderText(context.getMatrices(), StyledText.fromComponent(Text.of(editCategoriesText)), editCategoriesButton.getX() + 150f / scaleFactor, editCategoriesButton.getY() + 30f / scaleFactor, CustomColor.fromHexString("ffffff"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 3f / scaleFactor);

        for(WaypointElement element : elements) {
            if(element.categoryDropdown.isExpanded) {
                element.categoryDropdown.draw(context);
            }
        }
    }

    @Override
    public void init() {
        WaypointData.INSTANCE.activePackage = null;
        super.init();
        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return;
            }
            lastScrollTime = now;

            if (verticalAmount > 0) {
                scrollOffset -= 30 / scaleFactor; //Scroll up
            } else {
                scrollOffset += 30 / scaleFactor; //Scroll down
            }
            if(scrollOffset < 0) {
                scrollOffset = 0;
            }
        });
    }

    @Override
    public void close() {
        mouseX = 0;
        mouseY = 0;
        scrollOffset = 0;

        {   int i = 0;
            for (WaypointPackage waypointPackage : WaypointData.INSTANCE.packages) {
                if(packageElements == null) return;
                if(packageElements.isEmpty()) return;
                if(packageElements.get(i) == null) return;

                String rawName = packageElements.get(i).nameInput.getInput();
                if(rawName == null) rawName = WaypointData.INSTANCE.generateUniqueName("Unnamed Package");
                waypointPackage.name = rawName.replaceAll("[\\\\/:*?\"<>|]", "");

                i++;
            }
        }
        for(WaypointElement element : elements) {
            element.waypoint.name = element.nameInput.getInput();
            try {
                int x = Integer.parseInt(element.xInput.getInput());
                int y = Integer.parseInt(element.yInput.getInput());
                int z = Integer.parseInt(element.zInput.getInput());

                element.waypoint.x = x;
                element.waypoint.y = y;
                element.waypoint.z = z;
            } catch (NumberFormatException e) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§cThe coordinates must be a number")));
            }
        }

        if(WaypointData.INSTANCE.activePackage != null) {
            for (int i = 0; i < Math.min(categories.size(), WaypointData.INSTANCE.activePackage.categories.size()); i++) {
                CategoryElement element = categories.get(i);
                WaypointData.INSTANCE.activePackage.categories.get(i).name = element.nameInput.getInput();
            }
        }

        for (CategoryElement element : categories) {
            if (element.category != null) {
                String oldName = element.category.name;
                String newName = element.nameInput.getInput();

                if (!oldName.equals(newName)) {
                    element.category.name = newName;

                    if(WaypointData.INSTANCE.activePackage != null) {
                        for (Waypoint waypoint : WaypointData.INSTANCE.activePackage.waypoints) {
                            if (waypoint.categoryName != null && waypoint.categoryName.equals(oldName)) {
                                waypoint.categoryName = newName;
                            }
                        }
                    }
                }
            }
        }

        if(WaypointData.INSTANCE.activePackage != null) {
            for (int i = 0; i < Math.min(categories.size(), WaypointData.INSTANCE.activePackage.categories.size()); i++) {
                CategoryElement element = categories.get(i);
                WaypointData.INSTANCE.activePackage.categories.get(i).name = element.nameInput.getInput();
                WaypointData.INSTANCE.activePackage.categories.get(i).color = element.colorPicker.getSelectedColor();
            }
        }
        WaypointData.INSTANCE.activePackage = null;
        categories.clear();
        activeCategories.clear();
        elements.clear();
        WaypointData.save();
        super.close();
    }

    public static void onClick() {
        if (scaleFactor == 0) return;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        List<WaypointElement> toRemoveWaypoint = new ArrayList<>();
        List<CategoryElement> toRemoveCategory = new ArrayList<>();

        if(WaypointData.INSTANCE.activePackage == null) {
            for (PackageElement element : new ArrayList<>(packageElements)) {
                element.click(mouseX, mouseY);
            }
        }

        if(inMainScreen) {
            for (WaypointElement element : elements) {
                if(element.waypoint.getCategory() != null) {
                    if (!activeCategories.contains(element.waypoint.getCategory())) continue;
                }
                if (element.deleteButton.isClickInBounds(mouseX, mouseY)) {
                    element.deleteButton.click();
                    toRemoveWaypoint.add(element);
                    WaypointData.INSTANCE.activePackage.waypoints.remove(element.waypoint);
                }
                element.click(mouseX, mouseY);
                if (clickWhileExpanded) {
                    clickWhileExpanded = false;
                    break;
                }
            }
            if(categoryDropdown.isClickInBounds(mouseX, mouseY)) {
                categoryDropdown.click();
            } else {
                categoryDropdown.isExpanded = false;
            }
        } else {
            for(CategoryElement element : categories) {
                if (element.deleteButton.isClickInBounds(mouseX, mouseY)) {
                    element.deleteButton.click();
                    toRemoveCategory.add(element);
                    WaypointData.INSTANCE.activePackage.categories.remove(element.category);
                }
                element.click(mouseX, mouseY);
            }
        }

        WaypointData.save();

        elements.removeAll(toRemoveWaypoint);
        categories.removeAll(toRemoveCategory);

        if(importButton.isClickInBounds(mouseX, mouseY)) {
            importButton.click();
        }

        if(addNewButton.isClickInBounds(mouseX, mouseY)) {
            addNewButton.click();
        }

        if(WaypointData.INSTANCE.activePackage == null) return;


        if(editCategoriesButton.isClickInBounds(mouseX, mouseY)) {
            editCategoriesButton.click();
        }


        if(backToPackagesButton.isClickInBounds(mouseX, mouseY)) {
            backToPackagesButton.click();
        }
    }
}