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
import com.wynntils.models.containers.containers.trademarket.TradeMarketBuyContainer;
import com.wynntils.models.containers.containers.trademarket.TradeMarketContainer;
import com.wynntils.models.containers.containers.trademarket.TradeMarketFiltersContainer;
import com.wynntils.models.containers.containers.trademarket.TradeMarketOrderContainer;
import com.wynntils.models.containers.containers.trademarket.TradeMarketSellContainer;
import com.wynntils.models.containers.containers.trademarket.TradeMarketTradesContainer;
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
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.TradeMarketComparisonPanel;
import julianh06.wynnextras.features.inventory.BankOverlayType;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;
import static com.wynntils.utils.wynn.ContainerUtils.shiftClickOnSlot;
import static julianh06.wynnextras.features.inventory.BankOverlay.*;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredStack;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredWynnitem;

public class BankOverlay2 extends WEHandledScreen {
    static ItemStack hoveredSlot = Items.AIR.getDefaultStack();
    int hoveredX = -1;
    int hoveredY = -1;
    public int hoveredIndex = -1;
    public int hoveredInvIndex = -1;

    static ItemHighlightFeature itemHighlightFeature;

    public Identifier buttonBackground = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg.png");
    public Identifier buttonBackgroundShort = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort.png");
    public Identifier buttonBackgroundDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbg_dark.png");
    public Identifier buttonBackgroundShortDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/buttonsbgshort_dark.png");

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

    public CallbackInfo ci;
    public HandledScreen<?> screen;
    public Function<Void, Void> close;

    public static float targetOffset = 0;
    static float actualOffset = 0;

    public static List<PageWidget> pages = new ArrayList<>();
    private static InventoryWidget inventoryWidget = null;
    private static SwitchButtonWidget switchButtonWidget = null;
    private static QuickActionWidget quickActionWidget = null;
    public static TextInputWidget searchbar2 = null;
    private static ToggleOverlayWidget toggleOverlayWidget = null;
    static ScrollBarWidget scrollBarWidget = null;

    // Cross-class search
    private static List<CrossClassPageWidget> crossClassPages = new ArrayList<>();
    private static String lastCrossClassSearchQuery = "";
    private static boolean crossClassSearchActive = false;
    // Character ID to highlight in /class menu (set when clicking cross-class page)
    public static String targetCharacterIdForClassMenu = null;
    public static String targetCharacterNameForClassMenu = null;
    public static int targetCharacterLevelForClassMenu = 0;

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

    private static boolean wynncraftItemDatabaseInitialized = false;

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

