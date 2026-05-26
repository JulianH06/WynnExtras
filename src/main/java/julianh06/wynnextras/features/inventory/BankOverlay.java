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
import julianh06.wynnextras.event.WorldChangeEvent;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.bankoverlay.BankOverlaySlotBridge;
import julianh06.wynnextras.features.inventory.data.*;
import julianh06.wynnextras.features.misc.ClassSelectionOverlay;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@WEModule
public class BankOverlay {
    private static final Pattern CHARACTER_ID_PATTERN = Pattern.compile("^[a-z0-9]{8}$");
    private static final Pattern MINECRAFT_FORMATTING_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]");
    public static final DefaultedList<Slot> playerInvSlots = DefaultedList.of();
    public static final DefaultedList<Slot> activeInvSlots = DefaultedList.of();
    private static PersonalStorageUtilitiesFeature personalStorageUtils;

    public static BankData Pages;

    public static int activeInv = -1;

    public static ItemStack heldItem = Items.AIR.getDefaultStack();

    public static final Map<Integer, List<ItemAnnotation>> annotationCache = new HashMap<>();

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms

    private static EasyTextInput activeTextInput;

    public static volatile BankOverlayType currentOverlayType = BankOverlayType.NONE;
    public static volatile BankOverlayType expectedOverlayType = BankOverlayType.NONE;
    public static BankData currentData;
    public static String currentCharacterID;
    private static int currentMaxPages;

    public static boolean shouldWait = false;
    public static long shouldWaitSince = 0L;

    private static final boolean FORCE_MISSING_CHARACTER_ID_FOR_TESTING = false;

    private static boolean registeredScroll = false;

    public static PersonalStorageUtilitiesFeature getPersonalStorageUtils() {
        return personalStorageUtils;
    }

    public static void setPersonalStorageUtils(PersonalStorageUtilitiesFeature feature) {
        personalStorageUtils = feature;
    }

    public static void setActiveTextInput(EasyTextInput textInput) {
        activeTextInput = textInput;
    }

    public static void resetScrollRegistration() {
        registeredScroll = false;
    }

    public static int getCurrentMaxPages() {
        return currentMaxPages;
    }

    public static boolean hasValidCurrentCharacterId() {
        if (FORCE_MISSING_CHARACTER_ID_FOR_TESTING) return false;
        return currentCharacterID != null && !currentCharacterID.isBlank() && !"null".equalsIgnoreCase(currentCharacterID);
    }

    public static boolean isCharacterBankMissingCharacterId() {
        return currentOverlayType == BankOverlayType.CHARACTER && !hasValidCurrentCharacterId();
    }

    public static boolean syncCurrentCharacterIdFromWynntils() {
        String characterId = getCurrentCharacterIdFromCompass();
        if (characterId == null) {
            characterId = Models.Character.getId();
        }
        if (characterId == null || characterId.isBlank() || "-".equals(characterId) || "null".equalsIgnoreCase(characterId)) {
            return false;
        }
        if (characterId.equals(currentCharacterID)) {
            return true;
        }

        currentCharacterID = characterId;
        Pages = null;
        activeInvSlots.clear();
        annotationCache.clear();
        CharacterBankData.INSTANCE.load();
        if (currentOverlayType == BankOverlayType.CHARACTER) {
            currentData = CharacterBankData.INSTANCE;
        }
        WynnExtras.LOGGER.info("[WynnExtras] Synced character bank id from Wynntils: " + characterId);
        return true;
    }

    private static String getCurrentCharacterIdFromCompass() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return null;
        ItemStack compass = client.player.getInventory().getStack(7);
        if (compass == null || compass.isEmpty() || compass.getComponents() == null) return null;

        LoreComponent lore = compass.getComponents().get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) return null;

        for (Text line : lore.lines()) {
            String text = MINECRAFT_FORMATTING_CODE_PATTERN.matcher(line.getString()).replaceAll("").trim();
            if (CHARACTER_ID_PATTERN.matcher(text).matches()) {
                return text;
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onInput(KeyInputEvent event) {
        if (ClassSelectionOverlay.handleKeyInput(event)) return;
        if(event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            BankOverlay2.handleKeyPressed(event.getKey(), event.getScanCode(), event.getModifiers());
        }
        if(activeTextInput != null) {
            activeTextInput.onInput(event);
        }
    }

    @SubscribeEvent
    public void onChar(CharInputEvent event) {
        if (ClassSelectionOverlay.handleCharInput(event)) return;
        // Don't insert character if Ctrl is held (it's a shortcut like Ctrl+V)
        if (!isCtrlHeld()) {
            BankOverlay2.handleCharTyped(event.getCharacter());
        }
        if(activeTextInput != null && !isCtrlHeld()) {
                activeTextInput.onCharInput(event);
            }

    }

    private static boolean isCtrlHeld() {
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || org.lwjgl.glfw.GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        BankOverlay2.saveCurrentPlayerInventorySnapshot();
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
                BankOverlay.syncCurrentCharacterIdFromWynntils();
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
            if(expectedOverlayType != BankOverlayType.NONE && expectedOverlayType != currentOverlayType) return;

            String InventoryTitle = currScreen.getTitle().getString();
            if(InventoryTitle == null) { return; }

            if(BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                registeredScroll = true;
                ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                        screen,
                        mX,
                        mY,
                        horizontalAmount,
                        verticalAmount,
                        consumed
                ) -> {
                    long now = System.currentTimeMillis();
                    if (now - lastScrollTime < scrollCooldown) {
                        return true;
                    }
                    lastScrollTime = now;

                    if (BankOverlay.currentOverlayType != BankOverlayType.NONE) {
                        if (verticalAmount > 0) {
                            BankOverlay2.adjustTargetOffset(-104f);
                        } else {
                            BankOverlay2.adjustTargetOffset(104f);
                        }
                    }
                    return true;
                });
            }
            BankOverlay2.setBankSyncId(currScreenHandler.syncId);

            //most (almost all) of the functionality is in HandledScreenMixin
        });
    }

    @SubscribeEvent
    public void onWorldChange(WorldChangeEvent event) {
        currentOverlayType = BankOverlayType.NONE;
        expectedOverlayType = BankOverlayType.NONE;
        currentData = null;
        Pages = null;
        activeInv = -1;
        currentCharacterID = null;
        activeInvSlots.clear();
        annotationCache.clear();
        heldItem = Items.AIR.getDefaultStack();
        registeredScroll = false;
        BankOverlay2.invalidateBagTotalCache();
        BankOverlaySlotBridge.restoreAll();
    }
}
