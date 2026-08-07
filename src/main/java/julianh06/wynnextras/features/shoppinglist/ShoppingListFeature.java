package julianh06.wynnextras.features.shoppinglist;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingCart;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingCartService;
import julianh06.wynnextras.features.shoppinglist.service.ShoppingCartPersistenceService;
import julianh06.wynnextras.features.shoppinglist.service.WynnBuilderDecoder;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListScreenContext;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuExtension;
import julianh06.wynnextras.features.shoppinglist.ui.ShoppingListMenuLauncherButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WEModule
public final class ShoppingListFeature {
    private static final SubCommand ENABLE_COMMAND = new SubCommand("enable", "", ctx -> {
        send(ShoppingListMenuExtension.showFromCommand(currentScreenContext()).message());
        return 1;
    }, null, null);
    private static final SubCommand DISABLE_COMMAND = new SubCommand("disable", "", ctx -> {
        ShoppingListMenuExtension.close();
        send("Shopping list disabled.");
        return 1;
    }, null, null);
    private static final SubCommand CLEAR_COMMAND = new SubCommand("clear", "", ctx -> {
        boolean saved = ShoppingListMenuExtension.clearFromCommand();
        send(saved ? "Cleared shopping list." : "Cleared shopping list, but save failed.");
        return 1;
    }, null, null);
    private static final SubCommand COPY_COMMAND = new SubCommand("copy", "", ctx -> {
        ShoppingListMenuExtension.copyFromCommand();
        send("Copied shopping list.");
        return 1;
    }, null, null);
    private static final SubCommand RESET_POSITION_COMMAND = new SubCommand("resetposition", "", ctx -> {
        ShoppingListMenuExtension.resetPosition();
        ShoppingListMenuLauncherButton.resetPosition();
        send("Shopping List positions reset.");
        return 1;
    }, null, null);
    private static final Command SHOPPING_LIST_COMMAND = new Command("shoppinglist", "", ctx -> {
        send(ShoppingListMenuExtension.toggleFromCommand(currentScreenContext()).message());
        return 1;
    }, List.of(ENABLE_COMMAND, DISABLE_COMMAND, CLEAR_COMMAND, COPY_COMMAND, RESET_POSITION_COMMAND), null);

    private static final ShoppingCartService SHOPPING_CART_SERVICE =
            new ShoppingCartService(new ShoppingCart(), new WynnBuilderDecoder());
    private static final ShoppingCartPersistenceService PERSISTENCE = new ShoppingCartPersistenceService();
    private static UUID loadedPlayerUuid;
    private static ShoppingCartPersistenceService.SaveResult lastSaveResult = ShoppingCartPersistenceService.SaveResult.successResult();

    static {
        SHOPPING_CART_SERVICE.setAfterMutation(ShoppingListFeature::persistCurrentCart);
    }

    public ShoppingListFeature() {}

    public static ShoppingCartService shoppingCartService() {
        return SHOPPING_CART_SERVICE;
    }

    public static synchronized void loadPersistedCart() {
        ensureCartLoaded();
    }

    public static synchronized boolean consumeSaveFailure() {
        boolean failed = lastSaveResult != null && !lastSaveResult.success();
        if (failed) {
            lastSaveResult = ShoppingCartPersistenceService.SaveResult.successResult();
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

        PERSISTENCE.loadCurrent(SHOPPING_CART_SERVICE.cart());
        loadedPlayerUuid = playerUuid.get();
        lastSaveResult = ShoppingCartPersistenceService.SaveResult.successResult();
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
            WynnExtras.LOGGER.error("[WynnExtras] Shopping list cart {} failed: {}", operation, lastSaveResult.error());
        }
    }

    private static ShoppingListScreenContext currentScreenContext() {
        if (MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> screen) {
            return ShoppingListScreenContext.detect(screen, false);
        }
        if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
            return ShoppingListScreenContext.CHAT;
        }
        if (MinecraftClient.getInstance().currentScreen == null) {
            return ShoppingListScreenContext.HUD;
        }
        return ShoppingListScreenContext.UNSUPPORTED;
    }

    private static void send(String message) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
    }
}
