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
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.inventory.data.*;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Unique;

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

    public static int scrollOffset = 0;

    public static BankData Pages;

    public static int activeInv = -1;

    public static Long timeSinceSwitch = 0L;

    public static ItemStack heldItem = Items.AIR.getDefaultStack();

    public static Map<Integer, List<ItemAnnotation>> annotationCache = new HashMap<>();

    public static int xFitAmount;
    public static int yFitAmount;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms

    public static boolean canScrollFurther = true;

    public static EasyTextInput activeTextInput;

    public static BankOverlayType currentOverlayType = BankOverlayType.NONE;
    public static BankOverlayType expectedOverlayType = BankOverlayType.NONE;
    public static BankData currentData;
    public static String currentCharacterID;
    public static int currentMaxPages;

    public static HashMap<Integer, EasyTextInput> BankPageNameInputs = new HashMap<>();
    public static EnumMap<BankOverlayType, HashMap<Integer, EasyTextInput>> BankPageNameInputsByType = new EnumMap<>(BankOverlayType.class);


    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        if(!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) && SimpleConfig.getInstance(WynnExtrasConfig.class).enableScrollWithArrowKeys) {
            if (event.getKey() == GLFW.GLFW_KEY_UP && event.getAction() == GLFW.GLFW_PRESS) {
                if (BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                    scrollOffset -= xFitAmount; //Scroll up
                    if (scrollOffset < 0) {
                        scrollOffset = 0;
                    }
                }
            }
            if (event.getKey() == GLFW.GLFW_KEY_DOWN && event.getAction() == GLFW.GLFW_PRESS) {
                if (BankOverlay.currentOverlayType != BankOverlayType.NONE && canScrollFurther) {
                    scrollOffset += xFitAmount; //Scroll down
                }
            }
        }
        if(activeTextInput != null) {
            activeTextInput.onInput(event);
        }
    }

    @SubscribeEvent
    public void onChar(CharInputEvent event) {
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
        if (container instanceof AccountBankContainer) {
            BankOverlay.currentOverlayType = BankOverlayType.ACCOUNT;
            BankOverlay.currentData = AccountBankData.INSTANCE;
            currentMaxPages = 21;
            System.out.println("IS ACCOUNT BANK");
        } else if (container instanceof CharacterBankContainer) {
            BankOverlay.currentOverlayType = BankOverlayType.CHARACTER;
            BankOverlay.currentData = CharacterBankData.INSTANCE;
            currentMaxPages = 12;
            System.out.println("IS CHARACTER BANK");
        } else if (container instanceof BookshelfContainer) {
            BankOverlay.currentOverlayType = BankOverlayType.BOOKSHELF;
            BankOverlay.currentData = BookshelfData.INSTANCE;
            currentMaxPages = 12;
            System.out.println("IS BOOKSHELF");
        } else if (container instanceof MiscBucketContainer) {
            BankOverlay.currentOverlayType = BankOverlayType.MISC;
            BankOverlay.currentData = MiscBucketData.INSTANCE;
            currentMaxPages = 12;
            System.out.println("IS MISC BUCKET");
        } else {
            BankOverlay.currentOverlayType = BankOverlayType.NONE;
            BankOverlay.currentData = null;
        }
    }

    public static void registerBankOverlay() {
        String Bucketname = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68"; //
        String Tomename = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68"; //both currently broken i think
        WynnExtras.LOGGER.info("Registering Bankoverlay for " + WynnExtras.MOD_ID);

        ClientTickEvents.START_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null) { return; }

            ScreenHandler currScreenHandler = McUtils.containerMenu();

            Screen currScreen = McUtils.mc().currentScreen;
            if(currScreen == null) {
                return;
            }

            String InventoryTitle = currScreen.getTitle().getString();
            if(InventoryTitle == null) { return; }

            if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
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
                            scrollOffset -= xFitAmount; //Scroll up
                        } else if(canScrollFurther) {
                            scrollOffset += xFitAmount; //Scroll down
                        }
                        if (scrollOffset < 0) {
                            scrollOffset = 0;
                        }
                    }
                });
            }
            bankSyncid = currScreenHandler.syncId;

            //most (almost all) of the functionality is in HandledScreenMixin
        });
    }
}