        if (!wynncraftItemDatabaseInitialized) {
            wynncraftItemDatabaseInitialized = true;
            if (FabricLoader.getInstance().isModLoaded("wynnmod")) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Class<?> clazz = Class.forName("com.wynnmod.wynncraft.item.map.WynncraftItemDatabase");
                        clazz.getMethod("initialize").invoke(null);
                    } catch (Exception ignored) {}
                });
            }
        }
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Pages = currentData;
        MinecraftClient mc = MinecraftClient.getInstance();
        if(mc.getWindow() == null || !mc.isRunning()) return;
        if(mc.player == null || mc.currentScreen == null) return;

        if(ui == null) {
            ui = new UIUtils(context, 1, 0, 0);
        }

        if(bankSyncid == 0) {
            bankSyncid = McUtils.containerMenu().syncId;
        }

        Pair<Integer, Integer> xyRemain = calculateLayout();
        int xRemain = xyRemain.first();
        int yRemain = xyRemain.second();

        int xStart = xRemain / 2 - 2;
        int yStart = yRemain / 2 - 2;
        int buttonWidgetsX = (int) ((xStart + (xFitAmount / 2) * (162 + 4) - 166) * ui.getScaleFactor());

        if(currentOverlayType != BankOverlayType.NONE && expectedOverlayType != BankOverlayType.NONE && currentOverlayType != expectedOverlayType) {
            RenderUtils.drawRect(context, CustomColor.fromInt(-804253680), 0, 0, mc.currentScreen.width, mc.currentScreen.height);
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

        if(currentOverlayType == BankOverlayType.NONE || MinecraftClient.getInstance() == null) return;

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

        if(!WynnExtrasConfig.INSTANCE.toggleBankOverlay) return;
        if(Pages == null) return;

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
                            BankOverlay.PersonalStorageUtils.jumpToDestination(reloadOriginalPage + 1);
                        } catch (Exception ignored) {}
                        retryLoad();
                        reloadPageLoaded = false;
                        Pages.save();
                    } else {
                        reloadPageLoaded = false;
                        activeInv = reloadCurrentPage;
                        try {
                            BankOverlay.PersonalStorageUtils.jumpToDestination(reloadCurrentPage + 1);
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
            for (int i = 0; i < currentMaxPages; i++) {
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
                        ui.drawText(placeholder, x + 50, y + 7, CustomColor.fromHexString("FFFFFF"), 1.25f);
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
                            page.lastInput = "";
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

                    // Auto-switch to character bank if we're on account bank
                    if (currentOverlayType == BankOverlayType.ACCOUNT) {
                        ScreenHandler handler = McUtils.containerMenu();
                        if (handler != null) {
                            clickOnSlot(47, handler.syncId, 0, handler.getStacks());
                        }
                    }
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

        context.getMatrices().pushMatrix();
        ci.cancel();

        if (WynnExtras.testInv == null) {
            WynnExtras.testInv = screen.getScreenHandler().slots;
        }

        drawBackgroundRect(context, xRemain, yRemain);

        isMouseInOverlay = mouseY > yStart && mouseY < yStart + 100 * (yFitAmount - 1);

        int pageAmount = 0;
        {
            int i = 0;
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

            // Trigger cross-class search if needed
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
                for(PageWidget page : pages) {
                    float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                    float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) - actualOffset;
                    page.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                    page.setItems(buildInventoryForIndex(i, false));
                    page.updateValues();

                    if(searchInput != null && !searchInput.isEmpty()) {
                        // Check if this page was already evaluated for the current search term
                        boolean alreadyMatched = searchInput.equals(page.lastInput);
                        boolean containsSearch = alreadyMatched;

                        // Only search if we don't already know the result
                        if (!alreadyMatched) {
                            // Use advanced search parser
                            SearchQueryParser.ParsedQuery query = SearchQueryParser.parse(searchInput);

                            for(ItemStack stack : page.getItems()) {
                                if(stack == null) continue;
                                if(stack.isEmpty()) continue;

                                // Get WynnItem annotation if available
                                WynnItem wynnItem = null;
                                Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
                                if (optWynnItem.isPresent()) {
                                    wynnItem = optWynnItem.get();
                                }

                                // Use advanced search matching
                                if (SearchQueryParser.matches(stack, wynnItem, query)) {
                                    containsSearch = true;
                                    page.lastInput = searchInput;
                                    break;
                                }
                            }
                        }

                        if(!containsSearch) {
                            i++;
                            page.setEnabled(false);
                            page.lastInput = "";
                            continue;
                        } else {
                            page.setEnabled(true);
                            pageAmount++;
                        }
                    } else {
                        // No search input - reset lastInput and enable page
                        page.lastInput = "";
                        page.setEnabled(true);
                        pageAmount++;
                    }

                    if(invY > yStart - 100 && invY < yStart + 103 * (yFitAmount - 1)) page.draw(context, mouseX, mouseY, delta, ui);
                    i++;
                    visuali++;
                }
            }

            // Render cross-class pages after regular pages
            if (crossClassSearchActive && !crossClassPages.isEmpty()) {
                for (CrossClassPageWidget ccPage : crossClassPages) {
                    float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                    float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) - actualOffset;
                    ccPage.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                    ccPage.updateValues();

                    if (invY > yStart - 100 && invY < yStart + 103 * (yFitAmount - 1)) {
                        ccPage.draw(context, mouseX, mouseY, delta, ui);
                    }
                    visuali++;
                    pageAmount++;
                }
            }

            context.disableScissor();

            inventoryWidget.setBounds(buttonWidgetsX + 160, yStart + (yFitAmount - 1) * (90 + 4 + 10) - 3, (int) (176 * ui.getScaleFactor()), (int) (86 * ui.getScaleFactor()));
            inventoryWidget.setItems(buildInventoryForIndex(0, true));
            inventoryWidget.updateValues();
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

            searchbar2.setTextColor(CustomColor.fromHexString("FFFFFF"));
            searchbar2.setBackgroundColor(CustomColor.fromHSV(0, 0, 0, 0.5f));
            searchbar2.setFocusedColor(CustomColor.fromHSV(0, 0, 0.15f, 0.6f));
            searchbar2.draw(context, mouseX, mouseY, delta, ui);

            if (!allCharactersBrowseMode) {
                if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                    ui.drawCenteredText("Switch to " + (currentOverlayType == BankOverlayType.ACCOUNT ? "Character" : "Account") + " Bank", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, CustomColor.fromHexString("FFFFFF"), 1.1f);
                }
                if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                    ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 44, CustomColor.fromHexString("FFFFFF"), 1.1f);
                } else {
                    ui.drawCenteredText("Quick Actions", buttonWidgetsX + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, CustomColor.fromHexString("FFFFFF"), 1.1f);
                }
            }
        }

        shownPages = pageAmount;

        drawEmeraldOverlay(context, xStart - 36, yStart - 14);
        if (WynnExtrasConfig.INSTANCE.bankBagOverlay
                && (currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER)) {
            cacheCurrentBankPageIfPossible();
            drawBagOverlay(
                    context,
                    xStart - 36 - 56,
                    yStart - 14 + 4 * 28,
                    getCurrentPageStacks(),
                    collectAccountAndCharacterBagCounts());
        }

        renderHoveredSlotHighlight(context,  screen);
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
    }

    private void drawBackgroundRect(DrawContext context, float xRemain, float yRemain) {
        if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
            RenderUtils.drawRect(
                    context,
                    CustomColor.fromHexString("2c2d2f"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context,
                    CustomColor.fromHexString("1b1b1c"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
            );
        } else {
            RenderUtils.drawRect(
                    context,
                    CustomColor.fromHexString("81644b"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context,
                    CustomColor.fromHexString("4f342c"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 1
            );
        }
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

        for(PageWidget page : pages) {
            page.mouseClicked(x, y, button);
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
            BankPageNameInputsByType.putIfAbsent(currentOverlayType, new HashMap<>());

            for (int i = 0; i < currentMaxPages; i++) {
                BankPageNameInputsByType.get(currentOverlayType).put(i, new EasyTextInput(-1000, -1000, 13, 162 + 4));
            }

            initializedTypes.add(currentOverlayType);
        }

        if (Pages == null) Pages = currentData;

        PersonalStorageUtilitiesFeatureAccessor accessor = (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.PersonalStorageUtils;
        accessor.setLastPage(99);

        hoveredInvIndex = -1;
        hoveredIndex = -1;
        hoveredSlot = Items.AIR.getDefaultStack();

        if (activeInv == -1) activeInv = 1;
    }

    private Pair<Integer, Integer> calculateLayout() {
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();

        xFitAmount = Math.min(24, Math.floorDiv(screenWidth - 84, 162));
        yFitAmount = Math.min(24, Math.floorDiv(screenHeight, 104));

        xFitAmount = Math.min(xFitAmount, WynnExtrasConfig.INSTANCE.bankOverlayMaxColumns);
        yFitAmount = Math.min(yFitAmount, WynnExtrasConfig.INSTANCE.bankOverlayMaxRows + 1);

        if (currentData != null && currentData.lastPage > 0 && WynnExtrasConfig.INSTANCE.bankOverlayHideEmptyRows) {
            int totalPages = currentData.lastPage;
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

        return new Pair<>(xRemain, yRemain);
    }

    private List<ItemStack> buildInventoryForIndex(int index, boolean isPlayerInv) {
        List<ItemStack> inv = new ArrayList<>();

        if(isPlayerInv) {
            List<Slot> slots = BankOverlay.playerInvSlots;
            if (slots != null && slots.size() >= 36) {
                for (int j = 0; j < 36; j++) inv.add(slots.get(j).getStack().copy());
            } else {
                for (int j = 0; j < 36; j++) inv.add(Items.AIR.getDefaultStack());
            }
            return inv;
        }

        if (index == activeInv) {
            List<Slot> slots = BankOverlay.activeInvSlots;
            if (slots.size() < 45) {
                retryLoad();
                return inv;
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
                        return new ArrayList<>();
                    }
                    if(rightArrow == null) return new ArrayList<>();
                    if(rightArrow.getItem() == Items.POTION) {
                        String rawText = rightArrow.getName().getString();
                        String cleanedText = rawText.replaceAll("§[0-9a-fk-or]", "");
                        if (!cleanedText.contains("Page " + (activeInv + 2))) {
                            shouldWait = true;
                            if (!oldShouldWait) {
                                shouldWaitSince = System.currentTimeMillis();
                            }
                        } else if (oldShouldWait) {
                            Pages.BankPages.put(activeInv, slots.stream().map(Slot::getStack).toList());
                            if(annotationCache.get(activeInv) != null) annotationCache.get(activeInv).clear();
                        }
                    } else if(activeInv != currentData.lastPage - 1) {
                        if (!shouldWait) {
                            shouldWait = true;
                            shouldWaitSince = System.currentTimeMillis();
                        }
                    }
                }

                if (shouldWait) {
                    long waitDuration = System.currentTimeMillis() - shouldWaitSince;

                    if (waitDuration > 1500) {
                        System.out.println("retrying jump");
                        shouldWaitSince = System.currentTimeMillis();
                        retryLoad();
                        PersonalStorageUtilitiesFeatureAccessor accessor =
                                (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.PersonalStorageUtils;
                        accessor.setLastPage(99);
                        try {
                            BankOverlay.PersonalStorageUtils.jumpToDestination(activeInv + 1);
                        } catch (Exception e) {
                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                        }
                    }
                    List<ItemStack> cached = Pages.BankPages.get(activeInv);
                    if (cached != null && j < cached.size()) inv.add(cached.get(j));
                    continue;
                }

                inv.add(slots.get(j).getStack().copy());
            }
        } else {
            List<ItemStack> cached = Pages.BankPages.get(index);
            if (cached != null && cached.size() >= 45) {
                inv.addAll(cached.subList(0, 45));
            } else {
                for (int j = 0; j < 45; j++) inv.add(Items.AIR.getDefaultStack());
            }
        }

        return inv;
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

    private static void applyAnnotation(ItemStack stack, List<ItemAnnotation> annotations, int index) {
        if(stack == null) {
            annotations.add(null);
            return;
        }

        if(stack.getItem() == Items.AIR) return;

        if(annotations.size() <= index) return;

        ItemAnnotation annotation = annotations.get(index);
        if(annotation == null) {
            Text stackName = stack.getName();
            if(stack.getCustomName() != null) {
                if (stack.getCustomName().toString().contains("Key")) {
                    String clean = WynnUtils.normalizeBadString(stackName.getString());
                    stackName = Text.of(clean);
                }
            }
            StyledText name = StyledText.fromComponent(stackName);
            annotation = ((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(stack, name);
            annotations.set(index, annotation);
        }

        ((ItemStackExtension) (Object) stack).setAnnotation(annotation);
    }

    private static void renderDurabilityRing(DrawContext context, ItemStack stack, int x, int y) {
        Models.Item.asWynnItemProperty(stack, DurableItemProperty.class).ifPresent(durable -> {
            CappedValue durability = durable.getDurability();
            float fraction = (float) durability.current() / durability.max();
            int colorInt = MathHelper.hsvToRgb(Math.max(0.0F, fraction) / 3.0F, 1.0F, 1.0F);
            CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

            RenderUtils.drawArc(context, color, x, y, fraction, 6, 8);
        });
    }

    private static void renderEmeraldPouchRing(DrawContext context, ItemStack stack, int x, int y) {
        Models.Item.asWynnItem(stack, EmeraldPouchItem.class).ifPresent(pouch -> {
            CappedValue capacity = new CappedValue(pouch.getValue(), pouch.getCapacity());
            float fraction = (float) capacity.current() / capacity.max();
            int colorInt = MathHelper.hsvToRgb((1.0F - fraction) / 3.0F, 1.0F, 1.0F);
            CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

            RenderUtils.drawArc(context, color, x - 2, y - 2, Math.min(1.0F, fraction), 8, 10);
        });
    }

    private static void renderHighlightOverlay(DrawContext context, ItemStack stack, int x, int y) {
         if(stack.getItem() == Items.AIR) return;
         if (itemHighlightFeature == null) itemHighlightFeature = Managers.Feature.getFeatureInstance(ItemHighlightFeature.class);
         CustomColor color = ((ItemHighlightFeatureInvoker) itemHighlightFeature).invokeGetHighlightColor(stack, false);
         if (!Objects.equals(color, CustomColor.NONE)) {
             try {
                 RenderUtils.drawTexturedRect(
                     context,
                     Texture.HIGHLIGHT.identifier(),
                     color, (float)(x - 1), (float)(y - 1), 18.0F, 18.0F,
                     ((ItemHighlightFeature.HighlightTexture) itemHighlightFeature.getConfigOptionFromString("highlightTexture").get().get()).ordinal() * 18,
                     0.0F, 18.0F, 18.0F,
                     Texture.HIGHLIGHT.width(),
                     Texture.HIGHLIGHT.height());
             } catch (Exception ignored) {}
         }
    }

    private static void renderItemOverlays(DrawContext context, ItemStack stack, int x, int y) {
        Optional<WynnItem> item = asWynnItem(stack);
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

                 ((ItemTextOverlayFeatureMixin) Managers.Feature.getFeatureInstance(ItemTextOverlayFeature.class)).invokeDrawTextOverlay(context, stack, x, y, false);
            }

            ((UnidentifiedItemIconFeatureInvoker) Managers.Feature.getFeatureInstance(UnidentifiedItemIconFeature.class)).invokeDrawIcon(context, stack, x, y, 100);
            if(((ItemFavoriteFeatureAccessor) Managers.Feature.getFeatureInstance(ItemFavoriteFeature.class)).callIsFavorited(stack)) {
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

    private static void renderSearchOverlay(DrawContext context, ItemStack stack, int x, int y) {
        String rawInput = searchbar2.getInput();
        if (rawInput == null || rawInput.isEmpty()) return;

        // Strip @ for cross-class search indicator
        String input = rawInput.replace("@", "").trim();
        if (input.isEmpty()) return; // Just @ with no search term - show all

        if (stack == null || stack.isEmpty() || stack.getItem().equals(Items.AIR)) {
            RenderUtils.drawRect(context, CustomColor.fromHSV(0, 0, 0, 0.75f), x - 1, y - 1, 18, 18);
            return;
        }

        // Use advanced search parser for matching
        SearchQueryParser.ParsedQuery query = SearchQueryParser.parse(input);

        // Get WynnItem if available
        WynnItem wynnItem = null;
        Optional<WynnItem> optWynnItem = Models.Item.getWynnItem(stack);
        if (optWynnItem.isPresent()) {
            wynnItem = optWynnItem.get();
        }

        if (SearchQueryParser.matches(stack, wynnItem, query)) {
            // Item matches - draw green border
            RenderUtils.drawRectBorders(context, CustomColor.fromHexString("00FF00"), x, y, x + 16, y + 16, 1);
        } else {
            // Item doesn't match - dim it
            RenderUtils.drawRect(context, CustomColor.fromHSV(0, 0, 0, 0.75f), x - 1, y - 1, 18, 18);
        }
    }

    private void renderHoveredSlotHighlight(DrawContext context, HandledScreen<?> screen) {
        if (hoveredIndex == -1) return;

        Inventory dummy = new SimpleInventory(1);
        Slot focusedSlot = new Slot(dummy, hoveredIndex, 0, 0);
        ((SlotAccessor) focusedSlot).setX(hoveredX);
        ((SlotAccessor) focusedSlot).setY(hoveredY);
        ((HandledScreenAccessor) screen).setFocusedSlot(focusedSlot);

        ((HandledScreenInvoker) screen).invokeDrawSlotHighlightBack(context);
        ((HandledScreenInvoker) screen).invokeDrawSlotHighlightFront(context);
    }

    private void renderHoveredTooltip(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (hoveredSlot.getItem() == Items.AIR) return;

        try {
            if (FabricLoader.getInstance().isModLoaded("wynnventory")) {
                initWynnventoryReflection();

                if (wynnventoryReady) {
                    ItemStack stack = hoveredSlot;

                    if (WynnExtrasConfig.INSTANCE.wynnventoryOverlay && stack != null) {
                        renderPriceTooltipReflective(context, mouseX, mouseY, stack);
                    }
                }
            }
        } catch (Throwable ignored) {}

        Optional<WynnItem> item = asWynnItem(hoveredSlot);
        List<Text> tooltip = item.map(i -> {
                    currentHoveredStack = hoveredSlot;
                    currentHoveredWynnitem = i;
                    return TooltipUtils.getWynnItemTooltip(hoveredSlot, i);
                }).filter(t -> !t.isEmpty())
                .orElse(hoveredSlot.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC));

        List<TooltipComponent> components = new ArrayList<>(TooltipUtils.getClientTooltipComponent(tooltip));

        if (item.isPresent() && item.get() instanceof GearBoxItem gearBox) {
            List<Text> addon = ((ItemGuessFeatureAccessor)
                    Managers.Feature.getFeatureInstance(ItemGuessFeature.class))
                    .callGetTooltipAddon(gearBox);

            tooltip.addAll(addon);
            components.addAll(TooltipUtils.getClientTooltipComponent(addon));
        }

        int tooltipHeight = TooltipUtils.getTooltipHeight(components);
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
            context.drawTooltip(screen.getTextRenderer(), tooltip, (int) (mouseX / scale), y);
        } else {
            drawTooltip(screen.getTextRenderer(), components, (int) (mouseX + 14 * scale), y, context);
        }

    }

    private void renderPriceTooltipReflective(DrawContext context, int x, int y, ItemStack stack) {
        try {
            List<Text> vanillaLines = stack.getTooltip(
                    Item.TooltipContext.DEFAULT,
                    MinecraftClient.getInstance().player,
                    TooltipType.BASIC
            );

            if (vanillaLines == null || vanillaLines.isEmpty()) return;

            if (tooltipFactory == null) {
                Object builder = builderCtor.newInstance();
                tooltipFactory = factoryCtor.newInstance(builder);
            }

            List<Text> priceLines =
                    (List<Text>) getPriceTooltipMethod.invoke(tooltipFactory, stack);

            if (priceLines == null || priceLines.isEmpty()) return;

            List<?> priceComponents =
                    (List<?>) toClientComponentsMethod.invoke(null, priceLines, Optional.empty());

            List<?> vanillaComponents =
                    (List<?>) toClientComponentsMethod.invoke(null, vanillaLines, stack.getTooltipData());

            Object coords = calculateCoordsMethod.invoke(null, x, y, vanillaComponents, priceComponents);

            int cx = (int) coords.getClass().getField("x").get(coords);
            int cy = (int) coords.getClass().getField("y").get(coords);

            Object positioner = fixedPositionerClass
                    .getConstructor(int.class, int.class)
                    .newInstance(cx, cy);

            context.getMatrices().pushMatrix();

            float scale = (float) getScaleFactorMethod.invoke(null, priceComponents);
            context.getMatrices().scale(scale, scale);

            context.drawTooltipImmediately(
                    MinecraftClient.getInstance().textRenderer,
                    (List<TooltipComponent>) priceComponents,
                    x,
                    y,
                    (TooltipPositioner) positioner,
                    stack.get(DataComponentTypes.TOOLTIP_STYLE)
            );

            context.getMatrices().popMatrix();

        } catch (Exception ignored) {}
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
        InventoryEmeraldCountFeature emeraldCountFeature = Managers.Feature.getFeatureInstance(InventoryEmeraldCountFeature.class);
        int emeraldAmountInt = Models.Emerald.getAmountInContainer();
        String[] emeraldAmounts = ((InventoryEmeraldCountFeatureInvoker) emeraldCountFeature).invokeGetRenderableEmeraldAmounts(emeraldAmountInt);

        y += (3 * 28);


        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            String emeraldAmount = emeraldAmounts[i];

            if (emeraldAmount.equals("0")) continue;

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

            context.drawItem(EmeraldUnits.values()[i].getItemStack(), x + 6, y + 6 - (i * 28));

            if (EmeraldUnits.values()[i].getSymbol().equals("stx")) { // Make stx not look like normal LE
                context.drawItem(EmeraldUnits.values()[i].getItemStack(), x + 3, y + 4 - (i * 28));
                context.drawItem(EmeraldUnits.values()[i].getItemStack(), x + 6, y + 6 - (i * 28));
                context.drawItem(EmeraldUnits.values()[i].getItemStack(), x + 9, y + 8 - (i * 28));
            } else {
                // This needs to be separate since Z levels are determined by order here
                context.drawItem(EmeraldUnits.values()[i].getItemStack(), x + 6, y + 6 - (i * 28));
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
    private static final String[] BAG_RAID_ORDER = {"NOG", "NOL", "TCC", "TNA", "TWP"};
    private static final GearTier[] BAG_TIER_ORDER = {GearTier.LEGENDARY, GearTier.RARE, GearTier.UNIQUE};

    /** Sort mode for the top-right bag breakdown. Click the "[By Type]"/"[By Count]" label to toggle. */
    public enum BagSortMode { BY_TYPE, BY_AMOUNT }
    public static BagSortMode bagSortMode = BagSortMode.BY_TYPE;
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

    /** Simple (raidAbbrev, tier) pair for the top-right text lines. */
    private record BagCountEntry(String raidAbbrev, GearTier tier, int count) {}

    /**
     * Draws the "Total Bags" block floating in the top-right corner. Shows per-(raid, tier)
     * lines (skipping zero-count combos) and a clickable sort-mode toggle below the total.
     * Uses pre-counted numbers so it doesn't depend on Wynntils item annotations.
     */
    private static void drawBagTopRightHeader(DrawContext context, Map<String, Integer> totalCounts) {
        int totalCount = 0;
        for (int c : totalCounts.values()) totalCount += c;

        // Build non-zero lines
        List<BagCountEntry> lines = new ArrayList<>();
        for (String raid : BAG_RAID_ORDER) {
            for (GearTier tier : BAG_TIER_ORDER) {
                String key = raid + "|" + tier.name();
                int count = totalCounts.getOrDefault(key, 0);
                if (count > 0) lines.add(new BagCountEntry(raid, tier, count));
            }
        }

        // Sort per mode
        if (bagSortMode == BagSortMode.BY_AMOUNT) {
            lines.sort((a, b) -> Integer.compare(b.count(), a.count()));
        } else {
            lines.sort(Comparator
                    .<BagCountEntry>comparingInt(e -> indexInArray(BAG_RAID_ORDER, e.raidAbbrev()))
                    .thenComparingInt(e -> e.tier() != null ? -e.tier().ordinal() : 1));
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
            String tierName = e.tier() != null
                    ? e.tier().name().charAt(0) + e.tier().name().substring(1).toLowerCase()
                    : "?";
            String line = e.raidAbbrev() + " " + tierName + ": " + e.count();
            int lineWidth = tr.getWidth(line);
            context.drawText(tr, line, screenWidth - lineWidth - 5, lineY, getTierColorArgb(e.tier()), true);
            lineY += tr.fontHeight + 1;
        }
    }

    private static int indexInArray(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) return i;
        }
        return Integer.MAX_VALUE;
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
        Map<String, BagGroup> groups = groupBagsFromStacks(stacks);
        if (groups.isEmpty()) return;

        // Only render raids where at least one tier has a bag in the scoped data
        List<String> visibleRaids = new ArrayList<>();
        for (String raid : BAG_RAID_ORDER) {
            for (GearTier tier : BAG_TIER_ORDER) {
                BagGroup g = groups.get(raid + "|" + tier.name());
                if (g != null && g.count > 0) {
                    visibleRaids.add(raid);
                    break;
                }
            }
        }
        if (visibleRaids.isEmpty()) return;

        int row = 0;
        for (String raid : visibleRaids) {
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
                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                context,
                                StyledText.fromString(raid),
                                cellX + 1,
                                cellX + 28 - 1,
                                cellY + 1,
                                cellY + 28 - 2,
                                0,
                                tierColor,
                                HorizontalAlignment.LEFT,
                                VerticalAlignment.TOP,
                                TextShadow.OUTLINE);

                // Count in bottom-right (dimmed when zero, compacted for large counts so "1200" fits)
                CustomColor countColor = count > 0 ? CommonColors.WHITE : CustomColor.fromInt(0xFF808080);
                FontRenderer.getInstance()
                        .renderAlignedTextInBox(
                                context,
                                StyledText.fromString(formatCompactCount(count)),
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

    /** Compact-format a count so it fits in a 28px-wide box. 1420 -> "1.42k", 12345 -> "12.3k". */
    private static String formatCompactCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 10000) return String.format("%.2fk", count / 1000.0);
        if (count < 100000) return String.format("%.1fk", count / 1000.0);
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
    public static void drawVanillaBankBagsOverlay(DrawContext context, HandledScreen<?> screen) {
        if (!WynnExtrasConfig.INSTANCE.bankBagOverlay) return;
        if (WynnExtrasConfig.INSTANCE.toggleBankOverlay && currentOverlayType != BankOverlayType.NONE) return;

        // Only show in account bank or character bank
        Container container = Models.Container.getCurrentContainer();
        if (!(container instanceof AccountBankContainer) && !(container instanceof CharacterBankContainer)) return;

        cacheCurrentBankPageIfPossible();

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int x = accessor.getX() + accessor.getBackgroundWidth() + 4;
        int y = accessor.getY() + 14;

        drawBagOverlay(context, x, y, getCurrentPageStacks(), collectAccountAndCharacterBagCounts());
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
     * numbers in {@code BankData.bagCounts[pageNum]}. Auto-persists to disk (debounced).
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

        // Count bags from live (annotated) stacks
        HashMap<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : live) {
            if (stack == null || stack.isEmpty()) continue;
            Optional<WynnItem> item = asWynnItem(stack);
            if (item.isEmpty() || !(item.get() instanceof CrafterBagItem bag)) continue;
            RaidKind raidKind = bag.getRaidKind();
            GearTier tier = bag.getGearTier();
            String key = (raidKind != null ? raidKind.getAbbreviation() : "?")
                    + "|" + (tier != null ? tier.name() : "?");
            counts.merge(key, stack.getCount(), Integer::sum);
        }

        // Check if anything changed before saving
        HashMap<String, Integer> existing = data.bagCounts.get(pageNum);
        if (existing != null && existing.equals(counts)) return;

        data.bagCounts.put(pageNum, counts);

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
     * (stored as plain numbers in BankData.bagCounts, so they don't depend on
     * Wynntils item annotations).
     */
    /**
     * Aggregates bag counts from Account Bank + Character Bank only.
     */
    private static Map<String, Integer> collectAccountAndCharacterBagCounts() {
        Map<String, Integer> totals = new HashMap<>();
        for (BankData data : new BankData[]{AccountBankData.INSTANCE, CharacterBankData.INSTANCE}) {
            if (data == null || data.bagCounts == null) continue;
            for (Map<String, Integer> pageCounts : data.bagCounts.values()) {
                if (pageCounts == null) continue;
                for (Map.Entry<String, Integer> e : pageCounts.entrySet()) {
                    totals.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
        }
        return totals;
    }

    private static Map<String, BagGroup> groupBagsFromStacks(Iterable<ItemStack> stacks) {
        Map<String, BagGroup> groups = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            Optional<WynnItem> item = asWynnItem(stack);
            if (item.isEmpty() || !(item.get() instanceof CrafterBagItem bag)) continue;

            RaidKind raidKind = bag.getRaidKind();
            GearTier tier = bag.getGearTier();
            String raidAbbrev = raidKind != null ? raidKind.getAbbreviation() : "?";
            String key = raidAbbrev + "|" + (tier != null ? tier.name() : "?");

            BagGroup group = groups.get(key);
            if (group == null) {
                group = new BagGroup(raidAbbrev, tier, stack);
                groups.put(key, group);
            }
            group.count += stack.getCount();
        }
        return groups;
    }

    private static List<ItemStack> getCurrentPageStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        // Only trust activeInvSlots while the custom overlay is actively managing it.
        // In vanilla mode it's often empty OR holds stale references from a previous
        // custom-mode session, which would silently corrupt the cache.
        boolean customOverlayActive = WynnExtrasConfig.INSTANCE.toggleBankOverlay
                && currentOverlayType != BankOverlayType.NONE;
        if (customOverlayActive && BankOverlay.activeInvSlots != null && !BankOverlay.activeInvSlots.isEmpty()) {
            for (Slot slot : BankOverlay.activeInvSlots) {
                stacks.add(slot.getStack());
            }
            return stacks;
        }
        // Otherwise read the current ScreenHandler directly (excludes player inventory slots).
        MinecraftClient mc = MinecraftClient.getInstance();
        ScreenHandler menu = McUtils.containerMenu();
        if (menu == null || mc.player == null) return stacks;
        Inventory playerInv = mc.player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.inventory == playerInv) continue;
            stacks.add(slot.getStack());
        }
        return stacks;
    }

    /**
     * Returns the 0-based current bank page number (matching the Pages.BankPages cache key
     * convention, see HandledScreenMixin.onClose which does Pages.BankPages.put(activeInv, ...)),
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
    public Slot touchHoveredSlot;

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
                    addChild(slot);
                    i++;
                }
            }

            if(annotationCache.get(99) != null && annotationCache.get(99).isEmpty()) annotationCache.put(99, null);

            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(99, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));

            int i = 0;
            for(SlotWidget slot : slots) {
                applyAnnotation(items.get(i), annotations, i);
                slot.setStack(items.get(i));
                i++;
            }
        }

        @Override
        protected void updateValues() {
            if(slots.isEmpty()) return;

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

        public String lastInput = "";

        List<ItemStack> items;
        List<SlotWidget> slots = new ArrayList<>();
        final int index;
        int topBorder;
        int botBorder;

        public NameSignWidget sign;

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
            if(y > botBorder || y + height < topBorder) return;
            if(index >= currentData.lastPage) {
                if(sign == null) {
                    sign = new NameSignWidget(index);
                    addChild(sign);
                }

                sign.setBounds(x, y - 10, width, 10);
                ui.drawRect(x, y, width, height, CustomColor.fromHSV(0, 0, 0, 0.25f));
                return;
            }

            ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? bankTextureDark : bankTexture, x, y, width, height);

            if(items.isEmpty()) return;

            if(slots.isEmpty()) {
                int i = 0;
                for (ItemStack itemStack : items) {
                    SlotWidget slot = new SlotWidget(itemStack == null ? null : itemStack.copy(), i, false, index);
                    slots.add(slot);
                    addChild(slot);
                    i++;
                }
            }

            if(annotationCache.get(index) != null && annotationCache.get(index).isEmpty()) annotationCache.put(index, null);

            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(index, k -> new ArrayList<>(Collections.nCopies(slots.size(), null)));

            int i = 0;
            for(SlotWidget slot : slots) {
                if(i >= items.size()) break;
                applyAnnotation(items.get(i), annotations, i);
                slot.setStack(items.get(i));
                i++;
            }

            if(sign == null) {
                sign = new NameSignWidget(index);
                addChild(sign);
            }

            sign.setBounds(x, y - 10, width, 10);
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(McUtils.containerMenu() != null && index == currentData.lastPage) {
                if(priceText == null) {
                    String text = "§c✖ §7Price: §funknown.";
                    String text2 = "§7Go to page §f" + currentData.lastPage + " §7to check.";

                    ui.drawCenteredText(text, x + 81, y + 10, CustomColor.fromHexString("FFFFFF"), 1);
                    ui.drawCenteredText(text2, x + 81, y + 20, CustomColor.fromHexString("FFFFFF"), 1);
                } else {
                    ui.drawCenteredText(priceText, x + 81, y + 15, CustomColor.fromHexString("FFFFFF"), 1);
                }

                if (hovered) {
                    String buyText = confirmText.isEmpty() ? "§7Click to buy." : confirmText;

                    ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_unlocked_dark : lock_unlocked, x + 82 - 25, y + 46 - 19, 50, 50);
                    ui.drawCenteredText(buyText, x + 81, y + 80, CustomColor.fromHexString("FFFFFF"), 1);
                } else {
                    ui.drawImage(WynnExtrasConfig.INSTANCE.darkmodeToggle ? lock_locked_dark : lock_locked, x + 82 - 25, y + 46 - 19, 50, 50);
                }
            }

            if(index >= currentData.lastPage) return;

            if(hovered && isMouseInOverlay) {
                if(index != activeInv) {
                    ui.drawRect(x, y, width, height, CustomColor.fromHSV(0, 0, 1000, 0.25f));
                }
            }

            if(activeInv == index) {
                if(shouldWait) {
                    ui.drawRect(x, y, width, height, CustomColor.fromHexString("000000").withAlpha(0.75f));
                    int dots = (int) ((System.currentTimeMillis() / 750) % 3) + 1;

                    String arrowtext = "";

                    ItemStack rightArrow = null;
                    try {
                        rightArrow = McUtils.containerMenu().getSlot(52).getStack();
                    } catch (IndexOutOfBoundsException e) { }

                    if(rightArrow != null) {
                        if (rightArrow.getItem() == Items.POTION) {
                            String rawText = rightArrow.getName().getString();
                            String cleanedText = rawText.replaceAll("§[0-9a-fk-or]", "");
                            arrowtext = cleanedText;
                        }
                    }

                    String loadingText = "Loading" + ".".repeat(dots);

                    ui.drawCenteredText(loadingText, x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 1.5f);
                } else {
                    ui.drawRectBorders(x, y + 0.5f, x + 164, y + 92, CustomColor.fromHexString("FFFF00"));
                }
                CustomColor color = (!shouldWait)
                        ? CustomColor.fromHexString("FFFF00")
                        : CustomColor.fromHexString("FFFFFF");
            } else if (!hovered || !isMouseInOverlay) {
                ui.drawRect(x, y, width, height, CustomColor.fromHSV(0, 0, 0, 0.25f));
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
                        currentData.lastPage = Math.max(currentData.lastPage, activeInv + 1);
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
                    } else if (rightArrow.getCustomName() != null && rightArrow.getCustomName().getString().contains(String.valueOf(currentData.lastPage + 1)) && activeInv == currentData.lastPage - 1) {
                        currentData.lastPage++;
                        pageBuyCustomModelData = 0;
                        priceText = null;
                        retryLoad();
                    }
                } else {
                    confirmText = "§7Click to go to page " + currentData.lastPage;
                }
            } catch (Exception ignored) {}
        }

        @Override
        protected void updateValues() {
            if(slots.isEmpty()) return;

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

            if(activeInv == currentData.lastPage - 1 && index == currentData.lastPage) {
                ScreenHandler currScreenHandler = McUtils.containerMenu();
                if (currScreenHandler == null) {
                    return true;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return true;
            } else if(index == currentData.lastPage) {
                if(PersonalStorageUtils == null) return true;

                activeInv = currentData.lastPage - 1;
                try {
                    BankOverlay.PersonalStorageUtils.jumpToDestination(activeInv + 1);
                } catch (Exception e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                    return true;
                }
                if(annotationCache.get(activeInv) != null) annotationCache.get(activeInv).clear();
                retryLoad();
            }

            return true;
        }

        public List<ItemStack> getItems() {
            return this.items;
        }

        public void setItems(List<ItemStack> items) {
            this.items = items;
        }
    }

    private static class SlotWidget extends Widget {
        protected ItemStack stack;
        int index;
        final boolean isInventorySlot;
        final int inventoryIndex;

        public SlotWidget(ItemStack stack, int index, boolean isInventorySlot, int inventoryIndex) {
            super(0, 0, 0, 0);
            this.stack = stack;
            this.index = index;
            this.isInventorySlot = isInventorySlot;
            this.inventoryIndex = inventoryIndex;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (inventoryIndex >= currentData.lastPage && !isInventorySlot) return;

            if(hovered && (isMouseInOverlay || isInventorySlot)) {
                ui.drawRect(x, y, width, height, CustomColor.fromHSV(0, 0, 1000, 0.25f));
            }

            if(stack == null) {
                renderSearchOverlay(ctx, stack, x + 1, y + 1);
                return;
            }

            if(hovered && (isMouseInOverlay || isInventorySlot)) {
                hoveredSlot = stack;
            }

            boolean renderOne = false;
            Optional<WynnItem> item = asWynnItem(stack);
            if (item.isPresent()) {
                ItemAnnotation annotation = item.get();
                if (annotation instanceof PotionItem potionItem) {
                    stack.setCount(potionItem.getUses().current());
                }
                if (annotation instanceof MultiHealthPotionItem potionItem) {
                    int current = potionItem.getUses().current();
                    if(current == 1) renderOne = true;
                    else stack.setCount(current);
                }
                if (annotation instanceof CraftedConsumableItem consumableItem) {
                    stack.setCount(consumableItem.getCount());
                }
            }

            try {
                if (stack.getCustomName() != null && stack.getCustomName().getString().contains("Potions")) {
                    Pattern pattern = Pattern.compile("\\[(\\d+)/(\\d+)]");
                    Matcher matcher = pattern.matcher(stack.getCustomName().getString());

                    if (matcher.find()) {
                        int remainingUses = Integer.parseInt(matcher.group(1));
                        stack.setCount(remainingUses);
                    }
                }
            } catch (Exception ignored) {}

            renderDurabilityRing(ctx, stack, x + 1, y + 1);
            renderEmeraldPouchRing(ctx, stack, x + 1, y + 1);
            renderHighlightOverlay(ctx, stack, x + 1, y + 1);

            ctx.drawItem(stack, (int) (1 + x / ui.getScaleFactor()), (int) (1 + y / ui.getScaleFactor()));
            try {
                if(stack.getCustomName() != null && stack.getCustomName().getString().contains("Ingredient Pouch")) ctx.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, (int) (1 + x / ui.getScaleFactor()), (int) (1 + y / ui.getScaleFactor()), renderOne ? "1" : stack.getCount() == 1 ? "" : String.valueOf(stack.getCount()));
                else if(stack.getCount() > 1) ui.drawText(String.valueOf(stack.getCount()), (int) (width + x / ui.getScaleFactor()), (int) (height - 8 + y / ui.getScaleFactor()), CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.RIGHT, VerticalAlignment.TOP, 1);
            } catch (Exception ignored) {}

            renderItemOverlays(ctx, stack, x + 1, y + 1);
            renderSearchOverlay(ctx, stack, x + 1, y + 1);

            try {
                if (FabricLoader.getInstance().isModLoaded("wynnmod")) {
                    initWynnmodOverlay();

                    if (wynnmodReady && itemOverlayInstance != null && stack != null) {
                        onRenderItemMethod.invoke(itemOverlayInstance, ctx, stack, x + 1, y + 1, false);
                    }
                }
            } catch (Throwable ignored) {}
        }

        public void setStack(ItemStack stack) {
            this.stack = stack;
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
                    String itemName = stack.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "").trim();
                    if (!itemName.isEmpty()) {
                        searchbar2.setInput(itemName);
                        for (PageWidget page : pages) page.lastInput = "";
                    }
                }
                return true;
            }
            if (isReloading) return false;
            if(shouldWait) return false;
            if(!isMouseInOverlay && !isInventorySlot) return false;
            if(inventoryIndex >= currentData.lastPage && !isInventorySlot) return false;

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

                MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, index + (isInventorySlot ? 54 : 0), button, action, MinecraftClient.getInstance().player);
                if(annotationCache.get(inventoryIndex) != null) annotationCache.get(inventoryIndex).clear();
                lastClickedSlot = new Pair<>(inventoryIndex, index);
            } else if(heldItem.isEmpty()) {
                List<ItemStack> stacks = BankOverlay.activeInvSlots.stream()
                        .map(Slot::getStack)
                        .collect(Collectors.toList());

                Pages.BankPages.put(activeInv, stacks);
                activeInv = inventoryIndex;
                try {
                    BankOverlay.PersonalStorageUtils.jumpToDestination(inventoryIndex + 1);
                } catch (Exception e) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Please enable the \"Personal Storage Utilities\" feature in Wynntils. Please create a bug report on discord if this still appears after you have enabled."));
                    return true;
                }
                if(annotationCache.get(inventoryIndex) != null) annotationCache.get(inventoryIndex).clear();
            }
            return true;
        }
    }

    public static class NameSignWidget extends Widget {
        public TextInputWidget textInputWidget;
        int index;

        public NameSignWidget(int index) {
            super(0, 0, 0, 0);
            this.index = index;
            textInputWidget = new TextInputWidget(x, y, width, height, 3, 1, 1);
            textInputWidget.setBackgroundColor(null);
            addChild(textInputWidget);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.disableScissor();
            ctx.enableScissor(scissorx1, scissory1 - 12, scissorx2, scissory2);
            ui.updateContext(ctx, ui.getScaleFactor(), 0, 0);

            drawDynamicNameSign(ctx, textInputWidget.getInput(), x, y + 12);

            String pageName = textInputWidget.getInput().isEmpty()
                    ? Pages.BankPageNames.getOrDefault(index, "Page " + (index + 1))
                    : textInputWidget.getInput();

            Pages.BankPageNames.put(index, pageName);

            textInputWidget.setTextColor((activeInv == index && !shouldWait) ? CustomColor.fromHexString("DEC800") : CustomColor.fromHexString("FFFFFF"));
            textInputWidget.setBounds(x, y, width, height);
            textInputWidget.setInput(pageName);
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
                    .map(Slot::getStack)
                    .collect(Collectors.toList());

            Pages.BankPages.put(activeInv, stacks);
            activeInv = 0;
            actualOffset = 0;
            targetOffset = 0;
            currentData.save();
            BankOverlay2.pages.clear();
            heldItem = Items.AIR.getDefaultStack();
            BankOverlay.activeInvSlots.clear();
            annotationCache.clear();
            Pages.save();

            if(currentOverlayType == BankOverlayType.CHARACTER) expectedOverlayType = BankOverlayType.ACCOUNT;
            else if(currentOverlayType == BankOverlayType.ACCOUNT) expectedOverlayType = BankOverlayType.CHARACTER;

            if(currScreenHandler == null) { return false; }
            clickOnSlot(47, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
            registeredScroll = false;
            return true;
        }
    }

    private static class AllCharactersButtonWidget extends Widget {
        public AllCharactersButtonWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 3, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            String text = allCharactersBrowseMode ? "Back" : "All Characters";
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.85f);
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
            ui.drawButton(x, y, width, height, 3, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            String text;
            if (isReloading) {
                text = "Reloading " + (reloadCurrentPage + 1) + "/" + reloadTotalPages;
            } else {
                text = "Reload";
            }
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.85f);
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
                    BankOverlay.PersonalStorageUtils.jumpToDestination(reloadOriginalPage + 1);
                } catch (Exception ignored) {}
                retryLoad();
                Pages.save();
            } else {
                // Start reload
                if (allCharactersBrowseMode) return false; // Can't reload while browsing all chars
                reloadOriginalPage = activeInv;
                reloadTotalPages = Math.max(currentData.lastPage, currentMaxPages);
                if (reloadTotalPages <= 0) return false;
                reloadCurrentPage = 0;
                isReloading = true;
                reloadPageLoaded = false;
                activeInv = 0;
                try {
                    BankOverlay.PersonalStorageUtils.jumpToDestination(1);
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
            ui.drawButton(x, y, width, height, 5, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            ui.drawCenteredText("Click to " + (WynnExtrasConfig.INSTANCE.toggleBankOverlay ? "disable" : "enable") + " the Bank Overlay", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
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
            ui.drawSliderBackground(x, y, width, height, 5, WynnExtrasConfig.INSTANCE.darkmodeToggle);

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
            public boolean isHold;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHold = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 5, hovered || isHold, WynnExtrasConfig.INSTANCE.darkmodeToggle);
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
            if (y > botBorder || y + height < topBorder) return;

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
            ui.drawText("§e@" + name + levelStr + " §7Page " + pageNumber, x + 2, y - 9, CustomColor.fromHexString("FFFF00"), 0.9f);

            if (items.isEmpty()) return;

            // Create slots if needed
            if (slots.isEmpty()) {
                int i = 0;
                for (ItemStack itemStack : items) {
                    CrossClassSlotWidget slot = new CrossClassSlotWidget(itemStack == null ? null : itemStack.copy(), i);
                    slots.add(slot);
                    addChild(slot);
                    i++;
                }
            }

            // Update slot items and positions
            updateValues();
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
            ui.drawText(hint, x + 2, y + height - 10, CustomColor.fromHexString("AAAAAA"), 0.7f);
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
                        BankOverlay.PersonalStorageUtils.jumpToDestination(pageNumber + 1);
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
                    targetCharacterIdForClassMenu = characterId;
                    targetCharacterNameForClassMenu = (characterNickname != null && !characterNickname.isEmpty())
                            ? characterNickname : null;
                    targetCharacterLevelForClassMenu = characterLevel;

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

                    // Queue /class after a short delay to let the screen close
                    julianh06.wynnextras.utils.TickScheduler.runAfterTicks(5, () -> {
                        Handlers.Command.queueCommand("class");
                    });
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void updateValues() {
            if (ui == null) return;
            if (slots.isEmpty()) return;

            int i = 0;
            for (SlotWidget slot : slots) {
                slot.setBounds(
                        (int) (x + 18 * (i % 9) * ui.getScaleFactor() + 1),
                        (int) (y + 18 * (i / 9) * ui.getScaleFactor() + 1),
                        (int) (18 * ui.getScaleFactor()),
                        (int) (18 * ui.getScaleFactor())
                );
                if (i < items.size()) {
                    slot.setStack(items.get(i));
                }
                i++;
            }
        }

        public String getCharacterId() {
            return characterId;
        }

        public int getPageNumber() {
            return pageNumber;
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
                    String itemName = stack.getName().getString().replaceAll("\u00a7[0-9a-fk-or]", "").trim();
                    if (!itemName.isEmpty()) {
                        searchbar2.setInput(itemName);
                        for (PageWidget page : pages) page.lastInput = "";
                    }
                }
                return true;
            }
            return false;
        }
    }

    private Object tooltipFactory;

    private Method getPriceTooltipMethod;
    private Method toClientComponentsMethod;
    private Method calculateCoordsMethod;
    private Method getScaleFactorMethod;
    private Method drawTooltipMethod;

    private Constructor<?> factoryCtor;
    private Constructor<?> builderCtor;
    private Class<?> fixedPositionerClass;

    private boolean wynnventoryReady = false;

    private void initWynnventoryReflection() {
        if (wynnventoryReady) return;

        try {
            Class<?> builderClass = Class.forName("com.wynnventory.feature.tooltip.price.PriceTooltipBuilder");
            Class<?> factoryClass = Class.forName("com.wynnventory.feature.tooltip.price.PriceTooltipFactory");

            builderCtor = builderClass.getConstructor();
            factoryCtor = factoryClass.getConstructor(builderClass);

            getPriceTooltipMethod = factoryClass.getMethod("getPriceTooltip", ItemStack.class);

            Class<?> renderUtils = Class.forName("com.wynnventory.util.RenderUtils");

            toClientComponentsMethod = renderUtils.getMethod(
                    "toClientComponents",
                    List.class,
                    Optional.class
            );

            calculateCoordsMethod = renderUtils.getMethod(
                    "calculateTooltipCoords",
                    int.class,
                    int.class,
                    List.class,
                    List.class
            );

            getScaleFactorMethod = renderUtils.getMethod(
                    "getScaleFactor",
                    List.class
            );

            fixedPositionerClass = Class.forName(
                    "com.wynnventory.util.RenderUtils$FixedTooltipPositioner"
            );

            wynnventoryReady = true;

        } catch (Throwable ignored) {}
    }

    private static boolean wynnmodReady = false;
    private static Method onRenderItemMethod;
    private static Object itemOverlayInstance;

    private static void initWynnmodOverlay() {
        if (wynnmodReady) return;

        try {
            Class<?> overlayClass =
                    Class.forName("com.wynnmod.feature.item.ItemOverlayFeature");

            Class<?> featureClass =
                    Class.forName("com.wynnmod.feature.Feature");

            Method getInstance =
                    featureClass.getMethod("getInstance", Class.class);

            itemOverlayInstance = getInstance.invoke(null, overlayClass);

            onRenderItemMethod = overlayClass.getDeclaredMethod(
                    "onRenderItemPost",
                    DrawContext.class,
                    ItemStack.class,
                    int.class,
                    int.class,
                    boolean.class
            );

            onRenderItemMethod.setAccessible(true);

            wynnmodReady = true;

        } catch (Throwable ignored) {}
    }
}