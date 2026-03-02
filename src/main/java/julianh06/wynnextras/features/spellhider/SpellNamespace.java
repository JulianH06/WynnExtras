package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.config.SpellHiderConfig;
import org.jetbrains.annotations.NotNull;

public record SpellNamespace(SpellNamespace parent, String name) {
    public static SpellNamespace from(String FQName) {
        String[] split = FQName.split(":");
        SpellNamespace prev = null;
        for (int i = 0; i <= split.length - 1; i++) {
            prev = new SpellNamespace(prev, split[i]);
        }
        return prev;
    }

    public SpellNamespace(SpellNamespace parent, @NotNull String name) {
        if (name.contains(":")) throw new IllegalArgumentException();
        this.parent = parent;
        this.name = name.toLowerCase();
    }

    public SpellNamespace(@NotNull String name) {
        this(null, name);
    }

    public SpellNamespace with(@NotNull String child) {
        return new SpellNamespace(this, child);
    }

    public SpellNamespace reskinned() {
        return from("reskinned:" + getFQName());
    }

    public void addId(String id) {
        SpellHiderConfig.INSTANCE.addSpellIdentifier(id, this);
    }

    public void addId(String id, String id2) {
        addId(id);
        addId(id2);
    }

    public void addId(String id, String id2, String id3) {
        addId(id, id2);
        addId(id3);
    }

    public void addId(String id, String id2, String id3, String id4) {
        addId(id, id2, id3);
        addId(id4);
    }

    public void addId(String id, String id2, String id3, String id4, String id5) {
        addId(id, id2, id3, id4);
        addId(id5);
    }

    public void addId(String id, String id2, String id3, String id4, String id5, String id6) {
        addId(id, id2, id3, id4, id5);
        addId(id6);
    }

    public void addId(String id, String id2, String id3, String id4, String id5, String id6, String id7) {
        addId(id, id2, id3, id4, id5, id6);
        addId(id7);
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
