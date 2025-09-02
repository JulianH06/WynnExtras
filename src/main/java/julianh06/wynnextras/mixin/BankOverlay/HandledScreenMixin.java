package julianh06.wynnextras.mixin.BankOverlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Models;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.inventory.*;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.handlers.item.ItemHandler;
import com.wynntils.mc.extension.ItemStackExtension;
import com.wynntils.models.emeralds.type.EmeraldUnits;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.*;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.services.itemfilter.statproviders.RarityStatProvider;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.features.inventory.*;
import julianh06.wynnextras.features.inventory.BankOverlayButtons.*;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.mixin.Accessor.PersonalStorageUtilitiesFeatureAccessor;
import julianh06.wynnextras.mixin.Accessor.SlotAccessor;
import julianh06.wynnextras.mixin.Invoker.*;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.List;

import static julianh06.wynnextras.features.inventory.BankOverlay.*;

@WEModule
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Unique
    private static ItemStack heldItem = Items.AIR.getDefaultStack();

    @Unique
    int activeInv = -1;

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
    HashMap<Integer, EasyTextInput> BankPageNameInputs = new HashMap<>();

    @Unique
    ItemHighlightFeature itemHighlightFeature;

    @Unique
    boolean shouldWait = false;

    @Unique
    int lastPage = 21;

    @Unique
    Identifier ButtonTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/button.png");

    @Unique
    Identifier invTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/inv.png");

    @Unique
    Identifier bankTexture = Identifier.of("wynnextras", "textures/gui/bankoverlay/bank.png");

    @Unique
    Identifier signLeft = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_left.png");

    @Unique
    Identifier signRight = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_right.png");

    @Unique
    Identifier signMid1 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m1.png");

    @Unique
    Identifier signMid2 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m2.png");

    @Unique
    Identifier signMid3 = Identifier.of("wynnextras", "textures/gui/bankoverlay/sign_m3.png");

    @Unique
    List<Identifier> signMids = new ArrayList<>();

    @Unique
    String buyPageStageText = "NOT BOUGHT";

    @Unique
    BankOverlayData Pages;

    @Unique
    Map<Integer, List<ItemAnnotation>> annotationCache = new HashMap<>();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderInventory(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (BankOverlayData.INSTANCE == null) return;
        if (Pages == null) Pages = BankOverlayData.INSTANCE;


        if (BankPageNameInputs.isEmpty()) {
            for (int i = 0; i < 20; i++) {
                BankPageNameInputs.put(i, new EasyTextInput(-1000, -1000, 13, 162 + 4));
            }
        }

        hoveredInvIndex = -1;

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        xFitAmount = Math.min(3, Math.floorDiv(screenWidth - 84 /*112?*/, 162)); //Max amount is 3
        yFitAmount = Math.min(4, Math.floorDiv(screenHeight, 104));
        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount;
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

        if (BankOverlay.isBank) {
            if (MinecraftClient.getInstance() != null) {
                RenderUtils.drawRect(context.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
                int xStart = xRemain / 2 - 2;
                int yStart = yRemain / 2 - 2;

                FontRenderer.getInstance().renderText(
                        context.getMatrices(),
                        StyledText.fromString("If you see this text something went wrong, close the menu and try again"),
                        (float) xStart,
                        (float) yStart,
                        CustomColor.fromHexString("ff0000"),
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL,
                        1.0f
                );
            }


            if (activeInv == -1) {
                activeInv = 0;
            }

            hoveredIndex = -1;
            hoveredSlot = Items.AIR.getDefaultStack();

            if (lastPage == 21) {
                PersonalStorageUtilitiesFeatureAccessor lastPageGetter = (PersonalStorageUtilitiesFeatureAccessor) BankOverlay.PersonalStorageUtils;
                lastPage = lastPageGetter.getLastPage();
            }

            context.getMatrices().push();
            ci.cancel();
        } else {
            return;
        }

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (WynnExtras.testInv == null) {
            WynnExtras.testInv = screen.getScreenHandler().slots;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        for (int indexWithOffset = scrollOffset; indexWithOffset < xFitAmount * yFitAmount - (xFitAmount - 1) + scrollOffset; indexWithOffset++) {
            boolean pageContainsSearch = false;
            boolean isUnlocked = indexWithOffset < lastPage; // < instead of <= because the index starts at 0 and the pages at 1
            if (indexWithOffset - scrollOffset == playerInvIndex) {
                isUnlocked = true;
            } else if (indexWithOffset > 19) {
                continue;
            }

            canScrollFurther = (xFitAmount * yFitAmount - (xFitAmount - 1) + scrollOffset) < 21;
            int i = indexWithOffset - scrollOffset;

            if(i == 0) {
                int x = xRemain / 2 - 10;
                int y = yRemain / 2 - 10;
                drawEmeraldOverlay(context, x - 28, y - 5);
            }

            //int inventoryOffsetX = (int) ((offsetX - (i % 3) * 175 * 3 * widthFactor) * scale);
            //int inventoryOffsetY = (int) ((offsetY - Math.floorDiv(i, 3) * 100 * 3 * widthFactor) * scale);
            //int baseWidth = (int)(screenWidth * 0.45); // z. B. 45 % der Breite
            //int inventoryOffsetX = baseWidth - (i % 3) * (int)(screenWidth * 0.2) - offsetX;
            List<ItemStack> inv = new ArrayList<>();
            if (indexWithOffset == activeInv && activeInv != playerInvIndex + scrollOffset) {
                List<Slot> invslots = BankOverlay.activeInvSlots;
                if (invslots.isEmpty()) {
                    return;
//                    McUtils.sendErrorToClient("[WynnExtras] Error with Bank Overlay. Closing Menu, try again");
//                    if (McUtils.mc().currentScreen != null) {
//                        McUtils.mc().currentScreen.close();
//                        return;
//                    }
//                    for (int k = 0; k < 45; k++) {
//                        inv.add(Items.AIR.getDefaultStack());
//                    }
                } else if (invslots.size() < 45) {
                    invslots.clear();
                } else {
                    boolean oldShouldWait = shouldWait;
                    shouldWait = false;
                    for (int j = 0; j < 45; j++) {
                        //52  List<ItemStack> activeInvslots = WynnarschConfig.INSTANCE.BankPages.get(indexWithOffset);
//                        if(invslots.get(52).getStack().getName().contains("Page " + activeInv + 2))))
//                        {
//                            System.out.println("Should wait!");
//                        }
                        if (j == 0) {
                            String rawText = invslots.get(52).getStack().getName().getString();
                            String cleanedText = rawText.replaceAll("§[0-9a-fk-or]", "");
                            if (!cleanedText.contains("Page " + (activeInv + 2))) {
                                shouldWait = true;
                            } else if (oldShouldWait) {
                                List<ItemStack> stacks = new ArrayList<>();
                                for (Slot slot : BankOverlay.activeInvSlots) {
                                    stacks.add(slot.getStack());
                                }
                                Pages.BankPages.put(activeInv, stacks);
                            }
                        }
                        if (shouldWait) {
                            if (Pages.BankPages.get(activeInv) != null) {
                                if (j < Pages.BankPages.get(activeInv).size()) {
                                    inv.add(Pages.BankPages.get(activeInv).get(j));
                                }
                            }
                            continue;
                        }
                        inv.add(invslots.get(j).getStack());
                    }
                }
            } else if (i == playerInvIndex) {
                List<Slot> invslots = BankOverlay.playerInvSlots;
                if (invslots != null) {
                    if (invslots.size() < 36) {
                        invslots.clear();
                    }
                    for (int j = 0; j < 36; j++) {
                        inv.add(invslots.get(j).getStack());
                    }
                } else {
                    for (int k = 0; k < 36; k++) {
                        inv.add(Items.AIR.getDefaultStack());
                    }
                }
            } else {
                List<ItemStack> invslots = Pages.BankPages.get(indexWithOffset);
                if (invslots != null) {
                    if (invslots.size() < 45) {
                        invslots.clear();
                    }
                    for (int j = 0; j < 45; j++) {
                        ItemStack stack = invslots.get(j);
                        inv.add(stack);
                    }
                } else {
                    for (int k = 0; k < 45; k++) {
                        inv.add(Items.AIR.getDefaultStack());
                    }
                }
            }
//            List<SavedItem> savedItems = new ArrayList<>();
            List<ItemAnnotation> annotations = annotationCache.computeIfAbsent(indexWithOffset, k -> new ArrayList<>(Collections.nCopies(inv.size(), null)));
            int stackIndex = 0;
            for (ItemStack stack : inv) {
                if(i != playerInvIndex) {
                    if (stack == null) {
                        stack = new ItemStack(Items.AIR);
                        annotations.add(null);
                    } else if (annotations.size() > stackIndex && i != playerInvIndex) {
                        ItemAnnotation annotation = annotations.get(stackIndex);
                        if (annotation == null) {
                            StyledText name = StyledText.fromComponent(stack.getName());
                            annotation = ((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(stack, name);

                            ((ItemStackExtension) (Object) stack).setAnnotation(annotation);
                            annotations.set(stackIndex, annotation);
                        }

                        if (annotation != null) {
                            ((ItemStackExtension) (Object) stack).setAnnotation(annotation);
                        }
                    }
                }
                int x = xRemain / 2 + 18 * (stackIndex % 9) + (i % xFitAmount) * (162 + 4);
                int y = yRemain / 2 + 18 * Math.floorDiv(stackIndex, 9) + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);

                if (i == playerInvIndex) {
                    if (stackIndex == 0) {
                        personalStorageButton.setX(x - 9);
                        personalStorageButton.setY(y - 10 + 7);
                        quickStashButton.setX(x - 9);
                        quickStashButton.setY(y + 4 + 7);
                        dumpExceptHotbarButton.setX(x - 9);
                        dumpExceptHotbarButton.setY(y + 18 + 7);
                        dumpAllButton.setX(x - 9);
                        dumpAllButton.setY(y + 32 + 7);
                        resetSearchButton.setX(x - 9);
                        resetSearchButton.setY(y + 46 + 7);
                        Searchbar.setX(x - 9);
                        Searchbar.setY(y + 60 + 7);
                        RenderUtils.drawTexturedRect(context.getMatrices(), invTexture, x + 162 + 4 - 8, y - 12 + 7, 176, 86, 176, 86);
                    }
                    x += 162 + 4;
                    y -= 3;//10;
                    if (stackIndex > 26) {
                        y += 5;
                    }
                }
                if (i != playerInvIndex && stackIndex == 0) {
                    int xStart = xRemain / 2 - 2 + (i % xFitAmount) * (162 + 4);
                    int yStart = yRemain / 2 - 2 + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);
                    if (i == 0) {
                        RenderUtils.drawRect(
                                context.getMatrices(),
                                CustomColor.fromHexString("81644b"),
                                x - 2 - 7, y - 15, 1000,
                                xFitAmount * (162 + 4) + 11, (yFitAmount - 1) * (90 + 4 + 10) + 10
                        );
                        RenderUtils.drawRectBorders(
                                context.getMatrices(),
                                CustomColor.fromHexString("4f342c"),
                                x - 2 - 7, y - 15,
                                x - 2 - 7 + xFitAmount * (162 + 4) + 11, y - 15 + (yFitAmount - 1) * (90 + 4 + 10) + 10 + 0.5f, 0, 1
                        );
                    }
                    RenderUtils.drawTexturedRect(context.getMatrices(), bankTexture, xStart, yStart, 164, 92, 164, 92);

                    boolean hovered =
                            mouseX >= xStart &&
                                    mouseX < xStart + 162 &&
                                    mouseY >= yStart &&
                                    mouseY < yStart + 92;

                    if(hovered) {
                        hoveredInvIndex = indexWithOffset;
                        if(stackIndex == 0 && hoveredInvIndex != activeInv && Searchbar.getInput().isEmpty()) {
                            RenderUtils.drawRect(
                                    context.getMatrices(),
                                    CustomColor.fromHSV(0, 0, 1000, 0.25f),
                                    xStart, yStart, 0, 164, 92
                            );
                        }
                    }
                }

                Optional<DurableItemProperty> durableItemOpt = Models.Item.asWynnItemProperty(stack, DurableItemProperty.class);
                if (durableItemOpt.isPresent()) {
                    CappedValue durability = durableItemOpt.get().getDurability();
                    float durabilityFraction = (float) durability.current() / (float) durability.max();
                    int colorInt = MathHelper.hsvToRgb(Math.max(0.0F, durabilityFraction) / 3.0F, 1.0F, 1.0F);
                    CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);
                    RenderSystem.enableDepthTest();
                    RenderUtils.drawArc(context.getMatrices(), color, x, y, 100.0F, durabilityFraction, 6, 8);
                    RenderSystem.disableDepthTest();
                }

                Optional<EmeraldPouchItem> optionalItem = Models.Item.asWynnItem(stack, EmeraldPouchItem.class);
                if (!optionalItem.isEmpty()) {
                    CappedValue capacity = new CappedValue(((EmeraldPouchItem)optionalItem.get()).getValue(), ((EmeraldPouchItem)optionalItem.get()).getCapacity());
                    float capacityFraction = (float)capacity.current() / (float)capacity.max();
                    int colorInt = MathHelper.hsvToRgb((1.0F - capacityFraction) / 3.0F, 1.0F, 1.0F);
                    CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);
                    float ringFraction = Math.min(1.0F, capacityFraction);
                    RenderSystem.enableDepthTest();
                    RenderUtils.drawArc(context.getMatrices(), color, (float)(x - 2), (float)(y - 2), 100.0F, ringFraction, 8, 10);
                    RenderSystem.disableDepthTest();
                }

                RarityStatProvider statProvider = new RarityStatProvider();
                Optional<WynnItem> item = Optional.empty();
                if (stack.getItem() != null) {
                    if (!stack.getItem().equals(Items.AIR)) {
                        //Handlers.Item.updateItem(stack, new WynnItem(), StyledText.fromComponent(stack.getName()));
                        item = asWynnItem(stack);
                    }
                }
//                List<WynnItem> pageWynnItemList = BankOverlayData.INSTANCE.BankPagesAsWynnItems.get(indexWithOffset);
//                if(stack.getItem() != null) {
//                    if (!pageWynnItemList.isEmpty() && pageWynnItemList.size() > stackIndex) {
//                        WynnItem savedItem = pageWynnItemList.get(stackIndex);
//                        if (savedItem != null) {
//                            item = Optional.ofNullable(savedItem);
//                        } else if (!stack.getItem().equals(Items.AIR)) {
//                            item = asWynnItem(stack);
//                            if(item.isPresent()) {
//                                while(pageWynnItemList.size() <= 21) pageWynnItemList.add(null);
//                                pageWynnItemList.set(stackIndex, item.get());
//                                BankOverlayData.INSTANCE.BankPagesAsWynnItems.put(indexWithOffset, pageWynnItemList);
//                            }
//                        }
//                    } else if (!stack.getItem().equals(Items.AIR)) {
//                        item = asWynnItem(stack);
//                        if(item.isPresent()) {
//                            while(pageWynnItemList.size() <= 21) pageWynnItemList.add(null);
//                            pageWynnItemList.set(stackIndex, item.get());
//                            BankOverlayData.INSTANCE.BankPagesAsWynnItems.put(indexWithOffset, pageWynnItemList);
//                        }
//                    }
//                }


//                Optional<WynnItem> item;
//                if(i == activeInv
//                ) {
//                    item = Models.Item.getWynnItem(stack); //asWynnItem(stack);
//                } else {
//                    item = Models.Item.getWynnItem(stack);
//                    //item = Optional.ofNullable(WynnarschConfig.INSTANCE.BankPagesSavedItems.get(i).get(stackIndex).wynnItem());
//                }
                if (item.isPresent()) {
//                    if(statProvider.getValue(item.get()).isPresent()) {
//                        if(i == activeInv) {
//                            Models.ItemEncoding
//                            if (item.get() instanceof GearBoxItem) {
//                                savedItems.add(null);
//                            } else if ()
//                            else
//                                savedItems.add(SavedItem.create(item.get(), Set.of(), stack));
//                            }
//                    }
                    if (itemHighlightFeature == null) {
                        itemHighlightFeature = new ItemHighlightFeature();
                    }
                    CustomColor color = CustomColor.NONE;
                    color = ((ItemHighlightFeatureInvoker) itemHighlightFeature).invokeGetHighlightColor(stack, false);
                    if(!Objects.equals(color, new CustomColor(255, 255, 255))) {
                        RenderUtils.drawTexturedRectWithColor(
                                context.getMatrices(),
                                Texture.HIGHLIGHT.resource(),
                                color.withAlpha(100),
                                x - 1,
                                y - 1,
                                100,
                                18,
                                18,
                                highlightTexture.get().ordinal() * 18 + 18, //currently always circle transparent
                                0,                                                  //TODO: make it sync with the users wynntils config
                                18,
                                18,
                                Texture.HIGHLIGHT.width(),
                                Texture.HIGHLIGHT.height()
                        );
                    }
                }

                boolean hovered =
                        mouseX >= x - 1 &&
                                mouseX < x + 17 &&
                                mouseY >= y - 1 &&
                                mouseY < y + 17;

                if (hovered) {
                    hoveredSlot = stack;
                    //System.out.println("Hovered: " + stack.getName());
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

                if(item.isPresent()) {
                    if (item.get() instanceof ItemAnnotation) {
                        if(item.get() instanceof TeleportScrollItem ||
                                item.get() instanceof AmplifierItem ||
                                item.get() instanceof DungeonKeyItem ||
                                item.get() instanceof EmeraldPouchItem ||
                                item.get() instanceof GatheringToolItem ||
                                item.get() instanceof HorseItem ||
                                item.get() instanceof PowderItem
                        ) {
                            context.getMatrices().push();
                            context.getMatrices().translate(0, 0, 100);
                            ((ItemTextOverlayFeatureMixin) new ItemTextOverlayFeature()).invokeDrawTextOverlay(context.getMatrices(), stack, x, y, false);
                            context.getMatrices().pop();
                        }
                    }
                    ((UnidentifiedItemIconFeatureInvoker) new UnidentifiedItemIconFeature()).invokeDrawIcon(context.getMatrices(), stack, x, y, 100);
                }
                //338: standardOffsetX 113: standardOffsetY 131 148 pixel height: 16/2 = 8 (but it needs to be 9 for some reason)
                //int slotX = (int) (BankOverlay.firstBankSlot.x + 338 * 3 * widthFactor - inventoryOffsetX + 8 + (stackIndex % 9) * 16 * 3 * widthFactor  + (stackIndex % 9) * 2);
                //int slotY = (int) (BankOverlay.firstBankSlot.y + 113 * 3 * widthFactor  - inventoryOffsetY + 8 + Math.floorDiv(stackIndex, 9) * 16 * 3 * widthFactor  + Math.floorDiv(stackIndex, 9) * 2); //rundet nicht richti gab glaub ich


                stackIndex++;

                context.getMatrices().push();
                context.getMatrices().translate(0.0F, 0.0F, 250.0F);
                if (stack.getCustomName() != null) {
                    if (!Searchbar.getInput().isEmpty() && stack.getCustomName().getString().toLowerCase().contains(Searchbar.getInput().toLowerCase())) {
                        pageContainsSearch = true;
                        RenderUtils.drawRectBorders(
                                context.getMatrices(),
                                CustomColor.fromHexString("008000"),
                                x, y, x + 16, y + 16,
                                0, 1
                        );
                    } else if (!Searchbar.getInput().isEmpty() || stack.getItem().equals(Items.AIR)) {
//                        context.fill(
//                                x, y, x + 16, y + 16,
//                                0, 111111
//                        );
                        RenderUtils.drawRect(
                                context.getMatrices(),
                                CustomColor.fromHSV(0, 0, 0, 0.75f),
                                x - 1, y - 1, 0,
                                18, 18
                        );
                    }
                }
                if (!Searchbar.getInput().isEmpty() && stack.getItem().equals(Items.AIR)) {
                    RenderUtils.drawRect(
                            context.getMatrices(),
                            CustomColor.fromHSV(0, 0, 0, 0.75f),
                            x - 1, y - 1, 0,
                            18, 18
                    );
                }
                context.getMatrices().pop();
            }

//            if (indexWithOffset != activeInv) {
//                WynnarschConfig.INSTANCE.BankPagesSavedItems.put(indexWithOffset, savedItems);
//            }




            int playerInvOffset = i == playerInvIndex ? (162 + 4) : 0;
            int xStart = xRemain / 2 - 2 + (i % xFitAmount) * (162 + 4) + playerInvOffset;
            int yStart = yRemain / 2 - 2 + Math.floorDiv(i, xFitAmount) * (90 + 4 + 10);
            int color;
            if (indexWithOffset == activeInv && !shouldWait) {
                color = CustomColor.fromHexString("FFEA00").asInt();
            } else if (!pageContainsSearch && !Searchbar.getInput().isEmpty()) {
                color = CustomColor.fromHexString("808080").asInt();
            } else if (pageContainsSearch && indexWithOffset != activeInv) {
                color = CustomColor.fromHexString("008000").asInt();
            } else if (!isUnlocked && i != playerInvIndex) {
                color = CustomColor.fromHexString("FF0000").asInt();
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, buyPageStageText, xStart + 55, yStart + 40, color);
            } else {
                color = CustomColor.fromHexString("FFFFFF").asInt();
            }
            if (i != playerInvIndex) {
                String pageString;
                if (BankPageNameInputs.get(indexWithOffset).getInput().isEmpty()) {
                    pageString = Pages.BankPageNames.getOrDefault(indexWithOffset, "Page " + (indexWithOffset + 1));
                } else {
                    pageString = BankPageNameInputs.get(indexWithOffset).getInput();
                }
                Pages.BankPageNames.put(indexWithOffset, pageString);
                //if(i == 0) {
                BankPageNameInputs.get(indexWithOffset).setX(xStart); //+ 62);
                BankPageNameInputs.get(indexWithOffset).setY(yStart - 10);
                if (!BankPageNameInputs.get(indexWithOffset).isActive() && BankPageNameInputs.get(indexWithOffset).getInput().isEmpty()) {
                    BankPageNameInputs.get(indexWithOffset).setInput(pageString);
                }
//                } else {
//                    context.drawText(MinecraftClient.getInstance().textRenderer, pageString, xStart + 62, yStart - 10, color, true);
//                }
                if (indexWithOffset < 20) {
                    drawDynamicNameSign(context, BankPageNameInputs.get(indexWithOffset).getInput(), xStart, yStart);
                }
                if(indexWithOffset != activeInv && Searchbar.getInput().isEmpty() && indexWithOffset != hoveredInvIndex) {
                    RenderUtils.drawRect(
                            context.getMatrices(),
                            CustomColor.fromHSV(0, 0, 0, 0.25f),
                            xStart, yStart, 0, 164, 92
                    );
                } else if(indexWithOffset == activeInv) {
                    RenderUtils.drawRectBorders(
                            context.getMatrices(),
                            CustomColor.fromHexString("FFFF00"),
                            xStart, yStart,
                            xStart + 164, yStart + 92, 0, 1
                    );
                }
                //RenderUtils.drawTexturedRect(context.getMatrices(), Identifier.of("wynnextras", "textures/gui/bankoverlay/signtest3.png"), xStart/* + 54*/, yStart - 13, 55, 15, 55, 15);
//                context.drawBorder(
//                        xStart,
//                        yStart,
//                        162 + 2, //Width of one Inv + 2
//                        90 + 2, //Height of one Inv + 2
//                        color
//                );
            } else {
                yStart -= 10;
//                context.drawBorder(
//                        xStart,
//                        yStart,
//                        162 + 2, //Width of one Inv + 2
//                        72 + 2, //Height of the Player Inv + 2
//                        color
//                );
            }


            //context.drawBorder(0,0,16,16,CustomColor.fromHexString("FFFFFF").asInt());
            //((HandledScreenInvoker) screen).invokeDrawItem(context, Items.DIAMOND.getDefaultStack(), 0, 0, String.valueOf(1));

        }

        personalStorageButton.drawWithTexture(context, ButtonTexture);
        quickStashButton.drawWithTexture(context, ButtonTexture);
        dumpExceptHotbarButton.drawWithTexture(context, ButtonTexture);
        dumpAllButton.drawWithTexture(context, ButtonTexture);
        resetSearchButton.drawWithTexture(context, ButtonTexture);
        Searchbar.drawWithTexture(context, ButtonTexture);
        for (int i = scrollOffset; i < xFitAmount * yFitAmount - (xFitAmount - 1) + scrollOffset - 1; i++) {
            CustomColor color;
            if (i == activeInv && !shouldWait) {
                color = CustomColor.fromHexString("FFEA00");
            } else {
                color = CustomColor.fromHexString("FFFFFF");
            }
            if (i > 19) break;
            BankPageNameInputs.get(i).drawWithoutBackground(context, color);
        }

        if (hoveredIndex != -1) {
            Inventory dummy = new SimpleInventory(1);
            Slot focusedSlot = new Slot(dummy, hoveredIndex, 0, 0);
            //if(index == 9) {
//            ((SlotAccessor) focusedSlot).setX((int) ((BankOverlay.firstBankSlot.x + 1 + 338 * 3 * widthFactor  + (hoveredIndex % 9)  * 16 * 3 * widthFactor  + (hoveredIndex % 9) * 2 - (200 - 175) * 3 * widthFactor ) * scale));//hoveredSlot.x + (int)(0.39f * this.offsetX * Math.signum(this.offsetX)) + this.offsetX);
//            ((SlotAccessor) focusedSlot).setY((int) ((BankOverlay.firstBankSlot.y - 1 + 100 * 3 * widthFactor  - offsetY + 113 * 3 * widthFactor  + 2 * 3 * widthFactor  + (Math.floorDiv(hoveredIndex, 9)) * 16 * 3 * widthFactor  + Math.floorDiv(hoveredIndex, 9) * 2 - (100 - 300) * 3 * widthFactor ) * scale));//int focusedY = hoveredSlot.y + (int)(0.14f * this.offsetY * Math.signum(this.offsetY)/*vielleicht * signum*/);
//        } else {
            ((SlotAccessor) focusedSlot).setX(hoveredX);//hoveredSlot.x + (int)(0.39f * this.offsetX * Math.signum(this.offsetX)) + this.offsetX);
            ((SlotAccessor) focusedSlot).setY(hoveredY);//int focusedY = hoveredSlot.y + (int)(0.14f * this.offsetY * Math.signum(this.offsetY)/*vielleicht * signum*/);
            //}
            ((HandledScreenAccessor) screen).setFocusedSlot(focusedSlot);
            ((HandledScreenInvoker) screen).invokeDrawSlotHighlightBack(context);
            ((HandledScreenInvoker) screen).invokeDrawSlotHighlightFront(context);
        }

        //GameItemAnnotator
        if (hoveredSlot.getItem() != Items.AIR) {
            Optional<WynnItem> item = asWynnItem(hoveredSlot); //asWynnItem(hoveredSlot);
            List<Text> tooltip;
            if (item.isPresent()) {
                tooltip = TooltipUtils.getWynnItemTooltip(hoveredSlot, item.get());
                if (tooltip.isEmpty()) {
                    tooltip = hoveredSlot.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.ADVANCED);
                }
            } else {
                tooltip = hoveredSlot.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.ADVANCED);
            }
            context.drawTooltip(screen.getTextRenderer(), tooltip, mouseX, mouseY);
        }


        if (heldItem != null) {
            int guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue() + 1;
            String amountString = heldItem.getCount() == 1 ? "" : String.valueOf(heldItem.getCount());
            //((HandledScreenInvoker) screen).invokeDrawItem(context, heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale, String.valueOf(heldItem.getCount()));
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 300);
            context.drawItem(heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale);
            context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, heldItem, mouseX - 2 * guiScale, mouseY - 2 * guiScale, amountString);
