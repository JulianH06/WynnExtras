package julianh06.wynnextras.features.wci;

import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.wci.cart.ShoppingCart;
import julianh06.wynnextras.features.wci.service.ShoppingCartService;
import julianh06.wynnextras.features.wci.service.WciCartPersistenceService;
import julianh06.wynnextras.features.wci.service.WynnBuilderDecoder;
import julianh06.wynnextras.features.wci.service.WynnCraftIngredientService;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class WynnExtrasWciFeature {
    private static final WynnCraftIngredientService REGISTRY = new WynnCraftIngredientService();
    private static final ShoppingCartService SHOPPING_CART_SERVICE = new ShoppingCartService(new ShoppingCart(), new WynnBuilderDecoder(REGISTRY));
    private static final WciCartPersistenceService PERSISTENCE = new WciCartPersistenceService();
    private static UUID loadedPlayerUuid;
    private static String pendingStatus;
    private static WciCartPersistenceService.SaveResult lastSaveResult = WciCartPersistenceService.SaveResult.successResult();

    static {
        SHOPPING_CART_SERVICE.setAfterMutation(WynnExtrasWciFeature::persistCurrentCart);
    }

    private WynnExtrasWciFeature() {}

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
}
