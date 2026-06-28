package julianh06.wynnextras.features.waypoints;

import com.google.gson.JsonSyntaxException;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.waypoints.data.Waypoint;
import julianh06.wynnextras.features.waypoints.data.WaypointCategory;
import julianh06.wynnextras.features.waypoints.data.WaypointData;
import julianh06.wynnextras.features.waypoints.data.WaypointPackage;
import julianh06.wynnextras.utils.UI.ColorPickerWidget;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class WaypointScreen extends WEScreen {
    @Override protected double getTargetScaleFactor() { return 2.5; }
    @Override protected int getMinLogicalWidth() { return 1925; }
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
    private static final String UNCATEGORIZED_CATEGORY_TOOLTIP = "This category cannot be deleted and it's name can't be changed";
    private static final Identifier MOVE_ICON = Identifier.of("wynnextras", "textures/gui/waypointeditmodeui/move_icon.png");

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
    private static final long FEEDBACK_DURATION_MS = 3000;
    private static String feedbackMessage = "";
    private static int feedbackColor = TOGGLE_ON;
    private static long feedbackUntil = 0;
    private static float feedbackX = 0;
    private static float feedbackY = 0;

    private static SideBarWidget sideBarWidget;
    private MainWidget mainWidget;

    private enum ScrollType { Packages, Waypoints, Categories }

    static Map<ScrollType, Float> targetOffsets = new HashMap<>();
    static Map<ScrollType, Float> actualOffsets = new HashMap<>();

    protected WaypointScreen() {
        this(null, null);
    }

    protected WaypointScreen(WaypointPackage initialPackage, Waypoint initialWaypoint) {
        super(Text.of("WynnExtras Waypoint Screen"));
        String initialPackageId = initialPackage == null ? null : initialPackage.id;
        String initialPackageName = initialPackage == null ? null : initialPackage.name;
        String initialWaypointId = initialWaypoint == null ? null : initialWaypoint.id;
        WaypointData.reloadFromDisk();
        initialPackage = WaypointData.findPackage(initialPackageId, initialPackageName);
        initialWaypoint = WaypointData.findWaypoint(initialPackage, initialWaypointId);
        sideBarWidget = new SideBarWidget();
        addRootWidget(sideBarWidget);
        MainWidget.resetState(initialPackage, initialWaypoint);
        mainWidget = new MainWidget();
        addRootWidget(mainWidget);
    }

    public static void open(WaypointPackage initialPackage, Waypoint initialWaypoint) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        client.send(() -> client.setScreen(new WaypointScreen(initialPackage, initialWaypoint)));
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

        int sideBarWidth = 520;
        int logicalWidth = getLogicalWidth();
        int logicalHeight = getLogicalHeight();
        sideBarWidget.setBounds(0, 0, sideBarWidth, logicalHeight);
        mainWidget.setBounds(sideBarWidth, 0, logicalWidth - sideBarWidth, logicalHeight);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (feedbackMessage.isEmpty() || System.currentTimeMillis() > feedbackUntil) return;
        int logicalWidth = getLogicalWidth();
        int logicalHeight = getLogicalHeight();
        int toastWidth = 660;
        int toastHeight = 44;
        int toastX = (int) Math.clamp(feedbackX, 20, Math.max(20, logicalWidth - toastWidth - 20));
        int toastY = (int) Math.clamp(feedbackY, 20, Math.max(20, logicalHeight - toastHeight - 20));
        ui.drawRect(toastX, toastY, toastWidth, toastHeight, CustomColor.fromInt(BG_MEDIUM));
        ui.drawRect(toastX, toastY, toastWidth, 3, CustomColor.fromInt(feedbackColor));
        ui.drawRect(toastX, toastY + toastHeight - 3, toastWidth, 3, CustomColor.fromInt(BORDER_DARK));
        ui.drawCenteredText(feedbackMessage, toastX + toastWidth / 2f, toastY + toastHeight / 2f, CustomColor.fromInt(TEXT_LIGHT), 2.25f);
    }

    private static void showFeedback(String message, boolean success, double mouseX, double mouseY, UIUtils ui) {
        feedbackMessage = message;
        feedbackColor = success ? TOGGLE_ON : ACCENT_RED;
        feedbackX = ui == null ? (float) mouseX : (float) ((mouseX - ui.getXStart()) * ui.getScaleFactor());
        feedbackY = ui == null ? (float) mouseY : (float) ((mouseY - ui.getYStart()) * ui.getScaleFactor());
        feedbackUntil = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
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

    private static void drawButton(UIUtils ui, int x, int y, int width, int height, boolean hover, int accent) {
        ui.drawRect(x, y, width, height, CustomColor.fromInt(hover ? PARCHMENT_HOVER : PARCHMENT));
        ui.drawRect(x, y, width, 2, CustomColor.fromInt(hover ? GOLD : BORDER_LIGHT));
        ui.drawRect(x, y + height - 2, width, 2, CustomColor.fromInt(BORDER_DARK));
        ui.drawRect(x + 4, y + height - 5, width - 8, 2, CustomColor.fromInt(accent));
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
        private static final int PACKAGE_HEIGHT = 50;
        private static final int PACKAGE_SPACING = 8;
        private static final int PACKAGE_X_PADDING = 24;
        private static final int ADD_SECTION_HEIGHT = 172;
        private static final int ADD_BUTTON_HEIGHT = 46;

        public List<PackageWidget> packageWidgets = new ArrayList<>();
        boolean initialized = false;
        static int draggedIndex = -1;
        static int packageOverMouseIndex = -1;
        static int packageUnderMouseIndex = -1;
        private float packageScrollTarget = 0;
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
            packageScrollTarget = MathHelper.clamp(packageScrollTarget, 0, maxScroll);
            float diff = packageScrollTarget - packageScrollOffset;
            if (Math.abs(diff) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) packageScrollOffset = packageScrollTarget;
            else packageScrollOffset += diff * 0.3f * tickDelta;

            packageOverMouseIndex = -1;
            packageUnderMouseIndex = -1;

            float mouseYScaled = isInPackageList(mouseX, mouseY, packageY, listEndY) ? mouseY * ui.getScaleFactorF() + packageScrollOffset : -1;

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

            for (PackageWidget packageWidget : packageWidgets) {
                if (packageWidget.isDragging) {
                    packageWidget.drawDraggedPreview(ctx, mouseX, mouseY);
                    break;
                }
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
            drawButton(ui, PACKAGE_X_PADDING, getAddPackageButtonY(), packageWidth, ADD_BUTTON_HEIGHT, isAddPackageHovered(mouseX, mouseY), GOLD_DARK);
            ui.drawCenteredText("Add Package", PACKAGE_X_PADDING + packageWidth / 2f, getAddPackageButtonY() + ADD_BUTTON_HEIGHT / 2f, CustomColor.fromHexString("FFFFFF"), 2.6f);
            drawButton(ui, PACKAGE_X_PADDING, getImportPackageButtonY(), packageWidth, ADD_BUTTON_HEIGHT, isImportPackageHovered(mouseX, mouseY), TOGGLE_ON);
            ui.drawCenteredText("Import Package From Clipboard", PACKAGE_X_PADDING + packageWidth / 2f, getImportPackageButtonY() + ADD_BUTTON_HEIGHT / 2f, CustomColor.fromHexString("FFFFFF"), 2.25f);
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
            if (isImportPackageHovered(mx, my)) {
                importPackageFromClipboard(mx, my);
                return true;
            }
            if (!isInPackageList(mx, my)) return super.mouseClicked(mx, my, button);
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
            packageScrollTarget += delta > 0 ? -55 : 55;
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

        private boolean isInPackageList(double mx, double my) {
            int addSeparatorY = height - ADD_SECTION_HEIGHT;
            return isInPackageList(mx, my, LIST_START_Y, addSeparatorY - 16);
        }

        private boolean isInPackageList(double mx, double my, int listStartY, int listEndY) {
            return mx >= ui.sx(PACKAGE_X_PADDING)
                    && my >= ui.sy(listStartY)
                    && mx < ui.sx(width - 5)
                    && my < ui.sy(listEndY);
        }

        private boolean isAddPackageHovered(double mx, double my) {
            int packageWidth = width - PACKAGE_X_PADDING * 2 - 5;
            return mx >= ui.sx(PACKAGE_X_PADDING)
                    && my >= ui.sy(getAddPackageButtonY())
                    && mx < ui.sx(PACKAGE_X_PADDING) + ui.sw(packageWidth)
                    && my < ui.sy(getAddPackageButtonY() + ADD_BUTTON_HEIGHT);
        }

        private boolean isImportPackageHovered(double mx, double my) {
            int packageWidth = width - PACKAGE_X_PADDING * 2 - 5;
            return mx >= ui.sx(PACKAGE_X_PADDING)
                    && my >= ui.sy(getImportPackageButtonY())
                    && mx < ui.sx(PACKAGE_X_PADDING) + ui.sw(packageWidth)
                    && my < ui.sy(getImportPackageButtonY() + ADD_BUTTON_HEIGHT);
        }

        private int getAddPackageButtonY() {
            return height - 126;
        }

        private int getImportPackageButtonY() {
            return height - 72;
        }

        private void addPackage() {
            WaypointPackage waypointPackage = WaypointActions.createPackage("New Package");
            MainWidget.activePackage = waypointPackage;
            rebuildPackageWidgetsFromData();
            MainWidget.invalidateAllTabs();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        }

        private void importPackageFromClipboard(double mx, double my) {
            try {
                WaypointPackage imported = WaypointActions.importPackageFromJson(MinecraftClient.getInstance().keyboard.getClipboard());
                MainWidget.activePackage = imported;
                WaypointData.INSTANCE.activePackage = imported;
                rebuildPackageWidgetsFromData();
                MainWidget.invalidateAllTabs();
                showFeedback("Imported package \"" + imported.name + "\".", true, mx, my, ui);
            } catch (JsonSyntaxException | IllegalArgumentException e) {
                showFeedback("Failed to import package: " + e.getMessage(), false, mx, my, ui);
            } catch (Exception e) {
                showFeedback("Failed to import package.", false, mx, my, ui);
            }
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        }

        private static class PackageWidget extends Widget {
            private static final int TOGGLE_WIDTH = 104;
            private static final int TOGGLE_HEIGHT = 34;
            private static final int TOGGLE_RIGHT_PADDING = 14;
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
                boolean disabled = !waypointPackage.enabled;
                drawConfigRow(ui, x, y, width, height, hovered, selected, disabled ? ACCENT_RED : GOLD_DARK);
                if (disabled) {
                    ui.drawRect(x, y, width, height, CustomColor.fromInt(0x55200000));
                }
                int markerColor = disabled ? ACCENT_RED : (selected ? GOLD_DARK : BORDER_DARK);
                CustomColor nameColor = selected ? CustomColor.fromInt(TEXT_LIGHT) : CustomColor.fromInt(disabled ? 0xFFb88f8f : TEXT_DIM);
                drawDiamond(ctx, (int) ui.sx(x + 18), (int) ui.sy(y + height / 2f), Math.max(2, ui.sw(4)), markerColor);
                ui.drawText(truncateName(waypointPackage.name), x + 36, y + height / 2f, nameColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);
                drawEnabledToggle(waypointPackage.enabled, isToggleHovered(mouseX, mouseY));
            }

            private void drawDraggedPreview(DrawContext ctx, int mouseX, int mouseY) {
                float dragX = mouseX * ui.getScaleFactorF();
                float dragY = mouseY * ui.getScaleFactorF();
                boolean disabled = !waypointPackage.enabled;
                drawConfigRow(ui, dragX, dragY, width, height, true, true, GOLD_DARK);
                if (disabled) {
                    ui.drawRect(dragX, dragY, width, height, CustomColor.fromInt(0x55200000));
                }
                drawDiamond(ctx, (int) ui.sx(dragX + 18), (int) ui.sy(dragY + height / 2f), Math.max(2, ui.sw(4)), disabled ? ACCENT_RED : GOLD_DARK);
                ui.drawText(truncateName(waypointPackage.name), dragX + 36, dragY + height / 2f, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);
                drawEnabledToggleAt(dragX, dragY, waypointPackage.enabled, false);
            }

            private String truncateName(String name) {
                String value = name == null ? "" : name;
                int availableWidth = width - 36 - TOGGLE_WIDTH - TOGGLE_RIGHT_PADDING - 12;
                return truncateText(value, Math.max(0, availableWidth), 2.6f);
            }

            private void drawEnabledToggle(boolean enabled, boolean hover) {
                drawEnabledToggleAt(x, y, enabled, hover);
            }

            private void drawEnabledToggleAt(float rowX, float rowY, boolean enabled, boolean hover) {
                int toggleX = (int) (rowX + width - TOGGLE_WIDTH - TOGGLE_RIGHT_PADDING);
                int toggleY = (int) (rowY + (height - TOGGLE_HEIGHT) / 2f);
                int accent = enabled ? TOGGLE_ON : ACCENT_RED;
                ui.drawRect(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, CustomColor.fromInt(hover ? PARCHMENT_HOVER : BG_LIGHT));
                ui.drawRect(toggleX, toggleY, TOGGLE_WIDTH, 2, CustomColor.fromInt(hover ? GOLD : BORDER_LIGHT));
                ui.drawRect(toggleX, toggleY + TOGGLE_HEIGHT - 2, TOGGLE_WIDTH, 2, CustomColor.fromInt(BORDER_DARK));
                ui.drawRect(toggleX + 4, toggleY + TOGGLE_HEIGHT - 5, TOGGLE_WIDTH - 8, 2, CustomColor.fromInt(accent));
                ui.drawCenteredText(enabled ? "Enabled" : "Disabled", toggleX + TOGGLE_WIDTH / 2f, toggleY + TOGGLE_HEIGHT / 2f, CustomColor.fromInt(TEXT_LIGHT), 1.95f);
            }

            private boolean isToggleHovered(double mx, double my) {
                int toggleX = x + width - TOGGLE_WIDTH - TOGGLE_RIGHT_PADDING;
                int toggleY = y + (height - TOGGLE_HEIGHT) / 2;
                return mx >= ui.sx(toggleX)
                        && my >= ui.sy(toggleY)
                        && mx < ui.sx(toggleX) + ui.sw(TOGGLE_WIDTH)
                        && my < ui.sy(toggleY) + ui.sh(TOGGLE_HEIGHT);
            }

            private String truncateText(String text, int maxWidth, float scale) {
                if (textWidth(text, scale) <= maxWidth) return text;
                String suffix = "...";
                int suffixWidth = textWidth(suffix, scale);
                if (suffixWidth > maxWidth) return "";

                int low = 0;
                int high = text.length();
                while (low < high) {
                    int mid = (low + high + 1) / 2;
                    if (textWidth(text.substring(0, mid) + suffix, scale) <= maxWidth) low = mid;
                    else high = mid - 1;
                }
                return text.substring(0, low) + suffix;
            }

            private int textWidth(String text, float scale) {
                return (int) Math.ceil(MinecraftClient.getInstance().textRenderer.getWidth(text) * scale);
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
                if (isToggleHovered(mx, my)) {
                    WaypointActions.setPackageEnabled(waypointPackage, !waypointPackage.enabled);
                    MainWidget.invalidateAllTabs();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
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
                        WaypointData.INSTANCE.activePackage = waypointPackage;
                        targetOffsets.put(ScrollType.Waypoints, 0f);
                        actualOffsets.put(ScrollType.Waypoints, 0f);
                    }
                    MainWidget.invalidateAllTabs();
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

                        WaypointActions.savePackagesAndOrder();

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
        private static Waypoint waypointToExpand = null;

        private static Tab activeTab = Tab.Waypoints;
        private static List<TabWidget> tabWidgets = new ArrayList<>();

        private static MainAreaWidget mainAreaWidget;

        private static TabContentWidget activeTabWidget;

        private static WaypointsTabContent waypointsTab;
        private static CategoriesTabContent categoriesTab;
        private static SettingsTabContent settingsTab;

        private static void resetState(WaypointPackage initialPackage, Waypoint initialWaypoint) {
            activePackage = initialPackage != null && WaypointData.INSTANCE.packages.contains(initialPackage)
                    ? initialPackage
                    : null;
            if (activePackage == null && initialWaypoint != null) {
                activePackage = WaypointEditMode.packageOf(initialWaypoint);
            }
            waypointToExpand = initialWaypoint;
            activeTab = Tab.Waypoints;
            tabWidgets.clear();
            mainAreaWidget = null;
            activeTabWidget = null;
            waypointsTab = null;
            categoriesTab = null;
            settingsTab = null;
        }

        private static void invalidateAllTabs() {
            if (waypointsTab != null) waypointsTab.invalidate();
            if (categoriesTab != null) categoriesTab.invalidate();
            if (settingsTab != null) settingsTab.invalidate();
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

            if(activePackage == null) {
                drawNoPackageHint();
                return;
            }
            WaypointData.resolveWaypointCategories(activePackage);

            String description = activePackage.description == null ? "" : activePackage.description;
            boolean hasNoDescription = description.isEmpty();
            int detailTextWidth = Math.max(120, width - 100);
            List<String> descriptionLines = hasNoDescription ? List.of() : wrapText(description, detailTextWidth, 2.6f);
            int descriptionHeight = descriptionLines.isEmpty() ? 0 : descriptionLines.size() * 28;
            int bgHeight = hasNoDescription ? 80 : Math.max(120, 100 + descriptionHeight);
            int tabsY = y + 170 + bgHeight;
            int topOffset = 240 + bgHeight;

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
                tabWidget.setBounds((int) xStart, tabsY, tabWidth, 50);
                tabWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                xStart += tabWidth + spacing;
            }

            ui.drawRect(x + 30, y + 150, width - 60, bgHeight, CustomColor.fromInt(PARCHMENT));
            ui.drawText(activePackage.name, x + 40, y + 190);
            for (int i = 0; i < descriptionLines.size(); i++) {
                ui.drawText(descriptionLines.get(i), x + 40, y + 230 + i * 28, CustomColor.fromInt(TEXT_DIM), 2.6f);
            }

            if(activeTabWidget == null) return;

            activeTabWidget.setBounds(x, y + topOffset, width, height - topOffset);
            activeTabWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        private void drawNoPackageHint() {
            float centerX = x + width / 2f;
            float centerY = y + 150 + (height - 150) / 2f;
            ui.drawCenteredText("Select a package on the left to get started.", centerX, centerY - 32, CustomColor.fromInt(TEXT_DIM), 2.65f);
            ui.drawCenteredText("Use Waypoint Edit Mode if you want to create your own waypoints.", centerX, centerY + 2, CustomColor.fromInt(TEXT_DIM), 2.35f);
            ui.drawCenteredText("Open it with /we waypoints edit.", centerX, centerY + 32, CustomColor.fromInt(TEXT_DIM), 2.25f);
        }

        private List<String> wrapText(String text, int maxWidth, float textScale) {
            if (text == null || text.isEmpty()) return List.of();

            List<String> lines = new ArrayList<>();
            int lineStart = 0;
            int index = 0;
            int lastBreak = -1;
            while (index < text.length()) {
                int next = index + 1;
                if (Character.isWhitespace(text.charAt(index))) lastBreak = next;
                if (textWidth(text.substring(lineStart, next), textScale) <= maxWidth || index == lineStart) {
                    index = next;
                    continue;
                }
                if (lastBreak > lineStart) {
                    lines.add(text.substring(lineStart, lastBreak).stripTrailing());
                    lineStart = lastBreak;
                } else {
                    lines.add(text.substring(lineStart, index));
                    lineStart = index;
                }
                index = lineStart;
                lastBreak = -1;
            }
            lines.add(text.substring(lineStart).stripTrailing());
            return lines;
        }

        private int textWidth(String text, float textScale) {
            return (int) Math.ceil(MinecraftClient.getInstance().textRenderer.getWidth(text) * textScale);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            ScrollType scrollType = activeTab == Tab.Categories ? ScrollType.Categories : ScrollType.Waypoints;
            if (activeTab == Tab.Settings) return false;
            if(delta > 0) targetOffsets.put(scrollType, targetOffsets.getOrDefault(scrollType, 0f) - 33f);
            else targetOffsets.put(scrollType, targetOffsets.getOrDefault(scrollType, 0f) + 33f);
            if(targetOffsets.getOrDefault(scrollType, 0f) < 0) targetOffsets.put(scrollType, 0f);
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
            public void invalidate() {}
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

        private static void openConfirm(String title, String message, Runnable confirmedAction) {
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen;
            openConfirm(title, message, confirmedAction, () -> parent);
        }

        private static void openConfirm(String title, String message, Runnable confirmedAction, java.util.function.Supplier<Screen> returnScreenSupplier) {
            MinecraftClient client = MinecraftClient.getInstance();
            Screen parent = client.currentScreen;
            client.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) confirmedAction.run();
                Screen returnScreen = confirmed ? returnScreenSupplier.get() : parent;
                client.setScreen(returnScreen);
            }, Text.of(title), Text.of(message), Text.of("Delete"), Text.of("Cancel")));
        }

        private static class ScreenTextInput extends TextInputWidget {
            private ScreenTextInput(String input, String placeholder, java.util.function.Consumer<String> changeConsumer) {
                this(input, placeholder, 2f, changeConsumer);
            }

            private ScreenTextInput(String input, String placeholder, float textScale, java.util.function.Consumer<String> changeConsumer) {
                super(0, 0, 0, 0, 12, 14, textScale);
                setInput(input);
                setPlaceholder(placeholder);
                setBackgroundColor(CustomColor.fromInt(BG_LIGHT));
                setFocusedColor(CustomColor.fromInt(PARCHMENT_LIGHT));
                setTextColor(CustomColor.fromInt(TEXT_LIGHT));
                setPlaceholderColor(CustomColor.fromInt(TEXT_DIM));
                setOnChange(changeConsumer);
            }
        }

        private static class WaypointsTabContent extends TabContentWidget {
            public static List<CategoryWidget> categoryWidgets = new ArrayList<>();
            private WaypointScrollBarWidget scrollBar;

            private void rebuildCategoryWidgets() {
                categoryWidgets.clear();
                clearChildren();

                Map<WaypointCategory, List<Waypoint>> grouped = new LinkedHashMap<>();
                WaypointData.resolveWaypointCategories(activePackage);
                for (WaypointCategory category : activePackage.categories) {
                    grouped.put(category, new ArrayList<>());
                }
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
                            .forEach(w -> catWidget.addWaypoint(new WaypointWidget(w, w == waypointToExpand)));

                    categoryWidgets.add(catWidget);
                    addChild(catWidget);
                }
                waypointToExpand = null;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if (scrollBar == null) {
                    scrollBar = new WaypointScrollBarWidget(ScrollType.Waypoints);
                    addChild(scrollBar);
                }

                if (activePackage == null) {
                    categoryWidgets.clear();
                    clearChildren();
                    addChild(scrollBar);
                    return;
                }

                if (categoryWidgets.isEmpty()) {
                    rebuildCategoryWidgets();
                    addChild(scrollBar);
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

                float currentY = y + 10 - actualOffsets.getOrDefault(ScrollType.Waypoints, 0f);

                boolean mouseInViewport = isInViewport(mouseX, mouseY);
                int effectiveMouseX = mouseInViewport ? mouseX : -1;
                int effectiveMouseY = mouseInViewport ? mouseY : -1;
                for (CategoryWidget category : categoryWidgets) {
                    category.setBounds(x + 30, (int) currentY, width - 70, 50);
                    boolean visibleInViewport = currentY + category.getTotalHeight() >= mainAreaWidget.getY()
                            && currentY <= mainAreaWidget.getY() + mainAreaWidget.getHeight();
                    category.setVisible(visibleInViewport);
                    if (visibleInViewport) category.draw(ctx, effectiveMouseX, effectiveMouseY, tickDelta, ui);

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
                if(categoryWidgets.isEmpty()) return 10;

                float result = 10;

                for (int i = 0; i < categoryWidgets.size(); i++) {
                    result += categoryWidgets.get(i).getTotalHeight() + 10;
                }

                return result;
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!isInViewport(mx, my)) return false;
                return super.mouseClicked(mx, my, button);
            }

            private boolean isInViewport(double mx, double my) {
                if (mainAreaWidget == null || ui == null) return false;
                return mx >= ui.sx(x)
                        && my >= ui.sy(mainAreaWidget.getY())
                        && mx < ui.sx(x + width)
                        && my < ui.sy(mainAreaWidget.getY() + mainAreaWidget.getHeight());
            }

            @Override
            public void invalidate() {
                categoryWidgets.clear();
                clearChildren();
                if (scrollBar != null) addChild(scrollBar);
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
                    boolean mouseInViewport = MainWidget.waypointsTab != null && MainWidget.waypointsTab.isInViewport(mouseX, mouseY);
                    // update hover state for this widget
                    hovered = mouseInViewport && contains(mouseX, mouseY);
                    updateValues();
                    drawBackground(ctx, mouseX, mouseY, tickDelta);
                    drawContent(ctx, mouseX, mouseY, tickDelta);
                    // draw children in insertion order (lower z first)
                    for (Widget child : children) {
                        child.setUi(ui);
                        if(!child.isVisible() || child.getUi() == null) return;
                        // update hover state for this widget
                        child.setHovered(mouseInViewport && child.contains(mouseX, mouseY));
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
                    int addButtonW = 165;
                    int addButtonX = x + width - addButtonW - 18;
                    ui.drawText(waypoints.size() + " waypoint" + (waypoints.size() == 1 ? "" : "s"), addButtonX - 24, y + height / 2f, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.RIGHT, VerticalAlignment.MIDDLE, 2.35f);
                    drawSmallHeaderButton(mouseX, mouseY, addButtonX, y + 6, addButtonW, 38, "New Waypoint", TOGGLE_ON);

                    if (collapsed) return;

                    float offsetY = y + height + 10;

                    if (waypoints.isEmpty()) {
                        ui.drawText("No waypoints in this category", x + 22, offsetY + 12, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.4f);
                        return;
                    }

                    boolean mouseInViewport = MainWidget.waypointsTab != null && MainWidget.waypointsTab.isInViewport(mouseX, mouseY);
                    int childMouseX = mouseInViewport ? mouseX : -1;
                    int childMouseY = mouseInViewport ? mouseY : -1;
                    for (WaypointWidget waypoint : waypoints) {
                        waypoint.setBounds(x + 20, (int) offsetY, width - 40, waypoint.getTotalHeight());
                        waypoint.draw(ctx, childMouseX, childMouseY, tickDelta, ui);
                        offsetY += waypoint.getTotalHeight() + 10;
                    }
                }

                @Override
                protected boolean onClick(int button) {
                    collapsed = !collapsed;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }

                @Override
                public boolean mouseClicked(double mx, double my, int button) {
                    if (!visible || !enabled) return false;
                    int addButtonW = 165;
                    int addButtonX = x + width - addButtonW - 18;
                    if (button == 0 && isIn(mx, my, addButtonX, y + 6, addButtonW, 38)) {
                        addWaypointInCategory();
                        return true;
                    }
                    return super.mouseClicked(mx, my, button);
                }

                private void drawSmallHeaderButton(int mouseX, int mouseY, int x, int y, int width, int height, String label, int accent) {
                    drawButton(ui, x, y, width, height, isIn(mouseX, mouseY, x, y, width, height), accent);
                    ui.drawCenteredText(label, x + width / 2f, y + height / 2f, CustomColor.fromInt(TEXT_LIGHT), 2.2f);
                }

                private void addWaypointInCategory() {
                    MinecraftClient client = MinecraftClient.getInstance();
                    BlockPos pos = BlockPos.ORIGIN;
                    if (client.player != null) {
                        pos = new BlockPos((int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()) - 1, (int) Math.floor(client.player.getZ()));
                    }
                    waypointToExpand = WaypointActions.createWaypoint(MainWidget.activePackage, category, pos);
                    WaypointsTabContent tab = MainWidget.waypointsTab;
                    if (tab != null) tab.invalidate();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }

                private boolean isIn(double mx, double my, int x, int y, int width, int height) {
                    return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
                }

                public float getTotalHeight() {
                    if (collapsed) return height;
                    float result = height + 10;
                    if (waypoints.isEmpty()) return result + 34;
                    for (WaypointWidget waypoint : waypoints) {
                        result += waypoint.getTotalHeight() + 10;
                    }
                    return result;
                }
            }

            private static class WaypointWidget extends Widget {
                private static final int COLLAPSED_HEIGHT = 50;
                private static final int EXPANDED_HEIGHT = 413;

                final Waypoint waypoint;
                private boolean expanded = false;
                private boolean categoryExpanded = false;
                private WaypointTextInput nameInput;
                private WaypointTextInput xInput;
                private WaypointTextInput yInput;
                private WaypointTextInput zInput;

                public WaypointWidget(Waypoint waypoint, boolean expand) {
                    this.waypoint = waypoint;
                    expanded = expand;
                    if (waypoint != null) {
                        nameInput = new WaypointTextInput(waypoint.name == null ? "" : waypoint.name, "Waypoint name", this::applyName);
                        xInput = new WaypointTextInput(String.valueOf(waypoint.x), "X", ignored -> applyCoordinates());
                        yInput = new WaypointTextInput(String.valueOf(waypoint.y), "Y", ignored -> applyCoordinates());
                        zInput = new WaypointTextInput(String.valueOf(waypoint.z), "Z", ignored -> applyCoordinates());
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
                    int contentX = x + 15;
                    int contentY = y + COLLAPSED_HEIGHT + 18;
                    int inputHeight = 44;
                    int labelY = contentY + 24;
                    int fieldX = contentX + 155;
                    int fieldWidth = Math.max(240, width - 185);

                    ui.drawText("Text", contentX, labelY, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    setInputBounds(nameInput, fieldX, contentY, fieldWidth, inputHeight);

                    int coordsY = contentY + 62;
                    int coordFieldWidth = 170;
                    ui.drawText("Coordinates", contentX, coordsY + 24, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    setInputBounds(xInput, fieldX, coordsY, coordFieldWidth, inputHeight);
                    setInputBounds(yInput, fieldX + coordFieldWidth + 35, coordsY, coordFieldWidth, inputHeight);
                    setInputBounds(zInput, fieldX + (coordFieldWidth + 35) * 2, coordsY, coordFieldWidth, inputHeight);

                    int visibilityY = contentY + 124;
                    ui.drawText("Visibility", contentX, visibilityY + 24, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    int toggleGap = 12;
                    int toggleWidth = Math.max(190, (fieldWidth - toggleGap) / 2);
                    drawOverrideToggle(mouseX, mouseY, fieldX, visibilityY, toggleWidth, 40, "Name", waypoint.showNameOverride, waypoint.shouldShowName());
                    drawOverrideToggle(mouseX, mouseY, fieldX + toggleWidth + toggleGap, visibilityY, toggleWidth, 40, "Block", waypoint.showOverride, waypoint.shouldShowBlock());
                    drawOverrideToggle(mouseX, mouseY, fieldX, visibilityY + 48, toggleWidth, 40, "Distance", waypoint.showDistanceOverride, waypoint.shouldShowDistance());
                    drawOverrideToggle(mouseX, mouseY, fieldX + toggleWidth + toggleGap, visibilityY + 48, toggleWidth, 40, "Text see through", waypoint.seeThroughOverride, waypoint.shouldSeeThrough());

                    int categoryY = contentY + 232;
                    ui.drawText("Category", contentX, categoryY + 24, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.4f);
                    drawButton(ui, fieldX, categoryY, fieldWidth, 40, isIn(mouseX, mouseY, fieldX, categoryY, fieldWidth, 40), GOLD_DARK);
                    CustomColor categoryColor = waypoint.getCategory() == null ? CustomColor.fromHexString("FFFFFF") : waypoint.getCategory().color;
                    ui.drawText(categoryName, fieldX + 15, categoryY + 20, categoryColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);
                    ui.drawText(categoryExpanded ? "▲" : "▼", fieldX + fieldWidth - 24, categoryY + 20, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, 2.4f);

                    int categoryOptions = 0;
                    if (categoryExpanded) {
                        int optionY = categoryY + 45;
                        if (MainWidget.activePackage != null) {
                            for (WaypointCategory category : MainWidget.activePackage.categories) {
                                drawCategoryOption(mouseX, mouseY, fieldX, optionY, fieldWidth, 36, category);
                                optionY += 36;
                                categoryOptions++;
                            }
                        }
                    }

                    int actionsY = categoryY + 62 + categoryOptions * 36;
                    int actionGap = 15;
                    int actionW = Math.max(175, (fieldWidth - actionGap * 2) / 3);
                    drawSmallButton(mouseX, mouseY, fieldX, actionsY, actionW, 40, "Edit in World", false, GOLD_DARK);
                    drawSmallButton(mouseX, mouseY, fieldX + actionW + actionGap, actionsY, actionW, 40, "Duplicate", false, TOGGLE_ON);
                    drawSmallButton(mouseX, mouseY, fieldX + (actionW + actionGap) * 2, actionsY, actionW, 40, "Delete", false, ACCENT_RED);
                }

                private void drawSmallButton(int mouseX, int mouseY, int x, int y, int width, int height, String label, boolean selected, int accent) {
                    boolean hover = isIn(mouseX, mouseY, x, y, width, height);
                    drawButton(ui, x, y, width, height, hover, accent);
                    ui.drawCenteredText(label, x + width / 2f, y + height / 2f, CustomColor.fromInt(TEXT_LIGHT), 2.3f);
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

                private void drawOverrideToggle(int mouseX, int mouseY, int x, int y, int width, int height, String label, Boolean override, boolean effective) {
                    boolean enabled = override != null ? override : effective;
                    int color = enabled ? TOGGLE_ON : ACCENT_RED;
                    boolean hover = isIn(mouseX, mouseY, x, y, width, height);
                    ui.drawRect(x, y, width, height, CustomColor.fromInt(hover ? PARCHMENT_LIGHT : PARCHMENT));
                    ui.drawRect(x, y, 4, height, CustomColor.fromInt(color));
                    String state = override == null ? "Category (" + (effective ? "On" : "Off") + ")" : (override ? "On" : "Off");
                    ui.drawCenteredText(label + ": " + state, x + width / 2f, y + height / 2f, CustomColor.fromInt(color), 2.2f);
                }

                private void drawCategoryOption(int mouseX, int mouseY, int x, int y, int width, int height, WaypointCategory category) {
                    boolean selected = waypoint.getCategory() == category;
                    boolean hover = isIn(mouseX, mouseY, x, y, width, height);
                    drawConfigRow(ui, x, y, width, height, hover, selected, category == null ? GOLD_DARK : category.color.asInt());
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

                        if (isIn(mx, my, x + 25 + 135, y + COLLAPSED_HEIGHT + 18 + 232, Math.max(240, width - 185), 40)) {
                            categoryExpanded = !categoryExpanded;
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }

                        if (categoryExpanded && clickCategoryOption(mx, my)) return true;

                        int contentX = x + 25;
                        int fieldX = contentX + 135;
                        int fieldWidth = Math.max(240, width - 185);
                        int toggleGap = 12;
                        int toggleWidth = Math.max(190, (fieldWidth - toggleGap) / 2);
                        int visibilityY = y + COLLAPSED_HEIGHT + 18 + 124;
                        int categoryY = y + COLLAPSED_HEIGHT + 18 + 232;
                        int categoryOptions = categoryExpanded && MainWidget.activePackage != null ? MainWidget.activePackage.categories.size() : 0;
                        int actionsY = categoryY + 62 + categoryOptions * 36;
                        int actionGap = 15;
                        int actionW = Math.max(175, (fieldWidth - actionGap * 2) / 3);
                        if (isIn(mx, my, fieldX, actionsY, actionW, 40)) {
                            WaypointEditMode.editWaypoint(MainWidget.activePackage, waypoint);
                            return true;
                        }
                        if (isIn(mx, my, fieldX + actionW + actionGap, actionsY, actionW, 40)) {
                            waypointToExpand = WaypointActions.duplicateWaypoint(MainWidget.activePackage, waypoint);
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            WaypointsTabContent tab = MainWidget.waypointsTab;
                            if (tab != null) tab.invalidate();
                            return true;
                        }
                        if (isIn(mx, my, fieldX + (actionW + actionGap) * 2, actionsY, actionW, 40)) {
                            WaypointActions.deleteWaypoint(MainWidget.activePackage, waypoint);
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            WaypointsTabContent tab = MainWidget.waypointsTab;
                            if (tab != null) tab.invalidate();
                            return true;
                        }
                        if (isIn(mx, my, fieldX, visibilityY, toggleWidth, 40)) {
                            WaypointActions.setWaypointVisibility(waypoint, WaypointActions.VisibilityTarget.NAME, nextOverride(waypoint.showNameOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }
                        if (isIn(mx, my, fieldX + toggleWidth + toggleGap, visibilityY, toggleWidth, 40)) {
                            WaypointActions.setWaypointVisibility(waypoint, WaypointActions.VisibilityTarget.BLOCK, nextOverride(waypoint.showOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }
                        if (isIn(mx, my, fieldX, visibilityY + 48, toggleWidth, 40)) {
                            WaypointActions.setWaypointVisibility(waypoint, WaypointActions.VisibilityTarget.DISTANCE, nextOverride(waypoint.showDistanceOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            return true;
                        }
                        if (isIn(mx, my, fieldX + toggleWidth + toggleGap, visibilityY + 48, toggleWidth, 40)) {
                            WaypointActions.setWaypointVisibility(waypoint, WaypointActions.VisibilityTarget.SEE_THROUGH, nextOverride(waypoint.seeThroughOverride));
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                    int optionY = y + COLLAPSED_HEIGHT + 18 + 232 + 45;

                    if (MainWidget.activePackage == null) return false;
                    for (WaypointCategory category : MainWidget.activePackage.categories) {
                        if (isIn(mx, my, fieldX, optionY, fieldWidth, 36)) {
                            waypoint.setCategory(category);
                            categoryExpanded = false;
                            waypointToExpand = waypoint;
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            WaypointActions.setWaypointCategory(waypoint, category);
                            WaypointsTabContent tab = MainWidget.waypointsTab;
                            if (tab != null) tab.invalidate();
                            return true;
                        }
                        optionY += 36;
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
                    WaypointActions.updateWaypoint(waypoint, name, null, null, null);
                }

                private void applyCoordinates() {
                    if (waypoint == null) return;
                    try {
                        WaypointActions.updateWaypoint(waypoint, waypoint.name,
                                Integer.parseInt(xInput.getInput().trim()),
                                Integer.parseInt(yInput.getInput().trim()),
                                Integer.parseInt(zInput.getInput().trim()));
                    } catch (NumberFormatException ignored) {}
                }

                public int getTotalHeight() {
                    if (!expanded) return COLLAPSED_HEIGHT;
                    int categoryOptions = categoryExpanded && MainWidget.activePackage != null ? MainWidget.activePackage.categories.size() : 0;
                    return EXPANDED_HEIGHT + categoryOptions * 36;
                }

                private static class WaypointTextInput extends TextInputWidget {
                    private WaypointTextInput(String input, String placeholder, java.util.function.Consumer<String> changeConsumer) {
                        super(0, 0, 0, 0, 12, 11, 2.6f);
                        setInput(input);
                        setPlaceholder(placeholder);
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
            private static final int LIST_TOP_PADDING = 64;
            private ActionButtonWidget addCategoryButton;
            private WaypointScrollBarWidget scrollBar;
            private final List<CategoryRowWidget> rows = new ArrayList<>();
            private int draggedIndex = -1;
            private int categoryUnderMouseIndex = -1;

            @Override
            public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
                this.ui = ui;
                if(!visible || this.ui == null) return;
                hovered = contains(mouseX, mouseY);
                updateValues();
                drawBackground(ctx, mouseX, mouseY, tickDelta);
                drawContent(ctx, mouseX, mouseY, tickDelta);

                if (addCategoryButton != null) addCategoryButton.draw(ctx, mouseX, mouseY, tickDelta, ui);
                try {
                    ctx.enableScissor(
                            (int) ui.sx(x),
                            (int) ui.sy(listTop()),
                            (int) ui.sx(x + width),
                            (int) ui.sy(listBottom())
                    );
                } catch (Exception ignored) {}
                for (CategoryRowWidget row : rows) {
                    row.draw(ctx, mouseX, mouseY, tickDelta, ui);
                }
                try {
                    ctx.disableScissor();
                } catch (Exception ignored) {}
                if (scrollBar != null) scrollBar.draw(ctx, mouseX, mouseY, tickDelta, ui);

                drawForeground(ctx, mouseX, mouseY, tickDelta);
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if (addCategoryButton == null) {
                    addCategoryButton = new ActionButtonWidget("New Category", this::addCategory);
                    addChild(addCategoryButton);
                }
                if (scrollBar == null) {
                    scrollBar = new WaypointScrollBarWidget(ScrollType.Categories);
                    addChild(scrollBar);
                }

                addCategoryButton.setBounds(x + 30, y + 10, 260, 44);
                int listTop = listTop();
                int listBottom = listBottom();
                float viewportHeight = Math.max(0, listBottom - listTop);
                float maxOffset = Math.max(0, calculateTotalHeight() - viewportHeight);
                float currentTarget = Math.clamp(targetOffsets.getOrDefault(ScrollType.Categories, 0f), 0f, maxOffset);
                targetOffsets.put(ScrollType.Categories, currentTarget);
                float currentActual = actualOffsets.getOrDefault(ScrollType.Categories, 0f);
                float diff = currentTarget - currentActual;
                if (Math.abs(diff) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffsets.put(ScrollType.Categories, currentTarget);
                else actualOffsets.put(ScrollType.Categories, currentActual + diff * 0.3f * tickDelta);

                if(mainAreaWidget != null) {
                    scrollBar.setBounds(
                            x + width - 27,
                            listTop,
                            20,
                            (int) viewportHeight
                    );
                }
                scrollBar.setVisible(maxOffset > 0);

                if (activePackage != null && rows.isEmpty()) rebuildRows();

                int rowY = y + 75 - Math.round(actualOffsets.getOrDefault(ScrollType.Categories, 0f));
                if (activePackage == null || activePackage.categories.isEmpty()) {
                    ui.drawText("No categories", x + 35, rowY, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.6f);
                    return;
                }

                updateCategoryDragTarget(mouseX, mouseY);

                for (CategoryRowWidget row : rows) {
                    int rowHeight = row.getTotalHeight();
                    row.setBounds(x + 35, rowY, width - 80, rowHeight);
                    row.setVisible(rowY + rowHeight >= listTop && rowY <= listBottom);
                    rowY += rowHeight + 10;
                }
            }

            @Override
            protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                drawCategoryDropLine();
                for (CategoryRowWidget row : rows) {
                    if (row.isDragging) {
                        row.drawDraggedPreview(ctx, mouseX, mouseY);
                        break;
                    }
                }
            }

            private void drawCategoryDefaultToggle(int mouseX, int mouseY, int x, int y, int width, int height, String label, boolean enabled) {
                int color = enabled ? TOGGLE_ON : ACCENT_RED;
                boolean hover = isIn(mouseX, mouseY, x, y, width, height);
                ui.drawRect(x, y, width, height, CustomColor.fromInt(hover ? PARCHMENT_LIGHT : PARCHMENT));
                ui.drawRect(x, y, 4, height, CustomColor.fromInt(color));
                ui.drawCenteredText(label + ": " + (enabled ? "On" : "Off"), x + width / 2f, y + height / 2f, CustomColor.fromInt(color), label.length() > 12 ? 2.0f : 2.2f);
            }

            private boolean isIn(double mx, double my, int x, int y, int width, int height) {
                return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
            }

            private void saveCategoryDefaults() {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                MainWidget.invalidateAllTabs();
            }

            private void addCategory() {
                if (activePackage == null) return;

                WaypointActions.createCategory(activePackage, "New Category");
                invalidate();
                saveCategoryDefaults();
            }

            private void rebuildRows() {
                rows.clear();
                draggedIndex = -1;
                categoryUnderMouseIndex = -1;
                clearChildren();
                addChild(addCategoryButton);
                if (activePackage == null) return;
                WaypointData.resolveWaypointCategories(activePackage);
                for (WaypointCategory category : activePackage.categories) {
                    CategoryRowWidget row = new CategoryRowWidget(category);
                    rows.add(row);
                    addChild(row);
                }
                if (scrollBar != null) addChild(scrollBar);
            }

            @Override
            public void invalidate() {
                rows.clear();
                draggedIndex = -1;
                categoryUnderMouseIndex = -1;
                clearChildren();
                if (addCategoryButton != null) addChild(addCategoryButton);
                if (scrollBar != null) addChild(scrollBar);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!isInViewport(mx, my)) return false;
                return super.mouseClicked(mx, my, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                for (CategoryRowWidget row : rows) {
                    if (row.clicked) return row.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
                }
                return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                for (CategoryRowWidget row : new ArrayList<>(rows)) {
                    row.mouseReleased(mx, my, button);
                }
                draggedIndex = -1;
                categoryUnderMouseIndex = -1;
                return super.mouseReleased(mx, my, button);
            }

            private boolean isInViewport(double mx, double my) {
                if (mainAreaWidget == null || ui == null) return false;
                return mx >= ui.sx(x)
                        && my >= ui.sy(mainAreaWidget.getY())
                        && mx < ui.sx(x + width)
                        && my < ui.sy(mainAreaWidget.getY() + mainAreaWidget.getHeight());
            }

            private boolean isInListViewport(double mx, double my) {
                if (mainAreaWidget == null || ui == null) return false;
                return mx >= ui.sx(x)
                        && my >= ui.sy(listTop())
                        && mx < ui.sx(x + width)
                        && my < ui.sy(listBottom());
            }

            private int listTop() {
                return mainAreaWidget == null ? y + LIST_TOP_PADDING : (int) mainAreaWidget.getY() + LIST_TOP_PADDING;
            }

            private int listBottom() {
                return mainAreaWidget == null ? y + height : (int) (mainAreaWidget.getY() + mainAreaWidget.getHeight());
            }

            @Override
            public float calculateTotalHeight() {
                if (activePackage == null) return 75;
                if (!rows.isEmpty()) {
                    int total = 75;
                    for (CategoryRowWidget row : rows) total += row.getTotalHeight() + 10;
                    return total;
                }
                return 75 + activePackage.categories.size() * 106;
            }

            private void updateCategoryDragTarget(int mouseX, int mouseY) {
                categoryUnderMouseIndex = -1;
                if (draggedIndex < 0 || rows.isEmpty() || !isInListViewport(mouseX, mouseY)) return;

                float mouseContentY = mouseY * ui.getScaleFactorF() + actualOffsets.getOrDefault(ScrollType.Categories, 0f);
                int currentY = y + 75;
                int insertionIndex = 0;
                for (CategoryRowWidget row : rows) {
                    int rowHeight = row.getTotalHeight();
                    if (mouseContentY > currentY + rowHeight / 2f) insertionIndex++;
                    else break;
                    currentY += rowHeight + 10;
                }

                insertionIndex = Math.clamp(insertionIndex, 0, rows.size());
                int targetIndex = insertionIndex > draggedIndex ? insertionIndex - 1 : insertionIndex;
                if (targetIndex != draggedIndex) categoryUnderMouseIndex = insertionIndex;
            }

            private void drawCategoryDropLine() {
                if (draggedIndex < 0 || categoryUnderMouseIndex < 0 || rows.isEmpty()) return;

                float offset = actualOffsets.getOrDefault(ScrollType.Categories, 0f);
                int lineY;
                if (categoryUnderMouseIndex == 0) {
                    lineY = y + 75 - Math.round(offset) - 5;
                } else if (categoryUnderMouseIndex >= rows.size()) {
                    int currentY = y + 75 - Math.round(offset);
                    for (CategoryRowWidget row : rows) currentY += row.getTotalHeight() + 10;
                    lineY = currentY - 5;
                } else {
                    int currentY = y + 75 - Math.round(offset);
                    for (int i = 0; i < categoryUnderMouseIndex; i++) {
                        currentY += rows.get(i).getTotalHeight() + 10;
                    }
                    lineY = currentY - 5;
                }

                if (mainAreaWidget == null || lineY < listTop() || lineY > listBottom()) return;
                ui.drawRect(x + 35, lineY - 1, width - 80, 2, CustomColor.fromInt(GOLD));
            }

            private class CategoryRowWidget extends Widget {
                private final WaypointCategory category;
                private final ScreenTextInput nameInput;
                private final ColorPickerWidget colorPicker;
                private boolean clicked = false;
                private boolean isDragging = false;
                private float clickX = -1;
                private float clickY = -1;

                private CategoryRowWidget(WaypointCategory category) {
                    this.category = category;
                    nameInput = new ScreenTextInput(category.name == null ? "" : category.name, "Category name", 2.6f, value -> {
                        WaypointActions.renameCategory(activePackage, category, value);
                        if (waypointsTab != null) waypointsTab.invalidate();
                    });
                    nameInput.setDisabledTooltip(UNCATEGORIZED_CATEGORY_TOOLTIP);
                    nameInput.setBackgroundColor(CustomColor.fromInt(PARCHMENT));
                    nameInput.setTextOffset(14, 16);
                    colorPicker = new ColorPickerWidget(
                            () -> category == null || category.color == null ? 0xFFFFFF : category.color.asInt() & 0xFFFFFF,
                            rgb -> {
                                WaypointActions.setCategoryColor(category, rgb);
                                if (waypointsTab != null) waypointsTab.invalidate();
                            },
                            () -> category == null ? 1f : category.alpha,
                            alpha -> {
                                WaypointActions.setCategoryAlpha(category, alpha);
                                if (waypointsTab != null) waypointsTab.invalidate();
                            });
                    addChild(nameInput);
                    addChild(colorPicker);
                }

                @Override
                public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
                    int effectiveMouseX = isInListViewport(mouseX, mouseY) ? mouseX : -1;
                    int effectiveMouseY = isInListViewport(mouseX, mouseY) ? mouseY : -1;
                    super.draw(ctx, effectiveMouseX, effectiveMouseY, tickDelta, ui);
                }

                @Override
                protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    boolean uncategorized = WaypointData.isUncategorizedCategory(category);
                    drawConfigRow(ui, x, y, width, 148, hovered, false, category.color == null ? GOLD_DARK : category.color.asInt());
                    drawMoveHandle(mouseX, mouseY);
                    nameInput.setBounds(x + 58, y + 46, 430, 56);
                    nameInput.setEnabled(!uncategorized);
                    nameInput.setVisible(true);

                    colorPicker.setBounds(x + 518, y + 54, 227, 40);

                    int toggleX = x + width - 630;
                    int toggleW = 232;
                    int toggleGap = 12;
                    drawCategoryDefaultToggle(mouseX, mouseY, toggleX, y + 26, toggleW, 44, "Block", category.showBlockByDefault);
                    drawCategoryDefaultToggle(mouseX, mouseY, toggleX + toggleW + toggleGap, y + 26, toggleW, 44, "Text", category.showNameByDefault);
                    drawCategoryDefaultToggle(mouseX, mouseY, toggleX, y + 78, toggleW, 44, "Distance", category.showDistanceByDefault);
                    drawCategoryDefaultToggle(mouseX, mouseY, toggleX + toggleW + toggleGap, y + 78, toggleW, 44, "Text see through", category.showSeeThroughByDefault);
                    drawButton(ui, x + width - 128, y + 52, 112, 44, isIn(mouseX, mouseY, x + width - 128, y + 52, 112, 44), uncategorized ? BORDER_DARK : ACCENT_RED);
                    ui.drawCenteredText("Delete", x + width - 72, y + 74, uncategorized ? CustomColor.fromInt(TEXT_DIM) : CustomColor.fromInt(TEXT_LIGHT), 2.2f);
                }

                @Override
                public boolean mouseClicked(double mx, double my, int button) {
                    if (!visible || !enabled) return false;
                    if (!isInListViewport(mx, my)) return false;
                    for (int i = children.size() - 1; i >= 0; i--) {
                        if (children.get(i).mouseClicked(mx, my, button)) return true;
                    }
                    if (button != 0 || !contains((int) mx, (int) my)) return false;
                    if (isIn(mx, my, x + 9, y + 57, 34, 34)) {
                        clicked = true;
                        clickX = (float) mx;
                        clickY = (float) my;
                        return true;
                    }
                    int toggleX = x + width - 630;
                    int toggleW = 232;
                    int toggleGap = 12;
                    if (isIn(mx, my, toggleX, y + 26, toggleW, 44)) {
                        WaypointActions.setCategoryDefault(category, WaypointActions.VisibilityTarget.BLOCK, !category.showBlockByDefault);
                        saveCategoryDefaults();
                        return true;
                    }
                    if (isIn(mx, my, toggleX + toggleW + toggleGap, y + 26, toggleW, 44)) {
                        WaypointActions.setCategoryDefault(category, WaypointActions.VisibilityTarget.NAME, !category.showNameByDefault);
                        saveCategoryDefaults();
                        return true;
                    }
                    if (isIn(mx, my, toggleX, y + 78, toggleW, 44)) {
                        WaypointActions.setCategoryDefault(category, WaypointActions.VisibilityTarget.DISTANCE, !category.showDistanceByDefault);
                        saveCategoryDefaults();
                        return true;
                    }
                    if (isIn(mx, my, toggleX + toggleW + toggleGap, y + 78, toggleW, 44)) {
                        WaypointActions.setCategoryDefault(category, WaypointActions.VisibilityTarget.SEE_THROUGH, !category.showSeeThroughByDefault);
                        saveCategoryDefaults();
                        return true;
                    }
                    if (!WaypointData.isUncategorizedCategory(category) && isIn(mx, my, x + width - 128, y + 52, 112, 44)) {
                        String categoryName = category.name == null || category.name.isBlank() ? "Category" : category.name;
                        openConfirm("Delete Category?", "Delete \"" + categoryName + "\"? Its waypoints will move to uncategorized.", () -> {
                            WaypointActions.deleteCategory(activePackage, category);
                            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                            MainWidget.invalidateAllTabs();
                        });
                        return true;
                    }
                    return true;
                }

                @Override
                public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        if (children.get(i).mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
                    }
                    if (!clicked || button != 0) return false;
                    if (Math.abs(mouseX - clickX) > 2 || Math.abs(mouseY - clickY) > 2) isDragging = true;
                    draggedIndex = rows.indexOf(this);
                    return true;
                }

                @Override
                public boolean mouseReleased(double mx, double my, int button) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        if (children.get(i).mouseReleased(mx, my, button)) return true;
                    }
                    if (isDragging && draggedIndex >= 0 && categoryUnderMouseIndex >= 0 && activePackage != null) {
                        int insertionIndex = Math.clamp(categoryUnderMouseIndex, 0, activePackage.categories.size());
                        int targetIndex = insertionIndex > draggedIndex ? insertionIndex - 1 : insertionIndex;

                        if (targetIndex != draggedIndex) {
                            WaypointCategory moved = activePackage.categories.remove(draggedIndex);
                            activePackage.categories.add(targetIndex, moved);
                            WaypointData.save();
                            rebuildRows();
                            if (waypointsTab != null) waypointsTab.invalidate();
                        }
                    }

                    clicked = false;
                    isDragging = false;
                    return false;
                }

                private void drawMoveHandle(int mouseX, int mouseY) {
                    int handleX = x + 9;
                    int handleY = y + 57;
                    int handleSize = 34;
                    boolean handleHovered = isIn(mouseX, mouseY, handleX, handleY, handleSize, handleSize);
                    ui.drawRect(handleX - 2, handleY - 2, handleSize + 4, handleSize + 4, CustomColor.fromInt(isDragging || handleHovered ? PARCHMENT_HOVER : PARCHMENT));
                    ui.drawImage(MOVE_ICON, handleX - 1, handleY - 1, handleSize, handleSize, CustomColor.fromInt(isDragging ? 0xFFFFE36A : TEXT_DIM));
                }

                private void drawDraggedPreview(DrawContext ctx, int mouseX, int mouseY) {
                    float dragX = mouseX * ui.getScaleFactorF();
                    float dragY = mouseY * ui.getScaleFactorF();
                    drawConfigRow(ui, dragX, dragY, width, 148, true, true, category.color == null ? GOLD_DARK : category.color.asInt());
                    ui.drawImage(MOVE_ICON, dragX + 5, dragY + 57, 34, 34, CustomColor.fromInt(0xFFFFE36A));
                    ui.drawText(category.name == null ? "Category" : category.name, dragX + 58, dragY + 74, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 2.6f);
                }

                @Override
                protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    if (!WaypointData.isUncategorizedCategory(category)) return;
                    if (!isIn(mouseX, mouseY, x + width - 128, y + 52, 112, 44)) return;
                    TextInputWidget.drawFittingTooltip(ctx, List.of(Text.of(UNCATEGORIZED_CATEGORY_TOOLTIP)), mouseX, mouseY);
                }

                private int getTotalHeight() {
                    return colorPicker.isOpen() ? colorPicker.getY() - y + colorPicker.getExpandedHeight() + 10 : 164;
                }
            }
        }

        private static class SettingsTabContent extends TabContentWidget {
            private static final int DESCRIPTION_MAX_LENGTH = 160;

            private ScreenTextInput nameInput;
            private ScreenTextInput descriptionInput;
            private WaypointPackage inputPackage;

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if (activePackage == null) {
                    ui.drawText("Select a package first", x + 35, y + 35, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 3f);
                    return;
                }
                syncInputs();

                int contentX = x + 35;
                int fieldX = x + 240;
                int fieldW = Math.max(360, width - (fieldX - x) - 35);
                ui.drawText("Package", contentX, y + 35, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 3f);

                ui.drawText("Name", contentX, y + 95, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.5f);
                nameInput.setBounds(fieldX, y + 82, fieldW, 54);
                ui.drawText(nameInput.getInput().length() + "/" + WaypointActions.MAX_PACKAGE_NAME_LENGTH, fieldX + fieldW, y + 140, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 2f);

                ui.drawText("Description", contentX, y + 170, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.5f);
                descriptionInput.setBounds(fieldX, y + 158, fieldW, 112);
                ui.drawText(descriptionInput.getInput().length() + "/" + DESCRIPTION_MAX_LENGTH, fieldX + fieldW, y + 278, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 2f);

                ui.drawText("ID", contentX, y + 315, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.4f);
                ui.drawText(activePackage.id == null ? "" : activePackage.id, fieldX, y + 315, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.3f);
                ui.drawText("Version", contentX, y + 350, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.4f);
                ui.drawText(String.valueOf(activePackage.packageVersion), fieldX, y + 350, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.3f);
                ui.drawText("Contents", contentX, y + 385, CustomColor.fromInt(TEXT_DIM), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.4f);
                ui.drawText(activePackage.waypoints.size() + " waypoints, " + activePackage.categories.size() + " categories", fieldX, y + 385, CustomColor.fromInt(TEXT_LIGHT), HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2.3f);

                drawSettingsButton(mouseX, mouseY, contentX, y + 445, 180, 44, activePackage.enabled ? "Enabled" : "Disabled", activePackage.enabled ? TOGGLE_ON : TOGGLE_OFF);
                drawSettingsButton(mouseX, mouseY, contentX + 200, y + 445, 180, 44, "Duplicate", TOGGLE_ON);
                drawSettingsButton(mouseX, mouseY, contentX + 400, y + 445, 180, 44, "Export", GOLD_DARK);
                drawSettingsButton(mouseX, mouseY, contentX + 600, y + 445, 180, 44, "Delete", ACCENT_RED);
            }

            @Override
            public float calculateTotalHeight() {
                return 525;
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (!visible || !enabled) return false;
                for (int i = children.size() - 1; i >= 0; i--) {
                    if (children.get(i).mouseClicked(mx, my, button)) return true;
                }
                if (button != 0 || activePackage == null || !contains((int) mx, (int) my)) return false;

                int contentX = x + 35;
                int buttonY = y + 445;
                if (isIn(mx, my, contentX, buttonY, 180, 44)) {
                    WaypointActions.setPackageEnabled(activePackage, !activePackage.enabled);
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                if (isIn(mx, my, contentX + 200, buttonY, 180, 44)) {
                    WaypointPackage copy = WaypointActions.duplicatePackage(activePackage);
                    activePackage = copy;
                    WaypointData.INSTANCE.activePackage = copy;
                    if (sideBarWidget != null) sideBarWidget.rebuildPackageWidgetsFromData();
                    MainWidget.invalidateAllTabs();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                if (isIn(mx, my, contentX + 400, buttonY, 180, 44)) {
                    try {
                        MinecraftClient.getInstance().keyboard.setClipboard(WaypointData.gson.toJson(activePackage));
                        showFeedback("Exported package \"" + activePackage.name + "\" to clipboard.", true, mx, my, ui);
                    } catch (Exception e) {
                        showFeedback("Failed to export package.", false, mx, my, ui);
                    }
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                if (isIn(mx, my, contentX + 600, buttonY, 180, 44)) {
                    WaypointPackage deleted = activePackage;
                    String packageName = deleted.name == null || deleted.name.isBlank() ? "Package" : deleted.name;
                    openConfirm("Delete Package?", "Delete \"" + packageName + "\" and all of its waypoints?", () -> {
                        WaypointActions.deletePackage(deleted);
                        activePackage = WaypointData.INSTANCE.packages.isEmpty() ? null : WaypointData.INSTANCE.packages.getFirst();
                        WaypointData.INSTANCE.activePackage = activePackage;
                        if (sideBarWidget != null) sideBarWidget.rebuildPackageWidgetsFromData();
                        MainWidget.invalidateAllTabs();
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    }, () -> new WaypointScreen(activePackage, null));
                    return true;
                }
                return true;
            }

            private void syncInputs() {
                if (nameInput == null) {
                    nameInput = new ScreenTextInput("", "Package name", 2.6f, value -> {
                        if (activePackage == null) return;
                        WaypointActions.renamePackage(activePackage, value);
                        if (sideBarWidget != null) sideBarWidget.rebuildPackageWidgetsFromData();
                    });
                    descriptionInput = new ScreenTextInput("", "Package description", 2.6f, value -> {
                        if (activePackage == null) return;
                        WaypointActions.setPackageDescription(activePackage, value);
                    });
                    nameInput.setMaxLength(WaypointActions.MAX_PACKAGE_NAME_LENGTH);
                    descriptionInput.setWrapText(true);
                    descriptionInput.setMaxLength(DESCRIPTION_MAX_LENGTH);
                    addChild(nameInput);
                    addChild(descriptionInput);
                }
                if (inputPackage == activePackage) return;
                inputPackage = activePackage;
                nameInput.setInputAndMoveCursorToEnd(activePackage.name == null ? "" : activePackage.name);
                String description = activePackage.description == null ? "" : activePackage.description;
                descriptionInput.setInputAndMoveCursorToEnd(description);
                if (!descriptionInput.getInput().equals(description)) {
                    WaypointActions.setPackageDescription(activePackage, descriptionInput.getInput());
                }
            }

            private void drawSettingsButton(int mouseX, int mouseY, int x, int y, int width, int height, String text, int accent) {
                drawConfigRow(ui, x, y, width, height, isIn(mouseX, mouseY, x, y, width, height), false, accent);
                ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromInt(TEXT_LIGHT), 2.5f);
            }

            private boolean isIn(double mx, double my, int x, int y, int width, int height) {
                return mx >= ui.sx(x) && my >= ui.sy(y) && mx < ui.sx(x) + ui.sw(width) && my < ui.sy(y) + ui.sh(height);
            }

            @Override
            public void invalidate() {
                inputPackage = null;
            }
        }

        private static class WaypointScrollBarWidget extends Widget {
            private final ScrollType scrollType;
            private final int buttonHeight = 40;
            private boolean held = false;

            private WaypointScrollBarWidget(ScrollType scrollType) {
                this.scrollType = scrollType;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                float contentHeight = activeTabWidget.calculateTotalHeight();
                float viewportHeight = mainAreaWidget.getHeight();
                float maxOffset = Math.max(0, contentHeight - viewportHeight);

                if (maxOffset <= 0) return;

                ui.drawSliderBackground(x, y, width, height);

                float actualOffset = actualOffsets.getOrDefault(scrollType, 0f);
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

                    targetOffsets.put(scrollType, newOffset);
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