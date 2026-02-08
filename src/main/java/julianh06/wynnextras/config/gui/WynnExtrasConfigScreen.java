package julianh06.wynnextras.config.gui;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * WynnExtras Configuration Screen
 *
 * HOW TO ADD/EDIT SETTINGS:
 * 1. Find the category in initCategories()
 * 2. Add options using the helper methods:
 *    - toggle("Name", "Description", getter, setter)
 *    - slider("Name", "Description", min, max, getter, setter)
 *    - sliderF("Name", "Description", min, max, step, getter, setter)
 *    - dropdown("Name", "Description", EnumClass.class, getter, setter)
 *    - stringList("Name", "Description", getter, setter)
 * 3. To add subcategories: category.sub("SubcategoryName").add(...)
 */
public class WynnExtrasConfigScreen extends Screen {
    private final Screen parent;
    private final WynnExtrasConfig config;

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

    // ==================== STATE ====================
    private int selectedCategory = 0;
    private int selectedCategoryColor = 0;
    private final List<Category> categories = new ArrayList<>();
    private double scrollOffset = 0;
    private double maxScroll = 0;
    private boolean scrollbarDragging = false;
    private double scrollbarDragOffset = 0;
    private int scrollbarY, scrollbarHeight, scrollbarThumbY, scrollbarThumbH;

    // Dropdown state
    private EnumOption<?> activeDropdown = null;
    private int dropdownX, dropdownY, dropdownWidth;
    private double dropdownScroll = 0;
    private static final int DROPDOWN_MAX_HEIGHT = 150;
    private static final int DROPDOWN_ITEM_HEIGHT = 22;

    public WynnExtrasConfigScreen(Screen parent) {
        super(Text.literal("WynnExtras Configuration"));
        this.parent = parent;
        this.config = WynnExtrasConfig.INSTANCE;
        initCategories();
    }

