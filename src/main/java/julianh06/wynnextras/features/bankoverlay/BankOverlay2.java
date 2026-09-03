package julianh06.wynnextras.features.bankoverlay;

import julianh06.wynnextras.utils.WynnStringUtils;
import julianh06.wynnextras.utils.text.StyledText;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.wynncraft.item.ItemTier;
import julianh06.wynnextras.wynncraft.item.WynnItemData;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import julianh06.wynnextras.wynncraft.state.PartyState;
import julianh06.wynnextras.utils.TooltipUtils;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.TextShadow;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import julianh06.wynnextras.utils.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.data.BankData;
import julianh06.wynnextras.features.inventory.data.BookshelfData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.features.inventory.data.CrossClassBankSearch;
import julianh06.wynnextras.features.inventory.data.MiscBucketData;
import julianh06.wynnextras.mixin.Accessor.*;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.LunarCompat;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.SearchQueryParser;
import julianh06.wynnextras.utils.SlotAccess;
import julianh06.wynnextras.utils.ItemHighlightRenderer;
import julianh06.wynnextras.compat.wynntils.WynntilsItemUiAdapter;
import julianh06.wynnextras.compat.wynntils.WynntilsBankAdapter;
import julianh06.wynnextras.utils.WynnModItemOverlayBridge;
import julianh06.wynnextras.utils.UI.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static julianh06.wynnextras.utils.ContainerUtils.clickOnSlot;
import static julianh06.wynnextras.utils.ContainerUtils.shiftClickOnSlot;
import static julianh06.wynnextras.features.inventory.BankOverlay.*;

public class BankOverlay2 extends WEHandledScreen {
    private static boolean readOnlyViewerActive = false;
    private static final boolean MOUSE_TWEAKS_LOADED = FabricLoader.getInstance().isModLoaded("mousetweaks");
    private static final long BANK_PAGE_PROGRESS_INTERVAL_MS = 275L;
    private static final long BANK_PAGE_RESPONSE_SETTLE_MS = 100L;
    private static final long BANK_PAGE_REJECTED_RESPONSE_SETTLE_MS = 50L;
    private static final long BANK_PAGE_RESPONSE_TIMEOUT_MS = 1_500L;
    private static final Pattern MINECRAFT_FORMATTING_CODE_PATTERN = Pattern.compile("\u00a7[0-9a-fk-or]");
    private static final Pattern PAGE_RANK_REQUIREMENT_PATTERN = Pattern.compile("(?i)(?:only available to|requires(?: a)? rank:?|rank required:?)\\s+([A-Za-z0-9+]+)");
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("(?i)\\bPage\\s+(\\d+)\\b");
    private static ItemStack hoveredSlot = Items.AIR.getDefaultStack();
    private static int hoveredX = -1;
    private static int hoveredY = -1;
    private static int hoveredIndex = -1;
    private static int hoveredInvIndex = -1;
    private static TooltipRenderData hoveredTooltipData = null;

    static WynntilsBankAdapter.FeatureHandle itemHighlightFeature;
    static WynntilsBankAdapter.FeatureHandle itemTextOverlayFeature;
    static WynntilsBankAdapter.FeatureHandle unidentifiedItemIconFeature;
    static WynntilsBankAdapter.FeatureHandle itemFavoriteFeature;
    static WynntilsBankAdapter.FeatureHandle durabilityOverlayFeature;
    static WynntilsBankAdapter.FeatureHandle emeraldPouchFillArcFeature;
    private static WynntilsBankAdapter.FeatureHandle itemGuessFeature;
    private static WynntilsBankAdapter.FeatureHandle emeraldCountFeature;
    private static int cachedEmeraldAmount = Integer.MIN_VALUE;
    private static String[] cachedEmeraldAmounts = new String[0];
    private static boolean itemHighlightEnabled = false;
    private static boolean itemTextOverlayEnabled = false;
    private static boolean itemTextRenderInInv = false;
    private static boolean unidentifiedItemIconEnabled = false;
    private static boolean itemFavoriteEnabled = false;
    private static boolean durabilityOverlayEnabled = false;
    private static boolean emeraldPouchFillArcEnabled = false;
    private static final Identifier buttonBackground = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg.png");
    private static final Identifier buttonBackgroundShort = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort.png");
    private static final Identifier buttonBackgroundDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg_dark.png");
    private static final Identifier buttonBackgroundShortDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort_dark.png");
    private static final Identifier buttonBackgroundSingle = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttons_charactersearch.png");
    private static final Identifier buttonBackgroundSingleDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttons_charactersearch_dark.png");

    private record TooltipRenderData(List<Text> tooltip, List<TooltipComponent> components, int height) {}

    private static ItemStack cachedTooltipStack = null;
    private static int cachedTooltipCount = -1;
    private static int cachedTooltipComponentsHash = 0;
    private static int cachedTooltipModifierState = 0;
    private static TooltipRenderData cachedTooltipRenderData = null;

    static Identifier signLeft = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_left.png");
    static Identifier signLeftDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_left_dark.png");
    static Identifier signRight = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_right.png");
    static Identifier signRightDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_right_dark.png");
    static Identifier signMid1 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m1.png");
    static Identifier signMid1D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m1_dark.png");
    static Identifier signMid2 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m2.png");
    static Identifier signMid2D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m2_dark.png");
    static Identifier signMid3 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m3.png");
    static Identifier signMid3D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m3_dark.png");
    static Identifier lock_locked = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_locked.png");
    static Identifier lock_unlocked = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_unlocked.png");
    static Identifier lock_locked_dark = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_locked_dark.png");
    static Identifier lock_unlocked_dark = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_unlocked_dark.png");

    static List<Identifier> signMids = new ArrayList<>();
    private static boolean signMidsDarkMode = false; // Track which mode signMids was built for

    static String priceText;

    static String confirmText = "";

    private final EnumSet<BankOverlayType> initializedTypes = EnumSet.noneOf(BankOverlayType.class);

    private CallbackInfo ci;
    private HandledScreen<?> screen;
    private Function<Void, Void> close;
    private final boolean readOnlyViewer;
    private final SimpleInventory tooltipInventory = new SimpleInventory(1);
    private final Slot tooltipSlot = new Slot(tooltipInventory, 0, 0, 0);
    private final List<ItemStack> liveBankPageItems = new ArrayList<>(45);
    private final List<ItemStack> livePlayerInventoryItems = new ArrayList<>(36);
    private boolean initialBrowseScrollPending = false;

    private static float targetOffset = 0;
    static float actualOffset = 0;
    private static boolean preserveScrollOnNextOverlay = false;
    private static boolean restoreScrollAfterLayout = false;
    private static boolean deferPreservedScrollClamp = false;
    private static float preservedTargetOffset = 0;
    private static float preservedActualOffset = 0;
    private static boolean bankTypeSwitchInProgress = false;
    private static boolean bankTypeSwitchTargetApplied = false;
    private static BankOverlayType bankTypeSwitchTargetType = BankOverlayType.NONE;
    private static int bankTypeSwitchTargetPage = -1;
    private static int pageJumpTarget = -1;
    private static long pageJumpLastActionAt = 0L;
    private static long pageJumpLastProgressActionAt = 0L;
    private static long pageJumpLastResponseAt = 0L;
    private static int pageJumpLastActionPage = -1;
    private static TextRenderer frameTextRenderer;
    private static int bankSyncid = 0;
    private static int xFitAmount = 0;
    private static int yFitAmount = 0;
    private static float pageBuyCustomModelData = 0;
    private static BankOverlayType rankLockedOverlayType = BankOverlayType.NONE;
    private static int rankLockedPageCount = -1;
    private static String rankLockedRequiredRank = "";

    private static final List<PageWidget> pages = new ArrayList<>();
    private static final Map<Integer, List<ItemStack>> annotationStackCache = new HashMap<>();
    private static final Map<Integer, List<Object>> annotationComponentCache = new HashMap<>();
    private static int annotationCalculationsThisFrame = 0;
    private static InventoryWidget inventoryWidget = null;
    private static SwitchButtonWidget switchButtonWidget = null;
    private static QuickActionWidget quickActionWidget = null;
    private static TextInputWidget searchbar2 = null;
    private static ToggleOverlayWidget toggleOverlayWidget = null;
    private static ReadOnlyNoticeWidget readOnlyNoticeWidget = null;
    static ScrollBarWidget scrollBarWidget = null;

    // Cross-class search
    private static List<CrossClassPageWidget> crossClassPages = new ArrayList<>();
    private static final List<CrossClassPageWidget> renderedCrossClassPages = new ArrayList<>();
    private static String lastCrossClassSearchQuery = "";
    private static boolean crossClassSearchActive = false;
    private static volatile String activeCrossClassSearchKey = "";
    private static volatile int crossClassSearchGeneration = 0;
    private static volatile CrossClassSearchPayload completedCrossClassSearch = null;
    private static CompletableFuture<List<CrossClassBankSearch.SearchResult>> pendingCrossClassSearchTask = null;
    private static CompletableFuture<CrossClassSearchPayload> pendingCrossClassSearch = null;
    private static boolean crossClassSearchLoading = false;
    private static boolean crossClassSearchQueued = false;
    private static LocalCrossClassPageCache accountLocalCrossClassPageCache = null;
    private static LocalCrossClassPageCache currentCharacterLocalCrossClassPageCache = null;
    private static String queuedCrossClassSearchKey = "";
    private static String queuedCrossClassSearchInput = "";
    private static boolean queuedCrossClassIncludeCurrentCharacter = false;
    private static boolean queuedCrossClassIncludeAccountBank = false;
    private static boolean queuedCrossClassAllPages = false;
    private static long queuedCrossClassSearchAt = 0L;
    private static final long CROSS_CLASS_SEARCH_DEBOUNCE_MS = 175L;
    private static String activeSearchInput = "";
    private static SearchQueryParser.ParsedQuery activeSearchQuery = SearchQueryParser.parse("");
    private static HandledScreen<?> bridgeScreen = null;
    private static Slot hoveredBackingSlot = null;
    // Character ID to highlight in /class menu (set when clicking cross-class page)
    private static String targetCharacterIdForClassMenu = null;
    private static String targetCharacterNameForClassMenu = null;
    private static int targetCharacterLevelForClassMenu = 0;

    // All characters browse mode
    private static boolean allCharactersBrowseMode = false;
    private static AllCharactersButtonWidget allCharactersButtonWidget = null;
    private static boolean currentClassAccountBankUnavailable = false;
    private static boolean currentClassAccountBankAvailabilityDetected = false;
    private static long suppressAccountBankAvailabilityDetectionUntil = 0L;

    // Saved search from cross-class swap (persists across bank close/reopen)
    private static String savedCrossClassSearch = null;
    private static long savedCrossClassSearchTime = 0;
    private static final long SAVED_SEARCH_EXPIRY_MS = 2 * 60 * 1000; // 2 minutes

    // Reload bank
    private static boolean isReloading = false;
    private static int reloadCurrentPage = 0;
    private static int reloadTotalPages = 0;
    private static int reloadOriginalPage = -1;
    private static boolean reloadPageLoaded = false;
    private static Float reloadNextPageCustomModelData = null;
    private static int reloadStableTicks = 0;
    private static int reloadLastSlotFingerprint = 0;
    private static int reloadSavedPage = -1;
    private static long reloadLastContainerUpdateAt = 0L;
    private static final long RELOAD_PACKET_SETTLE_MS = 50L;
    private static final int RELOAD_STABLE_DELAY = 2;
    private static ReloadBankWidget reloadBankWidget = null;

    static int shownPages;

    private static boolean isMouseInOverlay = false;
    private static PendingRightClick pendingMouseTweaksRightClick = null;

    private static int scissorx1, scissory1, scissorx2, scissory2;

    private record PendingRightClick(SlotWidget slotWidget, Slot backingSlot, double startX, double startY) {}

    private static long lastClickTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL_MS = 500L;
    private static long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 50L;
    private static long blockSlotClicksAfterScrollUntil = 0L;
    private static final long SCROLL_SLOT_CLICK_BLOCK_MS = 150L;

    private static Pair<Integer, Integer> lastClickedSlot = new Pair<>(-1, -1);
    private static ItemStack lastQuickMoved = ItemStack.EMPTY;
    private static boolean dragSplitting = false;
    private static int dragSplittingButton = -1;
    private static final Set<Integer> dragSplittingSlots = new LinkedHashSet<>();
    private static SlotWidget dragSplittingFallbackSlot = null;

    private static final List<ItemStack> EMPTY_BANK_PAGE = Collections.nCopies(45, Items.AIR.getDefaultStack());
    private static final List<ItemStack> EMPTY_PLAYER_INVENTORY = Collections.nCopies(36, Items.AIR.getDefaultStack());
    private static final List<ItemStack> EMPTY_PLAYER_ARMOR = Collections.nCopies(4, Items.AIR.getDefaultStack());
    private static final int ACCOUNT_BANK_MAX_PAGES = 21;
    private static final int CROSS_CLASS_GROUP_GAP = 18;
    private static int layoutExtraScrollHeight = 0;
    private static final EquipmentSlot[] ARMOR_DISPLAY_ORDER = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static boolean clickedClassSelectionEntity = false;
    private static final CustomColor WHITE_TEXT_COLOR = CustomColor.fromHexString("FFFFFF");
    private static final CustomColor YELLOW_TEXT_COLOR = CustomColor.fromHexString("FFFF00");
    private static final CustomColor GOLD_TEXT_COLOR = CustomColor.fromHexString("DEC800");
    private static final CustomColor GRAY_TEXT_COLOR = CustomColor.fromHexString("BBBBBB");
    private static final CustomColor DARK_BACKGROUND_COLOR = CustomColor.fromHexString("2c2d2f");
    private static final CustomColor DARK_BORDER_COLOR = CustomColor.fromHexString("1b1b1c");
    private static final CustomColor LIGHT_BACKGROUND_COLOR = CustomColor.fromHexString("81644b");
    private static final CustomColor LIGHT_BORDER_COLOR = CustomColor.fromHexString("4f342c");
    private static final CustomColor BACKDROP_COLOR = CustomColor.fromInt(-804253680);
    private static final CustomColor PAGE_DIM_COLOR = CustomColor.fromHSV(0, 0, 0, 0.25f);
    private static final CustomColor WAIT_OVERLAY_COLOR = CustomColor.fromHexString("000000").withAlpha(0.75f);
    private static final CustomColor SLOT_HOVER_COLOR = CustomColor.fromHSV(0, 0, 1000, 0.25f);
    private static final CustomColor SEARCH_MATCH_COLOR = CustomColor.fromHexString("00FF00");
    private static final CustomColor SEARCH_DIM_COLOR = CustomColor.fromHSV(0, 0, 0, 0.75f);
    private static final CustomColor DIM_COUNT_COLOR = CustomColor.fromInt(0xFF808080);
    private int layoutXRemain = 0;
    private int layoutYRemain = 0;

    private record CrossClassSearchPayload(
            String cacheKey,
            int generation,
            List<CrossClassBankSearch.SearchResult> results,
            Throwable error
    ) {}

    private record LocalCrossClassPageCache(
            BankData data,
            String characterId,
            String displayName,
            int characterLevel,
            int maxPages,
            int pageCount,
            int bankPagesSize,
            int lastPage,
            String searchInput,
            int yStart,
            int bottomBorder,
            int yFitAmount,
            float scale,
            List<CrossClassPageWidget> pages
    ) {
        private boolean matches(BankData data, String characterId, String displayName, int characterLevel, int maxPages, int pageCount, String searchInput, int yStart, int bottomBorder, float scale) {
            return this.data == data
                    && Objects.equals(this.characterId, characterId)
                    && Objects.equals(this.displayName, displayName)
                    && this.characterLevel == characterLevel
                    && this.maxPages == maxPages
                    && this.pageCount == pageCount
                    && this.bankPagesSize == data.getBankPages().size()
                    && this.lastPage == data.getLastPage()
                    && Objects.equals(this.searchInput, searchInput)
                    && this.yStart == yStart
                    && this.bottomBorder == bottomBorder
                    && this.yFitAmount == BankOverlay2.yFitAmount
                    && Float.compare(this.scale, scale) == 0;
        }
    }

    public BankOverlay2(CallbackInfo ci, HandledScreen<?> screen) {
        this(ci, screen, false);
    }

    private BankOverlay2(CallbackInfo ci, HandledScreen<?> screen, boolean readOnlyViewer) {
        this.ci = ci;
        this.screen = screen;
        this.readOnlyViewer = readOnlyViewer;
        if (preserveScrollOnNextOverlay) {
            actualOffset = preservedActualOffset;
            targetOffset = preservedTargetOffset;
            preserveScrollOnNextOverlay = false;
            restoreScrollAfterLayout = true;
            initialBrowseScrollPending = false;
        } else {
            actualOffset = 0;
            targetOffset = 0;
            restoreScrollAfterLayout = false;
            deferPreservedScrollClamp = false;
            initialBrowseScrollPending = true;
        }
        pages.clear();
        clearCrossClassSearchState();
        currentClassAccountBankUnavailable = false;
        currentClassAccountBankAvailabilityDetected = false;
        allCharactersBrowseMode = WynnExtrasConfig.INSTANCE.bankAllCharactersBrowseMode;
        allCharactersButtonWidget = null;
        isReloading = false;
        resetReloadPageReadiness();
        reloadNextPageCustomModelData = null;
        reloadLastContainerUpdateAt = 0L;
        reloadBankWidget = null;
        pendingMouseTweaksRightClick = null;
        resetDragSplitting();
        signMids.clear();
        inventoryWidget = null;
        switchButtonWidget = null;
        quickActionWidget = null;
        searchbar2 = null;
        readOnlyNoticeWidget = null;
        priceText = null;
        activeInv = 0;
        shownPages = 0;
        scissorx1 = 0;
        scissory1 = 0;
        scissorx2 = 0;
        scissory2 = 0;

        refreshDurabilityCfg();
        refreshEmeraldPouchCfg();
        refreshHighlightCfg();
        refreshItemTextOverlayCfg();

    }

    public static BankOverlay2 createReadOnlyViewer() {
        readOnlyViewerActive = true;
        BankOverlay2 viewer = new BankOverlay2(null, null, true);
        setReadOnlyViewerType(BankOverlayType.ACCOUNT);
        return viewer;
    }

    public static boolean isReadOnlyViewerActive() {
        return readOnlyViewerActive;
    }

    public void closeReadOnlyViewer() {
        if (!readOnlyViewer) return;
        readOnlyViewerActive = false;
        clearCrossClassSearchState();
        pages.clear();
        rootWidgets.clear();
        inventoryWidget = null;
        switchButtonWidget = null;
        quickActionWidget = null;
        searchbar2 = null;
        readOnlyNoticeWidget = null;
        allCharactersButtonWidget = null;
        reloadBankWidget = null;
        scrollBarWidget = null;
        currentOverlayType = BankOverlayType.NONE;
        currentData = null;
        Pages = null;
        activeInv = -1;
        clearHoverState(null);
    }

    private static void setReadOnlyViewerType(BankOverlayType type) {
        if (!readOnlyViewerActive || type == null || type == BankOverlayType.NONE) return;
        currentOverlayType = type;
        currentData = switch (type) {
            case ACCOUNT -> AccountBankData.INSTANCE;
            case CHARACTER -> CharacterBankData.INSTANCE;
            case BOOKSHELF -> BookshelfData.INSTANCE;
            case MISC -> MiscBucketData.INSTANCE;
            case NONE -> null;
        };
        BankOverlay.setCurrentMaxPages(type == BankOverlayType.ACCOUNT ? 21 : 12);
        Pages = currentData;
        activeInv = 0;
        pages.clear();
        clearCrossClassSearchState();
        annotationCache.clear();
        annotationStackCache.clear();
        annotationComponentCache.clear();
        targetOffset = 0;
        actualOffset = 0;
        currentClassAccountBankUnavailable = false;
        currentClassAccountBankAvailabilityDetected = true;
    }

    private static BankOverlayType getNextReadOnlyViewerType() {
        return switch (currentOverlayType) {
            case ACCOUNT -> BankOverlayType.CHARACTER;
            case CHARACTER -> BankOverlayType.BOOKSHELF;
            case BOOKSHELF -> BankOverlayType.MISC;
            case MISC, NONE -> BankOverlayType.ACCOUNT;
        };
    }

    private static String getReadOnlyViewerTypeName(BankOverlayType type) {
        return switch (type) {
            case ACCOUNT -> "Account Bank";
            case CHARACTER -> "Character Bank";
            case BOOKSHELF -> "Bookshelf";
            case MISC -> "Misc Bucket";
            case NONE -> "Bank";
        };
    }

    public void updateRenderContext(CallbackInfo ci, HandledScreen<?> screen, Function<Void, Void> close) {
        this.ci = ci;
        this.screen = screen;
        this.close = close;
    }

    public Slot getTouchHoveredSlot() {
        return touchHoveredSlot;
    }

