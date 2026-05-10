package julianh06.wynnextras.config;

import julianh06.wynnextras.config.configoptions.*;
import static julianh06.wynnextras.config.ConfigTheme.*;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.spellhider.SpellProfiles;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.misc.HudEditScreen;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.tetris.TetrisScreen;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
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
public class WynnExtrasConfigScreen extends Screen implements ConfigScreenContext {
    private static Identifier logoTexture = Identifier.of("wynnextras", "textures/general/wynnextrasbanner.png");

    private final Screen parent;
    private final WynnExtrasConfig config;

    // ==================== STATE ====================
    private static int lastSelectedCategory = 0;
    private static double lastScrollTarget = 0;
    private static final Map<String, Boolean> lastExpandedSubs = new HashMap<>();

    private int selectedCategory = 0;
    private int selectedCategoryColor = 0;
    private final List<Category> categories = new ArrayList<>();
    private double scrollOffset = 0;
    private double scrollTarget = 0;
    private double maxScroll = 0;
    private boolean scrollbarDragging = false;
    private double scrollbarDragOffset = 0;
    private int scrollbarY, scrollbarHeight, scrollbarThumbY, scrollbarThumbH;

    // Sidebar scroll state
    private double sidebarScrollOffset = 0;
    private double sidebarScrollTarget = 0;
    private boolean sidebarScrollbarDragging = false;
    private double sidebarScrollbarDragOffset = 0;
    private int sidebarScrollbarY, sidebarScrollbarHeight, sidebarScrollbarThumbY, sidebarScrollbarThumbH;

    private static final float SCROLL_SPEED = 0.3f;
    private static final float SCROLL_SNAP = 0.5f;

    // Dropdown state
    private DropdownOption<?> activeDropdown = null;
    private int dropdownX, dropdownY, dropdownWidth;
    private double dropdownScroll = 0;

    // Sticky subcategory header state
    private SubCategory stickySub = null;

    // Search state
    private String searchQuery = "";
    private boolean searchFocused = false;
    private static final int SEARCH_BAR_HEIGHT = 28;

    public WynnExtrasConfigScreen(Screen parent) {
        super(Text.literal("WynnExtras Configuration"));
        this.parent = parent;
        this.config = WynnExtrasConfig.INSTANCE;
        initCategories();
    }

    // ==================== CATEGORY DEFINITIONS ====================
    private void initCategories() {
        categories.clear();

        // ===== GENERAL =====
        category("General", 0xFF81c539)
            .add(image(logoTexture, 800, 250, 0.4f,
                    List.of(
                            line("Welcome to WynnExtras!").center().underline().bold().scale(1.5f).color(TEXT_LIGHT),
                            emptyLine(0.75f),
                            line("Our goal is to make your Wynncraft experience as smooth and as customizable as possible!").center(),
                            emptyLine(0.2f),
                            line("We have a lot of different features, which help you with all kinds of areas this wonderful game has to offer!").center(),
                            emptyLine(0.2f),
                            line("We also have a ton of custom commands you can try out with \"/we <...>\"!").center(),
                            emptyLine(0.2f),
                            line("If you have any kind of suggestions or bug reports we would appreciate if you'd let us know on our §9Discord!").center(),
                            emptyLine(0.5f)
                    )))
            .sub("Links")
                .add(button("Discord", "Join the WynnExtras Discord server", (x) -> {
                    LinkUtils.openLink("https://discord.gg/UbC6vZDaD5");
                }, "Open"))
                .add(button("Modrinth", "WynnExtras on Modrinth", (x) -> {
                    LinkUtils.openLink("https://modrinth.com/mod/wynnextras");
                }, "Open"))
                .add(button("GitHub", "WynnExtras source code on GitHub", (x) -> {
                    LinkUtils.openLink("https://github.com/JulianH06/WynnExtras");
                }, "Open"))
//                .add(button("YouTube", "Julian's personal YouTube channel", (x) -> {
//                    LinkUtils.openLink("https://www.youtube.com/@H06Julian");
//                }, "Open"))
            .endSub()
            .sub("Quick Access")
                .add(button("Loot Pools", "Open the Loot Pools screen", (x) -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.LootPools;
                }, "Open"))
                .add(button("Profile Viewer", "View your stats", (x) -> {
                    PV.open(McUtils.playerName());
                }, "Open"))
                .add(button("Waypoints", "Open the Waypoints screen", (x) -> {
                    MinecraftClient.getInstance().setScreen(null);
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("we waypoints");
                    }
                }, "Open"))
                .add(button("Raid List", "Open the Raid List", (x) -> {
                    MinecraftClient.getInstance().setScreen(null);
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("we raidlist");
                    }
                }, "Open"))
            .endSub()
