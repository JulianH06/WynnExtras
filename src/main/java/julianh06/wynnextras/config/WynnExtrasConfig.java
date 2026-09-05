package julianh06.wynnextras.config;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.features.raid.RaidLootTrackerOverlay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WynnExtrasConfig {
    public enum Align { LEFT, CENTER, RIGHT }
    public enum TelemetryMode {
        ON("On"),
        ANONYMIZE("Anonymize"),
        OFF("Off");

        private final String displayName;

        TelemetryMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum MythicScaleSource {
        WYNNPOOL("Wynnpool"),
        NORI("Nori"),
        BOTH("Both");

        private final String displayName;

        MythicScaleSource(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum MountMaterialLevel {
        LEVEL_0(0),
        LEVEL_10(10),
        LEVEL_20(20),
        LEVEL_30(30),
        LEVEL_40(40),
        LEVEL_50(50),
        LEVEL_60(60),
        LEVEL_70(70),
        LEVEL_80(80),
        LEVEL_90(90),
        LEVEL_100(100),
        LEVEL_105(105),
        LEVEL_110(110),
        LEVEL_115(115);

        private final int level;

        MountMaterialLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return "Level " + level;
        }
    }

    public enum NotifierAnimation {
        APPEAR("Appear"),
        FADE("Fade"),
        FLY_IN("Fly in"),
        PEEK_IN("Peek in"),
        WIPE("Wipe"),
        ZOOM("Zoom"),
        PINWHEEL("Pinwheel"),
        BOUNCE("Bounce"),
        EXPAND("Expand"),
        FADED_SWIVEL("Faded swivel");

        private final String displayName;

        NotifierAnimation(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public boolean isDirectional() {
            return this == FLY_IN || this == PEEK_IN || this == WIPE;
        }
    }

    public enum NotifierExitAnimation {
        DISAPPEAR("Disappear"),
        FADE("Fade"),
        FLY_OUT("Fly out"),
        PEEK_OUT("Peek out"),
        WIPE("Wipe"),
        ZOOM("Zoom"),
        PINWHEEL("Pinwheel"),
        BOUNCE("Bounce"),
        CONTRACT("Contract"),
        FADED_SWIVEL("Faded swivel");

        private final String displayName;

        NotifierExitAnimation(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public boolean isDirectional() {
            return this == FLY_OUT || this == PEEK_OUT || this == WIPE;
        }
    }

    public enum NotifierAnimationDirection {
        LEFT("Left"),
        RIGHT("Right"),
        TOP("Top"),
        BOTTOM("Bottom");

        private final String displayName;

        NotifierAnimationDirection(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ClassSelectionContentProgressStyle {
        LINE("Line"),
        PROGRESS_BAR("Progress Bar"),
        COMPACT("Compact (inline with name)");

        private final String displayName;

        ClassSelectionContentProgressStyle(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ClassSelectionCompletionChromaMode {
        NAME_AND_LINES("Name + lines"),
        NAME_ONLY("Name only"),
        NONE("None");

        private final String displayName;

        ClassSelectionCompletionChromaMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ChatMediaPreviewLoadPolicy {
        HOVER("Hover"),
        CLICK_TO_LOAD("Click to load");

        private final String displayName;

        ChatMediaPreviewLoadPolicy(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ChatMediaPreviewPosition {
        TOP_LEFT("Top left"),
        TOP("Top"),
        TOP_RIGHT("Top right"),
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right"),
        BOTTOM_LEFT("Bottom left"),
        BOTTOM("Bottom"),
        BOTTOM_RIGHT("Bottom right");

        private final String displayName;

        ChatMediaPreviewPosition(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ChatMediaPreviewHoverPosition {
        CURSOR("Cursor", null),
        TOP_LEFT("Top left", ChatMediaPreviewPosition.TOP_LEFT),
        TOP("Top", ChatMediaPreviewPosition.TOP),
        TOP_RIGHT("Top right", ChatMediaPreviewPosition.TOP_RIGHT),
        LEFT("Left", ChatMediaPreviewPosition.LEFT),
        CENTER("Center", ChatMediaPreviewPosition.CENTER),
        RIGHT("Right", ChatMediaPreviewPosition.RIGHT),
        BOTTOM_LEFT("Bottom left", ChatMediaPreviewPosition.BOTTOM_LEFT),
        BOTTOM("Bottom", ChatMediaPreviewPosition.BOTTOM),
        BOTTOM_RIGHT("Bottom right", ChatMediaPreviewPosition.BOTTOM_RIGHT);

        private final String displayName;
        private final ChatMediaPreviewPosition fixedPosition;

        ChatMediaPreviewHoverPosition(String displayName, ChatMediaPreviewPosition fixedPosition) {
            this.displayName = displayName;
            this.fixedPosition = fixedPosition;
        }

        public ChatMediaPreviewPosition getFixedPosition() {
            return fixedPosition;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static final String CLASS_SELECTION_LINE_LEVEL = "level";
    public static final String CLASS_SELECTION_LINE_LOCATION = "location";
    public static final String CLASS_SELECTION_LINE_PLAYTIME = "playtime";
    public static final String CLASS_SELECTION_LINE_CONTENT_PROGRESS = "content_progress";
    public static final String CLASS_SELECTION_LINE_LAST_HELD_WEAPON = "last_held_weapon";
    public static final List<String> CLASS_SELECTION_BASE_LINE_IDS = List.of(
            CLASS_SELECTION_LINE_LEVEL,
            CLASS_SELECTION_LINE_PLAYTIME);
    public static final List<String> CLASS_SELECTION_LINE_IDS = List.of(
            CLASS_SELECTION_LINE_LEVEL,
            CLASS_SELECTION_LINE_LOCATION,
            CLASS_SELECTION_LINE_PLAYTIME,
            CLASS_SELECTION_LINE_CONTENT_PROGRESS,
            CLASS_SELECTION_LINE_LAST_HELD_WEAPON);
    public static final Map<String, String> CLASS_SELECTION_LINE_NAMES = createClassSelectionLineNames();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("wynnextras")
            .resolve("wynnextras.json");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static WynnExtrasConfig INSTANCE = new WynnExtrasConfig();

    /** Named bool-only profiles. Each profile maps fieldName -> value. Switching a profile
     *  applies its bool snapshot; non-bool settings (positions, colors, etc.) are shared. */
    //public LinkedHashMap<String, HashMap<String, Boolean>> configProfiles = new LinkedHashMap<>();
    //public String activeProfile = null;
    // ==================== HIDERS ====================
    public boolean playerHiderToggle = true;
    public int maxHideDistance = 3;
    public boolean hideAllPlayers = false;
    public boolean hideAllPlayersInWar = false;
    public List<String> hiddenPlayers = new ArrayList<>();
    public String spellProfile = "default_off";

    // ==================== CHAT NOTIFIER ====================
    public List<String> notifierWords = new ArrayList<>();
    public int textDurationInMs = 2000;
    public TextColor textColor = TextColor.WHITE;
    public NotificationSound notificationSound = NotificationSound.EXPERIENCE_ORB;
    public float soundVolume = 100f;
    public float soundPitch = 100f;
    public boolean privateMessageSound = false;
    public NotificationSound privateMessageSoundType = NotificationSound.NOTE_PLING;
    public float privateMessageSoundVolume = 100f;
    public float privateMessageSoundPitch = 100f;
    public int notifierX = -1;  // -1 = auto center
    public int notifierY = -1;  // -1 = auto (30% from top)
    public float notifierScale = 3.0f;
    public Align notifierAlignment = Align.CENTER;
    public NotifierAnimation notifierAnimation = NotifierAnimation.FADE;
    public NotifierAnimationDirection notifierEntranceDirection = NotifierAnimationDirection.TOP;
    public NotifierExitAnimation notifierExitAnimation = NotifierExitAnimation.FADE;
    public NotifierAnimationDirection notifierExitDirection = NotifierAnimationDirection.BOTTOM;
    public int notifierFadeInMs = 250;
    public int notifierFadeOutMs = 250;

    // ==================== CHAT NOTIFIER PREMADES ====================

    public Map<String, Boolean> premades;
    public boolean lostEye = true;
    public boolean oneGoo = false;
    public boolean twoGoo = false;
    public boolean soul = true;
    public boolean voidMatter = false;
    public boolean fourOutOfFiveVoidMatter = true;
    public boolean oneLightCrystal = false;
    public boolean twoLightCrystal = false;
    public boolean notgUpperPlatform = false;
    public boolean notgLowerPlatform = false;
    public boolean artifactRestored = true;
    public boolean itemZeroDurability = true;
    public boolean colossalCoreSpawned = false;

    public void syncPremades() {
        if(premades == null) premades = new HashMap<>();

        premades.put("You feel like thousands of eyes|LOST EYE", lostEye);
        premades.put("+1 Slimey Goo|+1 Goo", oneGoo);
        premades.put("+2 Slimey Goo|+2 Goos", twoGoo);
        premades.put("Another Soul must be given!|NEXT SOUL", soul);
        premades.put("+1 Void Matter|+1 Void Matter", voidMatter);
        premades.put("The Void Holes have begun to destabilize!|KILL THE VOID HOLES", fourOutOfFiveVoidMatter);
        premades.put("+1 Light Crystal|+1 Crystal", oneLightCrystal);
        premades.put("+2 Light Crystal|+2 Crystals", twoLightCrystal);
        premades.put("The players on the|UPPER PLATFORM SPAWNED", notgUpperPlatform);
        premades.put("A new platform has|LOWER PLATFORM SPAWNED", notgLowerPlatform);
        premades.put("The Artifact's power has been restored|SPEAR RECHARGED", artifactRestored);
        premades.put("One of your items has reached zero durability|ITEM BROKE", itemZeroDurability);
        premades.put("A Colossal Core has spawned!|CORE SPAWNED", colossalCoreSpawned);

        //Isoptera announcements
        premades.put("The Interdimensional Isoptera is in the Gray Grotto|GRAY", isopteraGray);
        premades.put("The Interdimensional Isoptera is in the Black Grotto|BLACK", isopteraBlack);
        premades.put("The Interdimensional Isoptera is in the White Grotto|WHITE", isopteraWhite);
        premades.put("The Interdimensional Isoptera is in the Orange Grotto|ORANGE", isopteraOrange);
        premades.put("The Interdimensional Isoptera is in the Blue Grotto|BLUE", isopteraBlue);
    }

    // ==================== ISOPTERA PREMADE NOTIFICATIONS ====================
    public boolean isopteraGray = false;
    public boolean isopteraBlack = false;
    public boolean isopteraWhite = false;
    public boolean isopteraOrange = false;
    public boolean isopteraBlue = false;


    // ==================== CHAT BLOCKER ====================
    public List<String> blockedWords = new ArrayList<>();

    // ==================== INVENTORY ====================
    public boolean toggleBankOverlay = true;
    public boolean smoothScrollToggle = true;
    public boolean bankQuickToggle = true;
    public boolean bankAllCharactersBrowseMode = false;
    public boolean allowAllCharactersModeOnIronmanClasses = false;
    public int bankOverlayMaxRows = 3;
    public int bankOverlayMaxColumns = 3;
    public boolean showWynntilsBankPageJumpButtons = false;
    public boolean bankOverlayHideEmptyRows = false;
    public boolean bankOverlayExcludeActivePageFromSearches = false;
    public boolean disableStickyNameplates = false;
    public boolean bankBagOverlay = false;
    public boolean showTotalBagsInBankOverlay = false;
    public int maxAnnotationCalculationsPerFrame = 75;
    public boolean showWeight = false;
    public boolean showScales = false;
    public MythicScaleSource mythicScaleSource = MythicScaleSource.WYNNPOOL;
    public boolean lockMythicScaleSource = false;
    public boolean scaleBackgroundEnabled = false;
    public ScaleBackgroundShape scaleBackgroundShape = ScaleBackgroundShape.BOX;
    public int scaleBackgroundOpacity = 100;
    public boolean hideTMInfoText = false;
    public boolean hideScaleBackgroundButton = false;
    public boolean craftingHelperOverlay = true;
    public boolean powderCombineHelper = true;
    public boolean craftingAutoStart = false;
    public int craftingLoadClipboardKey = GLFW.GLFW_KEY_T;
    public int craftingReuseLastKey = GLFW.GLFW_KEY_SPACE;
    public List<String> craftingLastMaterialNames = new ArrayList<>();
    public List<Integer> craftingLastMaterialCounts = new ArrayList<>();
    public List<String> craftingLastIngredientNames = new ArrayList<>();
    public boolean craftingPreviewOverlay = true;
    public boolean craftingPreviewBackground = true;
    public int craftingPreviewOverlayX = 20;
    public int craftingPreviewOverlayY = 20;
    public boolean craftingHelperReverseOrder = false;
    public float craftingHelperHeightPercent = 0.6f;
    public int craftingHelperWidth = 165;
    public boolean shoppingListMenuEnabled = false;
    public boolean shoppingListShowQuickToggleButton = true;
    public boolean shoppingListWynnMarketSearchCompatibility = true;
    public int shoppingListToggleKey = GLFW.GLFW_KEY_UNKNOWN;
    public boolean shoppingListProfessionSpeed = false;
    public int shoppingListCraftMultiplier = 1;
    public int shoppingListMenuWidth = 190;
    public int shoppingListMenuHeight = 222;
    public ShoppingListPosition shoppingListMenuDefaultPosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListMenuTradePosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListMenuBankOverlayPosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListMenuBankVanillaPosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListLauncherButtonDefaultPosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListLauncherButtonTradePosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListLauncherButtonBankOverlayPosition = new ShoppingListPosition();
    public ShoppingListPosition shoppingListLauncherButtonBankVanillaPosition = new ShoppingListPosition();
    public boolean skillpointHelper = true;
    public boolean tradeMarketOverlay = false;
    public int tradeMarketOverlayX = 10;
    public int tradeMarketOverlayY = 10;
    public boolean tradeMarketOverlayBackground = true;
    public boolean showMountHelper = false;
    public MountMaterialLevel mountHelperDefaultMaterialLevel = MountMaterialLevel.LEVEL_0;
    public boolean mountPrimaryColorBackground = false;

    // ==================== RAID ====================
    public boolean toggleRaidTimestamps = true;
    public boolean toggleRaidLootTracker = true;
    public boolean raidLootTrackerRenderInHud = true;
    public boolean raidLootTrackerRenderInInventory = true;
    public boolean raidLootTrackerRenderInChat = true;
    public boolean raidLootTrackerOnlyNearChest = true;
    public boolean raidLootTrackerCompact = false;
    public int raidLootTrackerX = 5;
    public int raidLootTrackerY = 5;
    public List<String> raidLootTrackerHiddenLines = new ArrayList<>();
    public boolean raidLootTrackerBackground = true;
    public RaidLootTrackerOverlay.mode raidLootTrackerMode = RaidLootTrackerOverlay.mode.ALL;
    public boolean autoIgnorePartyInRaid = false;
    public boolean encounterOverlayEnabled = false;
    public boolean rightClickToCopyChat = false;
    public boolean raidSessionEnabled = false;
    public boolean raidSessionOnlyInRaid = false;
    public boolean raidSessionOnlyInInventory = false;
    public boolean raidSessionShowRuns = false;
    public boolean raidSessionShowFails = false;
    public boolean raidSessionShowRate = false;
    public boolean raidSessionShowTime = false;
    public boolean raidSessionShowAvgTime = false;
    public int raidSessionHudX = 4;
    public int raidSessionHudY = 270;
    public float raidSessionHudScale = 1.0f;
    public boolean toggleFastRequeue = false;
    public boolean quickRepairEnabled = true;
    public int quickRepairKey = org.lwjgl.glfw.GLFW.GLFW_KEY_R;
    public int quickRepairDurabilityThreshold = 35;
    public boolean shiftDisableGuildRaid = true;

    public boolean autoStreamEnabled = false;
    public boolean autoSkipDialogueEnabled = false;
    public boolean autoSkipCutscenesEnabled = false;
    public boolean stackDuplicateMessages = false;
    public int stackDuplicateWindowMinutes = 5;
    public boolean auraPingEnabled = false;
    public String auraPingColor = "FF6F00";
    public boolean weeklyWarCountEnabled = false;
    public int weeklyWarCountX = 5;
    public int weeklyWarCountY = 5;
    public List<Long> weeklyWars = new ArrayList<>();
    public boolean warDpsEnabled = false;
    public int warDpsX = 5;
    public int warDpsY = 50;
    public boolean attackTimerMenuEnabled = false;
    public boolean attackTimerAutoBroadcast = false;
    public int attackTimerX = 5;
    public int attackTimerY = 150;
    public Integer attackTimerNormalColor = 0xFFAA00;
    public Integer attackTimerCurrentTerritoryColor = 0xFFFF55;
    public Integer attackTimerVeryLowDefenseColor = 0x55FF55;
    public Integer attackTimerLowDefenseColor = 0x55FF55;
    public Integer attackTimerMediumDefenseColor = 0xFFFF55;
    public Integer attackTimerHighDefenseColor = 0xFF5555;
    public Integer attackTimerVeryHighDefenseColor = 0xAA0000;
    public boolean warBeaconEnabled = false;
    public HashMap<String, Integer> hudColorOverrides = new HashMap<>();
    public boolean territoryMenuKeyEnabled = false;
    public int territoryMenuKey = org.lwjgl.glfw.GLFW.GLFW_KEY_I;
    public boolean guildBankKeyEnabled = false;
    public int guildBankKey = org.lwjgl.glfw.GLFW.GLFW_KEY_Y;
    public int waypointMaxRange = 1000;
    public int waypointEditFreeMoveToggleKey = GLFW.GLFW_KEY_I;
    public int waypointEditAddKey = GLFW.GLFW_KEY_ENTER;
    public int waypointEditRemoveKey = GLFW.GLFW_KEY_BACKSPACE;
    public int waypointEditExistingKey = GLFW.GLFW_KEY_E;
    public int waypointEditForwardKey = GLFW.GLFW_KEY_W;
    public int waypointEditLeftKey = GLFW.GLFW_KEY_A;
    public int waypointEditBackwardKey = GLFW.GLFW_KEY_S;
    public int waypointEditRightKey = GLFW.GLFW_KEY_D;
    public int waypointEditUpKey = GLFW.GLFW_KEY_SPACE;
    public int waypointEditDownKey = GLFW.GLFW_KEY_LEFT_SHIFT;
    public boolean provokeTimerToggle = false;
    public Map<String, Long> raidPBs = new HashMap<>();
    public boolean chiropTimer = false;
    public boolean automaticAspectScanning = false;
    public boolean passiveAspectScanning = true;
    public boolean tnaTreeMap = false;
    public float tnaTreeMapScale = 1.75f;
    public boolean showTreeMapOnlyWhileInsideOfTree = false;
    public boolean showPathsOnTreeMap = true;
    public boolean showTreeMapEverywhere = false;
    public int treeMapX = 5;
    public int treeMapY = 5;
    public float treeMapScale = 1.0f;
    public boolean drawPathInTree = false;

    // ==================== ASPECTS SCORING ====================
    public AspectScoringMode aspectScoringMode = AspectScoringMode.MAX;
    public boolean showIndividualAspectScore = true;
    public float mythicAspectMultiplier = 26;
    public float fabledAspectMultiplier = 1;
    public float legendaryAspectMultiplier = 0.4F;
    public float favoriteMultiplier = 3;

    // ==================== CHAT CLICK ====================
    public boolean chatClickPV = false;
    public boolean bombShareSuggestion = false;
    public boolean bombRethrowSuggestion = false;
    public boolean chatMediaPreviewEnabled = false;
    public ChatMediaPreviewLoadPolicy chatMediaPreviewLoadPolicy = ChatMediaPreviewLoadPolicy.CLICK_TO_LOAD;
    public boolean chatMediaPreviewAutoDisplay = false;
    public ChatMediaPreviewPosition chatMediaPreviewPosition = ChatMediaPreviewPosition.TOP_RIGHT;
    public ChatMediaPreviewHoverPosition chatMediaPreviewHoverPosition = ChatMediaPreviewHoverPosition.CENTER;
    public int chatMediaPreviewMaxScreenPercent = 50;
    public int chatMediaPreviewMaxDownloadMb = 8;
    public int chatMediaPreviewMaxPixels = 16777216;
    public int chatMediaPreviewMaxGifFrames = 120;

    // ==================== Crowd Sourcing ================
    public boolean crowdSourceGambits = true;

    // ==================== MISC ====================
    public TextColor provokeTimerColor = TextColor.WHITE;
    public boolean differentGUIScale = false;
    public boolean updateReminderDisabled = false;
    public boolean showLootpoolButtonInPartyFinder = true;
    public boolean redirectWynntilsViewStatsToPV = false;
    public boolean arrowHiderToggle = false;

    public boolean showOwnNametag = false;
    public boolean showWynnExtrasBadges = true;
    public boolean uploadAchievements = true;
    public boolean showAchievementUnlockMessages = true;

    // ==================== PRIVACY ====================
    public TelemetryMode telemetryMode = TelemetryMode.ON;
    public boolean doNotFetchWynnExtrasBadges = false;
    public boolean doNotFetchWynnExtrasAchievements = false;
    public boolean doNotFetchWynnExtrasAspects = false;
    public boolean doNotFetchWynnExtrasGambits = false;
    public boolean doNotFetchWynnExtrasProfileTitles = false;
    public boolean doNotFetchWynnExtrasResetTimes = false;
    public boolean doNotPublishOwnBadge = false;
    public boolean doNotPublishOwnAspects = false;

    // ==================== CHAT PEEK ====================
    public boolean chatPeekEnabled = false;
    public int chatPeekKey = GLFW.GLFW_KEY_Y;
    public boolean chatPeekToggle = false;
    public boolean chatPeekAllowVanillaScroll = false;
    //WIP, not used currently

    // ==================== TOTEM TIMER ====================
    public boolean totemTimerEnabled = false;
    public boolean totemTimerOwnOnly = true;
    public boolean totemTimerWarningText = true;
    public boolean totemTimerWarningSound = false;
    public float totemTimerWarningSoundVolume = 50f;
    public int totemTimerWarningThreshold = 2;
    public boolean totemTimerEstimate = true;
    public boolean totemTimerTimeOnly = false;
    public boolean totemTimerShowToxoplasmosis = false;
    public boolean totemTimerSolidColor = false;
    public int totemTimerX = -1;
    public int totemTimerY = 40;
    public float totemTimerScale = 1.0f;
    public TextColor totemTimerWarningTextColor = TextColor.RED;
    public Align totemTimerAlignment = Align.CENTER;
    public int totemWarningX = -1;  // -1 = auto center
    public int totemWarningY = 80;
    public float totemWarningScale = 2.0f;
    public Align totemWarningAlignment = Align.CENTER;

    // ==================== CURSE TRACKER ====================
    public boolean curseTrackerEnabled = false;
    public int curseTrackerX = -1;
    public int curseTrackerY = 80;
    public float curseTrackerScale = 1.0f;
    public Align curseTrackerAlignment = Align.CENTER;
    public int curseTimeout = 5;
    public boolean curseTrackerColorMobs = false;
    public TextColor curseTrackerMobColor = TextColor.DARK_PURPLE;

    // ==================== BLOOD SORROW TIMER ====================
    public boolean bloodSorrowTimerEnabled = false;
    public boolean autoDetectBloodSorrowTime = true;
    public boolean autoDetectAcolyteAspectTier = true;
    public boolean autoDetectResonanceInHand = true;
    public boolean resoInHand = false;
    public int acolyteAspect = 0;
    public int bloodSorrowTimerX = -1;
    public int bloodSorrowTimerY = 60;
    public float bloodSorrowTimerScale = 1.0f;
    public Align bloodSorrowAlignment = Align.CENTER;

    // ==================== PROFESSION OVERLAY ====================
    public boolean professionOverlayEnabled = false;
    public int professionOverlayX = 5;
    public int professionOverlayY = 100;
    public float professionOverlayScale = 1.0f;
    public boolean professionOverlayExactXp = false;
    public Map<String, Float> professionOverflowXp = new HashMap<>();
    public Map<String, Float> professionGoals = new HashMap<>();
    public Map<String, Integer> professionLevels = new HashMap<>();
    public Map<String, Integer> professionXpCurrent = new HashMap<>();
    public Map<String, Integer> professionXpMax = new HashMap<>();
    public Map<String, Integer> professionLevelGoals = new HashMap<>();

    // ==================== RADIANT HUD ====================
    public boolean radiantHudEnabled = false;
    public int radiantHudX = 5;
    public int radiantHudY = 80;
    public float radiantHudScale = 1.0f;

    // ==================== PROVOKE TIMER HUD ====================
    public int provokeTimerDuration = 5;
    public int provokeTimerX = -1;
    public int provokeTimerY = 20;
    public float provokeTimerScale = 1.0f;
    public Align provokeTimerAlignment = Align.CENTER;
    public int customGUIScale = 3;
    public boolean removeFrontPersonView = false;
    public boolean identifierCaseOpening = false;
    public boolean sourceOfTruthToggle = false;
    public boolean territoryEstimateToggle = false;
    public boolean removeChroma = false;
    public int debugItemComponentsKey = GLFW.GLFW_KEY_UNKNOWN;
    public int debugItemComponentsWindowX = 20;
    public int debugItemComponentsWindowY = 20;
    public int debugItemComponentsWindowW = 360;
    public int debugItemComponentsWindowH = 220;

    // ==================== CUSTOM CLASS SELECTION ====================
    public boolean customClassSelectionEnabled = true;
    public boolean classSelectionBackgroundEnabled = false;
    public boolean useCustomClassColors = false;
    public Map<String, Integer> classCardAccentColors = new HashMap<>();
    public boolean hideClassSelectionQuickToggleButton = false;
    public Map<String, String> clientNicknames = new HashMap<>(); // UUID -> nickname
    public List<String> classSelectionActiveLines = new ArrayList<>(CLASS_SELECTION_BASE_LINE_IDS);
    public List<String> classSelectionAvailableLines = new ArrayList<>();
    public ClassSelectionContentProgressStyle classSelectionContentProgressStyle = ClassSelectionContentProgressStyle.LINE;
    public ClassSelectionCompletionChromaMode classSelectionCompletionChromaMode = ClassSelectionCompletionChromaMode.NAME_AND_LINES;

    // ==================== TETRIS ====================
    public int tetrisBestScore = 0;
    public int tetrisBest40LinesMs = 0;
    public int tetrisDAS = 100;
    public int tetrisARR = 30;
    public int tetrisSDFDelay = 100;
    public int tetrisSDF = 30;
    public boolean hideTetrisBackgroundBlur = false;
    public boolean tetris20GEnabled = false;
    public int tetris20GLevel = 20;
    public int tetrisMoveRightKey = GLFW.GLFW_KEY_RIGHT;
    public int tetrisMoveRightAltKey = GLFW.GLFW_KEY_D;
    public int tetrisMoveLeftKey = GLFW.GLFW_KEY_LEFT;
    public int tetrisMoveLeftAltKey = GLFW.GLFW_KEY_A;
    public int tetrisSoftDropKey = GLFW.GLFW_KEY_DOWN;
    public int tetrisSoftDropAltKey = GLFW.GLFW_KEY_S;
    public int tetrisRotateClockwiseKey = GLFW.GLFW_KEY_UP;
    public int tetrisRotateClockwiseAltKey = GLFW.GLFW_KEY_W;
    public int tetrisRotateCounterClockwiseKey = GLFW.GLFW_KEY_Z;
    public int tetrisRotateCounterClockwiseAltKey = GLFW.GLFW_KEY_X;
    public int tetrisHardDropKey = GLFW.GLFW_KEY_SPACE;
    public int tetrisHoldKey = GLFW.GLFW_KEY_C;
    public int tetrisHoldAltKey = GLFW.GLFW_KEY_LEFT_SHIFT;
    public int tetrisStartKey = GLFW.GLFW_KEY_ENTER;
    public int tetrisRestartKey = GLFW.GLFW_KEY_R;
    public int tetrisToggleModeKey = GLFW.GLFW_KEY_TAB;
    public int tetrisQuitKey = GLFW.GLFW_KEY_Q;

    //==================== Dark Modes ==========================
    public boolean darkmodeToggle = false; //for bank overlay (dont wanna change the variable cause it would reset it to false for everyone)
    public boolean pvDarkmodeToggle = false;

    // ==================== ENUMS ====================
    public enum AspectScoringMode {
        MAX
    }

    public enum TextColor {
        WHITE(Formatting.WHITE),
        BLACK(Formatting.BLACK),
        AQUA(Formatting.AQUA),
        RED(Formatting.RED),
        YELLOW(Formatting.YELLOW),
        BLUE(Formatting.BLUE),
        GREEN(Formatting.GREEN),
        DARK_BLUE(Formatting.DARK_BLUE),
        DARK_GREEN(Formatting.DARK_GREEN),
        DARK_AQUA(Formatting.DARK_AQUA),
        DARK_RED(Formatting.DARK_RED),
        DARK_PURPLE(Formatting.DARK_PURPLE),
        LIGHT_PURPLE(Formatting.LIGHT_PURPLE),
        GRAY(Formatting.GRAY),
        DARK_GRAY(Formatting.DARK_GRAY),
        GOLD(Formatting.GOLD);

        private final Formatting formatting;

        TextColor(Formatting formatting) {
            this.formatting = formatting;
        }

        public Formatting getFormatting() {
            return formatting;
        }

        public int getRGB() {
            Integer color = formatting.getColorValue();
            return color != null ? color : 0xFFFFFF;
        }
    }

    public enum NotificationSound {
        EXPERIENCE_ORB("entity.experience_orb.pickup", "Experience Orb"),
        BELL("block.bell.use", "Bell"),
        LEVEL_UP("entity.player.levelup", "Level Up"),
        ANVIL("block.anvil.place", "Anvil"),
        NOTE_PLING("block.note_block.pling", "Note Pling"),
        NOTE_BELL("block.note_block.bell", "Note Bell"),
        NOTE_FLUTE("block.note_block.flute", "Note Flute"),
        NOTE_HARP("block.note_block.harp", "Note Harp"),
        FIREWORK("entity.firework_rocket.launch", "Firework"),
        ITEM_PICKUP("entity.item.pickup", "Item Pickup"),
        SKELETON("wynnextras:skeleton", "Skeleton"),
        AMOGUS("wynnextras:amogus", "Amogus");

        private final String soundId;
        private final String displayName;

        NotificationSound(String soundId, String displayName) {
            this.soundId = soundId;
            this.displayName = displayName;
        }

        public String getSoundId() {
            return soundId;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // ==================== RESET / DISABLE ALL ====================
    public void resetToDefaults() {
        WynnExtrasConfig defaults = new WynnExtrasConfig();
        for (java.lang.reflect.Field field : WynnExtrasConfig.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.set(this, field.get(defaults));
            } catch (IllegalAccessException ignored) {}
        }
    }

    public void disableAll() {
        for (java.lang.reflect.Field field : WynnExtrasConfig.class.getDeclaredFields()) {
            if (field.getType() == boolean.class) {
                try {
                    field.set(this, false);
                } catch (IllegalAccessException ignored) {}
            }
        }
    }

//    // ==================== CONFIG PROFILES ====================
//    /** Snapshots all current boolean field values into a profile with the given name. */
//    public void saveCurrentAsProfile(String name) {
//        if (name == null || name.isBlank()) return;
//        HashMap<String, Boolean> snap = new HashMap<>();
//        for (java.lang.reflect.Field field : WynnExtrasConfig.class.getDeclaredFields()) {
//            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
//            if (field.getType() != boolean.class) continue;
//            try { snap.put(field.getName(), field.getBoolean(this)); } catch (IllegalAccessException ignored) {}
//        }
//        configProfiles.put(name, snap);
//        activeProfile = name;
//    }
//
//    /** Applies the boolean snapshot stored under the given profile name. */
//    public void applyProfile(String name) {
//        HashMap<String, Boolean> snap = configProfiles.get(name);
//        if (snap == null) return;
//        for (Map.Entry<String, Boolean> entry : snap.entrySet()) {
//            try {
//                java.lang.reflect.Field field = WynnExtrasConfig.class.getDeclaredField(entry.getKey());
//                if (field.getType() == boolean.class) {
//                    field.setBoolean(this, entry.getValue());
//                }
//            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
//        }
//        activeProfile = name;
//    }
//
//    public void deleteProfile(String name) {
//        configProfiles.remove(name);
//        if (name != null && name.equals(activeProfile)) activeProfile = null;
//    }

    // ==================== SAVE/LOAD ====================
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                try {
                    INSTANCE = GSON.fromJson(json, WynnExtrasConfig.class);
                } catch (Exception e) {
                    // Corrupted config (invalid JSON): back it up and start fresh instead of crashing the game
                    WynnExtras.LOGGER.error("[WynnExtras] Config is corrupted, resetting to defaults: " + e.getMessage());
                    try {
                        Files.move(CONFIG_PATH, CONFIG_PATH.resolveSibling("wynnextras.json.corrupted"),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException moveEx) {
                        WynnExtras.LOGGER.error("[WynnExtras] Failed to back up corrupted config: " + moveEx.getMessage());
                    }
                    INSTANCE = new WynnExtrasConfig();
                }
                if (INSTANCE == null) {
                    INSTANCE = new WynnExtrasConfig();
                }
                // Ensure lists are not null
                if (INSTANCE.hiddenPlayers == null) INSTANCE.hiddenPlayers = new ArrayList<>();
                if (INSTANCE.notifierWords == null) INSTANCE.notifierWords = new ArrayList<>();
                if (INSTANCE.blockedWords == null) INSTANCE.blockedWords = new ArrayList<>();
                if (INSTANCE.raidLootTrackerHiddenLines == null) INSTANCE.raidLootTrackerHiddenLines = new ArrayList<>();
                if (INSTANCE.raidPBs == null) INSTANCE.raidPBs = new HashMap<>();
                if (INSTANCE.professionOverflowXp == null) INSTANCE.professionOverflowXp = new HashMap<>();
                if (INSTANCE.professionGoals == null) INSTANCE.professionGoals = new HashMap<>();
                if (INSTANCE.professionLevels == null) INSTANCE.professionLevels = new HashMap<>();
                if (INSTANCE.professionXpCurrent == null) INSTANCE.professionXpCurrent = new HashMap<>();
                if (INSTANCE.professionXpMax == null) INSTANCE.professionXpMax = new HashMap<>();
                if (INSTANCE.professionLevelGoals == null) INSTANCE.professionLevelGoals = new HashMap<>();
                if (INSTANCE.classCardAccentColors == null) INSTANCE.classCardAccentColors = new HashMap<>();
                if (INSTANCE.clientNicknames == null) INSTANCE.clientNicknames = new HashMap<>();
                if (INSTANCE.chatMediaPreviewMaxPixels == 4194304) INSTANCE.chatMediaPreviewMaxPixels = 16777216;
                INSTANCE.syncClassSelectionLines();
                if (INSTANCE.classSelectionContentProgressStyle == null) {
                    INSTANCE.classSelectionContentProgressStyle = ClassSelectionContentProgressStyle.LINE;
                }
                if (INSTANCE.classSelectionCompletionChromaMode == null) {
                    INSTANCE.classSelectionCompletionChromaMode = ClassSelectionCompletionChromaMode.NAME_AND_LINES;
                }
                if (INSTANCE.notifierAnimation == null) {
                    INSTANCE.notifierAnimation = NotifierAnimation.FADE;
                }
                if (INSTANCE.notifierExitAnimation == null) {
                    INSTANCE.notifierExitAnimation = NotifierExitAnimation.FADE;
                }
                if (INSTANCE.notifierEntranceDirection == null) {
                    INSTANCE.notifierEntranceDirection = NotifierAnimationDirection.BOTTOM;
                }
                if (INSTANCE.notifierExitDirection == null) {
                    INSTANCE.notifierExitDirection = NotifierAnimationDirection.BOTTOM;
                }
                //if (INSTANCE.configProfiles == null) INSTANCE.configProfiles = new LinkedHashMap<>();
                if (INSTANCE.weeklyWars == null) INSTANCE.weeklyWars = new ArrayList<>();
                if (INSTANCE.hudColorOverrides == null) INSTANCE.hudColorOverrides = new HashMap<>();
                INSTANCE.syncQuickRepairThreshold();
                INSTANCE.syncScaleBackgroundSettings();
                INSTANCE.syncAttackTimerColors();
                INSTANCE.syncTetrisSettings();
            }
        } catch (Exception e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load config, using defaults.", e);
            INSTANCE = new WynnExtrasConfig();
        }
        INSTANCE.syncQuickRepairThreshold();
        INSTANCE.syncScaleBackgroundSettings();
        INSTANCE.syncAttackTimerColors();
        INSTANCE.syncTetrisSettings();
        INSTANCE.syncClassSelectionLines();
        INSTANCE.syncShoppingListPositions();
        if (INSTANCE.mythicScaleSource == null) INSTANCE.mythicScaleSource = MythicScaleSource.WYNNPOOL;
        if (INSTANCE.classSelectionContentProgressStyle == null) {
            INSTANCE.classSelectionContentProgressStyle = ClassSelectionContentProgressStyle.LINE;
        }
        if (INSTANCE.classSelectionCompletionChromaMode == null) {
            INSTANCE.classSelectionCompletionChromaMode = ClassSelectionCompletionChromaMode.NAME_AND_LINES;
        }
        if (INSTANCE.notifierAnimation == null) {
            INSTANCE.notifierAnimation = NotifierAnimation.FADE;
        }
        if (INSTANCE.notifierExitAnimation == null) {
            INSTANCE.notifierExitAnimation = NotifierExitAnimation.FADE;
        }
        if (INSTANCE.notifierEntranceDirection == null) {
            INSTANCE.notifierEntranceDirection = NotifierAnimationDirection.BOTTOM;
        }
        if (INSTANCE.notifierExitDirection == null) {
            INSTANCE.notifierExitDirection = NotifierAnimationDirection.BOTTOM;
        }
    }

    private void syncAttackTimerColors() {
        WynnExtrasConfig defaults = new WynnExtrasConfig();
        if (attackTimerNormalColor == null) attackTimerNormalColor = defaults.attackTimerNormalColor;
        if (attackTimerCurrentTerritoryColor == null) attackTimerCurrentTerritoryColor = defaults.attackTimerCurrentTerritoryColor;
        if (attackTimerVeryLowDefenseColor == null) attackTimerVeryLowDefenseColor = defaults.attackTimerVeryLowDefenseColor;
        if (attackTimerLowDefenseColor == null) attackTimerLowDefenseColor = defaults.attackTimerLowDefenseColor;
        if (attackTimerMediumDefenseColor == null) attackTimerMediumDefenseColor = defaults.attackTimerMediumDefenseColor;
        if (attackTimerHighDefenseColor == null) attackTimerHighDefenseColor = defaults.attackTimerHighDefenseColor;
        if (attackTimerVeryHighDefenseColor == null) attackTimerVeryHighDefenseColor = defaults.attackTimerVeryHighDefenseColor;
    }

    private void syncQuickRepairThreshold() {
        quickRepairDurabilityThreshold = Math.clamp(quickRepairDurabilityThreshold, 0, 100);
    }

    private void syncScaleBackgroundSettings() {
        if (scaleBackgroundShape == null) scaleBackgroundShape = ScaleBackgroundShape.BOX;
        scaleBackgroundOpacity = Math.clamp(scaleBackgroundOpacity, 0, 100);
    }

    private void syncTetrisSettings() {
        tetrisDAS = Math.clamp(tetrisDAS, 0, 300);
        tetrisARR = Math.clamp(tetrisARR, 0, 100);
        tetrisSDFDelay = Math.clamp(tetrisSDFDelay, 0, 300);
        tetrisSDF = Math.clamp(tetrisSDF, 0, 100);
        tetris20GLevel = Math.clamp(tetris20GLevel, 1, 100);
    }

    private void syncShoppingListPositions() {
        if (shoppingListMenuWidth <= 0) shoppingListMenuWidth = 190;
        if (shoppingListMenuHeight <= 0) shoppingListMenuHeight = 222;
        if (shoppingListMenuDefaultPosition == null) shoppingListMenuDefaultPosition = new ShoppingListPosition();
        if (shoppingListMenuTradePosition == null) shoppingListMenuTradePosition = new ShoppingListPosition();
        if (shoppingListMenuBankOverlayPosition == null) shoppingListMenuBankOverlayPosition = new ShoppingListPosition();
        if (shoppingListMenuBankVanillaPosition == null) shoppingListMenuBankVanillaPosition = new ShoppingListPosition();
        if (shoppingListLauncherButtonDefaultPosition == null) shoppingListLauncherButtonDefaultPosition = new ShoppingListPosition();
        if (shoppingListLauncherButtonTradePosition == null) shoppingListLauncherButtonTradePosition = new ShoppingListPosition();
        if (shoppingListLauncherButtonBankOverlayPosition == null) shoppingListLauncherButtonBankOverlayPosition = new ShoppingListPosition();
        if (shoppingListLauncherButtonBankVanillaPosition == null) shoppingListLauncherButtonBankVanillaPosition = new ShoppingListPosition();
    }

    public static class ShoppingListPosition {
        public int x = -1;
        public int y = -1;

        public boolean isSet() {
            return x >= 0 && y >= 0;
        }

        public void set(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void reset() {
            x = -1;
            y = -1;
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save config: " + e.getMessage());
        }
    }

    // ==================== CONFIG SCREEN ====================
    public static Screen createConfigScreen(Screen parent) {
        return new WynnExtrasConfigScreen(parent);
    }

    private static Map<String, String> createClassSelectionLineNames() {
        LinkedHashMap<String, String> names = new LinkedHashMap<>();
        names.put(CLASS_SELECTION_LINE_LEVEL, "Level");
        names.put(CLASS_SELECTION_LINE_LOCATION, "Location");
        names.put(CLASS_SELECTION_LINE_PLAYTIME, "Playtime");
        names.put(CLASS_SELECTION_LINE_CONTENT_PROGRESS, "Content Progress");
        names.put(CLASS_SELECTION_LINE_LAST_HELD_WEAPON, "Last Held Weapon");
        return Collections.unmodifiableMap(names);
    }

    public void syncClassSelectionLines() {
        if (classSelectionActiveLines == null) classSelectionActiveLines = new ArrayList<>();
        if (classSelectionAvailableLines == null) classSelectionAvailableLines = new ArrayList<>();

        classSelectionActiveLines = sanitizeClassSelectionLineList(classSelectionActiveLines, new HashSet<>());
        Set<String> activeIds = new HashSet<>(classSelectionActiveLines);
        classSelectionAvailableLines = sanitizeClassSelectionLineList(classSelectionAvailableLines, activeIds);

        boolean showContentProgressLine = classSelectionContentProgressStyle == ClassSelectionContentProgressStyle.LINE;
        Set<String> configuredIds = new HashSet<>(classSelectionActiveLines);
        configuredIds.addAll(classSelectionAvailableLines);
        for (String id : CLASS_SELECTION_LINE_IDS) {
            if (!configuredIds.contains(id)) {
                classSelectionAvailableLines.add(id);
                configuredIds.add(id);
            }
        }
        if (showContentProgressLine && !configuredIds.contains(CLASS_SELECTION_LINE_CONTENT_PROGRESS)) {
            classSelectionAvailableLines.add(CLASS_SELECTION_LINE_CONTENT_PROGRESS);
        }
    }

    private List<String> sanitizeClassSelectionLineList(List<String> lines, Set<String> excludedIds) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>(excludedIds);
        for (String id : lines) {
            id = migrateClassSelectionLineId(id);
            if (!CLASS_SELECTION_LINE_IDS.contains(id) || seen.contains(id)) continue;
            result.add(id);
            seen.add(id);
        }
        return result;
    }

    private String migrateClassSelectionLineId(String id) {
        if ("detail_1".equals(id)) return CLASS_SELECTION_LINE_LEVEL;
        if ("detail_2".equals(id)) return CLASS_SELECTION_LINE_LOCATION;
        if ("detail_3".equals(id)) return CLASS_SELECTION_LINE_PLAYTIME;
        return id;
    }
}