    public static void handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchbar2 != null) {
            searchbar2.keyPressed(keyCode, scanCode, modifiers);
        }
        for (PageWidget page : pages) {
            page.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public static void handleCharTyped(char character) {
        if (searchbar2 != null) {
            searchbar2.charTyped(character, 0);
        }
        for (PageWidget page : pages) {
            page.charTyped(character, 0);
        }
    }

    public static boolean isAnyTextInputFocused() {
        if (searchbar2 != null && searchbar2.isFocused()) return true;
        for (PageWidget page : pages) {
            if (page.isNameInputFocused()) return true;
        }
        return false;
    }

    public static void adjustTargetOffset(float offset) {
        targetOffset += offset;
    }

    public static boolean handleMouseScrolled(double verticalAmount) {
        if (BankOverlay.currentOverlayType == BankOverlayType.NONE) return false;
        if (!readOnlyViewerActive && !WynnExtrasConfig.INSTANCE.toggleBankOverlay) return false;

        long now = System.currentTimeMillis();
        if (now - lastScrollTime < SCROLL_COOLDOWN_MS) {
            return true;
        }
        lastScrollTime = now;
        blockSlotClicksAfterScrollUntil = now + SCROLL_SLOT_CLICK_BLOCK_MS;

        if (verticalAmount > 0) {
            adjustTargetOffset(-104f);
        } else {
            adjustTargetOffset(104f);
        }
        return true;
    }

    public static boolean shouldBlockSlotClickAfterScroll(int syncId) {
        if (BankOverlay.currentOverlayType == BankOverlayType.NONE) return false;
        if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) return false;
        if (syncId != bankSyncid) return false;
        return System.currentTimeMillis() < blockSlotClicksAfterScrollUntil;
    }

    public static void setBankSyncId(int syncId) {
        bankSyncid = syncId;
    }

    public static String getTargetCharacterNameForClassMenu() {
        return targetCharacterNameForClassMenu;
    }

    public static int getTargetCharacterLevelForClassMenu() {
        return targetCharacterLevelForClassMenu;
    }

    public static void setTargetCharacterForClassMenu(String characterId, String characterName, int characterLevel) {
        targetCharacterIdForClassMenu = characterId;
        targetCharacterNameForClassMenu = characterName;
        targetCharacterLevelForClassMenu = characterLevel;
    }

    public static void clearTargetCharacterForClassMenu() {
        setTargetCharacterForClassMenu(null, null, 0);
    }

    public static boolean shouldShowWynntilsPageJumpButtons() {
        return WynnExtrasConfig.INSTANCE.toggleBankOverlay
                && WynnExtrasConfig.INSTANCE.showWynntilsBankPageJumpButtons
                && WynnExtrasConfig.INSTANCE.bankOverlayMaxRows == 1
                && WynnExtrasConfig.INSTANCE.bankOverlayMaxColumns == 1
                && currentOverlayType != BankOverlayType.NONE;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!readOnlyViewer && currentOverlayType == BankOverlayType.CHARACTER) {
            BankOverlay.syncCurrentCharacterId();
        }
        Pages = currentData;
        applyPendingBankTypeSwitchTargetIfReady();
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.getWindow() == null || !mc.isRunning()) return;
        if(mc.player == null || mc.currentScreen == null) return;
        if (!readOnlyViewer) {
            cacheCurrentBankPageIfPossible();
            saveCurrentPlayerInventorySnapshot();
        }
        frameTextRenderer = mc.textRenderer;
        clearHoverState(screen);
        annotationCalculationsThisFrame = 0;
        refreshFrameFeatureStates();

        if(ui == null) {
            ui = new UIUtils(context, 1, 0, 0);
        }

        if(!readOnlyViewer && bankSyncid == 0) {
            bankSyncid = MinecraftUtils.containerMenu().syncId;
        }

        calculateLayout();
        int xRemain = layoutXRemain;
        int yRemain = layoutYRemain;

        int xStart = xRemain / 2 - 2;
        int yStart = yRemain / 2 - 2;
        int buttonWidgetsX = getButtonWidgetsX(xStart);

        if (readOnlyViewer) {
            context.fillGradient(
                    0, 0, mc.currentScreen.width, mc.currentScreen.height,
                    0xC0101010,
                    0xD0101010
            );
        }

        if (WynncraftMenuService.isCurrentAny(MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK,
                MenuType.BOOKSHELF, MenuType.MISC_BUCKET)
        ) {
            if (toggleOverlayWidget == null) {
                toggleOverlayWidget = new ToggleOverlayWidget();
            }


            float xPos = mc.currentScreen.width / 2f;
            float yPos = yStart + (yFitAmount) * (90 + 4 + 10) - 20;

            if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                Screen screen = MinecraftUtils.screen();
                if (!(screen instanceof HandledScreen<?> containerScreen)) return;
                yPos = HandledScreenAccess.y(containerScreen) + (4 + MinecraftUtils.containerMenu().slots.size() / 9f) * 16;
            } else {
                context.fillGradient(
                        0, 0, mc.currentScreen.width, mc.currentScreen.height,
                        0xC0101010,
                        0xD0101010
                );
            }

            if(WynnExtrasConfig.INSTANCE.bankQuickToggle) {
                toggleOverlayWidget.setBounds((int) xPos - 70, (int) yPos, 140, 17);
                toggleOverlayWidget.draw(context, mouseX, mouseY, delta, ui);
            } else {
                toggleOverlayWidget.setBounds(0, 0, 0, 0);
            }
        }

        if(currentOverlayType == BankOverlayType.NONE || MinecraftClient.getInstance() == null) {
            BankOverlaySlotBridge.restoreAll();
            return;
        }

        initializeOverlayState();
        if (!readOnlyViewer) {
            syncActivePageFromWynntilsQuickJump();
            continuePageJump();
        }

        float snapValue = 0.5f;

        int maxOffset = getMaxScrollOffset(shownPages);

        if (!restoreScrollAfterLayout && targetOffset > maxOffset) {
            targetOffset = maxOffset;
            snapValue = 0.75f;
        }
        if (!restoreScrollAfterLayout && targetOffset <= 0) {
            targetOffset = 0;
            snapValue = 0.75f;
        }

        float speed = 0.3f;
        float diff = (targetOffset - actualOffset);
        if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
        else actualOffset += diff * speed * delta;

        if(!readOnlyViewer && !WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
            BankOverlaySlotBridge.restoreAll();
            return;
        }
        if(Pages == null) {
            BankOverlaySlotBridge.restoreAll();
            return;
        }

        bridgeScreen = screen;
        clearHoverState(screen);
        BankOverlaySlotBridge.beginFrame(screen);

        // Reload bank state machine
        if (isReloading) {
            if (!shouldWait && reloadPageLoaded) {
                if (!canReloadNextPage()) {
                    stopReloadAndReturnToOriginalPage();
                } else {
                    reloadCurrentPage++;
                    resetReloadPageReadiness();
                    jumpToBankPage(reloadCurrentPage);
                    retryLoad();
                }
            }
            if (!shouldWait && !reloadPageLoaded && isReloadCurrentPageReady()
                    && System.currentTimeMillis() - reloadLastContainerUpdateAt >= RELOAD_PACKET_SETTLE_MS) {
                int slotFingerprint = getActiveBankSlotFingerprint();
                if (slotFingerprint == reloadLastSlotFingerprint) {
                    reloadStableTicks++;
                } else {
                    reloadLastSlotFingerprint = slotFingerprint;
                    reloadStableTicks = 1;
                }

                if (reloadStableTicks >= RELOAD_STABLE_DELAY) {
                    saveReloadCurrentPage();
                    reloadPageLoaded = true;
                }
            } else if (!reloadPageLoaded && activeInv == reloadCurrentPage) {
                reloadStableTicks = 0;
                reloadLastSlotFingerprint = 0;
            }
        }

        if(pages.isEmpty()) {
            for (int i = 0; i < BankOverlay.getCurrentMaxPages(); i++) {
                PageWidget pageWidget = new PageWidget(i, yStart, (int) (yStart + (yFitAmount) * (90 + 4 + 10) * Math.max(2, ui.getScaleFactor())));
                pages.add(pageWidget);
            }
        }

        if(inventoryWidget == null) {
            inventoryWidget = new InventoryWidget();
        }

        if(switchButtonWidget == null) {
            switchButtonWidget = new SwitchButtonWidget();
        }

        if(searchbar2 == null) {
            searchbar2 = new TextInputWidget(0, 0, 0, 0, 7, 7, 1.25f);
            searchbar2.setPlaceholder("Search...");
            searchbar2.setPlaceholderColor(WHITE_TEXT_COLOR);
            searchbar2.setTextColor(WHITE_TEXT_COLOR);
            searchbar2.setCursorColor(WHITE_TEXT_COLOR);
            searchbar2.setSelectionColor(CustomColor.fromInt(0xAA3366CC));
            searchbar2.setOnChange(value -> {
                for (PageWidget page : pages) {
                    page.setEnabled(true);
                    page.invalidateSearchCache();
                }
                if (allCharactersBrowseMode
                        && currentOverlayType == BankOverlayType.CHARACTER
                        && (value == null || value.isEmpty())) {
                    initialBrowseScrollPending = true;
                }
            });
            rootWidgets.add(searchbar2);

            // Restore saved search from cross-class swap if still valid
            if (savedCrossClassSearch != null && !savedCrossClassSearch.isEmpty()) {
                long elapsed = System.currentTimeMillis() - savedCrossClassSearchTime;
                if (elapsed < SAVED_SEARCH_EXPIRY_MS) {
                    searchbar2.setInput(savedCrossClassSearch);
                }
                savedCrossClassSearch = null;
            }
        }

        if(quickActionWidget == null) {
            quickActionWidget = new QuickActionWidget();
        }

        if(allCharactersButtonWidget == null) {
            allCharactersButtonWidget = new AllCharactersButtonWidget();
        }

        if(reloadBankWidget == null) {
            reloadBankWidget = new ReloadBankWidget();
        }

        if(scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget();
        }

        if (ci != null) ci.cancel();

        if (!readOnlyViewer && !WynnExtras.hasTestInventory()) {
            WynnExtras.updateTestInventory(screen.getScreenHandler().slots);
        }

        drawBackgroundRect(context, xRemain, yRemain);
        if (readOnlyViewer) {
            if (readOnlyNoticeWidget == null) readOnlyNoticeWidget = new ReadOnlyNoticeWidget();
            int noticeWidth = xFitAmount * (162 + 4) - 4;
            readOnlyNoticeWidget.setBounds(xStart, Math.max(2, yStart - 30), noticeWidth, 12);
            readOnlyNoticeWidget.draw(context, mouseX, mouseY, delta, ui);
        }

        isMouseInOverlay = mouseY > yStart && mouseY < yStart + 100 * (yFitAmount - 1);

        int pageAmount = 0;
        {
            int visuali = 0;
            int extraYOffset = 0;
            layoutExtraScrollHeight = 0;
            renderedCrossClassPages.clear();
            scissorx1 = xStart - 5;
            scissory1 = yStart;
            scissorx2 = xStart + 166 * xFitAmount;
            scissory2 = yStart + 104 * (yFitAmount - 1) - 12;

            context.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
            ui.updateContext(context, ui.getScaleFactor(), 0, 0);

            if (!readOnlyViewer) detectCurrentClassAccountBankAvailabilityIfNeeded();

            // Check for cross-class search (@ or all characters browse mode)
            String rawSearchInput = searchbar2.getInput();
            boolean characterBankUnavailable = BankOverlay.isCharacterBankMissingCharacterId();
            boolean crossClassModeAllowed = shouldAllowCrossClassMode();
            boolean effectiveAllCharactersBrowseMode = allCharactersBrowseMode
                    && crossClassModeAllowed
                    && (!readOnlyViewer || currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER);
            boolean isCrossClassSearch = crossClassModeAllowed && ((rawSearchInput != null && rawSearchInput.contains("@")) || effectiveAllCharactersBrowseMode);
            String searchInput = rawSearchInput;

            // Strip @ from search query for actual matching
            if (searchInput != null && searchInput.contains("@")) {
                searchInput = searchInput.replace("@", "").trim();
            }
            if (!Objects.equals(activeSearchInput, searchInput)) {
                activeSearchInput = searchInput == null ? "" : searchInput;
                activeSearchQuery = SearchQueryParser.parse(activeSearchInput);
            }

            if (characterBankUnavailable && !isCrossClassSearch) {
                clearCrossClassSearchState();
                drawMissingCharacterIdWarning(xStart, yStart);
            } else if (isCrossClassSearch) {
                // Trigger cross-class search if needed (@ present, with or without search text)
                String cacheKey = effectiveAllCharactersBrowseMode ? ("__allchars__" + (rawSearchInput != null ? rawSearchInput : "")) : rawSearchInput;
                if (!cacheKey.equals(lastCrossClassSearchQuery)) {
                    lastCrossClassSearchQuery = cacheKey;
                    crossClassSearchActive = true;
                    crossClassPages.clear();
                    boolean includeCurrentCharacter = currentOverlayType != BankOverlayType.CHARACTER;
                    queueCrossClassSearch(cacheKey, searchInput, includeCurrentCharacter, true, effectiveAllCharactersBrowseMode);
                }
                startQueuedCrossClassSearchIfReady();
                applyCompletedCrossClassSearch(yStart);
            } else {
                // Clear cross-class results if not in cross-class mode
                if (crossClassSearchActive) {
                    clearCrossClassSearchState();
                }
            }

            boolean groupAllCharactersPages = effectiveAllCharactersBrowseMode && crossClassSearchActive;
            boolean accountPagesBeforeRegular = groupAllCharactersPages && currentOverlayType == BankOverlayType.CHARACTER;
            List<CrossClassPageWidget> accountCrossClassPages = Collections.emptyList();
            List<CrossClassPageWidget> currentCharacterCrossClassPages = Collections.emptyList();
            List<CrossClassPageWidget> remainingCrossClassPages = crossClassPages;
            boolean regularPagesHaveSearchResults = false;
            if (accountPagesBeforeRegular) {
                accountCrossClassPages = buildCachedAccountCrossClassPages(searchInput, yStart);
                remainingCrossClassPages = new ArrayList<>();
                for (CrossClassPageWidget ccPage : crossClassPages) {
                    if (!ccPage.isAccountBank() && !ccPage.isCurrentCharacter()) {
                        remainingCrossClassPages.add(ccPage);
                    }
                }
                if (initialBrowseScrollPending && !accountCrossClassPages.isEmpty() && (searchInput == null || searchInput.isEmpty())) {
                    int regularPageCount = characterBankUnavailable ? 0 : getRenderableRegularPageCount();
                    int estimatedPageAmount = accountCrossClassPages.size() + regularPageCount;
                    int estimatedExtraScrollHeight = 0;
                    if (!remainingCrossClassPages.isEmpty()) {
                        estimatedPageAmount = startNextRow(estimatedPageAmount);
                        estimatedPageAmount += remainingCrossClassPages.size();
                        estimatedExtraScrollHeight += CROSS_CLASS_GROUP_GAP;
                    }
                    int maxInitialOffset = getMaxScrollOffset(estimatedPageAmount, estimatedExtraScrollHeight);
                    float initialOffset = MathHelper.clamp(Math.floorDiv(accountCrossClassPages.size(), xFitAmount) * 104f, 0, maxInitialOffset);
                    actualOffset = initialOffset;
                    targetOffset = initialOffset;
                    initialBrowseScrollPending = false;
                }
                visuali = drawCrossClassPages(context, mouseX, mouseY, delta, xStart, yStart, visuali, extraYOffset, accountCrossClassPages);
                pageAmount += accountCrossClassPages.size();
            } else if (initialBrowseScrollPending && (!effectiveAllCharactersBrowseMode || currentOverlayType != BankOverlayType.CHARACTER)) {
                initialBrowseScrollPending = false;
            } else if (groupAllCharactersPages && currentOverlayType == BankOverlayType.ACCOUNT) {
                currentCharacterCrossClassPages = buildCachedCurrentCharacterCrossClassPages(searchInput, yStart);
                remainingCrossClassPages = new ArrayList<>();
                for (CrossClassPageWidget ccPage : crossClassPages) {
                    if (!ccPage.isCurrentCharacter() && !ccPage.isAccountBank()) {
                        remainingCrossClassPages.add(ccPage);
                    }
                }
            }

            if (!characterBankUnavailable) {
                int regularPageCount = getRenderableRegularPageCount();
                for(int i = 0; i < regularPageCount; i++) {
                    PageWidget page = pages.get(i);
                    float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                    float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) + extraYOffset - actualOffset;
                    page.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                    boolean pageVisible = pageIntersectsClip(invY, WynnExtrasConfig.INSTANCE.disableStickyNameplates ? 92 : 104, true);
                    boolean searching = searchInput != null && !searchInput.isEmpty();

                    if (!searching && !pageVisible && i != activeInv) {
                        page.setEnabled(false);
                        page.setSlotsVisible(false);
                        pageAmount++;
                        visuali++;
                        continue;
                    }

                    page.setItems(buildInventoryForIndex(i, false));

                    if(searching) {
                        boolean containsSearch = page.containsSearch(
                                searchInput,
                                activeSearchQuery,
                                i == activeInv && !WynnExtrasConfig.INSTANCE.bankOverlayExcludeActivePageFromSearches
                        );

                        if(!containsSearch) {
                            page.setEnabled(false);
                            page.setSlotsVisible(false);
                            continue;
                        } else {
                            page.setEnabled(true);
                            regularPagesHaveSearchResults = true;
                            pageAmount++;
                        }
                    } else {
                        page.setEnabled(true);
                        pageAmount++;
                    }

                    if(pageVisible) {
                        page.draw(context, mouseX, mouseY, delta, ui);
                    } else {
                        page.setEnabled(false);
                        page.setSlotsVisible(false);
                    }
                    visuali++;
                }
            }

            if (!currentCharacterCrossClassPages.isEmpty()) {
                visuali = drawCrossClassPages(context, mouseX, mouseY, delta, xStart, yStart, visuali, extraYOffset, currentCharacterCrossClassPages);
                pageAmount += currentCharacterCrossClassPages.size();
            }

            // Render cross-class pages after regular pages
            if (crossClassSearchActive) {
                boolean localSearchResultsEmpty = accountCrossClassPages.isEmpty() && currentCharacterCrossClassPages.isEmpty() && !regularPagesHaveSearchResults;
                if (crossClassPages.isEmpty() && crossClassSearchLoading && localSearchResultsEmpty) {
                    drawCrossClassSearchLoading(xStart, yStart);
                } else if (crossClassPages.isEmpty() && localSearchResultsEmpty) {
                    drawCrossClassSearchEmpty(xStart, yStart);
                } else if (!remainingCrossClassPages.isEmpty()) {
                    if (groupAllCharactersPages) {
                        int visualBeforeGap = visuali;
                        visuali = startNextRow(visuali);
                        pageAmount += visuali - visualBeforeGap;
                        extraYOffset += CROSS_CLASS_GROUP_GAP;
                        layoutExtraScrollHeight += CROSS_CLASS_GROUP_GAP;
                    }
                    visuali = drawCrossClassPages(context, mouseX, mouseY, delta, xStart, yStart, visuali, extraYOffset, remainingCrossClassPages);
                    pageAmount += remainingCrossClassPages.size();
                }
            }

            context.disableScissor();

            int bottomWidgetsY = yStart + (yFitAmount - 1) * (90 + 4 + 10);
            int inventoryWidgetX = buttonWidgetsX + (int) (160 * ui.getScaleFactor());
            int rightButtonWidgetsX = buttonWidgetsX + (int) (342 * ui.getScaleFactor());

            drawDetachedButtonPanelBarsIfNeeded(buttonWidgetsX, rightButtonWidgetsX, bottomWidgetsY - 8, xStart);

            inventoryWidget.setBounds(inventoryWidgetX, bottomWidgetsY - 3, (int) (176 * ui.getScaleFactor()), (int) (86 * ui.getScaleFactor()));
            inventoryWidget.setItems(buildInventoryForIndex(0, true));
            inventoryWidget.draw(context, mouseX, mouseY, delta, ui);

            boolean showSwitchButton = shouldShowBankSwitchButton();
            if(showSwitchButton) {
                switchButtonWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
                switchButtonWidget.draw(context, mouseX, mouseY, delta, ui);
            } else {
                switchButtonWidget.setBounds(0, 0, 0, 0);
            }

            if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
                ui.drawImage(showSwitchButton ? buttonBackgroundDark : buttonBackgroundShortDark, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            } else {
                ui.drawImage(showSwitchButton ? buttonBackground : buttonBackgroundShort, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            }

            if(showSwitchButton) {
                searchbar2.setBounds(buttonWidgetsX, bottomWidgetsY + 59, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            } else {
                searchbar2.setBounds(buttonWidgetsX, bottomWidgetsY + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            }

            searchbar2.setTextColor(WHITE_TEXT_COLOR);
            searchbar2.setBackgroundColor(null);
            searchbar2.draw(context, mouseX, mouseY, delta, ui);

            if(showSwitchButton) {
                String targetName = readOnlyViewer
                        ? getReadOnlyViewerTypeName(getNextReadOnlyViewerType())
                        : (currentOverlayType == BankOverlayType.ACCOUNT ? "Character" : "Account") + " Bank";
                ui.drawCenteredText("Switch to " + targetName, buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, WHITE_TEXT_COLOR, 1.1f);
            }
            if(showSwitchButton) {
                ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 44, WHITE_TEXT_COLOR, 1.1f);
            } else {
                ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, WHITE_TEXT_COLOR, 1.1f);
            }

            boolean showAllCharactersButton = currentOverlayType != BankOverlayType.NONE
                    && (!readOnlyViewer || currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER)
                    && shouldAllowCrossClassMode();
            boolean showReloadButton = currentOverlayType != BankOverlayType.NONE;
            boolean showRightButtonPanel = currentOverlayType != BankOverlayType.NONE && (showAllCharactersButton || showReloadButton);

            if(showRightButtonPanel) {
                Identifier rightButtonBackground = showAllCharactersButton && showReloadButton
                        ? (WynnExtrasConfig.INSTANCE.darkmodeToggle ? buttonBackgroundShortDark : buttonBackgroundShort)
                        : (WynnExtrasConfig.INSTANCE.darkmodeToggle ? buttonBackgroundSingleDark : buttonBackgroundSingle);
                ui.drawImage(rightButtonBackground,
                        rightButtonWidgetsX - 8, bottomWidgetsY - 8,
                        (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));

                if (showAllCharactersButton) {
                    allCharactersButtonWidget.setBounds(rightButtonWidgetsX, bottomWidgetsY + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
                    allCharactersButtonWidget.draw(context, mouseX, mouseY, delta, ui);
                } else {
                    allCharactersButtonWidget.setBounds(0, 0, 0, 0);
                }

                if (showReloadButton) {
                    int reloadButtonY = showAllCharactersButton ? bottomWidgetsY + 31 : bottomWidgetsY + 3;
                    reloadBankWidget.setBounds(rightButtonWidgetsX, reloadButtonY, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
                    reloadBankWidget.draw(context, mouseX, mouseY, delta, ui);
                } else {
                    reloadBankWidget.setBounds(0, 0, 0, 0);
                }
            } else {
                allCharactersButtonWidget.setBounds(0, 0, 0, 0);
                reloadBankWidget.setBounds(0, 0, 0, 0);
            }
        }

        shownPages = pageAmount;
        int currentMaxOffset = getMaxScrollOffset(shownPages);
        if (restoreScrollAfterLayout) {
            if (!deferPreservedScrollClamp || !crossClassSearchLoading) {
                targetOffset = MathHelper.clamp(preservedTargetOffset, 0, currentMaxOffset);
                actualOffset = MathHelper.clamp(preservedActualOffset, 0, currentMaxOffset);
                restoreScrollAfterLayout = false;
                deferPreservedScrollClamp = false;
            }
        }
        if (currentMaxOffset > 0) {
            int scrollBarHeight = (yFitAmount - 1) * 104 + (xFitAmount <= 2 ? 0 : 12);
            scrollBarWidget.setBounds(xStart + xFitAmount * 170, yStart - 13, 15, scrollBarHeight);
            scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);
        } else {
            targetOffset = 0;
            actualOffset = 0;
            scrollBarWidget.setBounds(0, 0, 0, 0);
        }

        drawEmeraldOverlay(context, xStart - 36, yStart - 14);
        drawSearchInfoButton(context, xStart, yStart, mouseX, mouseY);
        if (WynnExtrasConfig.INSTANCE.bankBagOverlay
                && !BankOverlay.isCharacterBankMissingCharacterId()
                && (currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER || currentOverlayType == BankOverlayType.MISC)) {
            // Top section: bank bags. Header is drawn top-right of the screen, grid sits in
            // the left margin alongside the bank pages.
            int bankGridX = xStart - 36 - 56;
            int bankGridY = yStart - 14 + 4 * 28;
            drawBagOverlay(
                    context,
                    bankGridX,
                    bankGridY,
                    getCurrentPageStacks(),
                    collectVisibleBankBagCounts());

            // Bottom section: bags currently in player inventory, in the same column directly
            // below the bank grid with a gap so the two read as separate sections. Reserve
            // BAG_RAID_ORDER.length rows worth of space so the inventory grid never collides
            // with the bank grid even when every raid is populated.
            if (readOnlyViewer ? !livePlayerInventoryItems.isEmpty()
                    : BankOverlay.playerInvSlots != null && !BankOverlay.playerInvSlots.isEmpty()) {
                int invBagY = bankGridY + BAG_RAID_ORDER.length * 28 + 18;
                drawBagGrid(context, bankGridX, invBagY, livePlayerInventoryItems);
            }
        }

        if(shouldShowBankSwitchButton()) {
            quickActionWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
        } else {
            quickActionWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
        }
        quickActionWidget.draw(context, mouseX, mouseY, delta, ui);

        renderHoveredTooltip(context, screen, mouseX, mouseY);
        if (!readOnlyViewer) renderHeldItemOverlay(context, mouseX, mouseY);

        touchHoveredSlot = hoveredBackingSlot;
        BankOverlaySlotBridge.endFrame();
    }

    private int getButtonWidgetsX(int xStart) {
        if (xFitAmount <= 2) {
            int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            float virtualThreeColumnWidth = 3 * (162 + 4) - 4;
            float virtualXStart = (screenWidth - virtualThreeColumnWidth) / 2f - 2;
            return (int) (virtualXStart * ui.getScaleFactor());
        }

        if (xFitAmount > 2 && xFitAmount % 2 == 0) {
            float pagesWidth = xFitAmount * (162 + 4) - 4;
            float centeredInventoryX = xStart + pagesWidth / 2f - 88;
            return (int) ((centeredInventoryX - 160) * ui.getScaleFactor());
        }

        return (int) ((xStart + (xFitAmount / 2) * (162 + 4) - 166) * ui.getScaleFactor());
    }

    private void drawDetachedButtonPanelBarsIfNeeded(int leftButtonWidgetsX, int rightButtonWidgetsX, int panelY, int xStart) {
        if (xFitAmount > 2) return;

        CustomColor barColor = WynnExtrasConfig.INSTANCE.darkmodeToggle ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
        CustomColor borderColor = WynnExtrasConfig.INSTANCE.darkmodeToggle ? DARK_BORDER_COLOR : LIGHT_BORDER_COLOR;

        float scale = ui.getScaleFactorF();
        float backgroundLeft = (xStart - 7) * scale;
        float backgroundRight = backgroundLeft + (xFitAmount * (162 + 4) + 11) * scale;
        float leftPanelLeft = leftButtonWidgetsX - 7.5f;
        float rightPanelRight = rightButtonWidgetsX - 7.5f + 169 * scale;

        drawDetachedButtonPanelBar(leftPanelLeft, panelY, backgroundLeft - leftPanelLeft, barColor, borderColor, true);
        drawDetachedButtonPanelBar(backgroundRight, panelY, rightPanelRight - backgroundRight, barColor, borderColor, false);
    }

    private void drawDetachedButtonPanelBar(float x, int panelY, float width, CustomColor barColor, CustomColor borderColor, boolean leftBar) {
        if (width <= 0) return;

        ui.drawRect(
                x,
                panelY - 4.5f,
                width,
                (int) (10 * ui.getScaleFactor()),
                borderColor
        );

        ui.drawRect(
                x - (leftBar ? -1 : 1),
                panelY - 3.5f,
                width,
                (int) (8 * ui.getScaleFactor()),
                barColor
        );
    }

    private void drawBackgroundRect(DrawContext context, float xRemain, float yRemain) {
        if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
            RenderUtils.drawRect(
                    context,
                    DARK_BACKGROUND_COLOR,
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context,
                    DARK_BORDER_COLOR,
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
            );
        } else {
            RenderUtils.drawRect(
                    context,
                    LIGHT_BACKGROUND_COLOR,
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context,
                    LIGHT_BORDER_COLOR,
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
            );
        }
    }

    private static boolean pageIntersectsClip(float pageY, float pageHeight, boolean hasLabel) {
        float labelTop = hasLabel ? pageY - 12 : pageY;
        return pageY + pageHeight > scissory1 && labelTop < scissory2;
    }

    private static boolean isPointInsidePageClip(double x, double y) {
        return x >= scissorx1 && x < scissorx2 && y >= scissory1 && y < scissory2;
    }

    private void drawMissingCharacterIdWarning(int xStart, int yStart) {
        float centerX = xStart + xFitAmount * (162 + 4) / 2f;
        float centerY = yStart + Math.max(70, (yFitAmount - 1) * 104 / 2f);
        ui.drawCenteredText("Character bank failed to load.", centerX, centerY - 24, GOLD_TEXT_COLOR, 1.4f);
        ui.drawCenteredText("Error while loading your character UUID.", centerX, centerY - 6, WHITE_TEXT_COLOR, 1.0f);
        ui.drawCenteredText("Use /class and try again.", centerX, centerY + 8, WHITE_TEXT_COLOR, 1.0f);
        ui.drawCenteredText("If this problem remains, please make a bug report on our Discord.", centerX, centerY + 22, WHITE_TEXT_COLOR, 1.0f);
        ui.drawCenteredText("Use /we discord to join.", centerX, centerY + 36, WHITE_TEXT_COLOR, 1.0f);
    }

    private static ItemStack getRightPageButton() {
        try {
            ScreenHandler menu = MinecraftUtils.containerMenu();
            if (menu == null) return Items.AIR.getDefaultStack();
            return menu.getSlot(52).getStack();
        } catch (Exception ignored) {
            return Items.AIR.getDefaultStack();
        }
    }

    private static ItemStack getLeftPageButton() {
        try {
            ScreenHandler menu = MinecraftUtils.containerMenu();
            if (menu == null) return Items.AIR.getDefaultStack();
            return menu.getSlot(51).getStack();
        } catch (Exception ignored) {
            return Items.AIR.getDefaultStack();
        }
    }

    private static int parsePageNumber(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        Matcher nameMatcher = PAGE_NUMBER_PATTERN.matcher(
                MINECRAFT_FORMATTING_CODE_PATTERN.matcher(stack.getName().getString()).replaceAll(""));
        if (nameMatcher.find()) return parsePageNumber(nameMatcher);
        return -1;
    }

    private static int parsePageNumber(Matcher matcher) {
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (IllegalArgumentException ignored) {
            return -1;
        }
    }

    private static boolean advertisesQuickJump(ItemStack stack, int pageNumber) {
        if (stack == null || stack.isEmpty() || stack.getComponents() == null) return false;
        var lore = stack.getComponents().get(DataComponentTypes.LORE);
        if (lore == null) return false;
        for (Text line : lore.lines()) {
            Matcher matcher = PAGE_NUMBER_PATTERN.matcher(
                    MINECRAFT_FORMATTING_CODE_PATTERN.matcher(line.getString()).replaceAll(""));
            if (matcher.find() && parsePageNumber(matcher) == pageNumber) return true;
        }
        return false;
    }

    private static int detectLiveBankPageIndex() {
        int nextPage = parsePageNumber(getRightPageButton());
        if (nextPage >= 2) return nextPage - 2;
        int previousPage = parsePageNumber(getLeftPageButton());
        if (previousPage >= 1) return previousPage;
        return -1;
    }

    private static boolean jumpToBankPage(int pageIndex) {
        int maxPage = Math.max(0, BankOverlay.getCurrentMaxPages() - 1);
        if (pageIndex < 0 || pageIndex > maxPage) return false;

        activeInv = pageIndex;
        shouldWait = true;
        shouldWaitSince = System.currentTimeMillis();
        clearAnnotationCache(pageIndex);
        retryLoad();

        pageJumpTarget = pageIndex;
        pageJumpLastActionAt = 0L;
        pageJumpLastProgressActionAt = 0L;
        pageJumpLastResponseAt = 0L;
        pageJumpLastActionPage = -1;
        continuePageJump();
        return true;
    }

    private static void continuePageJump() {
        if (pageJumpTarget < 0) return;
        ScreenHandler menu = MinecraftUtils.containerMenu();
        if (menu == null || menu.slots.size() <= 52) return;

        int currentPage = detectLiveBankPageIndex();
        if (currentPage < 0) return;
        if (currentPage == pageJumpTarget) {
            activeInv = currentPage;
            shouldWait = false;
            shouldWaitSince = 0L;
            pageJumpTarget = -1;
            pageJumpLastActionAt = 0L;
            retryLoad();
            clearAnnotationCache(currentPage);
            return;
        }

        long now = System.currentTimeMillis();
        if (pageJumpLastActionAt > 0L) {
            long sinceAction = now - pageJumpLastActionAt;
            boolean receivedResponse = pageJumpLastResponseAt >= pageJumpLastActionAt;
            long retryIn;
            if (receivedResponse) {
                boolean progressed = pageJumpLastActionPage >= 0 && currentPage != pageJumpLastActionPage;
                if (progressed) pageJumpLastProgressActionAt = pageJumpLastActionAt;
                long sinceProgress = pageJumpLastProgressActionAt == 0L
                        ? Long.MAX_VALUE
                        : now - pageJumpLastProgressActionAt;
                retryIn = Math.max(
                        BANK_PAGE_PROGRESS_INTERVAL_MS - sinceProgress,
                        (progressed ? BANK_PAGE_RESPONSE_SETTLE_MS : BANK_PAGE_REJECTED_RESPONSE_SETTLE_MS)
                                - (now - pageJumpLastResponseAt));
            } else {
                retryIn = BANK_PAGE_RESPONSE_TIMEOUT_MS - sinceAction;
            }
            if (retryIn > 0L) return;
        }
        pageJumpLastActionAt = now;
        pageJumpLastActionPage = currentPage;
        shouldWaitSince = now;

        int currentPageNumber = currentPage + 1;
        int targetPageNumber = pageJumpTarget + 1;
        int difference = targetPageNumber - currentPageNumber;
        if (Math.abs(difference) == 1) {
            int navigationSlot = difference > 0 ? 52 : 51;
            ContainerUtils.clickOnSlot(navigationSlot, menu.syncId, menu.getRevision(), 0, menu.getStacks());
            return;
        }

        int maxQuickJump = currentOverlayType == BankOverlayType.ACCOUNT ? 17 : 11;
        int nearestQuickJump = 1;
        for (int destination = 1; destination <= maxQuickJump; destination += 2) {
            if (Math.abs(targetPageNumber - destination) < Math.abs(targetPageNumber - nearestQuickJump)) {
                nearestQuickJump = destination;
            }
        }

        boolean quickJumpForward = difference > 0 && nearestQuickJump > currentPageNumber;
        boolean quickJumpBackward = difference < 0 && nearestQuickJump < currentPageNumber;
        if (quickJumpForward || quickJumpBackward) {
            int hotbarKey = (nearestQuickJump - 1) / 2;
            if (advertisesQuickJump(getRightPageButton(), nearestQuickJump)) {
                ContainerUtils.pressKeyOnSlot(52, menu.syncId, menu.getRevision(), hotbarKey, menu.getStacks());
                return;
            }
            if (advertisesQuickJump(getLeftPageButton(), nearestQuickJump)) {
                ContainerUtils.pressKeyOnSlot(51, menu.syncId, menu.getRevision(), hotbarKey, menu.getStacks());
                return;
            }
        }

        int navigationSlot = difference > 0 ? 52 : 51;
        ContainerUtils.clickOnSlot(navigationSlot, menu.syncId, menu.getRevision(), 0, menu.getStacks());
    }

    public static void onBankPageNavigationUpdate(int syncId, int revision, int slot) {
        if (pageJumpTarget < 0 || currentOverlayType == BankOverlayType.NONE) return;
        ScreenHandler menu = MinecraftUtils.containerMenu();
        if (menu == null || menu.syncId != syncId) return;
        pageJumpLastResponseAt = System.currentTimeMillis();
        continuePageJump();
    }

    public static void onBankContainerUpdate(int syncId, int slot) {
        if (!isReloading || currentOverlayType == BankOverlayType.NONE || slot > 52) return;
        ScreenHandler menu = MinecraftUtils.containerMenu();
        if (menu != null && menu.syncId == syncId) {
            reloadLastContainerUpdateAt = System.currentTimeMillis();
        }
    }

    private static boolean isPagePurchaseButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getComponents() == null) return false;

        String rawName = stack.getName().getString();
        if (rawName.contains(">§4>§c>§4>§c>")) {
            return true;
        }
        String cleanedName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(rawName).replaceAll("");
        if (cleanedName.toLowerCase(Locale.ROOT).contains("price")) {
            return true;
        }

        if (stack.getComponents().get(DataComponentTypes.CUSTOM_NAME) != null) {
            String customName = stack.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString();
            if (customName.contains(">§4>§c>§4>§c>")) {
                return true;
            }
            String cleanedCustomName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(customName).replaceAll("");
            if (cleanedCustomName.toLowerCase(Locale.ROOT).contains("price")) {
                return true;
            }
        }

        if (stack.getComponents().get(DataComponentTypes.LORE) != null) {
            for (Text line : stack.getComponents().get(DataComponentTypes.LORE).lines()) {
                String cleanedLine = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(line.getString()).replaceAll("");
                String lowerLine = cleanedLine.toLowerCase(Locale.ROOT);
                if (lowerLine.contains("price") || lowerLine.contains("click to buy") || lowerLine.contains("click again to confirm")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String getPageRankRequirement(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getComponents() == null) return null;
        if (stack.getComponents().get(DataComponentTypes.LORE) == null) return null;

        StringBuilder loreText = new StringBuilder();
        for (Text line : stack.getComponents().get(DataComponentTypes.LORE).lines()) {
            String cleanedLine = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(line.getString()).replaceAll("");
            if (!loreText.isEmpty()) loreText.append(' ');
            loreText.append(cleanedLine.trim());
        }
        String cleanedLore = loreText.toString().replaceAll("\\s+", " ").trim();
        String lowerLore = cleanedLore.toLowerCase(Locale.ROOT);
        if (!lowerLore.contains("the next page is only")
                && !lowerLore.contains("only available to")
                && !lowerLore.contains("requires a rank")) {
            return null;
        }

        Matcher matcher = PAGE_RANK_REQUIREMENT_PATTERN.matcher(cleanedLore);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "a rank";
    }

    private static void setRankLockedPageLimit(int pageCount, String requiredRank) {
        if (pageCount < 1 || currentOverlayType == BankOverlayType.NONE) return;
        rankLockedOverlayType = currentOverlayType;
        rankLockedPageCount = pageCount;
        rankLockedRequiredRank = requiredRank == null || requiredRank.isBlank() ? "a rank" : requiredRank;
        if (currentData != null && hasStoredPagePast(pageCount)) {
            truncateStoredPages(pageCount);
        }
    }

    private static boolean hasStoredPagePast(int pageCount) {
        if (currentData == null) return false;
        return currentData.getLastPage() > pageCount
                || currentData.getBankPages().keySet().stream().anyMatch(pageIndex -> pageIndex >= pageCount)
                || currentData.getBankPageNames().keySet().stream().anyMatch(pageIndex -> pageIndex >= pageCount)
                || currentData.getBagCounts().keySet().stream().anyMatch(pageIndex -> pageIndex >= pageCount);
    }

    private static void clearRankLockedPageLimit() {
        if (currentOverlayType != rankLockedOverlayType) return;
        rankLockedOverlayType = BankOverlayType.NONE;
        rankLockedPageCount = -1;
        rankLockedRequiredRank = "";
    }

    private static void clearRankLockedPageLimitIfPastBoundary(int currentPageCount) {
        if (currentOverlayType == rankLockedOverlayType && rankLockedPageCount > 0 && currentPageCount >= rankLockedPageCount) {
            clearRankLockedPageLimit();
        }
    }

    private static boolean isCurrentBankRankLockedAtLimit() {
        return currentOverlayType == rankLockedOverlayType
                && rankLockedPageCount > 0
                && currentData != null
                && currentData.getLastPage() >= rankLockedPageCount;
    }

    private static boolean rightButtonPointsToPage(ItemStack stack, int pageNumber) {
        if (stack == null || stack.isEmpty()) return false;
        String rawName = stack.getName().getString();
        String cleanedName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(rawName).replaceAll("");
        return cleanedName.contains("Page " + pageNumber);
    }

    private static Float getFirstCustomModelData(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getComponents() == null) return null;
        CustomModelDataComponent data = stack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null || data.floats().isEmpty()) return null;
        return data.floats().get(0);
    }

    private static boolean matchesKnownNextPageModelData(ItemStack stack) {
        Float modelData = getFirstCustomModelData(stack);
        return modelData != null
                && reloadNextPageCustomModelData != null
                && Float.compare(modelData, reloadNextPageCustomModelData) == 0;
    }

    private static boolean isReloadCurrentPageReady() {
        if (activeInv != reloadCurrentPage) return false;
        if (BankOverlay.activeInvSlots.size() < 45) return false;

        ItemStack rightButton = getRightPageButton();
        int currentPageNumber = reloadCurrentPage + 1;
        int nextPageNumber = reloadCurrentPage + 2;
        boolean pointsToNextPage = rightButtonPointsToPage(rightButton, nextPageNumber);
        boolean stillOnPreviousPage = reloadCurrentPage > 0 && rightButtonPointsToPage(rightButton, currentPageNumber);
        boolean lastKnownPage = reloadCurrentPage >= reloadTotalPages - 1;

        return pointsToNextPage || (lastKnownPage && !stillOnPreviousPage);
    }

    private static int getActiveBankSlotFingerprint() {
        if (BankOverlay.activeInvSlots.size() < 45) return 0;

        int result = 1;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = BankOverlay.activeInvSlots.get(i).getStack();
            result = 31 * result + getStackFingerprint(stack);
        }
        return result;
    }

    private static int getStackFingerprint(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int result = stack.getItem().hashCode();
        result = 31 * result + stack.getCount();
        result = 31 * result + stack.getName().getString().hashCode();
        result = 31 * result + stack.getComponents().hashCode();
        return result;
    }

    private static List<ItemStack> snapshotCurrentBankPageStacks() {
        List<ItemStack> stacks = new ArrayList<>(45);
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenHandler menu = MinecraftUtils.containerMenu();
        if (menu != null && mc.player != null) {
            Inventory playerInv = mc.player.getInventory();
            for (Slot slot : menu.slots) {
                if (slot.inventory == playerInv) continue;
                stacks.add(slot.getStack().copy());
                if (stacks.size() >= 45) return stacks;
            }
        }

        stacks.clear();
        for (Slot slot : BankOverlay.activeInvSlots) {
            stacks.add(slot.getStack().copy());
            if (stacks.size() >= 45) return stacks;
        }
        return stacks;
    }

    private static void saveReloadCurrentPage() {
        if (currentOverlayType == BankOverlayType.CHARACTER && !BankOverlay.syncCurrentCharacterId()) return;
        if (Pages == null) return;
        if (reloadSavedPage == reloadCurrentPage) return;

        List<ItemStack> snapshot = snapshotCurrentBankPageStacks();
        if (snapshot.size() < 45) return;
        Pages.getBankPages().put(reloadCurrentPage, snapshot);
        invalidateLocalCrossClassPageCaches();
        clearAnnotationCache(reloadCurrentPage);
        reloadSavedPage = reloadCurrentPage;
    }

    private static void truncateStoredPages(int pageCount) {
        if (currentData == null) return;

        int validPageCount = Math.max(1, Math.min(pageCount, BankOverlay.getCurrentMaxPages()));
        currentData.setLastPage(validPageCount);
        currentData.getBankPages().keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        currentData.getBankPageNames().keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        currentData.getBagCounts().keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        annotationCache.keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        annotationStackCache.keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        annotationComponentCache.keySet().removeIf(pageIndex -> pageIndex >= validPageCount);
        invalidateLocalCrossClassPageCaches();
        reloadTotalPages = Math.min(reloadTotalPages, validPageCount);
        invalidateBagTotalCache();
        currentData.saveAsyncDebounced();
    }

    private static void resetReloadPageReadiness() {
        reloadPageLoaded = false;
        reloadStableTicks = 0;
        reloadLastSlotFingerprint = 0;
        reloadSavedPage = -1;
        reloadLastContainerUpdateAt = System.currentTimeMillis();
    }

    private static void stopReloadAndReturnToOriginalPage() {
        isReloading = false;
        resetReloadPageReadiness();
        reloadNextPageCustomModelData = null;
        reloadLastContainerUpdateAt = 0L;
        int maxReturnPage = currentData == null ? BankOverlay.getCurrentMaxPages() - 1 : currentData.getLastPage() - 1;
        int returnPage = MathHelper.clamp(Math.max(0, reloadOriginalPage), 0, Math.max(0, maxReturnPage));
        jumpToBankPage(returnPage);
        retryLoad();
        if (Pages != null) Pages.saveAsyncDebounced();
    }

    private static boolean canReloadNextPage() {
        int maxPages = BankOverlay.getCurrentMaxPages();
        if (reloadCurrentPage >= maxPages - 1) {
            return false;
        }

        ItemStack rightButton = getRightPageButton();
        int nextPageNumber = reloadCurrentPage + 2;
        boolean pointsToNextPage = rightButtonPointsToPage(rightButton, nextPageNumber);
        boolean purchaseButton = isPagePurchaseButton(rightButton);
        Float buttonModelData = getFirstCustomModelData(rightButton);

        String requiredRank = getPageRankRequirement(rightButton);
        if (requiredRank != null) {
            setRankLockedPageLimit(reloadCurrentPage + 1, requiredRank);
            truncateStoredPages(reloadCurrentPage + 1);
            return false;
        }
        clearRankLockedPageLimitIfPastBoundary(reloadCurrentPage + 1);

        if (purchaseButton) {
            truncateStoredPages(reloadCurrentPage + 1);
            return false;
        }

        if (pointsToNextPage) {
            if (nextPageNumber <= reloadTotalPages) {
                if (reloadNextPageCustomModelData == null) {
                    reloadNextPageCustomModelData = buttonModelData;
                }
            } else if (!matchesKnownNextPageModelData(rightButton)) {
                return false;
            }

            if (currentData != null) {
                currentData.setLastPage(Math.max(currentData.getLastPage(), nextPageNumber));
            }
            reloadTotalPages = Math.max(reloadTotalPages, nextPageNumber);
        }

        if (currentData != null) {
            reloadTotalPages = Math.max(reloadTotalPages, currentData.getLastPage());
        }
        reloadTotalPages = Math.min(reloadTotalPages, maxPages);

        boolean canContinue = reloadCurrentPage + 1 < reloadTotalPages;
        return canContinue;
    }

    private static String describeRightPageButton(ItemStack stack) {
        if (stack == null) return "null";
        if (stack.isEmpty()) return "empty";

        String name = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(stack.getName().getString()).replaceAll("");
        String customName = "none";
        if (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.CUSTOM_NAME) != null) {
            customName = MINECRAFT_FORMATTING_CODE_PATTERN
                    .matcher(stack.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString())
                    .replaceAll("");
        }

        String modelData = "none";
        if (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA) != null) {
            try {
                CustomModelDataComponent data = stack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
                modelData = data.floats().toString();
            } catch (Exception ignored) {}
        }

        return "item=" + stack.getItem()
                + ", name=\"" + shortenForLog(name) + "\""
                + ", customName=\"" + shortenForLog(customName) + "\""
                + ", modelData=" + modelData
                + ", hasLore=" + (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.LORE) != null);
    }

    private static String shortenForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 120) return text;
        return text.substring(0, 117) + "...";
    }

    private static int getRenderableRegularPageCount() {
        if (currentData == null) return pages.size();
        if (readOnlyViewerActive) {
            int highestStoredPage = currentData.getBankPages().keySet().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(-1) + 1;
            return Math.min(pages.size(), Math.max(currentData.getLastPage(), highestStoredPage));
        }
        int storedPageCount = currentData.getLastPage();
        int requested = Math.max(storedPageCount + 1, activeInv + 1);
        return Math.min(pages.size(), Math.max(0, requested));
    }

    private static boolean isCrossClassMode() {
        return allCharactersBrowseMode || crossClassSearchActive;
    }

    private static int getMaxScrollOffset(int pageCount) {
        return getMaxScrollOffset(pageCount, layoutExtraScrollHeight);
    }

    private static int getMaxScrollOffset(int pageCount, int extraScrollHeight) {
        if (xFitAmount <= 0) return 0;
        int totalRows = (int) Math.ceil((double) pageCount / xFitAmount);
        int c = (xFitAmount % 2 == 0 ? 1 : 0);
        return Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * 3) - 104 * c + extraScrollHeight);
    }

    private static void scrollToPage(int pageIndex) {
        if (pageIndex < 0 || xFitAmount <= 0) return;

        int row = Math.floorDiv(pageIndex, xFitAmount);
        int maxOffset = getMaxScrollOffset(Math.max(Math.max(shownPages, BankOverlay.getCurrentMaxPages()), pageIndex + 1));
        float newOffset = MathHelper.clamp(row * 104f, 0, maxOffset);
        targetOffset = newOffset;
    }

    public static void saveActivePageSnapshot() {
        if (storeActivePageSnapshot()) {
            Pages.saveAsyncDebounced();
        }
    }

    private static boolean storeActivePageSnapshot() {
        if (bankTypeSwitchInProgress) return false;
        if (currentOverlayType == BankOverlayType.CHARACTER && !BankOverlay.syncCurrentCharacterId()) return false;
        if (BankOverlay.isCharacterBankMissingCharacterId()) return false;
        if (Pages == null || activeInv < 0 || shouldWait) return false;

        List<ItemStack> snapshot = snapshotCurrentBankPageStacks();
        if (snapshot.size() < 45) return false;
        Pages.getBankPages().put(activeInv, snapshot);
        invalidateLocalCrossClassPageCaches();
        clearAnnotationCache(activeInv);
        return true;
    }

    public static boolean isBankTypeSwitchInProgress() {
        return bankTypeSwitchInProgress;
    }

    public static void resetBankTypeSwitchState() {
        bankTypeSwitchInProgress = false;
        bankTypeSwitchTargetApplied = false;
        bankTypeSwitchTargetType = BankOverlayType.NONE;
        bankTypeSwitchTargetPage = -1;
    }

    public static void resetInteractionBlockers() {
        shouldWait = false;
        shouldWaitSince = 0L;
        resetBankTypeSwitchState();
        bankSyncid = 0;
        preserveScrollOnNextOverlay = false;
        restoreScrollAfterLayout = false;
        deferPreservedScrollClamp = false;
        heldItem = Items.AIR.getDefaultStack();
        isReloading = false;
        resetReloadPageReadiness();
        reloadNextPageCustomModelData = null;
        reloadLastContainerUpdateAt = 0L;
        pendingMouseTweaksRightClick = null;
        pageJumpTarget = -1;
        pageJumpLastActionAt = 0L;
        pageJumpLastProgressActionAt = 0L;
        pageJumpLastResponseAt = 0L;
        pageJumpLastActionPage = -1;
        BankOverlay.resetScrollRegistration();
        BankOverlaySlotBridge.restoreAll();
        clearHoverState(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> handledScreen ? handledScreen : null);
    }

    private static boolean isJumpInProgress() {
        if (bankTypeSwitchInProgress) {
            if (currentOverlayType == bankTypeSwitchTargetType && activeInv == bankTypeSwitchTargetPage && isActiveBankPageReady()) {
                shouldWait = false;
                resetBankTypeSwitchState();
            } else {
                return true;
            }
        }
        return shouldWait && !isActiveBankPageReady();
    }

    private static boolean hasHeldItem() {
        return heldItem != null && !heldItem.isEmpty() && heldItem.getItem() != Items.AIR;
    }

    private static ScreenHandler getLiveScreenHandlerForClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.currentScreenHandler == null) return null;
        ScreenHandler handler = MinecraftUtils.containerMenu();
        if (handler == null) return null;
        if (handler.syncId != mc.player.currentScreenHandler.syncId) return null;
        return mc.player.currentScreenHandler;
    }

    private static boolean isActiveBankPageReady() {
        if (activeInv < 0) return false;
        if (BankOverlay.activeInvSlots.size() < 45) return false;

        ItemStack rightButton = getRightPageButton();
        int nextPageNumber = activeInv + 2;
        if (rightButton.getItem() == Items.POTION) {
            return rightButtonPointsToPage(rightButton, nextPageNumber);
        }

        return currentData != null && activeInv == currentData.getLastPage() - 1;
    }

    private static void applyPendingBankTypeSwitchTargetIfReady() {
        if (!bankTypeSwitchInProgress || bankTypeSwitchTargetApplied) return;
        if (bankTypeSwitchTargetType == BankOverlayType.NONE || currentOverlayType != bankTypeSwitchTargetType) return;

        activeInv = MathHelper.clamp(bankTypeSwitchTargetPage, 0, Math.max(0, BankOverlay.getCurrentMaxPages() - 1));
        shouldWait = true;
        shouldWaitSince = System.currentTimeMillis();
        bankTypeSwitchTargetApplied = true;
        BankOverlay.activeInvSlots.clear();
        annotationCache.clear();
        clearAnnotationCache(activeInv);
        retryLoad();
    }

    private static void finishBankTypeSwitchIfReady(BankOverlayType targetType, int targetPage) {
        if (!bankTypeSwitchInProgress) return;
        if (currentOverlayType != targetType || activeInv != targetPage) return;
        if (shouldWait) {
            if (!isActiveBankPageReady()) return;
            shouldWait = false;
        }

        int livePage = getCurrentBankPageNumber();
        if (livePage != -1 && livePage != targetPage) return;

        resetBankTypeSwitchState();
    }

    private static void forceFinishBankTypeSwitch(BankOverlayType targetType) {
        if (!bankTypeSwitchInProgress || currentOverlayType != targetType) return;
        resetBankTypeSwitchState();
    }

    public static void saveCurrentPlayerInventorySnapshot() {
        boolean syncedCharacterId = BankOverlay.syncCurrentCharacterId();
        if (!syncedCharacterId && !BankOverlay.hasValidCurrentCharacterId()) return;
        if (!MinecraftUtils.isOnWynncraft()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.currentScreen instanceof HandledScreen<?>) return;

        List<ItemStack> inventory = snapshotCurrentPlayerInventory(mc.player);
        List<ItemStack> armor = snapshotCurrentPlayerArmor(mc.player);
        if (sameItemLists(CharacterBankData.INSTANCE.getPlayerInventory(), inventory)
                && sameItemLists(CharacterBankData.INSTANCE.getPlayerArmor(), armor)) {
            return;
        }

        CharacterBankData.INSTANCE.setPlayerInventorySnapshot(inventory, armor);
        CharacterBankData.INSTANCE.saveAsyncDebounced();
    }

    private static List<ItemStack> snapshotCurrentPlayerInventory(PlayerEntity player) {
        List<ItemStack> items = new ArrayList<>(36);
        List<Slot> slots = BankOverlay.playerInvSlots;
        if (!slots.isEmpty() && slots.size() >= 36) {
            for (int i = 0; i < 36; i++) {
                items.add(copyStack(slots.get(i).getStack()));
            }
            return items;
        }

        List<ItemStack> mainStacks = player.getInventory().getMainStacks();
        for (int i = 9; i < 36; i++) {
            items.add(i < mainStacks.size() ? copyStack(mainStacks.get(i)) : Items.AIR.getDefaultStack());
        }
        for (int i = 0; i < 9; i++) {
            items.add(i < mainStacks.size() ? copyStack(mainStacks.get(i)) : Items.AIR.getDefaultStack());
        }
        return items;
    }

    private static List<ItemStack> snapshotCurrentPlayerArmor(PlayerEntity player) {
        List<ItemStack> armor = new ArrayList<>(4);
        for (EquipmentSlot slot : ARMOR_DISPLAY_ORDER) {
            armor.add(copyStack(player.getEquippedStack(slot)));
        }
        return armor;
    }

    private static ItemStack copyStack(ItemStack stack) {
        return stack == null ? Items.AIR.getDefaultStack() : stack.copy();
    }

    private static boolean sameItemLists(List<ItemStack> left, List<ItemStack> right) {
        if (left == null) left = Collections.emptyList();
        if (right == null) right = Collections.emptyList();
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            ItemStack a = left.get(i);
            ItemStack b = right.get(i);
            if (a == null) a = Items.AIR.getDefaultStack();
            if (b == null) b = Items.AIR.getDefaultStack();
            if (a.getCount() != b.getCount()) return false;
            if (!ItemStack.areItemsAndComponentsEqual(a, b)) return false;
        }
        return true;
    }

    private static void setAllCharactersBrowseMode(boolean enabled) {
        allCharactersBrowseMode = enabled;
        if (WynnExtrasConfig.INSTANCE.bankAllCharactersBrowseMode != enabled) {
            WynnExtrasConfig.INSTANCE.bankAllCharactersBrowseMode = enabled;
            WynnExtrasConfig.save();
        }
    }

    private static boolean shouldShowBankSwitchButton() {
        if (readOnlyViewerActive) return currentOverlayType != BankOverlayType.NONE;
        if (currentOverlayType != BankOverlayType.ACCOUNT && currentOverlayType != BankOverlayType.CHARACTER) return false;
        BankOverlayType targetType = currentOverlayType == BankOverlayType.ACCOUNT
                ? BankOverlayType.CHARACTER
                : BankOverlayType.ACCOUNT;
        return hasBankSwitchSlotForTarget(targetType);
    }

    private static boolean shouldAllowCrossClassMode() {
        return !currentClassAccountBankUnavailable || WynnExtrasConfig.INSTANCE.allowAllCharactersModeOnIronmanClasses;
    }

    private static void detectCurrentClassAccountBankAvailabilityIfNeeded() {
        if (currentClassAccountBankAvailabilityDetected) return;
        if (currentOverlayType != BankOverlayType.CHARACTER) {
            currentClassAccountBankUnavailable = false;
            currentClassAccountBankAvailabilityDetected = true;
            return;
        }
        if (System.currentTimeMillis() < suppressAccountBankAvailabilityDetectionUntil) {
            currentClassAccountBankUnavailable = false;
            currentClassAccountBankAvailabilityDetected = true;
            return;
        }
        currentClassAccountBankUnavailable = !hasBankSwitchSlotForTarget(BankOverlayType.ACCOUNT);
        currentClassAccountBankAvailabilityDetected = true;
    }

    private static void suppressAccountBankAvailabilityDetectionAfterSwitch() {
        suppressAccountBankAvailabilityDetectionUntil = System.currentTimeMillis() + 2000L;
    }

    private static boolean hasBankSwitchSlotForTarget(BankOverlayType targetType) {
        ScreenHandler handler = MinecraftUtils.containerMenu();
        if (handler == null || handler.slots.size() <= 47) return false;
        Slot slot = handler.getSlot(47);
        if (slot == null || !slot.hasStack() || slot.getStack() == null || slot.getStack().isEmpty()) return false;

        return stackContainsText(slot.getStack(), "storage type");
    }

    private static boolean stackContainsText(ItemStack stack, String text) {
        if (stack == null || stack.isEmpty() || text == null || text.isEmpty()) return false;
        String expectedText = text.toLowerCase(Locale.ROOT);
        if (cleanStackText(stack.getName().getString()).contains(expectedText)) return true;
        if (stack.getCustomName() != null && cleanStackText(stack.getCustomName().getString()).contains(expectedText)) return true;

        if (stack.getComponents() != null && stack.getComponents().get(DataComponentTypes.LORE) != null) {
            for (Text line : stack.getComponents().get(DataComponentTypes.LORE).lines()) {
                if (cleanStackText(line.getString()).contains(expectedText)) return true;
            }
        }

        try {
            PlayerEntity player = MinecraftClient.getInstance().player;
            for (Text line : stack.getTooltip(Item.TooltipContext.DEFAULT, player, TooltipType.BASIC)) {
                if (cleanStackText(line.getString()).contains(expectedText)) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private static String cleanStackText(String text) {
        if (text == null) return "";
        return MINECRAFT_FORMATTING_CODE_PATTERN.matcher(text)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void invalidateLocalCrossClassPageCaches() {
        accountLocalCrossClassPageCache = null;
        currentCharacterLocalCrossClassPageCache = null;
    }

    private static void clearCrossClassSearchState() {
        crossClassPages.clear();
        invalidateLocalCrossClassPageCaches();
        lastCrossClassSearchQuery = "";
        activeCrossClassSearchKey = "";
        crossClassSearchActive = false;
        crossClassSearchLoading = false;
        crossClassSearchQueued = false;
        queuedCrossClassSearchKey = "";
        queuedCrossClassSearchInput = "";
        queuedCrossClassIncludeCurrentCharacter = false;
        queuedCrossClassIncludeAccountBank = false;
        queuedCrossClassAllPages = false;
        queuedCrossClassSearchAt = 0L;
        completedCrossClassSearch = null;
        crossClassSearchGeneration++;
        cancelPendingCrossClassSearch();
    }

    private static void cancelPendingCrossClassSearch() {
        if (pendingCrossClassSearchTask != null && !pendingCrossClassSearchTask.isDone()) {
            pendingCrossClassSearchTask.cancel(true);
        }
        if (pendingCrossClassSearch != null && !pendingCrossClassSearch.isDone()) {
            pendingCrossClassSearch.cancel(true);
        }
        pendingCrossClassSearchTask = null;
        pendingCrossClassSearch = null;
    }

    private static void queueCrossClassSearch(String cacheKey, String searchInput, boolean includeCurrentCharacter, boolean includeAccountBank, boolean allPages) {
        crossClassSearchGeneration++;
        activeCrossClassSearchKey = cacheKey;
        completedCrossClassSearch = null;
        crossClassSearchLoading = true;
        crossClassSearchQueued = true;
        queuedCrossClassSearchKey = cacheKey;
        queuedCrossClassSearchInput = searchInput == null ? "" : searchInput;
        queuedCrossClassIncludeCurrentCharacter = includeCurrentCharacter;
        queuedCrossClassIncludeAccountBank = includeAccountBank;
        queuedCrossClassAllPages = allPages;
        queuedCrossClassSearchAt = System.currentTimeMillis() + (allPages ? 0 : CROSS_CLASS_SEARCH_DEBOUNCE_MS);
        cancelPendingCrossClassSearch();
    }

    private static void startQueuedCrossClassSearchIfReady() {
        if (!crossClassSearchQueued) return;
        if (System.currentTimeMillis() < queuedCrossClassSearchAt) return;

        String cacheKey = queuedCrossClassSearchKey;
        String searchInput = queuedCrossClassSearchInput;
        boolean includeCurrentCharacter = queuedCrossClassIncludeCurrentCharacter;
        boolean includeAccountBank = queuedCrossClassIncludeAccountBank;
        boolean allPages = queuedCrossClassAllPages;
        crossClassSearchQueued = false;
        startCrossClassSearch(cacheKey, searchInput, includeCurrentCharacter, includeAccountBank, allPages);
    }

    private static void startCrossClassSearch(String cacheKey, String searchInput, boolean includeCurrentCharacter, boolean includeAccountBank, boolean allPages) {
        cancelPendingCrossClassSearch();

        int generation = ++crossClassSearchGeneration;
        activeCrossClassSearchKey = cacheKey;
        completedCrossClassSearch = null;
        crossClassSearchLoading = true;

        String query = searchInput == null ? "" : searchInput;
        saveActivePageSnapshot();
        if (!Objects.equals(activeSearchInput, query)) {
            activeSearchInput = query;
            activeSearchQuery = SearchQueryParser.parse(activeSearchInput);
        }
        CrossClassBankSearch.SearchRequest request = CrossClassBankSearch.createRequest(
                query,
                includeCurrentCharacter,
                includeAccountBank,
                query.isEmpty()
        );

        pendingCrossClassSearchTask = CrossClassBankSearch.searchAsync(request);
        pendingCrossClassSearch = pendingCrossClassSearchTask.handle((results, throwable) -> {
            Throwable error = unwrapCompletionError(throwable);
            List<CrossClassBankSearch.SearchResult> safeResults = results == null
                    ? Collections.emptyList()
                    : List.copyOf(results);
            CrossClassSearchPayload payload = new CrossClassSearchPayload(cacheKey, generation, safeResults, error);
            if (generation == crossClassSearchGeneration && Objects.equals(cacheKey, activeCrossClassSearchKey)) {
                completedCrossClassSearch = payload;
            }
            return payload;
        });
    }

    private static Throwable unwrapCompletionError(Throwable throwable) {
        if (throwable == null) return null;
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private void applyCompletedCrossClassSearch(int yStart) {
        CrossClassSearchPayload payload = completedCrossClassSearch;
        if (payload == null) return;
        if (payload.generation() != crossClassSearchGeneration || !Objects.equals(payload.cacheKey(), activeCrossClassSearchKey)) {
            return;
        }

        completedCrossClassSearch = null;
        pendingCrossClassSearchTask = null;
        pendingCrossClassSearch = null;
        crossClassSearchLoading = false;
        crossClassPages.clear();

        if (payload.error() != null && !(payload.error() instanceof CancellationException)) {
            WynnExtras.LOGGER.error("[WynnExtras] Error searching cross-class banks: " + payload.error().getMessage());
            return;
        }

        int bottomBorder = (int) (yStart + (yFitAmount) * (90 + 4 + 10) * Math.max(2, ui.getScaleFactor()));
        for (CrossClassBankSearch.SearchResult result : payload.results()) {
            if (!shouldShowCrossClassResult(result)) continue;

            CrossClassPageWidget ccPage = new CrossClassPageWidget(
                    result.characterId,
                    result.characterNickname,
                    result.characterLevel,
                    result.pageNumber,
                    result.pageItems,
                    result.armorItems,
                    result.type,
                    yStart,
                    bottomBorder
            );
            crossClassPages.add(ccPage);
        }
    }

    private static boolean shouldShowCrossClassResult(CrossClassBankSearch.SearchResult result) {
        if (result == null) return false;
        if ("__account__".equals(result.characterId) && currentOverlayType == BankOverlayType.ACCOUNT) return false;
        return switch (result.type) {
            case MISC_BUCKET -> currentOverlayType != BankOverlayType.MISC;
            case TOME_BOOKSHELF -> currentOverlayType != BankOverlayType.BOOKSHELF;
            default -> true;
        };
    }

    private static int startNextRow(int visualIndex) {
        if (xFitAmount <= 0) return visualIndex;
        int remainder = visualIndex % xFitAmount;
        return remainder == 0 ? visualIndex : visualIndex + (xFitAmount - remainder);
    }

    private int drawCrossClassPages(DrawContext context, int mouseX, int mouseY, float delta, int xStart, int yStart, int visualIndex, int extraYOffset, List<CrossClassPageWidget> pagesToDraw) {
        for (CrossClassPageWidget ccPage : pagesToDraw) {
            float invX = xStart + (visualIndex % xFitAmount) * (162 + 4);
            float invY = yStart + Math.floorDiv(visualIndex, xFitAmount) * (90 + 4 + 10) + extraYOffset - actualOffset;
            ccPage.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
            renderedCrossClassPages.add(ccPage);
            if (pageIntersectsClip(invY, 92, true)) {
                ccPage.draw(context, mouseX, mouseY, delta, ui);
            }
            visualIndex++;
        }
        return visualIndex;
    }

    private List<CrossClassPageWidget> buildCachedAccountCrossClassPages(String searchInput, int yStart) {
        BankData accountData = AccountBankData.INSTANCE;
        if (accountData == null || accountData.getBankPages() == null) return Collections.emptyList();

        return buildCachedBankCrossClassPages(
                accountData,
                "__account__",
                "Account Bank",
                0,
                ACCOUNT_BANK_MAX_PAGES,
                searchInput,
                yStart,
                true);
    }

    private List<CrossClassPageWidget> buildCachedCurrentCharacterCrossClassPages(String searchInput, int yStart) {
        if (!BankOverlay.hasValidCurrentCharacterId()) return Collections.emptyList();
        BankData characterData = CharacterBankData.INSTANCE;
        if (characterData == null || characterData.getBankPages() == null) return Collections.emptyList();

        String nickname = characterData.getCharacterNickname();
        if (nickname == null || nickname.isEmpty()) nickname = BankOverlay.currentCharacterID;
        return buildCachedBankCrossClassPages(
                characterData,
                BankOverlay.currentCharacterID,
                nickname,
                characterData.getCharacterLevel(),
                BankOverlay.getCurrentMaxPages(),
                searchInput,
                yStart,
                false);
    }

    private List<CrossClassPageWidget> buildCachedBankCrossClassPages(BankData data, String characterId, String displayName, int characterLevel, int maxPages, String searchInput, int yStart, boolean accountCache) {
        String queryText = searchInput == null ? "" : searchInput;
        boolean searching = !queryText.isEmpty();
        SearchQueryParser.ParsedQuery query = searching ? SearchQueryParser.parse(queryText) : null;
        int pageCount = Math.min(Math.max(data.getLastPage() + (searching ? 0 : 1), data.getBankPages().size()), maxPages);
        int bottomBorder = (int) (yStart + (yFitAmount) * (90 + 4 + 10) * Math.max(2, ui.getScaleFactor()));
        LocalCrossClassPageCache cache = accountCache ? accountLocalCrossClassPageCache : currentCharacterLocalCrossClassPageCache;
        if (cache != null && cache.matches(data, characterId, displayName, characterLevel, maxPages, pageCount, queryText, yStart, bottomBorder, ui.getScaleFactorF())) {
            return cache.pages();
        }

        List<CrossClassPageWidget> cachedPages = new ArrayList<>();

        for (int pageNum = 0; pageNum < pageCount; pageNum++) {
            List<ItemStack> pageItems = data.getBankPages().get(pageNum);
            boolean pagePlaceholder = !searching && pageItems == null && pageNum >= data.getLastPage();
            if (pageItems == null) pageItems = Collections.emptyList();

            if (searching && !pageContainsSearch(pageItems, query)) continue;

            CrossClassPageWidget pageWidget = new CrossClassPageWidget(
                    characterId,
                    displayName,
                    characterLevel,
                    pageNum,
                    pageItems,
                    EMPTY_PLAYER_ARMOR,
                    CrossClassBankSearch.SearchResult.Type.BANK_PAGE,
                    yStart,
                    bottomBorder
            );
            if (pagePlaceholder) {
                boolean rankLocked = data == currentData && currentOverlayType == rankLockedOverlayType && rankLockedPageCount == data.getLastPage();
                pageWidget.setPagePlaceholder(rankLocked ? rankLockedRequiredRank : null);
            }
            cachedPages.add(pageWidget);
        }
        LocalCrossClassPageCache newCache = new LocalCrossClassPageCache(
                data,
                characterId,
                displayName,
                characterLevel,
                maxPages,
                pageCount,
                data.getBankPages().size(),
                data.getLastPage(),
                queryText,
                yStart,
                bottomBorder,
                yFitAmount,
                ui.getScaleFactorF(),
                cachedPages);
        if (accountCache) {
            accountLocalCrossClassPageCache = newCache;
        } else {
            currentCharacterLocalCrossClassPageCache = newCache;
        }
        return cachedPages;
    }

    private static boolean pageContainsSearch(List<ItemStack> pageItems, SearchQueryParser.ParsedQuery query) {
        for (ItemStack stack : pageItems) {
            if (stack == null || stack.isEmpty()) continue;
            WynnItemData wynnItem = WynnItemParser.parse(stack).orElse(null);
            if (SearchQueryParser.matches(stack, wynnItem, query)) return true;
        }
        return false;
    }

    private void drawCrossClassSearchLoading(int xStart, int yStart) {
        int dots = (int) ((System.currentTimeMillis() / 350) % 3) + 1;
        float centerX = xStart + xFitAmount * (162 + 4) / 2f;
        float centerY = yStart + Math.max(40, (yFitAmount - 1) * 104 / 2f);
        ui.drawCenteredText("Loading" + ".".repeat(dots), centerX, centerY, WHITE_TEXT_COLOR, 1.3f);
    }

    private void drawCrossClassSearchEmpty(int xStart, int yStart) {
        float centerX = xStart + xFitAmount * (162 + 4) / 2f;
        float centerY = yStart + Math.max(40, (yFitAmount - 1) * 104 / 2f);
        ui.drawCenteredText("No results found", centerX, centerY, GRAY_TEXT_COLOR, 1.3f);
    }

    private static void preserveCurrentSearchForNextOverlay() {
        if (searchbar2 == null || searchbar2.getInput() == null || searchbar2.getInput().isEmpty()) return;
        savedCrossClassSearch = searchbar2.getInput().replace("@", "").trim();
        savedCrossClassSearchTime = System.currentTimeMillis();
    }

    private static void switchBankAndJumpToPage(BankOverlayType targetType, int pageIndex) {
        switchBankAndJumpToPage(targetType, pageIndex, false);
    }

    private static void switchBankAndJumpToPage(BankOverlayType targetType, int pageIndex, boolean adjustTargetScroll) {
        if (isJumpInProgress()) return;
        if (currentOverlayType == targetType) {
            PageWidget.jumpToPage(pageIndex);
            return;
        }
        if (!hasBankSwitchSlotForTarget(targetType)) return;

        storeActivePageSnapshot();
        preserveCurrentSearchForNextOverlay();
        clearHoverState(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> handledScreen ? handledScreen : null);
        preserveScrollOnNextOverlay = true;
        preservedActualOffset = actualOffset;
        preservedTargetOffset = adjustTargetScroll ? getSwitchTargetOffset(targetType) : targetOffset;
        deferPreservedScrollClamp = !adjustTargetScroll && (allCharactersBrowseMode || crossClassSearchActive);
        bankTypeSwitchInProgress = true;
        bankTypeSwitchTargetApplied = false;
        bankTypeSwitchTargetType = targetType;
        bankTypeSwitchTargetPage = pageIndex;

        if (!BankOverlay.isCharacterBankMissingCharacterId() && currentData != null) currentData.saveAsyncDebounced();
        if (!BankOverlay.isCharacterBankMissingCharacterId() && Pages != null) Pages.saveAsyncDebounced();

        expectedOverlayType = targetType;

        ScreenHandler handler = MinecraftUtils.containerMenu();
        if (handler == null) {
            resetBankTypeSwitchState();
            expectedOverlayType = BankOverlayType.NONE;
            return;
        }
        suppressAccountBankAvailabilityDetectionAfterSwitch();
        clickOnSlot(47, handler.syncId, 0, handler.getStacks());
        BankOverlay.resetScrollRegistration();

        julianh06.wynnextras.utils.TickScheduler.runWhen(
                () -> currentOverlayType == targetType && expectedOverlayType == BankOverlayType.NONE,
                () -> {
                    pages.clear();
                    heldItem = Items.AIR.getDefaultStack();
                    BankOverlay.activeInvSlots.clear();
                    annotationCache.clear();
                    annotationStackCache.clear();
                    annotationComponentCache.clear();
                    PageWidget.jumpToPage(pageIndex);
                    julianh06.wynnextras.utils.TickScheduler.runWhen(
                            () -> {
                                finishBankTypeSwitchIfReady(targetType, pageIndex);
                                return !bankTypeSwitchInProgress;
                            },
                            () -> {}
                    );
                    julianh06.wynnextras.utils.TickScheduler.runAfterTicks(80, () -> forceFinishBankTypeSwitch(targetType));
                }
        );
    }

    private static float getSwitchTargetOffset(BankOverlayType targetType) {
        if (targetType == BankOverlayType.ACCOUNT || !allCharactersBrowseMode || xFitAmount <= 0) return 0;

        BankData accountData = AccountBankData.INSTANCE;
        if (accountData == null || accountData.getBankPages() == null) return 0;
        int accountPageCount = Math.min(
                Math.max(accountData.getLastPage() + 1, accountData.getBankPages().size()),
                ACCOUNT_BANK_MAX_PAGES);
        return Math.floorDiv(accountPageCount, xFitAmount) * 104f;
    }

    private boolean shouldDeferMouseTweaksRightClick(double x, double y, int button) {
        if (!MOUSE_TWEAKS_LOADED || button != 1 || screen == null) return false;
        ScreenHandler handler = screen.getScreenHandler();
        return handler != null && !handler.getCursorStack().isEmpty() && BankOverlaySlotBridge.getExposedSlotAt(screen, x, y) != null;
    }

    private SlotWidget getLiveSlotWidgetAt(double x, double y) {
        if (inventoryWidget != null) {
            for (int i = inventoryWidget.slots.size() - 1; i >= 0; i--) {
                SlotWidget slot = inventoryWidget.slots.get(i);
                if (slot.isVisible() && slot.isEnabled() && slot.contains((int) x, (int) y)) return slot;
            }
        }

        if (activeInv >= 0 && activeInv < pages.size()) {
            PageWidget page = pages.get(activeInv);
            for (int i = page.slots.size() - 1; i >= 0; i--) {
                SlotWidget slot = page.slots.get(i);
                if (slot.isVisible() && slot.isEnabled() && slot.contains((int) x, (int) y)) return slot;
            }
        }

        return null;
    }

    private boolean deferMouseTweaksRightClickIfNeeded(double x, double y, int button) {
        if (!shouldDeferMouseTweaksRightClick(x, y, button)) return false;

        SlotWidget slotWidget = getLiveSlotWidgetAt(x, y);
        Slot backingSlot = BankOverlaySlotBridge.getExposedSlotAt(screen, x, y);
        if (slotWidget == null || backingSlot == null) return false;

        pendingMouseTweaksRightClick = new PendingRightClick(slotWidget, backingSlot, x, y);
        return true;
    }

    private void updatePendingMouseTweaksRightClick(double x, double y) {
        if (pendingMouseTweaksRightClick == null || screen == null) return;

        Slot currentSlot = BankOverlaySlotBridge.getExposedSlotAt(screen, x, y);
        if (currentSlot != pendingMouseTweaksRightClick.backingSlot()) {
            pendingMouseTweaksRightClick = null;
        }
    }

    private boolean releasePendingMouseTweaksRightClick(double x, double y, int button) {
        if (button != 1 || pendingMouseTweaksRightClick == null) return false;

        PendingRightClick pending = pendingMouseTweaksRightClick;
        pendingMouseTweaksRightClick = null;

        double movedX = Math.abs(x - pending.startX());
        double movedY = Math.abs(y - pending.startY());
        if (screen == null || movedX > 4 || movedY > 4 || BankOverlaySlotBridge.getExposedSlotAt(screen, x, y) != pending.backingSlot()) {
            return true;
        }

        pending.slotWidget().clickLiveSlot(button);
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return mouseClicked(x, y, button, false);
    }

    public boolean readOnlyMouseClicked(double x, double y, int button) {
        if (!readOnlyViewer) return false;
        clearTextInputFocus();
        if (searchbar2 != null && searchbar2.mouseClicked(x, y, button)) return true;
        if (scrollBarWidget != null && scrollBarWidget.mouseClicked(x, y, button)) return true;
        if (allCharactersButtonWidget != null && allCharactersButtonWidget.mouseClicked(x, y, button)) return true;
        if (switchButtonWidget != null && switchButtonWidget.mouseClicked(x, y, button)) return true;
        if (reloadBankWidget != null && reloadBankWidget.contains((int) x, (int) y)) return true;
        if (quickActionWidget != null && quickActionWidget.contains((int) x, (int) y)) return true;
        return isPointInsidePageClip(x, y)
                || inventoryWidget != null && inventoryWidget.contains((int) x, (int) y);
    }

    public boolean readOnlyMouseReleased(double x, double y, int button) {
        if (!readOnlyViewer) return false;
        if (scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        if (searchbar2 != null) searchbar2.mouseReleased(x, y, button);
        return true;
    }

    public boolean readOnlyMouseDragged(double x, double y, int button, double dx, double dy) {
        return readOnlyViewer && searchbar2 != null && searchbar2.mouseDragged(x, y, button, dx, dy);
    }

    public void scrollReadOnlyViewer(float direction) {
        if (!readOnlyViewer) return;
        adjustTargetOffset(direction > 0 ? -104f : 104f);
    }

    public boolean mouseClicked(double x, double y, int button, boolean doubleClick) {
        boolean inBank = WynncraftMenuService.isCurrentAny(MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK,
                MenuType.BOOKSHELF, MenuType.MISC_BUCKET);

        clearTextInputFocus();

        if(toggleOverlayWidget != null && WynnExtrasConfig.INSTANCE.bankQuickToggle && inBank
                && toggleOverlayWidget.contains((int) x, (int) y)) {
            toggleOverlayWidget.mouseClicked(x, y, button);
            return true;
        }

        if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) return false;
        if (currentOverlayType == BankOverlayType.NONE) return false;

        // Check UI controls first (so they don't get stolen by overlapping cross-class pages)
        if(searchbar2 != null && searchbar2.mouseClicked(x, y, button)) return true;
        if(scrollBarWidget != null && scrollBarWidget.mouseClicked(x, y, button)) return true;
        if(allCharactersButtonWidget != null && allCharactersButtonWidget.mouseClicked(x, y, button)) return true;
        if(reloadBankWidget != null && reloadBankWidget.mouseClicked(x, y, button)) return true;
        if(switchButtonWidget != null && switchButtonWidget.mouseClicked(x, y, button)) return true;
        if(quickActionWidget != null && quickActionWidget.mouseClicked(x, y, button)) return true;

        if (deferMouseTweaksRightClickIfNeeded(x, y, button)) return true;

        boolean characterBankUnavailable = BankOverlay.isCharacterBankMissingCharacterId();
        if (!characterBankUnavailable) {
            int regularPageCount = getRenderableRegularPageCount();
            for(int i = 0; i < regularPageCount; i++) {
                if (pages.get(i).mouseClicked(x, y, button, doubleClick)) return true;
            }
        }
        // Handle clicks on cross-class search results
        if (isPointInsidePageClip(x, y)) {
            List<CrossClassPageWidget> clickableCrossClassPages = renderedCrossClassPages.isEmpty() ? crossClassPages : renderedCrossClassPages;
            for(CrossClassPageWidget ccPage : clickableCrossClassPages) {
                if (ccPage.mouseClicked(x, y, button)) {
                    return true;
                }
            }
        }
        if (characterBankUnavailable) {
            if (dropHeldItemOutsideBankArea(x, y, button)) return true;
            return true;
        }
        if(inventoryWidget != null && inventoryWidget.mouseClicked(x, y, button, doubleClick)) return true;
        if (dropHeldItemOutsideBankArea(x, y, button)) return true;
        return false;
    }

    private static void clearTextInputFocus() {
        if (searchbar2 != null) searchbar2.setFocused(false);
        for (PageWidget page : pages) {
            page.clearNameInputFocus();
        }
    }

    private boolean dropHeldItemOutsideBankArea(double x, double y, int button) {
        if (!hasHeldItem() || button != 0 && button != 1 || isPointInsideBankArea(x, y)) return false;

        ScreenHandler liveHandler = getLiveScreenHandlerForClick();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (liveHandler == null || mc.interactionManager == null || mc.player == null) return false;

        mc.interactionManager.clickSlot(liveHandler.syncId, -999, button, SlotActionType.PICKUP, mc.player);
        syncHeldItemFromLiveHandler(liveHandler);
        bankSyncid = liveHandler.syncId;
        return true;
    }

    private boolean isPointInsideBankArea(double x, double y) {
        int xStart = layoutXRemain / 2 - 2;
        int yStart = layoutYRemain / 2 - 2;
        int pageAreaX = xStart - 7;
        int pageAreaY = yStart - 13;
        int pageAreaWidth = xFitAmount * (162 + 4) + 11;
        int pageAreaHeight = (yFitAmount - 1) * (90 + 4 + 10) + 10;
        if (x >= pageAreaX && x < pageAreaX + pageAreaWidth
                && y >= pageAreaY && y < pageAreaY + pageAreaHeight) return true;

        return contains(inventoryWidget, x, y)
                || contains(searchbar2, x, y)
                || contains(scrollBarWidget, x, y)
                || contains(allCharactersButtonWidget, x, y)
                || contains(reloadBankWidget, x, y)
                || contains(switchButtonWidget, x, y)
                || contains(quickActionWidget, x, y)
                || contains(toggleOverlayWidget, x, y);
    }

    private static boolean contains(Widget widget, double x, double y) {
        return widget != null && widget.contains((int) x, (int) y);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if(scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        if (finishDragSplitting(x, y, button)) return true;
        if (releasePendingMouseTweaksRightClick(x, y, button)) return true;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 1) updatePendingMouseTweaksRightClick(x, y);
        if (dragSplitting && button == dragSplittingButton) {
            addDragSplittingSlotAt(x, y);
            return true;
        }
        if(searchbar2 != null && searchbar2.mouseDragged(x, y, button, dx, dy)) return true;
        for(PageWidget page : pages) {
            if(page.mouseDragged(x, y, button, dx, dy)) return true;
        }
        for(CrossClassPageWidget page : crossClassPages) {
            if(page.mouseDragged(x, y, button, dx, dy)) return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    private static void resetDragSplitting() {
        dragSplitting = false;
        dragSplittingButton = -1;
        dragSplittingSlots.clear();
        dragSplittingFallbackSlot = null;
    }

    private static boolean beginDragSplitting(SlotWidget slotWidget, int button) {
        if (button != 0 && button != 1) return false;
        if (!hasHeldItem()) return false;
        if (slotWidget == null || !slotWidget.canUseLiveSlot()) return false;
        if (getLiveScreenHandlerForClick() == null) return false;

        dragSplitting = true;
        dragSplittingButton = button;
        dragSplittingSlots.clear();
        dragSplittingFallbackSlot = slotWidget;
        return true;
    }

    private static void addDragSplittingSlotAt(double x, double y) {
        SlotWidget slotWidget = getLiveSlotWidgetAtStatic(x, y);
        if (slotWidget == null || !slotWidget.canUseLiveSlot()) return;

        int slotIndex = slotWidget.getLiveSlotIndex();
        ScreenHandler liveHandler = getLiveScreenHandlerForClick();
        if (liveHandler == null || slotIndex < 0 || slotIndex >= liveHandler.slots.size()) return;

        Slot liveSlot = liveHandler.slots.get(slotIndex);
        if (!canDragSplitIntoSlot(liveSlot)) return;
        dragSplittingSlots.add(slotIndex);
    }

    private static boolean finishDragSplitting(double x, double y, int button) {
        if (!dragSplitting) return false;
        if (button != dragSplittingButton) {
            resetDragSplitting();
            return true;
        }

        ScreenHandler liveHandler = getLiveScreenHandlerForClick();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (liveHandler != null && mc.interactionManager != null && mc.player != null && !dragSplittingSlots.isEmpty()) {
            int syncId = liveHandler.syncId;
            mc.interactionManager.clickSlot(syncId, -999, ScreenHandler.packQuickCraftData(0, dragSplittingButton), SlotActionType.QUICK_CRAFT, mc.player);
            for (int slotIndex : dragSplittingSlots) {
                if (slotIndex >= 0 && slotIndex < liveHandler.slots.size()) {
                    mc.interactionManager.clickSlot(syncId, slotIndex, ScreenHandler.packQuickCraftData(1, dragSplittingButton), SlotActionType.QUICK_CRAFT, mc.player);
                }
            }
            mc.interactionManager.clickSlot(syncId, -999, ScreenHandler.packQuickCraftData(2, dragSplittingButton), SlotActionType.QUICK_CRAFT, mc.player);
            syncHeldItemFromLiveHandler(liveHandler);
            bankSyncid = syncId;
        } else {
            SlotWidget releaseSlot = getLiveSlotWidgetAtStatic(x, y);
            SlotWidget slotToClick = releaseSlot != null ? releaseSlot : dragSplittingFallbackSlot;
            if (slotToClick != null) slotToClick.clickLiveSlot(button);
        }

        resetDragSplitting();
        return true;
    }

    private static SlotWidget getLiveSlotWidgetAtStatic(double x, double y) {
        if (inventoryWidget != null) {
            for (int i = inventoryWidget.slots.size() - 1; i >= 0; i--) {
                SlotWidget slot = inventoryWidget.slots.get(i);
                if (slot.isVisible() && slot.isEnabled() && slot.contains((int) x, (int) y)) return slot;
            }
        }

        if (activeInv >= 0 && activeInv < pages.size()) {
            PageWidget page = pages.get(activeInv);
            for (int i = page.slots.size() - 1; i >= 0; i--) {
                SlotWidget slot = page.slots.get(i);
                if (slot.isVisible() && slot.isEnabled() && slot.contains((int) x, (int) y)) return slot;
            }
        }

        return null;
    }

    private static void syncHeldItemFromLiveHandler(ScreenHandler liveHandler) {
        if (liveHandler == null) return;
        ItemStack cursorStack = liveHandler.getCursorStack();
        heldItem = cursorStack.isEmpty() ? Items.AIR.getDefaultStack() : cursorStack.copy();
    }

    private static ItemStack getDragSplittingPreviewStack(int liveSlotIndex) {
        if (!dragSplitting || !dragSplittingSlots.contains(liveSlotIndex) || heldItem == null || heldItem.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ScreenHandler liveHandler = getLiveScreenHandlerForClick();
        if (liveHandler == null || liveSlotIndex < 0 || liveSlotIndex >= liveHandler.slots.size()) return ItemStack.EMPTY;

        Slot liveSlot = liveHandler.slots.get(liveSlotIndex);
        if (!canDragSplitIntoSlot(liveSlot)) return ItemStack.EMPTY;

        Set<Slot> liveDragSplittingSlots = new LinkedHashSet<>();
        for (int slotIndex : dragSplittingSlots) {
            if (slotIndex >= 0 && slotIndex < liveHandler.slots.size()) {
                Slot slot = liveHandler.slots.get(slotIndex);
                if (canDragSplitIntoSlot(slot)) liveDragSplittingSlots.add(slot);
            }
        }
        if (liveDragSplittingSlots.size() <= 1) return ItemStack.EMPTY;

        ItemStack existing = liveSlot.getStack();
        int existingCount = existing.isEmpty() ? 0 : existing.getCount();
        int maxCount = Math.min(heldItem.getMaxCount(), liveSlot.getMaxItemCount(heldItem));
        int previewCount = ScreenHandler.calculateStackSize(liveDragSplittingSlots, dragSplittingButton, heldItem) + existingCount;
        previewCount = Math.min(previewCount, maxCount);
        if (previewCount <= existingCount) return ItemStack.EMPTY;

        ItemStack preview = heldItem.copy();
        preview.setCount(previewCount);
        return preview;
    }

    private static boolean canDragSplitIntoSlot(Slot slot) {
        if (slot == null || heldItem == null || heldItem.isEmpty()) return false;
        if (!slot.canInsert(heldItem)) return false;
        if (!ScreenHandler.canInsertItemIntoSlot(slot, heldItem, true)) return false;

        ItemStack existing = slot.getStack();
        if (!existing.isEmpty() && !ItemStack.areItemsAndComponentsEqual(existing, heldItem)) return false;

        int maxCount = Math.min(heldItem.getMaxCount(), slot.getMaxItemCount(heldItem));
        return existing.getCount() < maxCount;
    }

    private static int getDragSplittingRemainingCount() {
        if (!dragSplitting || heldItem == null || heldItem.isEmpty() || dragSplittingSlots.isEmpty()) {
            return heldItem == null ? 0 : heldItem.getCount();
        }

        ScreenHandler liveHandler = getLiveScreenHandlerForClick();
        if (liveHandler == null) return heldItem.getCount();

        Set<Slot> liveDragSplittingSlots = new LinkedHashSet<>();
        for (int slotIndex : dragSplittingSlots) {
            if (slotIndex >= 0 && slotIndex < liveHandler.slots.size()) {
                liveDragSplittingSlots.add(liveHandler.slots.get(slotIndex));
            }
        }

        int remaining = heldItem.getCount();
        for (Slot slot : liveDragSplittingSlots) {
            if (!canDragSplitIntoSlot(slot)) continue;

            int existingCount = slot.getStack().isEmpty() ? 0 : slot.getStack().getCount();
            int maxCount = Math.min(heldItem.getMaxCount(), slot.getMaxItemCount(heldItem));
            int previewCount = ScreenHandler.calculateStackSize(liveDragSplittingSlots, dragSplittingButton, heldItem) + existingCount;
            previewCount = Math.min(previewCount, maxCount);
            remaining -= Math.max(0, previewCount - existingCount);
        }

        return Math.max(0, remaining);
    }

    private void initializeOverlayState() {
        if (!initializedTypes.contains(currentOverlayType)) {
            initializedTypes.add(currentOverlayType);
        }

        if (Pages == null) Pages = currentData;

        if (!readOnlyViewer) WynntilsBankAdapter.setLastPage(BankOverlay.getPersonalStorageUtils(), 99);

        if (activeInv == -1) activeInv = 1;
    }

    private static void syncActivePageFromWynntilsQuickJump() {
        if (LunarCompat.isLunarClient()) return;
        if (!shouldShowWynntilsPageJumpButtons()) return;
        if (shouldWait || isReloading || bankTypeSwitchInProgress) return;

        int livePage = getCurrentBankPageNumber();
        if (livePage < 0 || livePage == activeInv) return;

        activeInv = livePage;
        retryLoad();
        storeActivePageSnapshot();
        scrollToPage(activeInv);
        clearAnnotationCache(activeInv);
    }

    private static void clearHoverState(HandledScreen<?> screen) {
        hoveredBackingSlot = null;
        WeightDisplay.setCurrentHoveredStack(null);
        hoveredInvIndex = -1;
        hoveredIndex = -1;
        hoveredX = -1;
        hoveredY = -1;
        hoveredSlot = Items.AIR.getDefaultStack();
        hoveredTooltipData = null;
        if (screen != null) {
            HandledScreenAccess.setFocusedSlot(screen, null);
        }
    }

    public static void suppressHoveredTooltip(HandledScreen<?> screen) {
        clearHoverState(screen);
    }

    private static void setHoveredSlot(ItemStack stack, int index, int inventoryIndex, int itemX, int itemY) {
        hoveredSlot = stack;
        hoveredIndex = index;
        hoveredInvIndex = inventoryIndex;
        hoveredX = itemX;
        hoveredY = itemY;
    }

    private void calculateLayout() {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        xFitAmount = Math.min(24, Math.floorDiv(screenWidth - 84, 162));
        yFitAmount = Math.min(24, Math.floorDiv(screenHeight, 104));

        xFitAmount = Math.min(xFitAmount, WynnExtrasConfig.INSTANCE.bankOverlayMaxColumns);
        yFitAmount = Math.min(yFitAmount, WynnExtrasConfig.INSTANCE.bankOverlayMaxRows + 1);

        if (currentData != null && currentData.getLastPage() > 0 && WynnExtrasConfig.INSTANCE.bankOverlayHideEmptyRows) {
            int totalPages = currentData.getLastPage();
            int rowsNeeded = (int) Math.ceil((double) totalPages / xFitAmount);

            if (rowsNeeded < yFitAmount) {
                yFitAmount = rowsNeeded + 1;
            }
        }

        int xRemain = screenWidth - xFitAmount * 162 - (xFitAmount - 1) * 4;
        if (xRemain < 0) {
            xFitAmount--;
            xRemain = screenWidth - xFitAmount * 162 - (xFitAmount - 1) * 4;
        }

        int yRemain = screenHeight - yFitAmount * 90 - (yFitAmount - 1) * 4;
        if (yRemain < 0) {
            yFitAmount--;
            yRemain = screenHeight - yFitAmount * 90 - (yFitAmount - 1) * 4;
        }

        layoutXRemain = xRemain;
        layoutYRemain = yRemain;
    }

    private List<ItemStack> buildInventoryForIndex(int index, boolean isPlayerInv) {
        if (readOnlyViewer) {
            if (isPlayerInv) {
                livePlayerInventoryItems.clear();
                for (ItemStack stack : CharacterBankData.INSTANCE.getPlayerInventory()) {
                    livePlayerInventoryItems.add(stack == null ? ItemStack.EMPTY : stack.copy());
                }
                return livePlayerInventoryItems;
            }
            if (Pages == null) return EMPTY_BANK_PAGE;
            List<ItemStack> cached = Pages.getBankPages().get(index);
            return cached == null ? EMPTY_BANK_PAGE : cached;
        }
        if(isPlayerInv) {
            List<Slot> slots = BankOverlay.playerInvSlots;
            if (slots != null && slots.size() >= 36) {
                livePlayerInventoryItems.clear();
                for (int j = 0; j < 36; j++) livePlayerInventoryItems.add(slots.get(j).getStack());
                return livePlayerInventoryItems;
            } else {
                return EMPTY_PLAYER_INVENTORY;
            }
        }

        if (index == activeInv) {
            liveBankPageItems.clear();
            List<Slot> slots = BankOverlay.activeInvSlots;
            if (slots.size() < 45) {
                retryLoad();
                return liveBankPageItems;
            }
            boolean oldShouldWait = shouldWait;
            shouldWait = false;

            for (int j = 0; j < 45; j++) {
                if (j == 0) {
                    ItemStack rightArrow;
                    try {
                        rightArrow = MinecraftUtils.containerMenu().getSlot(52).getStack();
                    } catch (IndexOutOfBoundsException e) {
                        retryLoad();
                        activeInv = -1;
                        close.apply(null);
                        return EMPTY_BANK_PAGE;
                    }
                    if(rightArrow == null) return EMPTY_BANK_PAGE;
                    if(rightArrow.getItem() == Items.POTION) {
                        String rawText = rightArrow.getName().getString();
                        String cleanedText = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(rawText).replaceAll("");
                        if (!cleanedText.contains("Page " + (activeInv + 2))) {
                            shouldWait = true;
                            if (!oldShouldWait) {
                                shouldWaitSince = System.currentTimeMillis();
                            }
                        } else if (oldShouldWait && !isReloading) {
                            storeActivePageSnapshot();
                            clearAnnotationCache(activeInv);
                        }
                    } else if(activeInv != currentData.getLastPage() - 1) {
                        if (!shouldWait) {
                            shouldWait = true;
                            shouldWaitSince = System.currentTimeMillis();
                        }
                    }
                }

                if (shouldWait) {
                    long waitDuration = System.currentTimeMillis() - shouldWaitSince;

                    if (waitDuration > 1500) {
                        shouldWaitSince = System.currentTimeMillis();
                        retryLoad();
                        WynntilsBankAdapter.setLastPage(BankOverlay.getPersonalStorageUtils(), 99);
                        jumpToBankPage(activeInv);
                    }
                    List<ItemStack> cached = Pages.getBankPages().get(activeInv);
                    if (cached != null && j < cached.size()) liveBankPageItems.add(cached.get(j));
                    continue;
                }

                liveBankPageItems.add(slots.get(j).getStack());
            }
            return liveBankPageItems;
        } else {
            List<ItemStack> cached = Pages.getBankPages().get(index);
            if (cached != null && cached.size() >= 45) {
                return cached.size() == 45 ? cached : cached.subList(0, 45);
            } else {
                return EMPTY_BANK_PAGE;
            }
        }
    }

    public static void retryLoad() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ScreenHandler currScreenHandler = MinecraftUtils.containerMenu();
        if (currScreenHandler == null) return;

        Inventory playerInv = client.player.getInventory();
        BankOverlay.playerInvSlots.clear();
        BankOverlay.activeInvSlots.clear();

        for (Slot slot : currScreenHandler.slots) {
            if (slot.inventory == playerInv) {
                BankOverlay.playerInvSlots.add(slot);
            } else {
                BankOverlay.activeInvSlots.add(slot);
            }
        }
    }

    private static void exposeBackingSlot(boolean isInventorySlot, int inventoryIndex, int slotIndex, int screenX, int screenY, boolean hovered) {
        if (bridgeScreen == null) return;
        if (!isInventorySlot && inventoryIndex != activeInv) return;

        Slot backingSlot = null;
        if (isInventorySlot) {
            if (slotIndex >= 0 && slotIndex < BankOverlay.playerInvSlots.size()) {
                backingSlot = BankOverlay.playerInvSlots.get(slotIndex);
            }
        } else if (slotIndex >= 0 && slotIndex < BankOverlay.activeInvSlots.size()) {
            backingSlot = BankOverlay.activeInvSlots.get(slotIndex);
        }

        if (backingSlot == null) return;

        BankOverlaySlotBridge.expose(bridgeScreen, backingSlot, screenX, screenY);
        if (hovered) {
            hoveredBackingSlot = backingSlot;
            HandledScreenAccess.setFocusedSlot(bridgeScreen, backingSlot);
        }
    }

    private static void clearAnnotationCache(int pageIndex) {
        List<WynntilsBankAdapter.AnnotationHandle> annotations = annotationCache.get(pageIndex);
        if (annotations != null) annotations.clear();
        annotationStackCache.remove(pageIndex);
        annotationComponentCache.remove(pageIndex);
    }

    private static <T> void ensureCacheSize(List<T> cache, int size, T value) {
        while (cache.size() < size) {
            cache.add(value);
        }
    }

    private static void applyAnnotation(ItemStack stack, List<WynntilsBankAdapter.AnnotationHandle> annotations, List<ItemStack> annotationStacks, List<Object> annotationComponents, int index) {
        if(annotations.size() <= index || annotationStacks.size() <= index || annotationComponents.size() <= index) return;

        if(stack == null || stack.getItem() == Items.AIR) {
            annotations.set(index, null);
            annotationStacks.set(index, stack);
            annotationComponents.set(index, null);
            return;
        }

        WynntilsBankAdapter.AnnotationHandle annotation = annotations.get(index);
        Object components = stack.getComponents();
        if(annotation == null || annotationStacks.get(index) != stack
                || !Objects.equals(annotationComponents.get(index), components)) {
            if (annotationCalculationsThisFrame >= WynnExtrasConfig.INSTANCE.maxAnnotationCalculationsPerFrame) {
                annotations.set(index, null);
                annotationStacks.set(index, null);
                annotationComponents.set(index, null);
                return;
            }
            annotationCalculationsThisFrame++;
            StyledText originalName = ensureWynntilsOriginalName(stack);
            annotation = WynntilsItemUiAdapter.calculateAnnotation(stack, originalName.getComponent())
                    .orElse(null);
            annotations.set(index, annotation);
            annotationStacks.set(index, stack);
            annotationComponents.set(index, components);
        }

        if (annotation != null) {
            if (!Objects.equals(WynntilsBankAdapter.getAnnotation(stack).orElse(null), annotation)) {
                WynntilsBankAdapter.setAnnotation(stack, annotation);
            }
        }
    }

    private static StyledText ensureWynntilsOriginalName(ItemStack stack) {
        if (isEmptyStack(stack)) return StyledText.EMPTY;

        Text existingName = WynntilsBankAdapter.getOriginalName(stack);
        if (existingName != null) return StyledText.fromComponent(existingName);

        Text stackName = stack.getName();
        if (stack.getCustomName() != null && stack.getCustomName().toString().contains("Key")) {
            String clean = WynnStringUtils.normalizeBadString(stackName.getString());
            stackName = Text.of(clean);
        }

        StyledText originalName = StyledText.fromComponent(stackName);
        WynntilsBankAdapter.setOriginalName(stack, stackName);
        return originalName;
    }

    // Cached durability-overlay config so we don't reflect into Wynntils config options
    // on every slot draw (was 2 lookups × ~1000 slots per frame → ~5fps in bank).
    private static boolean durabilityRenderInInv = false;
    private static String durabilityMode = "ARC";

    private static void refreshDurabilityCfg() {
        try {
            if (durabilityOverlayFeature == null)
                durabilityOverlayFeature = WynntilsBankAdapter.getFeature("DurabilityOverlayFeature").orElse(null);
            durabilityRenderInInv = WynntilsBankAdapter.booleanConfig(durabilityOverlayFeature, "renderDurabilityOverlayInventories");
            durabilityMode = WynntilsBankAdapter.enumConfigName(durabilityOverlayFeature, "durabilityRenderMode", "ARC");
        } catch (Exception ignored) {}
    }

    private static boolean emeraldPouchRenderInInv = false;

    private static void refreshEmeraldPouchCfg() {
        try {
            if (emeraldPouchFillArcFeature == null)
                emeraldPouchFillArcFeature = WynntilsBankAdapter.getFeature("EmeraldPouchFillArcFeature").orElse(null);
            emeraldPouchRenderInInv = WynntilsBankAdapter.booleanConfig(
                    emeraldPouchFillArcFeature, "renderFillArcInventory");
        } catch (Exception ignored) {
            emeraldPouchRenderInInv = false;
        }
    }

    private static boolean highlightRenderInInv = false;

    private static void refreshHighlightCfg() {
        try {
            if (itemHighlightFeature == null)
                itemHighlightFeature = WynntilsBankAdapter.getFeature("ItemHighlightFeature").orElse(null);
            ItemHighlightRenderer.refreshWynntilsHighlightTexture();
            highlightRenderInInv = WynntilsBankAdapter.booleanConfig(itemHighlightFeature, "inventoryHighlightEnabled");
        } catch (Exception ignored) {
            highlightRenderInInv = false;
        }
    }

    private static void refreshItemTextOverlayCfg() {
        try {
            if (itemTextOverlayFeature == null)
                itemTextOverlayFeature = WynntilsBankAdapter.getFeature("ItemTextOverlayFeature").orElse(null);
            itemTextRenderInInv = WynntilsBankAdapter.booleanConfig(
                    itemTextOverlayFeature, "inventoryTextOverlayEnabled");
        } catch (Exception ignored) {
            itemTextRenderInInv = false;
        }
    }

    private static void refreshFrameFeatureStates() {
        try {
            if (durabilityOverlayFeature == null)
                durabilityOverlayFeature = WynntilsBankAdapter.getFeature("DurabilityOverlayFeature").orElse(null);
            durabilityOverlayEnabled = WynntilsBankAdapter.isEnabled(durabilityOverlayFeature);
        } catch (Exception ignored) {
            durabilityOverlayEnabled = false;
        }

        try {
            if (emeraldPouchFillArcFeature == null)
                emeraldPouchFillArcFeature = WynntilsBankAdapter.getFeature("EmeraldPouchFillArcFeature").orElse(null);
            emeraldPouchFillArcEnabled = WynntilsBankAdapter.isEnabled(emeraldPouchFillArcFeature);
        } catch (Exception ignored) {
            emeraldPouchFillArcEnabled = false;
        }

        try {
            if (itemHighlightFeature == null)
                itemHighlightFeature = WynntilsBankAdapter.getFeature("ItemHighlightFeature").orElse(null);
            itemHighlightEnabled = WynntilsBankAdapter.isEnabled(itemHighlightFeature);
        } catch (Exception ignored) {
            itemHighlightEnabled = false;
        }

        try {
            if (itemTextOverlayFeature == null)
                itemTextOverlayFeature = WynntilsBankAdapter.getFeature("ItemTextOverlayFeature").orElse(null);
            itemTextOverlayEnabled = WynntilsBankAdapter.isEnabled(itemTextOverlayFeature);
        } catch (Exception ignored) {
            itemTextOverlayEnabled = false;
        }

        try {
            if (unidentifiedItemIconFeature == null)
                unidentifiedItemIconFeature = WynntilsBankAdapter.getFeature("UnidentifiedItemIconFeature").orElse(null);
            unidentifiedItemIconEnabled = WynntilsBankAdapter.isEnabled(unidentifiedItemIconFeature);
        } catch (Exception ignored) {
            unidentifiedItemIconEnabled = false;
        }

        try {
            if (itemFavoriteFeature == null)
                itemFavoriteFeature = WynntilsBankAdapter.getFeature("ItemFavoriteFeature").orElse(null);
            itemFavoriteEnabled = WynntilsBankAdapter.isEnabled(itemFavoriteFeature);
        } catch (Exception ignored) {
            itemFavoriteEnabled = false;
        }
    }

    private static boolean isEmptyStack(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem() == Items.AIR;
    }

    private static ItemStack withoutVanillaDurabilityModelData(ItemStack stack) {
        CustomModelDataComponent modelData = stack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (modelData == null || modelData.floats().size() <= 1) {
            return stack;
        }

        float value = modelData.floats().get(1);
        if (value < 1 || value > 15) {
            return stack;
        }

        List<Float> floats = new ArrayList<>(modelData.floats());
        floats.remove(1);

        ItemStack renderStack = stack.copy();
        renderStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                floats,
                new ArrayList<>(modelData.flags()),
                new ArrayList<>(modelData.strings()),
                new ArrayList<>(modelData.colors())
        ));
        return renderStack;
    }

    private static void renderDurabilityOverlay(DrawContext context, ItemStack stack, int x, int y) {
        if (!durabilityOverlayEnabled || !durabilityRenderInInv) return;
        if (WynntilsBankAdapter.drawFeature(durabilityOverlayFeature, "drawDurability", context, stack, x, y)) return;

        String method = switch (durabilityMode) {
            case "BAR" -> "drawDurabilityBar";
            case "PERCENTAGE" -> "drawDurabilityPercentage";
            default -> "drawDurabilityArc";
        };
        WynntilsBankAdapter.drawFeature(durabilityOverlayFeature, method, context, stack, x, y);
    }

    private static void renderEmeraldPouchRing(DrawContext context, ItemStack stack, int x, int y) {
        if (!emeraldPouchFillArcEnabled || !emeraldPouchRenderInInv) return;
        WynntilsBankAdapter.drawFeature(emeraldPouchFillArcFeature, "drawFilledArc", context, stack, x, y);
    }

    private static Optional<CustomColor> getHighlightColor(ItemStack stack) {
         if (isEmptyStack(stack) || !itemHighlightEnabled || !highlightRenderInInv) {
             return Optional.empty();
         }
         return WynntilsBankAdapter.getHighlightColor(itemHighlightFeature, stack);
    }

    private static void renderHighlightOverlay(DrawContext context, Optional<CustomColor> color, int x, int y) {
         color.ifPresent(value -> {
             try {
                 ItemHighlightRenderer.drawWynntilsHighlightTexture(
                     context,
                     value, (float)(x - 8), (float)(y - 8), 32.0F, 32.0F);
             } catch (Exception ignored) {}
         });
    }

    private static void renderItemOverlays(DrawContext context, ItemStack stack, int x, int y) {
        if (itemTextOverlayEnabled && itemTextRenderInInv) {
            try {
                WynntilsBankAdapter.drawFeature(itemTextOverlayFeature, "drawTextOverlay", context, stack, x, y, false);
            } catch (Exception ignored) {}
        }

        if (unidentifiedItemIconEnabled) {
            WynntilsBankAdapter.drawFeature(unidentifiedItemIconFeature, "drawIcon", context, stack, x, y, 100);
        }
        if(itemFavoriteEnabled && WynntilsBankAdapter.isFavorited(itemFavoriteFeature, stack)) {
            RenderUtils.drawScalingTexturedRect(
                    context,
                    Texture.FAVORITE_ICON.identifier(),
                    x + 10,
                    y,
                    9,
                    9,
                    Texture.FAVORITE_ICON.width(),
                    Texture.FAVORITE_ICON.height());
        }
    }

    private static void renderSearchOverlay(DrawContext context, ItemStack stack, WynnItemData cachedItem, int x, int y) {
        if (activeSearchInput == null || activeSearchInput.isEmpty()) return;

        if (isEmptyStack(stack)) {
            RenderUtils.drawRect(context, SEARCH_DIM_COLOR, x - 1, y - 1, 18, 18);
            return;
        }

        WynnItemData wynnItem = cachedItem;
        if (wynnItem == null) {
            wynnItem = WynnItemParser.parse(stack).orElse(null);
        }

        if (SearchQueryParser.matches(stack, wynnItem, activeSearchQuery)) {
            // Item matches - draw green border
            RenderUtils.drawRectBorders(context, SEARCH_MATCH_COLOR, x, y, 16, 16, 1);
        } else {
            // Item doesn't match - dim it
            RenderUtils.drawRect(context, SEARCH_DIM_COLOR, x - 1, y - 1, 18, 18);
        }
    }

    private void renderHoveredTooltip(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (isJumpInProgress()) {
            clearHoverState(screen);
            return;
        }
        if (hoveredSlot.getItem() == Items.AIR) return;

        ensureWynntilsOriginalName(hoveredSlot);
        Optional<WynnItemData> item = asWynnItem(hoveredSlot);
        WeightDisplay.setCurrentHoveredStack(hoveredSlot);
        TooltipRenderData tooltipData = getTooltipRenderData(hoveredSlot, item);
        hoveredTooltipData = tooltipData;
        TradeMarketComparisonPanel.cacheHoveredTooltip(hoveredSlot, tooltipData.tooltip());

        if (readOnlyViewer) {
            drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipData.components(), mouseX + 14, mouseY, context);
            return;
        }

        Slot tooltipSource = getTooltipSourceSlot(screen);
        ensureWynntilsOriginalName(tooltipSource.getStack());
        HandledScreenAccess.setFocusedSlot(screen, tooltipSource);
    }

    public static void renderLunarFallbackTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredTooltipData == null) return;
        drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltipData.components(), mouseX + 14, mouseY, context);
    }

    private static TooltipRenderData getTooltipRenderData(ItemStack stack, Optional<WynnItemData> item) {
        int componentsHash = stack.getComponents().hashCode();
        int count = stack.getCount();
        int modifierState = getTooltipModifierState();
        if (stack == cachedTooltipStack
                && count == cachedTooltipCount
                && componentsHash == cachedTooltipComponentsHash
                && modifierState == cachedTooltipModifierState
                && cachedTooltipRenderData != null) {
            return cachedTooltipRenderData;
        }

        List<Text> tooltip = buildCallbackAwareTooltip(stack, item);
        List<TooltipComponent> components = new ArrayList<>(TooltipUtils.getClientTooltipComponent(tooltip));

        if (item.isPresent() && item.get().unidentified()) {
            if (itemGuessFeature == null) {
                itemGuessFeature = WynntilsBankAdapter.getFeature("ItemGuessFeature").orElse(null);
            }
            List<Text> addon = WynntilsItemUiAdapter.getItemGuessTooltip(stack);

            tooltip.addAll(addon);
            components.addAll(TooltipUtils.getClientTooltipComponent(addon));
        }

        cachedTooltipStack = stack;
        cachedTooltipCount = count;
        cachedTooltipComponentsHash = componentsHash;
        cachedTooltipModifierState = modifierState;
        cachedTooltipRenderData = new TooltipRenderData(tooltip, components, TooltipUtils.getTooltipHeight(components));
        return cachedTooltipRenderData;
    }

    private static int getTooltipModifierState() {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        int state = 0;
        if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            state |= 1;
        }
        if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            state |= 2;
        }
        if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            state |= 4;
        }
        return state;
    }

    private Slot getTooltipSourceSlot(HandledScreen<?> screen) {
        if (hoveredBackingSlot != null) {
            return hoveredBackingSlot;
        }

        tooltipInventory.setStack(0, hoveredSlot);
        SlotAccess.setPosition(tooltipSlot, hoveredX - HandledScreenAccess.x(screen), hoveredY - HandledScreenAccess.y(screen));
        return tooltipSlot;
    }

    private static List<Text> buildCallbackAwareTooltip(ItemStack stack, Optional<WynnItemData> item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Text> tooltip = new ArrayList<>();

        try {
            tooltip.addAll(stack.getTooltip(
                    Item.TooltipContext.DEFAULT,
                    mc.player,
                    TooltipType.BASIC
            ));
        } catch (Throwable ignored) {}

        return tooltip;
    }

    private static void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, DrawContext context) {
        if (!components.isEmpty()) {
            int i = 0;
            int j = components.size() == 1 ? -2 : 0;

            TooltipComponent tooltipComponent;
            for(Iterator<?> var9 = components.iterator(); var9.hasNext(); j += tooltipComponent.getHeight(textRenderer)) {
                tooltipComponent = (TooltipComponent)var9.next();
                int k = tooltipComponent.getWidth(textRenderer);
                if (k > i) {
                    i = k;
                }
            }

            int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
            if (x + i > screenWidth) x = Math.max(4, screenWidth - i - 4);
            if (y + j > screenHeight) y = Math.max(4, screenHeight - j - 4);

            int l = i;
            int m = j;
            TooltipBackgroundRenderer.render(context, x, y, i, j, null);

            int q = y;

            int r;
            TooltipComponent tooltipComponent2;
            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = components.get(r);
                tooltipComponent2.drawText(context, textRenderer, x, q);
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }

            q = y;

            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = components.get(r);
                tooltipComponent2.drawItems(textRenderer, x, q, l, m, context);
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }
        }
    }

    private void renderHeldItemOverlay(DrawContext context, int mouseX, int mouseY) {
        syncHeldItemFromCursorStack();
        if (heldItem == null) return;

        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue() + 1;
        int count = dragSplitting ? getDragSplittingRemainingCount() : heldItem.getCount();
        String amountString = count == 1 ? "" : String.valueOf(count);

        context.drawItem(heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale, amountString);
    }

    private void syncHeldItemFromCursorStack() {
        if (screen == null || screen.getScreenHandler() == null) return;

        ItemStack cursorStack = screen.getScreenHandler().getCursorStack();
        heldItem = cursorStack.isEmpty() ? Items.AIR.getDefaultStack() : cursorStack.copy();
    }

    private static boolean shouldCancelEmeraldPouch(ItemStack oldHeld, ItemStack newHeld) {
        if (oldHeld == null || newHeld == null || newHeld.getCustomName() == null) return false;

        return (oldHeld.getItem() == Items.EMERALD ||
                oldHeld.getItem() == Items.EMERALD_BLOCK ||
                oldHeld.getItem() == Items.EXPERIENCE_BOTTLE) &&
                newHeld.getCustomName().getString().contains("Pouch");
    }

    private static ItemStack getHeldItem(int index, SlotActionType type, int mouseButton) {
        MinecraftClient mc = MinecraftUtils.mc();
        PlayerEntity player = mc.player;
        ItemStack heldItem = Items.AIR.getDefaultStack();

        if (player == null || player.currentScreenHandler == null) return heldItem;
        ItemStack clickedStack = player.currentScreenHandler.slots.get(index).getStack().copy();
        ItemStack currentHeld = BankOverlay.heldItem;
        if (type == SlotActionType.QUICK_MOVE) return currentHeld == null ? Items.AIR.getDefaultStack() : currentHeld.copy();

        if (mouseButton == 0) { // Left Click
            switch (type) {
                case PICKUP -> {
                    if (!currentHeld.isEmpty() && ItemStack.areItemsAndComponentsEqual(clickedStack, currentHeld)) {
                        int maxStackSize = clickedStack.getMaxCount();
                        int combined = clickedStack.getCount() + currentHeld.getCount();

                        if (combined <= maxStackSize) {
                            heldItem = Items.AIR.getDefaultStack();
                        } else {
                            heldItem = currentHeld.copy();
                            heldItem.setCount(combined - maxStackSize);
                        }
                    } else {
                        heldItem = clickedStack.copy();
                    }
                }

                case PICKUP_ALL -> {
                    if (currentHeld == null) return heldItem;
                    if (currentHeld.getCount() == currentHeld.getMaxCount()) {
                        heldItem = currentHeld;
                        break;
                    }

                    int newAmount = currentHeld.getCount();
                    for (Slot slot : player.currentScreenHandler.slots) {
                        ItemStack stack = slot.getStack();
                        if (ItemStack.areItemsAndComponentsEqual(stack, currentHeld)) {
                            newAmount += stack.getCount();
                            if (newAmount >= currentHeld.getMaxCount()) {
                                newAmount = currentHeld.getMaxCount();
                                break;
                            }
                        }
                    }
                    heldItem = currentHeld.copy();
                    heldItem.setCount(newAmount);
                }

                case QUICK_MOVE -> heldItem = Items.AIR.getDefaultStack();
            }
        } else { // Right Click
            if (currentHeld == null || currentHeld.isEmpty()) {
                heldItem = clickedStack.copy();
                int half = heldItem.getCount() / 2;
                heldItem.setCount(heldItem.getCount() % 2 == 0 ? half : half + 1);
            } else if (clickedStack.isEmpty()) {
                heldItem = currentHeld.copy();
                if (heldItem.getCount() == 1) {
                    heldItem = Items.AIR.getDefaultStack();
                } else {
                    heldItem.setCount(currentHeld.getCount() - 1);
                }
            } else if (ItemStack.areItemsAndComponentsEqual(currentHeld, clickedStack)) {
                if (currentHeld.getCount() == 1) {
                    heldItem = Items.AIR.getDefaultStack();
                } else {
                    heldItem = currentHeld.copy();
                    heldItem.setCount(currentHeld.getCount() - 1);
                }
            } else {
                heldItem = clickedStack.copy();
            }
        }


        return heldItem;
    }

    public static Optional<WynnItemData> asWynnItem(ItemStack itemStack) {
        return WynnItemParser.parse(itemStack);
    }

    public static void drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        if (signMids.isEmpty() || signMidsDarkMode != WynnExtrasConfig.INSTANCE.darkmodeToggle) {
            signMids.clear();
            signMidsDarkMode = WynnExtrasConfig.INSTANCE.darkmodeToggle;
            if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
                signMids.add(signMid1D);
                signMids.add(signMid2D);
                signMids.add(signMid3D);
            } else {
                signMids.add(signMid1);
                signMids.add(signMid2);
                signMids.add(signMid3);
            }
        }
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input);
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
            RenderUtils.drawTexturedRect(context, signLeftDark, CustomColor.NONE, x, y - 15, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context, signLeft, CustomColor.NONE, x, y - 15, 10, 15, 10, 15);
        }
        if (strWidth > 15) {
            for (int i = 0; i < amount; i++) {
                RenderUtils.drawTexturedRect(context, signMids.get(i % 3), CustomColor.NONE, x + 10 + 10 * i, y - 15, 10, 15, 10, 15);
            }
        }
        if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
            RenderUtils.drawTexturedRect(context, signRightDark, CustomColor.NONE, x + 10 + 10 * amount, y - 15, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context, signRight, CustomColor.NONE, x + 10 + 10 * amount, y - 15, 10, 15, 10, 15);
        }
    }

    /** Top-left "[?]" hover for search-filter help. List comes from SearchQueryParser's
     *  supported filters — keep in sync if new filters are added. */
    private void drawSearchInfoButton(DrawContext context, int xStart, int yStart, int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        net.minecraft.client.font.TextRenderer tr = mc.textRenderer;
        String label = "[?]";
        int btnX = xStart - 36;
        int btnY = yStart - 30;
        int w = tr.getWidth(label);
        int h = tr.fontHeight;
        boolean hovered = mouseX >= btnX && mouseX < btnX + w && mouseY >= btnY && mouseY < btnY + h;
        context.drawText(tr, label, btnX, btnY, hovered ? 0xFFFFFFFF : 0xFFAAAAAA, true);
        if (!hovered) return;

        java.util.List<net.minecraft.text.Text> lines = new java.util.ArrayList<>();
        lines.add(net.minecraft.text.Text.literal("§eSearch filters"));
        lines.add(net.minecraft.text.Text.literal("§flevel:§fN§7, §flevel:§fA-B§7, §flevel:§fN+§7, §flevel:§fN-"));
        lines.add(net.minecraft.text.Text.literal("§7class:§fwarrior§7|§fmage§7|§farcher§7|§fassassin§7|§fshaman"));
        lines.add(net.minecraft.text.Text.literal("§7rarity:§fcommon§7|§funique§7|§frare§7|§flegendary§7|§ffabled§7|§fmythic"));
        lines.add(net.minecraft.text.Text.literal("§7prof:§fcooking§7|§falchemism§7|§fjeweling§7|..."));
        lines.add(net.minecraft.text.Text.literal("§7type:§fgear§7|§fbox§7|§fpowder§7|§fpotion§7|§ffood§7|§ftome§7|§ftool§7|"));
        lines.add(net.minecraft.text.Text.literal("§7      §fingredient§7|§fpouch§7|§fkey§7|§fhorse§7|§fscroll§7|§famplifier§7|"));
        lines.add(net.minecraft.text.Text.literal("§7      §fcharm§7|§ftrinket§7|§frune§7|§fmaterial"));
        lines.add(net.minecraft.text.Text.literal("§7materialtier:§f1§7|§f2§7|§f3"));
        lines.add(net.minecraft.text.Text.literal("§7crafted:§ftrue§7|§ffalse"));
        lines.add(net.minecraft.text.Text.literal("§7mountcolor:§f<primary color>§7 or §f<primary color>-<secondary color>"));
        lines.add(net.minecraft.text.Text.literal("§8Combine: §ftype:gear level:80-100 rarity:fabled"));
        lines.add(net.minecraft.text.Text.literal("§8Search-bar shortcuts: §fCtrl+C, Ctrl+V, Ctrl+X"));
        lines.add(net.minecraft.text.Text.literal("§7ingredienttier:§f0§7|§f1§7|§f2§7|§f3"));
        context.drawTooltip(tr, lines, mouseX, mouseY);
    }

    void drawEmeraldOverlay(DrawContext context, int x, int y) {
        if (emeraldCountFeature == null) {
            emeraldCountFeature = WynntilsBankAdapter.getFeature("InventoryEmeraldCountFeature").orElse(null);
        }
        int emeraldAmountInt = WynntilsBankAdapter.getEmeraldAmount();
        if (emeraldAmountInt != cachedEmeraldAmount) {
            cachedEmeraldAmount = emeraldAmountInt;
            cachedEmeraldAmounts = WynntilsBankAdapter.getRenderableEmeraldAmounts(emeraldCountFeature, emeraldAmountInt);
        }
        String[] emeraldAmounts = cachedEmeraldAmounts;
        List<WynntilsBankAdapter.EmeraldUnit> emeraldUnits = WynntilsBankAdapter.getEmeraldUnits();

        y += (3 * 28);


        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            String emeraldAmount = emeraldAmounts[i];

            if (emeraldAmount.equals("0")) continue;
            if (i >= emeraldUnits.size()) continue;
            WynntilsBankAdapter.EmeraldUnit unit = emeraldUnits.get(i);

            RenderUtils.drawTexturedRect(
                    context,
                    Texture.EMERALD_COUNT_BACKGROUND.identifier(),
                    x,
                    y - (i * 28),
                    28,
                    28,
                    0,
                    0,
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height(),
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height());

            if (unit.symbol().equals("stx")) { // Make stx not look like normal LE
                context.drawItem(unit.stack(), x + 3, y + 4 - (i * 28));
                context.drawItem(unit.stack(), x + 6, y + 6 - (i * 28));
                context.drawItem(unit.stack(), x + 9, y + 8 - (i * 28));
            } else {
                // This needs to be separate since Z levels are determined by order here
                context.drawItem(unit.stack(), x + 6, y + 6 - (i * 28));
            }

            FontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            context,
                            StyledText.fromString(emeraldAmount),
                            x,
                            x + 28 - 2,
                            y - (i * 28),
                            y + 28 - 2  - (i * 28),
                            0,
                            CustomColor.WHITE,
                            HorizontalAlignment.RIGHT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.OUTLINE);
        }
    }

    // Hardcoded layout for the total-bags grid: all known raids × the three crafter-bag tiers.
    // Bags that don't match one of these combos still get counted into the header total, but
    // their own row won't be shown (we also don't have icons for combinations that never occur).
    private static final String[] BAG_RAID_ORDER = {"NOG", "NOL", "TCC", "TNA", "WTP"};
    private enum BagTier {
        LEGENDARY(Formatting.AQUA), RARE(Formatting.LIGHT_PURPLE), UNIQUE(Formatting.YELLOW);

        private final Formatting formatting;

        BagTier(Formatting formatting) {
            this.formatting = formatting;
        }
    }
    private static final BagTier[] BAG_TIER_ORDER = {BagTier.LEGENDARY, BagTier.RARE, BagTier.UNIQUE};
    private static final Map<String, StyledText> BAG_RAID_LABELS = createBagRaidLabels();
    private static final List<ItemStack> CURRENT_PAGE_STACKS = new ArrayList<>(54);
    private static final List<ItemStack> PLAYER_INVENTORY_STACKS = new ArrayList<>(36);
    private static final List<ItemStack> SCREEN_HANDLER_STACKS = new ArrayList<>(54);
    private static final Map<String, Integer> BAG_TOTAL_CACHE = new HashMap<>();
    private static String bagTotalCacheKey = null;
    private static boolean bagTotalCacheDirty = true;
    private static final HashMap<String, Integer> BAG_PAGE_COUNT_SCRATCH = new HashMap<>();
    private static final Map<String, BagGroup> BAG_GROUP_SCRATCH = new LinkedHashMap<>();

    public static void invalidateBagTotalCache() {
        bagTotalCacheDirty = true;
        bagTotalCacheKey = null;
    }

    private static Map<String, StyledText> createBagRaidLabels() {
        Map<String, StyledText> labels = new HashMap<>();
        for (String raid : BAG_RAID_ORDER) {
            labels.put(raid, StyledText.fromString(raid));
        }
        return labels;
    }

    /** Sort mode for the top-right bag breakdown. Click the "[By Type]"/"[By Count]" label to toggle. */
    public enum BagSortMode { BY_TYPE, BY_AMOUNT }
    private static BagSortMode bagSortMode = BagSortMode.BY_TYPE;
    // Click bounds for the sort toggle label, updated each frame so the mixin click handler can hit-test.
    private static int sortToggleX = 0, sortToggleY = 0, sortToggleW = 0, sortToggleH = 0;

    static void drawBagOverlay(DrawContext context, int x, int y,
                               List<ItemStack> gridStacks, Map<String, Integer> totalCounts) {
        if(WynnExtrasConfig.INSTANCE.showTotalBagsInBankOverlay) drawBagTopRightHeader(context, totalCounts);
        drawBagGrid(context, x, y, gridStacks);
    }

    private static final class BagCountEntry {
        final String key;
        final String raidAbbrev;
        final BagTier tier;
        final String tierName;
        final int raidSortOrder;
        final int tierSortOrder;
        int count;

        BagCountEntry(String raidAbbrev, BagTier tier, int raidSortOrder) {
            this.key = raidAbbrev + "|" + tier.name();
            this.raidAbbrev = raidAbbrev;
            this.tier = tier;
            this.tierName = tier.name().charAt(0) + tier.name().substring(1).toLowerCase();
            this.raidSortOrder = raidSortOrder;
            this.tierSortOrder = -tier.ordinal();
        }
    }

    private static final List<BagCountEntry> BAG_HEADER_ENTRIES = createBagHeaderEntries();
    private static final List<BagCountEntry> BAG_HEADER_VISIBLE_ENTRIES =
            new ArrayList<>(BAG_RAID_ORDER.length * BAG_TIER_ORDER.length);

    private static List<BagCountEntry> createBagHeaderEntries() {
        List<BagCountEntry> entries = new ArrayList<>(BAG_RAID_ORDER.length * BAG_TIER_ORDER.length);
        for (int raidIndex = 0; raidIndex < BAG_RAID_ORDER.length; raidIndex++) {
            for (BagTier tier : BAG_TIER_ORDER) {
                entries.add(new BagCountEntry(BAG_RAID_ORDER[raidIndex], tier, raidIndex));
            }
        }
        return entries;
    }

    private static void drawBagTopRightHeader(DrawContext context, Map<String, Integer> totalCounts) {
        int totalCount = 0;
        for (int c : totalCounts.values()) totalCount += c;

        List<BagCountEntry> lines = BAG_HEADER_VISIBLE_ENTRIES;
        lines.clear();
        for (BagCountEntry entry : BAG_HEADER_ENTRIES) {
            entry.count = totalCounts.getOrDefault(entry.key, 0);
            if (entry.count > 0) lines.add(entry);
        }

        // Sort per mode
        if (bagSortMode == BagSortMode.BY_AMOUNT) {
            lines.sort((a, b) -> Integer.compare(b.count, a.count));
        } else {
            lines.sort((a, b) -> {
                int raidCompare = Integer.compare(a.raidSortOrder, b.raidSortOrder);
                if (raidCompare != 0) return raidCompare;
                return Integer.compare(a.tierSortOrder, b.tierSortOrder);
            });
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int screenWidth = mc.currentScreen != null ? mc.currentScreen.width : mc.getWindow().getScaledWidth();

        int lineY = 5;

        // Total header
        String header = "Total Bags: " + totalCount;
        int headerWidth = tr.getWidth(header);
        context.drawText(tr, header, screenWidth - headerWidth - 5, lineY, 0xFFFFFFFF, true);
        lineY += tr.fontHeight + 2;

        // Clickable sort toggle
        String toggle = "[Sort: " + (bagSortMode == BagSortMode.BY_TYPE ? "By Type" : "By Count") + "]";
        int toggleW = tr.getWidth(toggle);
        int toggleX = screenWidth - toggleW - 5;
        int toggleY = lineY;
        context.drawText(tr, toggle, toggleX, toggleY, 0xFFAAAAAA, true);
        sortToggleX = toggleX;
        sortToggleY = toggleY;
        sortToggleW = toggleW;
        sortToggleH = tr.fontHeight;
        lineY += tr.fontHeight + 3;

        // Per-(raid, tier) lines
        for (BagCountEntry e : lines) {
            String line = e.raidAbbrev + " " + e.tierName + ": " + e.count;
            int lineWidth = tr.getWidth(line);
            context.drawText(tr, line, screenWidth - lineWidth - 5, lineY, getTierColorArgb(e.tier), true);
            lineY += tr.fontHeight + 1;
        }
    }

    /**
     * Click hit-test for the sort-mode toggle label. Returns true if the click toggled the mode.
     * Called from HandledScreenMixin.mouseClicked.
     */
    public static boolean handleSortToggleClick(double mouseX, double mouseY) {
        if (sortToggleW <= 0 || sortToggleH <= 0) return false;
        if (mouseX < sortToggleX || mouseX >= sortToggleX + sortToggleW) return false;
        if (mouseY < sortToggleY || mouseY >= sortToggleY + sortToggleH) return false;
        bagSortMode = bagSortMode == BagSortMode.BY_TYPE ? BagSortMode.BY_AMOUNT : BagSortMode.BY_TYPE;
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        return true;
    }

    public static boolean setSearchInput(String input) {
        if (searchbar2 == null) {
            return false;
        }
        String previousInput = searchbar2.getInput();
        searchbar2.setInputAndMoveCursorToEnd(input);
        String normalizedInput = input == null ? "" : input.replace("@", "").trim();
        activeSearchInput = normalizedInput;
        activeSearchQuery = SearchQueryParser.parse(normalizedInput);
        if (!Objects.equals(previousInput, searchbar2.getInput())) {
            lastCrossClassSearchQuery = "";
            invalidateLocalCrossClassPageCaches();
            for (PageWidget page : pages) {
                page.invalidateSearchCache();
            }
        }
        return true;
    }

    /**
     * Draws the (raid × tier) grid of bag boxes. Raid rows with 0 total bags are skipped
     * entirely, and if no bags exist in the scoped data the grid isn't drawn at all.
     */
    private static void drawBagGrid(DrawContext context, int x, int y, List<ItemStack> stacks) {
        drawBagGridFromGroups(context, x, y, groupBagsFromStacks(stacks));
    }

    /** Builds groups directly from the cached (raid|tier → count) totals — no live stacks needed,
     *  so it works on screens where the Wynncraft container doesn't expose bag items
     *  (player inventory, trade market). Icons end up empty; cell still shows raid abbrev + count. */
    private static void drawBagGridFromCounts(DrawContext context, int x, int y, Map<String, Integer> totals) {
        Map<String, BagGroup> groups = new java.util.LinkedHashMap<>();
        for (String raid : BAG_RAID_ORDER) {
            for (BagTier tier : BAG_TIER_ORDER) {
                String key = raid + "|" + tier.name();
                int count = totals.getOrDefault(key, 0);
                if (count <= 0) continue;
                BagGroup g = new BagGroup(raid, tier, ItemStack.EMPTY);
                g.count = count;
                groups.put(key, g);
            }
        }
        drawBagGridFromGroups(context, x, y, groups);
    }

    private static void drawBagGridFromGroups(DrawContext context, int x, int y, Map<String, BagGroup> groups) {
        if (groups.isEmpty()) return;

        int row = 0;
        boolean shiftHeld = isShiftHeld();
        FontRenderer fontRenderer = FontRenderer.getInstance();
        for (String raid : BAG_RAID_ORDER) {
            boolean visible = false;
            for (BagTier tier : BAG_TIER_ORDER) {
                BagGroup g = groups.get(raid + "|" + tier.name());
                if (g != null && g.count > 0) {
                    visible = true;
                    break;
                }
            }
            if (!visible) continue;

            StyledText raidLabel = BAG_RAID_LABELS.get(raid);
            if (raidLabel == null) raidLabel = StyledText.fromString(raid);

            for (int col = 0; col < BAG_TIER_ORDER.length; col++) {
                BagTier tier = BAG_TIER_ORDER[col];
                String key = raid + "|" + tier.name();
                BagGroup group = groups.get(key);
                int count = group != null ? group.count : 0;

                int cellX = x + col * 28;
                int cellY = y + row * 28;

                RenderUtils.drawTexturedRect(
                        context,
                        Texture.EMERALD_COUNT_BACKGROUND.identifier(),
                        cellX,
                        cellY,
                        28,
                        28,
                        0,
                        0,
                        Texture.EMERALD_COUNT_BACKGROUND.width(),
                        Texture.EMERALD_COUNT_BACKGROUND.height(),
                        Texture.EMERALD_COUNT_BACKGROUND.width(),
                        Texture.EMERALD_COUNT_BACKGROUND.height());

                // Icon: only when we have a real stack for this combo
                if (group != null && !group.icon.isEmpty()) {
                    context.drawItem(group.icon, cellX + 6, cellY + 6);
                }

                CustomColor tierColor = CustomColor.fromChatFormatting(tier.formatting);

                // Raid abbreviation in top-left, colored by tier
                fontRenderer.renderAlignedTextInBox(
                                context,
                                raidLabel,
                                cellX + 1,
                                cellX + 28 - 1,
                                cellY + 1,
                                cellY + 28 - 2,
                                0,
                                tierColor,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);

                // Count in bottom-right (dimmed when zero, compacted for large counts so "1200" fits;
                // hold Shift to override the compact format and see the exact number).
                CustomColor countColor = count > 0 ? CustomColor.WHITE : DIM_COUNT_COLOR;
                String countLabel = shiftHeld ? String.valueOf(count) : formatCompactCount(count);
                fontRenderer.renderAlignedTextInBox(
                                context,
                                StyledText.fromString(countLabel),
                                cellX,
                                cellX + 28 - 2,
                                cellY,
                                cellY + 28 - 2,
                                0,
                                countColor,
                                HorizontalAlignment.RIGHT,
                                VerticalAlignment.BOTTOM,
                                TextShadow.OUTLINE);
            }
            row++;
        }
    }

    private static boolean isShiftHeld() {
        long w = MinecraftClient.getInstance().getWindow().getHandle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(w, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    /** Compact-format a count so it fits in a 28px-wide box. 1420 -> "1.42k", 12345 -> "12.3k". */
    private static String formatCompactCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 10000) {
            int whole = count / 1000;
            int fraction = (count % 1000) / 10;
            return whole + "." + (fraction < 10 ? "0" : "") + fraction + "k";
        }
        if (count < 100000) {
            return (count / 1000) + "." + ((count % 1000) / 100) + "k";
        }
        if (count < 1000000) return (count / 1000) + "k";
        return (count / 1000000) + "m";
    }

    /** ARGB int for a gear tier's chat color (for TextRenderer.drawText), or white if absent. */
    private static int getTierColorArgb(BagTier tier) {
        if (tier == null) return 0xFFFFFFFF;
        Integer rgb = tier.formatting.getColorValue();
        return rgb != null ? (0xFF000000 | rgb) : 0xFFFFFFFF;
    }

    /**
     * Entry point for drawing the bag overlay in vanilla bank mode or trade market
     * (i.e. whenever the custom overlay isn't drawing it itself).
     * Positions the boxes to the right of the vanilla container; the "Total Bags: N"
     * header is drawn by drawBagOverlay in the top-right of the screen.
     */
    /** Returns the live ItemStacks from a ScreenHandler that belong to the player's inventory
     *  (i.e. the slots whose source is the player Inventory, not the chest container). */
    private static List<ItemStack> collectPlayerInventoryStacks(net.minecraft.screen.ScreenHandler menu) {
        PLAYER_INVENTORY_STACKS.clear();
        if (menu == null) return PLAYER_INVENTORY_STACKS;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return PLAYER_INVENTORY_STACKS;
        Inventory playerInv = mc.player.getInventory();
        for (net.minecraft.screen.slot.Slot slot : menu.slots) {
            if (slot.inventory == playerInv) PLAYER_INVENTORY_STACKS.add(slot.getStack());
        }
        return PLAYER_INVENTORY_STACKS;
    }

    /** Registers an AFTER_RENDER listener for InventoryScreen — the HandledScreen.render mixin
     *  doesn't fire on it (verified via debug logging), so we hook through Fabric ScreenEvents instead. */
    public static void registerScreenHooks() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen inv)) return;
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> {
                drawVanillaBankBagsOverlay(ctx, inv);
            });
        });
    }

    public static void drawVanillaBankBagsOverlay(DrawContext context, HandledScreen<?> screen) {
        if (!WynnExtrasConfig.INSTANCE.bankBagOverlay) return;
        // Bank's own custom overlay already draws this — don't double-paint.
        if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && currentOverlayType != BankOverlayType.NONE) return;

        boolean isBank = WynncraftMenuService.isCurrentAny(MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK);
        boolean isInventory = screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen;

        int x = HandledScreenAccess.x(screen) + HandledScreenAccess.backgroundWidth(screen) + 4;
        int y = HandledScreenAccess.y(screen) + 14;

        if (isBank) {
            // Bank: top-right header (cumulative totals across pages) + grid for the current page.
            drawBagTopRightHeader(context, collectAccountAndCharacterBagCounts());
            drawBagGrid(context, x, y, getCurrentPageStacks());

            // Inventory grid stacked below the bank grid in the same column. Reserve
            // BAG_RAID_ORDER.length rows worth of space so the two grids never collide.
            List<ItemStack> invStacks = collectPlayerInventoryStacks(screen.getScreenHandler());
            if (!invStacks.isEmpty()) {
                int invY = y + BAG_RAID_ORDER.length * 28 + 18;
                drawBagGrid(context, x, invY, invStacks);
            }
        } else if (isInventory) {
            // Inventory: scan EVERY slot of the player handler (PlayerScreenHandler includes
            // crafting+armor+main+hotbar+offhand). getCurrentPageStacks would skip the player
            // inventory entirely and return empty.
            SCREEN_HANDLER_STACKS.clear();
            for (net.minecraft.screen.slot.Slot s : screen.getScreenHandler().slots) {
                SCREEN_HANDLER_STACKS.add(s.getStack());
            }
            drawBagGrid(context, x, y, SCREEN_HANDLER_STACKS);
        } else {
            // Trade menus / any other container — only bags visible in the container's own slots,
            // no top-right total. getCurrentPageStacks already excludes player-inventory slots.
            drawBagGrid(context, x, y, getCurrentPageStacks());
        }
    }

    private static BankData bankCacheLastData = null;
    private static int bankCacheLastPage = -1;
    private static int bankCacheLastFingerprint = 0;
    private static int bagCacheStableFrames = 0;
    private static final int BAG_CACHE_SETTLE_FRAMES = 10; // ~0.5s at 20 tps

    /**
     * Counts CrafterBags on the current live page via Wynntils annotations (which only
     * exist for live ItemStacks, NOT deserialized ones) and stores the counts as plain
     * numbers in {@code BankData}'s bag count cache. Auto-persists to disk (debounced).
     */
    public static void cacheCurrentBankPageIfPossible() {
        if (bankTypeSwitchInProgress) return;
        if (BankOverlay.shouldWait) return;
        if (WynncraftMenuService.isCurrent(MenuType.CHARACTER_BANK)
                && !BankOverlay.syncCurrentCharacterId()) return;

        BankData data = getBankDataForCurrentContainer();
        if (data == null) return;

        int pageNum = getCurrentBankPageNumber();
        if (pageNum < 0) return;

        List<ItemStack> live = getCurrentPageStacks();
        if (live.size() < 45) return;

        List<ItemStack> pageSnapshot = new ArrayList<>(45);
        int fingerprint = 1;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = live.get(i);
            ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
            pageSnapshot.add(copy);
            fingerprint = 31 * fingerprint + copy.getCount();
            fingerprint = 31 * fingerprint + copy.getComponents().hashCode();
            fingerprint = 31 * fingerprint + copy.getItem().hashCode();
        }

        if (data != bankCacheLastData || pageNum != bankCacheLastPage || fingerprint != bankCacheLastFingerprint) {
            bankCacheLastData = data;
            bankCacheLastPage = pageNum;
            bankCacheLastFingerprint = fingerprint;
            bagCacheStableFrames = 0;
            return;
        }
        if (++bagCacheStableFrames < BAG_CACHE_SETTLE_FRAMES) return;

        boolean changed = !bankPageEquals(data.getBankPages().get(pageNum), pageSnapshot);
        if (changed) {
            data.getBankPages().put(pageNum, pageSnapshot);
            invalidateLocalCrossClassPageCaches();
            clearAnnotationCache(pageNum);
        }
        if (data.getLastPage() < pageNum + 1) {
            data.setLastPage(pageNum + 1);
            changed = true;
        }

        BAG_PAGE_COUNT_SCRATCH.clear();
        for (ItemStack stack : pageSnapshot) {
            if (stack == null || stack.isEmpty()) continue;
            WynntilsBankAdapter.CrafterBag bag = WynntilsBankAdapter.getCrafterBag(stack).orElse(null);
            if (bag == null) continue;
            String key = bag.raid() + "|" + bag.tier();
            BAG_PAGE_COUNT_SCRATCH.merge(key, stack.getCount(), Integer::sum);
        }

        HashMap<String, Integer> existing = data.getBagCounts().get(pageNum);
        if (existing == null || !existing.equals(BAG_PAGE_COUNT_SCRATCH)) {
            data.getBagCounts().put(pageNum, new HashMap<>(BAG_PAGE_COUNT_SCRATCH));
            invalidateBagTotalCache();
            changed = true;
        }

        if (!changed) return;

        data.saveAsyncDebounced();
    }

    private static boolean bankPageEquals(List<ItemStack> first, List<ItemStack> second) {
        if (first == null || first.size() < 45 || second == null || second.size() < 45) return false;
        for (int i = 0; i < 45; i++) {
            ItemStack a = first.get(i);
            ItemStack b = second.get(i);
            if (a == null) a = ItemStack.EMPTY;
            if (b == null) b = ItemStack.EMPTY;
            if (a.getCount() != b.getCount() || !ItemStack.areItemsAndComponentsEqual(a, b)) return false;
        }
        return true;
    }

    public static boolean isCurrentContainerBank() {
        return WynncraftMenuService.isCurrentAny(MenuType.ACCOUNT_BANK, MenuType.CHARACTER_BANK,
                MenuType.BOOKSHELF, MenuType.MISC_BUCKET);
    }

    public static void saveCurrentBankData() {
        if (bankTypeSwitchInProgress) return;
        if (BankOverlay.isCharacterBankMissingCharacterId()) return;
        BankData data = getBankDataForCurrentContainer();
        if (data != null) data.saveAsyncDebounced();
    }

    private static Map<String, Integer> collectAccountAndCharacterBagCounts() {
        return collectBagCounts(AccountBankData.INSTANCE, CharacterBankData.INSTANCE);
    }

    private static Map<String, Integer> collectVisibleBankBagCounts() {
        if (currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
            return collectAccountAndCharacterBagCounts();
        }

        BankData data = getBankDataForCurrentContainer();
        if (data == null) {
            BAG_TOTAL_CACHE.clear();
            invalidateBagTotalCache();
            return BAG_TOTAL_CACHE;
        }
        return collectBagCounts(data);
    }

    private static Map<String, Integer> collectBagCounts(BankData... dataSources) {
        String cacheKey = createBagTotalCacheKey(dataSources);
        if (!bagTotalCacheDirty && Objects.equals(cacheKey, bagTotalCacheKey)) {
            return BAG_TOTAL_CACHE;
        }

        BAG_TOTAL_CACHE.clear();
        for (BankData data : dataSources) {
            if (data == null || data.getBagCounts() == null) continue;
            for (Map<String, Integer> pageCounts : data.getBagCounts().values()) {
                if (pageCounts == null) continue;
                for (Map.Entry<String, Integer> e : pageCounts.entrySet()) {
                    // Old caches were written with "TWP|..." keys before Wynntils renamed
                    String key = e.getKey();
                    if (key.startsWith("TWP|")) key = "WTP|" + key.substring(4);
                    BAG_TOTAL_CACHE.merge(key, e.getValue(), Integer::sum);
                }
            }
        }
        bagTotalCacheKey = cacheKey;
        bagTotalCacheDirty = false;
        return BAG_TOTAL_CACHE;
    }

    private static String createBagTotalCacheKey(BankData... dataSources) {
        StringBuilder key = new StringBuilder();
        for (BankData data : dataSources) {
            if (key.length() > 0) key.append('|');
            key.append(data == null ? "null" : System.identityHashCode(data));
        }
        return key.toString();
    }

    private static Map<String, BagGroup> groupBagsFromStacks(Iterable<ItemStack> stacks) {
        BAG_GROUP_SCRATCH.clear();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            WynntilsBankAdapter.CrafterBag bag = WynntilsBankAdapter.getCrafterBag(stack).orElse(null);
            if (bag == null) continue;
            BagTier tier = parseBagTier(bag.tier());
            if (tier == null) continue;
            // Wynntils renamed The Wartorn Palace from "TWP" to "WTP" between 4.1.8 and 4.1.9.
            // Fold the legacy abbreviation onto the new one so groups merge regardless of which
            // Wynntils version is annotating the stack.
            String raidAbbrev = bag.raid();
            if ("TWP".equals(raidAbbrev)) raidAbbrev = "WTP";
            String key = raidAbbrev + "|" + (tier != null ? tier.name() : "?");

            BagGroup group = BAG_GROUP_SCRATCH.get(key);
            if (group == null) {
                group = new BagGroup(raidAbbrev, tier, stack);
                BAG_GROUP_SCRATCH.put(key, group);
            }
            group.count += stack.getCount();
        }
        return BAG_GROUP_SCRATCH;
    }

    private static BagTier parseBagTier(String value) {
        try {
            return BagTier.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    private static List<ItemStack> getCurrentPageStacks() {
        CURRENT_PAGE_STACKS.clear();
        if (readOnlyViewerActive) {
            if (Pages == null) return CURRENT_PAGE_STACKS;
            List<ItemStack> cached = Pages.getBankPages().get(activeInv);
            if (cached != null) CURRENT_PAGE_STACKS.addAll(cached);
            return CURRENT_PAGE_STACKS;
        }
        // Only trust activeInvSlots while the custom overlay is actively managing it.
        // In vanilla mode it's often empty OR holds stale references from a previous
        // custom-mode session, which would silently corrupt the cache.
        boolean customOverlayActive = WynnExtrasConfig.INSTANCE.toggleBankOverlay
                && currentOverlayType != BankOverlayType.NONE;
        if (customOverlayActive && BankOverlay.activeInvSlots != null && !BankOverlay.activeInvSlots.isEmpty()) {
            for (Slot slot : BankOverlay.activeInvSlots) {
                CURRENT_PAGE_STACKS.add(slot.getStack());
            }
            return CURRENT_PAGE_STACKS;
        }
        // Otherwise read the current ScreenHandler directly (excludes player inventory slots).
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenHandler menu = MinecraftUtils.containerMenu();
        if (menu == null || mc.player == null) return CURRENT_PAGE_STACKS;
        Inventory playerInv = mc.player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.inventory == playerInv) continue;
            CURRENT_PAGE_STACKS.add(slot.getStack());
        }
        return CURRENT_PAGE_STACKS;
    }

    private static int getCurrentBankPageNumber() {
        if (LunarCompat.isLunarClient() && currentOverlayType != BankOverlayType.NONE
                && BankOverlay.activeInv != -1) return BankOverlay.activeInv;
        try {
            int p = WynntilsBankAdapter.getCurrentPage();
            if (p > 0) return p - 1;
        } catch (Exception ignored) {}
        int detected = detectLiveBankPageIndex();
        if (detected >= 0) return detected;
        if (BankOverlay.activeInv != -1) return BankOverlay.activeInv;
        return -1;
    }

    private static BankData getBankDataForCurrentContainer() {
        if (WynncraftMenuService.isCurrent(MenuType.ACCOUNT_BANK)) return AccountBankData.INSTANCE;
        if (WynncraftMenuService.isCurrent(MenuType.CHARACTER_BANK)) return CharacterBankData.INSTANCE;
        if (WynncraftMenuService.isCurrent(MenuType.BOOKSHELF)) return BookshelfData.INSTANCE;
        if (WynncraftMenuService.isCurrent(MenuType.MISC_BUCKET)) return MiscBucketData.INSTANCE;
        return null;
    }

    private static boolean isInCharacterSelectionLobby() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return false;
        julianh06.wynnextras.mixin.Accessor.InGameHudAccessor hud =
                (julianh06.wynnextras.mixin.Accessor.InGameHudAccessor) mc.inGameHud;
        Text overlay = hud.getOverlayMessage();
        if (overlay == null) return false;
        String text = overlay.getString();
        return text.contains("Left-Click to play") && text.contains("Right-Click to switch");
    }

    private static boolean isLobbyBlackscreenGone() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return true;
        return ((julianh06.wynnextras.mixin.Accessor.InGameHudAccessor) mc.inGameHud).getTitle() == null;
    }

    private static class BagGroup {
        final String raidAbbrev;
        final BagTier tier;
        final ItemStack icon;
        int count = 0;

        BagGroup(String raidAbbrev, BagTier tier, ItemStack icon) {
            this.raidAbbrev = raidAbbrev;
            this.tier = tier;
            this.icon = icon;
        }
    }

    //Weight display stuff

    // Hovered Slot
    private Slot touchHoveredSlot;

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    private static class InventoryWidget extends Widget {
        Identifier invTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/inv.png");
        Identifier invTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/inv_dark.png");

        List<ItemStack> items;
        List<SlotWidget> slots = new ArrayList<>();
        private int lastSlotLayoutX = Integer.MIN_VALUE;
        private int lastSlotLayoutY = Integer.MIN_VALUE;
        private double lastSlotLayoutScale = Double.NaN;
        private int lastSlotLayoutCount = -1;

        public InventoryWidget() {
            super(0, 0, 0, 0);
            items = new ArrayList<>();
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(ui == null) return;

            ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? invTextureDark : invTexture, x, y - 0.2f, width, height);

            if(slots.isEmpty()) {
                int i = 0;
                for (ItemStack itemStack : items) {
                    SlotWidget slot = new SlotWidget(itemStack == null ? null : itemStack.copy(), i, true, 99);
                    slots.add(slot);
                    i++;
                }
                updateValues();
            }

            if(annotationCache.get(99) != null && annotationCache.get(99).isEmpty()) {
                annotationCache.put(99, null);
                annotationStackCache.remove(99);
                annotationComponentCache.remove(99);
            }

            List<WynntilsBankAdapter.AnnotationHandle> annotations = annotationCache.computeIfAbsent(99, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            List<ItemStack> annotationStacks = annotationStackCache.computeIfAbsent(99, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            List<Object> annotationComponents = annotationComponentCache.computeIfAbsent(99, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            ensureCacheSize(annotations, slots.size(), null);
            ensureCacheSize(annotationStacks, slots.size(), null);
            ensureCacheSize(annotationComponents, slots.size(), null);

            int i = 0;
            for(SlotWidget slot : slots) {
                applyAnnotation(items.get(i), annotations, annotationStacks, annotationComponents, i);
                slot.setStack(items.get(i));
                slot.drawDirect(ctx, mouseX, mouseY, tickDelta, ui);
                i++;
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            return mouseClicked(mx, my, button, false);
        }

        public boolean mouseClicked(double mx, double my, int button, boolean doubleClick) {
            if (!visible || !enabled) return false;
            if (isJumpInProgress()) return contains((int) mx, (int) my);
            for (int i = slots.size() - 1; i >= 0; i--) {
                if (slots.get(i).mouseClicked(mx, my, button, doubleClick)) return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        protected void updateValues() {
            if(slots.isEmpty()) return;
            double scale = ui.getScaleFactor();
            if (lastSlotLayoutX == x
                    && lastSlotLayoutY == y
                    && lastSlotLayoutScale == scale
                    && lastSlotLayoutCount == slots.size()) {
                return;
            }
            lastSlotLayoutX = x;
            lastSlotLayoutY = y;
            lastSlotLayoutScale = scale;
            lastSlotLayoutCount = slots.size();

            int i = 0;
            for(SlotWidget slot : slots) {
                float hotbarOffset = 0;
                if(i >= 27) hotbarOffset = 5.25f;

                slot.setBounds(
                        (int) (x + 18 * (i % 9) * ui.getScaleFactor() + 7),
                        (int) (y + 18 * (i / 9) * ui.getScaleFactor() + 0.75 + hotbarOffset),
                        (int) (18 * ui.getScaleFactor()),
                        (int) (18 * ui.getScaleFactor())
                );
                i++;
            }
        }

        public void setItems(List<ItemStack> items) {
            this.items = items;
        }

    }

    public static class PageWidget extends Widget {
        Identifier bankTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank.png");
        Identifier bankTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank_dark.png");

        private String cachedContainsSearchInput = null;
        private boolean cachedContainsSearchResult = false;

        List<ItemStack> items;
        List<SlotWidget> slots = new ArrayList<>();
        final int index;
        int topBorder;
        int botBorder;
        private boolean slotsVisible = true;
        private int lastSlotLayoutX = Integer.MIN_VALUE;
        private int lastSlotLayoutY = Integer.MIN_VALUE;
        private double lastSlotLayoutScale = Double.NaN;
        private int lastSlotLayoutCount = -1;

        private NameSignWidget sign;

        public PageWidget(int index, int topBorder, int botBorder) {
            super(0, 0, 0, 0);
            this.index = index;
            items = new ArrayList<>();
            this.topBorder = topBorder;
            this.botBorder = botBorder;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(ui == null) return;
            int nameplateHeight = WynnExtrasConfig.INSTANCE.disableStickyNameplates ? 0 : 12;
            if(y > botBorder || y + height + nameplateHeight < topBorder) {
                setSlotsVisible(false);
                return;
            }
            setSlotsVisible(true);

            if(readOnlyViewerActive
                    ? !currentData.getBankPages().containsKey(index)
                    : index >= currentData.getLastPage()) {
                setSlotsVisible(false);
                if(sign == null) {
                    sign = new NameSignWidget(index);
                    addChild(sign);
                }

                positionNameSign();
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
                return;
            }

            ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankTextureDark : bankTexture, x, y, width, height);

            if(sign == null) {
                sign = new NameSignWidget(index);
                addChild(sign);
            }

            positionNameSign();

            if(items.isEmpty()) {
                setSlotsVisible(false);
                return;
            }
            if(items == EMPTY_BANK_PAGE && (activeSearchInput == null || activeSearchInput.isEmpty())) {
                setSlotsVisible(false);
                return;
            }

            if(slots.isEmpty()) {
                int i = 0;
                for (ItemStack itemStack : items) {
                    SlotWidget slot = new SlotWidget(itemStack == null ? null : itemStack.copy(), i, false, index);
                    slots.add(slot);
                    i++;
                }
                updateValues();
            }

            if(annotationCache.get(index) != null && annotationCache.get(index).isEmpty()) {
                annotationCache.put(index, null);
                annotationStackCache.remove(index);
                annotationComponentCache.remove(index);
            }

            List<WynntilsBankAdapter.AnnotationHandle> annotations = annotationCache.computeIfAbsent(index, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            List<ItemStack> annotationStacks = annotationStackCache.computeIfAbsent(index, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            List<Object> annotationComponents = annotationComponentCache.computeIfAbsent(index, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
            ensureCacheSize(annotations, slots.size(), null);
            ensureCacheSize(annotationStacks, slots.size(), null);
            ensureCacheSize(annotationComponents, slots.size(), null);

            int i = 0;
            for(SlotWidget slot : slots) {
                if(i >= items.size()) break;
                applyAnnotation(items.get(i), annotations, annotationStacks, annotationComponents, i);
                slot.setStack(items.get(i));
                slot.drawDirect(ctx, mouseX, mouseY, tickDelta, ui);
                i++;
            }

            if(sign == null) {
                sign = new NameSignWidget(index);
                addChild(sign);
            }

            positionNameSign();
        }

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
            this.ui = ui;
            if (!visible || this.ui == null) return;
            hovered = contains(mouseX, mouseY);
            updateValues();
            drawBackground(ctx, mouseX, mouseY, tickDelta);
            drawContent(ctx, mouseX, mouseY, tickDelta);
            drawForeground(ctx, mouseX, mouseY, tickDelta);
            for (Widget child : children) {
                child.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            return mouseClicked(mx, my, button, false);
        }

        public boolean mouseClicked(double mx, double my, int button, boolean doubleClick) {
            if (!visible || !enabled) return false;
            if (isJumpInProgress()) return contains((int) mx, (int) my);
            for (int i = slots.size() - 1; i >= 0; i--) {
                if (slots.get(i).mouseClicked(mx, my, button, doubleClick)) return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(MinecraftUtils.containerMenu() != null && index == currentData.getLastPage()) {
                if (isCurrentBankRankLockedAtLimit()) {
                    ui.drawCenteredText("§c✖ §Rank required: §f" + rankLockedRequiredRank + "§7.", x + 81, y + 14, WHITE_TEXT_COLOR, 0.9f);
                    ui.drawCenteredText("§7Upgrade to unlock this page.", x + 81, y + 78, WHITE_TEXT_COLOR, 0.8f);
                    ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_locked_dark : lock_locked, x + 82 - 25, y + 46 - 19, 50, 50);
                    return;
                }

                if(priceText == null) {
                    String text = "§c✖ §7Price: §funknown.";
                    String text2 = "§7Go to page §f" + currentData.getLastPage() + " §7to check.";

                    ui.drawCenteredText(text, x + 81, y + 10, WHITE_TEXT_COLOR, 1);
                    ui.drawCenteredText(text2, x + 81, y + 20, WHITE_TEXT_COLOR, 1);
                } else {
                    ui.drawCenteredText(priceText, x + 81, y + 15, WHITE_TEXT_COLOR, 1);
                }

                if (hovered) {
                    String buyText = confirmText.isEmpty() ? "§7Click to buy." : confirmText;

                    ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_unlocked_dark : lock_unlocked, x + 82 - 25, y + 46 - 19, 50, 50);
                    ui.drawCenteredText(buyText, x + 81, y + 80, WHITE_TEXT_COLOR, 1);
                } else {
                    ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_locked_dark : lock_locked, x + 82 - 25, y + 46 - 19, 50, 50);
                }
            }

            if(index >= currentData.getLastPage()) return;
            if (readOnlyViewerActive) return;

            if(hovered && isMouseInOverlay) {
                if(index != activeInv) {
                    ui.drawRect(x, y, width, height, SLOT_HOVER_COLOR);
                }
            }

            if(activeInv == index) {
                if(shouldWait) {
                    ui.drawRect(x, y, width, height, WAIT_OVERLAY_COLOR);
                    int dots = (int) ((System.currentTimeMillis() / 750) % 3) + 1;

                    String arrowtext = "";

                    ItemStack rightArrow = null;
                    try {
                        rightArrow = MinecraftUtils.containerMenu().getSlot(52).getStack();
                    } catch (IndexOutOfBoundsException e) { }

                    if(rightArrow != null) {
                        if (rightArrow.getItem() == Items.POTION) {
                            String rawText = rightArrow.getName().getString();
                            String cleanedText = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(rawText).replaceAll("");
                            arrowtext = cleanedText;
                        }
                    }

                    String loadingText = "Loading" + ".".repeat(dots);

                    ui.drawCenteredText(loadingText, x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, 1.5f);
                } else {
                    ui.drawRectBorders(x, y + 0.5f, width, height - 0.5f, YELLOW_TEXT_COLOR);
                }
                CustomColor color = (!shouldWait)
                        ? YELLOW_TEXT_COLOR
                        : WHITE_TEXT_COLOR;
            } else if (!hovered || !isMouseInOverlay) {
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
            }

            try {
                if (MinecraftUtils.containerMenu() != null && index == activeInv && !shouldWait && (expectedOverlayType == BankOverlayType.NONE || currentOverlayType == expectedOverlayType)) {
                    ItemStack rightArrow = MinecraftUtils.containerMenu().getSlot(52).getStack();
                    if(rightArrow.getComponents() == null ||
                            rightArrow.getComponents().get(DataComponentTypes.LORE) == null ||
                            rightArrow.getComponents().get(DataComponentTypes.CUSTOM_NAME) == null ||
                            rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA) == null
                    ) return;

                    List<Text> lore = rightArrow.getComponents().get(DataComponentTypes.LORE).lines();

                    String requiredRank = getPageRankRequirement(rightArrow);
                    if (requiredRank != null) {
                        setRankLockedPageLimit(activeInv + 1, requiredRank);
                        return;
                    }
                    clearRankLockedPageLimitIfPastBoundary(activeInv + 1);

                    if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString().contains(">§4>§c>§4>§c>") &&
                            (pageBuyCustomModelData == 0 || rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0) == pageBuyCustomModelData)
                    ) {
                        currentData.setLastPage(Math.max(currentData.getLastPage(), activeInv + 1));
                        try {
                            pageBuyCustomModelData = rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0);
                        } catch (Exception ignored) {}

                        for (Text text : lore) {
                            if (text.getString().contains("§7Price")) {
                                priceText = text.getString();
                                confirmText = "";
                                break;
                            }
                        }
                    } else if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_NAME).getString().contains(">§4>§c>§4>§c>") &&
                            pageBuyCustomModelData != 0 && rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0) != pageBuyCustomModelData
                    ) {
                        confirmText = "§7Click again to confirm.";
                    } else if (rightArrow.getCustomName() != null && rightArrow.getCustomName().getString().contains(String.valueOf(currentData.getLastPage() + 1)) && activeInv == currentData.getLastPage() - 1) {
                        currentData.incrementLastPage();
                        pageBuyCustomModelData = 0;
                        priceText = null;
                        retryLoad();
                    }
                } else {
                    confirmText = "§7Click to go to page " + currentData.getLastPage();
                }
            } catch (Exception ignored) {}
        }

        @Override
        protected void updateValues() {
            if(slots.isEmpty()) return;
            double scale = ui.getScaleFactor();
            if (lastSlotLayoutX == x
                    && lastSlotLayoutY == y
                    && lastSlotLayoutScale == scale
                    && lastSlotLayoutCount == slots.size()) {
                return;
            }
            lastSlotLayoutX = x;
            lastSlotLayoutY = y;
            lastSlotLayoutScale = scale;
            lastSlotLayoutCount = slots.size();

            int i = 0;
            for(SlotWidget slot : slots) {
                slot.setBounds(
                        (int) (x + 18 * (i % 9) * ui.getScaleFactor() + 1),
                        (int) (y + 18 * (i / 9) * ui.getScaleFactor() + 1),
                        (int) (18 * ui.getScaleFactor()),
                        (int) (18 * ui.getScaleFactor())
                );
                i++;
            }
        }

        @Override
        protected boolean onClick(int button) {
            if(!isMouseInOverlay) return true;
            if (isJumpInProgress()) return true;
            if (index == currentData.getLastPage() && isCurrentBankRankLockedAtLimit()) return true;

            if(index < currentData.getLastPage() && index != activeInv && !hasHeldItem()) {
                jumpToPage(index);
            } else if(activeInv == currentData.getLastPage() - 1 && index == currentData.getLastPage()) {
                ScreenHandler currScreenHandler = MinecraftUtils.containerMenu();
                if (currScreenHandler == null) {
                    return true;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return true;
            } else if(index == currentData.getLastPage()) {
                jumpToPage(currentData.getLastPage() - 1);
            }

            return true;
        }

        private static void jumpToPage(int pageIndex) {
            if (BankOverlay.isCharacterBankMissingCharacterId()) return;
            if (pageIndex < 0 || pageIndex >= BankOverlay.getCurrentMaxPages()) return;

            storeActivePageSnapshot();
            clearHoverState(MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> handledScreen ? handledScreen : null);

            if (!jumpToBankPage(pageIndex)) return;
            clearAnnotationCache(activeInv);
            retryLoad();
        }

        public List<ItemStack> getItems() {
            return this.items;
        }

        public void setItems(List<ItemStack> items) {
            this.items = items;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled) clearNameInputFocus();
        }

        private boolean containsSearch(String searchInput, SearchQueryParser.ParsedQuery query, boolean includeActivePage) {
            if (Objects.equals(searchInput, cachedContainsSearchInput)) {
                return cachedContainsSearchResult;
            }

            boolean containsSearch = includeActivePage;
            if (!containsSearch) {
                for(ItemStack stack : items) {
                    if(stack == null || stack.isEmpty()) continue;

                    WynnItemData wynnItem = WynnItemParser.parse(stack).orElse(null);

                    if (SearchQueryParser.matches(stack, wynnItem, query)) {
                        containsSearch = true;
                        break;
                    }
                }
            }

            cachedContainsSearchInput = searchInput;
            cachedContainsSearchResult = containsSearch;
            return containsSearch;
        }

        private void invalidateSearchCache() {
            cachedContainsSearchInput = null;
            cachedContainsSearchResult = false;
        }

        private void setSlotsVisible(boolean visible) {
            if (slotsVisible == visible) return;
            slotsVisible = visible;
            for (SlotWidget slot : slots) {
                slot.setVisible(visible);
            }
        }

        public boolean isNameInputFocused() {
            return sign != null && sign.isInputFocused();
        }

        private void clearNameInputFocus() {
            if (sign != null) sign.clearInputFocus();
        }

        private void positionNameSign() {
            int signY = y - 10;
            if (!WynnExtrasConfig.INSTANCE.disableStickyNameplates && y < topBorder) {
                int nextNameplateY = y + 104 - 10;
                signY = Math.min(topBorder - 10, nextNameplateY - 15);
            }
            sign.setBounds(x, signY, width, 10);
        }
    }

    private static class SlotWidget extends Widget {
        protected ItemStack stack;
        int index;
        final boolean isInventorySlot;
        final int inventoryIndex;
        private Optional<WynnItemData> cachedWynnItem = null;
        private WynntilsBankAdapter.AnnotationHandle cachedAnnotation = null;
        private boolean cachedAnnotationInitialized = false;
        private String cachedSearchInput = null;
        private boolean cachedSearchMatch = false;
        private Optional<CustomColor> cachedHighlightColor = null;
        private ItemStack cachedDurabilityModelInput = null;
        private CustomModelDataComponent cachedDurabilityModelData = null;
        private int cachedDurabilityModelCount = -1;
        private ItemStack cachedDurabilityModelOutput = null;

        public SlotWidget(ItemStack stack, int index, boolean isInventorySlot, int inventoryIndex) {
            super(0, 0, 0, 0);
            this.stack = stack;
            this.index = index;
            this.isInventorySlot = isInventorySlot;
            this.inventoryIndex = inventoryIndex;
        }

        private void drawDirect(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
            this.ui = ui;
            if(!visible || this.ui == null) return;
            hovered = contains(mouseX, mouseY);
            updateValues();
            drawBackground(ctx, mouseX, mouseY, tickDelta);
            drawContent(ctx, mouseX, mouseY, tickDelta);
            drawForeground(ctx, mouseX, mouseY, tickDelta);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (inventoryIndex >= currentData.getLastPage() && !isInventorySlot) return;
            if (!isInventorySlot && isOutsideScissor()) return;

            int itemX = (int) (1 + x / ui.getScaleFactor());
            int itemY = (int) (1 + y / ui.getScaleFactor());
            boolean slotHovered = hovered && (isMouseInOverlay || isInventorySlot);
            exposeBackingSlot(isInventorySlot, inventoryIndex, index, itemX, itemY, slotHovered);

            if(hovered && (isMouseInOverlay || isInventorySlot)) {
                ui.drawRect(x, y, width, height, SLOT_HOVER_COLOR);
            }

            ItemStack dragSplittingPreviewStack = canUseLiveSlot() ? getDragSplittingPreviewStack(getLiveSlotIndex()) : ItemStack.EMPTY;
            if (!dragSplittingPreviewStack.isEmpty()) {
                ctx.drawItem(dragSplittingPreviewStack, itemX, itemY);
                try {
                    ctx.drawStackOverlay(
                            frameTextRenderer != null ? frameTextRenderer : MinecraftClient.getInstance().textRenderer,
                            dragSplittingPreviewStack,
                            itemX,
                            itemY
                    );
                } catch (Exception ignored) {}
                if(slotHovered) {
                    setHoveredSlot(dragSplittingPreviewStack, index, inventoryIndex, itemX, itemY);
                }
                return;
            }

            if(isEmptyStack(stack)) {
                renderSearchOverlay(ctx, stack, null, itemX, itemY);
                return;
            }

            if(slotHovered) {
                setHoveredSlot(stack, index, inventoryIndex, itemX, itemY);
            }

            WynntilsBankAdapter.AnnotationHandle currentAnnotation = WynntilsBankAdapter.getAnnotation(stack).orElse(null);
            if (!cachedAnnotationInitialized || !Objects.equals(cachedAnnotation, currentAnnotation)) {
                cachedAnnotation = currentAnnotation;
                cachedAnnotationInitialized = true;
                cachedWynnItem = null;
                cachedSearchInput = null;
                cachedSearchMatch = false;
                cachedHighlightColor = null;
            }

            if (cachedWynnItem == null) cachedWynnItem = asWynnItem(stack);
            Optional<WynnItemData> item = cachedWynnItem;
            ItemStack renderStack = getCachedRenderStack();

            if (cachedHighlightColor == null) {
                cachedHighlightColor = getHighlightColor(stack);
            }
            renderHighlightOverlay(ctx, cachedHighlightColor, itemX, itemY);

            WynnModItemOverlayBridge.renderPre(ctx, stack, itemX, itemY);
            ctx.drawItem(renderStack, itemX, itemY);

            renderDurabilityOverlay(ctx, stack, itemX, itemY);

            try {
                ctx.drawStackOverlay(
                        frameTextRenderer != null ? frameTextRenderer : MinecraftClient.getInstance().textRenderer,
                        renderStack,
                        itemX,
                        itemY
                );
            } catch (Exception ignored) {}

            renderEmeraldPouchRing(ctx, stack, itemX, itemY);

            WynnModItemOverlayBridge.renderPost(ctx, stack, itemX, itemY);
            renderItemOverlays(ctx, stack, itemX, itemY);

            // Inline cached search overlay (uses the frame-level parsed query).
            if (activeSearchInput != null && !activeSearchInput.isEmpty()) {
                if (!activeSearchInput.equals(cachedSearchInput)) {
                    cachedSearchMatch = SearchQueryParser.matches(stack, item.orElse(null), activeSearchQuery);
                    cachedSearchInput = activeSearchInput;
                }
                if (cachedSearchMatch) {
                    RenderUtils.drawRectBorders(ctx, SEARCH_MATCH_COLOR, itemX, itemY, 16, 16, 1);
                } else {
                    RenderUtils.drawRect(ctx, SEARCH_DIM_COLOR, itemX - 1, itemY - 1, 18, 18);
                }
            }

        }

        private boolean isOutsideScissor() {
            return x + width <= scissorx1 || x >= scissorx2 || y + height <= scissory1 || y >= scissory2;
        }

        public void setStack(ItemStack stack) {
            if (stack != this.stack) {
                this.stack = stack;
                this.cachedWynnItem = null;
                this.cachedAnnotation = null;
                this.cachedAnnotationInitialized = false;
                this.cachedSearchInput = null;
                this.cachedHighlightColor = null;
                this.cachedDurabilityModelInput = null;
                this.cachedDurabilityModelData = null;
                this.cachedDurabilityModelCount = -1;
                this.cachedDurabilityModelOutput = null;
            }
        }

        private ItemStack getCachedRenderStack() {
            if (!durabilityOverlayEnabled || !durabilityRenderInInv) return stack;
            return withoutVanillaDurabilityModelDataCached(stack);
        }

        private ItemStack withoutVanillaDurabilityModelDataCached(ItemStack renderStack) {
            CustomModelDataComponent modelData = renderStack.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA);
            int count = renderStack.getCount();
            if (renderStack == cachedDurabilityModelInput
                    && modelData == cachedDurabilityModelData
                    && count == cachedDurabilityModelCount) {
                return cachedDurabilityModelOutput;
            }

            ItemStack output = withoutVanillaDurabilityModelData(renderStack);
            cachedDurabilityModelInput = renderStack;
            cachedDurabilityModelData = modelData;
            cachedDurabilityModelCount = count;
            cachedDurabilityModelOutput = output;
            return output;
        }

        private boolean canPickupAll(int mouseButton, boolean doubleClick) {
            if (mouseButton != 0 || !doubleClick) return false;
            if (heldItem == null || heldItem.isEmpty() || heldItem.getItem() == Items.AIR) return false;
            if (stack == null || stack.isEmpty()) return false;
            return ItemStack.areItemsAndComponentsEqual(stack, heldItem);
        }

        private SlotActionType determineActionType(int mouseButton, boolean doubleClick) {
            SlotActionType actionType = SlotActionType.PICKUP;

            if (canPickupAll(mouseButton, doubleClick)) return SlotActionType.PICKUP_ALL;
            if (isShiftHeld()) return SlotActionType.QUICK_MOVE;
            if(mouseButton == 1) return actionType;

            long now = System.currentTimeMillis();
            if (heldItem != null && heldItem.getItem() != Items.AIR) {
                if (now - lastClickTime < DOUBLE_CLICK_INTERVAL_MS && lastClickedSlot != null &&
                        lastClickedSlot.first() == inventoryIndex && lastClickedSlot.second() == index) {
                    actionType = SlotActionType.PICKUP_ALL;
                }
            }
            lastClickTime = now;

            return actionType;
        }

        private boolean isOverlayDoubleClick(int button, boolean vanillaDoubleClick) {
            if (button != 0) return false;
            if (vanillaDoubleClick) return true;

            long now = System.currentTimeMillis();
            return now - lastClickTime < DOUBLE_CLICK_INTERVAL_MS
                    && lastClickedSlot != null
                    && lastClickedSlot.first() == inventoryIndex
                    && lastClickedSlot.second() == index;
        }

        private boolean canBulkQuickMove(int button, boolean doubleClick) {
            return button == 0 && doubleClick && isShiftHeld() && hasHeldItem() && lastQuickMoved != null && !lastQuickMoved.isEmpty();
        }

        private boolean bulkQuickMoveMatchingStacks(ScreenHandler liveHandler, int clickedSlotIndex, int button) {
            if (MinecraftClient.getInstance().interactionManager == null || MinecraftClient.getInstance().player == null) return false;
            if (clickedSlotIndex < 0 || clickedSlotIndex >= liveHandler.slots.size()) return false;

            Slot clickedSlot = liveHandler.slots.get(clickedSlotIndex);
            ItemStack targetStack = lastQuickMoved;
            if (targetStack.isEmpty()) return false;
            boolean movedAny = false;
            for (Slot slot : liveHandler.slots) {
                if (slot == null || slot.inventory != clickedSlot.inventory || !slot.hasStack()) continue;
                if (!slot.canTakeItems(MinecraftClient.getInstance().player)) continue;
                if (!ItemStack.areItemsAndComponentsEqual(slot.getStack(), targetStack)) continue;

                MinecraftClient.getInstance().interactionManager.clickSlot(
                        liveHandler.syncId,
                        slot.id,
                        button,
                        SlotActionType.QUICK_MOVE,
                        MinecraftClient.getInstance().player
                );
                movedAny = true;
            }
            return movedAny;
        }

        private boolean clickLiveSlot(int button) {
            return clickLiveSlot(button, false);
        }

        private boolean clickLiveSlot(int button, boolean doubleClick) {
            if(index == 4 && isInventorySlot) return true; //Ingredient pouch, clicking it within the bank overlay crashes the game
            if(index == 34 && isInventorySlot) return true; //Compass, clicking it within the bank overlay crashes the game
            if(index == 35 && isInventorySlot) return true; //Content book, clicking it within the bank overlay crashes the game

            ScreenHandler liveHandler = getLiveScreenHandlerForClick();
            int slotIndex = index + (isInventorySlot ? 54 : 0);
            if (liveHandler == null || slotIndex < 0 || slotIndex >= liveHandler.slots.size()) {
                resetInteractionBlockers();
                return false;
            }

            boolean overlayDoubleClick = isOverlayDoubleClick(button, doubleClick);

            if (canBulkQuickMove(button, overlayDoubleClick)) {
                boolean moved = bulkQuickMoveMatchingStacks(liveHandler, slotIndex, button);
                if (moved) {
                    bankSyncid = liveHandler.syncId;
                    clearAnnotationCache(inventoryIndex);
                    lastClickedSlot = new Pair<>(inventoryIndex, index);
                    lastClickTime = System.currentTimeMillis();
                }
                return moved;
            }

            SlotActionType action = determineActionType(button, overlayDoubleClick);

            ItemStack oldHeld = heldItem;
            if (action == SlotActionType.QUICK_MOVE) {
                ItemStack clickedStack = liveHandler.slots.get(slotIndex).getStack();
                lastQuickMoved = clickedStack.isEmpty() ? ItemStack.EMPTY : clickedStack.copy();
            }
            heldItem = getHeldItem(slotIndex, action, button);

            if(heldItem.getCustomName() != null) {
                if ((heldItem.getCustomName().getString().contains("Pouch") || heldItem.getCustomName().getString().contains("Potions")) && button == 1) {
                    heldItem = oldHeld == null ? Items.AIR.getDefaultStack() : oldHeld;
                    return true;
                }
            }

            if (shouldCancelEmeraldPouch(oldHeld, heldItem)) {
                heldItem = Items.AIR.getDefaultStack();
            }

            if (MinecraftClient.getInstance().interactionManager == null) return false;

            MinecraftClient.getInstance().interactionManager.clickSlot(liveHandler.syncId, slotIndex, button, action, MinecraftClient.getInstance().player);
            bankSyncid = liveHandler.syncId;
            clearAnnotationCache(inventoryIndex);
            lastClickedSlot = new Pair<>(inventoryIndex, index);
            lastClickTime = System.currentTimeMillis();
            return true;
        }

        private int getLiveSlotIndex() {
            return index + (isInventorySlot ? 54 : 0);
        }

        private boolean canUseLiveSlot() {
            if(index == 4 && isInventorySlot) return false;
            if(index == 34 && isInventorySlot) return false;
            if(index == 35 && isInventorySlot) return false;
            if(!isInventorySlot && inventoryIndex != activeInv) return false;
            if(inventoryIndex >= currentData.getLastPage() && !isInventorySlot) return false;

            ScreenHandler liveHandler = getLiveScreenHandlerForClick();
            int slotIndex = getLiveSlotIndex();
            return liveHandler != null && slotIndex >= 0 && slotIndex < liveHandler.slots.size();
        }

        @Override
        protected boolean onClick(int button) {
            if (button == 2) {
                if (stack != null && !stack.isEmpty() && searchbar2 != null) {
                    String itemName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(stack.getName().getString()).replaceAll("").trim();
                    if (!itemName.isEmpty()) {
                        String previousInput = searchbar2.getInput();
                        searchbar2.setInput(itemName);
                        if (!Objects.equals(previousInput, searchbar2.getInput())) {
                            for (PageWidget page : pages) page.invalidateSearchCache();
                        }
                    }
                }
                return true;
            }
            if (isReloading) return false;
            if(shouldWait) return false;
            if(!isMouseInOverlay && !isInventorySlot) return false;
            if(inventoryIndex >= currentData.getLastPage() && !isInventorySlot) return false;

            if(activeInv == inventoryIndex || isInventorySlot) {
                clickLiveSlot(button);
            } else if(!hasHeldItem()) {
                if (isJumpInProgress()) return true;
                if (BankOverlay.isCharacterBankMissingCharacterId()) return false;
                storeActivePageSnapshot();
                if (!jumpToBankPage(inventoryIndex)) return true;
                clearAnnotationCache(inventoryIndex);
            }
            return true;
        }

        public boolean mouseClicked(double mx, double my, int button, boolean doubleClick) {
            if (!visible || !enabled) return false;
            if (contains((int) mx, (int) my)) {
                setFocused(true);
                if (button == 2) {
                    return onClick(button);
                }
                if (isReloading) return false;
                if(shouldWait) return false;
                if(!isMouseInOverlay && !isInventorySlot) return false;
                if(inventoryIndex >= currentData.getLastPage() && !isInventorySlot) return false;

                if(activeInv == inventoryIndex || isInventorySlot) {
                    if (beginDragSplitting(this, button)) return true;
                    if (clickLiveSlot(button, doubleClick)) {
                        pendingMouseTweaksRightClick = null;
                        return true;
                    }
                    return false;
                } else if(!hasHeldItem()) {
                    if (isJumpInProgress()) return true;
                    if (BankOverlay.isCharacterBankMissingCharacterId()) return false;
                    storeActivePageSnapshot();
                    if (!jumpToBankPage(inventoryIndex)) return true;
                    clearAnnotationCache(inventoryIndex);
                }
                return true;
            }
            setFocused(false);
            return false;
        }

    }

    public static class NameSignWidget extends Widget {
        private TextInputWidget textInputWidget;
        int index;
        private String lastSavedPageName = null;

        public NameSignWidget(int index) {
            super(0, 0, 0, 0);
            this.index = index;
            textInputWidget = new TextInputWidget(x, y, width, height, 3, 1, 1);
            textInputWidget.setBackgroundColor(null);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1 - 12, scissorx2, scissory2);
            ui.updateContext(ctx, ui.getScaleFactor(), 0, 0);

            String defaultPageName = Pages.getBankPageNames().getOrDefault(index, "Page " + (index + 1));
            boolean editingEmptyName = textInputWidget.isFocused() && textInputWidget.getInput().isEmpty();
            String pageName = textInputWidget.getInput().isEmpty() ? defaultPageName : textInputWidget.getInput();
            String visiblePageName = editingEmptyName ? "" : pageName;

            drawDynamicNameSign(ctx, visiblePageName, x, y + 12);

            if (!readOnlyViewerActive && !editingEmptyName && !Objects.equals(lastSavedPageName, pageName)) {
                Pages.getBankPageNames().put(index, pageName);
                lastSavedPageName = pageName;
            }

            textInputWidget.setTextColor((activeInv == index && !shouldWait) ? GOLD_TEXT_COLOR : WHITE_TEXT_COLOR);
            textInputWidget.setBounds(x, y, width, height);
            if (!editingEmptyName && !Objects.equals(textInputWidget.getInput(), pageName)) {
                textInputWidget.setInput(pageName);
            }
            textInputWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled) return false;
            if (contains((int) mx, (int) my)) {
                setFocused(true);
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return textInputWidget != null && textInputWidget.mouseClicked(mx, my, button);
            }
            setFocused(false);
            if (textInputWidget != null) {
                textInputWidget.setFocused(false);
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            return textInputWidget != null && textInputWidget.mouseDragged(mx, my, button, dx, dy);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            return textInputWidget != null && textInputWidget.mouseReleased(mx, my, button);
        }

        public boolean isInputFocused() {
            return textInputWidget != null && textInputWidget.isFocused();
        }

        private void clearInputFocus() {
            setFocused(false);
            if (textInputWidget != null) textInputWidget.setFocused(false);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return textInputWidget != null && textInputWidget.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return textInputWidget != null && textInputWidget.charTyped(chr, modifiers);
        }
    }

    private static class ReadOnlyNoticeWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawCenteredText("§7View only, opened via /we bank", x + width / 2f, y + height / 2f,
                    WHITE_TEXT_COLOR, 0.85f);
        }
    }

    private static class QuickActionWidget extends Widget {
        public QuickActionWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            try {
                if(hovered && MinecraftUtils.containerMenu().getSlot(46) != null && MinecraftUtils.containerMenu().getSlot(46).getStack() != null) {
                    ctx.drawTooltip(
                        MinecraftClient.getInstance().textRenderer,
                        MinecraftUtils.containerMenu().getSlot(46).getStack().getTooltip(
                            Item.TooltipContext.DEFAULT,
                            MinecraftClient.getInstance().player,
                            TooltipType.BASIC
                        ),
                        mouseX,
                        mouseY
                    );
                }
            } catch (Exception ignored) {}
        }

        @Override
        protected boolean onClick(int button) {
            if (readOnlyViewerActive) return true;
            if (hasHeldItem() || isJumpInProgress()) return true;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if (BankOverlay.isCharacterBankMissingCharacterId()) return true;
            ScreenHandler currScreenHandler = MinecraftUtils.containerMenu();
            if(currScreenHandler == null) { return false; }
            if (MinecraftClient.getInstance().options.sneakKey.isPressed()) {
                shiftClickOnSlot(46, currScreenHandler.syncId, button, currScreenHandler.getStacks());
            } else {
                clickOnSlot(46, currScreenHandler.syncId, button, currScreenHandler.getStacks());
            }
            return true;
        }
    }

    private static class SwitchButtonWidget extends Widget {
        public SwitchButtonWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        }

        @Override
        protected boolean onClick(int button) {
            if (readOnlyViewerActive) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                setReadOnlyViewerType(getNextReadOnlyViewerType());
                return true;
            }
            if (isReloading) return false;
            if (hasHeldItem() || isJumpInProgress()) return true;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            BankOverlayType targetType = currentOverlayType == BankOverlayType.CHARACTER
                    ? BankOverlayType.ACCOUNT
                    : BankOverlayType.CHARACTER;
            switchBankAndJumpToPage(targetType, 0, true);
            return true;
        }
    }

    private static class AllCharactersButtonWidget extends Widget {
        public AllCharactersButtonWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String text = allCharactersBrowseMode ? "Disable All Characters Mode" : "Enable All Characters Mode";
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, 1f);
        }

        @Override
        protected boolean onClick(int button) {
            if (isReloading) return false;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            setAllCharactersBrowseMode(!allCharactersBrowseMode);
            if (allCharactersBrowseMode) {
                if (!readOnlyViewerActive) saveActivePageSnapshot();
                // Force cross-class reload
                lastCrossClassSearchQuery = "";
                crossClassPages.clear();
                crossClassSearchActive = false;
                targetOffset = 0;
                actualOffset = 0;
            } else {
                crossClassPages.clear();
                lastCrossClassSearchQuery = "";
                crossClassSearchActive = false;
                targetOffset = 0;
                actualOffset = 0;
            }
            return true;
        }
    }

    private static class ReloadBankWidget extends Widget {
        public ReloadBankWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String text;
            float scale = 1.1f;
            if (isReloading) {
                text = "Reloading " + (reloadCurrentPage + 1) + "/" + reloadTotalPages;
                scale = 0.85f;
            } else {
                text = "Reload all pages";
            }
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, scale);
        }

        @Override
        protected boolean onClick(int button) {
            if (readOnlyViewerActive) return true;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            if (isReloading) {
                // Cancel reload
                stopReloadAndReturnToOriginalPage();
            } else {
                // Start reload
                if (BankOverlay.isCharacterBankMissingCharacterId()) return true;
                reloadOriginalPage = activeInv;
                reloadTotalPages = Math.min(Math.max(currentData.getLastPage(), 1), BankOverlay.getCurrentMaxPages());
                if (reloadTotalPages <= 0) return false;
                reloadCurrentPage = 0;
                isReloading = true;
                resetReloadPageReadiness();
                reloadNextPageCustomModelData = null;
                if (!jumpToBankPage(0)) {
                    isReloading = false;
                    return false;
                }
                retryLoad();
            }
            return true;
        }
    }

    private static class ToggleOverlayWidget extends Widget {
        public ToggleOverlayWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonCustom(x, y, width, height, 5, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            ui.drawCenteredText("Click to " + (WynnExtrasConfig.INSTANCE.toggleBankOverlay ? "disable" : "enable") + " the Bank Overlay", x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            WynnExtrasConfig.INSTANCE.toggleBankOverlay = !WynnExtrasConfig.INSTANCE.toggleBankOverlay;
            if(WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                activeInv = Math.max(0, getCurrentBankPageNumber());
            }
            WynnExtrasConfig.save();
            return false;
        }
    }

    private static class ScrollBarWidget extends Widget {
        ScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseY = 0;

        public ScrollBarWidget() {
            super(0, 0, 0, 0);
            this.scrollBarButtonWidget = new ScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
        }

        private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
            float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.max(0, Math.min(relativeY, scrollAreaHeight));

            float scrollPercent = relativeY / scrollAreaHeight;

            targetOffset = scrollPercent * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderFade(x, y, width, height, 5, WynnExtrasConfig.INSTANCE.darkmodeToggle ? 1 : 0);

            int maxOffset = getMaxScrollOffset(shownPages);
            int buttonHeight = 30;
            int scrollAreaHeight = height - buttonHeight;

            if (scrollBarButtonWidget.isHold) {
                setOffset(mouseY, maxOffset, scrollAreaHeight);
                actualOffset = targetOffset;
            }

            int yPos = maxOffset == 0 ? y : (int) (y + scrollAreaHeight * Math.min((actualOffset / maxOffset), 1));
            scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            int maxOffset = getMaxScrollOffset(shownPages);
            int buttonHeight = 30;
            int scrollAreaHeight = height - buttonHeight;

            setOffset(currentMouseY, maxOffset, scrollAreaHeight);

            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarButtonWidget.mouseReleased(mx, my, button);
            return true;
        }

        private static class ScrollBarButtonWidget extends Widget {
            private boolean isHold;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHold = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButtonCustom(x, y, width, height, 5, hovered || isHold, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            }

            @Override
            protected boolean onClick(int button) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                isHold = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                isHold = false;
                return true;
            }
        }
    }

    /**
     * Widget for displaying cross-class search results from other characters
     */
    public static class CrossClassPageWidget extends Widget {
        Identifier bankTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank.png");
        Identifier bankTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank_dark.png");
        Identifier bankInventoryTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank_inv.png");
        Identifier bankInventoryTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank_dark_inv.png");

        private final String characterId;
        private final String characterNickname;
        private final int characterLevel;
        private final int pageNumber;
        private final List<ItemStack> items;
        private final List<ItemStack> armorItems;
        private final CrossClassBankSearch.SearchResult.Type type;
        private final List<SlotWidget> slots = new ArrayList<>();
        private final List<WynntilsBankAdapter.AnnotationHandle> annotations = new ArrayList<>();
        private final List<ItemStack> annotationStacks = new ArrayList<>();
        private final List<Object> annotationComponents = new ArrayList<>();
        private int topBorder;
        private int botBorder;
        private boolean slotsVisible = true;
        private int lastSlotLayoutX = Integer.MIN_VALUE;
        private int lastSlotLayoutY = Integer.MIN_VALUE;
        private double lastSlotLayoutScale = Double.NaN;
        private int lastSlotLayoutCount = -1;
        private boolean pagePlaceholder = false;
        private String placeholderRequiredRank = null;

        public CrossClassPageWidget(String characterId, String characterNickname, int characterLevel, int pageNumber, List<ItemStack> items, List<ItemStack> armorItems, CrossClassBankSearch.SearchResult.Type type, int topBorder, int botBorder) {
            super(0, 0, 0, 0);
            this.characterId = characterId;
            this.characterNickname = characterNickname;
            this.characterLevel = characterLevel;
            this.pageNumber = pageNumber;
            this.items = items != null ? items : new ArrayList<>();
            this.armorItems = armorItems != null ? armorItems : EMPTY_PLAYER_ARMOR;
            this.type = type == null ? CrossClassBankSearch.SearchResult.Type.BANK_PAGE : type;
            this.topBorder = topBorder;
            this.botBorder = botBorder;
        }

        public void setPagePlaceholder(String requiredRank) {
            this.pagePlaceholder = true;
            this.placeholderRequiredRank = requiredRank;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (ui == null) return;
            if (y > botBorder || y + height < topBorder) {
                setSlotsVisible(false);
                return;
            }
            setSlotsVisible(true);

            // Draw solid background behind label area to cover vanilla UI
            String bgColor = WynnExtrasConfig.INSTANCE.darkmodeToggle ? "2c2d2f" : "81644b";
            ui.drawRect(x, y - 11, width, 11, CustomColor.fromHexString(bgColor));

            if (pagePlaceholder) {
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
            } else {
                Identifier texture = isPlayerInventoryPage()
                        ? (WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankInventoryTextureDark : bankInventoryTexture)
                        : (WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankTextureDark : bankTexture);
                ui.drawImage(texture, x, y, width, height);
            }

            // Draw character label above the page
            boolean localBankPage = isLocalBankPage();
            String name = getDisplayName();
            String levelStr = !localBankPage && !isStaticStoragePage() && characterLevel > 0 ? " Lv." + characterLevel : "";
            String pageLabel = isPlayerInventoryPage() ? "Inventory" : "Page " + (pageNumber + 1);
            String prefix = isStaticStoragePage() || localBankPage ? "§e" : "§e@";
            drawCrossClassLabel(ctx, prefix + name + levelStr + " §7" + pageLabel);

            if (pagePlaceholder) {
                setSlotsVisible(false);
                Identifier lockTexture = WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_locked_dark : lock_locked;
                ui.drawImage(lockTexture, x + 82 - 25, y + 46 - 19, 50, 50);
                boolean rankLockedPlaceholder = placeholderRequiredRank != null && !placeholderRequiredRank.isBlank();
                String line1 = rankLockedPlaceholder ? "§c✖ §7Requires §f" + placeholderRequiredRank + "§7." : "§c✖ §7Page not unlocked.";
                String line2 = rankLockedPlaceholder ? "§7Upgrade to unlock this page." : "§7Open the bank to buy it.";
                ui.drawCenteredText(line1, x + 81, y + 14, WHITE_TEXT_COLOR, 0.9f);
                ui.drawCenteredText(line2, x + 81, y + 78, WHITE_TEXT_COLOR, 0.8f);
                return;
            }

            if (items.isEmpty() && !hasAnyArmorItem()) {
                setSlotsVisible(false);
                return;
            }

            // Create slots if needed
            if (slots.isEmpty()) {
                int maxInventorySlots = isPlayerInventoryPage() ? 36 : Math.min(items.size(), 45);
                for (int i = 0; i < maxInventorySlots; i++) {
                    ItemStack itemStack = i < items.size() ? items.get(i) : Items.AIR.getDefaultStack();
                    CrossClassSlotWidget slot = new CrossClassSlotWidget(itemStack == null ? null : itemStack.copy(), i);
                    slots.add(slot);
                }
                if (isPlayerInventoryPage()) {
                    for (int i = 0; i < Math.min(armorItems.size(), 4); i++) {
                        ItemStack itemStack = armorItems.get(i);
                        CrossClassSlotWidget slot = new CrossClassSlotWidget(itemStack == null ? null : itemStack.copy(), 36 + i);
                        slots.add(slot);
                    }
                }
                updateValues();
            }

            ensureCacheSize(annotations, slots.size(), null);
            ensureCacheSize(annotationStacks, slots.size(), null);
            ensureCacheSize(annotationComponents, slots.size(), null);

            for (int i = 0; i < slots.size(); i++) {
                SlotWidget slot = slots.get(i);
                ItemStack stack;
                if (isPlayerInventoryPage() && i >= 36) {
                    int armorIndex = i - 36;
                    stack = armorIndex < armorItems.size() ? armorItems.get(armorIndex) : Items.AIR.getDefaultStack();
                } else {
                    stack = i < items.size() ? items.get(i) : Items.AIR.getDefaultStack();
                }
                applyAnnotation(stack, annotations, annotationStacks, annotationComponents, i);
                slot.setStack(stack);
                slot.drawDirect(ctx, mouseX, mouseY, tickDelta, ui);
            }
        }

        private void drawCrossClassLabel(DrawContext ctx, String text) {
            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1 - 12, scissorx2, scissory2);
            ui.updateContext(ctx, ui.getScaleFactor(), 0, 0);
            ui.drawText(text, x + 2, y - 9, YELLOW_TEXT_COLOR, 0.9f);
            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
            ui.updateContext(ctx, ui.getScaleFactor(), 0, 0);
        }

        private boolean isPlayerInventoryPage() {
            return type == CrossClassBankSearch.SearchResult.Type.PLAYER_INVENTORY;
        }

        private boolean isMiscBucketPage() {
            return type == CrossClassBankSearch.SearchResult.Type.MISC_BUCKET;
        }

        private boolean isTomeBookshelfPage() {
            return type == CrossClassBankSearch.SearchResult.Type.TOME_BOOKSHELF;
        }

        private boolean isStaticStoragePage() {
            return isMiscBucketPage() || isTomeBookshelfPage();
        }

        private String getDisplayName() {
            if (isMiscBucketPage()) return "Misc Bucket";
            if (isTomeBookshelfPage()) return "Tome Bookshelf";
            if (isAccountBank()) return "Account Bank";
            if (isCurrentCharacter()) return "Character Bank";
            if (characterNickname != null && !characterNickname.isEmpty()) return characterNickname;
            if (characterId == null) return "Unknown";
            return characterId.length() > 8 ? characterId.substring(0, 8) + "..." : characterId;
        }

        private boolean hasAnyArmorItem() {
            for (ItemStack stack : armorItems) {
                if (stack != null && !stack.isEmpty()) return true;
            }
            return false;
        }

        private boolean isCurrentCharacter() {
            return characterId != null && characterId.equals(BankOverlay.currentCharacterID);
        }

        private boolean isAccountBank() {
            return "__account__".equals(characterId);
        }

        private boolean isLocalBankPage() {
            return isCurrentCharacter() || isAccountBank();
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (ui == null) return;
            if (y > botBorder || y + height < topBorder) return;
            if (pagePlaceholder) return;

            if (isLocalBankPage()) {
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
                return;
            }

            ui.drawRect(x, y, width, height, CustomColor.fromHSV(40, 0.4f, 0.8f, 0.2f));

            String borderColor = isCurrentCharacter()
                    ? "55FF55"
                    : isAccountBank() ? "5555FF"
                    : isMiscBucketPage() ? "55FFFF"
                    : isTomeBookshelfPage() ? "AA55FF"
                    : "FFAA00";
            ui.drawRectBorders(x, y + 0.5f, width, height - 0.5f, CustomColor.fromHexString(borderColor));

            // Hint text
            String hint;
            if (isStaticStoragePage()) {
                hint = "";
            } else if (isCurrentCharacter()) {
                hint = currentOverlayType == BankOverlayType.CHARACTER ? "§7Click to go to page" : "§7Click to switch to character bank";
            } else if (isAccountBank()) {
                hint = currentOverlayType == BankOverlayType.ACCOUNT ? "§7Click to go to page" : "§7Click to switch to account bank";
            } else {
                hint = "§7Click to /class";
            }
            ui.drawText(hint, x + 2, y + height - 4, GRAY_TEXT_COLOR, 0.6f);
        }

        @Override
        protected boolean onClick(int button) {
            if (pagePlaceholder) return true;
            if (isStaticStoragePage()) return true;
            if (isJumpInProgress()) return true;
            if (button == 0) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

                if (isCurrentCharacter()) {
                    if (isPlayerInventoryPage()) return true;
                    switchBankAndJumpToPage(BankOverlayType.CHARACTER, pageNumber);
                    return true;
                } else if (isAccountBank()) {
                    switchBankAndJumpToPage(BankOverlayType.ACCOUNT, pageNumber);
                    return true;
                } else {
                    // Other character - save search, close bank, then run /class with auto-select
                    final String snapName = (characterNickname != null && !characterNickname.isEmpty())
                            ? characterNickname : null;
                    final int snapLevel = characterLevel;

                    // Tell ClassSelectionOverlay which character to auto-click
                    setTargetCharacterForClassMenu(null, snapName, snapLevel);

                    // Save current search so it persists after class swap
                    if (searchbar2 != null && searchbar2.getInput() != null && !searchbar2.getInput().isEmpty()) {
                        savedCrossClassSearch = searchbar2.getInput().replace("@", "").trim();
                        savedCrossClassSearchTime = System.currentTimeMillis();
                    }

                    // Close the bank screen properly to prevent stuck-in-inventory bug
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        mc.player.closeHandledScreen();
                    }
                    mc.setScreen(null);

                    // Reset bank overlay state
                    crossClassPages.clear();
                    crossClassSearchActive = false;
                    BankOverlay.currentOverlayType = BankOverlayType.NONE;
                    clickedClassSelectionEntity = false;

                    // Send initial /class after screen closes
                    julianh06.wynnextras.utils.TickScheduler.runAfterTicks(5, () -> PartyState.sendCommand("class"));

                    // Step 1: wait until we're in the lobby AND the blackscreen title overlay has cleared
                    julianh06.wynnextras.utils.TickScheduler.runWhen(
                        () -> isInCharacterSelectionLobby() && isLobbyBlackscreenGone(),
                        () -> {
                            MinecraftClient mc2 = MinecraftClient.getInstance();
                            julianh06.wynnextras.utils.TickScheduler.runUntil(
                                () -> clickedClassSelectionEntity,
                                () -> {
                                    if(mc2.world == null || mc2.player == null || mc2.getNetworkHandler() == null) return;

                                    for (Entity e : mc2.world.getEntities()) {
                                        if (e instanceof InteractionEntity && mc2.player.distanceTo(e) < 5) {
                                            clickedClassSelectionEntity = true;
                                            mc2.getNetworkHandler().sendPacket(
                                                    PlayerInteractEntityC2SPacket.interact(e, mc2.player.isSneaking(), Hand.MAIN_HAND)
                                            );
                                            break;
                                        }
                                    }
                                }
                            );
                        }
                    );
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled) return false;
            if (isJumpInProgress()) return contains((int) mx, (int) my);
            if (isStaticStoragePage() && contains((int) mx, (int) my)) return true;
            for (int i = slots.size() - 1; i >= 0; i--) {
                if (slots.get(i).mouseClicked(mx, my, button)) return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        protected void updateValues() {
            if (ui == null) return;
            if (slots.isEmpty()) return;
            double scale = ui.getScaleFactor();
            if (lastSlotLayoutX == x
                    && lastSlotLayoutY == y
                    && lastSlotLayoutScale == scale
                    && lastSlotLayoutCount == slots.size()) {
                return;
            }
            lastSlotLayoutX = x;
            lastSlotLayoutY = y;
            lastSlotLayoutScale = scale;
            lastSlotLayoutCount = slots.size();

            for (int i = 0; i < slots.size(); i++) {
                SlotWidget slot = slots.get(i);
                int column;
                int row;
                if (isPlayerInventoryPage() && i >= 36) {
                    column = 1 + (i - 36) * 2;
                    row = 4;
                } else {
                    column = i % 9;
                    row = i / 9;
                }
                slot.setBounds(
                        (int) (x + 18 * column * ui.getScaleFactor() + 1),
                        (int) (y + 18 * row * ui.getScaleFactor() + 1),
                        (int) (18 * ui.getScaleFactor()),
                        (int) (18 * ui.getScaleFactor())
                );
            }
        }

        public String getCharacterId() {
            return characterId;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        private void setSlotsVisible(boolean visible) {
            if (slotsVisible == visible) return;
            slotsVisible = visible;
            for (SlotWidget slot : slots) {
                slot.setVisible(visible);
            }
        }
    }

    /**
     * Slot widget for cross-class results (view-only, no interaction)
     */
    public static class CrossClassSlotWidget extends SlotWidget {
        public CrossClassSlotWidget(ItemStack stack, int slotIndex) {
            super(stack, slotIndex, false, -1);
        }

        @Override
        protected boolean onClick(int button) {
            if (button == 2) {
                if (stack != null && !stack.isEmpty() && searchbar2 != null) {
                    String itemName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(stack.getName().getString()).replaceAll("").trim();
                    if (!itemName.isEmpty()) {
                        String previousInput = searchbar2.getInput();
                        searchbar2.setInput(itemName);
                        if (!Objects.equals(previousInput, searchbar2.getInput())) {
                            for (PageWidget page : pages) page.invalidateSearchCache();
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

}