//            context.drawItem(heldItem, (int) (scaledMouseX - 8 * 3 * widthFactor), (int) (scaledMouseY - 8 * 3 * widthFactor), 0);
//            context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, heldItem, (int) (scaledMouseX - 8 * 3 * widthFactor), (int) (scaledMouseY - 8 * 3 * widthFactor), String.valueOf(heldItem.getCount()));
            context.getMatrices().pop();
        }
    }


    /*@Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getSlotAt(DD)Lnet/minecraft/screen/slot/Slot;"
            )
    )
    private Slot redirectGetSlotAt(HandledScreen<?> instance, double mouseX, double mouseY) {
        if(BankOverlay.isBank) {
            int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
            float scale = (float) screenWidth / MinecraftClient.getInstance().getWindow().getWidth(); // oder screenHeight / 1080f
            scale *= MinecraftClient.getInstance().options.getGuiScale().getValue();
            double adjustedX = mouseX * scale;
            double adjustedY = mouseY * scale;

            return ((HandledScreenInvoker) instance).innvokeGetSlotAt(adjustedX, adjustedY);
        }
        else return ((HandledScreenInvoker) instance).innvokeGetSlotAt(mouseX, mouseY);
    }*/

    @Unique
    private long lastClickTime = 0;
    @Unique
    private long lastDoubleClick = 0;

    private int lastClickedSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (BankOverlay.isBank) cir.cancel();

        BankOverlay.activeTextInput = null;

        if (personalStorageButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            personalStorageButton.click();
            return;
        }

        if (quickStashButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            quickStashButton.click();
            return;
        }

        if (dumpExceptHotbarButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            dumpExceptHotbarButton.click();
            return;
        }

        if (dumpAllButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            dumpAllButton.click();
            return;
        }

        if ((!Searchbar.isActive() && Searchbar.isClickInBounds((int) mouseX, (int) mouseY))
                || Searchbar.isActive() && !Searchbar.isClickInBounds((int) mouseX, (int) mouseY)) {
            Searchbar.click();
            return;
        }

        boolean clickedAnyNameInput = false;
        for (int i = scrollOffset; i < xFitAmount * yFitAmount - (xFitAmount - 1) + scrollOffset - 1; i++) {
            if (i > 19) break;
            if ((!BankPageNameInputs.get(i).isActive() && BankPageNameInputs.get(i).isClickInBounds((int) mouseX, (int) mouseY))
                    || BankPageNameInputs.get(i).isActive() && !BankPageNameInputs.get(i).isClickInBounds((int) mouseX, (int) mouseY)) {
                BankPageNameInputs.get(i).click();
                if (BankPageNameInputs.get(i).isClickInBounds((int) mouseX, (int) mouseY)) {
                    clickedAnyNameInput = true;
                }
            }
        }
        if (clickedAnyNameInput) {
            return;
        }
        if (resetSearchButton.isClickInBounds((int) mouseX, (int) mouseY)) {
            resetSearchButton.click();
            return;
        }

