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

    public MutationResult addManual(ShoppingEntry entry, int amount, ExistingEntryPolicy policy) {
        return mutate(null, entry, amount, policy);
    }

    public MutationResult edit(ShoppingEntry original, ShoppingEntry replacement, int amount,
                               ExistingEntryPolicy policy) {
        if (original == null || !cart.contains(original)) {
            return MutationResult.error("Shopping list entry no longer exists");
        }
        return mutate(original, replacement, amount, policy);
    }

    public MutationResult remove(ShoppingEntry entry) {
        if (entry == null || !cart.remove(entry)) {
            return MutationResult.error("Shopping list entry no longer exists");
        }
        runAfterMutation(MutationType.REMOVE);
        return MutationResult.success(cart.count(entry));
    }

    private MutationResult mutate(ShoppingEntry original, ShoppingEntry replacement, int amount,
                                  ExistingEntryPolicy policy) {
        if (replacement == null) return MutationResult.error("Shopping list entry is missing");
        if (amount <= 0) return MutationResult.error("Amount must be positive");

        boolean identityChanged = original == null || !original.equals(replacement);
        if (identityChanged && cart.contains(replacement) && policy == null) {
            return MutationResult.conflict(cart.count(replacement));
        }

        ShoppingCart snapshot = cart.copy();
        try {
            if (original != null) cart.remove(original);
            if (identityChanged && snapshot.contains(replacement) && policy == ExistingEntryPolicy.ADD) {
                int existing = original != null && original.equals(replacement) ? 0 : snapshot.count(replacement);
                cart.set(replacement, Math.addExact(existing, amount));
            } else {
                cart.set(replacement, amount);
            }
            runAfterMutation(original == null ? MutationType.ADD : MutationType.EDIT);
            return MutationResult.success(cart.count(replacement));
        } catch (ArithmeticException | IllegalArgumentException ex) {
            cart.replaceWith(snapshot);
            return MutationResult.error(ex instanceof ArithmeticException ? "Amount is too large" : ex.getMessage());
        }
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
        CLEAR,
        ADD,
        EDIT,
        REMOVE
    }

    public enum ExistingEntryPolicy {
        ADD,
        REPLACE
    }

    public record MutationResult(MutationStatus status, int amount, String error) {
        public static MutationResult success(int amount) {
            return new MutationResult(MutationStatus.SUCCESS, amount, null);
        }

        public static MutationResult conflict(int amount) {
            return new MutationResult(MutationStatus.CONFLICT, amount, null);
        }

        public static MutationResult error(String error) {
            return new MutationResult(MutationStatus.ERROR, 0, error == null ? "Unknown error" : error);
        }

        public boolean success() {
            return status == MutationStatus.SUCCESS;
        }
    }

    public enum MutationStatus {
        SUCCESS,
        CONFLICT,
        ERROR
    }

    public record ImportResult(boolean success, int importedEntries, int totalItems, String error) {
        public static ImportResult success(int importedEntries, int totalItems) { return new ImportResult(true, importedEntries, totalItems, null); }
        public static ImportResult error(String error) { return new ImportResult(false, 0, 0, error); }
    }
}
