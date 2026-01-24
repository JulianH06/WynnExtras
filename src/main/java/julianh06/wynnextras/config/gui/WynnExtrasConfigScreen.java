package julianh06.wynnextras.config.gui;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class WynnExtrasConfigScreen extends Screen {
    private final Screen parent;
    private final WynnExtrasConfig config;

    // Colors - Wynncraft Medieval Theme
    private static final int BG_DARK = 0xFF1a1410;
    private static final int BG_MEDIUM = 0xFF2d2419;
    private static final int BG_LIGHT = 0xFF3d3222;
    private static final int SIDEBAR_BG = 0xFF251e15;
    private static final int PARCHMENT = 0xFF4a3c2a;
    private static final int PARCHMENT_LIGHT = 0xFF5c4d3a;
    private static final int GOLD = 0xFFc9a227;
    private static final int GOLD_DARK = 0xFF8b7019;
    private static final int GOLD_LIGHT = 0xFFe8c252;
    private static final int TEXT_LIGHT = 0xFFe8dcc8;
    private static final int TEXT_DIM = 0xFF9a8b70;
    private static final int BORDER_DARK = 0xFF1a1410;
    private static final int BORDER_LIGHT = 0xFF5c4a35;
    private static final int TOGGLE_ON = 0xFF4a8c3a;
    private static final int TOGGLE_OFF = 0xFF5c4535;
    private static final int ACCENT_RED = 0xFFa83232;

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
        raid.addOption(new BooleanOption("Fast Requeue", "Auto /pf on chest close",
                () -> config.toggleFastRequeue, v -> config.toggleFastRequeue = v));
        raid.addOption(new BooleanOption("Provoke Timer", "Show provoke timer",
                () -> config.provokeTimerToggle, v -> config.provokeTimerToggle = v));
        raid.addOption(new BooleanOption("Chiropterror Timer", "Show Chiropterror spawn timer",
                () -> config.chiropTimer, v -> config.chiropTimer = v));
        categories.add(raid);

        // Misc
        Category misc = new Category("Misc", GOLD);
        misc.addOption(new BooleanOption("Show Item Weights", "Display Wynnpool weights",
                () -> config.showWeight, v -> config.showWeight = v));
        misc.addOption(new BooleanOption("Show Stat Scales", "Display stat weights",
                () -> config.showScales, v -> config.showScales = v));
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
        context.fill(0, 0, this.width, this.height, BG_DARK);

        // Sidebar
        context.fill(0, 0, SIDEBAR_WIDTH, this.height, SIDEBAR_BG);
        context.fill(SIDEBAR_WIDTH - 2, 0, SIDEBAR_WIDTH, this.height, BORDER_DARK);

        // Main content area
        context.fill(SIDEBAR_WIDTH + 5, 5, this.width - 5, this.height - 5, BG_MEDIUM);
        context.fill(SIDEBAR_WIDTH + 8, 8, this.width - 8, this.height - 8, BG_LIGHT);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "WynnExtras",
                SIDEBAR_WIDTH + (this.width - SIDEBAR_WIDTH) / 2, 20, GOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, "Configuration",
                SIDEBAR_WIDTH + (this.width - SIDEBAR_WIDTH) / 2, 32, TEXT_DIM);

        // Sidebar categories
        context.drawCenteredTextWithShadow(this.textRenderer, "Categories", SIDEBAR_WIDTH / 2, 15, GOLD);
        context.fill(15, 27, SIDEBAR_WIDTH - 15, 28, GOLD_DARK);

        int categoryY = 40;
        int categoryHeight = 28;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            boolean hovered = mouseX >= 5 && mouseX < SIDEBAR_WIDTH - 5 &&
                             mouseY >= categoryY && mouseY < categoryY + categoryHeight;
            boolean selected = i == selectedCategory;

            if (selected) {
                context.fill(8, categoryY, SIDEBAR_WIDTH - 8, categoryY + categoryHeight, PARCHMENT);
                context.fill(8, categoryY + categoryHeight - 1, SIDEBAR_WIDTH - 8, categoryY + categoryHeight, BORDER_DARK);
                context.fill(8, categoryY + 2, 11, categoryY + categoryHeight - 2, cat.color);
            } else if (hovered) {
                context.fill(8, categoryY, SIDEBAR_WIDTH - 8, categoryY + categoryHeight, BG_LIGHT);
            }

            int textY = categoryY + (categoryHeight - 8) / 2;
            context.drawTextWithShadow(this.textRenderer, cat.name, 20, textY, selected ? TEXT_LIGHT : TEXT_DIM);

            categoryY += categoryHeight + 4;
        }

        // Content area
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);

            int contentX = SIDEBAR_WIDTH + 20;
            int contentY = HEADER_HEIGHT + 15;
            int contentWidth = this.width - SIDEBAR_WIDTH - 40;

            context.drawTextWithShadow(this.textRenderer, cat.name, contentX + 5, contentY, cat.color);
            context.fill(contentX, contentY + 14, contentX + contentWidth, contentY + 15, GOLD_DARK);

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

            // Scrollbar
            if (maxScroll > 0) {
                int scrollbarX = this.width - 18;
                int scrollbarHeight = listBottom - listTop;
                int thumbHeight = Math.max(30, (int)(scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll)));
                int thumbY = listTop + (int)((scrollbarHeight - thumbHeight) * (scrollOffset / maxScroll));

                context.fill(scrollbarX, listTop, scrollbarX + 8, listTop + scrollbarHeight, BORDER_DARK);
                context.fill(scrollbarX + 1, thumbY, scrollbarX + 7, thumbY + thumbHeight, GOLD);
            }
        }

        // Footer buttons
        int buttonY = this.height - 35;
        int saveX = this.width - 115;
        int cancelX = this.width - 225;

        boolean saveHovered = mouseX >= saveX && mouseX < saveX + 100 && mouseY >= buttonY && mouseY < buttonY + 24;
        boolean cancelHovered = mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + 24;

        // Save button
        context.fill(saveX, buttonY, saveX + 100, buttonY + 24, BORDER_DARK);
        context.fill(saveX + 1, buttonY + 1, saveX + 99, buttonY + 23, saveHovered ? TOGGLE_ON : PARCHMENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "Save & Close", saveX + 50, buttonY + 8, TEXT_LIGHT);

        // Cancel button
        context.fill(cancelX, buttonY, cancelX + 100, buttonY + 24, BORDER_DARK);
        context.fill(cancelX + 1, buttonY + 1, cancelX + 99, buttonY + 23, cancelHovered ? ACCENT_RED : PARCHMENT);
        context.drawCenteredTextWithShadow(this.textRenderer, "Cancel", cancelX + 50, buttonY + 8, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int buttonY = this.height - 35;
        int saveX = this.width - 115;
        int cancelX = this.width - 225;

        if (mouseX >= saveX && mouseX < saveX + 100 && mouseY >= buttonY && mouseY < buttonY + 24) {
            WynnExtrasConfig.save();
            this.client.setScreen(parent);
            return true;
        }

        if (mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= buttonY && mouseY < buttonY + 24) {
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

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                option.mouseReleased(click.x(), click.y(), click.button());
            }
        }
        return super.mouseReleased(click);
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
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                if (option.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        int modifiers = input.modifiers();

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            for (ConfigOption option : categories.get(selectedCategory).options) {
                if (option.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(input);
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
        void mouseReleased(double mouseX, double mouseY, int button) {}
        boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
        boolean charTyped(char chr, int modifiers) { return false; }
    }

    // Boolean toggle
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

            context.fill(x, y, x + width, y + height - 5, PARCHMENT);
            context.fill(x, y, x + width, y + 1, BORDER_LIGHT);
            context.fill(x, y + height - 6, x + width, y + height - 5, BORDER_DARK);

            context.drawTextWithShadow(textRenderer, name, x + 8, y + 8, TEXT_LIGHT);
            context.drawTextWithShadow(textRenderer, description, x + 8, y + 22, TEXT_DIM);

            int toggleX = x + width - 55;
            int toggleY = y + 12;
            boolean value = getter.get();

            context.fill(toggleX, toggleY, toggleX + 44, toggleY + 20, BORDER_DARK);
            context.fill(toggleX + 1, toggleY + 1, toggleX + 43, toggleY + 19, value ? TOGGLE_ON : TOGGLE_OFF);

            int knobX = value ? toggleX + 25 : toggleX + 2;
            context.fill(knobX, toggleY + 2, knobX + 18, toggleY + 18, BORDER_DARK);
            context.fill(knobX + 1, toggleY + 3, knobX + 17, toggleY + 17, GOLD);
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

    // Integer slider
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

            context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, BORDER_DARK);
            context.fill(sliderX + 1, sliderY + 1, sliderX + sliderWidth - 1, sliderY + sliderHeight - 1, BG_MEDIUM);

            int fillWidth = (int)((sliderWidth - 2) * percent);
            if (fillWidth > 0) {
                context.fill(sliderX + 1, sliderY + 1, sliderX + 1 + fillWidth, sliderY + sliderHeight - 1, GOLD_DARK);
            }

            int knobWidth = 10;
            int knobX = sliderX + (int)(sliderWidth * percent) - knobWidth / 2;
            int knobY = sliderY - 3;
            context.fill(knobX, knobY, knobX + knobWidth, knobY + 14, BORDER_DARK);
            context.fill(knobX + 1, knobY + 1, knobX + knobWidth - 1, knobY + 13, GOLD);

            context.drawTextWithShadow(textRenderer, String.valueOf(value), sliderX + sliderWidth + 10, sliderY, GOLD);

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
        void mouseReleased(double mouseX, double mouseY, int button) {
            dragging = false;
        }
    }

    // Enum dropdown
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

            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, BORDER_DARK);
            context.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1,
                    hovered ? PARCHMENT_LIGHT : PARCHMENT);

            String displayText = value.toString();
            if (displayText.length() > 12) {
                displayText = displayText.substring(0, 10) + "..";
            }
            context.drawCenteredTextWithShadow(textRenderer, displayText, buttonX + buttonWidth / 2 - 6, buttonY + 7, TEXT_LIGHT);

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
}
