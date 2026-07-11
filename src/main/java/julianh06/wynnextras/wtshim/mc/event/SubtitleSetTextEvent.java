// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * WynnExtras standalone compat shim (wtshim) with Mojmap->Yarn mappings.
 */
package julianh06.wynnextras.wtshim.mc.event;

import net.minecraft.text.Text;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SubtitleSetTextEvent extends Event implements ICancellableEvent {
    private final Text component;

    public SubtitleSetTextEvent(Text component) {
        this.component = component;
    }

    public Text getComponent() {
        return component;
    }
}
