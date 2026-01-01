package julianh06.wynnextras.features.bankoverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynnmod.feature.Feature;
import com.wynnmod.feature.item.ItemOverlayFeature;
import com.wynnmod.util.wynncraft.item.map.WynncraftItemDatabase;
import com.wynntils.features.inventory.*;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.wynn.WynnUtils;
import com.wynnventory.config.ConfigManager;
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
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
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
import com.wynnventory.util.ItemStackUtils;
import com.wynnventory.util.PriceTooltipHelper;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.mixin.Accessor.*;
import julianh06.wynnextras.mixin.Invoker.*;
import julianh06.wynnextras.mixin.ItemFavoriteFeatureAccessor;
import julianh06.wynnextras.mixin.ItemGuessFeatureAccessor;
import julianh06.wynnextras.utils.Pair;
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
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;
import static com.wynntils.utils.wynn.ContainerUtils.shiftClickOnSlot;
import static julianh06.wynnextras.features.inventory.BankOverlay.*;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredStack;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredWynnitem;

public class BankOverlay2 extends WEHandledScreen {
    static ItemStack hoveredSlot = null;
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

    static int shownPages;

    private static boolean isMouseInOverlay = false;

    private static int scissorx1, scissory1, scissorx2, scissory2;

    private static long lastClickTime = 0;

    private static Pair<Integer, Integer> lastClickedSlot;

    public BankOverlay2(CallbackInfo ci, HandledScreen<?> screen) {
        this.ci = ci;
        this.screen = screen;
        actualOffset = 0;
        targetOffset = 0;
        pages.clear();
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

        try {
            if (FabricLoader.getInstance().isModLoaded("wynnmod")) {
                WynncraftItemDatabase.initialize();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Pages = currentData;
        if(MinecraftClient.getInstance().getWindow() == null || !MinecraftClient.getInstance().isRunning()) return;
        if(MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().currentScreen == null) return;

        if(ui == null) {
            ui = new UIUtils(context, 1, 0, 0);
        }

        Pair<Integer, Integer> xyRemain = calculateLayout();
        int xRemain = xyRemain.first();
        int yRemain = xyRemain.second();

        int xStart = xRemain / 2 - 2;
        int yStart = yRemain / 2 - 2;

        if(currentOverlayType != BankOverlayType.NONE && expectedOverlayType != BankOverlayType.NONE && currentOverlayType != expectedOverlayType) {
            RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
            drawBackgroundRect(context, xRemain, yRemain);
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackgroundDark : buttonBackgroundShortDark, xStart - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            } else {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackground : buttonBackgroundShort, xStart - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            }
            if(inventoryWidget != null) inventoryWidget.draw(context, mouseX, mouseY, delta, ui);
            if(quickActionWidget != null) quickActionWidget.draw(context, mouseX, mouseY, delta, ui);
            if(searchbar2 != null) searchbar2.draw(context, mouseX, mouseY, delta, ui);
            if(scrollBarWidget != null) scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);
            if(toggleOverlayWidget != null && SimpleConfig.getInstance(WynnExtrasConfig.class).bankQuickToggle) toggleOverlayWidget.draw(context, mouseX, mouseY, delta, ui);
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

            RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);

            float xPos = MinecraftClient.getInstance().currentScreen.width / 2f;
            float yPos = yStart + (yFitAmount) * (90 + 4 + 10) - 20;

            if (!SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) {
                Screen screen = McUtils.screen();
                if (!(screen instanceof HandledScreen<?> containerScreen)) return;
                yPos = ((HandledScreenAccessor) containerScreen).getY() + (4 + McUtils.containerMenu().slots.size() / 9f) * 16;
            }

            if(SimpleConfig.getInstance(WynnExtrasConfig.class).bankQuickToggle) {
                toggleOverlayWidget.setBounds((int) xPos - 70, (int) yPos, 140, 17);
                toggleOverlayWidget.draw(context, mouseX, mouseY, delta, ui);
            } else {
                toggleOverlayWidget.setBounds(0, 0, 0, 0);
            }
        }
//        else {
//            RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
//        } i dont remember why i added this but ill keep it here for now if i need it again

