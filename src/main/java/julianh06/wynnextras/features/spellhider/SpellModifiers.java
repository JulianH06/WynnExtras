package julianh06.wynnextras.features.spellhider;

import julianh06.wynnextras.core.WynnExtras;

import java.util.EnumMap;
import java.util.Map;

public class SpellModifiers {
    private final Map<SpellModifier, Object> values = new EnumMap<>(SpellModifier.class);

    public boolean set(SpellModifier key, Object value) {
        if (!key.getType().isInstance(value)) {
            WynnExtras.LOGGER.error("type miss-match on setting spell modifiers expected {} got {}", key.getType(), value.getClass());
            return false;
        }
        values.put(key, value);
        return true;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(SpellModifier key) {
        return (T) values.get(key);
    }

    @Override
    public String toString() {
        return "SpellModifiers{" + values + '}';
    }
}
