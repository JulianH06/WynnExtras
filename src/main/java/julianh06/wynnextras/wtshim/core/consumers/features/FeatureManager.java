// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Minimal FeatureManager.
 * Holds instances of the stand-in Feature classes WynnExtras mixes into.
 */
package julianh06.wynnextras.wtshim.core.consumers.features;

import julianh06.wynnextras.wtshim.core.components.Manager;
import java.util.HashMap;
import java.util.Map;

public class FeatureManager extends Manager {
    private final Map<Class<? extends Feature>, Feature> instances = new HashMap<>();

    public <T extends Feature> void register(T feature) {
        instances.put(feature.getClass(), feature);
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T getFeatureInstance(Class<T> type) {
        Feature f = instances.get(type);
        return f == null ? null : (T) f;
    }
}
