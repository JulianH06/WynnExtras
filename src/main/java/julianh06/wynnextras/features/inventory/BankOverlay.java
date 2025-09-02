package julianh06.wynnextras.features.inventory;

import com.wynntils.features.inventory.PersonalStorageUtilitiesFeature;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.event.CharInputEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.SubscribeEvent;

@WEModule
public class BankOverlay {
    public static boolean isBank = false;
    public static DefaultedList<Slot> playerInvSlots = DefaultedList.of();
    public static DefaultedList<Slot> activeInvSlots = DefaultedList.of();
    public static int bankSyncid;
    public static PersonalStorageUtilitiesFeature PersonalStorageUtils;

    public static int scrollOffset = 0;

    public static int xFitAmount;
    public static int yFitAmount;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms

    public static boolean canScrollFurther = true;

    public static EasyTextInput activeTextInput;

    private static final WynnExtrasConfig config = SimpleConfig.getInstance(WynnExtrasConfig.class);

    public static String Bankname = "\uDAFF\uDFF0\uE00F\uDAFF\uDF68\uF000";

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
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
                isBank = false;
                return;
            }

            String InventoryTitle = currScreen.getTitle().getString();
            if(InventoryTitle == null) { return; }

            if(config.toggleBankOverlay) {
                isBank = InventoryTitle.equals(Bankname);
            } else {
                isBank = false;
            }

            if(isBank) {
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

                    if (BankOverlay.isBank) {
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
