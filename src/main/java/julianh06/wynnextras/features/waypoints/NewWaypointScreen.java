package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.waypoints.old.Waypoint;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class NewWaypointScreen extends WEScreen {
    // ==================== THEME COLORS ====================
    private static int BG_DARK = 0xFF1a1410;
    private static int BG_MEDIUM = 0xFF2e251c;
    private static int BG_LIGHT = 0xFF4d3c2d;
    private static int PARCHMENT = 0xFF6c4f36;
    private static int PARCHMENT_LIGHT = 0xFF876141;
    private static int PARCHMENT_HOVER = 0xFF705030;
    private static int GOLD = 0xFFcca76f;
    private static int GOLD_DARK = 0xFFecc600;
    private static int TEXT_LIGHT = 0xFFe8dcc8;
    private static int TEXT_DIM = 0xFF9a8b70;
    private static int BORDER_DARK = 0xFF3a2d24;
    private static int BORDER_LIGHT = PARCHMENT_LIGHT;
    private static int TOGGLE_ON = 0xFF4a8c3a;
    private static int TOGGLE_OFF = 0xFF5c4535;
    private static int ACCENT_RED = 0xFFa83232;
    private static int SUBCATEGORY_BG = 0xFF694d33;

    // ==================== LAYOUT ====================
    private static final int SIDEBAR_WIDTH = 140;
    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 50;
    private static final int OPTION_HEIGHT = 45;
    private static final int OPTION_SPACING = 5;
    private static final int SUBCATEGORY_HEADER_HEIGHT = 25;

    private String searchQuery = "";
    private boolean searchFocused = false;
    private static final int SEARCH_BAR_HEIGHT = 28;

    private static SideBarWidget sideBarWidget;
    private MainWidget mainWidget;

    protected NewWaypointScreen() {
        super(Text.of("WynnExtras Waypoint Screen"));
        sideBarWidget = new SideBarWidget();
        addRootWidget(sideBarWidget);
        mainWidget = new MainWidget();
        MainWidget.activePackage = null;
        addRootWidget(mainWidget);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        //ctx.fill(0, 0, width, height, BG_DARK);

        // Disable hover when dropdown is open
//        int effectiveMouseX = activeDropdown != null ? -1 : mouseX;
//        int effectiveMouseY = activeDropdown != null ? -1 : mouseY;

        //drawSidebar(ctx, mouseX, mouseY);
        //drawMainPanel(ctx, mouseX, mouseY);
        //drawFooter(ctx, mouseX, mouseY);

        int sideBarWidth = 450;
        sideBarWidget.setBounds(0, 0, sideBarWidth, (int) (height * ui.getScaleFactorF()));
        mainWidget.setBounds(sideBarWidth, 0, (int) ((width * ui.getScaleFactor()) - sideBarWidth), (int) (height * ui.getScaleFactorF()));
    }

    private static void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    private void drawFooter(DrawContext ctx, int mouseX, int mouseY) {
        int footerY = height - FOOTER_HEIGHT + 5;

//        if (selectedCategory < 0 || selectedCategory >= categories.size()) return;
//        WynnExtrasConfigScreen.Category cat = categories.get(selectedCategory);

        int color = CustomColor.fromHexString("ecc600").asInt();
        ctx.fill(SIDEBAR_WIDTH + 10, footerY, width - 10, footerY + 1, color);

        int btnY = height - 35;
        int saveX = width - 115;
        int cancelX = width - 225;

        boolean saveHover = mouseX >= saveX && mouseX < saveX + 100 && mouseY >= btnY && mouseY < btnY + 24;
        boolean cancelHover = mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= btnY && mouseY < btnY + 24;

        drawButton(ctx, saveX, btnY, 100, 24, "Save & Close", saveHover, TOGGLE_ON);
        drawButton(ctx, cancelX, btnY, 100, 24, "Cancel", cancelHover, ACCENT_RED);
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String text, boolean hover, int accent) {
        ctx.fill(x, y, x + w, y + h, hover ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, hover ? GOLD : BORDER_LIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER_DARK);
        ctx.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, accent);
        ctx.drawCenteredTextWithShadow(textRenderer, text, x + w / 2, y + 8, TEXT_LIGHT);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        sideBarWidget.mouseDragged(click.x(), click.y(),  click.button(), dx, dy);
        return super.mouseDragged(click, dx, dy);
    }

    private static class SideBarWidget extends Widget {
        public List<PackageWidget> packageWidgets = new ArrayList<>();
        boolean initialized = false;
        static int draggedIndex = -1;
        static int packageOverMouseIndex = -1;
        static int packageUnderMouseIndex = -1;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!initialized) {
                int i = 0;
                for(WaypointPackage waypointPackage : WaypointData.INSTANCE.packages) {
                    packageWidgets.add(new PackageWidget(waypointPackage, i, this));
                    i++;
                }
                initialized = true;
            }

            ui.drawRect(0, 0, width, height, CustomColor.fromInt(BG_MEDIUM));
            ui.drawRect(width - 5, 0, 5, height, CustomColor.fromInt(BORDER_DARK));

            ui.drawCenteredText("Packages", x + width / 2f, y + 70, CustomColor.fromInt(GOLD));
            ui.drawRect(50, 100, width - 100, 4, CustomColor.fromInt(GOLD_DARK));

            int packageX = 50;
            int packageY = 120;
            int packageHeight = 50;
            int packageWidth = width - 100;
            int spacing = 20;

            packageOverMouseIndex = -1;
            packageUnderMouseIndex = -1;

            float mouseYScaled = mouseY * ui.getScaleFactorF();

            if (draggedIndex > -1) {
                List<Integer> centers = new ArrayList<>(packageWidgets.size());
                int tempY = packageY;
                for (int j = 0; j < packageWidgets.size(); j++) {
                    int centerY = tempY + packageHeight / 2;
                    centers.add(centerY);
                    tempY += packageHeight + spacing;
                }

                int insertionIndex = 0;
                for (int c : centers) {
                    if (mouseYScaled > c) insertionIndex++;
                    else break;
                }

                if (insertionIndex < 0) insertionIndex = 0;
                if (insertionIndex > packageWidgets.size()) insertionIndex = packageWidgets.size();

                int targetIndex;
                if (insertionIndex > draggedIndex) {
                    targetIndex = insertionIndex - 1;
                } else {
                    targetIndex = insertionIndex;
                }

                if (targetIndex == draggedIndex) {
                    packageOverMouseIndex = -1;
                    packageUnderMouseIndex = -1;
                } else {
                    packageOverMouseIndex = insertionIndex - 1;
                    packageUnderMouseIndex = insertionIndex;

                    // Clamp (sicher)
                    if (packageOverMouseIndex < -1) packageOverMouseIndex = -1;
                    if (packageUnderMouseIndex < 0) packageUnderMouseIndex = 0;
                    if (packageUnderMouseIndex > packageWidgets.size()) packageUnderMouseIndex = packageWidgets.size();
                }
            } else {
                packageOverMouseIndex = -1;
                packageUnderMouseIndex = -1;
            }

            int drawY = packageY;
            for (PackageWidget packageWidget : packageWidgets) {
                packageWidget.setBounds(packageX, drawY, packageWidth, packageHeight);
                packageWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                drawY += packageHeight + spacing;
            }

            if (draggedIndex > -1 && packageOverMouseIndex != -1 && packageUnderMouseIndex != -1) {
                int insertionIndexForLine = packageUnderMouseIndex;
                int lineY;
                if (insertionIndexForLine == 0) {
                    lineY = packageY - spacing / 2;
                } else if (insertionIndexForLine >= packageWidgets.size()) {
                    lineY = packageY + insertionIndexForLine * (packageHeight + spacing) - spacing / 2;
                } else {
                    int cAbove = packageY + (insertionIndexForLine - 1) * (packageHeight + spacing) + packageHeight / 2;
                    int cBelow = packageY + insertionIndexForLine * (packageHeight + spacing) + packageHeight / 2;
                    lineY = cAbove + (cBelow - cAbove) / 2;
                }

                int lineX1 = packageX;
                ui.drawRect(lineX1, lineY - 1, packageWidth, 2, CustomColor.fromInt(GOLD));
            }
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            for(PackageWidget packageWidget : packageWidgets) {
                if(packageWidget.isHovered() && packageWidget.clicked) {
                    return packageWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                }
            }
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            for(PackageWidget packageWidget : packageWidgets) {
                if(packageWidget.isHovered()) {
                    return packageWidget.mouseClicked(mx, my, button);
                }
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            for (PackageWidget packageWidget : new ArrayList<>(packageWidgets)) {
                packageWidget.mouseReleased(mx, my, button);
            }
            draggedIndex = -1;
            return super.mouseReleased(mx, my, button);
        }

        private void rebuildPackageWidgetsFromData() {
            packageWidgets.clear();
            draggedIndex = -1;
            int i = 0;
            for (WaypointPackage pkg : WaypointData.INSTANCE.packages) {
                packageWidgets.add(new PackageWidget(pkg, i, this));
                i++;
            }
        }

        private static class PackageWidget extends Widget {
            final WaypointPackage waypointPackage;
            final int index;
            final SideBarWidget parent;
            boolean isDragging = false;
            public boolean clicked = false;
            private float clickX = -1;
            private float clickY = -1;

            public PackageWidget(WaypointPackage waypointPackage, int index, SideBarWidget parent) {
                this.waypointPackage = waypointPackage;
                this.index = index;
                this.parent = parent;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                CustomColor color = hovered ? CustomColor.fromHexString("a0a0a0") : CustomColor.fromHexString("808080");
                ui.drawButton(x, y, width, height, 13, hovered);
                ui.drawCenteredText(waypointPackage.name, x + width / 2f, y + height / 2f);

                if(isDragging) {
                    ui.drawButton(mouseX * ui.getScaleFactorF(), mouseY * ui.getScaleFactorF(), width, height, 13, true);
                    ui.drawCenteredText(waypointPackage.name, mouseX * ui.getScaleFactorF() + width / 2f, mouseY * ui.getScaleFactorF() + height / 2f);
                }
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                if(Math.abs(mouseX - clickX) > 2 || Math.abs(mouseY - clickY) > 2)
                isDragging = true;
                draggedIndex = index;
                return true;
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                clicked = true;
                clickX = (float) mx;
                clickY = (float) my;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                if(clicked && !isDragging) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    if(MainWidget.activePackage == waypointPackage) {
                        MainWidget.activePackage = null;
                    } else MainWidget.activePackage = waypointPackage;
                    MainWidget.waypointWidgets.clear();
                }

                if (isDragging && draggedIndex >= 0 && packageUnderMouseIndex >= 0) {
                    int insertionIndex = packageUnderMouseIndex;
                    if (insertionIndex > WaypointData.INSTANCE.packages.size()) insertionIndex = WaypointData.INSTANCE.packages.size();

                    int targetIndex;
                    if (insertionIndex > draggedIndex) {
                        targetIndex = insertionIndex - 1;
                    } else {
                        targetIndex = insertionIndex;
                    }

                    if (targetIndex != draggedIndex) {
                        WaypointPackage moved = WaypointData.INSTANCE.packages.remove(draggedIndex);
                        WaypointData.INSTANCE.packages.add(targetIndex, moved);

                        OrderManager.saveOrder(WaypointData.INSTANCE.packages);
                        WaypointData.save();

                        parent.rebuildPackageWidgetsFromData();
                    }
                }

                clicked = false;
                isDragging = false;
                return false;
            }
        }
    }

    private static class MainWidget extends Widget {
        private enum Tab { Waypoints, Categories, Settings }

        public static WaypointPackage activePackage = null;

        private static Tab activeTab = Tab.Waypoints;
        private static List<TabWidget> tabWidgets = new ArrayList<>();
        private static List<WaypointWidget> waypointWidgets = new ArrayList<>();

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
            this.ui = ui;
            if(!visible || this.ui == null) return;
            // update hover state for this widget
            hovered = contains(mouseX, mouseY);
            updateValues();
            drawBackground(ctx, mouseX, mouseY, tickDelta);
            drawContent(ctx, mouseX, mouseY, tickDelta);
            drawForeground(ctx, mouseX, mouseY, tickDelta);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(tabWidgets.isEmpty()) {
                for(Tab tab : Tab.values()) {
                    TabWidget tabWidget = new TabWidget(tab);
                    tabWidgets.add(tabWidget);
                    addChild(tabWidget);
                }
            }

            ui.drawRect(x + 30, y + 30, width - 60, 120, CustomColor.fromInt(PARCHMENT).withAlpha(1f));
            ui.drawRect(x + 30, y + 30, width - 60, 5, CustomColor.fromInt(GOLD_DARK).withAlpha(1f));
            ui.drawRect(x + 50, y + 145, width - 100, 5, CustomColor.fromInt(GOLD_DARK).withAlpha(1f));

            drawDiamond(ctx, (int) ((x + 50) / ui.getScaleFactor()), (int) ((4 + HEADER_HEIGHT / 2f) * 3 / ui.getScaleFactor()), 9 / (int) ui.getScaleFactor(), GOLD_DARK);
            drawDiamond(ctx, (int) ((x + width - 50) / ui.getScaleFactor()), (int) ((4 + HEADER_HEIGHT / 2f) * 3 / ui.getScaleFactor()), 9 / (int) ui.getScaleFactor(), GOLD_DARK);

            ui.drawCenteredText("WynnExtras", x + width / 2f, y + 70, CustomColor.fromInt(TEXT_LIGHT));
            ui.drawCenteredText("Waypoints", x + width / 2f, y + 110, CustomColor.fromInt(TEXT_DIM));

            if(activePackage == null) {
                for(WaypointWidget waypointWidget : waypointWidgets) {
                    removeChild(waypointWidget);
                }
                waypointWidgets.clear();
                return;
            }

            if(waypointWidgets.isEmpty()) {
                for(Waypoint waypoint : activePackage.waypoints) {
                    WaypointWidget waypointWidget = new WaypointWidget(waypoint);
                    waypointWidgets.add(waypointWidget);
                    addChild(waypointWidget);
                }
            }

            boolean hasNoDescription = activePackage.description == null || activePackage.description.isEmpty();
            int bgHeight = hasNoDescription ? 80 : 120;
            ui.drawRect(x + 30, y + 150, width - 60, bgHeight, CustomColor.fromInt(PARCHMENT).withAlpha(1f));
            ui.drawText(activePackage.name, x + 40, y + 190);
            ui.drawText(activePackage.description, x + 40, y + 230, CustomColor.fromInt(TEXT_DIM));

            int tabWidth = 250;
            int spacing = 20;
            float xStart = x + width / 2f - 1.5f * tabWidth - spacing;
            for(TabWidget tabWidget : tabWidgets) {
                tabWidget.setBounds((int) xStart, y + (hasNoDescription ? 250 : 290), tabWidth, 50);
                tabWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                xStart += tabWidth + spacing;
            }

            int waypointY = y + (hasNoDescription ? 320 : 360);

            for(WaypointWidget waypointWidget : waypointWidgets) {
                waypointWidget.setBounds(x + 30, waypointY, width - 60, 50);
                waypointWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                waypointY += 70;
            }
        }

        private static class TabWidget extends Widget {
            final Tab tab;

            public TabWidget(Tab tab) {
                this.tab = tab;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 13, hovered, false);

                CustomColor color = CustomColor.fromHexString("FFFFFF");
                if(tab == activeTab) color = CustomColor.fromHexString("FFFF00");

                ui.drawCenteredText(tab.name(), x + width / 2f, y + height / 2f, color);
            }

            @Override
            protected boolean onClick(int button) {
                activeTab = tab;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        private static class WaypointWidget extends Widget {
            final Waypoint waypoint;

            public WaypointWidget(Waypoint waypoint) {
                this.waypoint = waypoint;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 13, hovered);

                if(waypoint != null) {
                    ui.drawCenteredText(waypoint.name + " x: " + waypoint.x + " y: " + waypoint.y + " z: " + waypoint.z, x + width / 2f, y + height / 2f);
                }
            }
        }
    }
}