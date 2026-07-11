// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Feature base class. Minimal subset used by WynnExtras mixins/targets.
 */
package julianh06.wynnextras.wtshim.core.consumers.features;

public abstract class Feature {
    protected boolean enabled = true;

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
