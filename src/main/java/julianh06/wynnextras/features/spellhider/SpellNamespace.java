package julianh06.wynnextras.features.spellhider;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellNamespace {
    private static final Map<String, SpellNamespace> registry = new HashMap<>();

    public static SpellNamespace from(String FQName) {
        SpellNamespace known = registry.get(FQName);
        return known != null ? known : new SpellNamespace(FQName);
    }

    private final SpellNamespace parent;
    private final String name;
    private final List<String> aliases;

    private SpellNamespace(SpellNamespace parent, @NotNull String name) {
        if (name.contains(":")) throw new IllegalArgumentException();
        this.aliases = new ArrayList<>();
        this.parent = parent;
        this.name = name.toLowerCase();
        registry.put(this.getFQName(), this);
    }

    public SpellNamespace(@NotNull String name) {
        this(null, name);
    }

    public SpellNamespace with(@NotNull String child) {
        return new SpellNamespace(this, child);
    }

    public String getFQName() {
        return parent == null ? name : parent.getFQName() + ':' + name;
    }

    public void addModel(int model) {
        SpellHider.addModel(model, this);
    }

    public void addModel(int min, int max) {
        SpellHider.addModel(min, max, this);
    }

    public void addAlias(@NotNull String alias) {
        this.aliases.add(alias.toLowerCase());
    }

    public boolean modify(SpellModifier type, Object value) {
        return SpellHider.modify(this, type, value);
    }

    public boolean isRelevant(String query) {
        String lQuery = query.toLowerCase();
        if (getFQName().contains(lQuery)) return true;
        for (String alias : aliases) {
            if (alias.contains(lQuery)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getFQName();
    }
}
