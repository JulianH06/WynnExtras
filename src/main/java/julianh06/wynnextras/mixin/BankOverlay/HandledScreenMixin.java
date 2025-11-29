package julianh06.wynnextras.mixin.BankOverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.inventory.*;
import com.wynntils.features.tooltips.TooltipFittingFeature;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.TooltipUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Time;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.InventoryKeyPressEvent;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.inventory.BankOverlayButtons.*;
import julianh06.wynnextras.mixin.Accessor.*;
import julianh06.wynnextras.mixin.Invoker.*;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static julianh06.wynnextras.features.inventory.BankOverlay.*;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredStack;
import static julianh06.wynnextras.features.inventory.WeightDisplay.currentHoveredWynnitem;
import static julianh06.wynnextras.features.misc.CustomClassSelection.inClassSelection;

@WEModule
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow public abstract void close();

    @Unique
    ItemStack hoveredSlot = null;
    @Unique
    int hoveredX = -1;
    @Unique
    int hoveredY = -1;
    @Unique
    int hoveredIndex = -1;
    @Unique
    int hoveredInvIndex = -1;

    @Unique
    public final Config<ItemHighlightFeature.HighlightTexture> highlightTexture = new Config<>(ItemHighlightFeature.HighlightTexture.CIRCLE_TRANSPARENT);

    @Unique
    ItemStack buyPageStack = null;

    @Unique
    CharacterBankButton personalStorageButton = new CharacterBankButton(-1000, -1000, 13, 162 + 4, "Switch to Character Bank");

    @Unique
    QuickStashButton quickStashButton = new QuickStashButton(-1000, -1000, 13, 162 + 4, "Quick stash");

    @Unique
    DumpExceptHotbarButton dumpExceptHotbarButton = new DumpExceptHotbarButton(-1000, -1000, 13, 162 + 4, "Dump except Hotbar");

    @Unique
    DumpAllButton dumpAllButton = new DumpAllButton(-1000, -1000, 13, 162 + 4, "Dump all");

    @Unique
    EasyTextInput Searchbar = new EasyTextInput(-1000, -1000, 13, 162 + 4);

    @Unique
    ResetSearchButton resetSearchButton = new ResetSearchButton(-1000, -1000, 13, 162 + 4, Searchbar, "Clear Search Bar");

    @Unique
    ItemHighlightFeature itemHighlightFeature;

    @Unique
    int lastPage = currentMaxPages;

    @Unique
    Identifier ButtonTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/button.png");

    @Unique
    Identifier ButtonTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/button_dark.png");

    @Unique
    Identifier invTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/inv.png");

    @Unique
    Identifier invTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/inv_dark.png");

    @Unique
    Identifier bankTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank.png");

    @Unique
    Identifier bankTextureDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank_dark.png");

    @Unique
    Identifier signLeft = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_left.png");

    @Unique
    Identifier signLeftDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_left_dark.png");

    @Unique
    Identifier signRight = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_right.png");

    @Unique
    Identifier signRightDark = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_right_dark.png");

    @Unique
    Identifier signMid1 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m1.png");

    @Unique
    Identifier signMid1D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m1_dark.png");

    @Unique
    Identifier signMid2 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m2.png");

    @Unique
    Identifier signMid2D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m2_dark.png");

    @Unique
    Identifier signMid3 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m3.png");

    @Unique
    Identifier signMid3D = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m3_dark.png");

    @Unique
    Identifier lock_locked = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_locked.png");

    @Unique
    Identifier lock_unlocked = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_unlocked.png");

    @Unique
    Identifier lock_locked_dark = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_locked_dark.png");

    @Unique
    Identifier lock_unlocked_dark = Identifier.of("wynnextras", "textures/gui/bankoverlay/lock_unlocked_dark.png");

    @Unique
    List<Identifier> signMids = new ArrayList<>();

    @Unique
    int visibleInventories;

    @Unique
    String priceText;

    @Unique
    String confirmText = "";


    @Unique
    private final EnumSet<BankOverlayType> initializedTypes = EnumSet.noneOf(BankOverlayType.class);


    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
