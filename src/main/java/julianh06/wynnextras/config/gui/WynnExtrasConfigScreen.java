package julianh06.wynnextras.config.gui;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class WynnExtrasConfigScreen extends Screen {
    private final Screen parent;
    private final WynnExtrasConfig config;

    // Colors - Wynncraft Medieval Theme
    private static final int BG_DARK = 0xFF1a1410;           // Dark wood/leather
    private static final int BG_MEDIUM = 0xFF2d2419;         // Medium brown
    private static final int BG_LIGHT = 0xFF3d3222;          // Light brown
    private static final int SIDEBAR_BG = 0xFF251e15;        // Dark sidebar
    private static final int PARCHMENT = 0xFF4a3c2a;         // Parchment-like
    private static final int PARCHMENT_LIGHT = 0xFF5c4d3a;   // Lighter parchment
    private static final int GOLD = 0xFFc9a227;              // Wynncraft gold
    private static final int GOLD_DARK = 0xFF8b7019;         // Dark gold
    private static final int GOLD_LIGHT = 0xFFe8c252;        // Light gold
    private static final int TEXT_LIGHT = 0xFFe8dcc8;        // Light text (parchment white)
    private static final int TEXT_GOLD = 0xFFdabc5e;         // Golden text
    private static final int TEXT_DIM = 0xFF9a8b70;          // Dimmed text
    private static final int BORDER_DARK = 0xFF1a1410;       // Dark border
    private static final int BORDER_LIGHT = 0xFF5c4a35;      // Light border
    private static final int TOGGLE_ON = 0xFF4a8c3a;         // Forest green (on)
    private static final int TOGGLE_OFF = 0xFF5c4535;        // Brown (off)
    private static final int ACCENT_RED = 0xFFa83232;        // Wynncraft red accent

    // Layout
    private static final int SIDEBAR_WIDTH = 150;
    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 50;

    private int selectedCategory = 0;
    private final List<Category> categories = new ArrayList<>();
    private double scrollOffset = 0;
    private double maxScroll = 0;

    public WynnExtrasConfigScreen(Screen parent) {
        super(Text.literal("WynnExtras Configuration"));
        this.parent = parent;
        this.config = WynnExtrasConfig.INSTANCE;
        initCategories();
    }

    private void initCategories() {
        categories.clear();

        // Player Hider
        Category playerHider = new Category("Player Hider", ACCENT_RED);
        playerHider.addOption(new BooleanOption("Enable Player Hider", "Toggle the player hiding feature",
                () -> config.partyMemberHide, v -> config.partyMemberHide = v));
        playerHider.addOption(new SliderOption("Max Hide Distance", "Players within this distance will be hidden",
                1, 20, () -> config.maxHideDistance, v -> config.maxHideDistance = v));
        playerHider.addOption(new BooleanOption("Only in NOTG", "Only hide in Nest of the Grootslangs",
                () -> config.onlyInNotg, v -> config.onlyInNotg = v));
        playerHider.addOption(new BooleanOption("Debug Output", "Print debug messages to console",
                () -> config.printDebugToConsole, v -> config.printDebugToConsole = v));
        playerHider.addOption(new StringListOption("Hidden Players", "Players to always hide",
                () -> config.hiddenPlayers, v -> config.hiddenPlayers = v));
        categories.add(playerHider);

        // Chat
        Category chat = new Category("Chat", 0xFF4a8c3a);
        chat.addOption(new SliderOption("Text Duration (ms)", "How long notification displays",
                500, 10000, () -> config.textDurationInMs, v -> config.textDurationInMs = v));
        chat.addOption(new EnumOption<>("Text Color", "Color of notification text",
                WynnExtrasConfig.TextColor.class, () -> config.textColor, v -> config.textColor = v));
        chat.addOption(new EnumOption<>("Sound", "Notification sound",
                WynnExtrasConfig.NotificationSound.class, () -> config.notificationSound, v -> config.notificationSound = v));
        chat.addOption(new SliderOption("Volume", "Sound volume",
                0, 100, () -> (int)(config.soundVolume * 100), v -> config.soundVolume = v / 100f));
        chat.addOption(new SliderOption("Pitch", "Sound pitch",
                50, 200, () -> (int)(config.soundPitch * 100), v -> config.soundPitch = v / 100f));
        chat.addOption(new StringListOption("Notifier Words", "Format: trigger|display text",
                () -> config.notifierWords, v -> config.notifierWords = v));
        chat.addOption(new StringListOption("Blocked Words", "Messages with these words are hidden",
                () -> config.blockedWords, v -> config.blockedWords = v));
        categories.add(chat);

        // Bank Overlay
        Category bank = new Category("Bank Overlay", 0xFF3a7a9c);
        bank.addOption(new BooleanOption("Enable Bank Overlay", "Toggle enhanced bank interface",
                () -> config.toggleBankOverlay, v -> config.toggleBankOverlay = v));
        bank.addOption(new BooleanOption("Smooth Scrolling", "Enable smooth scroll animation",
                () -> config.smoothScrollToggle, v -> config.smoothScrollToggle = v));
        bank.addOption(new BooleanOption("Quick Toggle", "Show quick toggle button",
                () -> config.bankQuickToggle, v -> config.bankQuickToggle = v));
        bank.addOption(new BooleanOption("Dark Mode", "Use dark theme",
                () -> config.darkmodeToggle, v -> config.darkmodeToggle = v));
        bank.addOption(new SliderOption("Rarity BG Intensity", "Item rarity background intensity",
                0, 255, () -> config.wynntilsItemRarityBackgroundAlpha, v -> config.wynntilsItemRarityBackgroundAlpha = v));
        categories.add(bank);

        // Raid
        Category raid = new Category("Raid", 0xFFa83232);
        raid.addOption(new BooleanOption("Raid Timestamps", "Show timestamps during raids",
                () -> config.toggleRaidTimestamps, v -> config.toggleRaidTimestamps = v));
        raid.addOption(new BooleanOption("Loot Tracker", "Track raid loot",
                () -> config.toggleRaidLootTracker, v -> config.toggleRaidLootTracker = v));
        raid.addOption(new BooleanOption("Only in Inventory", "Show tracker only when inventory open",
                () -> config.raidLootTrackerOnlyInInventory, v -> config.raidLootTrackerOnlyInInventory = v));
        raid.addOption(new BooleanOption("Only Near Chest", "Show only near reward chest",
                () -> config.raidLootTrackerOnlyNearChest, v -> config.raidLootTrackerOnlyNearChest = v));
        raid.addOption(new BooleanOption("Compact Mode", "Use compact display",
                () -> config.raidLootTrackerCompact, v -> config.raidLootTrackerCompact = v));
        raid.addOption(new BooleanOption("Show Session Stats", "Show current session stats",
                () -> config.raidLootTrackerShowSession, v -> config.raidLootTrackerShowSession = v));
        raid.addOption(new BooleanOption("Fast Requeue", "Auto /pf on chest close",
                () -> config.toggleFastRequeue, v -> config.toggleFastRequeue = v));
        raid.addOption(new BooleanOption("Provoke Timer", "Show provoke timer [WIP]",
                () -> config.provokeTimerToggle, v -> config.provokeTimerToggle = v));
        categories.add(raid);

        // Misc
        Category misc = new Category("Misc", GOLD);
        misc.addOption(new BooleanOption("Show Item Weights", "Display Wynnpool weights",
                () -> config.showWeight, v -> config.showWeight = v));
        misc.addOption(new BooleanOption("Show Stat Scales", "Display stat weights",
                () -> config.showScales, v -> config.showScales = v));
        misc.addOption(new BooleanOption("Financial Advice", "Smart advice in identifier menu",
                () -> config.sourceOfTruthToggle, v -> config.sourceOfTruthToggle = v));
        misc.addOption(new BooleanOption("Totem Range", "Show totem range circle",
                () -> config.totemRangeVisualizerToggle, v -> config.totemRangeVisualizerToggle = v));
        misc.addOption(new SliderOption("Totem Range", "Radius of totem circle",
                1, 30, () -> (int)config.totemRange, v -> config.totemRange = v));
        misc.addOption(new EnumOption<>("Totem Color", "Totem circle color",
                WynnExtrasConfig.TextColor.class, () -> config.totemColor, v -> config.totemColor = v));
        misc.addOption(new SliderOption("Eldritch Range", "Eldritch call radius",
                1, 30, () -> (int)config.eldritchCallRange, v -> config.eldritchCallRange = v));
        misc.addOption(new EnumOption<>("Eldritch Color", "Eldritch call color",
                WynnExtrasConfig.TextColor.class, () -> config.eldritchCallColor, v -> config.eldritchCallColor = v));
        misc.addOption(new EnumOption<>("Provoke Timer Color", "Timer color",
                WynnExtrasConfig.TextColor.class, () -> config.provokeTimerColor, v -> config.provokeTimerColor = v));
        misc.addOption(new BooleanOption("PV Dark Mode", "Profile viewer dark theme",
                () -> config.pvDarkmodeToggle, v -> config.pvDarkmodeToggle = v));
        misc.addOption(new BooleanOption("Custom GUI Scale", "Use different scale for menus",
                () -> config.differentGUIScale, v -> config.differentGUIScale = v));
        misc.addOption(new SliderOption("GUI Scale", "Custom GUI scale value",
                1, 5, () -> config.customGUIScale, v -> config.customGUIScale = v));
        misc.addOption(new BooleanOption("Skip Front View", "Skip front-facing 3rd person",
                () -> config.removeFrontPersonView, v -> config.removeFrontPersonView = v));
        categories.add(misc);

        // Waypoints
        Category waypoints = new Category("Waypoints", 0xFF7a5aa8);
        waypoints.addOption(new BooleanOption("Disable Default Waypoints", "Disable built-in waypoint packages",
                () -> config.disableAllDefaultWaypoints, v -> config.disableAllDefaultWaypoints = v));
        categories.add(waypoints);
    }

    @Override
    protected void init() {
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentHeight = cat.options.size() * 50 + 20;
            int visibleHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - 20;
            maxScroll = Math.max(0, contentHeight - visibleHeight);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Solid background
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        context.fill(0, 0, this.width, this.height, BG_DARK);

        // Sidebar background with wood texture effect
        drawWoodPanel(context, 0, 0, SIDEBAR_WIDTH, this.height);

        // Main content area - parchment style
        drawParchmentPanel(context, SIDEBAR_WIDTH + 5, 5, this.width - SIDEBAR_WIDTH - 10, this.height - 10);

        // Header with ornate border
        drawOrnateHeader(context, SIDEBAR_WIDTH + 10, 10, this.width - SIDEBAR_WIDTH - 20, HEADER_HEIGHT - 5);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "WynnExtras",
                SIDEBAR_WIDTH + (this.width - SIDEBAR_WIDTH) / 2, 25, GOLD);
        // Subtitle
        context.drawCenteredTextWithShadow(this.textRenderer, "Configuration",
                SIDEBAR_WIDTH + (this.width - SIDEBAR_WIDTH) / 2, 38, TEXT_DIM);

        // Sidebar title with decorative line
        int sidebarTitleY = 15;
        context.drawCenteredTextWithShadow(this.textRenderer, "Categories", SIDEBAR_WIDTH / 2, sidebarTitleY, GOLD);
        // Decorative lines
        context.fill(15, sidebarTitleY + 12, SIDEBAR_WIDTH - 15, sidebarTitleY + 13, GOLD_DARK);
        context.fill(25, sidebarTitleY + 14, SIDEBAR_WIDTH - 25, sidebarTitleY + 15, BORDER_DARK);

        // Sidebar categories
        int categoryY = 40;
        int categoryHeight = 28;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            boolean hovered = mouseX >= 5 && mouseX < SIDEBAR_WIDTH - 5 &&
                             mouseY >= categoryY && mouseY < categoryY + categoryHeight;
            boolean selected = i == selectedCategory;

            int catX = 8;
            int catWidth = SIDEBAR_WIDTH - 16;

            if (selected) {
                // Selected background - embossed look
                context.fill(catX, categoryY, catX + catWidth, categoryY + categoryHeight, PARCHMENT);
                context.fill(catX, categoryY, catX + catWidth, categoryY + 1, BORDER_LIGHT);
                context.fill(catX, categoryY + categoryHeight - 1, catX + catWidth, categoryY + categoryHeight, BORDER_DARK);
                // Gold accent bar on left
                context.fill(catX, categoryY + 2, catX + 3, categoryY + categoryHeight - 2, cat.color);
            } else if (hovered) {
                context.fill(catX, categoryY, catX + catWidth, categoryY + categoryHeight, BG_LIGHT);
            }

            // Category icon (diamond shape with color)
            int iconX = catX + 12;
            int iconY = categoryY + categoryHeight / 2;
            drawDiamond(context, iconX, iconY, 4, cat.color);

            // Category text - vertically centered
            int textY = categoryY + (categoryHeight - 8) / 2; // 8 is approx font height
            context.drawTextWithShadow(this.textRenderer, cat.name, catX + 24, textY,
                    selected ? TEXT_LIGHT : TEXT_DIM);

            categoryY += categoryHeight + 4;
        }

        // Content area
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);

            int contentX = SIDEBAR_WIDTH + 20;
            int contentY = HEADER_HEIGHT + 15;
            int contentWidth = this.width - SIDEBAR_WIDTH - 40;

            // Category title with decorative elements
            drawDiamond(context, contentX + 6, contentY + 5, 5, cat.color);
            context.drawTextWithShadow(this.textRenderer, cat.name, contentX + 18, contentY, cat.color);

            // Decorative separator
            int sepY = contentY + 14;
            context.fill(contentX, sepY, contentX + contentWidth, sepY + 1, GOLD_DARK);
            context.fill(contentX, sepY + 1, contentX + contentWidth, sepY + 2, BORDER_DARK);

            // Enable scissor for scrolling content
            int listTop = contentY + 20;
            int listBottom = this.height - FOOTER_HEIGHT - 10;
            context.enableScissor(SIDEBAR_WIDTH, listTop, this.width - 10, listBottom);

            int optionY = listTop - (int)scrollOffset;
            for (ConfigOption option : cat.options) {
                if (optionY + 45 > listTop && optionY < listBottom) {
                    option.render(context, contentX, optionY, contentWidth, 45, mouseX, mouseY, this);
                }
                optionY += 50;
            }

            context.disableScissor();

            // Scrollbar (medieval style)
            if (maxScroll > 0) {
                int scrollbarX = this.width - 18;
                int scrollbarTop = listTop;
                int scrollbarHeight = listBottom - listTop;
                int thumbHeight = Math.max(30, (int)(scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll)));
                int thumbY = scrollbarTop + (int)((scrollbarHeight - thumbHeight) * (scrollOffset / maxScroll));

                // Track
                context.fill(scrollbarX, scrollbarTop, scrollbarX + 8, scrollbarTop + scrollbarHeight, BORDER_DARK);
                context.fill(scrollbarX + 1, scrollbarTop + 1, scrollbarX + 7, scrollbarTop + scrollbarHeight - 1, BG_MEDIUM);
                // Thumb
                context.fill(scrollbarX + 1, thumbY, scrollbarX + 7, thumbY + thumbHeight, GOLD_DARK);
                context.fill(scrollbarX + 2, thumbY + 1, scrollbarX + 6, thumbY + thumbHeight - 1, GOLD);
            }
        }

        // Footer with buttons
        int footerY = this.height - FOOTER_HEIGHT + 5;
        context.fill(SIDEBAR_WIDTH + 10, footerY, this.width - 10, footerY + 1, GOLD_DARK);

        // Custom medieval-style buttons
        int buttonY = this.height - 35;
        int buttonHeight = 24;

        // Save & Close button
        int saveX = this.width - 115;
        boolean saveHovered = mouseX >= saveX && mouseX < saveX + 100 && mouseY >= buttonY && mouseY < buttonY + buttonHeight;
        drawMedievalButton(context, saveX, buttonY, 100, buttonHeight, "Save & Close", saveHovered, TOGGLE_ON);

        // Cancel button
        int cancelX = this.width - 225;
        boolean cancelHovered = mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + buttonHeight;
        drawMedievalButton(context, cancelX, buttonY, 100, buttonHeight, "Cancel", cancelHovered, ACCENT_RED);
    }

    // Draw wood panel effect
    private void drawWoodPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, SIDEBAR_BG);
        // Wood grain lines
        for (int i = 0; i < height; i += 12) {
            int shade = (i / 12) % 2 == 0 ? 0x08FFFFFF : 0x05000000;
            context.fill(x, y + i, x + width, y + i + 6, shade);
        }
        // Border
        context.fill(x + width - 2, y, x + width, y + height, BORDER_DARK);
        context.fill(x + width - 3, y, x + width - 2, y + height, BORDER_LIGHT);
    }

    // Draw parchment-style panel
    private void drawParchmentPanel(DrawContext context, int x, int y, int width, int height) {
        // Main fill
        context.fill(x, y, x + width, y + height, BG_MEDIUM);
        // Inner lighter area
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, BG_LIGHT);
        // Borders
        context.fill(x, y, x + width, y + 2, BORDER_DARK);
        context.fill(x, y + height - 2, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 2, y + height, BORDER_DARK);
        context.fill(x + width - 2, y, x + width, y + height, BORDER_DARK);
        // Inner highlight
        context.fill(x + 2, y + 2, x + width - 2, y + 3, BORDER_LIGHT);
        context.fill(x + 2, y + 2, x + 3, y + height - 2, BORDER_LIGHT);
    }

    // Draw ornate header
    private void drawOrnateHeader(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PARCHMENT);
        // Top border
        context.fill(x, y, x + width, y + 2, GOLD_DARK);
        // Bottom decorative border
        context.fill(x + 10, y + height - 2, x + width - 10, y + height, GOLD_DARK);
        // Corner accents
        drawDiamond(context, x + 5, y + height / 2, 3, GOLD);
        drawDiamond(context, x + width - 5, y + height / 2, 3, GOLD);
    }

    // Draw a diamond shape
    private void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    // Draw medieval-style button
    private void drawMedievalButton(DrawContext context, int x, int y, int width, int height, String text, boolean hovered, int accentColor) {
        int bgColor = hovered ? PARCHMENT_LIGHT : PARCHMENT;
        context.fill(x, y, x + width, y + height, bgColor);
        // Borders
        context.fill(x, y, x + width, y + 1, hovered ? GOLD : BORDER_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, BORDER_DARK);
        context.fill(x, y, x + 1, y + height, hovered ? GOLD : BORDER_LIGHT);
        context.fill(x + width - 1, y, x + width, y + height, BORDER_DARK);
        // Accent line at bottom
        context.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, accentColor);
        // Text
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + (height - 8) / 2, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Button clicks
        int buttonY = this.height - 35;
        int buttonHeight = 24;

        // Save & Close
        int saveX = this.width - 115;
        if (mouseX >= saveX && mouseX < saveX + 100 && mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
            WynnExtrasConfig.save();
            this.client.setScreen(parent);
            return true;
        }

        // Cancel
        int cancelX = this.width - 225;
        if (mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
            WynnExtrasConfig.load();
            this.client.setScreen(parent);
            return true;
        }

        // Category selection
        if (mouseX >= 5 && mouseX < SIDEBAR_WIDTH - 5) {
            int categoryY = 40;
            int categoryHeight = 28;
            for (int i = 0; i < categories.size(); i++) {
                if (mouseY >= categoryY && mouseY < categoryY + categoryHeight) {
                    selectedCategory = i;
                    scrollOffset = 0;
                    updateMaxScroll();
                    return true;
                }
                categoryY += categoryHeight + 4;
            }
        }

        // Option clicks
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentX = SIDEBAR_WIDTH + 20;
            int contentY = HEADER_HEIGHT + 35;
            int contentWidth = this.width - SIDEBAR_WIDTH - 40;

            int optionY = contentY - (int)scrollOffset;
            for (ConfigOption option : cat.options) {
                int listTop = HEADER_HEIGHT + 35;
                int listBottom = this.height - FOOTER_HEIGHT - 10;
                if (mouseY >= Math.max(listTop, optionY) && mouseY < Math.min(listBottom, optionY + 45)) {
                    if (option.mouseClicked(mouseX, mouseY, contentX, optionY, contentWidth, 45, button)) {
                        return true;
                    }
                }
                optionY += 50;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                option.mouseReleased(mouseX, mouseY, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX > SIDEBAR_WIDTH) {
            scrollOffset = MathHelper.clamp(scrollOffset - verticalAmount * 30, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                if (option.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                if (option.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    // ==================== Category ====================
    private static class Category {
        final String name;
        final int color;
        final List<ConfigOption> options = new ArrayList<>();

        Category(String name, int color) {
            this.name = name;
            this.color = color;
        }

        void addOption(ConfigOption option) {
            options.add(option);
        }
    }

    // ==================== Config Options ====================
    private static abstract class ConfigOption {
        final String name;
        final String description;

        ConfigOption(String name, String description) {
            this.name = name;
            this.description = description;
        }

        abstract void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, Screen screen);
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height, int button) { return false; }
        boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
        boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
        boolean charTyped(char chr, int modifiers) { return false; }
    }

    // Boolean toggle - medieval lever style
    private static class BooleanOption extends ConfigOption {
        private final java.util.function.Supplier<Boolean> getter;
        private final java.util.function.Consumer<Boolean> setter;

        BooleanOption(String name, String description, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
            super(name, description);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, Screen screen) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;

            // Option background
            context.fill(x, y, x + width, y + height - 5, PARCHMENT);
            context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
            context.fill(x, y + height - 6, x + width, y + height - 5, BORDER_DARK);

            context.drawTextWithShadow(textRenderer, name, x + 8, y + 8, TEXT_LIGHT);
            context.drawTextWithShadow(textRenderer, description, x + 8, y + 22, TEXT_DIM);

            // Toggle switch - medieval style
            int toggleX = x + width - 55;
            int toggleY = y + 12;
            int toggleWidth = 44;
            int toggleHeight = 20;
            boolean value = getter.get();

            // Track background
            context.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, BORDER_DARK);
            context.fill(toggleX + 1, toggleY + 1, toggleX + toggleWidth - 1, toggleY + toggleHeight - 1,
                    value ? TOGGLE_ON : TOGGLE_OFF);

            // Lever/knob
            int knobWidth = 18;
            int knobX = value ? toggleX + toggleWidth - knobWidth - 2 : toggleX + 2;
            context.fill(knobX, toggleY + 2, knobX + knobWidth, toggleY + toggleHeight - 2, BORDER_DARK);
            context.fill(knobX + 1, toggleY + 3, knobX + knobWidth - 1, toggleY + toggleHeight - 3, GOLD);
            context.fill(knobX + 2, toggleY + 4, knobX + knobWidth - 2, toggleY + toggleHeight - 4, GOLD_LIGHT);
            // Knob highlight
            context.fill(knobX + 2, toggleY + 4, knobX + knobWidth - 2, toggleY + 6, 0x40FFFFFF);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height, int button) {
            int toggleX = x + width - 55;
            int toggleY = y + 12;
            if (mouseX >= toggleX && mouseX < toggleX + 44 && mouseY >= toggleY && mouseY < toggleY + 20) {
                setter.accept(!getter.get());
                return true;
            }
            return false;
        }
    }

    // Integer slider - medieval style
    private static class SliderOption extends ConfigOption {
        private final int min, max;
        private final java.util.function.Supplier<Integer> getter;
        private final java.util.function.Consumer<Integer> setter;
        private boolean dragging = false;

        SliderOption(String name, String description, int min, int max,
                     java.util.function.Supplier<Integer> getter, java.util.function.Consumer<Integer> setter) {
            super(name, description);
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, Screen screen) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;

            // Option background
            context.fill(x, y, x + width, y + height - 5, PARCHMENT);
            context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
            context.fill(x, y + height - 6, x + width, y + height - 5, BORDER_DARK);

            context.drawTextWithShadow(textRenderer, name, x + 8, y + 8, TEXT_LIGHT);
            context.drawTextWithShadow(textRenderer, description, x + 8, y + 22, TEXT_DIM);

            int sliderX = x + width - 170;
            int sliderY = y + 15;
            int sliderWidth = 120;
            int sliderHeight = 8;
            int value = getter.get();
            float percent = (float)(value - min) / (max - min);

            // Track background
            context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, BORDER_DARK);
            context.fill(sliderX + 1, sliderY + 1, sliderX + sliderWidth - 1, sliderY + sliderHeight - 1, BG_MEDIUM);

            // Fill
            int fillWidth = (int)((sliderWidth - 2) * percent);
            if (fillWidth > 0) {
                context.fill(sliderX + 1, sliderY + 1, sliderX + 1 + fillWidth, sliderY + sliderHeight - 1, GOLD_DARK);
            }

            // Knob
            int knobWidth = 10;
            int knobHeight = 14;
            int knobX = sliderX + (int)(sliderWidth * percent) - knobWidth / 2;
            int knobY = sliderY - 3;
            context.fill(knobX, knobY, knobX + knobWidth, knobY + knobHeight, BORDER_DARK);
            context.fill(knobX + 1, knobY + 1, knobX + knobWidth - 1, knobY + knobHeight - 1, GOLD);
            context.fill(knobX + 2, knobY + 2, knobX + knobWidth - 2, knobY + 4, 0x40FFFFFF);

            // Value text - centered vertically with slider
            String valueStr = String.valueOf(value);
            int textWidth = textRenderer.getWidth(valueStr);
            int textX = sliderX + sliderWidth + 10;
            int textY = sliderY + (sliderHeight - 8) / 2; // Center text with slider
            context.drawTextWithShadow(textRenderer, valueStr, textX, textY, GOLD);

            if (dragging) {
                float newPercent = MathHelper.clamp((float)(mouseX - sliderX) / sliderWidth, 0, 1);
                int newValue = min + Math.round(newPercent * (max - min));
                setter.accept(newValue);
            }
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height, int button) {
            int sliderX = x + width - 170;
            int sliderY = y + 10;
            if (mouseX >= sliderX - 5 && mouseX < sliderX + 130 && mouseY >= sliderY && mouseY < sliderY + 20) {
                dragging = true;
                float percent = MathHelper.clamp((float)(mouseX - sliderX) / 120f, 0, 1);
                int newValue = min + Math.round(percent * (max - min));
                setter.accept(newValue);
                return true;
            }
            return false;
        }

        @Override
        boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;
                return true;
            }
            return false;
        }
    }

    // Enum dropdown - medieval style
    private static class EnumOption<T extends Enum<T>> extends ConfigOption {
        private final Class<T> enumClass;
        private final java.util.function.Supplier<T> getter;
        private final java.util.function.Consumer<T> setter;

        EnumOption(String name, String description, Class<T> enumClass,
                   java.util.function.Supplier<T> getter, java.util.function.Consumer<T> setter) {
            super(name, description);
            this.enumClass = enumClass;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, Screen screen) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;

            // Option background
            context.fill(x, y, x + width, y + height - 5, PARCHMENT);
            context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
            context.fill(x, y + height - 6, x + width, y + height - 5, BORDER_DARK);

            context.drawTextWithShadow(textRenderer, name, x + 8, y + 8, TEXT_LIGHT);
            context.drawTextWithShadow(textRenderer, description, x + 8, y + 22, TEXT_DIM);

            int buttonX = x + width - 135;
            int buttonY = y + 10;
            int buttonWidth = 125;
            int buttonHeight = 22;
            T value = getter.get();

            boolean hovered = mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                             mouseY >= buttonY && mouseY < buttonY + buttonHeight;

            // Button background
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, BORDER_DARK);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1,
                    hovered ? PARCHMENT_LIGHT : PARCHMENT);
            // Top highlight
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + 2, 0x20FFFFFF);

            // Text
            String displayText = value.toString();
            if (displayText.length() > 12) {
                displayText = displayText.substring(0, 10) + "..";
            }
            context.drawCenteredTextWithShadow(textRenderer, displayText, buttonX + buttonWidth / 2 - 6, buttonY + 7, TEXT_LIGHT);

            // Arrow indicators
            context.drawTextWithShadow(textRenderer, "<", buttonX + 5, buttonY + 7, TEXT_DIM);
            context.drawTextWithShadow(textRenderer, ">", buttonX + buttonWidth - 10, buttonY + 7, TEXT_DIM);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height, int button) {
            int buttonX = x + width - 135;
            int buttonY = y + 10;
            if (mouseX >= buttonX && mouseX < buttonX + 125 && mouseY >= buttonY && mouseY < buttonY + 22) {
                T[] values = enumClass.getEnumConstants();
                T current = getter.get();
                int index = (current.ordinal() + (button == 0 ? 1 : values.length - 1)) % values.length;
                setter.accept(values[index]);
                return true;
            }
            return false;
        }
    }

    // String list option - medieval style
    private static class StringListOption extends ConfigOption {
        private final java.util.function.Supplier<List<String>> getter;
        private final java.util.function.Consumer<List<String>> setter;

        StringListOption(String name, String description,
                         java.util.function.Supplier<List<String>> getter, java.util.function.Consumer<List<String>> setter) {
            super(name, description);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, Screen screen) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;

            // Option background
            context.fill(x, y, x + width, y + height - 5, PARCHMENT);
            context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
            context.fill(x, y + height - 6, x + width, y + height - 5, BORDER_DARK);

            context.drawTextWithShadow(textRenderer, name, x + 8, y + 8, TEXT_LIGHT);

            List<String> list = getter.get();
            String countText = list.size() + " item" + (list.size() != 1 ? "s" : "");
            context.drawTextWithShadow(textRenderer, countText, x + 8, y + 22, TEXT_DIM);

            // Edit button
            int buttonX = x + width - 75;
            int buttonY = y + 12;
            int buttonWidth = 65;
            int buttonHeight = 20;
            boolean hovered = mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + buttonHeight;

            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, BORDER_DARK);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1,
                    hovered ? PARCHMENT_LIGHT : PARCHMENT);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + 2, 0x20FFFFFF);

            context.drawCenteredTextWithShadow(textRenderer, "Edit...", buttonX + buttonWidth / 2, buttonY + 6, TEXT_LIGHT);
        }

        @Override
        boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height, int button) {
            int buttonX = x + width - 75;
            int buttonY = y + 12;
            if (mouseX >= buttonX && mouseX < buttonX + 65 && mouseY >= buttonY && mouseY < buttonY + 20) {
                MinecraftClient.getInstance().setScreen(new StringListEditorScreen(
                        (Screen) MinecraftClient.getInstance().currentScreen,
                        name, getter.get(), setter
                ));
                return true;
            }
            return false;
        }
    }

    // ==================== String List Editor Screen - Medieval Style ====================
    private static class StringListEditorScreen extends Screen {
        private final Screen parent;
        private final List<String> items;
        private final java.util.function.Consumer<List<String>> setter;
        private String newItemText = "";
        private double scrollOffset = 0;

        StringListEditorScreen(Screen parent, String title, List<String> items, java.util.function.Consumer<List<String>> setter) {
            super(Text.literal("Edit: " + title));
            this.parent = parent;
            this.items = new ArrayList<>(items);
            this.setter = setter;
        }

        @Override
        protected void init() {
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(0, 0, this.width, this.height, 0xFF000000);
            context.fill(0, 0, this.width, this.height, BG_DARK);

            // Main panel
            int panelX = this.width / 2 - 180;
            int panelWidth = 360;
            context.fill(panelX, 20, panelX + panelWidth, this.height - 20, BG_MEDIUM);
            context.fill(panelX + 2, 22, panelX + panelWidth - 2, this.height - 22, BG_LIGHT);
            // Border
            context.fill(panelX, 20, panelX + panelWidth, 22, BORDER_DARK);
            context.fill(panelX, this.height - 22, panelX + panelWidth, this.height - 20, BORDER_DARK);
            context.fill(panelX, 20, panelX + 2, this.height - 20, BORDER_DARK);
            context.fill(panelX + panelWidth - 2, 20, panelX + panelWidth, this.height - 20, BORDER_DARK);

            // Title
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 35, GOLD);
            context.fill(panelX + 20, 48, panelX + panelWidth - 20, 49, GOLD_DARK);

            // Add new item area
            int inputY = 60;
            context.fill(panelX + 15, inputY, panelX + panelWidth - 65, inputY + 24, BORDER_DARK);
            context.fill(panelX + 16, inputY + 1, panelX + panelWidth - 66, inputY + 23, PARCHMENT);
            context.drawTextWithShadow(this.textRenderer, newItemText + "_", panelX + 20, inputY + 8, TEXT_LIGHT);

            // Add button
            int addX = panelX + panelWidth - 60;
            boolean addHovered = mouseX >= addX && mouseX < addX + 45 && mouseY >= inputY && mouseY < inputY + 24;
            context.fill(addX, inputY, addX + 45, inputY + 24, BORDER_DARK);
            context.fill(addX + 1, inputY + 1, addX + 44, inputY + 23, addHovered ? TOGGLE_ON : PARCHMENT);
            context.drawCenteredTextWithShadow(this.textRenderer, "+ Add", addX + 22, inputY + 8, TEXT_LIGHT);

            // Items list
            int listY = 95;
            int itemHeight = 28;
            context.enableScissor(panelX + 10, listY, panelX + panelWidth - 10, this.height - 70);

            int y = listY - (int)scrollOffset;
            for (int i = 0; i < items.size(); i++) {
                if (y + itemHeight > listY && y < this.height - 70) {
                    // Item background
                    context.fill(panelX + 15, y, panelX + panelWidth - 50, y + itemHeight - 4, PARCHMENT);
                    context.fill(panelX + 15, y, panelX + panelWidth - 50, y + 1, BORDER_LIGHT);
                    context.fill(panelX + 15, y + itemHeight - 5, panelX + panelWidth - 50, y + itemHeight - 4, BORDER_DARK);

                    // Item text
                    String text = items.get(i);
                    if (text.length() > 35) text = text.substring(0, 33) + "..";
                    context.drawTextWithShadow(this.textRenderer, text, panelX + 20, y + 8, TEXT_LIGHT);

                    // Delete button
                    int delX = panelX + panelWidth - 45;
                    boolean delHovered = mouseX >= delX && mouseX < delX + 30 && mouseY >= y && mouseY < y + itemHeight - 4;
                    context.fill(delX, y, delX + 30, y + itemHeight - 4, BORDER_DARK);
                    context.fill(delX + 1, y + 1, delX + 29, y + itemHeight - 5, delHovered ? ACCENT_RED : PARCHMENT);
                    context.drawCenteredTextWithShadow(this.textRenderer, "X", delX + 15, y + 8, TEXT_LIGHT);
                }
                y += itemHeight;
            }
            context.disableScissor();

            if (items.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, "No items - type above and click Add", this.width / 2, this.height / 2, TEXT_DIM);
            }

            // Bottom buttons
            int buttonY = this.height - 55;
            int doneX = this.width / 2 - 105;
            int cancelX = this.width / 2 + 5;
            boolean doneHovered = mouseX >= doneX && mouseX < doneX + 100 && mouseY >= buttonY && mouseY < buttonY + 24;
            boolean cancelHovered = mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + 24;

            // Done button
            context.fill(doneX, buttonY, doneX + 100, buttonY + 24, BORDER_DARK);
            context.fill(doneX + 1, buttonY + 1, doneX + 99, buttonY + 23, doneHovered ? TOGGLE_ON : PARCHMENT);
            context.drawCenteredTextWithShadow(this.textRenderer, "Done", doneX + 50, buttonY + 8, TEXT_LIGHT);

            // Cancel button
            context.fill(cancelX, buttonY, cancelX + 100, buttonY + 24, BORDER_DARK);
            context.fill(cancelX + 1, buttonY + 1, cancelX + 99, buttonY + 23, cancelHovered ? ACCENT_RED : PARCHMENT);
            context.drawCenteredTextWithShadow(this.textRenderer, "Cancel", cancelX + 50, buttonY + 8, TEXT_LIGHT);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int panelX = this.width / 2 - 180;
            int panelWidth = 360;

            // Add button
            int inputY = 60;
            int addX = panelX + panelWidth - 60;
            if (mouseX >= addX && mouseX < addX + 45 && mouseY >= inputY && mouseY < inputY + 24) {
                if (!newItemText.isEmpty()) {
                    items.add(newItemText);
                    newItemText = "";
                }
                return true;
            }

            // Bottom buttons
            int buttonY = this.height - 55;
            int doneX = this.width / 2 - 105;
            int cancelX = this.width / 2 + 5;

            if (mouseX >= doneX && mouseX < doneX + 100 && mouseY >= buttonY && mouseY < buttonY + 24) {
                setter.accept(items);
                this.client.setScreen(parent);
                return true;
            }

            if (mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + 24) {
                this.client.setScreen(parent);
                return true;
            }

            // Delete buttons
            int listY = 95;
            int itemHeight = 28;
            int y = listY - (int)scrollOffset;
            for (int i = 0; i < items.size(); i++) {
                int delX = panelX + panelWidth - 45;
                if (mouseX >= delX && mouseX < delX + 30 && mouseY >= y && mouseY < y + itemHeight - 4) {
                    items.remove(i);
                    return true;
                }
                y += itemHeight;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == 259 && !newItemText.isEmpty()) { // Backspace
                newItemText = newItemText.substring(0, newItemText.length() - 1);
                return true;
            }
            if (keyCode == 257 && !newItemText.isEmpty()) { // Enter
                items.add(newItemText);
                newItemText = "";
                return true;
            }
            if (keyCode == 256) { // Escape
                this.client.setScreen(parent);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (chr >= 32) {
                newItemText += chr;
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int maxScroll = Math.max(0, items.size() * 28 - (this.height - 165));
            scrollOffset = MathHelper.clamp(scrollOffset - verticalAmount * 25, 0, maxScroll);
            return true;
        }

        @Override
        public void close() {
            this.client.setScreen(parent);
        }
    }
}
