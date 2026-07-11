// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.handlers.bossbar;

import julianh06.wynnextras.wtshim.handlers.bossbar.type.BossBarProgress;
import julianh06.wynnextras.wtshim.utils.type.CappedValue;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.hud.ClientBossBar;

public class TrackedBar {
    public final List<Pattern> patterns;

    private boolean rendered = true;
    private ClientBossBar event = null;
    private CappedValue value = CappedValue.EMPTY;

    public TrackedBar(Pattern pattern) {
        this.patterns = List.of(pattern);
    }

    public TrackedBar(List<Pattern> patterns) {
        this.patterns = Collections.unmodifiableList(patterns);
    }

    public void onUpdateName(Matcher match) {}

    public void onUpdateProgress(float progress) {}

    public boolean isRendered() {
        return rendered;
    }

    public void setEvent(ClientBossBar event) {
        this.event = event;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public float getTargetProgress() {
        // Deviation: Wynntils reads LerpingBossEvent#targetPercent via an accessor mixin. Yarn's
        // ClientBossBar overrides getPercent() to return the lerped (interpolated) value and exposes
        // no accessor for the raw target field (which lives on the abstract BossBar base). No shim
        // consumer currently calls getTargetProgress(), so we return the lerped current progress
        // instead of the raw target to avoid introducing a base-class accessor mixin.
        return event.getPercent();
    }

    public ClientBossBar getEvent() {
        return event;
    }

    protected void reset() {
        value = CappedValue.EMPTY;
        event = null;
        rendered = true;
    }

    public boolean isActive() {
        return event != null;
    }

    public BossBarProgress getBarProgress() {
        return isActive() ? new BossBarProgress(value, event.getPercent()) : null;
    }

    protected void updateValue(int current, int max) {
        value = new CappedValue(current, max);
    }
}