        if(currentOverlayType == BankOverlayType.NONE || MinecraftClient.getInstance() == null) return;

        initializeOverlayState();

        float snapValue = 0.5f;

        int totalRows = (int) Math.ceil((double) shownPages / xFitAmount);
        int c = (xFitAmount % 2 == 0 ? 1 : 0);
        int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * xFitAmount) - 52 * c);

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
        if(Math.abs(diff) < snapValue || !SimpleConfig.getInstance(WynnExtrasConfig.class).smoothScrollToggle) actualOffset = targetOffset;
        else actualOffset += diff * speed * delta;

        if(!SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) return;
        if(Pages == null) return;

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
                    }
                    setFocused(true);

                    cursorPos = input.length();
                    return true;
                }
            };
            rootWidgets.add(searchbar2);
        }

        if(quickActionWidget == null) {
            quickActionWidget = new QuickActionWidget();
        }

        if(scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget();
        }

        scrollBarWidget.setBounds(xStart + xFitAmount * 170, yStart - 13, 15, (yFitAmount - 1) * 104 + 12);
        scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);

        context.getMatrices().push();
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
            scissory1 = yStart;
            scissorx2 = xStart + 166 * xFitAmount;
            scissory2 = yStart + 100 * (yFitAmount - 1);

            context.enableScissor(scissorx1, scissory1, scissorx2, scissory2);
            for(PageWidget page : pages) {
                float invX = xStart + (visuali % xFitAmount) * (162 + 4);
                float invY = yStart + Math.floorDiv(visuali, xFitAmount) * (90 + 4 + 10) - actualOffset;
                page.setBounds((int) (invX * ui.getScaleFactor()), (int) (invY * ui.getScaleFactor()), (int) (164 * ui.getScaleFactor()), (int) (92 * ui.getScaleFactor()));
                page.setItems(buildInventoryForIndex(i, false));
                page.updateValues();

                if(searchbar2.getInput() != null && !searchbar2.getInput().isEmpty()) {
                    boolean containsSearch = searchbar2.getInput().equals(page.lastInput);
                    for(ItemStack stack : page.getItems()) {
                        if(stack == null) continue;
                        if(stack.getCustomName() == null) continue;

                        if (stack.getCustomName().getString().toLowerCase().contains(searchbar2.getInput().toLowerCase())) {
                            containsSearch = true;
                            page.lastInput = searchbar2.getInput();
                            break;
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
                    pageAmount++;
                }

                if(invY > yStart - 100 && invY < yStart + 103 * (yFitAmount - 1)) page.draw(context, mouseX, mouseY, delta, ui);
                i++;
                visuali++;
            }

            context.disableScissor();

            inventoryWidget.setBounds(xStart + 160, yStart + (yFitAmount - 1) * (90 + 4 + 10) - 3, (int) (176 * ui.getScaleFactor()), (int) (86 * ui.getScaleFactor()));
            inventoryWidget.setItems(buildInventoryForIndex(0, true));
            inventoryWidget.updateValues();
            inventoryWidget.draw(context, mouseX, mouseY, delta, ui);

            if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                switchButtonWidget.setBounds(xStart, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
                switchButtonWidget.draw(context, mouseX, mouseY, delta, ui);
            } else {
                switchButtonWidget.setBounds(0, 0, 0, 0);
            }

            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackgroundDark : buttonBackgroundShortDark, xStart - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            } else {
                ui.drawImage((currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) ? buttonBackground : buttonBackgroundShort, xStart - 8, yStart + (yFitAmount - 1) * (104) - 8, (int) (170 * ui.getScaleFactor()), (int) (91 * ui.getScaleFactor()));
            }

            if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                searchbar2.setBounds(xStart, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 59, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            } else {
                searchbar2.setBounds(xStart, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
            }

            searchbar2.setTextColor(CustomColor.fromHexString("FFFFFF"));
            searchbar2.setBackgroundColor(null);
            searchbar2.draw(context, mouseX, mouseY, delta, ui);

            if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                ui.drawCenteredText("Switch to " + (currentOverlayType == BankOverlayType.ACCOUNT ? "Character" : "Account") + " Bank", xStart + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, CustomColor.fromHexString("FFFFFF"), 1.1f);
            }
            if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
                ui.drawCenteredText("Quick Actions", xStart + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 44, CustomColor.fromHexString("FFFFFF"), 1.1f);
            } else {
                ui.drawCenteredText("Quick Actions", xStart + (77 * ui.getScaleFactorF()), yStart + (yFitAmount - 1) * (104) + 14, CustomColor.fromHexString("FFFFFF"), 1.1f);
            }
        }

        shownPages = pageAmount;

        renderHoveredSlotHighlight(context,  screen);
        renderHoveredTooltip(context, screen, mouseX, mouseY);
        renderHeldItemOverlay(context, mouseX, mouseY);

        if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
            quickActionWidget.setBounds(xStart, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 31, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
        } else {
            quickActionWidget.setBounds(xStart, yStart + (yFitAmount - 1) * (90 + 4 + 10) + 3, (int) (155 * ui.getScaleFactor()), (int) (23 * ui.getScaleFactor()));
        }

        quickActionWidget.draw(context, mouseX, mouseY, delta, ui);

        drawEmeraldOverlay(context, xStart - 36, yStart - 14);
    }

    private void drawBackgroundRect(DrawContext context, float xRemain, float yRemain) {
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            RenderUtils.drawRect(
                    context.getMatrices(),
                    CustomColor.fromHexString("2c2d2f"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15, 1000,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context.getMatrices(),
                    CustomColor.fromHexString("1b1b1c"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 0, 1
            );
        } else {
            RenderUtils.drawRect(
                    context.getMatrices(),
                    CustomColor.fromHexString("81644b"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15, 1000,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context.getMatrices(),
                    CustomColor.fromHexString("4f342c"),
                    xRemain / 2 - 2 - 7, yRemain / 2 - 15,
                    xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 0, 1
            );
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if(toggleOverlayWidget != null && SimpleConfig.getInstance(WynnExtrasConfig.class).bankQuickToggle) toggleOverlayWidget.mouseClicked(x, y, button);

        if (!SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) return false;
        if (currentOverlayType == BankOverlayType.NONE) return false;

        for(PageWidget page : pages) {
            page.mouseClicked(x, y, button);
        }
        if(inventoryWidget != null) inventoryWidget.mouseClicked(x, y, button);
        if(switchButtonWidget != null) switchButtonWidget.mouseClicked(x, y, button);
        if(quickActionWidget != null) quickActionWidget.mouseClicked(x, y, button);
        if(searchbar2 != null) searchbar2.mouseClicked(x, y, button);
        if(scrollBarWidget != null) scrollBarWidget.mouseClicked(x, y, button);
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

        xFitAmount = Math.min(3, Math.floorDiv(screenWidth - 84, 162));
        yFitAmount = Math.min(4, Math.floorDiv(screenHeight, 104));

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

    private List<ItemStack> buildInventoryForIndex(int index, boolean isPlayerInv) throws IndexOutOfBoundsException {
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
                        throw e;
                    }
                    if(rightArrow == null) return new ArrayList<>();
                    if(rightArrow.getItem() == Items.POTION) {
                        String rawText = rightArrow.getName().getString();
                        String cleanedText = rawText.replaceAll("§[0-9a-fk-or]", "");
                        if (!cleanedText.contains("Page " + (activeInv + 2))) {
                            shouldWait = true;
                        } else if (oldShouldWait) {
                            Pages.BankPages.put(activeInv, slots.stream().map(Slot::getStack).toList());
                            if(annotationCache.get(activeInv) != null) annotationCache.get(activeInv).clear();
                        }
                    } else if(activeInv != currentData.lastPage - 1) {
                        shouldWait = true;
                    }
                }

                if (shouldWait) {
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

            RenderSystem.enableDepthTest();
            RenderUtils.drawArc(context.getMatrices(), color, x, y, 100.0F, fraction, 6, 8);
            RenderSystem.disableDepthTest();
        });
    }

    private static void renderEmeraldPouchRing(DrawContext context, ItemStack stack, int x, int y) {
        Models.Item.asWynnItem(stack, EmeraldPouchItem.class).ifPresent(pouch -> {
            CappedValue capacity = new CappedValue(pouch.getValue(), pouch.getCapacity());
            float fraction = (float) capacity.current() / capacity.max();
            int colorInt = MathHelper.hsvToRgb((1.0F - fraction) / 3.0F, 1.0F, 1.0F);
            CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

            RenderSystem.enableDepthTest();
            RenderUtils.drawArc(context.getMatrices(), color, x - 2, y - 2, 100.0F, Math.min(1.0F, fraction), 8, 10);
            RenderSystem.disableDepthTest();
        });
    }

    private static void renderHighlightOverlay(DrawContext context, ItemStack stack, int x, int y) {
        if(stack.getItem() == Items.AIR) return;
        if (itemHighlightFeature == null) itemHighlightFeature = Managers.Feature.getFeatureInstance(ItemHighlightFeature.class);

        CustomColor color = ((ItemHighlightFeatureInvoker) itemHighlightFeature).invokeGetHighlightColor(stack, false);
        if (!Objects.equals(color, CustomColor.NONE)) {
            try {
                RenderUtils.drawTexturedRectWithColor(
                        context.getMatrices(),
                        Texture.HIGHLIGHT.resource(),
                        color.withAlpha(SimpleConfig.getInstance(WynnExtrasConfig.class).wynntilsItemRarityBackgroundAlpha),
                        x - 1, y - 1, 100, 18, 18,
                        ((ItemHighlightFeature.HighlightTexture) itemHighlightFeature.getConfigOptionFromString("highlightTexture").get().get()).ordinal() * 18 + 18, 0,
                        18, 18,
                        Texture.HIGHLIGHT.width(),
                        Texture.HIGHLIGHT.height()
                );
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
                    annotation instanceof HorseItem ||
                    annotation instanceof PowderItem ||
                    annotation instanceof PotionItem ||
                    annotation instanceof CrafterBagItem) {

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                ((ItemTextOverlayFeatureMixin) Managers.Feature.getFeatureInstance(ItemTextOverlayFeature.class)).invokeDrawTextOverlay(context.getMatrices(), stack, x, y, false);
                context.getMatrices().pop();
            }

            ((UnidentifiedItemIconFeatureInvoker) Managers.Feature.getFeatureInstance(UnidentifiedItemIconFeature.class)).invokeDrawIcon(context.getMatrices(), stack, x, y, 100);
            if(((ItemFavoriteFeatureAccessor) Managers.Feature.getFeatureInstance(ItemFavoriteFeature.class)).callIsFavorited(stack)) {
                BufferedRenderUtils.drawScalingTexturedRect(
                        context.getMatrices(),
                        ((DrawContextAccessor) context).getVertexConsumers(),
                        Texture.FAVORITE_ICON.resource(),
                        x + 10,
                        y,
                        200,
                        9,
                        9,
                        Texture.FAVORITE_ICON.width(),
                        Texture.FAVORITE_ICON.height());
            }
        }
    }

    private static void renderSearchOverlay(DrawContext context, ItemStack stack, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 250.0F);

        String input = searchbar2.getInput().toLowerCase();
        if(stack == null) {
            if(!input.isEmpty()) {
                RenderUtils.drawRect(context.getMatrices(), CustomColor.fromHSV(0, 0, 0, 0.75f), x - 1, y - 1, 0, 18, 18);
            }
            context.getMatrices().pop();
            return;
        }
        if (stack.getCustomName() != null && !input.isEmpty()) {
            if (stack.getCustomName().getString().toLowerCase().contains(input)) {
                RenderUtils.drawRectBorders(context.getMatrices(), CustomColor.fromHexString("008000"), x, y, x + 16, y + 16, 0, 1);
            } else {
                RenderUtils.drawRect(context.getMatrices(), CustomColor.fromHSV(0, 0, 0, 0.75f), x - 1, y - 1, 0, 18, 18);
            }
        } else if (!input.isEmpty() && stack.getItem().equals(Items.AIR)) {
            RenderUtils.drawRect(context.getMatrices(), CustomColor.fromHSV(0, 0, 0, 0.75f), x - 1, y - 1, 0, 18, 18);
        }

        context.getMatrices().pop();
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

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        if(!overflow) {
            context.drawTooltip(screen.getTextRenderer(), tooltip, (int) (mouseX / scale), y);
        } else {
            drawTooltip(screen.getTextRenderer(), components, (int)(mouseX / scale) + 12, y, context);
        }

        context.getMatrices().pop();

        try {
            if (FabricLoader.getInstance().isModLoaded("wynnventory")) {
                //Slightly modified version of the wynnventory price overlay render
                ConfigManager config = ConfigManager.getInstance();

                ItemStack stack = hoveredSlot;

                // Screen independent actions
                if (config.isShowTooltips()) {
                    Models.Item.getWynnItem(stack)
                            .ifPresent(wynnItem -> renderPriceTooltip(context, mouseX, mouseY, stack));
                }
            }
        } catch (Exception ignored) {}
    }

    private void renderPriceTooltip(DrawContext guiGraphics, int x, int y, ItemStack stack) {
        ConfigManager config = ConfigManager.getInstance();
        List<Text> tooltips = ItemStackUtils.getTooltips(stack);
        PriceTooltipHelper.renderPriceInfoTooltip(
                guiGraphics, x, y, stack, tooltips, config.isAnchorTooltips()
        );
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
            context.getMatrices().push();
            TooltipBackgroundRenderer.render(context, x, y, i, j, 400, null);
            context.getMatrices().translate(0.0F, 0.0F, 800.0F);

            int q = y;

            int r;
            TooltipComponent tooltipComponent2;
            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = components.get(r);
                tooltipComponent2.drawText(textRenderer, x, q, context.getMatrices().peek().getPositionMatrix(), ((DrawContextAccessor) context).getVertexConsumers());
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }

            q = y;

            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = components.get(r);
                tooltipComponent2.drawItems(textRenderer, x, q, l, m, context);
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }

            context.getMatrices().pop();
        }
    }

    private void renderHeldItemOverlay(DrawContext context, int mouseX, int mouseY) {
        if (heldItem == null) return;

        int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue() + 1;
        String amountString = heldItem.getCount() == 1 ? "" : String.valueOf(heldItem.getCount());

        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300);
        context.drawItem(heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale, amountString);
        context.getMatrices().pop();
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
        if (signMids.isEmpty()) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
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
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            RenderUtils.drawTexturedRect(context.getMatrices(), signLeftDark, x, y - 15, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), signLeft, x, y - 15, 10, 15, 10, 15);
        }
        if (strWidth > 15) {
            for (int i = 0; i < amount; i++) {
                RenderUtils.drawTexturedRect(context.getMatrices(), signMids.get(i % 3), x + 10 + 10 * i, y - 15, 10, 15, 10, 15);
            }
        }
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            RenderUtils.drawTexturedRect(context.getMatrices(), signRightDark, x + 10 + 10 * amount, y - 15, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), signRight, x + 10 + 10 * amount, y - 15, 10, 15, 10, 15);
        }
    }

    void drawEmeraldOverlay(DrawContext context, int x, int y) {
        InventoryEmeraldCountFeature emeraldCountFeature = Managers.Feature.getFeatureInstance(InventoryEmeraldCountFeature.class);
        int emeraldAmountInt = Models.Emerald.getAmountInContainer();
        String[] emeraldAmounts = ((InventoryEmeraldCountFeatureInvoker) emeraldCountFeature).invokeGetRenderableEmeraldAmounts(emeraldAmountInt);

        y += (3 * 28);

        MatrixStack poseStack = context.getMatrices();

        for (int i = emeraldAmounts.length - 1; i >= 0; i--) {
            String emeraldAmount = emeraldAmounts[i];

            if (emeraldAmount.equals("0")) continue;

            RenderUtils.drawTexturedRect(
                    poseStack,
                    Texture.EMERALD_COUNT_BACKGROUND.resource(),
                    x,
                    y - (i * 28),
                    0,
                    28,
                    28,
                    0,
                    0,
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height(),
                    Texture.EMERALD_COUNT_BACKGROUND.width(),
                    Texture.EMERALD_COUNT_BACKGROUND.height());

            poseStack.push();
            poseStack.translate(0, 0, 200);
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
                            poseStack,
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
            poseStack.pop();
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

            ui.drawImage(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? invTextureDark : invTexture, x, y - 0.2f, width, height);

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

            ui.drawImage(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? bankTextureDark : bankTexture, x, y, width, height);

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

                    ui.drawImage(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? lock_unlocked_dark : lock_unlocked, x + 82 - 25, y + 46 - 19, 50, 50);
                    ui.drawCenteredText(buyText, x + 81, y + 80, CustomColor.fromHexString("FFFFFF"), 1);
                } else {
                    ui.drawImage(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? lock_locked_dark : lock_locked, x + 82 - 25, y + 46 - 19, 50, 50);
                }
            }

            if(index >= currentData.lastPage) return;

            if(hovered && isMouseInOverlay) {
                if(index != activeInv) {
                    ui.drawRect(x, y, width, height, CustomColor.fromHSV(0, 0, 1000, 0.25f));
                }
            }

            if(activeInv == index) {
                CustomColor color = (!shouldWait)
                        ? CustomColor.fromHexString("FFFF00")
                        : CustomColor.fromHexString("FFFFFF");
                ui.drawRectBorders(x, y + 0.5f, x + 164, y + 92, color);
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
                        currentData.lastPage = activeInv + 1;
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
                    } else if (rightArrow.getCustomName().getString().contains(String.valueOf(currentData.lastPage + 1)) && activeInv == currentData.lastPage - 1) {
                        currentData.lastPage++;
                        pageBuyCustomModelData = 0;
                        priceText = null;
                        retryLoad();
                    }
                } else {
                    confirmText = "§7Click to go to page " + currentData.lastPage;
                }
            } catch (Exception ignored) {
            }
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
            if(activeInv == currentData.lastPage - 1) {
                ScreenHandler currScreenHandler = McUtils.containerMenu();
                if (currScreenHandler == null) {
                    return true;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return true;
            } else if(index == currentData.lastPage) {
                if(PersonalStorageUtils == null) return true;

                activeInv = currentData.lastPage - 1;
                BankOverlay.PersonalStorageUtils.jumpToDestination(activeInv + 1);
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
        private ItemStack stack;
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

            renderDurabilityRing(ctx, stack, x + 1, y + 1);
            renderEmeraldPouchRing(ctx, stack, x + 1, y + 1);
            renderHighlightOverlay(ctx, stack, x + 1, y + 1);

            ctx.drawItem(stack, (int) (1 + x / ui.getScaleFactor()), (int) (1 + y / ui.getScaleFactor()));
            ctx.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, (int) (1 + x / ui.getScaleFactor()), (int) (1 + y / ui.getScaleFactor()), stack.getCount() == 1 ? "" : String.valueOf(stack.getCount()));

            renderItemOverlays(ctx, stack, x + 1, y + 1);
            renderSearchOverlay(ctx, stack, x + 1, y + 1);

            try {
                if (FabricLoader.getInstance().isModLoaded("wynnmod")) {
                    ItemOverlayFeature itemOverlayFeature = Feature.getInstance(ItemOverlayFeature.class);
                    ((wmd$ItemOverlayFeatureInvoker) itemOverlayFeature).callOnRenderItem(ctx, stack, x, y, false);
                }
            } catch (Exception ignored) {}
        }

        public void setStack(ItemStack stack) {
            this.stack = stack;
        }

        private SlotActionType determineActionType(int mouseButton) {
            SlotActionType actionType = SlotActionType.PICKUP;

            if(mouseButton == 1) return actionType;

            long now = System.currentTimeMillis();
            if (heldItem != null && heldItem.getItem() != Items.AIR) {
                if (now - lastClickTime < 250 && (lastClickedSlot.equals(new Pair<>(inventoryIndex, index)))) {
                    actionType = SlotActionType.PICKUP_ALL;
                }
            }
            lastClickTime = now;

            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                actionType = SlotActionType.QUICK_MOVE;
            }

            return actionType;
        }

        @Override
        protected boolean onClick(int button) {
            if(shouldWait) return false;
            if(!isMouseInOverlay && !isInventorySlot) return false;
            if(inventoryIndex >= currentData.lastPage && !isInventorySlot) return false;

            if(activeInv == inventoryIndex || isInventorySlot) {
                if(index == 4 && isInventorySlot) return false; //Ingredient pouch, clicking it within the bank overlay crashes the game

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
            } else {
                List<ItemStack> stacks = BankOverlay.activeInvSlots.stream()
                        .map(Slot::getStack)
                        .collect(Collectors.toList());

                Pages.BankPages.put(activeInv, stacks);
                activeInv = inventoryIndex;
                BankOverlay.PersonalStorageUtils.jumpToDestination(inventoryIndex + 1);
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

            drawDynamicNameSign(ctx, textInputWidget.getInput(), x, y + 12);

            String pageName = textInputWidget.getInput().isEmpty()
                    ? Pages.BankPageNames.getOrDefault(index, "Page " + (index + 1))
                    : textInputWidget.getInput();

            Pages.BankPageNames.put(index, pageName);

            textInputWidget.setTextColor((activeInv == index && !shouldWait) ? CustomColor.fromHexString("FFEA00") : CustomColor.fromHexString("FFFFFF"));
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
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            ScreenHandler currScreenHandler = McUtils.containerMenu();
            if(currScreenHandler == null) { return false; }
            if(InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
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

            if(currentOverlayType == BankOverlayType.CHARACTER) expectedOverlayType = BankOverlayType.ACCOUNT;
            else if (currentOverlayType == BankOverlayType.ACCOUNT) expectedOverlayType = BankOverlayType.CHARACTER;

            if(currScreenHandler == null) { return false; }
            clickOnSlot(47, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
            return true;
        }
    }

    private static class ToggleOverlayWidget extends Widget {
        public ToggleOverlayWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 5, hovered, SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle);
            ui.drawCenteredText("Click to " + ((SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay) ? "disable" : "enable") + " the Bank Overlay", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 0.75f);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay = !SimpleConfig.getInstance(WynnExtrasConfig.class).toggleBankOverlay;
            SimpleConfig.save(WynnExtrasConfig.class);
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
            ui.drawSliderBackground(x, y, width, height, 5, SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle);

            int totalRows = (int) Math.ceil((double) shownPages / xFitAmount);
            int c = (xFitAmount % 2 == 0 ? 1 : 0);
            int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * xFitAmount) - 52 * c);
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
            int maxOffset = Math.max(0, (totalRows - yFitAmount + c + 1) * (260 - 52 * xFitAmount) - 52 * c);
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
                ui.drawButton(x, y, width, height, 5, hovered || isHold, SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle);
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
}