//        if(inClassSelection) {
//            ci.cancel();
//            return;
//        }

        Pages = currentData;
        if (currentOverlayType == BankOverlayType.NONE || MinecraftClient.getInstance() == null || Pages == null) return;
        if (MinecraftClient.getInstance().getWindow() == null || !MinecraftClient.getInstance().isRunning()) return;
        if(MinecraftClient.getInstance().player == null || MinecraftClient.getInstance().currentScreen == null) return;
        initializeOverlayState();

        Pair<Integer, Integer> xyRemain = calculateLayout();
        int xRemain = xyRemain.first();
        int yRemain = xyRemain.second();
        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount;

        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
        int xStart = xRemain / 2 - 2;
        int yStart = yRemain / 2 - 2;

        context.getMatrices().push();
        ci.cancel();

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (WynnExtras.testInv == null) {
            WynnExtras.testInv = screen.getScreenHandler().slots;
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            RenderUtils.drawRect(
                    context.getMatrices(),
                    CustomColor.fromHexString("2c2d2f"),
                    (float) xRemain / 2 - 2 - 7, (float) yRemain / 2 - 15, 1000,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context.getMatrices(),
                    CustomColor.fromHexString("1b1b1c"),
                    (float) xRemain / 2 - 2 - 7, (float) yRemain / 2 - 15,
                    (float) xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, (float) yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 0, 1
            );
        } else {
            RenderUtils.drawRect(
                    context.getMatrices(),
                    CustomColor.fromHexString("81644b"),
                    (float) xRemain / 2 - 2 - 7, (float) yRemain / 2 - 15, 1000,
                    xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
            );
            RenderUtils.drawRectBorders(
                    context.getMatrices(),
                    CustomColor.fromHexString("4f342c"),
                    (float) xRemain / 2 - 2 - 7, (float) yRemain / 2 - 15,
                    (float) xRemain / 2 - 2 - 7 + xFitAmount * (162 + 4) + 11, (float) yRemain / 2 - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10, 0, 1
            );
        }

        //System.out.println(lastPage);

        for (int indexWithOffset = scrollOffset; indexWithOffset < visibleInventories; indexWithOffset++) {
            boolean pageContainsSearch = false;
            boolean isUnlocked = indexWithOffset < currentData.lastPage; // < instead of <= because the index starts at 0 and the pages at 1
            if (indexWithOffset - scrollOffset == playerInvIndex) {
                isUnlocked = true;
            } else if (indexWithOffset > currentMaxPages - 1) {
                continue;
            }

            canScrollFurther = (visibleInventories) < currentMaxPages;
            int i = indexWithOffset - scrollOffset;

            if(i == 0) {
                int x = xRemain / 2 - 10;
                int y = yRemain / 2 - 10;
                drawEmeraldOverlay(context, x - 28, y - 5);
            }

            List<ItemStack> inv;
            try {
                inv = buildInventoryForIndex(indexWithOffset, playerInvIndex);
            } catch (IndexOutOfBoundsException e) {
                close();
                return;
            }

            try {
                if (McUtils.containerMenu() != null && indexWithOffset == activeInv + 1 && !shouldWait && (expectedOverlayType == BankOverlayType.NONE || currentOverlayType == expectedOverlayType)) {
                    ItemStack rightArrow = McUtils.containerMenu().getSlot(52).getStack();
                    List<Text> lore = rightArrow.getComponents().get(DataComponentTypes.LORE).lines();
                    //System.out.println(lore);
                    //System.out.println(rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0));
                    if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(284.0f)) {
                        //System.out.println("RED ARROW");
                        currentData.lastPage = activeInv + 1;
                        for (Text text : lore) {
                            if (text.getString().contains("§7Price")) {
                                priceText = text.getString();
                                confirmText = "";
                                //System.out.println(text.getString() + (text.getString().contains("✖") ? " CANNOT AFFORD" : " CAN AFFORD"));
                                break;
                            }
                        }
                    } else if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(283.0f)) {
                        //System.out.println("GREEN ARROW");
                        confirmText = "§7Click again to confirm.";
                    } else if (rightArrow.getComponents().get(DataComponentTypes.CUSTOM_MODEL_DATA).getFloat(0).equals(281.0f) && rightArrow.getCustomName().getString().contains(String.valueOf(currentData.lastPage + 1)) && activeInv == currentData.lastPage - 1) {
                        //System.out.println(rightArrow.getCustomName().getString());
                        //System.out.println("BOUGHT" + " ACITVE PAGE " + activeInv + " LAST PAGE" + currentData.lastPage);
                        currentData.lastPage++;
                        //PersonalStorageUtilitiesFeatureAccessor accessor = (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.PersonalStorageUtils;
                        //accessor.setLastPage(lastPage);
                        priceText = null;
                        //McUtils.sendMessageToClient(Text.of("bought new page"));
                        retryLoad();
                    }
                } else {
                    confirmText = "§7Click to go to page " + currentData.lastPage;
                }
            } catch (Exception ignored) {
            }

            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(indexWithOffset, k -> new ArrayList<>(Collections.nCopies(inv.size(), null)));

            if(McUtils.containerMenu() != null && indexWithOffset == currentData.lastPage && i != playerInvIndex) {
                int textX = xRemain / 2 + (i % xFitAmount) * (162 + 4);
                int textY = yRemain / 2 + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);
                TextRenderer textRenderer = McUtils.mc().textRenderer;
                if(priceText == null) {
                    String text = "§c✖ §7Price: §funknown.";
                    String text2 = "§7Go to page §f" + currentData.lastPage + " §7to check.";
                    context.drawTextWithShadow(textRenderer, text, textX + 81 - textRenderer.getWidth(text) / 2, textY + 5, CustomColor.fromHexString("FFFFFF").asInt());
                    context.drawTextWithShadow(textRenderer, text2, textX + 81 - textRenderer.getWidth(text2) / 2, textY + 15, CustomColor.fromHexString("FFFFFF").asInt());
                } else {
                    context.drawTextWithShadow(textRenderer, priceText, textX + 81 - textRenderer.getWidth(priceText) / 2, textY + 10, CustomColor.fromHexString("FFFFFF").asInt());
                }

                int playerXStart = xStart + (i % xFitAmount) * (162 + 4);
                int playerYStart = yStart + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);

                boolean hovered =
                        mouseX >= playerXStart &&
                                mouseX < playerXStart + 162 &&
                                mouseY >= playerYStart &&
                                mouseY < playerYStart + 92;

                if (hovered) {
                    hoveredInvIndex = indexWithOffset;

                    if(hoveredInvIndex != activeInv && Searchbar.getInput().isEmpty()) {
                        RenderUtils.drawRect(context.getMatrices(), CustomColor.fromHSV(0, 0, 0, 0.25f), playerXStart, playerYStart, 0, 164, 92);
                    }

                    String buyText = confirmText.equals("") ? "§7Click to buy." : confirmText;

                    RenderUtils.drawTexturedRect(context.getMatrices(), SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? lock_unlocked_dark : lock_unlocked, playerXStart + 82 - 25, playerYStart + 46 - 19, 50, 50, 50, 50);
                    context.drawTextWithShadow(textRenderer, buyText, textX + 81 - textRenderer.getWidth(buyText) / 2, playerYStart + 75, CustomColor.fromHexString("FFFFFF").asInt());
                } else {
                    RenderUtils.drawTexturedRect(context.getMatrices(), SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle ? lock_locked_dark : lock_locked, playerXStart + 82 - 25, playerYStart + 46 - 19, 50, 50, 50, 50);
                }
            }

            if(inv == null) return;
            int stackIndex = 0;
            for (ItemStack stack : inv) {
                if(i != playerInvIndex) {
                    if(!isUnlocked) break;
                    if (stack == null) stack = new ItemStack(Items.AIR);
                    applyAnnotation(stack, annotations, stackIndex);
                }

                int x = xRemain / 2 + 18 * (stackIndex % 9) + (i % xFitAmount) * (162 + 4);
                int y = yRemain / 2 + 18 * Math.floorDiv(stackIndex, 9) + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);

                if (i == playerInvIndex) {
                    if (stackIndex == 0) {
                        positionButtons(x, y);
                        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                            RenderUtils.drawTexturedRect(context.getMatrices(), invTextureDark, x + 162 + 4 - 8, y - 12 + 7, 176, 86, 176, 86);
                        } else {
                            RenderUtils.drawTexturedRect(context.getMatrices(), invTexture, x + 162 + 4 - 8, y - 12 + 7, 176, 86, 176, 86);
                        }
                    }

                    x += 162 + 4;
                    y -= 3;
                    if (stackIndex > 26) {
                        y += 5;
                    }
                }
                if (i != playerInvIndex && stackIndex == 0) {
                    int playerXStart = xStart + (i % xFitAmount) * (162 + 4);
                    int playerYStart = yStart + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);

                    if (i == 0) {
                    }

                    if(SimpleConfig.getConfigHolder(WynnExtrasConfig.class).get().darkmodeToggle) {
                        RenderUtils.drawTexturedRect(context.getMatrices(), bankTextureDark, playerXStart, playerYStart, 164, 92, 164, 92);
                    } else {
                        RenderUtils.drawTexturedRect(context.getMatrices(), bankTexture, playerXStart, playerYStart, 164, 92, 164, 92);
                    }

                    boolean hovered =
                            mouseX >= playerXStart &&
                                    mouseX < playerXStart + 162 &&
                                    mouseY >= playerYStart &&
                                    mouseY < playerYStart + 92;

                    if(hovered) {
                        hoveredInvIndex = indexWithOffset;
                        if(stackIndex == 0 && hoveredInvIndex != activeInv && Searchbar.getInput().isEmpty()) {
                            RenderUtils.drawRect(
                                    context.getMatrices(),
                                    CustomColor.fromHSV(0, 0, 1000, 0.25f),
                                    playerXStart, playerYStart, 0, 164, 92
                            );
                        }
                    }
                }

                renderDurabilityRing(context, stack, x, y);
                renderEmeraldPouchRing(context, stack, x, y);
                renderHighlightOverlay(context, stack, x, y);

                boolean hovered =
                        mouseX >= x - 1 &&
                                mouseX < x + 17 &&
                                mouseY >= y - 1 &&
                                mouseY < y + 17;

                if (hovered) {
                    hoveredSlot = stack;
                    hoveredX = x;
                    hoveredY = y;
                    hoveredIndex = stackIndex;
                    hoveredInvIndex = indexWithOffset;
                }

                @Nullable String amountString = null;
                if (stack.getCount() != 1) {
                    amountString = String.valueOf(stack.getCount());
                }
                ((HandledScreenInvoker) screen).invokeDrawItem(context, stack, x, y, amountString);

                renderItemOverlays(context, stack, x, y);
                renderSearchOverlay(context, stack, x, y);

                stackIndex++;
            }

            int playerInvOffset = i == playerInvIndex ? (162 + 4) : 0;
            int playerXStart = xStart + (i % xFitAmount) * (162 + 4) + playerInvOffset;
            int playerYStart = yStart + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);
            int color;
            if (indexWithOffset == activeInv && !shouldWait) {
                color = CustomColor.fromHexString("FFEA00").asInt();
            } else if (!pageContainsSearch && !Searchbar.getInput().isEmpty()) {
                color = CustomColor.fromHexString("808080").asInt();
            } else if (pageContainsSearch && indexWithOffset != activeInv) {
                color = CustomColor.fromHexString("008000").asInt();
            } else if (!isUnlocked && i != playerInvIndex) {
                color = CustomColor.fromHexString("FF0000").asInt();
                //context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, buyPageStageText/* + " Buying pages"*/, playerXStart + 20, playerYStart + 50, color);
//                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "is currently not implemented.", playerXStart, playerYStart + 30, color);
//                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "To buy pages disable the", playerXStart, playerYStart + 50, color);
//                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, "menu in the config (/we config).", playerXStart, playerYStart + 70, color);
            } else {
                color = CustomColor.fromHexString("FFFFFF").asInt();
            }
            if (i != playerInvIndex) {
                renderPageOverlay(context, indexWithOffset, i, playerInvIndex, playerXStart, playerYStart);
            }
        }

        renderButtons(context);
        renderNameInputs(context);
        renderHoveredSlotHighlight(context,  (HandledScreen<?>) (Object) this);
        renderHoveredTooltip(context, (HandledScreen<?>) (Object) this, mouseX, mouseY);
        renderHeldItemOverlay(context, mouseX, mouseY);
    }

    @Unique
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

        if (activeInv == -1) activeInv = 0;
    }

    @Unique
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

        visibleInventories = xFitAmount * yFitAmount - (xFitAmount - 1) + scrollOffset;
        return new Pair<>(xRemain, yRemain);
    }

    @Unique
    private List<ItemStack> buildInventoryForIndex(int indexWithOffset, int playerInvIndex) throws IndexOutOfBoundsException {
        List<ItemStack> inv = new ArrayList<>();

        if (indexWithOffset == activeInv && activeInv != playerInvIndex + scrollOffset) {
            List<Slot> slots = BankOverlay.activeInvSlots;
            if (slots.size() < 45) {
                retryLoad();
                return inv;
            }
            boolean oldShouldWait = shouldWait;
            shouldWait = false;

            for (int j = 0; j < 45; j++) {
                if (j == 0) {
                    ItemStack rightArrow = null;
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
                        }
                    }
                }

                if (shouldWait) {
                    List<ItemStack> cached = Pages.BankPages.get(activeInv);
                    if (cached != null && j < cached.size()) inv.add(cached.get(j));
                    continue;
                }

                inv.add(slots.get(j).getStack());
            }
        } else if (indexWithOffset - scrollOffset == playerInvIndex) {
            List<Slot> slots = BankOverlay.playerInvSlots;
            if (slots != null && slots.size() >= 36) {
                for (int j = 0; j < 36; j++) inv.add(slots.get(j).getStack());
            } else {
                for (int j = 0; j < 36; j++) inv.add(Items.AIR.getDefaultStack());
            }
        } else {
            List<ItemStack> cached = Pages.BankPages.get(indexWithOffset);
            if (cached != null && cached.size() >= 45) {
                inv.addAll(cached.subList(0, 45));
            } else {
                for (int j = 0; j < 45; j++) inv.add(Items.AIR.getDefaultStack());
            }
        }

        return inv;
    }

    @Unique
    private void retryLoad() {
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

    @Unique
    private void applyAnnotation(ItemStack stack, List<ItemAnnotation> annotations, int index) {
        if(stack.getItem() == Items.AIR) return;

        if (stack == null) {
            annotations.add(null);
            return;
        }

        if (annotations.size() <= index) return;

        ItemAnnotation annotation = annotations.get(index);
        if (annotation == null) {
            StyledText name = StyledText.fromComponent(stack.getName());
            annotation = ((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(stack, name);
            annotations.set(index, annotation);
        }

        if (annotation != null) {
            ((ItemStackExtension) (Object) stack).setAnnotation(annotation);
        }
    }

    @Unique
    private void positionButtons(int x, int y) {
        int baseX = x - 9;
        int baseY = y + 7;

        if(currentOverlayType == BankOverlayType.ACCOUNT || currentOverlayType == BankOverlayType.CHARACTER) {
            if (currentOverlayType == BankOverlayType.ACCOUNT) {
                personalStorageButton.buttonText = "Switch to Character Bank";
            } else {
                personalStorageButton.buttonText = "Switch to Account Bank";
            }
            personalStorageButton.setPosition(baseX, baseY - 10);
        } else {
            baseY -= 14;
        }
        quickStashButton.setPosition(baseX, baseY + 4);
        dumpExceptHotbarButton.setPosition(baseX, baseY + 18);
        dumpAllButton.setPosition(baseX, baseY + 32);
        resetSearchButton.setPosition(baseX, baseY + 46);
        Searchbar.setPosition(baseX, baseY + 60);
    }

    @Unique
    private void renderDurabilityRing(DrawContext context, ItemStack stack, int x, int y) {
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

    @Unique
    private void renderEmeraldPouchRing(DrawContext context, ItemStack stack, int x, int y) {
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

    @Unique
    private void renderHighlightOverlay(DrawContext context, ItemStack stack, int x, int y) {
        if(stack.getItem() == Items.AIR) return;
        if (itemHighlightFeature == null) itemHighlightFeature = new ItemHighlightFeature();

        CustomColor color = ((ItemHighlightFeatureInvoker) itemHighlightFeature).invokeGetHighlightColor(stack, false);
        if (!Objects.equals(color, CustomColor.NONE)) {
            RenderUtils.drawTexturedRectWithColor(
                    context.getMatrices(),
                    Texture.HIGHLIGHT.resource(),
                    color.withAlpha(SimpleConfig.getInstance(WynnExtrasConfig.class).wynntilsItemRarityBackgroundAlpha),
                    x - 1, y - 1, 100, 18, 18,
                    highlightTexture.get().ordinal() * 18 + 18, 0,
                    18, 18,
                    Texture.HIGHLIGHT.width(),
                    Texture.HIGHLIGHT.height()
            );
        }
    }

    @Unique
    private void renderItemOverlays(DrawContext context, ItemStack stack, int x, int y) {
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
                    annotation instanceof PotionItem) {

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                ((ItemTextOverlayFeatureMixin) new ItemTextOverlayFeature()).invokeDrawTextOverlay(context.getMatrices(), stack, x, y, false);
                context.getMatrices().pop();
            }
            ((UnidentifiedItemIconFeatureInvoker) new UnidentifiedItemIconFeature()).invokeDrawIcon(context.getMatrices(), stack, x, y, 100);
        }
    }

    @Unique
    private void renderSearchOverlay(DrawContext context, ItemStack stack, int x, int y) {
        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 250.0F);

        String input = Searchbar.getInput().toLowerCase();
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

    @Unique
    private void renderPageOverlay(DrawContext context, int indexWithOffset, int i, int playerInvIndex, int playerXStart, int playerYStart) {
        if (i == playerInvIndex) return;
        if(BankPageNameInputsByType.get(currentOverlayType).get(indexWithOffset) == null) return;
        String pageName = BankPageNameInputsByType.get(currentOverlayType).get(indexWithOffset).getInput().isEmpty()
                ? Pages.BankPageNames.getOrDefault(indexWithOffset, "Page " + (indexWithOffset + 1))
                : BankPageNameInputsByType.get(currentOverlayType).get(indexWithOffset).getInput();

        Pages.BankPageNames.put(indexWithOffset, pageName);
        EasyTextInput input = BankPageNameInputsByType.get(currentOverlayType).get(indexWithOffset);
        input.setX(playerXStart);
        input.setY(playerYStart - 10);

        if (!input.isActive() && input.getInput().isEmpty()) {
            input.setInput(pageName);
        }

        if (indexWithOffset < currentMaxPages) {
            drawDynamicNameSign(context, input.getInput(), playerXStart, playerYStart);
        }

        boolean isHovered = indexWithOffset == hoveredInvIndex;
        boolean isActive = indexWithOffset == activeInv;

        if (!isActive && Searchbar.getInput().isEmpty() && !isHovered) {
            RenderUtils.drawRect(context.getMatrices(), CustomColor.fromHSV(0, 0, 0, 0.25f), playerXStart, playerYStart, 0, 164, 92);
        } else if (isActive) {
            RenderUtils.drawRectBorders(context.getMatrices(), CustomColor.fromHexString("FFFF00"), playerXStart, playerYStart, playerXStart + 164, playerYStart + 92, 0, 1);
        }
    }

    @Unique
    private void renderButtons(DrawContext context) {
        if(currentOverlayType == BankOverlayType.ACCOUNT) {
            personalStorageButton.buttonText = "Switch to Character Bank";
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                personalStorageButton.drawWithTexture(context, ButtonTextureDark);
            } else {
                personalStorageButton.drawWithTexture(context, ButtonTexture);
            }
        } else {
            personalStorageButton.buttonText = "Switch to Account Bank";
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                personalStorageButton.drawWithTexture(context, ButtonTextureDark);
            } else {
                personalStorageButton.drawWithTexture(context, ButtonTexture);
            }
        }
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            quickStashButton.drawWithTexture(context, ButtonTextureDark);
            dumpExceptHotbarButton.drawWithTexture(context, ButtonTextureDark);
            dumpAllButton.drawWithTexture(context, ButtonTextureDark);
            resetSearchButton.drawWithTexture(context, ButtonTextureDark);
            Searchbar.drawWithTexture(context, ButtonTextureDark);
        } else {
            quickStashButton.drawWithTexture(context, ButtonTexture);
            dumpExceptHotbarButton.drawWithTexture(context, ButtonTexture);
            dumpAllButton.drawWithTexture(context, ButtonTexture);
            resetSearchButton.drawWithTexture(context, ButtonTexture);
            Searchbar.drawWithTexture(context, ButtonTexture);
        }
    }

    @Unique
    private void renderNameInputs(DrawContext context) {
        for (int i = scrollOffset; i < visibleInventories - 1 && i < currentMaxPages; i++) {
            CustomColor color = (i == activeInv && !shouldWait)
                    ? CustomColor.fromHexString("FFEA00")
                    : CustomColor.fromHexString("FFFFFF");

            if(BankPageNameInputsByType.get(currentOverlayType).get(i) == null) continue;
            BankPageNameInputsByType.get(currentOverlayType).get(i).drawWithoutBackground(context, color);
            }
    }

    @Unique
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

    @Unique
    private void renderHoveredTooltip(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (hoveredSlot.getItem() == Items.AIR) return;

        Optional<WynnItem> item = asWynnItem(hoveredSlot);
        List<Text> tooltip = item.map(i -> {
            currentHoveredStack = hoveredSlot;
            currentHoveredWynnitem = i;
            return TooltipUtils.getWynnItemTooltip(hoveredSlot, i);
        }).filter(t -> !t.isEmpty())
            .orElse(hoveredSlot.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.ADVANCED));

        List<TooltipComponent> components = TooltipUtils.getClientTooltipComponent(tooltip);
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
            drawTooltip(screen.getTextRenderer(), components, (int)(mouseX / scale) + 12, y, null, context);
            //context.drawTooltip(screen.getTextRenderer(), components, (int)(mouseX / scale), y, bottomPositioner, null);
        }

        context.getMatrices().pop();
    }

    @Unique
    private void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, @Nullable Identifier texture, DrawContext context) {
        if (!components.isEmpty()) {
            int i = 0;
            int j = components.size() == 1 ? -2 : 0;

            TooltipComponent tooltipComponent;
            for(Iterator var9 = components.iterator(); var9.hasNext(); j += tooltipComponent.getHeight(textRenderer)) {
                tooltipComponent = (TooltipComponent)var9.next();
                int k = tooltipComponent.getWidth(textRenderer);
                if (k > i) {
                    i = k;
                }
            }

            int l = i;
            int m = j;
            int n = x;
            int o = y;
            context.getMatrices().push();
            TooltipBackgroundRenderer.render(context, n, o, i, j, 400, texture);
            context.getMatrices().translate(0.0F, 0.0F, 800.0F);
            int q = o;

            int r;
            TooltipComponent tooltipComponent2;
            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = (TooltipComponent)components.get(r);
                tooltipComponent2.drawText(textRenderer, n, q, context.getMatrices().peek().getPositionMatrix(), ((DrawContextAccessor) context).getVertexConsumers());
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }

            q = o;

            for(r = 0; r < components.size(); ++r) {
                tooltipComponent2 = (TooltipComponent)components.get(r);
                tooltipComponent2.drawItems(textRenderer, n, q, l, m, context);
                q += tooltipComponent2.getHeight(textRenderer) + (r == 0 ? 2 : 0);
            }

            context.getMatrices().pop();
        }
    }

    @Unique
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

    @Unique
    private long lastClickTime = 0;

    @Unique
    private int lastClickedSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