//            .add(visibleWhen(button("Disable WynnExtras", "Turn off all features (your settings are preserved)",
//                (x) -> {
//                    config.disableWynnExtras();
//                }, "Disable"), config::isWynnExtrasEnabled))
//            .add(visibleWhen(button("Enable WynnExtras", "Re-enable all features with your previous settings",
//                (x) -> {
//                    config.enableWynnExtras();
//                }, "Enable"), () -> !config.isWynnExtrasEnabled()))
//            .add(button("Config Profiles", "Save and switch between named on/off setting presets",
//                (x) -> {
//                    MinecraftClient.getInstance().setScreen(new ProfilesScreen(MinecraftClient.getInstance().currentScreen));
//                }, "Manage"))
            .add(button("Reset to defaults", "Reset all settings back to their default values",
                    (x) -> {
                        config.resetToDefaults();
                    }, "Reset"))
            .add(button("Disable everything", "Click this to turn off everything so you can configure it yourself",
                (x) -> {
                    config.disableAll();
                }, "Disable"))
            .add(text("", "If you accidentally clicked on one of these buttons click on \"cancel\" to get your old settings back.")) //TODO; improve wording here
            .sub("Minigames")
                .add(text("Bored during raid downtime, attack queues or waiting for a friend?", "Then try out these minigames! Have fun!"))
                .add(button("Tetris", "A fully fledged Integration of the the game everyone knows and loves!",
                    (x) -> {
                        TetrisScreen.open();
                    }, "Play"))
                .add(text("More to come!", "More minigames are planned to be released in the future!"))
            .endSub();
        // ===== NEW =====
        category("New", 0xFFff5ea8)
            .add(text("", "All features added in this update. Toggle any of them on or off."))
                .add(toggle("Auto-ignore party in raid", "Auto /ignore party members on raid start, /ignore remove on raid end (reduces lag from teammate effects)",
                        () -> config.autoIgnorePartyInRaid, v -> config.autoIgnorePartyInRaid = v))
                .add(toggle("Encounter Selection overlay", "Replace the Encounter Selection chest with a big element-colored panel per option (click to select)",
                        () -> config.encounterOverlayEnabled, v -> config.encounterOverlayEnabled = v))
                .add(toggle("Right-click chat to copy", "Right-click a chat message (while chat is open) to copy it to the clipboard",
                        () -> config.rightClickToCopyChat, v -> config.rightClickToCopyChat = v))
                .add(toggle("Item broke notifier", "Show 'ITEM BROKE' when one of your items reaches zero durability",
                        () -> config.itemZeroDurability, v -> config.itemZeroDurability = v))
                .add(toggle("Raid Session Tracker", "HUD showing raid completion/failure counts and avg time",
                        () -> config.raidSessionEnabled, v -> config.raidSessionEnabled = v))
                .add(toggle("Quick Repair", "Press keybind at blacksmith to auto-repair all items",
                        () -> config.quickRepairEnabled, v -> config.quickRepairEnabled = v))
                .add(visibleWhen(keybind("Repair Key", "Key to start repair at blacksmith",
                        () -> config.quickRepairKey, v -> config.quickRepairKey = v),
                        () -> config.quickRepairEnabled))
                .add(toggle("Shift-toggle Guild Raid", "Block Guild Raid clicks by default, hold SHIFT to allow them through",
                        () -> config.shiftDisableGuildRaid, v -> config.shiftDisableGuildRaid = v))
                .sub("Tree Room Grotto Announcements")
                    .add(toggle("Isoptera in Gray Grotto", "Show 'GRAY' when the Interdimensional Isoptera is in the Gray Grotto",
                            () -> config.isopteraGray, v -> config.isopteraGray = v))
                    .add(toggle("Isoptera in Black Grotto", "Show 'BLACK' when the Interdimensional Isoptera is in the Black Grotto",
                            () -> config.isopteraBlack, v -> config.isopteraBlack = v))
                    .add(toggle("Isoptera in White Grotto", "Show 'WHITE' when the Interdimensional Isoptera is in the White Grotto",
                            () -> config.isopteraWhite, v -> config.isopteraWhite = v))
                    .add(toggle("Isoptera in Orange Grotto", "Show 'ORANGE' when the Interdimensional Isoptera is in the Orange Grotto",
                            () -> config.isopteraOrange, v -> config.isopteraOrange = v))
                    .add(toggle("Isoptera in Blue Grotto", "Show 'BLUE' when the Interdimensional Isoptera is in the Blue Grotto",
                            () -> config.isopteraBlue, v -> config.isopteraBlue = v))
                .add(toggle("Auto /stream", "Automatically send /stream when streamer mode disables (e.g. world swap)",
                        () -> config.autoStreamEnabled, v -> config.autoStreamEnabled = v))
                .add(toggle("Auto Skip Dialogue", "Automatically skip 'Press SHIFT to continue' NPC dialogue",
                        () -> config.autoSkipDialogueEnabled, v -> config.autoSkipDialogueEnabled = v))
                .add(toggle("Auto Skip Cutscenes", "Automatically swap-hand-skip cutscenes that show 'Swap Hands to skip'",
                        () -> config.autoSkipCutscenesEnabled, v -> config.autoSkipCutscenesEnabled = v))
                .add(toggle("Stack Duplicate Messages", "Collapse repeated messages into one with a (N) counter",
                        () -> config.stackDuplicateMessages, v -> config.stackDuplicateMessages = v))
                .add(visibleWhen(slider("Stack Window (minutes)", "Only stack messages sent within the last X minutes",
                        1, 60, () -> config.stackDuplicateWindowMinutes, v -> config.stackDuplicateWindowMinutes = v),
                        () -> config.stackDuplicateMessages))
                .add(toggle("Aura Ping", "Flash screen and show countdown when aura procs",
                        () -> config.auraPingEnabled, v -> config.auraPingEnabled = v))
                .add(toggle("Weekly War Count", "Show number of wars in last 7 days on HUD",
                        () -> config.weeklyWarCountEnabled, v -> config.weeklyWarCountEnabled = v))
                .add(toggle("War DPS Info", "Show tower EHP, DPS, team DPS, and ETA during wars",
                        () -> config.warDpsEnabled, v -> config.warDpsEnabled = v))
                .add(toggle("Attack Timer Menu", "Show upcoming attack times from scoreboard",
                        () -> config.attackTimerMenuEnabled, v -> config.attackTimerMenuEnabled = v))
                .add(visibleWhen(toggle("Auto-broadcast Defense", "After opening Attacking menu and war starts, auto-send '/g X defense is Y'",
                        () -> config.attackTimerAutoBroadcast, v -> config.attackTimerAutoBroadcast = v),
                        () -> config.attackTimerMenuEnabled))
                .add(toggle("War Beacon", "Green beacon beam at the soonest war territory",
                        () -> config.warBeaconEnabled, v -> config.warBeaconEnabled = v))
                .add(toggle("Territory/Eco Menu Keybind", "Press a key to open /gu manage > Territories directly",
                        () -> config.territoryMenuKeyEnabled, v -> config.territoryMenuKeyEnabled = v))
                .add(visibleWhen(keybind("Territory Key", "Key to open the territory/eco menu",
                        () -> config.territoryMenuKey, v -> config.territoryMenuKey = v),
                        () -> config.territoryMenuKeyEnabled))
                .add(toggle("Guild Bank Keybind", "Press a key to open /gu manage > Bank directly",
                        () -> config.guildBankKeyEnabled, v -> config.guildBankKeyEnabled = v))
                .add(visibleWhen(keybind("Guild Bank Key", "Key to open the guild bank",
                        () -> config.guildBankKey, v -> config.guildBankKey = v),
                        () -> config.guildBankKeyEnabled))
                .add(text("", "Full configuration for each feature lives in its own category (Raiding, Chat, Misc, etc.)."));

        // ===== RAIDS =====
        category("Raiding", GOLD_DARK)
            .sub("Loot Tracker")
                .add(toggle("Enable Tracker", "Track raid loot drops",
                        () -> config.toggleRaidLootTracker, v -> config.toggleRaidLootTracker = v))
                .add(visibleWhen(toggle("Render in HUD", "Render the Overlay in the HUD",
                                () -> config.raidLootTrackerRenderInHud, v -> config.raidLootTrackerRenderInHud = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(toggle("Render in Inventory", "Render the Overlay while in the inventory",
                                () -> config.raidLootTrackerRenderInInventory, v -> config.raidLootTrackerRenderInInventory = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(toggle("Render in Chat", "Render the Overlay while the chat is open",
                                () -> config.raidLootTrackerRenderInChat, v -> config.raidLootTrackerRenderInChat = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(toggle("Only Near Chest", "Show only near reward chest",
                                () -> config.raidLootTrackerOnlyNearChest, v -> config.raidLootTrackerOnlyNearChest = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(toggle("Compact Mode", "Use compact display",
                                () -> config.raidLootTrackerCompact, v -> config.raidLootTrackerCompact = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(toggle("Show Background", "Show dark background",
                                () -> config.raidLootTrackerBackground, v -> config.raidLootTrackerBackground = v),
                        () -> config.toggleRaidLootTracker))
                .add(visibleWhen(text("The Tracker is movable", "To change its position open your inventory and drag it where you want"),
                        () -> config.toggleRaidLootTracker))
            .sub("Session Tracker")
                .add(toggle("Enable Session Tracker", "Track raids per hour, completions, fails, and avg time",
                        () -> config.raidSessionEnabled, v -> config.raidSessionEnabled = v))
                .add(visibleWhen(toggle("Only show in raid", "Only display the HUD while inside a raid",
                                () -> config.raidSessionOnlyInRaid, v -> config.raidSessionOnlyInRaid = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Only show in inventory", "Only display the HUD while inventory is open",
                                () -> config.raidSessionOnlyInInventory, v -> config.raidSessionOnlyInInventory = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(sliderF("HUD Scale", "Scale of the session tracker HUD", 0.5f, 3.0f, 0.1f,
                                () -> config.raidSessionHudScale, v -> config.raidSessionHudScale = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Show Runs", "Display 'Runs: N' counter",
                                () -> config.raidSessionShowRuns, v -> config.raidSessionShowRuns = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Show Fails", "Display fail count (e.g. (3 F))",
                                () -> config.raidSessionShowFails, v -> config.raidSessionShowFails = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Show Runs/hr", "Display runs-per-hour rate",
                                () -> config.raidSessionShowRate, v -> config.raidSessionShowRate = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Show Elapsed Time", "Display session elapsed time",
                                () -> config.raidSessionShowTime, v -> config.raidSessionShowTime = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(toggle("Show Avg Run Time", "Display average raid completion time",
                                () -> config.raidSessionShowAvgTime, v -> config.raidSessionShowAvgTime = v),
                        () -> config.raidSessionEnabled))
                .add(visibleWhen(text("Movable in inventory", "Open inventory to drag the tracker or click [ADD]/[X]/[||] buttons"),
                        () -> config.raidSessionEnabled))
            .sub("Auto-ignore party in raid")
                .add(toggle("Auto-ignore party in raid", "On raid start, /ignore add all party members to reduce lag from their effects; /ignore remove them on raid end",
                        () -> config.autoIgnorePartyInRaid, v -> config.autoIgnorePartyInRaid = v))
            .sub("TNA Tree Room Map")
                .add(toggle("Enable Tree Map", "Enable a minimap that helps with TNA's tree room",
                        () -> config.tnaTreeMap, v -> config.tnaTreeMap = v))
                .add(visibleWhen(toggle("Show Tree Map only inside of tree", "Only show the Tree Map while you are the person inside of the tree",
                                () -> config.showTreeMapOnlyWhileInsideOfTree, v -> config.showTreeMapOnlyWhileInsideOfTree = v),
                        () -> config.tnaTreeMap))
                .add(visibleWhen(toggle("Show paths on Tree Map", "Show the optimal path to the soul while inside the tree",
                                () -> config.showPathsOnTreeMap, v -> config.showPathsOnTreeMap = v),
                        () -> config.tnaTreeMap))
                .add(visibleWhen(toggle("Show Map everywhere", "Enable this if you want to edit the position without going into TNA",
                                () -> config.showTreeMapEverywhere, v -> config.showTreeMapEverywhere = v),
                        () -> config.tnaTreeMap))
                .add(visibleWhen(text("The Map is movable", "To change its position open your inventory and drag it where you want"), () -> config.tnaTreeMap))
            .endSub()
                .add(toggle("Timestamps", "Show timestamps during raids",
                        () -> config.toggleRaidTimestamps, v -> config.toggleRaidTimestamps = v))
                .add(toggle("Fast Requeue", "Auto /pf on chest close",
                        () -> config.toggleFastRequeue, v -> config.toggleFastRequeue = v))
                .add(toggle("Block Guild Raid (Shift to allow)", "Blocks clicks on 'Guild Raid Available' in party finder unless SHIFT is held",
                        () -> config.shiftDisableGuildRaid, v -> config.shiftDisableGuildRaid = v))
                .add(toggle("Chiropterror Timer", "Spawn timer for the Chiropterror boss in TNA light room",
                        () -> config.chiropTimer, v -> config.chiropTimer = v))
                .add(toggle("Automatic aspect scanning", "Automatically scan aspects in raid reward chests by quickly clicking through the rewards",
                        () -> config.automaticAspectScanning, v -> config.automaticAspectScanning = v))
                .add(visibleWhen(toggle("Passive aspect scanning", "Scan your aspects passively without bothering you",
                        () -> config.passiveAspectScanning, v -> config.passiveAspectScanning = v),
                        () -> !config.automaticAspectScanning));

        // ===== COMBAT =====
        category("Combat", 0xFFfda216)
                .sub("Shaman Totem Timer")
                .add(toggle("Totem Timer", "Show totem countdown timer on HUD",
                        () -> config.totemTimerEnabled, v -> config.totemTimerEnabled = v))
                .add(visibleWhen(toggle("Own Totems Only", "Only show timers for your own totems",
                                () -> config.totemTimerOwnOnly, v -> config.totemTimerOwnOnly = v),
                        () -> config.totemTimerEnabled))
                .add(visibleWhen(toggle("Minimalistic Timer", "Show only the time, without the totem label",
                                () -> config.totemTimerTimeOnly, v -> config.totemTimerTimeOnly = v),
                        () -> config.totemTimerEnabled && config.totemTimerOwnOnly))
                .add(visibleWhen(toggle("Warning Text", "Show RECAST TOTEM! on screen when low (movable in Edit Gui)",
                                () -> config.totemTimerWarningText, v -> config.totemTimerWarningText = v),
                        () -> config.totemTimerEnabled))
                .add(visibleWhen(dropdown("Warning Text Color", "Color of the totem timer warning text",
                                WynnExtrasConfig.TextColor.class, () -> config.totemTimerWarningTextColor, v -> config.totemTimerWarningTextColor = v),
                        () -> config.totemTimerEnabled && config.totemTimerWarningText))
                .add(visibleWhen(toggle("Warning Sound", "Play pling sound when totem is low",
                                () -> config.totemTimerWarningSound, v -> config.totemTimerWarningSound = v),
                        () -> config.totemTimerEnabled))
                .add(visibleWhen(slider("Warning Volume", "The volume of the totem warning",
                                0, 200, () -> (int)(config.totemTimerWarningSoundVolume), v -> config.totemTimerWarningSoundVolume = v),
                        () -> config.totemTimerEnabled && config.totemTimerWarningSound))
                .add(visibleWhen(slider("Warning Threshold", "Seconds remaining to trigger warning",
                                1, 6, () -> config.totemTimerWarningThreshold, v -> config.totemTimerWarningThreshold = v),
                        () -> config.totemTimerEnabled && (config.totemTimerWarningSound || config.totemTimerWarningText)))
                .add(visibleWhen(toggle("Estimate Out-of-Range", "Continue countdown when totem leaves render distance",
                                () -> config.totemTimerEstimate, v -> config.totemTimerEstimate = v),
                        () -> config.totemTimerEnabled))
                .add(visibleWhen(toggle("Solid Color", "Use the color set in /we gui instead of the time-based green→red gradient",
                                () -> config.totemTimerSolidColor, v -> config.totemTimerSolidColor = v),
                        () -> config.totemTimerEnabled))
                .sub("Shaman Blood Sorrow Timer")
                .add(toggle("Blood Sorrow Timer", "Show Blood Sorrow cooldown on HUD",
                        () -> config.bloodSorrowTimerEnabled, v -> config.bloodSorrowTimerEnabled = v))
                .add(visibleWhen(toggle("Auto detect blood sorrow time", "Checks for acolyte aspect and resonance to calculate the time",
                                () -> config.autoDetectBloodSorrowTime, v -> config.autoDetectBloodSorrowTime = v),
                        () -> config.bloodSorrowTimerEnabled))
                .add(visibleWhen(toggle("Auto detect acolyte aspect", "Checks for the acolyte aspect tier to calculate the time",
                                () -> config.autoDetectAcolyteAspectTier, v -> config.autoDetectAcolyteAspectTier = v),
                        () -> !config.autoDetectBloodSorrowTime && config.bloodSorrowTimerEnabled))
                .add(visibleWhen(slider("Acolyte aspect tier", "Use this to manually set the tier of your acolyte aspect for the timer",
                                0, 3, () -> config.acolyteAspect, v -> config.acolyteAspect = v),
                        () -> !config.autoDetectAcolyteAspectTier && !config.autoDetectBloodSorrowTime && config.bloodSorrowTimerEnabled))
                .add(visibleWhen(toggle("Auto detect resonance", "Checks if you are holding a resonance to calculate the time",
                                () -> config.autoDetectResonanceInHand, v -> config.autoDetectResonanceInHand = v),
                        () -> !config.autoDetectBloodSorrowTime && config.bloodSorrowTimerEnabled))
                .add(visibleWhen(toggle("Resonance", "Manually set if you use a resonance or not",
                                () -> config.resoInHand, v -> config.resoInHand = v),
                        () -> !config.autoDetectResonanceInHand && !config.autoDetectBloodSorrowTime && config.bloodSorrowTimerEnabled))
                .sub("Provoke Timer")
                .add(toggle("Enable Provoke Timer", "Show provoke timer on HUD",
                        () -> config.provokeTimerToggle, v -> config.provokeTimerToggle = v))
                .add(visibleWhen(dropdown("Timer Color", "Timer text color",
                                WynnExtrasConfig.TextColor.class, () -> config.provokeTimerColor, v -> config.provokeTimerColor = v),
                        () -> config.provokeTimerToggle));

        // ===== INVENTORY =====
        Category invCategory = category("Inventory", 0xFFea1219);

        invCategory
            .sub("Bank Overlay")
                .add(toggle("Enable Bank Overlay", "Custom Bank Overlay",
                        () -> config.toggleBankOverlay, v -> config.toggleBankOverlay = v))
                .add(toggle("Smooth Scroll", "Smooth scrolling",
                        () -> config.smoothScrollToggle, v -> config.smoothScrollToggle = v))
                .add(toggle("Quick Toggle", "Show quick toggle button",
                        () -> config.bankQuickToggle, v -> config.bankQuickToggle = v))
                .add(toggle("Dark Mode", "Dark bank theme",
                        () -> config.darkmodeToggle, v -> config.darkmodeToggle = v))
                .add(slider("Max Rows", "The maximum amount of rows (lower can reduce lag)",
                        2, 24, () -> config.bankOverlayMaxRows, v -> config.bankOverlayMaxRows = v))
                .add(slider("Max Columns", "The maximum amount of columns (lower can reduce lag)",
                        2, 24, () -> config.bankOverlayMaxColumns, v -> config.bankOverlayMaxColumns = v))
                .add(toggle("Hide empty rows", "Hides rows that only have locked pages",
                        () -> config.bankOverlayHideEmptyRows, v -> config.bankOverlayHideEmptyRows = v))
                .add(toggle("Bag Overlay", "Show crafter bag counts by raid/tier on bank screens",
                        () -> config.bankBagOverlay, v -> config.bankBagOverlay = v)).endSub()
            .sub("Tooltips")
                .add(toggle("Item Weights", "Show Wynnpool weights for mythic items",
                        () -> config.showWeight, v -> {
                            config.showWeight = v;
                            if(!v) config.showScales = true;
                        }))
                .add(visibleWhen(toggle("Stat Scales", "Show weights for each stat",
                        () -> config.showScales, v -> config.showScales = v),
                        () -> config.showWeight)).endSub()
            .sub("Trade Market")
                .add(toggle("Scale background", "Use mythic scale as item background",
                        () -> config.scaleBackgroundEnabled, v -> config.scaleBackgroundEnabled = v))
                .add(toggle("Hide scale background button", "Hides the quick toggle for the scale background setting",
                        () -> config.hideScaleBackgroundButton, v -> config.hideScaleBackgroundButton = v))
                .add(toggle("Hide comparing info text", "Shows a text that informs you that you can compare items with F1",
                        () -> config.hideTMInfoText, v -> config.hideTMInfoText = v))
                .add(text("The Comparison panels are movable", "To change their position just drag it where you want"))
                .add(toggle("Trade market price summary", "Trade market overlay that shows you how much money you can claim",
                        () -> config.tradeMarketOverlay, v -> config.tradeMarketOverlay = v))
                .add(toggle("Price overlay background", "Show a dark background for the price overlay",
                        () -> config.tradeMarketOverlayBackground, v -> config.tradeMarketOverlayBackground = v))
                .add(text("The price summary is movable", "To change its position just drag it where you want")).endSub()
            .sub("Crafting")
                .add(toggle("Crafting helper", "Crafting Helper toggle",
                        () -> config.craftingHelperOverlay, v -> config.craftingHelperOverlay = v))
                .add(toggle("Dynamic textures in crafting helper", "Use dynamic material textures, supports Variants-CIT texture packs",
                        () -> config.craftingDynamicTextures, v -> config.craftingDynamicTextures = v))
                .add(toggle("Auto Start", "Automatically start crafting when a recipe is loaded",
                        () -> config.craftingAutoStart, v -> config.craftingAutoStart = v))
                .add(toggle("Crafting preview", "Crafting preview toggle",
                        () -> config.craftingPreviewOverlay, v -> config.craftingPreviewOverlay = v))
                .add(toggle("Crafting preview background", "Show a dark background for the crafting preview overlay",
                        () -> config.craftingPreviewBackground, v -> config.craftingPreviewBackground = v))
                .add(text("The preview is movable", "To change its position just drag it where you want"))
            .endSub()
                .add(toggle("Skill point helper (experimental)", "Show you your armor in the compass menu and a button to automatically assign skill points",
                        () -> config.skillpointHelper, v -> config.skillpointHelper = v));

        // ===== CHAT =====
        category("Chat", 0xFFc80069)
            .sub("Notifications")
                .add(stringListDual("Notifier Words", "Trigger word and display text",
                        () -> config.notifierWords, v -> config.notifierWords = v, "Words"))
                .add(sliderF("Duration (ms)", "How long notification shows",
                        500, 10000, 100, () -> (float) config.textDurationInMs, v -> config.textDurationInMs = v.intValue()))
                .add(sliderF("Fade in duration (ms)", "How long should the text fade in",
                        0, 5000, 50, () -> (float) config.notifierFadeInMs, v -> config.notifierFadeInMs = v.intValue()))
                .add(sliderF("Fade out duration (ms)", "How long should the text fade out",
                        0, 5000, 50, () -> (float) config.notifierFadeOutMs, v -> config.notifierFadeOutMs = v.intValue()))
                .add(dropdown("Text Color", "Notification color",
                        WynnExtrasConfig.TextColor.class, () -> config.textColor, v -> config.textColor = v))
                .add(dropdown("Sound", "Notification sound",
                        WynnExtrasConfig.NotificationSound.class, () -> config.notificationSound, v -> config.notificationSound = v))
                .add(slider("Volume", "Sound volume",
                        0, 200, () -> (int)(config.soundVolume), v -> config.soundVolume = v))
                .add(slider("Pitch", "Sound pitch",
                        0, 200, () -> (int)(config.soundPitch), v -> config.soundPitch = v))
                .add(button("Sound Test", "Click the button to test the sound",
                        v -> McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.notificationSound.getSoundId())), config.soundVolume / 100, config.soundPitch / 100), "Test")).endSub()
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
                        () -> config.notgLowerPlatform, v -> config.notgLowerPlatform = v))
                .add(toggle("Artifacts power restored", "When you can charge again in TWP room 3",
                        () -> config.artifactRestored, v -> config.artifactRestored = v))
                .add(toggle("Item broke (0 durability)", "Show 'ITEM BROKE' when one of your items reaches zero durability",
                        () -> config.itemZeroDurability, v -> config.itemZeroDurability = v)).endSub()
            .sub("Tree Room Grotto Announcements")
                .add(toggle("Isoptera in Gray Grotto", "Show 'GRAY' when the Interdimensional Isoptera is in the Gray Grotto",
                        () -> config.isopteraGray, v -> config.isopteraGray = v))
                .add(toggle("Isoptera in Black Grotto", "Show 'BLACK' when the Interdimensional Isoptera is in the Black Grotto",
                        () -> config.isopteraBlack, v -> config.isopteraBlack = v))
                .add(toggle("Isoptera in White Grotto", "Show 'WHITE' when the Interdimensional Isoptera is in the White Grotto",
                        () -> config.isopteraWhite, v -> config.isopteraWhite = v))
                .add(toggle("Isoptera in Orange Grotto", "Show 'ORANGE' when the Interdimensional Isoptera is in the Orange Grotto",
                        () -> config.isopteraOrange, v -> config.isopteraOrange = v))
                .add(toggle("Isoptera in Blue Grotto", "Show 'BLUE' when the Interdimensional Isoptera is in the Blue Grotto",
                        () -> config.isopteraBlue, v -> config.isopteraBlue = v))
            .endSub()
            .add(stringList("Blocked Words", "Hide messages with these",
                    () -> config.blockedWords, v -> config.blockedWords = v, "Words"))
            .add(toggle("Quick PV/GV Access (EXPERIMENTAL)", "Click on a players name or guild to open the pv/gv!",
                    () -> config.chatClickPV, v -> config.chatClickPV = v))
            .add(toggle("Bomb Share Suggestion", "Show a clickable suggestion to share bombs when someone asks about them in chat",
                    () -> config.bombShareSuggestion, v -> config.bombShareSuggestion = v));

        // ===== Hiders =====
        category("Hiders", 0xFF673190)
                .add(toggle("Enable Player Hider", "Enable the Player Hider",
                        () -> config.playerHiderToggle, v -> config.playerHiderToggle = v))
                .add(slider("Hide Distance", "Max distance to hide",
                        1, 20, () -> config.maxHideDistance, v -> config.maxHideDistance = v))
                .add(toggle("Hide All Players", "Hide all players in range",
                        () -> config.hideAllPlayers, v -> config.hideAllPlayers = v))
                .add(toggle("Hide All Players while in Wars", "Hide all players during wars",
                        () -> config.hideAllPlayersInWar, v -> config.hideAllPlayersInWar = v))
                .add(stringList("Hidden Players", "Always hide these players",
                        () -> config.hiddenPlayers, v -> config.hiddenPlayers = v, "Players"))
            .add(dropdown("Spell Hider Profile (EXPERIMENTAL)", "The default values for the spell hider, this can be changed at will without changing the overrides set with /Wynnextras SpellHider modify",
                    SpellProfiles.getProfileNames(), () -> config.spellProfile, v -> config.spellProfile = v));

        // ===== MISC =====
        category("Misc", 0xFF0872bc)
            .sub("Profession Overlay")
                .add(toggle("Enable Profession Overlay", "Show XP gain overlay when gathering/crafting",
                        () -> config.professionOverlayEnabled, v -> config.professionOverlayEnabled = v))
                .add(visibleWhen(toggle("Show Exact XP", "Show exact XP values instead of percentages",
                                () -> config.professionOverlayExactXp, v -> config.professionOverlayExactXp = v),
                        () -> config.professionOverlayEnabled))
            .sub("Radiant HUD")
                .add(toggle("Enable Radiant HUD", "Show radiant aspect tracking overlay",
                        () -> config.radiantHudEnabled, v -> config.radiantHudEnabled = v))
                .add(toggle("Custom Class Selection", "Replace vanilla class selection with a custom overlay",
                        () -> config.customClassSelectionEnabled, v -> config.customClassSelectionEnabled = v))
            .sub("Dark Mode Toggles")
                .add(toggle("Bank Overlay", "Dark mode for the Bank Overlay",
                        () -> config.darkmodeToggle, v -> config.darkmodeToggle = v))
                .add(toggle("Profile Viewer", "Dark mode for the Profile viewer",
                        () -> config.pvDarkmodeToggle, v -> config.pvDarkmodeToggle = v))
                .add(toggle("Crafting helper", "Dark mode for the Crafting helper",
                        () -> config.craftingHelperDarkMode, v -> config.craftingHelperDarkMode = v))
                .add(toggle("Main menu", "Dark mode for the WynnExtras main menu (/we)",
                        () -> config.mainMenuDarkMode, v -> config.mainMenuDarkMode = v))
                .add(button("Enable for all", "Enable the Dark mode for all options above",
                        v -> {
                            config.darkmodeToggle = true;
                            config.pvDarkmodeToggle = true;
                            config.craftingHelperDarkMode = true;
                            config.mainMenuDarkMode = true; }, "Enable"))
                .add(button("Disable for all", "Disable the Dark mode for all options above",
                        v -> {
                            config.darkmodeToggle = false;
                            config.pvDarkmodeToggle = false;
                            config.craftingHelperDarkMode = false;
                            config.mainMenuDarkMode = false; }, "Disable"))
            .sub("Tetris")
                .add(slider("DAS", "Delayed Auto Shift (ms) — delay before repeated movement begins",
                        0, 300, () -> config.tetrisDAS, v -> config.tetrisDAS = v))
                .add(slider("ARR", "Auto Repeat Rate (ms) — speed of repeated moves, 0 = instant",
                        0, 100, () -> config.tetrisARR, v -> config.tetrisARR = v))
                .add(slider("SDF Delay", "Soft Drop delay (ms) before fast-fall kicks in",
                        0, 300, () -> config.tetrisSDFDelay, v -> config.tetrisSDFDelay = v))
                .add(slider("SDF", "Soft Drop Factor (ms) — soft drop repeat speed, 0 = instant",
                        0, 100, () -> config.tetrisSDF, v -> config.tetrisSDF = v))
            .sub("Crowd sourcing")
                .add(toggle("Lootrun lootpools", "Help gather the current lootrun lootpool so others can see it with /we lootruns",
                        () -> config.crowdSourceLootrunLootpools, v -> config.crowdSourceLootrunLootpools = v))
                .add(toggle("Raid lootpools", "Help gather the current raid lootpool so others can see it with /we lootpool",
                        () -> config.crowdSourceRaidLootpools, v -> config.crowdSourceRaidLootpools = v))
                .add(toggle("Gambits", "Help gather the current gambits so others can see them with /we gambits",
                        () -> config.crowdSourceGambits, v -> config.crowdSourceGambits = v))
            .sub("Quick Repair")
                .add(toggle("Quick Repair", "Press keybind at blacksmith to auto-repair all items",
                        () -> config.quickRepairEnabled, v -> config.quickRepairEnabled = v))
                .add(visibleWhen(keybind("Repair Key", "Key to start repair at blacksmith",
                                () -> config.quickRepairKey, v -> config.quickRepairKey = v),
                        () -> config.quickRepairEnabled))
            .endSub()
            .add(toggle("Show Own Nametag", "Render your nametag above your head",
                    () -> config.showOwnNametag, v -> config.showOwnNametag = v))
            .add(toggle("Custom GUI Scale", "Use different scale inside of inventories",
                    () -> config.differentGUIScale, v -> config.differentGUIScale = v))
            .add(slider("GUI Scale", "Custom GUI scale value",
                    1, 5, () -> config.customGUIScale, v -> config.customGUIScale = v))
            .add(toggle("Lootpool button in pf menu", "Show a button to quickly access /we lootpool through the pf menu",
                    () -> config.showLootpoolButtonInPartyFinder, v -> config.showLootpoolButtonInPartyFinder = v))
            .add(toggle("Redirect Wynntils View Stats", "Changes the Wynntils 'View Player Stats' button to open the pv instead of the wynn website",
                    () -> config.redirectWynntilsViewStatsToPV, v -> config.redirectWynntilsViewStatsToPV = v))
            .add(toggle("Skip Front View", "Skip front-facing view in 3rd person",
                    () -> config.removeFrontPersonView, v -> config.removeFrontPersonView = v))
            .add(toggle("Financial Advice", "Receive smart financial advise in the Identifier menu",
                    () -> config.sourceOfTruthToggle, v -> config.sourceOfTruthToggle = v))
            .add(toggle("Territory Estimates", "Show territory estimates in the Wynntils guild map",
                    () -> config.territoryEstimateToggle, v -> config.territoryEstimateToggle = v))
            .add(toggle("WynnExtras Player Badges", "Display a badge above other players who also use WynnExtras!",
                    () -> config.badgesEnabled, v -> config.badgesEnabled = v))
            .add(toggle("Remove chroma", "Removes rainbow text and visuals from the aspect pages and profile viewer",
                    () -> config.removeChroma, v -> config.removeChroma = v));
    }

    // ==================== BUILDER HELPERS ====================
    private Category category(String name, int color) {
        Category cat = new Category(name, color, this);
        categories.add(cat);
        return cat;
    }

    @Override
    public int getContentWidth() { return width - SIDEBAR_WIDTH - 40; }

    @Override
    public void openDropdown(DropdownOption<?> opt, int x, int y, int w) {
        this.activeDropdown = opt;
        this.dropdownX = x;
        this.dropdownY = y;
        this.dropdownWidth = w;
        this.dropdownScroll = 0;
    }

    // Check if option matches search query
    @Override
    public boolean matchesSearch(ConfigOption opt) {
        if (!opt.isVisible()) return false;
        if (searchQuery.isEmpty()) return true;
        String query = searchQuery.toLowerCase();
        return opt.name.toLowerCase().contains(query) || opt.desc.toLowerCase().contains(query);
    }

    // Check if subcategory has any matching options
    @Override
    public boolean subHasMatches(SubCategory sub) {
        if (searchQuery.isEmpty()) return true;
        for (ConfigOption opt : sub.options) {
            if (matchesSearch(opt)) return true;
        }
        return false;
    }

    // Check if category has any matching options
    private boolean categoryHasMatches(Category cat) {
        if (searchQuery.isEmpty()) return true;
        if (cat.name.toLowerCase().contains(searchQuery.toLowerCase())) return true;
        for (Object item : cat.items) {
            if (item instanceof ConfigOption opt && matchesSearch(opt)) return true;
            if (item instanceof SubCategory sub && subHasMatches(sub)) return true;
        }
        return false;
    }

    private ConfigOption text(String name, String desc) {
        return new TextOption(name, desc);
    }

    private ConfigOption keybind(String name, String desc, Supplier<Integer> get, Consumer<Integer> set) {
        return new KeybindOption(name, desc, get, set);
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
        return new EnumOption<>(name, desc, cls, get, set, this);
    }

    private <T> ConfigOption dropdown(String name, String desc, List<T> vals, Supplier<T> get, Consumer<T> set) {
        return new ListOption<>(name, desc, vals, get, set, this);
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

    private static DescLine line(String text) { return DescLine.of(text); }
    
    private static DescLine emptyLine() { return DescLine.of(" "); }

    private static DescLine emptyLine(float scale) { return DescLine.of(" ").scale(scale); }

    private ConfigOption image(Identifier identifier, int imgW, int imgH) {
        return new ImageOption(identifier, imgW, imgH, 1.0f, List.of());
    }

    private ConfigOption image(Identifier identifier, int imgW, int imgH, float widthFraction) {
        return new ImageOption(identifier, imgW, imgH, widthFraction, List.of());
    }

    private ConfigOption image(Identifier identifier, int imgW, int imgH, List<DescLine> lines) {
        return new ImageOption(identifier, imgW, imgH, 1.0f, lines);
    }

    private ConfigOption image(Identifier identifier, int imgW, int imgH, float widthFraction, List<DescLine> lines) {
        return new ImageOption(identifier, imgW, imgH, widthFraction, lines);
    }

    private ConfigOption visibleWhen(ConfigOption option, BooleanSupplier condition) {
        option.visibleWhen(condition);
        return option;
    }

    // ==================== SCREEN LIFECYCLE ====================
    @Override
    protected void init() {
        selectedCategory = MathHelper.clamp(lastSelectedCategory, 0, categories.size() - 1);
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    Boolean saved = lastExpandedSubs.get(cat.name + "/" + sub.name);
                    if (saved != null) sub.setExpanded(saved);
                }
            }
        }
        updateMaxScroll();
        scrollTarget = MathHelper.clamp(lastScrollTarget, 0, maxScroll);
        scrollOffset = scrollTarget;
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
        updateMaxScroll();
        scrollTarget = MathHelper.clamp(scrollTarget, 0, maxScroll);
        double scrollDiff = scrollTarget - scrollOffset;
        if (Math.abs(scrollDiff) < SCROLL_SNAP) scrollOffset = scrollTarget;
        else scrollOffset += scrollDiff * SCROLL_SPEED * delta;

        double sidebarMax = getSidebarMaxScroll();
        sidebarScrollTarget = MathHelper.clamp(sidebarScrollTarget, 0, sidebarMax);
        double sidebarDiff = sidebarScrollTarget - sidebarScrollOffset;
        if (Math.abs(sidebarDiff) < SCROLL_SNAP) sidebarScrollOffset = sidebarScrollTarget;
        else sidebarScrollOffset += sidebarDiff * SCROLL_SPEED * delta;

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

    private double getSidebarMaxScroll() {
        int listStartY = 40 + SEARCH_BAR_HEIGHT + 8;
        int listH = height - 5 - listStartY;
        long count = categories.stream().filter(c -> searchQuery.isEmpty() || categoryHasMatches(c)).count();
        return Math.max(0, count * 28 - listH);
    }

    private void drawSidebar(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, SIDEBAR_WIDTH, height, BG_MEDIUM);
        ctx.fill(SIDEBAR_WIDTH - 2, 0, SIDEBAR_WIDTH, height, BORDER_DARK);

        ctx.drawCenteredTextWithShadow(textRenderer, "Categories", SIDEBAR_WIDTH / 2, 18, GOLD);
        ctx.fill(20, 32, SIDEBAR_WIDTH - 20, 33, GOLD_DARK);

        // Search bar
        int searchY = 40;
        boolean searchHovered = mouseX >= 8 && mouseX < SIDEBAR_WIDTH - 8
                && mouseY >= searchY && mouseY < searchY + SEARCH_BAR_HEIGHT;

        ctx.fill(8, searchY, SIDEBAR_WIDTH - 8, searchY + SEARCH_BAR_HEIGHT, BORDER_DARK);
        ctx.fill(9, searchY + 1, SIDEBAR_WIDTH - 9, searchY + SEARCH_BAR_HEIGHT - 1,
                searchFocused ? PARCHMENT_LIGHT : (searchHovered ? PARCHMENT_HOVER : PARCHMENT));

        String searchText = searchQuery.isEmpty() ? (searchFocused ? "" : "Search...") : searchQuery;
        int searchTextColor = searchQuery.isEmpty() && !searchFocused ? TEXT_DIM : TEXT_LIGHT;
        String displayText = searchText;
        if (displayText.length() > 12) displayText = displayText.substring(0, 10) + "..";
        ctx.drawTextWithShadow(textRenderer, displayText + (searchFocused ? "_" : ""),
                14, searchY + 10, searchTextColor);

        if (!searchQuery.isEmpty()) {
            int clearX = SIDEBAR_WIDTH - 28;
            boolean clearHovered = mouseX >= clearX && mouseX < clearX + 20
                    && mouseY >= searchY + 4 && mouseY < searchY + 24;
            ctx.fill(clearX, searchY + 4, clearX + 20, searchY + 24, clearHovered ? ACCENT_RED : BG_DARK);
            ctx.drawCenteredTextWithShadow(textRenderer, "X", clearX + 10, searchY + 10, TEXT_LIGHT);
        }

        int listStartY = searchY + SEARCH_BAR_HEIGHT + 8;
        int listEndY = height - 5;
        int listH = listEndY - listStartY;

        double sidebarMaxScroll = getSidebarMaxScroll();
        sidebarScrollOffset = MathHelper.clamp(sidebarScrollOffset, 0, sidebarMaxScroll);

        ctx.enableScissor(0, listStartY, SIDEBAR_WIDTH - 2, listEndY);

        int y = listStartY - (int) sidebarScrollOffset;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);

            if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) continue;

            boolean hovered = mouseX >= 8 && mouseX < SIDEBAR_WIDTH - 8 && mouseY >= y && mouseY < y + 22;
            boolean selected = i == selectedCategory;

            if (selected) {
                ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + 22, PARCHMENT);
                ctx.fill(8, y, 12, y + 22, cat.color);
            } else if (hovered) {
                ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + 22, BG_LIGHT);
            }

            drawDiamond(ctx, 20, y + 10, 4, cat.color);
            ctx.drawTextWithShadow(textRenderer, cat.name, 30, y + 7, selected ? TEXT_LIGHT : TEXT_DIM);

            y += 28;
        }

        ctx.disableScissor();

        if (sidebarMaxScroll > 0) {
            int sbX = SIDEBAR_WIDTH - 9;
            sidebarScrollbarY = listStartY;
            sidebarScrollbarHeight = listH;
            sidebarScrollbarThumbH = Math.max(16, (int)(listH * listH / (double)(listH + sidebarMaxScroll)));
            sidebarScrollbarThumbY = sidebarScrollbarY + (int)((listH - sidebarScrollbarThumbH) * (sidebarScrollOffset / sidebarMaxScroll));

            ctx.fill(sbX, sidebarScrollbarY, sbX + 5, sidebarScrollbarY + sidebarScrollbarHeight, BORDER_DARK);
            ctx.fill(sbX + 1, sidebarScrollbarThumbY, sbX + 4, sidebarScrollbarThumbY + sidebarScrollbarThumbH, GOLD_DARK);
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
        ctx.drawCenteredTextWithShadow(textRenderer, "Configuration - v" + CurrentVersionData.INSTANCE.version, panelX + panelW / 2, 32, TEXT_DIM);
        ctx.fill(panelX + 15, 48, panelX + panelW - 15, 50, cat.color);

        drawDiamond(ctx, panelX + 11, 4 + HEADER_HEIGHT / 2, 3, cat.color);
        drawDiamond(ctx, panelX + panelW - 11, 4 + HEADER_HEIGHT / 2, 3, cat.color);

        int contentX = panelX + 15;
        int contentW = panelW - 30;
        int listTop = HEADER_HEIGHT + 15;
        int listBottom = height - FOOTER_HEIGHT - 10;

        drawDiamond(ctx, contentX + 5, listTop + 2, 5, cat.color);
        ctx.drawTextWithShadow(textRenderer, cat.name, contentX + 16, listTop - 1, cat.color);
        ctx.fill(contentX, listTop + 12, contentX + contentW, listTop + 13, cat.color);

        ctx.enableScissor(panelX, listTop + 15, panelX + panelW - 15, listBottom);

        int y = listTop + 20 - (int)scrollOffset;

        SubCategory stickyCandidate = null;
        for (Object item : cat.items) {
            if (item instanceof SubCategory sub && subHasMatches(sub)) {
                int headerY = y;
                y = renderSubCategory(ctx, sub, contentX, y, contentW, mouseX, mouseY, listTop + 15, listBottom);
                if (sub.isExpanded() && headerY < listTop + 15 && y > listTop + 15) {
                    stickyCandidate = sub;
                }
            } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                int optH = opt.getHeight(contentW);
                if (y + optH > listTop && y < listBottom) {
                    boolean hovered = mouseX >= contentX && mouseX < contentX + contentW && mouseY >= y && mouseY < y + optH - 5;
                    opt.render(ctx, contentX, y, contentW, optH, mouseX, mouseY, hovered, cat.color);
                }
                y += optH + OPTION_SPACING;
            }
        }
        stickySub = stickyCandidate;

        ctx.disableScissor();

        if (stickySub != null) {
            int stickyY = listTop + 15;
            boolean hovered = mouseX >= contentX && mouseX < contentX + contentW
                           && mouseY >= stickyY && mouseY < stickyY + SUBCATEGORY_HEADER_HEIGHT;
            ctx.fill(contentX, stickyY, contentX + contentW, stickyY + SUBCATEGORY_HEADER_HEIGHT, hovered ? PARCHMENT_LIGHT : SUBCATEGORY_BG);
            ctx.fill(contentX, stickyY, contentX + contentW, stickyY + 1, BORDER_LIGHT);
            ctx.fill(contentX, stickyY + SUBCATEGORY_HEADER_HEIGHT - 1, contentX + contentW, stickyY + SUBCATEGORY_HEADER_HEIGHT, BORDER_DARK);
            String arrow = stickySub.isExpanded() ? "▼" : "▶";
            ctx.drawTextWithShadow(textRenderer, arrow, contentX + 8, stickyY + 8, selectedCategoryColor);
            ctx.drawTextWithShadow(textRenderer, stickySub.name, contentX + 22, stickyY + 8, TEXT_LIGHT);
        }

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

            String arrow = sub.isExpanded() ? "\u25BC" : "\u25B6";
            ctx.drawTextWithShadow(textRenderer, arrow, x + 8, y + 8, selectedCategoryColor);
            ctx.drawTextWithShadow(textRenderer, sub.name, x + 22, y + 8, TEXT_LIGHT);
        }
        y += SUBCATEGORY_HEADER_HEIGHT + 5;

        if (sub.isExpanded()) {
            for (ConfigOption opt : sub.options) {
                if (matchesSearch(opt)) {
                    int optH = opt.getHeight(w - 8);
                    if (y + optH > top && y < bot) {
                        boolean hovered = mX >= x + 8 && mX < x + w && mY >= y && mY < y + optH - 5;
                        ctx.fill(x, y, x + 4, y + optH - 5, selectedCategoryColor);
                        opt.render(ctx, x + 8, y, w - 8, optH, mX, mY, hovered, selectedCategoryColor);
                    }
                    y += optH + OPTION_SPACING;
                }
            }
        }
        return y;
    }

    private void renderDropdownOverlay(DrawContext ctx, int mouseX, int mouseY) {
        Object[] values = activeDropdown.getValues();
        int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
        int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
        boolean needsScroll = totalContentH > DROPDOWN_MAX_HEIGHT;

        int ddW = dropdownWidth + (needsScroll ? 10 : 0);
        int ddX = dropdownX;
        int ddY = dropdownY;

        if (ddY + visibleH > height - 10) {
            ddY = dropdownY - visibleH - 24;
        }

        double maxScroll = Math.max(0, totalContentH - visibleH);
        dropdownScroll = MathHelper.clamp(dropdownScroll, 0, maxScroll);

        ctx.fill(ddX - 3, ddY - 3, ddX + ddW + 3, ddY + visibleH + 3, BORDER_DARK);
        ctx.fill(ddX - 2, ddY - 2, ddX + ddW + 2, ddY + visibleH + 2, selectedCategoryColor);
        ctx.fill(ddX - 1, ddY - 1, ddX + ddW + 1, ddY + visibleH + 1, BG_MEDIUM);
        ctx.fill(ddX, ddY, ddX + ddW, ddY + visibleH, PARCHMENT);

        ctx.enableScissor(ddX, ddY, ddX + ddW - (needsScroll ? 8 : 0), ddY + visibleH);

        for (int i = 0; i < values.length; i++) {
            int iy = ddY + i * DROPDOWN_ITEM_HEIGHT - (int)dropdownScroll;

            if (iy + DROPDOWN_ITEM_HEIGHT < ddY || iy > ddY + visibleH) continue;

            boolean hovered = mouseX >= ddX && mouseX < ddX + ddW - (needsScroll ? 8 : 0)
                    && mouseY >= Math.max(ddY, iy) && mouseY < Math.min(ddY + visibleH, iy + DROPDOWN_ITEM_HEIGHT);
            boolean selected = values[i].equals(activeDropdown.getter.get());

            int itemBg = selected ? selectedCategoryColor : (hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(ddX, iy, ddX + ddW - (needsScroll ? 8 : 0), iy + DROPDOWN_ITEM_HEIGHT, itemBg);

            if (i > 0) {
                ctx.fill(ddX + 8, iy, ddX + ddW - (needsScroll ? 16 : 8), iy + 1, BG_LIGHT);
            }

            String text = values[i].toString();
            if (text.length() > 14) text = text.substring(0, 12) + "..";
            ctx.drawTextWithShadow(textRenderer, text, ddX + 8, iy + 7, selected ? GOLD : TEXT_LIGHT);
        }

        ctx.disableScissor();

        if (needsScroll) {
            int sbX = ddX + ddW - 6;
            int sbH = visibleH;
            int thumbH = Math.max(20, (int)(sbH * visibleH / (double)totalContentH));
            int thumbY = ddY + (int)((sbH - thumbH) * (dropdownScroll / maxScroll));

            ctx.fill(sbX, ddY, sbX + 5, ddY + sbH, BG_DARK);
            ctx.fill(sbX + 1, thumbY, sbX + 4, thumbY + thumbH, selectedCategoryColor);
        }
    }

    private void drawFooter(DrawContext ctx, int mouseX, int mouseY) {
        int footerY = height - FOOTER_HEIGHT + 5;

        if (selectedCategory < 0 || selectedCategory >= categories.size()) return;
        Category cat = categories.get(selectedCategory);

        ctx.fill(SIDEBAR_WIDTH + 10, footerY, width - 10, footerY + 1, cat.color);

        int btnY = height - 35;
        int saveX = width - 115;
        int cancelX = width - 225;
        int editX = width - 335;

        boolean saveHover = mouseX >= saveX && mouseX < saveX + 100 && mouseY >= btnY && mouseY < btnY + 24;
        boolean cancelHover = mouseX >= cancelX && mouseX < cancelX + 100 && mouseY >= btnY && mouseY < btnY + 24;
        boolean editHover = mouseX >= editX && mouseX < editX + 100 && mouseY >= btnY && mouseY < btnY + 24;

        drawButton(ctx, saveX, btnY, 100, 24, "Save & Close", saveHover, TOGGLE_ON);
        drawButton(ctx, cancelX, btnY, 100, 24, "Cancel", cancelHover, ACCENT_RED);
        drawButton(ctx, editX, btnY, 100, 24, "Edit HUD positions", editHover, PARCHMENT_LIGHT);
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
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x();
        double my = click.y();
        int btn = click.button();

        if (activeDropdown != null) {
            Object[] values = activeDropdown.getValues();
            int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
            int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
            boolean needsScroll = totalContentH > DROPDOWN_MAX_HEIGHT;
            int ddW = dropdownWidth + (needsScroll ? 10 : 0);
            int ddX = dropdownX;
            int ddY = dropdownY;

            if (ddY + visibleH > height - 10) {
                ddY = dropdownY - visibleH - 24;
            }

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
                return true;
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
            //======== Edit HUD Position =========
            if (mx >= width - 335 && mx < width - 235) {
                WynnExtrasConfig.save();
                WynnExtrasConfig.load();
                client.setScreen(new HudEditScreen(this));
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Search bar in sidebar
        int sidebarSearchY = 40;
        if (mx >= 8 && mx < SIDEBAR_WIDTH - 8 && my >= sidebarSearchY && my < sidebarSearchY + SEARCH_BAR_HEIGHT) {
            if (!searchQuery.isEmpty()) {
                int clearX = SIDEBAR_WIDTH - 28;
                if (mx >= clearX && mx < clearX + 20) {
                    searchQuery = "";
                    scrollOffset = 0; scrollTarget = 0;
                    updateMaxScroll();
                    autoSelectMatchingCategory();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }
            searchFocused = true;
            return true;
        }

        if (searchFocused && (my < sidebarSearchY || my >= sidebarSearchY + SEARCH_BAR_HEIGHT || mx < 8 || mx >= SIDEBAR_WIDTH - 8)) {
            searchFocused = false;
        }

        // Categories in sidebar
        if (mx >= 8 && mx < SIDEBAR_WIDTH - 8) {
            int y = sidebarSearchY + SEARCH_BAR_HEIGHT + 8 - (int) sidebarScrollOffset;
            for (int i = 0; i < categories.size(); i++) {
                Category cat = categories.get(i);
                if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) continue;

                if (my >= y && my < y + 24) {
                    selectedCategory = i;
                    scrollOffset = 0; scrollTarget = 0;
                    updateMaxScroll();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                y += 28;
            }
        }

        // Sidebar scrollbar
        double sidebarMaxScroll = getSidebarMaxScroll();
        if (sidebarMaxScroll > 0 && mx >= SIDEBAR_WIDTH - 9 && mx < SIDEBAR_WIDTH - 4) {
            if (my >= sidebarScrollbarThumbY && my < sidebarScrollbarThumbY + sidebarScrollbarThumbH) {
                sidebarScrollbarDragging = true;
                sidebarScrollbarDragOffset = my - sidebarScrollbarThumbY;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else if (my >= sidebarScrollbarY && my < sidebarScrollbarY + sidebarScrollbarHeight) {
                double clickPercent = (my - sidebarScrollbarY - sidebarScrollbarThumbH / 2.0) / (sidebarScrollbarHeight - sidebarScrollbarThumbH);
                sidebarScrollTarget = MathHelper.clamp(clickPercent * sidebarMaxScroll, 0, sidebarMaxScroll);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Scrollbar
        if (maxScroll > 0 && mx >= width - 17 && mx < width - 11) {
            if (my >= scrollbarThumbY && my < scrollbarThumbY + scrollbarThumbH) {
                scrollbarDragging = true;
                scrollbarDragOffset = my - scrollbarThumbY;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else if (my >= scrollbarY && my < scrollbarY + scrollbarHeight) {
                double clickPercent = (my - scrollbarY - scrollbarThumbH / 2.0) / (scrollbarHeight - scrollbarThumbH);
                scrollTarget = MathHelper.clamp(clickPercent * maxScroll, 0, maxScroll);
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (stickySub != null) {
            int stickyY = HEADER_HEIGHT + 30;
            if (my >= stickyY && my < stickyY + SUBCATEGORY_HEADER_HEIGHT
                    && mx >= SIDEBAR_WIDTH + 20 && mx < width - 30) {
                stickySub.toggleExpanded();
                updateMaxScroll();
                if (!stickySub.isExpanded()) {
                    // scroll so the now collapsed header sits at the top of the viewport
                    int contentW = width - SIDEBAR_WIDTH - 40;
                    int contentY = 0;
                    Category cat = categories.get(selectedCategory);
                    for (Object item : cat.items) {
                        if (item == stickySub) break;
                        if (item instanceof SubCategory sub && subHasMatches(sub)) {
                            contentY += SUBCATEGORY_HEADER_HEIGHT + 5;
                            if (sub.isExpanded()) {
                                for (ConfigOption opt : sub.options) {
                                    if (matchesSearch(opt)) contentY += opt.getHeight(contentW - 8) + OPTION_SPACING;
                                }
                            }
                        } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                            contentY += opt.getHeight(contentW) + OPTION_SPACING;
                        }
                    }
                    scrollTarget = MathHelper.clamp(contentY + 5, 0, maxScroll);
                    scrollOffset = scrollTarget;
                }
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentX = SIDEBAR_WIDTH + 20;
            int contentW = width - SIDEBAR_WIDTH - 40;
            int listTop = HEADER_HEIGHT + 30;
            int listBot = height - FOOTER_HEIGHT - 10;

            int y = listTop - (int)scrollOffset + 5;

            for (Object item : cat.items) {
                if (item instanceof SubCategory sub && subHasMatches(sub)) {
                    if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + SUBCATEGORY_HEADER_HEIGHT) && mx >= contentX && mx < contentX + contentW) {
                        sub.toggleExpanded();
                        updateMaxScroll();
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                    y += SUBCATEGORY_HEADER_HEIGHT + 5;

                    if (sub.isExpanded()) {
                        for (ConfigOption opt : sub.options) {
                            if (matchesSearch(opt)) {
                                int optH = opt.getHeight(contentW - 8);
                                if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + optH)) {
                                    if (opt.mouseClicked(mx, my, contentX + 8, y, contentW - 8, optH, btn)) return true;
                                }
                                y += optH + OPTION_SPACING;
                            }
                        }
                    }
                } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                    int optH = opt.getHeight(contentW);
                    if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + optH)) {
                        if (opt.mouseClicked(mx, my, contentX, y, contentW, optH, btn)) return true;
                    }
                    y += optH + OPTION_SPACING;
                }
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mx = click.x();
        double my = click.y();
        int btn = click.button();

        scrollbarDragging = false;
        sidebarScrollbarDragging = false;
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    for (ConfigOption opt : sub.options) opt.mouseReleased(mx, my, btn);
                } else if (item instanceof ConfigOption opt) {
                    opt.mouseReleased(mx, my, btn);
                }
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x();
        double my = click.y();
        int btn = click.button();

        if (sidebarScrollbarDragging) {
            double sidebarMaxScroll = getSidebarMaxScroll();
            if (sidebarMaxScroll > 0) {
                double newThumbY = my - sidebarScrollbarDragOffset;
                double percent = (newThumbY - sidebarScrollbarY) / (sidebarScrollbarHeight - sidebarScrollbarThumbH);
                sidebarScrollTarget = MathHelper.clamp(percent * sidebarMaxScroll, 0, sidebarMaxScroll);
            }
            return true;
        }

        if (scrollbarDragging && maxScroll > 0) {
            double newThumbY = my - scrollbarDragOffset;
            double percent = (newThumbY - scrollbarY) / (scrollbarHeight - scrollbarThumbH);
            scrollTarget = MathHelper.clamp(percent * maxScroll, 0, maxScroll);
            return true;
        }

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            int contentX = SIDEBAR_WIDTH + 20;
            int contentW = width - SIDEBAR_WIDTH - 40;
            int y = HEADER_HEIGHT + 35 - (int)scrollOffset;

            for (Object item : cat.items) {
                if (item instanceof SubCategory sub && subHasMatches(sub)) {
                    y += SUBCATEGORY_HEADER_HEIGHT + 5;
                    if (sub.isExpanded()) {
                        for (ConfigOption opt : sub.options) {
                            if (matchesSearch(opt)) {
                                int optH = opt.getHeight(contentW - 8);
                                if (opt.mouseDragged(mx, my, contentX + 8, y, contentW - 8, optH)) return true;
                                y += optH + OPTION_SPACING;
                            }
                        }
                    }
                } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                    int optH = opt.getHeight(contentW);
                    if (opt.mouseDragged(mx, my, contentX, y, contentW, optH)) return true;
                    y += optH + OPTION_SPACING;
                }
            }
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (activeDropdown != null) {
            Object[] values = activeDropdown.getValues();
            int totalContentH = values.length * DROPDOWN_ITEM_HEIGHT;
            int visibleH = Math.min(totalContentH, DROPDOWN_MAX_HEIGHT);
            double maxDropScroll = Math.max(0, totalContentH - visibleH);
            dropdownScroll = MathHelper.clamp(dropdownScroll - vAmt * 20, 0, maxDropScroll);
            return true;
        }
        if (mx > SIDEBAR_WIDTH) {
            scrollTarget = MathHelper.clamp(scrollTarget - vAmt * 30, 0, maxScroll);
        } else {
            sidebarScrollTarget = MathHelper.clamp(sidebarScrollTarget - vAmt * 20, 0, getSidebarMaxScroll());
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // Relay to any listening KeybindOption
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof ConfigOption opt) {
                    if (opt instanceof KeybindOption kb && kb.onKeyPressed(input.key())) return true;
                } else if (item instanceof SubCategory sub) {
                    for (ConfigOption opt : sub.options) {
                        if (opt instanceof KeybindOption kb && kb.onKeyPressed(input.key())) return true;
                    }
                }
            }
        }

        int key = input.key();
        if (activeDropdown != null && key == 256) {
            activeDropdown = null;
            return true;
        }

        if (searchFocused) {
            boolean ctrl = (input.modifiers() & org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL) != 0;
            if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
                String clipboard = net.minecraft.client.MinecraftClient.getInstance().keyboard.getClipboard();
                if (clipboard != null && !clipboard.isEmpty()) {
                    searchQuery += clipboard.replaceAll("[\\r\\n\\t]", "");
                    scrollOffset = 0; scrollTarget = 0;
                    updateMaxScroll();
                    autoSelectMatchingCategory();
                }
                return true;
            } else if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(searchQuery);
                return true;
            } else if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_X) {
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(searchQuery);
                searchQuery = "";
                scrollOffset = 0; scrollTarget = 0;
                updateMaxScroll();
                autoSelectMatchingCategory();
                return true;
            } else if (key == 259) { // Backspace
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    scrollOffset = 0; scrollTarget = 0;
                    updateMaxScroll();
                    autoSelectMatchingCategory();
                }
                return true;
            } else if (key == 256) { // Escape
                searchFocused = false;
                return true;
            } else if (key == 257) { // Enter
                searchFocused = false;
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        // Block character input when Ctrl is held (Ctrl+V etc.)
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrlHeld = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (ctrlHeld) return true;

        if (searchFocused) {
            char c = (char) charInput.codepoint();
            if (c >= 32 && c < 127) {
                searchQuery += c;
                scrollOffset = 0; scrollTarget = 0;
                updateMaxScroll();
                autoSelectMatchingCategory();
                return true;
            }
        }
        return super.charTyped(charInput);
    }

    private void autoSelectMatchingCategory() {
        if (!searchQuery.isEmpty() && selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category currentCat = categories.get(selectedCategory);
            if (!categoryHasMatches(currentCat)) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categoryHasMatches(categories.get(i))) {
                        selectedCategory = i;
                        updateMaxScroll();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        saveLastScreenState(selectedCategory, scrollTarget, categories);
        client.setScreen(parent);
    }

    private static void saveLastScreenState(int selectedCategory, double scrollTarget, List<Category> categories) {
        lastSelectedCategory = selectedCategory;
        lastScrollTarget = scrollTarget;
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    lastExpandedSubs.put(cat.name + "/" + sub.name, sub.isExpanded());
                }
            }
        }
    }
}