//        if(textInputBoxWidget != null) {
//            textInputBoxWidget.mouseClicked(mouseX, mouseY, button);
//        }

        if (hoveredIndex == -1 || hoveredInvIndex == -1) {
            return;
        }

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        int xFitAmount = Math.min(3, Math.floorDiv(screenWidth, 162)); //Max amount is 3
        int yFitAmount = Math.min(4, Math.floorDiv(screenHeight, 90));
        int playerInvIndex = xFitAmount * yFitAmount - xFitAmount;
        System.out.println("xFitAmount " + xFitAmount + " yFitAmount: " + yFitAmount + "PlayerInvIndex = " + playerInvIndex);

        if (BankOverlay.isBank && !shouldWait) {
            SlotActionType actionType = SlotActionType.PICKUP;
            long now = System.currentTimeMillis();
            if (heldItem != null) {
                if (now - lastClickTime < 250 && heldItem.getItem() != Items.AIR && (lastClickedSlot == hoveredIndex || lastClickedSlot == hoveredIndex + 54)) {
                    System.out.println("DoubleClick");
                    lastDoubleClick = now;
                    actionType = SlotActionType.PICKUP_ALL;
                } //double click stuff
            }
            lastClickTime = now;


            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                actionType = SlotActionType.QUICK_MOVE;
                annotationCache.get(activeInv).clear();
            }
            System.out.println("Click in: " + (hoveredInvIndex) + " playerInvIndex " + playerInvIndex + " activeInv " + activeInv);
            if (hoveredInvIndex == activeInv) {
                ItemStack oldHeld = heldItem;
                heldItem = getHeldItem(hoveredIndex, actionType, button); //heldItem = McUtils.mc().player.currentScreenHandler.slots.get(hoveredIndex).getStack().copy();
                if(oldHeld != null) {
                    if ((oldHeld.getItem() == Items.EMERALD ||
                            oldHeld.getItem() == Items.EMERALD_BLOCK ||
                            oldHeld.getItem() == Items.EXPERIENCE_BOTTLE) &&
                            heldItem.getCustomName().getString().contains("Pouch")) {
                        heldItem = null;
                    }
                }
                //System.out.println("Clicked: " + heldItem.getName() + " hoveredIndex: " + hoveredInvIndex);

                MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, hoveredIndex, button, actionType, MinecraftClient.getInstance().player);
                annotationCache.get(activeInv).clear();
                lastClickedSlot = hoveredIndex;
                cir.cancel();
                return;
            } else if (hoveredInvIndex == playerInvIndex + scrollOffset) { //i know this is ugly i wanted to do it with a variable but that somehow didnt work dont ask me why
                System.out.println("playerinv click");
                ItemStack oldHeld = heldItem;
                heldItem = getHeldItem(hoveredIndex + 54, actionType, button); //McUtils.mc().player.currentScreenHandler.slots.get(hoveredIndex + 54).getStack().copy();
                if(oldHeld != null) {
                    if ((oldHeld.getItem() == Items.EMERALD ||
                            oldHeld.getItem() == Items.EMERALD_BLOCK ||
                            oldHeld.getItem() == Items.EXPERIENCE_BOTTLE) &&
                            heldItem.getCustomName().getString().contains("Pouch")) {
                        heldItem = null;
                    }
                }
                //System.out.println("Clicked: " + heldItem.getName() + " hoveredIndex: " + hoveredInvIndex);

                MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, hoveredIndex + 54, button, actionType, MinecraftClient.getInstance().player);

                annotationCache.get(playerInvIndex).clear();
                lastClickedSlot = hoveredIndex + 54;
                cir.cancel();
                return;
            } else {
                if(heldItem.getItem() != Items.AIR) {
                    cir.cancel();
                    return;
                }
                int clickedPage = hoveredInvIndex + 1;
                if (clickedPage <= lastPage) {
                    List<ItemStack> stacks = new ArrayList<>();
                    for (Slot slot : BankOverlay.activeInvSlots) {
                        stacks.add(slot.getStack());
                    }
                    Pages.BankPages.put(activeInv, stacks);
                    activeInv = hoveredInvIndex;
                    BankOverlay.PersonalStorageUtils.jumpToDestination(clickedPage);
                } else {
                    System.out.println("NOT BOUGHT");
                    if (activeInv != lastPage - 1) {
                        activeInv = lastPage - 1;
                        BankOverlay.PersonalStorageUtils.jumpToDestination(lastPage);
                        System.out.println("a");
                        buyPageStack = null;
                    } else {
                        System.out.println("b");
                        Slot pageBuySlot = McUtils.containerMenu().getSlot(52);
                        ItemStack newStack = pageBuySlot.getStack();
                        if (buyPageStack == null) {
                            buyPageStack = newStack;
                            buyPageStageText = "Click again to buy Page " + (lastPage + 1) + ".";
                        } else if (buyPageStack.equals(newStack)) {
                            System.out.println("ITS THE SAME, CLICKING");
                            Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap();
                            changedSlots.put(52, new ItemStack(Items.AIR));
                            McUtils.sendPacket(new ClickSlotC2SPacket(bankSyncid, 0, 52, 0, SlotActionType.PICKUP, buyPageStack, changedSlots));
                            buyPageStageText = "Click again to confirm.";
                            //ContainerUtils.clickOnSlot(52, bankSyncid, 0, );
                            //MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, pageBuySlot.id, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
                            //System.out.println(buyPageStack.equals(newStack));
                        } else {
                            Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap();
                            changedSlots.put(52, new ItemStack(Items.AIR));
                            McUtils.sendPacket(new ClickSlotC2SPacket(bankSyncid, 0, 52, 0, SlotActionType.PICKUP, buyPageStack, changedSlots));
                            lastPage++;
                            System.out.println("ITS DIFFERENT");
                            buyPageStageText = "NOT BOUGHT";
                        }

                        System.out.println(Objects.requireNonNull(McUtils.containerMenu().getSlot(52).getStack().getCustomName()).getString().contains(">§4>§c>§4>§c>"));
                    }
//                    if(activeInv != lastPage - 1) {
//                        List<ItemStack> stacks = new ArrayList<>();
//                        for (Slot slot : BankOverlay.activeInvSlots) {
//                            stacks.add(slot.getStack());
//                        }
//                        WynnExtrasConfig.INSTANCE.BankPages.put(activeInv, stacks);
//                        activeInv = lastPage - 1;
//                        BankOverlay.PersonalStorageUtils.jumpToDestination(lastPage);
//                    } else {
//                        System.out.println("buy?");
//                    }
                }
            }
            if (actionType == SlotActionType.QUICK_MOVE) {
                heldItem = null;
            }
            cir.cancel();
        }
    }

    @Unique
    private static ItemStack getHeldItem(int index, SlotActionType type, int mouseButton) {
        ItemStack heldItem = null;
        if (mouseButton == 0) { //Left Click
            switch (type) {
                case SlotActionType.PICKUP -> {
                    heldItem = McUtils.mc().player.currentScreenHandler.slots.get(index).getStack().copy();
                    if (heldItem != null && HandledScreenMixin.heldItem != null) {
                        if (ItemStack.areItemsAndComponentsEqual(heldItem, HandledScreenMixin.heldItem)) {
                            if (HandledScreenMixin.heldItem.getCount() + heldItem.getCount() <= heldItem.getMaxCount()) {
                                heldItem = Items.AIR.getDefaultStack();
                            } else {
                                int spaceLeft = HandledScreenMixin.heldItem.getMaxCount() - HandledScreenMixin.heldItem.getCount();
                                heldItem = HandledScreenMixin.heldItem;
                                heldItem.setCount(heldItem.getCount() - spaceLeft);
                            }
                        }
                    }
                }
                case SlotActionType.PICKUP_ALL -> {
                    ItemStack currentStack = HandledScreenMixin.heldItem;
                    if (currentStack.getCount() == currentStack.getMaxCount()) {
                        heldItem = currentStack;
                        break;
                    }
                    int newAmount = HandledScreenMixin.heldItem.getCount();
                    for (Slot slot : McUtils.mc().player.currentScreenHandler.slots) {
                        if (ItemStack.areItemsAndComponentsEqual(slot.getStack(), currentStack)) {
                            newAmount += slot.getStack().getCount();
                            System.out.println("new amount is " + newAmount);
                            if (newAmount > currentStack.getMaxCount()) {
                                newAmount = currentStack.getMaxCount();
                                break;
                            }
                        }
                    }
                    currentStack.setCount(newAmount);
                    heldItem = currentStack;
                }
                case SlotActionType.QUICK_MOVE -> {
                    heldItem = Items.AIR.getDefaultStack();
                }
            }
        } else { //Right Click
            heldItem = HandledScreenMixin.heldItem;
            if(heldItem != null) {
                if (heldItem.getItem() == Items.AIR) {
                    heldItem = McUtils.mc().player.currentScreenHandler.slots.get(index).getStack().copy();
                    if (heldItem.getCount() % 2 == 0) {
                        heldItem.setCount(heldItem.getCount() / 2);
                    } else {
                        heldItem.setCount(heldItem.getCount() / 2 + 1);
                    }
                } else if (heldItem.getCount() == 1) {
                    heldItem = Items.AIR.getDefaultStack();
                } else {
                    heldItem.setCount(heldItem.getCount() - 1);
                }
            }
        }
        return heldItem;
    }

