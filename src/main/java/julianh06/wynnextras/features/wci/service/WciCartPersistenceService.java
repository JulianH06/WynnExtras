package julianh06.wynnextras.features.wci.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.wci.cart.ShoppingCart;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

public class WciCartPersistenceService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CART_FILE_NAME = "wci_cart.json";

    public Optional<UUID> currentPlayerUuid() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) {
                return Optional.empty();
            }
            return Optional.of(client.player.getUuid());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public Optional<Path> currentPlayerPath() {
        return currentPlayerUuid().map(this::pathForPlayer);
    }

    public Path pathForPlayer(UUID playerUuid) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("wynnextras")
                .resolve(playerUuid.toString())
                .resolve(CART_FILE_NAME);
    }

    public LoadResult loadCurrent(ShoppingCart cart) {
        Optional<Path> path = currentPlayerPath();
        if (path.isEmpty()) {
            return LoadResult.unavailableResult();
        }
        return load(cart, path.get());
    }

    public LoadResult load(ShoppingCart cart, Path path) {
        if (cart == null) {
            return LoadResult.errorResult("Cart is unavailable");
        }
        if (path == null) {
            return LoadResult.unavailableResult();
        }

        try {
            createParentDirectories(path);
            if (!Files.exists(path)) {
                cart.replaceWith(new ShoppingCart());
                return LoadResult.missingResult();
            }

            String json = Files.readString(path);
            if (json == null || json.isBlank()) {
                cart.replaceWith(new ShoppingCart());
                return LoadResult.missingResult();
            }

            ShoppingCart loadedCart = parseCart(json);
            cart.replaceWith(loadedCart);
            return LoadResult.restoredResult(loadedCart.entries().size());
        } catch (RuntimeException | IOException ex) {
            cart.replaceWith(new ShoppingCart());
            WynnExtras.LOGGER.error("[WynnExtras] Failed to load WCI cart: {}", ex.getMessage());
            return LoadResult.errorResult(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    public SaveResult saveCurrent(ShoppingCart cart) {
        Optional<Path> path = currentPlayerPath();
        if (path.isEmpty()) {
            return SaveResult.errorResult("Player unavailable");
        }
        return save(cart, path.get());
    }

    public SaveResult save(ShoppingCart cart, Path path) {
        if (cart == null) {
            return SaveResult.errorResult("Cart is unavailable");
        }
        if (path == null) {
            return SaveResult.errorResult("Path is unavailable");
        }

        try {
            createParentDirectories(path);
            Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempPath)) {
                GSON.toJson(WciCartData.fromCart(cart), writer);
            }
            moveIntoPlace(tempPath, path);
            return SaveResult.successResult();
        } catch (IOException | RuntimeException ex) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to save WCI cart: {}", ex.getMessage());
            return SaveResult.errorResult(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    public SaveResult deleteCurrent() {
        Optional<Path> path = currentPlayerPath();
        if (path.isEmpty()) {
            return SaveResult.errorResult("Player unavailable");
        }
        return delete(path.get());
    }

    public SaveResult delete(Path path) {
        if (path == null) {
            return SaveResult.errorResult("Path is unavailable");
        }

        try {
            Files.deleteIfExists(path);
            return SaveResult.successResult();
        } catch (IOException | RuntimeException ex) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to delete WCI cart: {}", ex.getMessage());
            return SaveResult.errorResult(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }

    public String serialize(ShoppingCart cart) {
        return GSON.toJson(WciCartData.fromCart(cart));
    }

    public ShoppingCart deserialize(String json) {
        if (json == null || json.isBlank()) {
            return new ShoppingCart();
        }
        try {
            return parseCart(json);
        } catch (RuntimeException ex) {
            WynnExtras.LOGGER.error("[WynnExtras] Failed to deserialize WCI cart: {}", ex.getMessage());
            return new ShoppingCart();
        }
    }

    private static ShoppingCart parseCart(String json) {
        WciCartData data = GSON.fromJson(json, WciCartData.class);
        return data == null ? new ShoppingCart() : data.toCart();
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static void moveIntoPlace(Path tempPath, Path targetPath) throws IOException {
        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record LoadResult(boolean success, boolean restored, boolean missing, int restoredEntries, String error) {
        public static LoadResult restoredResult(int restoredEntries) {
            return new LoadResult(true, true, false, Math.max(0, restoredEntries), null);
        }

        public static LoadResult missingResult() {
            return new LoadResult(true, false, true, 0, null);
        }

        public static LoadResult unavailableResult() {
            return new LoadResult(false, false, false, 0, "Player unavailable");
        }

        public static LoadResult errorResult(String error) {
            return new LoadResult(false, false, false, 0, error);
        }
    }

    public record SaveResult(boolean success, String error) {
        public static SaveResult successResult() {
            return new SaveResult(true, null);
        }

        public static SaveResult errorResult(String error) {
            return new SaveResult(false, error);
        }
    }
}
