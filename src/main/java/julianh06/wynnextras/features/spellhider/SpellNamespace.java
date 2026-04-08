package julianh06.wynnextras.features.spellhider;

import org.jetbrains.annotations.NotNull;

public record SpellNamespace(SpellNamespace parent, String name) {
    public static SpellNamespace from(String FQName) {
        String trimmed = FQName.trim().replaceAll("^:|:$", "");
        String[] split = trimmed.split(":");
        SpellNamespace prev = null;
        for (int i = 0; i <= split.length - 1; i++) {
            prev = new SpellNamespace(prev, split[i]);
        }
        return prev;
    }

    public SpellNamespace(SpellNamespace parent, @NotNull String name) {
        if (name.contains(":")) throw new IllegalArgumentException("please use SpellNamespace.from() when creating from an FQName");
        this.parent = parent;
        this.name = name.toLowerCase();
    }

    public SpellNamespace(@NotNull String name) {
        this(null, name);
    }

    public SpellNamespace with(@NotNull String child) {
        return new SpellNamespace(this, child);
    }

    public void addId(Integer hash) {
        SpellHiderMappings.INSTANCE.addSpellIdentifier(hash, this);
    }

    public String getFQName() {
        return parent == null ? name : parent.getFQName() + ':' + name;
    }

    public boolean modify(SpellModifier type, Object value) {
        return SpellHider.modify(this, type, value);
    }

    public boolean isRelevant(String query) {
        String lQuery = query.toLowerCase();
        return getFQName().contains(lQuery);
    }

    public boolean isEmpty() {
        return getFQName().isEmpty();
    }

    @Override
    public @NotNull String toString() {
        return getFQName();
    }
}
