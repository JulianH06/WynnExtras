package julianh06.wynnextras.features.shoppinglist.service;

import julianh06.wynnextras.features.shoppinglist.cart.ShoppingCart;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingEntry;
import julianh06.wynnextras.features.shoppinglist.model.WynnBuilderBuild;

import java.util.function.Consumer;

public class ShoppingCartService {
    private final ShoppingCart cart;
    private final WynnBuilderDecoder decoder;
    private Consumer<MutationType> afterMutation = mutationType -> {};

    public ShoppingCartService(ShoppingCart cart, WynnBuilderDecoder decoder) { this.cart = cart; this.decoder = decoder; }
    public ShoppingCart cart() { return cart; }

    public void setAfterMutation(Runnable afterMutation) {
        this.afterMutation = afterMutation == null ? mutationType -> {} : ignored -> afterMutation.run();
    }

    public void setAfterMutation(Consumer<MutationType> afterMutation) {
        this.afterMutation = afterMutation == null ? mutationType -> {} : afterMutation;
    }

    public void clear() {
        cart.replaceWith(new ShoppingCart());
        runAfterMutation(MutationType.CLEAR);
    }

    public ImportResult importUrl(String url) {
        ShoppingCart snapshot = cart.copy();
        try {
            WynnBuilderBuild build = decoder.decode(url);
            build.requirements().forEach(req -> cart.add(new ShoppingEntry(
                    req.id(),
                    req.displayName(),
                    req.type(),
                    req.materialTier(),
                    req.source()), req.amount()));
            runAfterMutation(MutationType.IMPORT);
            return ImportResult.success(build.requirements().size(), cart.entries().values().stream().mapToInt(Integer::intValue).sum());
        } catch (RuntimeException e) {
            cart.replaceWith(snapshot);
            return ImportResult.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private void runAfterMutation(MutationType mutationType) {
        try {
            afterMutation.accept(mutationType);
        } catch (RuntimeException ignored) {
        }
    }

    public enum MutationType {
        IMPORT,
        CLEAR
    }

    public record ImportResult(boolean success, int importedEntries, int totalItems, String error) {
        public static ImportResult success(int importedEntries, int totalItems) { return new ImportResult(true, importedEntries, totalItems, null); }
        public static ImportResult error(String error) { return new ImportResult(false, 0, 0, error); }
    }
}
