package julianh06.wynnextras.features.wci;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.wci.cart.ShoppingCart;
import julianh06.wynnextras.features.wci.service.ShoppingCartService;
import julianh06.wynnextras.features.wci.service.WciCartPersistenceService;
import julianh06.wynnextras.features.wci.service.WynnBuilderDecoder;
import julianh06.wynnextras.features.wci.ui.WciScreenContext;
import julianh06.wynnextras.features.wci.ui.WciShoppingMenuExtension;
import julianh06.wynnextras.features.wci.ui.WciShoppingMenuLauncherButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WEModule
public final class WynnExtrasWciFeature {
    private static final SubCommand ENABLE_COMMAND = new SubCommand("enable", "", ctx -> {
        send(WciShoppingMenuExtension.showFromCommand(currentScreenContext()).message());
        return 1;
    }, null, null);
    private static final SubCommand DISABLE_COMMAND = new SubCommand("disable", "", ctx -> {
        WciShoppingMenuExtension.close();
        send("WCI shopping menu disabled.");
        return 1;
    }, null, null);
    private static final SubCommand CLEAR_COMMAND = new SubCommand("clear", "", ctx -> {
        boolean saved = WciShoppingMenuExtension.clearFromCommand();
        send(saved ? "Cleared WCI shopping cart." : "Cleared WCI shopping cart, but save failed.");
        return 1;
    }, null, null);
    private static final SubCommand COPY_COMMAND = new SubCommand("copy", "", ctx -> {
        WciShoppingMenuExtension.copyFromCommand();
        send("Copied WCI shopping list.");
        return 1;
    }, null, null);
    private static final SubCommand RESET_POSITION_COMMAND = new SubCommand("resetposition", "", ctx -> {
        WciShoppingMenuExtension.resetPosition();
        WciShoppingMenuLauncherButton.resetPosition();
        send("WCI positions reset.");
        return 1;
    }, null, null);
    private static final Command WCI_COMMAND = new Command("wci", "", ctx -> {
        send(WciShoppingMenuExtension.toggleFromCommand(currentScreenContext()).message());
        return 1;
    }, List.of(ENABLE_COMMAND, DISABLE_COMMAND, CLEAR_COMMAND, COPY_COMMAND, RESET_POSITION_COMMAND), null);

    private static final ShoppingCartService SHOPPING_CART_SERVICE =
            new ShoppingCartService(new ShoppingCart(), new WynnBuilderDecoder());
    private static final WciCartPersistenceService PERSISTENCE = new WciCartPersistenceService();
    private static UUID loadedPlayerUuid;
    private static String pendingStatus;
    private static WciCartPersistenceService.SaveResult lastSaveResult = WciCartPersistenceService.SaveResult.successResult();

    static {
        SHOPPING_CART_SERVICE.setAfterMutation(WynnExtrasWciFeature::persistCurrentCart);
    }

    public WynnExtrasWciFeature() {}

    public static ShoppingCartService shoppingCartService() {
        ensureCartLoaded();
        return SHOPPING_CART_SERVICE;
    }

    public static synchronized void loadPersistedCart() {
        ensureCartLoaded();
    }

    public static synchronized Optional<String> consumePendingStatus() {
        String status = pendingStatus;
        pendingStatus = null;
        return Optional.ofNullable(status);
    }

    public static synchronized boolean consumeSaveFailure() {
        boolean failed = lastSaveResult != null && !lastSaveResult.success();
        if (failed) {
            lastSaveResult = WciCartPersistenceService.SaveResult.successResult();
        }
        return failed;
    }

    public static Optional<Path> cartPath() {
        return PERSISTENCE.currentPlayerPath();
    }

    private static synchronized void ensureCartLoaded() {
        Optional<UUID> playerUuid = PERSISTENCE.currentPlayerUuid();
        if (playerUuid.isEmpty()) {
            return;
        }
        if (playerUuid.get().equals(loadedPlayerUuid)) {
            return;
        }

        WciCartPersistenceService.LoadResult result = PERSISTENCE.loadCurrent(SHOPPING_CART_SERVICE.cart());
        loadedPlayerUuid = playerUuid.get();
        lastSaveResult = WciCartPersistenceService.SaveResult.successResult();
        if (result.success() && result.restored() && result.restoredEntries() > 0) {
            pendingStatus = "Restored WCI cart.";
        } else if (!result.success() && result.error() != null && !"Player unavailable".equals(result.error())) {
            pendingStatus = "WCI cart restore failed.";
        }
    }

    private static synchronized void persistCurrentCart(ShoppingCartService.MutationType mutationType) {
        if (loadedPlayerUuid == null) {
            ensureCartLoaded();
        }
        lastSaveResult = mutationType == ShoppingCartService.MutationType.CLEAR
                ? PERSISTENCE.deleteCurrent()
                : PERSISTENCE.saveCurrent(SHOPPING_CART_SERVICE.cart());
        if (!lastSaveResult.success()) {
            String operation = mutationType == ShoppingCartService.MutationType.CLEAR ? "delete" : "save";
            WynnExtras.LOGGER.error("[WynnExtras] WCI cart {} failed: {}", operation, lastSaveResult.error());
        }
    }

    private static WciScreenContext currentScreenContext() {
        if (MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> screen) {
            return WciScreenContext.detect(screen, false);
        }
        if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
            return WciScreenContext.CHAT;
        }
        if (MinecraftClient.getInstance().currentScreen == null) {
            return WciScreenContext.HUD;
        }
        return WciScreenContext.UNSUPPORTED;
    }

    private static void send(String message) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
    }
}
