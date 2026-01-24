package julianh06.wynnextras.features.waypoints;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class WaypointScreen extends Screen {
    // Wynncraft Medieval Theme Colors
    private static final int BG_DARK = 0xFF1a1410;
    private static final int BG_MEDIUM = 0xFF2d2419;
    private static final int BG_LIGHT = 0xFF3d3222;
    private static final int PARCHMENT = 0xFF4a3c2a;
    private static final int PARCHMENT_LIGHT = 0xFF5c4d3a;
    private static final int GOLD = 0xFFc9a227;
    private static final int GOLD_DARK = 0xFF8b7019;
    private static final int GOLD_LIGHT = 0xFFe8c252;
    private static final int TEXT_LIGHT = 0xFFe8dcc8;
    private static final int TEXT_DIM = 0xFF9a8b70;
    private static final int BORDER_DARK = 0xFF1a1410;
    private static final int BORDER_LIGHT = 0xFF5c4a35;
    private static final int SUCCESS_GREEN = 0xFF4a8c3a;
    private static final int FAIL_RED = 0xFFa83232;
    private static final int ACCENT_BLUE = 0xFF3a6a9c;

    // View state
    private enum ViewState { PACKAGES, WAYPOINTS, CATEGORIES }
    private ViewState currentView = ViewState.PACKAGES;

    // Scroll
    private double currentScrollOffset = 0;
    private double maxScroll = 0;
    private static final int HEADER_HEIGHT = 60;
    private static final int ITEM_HEIGHT_PACKAGE = 90;
    private static final int ITEM_HEIGHT_WAYPOINT = 70;
    private static final int ITEM_HEIGHT_CATEGORY = 50;

    // Input state
    private int editingPackageIndex = -1;
    private int editingWaypointIndex = -1;
    private int editingCategoryIndex = -1;
    private String editingField = null; // "name", "x", "y", "z", "catName"
    private String editBuffer = "";
    private int cursorPos = 0;
    private long lastBlink = 0;
    private boolean cursorVisible = true;

    // Category filter - use the static one for compatibility

    // Textures for icons
    private final Identifier checkboxActive = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxactive.png");
    private final Identifier checkboxInactive = Identifier.of("wynnextras", "textures/gui/waypoints/checkboxinactive.png");

    public static List<String> currentPlayers = new ArrayList<>();

    // Legacy static fields for compatibility with old element classes
    public static int mouseX = 0;
    public static int mouseY = 0;
    public static int scaleFactor = 1;
    public static int scrollOffset = 0;
    public static boolean clickWhileExpanded = false;
    public static boolean inMainScreen = true;
    static List<WaypointElement> elements = new ArrayList<>();
    static List<CategoryElement> categories = new ArrayList<>();
    static List<PackageElement> packageElements = new ArrayList<>();
    public static List<WaypointCategory> activeCategories = new ArrayList<>();
    public static julianh06.wynnextras.utils.overlays.EasyDropdown categoryDropdown = null;

    public WaypointScreen() {
        super(Text.of("Waypoints"));
    }

    @Override
    protected void init() {
        currentScrollOffset = 0;
        currentView = ViewState.PACKAGES;
        WaypointData.INSTANCE.activePackage = null;
        activeCategories.clear();
        clearEditing();
    }

    private void clearEditing() {
        editingPackageIndex = -1;
        editingWaypointIndex = -1;
        editingCategoryIndex = -1;
        editingField = null;
        editBuffer = "";
        cursorPos = 0;
    }

    private void updateMaxScroll() {
        int itemCount = 0;
        int itemHeight = ITEM_HEIGHT_PACKAGE;

        switch (currentView) {
            case PACKAGES -> {
                itemCount = WaypointData.INSTANCE.packages.size() + 1; // +1 for add button
                itemHeight = ITEM_HEIGHT_PACKAGE;
            }
            case WAYPOINTS -> {
                if (WaypointData.INSTANCE.activePackage != null) {
                    itemCount = (int) WaypointData.INSTANCE.activePackage.waypoints.stream()
                            .filter(w -> w.getCategory() == null || activeCategories.contains(w.getCategory()))
                            .count() + 1;
                }
                itemHeight = ITEM_HEIGHT_WAYPOINT;
            }
            case CATEGORIES -> {
                if (WaypointData.INSTANCE.activePackage != null) {
                    itemCount = WaypointData.INSTANCE.activePackage.categories.size() + 1;
                }
                itemHeight = ITEM_HEIGHT_CATEGORY;
            }
        }

        int totalHeight = itemCount * (itemHeight + 5);
        int visibleHeight = this.height - HEADER_HEIGHT - 30;
        maxScroll = Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        context.fill(0, 0, this.width, this.height, BG_DARK);

        int panelWidth = Math.min((int)(this.width * 0.85), 600);
        int panelX = (this.width - panelWidth) / 2;

        // Main panel
        drawParchmentPanel(context, panelX - 10, 10, panelWidth + 20, this.height - 20);

        // Header
        drawHeader(context, panelX, 15, panelWidth, HEADER_HEIGHT - 10, mouseX, mouseY);

        // Content
        int listTop = HEADER_HEIGHT + 10;
        int listBottom = this.height - 30;
        context.enableScissor(panelX, listTop, panelX + panelWidth, listBottom);

        switch (currentView) {
            case PACKAGES -> renderPackageList(context, panelX, listTop, panelWidth, mouseX, mouseY);
            case WAYPOINTS -> renderWaypointList(context, panelX, listTop, panelWidth, mouseX, mouseY);
            case CATEGORIES -> renderCategoryList(context, panelX, listTop, panelWidth, mouseX, mouseY);
        }

        context.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            drawScrollbar(context, panelX + panelWidth - 8, listTop, listBottom - listTop);
        }

        // Footer hint
        context.drawCenteredTextWithShadow(this.textRenderer, "Press ESC to close", this.width / 2, this.height - 18, TEXT_DIM);

        updateMaxScroll();
    }

    private void drawParchmentPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, BG_MEDIUM);
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, BG_LIGHT);
        context.fill(x, y, x + width, y + 2, BORDER_DARK);
        context.fill(x, y + height - 2, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 2, y + height, BORDER_DARK);
        context.fill(x + width - 2, y, x + width, y + height, BORDER_DARK);
        context.fill(x + 2, y + 2, x + width - 2, y + 3, BORDER_LIGHT);
        context.fill(x + 2, y + 2, x + 3, y + height - 2, BORDER_LIGHT);
    }

    private void drawHeader(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        context.fill(x, y, x + width, y + height, PARCHMENT);
        context.fill(x, y, x + width, y + 2, GOLD_DARK);
        context.fill(x + 10, y + height - 2, x + width - 10, y + height, GOLD_DARK);
        drawDiamond(context, x + 5, y + height / 2, 3, GOLD);
        drawDiamond(context, x + width - 5, y + height / 2, 3, GOLD);

        String title = switch (currentView) {
            case PACKAGES -> "Waypoint Packages";
            case WAYPOINTS -> WaypointData.INSTANCE.activePackage != null ? WaypointData.INSTANCE.activePackage.name : "Waypoints";
            case CATEGORIES -> "Edit Categories";
        };
        context.drawCenteredTextWithShadow(this.textRenderer, title, x + width / 2, y + 8, GOLD);

        // Navigation buttons
        if (currentView != ViewState.PACKAGES) {
            // Back button
            int backX = x + 5;
            int backY = y + height - 22;
            int backW = 70;
            int backH = 18;
            boolean backHovered = mouseX >= backX && mouseX < backX + backW && mouseY >= backY && mouseY < backY + backH;
            drawMedievalButton(context, backX, backY, backW, backH, "< Back", backHovered, ACCENT_BLUE);
        }

        if (currentView == ViewState.WAYPOINTS) {
            // Categories button
            int catX = x + width - 90;
            int catY = y + height - 22;
            int catW = 85;
            int catH = 18;
            boolean catHovered = mouseX >= catX && mouseX < catX + catW && mouseY >= catY && mouseY < catY + catH;
            drawMedievalButton(context, catX, catY, catW, catH, "Categories", catHovered, GOLD_DARK);
        }

        if (currentView == ViewState.PACKAGES) {
            // Import button
            int impX = x + 5;
            int impY = y + height - 22;
            int impW = 100;
            int impH = 18;
            boolean impHovered = mouseX >= impX && mouseX < impX + impW && mouseY >= impY && mouseY < impY + impH;
            drawMedievalButton(context, impX, impY, impW, impH, "Import Clipboard", impHovered, SUCCESS_GREEN);
        }
    }

    private void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    private void drawMedievalButton(DrawContext context, int x, int y, int width, int height, String text, boolean hovered, int accentColor) {
        int bgColor = hovered ? PARCHMENT_LIGHT : PARCHMENT;
        context.fill(x, y, x + width, y + height, bgColor);
        context.fill(x, y, x + width, y + 1, hovered ? GOLD : BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 1, y + height, hovered ? GOLD : BORDER_LIGHT);
        context.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
        context.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, accentColor);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + (height - 8) / 2, TEXT_LIGHT);
    }

    private void drawScrollbar(DrawContext context, int x, int y, int height) {
        int thumbHeight = Math.max(20, (int)(height * height / (height + maxScroll)));
        int thumbY = y + (int)((height - thumbHeight) * (currentScrollOffset / maxScroll));
        context.fill(x, y, x + 6, y + height, BORDER_DARK);
        context.fill(x + 1, y + 1, x + 5, y + height - 1, BG_MEDIUM);
        context.fill(x + 1, thumbY, x + 5, thumbY + thumbHeight, GOLD_DARK);
        context.fill(x + 2, thumbY + 1, x + 4, thumbY + thumbHeight - 1, GOLD);
    }

    // ==================== PACKAGE LIST ====================
    private void renderPackageList(DrawContext context, int panelX, int listTop, int panelWidth, int mouseX, int mouseY) {
        int y = listTop - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;

        for (int i = 0; i < WaypointData.INSTANCE.packages.size(); i++) {
            WaypointPackage pkg = WaypointData.INSTANCE.packages.get(i);
            if (y + ITEM_HEIGHT_PACKAGE > listTop - 20 && y < this.height - 30) {
                drawPackageItem(context, panelX + 15, y, itemWidth, ITEM_HEIGHT_PACKAGE - 5, pkg, i, mouseX, mouseY);
            }
            y += ITEM_HEIGHT_PACKAGE;
        }

        // Add new package button
        if (y + 30 > listTop - 20 && y < this.height - 30) {
            boolean hovered = mouseX >= panelX + 15 && mouseX < panelX + 15 + itemWidth && mouseY >= y && mouseY < y + 28;
            drawMedievalButton(context, panelX + 15, y, itemWidth, 28, "+ Add New Package", hovered, SUCCESS_GREEN);
        }
    }

    private void drawPackageItem(DrawContext context, int x, int y, int width, int height, WaypointPackage pkg, int index, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        context.fill(x, y, x + width, y + height, hovered ? PARCHMENT_LIGHT : PARCHMENT);
        context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 1, y + height, BORDER_LIGHT);
        context.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);

        // Enabled indicator
        int statusColor = pkg.enabled ? SUCCESS_GREEN : FAIL_RED;
        context.fill(x + 3, y + 5, x + 6, y + height - 5, statusColor);

        // Name (editable)
        int nameX = x + 12;
        int nameY = y + 8;
        if (editingPackageIndex == index && "name".equals(editingField)) {
            drawEditableField(context, nameX, nameY, width - 80, 16, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, pkg.name, nameX, nameY, TEXT_LIGHT);
        }

        // Info line
        int waypointCount = pkg.waypoints.size();
        int categoryCount = pkg.categories.size();
        String info = waypointCount + " waypoint" + (waypointCount != 1 ? "s" : "") + ", " +
                     categoryCount + " categor" + (categoryCount != 1 ? "ies" : "y");
        context.drawTextWithShadow(this.textRenderer, info, nameX, nameY + 14, TEXT_DIM);

        // Buttons row
        int btnY = y + height - 26;
        int btnH = 20;
        int btnW = 55;
        int btnSpacing = 5;

        // Enable/Disable button
        int enableX = x + 10;
        boolean enableHovered = mouseX >= enableX && mouseX < enableX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, enableX, btnY, btnW, btnH, pkg.enabled ? "Disable" : "Enable", enableHovered, pkg.enabled ? FAIL_RED : SUCCESS_GREEN);

        // Edit button
        int editX = enableX + btnW + btnSpacing;
        boolean editHovered = mouseX >= editX && mouseX < editX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, editX, btnY, btnW, btnH, "Edit", editHovered, ACCENT_BLUE);

        // Export button
        int exportX = editX + btnW + btnSpacing;
        boolean exportHovered = mouseX >= exportX && mouseX < exportX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, exportX, btnY, btnW, btnH, "Export", exportHovered, GOLD_DARK);

        // Duplicate button
        int dupX = exportX + btnW + btnSpacing;
        boolean dupHovered = mouseX >= dupX && mouseX < dupX + btnW + 10 && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, dupX, btnY, btnW + 10, btnH, "Duplicate", dupHovered, GOLD_DARK);

        // Delete button
        int delX = x + width - 25;
        int delY = y + 5;
        boolean delHovered = mouseX >= delX && mouseX < delX + 20 && mouseY >= delY && mouseY < delY + 20;
        context.fill(delX, delY, delX + 20, delY + 20, delHovered ? FAIL_RED : PARCHMENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "X", delX + 10, delY + 6, TEXT_LIGHT);
    }

    // ==================== WAYPOINT LIST ====================
    private void renderWaypointList(DrawContext context, int panelX, int listTop, int panelWidth, int mouseX, int mouseY) {
        if (WaypointData.INSTANCE.activePackage == null) return;

        // Category filter bar
        int filterY = listTop;
        int filterH = 22;
        context.fill(panelX + 10, filterY, panelX + panelWidth - 10, filterY + filterH, PARCHMENT);
        context.drawTextWithShadow(this.textRenderer, "Filter:", panelX + 15, filterY + 7, TEXT_DIM);

        int catX = panelX + 50;
        for (WaypointCategory cat : WaypointData.INSTANCE.activePackage.categories) {
            boolean active = activeCategories.contains(cat);
            int catW = this.textRenderer.getWidth(cat.name) + 10;
            boolean catHovered = mouseX >= catX && mouseX < catX + catW && mouseY >= filterY + 2 && mouseY < filterY + filterH - 2;

            context.fill(catX, filterY + 2, catX + catW, filterY + filterH - 2, active ? cat.color.asInt() | 0xFF000000 : BORDER_DARK);
            context.drawCenteredTextWithShadow(this.textRenderer, cat.name, catX + catW / 2, filterY + 6, active ? 0xFFFFFFFF : TEXT_DIM);
            catX += catW + 5;
        }

        int y = listTop + filterH + 5 - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;
        int wpIndex = 0;

        for (Waypoint wp : WaypointData.INSTANCE.activePackage.waypoints) {
            if (wp.getCategory() != null && !activeCategories.contains(wp.getCategory())) continue;

            if (y + ITEM_HEIGHT_WAYPOINT > listTop + filterH && y < this.height - 30) {
                drawWaypointItem(context, panelX + 15, y, itemWidth, ITEM_HEIGHT_WAYPOINT - 5, wp, wpIndex, mouseX, mouseY);
            }
            y += ITEM_HEIGHT_WAYPOINT;
            wpIndex++;
        }

        // Add new waypoint button
        if (y + 28 > listTop + filterH && y < this.height - 30) {
            boolean hovered = mouseX >= panelX + 15 && mouseX < panelX + 15 + itemWidth && mouseY >= y && mouseY < y + 28;
            drawMedievalButton(context, panelX + 15, y, itemWidth, 28, "+ Add Waypoint at Current Position", hovered, SUCCESS_GREEN);
        }
    }

    private void drawWaypointItem(DrawContext context, int x, int y, int width, int height, Waypoint wp, int index, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        context.fill(x, y, x + width, y + height, hovered ? PARCHMENT_LIGHT : PARCHMENT);
        context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);

        // Show indicator
        int statusColor = wp.show ? SUCCESS_GREEN : FAIL_RED;
        context.fill(x + 3, y + 5, x + 6, y + height - 5, statusColor);

        // Name
        if (editingWaypointIndex == index && "name".equals(editingField)) {
            drawEditableField(context, x + 12, y + 5, width / 2 - 20, 14, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, wp.name, x + 12, y + 5, TEXT_LIGHT);
        }

        // Category
        if (wp.getCategory() != null) {
            context.drawTextWithShadow(this.textRenderer, "[" + wp.getCategory().name + "]", x + 12, y + 18, wp.getCategory().color.asInt() | 0xFF000000);
        }

        // Coordinates
        String coordLabel = "X:";
        int coordX = x + width / 2;
        context.drawTextWithShadow(this.textRenderer, coordLabel, coordX, y + 5, TEXT_DIM);

        if (editingWaypointIndex == index && "x".equals(editingField)) {
            drawEditableField(context, coordX + 12, y + 5, 40, 12, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, String.valueOf(wp.x), coordX + 12, y + 5, TEXT_LIGHT);
        }

        coordX += 60;
        context.drawTextWithShadow(this.textRenderer, "Y:", coordX, y + 5, TEXT_DIM);
        if (editingWaypointIndex == index && "y".equals(editingField)) {
            drawEditableField(context, coordX + 12, y + 5, 40, 12, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, String.valueOf(wp.y), coordX + 12, y + 5, TEXT_LIGHT);
        }

        coordX += 60;
        context.drawTextWithShadow(this.textRenderer, "Z:", coordX, y + 5, TEXT_DIM);
        if (editingWaypointIndex == index && "z".equals(editingField)) {
            drawEditableField(context, coordX + 12, y + 5, 40, 12, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, String.valueOf(wp.z), coordX + 12, y + 5, TEXT_LIGHT);
        }

        // Toggle buttons row
        int btnY = y + height - 22;
        int btnH = 18;
        int btnX = x + 10;

        // Show toggle
        boolean showHovered = mouseX >= btnX && mouseX < btnX + 45 && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, btnX, btnY, 45, btnH, wp.show ? "Hide" : "Show", showHovered, wp.show ? FAIL_RED : SUCCESS_GREEN);

        // Name toggle
        btnX += 50;
        boolean nameHovered = mouseX >= btnX && mouseX < btnX + 50 && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, btnX, btnY, 50, btnH, "Name " + (wp.showName ? "ON" : "OFF"), nameHovered, wp.showName ? SUCCESS_GREEN : TEXT_DIM);

        // Distance toggle
        btnX += 55;
        boolean distHovered = mouseX >= btnX && mouseX < btnX + 50 && mouseY >= btnY && mouseY < btnY + btnH;
        drawMedievalButton(context, btnX, btnY, 50, btnH, "Dist " + (wp.showDistance ? "ON" : "OFF"), distHovered, wp.showDistance ? SUCCESS_GREEN : TEXT_DIM);

        // Delete button
        int delX = x + width - 22;
        int delY = y + 5;
        boolean delHovered = mouseX >= delX && mouseX < delX + 18 && mouseY >= delY && mouseY < delY + 18;
        context.fill(delX, delY, delX + 18, delY + 18, delHovered ? FAIL_RED : PARCHMENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "X", delX + 9, delY + 5, TEXT_LIGHT);
    }

    // ==================== CATEGORY LIST ====================
    private void renderCategoryList(DrawContext context, int panelX, int listTop, int panelWidth, int mouseX, int mouseY) {
        if (WaypointData.INSTANCE.activePackage == null) return;

        int y = listTop - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;

        for (int i = 0; i < WaypointData.INSTANCE.activePackage.categories.size(); i++) {
            WaypointCategory cat = WaypointData.INSTANCE.activePackage.categories.get(i);
            if (y + ITEM_HEIGHT_CATEGORY > listTop - 20 && y < this.height - 30) {
                drawCategoryItem(context, panelX + 15, y, itemWidth, ITEM_HEIGHT_CATEGORY - 5, cat, i, mouseX, mouseY);
            }
            y += ITEM_HEIGHT_CATEGORY;
        }

        // Add new category button
        if (y + 28 > listTop - 20 && y < this.height - 30) {
            boolean hovered = mouseX >= panelX + 15 && mouseX < panelX + 15 + itemWidth && mouseY >= y && mouseY < y + 28;
            drawMedievalButton(context, panelX + 15, y, itemWidth, 28, "+ Add New Category", hovered, SUCCESS_GREEN);
        }
    }

    private void drawCategoryItem(DrawContext context, int x, int y, int width, int height, WaypointCategory cat, int index, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        context.fill(x, y, x + width, y + height, hovered ? PARCHMENT_LIGHT : PARCHMENT);
        context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);

        // Color indicator
        context.fill(x + 3, y + 5, x + 18, y + height - 5, cat.color.asInt() | 0xFF000000);
        context.fill(x + 3, y + 5, x + 18, y + 6, 0x40FFFFFF);

        // Name
        if (editingCategoryIndex == index && "catName".equals(editingField)) {
            drawEditableField(context, x + 25, y + (height - 12) / 2, width - 80, 14, editBuffer, true);
        } else {
            context.drawTextWithShadow(this.textRenderer, cat.name, x + 25, y + (height - 8) / 2, TEXT_LIGHT);
        }

        // Delete button
        int delX = x + width - 22;
        int delY = y + (height - 18) / 2;
        boolean delHovered = mouseX >= delX && mouseX < delX + 18 && mouseY >= delY && mouseY < delY + 18;
        context.fill(delX, delY, delX + 18, delY + 18, delHovered ? FAIL_RED : PARCHMENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "X", delX + 9, delY + 5, TEXT_LIGHT);
    }

    private void drawEditableField(DrawContext context, int x, int y, int width, int height, String text, boolean active) {
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER_DARK);
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, active ? PARCHMENT_LIGHT : PARCHMENT);
        context.drawTextWithShadow(this.textRenderer, text, x, y, TEXT_LIGHT);

        if (active) {
            long now = System.currentTimeMillis();
            if (now - lastBlink > 500) {
                cursorVisible = !cursorVisible;
                lastBlink = now;
            }
            if (cursorVisible) {
                int cursorX = x + this.textRenderer.getWidth(text.substring(0, Math.min(cursorPos, text.length())));
                context.fill(cursorX, y, cursorX + 1, y + height, TEXT_LIGHT);
            }
        }
    }

    // ==================== INPUT HANDLING ====================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = Math.min((int)(this.width * 0.85), 600);
        int panelX = (this.width - panelWidth) / 2;
        int listTop = HEADER_HEIGHT + 10;

        // Header buttons
        if (mouseY >= 15 && mouseY < HEADER_HEIGHT) {
            // Back button
            if (currentView != ViewState.PACKAGES) {
                int backX = panelX + 5;
                int backY = HEADER_HEIGHT - 17;
                if (mouseX >= backX && mouseX < backX + 70 && mouseY >= backY && mouseY < backY + 18) {
                    saveCurrentEditing();
                    if (currentView == ViewState.CATEGORIES) {
                        currentView = ViewState.WAYPOINTS;
                    } else {
                        currentView = ViewState.PACKAGES;
                        WaypointData.INSTANCE.activePackage = null;
                    }
                    currentScrollOffset = 0;
                    clearEditing();
                    return true;
                }
            }

            // Categories button
            if (currentView == ViewState.WAYPOINTS) {
                int catX = panelX + panelWidth - 90;
                int catY = HEADER_HEIGHT - 17;
                if (mouseX >= catX && mouseX < catX + 85 && mouseY >= catY && mouseY < catY + 18) {
                    saveCurrentEditing();
                    currentView = ViewState.CATEGORIES;
                    currentScrollOffset = 0;
                    clearEditing();
                    return true;
                }
            }

            // Import button
            if (currentView == ViewState.PACKAGES) {
                int impX = panelX + 5;
                int impY = HEADER_HEIGHT - 17;
                if (mouseX >= impX && mouseX < impX + 100 && mouseY >= impY && mouseY < impY + 18) {
                    importFromClipboard();
                    return true;
                }
            }
        }

        // Content clicks
        saveCurrentEditing();
        clearEditing();

        switch (currentView) {
            case PACKAGES -> handlePackageClick((int)mouseX, (int)mouseY, panelX, listTop, panelWidth);
            case WAYPOINTS -> handleWaypointClick((int)mouseX, (int)mouseY, panelX, listTop, panelWidth);
            case CATEGORIES -> handleCategoryClick((int)mouseX, (int)mouseY, panelX, listTop, panelWidth);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handlePackageClick(int mouseX, int mouseY, int panelX, int listTop, int panelWidth) {
        int y = listTop - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;

        for (int i = 0; i < WaypointData.INSTANCE.packages.size(); i++) {
            WaypointPackage pkg = WaypointData.INSTANCE.packages.get(i);
            int itemX = panelX + 15;

            if (mouseY >= y && mouseY < y + ITEM_HEIGHT_PACKAGE - 5) {
                int btnY = y + ITEM_HEIGHT_PACKAGE - 31;
                int btnH = 20;

                // Delete button
                int delX = itemX + itemWidth - 25;
                if (mouseX >= delX && mouseX < delX + 20 && mouseY >= y + 5 && mouseY < y + 25) {
                    WaypointData.INSTANCE.packages.remove(i);
                    WaypointData.save();
                    return;
                }

                // Enable button
                int enableX = itemX + 10;
                if (mouseX >= enableX && mouseX < enableX + 55 && mouseY >= btnY && mouseY < btnY + btnH) {
                    pkg.enabled = !pkg.enabled;
                    WaypointData.save();
                    return;
                }

                // Edit button
                int editX = enableX + 60;
                if (mouseX >= editX && mouseX < editX + 55 && mouseY >= btnY && mouseY < btnY + btnH) {
                    WaypointData.INSTANCE.activePackage = pkg;
                    activeCategories.clear();
                    activeCategories.addAll(pkg.categories);
                    currentView = ViewState.WAYPOINTS;
                    currentScrollOffset = 0;
                    return;
                }

                // Export button
                int exportX = editX + 60;
                if (mouseX >= exportX && mouseX < exportX + 55 && mouseY >= btnY && mouseY < btnY + btnH) {
                    String json = new Gson().toJson(pkg);
                    MinecraftClient.getInstance().keyboard.setClipboard(json);
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Package exported to clipboard")));
                    return;
                }

                // Duplicate button
                int dupX = exportX + 60;
                if (mouseX >= dupX && mouseX < dupX + 65 && mouseY >= btnY && mouseY < btnY + btnH) {
                    WaypointData.INSTANCE.duplicatePackage(pkg.name);
                    return;
                }

                // Name click - start editing
                if (mouseX >= itemX + 12 && mouseX < itemX + itemWidth - 80 && mouseY >= y + 5 && mouseY < y + 25) {
                    editingPackageIndex = i;
                    editingField = "name";
                    editBuffer = pkg.name;
                    cursorPos = editBuffer.length();
                    return;
                }
            }
            y += ITEM_HEIGHT_PACKAGE;
        }

        // Add new package button
        if (mouseX >= panelX + 15 && mouseX < panelX + panelWidth - 15 && mouseY >= y && mouseY < y + 28) {
            WaypointPackage newPkg = new WaypointPackage(WaypointData.INSTANCE.generateUniqueName("New Package"));
            WaypointData.INSTANCE.packages.add(newPkg);
            WaypointData.save();
        }
    }

    private void handleWaypointClick(int mouseX, int mouseY, int panelX, int listTop, int panelWidth) {
        if (WaypointData.INSTANCE.activePackage == null) return;

        // Category filter clicks
        int filterY = listTop;
        int filterH = 22;
        if (mouseY >= filterY && mouseY < filterY + filterH) {
            int catX = panelX + 50;
            for (WaypointCategory cat : WaypointData.INSTANCE.activePackage.categories) {
                int catW = this.textRenderer.getWidth(cat.name) + 10;
                if (mouseX >= catX && mouseX < catX + catW) {
                    if (activeCategories.contains(cat)) {
                        activeCategories.remove(cat);
                    } else {
                        activeCategories.add(cat);
                    }
                    return;
                }
                catX += catW + 5;
            }
        }

        int y = listTop + filterH + 5 - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;
        int wpIndex = 0;

        for (int i = 0; i < WaypointData.INSTANCE.activePackage.waypoints.size(); i++) {
            Waypoint wp = WaypointData.INSTANCE.activePackage.waypoints.get(i);
            if (wp.getCategory() != null && !activeCategories.contains(wp.getCategory())) continue;

            int itemX = panelX + 15;
            int itemH = ITEM_HEIGHT_WAYPOINT - 5;

            if (mouseY >= y && mouseY < y + itemH) {
                // Delete button
                int delX = itemX + itemWidth - 22;
                if (mouseX >= delX && mouseX < delX + 18 && mouseY >= y + 5 && mouseY < y + 23) {
                    WaypointData.INSTANCE.activePackage.waypoints.remove(i);
                    WaypointData.save();
                    return;
                }

                int btnY = y + itemH - 22;

                // Show toggle
                if (mouseX >= itemX + 10 && mouseX < itemX + 55 && mouseY >= btnY && mouseY < btnY + 18) {
                    wp.show = !wp.show;
                    WaypointData.save();
                    return;
                }

                // Name toggle
                if (mouseX >= itemX + 60 && mouseX < itemX + 110 && mouseY >= btnY && mouseY < btnY + 18) {
                    wp.showName = !wp.showName;
                    WaypointData.save();
                    return;
                }

                // Distance toggle
                if (mouseX >= itemX + 115 && mouseX < itemX + 165 && mouseY >= btnY && mouseY < btnY + 18) {
                    wp.showDistance = !wp.showDistance;
                    WaypointData.save();
                    return;
                }

                // Editable fields
                if (mouseY >= y + 3 && mouseY < y + 20) {
                    // Name field
                    if (mouseX >= itemX + 12 && mouseX < itemX + itemWidth / 2 - 20) {
                        editingWaypointIndex = wpIndex;
                        editingField = "name";
                        editBuffer = wp.name;
                        cursorPos = editBuffer.length();
                        return;
                    }

                    // Coordinate fields
                    int coordX = itemX + itemWidth / 2;
                    if (mouseX >= coordX + 12 && mouseX < coordX + 52) {
                        editingWaypointIndex = wpIndex;
                        editingField = "x";
                        editBuffer = String.valueOf(wp.x);
                        cursorPos = editBuffer.length();
                        return;
                    }
                    coordX += 60;
                    if (mouseX >= coordX + 12 && mouseX < coordX + 52) {
                        editingWaypointIndex = wpIndex;
                        editingField = "y";
                        editBuffer = String.valueOf(wp.y);
                        cursorPos = editBuffer.length();
                        return;
                    }
                    coordX += 60;
                    if (mouseX >= coordX + 12 && mouseX < coordX + 52) {
                        editingWaypointIndex = wpIndex;
                        editingField = "z";
                        editBuffer = String.valueOf(wp.z);
                        cursorPos = editBuffer.length();
                        return;
                    }
                }
            }
            y += ITEM_HEIGHT_WAYPOINT;
            wpIndex++;
        }

        // Add new waypoint button
        if (mouseX >= panelX + 15 && mouseX < panelX + panelWidth - 15 && mouseY >= y && mouseY < y + 28) {
            if (MinecraftClient.getInstance().player != null) {
                int px = (int) Math.floor(MinecraftClient.getInstance().player.getX());
                int py = (int) Math.floor(MinecraftClient.getInstance().player.getY()) - 1;
                int pz = (int) Math.floor(MinecraftClient.getInstance().player.getZ());
                Waypoint wp = new Waypoint(px, py, pz);
                WaypointData.INSTANCE.activePackage.waypoints.add(wp);
                WaypointData.save();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Waypoint added at " + px + " " + py + " " + pz)));
            }
        }
    }

    private void handleCategoryClick(int mouseX, int mouseY, int panelX, int listTop, int panelWidth) {
        if (WaypointData.INSTANCE.activePackage == null) return;

        int y = listTop - (int)currentScrollOffset;
        int itemWidth = panelWidth - 30;

        for (int i = 0; i < WaypointData.INSTANCE.activePackage.categories.size(); i++) {
            WaypointCategory cat = WaypointData.INSTANCE.activePackage.categories.get(i);
            int itemX = panelX + 15;
            int itemH = ITEM_HEIGHT_CATEGORY - 5;

            if (mouseY >= y && mouseY < y + itemH) {
                // Delete button
                int delX = itemX + itemWidth - 22;
                int delY = y + (itemH - 18) / 2;
                if (mouseX >= delX && mouseX < delX + 18 && mouseY >= delY && mouseY < delY + 18) {
                    WaypointData.INSTANCE.activePackage.categories.remove(i);
                    activeCategories.remove(cat);
                    WaypointData.save();
                    return;
                }

                // Name click
                if (mouseX >= itemX + 25 && mouseX < itemX + itemWidth - 50) {
                    editingCategoryIndex = i;
                    editingField = "catName";
                    editBuffer = cat.name;
                    cursorPos = editBuffer.length();
                    return;
                }
            }
            y += ITEM_HEIGHT_CATEGORY;
        }

        // Add new category button
        if (mouseX >= panelX + 15 && mouseX < panelX + panelWidth - 15 && mouseY >= y && mouseY < y + 28) {
            WaypointCategory cat = new WaypointCategory("New Category");
            WaypointData.INSTANCE.activePackage.categories.add(cat);
            activeCategories.add(cat);
            WaypointData.save();
        }
    }

    private void saveCurrentEditing() {
        if (editingField == null) return;

        if (editingPackageIndex >= 0 && editingPackageIndex < WaypointData.INSTANCE.packages.size()) {
            if ("name".equals(editingField)) {
                WaypointData.INSTANCE.packages.get(editingPackageIndex).name = editBuffer;
            }
        }

        if (WaypointData.INSTANCE.activePackage != null && editingWaypointIndex >= 0) {
            List<Waypoint> visible = WaypointData.INSTANCE.activePackage.waypoints.stream()
                    .filter(w -> w.getCategory() == null || activeCategories.contains(w.getCategory()))
                    .toList();
            if (editingWaypointIndex < visible.size()) {
                Waypoint wp = visible.get(editingWaypointIndex);
                switch (editingField) {
                    case "name" -> wp.name = editBuffer;
                    case "x" -> { try { wp.x = Integer.parseInt(editBuffer); } catch (NumberFormatException ignored) {} }
                    case "y" -> { try { wp.y = Integer.parseInt(editBuffer); } catch (NumberFormatException ignored) {} }
                    case "z" -> { try { wp.z = Integer.parseInt(editBuffer); } catch (NumberFormatException ignored) {} }
                }
            }
        }

        if (WaypointData.INSTANCE.activePackage != null && editingCategoryIndex >= 0 &&
            editingCategoryIndex < WaypointData.INSTANCE.activePackage.categories.size()) {
            if ("catName".equals(editingField)) {
                String oldName = WaypointData.INSTANCE.activePackage.categories.get(editingCategoryIndex).name;
                String newName = editBuffer;
                WaypointData.INSTANCE.activePackage.categories.get(editingCategoryIndex).name = newName;

                // Update waypoints with this category
                for (Waypoint wp : WaypointData.INSTANCE.activePackage.waypoints) {
                    if (wp.categoryName != null && wp.categoryName.equals(oldName)) {
                        wp.categoryName = newName;
                    }
                }
            }
        }

        WaypointData.save();
    }

    private void importFromClipboard() {
        String json = MinecraftClient.getInstance().keyboard.getClipboard();
        try {
            WaypointPackage imported = new Gson().fromJson(json, WaypointPackage.class);
            if (imported == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid package data")));
                return;
            }
            if (imported.name == null || imported.name.isEmpty()) {
                imported.name = "Unnamed Package";
            }

            // Link categories
            for (Waypoint wp : imported.waypoints) {
                if (wp.categoryName != null) {
                    for (WaypointCategory cat : imported.categories) {
                        if (cat.name.equals(wp.categoryName)) {
                            wp.setCategory(cat);
                            break;
                        }
                    }
                }
            }

            // Ensure unique name
            if (WaypointData.INSTANCE.packages.stream().anyMatch(p -> p.name.equals(imported.name))) {
                imported.name = WaypointData.INSTANCE.generateUniqueName(imported.name);
            }

            WaypointData.INSTANCE.packages.add(imported);
            WaypointData.save();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Imported \"" + imported.name + "\" with " +
                    imported.waypoints.size() + " waypoints and " + imported.categories.size() + " categories")));
        } catch (JsonSyntaxException e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid JSON in clipboard")));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingField != null) {
            if (keyCode == 259 && cursorPos > 0) { // Backspace
                editBuffer = editBuffer.substring(0, cursorPos - 1) + editBuffer.substring(cursorPos);
                cursorPos--;
                return true;
            }
            if (keyCode == 261 && cursorPos < editBuffer.length()) { // Delete
                editBuffer = editBuffer.substring(0, cursorPos) + editBuffer.substring(cursorPos + 1);
                return true;
            }
            if (keyCode == 263 && cursorPos > 0) { // Left
                cursorPos--;
                return true;
            }
            if (keyCode == 262 && cursorPos < editBuffer.length()) { // Right
                cursorPos++;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter
                saveCurrentEditing();
                clearEditing();
                return true;
            }
            if (keyCode == 256) { // Escape
                clearEditing();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingField != null && chr >= 32) {
            editBuffer = editBuffer.substring(0, cursorPos) + chr + editBuffer.substring(cursorPos);
            cursorPos++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        currentScrollOffset = MathHelper.clamp(currentScrollOffset - verticalAmount * 25, 0, maxScroll);
        return true;
    }

    @Override
    public void close() {
        saveCurrentEditing();
        WaypointData.INSTANCE.activePackage = null;
        activeCategories.clear();
        WaypointData.save();
        super.close();
    }

    // Static method for compatibility
    public static void onClick() {
        // Handled via mouseClicked now
    }
}
