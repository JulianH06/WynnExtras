package julianh06.wynnextras.config;

import julianh06.wynnextras.config.configoptions.*;
import static julianh06.wynnextras.config.ConfigTheme.*;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.core.CurrentVersionData;
import julianh06.wynnextras.features.achievements.AchievementScreen;
import julianh06.wynnextras.features.badges.BadgeService;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.features.spellhider.SpellProfiles;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.misc.HudEditScreen;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.tetris.TetrisScreen;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.WynncraftApiHandler;
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
    private static final WynnExtrasConfig DEFAULT_CONFIG = new WynnExtrasConfig();

    private final Screen parent;
    private final WynnExtrasConfig config;
    private final WynnExtrasConfig.TelemetryMode originalTelemetryMode;
    private final boolean originalDoNotPublishOwnBadge;
    private final boolean originalDoNotPublishOwnAspects;

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
    private int dropdownX, dropdownY, dropdownWidth, dropdownOptionWidth;
    private double dropdownScroll = 0;

    // Sticky subcategory header state
    private SubCategory stickySub = null;

    // Search state
    private String searchQuery = "";
    private boolean searchFocused = false;
    private Map<String, Boolean> expandedSubsBeforeSearch = null;
    private static final int SEARCH_BAR_HEIGHT = 28;

    public WynnExtrasConfigScreen(Screen parent) {
        super(Text.literal("WynnExtras Configuration"));
        this.parent = parent;
        this.config = WynnExtrasConfig.INSTANCE;
        this.originalTelemetryMode = config.telemetryMode;
        this.originalDoNotPublishOwnBadge = config.doNotPublishOwnBadge;
        this.originalDoNotPublishOwnAspects = config.doNotPublishOwnAspects;
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
            .sub("Quick Access")
                .add(button("Loot Pools", "Open the Loot Pools screen", (x) -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.AspectLootpool;
                }, "Open"))
                .add(button("Profile Viewer", "View your stats", (x) -> {
                    PV.open(MinecraftUtils.playerName());
                }, "Open"))
                .add(button("Waypoints", "Open the Waypoints screen", (x) -> {
                    MinecraftClient.getInstance().setScreen(null);
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("we waypoints");
                    }
                }, "Open"))
                .add(button("Achievements", "Open achievements and badge customization", (x) -> {
                    WEScreen.open(AchievementScreen::new);
                }, "Open"))
                .add(button("Raid List", "Open the Raid List", (x) -> {
                    MinecraftClient.getInstance().setScreen(null);
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("we raidlist");
                    }
                }, "Open"))
            .endSub()
            .sub("Links")
                .add(button("Discord", "Join the WynnExtras Discord server", (x) -> {
                    LinkUtils.openLink("https://wynnextras.com/discord");
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
            .add(toggle("Disable Update Reminder", "Do not show a chat message when a new WynnExtras version is available",
                () -> config.updateReminderDisabled, v -> config.updateReminderDisabled = v))
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
            .add(text("", "If you accidentally clicked on one of these buttons click on \"cancel\" to get your old settings back."))
            .sub("Minigames")
                .add(text("Bored during raid downtime, attack queues or waiting for a friend?", "Then try out these minigames! Have fun!"))
                .add(button("Tetris", "A fully fledged Integration of the the game everyone knows and loves!",
                    (x) -> {
                        TetrisScreen.open();
                    }, "Play"))
                .add(text("More to come!", "More minigames are planned to be released in the future!"))
            .endSub()
            .sub("Privacy")
                .add(button("Privacy policy", "You can find more information here", (x) -> {
                    LinkUtils.openLink("https://wynnextras.com/privacy");
                }, "Open"))
                .add(dropdown("Telemetry", "Choose whether usage statistics are sent with your Minecraft UUID, anonymously, or not at all",
                        WynnExtrasConfig.TelemetryMode.class, () -> config.telemetryMode, v -> config.telemetryMode = v))
                .add(toggle("Do Not Publish Own Badge", "Hide your WynnExtras badge from other players and stop uploading it",
                        () -> config.doNotPublishOwnBadge, v -> config.doNotPublishOwnBadge = v))
                .add(toggle("Do Not Publish Own Aspects", "Hide your personal aspects and stop uploading them",
                        () -> config.doNotPublishOwnAspects, v -> config.doNotPublishOwnAspects = v))
                .add(toggle("Do Not Publish Own Achievements", "Stop uploading your achievement progress to the WynnExtras server",
                        () -> !config.uploadAchievements, v -> config.uploadAchievements = !v))
                .add(toggle("Do Not Crowdsource Daily Gambits", "Stop sharing discovered gambits with the WynnExtras server",
                        () -> !config.crowdSourceGambits, v -> config.crowdSourceGambits = !v))
                .add(toggle("Do Not Fetch Badges", "Stop fetching other players' badges from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasBadges, v -> config.doNotFetchWynnExtrasBadges = v))
                .add(toggle("Do Not Fetch Achievements", "Stop fetching other players' achievements from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasAchievements, v -> config.doNotFetchWynnExtrasAchievements = v))
                .add(toggle("Do Not Fetch Aspects", "Stop fetching player aspects and the aspect leaderboard from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasAspects, v -> config.doNotFetchWynnExtrasAspects = v))
                .add(toggle("Do Not Fetch Gambits", "Stop fetching crowdsourced gambits from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasGambits, v -> config.doNotFetchWynnExtrasGambits = v))
                .add(toggle("Do Not Fetch Profile Titles", "Stop fetching custom profile titles from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasProfileTitles, v -> config.doNotFetchWynnExtrasProfileTitles = v))
                .add(toggle("Do Not Fetch Reset Times", "Stop fetching loot pool, lootrun, and gambit reset times from the WynnExtras server",
                        () -> config.doNotFetchWynnExtrasResetTimes, v -> config.doNotFetchWynnExtrasResetTimes = v))
                .add(text("Fetch settings", "Changes apply to future requests. Some data may require a game restart to be fetched again."))
            .endSub();

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
            .sub("TNA Tree Room")
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
                .add(toggle("Draw Path", "Draw a path in the 3d world to the next door",
                                () -> config.drawPathInTree, v -> config.drawPathInTree = v))
                .add(text("Path notice", "The 3d path feature is experimental, the path might be different then the path shown on the minimap in some cases. This will be improved in the future."))
            .sub("Aspect Scoring")
                .add(toggle("Show Score", "Shows the individual score for each aspect",
                        () -> config.showIndividualAspectScore, v -> config.showIndividualAspectScore = v))
                .add(sliderF("Mythic Multiplier", "Multiplier applied to mythic aspects for scoring", 0.f, 200.f, 0.1f,
                        () -> config.mythicAspectMultiplier, v -> config.mythicAspectMultiplier = v))
                .add(sliderF("Fabled Multiplier", "Multiplier applied to fabled aspects for scoring", 0.f, 20.f, 0.1f,
                        () -> config.fabledAspectMultiplier, v -> config.fabledAspectMultiplier = v))
                .add(sliderF("Legendary Multiplier", "Multiplier applied to legendary aspects for scoring", 0.f, 2.f, 0.1f,
                        () -> config.legendaryAspectMultiplier, v -> config.legendaryAspectMultiplier = v))
                .add(sliderF("Favorite Multiplier", "Multiplier applied to favorite aspects for scoring (applies on top of rarity multiplier)", 0.f, 10.f, 0.1f,
                        () -> config.favoriteMultiplier, v -> config.favoriteMultiplier = v))
            .endSub()
            .add(toggle("Timestamps", "Show timestamps during raids",
                    () -> config.toggleRaidTimestamps, v -> config.toggleRaidTimestamps = v))
            .add(toggle("Fast Requeue", "Auto /pf on chest close",
                    () -> config.toggleFastRequeue, v -> config.toggleFastRequeue = v))
            .add(toggle("Block GRaid toggle (Shift to bypass)", "Blocks clicks on 'Guild Raid Available' in party finder unless SHIFT is held to prevent accidentally toggling graids",
                    () -> config.shiftDisableGuildRaid, v -> config.shiftDisableGuildRaid = v))
            .add(toggle("Chiropterror Timer", "Spawn timer for the Chiropterror boss in TNA light room. This is only an estimation since the spawn time is not 100% consistent",
                    () -> config.chiropTimer, v -> config.chiropTimer = v))
            .add(toggle("Automatic aspect scanning", "Automatically scan aspects in raid reward chests by quickly clicking through the rewards. This updates your aspect data in the raid lootpool screens and helps with certain features that change based on aspect progress (e.g. blood sorrow timer)",
                    () -> config.automaticAspectScanning, v -> config.automaticAspectScanning = v))
            .add(visibleWhen(toggle("Passive aspect scanning", "Scan your aspects passively without bothering you",
                    () -> config.passiveAspectScanning, v -> config.passiveAspectScanning = v),
                    () -> !config.automaticAspectScanning))
            .add(toggle("Encounter Selection overlay (Very Experimental)", "Replace the Encounter Selection chest with a big element-colored panel per option (click to select)",
                    () -> config.encounterOverlayEnabled, v -> config.encounterOverlayEnabled = v))
            .add(toggle("Auto-ignore party in raid", "On raid start, /ignore add all party members to reduce lag from their effects; /ignore remove them on raid end",
                    () -> config.autoIgnorePartyInRaid, v -> config.autoIgnorePartyInRaid = v));

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
                .add(visibleWhen(toggle("Toxoplasmosis", "Show the toxoplasmosis value in the totem timer",
                                () -> config.totemTimerShowToxoplasmosis, v -> config.totemTimerShowToxoplasmosis = v),
                        () -> config.totemTimerEnabled))
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
            .sub("Curse Tracker")
                .add(toggle("Curse Tracker", "Show Curse remaining time on HUD",
                        () -> config.curseTrackerEnabled, v -> config.curseTrackerEnabled = v))
                .add(visibleWhen(toggle("Color mobs based on curse", "Highlight cursed mobs with a colored bounding box",
                                () -> config.curseTrackerColorMobs, v -> config.curseTrackerColorMobs = v),
                        () -> config.curseTrackerEnabled))
                .add(visibleWhen(dropdown("Mob highlight color", "Color of the cursed mob highlight",
                                WynnExtrasConfig.TextColor.class, () -> config.curseTrackerMobColor, v -> config.curseTrackerMobColor = v),
                        () -> config.curseTrackerEnabled && config.curseTrackerColorMobs))
                .add(visibleWhen(slider("Timeout duration", "Time for how long the tracker shows \"Curse: expired\" after curse ended (in seconds)", 0, 60,
                        () -> config.curseTimeout, v -> config.curseTimeout = v),
                        () -> config.curseTrackerEnabled))
                .sub("Provoke Timer")
                .add(toggle("Enable Provoke Timer", "Show provoke timer on HUD",
                        () -> config.provokeTimerToggle, v -> config.provokeTimerToggle = v))
                .add(visibleWhen(slider("Duration", "Duration of the provoke timer in seconds", 0, 10,
                                () -> config.provokeTimerDuration, v -> config.provokeTimerDuration = v),
                        () -> config.provokeTimerToggle))
                .sub("Radiant HUD")
                .add(toggle("Enable Radiant HUD", "Show radiant aspect tracking overlay",
                        () -> config.radiantHudEnabled, v -> config.radiantHudEnabled = v))
            .sub("Aura")
                .add(toggle("Aura Ping", "Flash screen and show countdown when aura procs",
                        () -> config.auraPingEnabled, v -> config.auraPingEnabled = v))
            .sub("Attack Timer")
                .add(toggle("Attack Timer", "Show upcoming attack times from scoreboard",
                        () -> config.attackTimerMenuEnabled, v -> config.attackTimerMenuEnabled = v))
                .add(visibleWhen(toggle("Auto-broadcast Defense", "After opening Attacking menu and war starts, auto-send '/g X defense is Y'",
                                () -> config.attackTimerAutoBroadcast, v -> config.attackTimerAutoBroadcast = v),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Normal Color", "Default attack timer text color",
                                () -> config.attackTimerNormalColor, v -> config.attackTimerNormalColor = v,
                                DEFAULT_CONFIG.attackTimerNormalColor, DEFAULT_CONFIG.attackTimerNormalColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Current Territory Color", "Attack timer text color while standing in that territory",
                                () -> config.attackTimerCurrentTerritoryColor, v -> config.attackTimerCurrentTerritoryColor = v,
                                DEFAULT_CONFIG.attackTimerCurrentTerritoryColor, DEFAULT_CONFIG.attackTimerCurrentTerritoryColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Very Low Defense Color", "Defense color for Very Low territories",
                                () -> config.attackTimerVeryLowDefenseColor, v -> config.attackTimerVeryLowDefenseColor = v,
                                DEFAULT_CONFIG.attackTimerVeryLowDefenseColor, DEFAULT_CONFIG.attackTimerVeryLowDefenseColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Low Defense Color", "Defense color for Low territories",
                                () -> config.attackTimerLowDefenseColor, v -> config.attackTimerLowDefenseColor = v,
                                DEFAULT_CONFIG.attackTimerLowDefenseColor, DEFAULT_CONFIG.attackTimerLowDefenseColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Medium Defense Color", "Defense color for Medium territories",
                                () -> config.attackTimerMediumDefenseColor, v -> config.attackTimerMediumDefenseColor = v,
                                DEFAULT_CONFIG.attackTimerMediumDefenseColor, DEFAULT_CONFIG.attackTimerMediumDefenseColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("High Defense Color", "Defense color for High territories",
                                () -> config.attackTimerHighDefenseColor, v -> config.attackTimerHighDefenseColor = v,
                                DEFAULT_CONFIG.attackTimerHighDefenseColor, DEFAULT_CONFIG.attackTimerHighDefenseColor),
                        () -> config.attackTimerMenuEnabled))
                .add(visibleWhen(color("Very High Defense Color", "Defense color for Very High territories",
                                () -> config.attackTimerVeryHighDefenseColor, v -> config.attackTimerVeryHighDefenseColor = v,
                                DEFAULT_CONFIG.attackTimerVeryHighDefenseColor, DEFAULT_CONFIG.attackTimerVeryHighDefenseColor),
                        () -> config.attackTimerMenuEnabled))
                .sub("Wars / Territory")
                .add(toggle("Weekly War Count", "Show number of wars in last 7 days on HUD",
                        () -> config.weeklyWarCountEnabled, v -> config.weeklyWarCountEnabled = v))
                .add(toggle("War DPS Info", "Show tower EHP, DPS, team DPS, and ETA during wars",
                        () -> config.warDpsEnabled, v -> config.warDpsEnabled = v))
                .add(toggle("War Beacon (EXPERIMENTAL)", "Green beacon beam at the soonest war territory (Experimental, might not render correctly)",
                        () -> config.warBeaconEnabled, v -> config.warBeaconEnabled = v));

        // ===== OVERLAYS =====
        Category invCategory = category("Overlays", 0xFFea1219);

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
                        1, 24, () -> config.bankOverlayMaxRows, v -> config.bankOverlayMaxRows = v))
                .add(slider("Max Columns", "The maximum amount of columns (lower can reduce lag)",
                        1, 24, () -> config.bankOverlayMaxColumns, v -> config.bankOverlayMaxColumns = v))
                .add(visibleWhen(toggle("Wynntils Page Jump Buttons", "Show the normal Wynntils bank page jump buttons on the left",
                        () -> config.showWynntilsBankPageJumpButtons, v -> config.showWynntilsBankPageJumpButtons = v),
                        () -> config.bankOverlayMaxRows == 1 && config.bankOverlayMaxColumns == 1))
                .add(toggle("Hide empty rows", "Hides rows that only have locked pages",
                        () -> config.bankOverlayHideEmptyRows, v -> config.bankOverlayHideEmptyRows = v))
                .add(toggle("Disable sticky nameplates", "Makes bank page nameplates scroll without sticking to the top",
                        () -> config.disableStickyNameplates, v -> config.disableStickyNameplates = v))
                .add(toggle("Bag Overlay", "Show crafter bag counts by raid/tier on bank screens",
                        () -> config.bankBagOverlay, v -> config.bankBagOverlay = v))
                .add(visibleWhen(toggle("Show total bag count in bank overlay", "Shows you a breakdown of all crafter bags you have across all pages of your bank",
                        () -> config.showTotalBagsInBankOverlay, v -> config.showTotalBagsInBankOverlay = v), () -> config.bankBagOverlay))
                .add(visibleWhen(slider("Max Wynntils annotation calculations per frame", "Limits the amount of Wynntils item annotation calculations being done each frame to reduce lag", 10, 200,
                        () -> config.maxAnnotationCalculationsPerFrame, v -> config.maxAnnotationCalculationsPerFrame = v), () -> config.toggleBankOverlay))
                .add(toggle("Exclude active page from searches", "Hide the currently open page when it has no matching items",
                        () -> config.bankOverlayExcludeActivePageFromSearches, v -> config.bankOverlayExcludeActivePageFromSearches = v))
                .add(toggle("Allow all characters mode on ironman classes", "Shows cross-class bank data while playing an ironman class",
                        () -> config.allowAllCharactersModeOnIronmanClasses, v -> config.allowAllCharactersModeOnIronmanClasses = v))
                .endSub()
            .sub("Class Selection")
                .add(toggle("Custom Class Selection", "Replace vanilla class selection with a custom overlay",
                        () -> config.customClassSelectionEnabled, v -> config.customClassSelectionEnabled = v))
                .add(toggle("Class Selection Background", "Show the dark fullscreen background behind the class selection overlay",
                        () -> config.classSelectionBackgroundEnabled, v -> config.classSelectionBackgroundEnabled = v))
                .add(dropdown("Content Progress Style", "How content progress is shown on class cards",
                        WynnExtrasConfig.ClassSelectionContentProgressStyle.class,
                        () -> config.classSelectionContentProgressStyle,
                        v -> {
                            config.classSelectionContentProgressStyle = v;
                            config.syncClassSelectionLines();
                        }))
                .add(classSelectionLines("Class Card Lines", "Choose which current stat lines are shown and in which order"))
                .add(dropdown("Completion Chroma", "Where rainbow text is used for classes with 100% content completion",
                        WynnExtrasConfig.ClassSelectionCompletionChromaMode.class,
                        () -> config.classSelectionCompletionChromaMode,
                        v -> config.classSelectionCompletionChromaMode = v))
                .add(toggle("Use custom class colors", "Configure the accent color for each class and reskin",
                        () -> config.useCustomClassColors, v -> config.useCustomClassColors = v))
                .add(visibleWhen(classColor("Warrior Color", "Accent color for Warrior class cards", "warrior", 0xCC4444),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Knight Color", "Accent color for Knight class cards", "knight", 0xCC4444),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Mage Color", "Accent color for Mage class cards", "mage", 0x55BBFF),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Dark Wizard Color", "Accent color for Dark Wizard class cards", "dark_wizard", 0x55BBFF),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Assassin Color", "Accent color for Assassin class cards", "assassin", 0xFF55FF),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Ninja Color", "Accent color for Ninja class cards", "ninja", 0xFF55FF),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Archer Color", "Accent color for Archer class cards", "archer", 0x55FF55),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Hunter Color", "Accent color for Hunter class cards", "hunter", 0x55FF55),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Shaman Color", "Accent color for Shaman class cards", "shaman", 0xFFFF55),
                        () -> config.useCustomClassColors))
                .add(visibleWhen(classColor("Skyseer Color", "Accent color for Skyseer class cards", "skyseer", 0xFFFF55),
                        () -> config.useCustomClassColors))
                .add(toggle("Hide quick toggle button", "Hide the enable/disable class overlay button on class selection screens",
                        () -> config.hideClassSelectionQuickToggleButton, v -> config.hideClassSelectionQuickToggleButton = v))
            .sub("Crafting")
                .add(toggle("Crafting helper", "Crafting Helper toggle",
                        () -> config.craftingHelperOverlay, v -> config.craftingHelperOverlay = v))
                .add(toggle("Reverse crafting helper order", "Show recipes from lowest to highest level",
                        () -> config.craftingHelperReverseOrder, v -> config.craftingHelperReverseOrder = v))
                .add(toggle("Auto Start", "Automatically start crafting when a recipe is loaded",
                        () -> config.craftingAutoStart, v -> config.craftingAutoStart = v))
                .add(keybind("Load from Clipboard Key", "Load a WynnBuilder craft from the clipboard while the crafting helper is open",
                        () -> config.craftingLoadClipboardKey, v -> config.craftingLoadClipboardKey = v,
                        DEFAULT_CONFIG.craftingLoadClipboardKey))
                .add(keybind("Reuse Last Key", "Reuse the previous craft while the crafting helper is open",
                        () -> config.craftingReuseLastKey, v -> config.craftingReuseLastKey = v,
                        DEFAULT_CONFIG.craftingReuseLastKey))
                .add(toggle("Crafting preview", "Crafting preview toggle",
                        () -> config.craftingPreviewOverlay, v -> config.craftingPreviewOverlay = v))
                .add(toggle("Crafting preview background", "Show a dark background for the crafting preview overlay",
                        () -> config.craftingPreviewBackground, v -> config.craftingPreviewBackground = v))
                .add(text("The preview is movable", "To change its position just drag it where you want"))
            .sub("Shopping List")
                .add(toggle("Shopping List", "Show the shopping list on the HUD and in screens like the Trade Market or bank",
                        () -> config.shoppingListMenuEnabled, v -> config.shoppingListMenuEnabled = v))
                .add(toggle("Show quick toggle button", "Show the quick toggle button in menus",
                        () -> config.shoppingListShowQuickToggleButton, v -> config.shoppingListShowQuickToggleButton = v))
                .add(keybind("Toggle Shopping List", "Toggle the shopping list",
                        () -> config.shoppingListToggleKey, v -> config.shoppingListToggleKey = v,
                        DEFAULT_CONFIG.shoppingListToggleKey))
                .add(toggle("WynnMarketSearch compatibility", "Temporarily suppresses WynnMarketSearch while the shopping list performs Trade Market row searches",
                        () -> config.shoppingListWynnMarketSearchCompatibility, v -> config.shoppingListWynnMarketSearchCompatibility = v))
            .sub("Profession Overlay")
                .add(toggle("Enable Profession Overlay", "Show XP gain overlay when gathering/crafting",
                        () -> config.professionOverlayEnabled, v -> config.professionOverlayEnabled = v))
                .add(visibleWhen(toggle("Show Exact XP", "Show exact XP values instead of percentages",
                                () -> config.professionOverlayExactXp, v -> config.professionOverlayExactXp = v),
                        () -> config.professionOverlayEnabled))
            .sub("Tooltips")
                .add(toggle("Item Weights", "Show weight scale for mythic items",
                        () -> config.showWeight, v -> {
                            config.showWeight = v;
                            if(!v) config.showScales = true;
                        }))
                .add(visibleWhen(toggle("Stat Scales", "Show weights for each stat",
                        () -> config.showScales, v -> config.showScales = v),
                        () -> config.showWeight))
                .add(visibleWhen(dropdown("Scale source", "Select the preferred mythic scale provider",
                                WynnExtrasConfig.MythicScaleSource.class,
                                () -> config.mythicScaleSource,
                                WeightDisplay::setConfiguredScaleSource),
                        () -> config.showWeight))
                .add(visibleWhen(toggle("Lock scale source", "Disable switching the scale provider and hide the lines in the tooltip.",
                                () -> config.lockMythicScaleSource, v -> {
                                    config.lockMythicScaleSource = v;
                                    WeightDisplay.clearCycleInput();
                                }),
                        () -> config.showWeight)).endSub()
            .sub("Trade Market")
                .add(toggle("Scale background", "Use mythic scale as item background",
                        () -> config.scaleBackgroundEnabled, v -> config.scaleBackgroundEnabled = v))
                .add(visibleWhen(dropdown("Scale background shape", "Shape used for the scale background",
                                ScaleBackgroundShape.class,
                                () -> config.scaleBackgroundShape,
                                v -> config.scaleBackgroundShape = v),
                        () -> config.scaleBackgroundEnabled))
                .add(visibleWhen(slider("Scale background opacity", "Opacity of the scale background in percent",
                                0, 100,
                                () -> config.scaleBackgroundOpacity,
                                v -> config.scaleBackgroundOpacity = v),
                        () -> config.scaleBackgroundEnabled))
                .add(toggle("Hide scale background button", "Hides the quick toggle for the scale background setting",
                        () -> config.hideScaleBackgroundButton, v -> config.hideScaleBackgroundButton = v))
                .add(toggle("Hide comparing info text", "Shows a text that informs you that you can compare items with F1",
                        () -> config.hideTMInfoText, v -> config.hideTMInfoText = v))
                .add(text("The Comparison panels are movable", "To change their position just drag it where you want"))
                .add(toggle("Trade market price summary", "Trade market overlay that shows you how much money you can claim",
                        () -> config.tradeMarketOverlay, v -> config.tradeMarketOverlay = v))
                .add(toggle("Price overlay background", "Show a dark background for the price overlay",
                        () -> config.tradeMarketOverlayBackground, v -> config.tradeMarketOverlayBackground = v))
                .add(text("The price summary is movable", "To change its position just drag it where you want"))
            .endSub()
            .add(toggle("Skill point helper", "Show you your armor in the compass menu and a button to automatically assign skill points",
                    () -> config.skillpointHelper, v -> config.skillpointHelper = v))
            .add(toggle("Powder combine helper", "Show quick combine buttons in the powder master menu",
                    () -> config.powderCombineHelper, v -> config.powderCombineHelper = v))
            .add(toggle("Show Mount Helper", "Renders the needed materials to max out a mounts stats in the feeder",
                    () -> config.showMountHelper, v -> config.showMountHelper = v));

        // ===== CHAT =====
        category("Chat", 0xFFc80069)
            .sub("Notifications")
                .add(stringListDual("Notifier Words", "Trigger word and display text",
                        () -> config.notifierWords, v -> config.notifierWords = v, "Words"))
                .add(sliderF("Duration (ms)", "How long notification shows",
                        500, 10000, 100, () -> (float) config.textDurationInMs, v -> config.textDurationInMs = v.intValue()))
                .add(dropdown("Entrance effect", "How the notification enters",
                        WynnExtrasConfig.NotifierAnimation.class, () -> config.notifierAnimation, v -> config.notifierAnimation = v))
                .add(visibleWhen(dropdown("Entrance direction", "Direction of the entrance animation",
                                WynnExtrasConfig.NotifierAnimationDirection.class,
                                () -> config.notifierEntranceDirection, v -> config.notifierEntranceDirection = v),
                        () -> config.notifierAnimation != null && config.notifierAnimation.isDirectional()))
                .add(sliderF("Entrance duration (ms)", "How long the entrance animation takes",
                        0, 5000, 50, () -> (float) config.notifierFadeInMs, v -> config.notifierFadeInMs = v.intValue()))
                .add(dropdown("Exit effect", "How the notification exits",
                        WynnExtrasConfig.NotifierExitAnimation.class, () -> config.notifierExitAnimation, v -> config.notifierExitAnimation = v))
                .add(visibleWhen(dropdown("Exit direction", "Direction of the exit animation",
                                WynnExtrasConfig.NotifierAnimationDirection.class,
                                () -> config.notifierExitDirection, v -> config.notifierExitDirection = v),
                        () -> config.notifierExitAnimation != null && config.notifierExitAnimation.isDirectional()))
                .add(sliderF("Exit duration (ms)", "How long the exit animation takes",
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
                        v -> MinecraftUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.notificationSound.getSoundId())), config.soundVolume / 100, config.soundPitch / 100), "Test")).endSub()
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
                        () -> config.itemZeroDurability, v -> config.itemZeroDurability = v))
                .add(toggle("Colossal Core spawned", "Show 'CORE SPAWNED' when a Colossal Core spawns in TCC",
                        () -> config.colossalCoreSpawned, v -> config.colossalCoreSpawned = v)).endSub()
            .sub("Media Preview (Experimental)")
                .add(text("Warning", "Media previews can download images from any public HTTPS site. Download size, image dimensions, redirects and file contents are validated, but vulnerabilities can never be completely ruled out. Use at your own risk."))
                .add(toggle("Chat Media Preview", "Preview PNG, JPEG and GIF links, including supported WebP and AVIF services",
                        () -> config.chatMediaPreviewEnabled, v -> config.chatMediaPreviewEnabled = v))
                .add(visibleWhen(dropdown("Media Preview Loading", "When media previews are downloaded",
                                WynnExtrasConfig.ChatMediaPreviewLoadPolicy.class,
                                () -> config.chatMediaPreviewLoadPolicy,
                                v -> config.chatMediaPreviewLoadPolicy = v),
                        () -> config.chatMediaPreviewEnabled))
                .add(visibleWhen(dropdown("Hover-preview Position", "Where media previews appear while hovering links",
                                WynnExtrasConfig.ChatMediaPreviewHoverPosition.class,
                                () -> config.chatMediaPreviewHoverPosition,
                                v -> config.chatMediaPreviewHoverPosition = v),
                        () -> config.chatMediaPreviewEnabled))
                .add(visibleWhen(toggle("Auto-show Media Preview", "Automatically download supported media when its link appears in chat. Use at your own risk.",
                                () -> config.chatMediaPreviewAutoDisplay, v -> config.chatMediaPreviewAutoDisplay = v),
                        () -> config.chatMediaPreviewEnabled))
                .add(visibleWhen(dropdown("Auto-preview Position", "Where automatic media previews appear",
                                WynnExtrasConfig.ChatMediaPreviewPosition.class,
                                () -> config.chatMediaPreviewPosition,
                                v -> config.chatMediaPreviewPosition = v),
                        () -> config.chatMediaPreviewEnabled && config.chatMediaPreviewAutoDisplay))
                .add(visibleWhen(slider("Preview Max Screen %", "Maximum percentage of screen width and height used by previews",
                                10, 50, () -> config.chatMediaPreviewMaxScreenPercent, v -> config.chatMediaPreviewMaxScreenPercent = v),
                        () -> config.chatMediaPreviewEnabled))
                .add(visibleWhen(slider("Preview Max MB", "Maximum media download size",
                                1, 25, () -> config.chatMediaPreviewMaxDownloadMb, v -> config.chatMediaPreviewMaxDownloadMb = v),
                        () -> config.chatMediaPreviewEnabled))
                .add(visibleWhen(slider("Preview Max GIF Frames", "Maximum decoded GIF frames",
                                1, 240, () -> config.chatMediaPreviewMaxGifFrames, v -> config.chatMediaPreviewMaxGifFrames = v),
                        () -> config.chatMediaPreviewEnabled))
            .endSub()
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
            .add(toggle("Bomb Share Suggestion", "Show a clickable suggestion to share bombs when someone asks about them",
                    () -> config.bombShareSuggestion, v -> config.bombShareSuggestion = v))
            .add(toggle("Bomb Rethrow Suggestion", "Show a clickable suggestion to rethrow a bomb when it expires",
                    () -> config.bombRethrowSuggestion, v -> config.bombRethrowSuggestion = v))
            .add(toggle("Right-click chat to copy", "Right-click a chat message (while chat is open) to copy it to the clipboard",
                    () -> config.rightClickToCopyChat, v -> config.rightClickToCopyChat = v))
            .add(toggle("Stack Duplicate Messages (EXPERIMENTAL)", "Collapse repeated messages into one with a (N) counter (Experimental, might break your chat)",
                    () -> config.stackDuplicateMessages, v -> config.stackDuplicateMessages = v))
            .add(visibleWhen(slider("Stack Window (minutes)", "Only stack messages sent within the last X minutes",
                    1, 60, () -> config.stackDuplicateWindowMinutes, v -> config.stackDuplicateWindowMinutes = v),
                    () -> config.stackDuplicateMessages));

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
                .add(toggle("Arrow Hider", "Hides arrows",
                        () -> config.arrowHiderToggle, v -> config.arrowHiderToggle = v))
            .add(dropdown("Spell Hider Profile (EXPERIMENTAL)", "The default values for the spell hider, this can be changed at will without changing the overrides set with /Wynnextras SpellHider modify",
                    SpellProfiles.getProfileNames(), () -> config.spellProfile, v -> config.spellProfile = v));

        // ===== MISC =====
        category("Misc", 0xFF3664AD)
            .sub("Auto Actions")
                .add(toggle("Auto /stream", "Automatically send /stream when swapping worlds, changing classes, etc.",
                        () -> config.autoStreamEnabled, v -> config.autoStreamEnabled = v))
                .add(toggle("Auto Skip Dialogue", "Automatically skip 'Press SHIFT to continue' NPC dialogue",
                        () -> config.autoSkipDialogueEnabled, v -> config.autoSkipDialogueEnabled = v))
                .add(toggle("Auto Skip Cutscenes", "Automatically skip cutscenes that show 'Swap Hands to skip'",
                        () -> config.autoSkipCutscenesEnabled, v -> config.autoSkipCutscenesEnabled = v))
            .sub("Achievements and badges")
                .add(toggle("Achievement unlock messages", "Show chat messages when you unlock WynnExtras achievements",
                        () -> config.showAchievementUnlockMessages, v -> config.showAchievementUnlockMessages = v))
                .add(toggle("WynnExtras Player Badges", "Display a badge for other players who also use WynnExtras!",
                        () -> config.showWynnExtrasBadges, v -> config.showWynnExtrasBadges = v))
                .add(button("Achievements & Badges", "Open achievements and select your badge icon and color", (x) -> {
                    WEScreen.open(AchievementScreen::new);
                }, "Open"))
            .sub("Quick Repair")
                .add(toggle("Quick Repair", "Press keybind at blacksmith to auto-repair all items",
                        () -> config.quickRepairEnabled, v -> config.quickRepairEnabled = v))
                .add(visibleWhen(slider("Durability Threshold", "Only repair items at or below this durability percentage",
                                0, 100, () -> config.quickRepairDurabilityThreshold,
                                v -> config.quickRepairDurabilityThreshold = v),
                        () -> config.quickRepairEnabled))
                .add(visibleWhen(keybind("Repair Key", "Key to start repair at blacksmith",
                                () -> config.quickRepairKey, v -> config.quickRepairKey = v,
                                DEFAULT_CONFIG.quickRepairKey),
                        () -> config.quickRepairEnabled))
            .endSub()
            .sub("Dark Mode Toggles")
                .add(toggle("Bank Overlay", "Dark mode for the Bank Overlay",
                        () -> config.darkmodeToggle, v -> config.darkmodeToggle = v))
                .add(toggle("Profile Viewer", "Dark mode for the Profile viewer",
                        () -> config.pvDarkmodeToggle, v -> config.pvDarkmodeToggle = v))
            .sub("Waypoints")
                .add(slider("Max range", "Waypoints and their text are hidden beyond this distance",
                        1, 1000, () -> config.waypointMaxRange, v -> config.waypointMaxRange = v))
            .sub("Tetris")
                .add(slider("DAS", "Delayed Auto Shift (ms) — delay before repeated movement begins",
                        0, 300, () -> config.tetrisDAS, v -> config.tetrisDAS = v))
                .add(slider("ARR", "Auto Repeat Rate (ms) — speed of repeated moves, 0 = instant",
                        0, 100, () -> config.tetrisARR, v -> config.tetrisARR = v))
                .add(slider("SDF Delay", "Soft Drop delay (ms) before fast-fall kicks in",
                        0, 300, () -> config.tetrisSDFDelay, v -> config.tetrisSDFDelay = v))
                .add(slider("SDF", "Soft Drop Factor (ms) — soft drop repeat speed, 0 = instant",
                        0, 100, () -> config.tetrisSDF, v -> config.tetrisSDF = v))
                .add(toggle("Remove background blur", "Hide the background blur in the Tetris menu",
                        () -> config.hideTetrisBackgroundBlur, v -> config.hideTetrisBackgroundBlur = v))
                .add(toggle("20G after level", "Instant gravity after the selected level.",
                        () -> config.tetris20GEnabled, v -> config.tetris20GEnabled = v))
                .add(visibleWhen(slider("20G Level", "Level where instant gravity starts",
                                1, 100, () -> config.tetris20GLevel, v -> config.tetris20GLevel = v),
                        () -> config.tetris20GEnabled))
            .add(toggle("Mount color backgrounds", "Use the mount's primary color as its item background",
                    () -> config.mountPrimaryColorBackground, v -> config.mountPrimaryColorBackground = v))
            .add(toggle("Show Own Nametag", "Render your nametag above your head",
                    () -> config.showOwnNametag, v -> config.showOwnNametag = v))
            .add(toggle("Custom GUI Scale", "Use different scale inside of inventories",
                    () -> config.differentGUIScale, v -> config.differentGUIScale = v))
            .add(visibleWhen(slider("GUI Scale", "Custom GUI scale value",
                    1, 5, () -> config.customGUIScale, v -> config.customGUIScale = v),
                    () -> config.differentGUIScale))
            .add(toggle("Lootpool button in pf menu", "Show a button to quickly access /we lootpool through the pf menu",
                    () -> config.showLootpoolButtonInPartyFinder, v -> config.showLootpoolButtonInPartyFinder = v))
            .add(toggle("Redirect Wynntils View Stats", "Changes the Wynntils 'View Player Stats' button to open the pv instead of the wynn website",
                    () -> config.redirectWynntilsViewStatsToPV, v -> config.redirectWynntilsViewStatsToPV = v))
            .add(toggle("Skip Front View", "Skip front-facing view in 3rd person",
                    () -> config.removeFrontPersonView, v -> config.removeFrontPersonView = v))
            .add(toggle("Fun item identifier", "Make rolling items more fun",
                    () -> config.identifierCaseOpening, v -> config.identifierCaseOpening = v))
            .add(toggle("Financial Advice", "Receive smart financial advise in the Identifier menu",
                    () -> config.sourceOfTruthToggle, v -> config.sourceOfTruthToggle = v))
            .add(toggle("Territory Estimates", "Show territory estimates in the Wynntils guild map",
                    () -> config.territoryEstimateToggle, v -> config.territoryEstimateToggle = v))
            .add(toggle("Remove chroma", "Removes rainbow text and visuals from the aspect pages and profile viewer",
                    () -> config.removeChroma, v -> config.removeChroma = v));

        // ===== KEYBINDS =====
        category("Keybinds", 0xFF0496C9)
            .sub("Wars / Territory")
                .add(toggle("Territory/Eco Menu Keybind", "Press a key to open /gu manage > Territories directly",
                        () -> config.territoryMenuKeyEnabled, v -> config.territoryMenuKeyEnabled = v))
                .add(visibleWhen(keybind("Territory Key", "Key to open the territory/eco menu",
                                () -> config.territoryMenuKey, v -> config.territoryMenuKey = v,
                                DEFAULT_CONFIG.territoryMenuKey),
                        () -> config.territoryMenuKeyEnabled))
            .sub("Shopping List")
                .add(keybind("Toggle Shopping List", "Toggle the shopping list",
                        () -> config.shoppingListToggleKey, v -> config.shoppingListToggleKey = v,
                        DEFAULT_CONFIG.shoppingListToggleKey))
            .sub("Crafting Helper")
                .add(keybind("Load from Clipboard", "Load a WynnBuilder craft from the clipboard while the crafting helper is open",
                        () -> config.craftingLoadClipboardKey, v -> config.craftingLoadClipboardKey = v,
                        DEFAULT_CONFIG.craftingLoadClipboardKey))
                .add(keybind("Reuse Last", "Reuse the previous craft while the crafting helper is open",
                        () -> config.craftingReuseLastKey, v -> config.craftingReuseLastKey = v,
                        DEFAULT_CONFIG.craftingReuseLastKey))
            .sub("Quick Repair")
                .add(visibleWhen(keybind("Repair Key", "Key to start repair at blacksmith",
                                () -> config.quickRepairKey, v -> config.quickRepairKey = v,
                                DEFAULT_CONFIG.quickRepairKey),
                        () -> config.quickRepairEnabled))
            .sub("Waypoint edit mode")
                .add(keybind("Free move toggle", "Toggle the free move mode in the waypoint edit mode",
                        () -> config.waypointEditFreeMoveToggleKey, v -> config.waypointEditFreeMoveToggleKey = v,
                        DEFAULT_CONFIG.waypointEditFreeMoveToggleKey))
                .add(keybind("Add waypoint", "Add a waypoint at the preview position in waypoint edit mode",
                        () -> config.waypointEditAddKey, v -> config.waypointEditAddKey = v,
                        DEFAULT_CONFIG.waypointEditAddKey))
                .add(keybind("Remove waypoint", "Remove waypoints at the preview position in the active package",
                        () -> config.waypointEditRemoveKey, v -> config.waypointEditRemoveKey = v,
                        DEFAULT_CONFIG.waypointEditRemoveKey))
                .add(keybind("Edit waypoint", "Edit waypoints at the preview position in waypoint edit mode",
                        () -> config.waypointEditExistingKey, v -> config.waypointEditExistingKey = v,
                        DEFAULT_CONFIG.waypointEditExistingKey))
                .add(keybind("Move forward", "Move the waypoint preview forward",
                        () -> config.waypointEditForwardKey, v -> config.waypointEditForwardKey = v,
                        DEFAULT_CONFIG.waypointEditForwardKey))
                .add(keybind("Move left", "Move the waypoint preview left",
                        () -> config.waypointEditLeftKey, v -> config.waypointEditLeftKey = v,
                        DEFAULT_CONFIG.waypointEditLeftKey))
                .add(keybind("Move backwards", "Move the waypoint preview backward",
                        () -> config.waypointEditBackwardKey, v -> config.waypointEditBackwardKey = v,
                        DEFAULT_CONFIG.waypointEditBackwardKey))
                .add(keybind("Move right", "Move the waypoint preview right",
                        () -> config.waypointEditRightKey, v -> config.waypointEditRightKey = v,
                        DEFAULT_CONFIG.waypointEditRightKey))
                .add(keybind("Move up", "Move the waypoint preview up",
                        () -> config.waypointEditUpKey, v -> config.waypointEditUpKey = v,
                        DEFAULT_CONFIG.waypointEditUpKey))
                .add(keybind("Move down", "Move the waypoint preview down",
                        () -> config.waypointEditDownKey, v -> config.waypointEditDownKey = v,
                        DEFAULT_CONFIG.waypointEditDownKey))
            .sub("Tetris")
                .add(keybind("Move right", "Move the active piece right",
                        () -> config.tetrisMoveRightKey, v -> config.tetrisMoveRightKey = v,
                        DEFAULT_CONFIG.tetrisMoveRightKey))
                .add(keybind("Move right alt", "Alternative key to move the active piece right",
                        () -> config.tetrisMoveRightAltKey, v -> config.tetrisMoveRightAltKey = v,
                        DEFAULT_CONFIG.tetrisMoveRightAltKey))
                .add(keybind("Move left", "Move the active piece left",
                        () -> config.tetrisMoveLeftKey, v -> config.tetrisMoveLeftKey = v,
                        DEFAULT_CONFIG.tetrisMoveLeftKey))
                .add(keybind("Move left alt", "Alternative key to move the active piece left",
                        () -> config.tetrisMoveLeftAltKey, v -> config.tetrisMoveLeftAltKey = v,
                        DEFAULT_CONFIG.tetrisMoveLeftAltKey))
                .add(keybind("Soft drop", "Move the active piece down faster",
                        () -> config.tetrisSoftDropKey, v -> config.tetrisSoftDropKey = v,
                        DEFAULT_CONFIG.tetrisSoftDropKey))
                .add(keybind("Soft drop alt", "Alternative key to move the active piece down faster",
                        () -> config.tetrisSoftDropAltKey, v -> config.tetrisSoftDropAltKey = v,
                        DEFAULT_CONFIG.tetrisSoftDropAltKey))
                .add(keybind("Rotate clockwise", "Rotate the active piece clockwise",
                        () -> config.tetrisRotateClockwiseKey, v -> config.tetrisRotateClockwiseKey = v,
                        DEFAULT_CONFIG.tetrisRotateClockwiseKey))
                .add(keybind("Rotate clockwise alt", "Alternative key to rotate the active piece clockwise",
                        () -> config.tetrisRotateClockwiseAltKey, v -> config.tetrisRotateClockwiseAltKey = v,
                        DEFAULT_CONFIG.tetrisRotateClockwiseAltKey))
                .add(keybind("Rotate counterclockwise", "Rotate the active piece counterclockwise",
                        () -> config.tetrisRotateCounterClockwiseKey, v -> config.tetrisRotateCounterClockwiseKey = v,
                        DEFAULT_CONFIG.tetrisRotateCounterClockwiseKey))
                .add(keybind("Rotate counterclockwise alt", "Alternative key to rotate the active piece counterclockwise",
                        () -> config.tetrisRotateCounterClockwiseAltKey, v -> config.tetrisRotateCounterClockwiseAltKey = v,
                        DEFAULT_CONFIG.tetrisRotateCounterClockwiseAltKey))
                .add(keybind("Hard drop", "Instantly drop and lock the active piece",
                        () -> config.tetrisHardDropKey, v -> config.tetrisHardDropKey = v,
                        DEFAULT_CONFIG.tetrisHardDropKey))
                .add(keybind("Hold", "Hold or swap the active piece",
                        () -> config.tetrisHoldKey, v -> config.tetrisHoldKey = v,
                        DEFAULT_CONFIG.tetrisHoldKey))
                .add(keybind("Hold alt", "Alternative key to hold or swap the active piece",
                        () -> config.tetrisHoldAltKey, v -> config.tetrisHoldAltKey = v,
                        DEFAULT_CONFIG.tetrisHoldAltKey))
                .add(keybind("Start", "Start a game from the game-over screen",
                        () -> config.tetrisStartKey, v -> config.tetrisStartKey = v,
                        DEFAULT_CONFIG.tetrisStartKey))
                .add(keybind("Restart", "Restart the current game",
                        () -> config.tetrisRestartKey, v -> config.tetrisRestartKey = v,
                        DEFAULT_CONFIG.tetrisRestartKey))
                .add(keybind("Toggle mode", "Switch between classic and 40 lines on the game-over screen",
                        () -> config.tetrisToggleModeKey, v -> config.tetrisToggleModeKey = v,
                        DEFAULT_CONFIG.tetrisToggleModeKey))
                .add(keybind("Quit", "End the current game",
                        () -> config.tetrisQuitKey, v -> config.tetrisQuitKey = v,
                        DEFAULT_CONFIG.tetrisQuitKey))
            .sub("Debug")
                .add(keybind("Item Components Key", "Show the hovered container item's components in a debug window",
                        () -> config.debugItemComponentsKey, v -> config.debugItemComponentsKey = v,
                        DEFAULT_CONFIG.debugItemComponentsKey));

        // ===== NEW =====
        category("New", 0xFF00bad5)
        .excludeFromSearch()
        .add(text("", "All features added in this update. Toggle any of them on or off."));
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
    public void openDropdown(DropdownOption<?> opt, int x, int y, int w, int optionW) {
        this.activeDropdown = opt;
        this.dropdownX = x;
        this.dropdownY = y;
        this.dropdownWidth = w;
        this.dropdownOptionWidth = optionW;
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
        if (!cat.searchable) return false;
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

    private ConfigOption keybind(String name, String desc, Supplier<Integer> get, Consumer<Integer> set, int defaultKey) {
        return new KeybindOption(name, desc, get, set, defaultKey);
    }

    private ConfigOption toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        return new BooleanOption(name, desc, get, set);
    }

    private ConfigOption color(String name, String desc, Supplier<Integer> get, Consumer<Integer> set, int resetValue, int fallbackColor) {
        return new ColorOption(name, desc, get, set, resetValue, fallbackColor);
    }

    private ConfigOption classColor(String name, String desc, String classKey, int defaultColor) {
        return color(name, desc,
                () -> config.classCardAccentColors.getOrDefault(classKey, -1),
                v -> {
                    if (v == null || v < 0) {
                        config.classCardAccentColors.remove(classKey);
                    } else {
                        config.classCardAccentColors.put(classKey, v);
                    }
                },
                -1, defaultColor);
    }

    private ConfigOption classSelectionLines(String name, String desc) {
        return new LineListOption(name, desc,
                () -> {
                    config.syncClassSelectionLines();
                    return visibleClassSelectionLines(config.classSelectionActiveLines);
                },
                v -> {
                    config.classSelectionActiveLines = mergeHiddenClassSelectionLines(v, config.classSelectionActiveLines);
                    config.syncClassSelectionLines();
                },
                () -> {
                    config.syncClassSelectionLines();
                    return visibleClassSelectionLines(config.classSelectionAvailableLines);
                },
                v -> {
                    config.classSelectionAvailableLines = mergeHiddenClassSelectionLines(v, config.classSelectionAvailableLines);
                    config.syncClassSelectionLines();
                },
                () -> WynnExtrasConfig.CLASS_SELECTION_LINE_NAMES,
                "Active lines",
                "Available lines");
    }

    private List<String> visibleClassSelectionLines(List<String> lines) {
        if (config.classSelectionContentProgressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.LINE) {
            return lines;
        }

        List<String> visible = new ArrayList<>();
        for (String line : lines) {
            if (!WynnExtrasConfig.CLASS_SELECTION_LINE_CONTENT_PROGRESS.equals(line)) {
                visible.add(line);
            }
        }
        return visible;
    }

    private List<String> mergeHiddenClassSelectionLines(List<String> visibleLines, List<String> previousLines) {
        List<String> merged = new ArrayList<>(visibleLines);
        if (config.classSelectionContentProgressStyle == WynnExtrasConfig.ClassSelectionContentProgressStyle.LINE) {
            return merged;
        }

        for (int i = 0; i < previousLines.size(); i++) {
            String line = previousLines.get(i);
            if (!WynnExtrasConfig.CLASS_SELECTION_LINE_CONTENT_PROGRESS.equals(line) || merged.contains(line)) continue;
            merged.add(Math.min(i, merged.size()), line);
        }
        return merged;
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
            Category cat = categories.get(selectedCategory);
            int contentHeight = searchQuery.isEmpty() || categoryHasMatches(cat) ? cat.getTotalHeight() : 0;
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
        boolean categoryVisibleForSearch = searchQuery.isEmpty() || categoryHasMatches(cat);

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
        if (categoryVisibleForSearch) {
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

        DropdownBounds bounds = getDropdownBounds(values, needsScroll);
        int ddW = bounds.width();
        int ddX = bounds.x();
        int ddY = dropdownY;
        int itemW = bounds.itemWidth();

        if (ddY + visibleH > height - 10) {
            ddY = dropdownY - visibleH - 24;
        }

        double maxScroll = Math.max(0, totalContentH - visibleH);
        dropdownScroll = MathHelper.clamp(dropdownScroll, 0, maxScroll);

        ctx.fill(ddX - 3, ddY - 3, ddX + ddW + 3, ddY + visibleH + 3, BORDER_DARK);
        ctx.fill(ddX - 2, ddY - 2, ddX + ddW + 2, ddY + visibleH + 2, selectedCategoryColor);
        ctx.fill(ddX - 1, ddY - 1, ddX + ddW + 1, ddY + visibleH + 1, BG_MEDIUM);
        ctx.fill(ddX, ddY, ddX + ddW, ddY + visibleH, PARCHMENT);

        ctx.enableScissor(ddX, ddY, ddX + itemW, ddY + visibleH);

        for (int i = 0; i < values.length; i++) {
            int iy = ddY + i * DROPDOWN_ITEM_HEIGHT - (int)dropdownScroll;

            if (iy + DROPDOWN_ITEM_HEIGHT < ddY || iy > ddY + visibleH) continue;

            boolean hovered = mouseX >= ddX && mouseX < ddX + itemW
                    && mouseY >= Math.max(ddY, iy) && mouseY < Math.min(ddY + visibleH, iy + DROPDOWN_ITEM_HEIGHT);
            boolean selected = values[i].equals(activeDropdown.getter.get());

            int itemBg = selected ? selectedCategoryColor : (hovered ? PARCHMENT_HOVER : PARCHMENT);
            ctx.fill(ddX, iy, ddX + itemW, iy + DROPDOWN_ITEM_HEIGHT, itemBg);

            if (i > 0) {
                ctx.fill(ddX + 8, iy, ddX + itemW - 8, iy + 1, BG_LIGHT);
            }

            String text = trimDropdownText(values[i].toString(), itemW - 16);
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

    private DropdownBounds getDropdownBounds(Object[] values, boolean needsScroll) {
        int scrollW = needsScroll ? 10 : 0;
        int longestTextW = 0;
        for (Object value : values) {
            longestTextW = Math.max(longestTextW, textRenderer.getWidth(value.toString()));
        }

        int desiredW = Math.max(dropdownWidth, longestTextW + 16 + scrollW);
        int maxW = Math.max(dropdownWidth, dropdownOptionWidth / 2);
        int ddW = Math.min(desiredW, maxW);
        int ddX = dropdownX + dropdownWidth - ddW;
        return new DropdownBounds(ddX, ddW, ddW - scrollW);
    }

    private String trimDropdownText(String text, int maxTextW) {
        if (textRenderer.getWidth(text) <= maxTextW) return text;

        String suffix = "..";
        int suffixW = textRenderer.getWidth(suffix);
        if (maxTextW <= suffixW) return textRenderer.trimToWidth(text, maxTextW);
        return textRenderer.trimToWidth(text, maxTextW - suffixW) + suffix;
    }

    private record DropdownBounds(int x, int width, int itemWidth) {}

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
            DropdownBounds bounds = getDropdownBounds(values, needsScroll);
            int ddW = bounds.width();
            int ddX = bounds.x();
            int ddY = dropdownY;
            int itemW = bounds.itemWidth();

            if (ddY + visibleH > height - 10) {
                ddY = dropdownY - visibleH - 24;
            }

            if (mx >= ddX && mx < ddX + ddW && my >= ddY && my < ddY + visibleH) {
                for (int i = 0; i < values.length; i++) {
                    int iy = ddY + i * DROPDOWN_ITEM_HEIGHT - (int)dropdownScroll;
                    if (iy < ddY - DROPDOWN_ITEM_HEIGHT || iy > ddY + visibleH) continue;

                    if (my >= Math.max(ddY, iy) && my < Math.min(ddY + visibleH, iy + DROPDOWN_ITEM_HEIGHT)
                            && mx < ddX + itemW) {
                        activeDropdown.setValueByIndex(i);
                        activeDropdown = null;
                        dropdownScroll = 0;
                        updateMaxScroll();
                        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                saveCurrentScreenState();
                WynnExtrasConfig.save();
                WynnExtrasConfig.load();
                applyPrivacyChanges();
                client.setScreen(parent);
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            //======== Cancel =========
            if (mx >= width - 225 && mx < width - 125) {
                saveCurrentScreenState();
                WynnExtrasConfig.load();
                client.setScreen(parent);
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            //======== Edit HUD Position =========
            if (mx >= width - 335 && mx < width - 235) {
                WynnExtrasConfig.save();
                WynnExtrasConfig.load();
                client.setScreen(new HudEditScreen(this));
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                    onSearchQueryChanged();
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else if (my >= sidebarScrollbarY && my < sidebarScrollbarY + sidebarScrollbarHeight) {
                double clickPercent = (my - sidebarScrollbarY - sidebarScrollbarThumbH / 2.0) / (sidebarScrollbarHeight - sidebarScrollbarThumbH);
                sidebarScrollTarget = MathHelper.clamp(clickPercent * sidebarMaxScroll, 0, sidebarMaxScroll);
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        // Scrollbar
        if (maxScroll > 0 && mx >= width - 17 && mx < width - 11) {
            if (my >= scrollbarThumbY && my < scrollbarThumbY + scrollbarThumbH) {
                scrollbarDragging = true;
                scrollbarDragOffset = my - scrollbarThumbY;
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else if (my >= scrollbarY && my < scrollbarY + scrollbarHeight) {
                double clickPercent = (my - scrollbarY - scrollbarThumbH / 2.0) / (scrollbarHeight - scrollbarThumbH);
                scrollTarget = MathHelper.clamp(clickPercent * maxScroll, 0, maxScroll);
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (stickySub != null) {
            int stickyY = HEADER_HEIGHT + 30;
            if (my >= stickyY && my < stickyY + SUBCATEGORY_HEADER_HEIGHT
                    && mx >= SIDEBAR_WIDTH + 20 && mx < width - 30) {
                Category cat = categories.get(selectedCategory);
                if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) return true;

                stickySub.toggleExpanded();
                updateMaxScroll();
                if (!stickySub.isExpanded()) {
                    // scroll so the now collapsed header sits at the top of the viewport
                    int contentW = width - SIDEBAR_WIDTH - 40;
                    int contentY = 0;
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
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
        }

        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            Category cat = categories.get(selectedCategory);
            if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) return super.mouseClicked(click, doubleClick);

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
                        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                    y += SUBCATEGORY_HEADER_HEIGHT + 5;

                    if (sub.isExpanded()) {
                        for (ConfigOption opt : sub.options) {
                            if (matchesSearch(opt)) {
                                int optH = opt.getHeight(contentW - 8);
                                if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + optH)) {
                                    if (opt.mouseClicked(mx, my, contentX + 8, y, contentW - 8, optH, btn)) {
                                        updateMaxScroll();
                                        return true;
                                    }
                                }
                                y += optH + OPTION_SPACING;
                            }
                        }
                    }
                } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                    int optH = opt.getHeight(contentW);
                    if (my >= Math.max(listTop, y) && my < Math.min(listBot, y + optH)) {
                        if (opt.mouseClicked(mx, my, contentX, y, contentW, optH, btn)) {
                            updateMaxScroll();
                            return true;
                        }
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
            if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) return super.mouseReleased(click);

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
            if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) return super.mouseDragged(click, dx, dy);

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
        for (ConfigOption opt : getCurrentOptions()) {
            if (opt.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

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
                    onSearchQueryChanged();
                }
                return true;
            } else if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(searchQuery);
                return true;
            } else if (ctrl && key == org.lwjgl.glfw.GLFW.GLFW_KEY_X) {
                net.minecraft.client.MinecraftClient.getInstance().keyboard.setClipboard(searchQuery);
                searchQuery = "";
                onSearchQueryChanged();
                return true;
            } else if (key == 259) { // Backspace
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    onSearchQueryChanged();
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
        char c = (char) charInput.codepoint();
        for (ConfigOption opt : getCurrentOptions()) {
            if (opt.charTyped(c, charInput.modifiers())) return true;
        }

        // Block character input when Ctrl is held (Ctrl+V etc.)
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        boolean ctrlHeld = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        if (ctrlHeld) return true;

        if (searchFocused) {
            if (c >= 32 && c < 127) {
                searchQuery += c;
                onSearchQueryChanged();
                return true;
            }
        }
        return super.charTyped(charInput);
    }

    private List<ConfigOption> getCurrentOptions() {
        List<ConfigOption> options = new ArrayList<>();
        if (selectedCategory < 0 || selectedCategory >= categories.size()) return options;

        Category cat = categories.get(selectedCategory);
        if (!searchQuery.isEmpty() && !categoryHasMatches(cat)) return options;

        for (Object item : cat.items) {
            if (item instanceof SubCategory sub) {
                if (!subHasMatches(sub) || !sub.isExpanded()) continue;
                for (ConfigOption opt : sub.options) {
                    if (matchesSearch(opt)) options.add(opt);
                }
            } else if (item instanceof ConfigOption opt && matchesSearch(opt)) {
                options.add(opt);
            }
        }
        return options;
    }

    private void onSearchQueryChanged() {
        if (searchQuery.isEmpty()) {
            restoreExpandedSubsBeforeSearch();
        } else {
            if (expandedSubsBeforeSearch == null) {
                expandedSubsBeforeSearch = snapshotExpandedSubs();
            }
            expandSubCategoriesWithSearchMatches();
        }
        scrollOffset = 0;
        scrollTarget = 0;
        autoSelectMatchingCategory();
        updateMaxScroll();
    }

    private Map<String, Boolean> snapshotExpandedSubs() {
        Map<String, Boolean> snapshot = new HashMap<>();
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    snapshot.put(subKey(cat, sub), sub.isExpanded());
                }
            }
        }
        return snapshot;
    }

    private void restoreExpandedSubsBeforeSearch() {
        if (expandedSubsBeforeSearch == null) return;
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    Boolean expanded = expandedSubsBeforeSearch.get(subKey(cat, sub));
                    if (expanded != null) sub.setExpanded(expanded);
                }
            }
        }
        expandedSubsBeforeSearch = null;
    }

    private void expandSubCategoriesWithSearchMatches() {
        for (Category cat : categories) {
            if (!cat.searchable) continue;
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub && subHasMatches(sub)) {
                    sub.setExpanded(true);
                }
            }
        }
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
        saveCurrentScreenState();
        client.setScreen(parent);
    }

    private void saveCurrentScreenState() {
        restoreExpandedSubsBeforeSearch();
        saveLastScreenState(selectedCategory, scrollTarget, categories);
    }

    private void applyPrivacyChanges() {
        WynnExtrasConfig saved = WynnExtrasConfig.INSTANCE;
        if (originalTelemetryMode != saved.telemetryMode
                || originalDoNotPublishOwnBadge != saved.doNotPublishOwnBadge) {
            BadgeService.syncWithServerSoon();
        }
        if (originalDoNotPublishOwnAspects != saved.doNotPublishOwnAspects) {
            WynncraftApiHandler.syncAspectPublication(!saved.doNotPublishOwnAspects, !saved.doNotPublishOwnAspects);
        }
    }

    private static void saveLastScreenState(int selectedCategory, double scrollTarget, List<Category> categories) {
        lastSelectedCategory = selectedCategory;
        lastScrollTarget = scrollTarget;
        for (Category cat : categories) {
            for (Object item : cat.items) {
                if (item instanceof SubCategory sub) {
                    lastExpandedSubs.put(subKey(cat, sub), sub.isExpanded());
                }
            }
        }
    }

    private static String subKey(Category cat, SubCategory sub) {
        return cat.name + "/" + sub.name;
    }
}
