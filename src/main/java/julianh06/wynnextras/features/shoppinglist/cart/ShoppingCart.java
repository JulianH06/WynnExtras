package julianh06.wynnextras.features.shoppinglist.cart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShoppingCart {
    private final LinkedHashMap<ShoppingEntry, Integer> entries = new LinkedHashMap<>();
    public void add(ShoppingEntry entry, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int current = entries.getOrDefault(entry, 0);
        entries.put(entry, Math.addExact(current, amount));
    }
    public void set(ShoppingEntry entry, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        entries.remove(entry);
        entries.put(entry, amount);
    }
    public boolean remove(ShoppingEntry entry) { return entries.remove(entry) != null; }
    public boolean contains(ShoppingEntry entry) { return entries.containsKey(entry); }
    public int count(ShoppingEntry entry) { return entries.getOrDefault(entry, 0); }
    public Map<ShoppingEntry, Integer> entries() { return Collections.unmodifiableMap(entries); }
    public ShoppingCart copy() { ShoppingCart copy = new ShoppingCart(); entries.forEach(copy.entries::put); return copy; }
    public void replaceWith(ShoppingCart other) { entries.clear(); entries.putAll(other.entries); }
}