    // ==================== CATEGORY DEFINITIONS ====================
    private void initCategories() {
        categories.clear();

        // ===== RAIDS =====
        category("Raiding", GOLD_DARK)
            .add(toggle("Timestamps", "Show timestamps during raids",
                () -> config.toggleRaidTimestamps, v -> config.toggleRaidTimestamps = v))
            .add(toggle("Fast Requeue", "Auto /pf on chest close",
                () -> config.toggleFastRequeue, v -> config.toggleFastRequeue = v))
            .add(toggle("Chiropterror Timer", "Spawn timer for the Chiropterror boss in TNA light room",
                    () -> config.chiropTimer, v -> config.chiropTimer = v))
            .sub("Loot Tracker")
                .add(toggle("Enable Tracker", "Track raid loot drops",
                    () -> config.toggleRaidLootTracker, v -> config.toggleRaidLootTracker = v))
                .add(toggle("Render in HUD", "Render the Overlay in the HUD",
                    () -> config.raidLootTrackerRenderInHud, v -> config.raidLootTrackerRenderInHud = v))
                .add(toggle("Render in Inventory", "Render the Overlay while in the inventory",
                        () -> config.raidLootTrackerRenderInInventory, v -> config.raidLootTrackerRenderInInventory = v))
                .add(toggle("Render in Chat", "Render the Overlay while the chat is open",
                        () -> config.raidLootTrackerRenderInChat, v -> config.raidLootTrackerRenderInChat = v))
                .add(toggle("Only Near Chest", "Show only near reward chest",
                    () -> config.raidLootTrackerOnlyNearChest, v -> config.raidLootTrackerOnlyNearChest = v))
                .add(toggle("Compact Mode", "Use compact display",
                    () -> config.raidLootTrackerCompact, v -> config.raidLootTrackerCompact = v))
                .add(toggle("Show Background", "Show dark background",
                        () -> config.raidLootTrackerBackground, v -> config.raidLootTrackerBackground = v));

        // ===== COMBAT =====
        category("Combat", 0xFFfda216)
            .sub("Shaman")
                .add(toggle("Show Totem Range", "Display totem range circle",
                        () -> config.totemRangeVisualizerToggle, v -> config.totemRangeVisualizerToggle = v))
                .add(sliderF("Totem Radius", "Size of totem circle",
                        1f, 30f, 0.5f, () -> config.totemRange, v -> config.totemRange = v))
                .add(dropdown("Totem Color", "Circle color",
                        WynnExtrasConfig.TextColor.class, () -> config.totemColor, v -> config.totemColor = v))
                .add(sliderF("Eldritch Radius", "Eldritch call range",
                        1f, 30f, 0.5f, () -> config.eldritchCallRange, v -> config.eldritchCallRange = v))
                .add(dropdown("Eldritch Color", "Circle color",
                        WynnExtrasConfig.TextColor.class, () -> config.eldritchCallColor, v -> config.eldritchCallColor = v))
            .sub("Provoke Timer [WIP]")
                .add(toggle("Enable Provoke Timer", "Show provoke timer",
                        () -> config.provokeTimerToggle, v -> config.provokeTimerToggle = v))
                .add(dropdown("Timer Color", "Timer text color",
                        WynnExtrasConfig.TextColor.class, () -> config.provokeTimerColor, v -> config.provokeTimerColor = v));

        // ===== INVENTORY =====
        Category invCategory = category("Inventory", 0xFFea1219);

        if (FabricLoader.getInstance().isModLoaded("wynnventory")) {
            invCategory.add(toggle("Wynnventory price overlay in bank", "Enable the Wynnventory price overlay in the bank overlay",
                    () -> config.wynnventoryOverlay, v -> config.wynnventoryOverlay = v));
        }

        invCategory.sub("Bank Overlay")
            .add(toggle("Enable Bank Overlay", "Custom Bank Overlay",
                    () -> config.toggleBankOverlay, v -> config.toggleBankOverlay = v))
            .add(toggle("Smooth Scroll", "Smooth scrolling",
                    () -> config.smoothScrollToggle, v -> config.smoothScrollToggle = v))
            .add(toggle("Quick Toggle", "Show quick toggle button",
                    () -> config.bankQuickToggle, v -> config.bankQuickToggle = v))
            .add(toggle("Dark Mode", "Dark bank theme",
                    () -> config.darkmodeToggle, v -> config.darkmodeToggle = v))
            .add(slider("Rarity BG Alpha", "Item rarity background opacity",
                    0, 255, () -> config.wynntilsItemRarityBackgroundAlpha, v -> config.wynntilsItemRarityBackgroundAlpha = v))
            .add(slider("Max Rows", "The maximum amount of rows (lower can reduce lag)",
                    2, 3, () -> config.bankOverlayMaxRows, v -> config.bankOverlayMaxRows = v))
            .add(slider("Max Columns", "The maximum amount of columns (lower can reduce lag)",
                    2, 3, () -> config.bankOverlayMaxColumns, v -> config.bankOverlayMaxColumns = v))
            .sub("Tooltips")
                .add(toggle("Item Weights", "Show Wynnpool weights for mythic items",
                    () -> config.showWeight, v -> config.showWeight = v))
                .add(toggle("Stat Scales", "Show weights for each stat",
                    () -> config.showScales, v -> config.showScales = v))
            .sub("Trade Market")
                .add(toggle("Trade market price summary", "Trade market overlay that shows you how much money you can claim",
                    () -> config.tradeMarketOverlay, v -> config.tradeMarketOverlay = v))
                .add(toggle("Price overlay background", "Show a dark background for the price overlay",
                    () -> config.tradeMarketOverlayBackground, v -> config.tradeMarketOverlayBackground = v))
            .sub("Crafting")
                .add(toggle("Crafting helper", "Crafting Helper toggle",
                    () -> config.craftingHelperOverlay, v -> config.craftingHelperOverlay = v))
                .add(toggle("Dynamic textures in crafting helper", "Use dynamic material textures, supports Variants-CIT texture packs",
                    () -> config.craftingDynamicTextures, v -> config.craftingDynamicTextures = v))
                .add(toggle("Crafting preview", "Crafting preview toggle",
                () -> config.craftingPreviewOverlay, v -> config.craftingPreviewOverlay = v));

                // ===== CHAT =====
        category("Chat", 0xFFc80069)
            .add(stringList("Blocked Words", "Hide messages with these",
                    () -> config.blockedWords, v -> config.blockedWords = v, "Words"))
            .add(toggle("Quick PV/GV Access", "Click on a players name or guild to open the pv/gv!",
                    () -> config.chatClickOpensPV, v -> config.chatClickOpensPV = v))
            .sub("Notifications")
                .add(stringListDual("Notifier Words", "Trigger word and display text",
                        () -> config.notifierWords, v -> config.notifierWords = v, "Words"))
                .add(sliderF("Duration (ms)", "How long notification shows",
                        500, 10000, 100, () -> (float) config.textDurationInMs, v -> config.textDurationInMs = v.intValue()))
                .add(dropdown("Text Color", "Notification color",
                        WynnExtrasConfig.TextColor.class, () -> config.textColor, v -> config.textColor = v))
                .add(dropdown("Sound", "Notification sound",
                        WynnExtrasConfig.NotificationSound.class, () -> config.notificationSound, v -> config.notificationSound = v))
                .add(slider("Volume", "Sound volume",
                        0, 200, () -> (int)(config.soundVolume), v -> config.soundVolume = v))
                .add(slider("Pitch", "Sound pitch",
                        0, 200, () -> (int)(config.soundPitch), v -> config.soundPitch = v))
                .add(button("Sound Test", "Click the button to test the sound",
                    v -> McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.notificationSound.getSoundId())), config.soundVolume / 100, config.soundPitch / 100), "Test"))
            .sub("Premade Notifications")
                .add(toggle("Lost Eye", "Lost Eye in TNA light room",
                    () -> config.lostEye, v -> config.lostEye = v))
                .add(toggle("+1 Goo", "+1 Goo in NOTG Slime Gathering",
                    () -> config.oneGoo, v -> config.oneGoo = v))
                .add(toggle("+2 Goos", "+2 Goos in NOTG Slime Gathering",
                    () -> config.twoGoo, v -> config.twoGoo = v))
                .add(toggle("Next Soul", "When next soul is ready in TNA tree room",
                    () -> config.soul, v -> config.soul = v))
                .add(toggle("+1 Void Matter", "+1 Void Matter in TNA void gathering room",
                    () -> config.voidMatter, v -> config.voidMatter = v))
                .add(toggle("Kill the voidholes", "When holes can be attacked in TNA gathering room",
                    () -> config.fourOutOfFiveVoidMatter, v -> config.fourOutOfFiveVoidMatter = v))
                .add(toggle("+1 Crystal", "+1 Crystal in NOL gathering room",
                    () -> config.oneLightCrystal, v -> config.oneLightCrystal = v))
                .add(toggle("+2 Crystals", "+2 Crystals in NOL gathering room",
                    () -> config.twoLightCrystal, v -> config.twoLightCrystal = v))
                .add(toggle("Upper platform spawned", "Upper platform spawn in NOTG minibosses",
                    () -> config.notgUpperPlatform, v -> config.notgUpperPlatform = v))
                .add(toggle("Lower platform spawned", "Lower platform spawn in NOTG minibosses",
                    () -> config.notgLowerPlatform, v -> config.notgLowerPlatform = v)
                );

        // ===== Player Hider =====
        category("Player Hider", 0xFF673190)
            .add(toggle("Enable Player Hider", "Enable the Player Hider",
                    () -> config.playerHiderToggle, v -> config.playerHiderToggle = v))
            .add(slider("Hide Distance", "Max distance to hide",
                    1, 20, () -> config.maxHideDistance, v -> config.maxHideDistance = v))
            .add(toggle("Hide All Players", "Hide all players in range",
                    () -> config.hideAllPlayers, v -> config.hideAllPlayers = v))
            .add(toggle("Hide All Players while in Wars", "Hide all players during wars",
                    () -> config.hideAllPlayersInWar, v -> config.hideAllPlayersInWar = v))
            .add(stringList("Hidden Players", "Always hide these players",
                    () -> config.hiddenPlayers, v -> config.hiddenPlayers = v, "Players"));


        // ===== MISC =====
        category("Misc", 0xFF0872bc)
                .add(toggle("Custom GUI Scale", "Use different scale for WE menus",
                        () -> config.differentGUIScale, v -> config.differentGUIScale = v))
                .add(slider("GUI Scale", "Custom GUI scale value",
                        1, 5, () -> config.customGUIScale, v -> config.customGUIScale = v))
                .add(toggle("Skip Front View", "Skip front-facing view in 3rd person",
                        () -> config.removeFrontPersonView, v -> config.removeFrontPersonView = v))
                .add(toggle("PV Dark Mode", "Dark theme for profile viewer",
                        () -> config.pvDarkmodeToggle, v -> config.pvDarkmodeToggle = v))
                .add(toggle("Financial Advice", "Receive smart financial advise in the Identifier menu",
                        () -> config.sourceOfTruthToggle, v -> config.sourceOfTruthToggle = v))
                .add(toggle("Territory Estimates", "Show territory estimates in the Wynntils guild map",
                        () -> config.territoryEstimateToggle, v -> config.territoryEstimateToggle = v));
    }

    // ==================== BUILDER HELPERS ====================
    private Category category(String name, int color) {
        Category cat = new Category(name, color);
        categories.add(cat);
        return cat;
    }

    private ConfigOption toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        return new BooleanOption(name, desc, get, set);
    }

    private ConfigOption slider(String name, String desc, int min, int max, Supplier<Integer> get, Consumer<Integer> set) {
        return new SliderOption(name, desc, min, max, get, set);
    }

    private ConfigOption sliderF(String name, String desc, float min, float max, float step, Supplier<Float> get, Consumer<Float> set) {
        return new FloatSliderOption(name, desc, min, max, step, get, set);
    }

    private <T extends Enum<T>> ConfigOption dropdown(String name, String desc, Class<T> cls, Supplier<T> get, Consumer<T> set) {
        return new EnumOption<>(name, desc, cls, get, set);
    }

    private ConfigOption stringList(String name, String desc, Supplier<List<String>> get, Consumer<List<String>> set, String itemName) {
        return new StringListOption(name, desc, get, set, itemName, false);
    }

    private ConfigOption stringListDual(String name, String desc, Supplier<List<String>> get, Consumer<List<String>> set, String itemName) {
        return new StringListOption(name, desc, get, set, itemName, true);
    }

    private ConfigOption button(String name, String desc, Consumer<Void> action, String buttonText) {
        return new ButtonOption(name, desc, action, buttonText);
    }

    // ==================== SCREEN LIFECYCLE ====================
    @Override
    protected void init() {
        updateMaxScroll();
    }

    private void updateMaxScroll() {
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            int contentHeight = categories.get(selectedCategory).getTotalHeight();
            int visibleHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT - 40;
            maxScroll = Math.max(0, contentHeight - visibleHeight);
        }
    }

    // ==================== RENDERING ====================
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Normal render
        ctx.fill(0, 0, width, height, BG_DARK);

        // Disable hover when dropdown is open
        int effectiveMouseX = activeDropdown != null ? -1 : mouseX;
        int effectiveMouseY = activeDropdown != null ? -1 : mouseY;

        drawSidebar(ctx, effectiveMouseX, effectiveMouseY);
        drawMainPanel(ctx, effectiveMouseX, effectiveMouseY);
        drawFooter(ctx, effectiveMouseX, effectiveMouseY);

        // Dropdown renders on top of everything
        if (activeDropdown != null) {
            renderDropdownOverlay(ctx, mouseX, mouseY);
        }
    }

    private void drawDiamond(DrawContext context, int cx, int cy, int size, int color) {
        for (int i = 0; i <= size; i++) {
            context.fill(cx - i, cy - size + i, cx + i + 1, cy - size + i + 1, color);
            context.fill(cx - i, cy + size - i, cx + i + 1, cy + size - i + 1, color);
        }
    }

    private void drawSidebar(DrawContext ctx, int mouseX, int mouseY) {
        // Clean solid background
        ctx.fill(0, 0, SIDEBAR_WIDTH, height, BG_MEDIUM);

        // Right border
        ctx.fill(SIDEBAR_WIDTH - 2, 0, SIDEBAR_WIDTH, height, BORDER_DARK);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "Categories", SIDEBAR_WIDTH / 2, 18, GOLD);
        ctx.fill(20, 32, SIDEBAR_WIDTH - 20, 33, GOLD_DARK);

        int y = 45;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            boolean hovered = mouseX >= 8 && mouseX < SIDEBAR_WIDTH - 8 && mouseY >= y && mouseY < y + 22;
            boolean selected = i == selectedCategory;

            // Background
            if (selected) {
                ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + 22, PARCHMENT);
                ctx.fill(8, y, 12, y + 22, cat.color);
            } else if (hovered) {
                ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + 22, BG_LIGHT);
            }

            // Color dot and text
            drawDiamond(ctx, 20, y + 10, 4, cat.color);
            //ctx.fill(18, y + 9, 24, y + 15, cat.color);
            ctx.drawTextWithShadow(textRenderer, cat.name, 30, y + 7, selected ? TEXT_LIGHT : TEXT_DIM);

            y += 28;
        }
    }

    private void drawMainPanel(DrawContext ctx, int mouseX, int mouseY) {
        int panelX = SIDEBAR_WIDTH + 5;
        int panelW = width - SIDEBAR_WIDTH - 10;

        ctx.fill(panelX, 5, panelX + panelW, height - 5, BG_LIGHT);

        if (selectedCategory < 0 || selectedCategory >= categories.size()) return;
        Category cat = categories.get(selectedCategory);

        selectedCategoryColor = cat.color;

        // Header
        ctx.fill(panelX + 5, 10, panelX + panelW - 5, HEADER_HEIGHT, PARCHMENT);
        ctx.fill(panelX + 5, 10, panelX + panelW - 5, 12, cat.color);
        ctx.drawCenteredTextWithShadow(textRenderer, "WynnExtras", panelX + panelW / 2, 19, TEXT_LIGHT);
        ctx.drawCenteredTextWithShadow(textRenderer, "Configuration", panelX + panelW / 2, 32, TEXT_DIM);
        ctx.fill(panelX + 15, 48, panelX + panelW - 15, 50, cat.color);

        drawDiamond(ctx, panelX + 11, 4 + HEADER_HEIGHT / 2, 3, cat.color);
        drawDiamond(ctx, panelX + panelW - 11, 4 + HEADER_HEIGHT / 2, 3, cat.color);

        int contentX = panelX + 15;
        int contentW = panelW - 30;
        int listTop = HEADER_HEIGHT + 15;
        int listBottom = height - FOOTER_HEIGHT - 10;

        // Category header

        //ctx.fill(contentX, listTop - 3, contentX + 6, listTop + 9, cat.color);
        drawDiamond(ctx, contentX + 5, listTop + 2, 5, cat.color);
        ctx.drawTextWithShadow(textRenderer, cat.name, contentX + 16, listTop - 1, cat.color);
        ctx.fill(contentX, listTop + 12, contentX + contentW, listTop + 13, cat.color);

        ctx.enableScissor(panelX, listTop + 15, panelX + panelW - 15, listBottom);

        int y = listTop + 20 - (int)scrollOffset;

        for (SubCategory sub : cat.subCategories) {
            y = renderSubCategory(ctx, sub, contentX, y, contentW, mouseX, mouseY, listTop + 15, listBottom);
        }

        for (ConfigOption opt : cat.options) {
            if (y + OPTION_HEIGHT > listTop && y < listBottom) {
                boolean hovered = mouseX >= contentX && mouseX < contentX + contentW && mouseY >= y && mouseY < y + OPTION_HEIGHT - 5;
                opt.render(ctx, contentX, y, contentW, OPTION_HEIGHT, mouseX, mouseY, hovered, cat.color);
            }
            y += OPTION_HEIGHT + OPTION_SPACING;
        }

        ctx.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int sbX = panelX + panelW - 12;
            scrollbarY = listTop + 15;
            scrollbarHeight = listBottom - listTop - 20;
            scrollbarThumbH = Math.max(30, (int)(scrollbarHeight * scrollbarHeight / (scrollbarHeight + maxScroll)));
            scrollbarThumbY = scrollbarY + (int)((scrollbarHeight - scrollbarThumbH) * (scrollOffset / maxScroll));

            ctx.fill(sbX, scrollbarY, sbX + 6, scrollbarY + scrollbarHeight, BORDER_DARK);
            ctx.fill(sbX + 1, scrollbarThumbY, sbX + 5, scrollbarThumbY + scrollbarThumbH, cat.color);
        }
    }

    private int renderSubCategory(DrawContext ctx, SubCategory sub, int x, int y, int w, int mX, int mY, int top, int bot) {
        if (y + SUBCATEGORY_HEADER_HEIGHT > top && y < bot) {
            boolean hovered = mX >= x && mX < x + w && mY >= y && mY < y + SUBCATEGORY_HEADER_HEIGHT;
            ctx.fill(x, y, x + w, y + SUBCATEGORY_HEADER_HEIGHT, hovered ? PARCHMENT_LIGHT : SUBCATEGORY_BG);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + SUBCATEGORY_HEADER_HEIGHT - 1, x + w, y + SUBCATEGORY_HEADER_HEIGHT, BORDER_DARK);

            String arrow = sub.expanded ? "\u25BC" : "\u25B6";
            ctx.drawTextWithShadow(textRenderer, arrow, x + 8, y + 8, selectedCategoryColor);
            ctx.drawTextWithShadow(textRenderer, sub.name, x + 22, y + 8, TEXT_LIGHT);
        }
        y += SUBCATEGORY_HEADER_HEIGHT + 5;

        if (sub.expanded) {
            for (ConfigOption opt : sub.options) {
                if (y + OPTION_HEIGHT > top && y < bot) {
                    boolean hovered = mX >= x + 8 && mX < x + w && mY >= y && mY < y + OPTION_HEIGHT - 5;
                    ctx.fill(x, y, x + 4, y + OPTION_HEIGHT - 5, selectedCategoryColor);
                    opt.render(ctx, x + 8, y, w - 8, OPTION_HEIGHT, mX, mY, hovered, selectedCategoryColor);
                }
                y += OPTION_HEIGHT + OPTION_SPACING;
            }
        }
        return y;
    }

    // Dropdown overlay - renders in place on top of content
    private void renderDropdownOverlay(DrawContext ctx, int mouseX, int mouseY) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 300);

        Object[] values = activeDropdown.enumClass.getEnumConstants();
        int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
        int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
        boolean needsScroll = totalContentH > DROPDOWN_MAX_HEIGHT;

        // Position near the button
        int ddW = dropdownWidth + (needsScroll ? 10 : 0);
        int ddX = dropdownX;
        int ddY = dropdownY;

        // Make sure dropdown fits on screen
        if (ddY + visibleH > height - 10) {
            ddY = dropdownY - visibleH - 24;
        }

        // Clamp scroll
        double maxScroll = Math.max(0, totalContentH - visibleH);
        dropdownScroll = MathHelper.clamp(dropdownScroll, 0, maxScroll);

        // Outer frame - solid border
        ctx.fill(ddX - 3, ddY - 3, ddX + ddW + 3, ddY + visibleH + 3, BORDER_DARK);
        ctx.fill(ddX - 2, ddY - 2, ddX + ddW + 2, ddY + visibleH + 2, selectedCategoryColor);
        ctx.fill(ddX - 1, ddY - 1, ddX + ddW + 1, ddY + visibleH + 1, BG_MEDIUM);

        // Content area - FULLY OPAQUE solid background
        ctx.fill(ddX, ddY, ddX + ddW, ddY + visibleH, PARCHMENT);

        // Scissor for scrolling content
        ctx.enableScissor(ddX, ddY, ddX + ddW - (needsScroll ? 8 : 0), ddY + visibleH);

        for (int i = 0; i < values.length; i++) {
            int iy = ddY + i * DROPDOWN_ITEM_HEIGHT - (int)dropdownScroll;

            // Skip if out of visible area
            if (iy + DROPDOWN_ITEM_HEIGHT < ddY || iy > ddY + visibleH) continue;

            boolean hovered = mouseX >= ddX && mouseX < ddX + ddW - (needsScroll ? 8 : 0)
                    && mouseY >= Math.max(ddY, iy) && mouseY < Math.min(ddY + visibleH, iy + DROPDOWN_ITEM_HEIGHT);
            boolean selected = values[i].equals(activeDropdown.getter.get());

            // Item background - fully opaque
            int itemBg = selected ? selectedCategoryColor : (hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(ddX, iy, ddX + ddW - (needsScroll ? 8 : 0), iy + DROPDOWN_ITEM_HEIGHT, itemBg);

            // Separator
            if (i > 0) {
                ctx.fill(ddX + 8, iy, ddX + ddW - (needsScroll ? 16 : 8), iy + 1, BG_LIGHT);
            }

            String text = values[i].toString();
            if (text.length() > 14) text = text.substring(0, 12) + "..";
            ctx.drawTextWithShadow(textRenderer, text, ddX + 8, iy + 7, selected ? GOLD : TEXT_LIGHT);
        }

        ctx.disableScissor();

        // Scrollbar if needed
        if (needsScroll) {
            int sbX = ddX + ddW - 6;
            int sbH = visibleH;
            int thumbH = Math.max(20, (int)(sbH * visibleH / (double)totalContentH));
            int thumbY = ddY + (int)((sbH - thumbH) * (dropdownScroll / maxScroll));

            ctx.fill(sbX, ddY, sbX + 5, ddY + sbH, BG_DARK);
            ctx.fill(sbX + 1, thumbY, sbX + 4, thumbY + thumbH, selectedCategoryColor);
        }

        ctx.getMatrices().pop();
    }

    private void drawFooter(DrawContext ctx, int mouseX, int mouseY) {
        int footerY = height - FOOTER_HEIGHT + 5;

        if (selectedCategory < 0 || selectedCategory >= categories.size()) return;
        Category cat = categories.get(selectedCategory);

        ctx.fill(SIDEBAR_WIDTH + 10, footerY, width - 10, footerY + 1, cat.color);

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

    // ==================== INPUT HANDLING ====================
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (activeDropdown != null) {
            Object[] values = activeDropdown.enumClass.getEnumConstants();
            int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
            int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
            boolean needsScroll = totalContentH > DROPDOWN_MAX_HEIGHT;
            int ddW = dropdownWidth + (needsScroll ? 10 : 0);
            int ddX = dropdownX;
            int ddY = dropdownY;

            // Match the flip logic from render
            if (ddY + visibleH > height - 10) {
                ddY = dropdownY - visibleH - 24;
            }

            // Check if click is inside dropdown area
            if (mx >= ddX && mx < ddX + ddW && my >= ddY && my < ddY + visibleH) {
                for (int i = 0; i < values.length; i++) {
                    int iy = ddY + i * DROPDOWN_ITEM_HEIGHT - (int)dropdownScroll;
                    if (iy < ddY - DROPDOWN_ITEM_HEIGHT || iy > ddY + visibleH) continue;

                    if (my >= Math.max(ddY, iy) && my < Math.min(ddY + visibleH, iy + DROPDOWN_ITEM_HEIGHT)
                            && mx < ddX + ddW - (needsScroll ? 8 : 0)) {
                        activeDropdown.setValueByIndex(i);
                        activeDropdown = null;
                        dropdownScroll = 0;
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                }
                return true; // Clicked inside but not on item (scrollbar area)
            }

            activeDropdown = null;
            dropdownScroll = 0;
            return true;
        }

        int btnY = height - 35;
        if (my >= btnY && my < btnY + 24) {
            //======== Save & Close =========
            if (mx >= width - 115 && mx < width - 15) {
                WynnExtrasConfig.save();
                WynnExtrasConfig.load();
                client.setScreen(parent);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            //======== Cancel =========
            if (mx >= width - 225 && mx < width - 125) {
                WynnExtrasConfig.load();
                client.setScreen(parent);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (mx >= 8 && mx < SIDEBAR_WIDTH - 8) {
            int y = 45;
            for (int i = 0; i < categories.size(); i++) {
                if (my >= y && my < y + 24) {
                    selectedCategory = i;
                    scrollOffset = 0;
                    updateMaxScroll();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                y += 28;
            }
        }

        // Scrollbar
        if (maxScroll > 0 && mx >= width - 17 && mx < width - 5) {
            if (my >= scrollbarThumbY && my < scrollbarThumbY + scrollbarThumbH) {
                scrollbarDragging = true;
                scrollbarDragOffset = my - scrollbarThumbY;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else if (my >= scrollbarY && my < scrollbarY + scrollbarHeight) {
                double clickPercent = (my - scrollbarY - scrollbarThumbH / 2.0) / (scrollbarHeight - scrollbarThumbH);
                scrollOffset = MathHelper.clamp(clickPercent * maxScroll, 0, maxScroll);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentX = SIDEBAR_WIDTH + 20;
            int contentW = width - SIDEBAR_WIDTH - 50;
            int listTop = HEADER_HEIGHT + 30;
            int listBot = height - FOOTER_HEIGHT - 10;

            int y = listTop - (int)scrollOffset + 5;

            for (SubCategory sub : cat.subCategories) {
                if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + SUBCATEGORY_HEADER_HEIGHT) && mx >= contentX && mx < contentX + contentW) {
                    sub.expanded = !sub.expanded;
                    updateMaxScroll();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                y += SUBCATEGORY_HEADER_HEIGHT + 5;

                if (sub.expanded) {
                    for (ConfigOption opt : sub.options) {
                        if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + OPTION_HEIGHT)) {
                            if (opt.mouseClicked(mx, my, contentX + 8, y, contentW - 8, OPTION_HEIGHT, btn)) return true;
                        }
                        y += OPTION_HEIGHT + OPTION_SPACING;
                    }
                }
            }

            for (ConfigOption opt : cat.options) {
                if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + OPTION_HEIGHT)) {
                    if (opt.mouseClicked(mx, my, contentX, y, contentW, OPTION_HEIGHT, btn)) return true;
                }
                y += OPTION_HEIGHT + OPTION_SPACING;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        scrollbarDragging = false;
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            for (SubCategory sub : cat.subCategories) {
                for (ConfigOption opt : sub.options) opt.mouseReleased(mx, my, btn);
            }
            for (ConfigOption opt : cat.options) opt.mouseReleased(mx, my, btn);
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (scrollbarDragging && maxScroll > 0) {
            double newThumbY = my - scrollbarDragOffset;
            double percent = (newThumbY - scrollbarY) / (scrollbarHeight - scrollbarThumbH);
            scrollOffset = MathHelper.clamp(percent * maxScroll, 0, maxScroll);
            return true;
        }

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentX = SIDEBAR_WIDTH + 20;
            int contentW = width - SIDEBAR_WIDTH - 50;
            int y = HEADER_HEIGHT + 30 - (int)scrollOffset;

            for (SubCategory sub : cat.subCategories) {
                y += SUBCATEGORY_HEADER_HEIGHT + 5;
                if (sub.expanded) {
                    for (ConfigOption opt : sub.options) {
                        if (opt.mouseDragged(mx, my, contentX + 8, y, contentW - 8, OPTION_HEIGHT)) return true;
                        y += OPTION_HEIGHT + OPTION_SPACING;
                    }
                }
            }

            for (ConfigOption opt : cat.options) {
                if (opt.mouseDragged(mx, my, contentX, y, contentW, OPTION_HEIGHT)) return true;
                y += OPTION_HEIGHT + OPTION_SPACING;
            }
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (activeDropdown != null) {
            // Scroll the dropdown
            Object[] values = activeDropdown.enumClass.getEnumConstants();
            int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
            int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
            double maxDropScroll = Math.max(0, totalContentH - visibleH);
            dropdownScroll = MathHelper.clamp(dropdownScroll - vAmt * 20, 0, maxDropScroll);
            return true;
        }
        if (mx > SIDEBAR_WIDTH) {
            scrollOffset = MathHelper.clamp(scrollOffset - vAmt * 30, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (activeDropdown != null && key == 256) {
            activeDropdown = null;
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // ==================== DATA CLASSES ====================
    private class Category {
        final String name;
        final int color;
        final List<ConfigOption> options = new ArrayList<>();
        final List<SubCategory> subCategories = new ArrayList<>();
        private SubCategory currentSub = null;

        Category(String name, int color) { this.name = name; this.color = color; }

        Category add(ConfigOption opt) {
            if (currentSub != null) currentSub.options.add(opt);
            else options.add(opt);
            return this;
        }

        Category sub(String name) {
            currentSub = new SubCategory(name);
            subCategories.add(currentSub);
            return this;
        }

        int getTotalHeight() {
            int h = 0;
            for (SubCategory s : subCategories) {
                h += SUBCATEGORY_HEADER_HEIGHT + 5;
                if (s.expanded) h += s.options.size() * (OPTION_HEIGHT + OPTION_SPACING);
            }
            h += options.size() * (OPTION_HEIGHT + OPTION_SPACING);
            return h + 20;
        }
    }

    private static class SubCategory {
        final String name;
        final List<ConfigOption> options = new ArrayList<>();
        boolean expanded = true;
        SubCategory(String name) { this.name = name;}
    }

    // ==================== CONFIG OPTIONS ====================
    private static abstract class ConfigOption {
        final String name, desc;
        ConfigOption(String name, String desc) { this.name = name; this.desc = desc; }
        abstract void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor);
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) { return false; }
        boolean mouseReleased(double mx, double my, int btn) { return false; }
        boolean mouseDragged(double mx, double my, int x, int y, int w, int h) { return false; }
    }

    private static class BooleanOption extends ConfigOption {
        final Supplier<Boolean> getter;
        final Consumer<Boolean> setter;

        BooleanOption(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(name, desc);
            this.getter = get;
            this.setter = set;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, desc, x + 8, y + 22, TEXT_DIM);

            int tx = x + w - 55, ty = y + 12;
            boolean val = getter.get();
            ctx.fill(tx, ty, tx + 44, ty + 20, BORDER_DARK);
            ctx.fill(tx + 1, ty + 1, tx + 43, ty + 19, val ? TOGGLE_ON : TOGGLE_OFF);
            int kx = val ? tx + 24 : tx + 2;
            ctx.fill(kx, ty + 2, kx + 18, ty + 18, BORDER_DARK);
            ctx.fill(kx + 1, ty + 3, kx + 17, ty + 17, GOLD);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            int tx = x + w - 45, ty = y + 12;
            if (mx >= tx && mx < tx + 44 && my >= ty && my < ty + 20) {
                setter.accept(!getter.get());
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }
    }

    private static class SliderOption extends ConfigOption {
        final int min, max;
        final Supplier<Integer> getter;
        final Consumer<Integer> setter;
        boolean dragging = false;
        int sliderX, sliderW = 120;

        SliderOption(String name, String desc, int min, int max, Supplier<Integer> get, Consumer<Integer> set) {
            super(name, desc);
            this.min = min; this.max = max; this.getter = get; this.setter = set;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, desc, x + 8, y + 22, TEXT_DIM);

            sliderX = x + w - 130;
            int sy = y + 15, val = getter.get();
            float pct = (float)(val - min) / (max - min);

            ctx.fill(sliderX, sy, sliderX + sliderW, sy + 8, BORDER_DARK);
            ctx.fill(sliderX + 1, sy + 1, sliderX + sliderW - 1, sy + 7, BG_MEDIUM);
            int fill = (int)((sliderW - 2) * pct);
            if (fill > 0) ctx.fill(sliderX + 1, sy + 1, sliderX + 1 + fill, sy + 7, categoryColor);

            int kx = sliderX + (int)(sliderW * pct) - 5;
            ctx.fill(kx, sy - 3, kx + 10, sy + 11, BORDER_DARK);
            ctx.fill(kx + 1, sy - 2, kx + 9, sy + 10, GOLD);

            ctx.drawTextWithShadow(tr, String.valueOf(val), x + w - 135 - MinecraftClient.getInstance().textRenderer.getWidth(String.valueOf(val)), sy, GOLD);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            int sy = y + 10;
            if (mx >= sliderX - 5 && mx < sliderX + sliderW + 10 && my >= sy && my < sy + 20) {
                dragging = true;
                updateValue(mx);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }

        @Override
        boolean mouseReleased(double mx, double my, int btn) {
            if (dragging) { dragging = false; return true; }
            return false;
        }

        @Override
        boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
            if (dragging) { updateValue(mx); return true; }
            return false;
        }

        void updateValue(double mx) {
            float pct = MathHelper.clamp((float)(mx - sliderX) / sliderW, 0, 1);
            setter.accept(min + Math.round(pct * (max - min)));
        }
    }

    private static class FloatSliderOption extends ConfigOption {
        final float min, max, step;
        final Supplier<Float> getter;
        final Consumer<Float> setter;
        boolean dragging = false;
        int sliderX, sliderW = 120;

        FloatSliderOption(String name, String desc, float min, float max, float step, Supplier<Float> get, Consumer<Float> set) {
            super(name, desc);
            this.min = min; this.max = max; this.step = step; this.getter = get; this.setter = set;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, desc, x + 8, y + 22, TEXT_DIM);

            sliderX = x + w - 130;
            int sy = y + 15;
            float val = getter.get();
            float pct = (val - min) / (max - min);

            ctx.fill(sliderX, sy, sliderX + sliderW, sy + 8, BORDER_DARK);
            ctx.fill(sliderX + 1, sy + 1, sliderX + sliderW - 1, sy + 7, BG_MEDIUM);
            int fill = (int)((sliderW - 2) * pct);
            if (fill > 0) ctx.fill(sliderX + 1, sy + 1, sliderX + 1 + fill, sy + 7, categoryColor);

            int kx = sliderX + (int)(sliderW * pct) - 5;
            ctx.fill(kx, sy - 3, kx + 10, sy + 11, BORDER_DARK);
            ctx.fill(kx + 1, sy - 2, kx + 9, sy + 10, GOLD);

            String valStr = step >= 1 ? String.valueOf((int)val) : String.format("%.1f", val);
            ctx.drawTextWithShadow(tr, valStr, x + w - 135 - MinecraftClient.getInstance().textRenderer.getWidth(valStr), sy, GOLD);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            int sy = y + 10;
            if (mx >= sliderX - 5 && mx < sliderX + sliderW + 10 && my >= sy && my < sy + 20) {
                dragging = true;
                updateValue(mx);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }

        @Override
        boolean mouseReleased(double mx, double my, int btn) {
            if (dragging) { dragging = false; return true; }
            return false;
        }

        @Override
        boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
            if (dragging) { updateValue(mx); return true; }
            return false;
        }

        void updateValue(double mx) {
            float pct = MathHelper.clamp((float)(mx - sliderX) / sliderW, 0, 1);
            float rawVal = min + pct * (max - min);
            float stepped = Math.round(rawVal / step) * step;
            setter.accept(MathHelper.clamp(stepped, min, max));
        }
    }

    private class EnumOption<T extends Enum<T>> extends ConfigOption {
        final Class<T> enumClass;
        final Supplier<T> getter;
        final Consumer<T> setter;
        int btnX, btnY, btnW = 125, btnH = 22;

        EnumOption(String name, String desc, Class<T> cls, Supplier<T> get, Consumer<T> set) {
            super(name, desc);
            this.enumClass = cls; this.getter = get; this.setter = set;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, desc, x + 8, y + 22, TEXT_DIM);

            btnX = x + w - 135; btnY = y + 10;
            T val = getter.get();
            boolean btnHover = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;

            ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, BORDER_DARK);
            ctx.fill(btnX + 1, btnY + 1, btnX + btnW - 1, btnY + btnH - 1, btnHover ? PARCHMENT_HOVER : PARCHMENT);

            String txt = val.toString();
            if (txt.length() > 14) txt = txt.substring(0, 12) + "..";
            ctx.drawTextWithShadow(tr, txt, btnX + 8, btnY + 7, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, "\u25BC", btnX + btnW - 14, btnY + 7, TEXT_DIM);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                activeDropdown = this;
                dropdownX = btnX;
                dropdownY = btnY + btnH;
                dropdownWidth = btnW;
                dropdownScroll = 0; // Reset scroll when opening
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }

        void setValueByIndex(int idx) {
            T[] vals = enumClass.getEnumConstants();
            if (idx >= 0 && idx < vals.length) setter.accept(vals[idx]);
        }
    }

    private static class StringListOption extends ConfigOption {
        final Supplier<List<String>> getter;
        final Consumer<List<String>> setter;
        final String itemName;
        final boolean dualInput;

        StringListOption(String name, String desc, Supplier<List<String>> get, Consumer<List<String>> set, String itemName, boolean dualInput) {
            super(name, desc);
            this.getter = get; this.setter = set;
            this.itemName = itemName;
            this.dualInput = dualInput;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, getter.get().size() + " " + itemName, x + 8, y + 22, TEXT_DIM);

            int bx = x + w - 75, by = y + 12;
            boolean btnHover = mx >= bx && mx < bx + 65 && my >= by && my < by + 20;
            ctx.fill(bx, by, bx + 65, by + 20, BORDER_DARK);
            ctx.fill(bx + 1, by + 1, bx + 64, by + 19, btnHover ? PARCHMENT_HOVER : PARCHMENT);
            ctx.drawCenteredTextWithShadow(tr, "Edit...", bx + 32, by + 6, TEXT_LIGHT);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            int bx = x + w - 75, by = y + 12;
            if (mx >= bx && mx < bx + 65 && my >= by && my < by + 20) {
                MinecraftClient.getInstance().setScreen(new StringListEditorScreen(
                        MinecraftClient.getInstance().currentScreen, name, getter.get(), setter, dualInput));
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }
    }

    private static class ButtonOption extends ConfigOption {
        final Consumer<Void> action;
        final String buttonText;

        ButtonOption(String name, String desc, Consumer<Void> action, String buttonText) {
            super(name, desc);
            this.action = action;
            this.buttonText = buttonText;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
            ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
            ctx.drawTextWithShadow(tr, name, x + 8, y + 8, TEXT_LIGHT);
            ctx.drawTextWithShadow(tr, desc, x + 8, y + 22, TEXT_DIM);

            int bx = x + w - 75, by = y + 12;
            boolean btnHover = mx >= bx && mx < bx + 65 && my >= by && my < by + 20;
            ctx.fill(bx, by, bx + 65, by + 20, BORDER_DARK);
            ctx.fill(bx + 1, by + 1, bx + 64, by + 19, btnHover ? PARCHMENT_HOVER : PARCHMENT);
            ctx.drawCenteredTextWithShadow(tr, buttonText, bx + 32, by + 6, TEXT_LIGHT);
        }

        @Override
        boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
            int bx = x + w - 75, by = y + 12;
            if (mx >= bx && mx < bx + 65 && my >= by && my < by + 20) {
                action.accept(null);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            return false;
        }
    }

    // ==================== STRING LIST EDITOR ====================
    private static class StringListEditorScreen extends Screen {
        final Screen parent;
        final List<String> items;
        final Consumer<List<String>> setter;
        final boolean dualInput;
        String input1 = "";
        String input2 = "";
        int activeField = 0; // 0 = input1, 1 = input2
        int editingIndex = -1; // -1 = adding new, >= 0 = editing existing
        double scroll = 0;

        StringListEditorScreen(Screen parent, String title, List<String> items, Consumer<List<String>> setter, boolean dualInput) {
            super(Text.literal("Edit: " + title));
            this.parent = parent;
            this.items = new ArrayList<>(items);
            this.setter = setter;
            this.dualInput = dualInput;
        }

        private String getActiveInput() {
            return activeField == 0 ? input1 : input2;
        }

        private void setActiveInput(String val) {
            if (activeField == 0) input1 = val;
            else input2 = val;
        }

        private void clearInputs() {
            input1 = "";
            input2 = "";
            editingIndex = -1;
            activeField = 0;
        }

        private void loadItemForEditing(int index) {
            if (index < 0 || index >= items.size()) return;
            String item = items.get(index);
            editingIndex = index;
            if (dualInput && item.contains("|")) {
                String[] parts = item.split("\\|", 2);
                input1 = parts[0];
                input2 = parts.length > 1 ? parts[1] : "";
            } else {
                input1 = item;
                input2 = "";
            }
            activeField = 0;
        }

        private void saveCurrentInput() {
            String value = dualInput ? input1 + "|" + input2 : input1;
            if (value.isEmpty() || (dualInput && input1.isEmpty())) return;

            if (editingIndex >= 0 && editingIndex < items.size()) {
                items.set(editingIndex, value);
            } else {
                items.add(value);
            }
            clearInputs();
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            boolean isEditing = editingIndex >= 0;

            ctx.fill(0, 0, width, height, BG_DARK);

            int px = width / 2 - 180, pw = 360;
            ctx.fill(px, 20, px + pw, height - 20, BG_MEDIUM);
            ctx.fill(px + 2, 22, px + pw - 2, height - 22, BG_LIGHT);

            ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 35, GOLD);
            ctx.fill(px + 20, 48, px + pw - 20, 49, GOLD_DARK);

            // Input fields
            int inputY = 65;
            if (dualInput) {
                if (isEditing) {
                    // Two input fields for trigger|display
                    int fieldW = (pw - 140) / 2;

                    // Trigger field
                    ctx.drawTextWithShadow(textRenderer, "Trigger:", px + 15, inputY - 10, TEXT_DIM);
                    ctx.fill(px + 15, inputY, px + 15 + fieldW, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 16, inputY + 1, px + 14 + fieldW, inputY + 23, activeField == 0 ? PARCHMENT_LIGHT : PARCHMENT);
                    ctx.drawTextWithShadow(textRenderer, input1 + (activeField == 0 ? "_" : ""), px + 20, inputY + 8, TEXT_LIGHT);

                    // Display field
                    ctx.drawTextWithShadow(textRenderer, "Display:", px + 20 + fieldW, inputY - 10, TEXT_DIM);
                    ctx.fill(px + 20 + fieldW, inputY, px + 20 + fieldW * 2, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 21 + fieldW, inputY + 1, px + 19 + fieldW * 2, inputY + 23, activeField == 1 ? PARCHMENT_LIGHT : PARCHMENT);
                    ctx.drawTextWithShadow(textRenderer, input2 + (activeField == 1 ? "_" : ""), px + 25 + fieldW, inputY + 8, TEXT_LIGHT);
                } else {
                    // Two input fields for trigger|display
                    int fieldW = (pw - 90) / 2;

                    // Trigger field
                    ctx.drawTextWithShadow(textRenderer, "Trigger:", px + 15, inputY - 10, TEXT_DIM);
                    ctx.fill(px + 15, inputY, px + 15 + fieldW, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 16, inputY + 1, px + 14 + fieldW, inputY + 23, activeField == 0 ? PARCHMENT_LIGHT : PARCHMENT);
                    String t1 = input1.length() > 18 ? input1.substring(0, 16) + ".." : input1;
                    ctx.drawTextWithShadow(textRenderer, t1 + (activeField == 0 ? "_" : ""), px + 20, inputY + 8, TEXT_LIGHT);

                    // Display field
                    ctx.drawTextWithShadow(textRenderer, "Display:", px + 23 + fieldW, inputY - 10, TEXT_DIM);
                    ctx.fill(px + 23 + fieldW, inputY, px + 23 + fieldW * 2, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 24 + fieldW, inputY + 1, px + 22 + fieldW * 2, inputY + 23, activeField == 1 ? PARCHMENT_LIGHT : PARCHMENT);
                    String t2 = input2.length() > 18 ? input2.substring(0, 16) + ".." : input2;
                    ctx.drawTextWithShadow(textRenderer, t2 + (activeField == 1 ? "_" : ""), px + 28 + fieldW, inputY + 8, TEXT_LIGHT);
                }
            } else {
                // Single input field
                if (isEditing) {
                    ctx.fill(px + 15, inputY, px + pw - 120, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 16, inputY + 1, px + pw - 121, inputY + 23, PARCHMENT);
                    ctx.drawTextWithShadow(textRenderer, input1 + "_", px + 20, inputY + 8, TEXT_LIGHT);
                } else {
                    ctx.fill(px + 15, inputY, px + pw - 65, inputY + 24, BORDER_DARK);
                    ctx.fill(px + 16, inputY + 1, px + pw - 66, inputY + 23, PARCHMENT);
                    ctx.drawTextWithShadow(textRenderer, input1 + "_", px + 20, inputY + 8, TEXT_LIGHT);
                }
            }

            // Add/Save and Cancel buttons
            if (isEditing) {
                // Save button (left)
                boolean saveH = mx >= px + pw - 115 && mx < px + pw - 68 && my >= inputY && my < inputY + 24;
                ctx.fill(px + pw - 115, inputY, px + pw - 68, inputY + 24, BORDER_DARK);
                ctx.fill(px + pw - 114, inputY + 1, px + pw - 69, inputY + 23, saveH ? TOGGLE_ON : PARCHMENT);
                ctx.drawCenteredTextWithShadow(textRenderer, "Save", px + pw - 91, inputY + 8, TEXT_LIGHT);

                // Cancel button (right)
                boolean cancelEditH = mx >= px + pw - 63 && mx < px + pw - 16 && my >= inputY && my < inputY + 24;
                ctx.fill(px + pw - 63, inputY, px + pw - 16, inputY + 24, BORDER_DARK);
                ctx.fill(px + pw - 62, inputY + 1, px + pw - 17, inputY + 23, cancelEditH ? ACCENT_RED : PARCHMENT);
                ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", px + pw - 39, inputY + 8, TEXT_LIGHT);
            } else {
                // Add button
                boolean addH = mx >= px + pw - 60 && mx < px + pw - 15 && my >= inputY && my < inputY + 24;
                ctx.fill(px + pw - 60, inputY, px + pw - 15, inputY + 24, BORDER_DARK);
                ctx.fill(px + pw - 59, inputY + 1, px + pw - 16, inputY + 23, addH ? TOGGLE_ON : PARCHMENT);
                ctx.drawCenteredTextWithShadow(textRenderer, "+ Add", px + pw - 37, inputY + 8, TEXT_LIGHT);
            }

            int listTop = inputY + 30;
            ctx.enableScissor(px + 10, listTop, px + pw - 10, height - 70);
            int y = listTop - (int)scroll;
            for (int i = 0; i < items.size(); i++) {
                if (y + 24 > listTop && y < height - 70) {
                    boolean isSelected = i == editingIndex;
                    boolean itemHover = mx >= px + 15 && mx < px + pw - 50 && my >= y && my < y + 24;
                    ctx.fill(px + 15, y, px + pw - 50, y + 24, isSelected ? PARCHMENT_LIGHT : (itemHover ? PARCHMENT_HOVER : PARCHMENT));
                    String t = items.get(i);
                    if (t.length() > 35) t = t.substring(0, 33) + "..";
                    ctx.drawTextWithShadow(textRenderer, t, px + 20, y + 8, isSelected ? GOLD : TEXT_LIGHT);

                    boolean delH = mx >= px + pw - 45 && mx < px + pw - 15 && my >= y && my < y + 24;
                    ctx.fill(px + pw - 45, y, px + pw - 15, y + 24, BORDER_DARK);
                    ctx.fill(px + pw - 44, y + 1, px + pw - 16, y + 23, delH ? ACCENT_RED : PARCHMENT);
                    ctx.drawCenteredTextWithShadow(textRenderer, "X", px + pw - 30, y + 8, TEXT_LIGHT);
                }
                y += 28;
            }
            ctx.disableScissor();

            if (items.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, "No items", width / 2, height / 2, TEXT_DIM);

            int by = height - 55;
            boolean doneH = mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24;
            boolean cancelH = mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24;

            ctx.fill(width / 2 - 105, by, width / 2 - 5, by + 24, BORDER_DARK);
            ctx.fill(width / 2 - 104, by + 1, width / 2 - 6, by + 23, doneH ? TOGGLE_ON : PARCHMENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Done", width / 2 - 55, by + 8, TEXT_LIGHT);

            ctx.fill(width / 2 + 5, by, width / 2 + 105, by + 24, BORDER_DARK);
            ctx.fill(width / 2 + 6, by + 1, width / 2 + 104, by + 23, cancelH ? ACCENT_RED : PARCHMENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", width / 2 + 55, by + 8, TEXT_LIGHT);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            int px = width / 2 - 180, pw = 360;
            int inputY = 65;
            boolean isEditing = editingIndex >= 0;

            // Click on input fields (for dual input mode)
            if (dualInput) {
                if(isEditing) {
                    int fieldW = (pw - 140) / 2;
                    if (mx >= px + 15 && mx < px + 15 + fieldW && my >= inputY && my < inputY + 24) {
                        activeField = 0;
                        return true;
                    }
                    if (mx >= px + 20 + fieldW && mx < px + 20 + fieldW * 2 && my >= inputY && my < inputY + 24) {
                        activeField = 1;
                        return true;
                    }
                } else {
                    int fieldW = (pw - 90) / 2;
                    if (mx >= px + 15 && mx < px + 15 + fieldW && my >= inputY && my < inputY + 24) {
                        activeField = 0;
                        return true;
                    }
                    if (mx >= px + 23 + fieldW && mx < px + 23 + fieldW * 2 && my >= inputY && my < inputY + 24) {
                        activeField = 1;
                        return true;
                    }
                }
            }

            // Add/Save and Cancel buttons
            if (isEditing) {
                // Save button (left)
                if (mx >= px + pw - 115 && mx < px + pw - 68 && my >= inputY && my < inputY + 24) {
                    if (!input1.isEmpty()) {
                        saveCurrentInput();
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    }
                    return true;
                }
                // Cancel button (right)
                if (mx >= px + pw - 63 && mx < px + pw - 16 && my >= inputY && my < inputY + 24) {
                    clearInputs();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            } else {
                // Add button
                if (mx >= px + pw - 60 && mx < px + pw - 15 && my >= inputY && my < inputY + 24) {
                    if (!input1.isEmpty()) {
                        saveCurrentInput();
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    }
                    return true;
                }
            }

            // Done/Cancel buttons
            int by = height - 55;
            if (mx >= width / 2 - 105 && mx < width / 2 - 5 && my >= by && my < by + 24) {
                setter.accept(items);
                client.setScreen(parent);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            if (mx >= width / 2 + 5 && mx < width / 2 + 105 && my >= by && my < by + 24) {
                client.setScreen(parent);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }

            // List items
            int listTop = inputY + 30;
            int y = listTop - (int)scroll;
            for (int i = 0; i < items.size(); i++) {
                if (my >= y && my < y + 24) {
                    // Delete button
                    if (mx >= px + pw - 45 && mx < px + pw - 15) {
                        items.remove(i);
                        if (editingIndex == i) clearInputs();
                        else if (editingIndex > i) editingIndex--;
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                    // Click on item to edit
                    if (mx >= px + 15 && mx < px + pw - 50) {
                        loadItemForEditing(i);
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                }
                y += 28;
            }
            return super.mouseClicked(mx, my, btn);
        }

        @Override
        public boolean keyPressed(int key, int scan, int mod) {
            String current = getActiveInput();
            if (key == 259 && !current.isEmpty()) {
                setActiveInput(current.substring(0, current.length() - 1));
                return true;
            }
            if (key == 257) { // Enter
                if (!input1.isEmpty()) {
                    saveCurrentInput();
                }
                return true;
            }
            if (key == 258 && dualInput) { // Tab - switch fields
                activeField = activeField == 0 ? 1 : 0;
                return true;
            }
            if (key == 256) { // Escape
                if (editingIndex >= 0) {
                    clearInputs();
                } else {
                    client.setScreen(parent);
                }
                return true;
            }
            return super.keyPressed(key, scan, mod);
        }

        @Override
        public boolean charTyped(char c, int mod) {
            if (c >= 32) {
                setActiveInput(getActiveInput() + c);
                return true;
            }
            return super.charTyped(c, mod);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
            int max = Math.max(0, items.size() * 28 - (height - 165));
            scroll = MathHelper.clamp(scroll - v * 25, 0, max);
            return true;
        }

        @Override
        public void close() { client.setScreen(parent); }
    }
}