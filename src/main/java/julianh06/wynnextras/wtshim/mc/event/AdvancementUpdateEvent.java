// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.Event;

public class AdvancementUpdateEvent extends Event {
    private final boolean reset;
    private final List<AdvancementEntry> added;
    private final Set<Identifier> removed;
    private final Map<Identifier, AdvancementProgress> progress;

    public AdvancementUpdateEvent(
            boolean reset,
            List<AdvancementEntry> added,
            Set<Identifier> removed,
            Map<Identifier, AdvancementProgress> progress) {
        this.reset = reset;
        this.added = added;
        this.removed = removed;
        this.progress = progress;
    }

    public boolean isReset() {
        return reset;
    }

    public List<AdvancementEntry> getAdded() {
        return added;
    }

    public Set<Identifier> getRemoved() {
        return removed;
    }

    public Map<Identifier, AdvancementProgress> getProgress() {
        return progress;
    }
}
