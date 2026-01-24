package julianh06.wynnextras.features.inventory;

import com.wynntils.core.components.Models;
import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import com.wynntils.handlers.item.ItemAnnotation;
import com.wynntils.models.containers.Container;
import com.wynntils.models.containers.containers.personal.AccountBankContainer;
import com.wynntils.models.containers.containers.personal.BookshelfContainer;
import com.wynntils.models.containers.containers.personal.CharacterBankContainer;
import com.wynntils.models.containers.containers.personal.MiscBucketContainer;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.data.*;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WEModule
public class BankOverlay {
    public static DefaultedList<Slot> playerInvSlots = DefaultedList.of();
    public static DefaultedList<Slot> activeInvSlots = DefaultedList.of();
    public static int bankSyncid;
    public static PersonalStorageUtilitiesFeature PersonalStorageUtils;

    public static BankData Pages;

    public static int activeInv = -1;

    public static ItemStack heldItem = Items.AIR.getDefaultStack();

    public static Map<Integer, List<ItemAnnotation>> annotationCache = new HashMap<>();

    public static int xFitAmount;
    public static int yFitAmount;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms

    public static boolean canScrollFurther = true;

    public static EasyTextInput activeTextInput;

    public volatile static BankOverlayType currentOverlayType = BankOverlayType.NONE;
    public volatile static BankOverlayType expectedOverlayType = BankOverlayType.NONE;
    public static BankData currentData;
    public static String currentCharacterID;
    public static int currentMaxPages;

    public static boolean shouldWait = false;

    public static EnumMap<BankOverlayType, HashMap<Integer, EasyTextInput>> BankPageNameInputsByType = new EnumMap<>(BankOverlayType.class);

    public static float pageBuyCustomModelData = 0;

    public static boolean registeredScroll = false;

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        if(BankOverlay2.searchbar2 != null && (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT)) {
            BankOverlay2.searchbar2.keyPressed(event.getKey(), event.getScanCode(), 0);
        }
        for(BankOverlay2.PageWidget page : BankOverlay2.pages) {
            if((event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT)) {
                page.keyPressed(event.getKey(), event.getScanCode(), 0);
            }
        }
        if(activeTextInput != null) {
            activeTextInput.onInput(event);
        }
    }

    @SubscribeEvent
    public void onChar(CharInputEvent event) {
        if(BankOverlay2.searchbar2 != null) {
            BankOverlay2.searchbar2.charTyped(event.getCharacter(), 0);
        }
        for(BankOverlay2.PageWidget page : BankOverlay2.pages) {
            page.charTyped(event.getCharacter(), 0);
        }
        if(activeTextInput != null) {
            activeTextInput.onCharInput(event);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(expectedOverlayType == BankOverlayType.NONE) return;
        if(expectedOverlayType == currentOverlayType) {
            activeInvSlots.clear();
            annotationCache.clear();
            expectedOverlayType = BankOverlayType.NONE;
            return;
        }
        updateOverlayType();
    }

    public static void updateOverlayType() {
        Container container = Models.Container.getCurrentContainer();
        switch (container) {
            case AccountBankContainer accountBankContainer -> {
                BankOverlay.currentOverlayType = BankOverlayType.ACCOUNT;
                BankOverlay.currentData = AccountBankData.INSTANCE;
                currentMaxPages = 21;
            }
            case CharacterBankContainer characterBankContainer -> {
                BankOverlay.currentOverlayType = BankOverlayType.CHARACTER;
                BankOverlay.currentData = CharacterBankData.INSTANCE;
                currentMaxPages = 12;
            }
            case BookshelfContainer bookshelfContainer -> {
                BankOverlay.currentOverlayType = BankOverlayType.BOOKSHELF;
                BankOverlay.currentData = BookshelfData.INSTANCE;
                currentMaxPages = 12;
            }
            case MiscBucketContainer miscBucketContainer -> {
                BankOverlay.currentOverlayType = BankOverlayType.MISC;
                BankOverlay.currentData = MiscBucketData.INSTANCE;
                currentMaxPages = 12;
            }
            case null, default -> {
                BankOverlay.currentOverlayType = BankOverlayType.NONE;
                BankOverlay.currentData = null;
            }
        }
    }

    public static void registerBankOverlay() {
        WynnExtras.LOGGER.info("Registering Bankoverlay for " + WynnExtras.MOD_ID);

        ClientTickEvents.START_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null) { return; }

            ScreenHandler currScreenHandler = McUtils.containerMenu();

            Screen currScreen = McUtils.mc().currentScreen;
            if(currScreen == null) {
                registeredScroll = false;
                return;
            }

            if(registeredScroll) return;
            if(expectedOverlayType != BankOverlayType.NONE) {
                if (expectedOverlayType != currentOverlayType) return;
            }
            String InventoryTitle = currScreen.getTitle().getString();
            if(InventoryTitle == null) { return; }

            if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                registeredScroll = true;
                ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                        screen,
                        mX,
                        mY,
                        horizontalAmount,
                        verticalAmount
                ) -> {
                    long now = System.currentTimeMillis();
                    if (now - lastScrollTime < scrollCooldown) {
                        return;
                    }
                    lastScrollTime = now;

                    if (BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                        if (verticalAmount > 0) {
                            BankOverlay2.targetOffset -= 104f;
                        } else if(canScrollFurther) {
                            BankOverlay2.targetOffset += 104f;
                        }
                    }
                });
            }
            bankSyncid = currScreenHandler.syncId;

            //most (almost all) of the functionality is in HandledScreenMixin
        });
    }
}