//        if(inClassSelection) {
//            cir.cancel();
//            return;
//        }

        if (currentOverlayType != BankOverlayType.NONE) {
            cir.cancel();
        } else {
            return;
        }

        if (shouldWait) return;

        BankOverlay.activeTextInput = null;

        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount + scrollOffset;
        if(hoveredInvIndex != playerInvIndex) {
            if (hoveredInvIndex == currentData.lastPage && activeInv == hoveredInvIndex - 1) {
                ScreenHandler currScreenHandler = McUtils.containerMenu();
                if (currScreenHandler == null) {
                    return;
                }
                ContainerUtils.clickOnSlot(52, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
                return;
            } else if (hoveredInvIndex == currentData.lastPage) {
                if(PersonalStorageUtils != null) {
                    BankOverlay.PersonalStorageUtils.jumpToDestination(currentData.lastPage);
                    System.out.println("Jump 1");
                    activeInv = currentData.lastPage - 1;
                }
                retryLoad();
                return;
            }
        }

        handleButtonClick(mouseX, mouseY);

        if (Searchbar.isClickInBounds((int) mouseX, (int) mouseY) != Searchbar.isActive()) {
            Searchbar.click();
        }

        handleNameInputs(mouseX, mouseY);

        if (hoveredIndex < 0 || hoveredIndex >= 63) return;

        SlotActionType actionType = determineActionType(button);
        if (handleBankSlotClick(hoveredIndex, button, actionType, cir)) return;
        if (handlePlayerSlotClick(hoveredIndex, button, actionType, playerInvIndex, cir)) return;
        if (handlePageClick()) {
            if (actionType == SlotActionType.QUICK_MOVE) {
                heldItem = Items.AIR.getDefaultStack();
            }
            cir.cancel();
        }
    }

    @Unique
    private void handleButtonClick(double mouseX, double mouseY) {
        if (personalStorageButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            personalStorageButton.click(); return;
        }
        if (quickStashButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            quickStashButton.click(); return;
        }
        if (dumpExceptHotbarButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            dumpExceptHotbarButton.click(); return;
        }
        if (dumpAllButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            dumpAllButton.click(); return;
        }
        if (resetSearchButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            resetSearchButton.click();
        }
    }

    @Unique
    private void handleNameInputs(double mouseX, double mouseY) {
        var inputs = BankPageNameInputsByType.get(currentOverlayType);
        if (inputs == null) return;

        for (int i = scrollOffset; i < Math.min(inputs.size(), scrollOffset + xFitAmount * yFitAmount); i++) {
            var input = inputs.get(i);
            if (input == null) continue;

            if (input.isClickInBounds((int) mouseX, (int) mouseY) != input.isActive()) {
                input.click();
            }
        }
    }

    @Unique
    private boolean handleBankSlotClick(int hoveredIndex, int button, SlotActionType actionType, CallbackInfoReturnable<Boolean> cir) {
        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount + scrollOffset;

        if(hoveredInvIndex != activeInv) return false;
        if(hoveredInvIndex == playerInvIndex) return false;

        ItemStack oldHeld = heldItem;
        heldItem = getHeldItem(hoveredIndex, actionType, button);

        if(heldItem.getCustomName() != null) {
            if ((heldItem.getCustomName().getString().contains("Pouch") || heldItem.getCustomName().getString().contains("Potions")) && button == 1) {
                heldItem = oldHeld == null ? Items.AIR.getDefaultStack() : oldHeld;
                return true;
            }
        }

        if (shouldCancelEmeraldPouch(oldHeld, heldItem)) {
            heldItem = Items.AIR.getDefaultStack();
        }

        if (MinecraftClient.getInstance().interactionManager == null) return true;

        MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, hoveredIndex, button, actionType, MinecraftClient.getInstance().player);
        annotationCache.get(activeInv).clear();
        lastClickedSlot = hoveredIndex;
        cir.cancel();
        return true;
    }

    @Unique
    private boolean handlePlayerSlotClick(int hoveredIndex, int button, SlotActionType actionType, int playerInvIndex, CallbackInfoReturnable<Boolean> cir) {
        if (hoveredInvIndex != playerInvIndex) return false;
        if(hoveredIndex == 4) return true; //Ingredient pouch, clicking it within the bank overlay crashes the game

        ItemStack oldHeld = heldItem;
        heldItem = getHeldItem(hoveredIndex + 54, actionType, button);

        if(heldItem.getCustomName() != null) {
            if ((heldItem.getCustomName().getString().contains("Pouch") || heldItem.getCustomName().getString().contains("Potions")) && button == 1) {
                heldItem = oldHeld == null ? Items.AIR.getDefaultStack() : oldHeld;
                return true;
            }
        }

        if (shouldCancelEmeraldPouch(oldHeld, heldItem)) {
            heldItem = Items.AIR.getDefaultStack();
        }

        if (MinecraftClient.getInstance().interactionManager == null) return true;

        MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, hoveredIndex + 54, button, actionType, MinecraftClient.getInstance().player);
        annotationCache.get(playerInvIndex).clear();
        lastClickedSlot = hoveredIndex + 54;
        cir.cancel();
        return true;
    }


    @Unique
    private boolean handlePageClick() {
        if (heldItem.getItem() != Items.AIR) return false;

        int clickedPage = hoveredInvIndex + 1;
        if (clickedPage <= currentData.lastPage) {
            if(PersonalStorageUtils != null) {
                List<ItemStack> stacks = BankOverlay.activeInvSlots.stream()
                        .map(Slot::getStack)
                        .collect(Collectors.toList());

                Pages.BankPages.put(activeInv, stacks);
                activeInv = hoveredInvIndex;
                System.out.println("Jump 2");
                BankOverlay.PersonalStorageUtils.jumpToDestination(clickedPage);
            }
        } else {
            if (activeInv != currentData.lastPage - 1 && PersonalStorageUtils != null) {
                activeInv = currentData.lastPage - 1;
                System.out.println("Jump 3");
                BankOverlay.PersonalStorageUtils.jumpToDestination(currentData.lastPage);
                buyPageStack = null;
            }
        }
        return true;
    }

    @Unique
    private SlotActionType determineActionType(int mouseButton) {
        SlotActionType actionType = SlotActionType.PICKUP;

        if(mouseButton == 1) return actionType;

        long now = System.currentTimeMillis();
        if (heldItem != null && heldItem.getItem() != Items.AIR) {
            if (now - lastClickTime < 250 && (lastClickedSlot == hoveredIndex || lastClickedSlot == hoveredIndex + 54)) {
                actionType = SlotActionType.PICKUP_ALL;
            }
        }
        lastClickTime = now;

        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
            actionType = SlotActionType.QUICK_MOVE;
            annotationCache.get(activeInv).clear();
        }

        return actionType;
    }


    @Unique
    private boolean shouldCancelEmeraldPouch(ItemStack oldHeld, ItemStack newHeld) {
        if (oldHeld == null || newHeld == null || newHeld.getCustomName() == null) return false;

        return (oldHeld.getItem() == Items.EMERALD ||
                oldHeld.getItem() == Items.EMERALD_BLOCK ||
                oldHeld.getItem() == Items.EXPERIENCE_BOTTLE) &&
                newHeld.getCustomName().getString().contains("Pouch");
    }

    @Unique
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


    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentOverlayType != BankOverlayType.NONE) {
            cir.cancel();
        }
    }

    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (currentOverlayType != BankOverlayType.NONE) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Unique
    public <T extends WynnItem> Optional<T> asWynnItem(ItemStack itemStack) {
        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if(annotationOpt.isEmpty()) return Optional.empty();
        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();
        return Optional.of((T) wynnItem);
    }

    @Unique
    public void drawDynamicNameSign(DrawContext context, String input, int x, int y) {
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
            RenderUtils.drawTexturedRect(context.getMatrices(), signLeftDark, x, y - 13, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), signLeft, x, y - 13, 10, 15, 10, 15);
        }
        if (strWidth > 15) {
            for (int i = 0; i < amount; i++) {
                RenderUtils.drawTexturedRect(context.getMatrices(), signMids.get(i % 3), x + 10 + 10 * i, y - 13, 10, 15, 10, 15);
            }
        }
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            RenderUtils.drawTexturedRect(context.getMatrices(), signRightDark, x + 10 + 10 * amount, y - 13, 10, 15, 10, 15);
        } else {
            RenderUtils.drawTexturedRect(context.getMatrices(), signRight, x + 10 + 10 * amount, y - 13, 10, 15, 10, 15);
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    public void onInit(CallbackInfo ci) {
        heldItem = Items.AIR.getDefaultStack();
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void onClose(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if (currScreenHandler == null) {
            return;
        }

        Screen currScreen = McUtils.mc().currentScreen;
        if (currScreen == null) {
            return;
        }

        if (currentOverlayType != BankOverlayType.NONE) {
            heldItem = Items.AIR.getDefaultStack();

            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : BankOverlay.activeInvSlots) {
                stacks.add(slot.getStack());
            }
            if(activeInv != -1) {
                Pages.BankPages.put(activeInv, stacks);
            }
            BankOverlay.activeInvSlots.clear();
            activeInv = 0;
            annotationCache.clear();
            scrollOffset = 0;
            Pages.save();
        }
        currentOverlayType = BankOverlayType.NONE;
    }

    @Unique
    void drawEmeraldOverlay(DrawContext context, int x, int y) {
        InventoryEmeraldCountFeature emeraldCountFeature = new InventoryEmeraldCountFeature();
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
    @Shadow
    private Slot touchHoveredSlot;

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void keyPressedPre(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        InventoryKeyPressEvent event = new InventoryKeyPressEvent(keyCode, scanCode, modifiers, this.touchHoveredSlot);
        event.post();

        if (event.isCanceled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Unique
    private Pair<List<Text>, Float> fitTooltip(MatrixStack poseStack, List<Text> tooltips, int mouseX, int mouseY) {
        TooltipFittingFeature fitting = Managers.Feature.getFeatureInstance(TooltipFittingFeature.class);
        Window window = MinecraftClient.getInstance().getWindow();

        float scale = fitting.universalScale.get();
        boolean wrap = ((TooltipFittingFeatureAccessor) fitting).getWrapText().get();
        boolean fit = ((TooltipFittingFeatureAccessor) fitting).getFitToScreen().get();

        List<Text> wrapped = tooltips;

        if (wrap) {
            int maxWidth = (int)(window.getScaledWidth() / scale);
            int tooltipWidth = ComponentUtils.getOptimalTooltipWidth(wrapped, maxWidth, (int)(mouseX / scale));
            wrapped = ComponentUtils.wrapTooltips(wrapped, tooltipWidth);
        }

        List<TooltipComponent> components = TooltipUtils.getClientTooltipComponent(wrapped);
        int height = TooltipUtils.getTooltipHeight(components);
        height = (int)(height * scale) + 10;

        if (fit && height > window.getScaledHeight()) {
            scale *= (float)window.getScaledHeight() / (float)height;
        }

        poseStack.push();
        poseStack.scale(scale, scale, 1.0F);

        return new Pair<>(wrapped, scale);
    }

    @Unique
    private void positionTooltip(int screenWidth, int screenHeight, Vector2i tooltipPos, int tooltipWidth, int tooltipHeight) {
        if (tooltipPos.x + tooltipWidth > screenWidth) {
            tooltipPos.x = Math.max(tooltipPos.x - 24 - tooltipWidth, 4);
        }

        if (tooltipPos.y + tooltipHeight > screenHeight) {
            tooltipPos.y = screenHeight - tooltipHeight;
        }
    }

}

//TODO SHOW POSSIBLE THINGS WHEN ITEM IS UNIDENTIFIED
//TODO PAGE NAME INPUT MORE SPACE BETWEEN BORDER AND TEXT