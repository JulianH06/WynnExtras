package julianh06.wynnextras.features.waypoints;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.waypoints.old.Waypoint;
import julianh06.wynnextras.features.waypoints.old.WaypointCategory;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.*;

public class NewWaypointScreen extends WEScreen {
    @Override protected double getTargetScaleFactor() { return 2.5; }
    @Override protected int getMinLogicalWidth() { return 1900; }
    @Override protected int getMinLogicalHeight() { return 870; }

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

    private enum ScrollType { Packages, Waypoints, Categories }

    static Map<ScrollType, Float> targetOffsets = new HashMap<>();
    static Map<ScrollType, Float> actualOffsets = new HashMap<>();

    protected NewWaypointScreen() {
        super(Text.of("WynnExtras Waypoint Screen"));
        sideBarWidget = new SideBarWidget();
        addRootWidget(sideBarWidget);
        MainWidget.resetState();
        mainWidget = new MainWidget();
        addRootWidget(mainWidget);
    }

    @Override
    protected void init() {
        super.init();

        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount,
                consumed
        ) -> {
            double mx = mX / matrixScale;
            double my = mY / matrixScale;
            if (sideBarWidget.mouseScrolled(mx, my, verticalAmount)) return true;
            return mainWidget.mouseScrolled(mX, mY, verticalAmount);
        });
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
        int logicalWidth = getLogicalWidth();
        int logicalHeight = getLogicalHeight();
        sideBarWidget.setBounds(0, 0, sideBarWidth, logicalHeight);
        mainWidget.setBounds(sideBarWidth, 0, logicalWidth - sideBarWidth, logicalHeight);
    }

    private static void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    private static void drawConfigRow(UIUtils ui, float x, float y, float width, float height, boolean hovered, boolean selected, int accentColor) {
        CustomColor bg = CustomColor.fromInt(selected ? PARCHMENT : (hovered ? BG_LIGHT : BG_MEDIUM));
        ui.drawRect(x, y, width, height, bg);
        if (selected) {
            ui.drawRect(x, y, 4, height, CustomColor.fromInt(accentColor));
        } else if (hovered) {
            ui.drawRect(x, y, 4, height, CustomColor.fromInt(BG_LIGHT));
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
        sideBarWidget.mouseDragged(click.x() / matrixScale, click.y() / matrixScale, click.button(), dx / matrixScale, dy / matrixScale);
        return super.mouseDragged(click, dx, dy);
    }

    private static class SideBarWidget extends Widget {
        private static final int LIST_START_Y = 120;
        private static final int PACKAGE_HEIGHT = 44;
        private static final int PACKAGE_SPACING = 8;
        private static final int PACKAGE_X_PADDING = 24;
        private static final int ADD_SECTION_HEIGHT = 118;
        private static final int ADD_BUTTON_HEIGHT = 46;

        public List<PackageWidget> packageWidgets = new ArrayList<>();
        boolean initialized = false;
        static int draggedIndex = -1;
        static int packageOverMouseIndex = -1;
        static int packageUnderMouseIndex = -1;
        private float packageScrollOffset = 0;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!initialized) {
                rebuildPackageWidgetsFromData();
                initialized = true;
            }

            ui.drawRect(0, 0, width, height, CustomColor.fromInt(BG_MEDIUM));
            ui.drawRect(width - 5, 0, 5, height, CustomColor.fromInt(BORDER_DARK));

            ui.drawCenteredText("Packages", x + width / 2f, y + 70, CustomColor.fromInt(GOLD));
            ui.drawRect(50, 100, width - 100, 4, CustomColor.fromInt(GOLD_DARK));

            int packageX = PACKAGE_X_PADDING;
            int packageY = LIST_START_Y;
            int packageHeight = PACKAGE_HEIGHT;
            int packageWidth = width - PACKAGE_X_PADDING * 2 - 5;
            int spacing = PACKAGE_SPACING;
            int addSeparatorY = height - ADD_SECTION_HEIGHT;
            int listEndY = addSeparatorY - 16;
            int listHeight = Math.max(0, listEndY - packageY);
            float maxScroll = Math.max(0, packageWidgets.size() * (packageHeight + spacing) - spacing - listHeight);
            packageScrollOffset = Math.max(0, Math.min(packageScrollOffset, maxScroll));

            packageOverMouseIndex = -1;
            packageUnderMouseIndex = -1;

            float mouseYScaled = mouseY * ui.getScaleFactorF() + packageScrollOffset;

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

            try {
                ctx.enableScissor(
                        (int) ui.sx(0),
                        (int) ui.sy(packageY),
                        (int) ui.sx(width - 5),
                        (int) ui.sy(listEndY)
                );
            } catch (Exception ignored) {}

            int drawY = (int) (packageY - packageScrollOffset);
            for (PackageWidget packageWidget : packageWidgets) {
                packageWidget.setBounds(packageX, drawY, packageWidth, packageHeight);
                if (drawY + packageHeight >= packageY && drawY <= listEndY) {
                    packageWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                }
                drawY += packageHeight + spacing;
            }

            if (draggedIndex > -1 && packageOverMouseIndex != -1 && packageUnderMouseIndex != -1) {
                int insertionIndexForLine = packageUnderMouseIndex;
                int lineY;
                if (insertionIndexForLine == 0) {
                    lineY = (int) (packageY - packageScrollOffset - spacing / 2);
                } else if (insertionIndexForLine >= packageWidgets.size()) {
                    lineY = (int) (packageY - packageScrollOffset + insertionIndexForLine * (packageHeight + spacing) - spacing / 2);
                } else {
                    int cAbove = (int) (packageY - packageScrollOffset + (insertionIndexForLine - 1) * (packageHeight + spacing) + packageHeight / 2);
                    int cBelow = (int) (packageY - packageScrollOffset + insertionIndexForLine * (packageHeight + spacing) + packageHeight / 2);
                    lineY = cAbove + (cBelow - cAbove) / 2;
                }

                int lineX1 = packageX;
                if (lineY >= packageY && lineY <= listEndY) ui.drawRect(lineX1, lineY - 1, packageWidth, 2, CustomColor.fromInt(GOLD));
            }

            try {
                ctx.disableScissor();
            } catch (Exception ignored) {}

            if (maxScroll > 0) {
                int sbX = width - 14;
                int thumbH = Math.max(28, (int) (listHeight * listHeight / (listHeight + maxScroll)));
                int thumbY = packageY + (int) ((listHeight - thumbH) * (packageScrollOffset / maxScroll));
                ui.drawRect(sbX, packageY, 5, listHeight, CustomColor.fromInt(BORDER_DARK));
                ui.drawRect(sbX + 1, thumbY, 3, thumbH, CustomColor.fromInt(GOLD_DARK));
            }

            ui.drawRect(50, addSeparatorY, width - 100, 4, CustomColor.fromInt(GOLD_DARK));
            drawConfigRow(ui, PACKAGE_X_PADDING, height - 72, packageWidth, ADD_BUTTON_HEIGHT, isAddPackageHovered(mouseX, mouseY), false, GOLD_DARK);
            ui.drawCenteredText("Add Package", PACKAGE_X_PADDING + packageWidth / 2f, height - 72 + ADD_BUTTON_HEIGHT / 2f, CustomColor.fromHexString("FFFFFF"), 2.6f);
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
            if (isAddPackageHovered(mx, my)) {
                addPackage();
                return true;
            }
            for(PackageWidget packageWidget : packageWidgets) {
                if(packageWidget.contains((int) mx, (int) my)) {
                    return packageWidget.mouseClicked(mx, my, button);
                }
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (!contains((int) mx, (int) my)) return false;
            packageScrollOffset += delta > 0 ? -55 : 55;
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            for (PackageWidget packageWidget : new ArrayList<>(packageWidgets)) {
                packageWidget.mouseReleased(mx, my, button);
            }

            draggedIndex = -1;
            return false;
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

        private boolean isAddPackageHovered(double mx, double my) {
            int packageWidth = width - PACKAGE_X_PADDING * 2 - 5;
            return mx >= ui.sx(PACKAGE_X_PADDING)
                    && my >= ui.sy(height - 72)
                    && mx < ui.sx(PACKAGE_X_PADDING) + ui.sw(packageWidth)
                    && my < ui.sy(height - 72 + ADD_BUTTON_HEIGHT);
        }

        private void addPackage() {
            WaypointPackage waypointPackage = new WaypointPackage(WaypointData.INSTANCE.generateUniqueName("New package"));
            WaypointData.ensureUncategorizedCategory(waypointPackage);
            WaypointData.INSTANCE.packages.add(waypointPackage);
            OrderManager.saveOrder(WaypointData.INSTANCE.packages);
            WaypointData.save();
            rebuildPackageWidgetsFromData();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                boolean selected = MainWidget.activePackage == waypointPackage;
                drawConfigRow(ui, x, y, width, height, hovered, selected, GOLD_DARK);
                drawDiamond(ctx, (int) ui.sx(x + 18), (int) ui.sy(y + height / 2f), Math.max(2, ui.sw(4)), selected ? GOLD_DARK : BORDER_DARK);
                ui.drawText(waypointPackage.name, x + 36, y + height / 2f, selected ? CustomColor.fromInt(TEXT_LIGHT) : CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);

                if(isDragging) {
                    float dragX = mouseX * ui.getScaleFactorF();
                    float dragY = mouseY * ui.getScaleFactorF();
                    drawConfigRow(ui, dragX, dragY, width, height, true, true, GOLD_DARK);
                    drawDiamond(ctx, (int) ui.sx(dragX + 18), (int) ui.sy(dragY + height / 2f), Math.max(2, ui.sw(4)), GOLD_DARK);
                    ui.drawText(waypointPackage.name, dragX + 36, dragY + height / 2f, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);
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
                    } else {
                        MainWidget.activePackage = waypointPackage;
                        targetOffsets.put(ScrollType.Waypoints, 0f);
                        actualOffsets.put(ScrollType.Waypoints, 0f);
                    }
                    MainWidget.WaypointsTabContent.categoryWidgets.clear();
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

        private static MainAreaWidget mainAreaWidget;

        private static TabContentWidget activeTabWidget;

        private static WaypointsTabContent waypointsTab;
        private static CategoriesTabContent categoriesTab;
        private static SettingsTabContent settingsTab;

        private static void resetState() {
            activePackage = null;
            activeTab = Tab.Waypoints;
            tabWidgets.clear();
            mainAreaWidget = null;
            activeTabWidget = null;
            waypointsTab = null;
            categoriesTab = null;
            settingsTab = null;
        }

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
            this.ui = ui;
            if(!visible || this.ui == null) return;

            if(mainAreaWidget != null) mainAreaWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            // update hover state for this widget
            hovered = contains(mouseX, mouseY);
            updateValues();
            drawBackground(ctx, mouseX, mouseY, tickDelta);
            drawContent(ctx, mouseX, mouseY, tickDelta);
            drawForeground(ctx, mouseX, mouseY, tickDelta);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (waypointsTab == null) {
                waypointsTab = new WaypointsTabContent();
                categoriesTab = new CategoriesTabContent();
                settingsTab = new SettingsTabContent();

                addChild(waypointsTab);
                addChild(categoriesTab);
                addChild(settingsTab);
                categoriesTab.setVisible(false);
                settingsTab.setVisible(false);
                activeTabWidget = waypointsTab;
            }

            if(activeTab == null) {
                activeTab = Tab.Waypoints;
                TabWidget.updateActiveTab();
            }

            if(tabWidgets.isEmpty()) {
                for(Tab tab : Tab.values()) {
                    TabWidget tabWidget = new TabWidget(tab);
                    tabWidgets.add(tabWidget);
                    addChild(tabWidget);
                }
            }

            if(mainAreaWidget == null) {
                mainAreaWidget = new MainAreaWidget();
                addChild(mainAreaWidget);
            }

            ui.drawRect(x + 30, y + 30, width - 60, 120, CustomColor.fromInt(PARCHMENT).withAlpha(1f));
            ui.drawRect(x + 30, y + 30, width - 60, 5, CustomColor.fromInt(GOLD_DARK).withAlpha(1f));
            ui.drawRect(x + 50, y + 145, width - 100, 5, CustomColor.fromInt(GOLD_DARK).withAlpha(1f));

            drawDiamond(ctx, (int) ui.sx(x + 50), (int) ui.sy(y + 80), Math.max(2, ui.sw(9)), GOLD_DARK);
            drawDiamond(ctx, (int) ui.sx(x + width - 50), (int) ui.sy(y + 80), Math.max(2, ui.sw(9)), GOLD_DARK);

            ui.drawCenteredText("WynnExtras", x + width / 2f, y + 70, CustomColor.fromInt(TEXT_LIGHT));
            ui.drawCenteredText("Waypoints", x + width / 2f, y + 110, CustomColor.fromInt(TEXT_DIM));

            if(activePackage == null) return;
            WaypointData.resolveWaypointCategories(activePackage);

            boolean hasNoDescription = activePackage.description == null || activePackage.description.isEmpty();
            int topOffset = hasNoDescription ? 320 : 360;

            int top = this.y + topOffset;
            int bottom = this.y + this.height;

            mainAreaWidget.setBounds(
                    this.x,
                    top,
                    this.width,
                    bottom - top
            );

            int tabWidth = 250;
            int spacing = 20;
            float xStart = x + width / 2f - 1.5f * tabWidth - spacing;
            for(TabWidget tabWidget : tabWidgets) {
                tabWidget.setBounds((int) xStart, y + (hasNoDescription ? 250 : 290), tabWidth, 50);
                tabWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                xStart += tabWidth + spacing;
            }

            int bgHeight = hasNoDescription ? 80 : 120;
            ui.drawRect(x + 30, y + 150, width - 60, bgHeight, CustomColor.fromInt(PARCHMENT));
            ui.drawText(activePackage.name, x + 40, y + 190);
            ui.drawText(activePackage.description, x + 40, y + 230, CustomColor.fromInt(TEXT_DIM));

            if(activeTabWidget == null) return;

            activeTabWidget.setBounds(x, y + topOffset, width, height - topOffset);
            activeTabWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (activeTab != Tab.Waypoints) return false;
            if(delta > 0) targetOffsets.put(ScrollType.Waypoints, targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) - 33f);
            else targetOffsets.put(ScrollType.Waypoints, targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) + 33f);
            if(targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) < 0) targetOffsets.put(ScrollType.Waypoints, 0f);
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled) return false;
            // propagate to children in reverse order (topmost first)
            for (int i = children.size() - 1; i >= 0; i--) {
                Widget child = children.get(i);
                if (child.mouseClicked(mx, my, button)) return true;
            }
            // if this widget contains the click, handle it
            if (contains((int) mx, (int) my)) {
                // manage focus
                setFocused(true);
                if (onClickCallback != null) onClickCallback.accept(this);
                return onClick(mx, my, button);
            } else {
                // clicking outside removes focus
                if (focused) setFocused(false);
            }
            return false;
        }

        protected boolean onClick(double mx, double my, int button) {
            if(activeTabWidget == null) return false;
            return activeTabWidget.mouseClicked(mx, my, button);
        }

        private static class MainAreaWidget extends Widget {
            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            }
        }

        private static class TabWidget extends Widget {
            final Tab tab;

            public TabWidget(Tab tab) {
                this.tab = tab;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                drawConfigRow(ui, x, y, width, height, hovered, tab == activeTab, GOLD_DARK);

                CustomColor color = tab == activeTab ? CustomColor.fromInt(TEXT_LIGHT) : CustomColor.fromInt(TEXT_DIM);

                ui.drawCenteredText(tab.name(), x + width / 2f, y + height / 2f, color);
            }

            @Override
            protected boolean onClick(int button) {
                activeTab = tab;
                updateActiveTab();
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }

            public static void updateActiveTab() {
                waypointsTab.setVisible(false);
                categoriesTab.setVisible(false);
                settingsTab.setVisible(false);

                switch (activeTab) {
                    case Waypoints -> {
                        activeTabWidget = waypointsTab;
                    }
                    case Categories -> {
                        activeTabWidget = categoriesTab;
                    }
                    case Settings -> {
                        activeTabWidget = settingsTab;
                    }
                }

                if(activeTabWidget != null) activeTabWidget.setVisible(true);
            }
        }

        private static abstract class TabContentWidget extends Widget {
            public abstract float calculateTotalHeight();
        }

        private static class ActionButtonWidget extends Widget {
            private final String text;
            private final Runnable action;

            private ActionButtonWidget(String text, Runnable action) {
                this.text = text;
                this.action = action;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                drawConfigRow(ui, x, y, width, height, hovered, false, GOLD_DARK);
                ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromInt(TEXT_LIGHT), 2.6f);
            }

            @Override
            protected boolean onClick(int button) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                action.run();
                return true;
            }
        }

        private static class WaypointsTabContent extends TabContentWidget {
            public static List<CategoryWidget> categoryWidgets = new ArrayList<>();
            private WaypointScrollBarWidget scrollBar;
            private ActionButtonWidget addWaypointButton;

            private void rebuildCategoryWidgets() {
                categoryWidgets.clear();
                clearChildren();

                Map<WaypointCategory, List<Waypoint>> grouped = new LinkedHashMap<>();
                WaypointData.resolveWaypointCategories(activePackage);
                activePackage.waypoints.stream()
                        .sorted(Comparator.comparing(
                                w -> w.getCategory().name == null ? "" : w.getCategory().name,
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .forEach(w -> grouped.computeIfAbsent(w.getCategory(), ignored -> new ArrayList<>()).add(w));

                for (Map.Entry<WaypointCategory, List<Waypoint>> entry : grouped.entrySet()) {
                    CategoryWidget catWidget = new CategoryWidget(entry.getKey());

                    entry.getValue().stream()
                            .sorted(Comparator.comparing(w -> w.name, String.CASE_INSENSITIVE_ORDER))
                            .forEach(w -> catWidget.addWaypoint(new WaypointWidget(w)));

                    categoryWidgets.add(catWidget);
                    addChild(catWidget);
                }
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if (scrollBar == null) {
                    scrollBar = new WaypointScrollBarWidget();
                    addChild(scrollBar);
                }
                if (addWaypointButton == null) {
                    addWaypointButton = new ActionButtonWidget("Add Waypoint", this::addWaypoint);
                    addChild(addWaypointButton);
                }

                if (activePackage == null) {
                    categoryWidgets.clear();
                    clearChildren();
                    addChild(scrollBar);
                    addChild(addWaypointButton);
                    return;
                }

                if (categoryWidgets.isEmpty()) {
                    rebuildCategoryWidgets();
                    addChild(scrollBar);
                    addChild(addWaypointButton);
                }

                try {
                    ctx.enableScissor(
                            (int) ui.sx(x),
                            (int) ui.sy(mainAreaWidget.getY()),
                            (int) ui.sx(x + width),
                            (int) ui.sy(mainAreaWidget.getY() + mainAreaWidget.getHeight())
                    );
                } catch (Exception ignored) {}


                float maxOffset = 0;
                float viewportHeight = 0;

                if (activeTabWidget != null && mainAreaWidget != null) {
                    float contentHeight = activeTabWidget.calculateTotalHeight();
                    viewportHeight = mainAreaWidget.getHeight();
                    maxOffset = Math.max(0, contentHeight - viewportHeight);
                }

                if(mainAreaWidget != null) {
                    scrollBar.setBounds(
                            x + width - 27,
                            (int) mainAreaWidget.getY(),
                            20,
                            (int) viewportHeight
                    );
                }
                addWaypointButton.setBounds(x + 30, y + 10, 260, 44);

                if (maxOffset > 0) {
                    scrollBar.setVisible(true);
                } else {
                    scrollBar.setVisible(false);
                }

                float snapValue = 0.5f;
                if (targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) > maxOffset) {
                    targetOffsets.put(ScrollType.Waypoints, maxOffset);
                    snapValue = 0.75f;
                }
                if (targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) <= 0) {
                    targetOffsets.put(ScrollType.Waypoints, 0f);
                    snapValue = 0.75f;
                }

                float speed = 0.3f;
                float diff = (targetOffsets.getOrDefault(ScrollType.Waypoints, 0f) - actualOffsets.getOrDefault(ScrollType.Waypoints, 0f));
                if (Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffsets.put(ScrollType.Waypoints, targetOffsets.getOrDefault(ScrollType.Waypoints, 0f));
                else actualOffsets.put(ScrollType.Waypoints, actualOffsets.getOrDefault(ScrollType.Waypoints, 0f) + diff * speed * tickDelta);

                float currentY = y + 65 - actualOffsets.getOrDefault(ScrollType.Waypoints, 0f);

                for (CategoryWidget category : categoryWidgets) {
                    category.setBounds(x + 30, (int) currentY, width - 70, 50);
                    category.draw(ctx, mouseX, mouseY, tickDelta, ui);

                    currentY += category.getTotalHeight() + 10;
                }
            }

            @Override
            protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                try {
                    ctx.disableScissor();
                } catch (Exception ignored) {}
            }

            @Override
            public float calculateTotalHeight() {
                if(categoryWidgets.isEmpty()) return 65;

                float result = 65;

                for (int i = 0; i < categoryWidgets.size(); i++) {
                    result += categoryWidgets.get(i).getTotalHeight() + 10;
                }

                return result;
            }

            private void addWaypoint() {
                if (activePackage == null) return;

                MinecraftClient client = MinecraftClient.getInstance();
                int x = 0;
                int y = 0;
                int z = 0;
                if (client.player != null) {
                    x = (int) Math.floor(client.player.getX());
                    y = (int) Math.floor(client.player.getY()) - 1;
                    z = (int) Math.floor(client.player.getZ());
                }

                Waypoint waypoint = new Waypoint(x, y, z);
                waypoint.id = UUID.randomUUID().toString();
                waypoint.setCategory(WaypointData.ensureUncategorizedCategory(activePackage));
                activePackage.waypoints.add(waypoint);
                WaypointData.save();

                categoryWidgets.clear();
                clearChildren();
                addChild(scrollBar);
                addChild(addWaypointButton);
                targetOffsets.put(ScrollType.Waypoints, 0f);
                actualOffsets.put(ScrollType.Waypoints, 0f);
            }

            private static class CategoryWidget extends Widget {
                private final WaypointCategory category;
                private final List<WaypointWidget> waypoints = new ArrayList<>();
                private boolean collapsed = false;

                public CategoryWidget(WaypointCategory category) {
                    this.category = category;
                }

                public void addWaypoint(WaypointWidget widget) {
                    waypoints.add(widget);
                    addChild(widget);
                }

                @Override
                public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
                    this.ui = ui;
                    if(!visible || this.ui == null) return;
                    // update hover state for this widget
                    hovered = contains(mouseX, mouseY);
                    updateValues();
                    drawBackground(ctx, mouseX, mouseY, tickDelta);
                    drawContent(ctx, mouseX, mouseY, tickDelta);
                    // draw children in insertion order (lower z first)
                    for (Widget child : children) {
                        child.setUi(ui);
                        if(!child.isVisible() || child.getUi() == null) return;
                        // update hover state for this widget
                        child.setHovered(child.contains(mouseX, mouseY));
                    }
                    drawForeground(ctx, mouseX, mouseY, tickDelta);
                }

                @Override
                protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    drawConfigRow(ui, x, y, width, height, hovered, false, GOLD_DARK);

                    String name = category == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : category.name;
                    String arrow = collapsed ? "▶ " : "▼ ";
                    CustomColor color = category == null ? CustomColor.fromHexString("FFFFFF") : category.color;

                    ui.drawText(arrow, x + 15, y + height / 2f, color, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);
                    ui.drawText(name, x + 50, y + height / 2f, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);

                    if (collapsed) return;

                    float offsetY = y + height + 10;

                    for (WaypointWidget waypoint : waypoints) {
                        waypoint.setBounds(x + 20, (int) offsetY, width - 40, waypoint.getTotalHeight());
                        waypoint.draw(ctx, mouseX, mouseY, tickDelta, ui);
                        offsetY += waypoint.getTotalHeight() + 10;
                    }
                }

                @Override
                protected boolean onClick(int button) {
                    collapsed = !collapsed;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }

                public float getTotalHeight() {
                    if (collapsed) return height;
                    float result = height + 10;
                    for (WaypointWidget waypoint : waypoints) {
                        result += waypoint.getTotalHeight() + 10;
                    }
                    return result;
                }
            }

            private static class WaypointWidget extends Widget {
                private static final int COLLAPSED_HEIGHT = 50;
                private static final int EXPANDED_HEIGHT = 275;

                final Waypoint waypoint;
                private boolean expanded = false;
                private boolean categoryExpanded = false;
                private WaypointTextInput nameInput;
                private WaypointTextInput xInput;
                private WaypointTextInput yInput;
                private WaypointTextInput zInput;

                public WaypointWidget(Waypoint waypoint) {
                    this.waypoint = waypoint;
                    if (waypoint != null) {
                        nameInput = new WaypointTextInput(waypoint.name == null ? "" : waypoint.name, this::applyName);
                        xInput = new WaypointTextInput(String.valueOf(waypoint.x), ignored -> applyCoordinates());
                        yInput = new WaypointTextInput(String.valueOf(waypoint.y), ignored -> applyCoordinates());
                        zInput = new WaypointTextInput(String.valueOf(waypoint.z), ignored -> applyCoordinates());
                        xInput.setCharacterFilter(character -> (character >= '0' && character <= '9') || character == '-');
                        yInput.setCharacterFilter(character -> (character >= '0' && character <= '9') || character == '-');
                        zInput.setCharacterFilter(character -> (character >= '0' && character <= '9') || character == '-');

                        addChild(nameInput);
                        addChild(xInput);
                        addChild(yInput);
                        addChild(zInput);
                        setInputsVisible(false);
                    }
                }

                @Override
                protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    boolean headerHovered = isIn(mouseX, mouseY, x, y, width, COLLAPSED_HEIGHT);
                    drawConfigRow(ui, x, y, width, COLLAPSED_HEIGHT, headerHovered && mainAreaWidget.isHovered(), false, GOLD_DARK);
                    if (expanded) {
                        ui.drawRect(x, y + COLLAPSED_HEIGHT, width, height - COLLAPSED_HEIGHT, CustomColor.fromInt(BG_MEDIUM));
                        ui.drawRect(x + 12, y + COLLAPSED_HEIGHT, width - 24, 2, CustomColor.fromInt(BORDER_DARK));
                    }
                    if(waypoint != null) {
                        String categoryName = waypoint.getCategory() == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : waypoint.getCategory().name;
                        ui.drawText((expanded ? "▼ " : "▶ ") + waypoint.name, x + 20, y + 25, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);
                        ui.drawText("x: " + waypoint.x + " y: " + waypoint.y + " z: " + waypoint.z, x + width - 25, y + 25, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE, 2.5f);

                        setInputsVisible(expanded);
                        if (!expanded) return;

                        drawExpandedContent(ctx, mouseX, mouseY, tickDelta, categoryName);
                    }
                }

                private void drawExpandedContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta, String categoryName) {
                    int contentX = x + 25;
                    int contentY = y + COLLAPSED_HEIGHT + 18;
                    int inputHeight = 38;
                    int labelY = contentY + 8;
                    int fieldX = contentX + 135;
                    int fieldWidth = Math.max(240, width - 185);

                    ui.drawText("Text", contentX, labelY, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    setInputBounds(nameInput, fieldX, contentY, fieldWidth, inputHeight);

                    int coordsY = contentY + 54;
                    int coordFieldWidth = 150;
                    ui.drawText("Coordinates", contentX, coordsY + 8, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    setInputBounds(xInput, fieldX, coordsY, coordFieldWidth, inputHeight);
                    setInputBounds(yInput, fieldX + coordFieldWidth + 35, coordsY, coordFieldWidth, inputHeight);
                    setInputBounds(zInput, fieldX + (coordFieldWidth + 35) * 2, coordsY, coordFieldWidth, inputHeight);

                    int visibilityY = contentY + 108;
                    ui.drawText("Visibility", contentX, visibilityY + 15, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    drawOverrideToggle(contentX + 135, visibilityY, 150, 38, "Name", waypoint.showNameOverride, waypoint.shouldShowName());
                    drawOverrideToggle(contentX + 305, visibilityY, 150, 38, "Block", waypoint.showOverride, waypoint.shouldShowBlock());
                    drawOverrideToggle(contentX + 475, visibilityY, 180, 38, "Distance", waypoint.showDistanceOverride, waypoint.shouldShowDistance());

                    int categoryY = contentY + 162;
                    ui.drawText("Category", contentX, categoryY + 15, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    drawConfigRow(ui, fieldX, categoryY, fieldWidth, 38, categoryExpanded, categoryExpanded, GOLD_DARK);
                    CustomColor categoryColor = waypoint.getCategory() == null ? CustomColor.fromHexString("FFFFFF") : waypoint.getCategory().color;
                    ui.drawText(categoryName, fieldX + 15, categoryY + 19, categoryColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);

                    if (categoryExpanded) {
                        int optionY = categoryY + 43;
                        if (MainWidget.activePackage != null) {
                            for (WaypointCategory category : MainWidget.activePackage.categories) {
                                drawCategoryOption(fieldX, optionY, fieldWidth, 34, category);
                                optionY += 34;
                            }
                        }
                    }
                }

                private void setInputsVisible(boolean visible) {
                    if (nameInput == null) return;
                    nameInput.setVisible(visible);
                    xInput.setVisible(visible);
                    yInput.setVisible(visible);
                    zInput.setVisible(visible);
                }

                private void setInputBounds(WaypointTextInput input, int x, int y, int width, int height) {
                    input.setBounds(x, y, width, height);
                    input.setUi(ui);
                }

                private void drawToggle(int x, int y, int width, int height, String label, boolean enabled) {
                    drawConfigRow(ui, x, y, width, height, enabled, enabled, TOGGLE_ON);
                    ui.drawCenteredText(label + ": " + (enabled ? "On" : "Off"), x + width / 2f, y + height / 2f, enabled ? CustomColor.fromInt(TOGGLE_ON) : CustomColor.fromInt(TEXT_DIM), 2.4f);
                }

                private void drawOverrideToggle(int x, int y, int width, int height, String label, Boolean override, boolean effective) {
                    boolean enabled = override != null ? override : effective;
                    drawConfigRow(ui, x, y, width, height, enabled, enabled, TOGGLE_ON);
                    String state = override == null ? "Default " + (effective ? "On" : "Off") : (override ? "On" : "Off");
                    ui.drawCenteredText(label + ": " + state, x + width / 2f, y + height / 2f, enabled ? CustomColor.fromInt(TOGGLE_ON) : CustomColor.fromInt(TEXT_DIM), 2.2f);
                }

                private void drawCategoryOption(int x, int y, int width, int height, WaypointCategory category) {
                    boolean selected = waypoint.getCategory() == category;
                    drawConfigRow(ui, x, y, width, height, selected, selected, category == null ? GOLD_DARK : category.color.asInt());
                    String name = category == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : category.name;
                    CustomColor color = category == null ? CustomColor.fromHexString("FFFFFF") : category.color;
                    ui.drawText(name, x + 12, y + height / 2f, color, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                }

                @Override
                public boolean mouseClicked(double mx, double my, int button) {
                    if (!visible || !enabled || waypoint == null) return false;

                    if (expanded) {
                        for (int i = children.size() - 1; i >= 0; i--) {
                            if (children.get(i).mouseClicked(mx, my, button)) return true;
                        }

                        if (isIn(mx, my, x + 25 + 135, y + COLLAPSED_HEIGHT + 18 + 162, Math.max(240, width - 185), 38)) {
                            categoryExpanded = !categoryExpanded;
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }

                        if (categoryExpanded && clickCategoryOption(mx, my)) return true;

                        int visibilityY = y + COLLAPSED_HEIGHT + 18 + 108;
                        int contentX = x + 25;
                        if (isIn(mx, my, contentX + 135, visibilityY, 150, 38)) {
                            waypoint.setShowNameOverride(nextOverride(waypoint.showNameOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            saveWaypoint();
                            return true;
                        }
                        if (isIn(mx, my, contentX + 305, visibilityY, 150, 38)) {
                            waypoint.setShowOverride(nextOverride(waypoint.showOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            saveWaypoint();
                            return true;
                        }
                        if (isIn(mx, my, contentX + 475, visibilityY, 180, 38)) {
                            waypoint.setShowDistanceOverride(nextOverride(waypoint.showDistanceOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            saveWaypoint();
                            return true;
                        }
                    }

                    if (isIn(mx, my, x, y, width, COLLAPSED_HEIGHT)) {
                        expanded = !expanded;
                        categoryExpanded = false;
                        setFocused(true);
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }

                    if (expanded && contains((int) mx, (int) my)) return true;

                    if (focused) setFocused(false);
                    return false;
                }

                private boolean clickCategoryOption(double mx, double my) {
                    int fieldX = x + 25 + 135;
                    int fieldWidth = Math.max(240, width - 185);
                    int optionY = y + COLLAPSED_HEIGHT + 18 + 162 + 43;

                    if (MainWidget.activePackage == null) return false;
                    for (WaypointCategory category : MainWidget.activePackage.categories) {
                        if (isIn(mx, my, fieldX, optionY, fieldWidth, 34)) {
                            waypoint.setCategory(category);
                            categoryExpanded = false;
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            saveWaypoint();
                            WaypointsTabContent.categoryWidgets.clear();
                            return true;
                        }
                        optionY += 34;
                    }
                    return false;
                }

                private boolean isIn(double mx, double my, int x, int y, int width, int height) {
                    return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
                }

                private Boolean nextOverride(Boolean override) {
                    if (override == null) return true;
                    if (override) return false;
                    return null;
                }

                private void applyName(String name) {
                    if (waypoint == null) return;
                    waypoint.name = name == null || name.isBlank() ? "Waypoint" : name;
                    saveWaypoint();
                }

                private void applyCoordinates() {
                    if (waypoint == null) return;
                    try {
                        waypoint.x = Integer.parseInt(xInput.getInput().trim());
                        waypoint.y = Integer.parseInt(yInput.getInput().trim());
                        waypoint.z = Integer.parseInt(zInput.getInput().trim());
                        saveWaypoint();
                    } catch (NumberFormatException ignored) {}
                }

                private void saveWaypoint() {
                    WaypointData.save();
                }

                public int getTotalHeight() {
                    if (!expanded) return COLLAPSED_HEIGHT;
                    int categoryOptions = categoryExpanded && MainWidget.activePackage != null ? MainWidget.activePackage.categories.size() : 0;
                    return EXPANDED_HEIGHT + categoryOptions * 34;
                }

                private static class WaypointTextInput extends TextInputWidget {
                    private WaypointTextInput(String input, java.util.function.Consumer<String> changeConsumer) {
                        super(0, 0, 0, 0, 10, 11, 2);
                        setInput(input);
                        setBackgroundColor(CustomColor.fromInt(BG_LIGHT));
                        setFocusedColor(CustomColor.fromInt(PARCHMENT_LIGHT));
                        setTextColor(CustomColor.fromInt(TEXT_LIGHT));
                        setPlaceholderColor(CustomColor.fromInt(TEXT_DIM));
                        setOnChange(changeConsumer);
                    }
                }
            }
        }

        private static class CategoriesTabContent extends TabContentWidget {
            private ActionButtonWidget addCategoryButton;

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if (addCategoryButton == null) {
                    addCategoryButton = new ActionButtonWidget("Add Category", this::addCategory);
                    addChild(addCategoryButton);
                }

                addCategoryButton.setBounds(x + 30, y + 10, 260, 44);

                int rowY = y + 75;
                if (activePackage == null || activePackage.categories.isEmpty()) {
                    ui.drawText("No categories", x + 35, rowY, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.6f);
                    return;
                }

                for (WaypointCategory category : activePackage.categories) {
                    ui.drawRect(x + 35, rowY, width - 80, 44, CustomColor.fromInt(BG_MEDIUM));
                    ui.drawRect(x + 50, rowY + 12, 20, 20, category.color);
                    ui.drawText(category.name, x + 85, rowY + 22, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.8f);
                    int toggleY = rowY + 6;
                    drawCategoryDefaultToggle(x + width - 445, toggleY, 120, 32, "Block", category.showBlockByDefault);
                    drawCategoryDefaultToggle(x + width - 315, toggleY, 120, 32, "Text", category.showNameByDefault);
                    drawCategoryDefaultToggle(x + width - 185, toggleY, 140, 32, "Distance", category.showDistanceByDefault);
                    rowY += 54;
                }
            }

            private void drawCategoryDefaultToggle(int x, int y, int width, int height, String label, boolean enabled) {
                drawConfigRow(ui, x, y, width, height, enabled, enabled, TOGGLE_ON);
                ui.drawCenteredText(label + ": " + (enabled ? "On" : "Off"), x + width / 2f, y + height / 2f, enabled ? CustomColor.fromInt(TOGGLE_ON) : CustomColor.fromInt(TEXT_DIM), 2.2f);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!visible || !enabled) return false;
                for (int i = children.size() - 1; i >= 0; i--) {
                    if (children.get(i).mouseClicked(mx, my, button)) return true;
                }
                if (button != 0 || activePackage == null || !contains((int) mx, (int) my)) return false;

                int rowY = y + 75;
                for (WaypointCategory category : activePackage.categories) {
                    int toggleY = rowY + 6;
                    if (isIn(mx, my, x + width - 445, toggleY, 120, 32)) {
                        category.showBlockByDefault = !category.showBlockByDefault;
                        saveCategoryDefaults();
                        return true;
                    }
                    if (isIn(mx, my, x + width - 315, toggleY, 120, 32)) {
                        category.showNameByDefault = !category.showNameByDefault;
                        saveCategoryDefaults();
                        return true;
                    }
                    if (isIn(mx, my, x + width - 185, toggleY, 140, 32)) {
                        category.showDistanceByDefault = !category.showDistanceByDefault;
                        saveCategoryDefaults();
                        return true;
                    }
                    rowY += 54;
                }
                return false;
            }

            private boolean isIn(double mx, double my, int x, int y, int width, int height) {
                return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
            }

            private void saveCategoryDefaults() {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                WaypointData.save();
                WaypointsTabContent.categoryWidgets.clear();
            }

            private void addCategory() {
                if (activePackage == null) return;

                WaypointCategory category = new WaypointCategory(generateUniqueCategoryName());
                activePackage.categories.add(category);
                WaypointData.save();
                WaypointsTabContent.categoryWidgets.clear();
            }

            private String generateUniqueCategoryName() {
                if (activePackage == null) return "New Category";

                String base = "New Category";
                String candidate = base;
                int i = 1;
                while (true) {
                    String check = candidate;
                    boolean exists = activePackage.categories.stream()
                            .anyMatch(category -> category.name != null && category.name.equalsIgnoreCase(check));
                    if (!exists) return candidate;
                    candidate = base + " " + i;
                    i++;
                }
            }

            @Override
            public float calculateTotalHeight() {
                if (activePackage == null) return 75;
                return 75 + activePackage.categories.size() * 54;
            }
        }

        private static class SettingsTabContent extends TabContentWidget {
            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawText("Settings", x + 35, y + 35, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 3f);
            }

            @Override
            public float calculateTotalHeight() {
                return 80;
            }
        }

        private static class WaypointScrollBarWidget extends Widget {
            private final int buttonHeight = 40;
            private boolean held = false;

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                float contentHeight = activeTabWidget.calculateTotalHeight();
                float viewportHeight = mainAreaWidget.getHeight();
                float maxOffset = Math.max(0, contentHeight - viewportHeight);

                if (maxOffset <= 0) return;

                ui.drawSliderBackground(x, y, width, height);

                float actualOffset = actualOffsets.getOrDefault(ScrollType.Waypoints, 0f);
                float percent = actualOffset / maxOffset;
                percent = Math.clamp(percent, 0f, 1f);

                int scrollAreaHeight = height - buttonHeight;
                int yPos = y + (int)(scrollAreaHeight * percent);

                ui.drawButton(x, yPos, width, buttonHeight, hovered || held);

                if (held) {
                    float relativeY = mouseY * ui.getScaleFactorF() - y - buttonHeight / 2f;
                    relativeY = Math.max(0, Math.min(relativeY, scrollAreaHeight));

                    float scrollPercent = relativeY / scrollAreaHeight;
                    float newOffset = scrollPercent * maxOffset;

                    targetOffsets.put(ScrollType.Waypoints, newOffset);
                }
            }

            @Override
            protected boolean onClick(int button) {
                held = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                held = false;
                return false;
            }
        }
    }
}
