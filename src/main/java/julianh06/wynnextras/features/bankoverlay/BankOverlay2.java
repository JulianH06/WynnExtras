package julianh06.wynnextras.features.bankoverlay;

import com.wynntils.features.inventory.*;
import com.wynntils.utils.wynn.WynnUtils;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.tooltips.ItemGuessFeature;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.personal.AccountBankContainer;
import com.wynntils.models.containers.containers.personal.BookshelfContainer;
import com.wynntils.models.containers.containers.personal.CharacterBankContainer;
import com.wynntils.models.containers.containers.personal.MiscBucketContainer;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.gear.type.GearTier;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ContainerUtils;
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
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.mixin.Accessor.*;
import julianh06.wynnextras.mixin.Invoker.*;
import julianh06.wynnextras.mixin.ItemFavoriteFeatureAccessor;
import julianh06.wynnextras.mixin.ItemGuessFeatureAccessor;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.SearchQueryParser;
import julianh06.wynnextras.utils.UI.*;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
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
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.Collections;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;
import static com.wynntils.utils.wynn.ContainerUtils.shiftClickOnSlot;
import static julianh06.wynnextras.features.inventory.BankOverlay.*;

public class BankOverlay2 extends WEHandledScreen {
    private static final Pattern POTIONS_USES_PATTERN = Pattern.compile("\\[(\\d+)/(\\d+)]");
    private static final Pattern MINECRAFT_FORMATTING_CODE_PATTERN = Pattern.compile("\u00a7[0-9a-fk-or]");
    private static final EmeraldUnits[] EMERALD_UNITS = EmeraldUnits.values();
    private static ItemStack hoveredSlot = Items.AIR.getDefaultStack();
    private static int hoveredX = -1;
    private static int hoveredY = -1;
    private static int hoveredIndex = -1;
    private static int hoveredInvIndex = -1;

    static ItemHighlightFeature itemHighlightFeature;
    static ItemTextOverlayFeature itemTextOverlayFeature;
    static UnidentifiedItemIconFeature unidentifiedItemIconFeature;
    static ItemFavoriteFeature itemFavoriteFeature;
    static DurabilityOverlayFeature durabilityOverlayFeature;
    private static ItemGuessFeature itemGuessFeature;
    private static InventoryEmeraldCountFeature emeraldCountFeature;
    private static int cachedEmeraldAmount = Integer.MIN_VALUE;
    private static String[] cachedEmeraldAmounts = new String[0];
    private static boolean itemHighlightEnabled = false;
    private static boolean itemTextOverlayEnabled = false;
    private static boolean unidentifiedItemIconEnabled = false;
    private static boolean itemFavoriteEnabled = false;
    private static boolean durabilityOverlayEnabled = false;
    private static final Identifier buttonBackground = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg.png");
    private static final Identifier buttonBackgroundShort = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort.png");
    private static final Identifier buttonBackgroundDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg_dark.png");
    private static final Identifier buttonBackgroundShortDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort_dark.png");

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
    private final SimpleInventory tooltipInventory = new SimpleInventory(1);
    private final Slot tooltipSlot = new Slot(tooltipInventory, 0, 0, 0);
    private final List<ItemStack> liveBankPageItems = new ArrayList<>(45);
    private final List<ItemStack> livePlayerInventoryItems = new ArrayList<>(36);

    private static float targetOffset = 0;
    static float actualOffset = 0;
    private static TextRenderer frameTextRenderer;
    private static int bankSyncid = 0;
    private static int xFitAmount = 0;
    private static int yFitAmount = 0;
    private static float pageBuyCustomModelData = 0;

    private static final List<PageWidget> pages = new ArrayList<>();
    private static final Map<Integer, List<ItemStack>> annotationStackCache = new HashMap<>();
    private static final Map<Integer, List<Object>> annotationComponentCache = new HashMap<>();
    private static final EnumMap<BankOverlayType, HashMap<Integer, EasyTextInput>> BANK_PAGE_NAME_INPUTS_BY_TYPE =
            new EnumMap<>(BankOverlayType.class);
    private static InventoryWidget inventoryWidget = null;
    private static SwitchButtonWidget switchButtonWidget = null;
    private static QuickActionWidget quickActionWidget = null;
    private static TextInputWidget searchbar2 = null;
    private static ToggleOverlayWidget toggleOverlayWidget = null;
    static ScrollBarWidget scrollBarWidget = null;

    // Cross-class search
    private static List<CrossClassPageWidget> crossClassPages = new ArrayList<>();
    private static String lastCrossClassSearchQuery = "";
    private static boolean crossClassSearchActive = false;
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
    private static int reloadSettleTicks = 0;
    private static final int RELOAD_SETTLE_DELAY = 5;
    private static ReloadBankWidget reloadBankWidget = null;

    static int shownPages;

    private static boolean isMouseInOverlay = false;

    private static int scissorx1, scissory1, scissorx2, scissory2;

    private static long lastClickTime = 0;

    private static Pair<Integer, Integer> lastClickedSlot = new Pair<>(-1, -1);

    private static final List<ItemStack> EMPTY_BANK_PAGE = Collections.nCopies(45, Items.AIR.getDefaultStack());
    private static final List<ItemStack> EMPTY_PLAYER_INVENTORY = Collections.nCopies(36, Items.AIR.getDefaultStack());

    private static boolean clickedClassSelectionEntity = false;
    private static final CustomColor WHITE_TEXT_COLOR = CustomColor.fromHexString("FFFFFF");
    private static final CustomColor YELLOW_TEXT_COLOR = CustomColor.fromHexString("FFFF00");
    private static final CustomColor GOLD_TEXT_COLOR = CustomColor.fromHexString("DEC800");
    private static final CustomColor GRAY_TEXT_COLOR = CustomColor.fromHexString("AAAAAA");
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

    public BankOverlay2(CallbackInfo ci, HandledScreen<?> screen) {
        this.ci = ci;
        this.screen = screen;
        actualOffset = 0;
        targetOffset = 0;
        pages.clear();
        crossClassPages.clear();
        lastCrossClassSearchQuery = "";
        crossClassSearchActive = false;
        allCharactersBrowseMode = false;
        allCharactersButtonWidget = null;
        isReloading = false;
        reloadPageLoaded = false;
        reloadBankWidget = null;
        signMids.clear();
        inventoryWidget = null;
        switchButtonWidget = null;
        quickActionWidget = null;
        searchbar2 = null;
        priceText = null;
        activeInv = 0;
        shownPages = 0;
        scissorx1 = 0;
        scissory1 = 0;
        scissorx2 = 0;
        scissory2 = 0;

        refreshDurabilityCfg();
        refreshHighlightCfg();

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


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Pages = currentData;
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.getWindow() == null || !mc.isRunning()) return;
        if(mc.player == null || mc.currentScreen == null) return;
        frameTextRenderer = mc.textRenderer;
        clearHoverState(screen);
        refreshFrameFeatureStates();

        if(ui == null) {
            ui = new UIUtils(context, 1, 0, 0);
        }

        if(bankSyncid == 0) {
            bankSyncid = McUtils.containerMenu().syncId;
        }

        calculateLayout();
        int xRemain = layoutXRemain;
        int yRemain = layoutYRemain;

        int xStart = xRemain / 2 - 2;
        int yStart = yRemain / 2 - 2;
        int buttonWidgetsX = (int) ((xStart + (xFitAmount / 2) * (162 + 4) - 166) * ui.getScaleFactor());