//        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
//        //int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
//        //float scale = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
//        float widthFactor = (float) screenWidth / MinecraftClient.getInstance().getWindow().getWidth();
//        float scale = widthFactor *  MinecraftClient.getInstance().options.getGuiScale().getValue();
//        if(BankOverlay.isBank) {
//            for (int i = 0; i < 10; i++) {
//                int inventoryOffsetX;
//                int inventoryOffsetY;
//                if(i == 9) {
//                    inventoryOffsetX = (int) (offsetX - 175 * 3 * widthFactor);
//                    inventoryOffsetY = (int) (offsetY - 300 * 3 * widthFactor);
//                } else {
//                    inventoryOffsetX = (int) (offsetX - (i % 3) * 175 * 3 * widthFactor);
//                    inventoryOffsetY = (int) (offsetY - Math.floorDiv(i, 3) * 100 * 3 * widthFactor);
//                }
//                if(i == 9) {
//                    if(mouseX < ((HandledScreenAccessor) MinecraftClient.getInstance().currentScreen).getX() - inventoryOffsetX) { continue; }
//                    if(mouseX > ((HandledScreenAccessor) MinecraftClient.getInstance().currentScreen).getX() - inventoryOffsetX + 175 * 3 * widthFactor ) { continue; }
//                    if(mouseY < - inventoryOffsetY + 130 * 3 * widthFactor ) { continue; }
//                    if(mouseY > - inventoryOffsetY + 229 * 3 * widthFactor ) { continue; }
//
//                    SlotActionType actionType = SlotActionType.PICKUP;
//
//
//                    long now = System.currentTimeMillis();
//                    if (now - lastClickTime < 250 && heldItem != null && lastClickedSlot == hoveredIndex) {
//                        lastDoubleClick = now;
//                        actionType = SlotActionType.PICKUP_ALL;
//                    } //double click stuff
//
//                    lastClickTime = now;
//
//
//                    if(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
//                        actionType = SlotActionType.QUICK_MOVE;
//                    }
//                    heldItem = McUtils.mc().player.currentScreenHandler.slots.get(54 + hoveredIndex).getStack().copy();
//                    MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, 54 + hoveredIndex, button, actionType, MinecraftClient.getInstance().player);
//                    lastClickedSlot = hoveredIndex;
//                } else {
//                    if(mouseX < ((HandledScreenAccessor) MinecraftClient.getInstance().currentScreen).getX() - inventoryOffsetX) { continue; }
//                    if(mouseX > ((HandledScreenAccessor) MinecraftClient.getInstance().currentScreen).getX() - inventoryOffsetX + 175 * 3 * widthFactor ) { continue; }
//                    if(mouseY < - inventoryOffsetY + 130 * 3 * widthFactor ) { continue; }
//                    if(mouseY > - inventoryOffsetY + 230 * 3 * widthFactor ) { continue; }
//
//                    int oldActiveInv = activeInv;
//                    activeInv = i;
//
//                    if(oldActiveInv == activeInv) {
//                        SlotActionType actionType = SlotActionType.PICKUP;
//
//                        long now = System.currentTimeMillis();
//                        if (now - lastClickTime < 250 && heldItem != null && lastClickedSlot == hoveredIndex) {
//                            lastDoubleClick = now;
//                            actionType = SlotActionType.PICKUP_ALL;
//                        } //double click stuff
//
//                        lastClickTime = now;
//
//
//                        if(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
//                            actionType = SlotActionType.QUICK_MOVE;
//                        }
//                        if(button == 0) {
//                            heldItem = McUtils.mc().player.currentScreenHandler.slots.get(hoveredIndex).getStack().copy();
//                        } else if(heldItem.getItem() == Items.AIR) {
//                            heldItem = McUtils.mc().player.currentScreenHandler.slots.get(hoveredIndex).getStack().copy();
//                            heldItem.setCount(heldItem.getCount()/2);
//                        } else {
//                            heldItem.setCount(heldItem.getCount() - 1);
//                        }
//
//                        MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, hoveredIndex, button, actionType, MinecraftClient.getInstance().player);
//                        lastClickedSlot = hoveredIndex;
//                        break;
//                    }
//
//                    List<ItemStack> stacks = new ArrayList<>();
//                    for(Slot slot : BankOverlay.activeInvSlots) {
//                        stacks.add(slot.getStack());
//                    }
//                    WynnarschConfig.INSTANCE.BankPages.put(oldActiveInv, stacks);
//
//                    BankOverlay.PersonalStorageUtils.jumpToDestination(i + 1);
//                }
//                cir.cancel();
//                return;
//            }
//        }


    @Unique
    private boolean isDragging = false;
    @Unique
    private double dragStartX = 0;
    @Unique
    private double dragStartY = 0;
    @Unique
    private Set<Integer> draggedSlotIds = new HashSet<>();
    @Unique
    private Slot startSlot;
    @Unique
    private ItemStack draggedStack = ItemStack.EMPTY;

    /*@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        double dx = Math.abs(mouseX - dragStartX);
        double dy = Math.abs(mouseY - dragStartY);
        if (dx > 5 || dy > 5) { // z.B. ab 5 Pixel Bewegung
            isDragging = true;
        }
        if (BankOverlay.isBank && isDragging && button == 1) {
            double adjustedX = (mouseX);
            double adjustedY = (mouseY);
            Slot slot = ((HandledScreenInvoker) this).innvokeGetSlotAt(adjustedX, adjustedY);
            boolean canPlace;
            if(slot == startSlot) {
                cir.setReturnValue(true);
                return;
            }
            if (heldItem != null && slot != null) {
                 canPlace = slot.getStack().getItem() == heldItem.getItem() || slot.getStack().getItem() == Items.AIR;
            } else {
                canPlace = false;
            }
            if (slot != null && !draggedSlotIds.contains(slot.id) && canPlace) {
                draggedSlotIds.add(slot.id);
                MinecraftClient.getInstance().interactionManager.clickSlot(BankOverlay.bankSyncid, slot.id, 1, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
            }
            cir.setReturnValue(true);
        }
    }*/

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (BankOverlay.isBank) {

                    /*
            int totalSlots = draggedSlotIds.size();
            if (totalSlots == 0 || draggedStack.isEmpty()) return;

            int perSlot = draggedStack.getCount() / totalSlots;
            int remainder = draggedStack.getCount() % totalSlots;

            int count = 0;
            for (int slotId : draggedSlotIds) {
                int amount = perSlot + (count < remainder ? 1 : 0);
                MinecraftClient.getInstance().interactionManager.clickSlot(
                        BankOverlay.bankSyncid, slotId, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player
                );
                count++;
            }*/ //left click drag not working

            isDragging = false;
            draggedSlotIds.clear();
            draggedStack = ItemStack.EMPTY;
            cir.cancel();
        }
    }


    @Inject(method = "isClickOutsideBounds", at = @At("HEAD"), cancellable = true)
    private void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> cir) {
        if (BankOverlay.isBank) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Unique
    public <T extends WynnItem> Optional<T> asWynnItem(ItemStack itemStack) {

        Optional<ItemAnnotation> annotationOpt = ItemHandler.getItemStackAnnotation(itemStack);
        if(annotationOpt.isEmpty()) return Optional.empty();
//        if (annotationOpt.isEmpty()) {
//            ItemAnnotation calculatedAnnotation = ((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(itemStack, StyledText.fromComponent(itemStack.getName()).getNormalized());
//            if(calculatedAnnotation == null) return Optional.empty();
//            annotationOpt = Optional.of(calculatedAnnotation);
//        }
        //Optional<ItemAnnotation> annotationOpt = Optional.ofNullable(((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(itemStack, StyledText.fromComponent(itemStack.getName())));

        //if (annotationOpt.isEmpty()) annotationOpt = Optional.ofNullable(((ItemHandlerInvoker) (Object) Handlers.Item).invokeCalculateAnnotation(itemStack, StyledText.fromComponent(itemStack.getName())));

        if (!(annotationOpt.get() instanceof WynnItem wynnItem)) return Optional.empty();
        return Optional.of((T) wynnItem);
    }

    @Unique
    public void drawDynamicNameSign(DrawContext context, String input, int x, int y) {
        if (signMids.isEmpty()) {
            signMids.add(signMid1);
            signMids.add(signMid2);
            signMids.add(signMid3);
        }
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int strWidth = textRenderer.getWidth(input);
        int strMidWidth = strWidth - 15;
        int amount = Math.max(0, Math.ceilDiv(strMidWidth, 10));
        RenderUtils.drawTexturedRect(context.getMatrices(), signLeft, x/* + 54*/, y - 13, 10, 15, 10, 15);
        if (strWidth > 15) {
            for (int i = 0; i < amount; i++) {
                RenderUtils.drawTexturedRect(context.getMatrices(), signMids.get(i % 3), x + 10 + 10 * i/* + 54*/, y - 13, 10, 15, 10, 15);
            }
        }
        RenderUtils.drawTexturedRect(context.getMatrices(), signRight, x + 10 + 10 * amount/* + 54*/, y - 13, 10, 15, 10, 15);

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

        String InventoryTitle = currScreen.getTitle().getString();
        if (InventoryTitle.equals("\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF000")) {
            heldItem = Items.AIR.getDefaultStack();

            List<ItemStack> stacks = new ArrayList<>();
            for (Slot slot : BankOverlay.activeInvSlots) {
                stacks.add(slot.getStack());
            }
            Pages.BankPages.put(activeInv, stacks);

            BankOverlayData.save();
        }
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

        //int[] emeraldsInUnits = Models.Emerald.emeraldsPerUnit(emeraldAmount);
        //System.out.println(emerandsInUnits[3] + "stx " + emerandsInUnits[2] + "le " + emerandsInUnits[1] + "eb " + emerandsInUnits[0] + "em");
        //emeraldCountFeature.renderTexturedCount()

}


    //TODO: Cleanup
    //TODO: WYNNTILS TOOLTIPS ANZEIGEN