        if(currentOverlayType != BankOverlayType.NONE && expectedOverlayType != BankOverlayType.NONE && currentOverlayType != expectedOverlayType) {
            bridgeScreen = screen;
            clearHoverState(screen);
            BankOverlaySlotBridge.beginFrame(screen);
            RenderUtils.drawRect(context, BACKDROP_COLOR, 0, 0, mc.currentScreen.width, mc.currentScreen.height);
            drawBackgroundRect(context, xRemain, yRemain);
            if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackgroundDark : buttonBackgroundShortDark, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            } else {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackground : buttonBackgroundShort, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            }
            if(inventoryWidget != null) inventoryWidget.draw(context, mouseX, mouseY, delta, ui);
            if(quickActionWidget != null) quickActionWidget.draw(context, mouseX, mouseY, delta, ui);
            if(searchbar2 != null) searchbar2.draw(context, mouseX, mouseY, delta, ui);
            if(scrollBarWidget != null) scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);
            if(toggleOverlayWidget != null && WynnExtrasConfig.INSTANCE.bankQuickToggle) toggleOverlayWidget.draw(context, mouseX, mouseY, delta, ui);
            BankOverlaySlotBridge.endFrame();
            ci.cancel();
            return;
        }

        Container container = Models.Container.getCurrentContainer();
        if (container instanceof AccountBankContainer ||
                container instanceof CharacterBankContainer ||
                container instanceof BookshelfContainer ||
                container instanceof MiscBucketContainer
        ) {
            if (toggleOverlayWidget == null) {
                toggleOverlayWidget = new ToggleOverlayWidget();
            }


            float xPos = mc.currentScreen.width / 2f;
            float yPos = yStart + (yFitAmount) * (90 + 4 + 10) - 20;

            if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                Screen screen = McUtils.screen();
                if (!(screen instanceof HandledScreen<?> containerScreen)) return;
                yPos = ((HandledScreenAccessor) containerScreen).getY() + (4 + McUtils.containerMenu().slots.size() / 9f) * 16;
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
//        else {
//            RenderUtils.drawRect(context, CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
//        } i dont remember why i added this but ill keep it here for now if i need it again

        if(currentOverlayType == BankOverlayType.NONE || MinecraftClient.getInstance() == null) {
            BankOverlaySlotBridge.restoreAll();
            return;
        }

        initializeOverlayState();

        float snapValue = 0.5f;

        int totalRows = (int) Math.ceil((double) shownPages / xFitAmount);
        int c = (xFitAmount % 2 == 0 ? 1 : 0);
        int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * 3) - 104 * c);

        if (targetOffset > maxOffset) {
            targetOffset = maxOffset;
            snapValue = 0.75f;
        }
        if (targetOffset <= 0) {
            targetOffset = 0;
            snapValue = 0.75f;
        }

        float speed = 0.3f;
        float diff = (targetOffset - actualOffset);
        if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
        else actualOffset += diff * speed * delta;

        if(!WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
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
                if (++reloadSettleTicks < RELOAD_SETTLE_DELAY) {
                    // Wait a few ticks for server to fully process the page
                } else {
                    reloadSettleTicks = 0;
                    reloadCurrentPage++;
                    if (reloadCurrentPage >= reloadTotalPages) {
                        isReloading = false;
                        activeInv = reloadOriginalPage;
                        try {
                            BankOverlay.getPersonalStorageUtils().jumpToDestination(reloadOriginalPage + 1);
                        } catch (Exception ignored) {}
                        retryLoad();
                        reloadPageLoaded = false;
                        Pages.save();
                    } else {
                        reloadPageLoaded = false;
                        activeInv = reloadCurrentPage;
                        try {
                            BankOverlay.getPersonalStorageUtils().jumpToDestination(reloadCurrentPage + 1);
                        } catch (Exception ignored) {}
                        retryLoad();
                    }
                }
            }
            if (!shouldWait && !reloadPageLoaded && activeInv == reloadCurrentPage) {
                reloadPageLoaded = true;
                reloadSettleTicks = 0;
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
            searchbar2 = new TextInputWidget(0, 0, 0, 0, 0, 0, 1) {
                @Override
                protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    TextRenderer font = client.textRenderer;

                    if (input.isEmpty() && !isFocused()) {
                        ui.drawText(placeholder, x + 50, y + 7, WHITE_TEXT_COLOR, 1.25f);
                    } else {
                        if (cursorPos > input.length()) cursorPos = input.length();
                        ui.drawText(input, x + 7, y + 7, textColor, 1.25f);

                        long now = System.currentTimeMillis();
                        if (now - lastBlink > 500) {
                            blinkToggle = !blinkToggle;
                            lastBlink = now;
                        }

                        if (blinkToggle && isFocused()) {
                            int cursorX = (int) (x + 8 + (font.getWidth(input.substring(0, cursorPos))) * 1.25f * ui.getScaleFactor());
                            ui.drawLine(cursorX, y + 4, cursorX, y + 20, 1.25f, textColor);
                        }
                    }
                }

                @Override
                public boolean onClick(int button) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    if(button == 1) {
                        input = "";
                        for (PageWidget page : pages) {
                            page.setEnabled(true);
                            page.invalidateSearchCache();
                        }
                    }
                    setFocused(true);

                    cursorPos = input.length();
                    return true;
                }
            };
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

        scrollBarWidget.setBounds(xStart + xFitAmount * 170, yStart - 13, 15, (yFitAmount - 1) * 104 + 12);
        scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);

        ci.cancel();

        if (!WynnExtras.hasTestInventory()) {
            WynnExtras.updateTestInventory(screen.getScreenHandler().slots);
        }

        drawBackgroundRect(context, xRemain, yRemain);

        isMouseInOverlay = mouseY > yStart && mouseY < yStart + 100 * (yFitAmount - 1);

        int pageAmount = 0;
        {
            int visuali = 0;
            scissorx1 = xStart - 5;
            scissory1 = yStart - (allCharactersBrowseMode || crossClassSearchActive ? 12 : 0);
            scissorx2 = xStart + 166 * xFitAmount;
            scissory2 = yStart + 104 * (yFitAmount - 1) - 12;

            context.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
            ui.updateContext(context, ui.getScaleFactor(), 0, 0);

            // Check for cross-class search (@ or all characters browse mode)
            String rawSearchInput = searchbar2.getInput();
            boolean isCrossClassSearch = (rawSearchInput != null && rawSearchInput.contains("@")) || allCharactersBrowseMode;
            String searchInput = rawSearchInput;

            // Strip @ from search query for actual matching
            if (searchInput != null && searchInput.contains("@")) {
                searchInput = searchInput.replace("@", "").trim();
            }

            if (!Objects.equals(activeSearchInput, searchInput)) {
                activeSearchInput = searchInput == null ? "" : searchInput;
                activeSearchQuery = SearchQueryParser.parse(activeSearchInput);
            }

            // Trigger cross-class search if needed (@ present, with or without search text)
            if (isCrossClassSearch) {
                String cacheKey = allCharactersBrowseMode ? ("__allchars__" + (rawSearchInput != null ? rawSearchInput : "")) : rawSearchInput;
                if (!cacheKey.equals(lastCrossClassSearchQuery)) {
                    lastCrossClassSearchQuery = cacheKey;
                    crossClassSearchActive = true;

                    crossClassPages.clear();

                    List<CrossClassBankSearch.SearchResult> results;
                    if (allCharactersBrowseMode) {
                        if (searchInput == null || searchInput.isEmpty()) {
                            results = CrossClassBankSearch.getAllCharacterPagesIncludingCurrent();
                        } else {
                            results = CrossClassBankSearch.searchAllCharactersIncludingCurrent(searchInput);
                        }
                    } else {
                        if (searchInput == null || searchInput.isEmpty()) {
                            results = CrossClassBankSearch.getAllCharacterPages();
                        } else {
                            results = CrossClassBankSearch.searchAllCharacters(searchInput);
                        }
                    }

                    for (CrossClassBankSearch.SearchResult result : results) {
                        CrossClassPageWidget ccPage = new CrossClassPageWidget(
                                result.characterId,
                                result.characterNickname,
                                result.characterLevel,
                                result.pageNumber,
                                result.pageItems,
                                yStart,
                                (int) (yStart + (yFitAmount) * (90 + 4 + 10) * Math.max(2, ui.getScaleFactor()))
                        );
                        crossClassPages.add(ccPage);
                    }
                }
            } else {
                // Clear cross-class results if not in cross-class mode
                if (crossClassSearchActive) {
                    crossClassPages.clear();
                    lastCrossClassSearchQuery = "";
                    crossClassSearchActive = false;
                }
            }

            // Skip regular pages when browsing all characters
            if (!allCharactersBrowseMode) {
                int regularPageCount = getRenderableRegularPageCount();
                for(int i = 0; i < regularPageCount; i++) {
                    PageWidget page = pages.get(i);
                    float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                    float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) - actualOffset;
                    page.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                    boolean pageVisible = pageIntersectsClip(invY, 92, true);
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
                        boolean containsSearch = page.containsSearch(searchInput, activeSearchQuery);

                        if(!containsSearch) {
                            page.setEnabled(false);
                            page.setSlotsVisible(false);
                            continue;
                        } else {
                            page.setEnabled(true);
                            pageAmount++;
                        }
                    } else {
                        page.setEnabled(true);
                        pageAmount++;
                    }

                    if(pageVisible) {
                        page.draw(context, mouseX, mouseY, delta, ui);
                    } else {
                        page.setSlotsVisible(false);
                    }
                    visuali++;
                }
            }

            // Render cross-class pages after regular pages
            if (crossClassSearchActive && !crossClassPages.isEmpty()) {
                for (CrossClassPageWidget ccPage : crossClassPages) {
                    float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                    float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) - actualOffset;
                    ccPage.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                    if (pageIntersectsClip(invY, 92, true)) {
                        ccPage.draw(context, mouseX, mouseY, delta, ui);
                    }
                    visuali++;
                    pageAmount++;
                }
            }

            context.disableScissor();

            inventoryWidget.setBounds(buttonWidgetsX + 160, yStart + (yFitAmount - 1) * (90 + 4 + 10) - 3, (int) (176 * ui.getScaleFactor()), (int) (86 * ui.getScaleFactor()));
            inventoryWidget.setItems(buildInventoryForIndex(0, true));
            inventoryWidget.draw(context, mouseX, mouseY, delta, ui);

            if (!allCharactersBrowseMode) {
                if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                    switchButtonWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
                    switchButtonWidget.draw(context, mouseX, mouseY, delta, ui);
                } else {
                    switchButtonWidget.setBounds(0, 0, 0, 0);
                }

                if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
                    ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackgroundDark : buttonBackgroundShortDark, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
                } else {
                    ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackground : buttonBackgroundShort, buttonWidgetsX - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
                }
            } else {
                switchButtonWidget.setBounds(0, 0, 0, 0);
            }

            if (allCharactersBrowseMode) {
                // In browse mode: only show search bar and new buttons, positioned compactly
                searchbar2.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            } else if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                searchbar2.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 59, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            } else {
                searchbar2.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            }

            searchbar2.setTextColor(WHITE_TEXT_COLOR);
            searchbar2.setBackgroundColor(null);
            searchbar2.draw(context, mouseX, mouseY, delta, ui);

            if (!allCharactersBrowseMode) {
                if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                    ui.drawCenteredText("Switch to " + (currentOverlayType == BankOverlayType.ACCOUNT ? "Character" : "Account") + " Bank", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, WHITE_TEXT_COLOR, 1.1f);
                }
                if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                    ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 44, WHITE_TEXT_COLOR, 1.1f);
                } else {
                    ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, WHITE_TEXT_COLOR, 1.1f);
                }
            }
        }

        shownPages = pageAmount;

        drawEmeraldOverlay(context, xStart - 36, yStart - 14);
        if (WynnExtrasConfig.INSTANCE.bankBagOverlay
                && (currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER || currentOverlayType == BankOverlayType.MISC)) {
            cacheCurrentBankPageIfPossible();

            // Top section: bank bags. Header is drawn top-right of the screen, grid sits in
            // the left margin alongside the bank pages.
            int bankGridX = xStart - 36 - 56;
            int bankGridY = yStart - 14 + 4 * 28;
            drawBagOverlay(
                    context,
                    bankGridX,
                    bankGridY,
                    getCurrentPageStacks(),
                    collectAccountAndCharacterBagCounts());

            // Bottom section: bags currently in player inventory, in the same column directly
            // below the bank grid with a gap so the two read as separate sections. Reserve
            // BAG_RAID_ORDER.length rows worth of space so the inventory grid never collides
            // with the bank grid even when every raid is populated.
            if (BankOverlay.playerInvSlots != null && !BankOverlay.playerInvSlots.isEmpty()) {
                int invBagY = bankGridY + BAG_RAID_ORDER.length * 28 + 18;
                drawBagGrid(context, bankGridX, invBagY, livePlayerInventoryItems);
            }
        }

        renderHoveredTooltip(context, screen, mouseX, mouseY);
        renderHeldItemOverlay(context, mouseX, mouseY);

        if (!allCharactersBrowseMode) {
            if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                quickActionWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            } else {
                quickActionWidget.setBounds(buttonWidgetsX, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            }
            quickActionWidget.draw(context, mouseX, mouseY, delta, ui);
        } else {
            quickActionWidget.setBounds(0, 0, 0, 0);
        }

        // Draw new buttons (below search bar normally, or below search bar in browse mode)
        if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
            int btnY = allCharactersBrowseMode
                    ? yStart + (yFitAmount - 1) * (90 + 4 + 10) + 29
                    : yStart + (yFitAmount - 1) * (90 + 4 + 10) + 85;
            int halfWidth = (int) (75 * ui.getScaleFactor());
            int btnHeight = (int) (18 * ui.getScaleFactor());
            int gap = (int) (5 * ui.getScaleFactor());

            allCharactersButtonWidget.setBounds(xStart, btnY, halfWidth, btnHeight);
            allCharactersButtonWidget.draw(context, mouseX, mouseY, delta, ui);

            reloadBankWidget.setBounds(xStart + halfWidth + gap, btnY, halfWidth, btnHeight);
            reloadBankWidget.draw(context, mouseX, mouseY, delta, ui);
        } else {
            allCharactersButtonWidget.setBounds(0, 0, 0, 0);
            reloadBankWidget.setBounds(0, 0, 0, 0);
        }

        touchHoveredSlot = hoveredBackingSlot;
        BankOverlaySlotBridge.endFrame();
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
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
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
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
            );
        }
    }

    private static boolean pageIntersectsClip(float pageY, float pageHeight, boolean hasLabel) {
        float labelTop = hasLabel ? pageY - 12 : pageY;
        return pageY + pageHeight > scissory1 && labelTop < scissory2;
    }

    private static int getRenderableRegularPageCount() {
        if (currentData == null) return pages.size();
        int requested = Math.max(currentData.getLastPage() + 1, activeInv + 1);
        return Math.min(pages.size(), Math.max(0, requested));
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Container container = Models.Container.getCurrentContainer();
        boolean inBank = container instanceof AccountBankContainer ||
                container instanceof CharacterBankContainer ||
                container instanceof BookshelfContainer ||
                container instanceof MiscBucketContainer;

        if(toggleOverlayWidget != null && WynnExtrasConfig.INSTANCE.bankQuickToggle && inBank) toggleOverlayWidget.mouseClicked(x, y, button);

        if (!WynnExtrasConfig.INSTANCE.toggleBankOverlay) return false;
        if (currentOverlayType == BankOverlayType.NONE) return false;

        if (allCharactersBrowseMode) {
            // In browse mode: check UI controls first, then cross-class pages
            if(searchbar2 != null && searchbar2.mouseClicked(x, y, button)) return true;
            if(scrollBarWidget != null && scrollBarWidget.mouseClicked(x, y, button)) return true;
            if(allCharactersButtonWidget != null && allCharactersButtonWidget.mouseClicked(x, y, button)) return true;
            if(reloadBankWidget != null && reloadBankWidget.mouseClicked(x, y, button)) return true;
            for(CrossClassPageWidget ccPage : crossClassPages) {
                if (ccPage.mouseClicked(x, y, button)) {
                    return true;
                }
            }
            return true;
        }

        // Check UI controls first (so they don't get stolen by overlapping cross-class pages)
        if(searchbar2 != null && searchbar2.mouseClicked(x, y, button)) return true;
        if(scrollBarWidget != null && scrollBarWidget.mouseClicked(x, y, button)) return true;
        if(allCharactersButtonWidget != null && allCharactersButtonWidget.mouseClicked(x, y, button)) return true;
        if(reloadBankWidget != null && reloadBankWidget.mouseClicked(x, y, button)) return true;
        if(switchButtonWidget != null && switchButtonWidget.mouseClicked(x, y, button)) return true;
        if(quickActionWidget != null && quickActionWidget.mouseClicked(x, y, button)) return true;

        int regularPageCount = getRenderableRegularPageCount();
        for(int i = 0; i < regularPageCount; i++) {
            pages.get(i).mouseClicked(x, y, button);
        }
        // Handle clicks on cross-class search results
        for(CrossClassPageWidget ccPage : crossClassPages) {
            if (ccPage.mouseClicked(x, y, button)) {
                return true;
            }
        }
        if(inventoryWidget != null) inventoryWidget.mouseClicked(x, y, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if(scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        return super.mouseReleased(x, y, button);
    }

    private void initializeOverlayState() {
        if (!initializedTypes.contains(currentOverlayType)) {
            BANK_PAGE_NAME_INPUTS_BY_TYPE.putIfAbsent(currentOverlayType, new HashMap<>());

            for (int i = 0; i < BankOverlay.getCurrentMaxPages(); i++) {
                BANK_PAGE_NAME_INPUTS_BY_TYPE.get(currentOverlayType).put(i, new EasyTextInput(-1000, -1000, 13, 162 + 4));
            }

            initializedTypes.add(currentOverlayType);
        }

        if (Pages == null) Pages = currentData;

        PersonalStorageUtilitiesFeatureAccessor accessor = (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.getPersonalStorageUtils();
        accessor.setLastPage(99);

        if (activeInv == -1) activeInv = 1;
    }

    private static void clearHoverState(HandledScreen<?> screen) {
        hoveredBackingSlot = null;
        WeightDisplay.setCurrentHoveredStack(null);
        hoveredInvIndex = -1;
        hoveredIndex = -1;
        hoveredX = -1;
        hoveredY = -1;
        hoveredSlot = Items.AIR.getDefaultStack();
        if (screen != null) {
            ((HandledScreenAccessor) screen).setFocusedSlot(null);
        }
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
                        rightArrow = McUtils.containerMenu().getSlot(52).getStack();
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
                        } else if (oldShouldWait) {
                            Pages.getBankPages().put(activeInv, slots.stream().map(Slot::getStack).toList());
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
                        WynnExtras.LOGGER.info("retrying jump");
                        shouldWaitSince = System.currentTimeMillis();
                        retryLoad();
                        PersonalStorageUtilitiesFeatureAccessor accessor =
                                (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.getPersonalStorageUtils();
                        accessor.setLastPage(99);
                        try {
                            BankOverlay.getPersonalStorageUtils().jumpToDestination(activeInv + 1);
                        } catch (Exception e) {
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                        }
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

        ScreenHandler currScreenHandler = McUtils.containerMenu();
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
            ((HandledScreenAccessor) bridgeScreen).setFocusedSlot(backingSlot);
        }
    }

    private static void clearAnnotationCache(int pageIndex) {
        List<ItemAnnotation> annotations = annotationCache.get(pageIndex);
        if (annotations != null) annotations.clear();
        annotationStackCache.remove(pageIndex);
        annotationComponentCache.remove(pageIndex);
    }

    private static <T> void ensureCacheSize(List<T> cache, int size, T value) {
        while (cache.size() < size) {
            cache.add(value);
        }
    }

    private static void applyAnnotation(ItemStack stack, List<ItemAnnotation> annotations, List<ItemStack> annotationStacks, List<Object> annotationComponents, int index) {
        if(annotations.size() <= index || annotationStacks.size() <= index || annotationComponents.size() <= index) return;

        if(stack == null || stack.getItem() == Items.AIR) {
            annotations.set(index, null);
            annotationStacks.set(index, stack);
            annotationComponents.set(index, null);
            return;
        }

        ItemAnnotation annotation = annotations.get(index);
        Object components = stack.getComponents();
        if(annotation == null || annotationStacks.get(index) != stack || annotationComponents.get(index) != components) {
            StyledText originalName = ensureWynntilsOriginalName(stack);
            annotation = ((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(stack, originalName);
            annotations.set(index, annotation);
            annotationStacks.set(index, stack);
            annotationComponents.set(index, components);
        }

        if (annotation != null) {
            ItemStackExtension extension = (ItemStackExtension) (Object) stack;
            if (extension.getAnnotation() != annotation) {
                extension.setAnnotation(annotation);
            }
        }
    }

    private static StyledText ensureWynntilsOriginalName(ItemStack stack) {
        if (isEmptyStack(stack)) return StyledText.EMPTY;

        ItemStackExtension extension = (ItemStackExtension) (Object) stack;
        StyledText originalName = extension.getOriginalName();
        if (originalName != null) return originalName;

        Text stackName = stack.getName();
        if (stack.getCustomName() != null && stack.getCustomName().toString().contains("Key")) {
            String clean = WynnUtils.normalizeBadString(stackName.getString());
            stackName = Text.of(clean);
        }

        originalName = StyledText.fromComponent(stackName);
        extension.setOriginalName(originalName);
        return originalName;
    }

    // Cached durability-overlay config so we don't reflect into Wynntils config options
    // on every slot draw (was 2 lookups × ~1000 slots per frame → ~5fps in bank).
    private static boolean durabilityRenderInInv = false;
    private static String durabilityMode = "ARC";

    private static void refreshDurabilityCfg() {
        try {
            if (durabilityOverlayFeature == null)
                durabilityOverlayFeature = Managers.Feature.getFeatureInstance(DurabilityOverlayFeature.class);
            durabilityRenderInInv = (Boolean) durabilityOverlayFeature.getConfigOptionFromString("renderDurabilityOverlayInventories").get().get();
            durabilityMode = ((Enum<?>) durabilityOverlayFeature.getConfigOptionFromString("durabilityRenderMode").get().get()).name();
        } catch (Exception ignored) {}
    }

    // Cached highlight-texture config so we don't reflect into Wynntils config options
    // on every slot draw (was 1 lookup per highlighted slot per frame).
    private static int highlightTextureOrdinal = 0;

    private static void refreshHighlightCfg() {
        try {
            if (itemHighlightFeature == null)
                itemHighlightFeature = Managers.Feature.getFeatureInstance(ItemHighlightFeature.class);
            highlightTextureOrdinal = ((ItemHighlightFeature.HighlightTexture) itemHighlightFeature.getConfigOptionFromString("highlightTexture").get().get()).ordinal();
        } catch (Exception ignored) {}
    }

    private static void refreshFrameFeatureStates() {
        try {
            if (durabilityOverlayFeature == null)
                durabilityOverlayFeature = Managers.Feature.getFeatureInstance(DurabilityOverlayFeature.class);
            durabilityOverlayEnabled = durabilityOverlayFeature != null && durabilityOverlayFeature.isEnabled();
        } catch (Exception ignored) {
            durabilityOverlayEnabled = false;
        }

        try {
            if (itemHighlightFeature == null)
                itemHighlightFeature = Managers.Feature.getFeatureInstance(ItemHighlightFeature.class);
            itemHighlightEnabled = itemHighlightFeature != null && itemHighlightFeature.isEnabled();
        } catch (Exception ignored) {
            itemHighlightEnabled = false;
        }

        try {
            if (itemTextOverlayFeature == null)
                itemTextOverlayFeature = Managers.Feature.getFeatureInstance(ItemTextOverlayFeature.class);
            itemTextOverlayEnabled = itemTextOverlayFeature != null && itemTextOverlayFeature.isEnabled();
        } catch (Exception ignored) {
            itemTextOverlayEnabled = false;
        }

        try {
            if (unidentifiedItemIconFeature == null)
                unidentifiedItemIconFeature = Managers.Feature.getFeatureInstance(UnidentifiedItemIconFeature.class);
            unidentifiedItemIconEnabled = unidentifiedItemIconFeature != null && unidentifiedItemIconFeature.isEnabled();
        } catch (Exception ignored) {
            unidentifiedItemIconEnabled = false;
        }

        try {
            if (itemFavoriteFeature == null)
                itemFavoriteFeature = Managers.Feature.getFeatureInstance(ItemFavoriteFeature.class);
            itemFavoriteEnabled = itemFavoriteFeature != null && itemFavoriteFeature.isEnabled();
        } catch (Exception ignored) {
            itemFavoriteEnabled = false;
        }
    }

    private static boolean isEmptyStack(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem() == Items.AIR;
    }

    private static ItemStack copyForRenderMutation(ItemStack original, ItemStack current) {
        return current == original ? original.copy() : current;
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

    private static void renderDurabilityRing(DrawContext context, ItemStack stack, WynnItem cachedItem, int x, int y) {
        try {
            if (!durabilityOverlayEnabled) return;
            if (!durabilityRenderInInv) return;
            if (!(cachedItem instanceof DurableItemProperty)) return;

            DurabilityOverlayFeatureInvoker invoker = (DurabilityOverlayFeatureInvoker) durabilityOverlayFeature;
            switch (durabilityMode) {
                case "ARC"        -> invoker.invokeDrawDurabilityArc(context, stack, x, y);
                case "BAR"        -> invoker.invokeDrawDurabilityBar(context, stack, x, y);
                case "PERCENTAGE" -> invoker.invokeDrawDurabilityPercentage(context, stack, x, y);
            }
        } catch (Exception ignored) {}
    }

    private static void renderEmeraldPouchRing(DrawContext context, WynnItem cachedItem, int x, int y) {
        if (!(cachedItem instanceof EmeraldPouchItem pouch)) return;
        int capacity = pouch.getCapacity();
        if (capacity <= 0) return;
        float fraction = (float) pouch.getValue() / capacity;
        int colorInt = MathHelper.hsvToRgb((1.0F - fraction) / 3.0F, 1.0F, 1.0F);
        CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

        RenderUtils.drawArc(context, color, x - 2, y - 2, Math.min(1.0F, fraction), 8, 10);
    }

    private static CustomColor getHighlightColor(ItemStack stack) {
         if(isEmptyStack(stack)) return CustomColor.NONE;
         if (!itemHighlightEnabled) return CustomColor.NONE;
         return ((ItemHighlightFeatureInvoker) itemHighlightFeature).invokeGetHighlightColor(stack, false);
    }

    private static void renderHighlightOverlay(DrawContext context, CustomColor color, int x, int y) {
         if (!Objects.equals(color, CustomColor.NONE)) {
             try {
                 RenderUtils.drawTexturedRect(
                     context,
                     Texture.HIGHLIGHT.identifier(),
                     color, (float)(x - 1), (float)(y - 1), 18.0F, 18.0F,
                     highlightTextureOrdinal * 18,
                     0.0F, 18.0F, 18.0F,
                     Texture.HIGHLIGHT.width(),
                     Texture.HIGHLIGHT.height());
             } catch (Exception ignored) {}
         }
    }

    private static void renderItemOverlays(DrawContext context, ItemStack stack, int x, int y, Optional<WynnItem> item) {
        if (item.isPresent()) {
            ItemAnnotation annotation = item.get();
            if (annotation instanceof TeleportScrollItem ||
                    annotation instanceof AmplifierItem ||
                    annotation instanceof DungeonKeyItem ||
                    annotation instanceof EmeraldPouchItem ||
                    annotation instanceof GatheringToolItem ||
                    annotation instanceof PowderItem ||
                    annotation instanceof PotionItem ||
                    annotation instanceof CrafterBagItem) {
                if (itemTextOverlayEnabled) {
                    ((ItemTextOverlayFeatureMixin) itemTextOverlayFeature).invokeDrawTextOverlay(context, stack, x, y, false);
                }
            }

            if (unidentifiedItemIconEnabled) {
                ((UnidentifiedItemIconFeatureInvoker) unidentifiedItemIconFeature).invokeDrawIcon(context, stack, x, y, 100);
            }
            if(itemFavoriteEnabled && ((ItemFavoriteFeatureAccessor) itemFavoriteFeature).callIsFavorited(stack)) {
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
    }

    private static void renderSearchOverlay(DrawContext context, ItemStack stack, WynnItem cachedItem, int x, int y) {
        if (activeSearchInput == null || activeSearchInput.isEmpty()) return;

        if (isEmptyStack(stack)) {
            RenderUtils.drawRect(context, SEARCH_DIM_COLOR, x - 1, y - 1, 18, 18);
            return;
        }

        WynnItem wynnItem = cachedItem;
        if (wynnItem == null) {
            Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
            if (optWynnItem.isPresent()) {
                wynnItem = optWynnItem.get();
            }
        }

        if (SearchQueryParser.matches(stack, wynnItem, activeSearchQuery)) {
            // Item matches - draw green border
            RenderUtils.drawRectBorders(context, SEARCH_MATCH_COLOR, x, y, x + 16, y + 16, 1);
        } else {
            // Item doesn't match - dim it
            RenderUtils.drawRect(context, SEARCH_DIM_COLOR, x - 1, y - 1, 18, 18);
        }
    }

    private void renderHoveredTooltip(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (hoveredSlot.getItem() == Items.AIR) return;

        ensureWynntilsOriginalName(hoveredSlot);
        Optional<WynnItem> item = asWynnItem(hoveredSlot);
        WeightDisplay.setCurrentHoveredStack(hoveredSlot);
        TooltipRenderData tooltipData = getTooltipRenderData(hoveredSlot, item);
        TradeMarketComparisonPanel.cacheHoveredTooltip(hoveredSlot, tooltipData.tooltip());

        int tooltipHeight = tooltipData.height();
        int screenHeight = screen.height;
        float scale = 1.0f;

        int y = mouseY;
        boolean overflow = false;
        if (tooltipHeight > screenHeight) {
            scale = (float) screenHeight / (float) tooltipHeight;
            y = 0; //ganz unten am screen
            overflow = true;
        }

        if(!overflow) {
            Slot tooltipSource = getTooltipSourceSlot(screen);
            ensureWynntilsOriginalName(tooltipSource.getStack());
            ((HandledScreenAccessor) screen).setFocusedSlot(tooltipSource);
            ((HandledScreenInvoker) screen).invokeDrawMouseoverTooltip(context, mouseX, mouseY);
        } else {
            drawTooltip(screen.getTextRenderer(), tooltipData.components(), (int) (mouseX + 14 * scale), y, context);
        }

    }

    private static TooltipRenderData getTooltipRenderData(ItemStack stack, Optional<WynnItem> item) {
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

        if (item.isPresent() && item.get() instanceof GearBoxItem gearBox) {
            if (itemGuessFeature == null) {
                itemGuessFeature = Managers.Feature.getFeatureInstance(ItemGuessFeature.class);
            }
            List<Text> addon = ((ItemGuessFeatureAccessor) itemGuessFeature).callGetTooltipAddon(gearBox);

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
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        ((SlotAccessor) tooltipSlot).setX(hoveredX - accessor.getX());
        ((SlotAccessor) tooltipSlot).setY(hoveredY - accessor.getY());
        return tooltipSlot;
    }

    private static List<Text> buildCallbackAwareTooltip(ItemStack stack, Optional<WynnItem> item) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Text> tooltip = new ArrayList<>();

        try {
            tooltip.addAll(stack.getTooltip(
                    Item.TooltipContext.DEFAULT,
                    mc.player,
                    TooltipType.BASIC
            ));
        } catch (Throwable ignored) {}

        if (tooltip.isEmpty() && item.isPresent()) {
            try {
                tooltip.addAll(TooltipUtils.getWynnItemTooltip(stack, item.get()));
            } catch (Throwable ignored) {}
        }

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
        if (heldItem == null) return;

        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue() + 1;
        String amountString = heldItem.getCount() == 1 ? "" : String.valueOf(heldItem.getCount());

        context.drawItem(heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale, amountString);
    }

    private static boolean shouldCancelEmeraldPouch(ItemStack oldHeld, ItemStack newHeld) {
        if (oldHeld == null || newHeld == null || newHeld.getCustomName() == null) return false;

        return (oldHeld.getItem() == Items.EMERALD ||
                oldHeld.getItem() == Items.EMERALD_BLOCK ||
                oldHeld.getItem() == Items.EXPERIENCE_BOTTLE) &&
                newHeld.getCustomName().getString().contains("Pouch");
    }

    private static ItemStack getHeldItem(int index, SlotActionType type, int mouseButton) {
        MinecraftClient mc = McUtils.mc();
        PlayerEntity player = mc.player;
        ItemStack heldItem = Items.AIR.getDefaultStack();

        if (player == null || player.currentScreenHandler == null) return heldItem;

        ItemStack clickedStack = player.currentScreenHandler.slots.get(index).getStack().copy();
        ItemStack currentHeld = BankOverlay.heldItem;

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

    public static <T extends WynnItem> Optional<T> asWynnItem(ItemStack itemStack) {
        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if(annotationOpt.isEmpty()) return Optional.empty();
        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();
        return Optional.of((T) wynnItem);
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

    void drawEmeraldOverlay(DrawContext context, int x, int y) {
        if (emeraldCountFeature == null) {
            emeraldCountFeature = Managers.Feature.getFeatureInstance(InventoryEmeraldCountFeature.class);
        }
        int emeraldAmountInt = Models.Emerald.getAmountInContainer();
        if (emeraldAmountInt != cachedEmeraldAmount) {
            cachedEmeraldAmount = emeraldAmountInt;
            cachedEmeraldAmounts = ((InventoryEmeraldCountFeatureInvoker) emeraldCountFeature).invokeGetRenderableEmeraldAmounts(emeraldAmountInt);
        }
        String[] emeraldAmounts = cachedEmeraldAmounts;

        y += (3 * 28);


        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            String emeraldAmount = emeraldAmounts[i];

            if (emeraldAmount.equals("0")) continue;
            EmeraldUnits unit = EMERALD_UNITS[i];

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

            if (unit.getSymbol().equals("stx")) { // Make stx not look like normal LE
                context.drawItem(unit.getItemStack(), x + 3, y + 4 - (i * 28));
                context.drawItem(unit.getItemStack(), x + 6, y + 6 - (i * 28));
                context.drawItem(unit.getItemStack(), x + 9, y + 8 - (i * 28));
            } else {
                // This needs to be separate since Z levels are determined by order here
                context.drawItem(unit.getItemStack(), x + 6, y + 6 - (i * 28));
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
                            CommonColors.WHITE,
                            HorizontalAlignment.RIGHT,
                            VerticalAlignment.BOTTOM,
                            TextShadow.OUTLINE);
        }
    }

    // Hardcoded layout for the total-bags grid: all known raids × the three crafter-bag tiers.
    // Bags that don't match one of these combos still get counted into the header total, but
    // their own row won't be shown (we also don't have icons for combinations that never occur).
    private static final String[] BAG_RAID_ORDER = {"NOG", "NOL", "TCC", "TNA", "WTP"};
    private static final GearTier[] BAG_TIER_ORDER = {GearTier.LEGENDARY, GearTier.RARE, GearTier.UNIQUE};
    private static final Map<String, StyledText> BAG_RAID_LABELS = createBagRaidLabels();
    private static final List<ItemStack> CURRENT_PAGE_STACKS = new ArrayList<>(54);
    private static final List<ItemStack> PLAYER_INVENTORY_STACKS = new ArrayList<>(36);
    private static final List<ItemStack> SCREEN_HANDLER_STACKS = new ArrayList<>(54);
    private static final Map<String, Integer> BAG_TOTAL_CACHE = new HashMap<>();
    private static final HashMap<String, Integer> BAG_PAGE_COUNT_SCRATCH = new HashMap<>();
    private static final Map<String, BagGroup> BAG_GROUP_SCRATCH = new LinkedHashMap<>();
    private static final long BAG_TOTAL_CACHE_TTL_MS = 250;
    private static long bagTotalCacheLastUpdateMs = 0;

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

    /**
     * Draws the bag grid from live {@code gridStacks} and the top-right "Total Bags"
     * breakdown from pre-counted {@code totalCounts} (raid|tier → count).
     */
    static void drawBagOverlay(DrawContext context, int x, int y,
                               List<ItemStack> gridStacks, Map<String, Integer> totalCounts) {
        drawBagTopRightHeader(context, totalCounts);
        drawBagGrid(context, x, y, gridStacks);
    }

    /** Reused (raidAbbrev, tier) entry for the top-right text lines. */
    private static final class BagCountEntry {
        final String key;
        final String raidAbbrev;
        final GearTier tier;
        final String tierName;
        final int raidSortOrder;
        final int tierSortOrder;
        int count;

        BagCountEntry(String raidAbbrev, GearTier tier, int raidSortOrder) {
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
            for (GearTier tier : BAG_TIER_ORDER) {
                entries.add(new BagCountEntry(BAG_RAID_ORDER[raidIndex], tier, raidIndex));
            }
        }
        return entries;
    }

    /**
     * Draws the "Total Bags" block floating in the top-right corner. Shows per-(raid, tier)
     * lines (skipping zero-count combos) and a clickable sort-mode toggle below the total.
     * Uses pre-counted numbers so it doesn't depend on Wynntils item annotations.
     */
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
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
            for (GearTier tier : BAG_TIER_ORDER) {
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
            for (GearTier tier : BAG_TIER_ORDER) {
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
                GearTier tier = BAG_TIER_ORDER[col];
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

                CustomColor tierColor = CustomColor.fromChatFormatting(tier.getChatFormatting());

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
                CustomColor countColor = count > 0 ? CommonColors.WHITE : DIM_COUNT_COLOR;
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
    private static int getTierColorArgb(GearTier tier) {
        if (tier == null) return 0xFFFFFFFF;
        Integer rgb = tier.getChatFormatting().getColorValue();
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

        Container container = Models.Container.getCurrentContainer();
        boolean isBank = container instanceof AccountBankContainer || container instanceof CharacterBankContainer;
        boolean isInventory = screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen;

        if (isBank) cacheCurrentBankPageIfPossible();

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int x = accessor.getX() + accessor.getBackgroundWidth() + 4;
        int y = accessor.getY() + 14;

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

    // Debounce for the auto-save that runs while the bag overlay is caching pages.
    private static long lastBagCacheSaveMs = 0;
    private static final long BAG_CACHE_SAVE_DEBOUNCE_MS = 2000;

    private static int bagCacheLastPage = -1;
    private static int bagCacheStableFrames = 0;
    private static final int BAG_CACHE_SETTLE_FRAMES = 10; // ~0.5s at 20 tps

    /**
     * Counts CrafterBags on the current live page via Wynntils annotations (which only
     * exist for live ItemStacks, NOT deserialized ones) and stores the counts as plain
     * numbers in {@code BankData}'s bag count cache. Auto-persists to disk (debounced).
     */
    public static void cacheCurrentBankPageIfPossible() {
        if (BankOverlay.shouldWait) return;

        BankData data = getBankDataForCurrentContainer();
        if (data == null) return;

        int pageNum = getCurrentBankPageNumber();
        if (pageNum < 0) return;

        if (pageNum != bagCacheLastPage) {
            bagCacheLastPage = pageNum;
            bagCacheStableFrames = 0;
            return;
        }
        if (++bagCacheStableFrames < BAG_CACHE_SETTLE_FRAMES) return;

        List<ItemStack> live = getCurrentPageStacks();
        if (live.isEmpty()) return;

        BAG_PAGE_COUNT_SCRATCH.clear();
        for (ItemStack stack : live) {
            if (stack == null || stack.isEmpty()) continue;
            Optional<WynnItem> item = asWynnItem(stack);
            if (item.isEmpty() || !(item.get() instanceof CrafterBagItem bag)) continue;
            RaidKind raidKind = bag.getRaidKind();
            GearTier tier = bag.getGearTier();
            String key = (raidKind != null ? raidKind.getAbbreviation() : "?")
                    + "|" + (tier != null ? tier.name() : "?");
            BAG_PAGE_COUNT_SCRATCH.merge(key, stack.getCount(), Integer::sum);
        }

        // Check if anything changed before saving
        HashMap<String, Integer> existing = data.getBagCounts().get(pageNum);
        if (existing != null && existing.equals(BAG_PAGE_COUNT_SCRATCH)) return;

        data.getBagCounts().put(pageNum, new HashMap<>(BAG_PAGE_COUNT_SCRATCH));
        bagTotalCacheLastUpdateMs = 0;

        long now = System.currentTimeMillis();
        if (now - lastBagCacheSaveMs > BAG_CACHE_SAVE_DEBOUNCE_MS) {
            data.save();
            lastBagCacheSaveMs = now;
        }
    }

    /** True if the current open container is one of the personal bank types. */
    public static boolean isCurrentContainerBank() {
        Container container = Models.Container.getCurrentContainer();
        return container instanceof AccountBankContainer
                || container instanceof CharacterBankContainer
                || container instanceof BookshelfContainer
                || container instanceof MiscBucketContainer;
    }

    /** Saves the BankData for the currently open bank container, if any. */
    public static void saveCurrentBankData() {
        BankData data = getBankDataForCurrentContainer();
        if (data != null) data.save();
    }

    /**
     * Aggregates bag counts from all cached pages of the current bank container
     * (stored as plain numbers in BankData's bag count cache, so they don't depend on
     * Wynntils item annotations).
     */
    /**
     * Aggregates bag counts from Account Bank + Character Bank only.
     */
    private static Map<String, Integer> collectAccountAndCharacterBagCounts() {
        long now = System.currentTimeMillis();
        if (now - bagTotalCacheLastUpdateMs < BAG_TOTAL_CACHE_TTL_MS) {
            return BAG_TOTAL_CACHE;
        }

        BAG_TOTAL_CACHE.clear();
        for (BankData data : new BankData[]{AccountBankData.INSTANCE, CharacterBankData.INSTANCE}) {
            if (data == null || data.getBagCounts() == null) continue;
            for (Map<String, Integer> pageCounts : data.getBagCounts().values()) {
                if (pageCounts == null) continue;
                for (Map.Entry<String, Integer> e : pageCounts.entrySet()) {
                    // Old caches were written with "TWP|..." keys before Wynntils renamed
                    // the abbreviation to WTP. Fold them so they're not stranded.
                    String key = e.getKey();
                    if (key.startsWith("TWP|")) key = "WTP|" + key.substring(4);
                    BAG_TOTAL_CACHE.merge(key, e.getValue(), Integer::sum);
                }
            }
        }
        bagTotalCacheLastUpdateMs = now;
        return BAG_TOTAL_CACHE;
    }

    private static Map<String, BagGroup> groupBagsFromStacks(Iterable<ItemStack> stacks) {
        BAG_GROUP_SCRATCH.clear();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Optional<WynnItem> item = asWynnItem(stack);
            if (item.isEmpty() || !(item.get() instanceof CrafterBagItem bag)) continue;

            RaidKind raidKind = bag.getRaidKind();
            GearTier tier = bag.getGearTier();
            // Wynntils renamed The Wartorn Palace from "TWP" to "WTP" between 4.1.8 and 4.1.9.
            // Fold the legacy abbreviation onto the new one so groups merge regardless of which
            // Wynntils version is annotating the stack.
            String raidAbbrev = raidKind != null ? raidKind.getAbbreviation() : "?";
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

    private static List<ItemStack> getCurrentPageStacks() {
        CURRENT_PAGE_STACKS.clear();
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
        ScreenHandler menu = McUtils.containerMenu();
        if (menu == null || mc.player == null) return CURRENT_PAGE_STACKS;
        Inventory playerInv = mc.player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.inventory == playerInv) continue;
            CURRENT_PAGE_STACKS.add(slot.getStack());
        }
        return CURRENT_PAGE_STACKS;
    }

    /**
     * Returns the 0-based current bank page number (matching the cached bank page key
     * convention, see HandledScreenMixin.onClose which stores activeInv),
     * or -1 if unavailable. Prefers {@code Models.Bank.getCurrentPage()} because it's accurate
     * in both custom and vanilla modes, falling back to BankOverlay.activeInv only if Wynntils
     * hasn't reported a page yet. {@code Models.Bank.getCurrentPage()} is 1-based, so we
     * subtract 1 to align with the cache.
     */
    private static int getCurrentBankPageNumber() {
        try {
            int p = Models.Bank.getCurrentPage();
            if (p > 0) return p - 1;
        } catch (Exception ignored) {}
        if (BankOverlay.activeInv != -1) return BankOverlay.activeInv;
        return -1;
    }

    private static BankData getBankDataForCurrentContainer() {
        Container container = Models.Container.getCurrentContainer();
        if (container instanceof AccountBankContainer) return AccountBankData.INSTANCE;
        if (container instanceof CharacterBankContainer) return CharacterBankData.INSTANCE;
        if (container instanceof BookshelfContainer) return BookshelfData.INSTANCE;
        if (container instanceof MiscBucketContainer) return MiscBucketData.INSTANCE;
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
        final GearTier tier;
        final ItemStack icon;
        int count = 0;

        BagGroup(String raidAbbrev, GearTier tier, ItemStack icon) {
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

            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(99, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
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
            if (!visible || !enabled) return false;
            for (int i = slots.size() - 1; i >= 0; i--) {
                if (slots.get(i).mouseClicked(mx, my, button)) return true;
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
        private List<ItemStack> cachedContainsSearchItems = null;
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
            if(y > botBorder || y + height < topBorder) {
                setSlotsVisible(false);
                return;
            }
            setSlotsVisible(true);

            if(index >= currentData.getLastPage()) {
                setSlotsVisible(false);
                if(sign == null) {
                    sign = new NameSignWidget(index);
                    addChild(sign);
                }

                sign.setBounds(x, y - 10, width, 10);
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
                return;
            }

            ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankTextureDark : bankTexture, x, y, width, height);

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

            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(index, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));
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

            sign.setBounds(x, y - 10, width, 10);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled) return false;
            for (int i = slots.size() - 1; i >= 0; i--) {
                if (slots.get(i).mouseClicked(mx, my, button)) return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(McUtils.containerMenu() != null && index == currentData.getLastPage()) {
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
                        rightArrow = McUtils.containerMenu().getSlot(52).getStack();
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
                    ui.drawRectBorders(x, y + 0.5f, x + 164, y + 92, YELLOW_TEXT_COLOR);
                }
                CustomColor color = (!shouldWait)
                        ? YELLOW_TEXT_COLOR
                        : WHITE_TEXT_COLOR;
            } else if (!hovered || !isMouseInOverlay) {
                ui.drawRect(x, y, width, height, PAGE_DIM_COLOR);
            }

            try {
                if (McUtils.containerMenu() != null && index == activeInv && !shouldWait && (expectedOverlayType == BankOverlayType.NONE || currentOverlayType == expectedOverlayType)) {
                    ItemStack rightArrow = McUtils.containerMenu().getSlot(52).getStack();
                    if(rightArrow.getComponents() == null ||
                            rightArrow.getComponents().get(DataComponentTypes.LORE) == null ||
                            rightArrow.getComponents().get(DataComponentTypes.CUSTOM_NAME) == null ||
                            rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA) == null
                    ) return;

                    List<Text> lore = rightArrow.getComponents().get(DataComponentTypes.LORE).lines();

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

            if(activeInv == currentData.getLastPage() - 1 && index == currentData.getLastPage()) {
                ScreenHandler currScreenHandler = McUtils.containerMenu();
                if (currScreenHandler == null) {
                    return true;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return true;
            } else if(index == currentData.getLastPage()) {
                if(BankOverlay.getPersonalStorageUtils() == null) return true;

                activeInv = currentData.getLastPage() - 1;
                try {
                    BankOverlay.getPersonalStorageUtils().jumpToDestination(activeInv + 1);
                } catch (Exception e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                    return true;
                }
                clearAnnotationCache(activeInv);
                retryLoad();
            }

            return true;
        }

        public List<ItemStack> getItems() {
            return this.items;
        }

        public void setItems(List<ItemStack> items) {
            if (items != this.items) {
                invalidateSearchCache();
            }
            this.items = items;
        }

        private boolean containsSearch(String searchInput, SearchQueryParser.ParsedQuery query) {
            if (index != activeInv
                    && Objects.equals(searchInput, cachedContainsSearchInput)
                    && items == cachedContainsSearchItems) {
                return cachedContainsSearchResult;
            }

            boolean containsSearch = false;
            for(ItemStack stack : items) {
                if(stack == null || stack.isEmpty()) continue;

                WynnItem wynnItem = null;
                Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
                if (optWynnItem.isPresent()) {
                    wynnItem = optWynnItem.get();
                }

                if (SearchQueryParser.matches(stack, wynnItem, query)) {
                    containsSearch = true;
                    break;
                }
            }

            cachedContainsSearchInput = searchInput;
            cachedContainsSearchItems = items;
            cachedContainsSearchResult = containsSearch;
            return containsSearch;
        }

        private void invalidateSearchCache() {
            cachedContainsSearchInput = null;
            cachedContainsSearchItems = null;
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
    }

    private static class SlotWidget extends Widget {
        protected ItemStack stack;
        int index;
        final boolean isInventorySlot;
        final int inventoryIndex;
        private Optional<WynnItem> cachedWynnItem = null;
        private String cachedSearchInput = null;
        private boolean cachedSearchMatch = false;
        private CustomColor cachedHighlightColor = null;
        private ItemStack cachedDurabilityModelInput = null;
        private CustomModelDataComponent cachedDurabilityModelData = null;
        private int cachedDurabilityModelCount = -1;
        private ItemStack cachedDurabilityModelOutput = null;
        private ItemStack cachedRenderStateInput = null;
        private Object cachedRenderStateComponents = null;
        private int cachedRenderStateCount = -1;
        private ItemStack cachedRenderStateStack = null;
        private boolean cachedRenderStateOne = false;

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

            if(isEmptyStack(stack)) {
                renderSearchOverlay(ctx, stack, null, x + 1, y + 1);
                return;
            }

            if(slotHovered) {
                setHoveredSlot(stack, index, inventoryIndex, itemX, itemY);
            }

            if (cachedWynnItem == null) cachedWynnItem = asWynnItem(stack);
            Optional<WynnItem> item = cachedWynnItem;
            ItemStack renderStack = getCachedRenderStack(item);
            boolean renderOne = cachedRenderStateOne;

            renderEmeraldPouchRing(ctx, item.orElse(null), x + 1, y + 1);
            if (cachedHighlightColor == null) {
                cachedHighlightColor = getHighlightColor(stack);
            }
            renderHighlightOverlay(ctx, cachedHighlightColor, x + 1, y + 1);

            ctx.drawItem(renderStack, itemX, itemY);

            renderDurabilityRing(ctx, stack, item.orElse(null), x + 1, y + 1);

            try {
                ctx.drawStackOverlay(
                        frameTextRenderer != null ? frameTextRenderer : MinecraftClient.getInstance().textRenderer,
                        renderStack,
                        itemX,
                        itemY,
                        renderOne ? "1" : null
                );
            } catch (Exception ignored) {}

            renderItemOverlays(ctx, stack, x + 1, y + 1, item);

            // Inline cached search overlay (uses the frame-level parsed query).
            if (activeSearchInput != null && !activeSearchInput.isEmpty()) {
                if (!activeSearchInput.equals(cachedSearchInput)) {
                    cachedSearchMatch = SearchQueryParser.matches(stack, item.orElse(null), activeSearchQuery);
                    cachedSearchInput = activeSearchInput;
                }
                if (cachedSearchMatch) {
                    RenderUtils.drawRectBorders(ctx, SEARCH_MATCH_COLOR, x + 1, y + 1, x + 17, y + 17, 1);
                } else {
                    RenderUtils.drawRect(ctx, SEARCH_DIM_COLOR, x, y, 18, 18);
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
                this.cachedSearchInput = null;
                this.cachedHighlightColor = null;
                this.cachedDurabilityModelInput = null;
                this.cachedDurabilityModelData = null;
                this.cachedDurabilityModelCount = -1;
                this.cachedDurabilityModelOutput = null;
                this.cachedRenderStateInput = null;
                this.cachedRenderStateComponents = null;
                this.cachedRenderStateCount = -1;
                this.cachedRenderStateStack = null;
                this.cachedRenderStateOne = false;
            }
        }

        private ItemStack getCachedRenderStack(Optional<WynnItem> item) {
            Object components = stack.getComponents();
            int count = stack.getCount();
            if (stack == cachedRenderStateInput
                    && components == cachedRenderStateComponents
                    && count == cachedRenderStateCount
                    && cachedRenderStateStack != null) {
                return cachedRenderStateStack;
            }

            ItemStack renderStack = stack;
            boolean renderOne = false;
            if (item.isPresent()) {
                ItemAnnotation annotation = item.get();
                if (annotation instanceof PotionItem potionItem) {
                    renderStack = copyForRenderMutation(stack, renderStack);
                    renderStack.setCount(potionItem.getUses().current());
                }
                if (annotation instanceof MultiHealthPotionItem potionItem) {
                    int current = potionItem.getUses().current();
                    if(current == 1) renderOne = true;
                    else {
                        renderStack = copyForRenderMutation(stack, renderStack);
                        renderStack.setCount(current);
                    }
                }
                if (annotation instanceof CraftedConsumableItem consumableItem) {
                    renderStack = copyForRenderMutation(stack, renderStack);
                    renderStack.setCount(consumableItem.getCount());
                }
            }

            if (stack.getItem() == Items.POTION) {
                try {
                    Text customName = stack.getCustomName();
                    String customNameStr = customName != null ? customName.getString() : null;
                    if (customNameStr != null && customNameStr.contains("Potions")) {
                        Matcher matcher = POTIONS_USES_PATTERN.matcher(customNameStr);

                        if (matcher.find()) {
                            int remainingUses = Integer.parseInt(matcher.group(1));
                            renderStack = copyForRenderMutation(stack, renderStack);
                            renderStack.setCount(remainingUses);
                        }
                    }
                } catch (Exception ignored) {}
            }

            cachedRenderStateInput = stack;
            cachedRenderStateComponents = components;
            cachedRenderStateCount = count;
            cachedRenderStateStack = withoutVanillaDurabilityModelDataCached(renderStack);
            cachedRenderStateOne = renderOne;
            return cachedRenderStateStack;
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

        private SlotActionType determineActionType(int mouseButton) {
            SlotActionType actionType = SlotActionType.PICKUP;

            if(mouseButton == 1) return actionType;

            long now = System.currentTimeMillis();
            if (heldItem != null && heldItem.getItem() != Items.AIR) {
                if (now - lastClickTime < 250 && lastClickedSlot != null &&
                        lastClickedSlot.first() == inventoryIndex && lastClickedSlot.second() == index) {
                    actionType = SlotActionType.PICKUP_ALL;
                }
            }
            lastClickTime = now;

            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                actionType = SlotActionType.QUICK_MOVE;
            }

            return actionType;
        }

        @Override
        protected boolean onClick(int button) {
            if (button == 2) {
                if (stack != null && !stack.isEmpty() && searchbar2 != null) {
                    String itemName = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(stack.getName().getString()).replaceAll("").trim();
                    if (!itemName.isEmpty()) {
                        searchbar2.setInput(itemName);
                        for (PageWidget page : pages) page.invalidateSearchCache();
                    }
                }
                return true;
            }
            if (isReloading) return false;
            if(shouldWait) return false;
            if(!isMouseInOverlay && !isInventorySlot) return false;
            if(inventoryIndex >= currentData.getLastPage() && !isInventorySlot) return false;

            if(activeInv == inventoryIndex || isInventorySlot) {
                if(index == 4 && isInventorySlot) return false; //Ingredient pouch, clicking it within the bank overlay crashes the game
                if(index == 34 && isInventorySlot) return false; //Compass, clicking it within the bank overlay crashes the game
                if(index == 35 && isInventorySlot) return false; //Content book, clicking it within the bank overlay crashes the game

                SlotActionType action = determineActionType(button);

                ItemStack oldHeld = heldItem;
                heldItem = getHeldItem(index + (isInventorySlot ? 54 : 0), action, button);

                if(heldItem.getCustomName() != null) {
                    if ((heldItem.getCustomName().getString().contains("Pouch") || heldItem.getCustomName().getString().contains("Potions")) && button == 1) {
                        heldItem = oldHeld == null ? Items.AIR.getDefaultStack() : oldHeld;
                        return false;
                    }
                }

                if (shouldCancelEmeraldPouch(oldHeld, heldItem)) {
                    heldItem = Items.AIR.getDefaultStack();
                }

                if (MinecraftClient.getInstance().interactionManager == null) return false;

                MinecraftClient.getInstance().interactionManager.clickSlot(bankSyncid, index + (isInventorySlot ? 54 : 0), button, action, MinecraftClient.getInstance().player);
                clearAnnotationCache(inventoryIndex);
                lastClickedSlot = new Pair<>(inventoryIndex, index);
            } else if(heldItem.isEmpty()) {
                List<ItemStack> stacks = BankOverlay.activeInvSlots.stream()
                        .limit(45)
                        .map(Slot::getStack)
                        .collect(Collectors.toList());

                Pages.getBankPages().put(activeInv, stacks);
                activeInv = inventoryIndex;
                try {
                    BankOverlay.getPersonalStorageUtils().jumpToDestination(inventoryIndex + 1);
                } catch (Exception e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                    return true;
                }
                clearAnnotationCache(inventoryIndex);
            }
            return true;
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

            drawDynamicNameSign(ctx, textInputWidget.getInput(), x, y + 12);

            String pageName = textInputWidget.getInput().isEmpty()
                    ? Pages.getBankPageNames().getOrDefault(index, "Page " + (index + 1))
                    : textInputWidget.getInput();

            if (!Objects.equals(lastSavedPageName, pageName)) {
                Pages.getBankPageNames().put(index, pageName);
                lastSavedPageName = pageName;
            }

            textInputWidget.setTextColor((activeInv == index && !shouldWait) ? GOLD_TEXT_COLOR : WHITE_TEXT_COLOR);
            textInputWidget.setBounds(x, y, width, height);
            if (!Objects.equals(textInputWidget.getInput(), pageName)) {
                textInputWidget.setInput(pageName);
            }
            textInputWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            textInputWidget.onClick(button);
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled) return false;
            if (contains((int) mx, (int) my)) {
                setFocused(true);
                return onClick(button);
            }
            setFocused(false);
            if (textInputWidget != null) {
                textInputWidget.setFocused(false);
            }
            return false;
        }

        public boolean isInputFocused() {
            return textInputWidget != null && textInputWidget.isFocused();
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

    private static class QuickActionWidget extends Widget {
        public QuickActionWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            try {
                if(hovered && McUtils.containerMenu().getSlot(46) != null && McUtils.containerMenu().getSlot(46).getStack() != null) {
                    ctx.drawTooltip(
                        MinecraftClient.getInstance().textRenderer,
                        McUtils.containerMenu().getSlot(46).getStack().getTooltip(
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
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            ScreenHandler currScreenHandler = McUtils.containerMenu();
            if(currScreenHandler == null) { return false; }
            if(InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow(),
                ((KeybindingAccessor) MinecraftClient.getInstance().options.sneakKey).getBoundKey().getCode())
            ) {
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
            if (isReloading) return false;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            ScreenHandler currScreenHandler = McUtils.containerMenu();

            List<ItemStack> stacks = BankOverlay.activeInvSlots.stream()
                    .limit(45)
                    .map(Slot::getStack)
                    .collect(Collectors.toList());

            Pages.getBankPages().put(activeInv, stacks);
            activeInv = 0;
            actualOffset = 0;
            targetOffset = 0;
            currentData.save();
            BankOverlay2.pages.clear();
            heldItem = Items.AIR.getDefaultStack();
            BankOverlay.activeInvSlots.clear();
            annotationCache.clear();
            annotationStackCache.clear();
            annotationComponentCache.clear();
            Pages.save();

            if(currentOverlayType == BankOverlayType.CHARACTER) expectedOverlayType = BankOverlayType.ACCOUNT;
            else if(currentOverlayType == BankOverlayType.ACCOUNT) expectedOverlayType = BankOverlayType.CHARACTER;

            if(currScreenHandler == null) { return false; }
            clickOnSlot(47, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
            BankOverlay.resetScrollRegistration();
            return true;
        }
    }

    private static class AllCharactersButtonWidget extends Widget {
        public AllCharactersButtonWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonCustom(x, y, width, height, 3, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            String text = allCharactersBrowseMode ? "Back" : "All Characters";
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, 0.85f);
        }

        @Override
        protected boolean onClick(int button) {
            if (isReloading) return false;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            allCharactersBrowseMode = !allCharactersBrowseMode;
            if (allCharactersBrowseMode) {
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
            ui.drawButtonCustom(x, y, width, height, 3, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            String text;
            if (isReloading) {
                text = "Reloading " + (reloadCurrentPage + 1) + "/" + reloadTotalPages;
            } else {
                text = "Reload";
            }
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, WHITE_TEXT_COLOR, 0.85f);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            if (isReloading) {
                // Cancel reload
                isReloading = false;
                reloadPageLoaded = false;
                activeInv = reloadOriginalPage;
                try {
                    BankOverlay.getPersonalStorageUtils().jumpToDestination(reloadOriginalPage + 1);
                } catch (Exception ignored) {}
                retryLoad();
                Pages.save();
            } else {
                // Start reload
                if (allCharactersBrowseMode) return false; // Can't reload while browsing all chars
                reloadOriginalPage = activeInv;
                reloadTotalPages = Math.max(currentData.getLastPage(), BankOverlay.getCurrentMaxPages());
                if (reloadTotalPages <= 0) return false;
                reloadCurrentPage = 0;
                isReloading = true;
                reloadPageLoaded = false;
                activeInv = 0;
                try {
                    BankOverlay.getPersonalStorageUtils().jumpToDestination(1);
                } catch (Exception e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils."));
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
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            WynnExtrasConfig.INSTANCE.toggleBankOverlay = !WynnExtrasConfig.INSTANCE.toggleBankOverlay;
            if(WynnExtrasConfig.INSTANCE.toggleBankOverlay) {
                activeInv = Models.Bank.getCurrentPage() - 1;
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
            ui.drawSliderBackground(x, y, width, height);

            int totalRows = (int) Math.ceil((double) shownPages / xFitAmount);
            int c = (xFitAmount % 2 == 0 ? 1 : 0);
            int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * 3) - 104 * c);
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
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            int totalRows = (int) Math.ceil((double) shownPages / xFitAmount);
            int c = (xFitAmount % 2 == 0 ? 1 : 0);
            int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * 3) - 104 * c);
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
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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

        private final String characterId;
        private final String characterNickname;
        private final int characterLevel;
        private final int pageNumber;
        private final List<ItemStack> items;
        private final List<SlotWidget> slots = new ArrayList<>();
        private int topBorder;
        private int botBorder;
        private boolean slotsVisible = true;
        private int lastSlotLayoutX = Integer.MIN_VALUE;
        private int lastSlotLayoutY = Integer.MIN_VALUE;
        private double lastSlotLayoutScale = Double.NaN;
        private int lastSlotLayoutCount = -1;

        public CrossClassPageWidget(String characterId, String characterNickname, int characterLevel, int pageNumber, List<ItemStack> items, int topBorder, int botBorder) {
            super(0, 0, 0, 0);
            this.characterId = characterId;
            this.characterNickname = characterNickname;
            this.characterLevel = characterLevel;
            this.pageNumber = pageNumber;
            this.items = items != null ? items : new ArrayList<>();
            this.topBorder = topBorder;
            this.botBorder = botBorder;
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

            // Draw bank texture background
            ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankTextureDark : bankTexture, x, y, width, height);

            // Draw character label above the page
            String name = (characterNickname != null && !characterNickname.isEmpty())
                    ? characterNickname
                    : (characterId.length() > 8 ? characterId.substring(0, 8) + "..." : characterId);
            String levelStr = characterLevel > 0 ? " Lv." + characterLevel : "";
            ui.drawText("§e@" + name + levelStr + " §7Page " + pageNumber, x + 2, y - 9, YELLOW_TEXT_COLOR, 0.9f);

            if (items.isEmpty()) {
                setSlotsVisible(false);
                return;
            }

            // Create slots if needed
            if (slots.isEmpty()) {
                int i = 0;
                for (ItemStack itemStack : items) {
                    if (i >= 45) break;
                    CrossClassSlotWidget slot = new CrossClassSlotWidget(itemStack == null ? null : itemStack.copy(), i);
                    slots.add(slot);
                    i++;
                }
                updateValues();
            }

            int i = 0;
            for (SlotWidget slot : slots) {
                if (i >= items.size()) break;
                slot.setStack(items.get(i));
                slot.drawDirect(ctx, mouseX, mouseY, tickDelta, ui);
                i++;
            }
        }

        private boolean isCurrentCharacter() {
            return characterId != null && characterId.equals(BankOverlay.currentCharacterID);
        }

        private boolean isAccountBank() {
            return "__account__".equals(characterId);
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (ui == null) return;
            if (y > botBorder || y + height < topBorder) return;

            // Dim overlay
            ui.drawRect(x, y, width, height, CustomColor.fromHSV(40, 0.4f, 0.8f, 0.2f));

            // Border color: green for current character, blue for account, orange for others
            String borderColor = isCurrentCharacter() ? "55FF55" : isAccountBank() ? "5555FF" : "FFAA00";
            ui.drawRectBorders(x, y + 0.5f, x + 164, y + 92, CustomColor.fromHexString(borderColor));

            // Hint text
            String hint;
            if (isCurrentCharacter()) {
                hint = "§7Click to go to page";
            } else if (isAccountBank()) {
                hint = "§7Click to switch to account bank";
            } else {
                hint = "§7Click to /class";
            }
            ui.drawText(hint, x + 2, y + height - 10, GRAY_TEXT_COLOR, 0.7f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button == 0) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

                if (isCurrentCharacter()) {
                    // Switch back to character bank and navigate to this page
                    allCharactersBrowseMode = false;
                    crossClassPages.clear();
                    lastCrossClassSearchQuery = "";
                    crossClassSearchActive = false;
                    targetOffset = 0;
                    actualOffset = 0;

                    activeInv = pageNumber;
                    try {
                        BankOverlay.getPersonalStorageUtils().jumpToDestination(pageNumber + 1);
                    } catch (Exception ignored) {}
                    retryLoad();
                    return true;
                } else if (isAccountBank()) {
                    // Switch to account bank
                    allCharactersBrowseMode = false;
                    crossClassPages.clear();
                    lastCrossClassSearchQuery = "";
                    crossClassSearchActive = false;
                    targetOffset = 0;
                    actualOffset = 0;

                    // Click slot 47 to switch to account bank if we're on character bank
                    if (currentOverlayType == BankOverlayType.CHARACTER) {
                        ScreenHandler handler = McUtils.containerMenu();
                        if (handler != null) {
                            clickOnSlot(47, handler.syncId, 0, handler.getStacks());
                        }
                    }
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
                    allCharactersBrowseMode = false;
                    crossClassPages.clear();
                    crossClassSearchActive = false;
                    BankOverlay.currentOverlayType = BankOverlayType.NONE;
                    clickedClassSelectionEntity = false;

                    // Send initial /class after screen closes
                    julianh06.wynnextras.utils.TickScheduler.runAfterTicks(5, () -> Handlers.Command.queueCommand("class"));

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

            int i = 0;
            for (SlotWidget slot : slots) {
                slot.setBounds(
                        (int) (x + 18 * (i % 9) * ui.getScaleFactor() + 1),
                        (int) (y + 18 * (i / 9) * ui.getScaleFactor() + 1),
                        (int) (18 * ui.getScaleFactor()),
                        (int) (18 * ui.getScaleFactor())
                );
                i++;
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
                        searchbar2.setInput(itemName);
                        for (PageWidget page : pages) page.invalidateSearchCache();
                    }
                }
                return true;
            }
            return false;
        }
    }

}
